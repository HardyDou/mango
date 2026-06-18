import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import type { AliasOptions } from 'vite';

export type MangoFrontendMode = 'source' | 'package';

export type MangoAliasEntry = {
  find: string | RegExp;
  replacement: string;
};

export type MangoWorkspaceAliasOptions = {
  appDir: string;
  appSrcAlias?: string;
  includeStyles?: boolean;
};

export type MangoPackageModeDistOptions = {
  command?: 'serve' | 'build';
};

type PackageEntry = {
  name: string;
  entries: Record<string, string>;
};

type AdminModuleRegistrar = {
  import?: string;
};

type AdminModuleEntry = {
  packageName?: string;
  name?: string;
  style?: string;
  registrars?: AdminModuleRegistrar[];
};

type AdminModulesManifest = {
  defaultPackages?: AdminModuleEntry[];
  fullPackages?: AdminModuleEntry[];
};

const BASE_PACKAGE_ENTRIES: PackageEntry[] = [
  {
    name: 'admin',
    entries: {
      '.': 'src/index.ts',
      full: 'src/full.ts',
    },
  },
  {
    name: 'admin-shell',
    entries: {
      '.': 'src/index.ts',
      runtime: 'src/runtime/runtimeHost.ts',
      menu: 'src/runtime/menuHost.ts',
      stores: 'src/stores/index.ts',
      router: 'src/router.ts',
      home: 'src/views/home/index.vue',
      'dev-pages': 'src/views/demo/registerDevPages.ts',
      'dev-base-pages': 'src/views/demo/registerBaseDevPages.ts',
      'dev-upload-page': 'src/views/demo/components/UploadView.vue',
      'dev-workflow-page': 'src/views/demo/components/WorkflowComponentsView.vue',
    },
  },
  {
    name: 'admin-pages',
    entries: {
      '.': 'src/index.ts',
      core: 'src/core.ts',
      defaults: 'src/defaults.ts',
      'dev-pages': 'src/dev-pages.ts',
      'dev-component-pages': 'src/devComponentPages.ts',
      features: 'src/features.ts',
      notice: 'src/notice.ts',
    },
  },
  {
    name: 'app-runtime',
    entries: {
      '.': 'src/index.ts',
    },
  },
];

export function resolveMangoFrontendMode(value = process.env.MANGO_FRONTEND_MODE): MangoFrontendMode {
  if (!value || value === 'source') {
    return 'source';
  }
  if (value === 'package') {
    return 'package';
  }
  throw new Error(`Invalid MANGO_FRONTEND_MODE: ${value}. Expected "source" or "package".`);
}

export function createMangoWorkspaceAliases(options: MangoWorkspaceAliasOptions): AliasOptions {
  const frontendMode = resolveMangoFrontendMode();
  const repoRoot = resolve(options.appDir, '../..');
  const aliases: MangoAliasEntry[] = [];

  if (options.appSrcAlias) {
    aliases.push({ find: '@', replacement: options.appSrcAlias });
  }

  if (frontendMode === 'package') {
    aliases.push({ find: 'vue-i18n', replacement: 'vue-i18n/dist/vue-i18n.cjs.js' });
    return aliases;
  }

  if (options.includeStyles !== false) {
    aliases.push(
      { find: '@mango/admin/style.css', replacement: resolve(repoRoot, 'packages/admin/style.css') },
      { find: '@mango/admin/style-full.css', replacement: resolve(repoRoot, 'packages/admin/style-full.css') },
    );

    for (const packageName of getConfiguredStylePackages(repoRoot)) {
      aliases.push({
        find: `@mango/${packageName}/style.css`,
        replacement: resolveSourceStylePath(repoRoot, packageName),
      });
    }
  }

  aliases.push(
    { find: '@mango/common/theme/index.css', replacement: resolve(repoRoot, 'packages/common/theme/index.css') },
    { find: '@mango/common/theme/index.scss', replacement: resolve(repoRoot, 'packages/common/theme/index.scss') },
    { find: /^@mango\/common\/(.*)$/, replacement: `${resolve(repoRoot, 'packages/common')}/$1` },
    { find: /^@mango\/common$/, replacement: resolve(repoRoot, 'packages/common/index.ts') },
  );

  for (const packageEntry of getSourcePackageEntries(repoRoot)) {
    const packageRoot = resolve(repoRoot, 'packages', packageEntry.name);
    for (const [entryName, entryPath] of Object.entries(packageEntry.entries)) {
      aliases.push({
        find: createPackageEntryMatcher(packageEntry.name, entryName),
        replacement: resolve(packageRoot, entryPath),
      });
    }
  }

  aliases.push({ find: 'vue-i18n', replacement: 'vue-i18n/dist/vue-i18n.cjs.js' });
  return aliases;
}

