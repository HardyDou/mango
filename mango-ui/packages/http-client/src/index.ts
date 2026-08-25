import axios from 'axios';
import type {
  AxiosError,
  AxiosInstance,
  AxiosProgressEvent,
  AxiosRequestConfig,
  AxiosResponse,
  ResponseType,
} from 'axios';
import type {
  HttpClient,
  HttpError,
  HttpFailureKind,
  HttpHeaders,
  HttpProgress,
  HttpRequest,
  HttpResponseType,
} from '@mango/api-schema';

type MaybePromise<T> = T | Promise<T>;
type ContextValue = string | null | undefined;

export type MangoHttpClientState = 'active' | 'inactive' | 'destroyed';

export interface MangoHttpClientOptions {
  baseUrl: string;
  timeoutMs?: number;
  defaultHeaders?: HttpHeaders;
  getAccessToken?: () => MaybePromise<ContextValue>;
  getTenantId?: () => MaybePromise<ContextValue>;
  getTraceId?: () => MaybePromise<ContextValue>;
  refreshAccessToken?: (signal: AbortSignal) => Promise<ContextValue>;
  onUnauthorized?: (error: HttpError) => MaybePromise<void>;
  tenantHeaderNames?: readonly string[];
  traceHeaderName?: string;
  maxRetries?: number;
  retryDelayMs?: number;
  authExpiredCodes?: readonly (number | string)[];
}

export interface MangoHttpClient extends HttpClient {
  readonly state: MangoHttpClientState;
  activate(): void;
  deactivate(reason?: string): void;
  destroy(): void;
}

interface ApiEnvelope {
  code?: number | string;
  data?: unknown;
  message?: string;
  msg?: string;
  success?: boolean;
}

interface DispatchContext {
  controller: AbortController;
  request: HttpRequest<unknown>;
  refreshedToken?: string;
}

const DEFAULT_TIMEOUT_MS = 30_000;
const DEFAULT_AUTH_EXPIRED_CODES = [401, 1410, 1411] as const;
const DEFAULT_TENANT_HEADERS = ['X-Mango-Tenant-Id', 'TENANT-ID'] as const;
const SAFE_RETRY_METHODS = new Set(['GET', 'HEAD', 'OPTIONS']);

export class MangoHttpError extends Error implements HttpError {
  readonly name = 'HttpError' as const;
  readonly kind: HttpFailureKind;
  readonly status?: number;
  readonly code?: string;
  readonly retryable: boolean;
  readonly requestId?: string;
  readonly details?: unknown;

  constructor(
    message: string,
    options: {
      kind: HttpFailureKind;
      status?: number;
      code?: string;
      retryable?: boolean;
      requestId?: string;
      details?: unknown;
    },
  ) {
    super(message);
    this.kind = options.kind;
    this.status = options.status;
    this.code = options.code;
    this.retryable = options.retryable ?? false;
    this.requestId = options.requestId;
    this.details = options.details;
  }
}

export function createMangoHttpClient(options: MangoHttpClientOptions): MangoHttpClient {
  validateOptions(options);
  return new AxiosMangoHttpClient(options);
}

class AxiosMangoHttpClient implements MangoHttpClient {
  #state: MangoHttpClientState = 'active';
  #activationController = new AbortController();
  readonly #transport: AxiosInstance;
  readonly #options: Required<Pick<MangoHttpClientOptions, 'maxRetries' | 'retryDelayMs' | 'timeoutMs'>> &
    MangoHttpClientOptions;
  readonly #pending = new Set<AbortController>();
  readonly #requestInterceptorId: number;
  readonly #responseInterceptorId: number;
  #refreshPromise: Promise<ContextValue> | null = null;

