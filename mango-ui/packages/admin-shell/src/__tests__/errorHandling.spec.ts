import { beforeEach, describe, expect, it, vi } from 'vitest';
import { markErrorHandled } from '@mango/common/utils/errorHandling';
import { reportUnhandledError } from '../errorHandling';

const mocks = vi.hoisted(() => ({
  messageError: vi.fn(),
}));

vi.mock('@mango/common/utils/message', () => ({
  mangoMessage: { error: mocks.messageError },
}));

describe('admin shell global error boundary', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('reports a genuinely unhandled error once', () => {
    const error = new Error('render failure');

    reportUnhandledError(error);
    reportUnhandledError(error);

    expect(mocks.messageError).toHaveBeenCalledTimes(1);
    expect(mocks.messageError).toHaveBeenCalledWith('系统错误，请刷新页面');
  });

  it('does not report an error already handled by a request or business boundary', () => {
    const error = markErrorHandled(new Error('business failure'));

    reportUnhandledError(error);

    expect(mocks.messageError).not.toHaveBeenCalled();
  });
});
