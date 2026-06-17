import { existsSync } from 'node:fs';
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

const PACKAGE_ENTRIES: PackageEntry[] = [
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
  {
    name: 'auth',
    entries: {
      '.': 'src/index.ts',
    },
  },
  {
    name: 'rbac',
    entries: {
      '.': 'src/index.ts',
    },
  },
  {
    name: 'system',
    entries: {
      '.': 'src/index.ts',
    },
  },
  {
    name: 'calendar',
    entries: {
      '.': 'src/index.ts',
      'admin-pages': 'src/admin-pages.ts',
    },
  },
  {
    name: 'file',
    entries: {
      '.': 'src/index.ts',
      'admin-pages': 'src/admin-pages.ts',
    },
  },
  {
    name: 'job',
    entries: {
      '.': 'src/index.ts',
      'admin-pages': 'src/admin-pages.ts',
    },
  },
  {
    name: 'notice',
    entries: {
      '.': 'src/index.ts',
      admin: 'src/admin.ts',
      'admin-pages': 'src/admin-pages.ts',
      'admin-shell': 'src/admin-shell.ts',
      client: 'src/client.ts',
      realtime: 'src/realtime.ts',
    },
  },
  {
    name: 'numgen',
    entries: {
      '.': 'src/index.ts',
      'admin-pages': 'src/admin-pages.ts',
    },
  },
  {
    name: 'payment',
    entries: {
      '.': 'src/index.ts',
      'admin-pages': 'src/admin-pages.ts',
    },
  },
  {
    name: 'template',
    entries: {
      '.': 'src/index.ts',
      'admin-pages': 'src/admin-pages.ts',
    },
  },
  {
    name: 'workflow',
    entries: {
      '.': 'src/index.ts',
      'admin-pages': 'src/admin-pages.ts',
    },
  },
  {
    name: 'workflow-business-example',
    entries: {
      '.': 'src/index.ts',
      'admin-pages': 'src/admin-pages.ts',
    },
  },
];

const STYLE_PACKAGES = [
  'common',
  'auth',
  'rbac',
  'system',
  'workflow',
  'workflow-business-example',
  'file',
  'job',
  'calendar',
  'numgen',
  'payment',
  'template',
  'notice',
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
      { find: '@mango/admin-shell/style.css', replacement: resolve(repoRoot, 'build-config/source-package-style.css') },
    );

    for (const packageName of STYLE_PACKAGES) {
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

  for (const packageEntry of PACKAGE_ENTRIES) {
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
  const distPackages = PACKAGE_ENTRIES
    .map(packageEntry => packageEntry.name)
    .filter(packageName => packageName !== 'app-runtime');
  const missing = [
    ...distPackages.map(packageName => resolve(repoRoot, 'packages', packageName, 'dist/index.js')),
    ...STYLE_PACKAGES.map(packageName => resolve(repoRoot, 'packages', packageName, 'dist/style.css')),
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
  return resolve(repoRoot, 'build-config/source-package-style.css');
}
