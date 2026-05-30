export type MangoPageLoader = () => Promise<unknown>;

export type MangoPageRegistry = {
  moduleCode: string;
  pages: Record<string, MangoPageLoader>;
};

export type MangoCapabilityPage = {
  component: string;
  loader: MangoPageLoader;
  menuCode?: string;
  permissions?: string[];
};

export type MangoCapabilityMenu = {
  menuCode: string;
  moduleCode: string;
  component?: string;
  permissions: string[];
  source: string;
};

export type MangoCapabilityBackend = {
  moduleCode: string;
  menuSource: string;
  resourceManifest?: string;
  requiredApis: string[];
};

export type MangoCapabilityRuntime = {
  modes: string[];
  defaultMode: string;
};

export type MangoCapabilityE2e = {
  smoke: string[];
  screenshots: string[];
  dataChecks: string[];
};

export type MangoCapabilityManifest = {
  moduleCode: string;
  packageName: string;
  capabilityCode: string;
  capabilityName: string;
  requires: string[];
  optional: string[];
  conflicts?: string[];
  backend: MangoCapabilityBackend;
  pages: MangoCapabilityPage[];
  menus: MangoCapabilityMenu[];
  permissions: string[];
  styles: string[];
  runtime: MangoCapabilityRuntime;
  e2e: MangoCapabilityE2e;
};

export type MangoCapabilityDependencyOrigin = 'selected' | 'catalog';

export type MangoCapabilityDependencyNode = {
  capabilityCode: string;
  packageName: string;
  origin: MangoCapabilityDependencyOrigin;
  requiredBy: string[];
  requires: string[];
  optional: string[];
  conflicts: string[];
};

export type MangoCapabilityDependencyReport = {
  preset: string;
  selectedCodes: string[];
  catalogCodes: string[];
  resolvedCodes: string[];
  autoInstalledCodes: string[];
  nodes: MangoCapabilityDependencyNode[];
  diagnostics: string[];
};

export type ResolveMangoCapabilityDependenciesOptions = {
  preset: string;
  selected: MangoCapabilityManifest[];
  catalog?: MangoCapabilityManifest[];
  autoInstallRequired?: boolean;
};

export type ResolveMangoCapabilityDependenciesResult = {
  capabilities: MangoCapabilityManifest[];
  diagnostics: string[];
  report: MangoCapabilityDependencyReport;
};

export type MangoShellPageLoaders = {
  home?: MangoPageLoader;
  notFound?: MangoPageLoader;
};

type MangoPageRegistryState = {
  pageLoaders: Map<string, MangoPageLoader>;
  moduleByPage: Map<string, string>;
};

const registryKey = Symbol.for('mango.admin-pages.registry');
const registryState = getGlobalRegistryState();
const pageLoaders = registryState.pageLoaders;
const moduleByPage = registryState.moduleByPage;

function getGlobalRegistryState(): MangoPageRegistryState {
  const globalScope = globalThis as typeof globalThis & {
    [registryKey]?: MangoPageRegistryState;
  };
  if (!globalScope[registryKey]) {
    globalScope[registryKey] = {
      pageLoaders: new Map<string, MangoPageLoader>(),
      moduleByPage: new Map<string, string>(),
    };
  }
  return globalScope[registryKey];
}

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

export function toPageRegistry(manifest: MangoCapabilityManifest): MangoPageRegistry {
  return {
    moduleCode: manifest.moduleCode,
    pages: Object.fromEntries(
      manifest.pages.map(page => [page.component, page.loader]),
    ),
  };
}

export function registerCapabilityPages(manifest: MangoCapabilityManifest) {
  registerModulePages(toPageRegistry(manifest));
}

export function registerCapabilities(manifests: MangoCapabilityManifest[]) {
  manifests.forEach(registerCapabilityPages);
}

