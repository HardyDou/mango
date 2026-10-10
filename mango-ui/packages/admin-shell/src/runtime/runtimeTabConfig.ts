import type { MangoRuntimeAppConfig } from '@mango/app-runtime';

export function createTabRuntimeConfig(
  config: MangoRuntimeAppConfig,
  tabKey: string,
  keepAlive = false,
): MangoRuntimeAppConfig {
  const baseInstanceId = config.instanceId?.trim() || config.appCode;
  const instanceId = !tabKey || tabKey === 'default' ? baseInstanceId : `${baseInstanceId}::tab-${hashTabKey(tabKey)}`;
  return {
    ...config,
    instanceId,
    alive: keepAlive || config.alive,
  };
}

export function hashTabKey(value: string): string {
  let hash = 2166136261;
  for (let index = 0; index < value.length; index += 1) {
    hash ^= value.charCodeAt(index);
    hash = Math.imul(hash, 16777619);
  }
  return (hash >>> 0).toString(16).padStart(8, '0');
}
