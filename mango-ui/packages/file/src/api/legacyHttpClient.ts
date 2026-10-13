import { request } from '@mango/common';
import type { HttpClient, HttpProgress, HttpRequest } from '@mango/api-schema';

let client: HttpClient | undefined;

/** Compatibility transport for consumers that have not installed a provider yet. */
export function getLegacyHttpClient(): HttpClient {
  if (client) return client;
  client = {
    request: async <TResponse = unknown>(input: HttpRequest): Promise<TResponse> => {
      const controller = new AbortController();
      const unlinkSignal = linkSignal(input.signal, controller);
      const inactivityTimeoutMs = positiveTimeout(input.inactivityTimeoutMs);
      let inactivityTimer: ReturnType<typeof setTimeout> | undefined;
      const resetInactivityTimer = () => {
        if (!inactivityTimeoutMs) return;
        if (inactivityTimer) clearTimeout(inactivityTimer);
        inactivityTimer = setTimeout(() => controller.abort('HTTP request inactivity timeout'), inactivityTimeoutMs);
      };
      resetInactivityTimer();
      const onUploadProgress =
        input.onUploadProgress || inactivityTimeoutMs
          ? (event: { loaded: number; total?: number; progress?: number; rate?: number }) => {
              resetInactivityTimer();
              input.onUploadProgress?.(toHttpProgress(event));
            }
          : undefined;
      try {
        const response = await request.request({
          method: input.method,
          url: input.url,
          data: input.body,
          params: input.query,
          headers: input.headers,
          signal: controller.signal,
          timeout: input.timeoutMs,
          responseType: input.responseType === 'arrayBuffer' ? 'arraybuffer' : input.responseType,
          rawResponse: input.responseType === 'blob',
          onUploadProgress,
        } as never);
        return response as TResponse;
      } catch (error) {
        if (controller.signal.aborted) {
          throw new Error('Request aborted or timed out');
        }
        throw error;
      } finally {
        unlinkSignal();
        if (inactivityTimer) clearTimeout(inactivityTimer);
      }
    },
  };
  return client;
}

function linkSignal(source: AbortSignal | undefined, controller: AbortController) {
  if (!source) return () => undefined;
  if (source.aborted) controller.abort(source.reason);
  const abort = () => controller.abort(source.reason);
  source.addEventListener('abort', abort, { once: true });
  return () => source.removeEventListener('abort', abort);
}

function positiveTimeout(value?: number) {
  return value && Number.isFinite(value) && value > 0 ? value : 0;
}

function toHttpProgress(event: {
  loaded: number;
  total?: number;
  progress?: number;
  bytes?: number;
  rate?: number;
  lengthComputable?: boolean;
}): HttpProgress {
  return {
    loaded: event.loaded,
    total: event.total,
    progress: event.progress,
    bytesPerSecond: event.rate,
    bytes: event.bytes,
    lengthComputable: event.lengthComputable,
  };
}
