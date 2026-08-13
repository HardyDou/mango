const handledErrors = new WeakSet<object>();
const handledPrimitiveErrors = new Set<unknown>();

export function markErrorHandled<T>(error: T): T {
  if (isObject(error)) {
    handledErrors.add(error);
  } else {
    handledPrimitiveErrors.add(error);
    setTimeout(() => handledPrimitiveErrors.delete(error), 0);
  }
  return error;
}

export function isErrorHandled(error: unknown): boolean {
  return isObject(error) ? handledErrors.has(error) : handledPrimitiveErrors.has(error);
}

export function claimUnhandledError(error: unknown): boolean {
  if (isErrorHandled(error)) {
    return false;
  }
  markErrorHandled(error);
  return true;
}

function isObject(value: unknown): value is object {
  return (typeof value === 'object' && value !== null) || typeof value === 'function';
}
