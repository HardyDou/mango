import type { ApiId, HttpClient, HttpError, HttpProgress } from '../../../../packages/api-schema/src/index';

interface Order {
  id: ApiId;
  amount: string;
}

declare const client: HttpClient;
declare const signal: AbortSignal;

const order = client.request<Order>({
  method: 'GET',
  url: '/orders/100',
  query: { includeLines: true, tag: ['new', 'paid'] },
  signal,
  onDownloadProgress(progress: HttpProgress) {
    progress.loaded.toFixed();
  },
});

order.then((value) => value.id.toUpperCase());

export function isRetryable(error: HttpError) {
  return error.retryable && error.kind !== 'configuration';
}
