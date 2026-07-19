import type { MangoRuntimeAppConfig, MangoRuntimeConfig } from '@mango/app-runtime';

export function toRuntimeApps(config: MangoRuntimeConfig): MangoRuntimeAppConfig[] {
  return Object.entries(config.modules)
    .filter(([, module]) => module.mode === 'micro' && module.entry)
    .map(([moduleCode, module]) => ({
      appCode: module.runtimeCode || moduleCode,
      instanceId: module.instanceId,
      appName: module.runtimeCode || moduleCode,
      appType: module.appType || 'MICRO_APP',
      deployMode: 'REMOTE',
      entryUrl: module.entry,
      styleUrl: module.style,
      framework: module.framework || 'vue3',
      sandboxEnabled: false,
      styleIsolation: 'NONE',
      status: 1,
      timeoutMs: module.timeoutMs || 15000,
      preload: module.preload === true,
      alive: module.alive === true,
    }));
}

export function resolveRuntimeAppConfig(apps: MangoRuntimeAppConfig[], runtimeCode: string, instanceId?: string) {
  if (instanceId) {
    return apps.find((app) => app.appCode === runtimeCode && app.instanceId === instanceId);
  }
  const matches = apps.filter((app) => app.appCode === runtimeCode);
  return matches.length === 1 ? matches[0] : undefined;
}
