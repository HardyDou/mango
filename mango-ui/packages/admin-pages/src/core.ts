export type MangoPageLoader = () => Promise<unknown>;

export type MangoPageRegistry = {
  moduleCode: string;
  pages: Record<string, MangoPageLoader>;
  routes?: MangoPageRoute[];
  packageName?: string;
  packageVersion?: string;
};

export type MangoRegisteredModulePages = Readonly<{
  moduleCode: string;
  packageName?: string;
  packageVersion?: string;
  pages: readonly string[];
}>;

export type MangoPageProbeStage = Readonly<{
  status: 'PASS' | 'FAIL' | 'SKIPPED';
  reasonCode: string;
}>;

export type MangoPageProbeResult = Readonly<{
  moduleCode: string;
  component: string;
  packageName?: string;
  actualVersion?: string;
  status: 'PASS' | 'FAIL';
  reasonCode: string;
  stages: Readonly<{
    registration: MangoPageProbeStage;
    loader: MangoPageProbeStage;
    component: MangoPageProbeStage;
  }>;
}>;

export type MangoShellPageLoaders = {
  home?: MangoPageLoader;
  notFound?: MangoPageLoader;
};

export type MangoPageRoute = {
  path: string;
  component: string;
  menuName?: string;
  menuCode?: string;
  icon?: string;
  sort?: number;
  visible?: number;
  keepAlive?: number;
};

const pageLoaders = new Map<string, MangoPageLoader>();
const moduleByPage = new Map<string, string>();
const routeRegistries = new Map<string, MangoPageRoute[]>();
const moduleRegistrations = new Map<string, { packageName?: string; packageVersion?: string }>();

