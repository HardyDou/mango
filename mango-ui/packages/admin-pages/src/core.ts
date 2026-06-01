export type MangoPageLoader = () => Promise<unknown>;

export type MangoPageRegistry = {
  moduleCode: string;
  pages: Record<string, MangoPageLoader>;
  routes?: MangoPageRoute[];
};

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

export function normalizeComponentPath(componentPath?: string) {
  return (componentPath || '')
    .replace(/^@\//, '')
    .replace(/^\//, '')
    .replace(/^src\//, '')
    .replace(/^views\//, '')
    .replace(/\.vue$/, '');
}

export function registerModulePages(registry: MangoPageRegistry) {
  Object.entries(registry.pages).forEach(([component, loader]) => {
    const normalized = normalizeComponentPath(component);
    pageLoaders.set(`${registry.moduleCode}:${normalized}`, loader);
    moduleByPage.set(normalized, registry.moduleCode);
  });
  if (registry.routes?.length) {
    const routes = routeRegistries.get(registry.moduleCode) || [];
    registry.routes.forEach((route) => {
      const normalizedRoute = normalizePageRoute(route);
      const existingIndex = routes.findIndex(item => item.path === normalizedRoute.path);
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
    .flatMap(([moduleCode, routes]) => routes.map(route => ({ ...route, moduleCode })));
}

function normalizeRoutePath(path?: string) {
  return (path || '')
    .replace(/^#/, '')
    .replace(/^\//, '')
    .replace(/\/$/, '');
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
