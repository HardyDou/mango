import { createApp, type App as VueApp } from 'vue';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { installShellErrorHandler, isElementPlusMessageBoxCancellation } from '../errorHandling';

const mocks = vi.hoisted(() => ({
  messageError: vi.fn(),
}));

vi.mock('@mango/common/utils/message', () => ({
  mangoMessage: { error: mocks.messageError },
}));

describe('admin shell error handling', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it.each(['cancel', 'close', { action: 'cancel' }, { action: 'close' }])(
    'recognizes Element Plus MessageBox cancellation result %#',
    (error) => {
      expect(isElementPlusMessageBoxCancellation(error)).toBe(true);
    },
  );

  it.each([undefined, null, '', 'confirm', new Error('render failed'), { action: 'confirm' }, { action: 0 }])(
    'does not classify a genuine error as MessageBox cancellation: %#',
    (error) => {
      expect(isElementPlusMessageBoxCancellation(error)).toBe(false);
    },
  );

  it('silently handles cancellation in every Shell-created Vue app', () => {
    const outerApp = createTestApp();
    const runtimePageApp = createTestApp();
    installShellErrorHandler(outerApp);
    installShellErrorHandler(runtimePageApp);
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined);

    outerApp.config.errorHandler?.('cancel', null, 'component event handler');
    runtimePageApp.config.errorHandler?.({ action: 'close' }, null, 'component event handler');

    expect(consoleError).not.toHaveBeenCalled();
    expect(mocks.messageError).not.toHaveBeenCalled();
    consoleError.mockRestore();
  });

  it('keeps genuine Vue errors visible to the existing global fallback', () => {
    const app = createTestApp();
    const error = new Error('render failed');
    installShellErrorHandler(app);
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined);

    app.config.errorHandler?.(error, null, 'render function');

    expect(consoleError).toHaveBeenCalledWith('[mango-shell] Vue error:', error);
    expect(mocks.messageError).toHaveBeenCalledWith('系统错误，请刷新页面');
    consoleError.mockRestore();
  });

  it('keeps request errors out of the duplicate system fallback', () => {
    const app = createTestApp();
    const requestError = { response: { status: 500 } };
    installShellErrorHandler(app);
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined);

    app.config.errorHandler?.(requestError, null, 'component event handler');

    expect(consoleError).toHaveBeenCalledWith('[mango-shell] Vue error:', requestError);
    expect(mocks.messageError).not.toHaveBeenCalled();
    consoleError.mockRestore();
  });
});

function createTestApp(): VueApp {
  return createApp({ render: () => null });
}