export function normalizeComponentPath(componentPath?: string) {
  return (componentPath || '')
    .replace(/^@\//, '')
    .replace(/^\//, '')
    .replace(/^src\//, '')
    .replace(/^views\//, '')
    .replace(/\.vue$/, '');
}

export function registerModulePages(registry: MangoPageRegistry) {
  const existingRegistration = moduleRegistrations.get(registry.moduleCode);
  moduleRegistrations.set(registry.moduleCode, {
    packageName: registry.packageName || existingRegistration?.packageName,
    packageVersion: registry.packageVersion || existingRegistration?.packageVersion,
  });
  Object.entries(registry.pages).forEach(([component, loader]) => {
    const normalized = normalizeComponentPath(component);
    pageLoaders.set(`${registry.moduleCode}:${normalized}`, loader);
    moduleByPage.set(normalized, registry.moduleCode);
  });
  if (registry.routes?.length) {
    const routes = routeRegistries.get(registry.moduleCode) || [];
    registry.routes.forEach((route) => {
      const normalizedRoute = normalizePageRoute(route);
      const existingIndex = routes.findIndex((item) => item.path === normalizedRoute.path);
      if (existingIndex >= 0) {
        routes[existingIndex] = normalizedRoute;
        return;
      }
      routes.push(normalizedRoute);
    });
    routeRegistries.set(registry.moduleCode, routes);
  }
}

export function registerPage(moduleCode: string, component: string, loader: MangoPageLoader) {
  if (!moduleRegistrations.has(moduleCode)) {
    moduleRegistrations.set(moduleCode, {});
  }
  const normalized = normalizeComponentPath(component);
  pageLoaders.set(`${moduleCode}:${normalized}`, loader);
  moduleByPage.set(normalized, moduleCode);
}

export function registerShellPages(loaders: MangoShellPageLoaders) {
  const pages: Record<string, MangoPageLoader> = {};
  if (loaders.home) {
    pages['home/index'] = loaders.home;
  }
  if (loaders.notFound) {
    pages['error/404'] = loaders.notFound;
    pages['error/not-found'] = loaders.notFound;
  }
  if (Object.keys(pages).length > 0) {
    registerModulePages({
      moduleCode: 'mango-shell',
      pages,
    });
  }
}

export function getPageLoader(moduleCode?: string, component?: string) {
  const normalized = normalizeComponentPath(component);
  if (moduleCode) {
    return pageLoaders.get(`${moduleCode}:${normalized}`);
  }
  for (const [key, loader] of pageLoaders.entries()) {
    if (key.endsWith(`:${normalized}`)) {
      return loader;
    }
  }
  return undefined;
}

export function resolvePageModuleCode(component?: string, path?: string) {
  const normalizedComponent = normalizeComponentPath(component);
  const byComponent = moduleByPage.get(normalizedComponent);
  if (byComponent) {
    return byComponent;
  }
  const normalizedRoute = normalizeRoutePath(path);
  const routeAsIndex = normalizedRoute ? `${normalizedRoute}/index` : '';
  return moduleByPage.get(routeAsIndex) || moduleByPage.get(normalizedRoute);
}

export function getRegisteredPageRoutes(moduleCodes?: string[]) {
  const enabledModules = moduleCodes?.length ? new Set(moduleCodes) : undefined;
  return Array.from(routeRegistries.entries())
    .filter(([moduleCode]) => !enabledModules || enabledModules.has(moduleCode))
    .flatMap(([moduleCode, routes]) => routes.map((route) => ({ ...route, moduleCode })));
}

export function getRegisteredModulePagesSnapshot(moduleCode?: string): readonly MangoRegisteredModulePages[] {
  const modules = Array.from(moduleRegistrations.entries())
    .filter(([registeredModule]) => !moduleCode || registeredModule === moduleCode)
    .map(([registeredModule, metadata]) => {
      const prefix = `${registeredModule}:`;
      const pages = Array.from(pageLoaders.keys())
        .filter((key) => key.startsWith(prefix))
        .map((key) => key.slice(prefix.length))
        .sort();
      return Object.freeze({
        moduleCode: registeredModule,
        packageName: metadata.packageName,
        packageVersion: metadata.packageVersion,
        pages: Object.freeze(pages),
      });
    })
    .sort((left, right) => left.moduleCode.localeCompare(right.moduleCode));
  return Object.freeze(modules);
}

export async function probeRegisteredPage(moduleCode: string, component: string): Promise<MangoPageProbeResult> {
  const normalized = normalizeComponentPath(component);
  const registration = moduleRegistrations.get(moduleCode);
  const loader = pageLoaders.get(`${moduleCode}:${normalized}`);
  if (!registration || !loader) {
    return probeResult(moduleCode, normalized, registration, 'PAGE_NOT_REGISTERED', {
      registration: stage('FAIL', 'PAGE_NOT_REGISTERED'),
      loader: stage('SKIPPED', 'REGISTRATION_REQUIRED'),
      component: stage('SKIPPED', 'LOADER_REQUIRED'),
    });
  }
  let loaded: unknown;
  try {
    loaded = await loader();
  } catch (error) {
    const reasonCode = isChunkLoadError(error) ? 'CHUNK_LOAD_FAILED' : 'LOADER_REJECTED';
    return probeResult(moduleCode, normalized, registration, reasonCode, {
      registration: stage('PASS', 'PAGE_REGISTERED'),
      loader: stage('FAIL', reasonCode),
      component: stage('SKIPPED', 'LOADER_REQUIRED'),
    });
  }
  if (!isVueComponent(loaded)) {
    return probeResult(moduleCode, normalized, registration, 'VUE_COMPONENT_INVALID', {
      registration: stage('PASS', 'PAGE_REGISTERED'),
      loader: stage('PASS', 'LOADER_RESOLVED'),
      component: stage('FAIL', 'VUE_COMPONENT_INVALID'),
    });
  }
  return probeResult(moduleCode, normalized, registration, 'PAGE_RUNTIME_READY', {
    registration: stage('PASS', 'PAGE_REGISTERED'),
    loader: stage('PASS', 'LOADER_RESOLVED'),
    component: stage('PASS', 'VUE_COMPONENT_RESOLVED'),
  });
}

function stage(status: MangoPageProbeStage['status'], reasonCode: string): MangoPageProbeStage {
  return Object.freeze({ status, reasonCode });
}

function probeResult(
  moduleCode: string,
  component: string,
  registration: { packageName?: string; packageVersion?: string } | undefined,
  reasonCode: string,
  stages: MangoPageProbeResult['stages'],
): MangoPageProbeResult {
  return Object.freeze({
    moduleCode,
    component,
    packageName: registration?.packageName,
    actualVersion: registration?.packageVersion,
    status: reasonCode === 'PAGE_RUNTIME_READY' ? 'PASS' : 'FAIL',
    reasonCode,
    stages: Object.freeze(stages),
  });
}

function isVueComponent(value: unknown) {
  const candidate =
    value && typeof value === 'object' && 'default' in value ? (value as { default?: unknown }).default : value;
  if (typeof candidate === 'function') {
    return true;
  }
  if (!candidate || typeof candidate !== 'object') {
    return false;
  }
  const component = candidate as Record<string, unknown>;
  return (
    typeof component.setup === 'function' ||
    typeof component.render === 'function' ||
    typeof component.__name === 'string' ||
    typeof component.name === 'string' ||
    typeof component.__file === 'string'
  );
}

function isChunkLoadError(error: unknown) {
  if (!(error instanceof Error)) {
    return false;
  }
  const signal = `${error.name} ${error.message}`.toLowerCase();
  return (
    signal.includes('chunkloaderror') ||
    signal.includes('dynamically imported module') ||
    signal.includes('dynamic import') ||
    signal.includes('failed to fetch') ||
    signal.includes('404')
  );
}

function normalizeRoutePath(path?: string) {
  return (path || '').replace(/^#/, '').replace(/^\//, '').replace(/\/$/, '');
}

function normalizePageRoute(route: MangoPageRoute): MangoPageRoute {
  return {
    ...route,
    path: route.path.startsWith('/') ? route.path : `/${route.path}`,
    component: normalizeComponentPath(route.component),
    visible: route.visible ?? 0,
    keepAlive: route.keepAlive ?? 0,
  };
}