export function assertMangoPackageModeDist(appDir: string, options: MangoPackageModeDistOptions = {}): void {
  if (resolveMangoFrontendMode() !== 'package') {
    return;
  }
  if (options.command && options.command !== 'serve') {
    return;
  }

  const repoRoot = resolve(appDir, '../..');
  const stylePackages = getConfiguredStylePackages(repoRoot);
  const distPackages = getSourcePackageEntries(repoRoot)
    .map(packageEntry => packageEntry.name)
    .filter(packageName => packageName !== 'app-runtime');
  const missing = [
    ...distPackages.map(packageName => resolve(repoRoot, 'packages', packageName, 'dist/index.js')),
    ...stylePackages.map(packageName => resolve(repoRoot, 'packages', packageName, 'dist/style.css')),
  ].filter(path => !existsSync(path));

  if (missing.length > 0) {
    throw new Error(
      [
        'MANGO_FRONTEND_MODE=package requires built package artifacts.',
        'Run package builds before package-mode validation.',
        ...missing.map(path => `Missing: ${path}`),
      ].join('\n'),
    );
  }
}

function createPackageEntryMatcher(packageName: string, entryName: string): string | RegExp {
  const escapedName = packageName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  if (entryName === '.') {
    return new RegExp(`^@mango/${escapedName}$`);
  }
  return `@mango/${packageName}/${entryName}`;
}

function resolveSourceStylePath(repoRoot: string, packageName: string): string {
  if (packageName === 'common') {
    return resolve(repoRoot, 'packages/common/theme/index.css');
  }
  const packageStylePath = resolve(repoRoot, 'packages', packageName, 'style.css');
  if (existsSync(packageStylePath)) {
    return packageStylePath;
  }
  return resolve(repoRoot, 'build-config/source-package-style.css');
}

function getConfiguredStylePackages(repoRoot: string): string[] {
  const manifest = readAdminModulesManifest(repoRoot);
  const modules = [...(manifest.defaultPackages || []), ...(manifest.fullPackages || [])];
  const packages: string[] = [];
  const seen = new Set<string>();

  for (const module of modules) {
    const packageName = module.packageName || module.name;
    const style = module.style;
    if (!packageName || !style) {
      continue;
    }
    const expectedStyle = `${packageName}/style.css`;
    if (style !== expectedStyle || !packageName.startsWith('@mango/')) {
      continue;
    }
    const packageFolder = packageName.slice('@mango/'.length);
    if (!seen.has(packageFolder)) {
      packages.push(packageFolder);
      seen.add(packageFolder);
    }
  }

  return packages;
}

function getSourcePackageEntries(repoRoot: string): PackageEntry[] {
  const entries = new Map<string, Record<string, string>>();

  for (const packageEntry of BASE_PACKAGE_ENTRIES) {
    entries.set(packageEntry.name, { ...packageEntry.entries });
  }

  for (const module of getConfiguredAdminModules(repoRoot)) {
    const packageName = module.packageName || module.name;
    if (!packageName?.startsWith('@mango/')) {
      continue;
    }
    const packageFolder = packageName.slice('@mango/'.length);
    if (packageFolder === 'common' || entries.has(packageFolder)) {
      continue;
    }

    const moduleEntries: Record<string, string> = {};
    const packageRoot = resolve(repoRoot, 'packages', packageFolder);
    if (existsSync(resolve(packageRoot, 'src/index.ts'))) {
      moduleEntries['.'] = 'src/index.ts';
    }

    for (const registrar of module.registrars || []) {
      const importPath = registrar.import || '';
      const subpathPrefix = `${packageName}/`;
      if (!importPath.startsWith(subpathPrefix)) {
        continue;
      }
      const subpath = importPath.slice(subpathPrefix.length);
      const sourcePath = `src/${subpath}.ts`;
      if (existsSync(resolve(packageRoot, sourcePath))) {
        moduleEntries[subpath] = sourcePath;
      }
    }

    if (Object.keys(moduleEntries).length > 0) {
      entries.set(packageFolder, moduleEntries);
    }
  }

  return Array.from(entries, ([name, packageEntries]) => ({ name, entries: packageEntries }));
}

function getConfiguredAdminModules(repoRoot: string): AdminModuleEntry[] {
  const manifest = readAdminModulesManifest(repoRoot);
  return [...(manifest.defaultPackages || []), ...(manifest.fullPackages || [])];
}

function readAdminModulesManifest(repoRoot: string): AdminModulesManifest {
  const manifestPath = resolve(repoRoot, 'packages/admin/admin-modules.json');
  return JSON.parse(readFileSync(manifestPath, 'utf8')) as AdminModulesManifest;
}
