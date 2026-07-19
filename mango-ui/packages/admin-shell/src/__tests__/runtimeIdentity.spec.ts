import { describe, expect, it } from 'vitest';
import { normalizeRuntimeConfig, type MangoRuntimeAppConfig } from '@mango/app-runtime';
import { resolveRuntimeAppConfig, toRuntimeApps } from '../runtime/runtimeIdentity';

describe('admin shell runtime identity', () => {
  it('selects the exact routed instance when appCode is shared', () => {
    const config = normalizeRuntimeConfig({
      profile: 'micro',
      modules: {
        'cms-brand-a': microModule('/cms-a/'),
        'cms-brand-b': microModule('/cms-b/'),
      },
    });
    const apps = toRuntimeApps(config);

    expect(resolveRuntimeAppConfig(apps, 'mango-admin-cms-app', 'mango-admin-cms-app:cms-brand-a')?.entryUrl).toBe(
      '/cms-a/',
    );
    expect(resolveRuntimeAppConfig(apps, 'mango-admin-cms-app', 'mango-admin-cms-app:cms-brand-b')?.entryUrl).toBe(
      '/cms-b/',
    );
  });

  it('does not fall through to a sibling when an instance identity is unknown', () => {
    const apps = [runtimeApp('cms-a'), runtimeApp('cms-b')];

    expect(resolveRuntimeAppConfig(apps, 'mango-admin-cms-app', 'cms-missing')).toBeUndefined();
  });

  it('fails closed when a shared appCode is routed without an instance identity', () => {
    const apps = [runtimeApp('cms-a'), runtimeApp('cms-b')];

    expect(resolveRuntimeAppConfig(apps, 'mango-admin-cms-app')).toBeUndefined();
  });

  it('keeps the legacy appCode fallback only when the match is unique', () => {
    const apps = [runtimeApp('cms-a')];

    expect(resolveRuntimeAppConfig(apps, 'mango-admin-cms-app')).toBe(apps[0]);
  });
});

function microModule(entry: string) {
  return {
    mode: 'micro' as const,
    runtimeCode: 'mango-admin-cms-app',
    entry,
  };
}

function runtimeApp(instanceId: string): MangoRuntimeAppConfig {
  return {
    appCode: 'mango-admin-cms-app',
    instanceId,
    appName: instanceId,
    appType: 'MICRO_APP',
    deployMode: 'REMOTE',
    entryUrl: `/${instanceId}/`,
    status: 1,
  };
}
