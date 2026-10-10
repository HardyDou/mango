import { describe, expect, it } from 'vitest';
import { createTabRuntimeConfig } from '../runtime/runtimeTabConfig';
import type { MangoRuntimeAppConfig } from '@mango/app-runtime';

const baseConfig: MangoRuntimeAppConfig = {
  appCode: 'mango-admin-rbac-app',
  instanceId: 'mango-admin-rbac-app',
  appName: 'RBAC',
  appType: 'MICRO_APP',
  deployMode: 'REMOTE',
  entryUrl: 'http://rbac.example.test/',
  status: 1,
};

describe('runtime tab configuration', () => {
  it('keeps Wujie alive when a micro app tab is cached', () => {
    const config = createTabRuntimeConfig(baseConfig, '/system/role::role', true);

    expect(config.instanceId).toMatch(/^mango-admin-rbac-app::tab-/);
    expect(config.alive).toBe(true);
  });

  it('preserves the configured alive state for non-cached tabs', () => {
    expect(createTabRuntimeConfig(baseConfig, '/system/role::role').alive).toBeUndefined();
    expect(createTabRuntimeConfig({ ...baseConfig, alive: true }, '/system/role::role').alive).toBe(true);
  });
});