export function resolveMangoCapabilityDependencies(
  options: ResolveMangoCapabilityDependenciesOptions,
): ResolveMangoCapabilityDependenciesResult {
  const selectedCodes = uniqueCodes(options.selected);
  const catalog = options.catalog || [];
  const catalogMap = toCapabilityMap(catalog);
  const resolvedMap = new Map<string, MangoCapabilityManifest>();
  const selectedSet = new Set<string>();
  const autoInstalledSet = new Set<string>();
  const requiredBy = new Map<string, Set<string>>();
  const diagnostics: string[] = [];

  for (const capability of options.selected) {
    if (selectedSet.has(capability.capabilityCode)) {
      diagnostics.push(`duplicate selected capability ${capability.capabilityCode}`);
      continue;
    }
    selectedSet.add(capability.capabilityCode);
    resolvedMap.set(capability.capabilityCode, capability);
  }

  const visiting: string[] = [];
  const visited = new Set<string>();

  const ensureCapability = (capabilityCode: string, parentCode?: string) => {
    if (parentCode) {
      addRequiredBy(requiredBy, capabilityCode, parentCode);
    }
    if (resolvedMap.has(capabilityCode)) {
      return resolvedMap.get(capabilityCode);
    }
    const catalogCapability = catalogMap.get(capabilityCode);
    if (!catalogCapability) {
      const parent = parentCode ? `${parentCode} requires missing capability ${capabilityCode}` : `missing capability ${capabilityCode}`;
      diagnostics.push(parent);
      return undefined;
    }
    if (!options.autoInstallRequired) {
      const parent = parentCode ? `${parentCode} requires missing capability ${capabilityCode}` : `missing capability ${capabilityCode}`;
      diagnostics.push(parent);
      return undefined;
    }
    resolvedMap.set(capabilityCode, catalogCapability);
    autoInstalledSet.add(capabilityCode);
    return catalogCapability;
  };

  const visit = (capability: MangoCapabilityManifest) => {
    const capabilityCode = capability.capabilityCode;
    if (visiting.includes(capabilityCode)) {
      diagnostics.push(`circular capability dependency detected: ${[...visiting.slice(visiting.indexOf(capabilityCode)), capabilityCode].join(' -> ')}`);
      return;
    }
    if (visited.has(capabilityCode)) {
      return;
    }
    visiting.push(capabilityCode);
    for (const dependencyCode of capability.requires || []) {
      const dependency = ensureCapability(dependencyCode, capabilityCode);
      if (dependency) {
        visit(dependency);
      }
    }
    visiting.pop();
    visited.add(capabilityCode);
  };

  for (const capability of Array.from(resolvedMap.values())) {
    visit(capability);
  }

  for (const capability of Array.from(resolvedMap.values())) {
    for (const conflictCode of capability.conflicts || []) {
      if (resolvedMap.has(conflictCode)) {
        diagnostics.push(`${capability.capabilityCode} conflicts with capability ${conflictCode}`);
      }
    }
  }

  const ordered = orderCapabilities(resolvedMap, selectedCodes);
  const nodes = ordered.map(capability => ({
    capabilityCode: capability.capabilityCode,
    packageName: capability.packageName,
    origin: selectedSet.has(capability.capabilityCode) ? 'selected' as const : 'catalog' as const,
    requiredBy: [...(requiredBy.get(capability.capabilityCode) || new Set<string>())],
    requires: [...(capability.requires || [])],
    optional: [...(capability.optional || [])],
    conflicts: [...(capability.conflicts || [])],
  }));
  const report: MangoCapabilityDependencyReport = {
    preset: options.preset,
    selectedCodes,
    catalogCodes: uniqueCodes(catalog),
    resolvedCodes: ordered.map(capability => capability.capabilityCode),
    autoInstalledCodes: ordered
      .map(capability => capability.capabilityCode)
      .filter(capabilityCode => autoInstalledSet.has(capabilityCode)),
    nodes,
    diagnostics,
  };

  return {
    capabilities: ordered,
    diagnostics,
    report,
  };
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

function uniqueCodes(capabilities: MangoCapabilityManifest[]) {
  return [...new Set(capabilities.map(capability => capability.capabilityCode))];
}

function toCapabilityMap(capabilities: MangoCapabilityManifest[]) {
  const map = new Map<string, MangoCapabilityManifest>();
  for (const capability of capabilities) {
    map.set(capability.capabilityCode, capability);
  }
  return map;
}

function addRequiredBy(requiredBy: Map<string, Set<string>>, dependencyCode: string, parentCode: string) {
  const parents = requiredBy.get(dependencyCode) || new Set<string>();
  parents.add(parentCode);
  requiredBy.set(dependencyCode, parents);
}

function orderCapabilities(resolvedMap: Map<string, MangoCapabilityManifest>, preferredOrder: string[]) {
  const ordered: MangoCapabilityManifest[] = [];
  const visited = new Set<string>();
  const preferredOrderSet = new Set(preferredOrder);
  const allCapabilities = [
    ...Array.from(resolvedMap.values()).filter(capability => !preferredOrderSet.has(capability.capabilityCode)),
    ...preferredOrder
      .map(capabilityCode => resolvedMap.get(capabilityCode))
      .filter((capability): capability is MangoCapabilityManifest => Boolean(capability)),
  ];

  const append = (capability: MangoCapabilityManifest) => {
    if (visited.has(capability.capabilityCode)) {
      return;
    }
    visited.add(capability.capabilityCode);
    for (const dependencyCode of capability.requires || []) {
      const dependency = resolvedMap.get(dependencyCode);
      if (dependency) {
        append(dependency);
      }
    }
    ordered.push(capability);
  };

  for (const capability of allCapabilities) {
    append(capability);
  }
  return ordered;
}