  constructor(options: MangoHttpClientOptions) {
    this.#options = {
      ...options,
      maxRetries: options.maxRetries ?? 0,
      retryDelayMs: options.retryDelayMs ?? 0,
      timeoutMs: options.timeoutMs ?? DEFAULT_TIMEOUT_MS,
    };
    this.#transport = axios.create({
      baseURL: options.baseUrl,
      timeout: this.#options.timeoutMs,
    });
    this.#requestInterceptorId = this.#transport.interceptors.request.use((config) => {
      if (this.#state === 'destroyed') {
        throw configurationError('HTTP client has been destroyed');
      }
      return config;
    });
    this.#responseInterceptorId = this.#transport.interceptors.response.use(
      (response) => response,
      (error: unknown) => Promise.reject(error),
    );
  }

  get state(): MangoHttpClientState {
    return this.#state;
  }

  activate(): void {
    if (this.#state === 'destroyed') {
      throw configurationError('HTTP client has been destroyed');
    }
    if (this.#state === 'inactive') this.#activationController = new AbortController();
    this.#state = 'active';
  }

  deactivate(reason = 'HTTP client deactivated'): void {
    if (this.#state === 'destroyed') return;
    this.#state = 'inactive';
    this.#activationController.abort(reason);
    this.#refreshPromise = null;
    this.#abortPending(reason);
  }

  destroy(): void {
    if (this.#state === 'destroyed') return;
    this.#state = 'destroyed';
    this.#activationController.abort('HTTP client destroyed');
    this.#abortPending('HTTP client destroyed');
    this.#refreshPromise = null;
    this.#transport.interceptors.request.eject(this.#requestInterceptorId);
    this.#transport.interceptors.response.eject(this.#responseInterceptorId);
  }

  async request<TResponse = unknown, TBody = unknown>(request: HttpRequest<TBody>): Promise<TResponse> {
    this.#assertActive();
    validateRequest(request);

    const controller = new AbortController();
    const unlinkSignal = linkAbortSignal(request.signal, controller);
    const context: DispatchContext = {
      controller,
      request: request as HttpRequest<unknown>,
    };
    this.#pending.add(controller);

    let streamOwnedCleanup = false;
    const cleanup = () => {
      unlinkSignal();
      this.#pending.delete(controller);
    };
    try {
      const response = await this.#execute<TResponse>(context);
      if (request.responseType === 'stream' && isReadableStream(response)) {
        streamOwnedCleanup = true;
        return managedReadableStream(response, cleanup) as TResponse;
      }
      return response;
    } finally {
      if (!streamOwnedCleanup) cleanup();
    }
  }

  async #execute<TResponse>(context: DispatchContext): Promise<TResponse> {
    let retries = 0;
    let refreshAttempted = false;

    while (true) {
      this.#throwIfAborted(context.controller.signal);
      try {
        const response = await this.#dispatch(context);
        return this.#decode<TResponse>(response);
      } catch (failure) {
        const error = normalizeFailure(failure, context.controller.signal, this.#options.authExpiredCodes);

        if (
          error.kind === 'unauthorized' &&
          !refreshAttempted &&
          this.#options.refreshAccessToken &&
          requestUsesAuth(context.request)
        ) {
          refreshAttempted = true;
          const refreshedToken = await this.#refresh();
          this.#throwIfAborted(context.controller.signal);
          if (refreshedToken) {
            context.refreshedToken = refreshedToken;
            continue;
          }
        }

        if (error.retryable && retries < this.#options.maxRetries && isRetryAllowed(context.request)) {
          retries += 1;
          await waitForRetry(this.#options.retryDelayMs, context.controller.signal);
          continue;
        }

        if (error.kind === 'unauthorized' && requestUsesAuth(context.request)) {
          try {
            await this.#options.onUnauthorized?.(error);
          } catch {
            // Host-side unauthorized handling must not replace the normalized request failure.
          }
        }
        throw error;
      }
    }
  }

  async #dispatch(context: DispatchContext): Promise<AxiosResponse<unknown>> {
    const request = context.request;
    const headers = await this.#createHeaders(request, context.refreshedToken);
    const config: AxiosRequestConfig = {
      method: request.method,
      url: request.url,
      data: request.body,
      params: request.query,
      headers,
      signal: context.controller.signal,
      timeout: request.timeoutMs ?? this.#options.timeoutMs,
      responseType: toAxiosResponseType(request.responseType),
      adapter: request.responseType === 'stream' ? 'fetch' : undefined,
      onUploadProgress: request.onUploadProgress
        ? (event) => request.onUploadProgress?.(toHttpProgress(event))
        : undefined,
      onDownloadProgress: request.onDownloadProgress
        ? (event) => request.onDownloadProgress?.(toHttpProgress(event))
        : undefined,
    };
    return this.#transport.request(config);
  }

  async #createHeaders(
    request: HttpRequest<unknown>,
    refreshedToken?: string,
  ): Promise<Record<string, string | string[]>> {
    const headers = mergeHeaders(this.#options.defaultHeaders, request.headers);
    if (request.idempotencyKey) headers['Idempotency-Key'] = request.idempotencyKey;
    if (!requestUsesAuth(request)) return headers;

    const [accessToken, tenantId, traceId] = await Promise.all([
      refreshedToken ?? this.#options.getAccessToken?.(),
      this.#options.getTenantId?.(),
      this.#options.getTraceId?.(),
    ]);
    if (accessToken) headers.Authorization = `Bearer ${accessToken}`;
    if (tenantId) {
      for (const name of this.#options.tenantHeaderNames ?? DEFAULT_TENANT_HEADERS) {
        headers[name] = tenantId;
      }
    }
    if (traceId) headers[this.#options.traceHeaderName ?? 'X-Trace-Id'] = traceId;
    return headers;
  }

  #decode<TResponse>(response: AxiosResponse<unknown>): TResponse {
    const body = response.data;
    if (!isApiEnvelope(body)) return body as TResponse;
    if (body.success === true || body.code === 200 || body.code === '200') {
      return body.data as TResponse;
    }

    const code = body.code === undefined ? undefined : String(body.code);
    const unauthorized = isExpiredCode(code, this.#options.authExpiredCodes);
    throw new MangoHttpError(body.message || body.msg || 'Request failed', {
      kind: unauthorized ? 'unauthorized' : 'protocol',
      status: response.status,
      code,
      retryable: isRetryableStatus(response.status),
      requestId: responseHeader(response, 'x-request-id') ?? responseHeader(response, 'x-trace-id'),
      details: body.data,
    });
  }

  #refresh(): Promise<ContextValue> {
    if (this.#refreshPromise) return this.#refreshPromise;
    const promise = Promise.resolve()
      .then(() => this.#options.refreshAccessToken?.(this.#activationController.signal))
      .then((token) => token)
      .catch(() => null)
      .finally(() => {
        if (this.#refreshPromise === promise) this.#refreshPromise = null;
      });
    this.#refreshPromise = promise;
    return promise;
  }

  #assertActive(): void {
    if (this.#state !== 'active') {
      throw configurationError(`HTTP client is ${this.#state}`);
    }
  }

  #throwIfAborted(signal: AbortSignal): void {
    if (signal.aborted) {
      throw new MangoHttpError('Request aborted', { kind: 'aborted', retryable: false });
    }
  }

  #abortPending(reason: string): void {
    for (const controller of this.#pending) controller.abort(reason);
    this.#pending.clear();
  }
}

function validateOptions(options: MangoHttpClientOptions): void {
  if (!options.baseUrl.trim()) throw configurationError('baseUrl is required');
  if ((options.timeoutMs ?? DEFAULT_TIMEOUT_MS) <= 0) throw configurationError('timeoutMs must be positive');
  if ((options.maxRetries ?? 0) < 0 || !Number.isInteger(options.maxRetries ?? 0)) {
    throw configurationError('maxRetries must be a non-negative integer');
  }
  if ((options.retryDelayMs ?? 0) < 0) throw configurationError('retryDelayMs must be non-negative');
}

function validateRequest(request: HttpRequest<unknown>): void {
  if (!request.url.trim()) throw configurationError('request url is required');
  if (/^[a-z][a-z\d+.-]*:/iu.test(request.url) || request.url.startsWith('//')) {
    throw configurationError('request url must be relative');
  }
}

function configurationError(message: string): MangoHttpError {
  return new MangoHttpError(message, { kind: 'configuration', retryable: false });
}

function requestUsesAuth(request: HttpRequest<unknown>): boolean {
  return request.metadata?.auth !== 'none';
}

function isRetryAllowed(request: HttpRequest<unknown>): boolean {
  return SAFE_RETRY_METHODS.has(request.method) || Boolean(request.idempotencyKey);
}

function mergeHeaders(...inputs: Array<HttpHeaders | undefined>): Record<string, string | string[]> {
  const result: Record<string, string | string[]> = {};
  for (const headers of inputs) {
    for (const [key, value] of Object.entries(headers ?? {})) {
      if (value === undefined) delete result[key];
      else result[key] = typeof value === 'string' ? value : [...value];
    }
  }
  return result;
}

function toAxiosResponseType(responseType?: HttpResponseType): ResponseType | undefined {
  return responseType === 'arrayBuffer' ? 'arraybuffer' : responseType;
}

function isReadableStream(value: unknown): value is ReadableStream<Uint8Array> {
  return Boolean(value && typeof value === 'object' && 'getReader' in value && typeof value.getReader === 'function');
}

function managedReadableStream(source: ReadableStream<Uint8Array>, cleanup: () => void): ReadableStream<Uint8Array> {
  const reader = source.getReader();
  let finished = false;
  const finish = () => {
    if (finished) return;
    finished = true;
    cleanup();
  };
  return new ReadableStream<Uint8Array>({
    async pull(controller) {
      try {
        const result = await reader.read();
        if (result.done) {
          controller.close();
          finish();
          return;
        }
        controller.enqueue(result.value);
      } catch (error) {
        controller.error(error);
        finish();
      }
    },
    async cancel(reason) {
      try {
        await reader.cancel(reason);
      } finally {
        finish();
      }
    },
  });
}

function toHttpProgress(event: AxiosProgressEvent): HttpProgress {
  return {
    loaded: event.loaded,
    total: event.total,
    progress: event.progress,
    bytesPerSecond: event.rate,
  };
}

function isApiEnvelope(value: unknown): value is ApiEnvelope {
  return Boolean(
    value &&
    typeof value === 'object' &&
    ('success' in value || ('code' in value && ('data' in value || 'message' in value || 'msg' in value))),
  );
}

function isExpiredCode(
  code: string | undefined,
  configured: readonly (number | string)[] = DEFAULT_AUTH_EXPIRED_CODES,
): boolean {
  return code !== undefined && configured.some((value) => String(value) === code);
}

function normalizeFailure(
  failure: unknown,
  signal: AbortSignal,
  authExpiredCodes: readonly (number | string)[] = DEFAULT_AUTH_EXPIRED_CODES,
): MangoHttpError {
  if (failure instanceof MangoHttpError) return failure;
  if (signal.aborted) {
    return new MangoHttpError('Request aborted', { kind: 'aborted', retryable: false });
  }
  if (!axios.isAxiosError(failure)) {
    return new MangoHttpError(messageOf(failure, 'Request failed'), {
      kind: 'unknown',
      retryable: false,
    });
  }

  const error = failure as AxiosError<unknown>;
  const status = error.response?.status;
  const body = error.response?.data;
  const envelope = isApiEnvelope(body) ? body : undefined;
  const code = envelope?.code === undefined ? error.code : String(envelope.code);
  const unauthorized = status === 401 || isExpiredCode(code, authExpiredCodes);
  const timeout = error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT';
  const aborted = error.code === 'ERR_CANCELED';
  const kind: HttpFailureKind = aborted
    ? 'aborted'
    : timeout
      ? 'timeout'
      : unauthorized
        ? 'unauthorized'
        : error.response
          ? 'protocol'
          : error.request
            ? 'network'
            : 'configuration';

  return new MangoHttpError(envelope?.message || envelope?.msg || error.message || 'Request failed', {
    kind,
    status,
    code,
    retryable: kind === 'network' || kind === 'timeout' || isRetryableStatus(status),
    requestId: error.response
      ? (responseHeader(error.response, 'x-request-id') ?? responseHeader(error.response, 'x-trace-id'))
      : undefined,
    details: envelope?.data ?? body,
  });
}

function isRetryableStatus(status?: number): boolean {
  return status === 408 || status === 429 || (status !== undefined && status >= 500);
}

function responseHeader(response: AxiosResponse<unknown>, name: string): string | undefined {
  const value = response.headers?.[name];
  if (Array.isArray(value)) return value[0];
  return value === undefined || value === null ? undefined : String(value);
}

function messageOf(value: unknown, fallback: string): string {
  return value instanceof Error && value.message ? value.message : fallback;
}

function linkAbortSignal(source: AbortSignal | undefined, target: AbortController): () => void {
  if (!source) return () => undefined;
  if (source.aborted) {
    target.abort(source.reason);
    return () => undefined;
  }
  const abort = () => target.abort(source.reason);
  source.addEventListener('abort', abort, { once: true });
  return () => source.removeEventListener('abort', abort);
}

function waitForRetry(delayMs: number, signal: AbortSignal): Promise<void> {
  if (delayMs === 0) return Promise.resolve();
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      signal.removeEventListener('abort', abort);
      resolve();
    }, delayMs);
    const abort = () => {
      clearTimeout(timer);
      reject(new MangoHttpError('Request aborted', { kind: 'aborted', retryable: false }));
    };
    signal.addEventListener('abort', abort, { once: true });
  });
}
