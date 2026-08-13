import { afterEach, describe, expect, it, vi } from 'vitest';
import { claimUnhandledError, isErrorHandled, markErrorHandled } from '../errorHandling';

describe('error handling ownership', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('lets only the first global boundary claim an object error', () => {
    const error = new Error('boom');

    expect(claimUnhandledError(error)).toBe(true);
    expect(isErrorHandled(error)).toBe(true);
    expect(claimUnhandledError(error)).toBe(false);
  });

  it('recognizes an error already handled by a lower layer', () => {
    const error = new Error('business failure');

    expect(markErrorHandled(error)).toBe(error);
    expect(claimUnhandledError(error)).toBe(false);
  });

  it('deduplicates primitive rejection reasons for the current event turn', () => {
    vi.useFakeTimers();

    expect(claimUnhandledError('cancel')).toBe(true);
    expect(claimUnhandledError('cancel')).toBe(false);
    vi.runAllTimers();
    expect(claimUnhandledError('cancel')).toBe(true);
  });
});
