import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { HttpClient } from '@mango/api-schema';
import {
  createRuntimeEventBus,
  microAppAdapter,
  normalizeRuntimeConfig,
  resolveRuntimeInstanceId,
  type MangoAppRuntime,
  type MangoRuntimeAppConfig,
} from './index';

const wujie = vi.hoisted(() => ({
  destroyApp: vi.fn(),
  preloadApp: vi.fn(),
  startApp: vi.fn(),
}));

vi.mock('wujie', () => wujie);

describe('micro app instance isolation', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('uses instanceId for Wujie identity and destroys only the selected runtime', async () => {
    const destroyFirst = vi.fn();
    const destroySecond = vi.fn();
    wujie.startApp.mockResolvedValueOnce(destroyFirst).mockResolvedValueOnce(destroySecond);
    const first = runtime('orders-a');
    const second = runtime('orders-b');
    const container = { innerHTML: '' } as HTMLElement;

    await microAppAdapter.mount(config('orders-a'), container, first.value);
    await microAppAdapter.mount(config('orders-b'), container, second.value);

    expect(wujie.startApp).toHaveBeenNthCalledWith(1, expect.objectContaining({ name: 'orders-a' }));
    expect(wujie.startApp).toHaveBeenNthCalledWith(2, expect.objectContaining({ name: 'orders-b' }));

    await microAppAdapter.unmount?.(config('orders-a'));
    expect(destroyFirst).toHaveBeenCalledTimes(1);
    expect(first.dispose).toHaveBeenCalledTimes(1);
    expect(destroySecond).not.toHaveBeenCalled();
    expect(second.dispose).not.toHaveBeenCalled();

    await microAppAdapter.unmount?.(config('orders-b'));
    expect(destroySecond).toHaveBeenCalledTimes(1);
    expect(second.dispose).toHaveBeenCalledTimes(1);
  });

  it('falls back to appCode when an instance id is absent or blank', () => {
    expect(resolveRuntimeInstanceId({ appCode: 'orders' })).toBe('orders');
    expect(resolveRuntimeInstanceId({ appCode: 'orders', instanceId: '  ' })).toBe('orders');
  });

  it('derives route-slot identities when one app is mounted by multiple modules', () => {
    const normalized = normalizeRuntimeConfig({
      profile: 'micro',
      modules: {
        'orders-east': microModule('https://orders.example.test/east'),
        'orders-west': microModule('https://orders.example.test/west'),
      },
    });

    expect(normalized.modules['orders-east'].instanceId).toBe('orders:orders-east');
    expect(normalized.modules['orders-west'].instanceId).toBe('orders:orders-west');
    expect(normalized.diagnostics).toEqual([]);
  });

  it('rejects duplicate explicit micro app identities', () => {
    const normalized = normalizeRuntimeConfig({
      profile: 'micro',
      modules: {
        'orders-east': { ...microModule('https://orders.example.test/east'), instanceId: 'orders-primary' },
        'orders-west': { ...microModule('https://orders.example.test/west'), instanceId: 'orders-primary' },
      },
    });

    expect(normalized.diagnostics).toContainEqual(
      expect.objectContaining({
        level: 'error',
        moduleCode: 'orders-west',
        field: 'instanceId',
      }),
    );
  });
});

function microModule(entry: string) {
  return {
    mode: 'micro' as const,
    runtimeCode: 'orders',
    entry,
  };
}

function config(instanceId: string): MangoRuntimeAppConfig {
  return {
    appCode: 'orders',
    instanceId,
    appName: 'Orders',
    appType: 'MICRO_APP',
    deployMode: 'REMOTE',
    entryUrl: 'http://orders.example.test/',
    status: 1,
  };
}

function runtime(instanceId: string) {
  const dispose = vi.fn();
  const httpClient: HttpClient = { request: vi.fn() };
  const value: MangoAppRuntime = {
    instanceId,
    token: 'token',
    appCode: 'orders',
    userInfo: {},
    permissions: [],
    theme: {},
    request: {
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      delete: vi.fn(),
    },
    httpClient,
    dispose,
    eventBus: createRuntimeEventBus(),
  };
  return { dispose, value };
}
