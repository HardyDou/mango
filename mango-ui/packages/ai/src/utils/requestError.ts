import type { HttpError } from '@mango/api-schema';

export function isRequestAborted(error: unknown): boolean {
  if (!(error instanceof Error)) {
    return false;
  }
  return error.name === 'AbortError' || (error as Partial<HttpError>).kind === 'aborted';
}

export function requestErrorMessage(error: unknown, defaultMessage: string): string {
  if (!(error instanceof Error) || !error.message) return defaultMessage;
  const message = error.message.trim();
  if (/^Request failed(?: with status code \d{3})?$/i.test(message) || /^Network Error$/i.test(message)) {
    return defaultMessage;
  }
  return message;
}

export function isDialogCancellation(error: unknown): boolean {
  return error === 'cancel' || error === 'close';
}
