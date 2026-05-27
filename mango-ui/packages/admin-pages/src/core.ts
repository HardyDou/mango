export type MangoPageLoader = () => Promise<unknown>;

export type MangoPageRegistry = {
  moduleCode: string;
  pages: Record<string, MangoPageLoader>;
};

export type MangoShellPageLoaders = {
  home?: MangoPageLoader;
  notFound?: MangoPageLoader;
};

const pageLoaders = new Map<string, MangoPageLoader>();
const moduleByPage = new Map<string, string>();

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

function normalizeRoutePath(path?: string) {
  return (path || '')
    .replace(/^#/, '')
    .replace(/^\//, '')
    .replace(/\/$/, '');
}
