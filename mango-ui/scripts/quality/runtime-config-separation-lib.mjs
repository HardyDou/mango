const DEV_ENTRY_PATTERNS = [
  /https?:\/\/(?:localhost|127\.0\.0\.1):518[1-4]\b/u,
  /https?:\/\/[a-e]\.mango\.io(?::\d+)?/u,
];

export function validateRuntimeConfigSeparation(deployConfig, developmentConfig, distSources = []) {
  const failures = [];
  const deployModules = Object.values(deployConfig?.modules ?? {});
  if (deployConfig?.profile !== 'monolith') {
    failures.push('public runtime config must use the production-safe monolith profile');
  }
  if (deployModules.length === 0 || deployModules.some((module) => module.mode !== 'local')) {
    failures.push('public runtime config must contain only local modules');
  }
  const deploySource = JSON.stringify(deployConfig);
  if (DEV_ENTRY_PATTERNS.some((pattern) => pattern.test(deploySource))) {
    failures.push('public runtime config contains a development micro-app entry');
  }

  const developmentModules = Object.values(developmentConfig?.modules ?? {});
  if (!developmentModules.some((module) => module.mode === 'micro' && module.entry)) {
    failures.push('development runtime config must contain at least one explicit micro-app entry');
  }
  for (const source of distSources) {
    if (/^runtime-config\.(?!json$).+\.json$/u.test(source.path)) {
      failures.push(`production dist contains a non-deploy runtime config: ${source.path}`);
    }
    if (DEV_ENTRY_PATTERNS.some((pattern) => pattern.test(source.content))) {
      failures.push(`production dist contains a development micro-app entry: ${source.path}`);
    }
  }
  return failures;
}
