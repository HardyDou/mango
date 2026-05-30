#!/usr/bin/env node
import { copyFileSync, existsSync, mkdirSync, readdirSync, readFileSync, rmSync, statSync, writeFileSync } from 'node:fs';
import { dirname, extname, join, relative, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const scriptFile = fileURLToPath(import.meta.url);
const repoRoot = resolve(dirname(scriptFile), '..');
const packagesRoot = join(repoRoot, 'packages');
const buildDirName = '.mango-build';

const args = parseArgs(process.argv.slice(2));
const packages = resolvePackages(args.filter);
const publicEntryMap = {
  '@mango/admin-pages': {
    './core': 'src/core.ts',
    './defaults': 'src/defaults.ts',
    './dev-component-pages': 'src/devComponentPages.ts',
  },
  '@mango/admin': {
    './presets': 'src/presets.ts',
    './menu': 'src/menu.ts',
  },
  '@mango/auth': {
    './capability': 'src/capability.ts',
  },
  '@mango/calendar': {
    './capability': 'src/capability.ts',
  },
  '@mango/file': {
    './capability': 'src/capability.ts',
  },
  '@mango/numgen': {
    './capability': 'src/capability.ts',
  },
  '@mango/rbac': {
    './capability': 'src/capability.ts',
  },
  '@mango/system': {
    './capability': 'src/capability.ts',
  },
  '@mango/template': {
    './capability': 'src/capability.ts',
  },
  '@mango/workflow': {
    './capability': 'src/capability.ts',
  },
  '@mango/admin-shell': {
    './runtime': 'src/runtime/runtimeHost.ts',
    './menu': 'src/runtime/menuHost.ts',
    './stores': 'src/stores/index.ts',
    './router': 'src/router.ts',
  },
  '@mango/app-runtime': {
    './vue-micro': 'src/vue-micro.ts',
  },
  '@mango/notice': {
    './admin': 'src/admin.ts',
    './capability': 'src/capability.ts',
    './client': 'src/client.ts',
    './realtime': 'src/realtime.ts',
    './api': 'src/api/notice.ts',
    './types': 'src/types/notice.ts',
  },
};
publicEntryMap['@mango/common'] = [
  'components/DictSelect/index',
  'components/DictTag/index',
  'components/IconSelector/index',
  'components/Pagination/index',
  'api/area',
  'api/captcha',
  'api/dict',
  'api/org',
  'api/upload',
  'hooks/useDict',
  'hooks/useECharts',
  'hooks/useLocale',
  'hooks/useTitle',
  'theme/index',
  'utils/apiCrypto',
  'utils/arrayOperation',
  'utils/authFunction',
  'utils/captchaRequest',
  'utils/errorCode',
  'utils/formatTime',
  'utils/getStyleSheets',
  'utils/iconConfig',
  'utils/menuTree',
  'utils/message',
  'utils/mitt',
  'utils/other',
  'utils/request',
  'utils/storage',
  'utils/tagsView',
  'utils/theme',
  'utils/toolsValidate',
  'utils/validate',
].reduce((entries, subpath) => {
  entries[`./${subpath}`] = `${subpath}.ts`;
  return entries;
}, {});
publicEntryMap['@mango/common']['./theme/index.scss'] = 'theme/index.scss';

if (packages.length === 0) {
  fail(`No packages matched filter: ${args.filter || '<all>'}`);
}

for (const packageDir of packages) {
  buildPackage(packageDir);
}

function parseArgs(argv) {
  const result = { filter: '' };
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--filter' || arg === '-F') {
      result.filter = argv[index + 1] || '';
      index += 1;
      continue;
    }
    if (!result.filter) {
      result.filter = arg;
      continue;
    }
    fail(`Unknown argument: ${arg}`);
  }
  return result;
}

function resolvePackages(filter) {
  return readdirSync(packagesRoot)
    .map((entry) => join(packagesRoot, entry))
    .filter((packageDir) => existsSync(join(packageDir, 'package.json')))
    .filter((packageDir) => {
      const packageJson = readPackageJson(packageDir);
      if (packageJson.name === 'create-mango-app') {
        return false;
      }
      if (!filter) {
        return packageJson.name?.startsWith('@mango/');
      }
      return packageJson.name === filter || packageDir.endsWith(`/${filter}`);
    });
}

function buildPackage(packageDir) {
  const packageJson = readPackageJson(packageDir);
  const entries = resolveEntries(packageDir, packageJson);
  const sourceRoot = existsSync(join(packageDir, 'src')) ? join(packageDir, 'src') : packageDir;
  const tempDir = join(packageDir, buildDirName);
  const distDir = join(packageDir, 'dist');

  rmSync(distDir, { recursive: true, force: true });
  rmSync(tempDir, { recursive: true, force: true });
  mkdirSync(tempDir, { recursive: true });

  const viteConfigPath = join(tempDir, 'vite.config.mjs');
  const tsconfigPath = join(tempDir, 'tsconfig.json');

  try {
    writeFileSync(viteConfigPath, createViteConfig(packageDir, entries, sourceRoot), 'utf8');
    writeFileSync(join(tempDir, 'workspace-modules.d.ts'), createWorkspaceModuleShims(packageJson.name), 'utf8');
    writeFileSync(join(tempDir, 'pinia-persist-shim.d.ts'), createPiniaPersistShim(), 'utf8');
    writeFileSync(join(tempDir, 'third-party-shims.d.ts'), createThirdPartyShims(), 'utf8');
    writeFileSync(tsconfigPath, createTsConfig(packageDir, entries, sourceRoot), 'utf8');

    run('pnpm', ['exec', 'vite', 'build', '--config', viteConfigPath], repoRoot, packageJson.name);
    run('pnpm', ['exec', 'tsc', '-p', tsconfigPath], repoRoot, packageJson.name);
    patchVitePreloadHelper(distDir);
    copyStyleEntries(entries, sourceRoot, distDir);
    createStyleBundle(distDir);
    stripCssPreloadDeps(distDir);
    stripVitePreloadWrappers(distDir);
    assertNoVitePreloadWrappers(distDir);
  } finally {
    rmSync(tempDir, { recursive: true, force: true });
  }
}

function patchVitePreloadHelper(distDir) {
  const helperPath = join(distDir, '_virtual/preload-helper.js');
  if (!existsSync(helperPath)) {
    return;
  }
  const source = readFileSync(helperPath, 'utf8');
  const patched = source.replace(
    /function\((\w+)\)\{return"\/"\+\1\}/,
    'function($1){return new URL("../"+$1,import.meta.url).href}',
  );
  if (patched === source) {
    return;
  }
  writeFileSync(helperPath, patched, 'utf8');
}

function copyStyleEntries(entries, sourceRoot, distDir) {
  for (const entry of entries) {
    if (extname(entry) !== '.scss') {
      continue;
    }
    const relativeEntry = normalizePath(relative(sourceRoot, entry));
    const targetPath = join(distDir, relativeEntry);
    mkdirSync(dirname(targetPath), { recursive: true });
    copyFileSync(entry, targetPath);
  }
}

function createStyleBundle(distDir) {
  const cssFiles = findCssFiles(join(distDir, 'assets'));
  if (cssFiles.length === 0) {
    return;
  }
  const bundle = cssFiles
    .map((file) => {
      const relativeFile = normalizePath(relative(distDir, file));
      return [
        `/* ${relativeFile} */`,
        readFileSync(file, 'utf8').trim(),
        '',
      ].join('\n');
    })
    .join('\n');
  writeFileSync(join(distDir, 'style.css'), `${bundle}\n`, 'utf8');
}

function stripCssPreloadDeps(distDir) {
  for (const file of findJsFiles(distDir)) {
    const source = readFileSync(file, 'utf8');
    const mapDepsMatch = source.match(/const __vite__mapDeps=\(i,m=__vite__mapDeps,d=\(m\.f\|\|\(m\.f=(\[[^\]]*\])\)\)\)=>i\.map\(i=>d\[i\]\);?/);
    if (!mapDepsMatch) {
      continue;
    }
    let deps;
    try {
      deps = JSON.parse(mapDepsMatch[1]);
    } catch {
      continue;
    }
    if (deps.length === 0 || !deps.every((dep) => typeof dep === 'string' && dep.endsWith('.css'))) {
      continue;
    }
    const patched = source
      .replace(mapDepsMatch[0], '')
      .replace(/__vite__mapDeps\(\[[^\]]*\]\)/g, '[]');
    if (patched !== source) {
      writeFileSync(file, patched, 'utf8');
    }
  }
}

function stripVitePreloadWrappers(distDir) {
  for (const file of findJsFiles(distDir)) {
    const source = readFileSync(file, 'utf8');
    if (!source.includes('__vitePreload(')) {
      continue;
    }
    const patched = stripVitePreloadFromSource(source);
    writeFileSync(file, patched, 'utf8');
  }
}

function stripVitePreloadFromSource(source) {
  const withoutWrappedImports = source
    .replace(
      /__vitePreload\(\(\)\s*=>\s*import\((["'][^"']+["'])\),\s*true\s*\?\s*\[\]\s*:\s*void 0\)/g,
      'import($1)',
    )
    .replace(
      /__vitePreload\(\(\)\s*=>\s*import\((["'][^"']+["'])\),\s*\[\]\)/g,
      'import($1)',
    )
    .replace(
      /__vitePreload\(async\s*\(\)\s*=>\s*\{([\s\S]*?)\},\s*true\s*\?\s*\[\]\s*:\s*void 0\)/g,
      '(async () => {$1})()',
    )
    .replace(
      /__vitePreload\(async\s*\(\)\s*=>\s*\{([\s\S]*?)\},\s*\[\]\)/g,
      '(async () => {$1})()',
    );

  if (withoutWrappedImports.includes('__vitePreload(')) {
    return withoutWrappedImports;
  }
  return withoutWrappedImports
    .replace(/import\s*\{\s*__vitePreload\s*\}\s*from\s*["'][^"']+_virtual\/preload-helper\.js["'];\n?/g, '')
    .replace(/import\s*\{\s*__vitePreload\s*\}\s*from\s*["'][^"']+preload-helper\.js["'];\n?/g, '');
}

function assertNoVitePreloadWrappers(distDir) {
  const offenders = findJsFiles(distDir).filter((file) => {
    if (normalizePath(relative(distDir, file)) === '_virtual/preload-helper.js') {
      return false;
    }
    return readFileSync(file, 'utf8').includes('__vitePreload');
  });
  if (offenders.length > 0) {
    fail(`Vite preload wrappers leaked into published files:\n${offenders.map((file) => `- ${file}`).join('\n')}`);
  }
}

function findCssFiles(root) {
  if (!existsSync(root)) {
    return [];
  }
  return readdirSync(root, { withFileTypes: true })
    .flatMap((entry) => {
      const path = join(root, entry.name);
      if (entry.isDirectory()) {
        return findCssFiles(path);
      }
      return entry.isFile() && extname(entry.name) === '.css' ? [path] : [];
    })
    .sort();
}

function findJsFiles(root) {
  if (!existsSync(root)) {
    return [];
  }
  return readdirSync(root, { withFileTypes: true })
    .flatMap((entry) => {
      const path = join(root, entry.name);
      if (entry.isDirectory()) {
        return findJsFiles(path);
      }
      return entry.isFile() && extname(entry.name) === '.js' ? [path] : [];
    })
    .sort();
}

function createViteConfig(packageDir, entries, sourceRoot) {
  return `import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  build: {
    emptyOutDir: false,
    minify: false,
    modulePreload: false,
    sourcemap: false,
    outDir: ${JSON.stringify(join(packageDir, 'dist'))},
    rollupOptions: {
      input: ${JSON.stringify(entries)},
      preserveEntrySignatures: 'strict',
      external: (id) => {
        return !id.startsWith('.') && !id.startsWith('/') && !id.startsWith('\\0');
      },
      output: {
        dir: ${JSON.stringify(join(packageDir, 'dist'))},
        format: 'es',
        preserveModules: true,
        preserveModulesRoot: ${JSON.stringify(sourceRoot)},
        entryFileNames: '[name].js',
        chunkFileNames: '[name]-[hash].js',
        assetFileNames: 'assets/[name][extname]',
      },
    },
  },
});
`;
}

function createTsConfig(packageDir, entries, sourceRoot) {
  const extendsPath = existsSync(join(packageDir, 'tsconfig.json'))
    ? relative(join(packageDir, buildDirName), join(packageDir, 'tsconfig.json'))
    : '';
  const config = {
    ...(extendsPath ? { extends: normalizePath(extendsPath) } : {}),
    compilerOptions: {
      target: 'ES2020',
      module: 'ESNext',
      moduleResolution: 'bundler',
      lib: ['ES2020', 'DOM', 'DOM.Iterable'],
      jsx: 'preserve',
      strict: true,
      skipLibCheck: true,
      resolveJsonModule: true,
      allowImportingTsExtensions: true,
      noEmit: false,
      declaration: true,
      emitDeclarationOnly: true,
      declarationMap: false,
      outDir: '../dist',
      declarationDir: '../dist',
      rootDir: normalizePath(relative(join(packageDir, buildDirName), sourceRoot)),
      types: ['vite/client'],
    },
    files: [
      '../.mango-build/vue-shim.d.ts',
      '../.mango-build/workspace-modules.d.ts',
      '../.mango-build/pinia-persist-shim.d.ts',
      '../.mango-build/third-party-shims.d.ts',
      ...resolveDeclarationEntries(entries).map((entry) => normalizePath(relative(join(packageDir, buildDirName), entry))),
    ],
    exclude: [
      '../dist',
      '../node_modules',
      '../**/__tests__/**',
      '../**/*.spec.ts',
      '../**/*.test.ts',
      '../vite.config.ts',
      '../vitest.config.ts',
    ],
  };
  writeFileSync(
    join(packageDir, buildDirName, 'vue-shim.d.ts'),
    [
      "declare module '*.vue' {",
      "  import type { DefineComponent } from 'vue';",
      '  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>;',
      '  export default component;',
      '}',
      '',
    ].join('\n'),
    'utf8',
  );
  return `${JSON.stringify(config, null, 2)}\n`;
}

function resolveEntries(packageDir, packageJson) {
  const srcEntry = join(packageDir, 'src/index.ts');
  if (existsSync(srcEntry)) {
    return [
      srcEntry,
      ...resolvePublicEntries(packageDir, packageJson),
    ];
  }
  const rootEntry = join(packageDir, 'index.ts');
  if (existsSync(rootEntry)) {
    return [
      rootEntry,
      ...resolvePublicEntries(packageDir, packageJson),
    ];
  }
  fail(`Missing package entry: ${packageDir}`);
}

function resolvePublicEntries(packageDir, packageJson) {
  return Object.entries(packageJson.exports || {})
    .filter(([subpath]) => subpath !== '.')
    .map(([subpath, target]) => resolvePublicEntry(packageDir, packageJson, subpath, target))
    .filter((entry) => entry && existsSync(entry));
}

function resolvePublicEntry(packageDir, packageJson, subpath, target) {
  const mapped = publicEntryMap[packageJson.name]?.[subpath];
  if (mapped) {
    return join(packageDir, mapped);
  }
  if (!target || target.style) {
    return '';
  }
  const importPath = typeof target === 'string' ? target : target.import;
  if (!importPath || !importPath.startsWith('./dist/')) {
    return '';
  }
  const relativeEntry = importPath
    .replace(/^\.\/dist\//, '')
    .replace(/\.js$/, '.ts');
  return join(packageDir, 'src', relativeEntry);
}

function resolveDeclarationEntries(entries) {
  return entries.filter((entry) => extname(entry) !== '.scss');
}

function readPackageJson(packageDir) {
  return JSON.parse(readFileSync(join(packageDir, 'package.json'), 'utf8'));
}

function createWorkspaceModuleShims(currentPackageName) {
  const moduleEntries = readdirSync(packagesRoot)
    .map((entry) => join(packagesRoot, entry))
    .filter((packageDir) => existsSync(join(packageDir, 'package.json')))
    .map((packageDir) => ({ packageDir, packageJson: readPackageJson(packageDir) }))
    .filter(({ packageJson }) => packageJson.name?.startsWith('@mango/') && packageJson.name !== currentPackageName)
    .flatMap(({ packageDir, packageJson }) => [
      {
        moduleName: packageJson.name,
        entryPath: existsSync(join(packageDir, 'src/index.ts')) ? join(packageDir, 'src/index.ts') : join(packageDir, 'index.ts'),
      },
      ...resolveWorkspacePublicModuleEntries(packageDir, packageJson),
    ]);

  return [
    createKnownWorkspaceModuleShim('@mango/common'),
    createKnownWorkspaceModuleShim('@mango/common/utils/request'),
    ...moduleEntries.map(({ moduleName, entryPath }) => [
      `declare module '${moduleName}' {`,
      ...extractExportNames(entryPath).flatMap((name) => [
        `  export type ${name} = any;`,
        `  export const ${name}: any;`,
      ]),
      '  const value: Record<string, unknown>;',
      '  export default value;',
      '}',
      '',
    ].join('\n')),
  ].join('\n');
}

function resolveWorkspacePublicModuleEntries(packageDir, packageJson) {
  return Object.entries(packageJson.exports || {})
    .filter(([subpath]) => subpath !== '.')
    .map(([subpath, target]) => ({
      moduleName: `${packageJson.name}/${subpath.slice(2)}`,
      entryPath: resolvePublicEntry(packageDir, packageJson, subpath, target),
    }))
    .filter(({ entryPath }) => entryPath && existsSync(entryPath));
}

function createKnownWorkspaceModuleShim(moduleName) {
  if (moduleName !== '@mango/common' && moduleName !== '@mango/common/utils/request') {
    return '';
  }
  return [
    `declare module '${moduleName}' {`,
    '  export interface RequestConfig extends Record<string, any> {}',
    '  export interface ResponseResult<T = any> {',
    '    code: number;',
    '    data: T;',
    '    message?: string;',
    '    msg?: string;',
    '    success: boolean;',
    '  }',
    '  export interface RequestError {',
    '    code?: number;',
    '    message: string;',
    '    response?: any;',
    '  }',
    '  export function registerUnauthorizedHandler(handler: () => void | Promise<void>): void;',
    '  export function setRequestBaseUrl(baseURL: string): void;',
    '  export function get<T = any>(url: string, config?: RequestConfig): Promise<T>;',
    '  export function post<T = any>(url: string, data?: any, config?: RequestConfig): Promise<T>;',
    '  export function put<T = any>(url: string, data?: any, config?: RequestConfig): Promise<T>;',
    '  export function del<T = any>(url: string, config?: RequestConfig): Promise<T>;',
    '  export const request: {',
    '    get<T = any>(url: string, config?: RequestConfig): Promise<T>;',
    '    post<T = any>(url: string, data?: any, config?: RequestConfig): Promise<T>;',
    '    put<T = any>(url: string, data?: any, config?: RequestConfig): Promise<T>;',
    '    delete<T = any>(url: string, config?: RequestConfig): Promise<T>;',
    '  };',
    '  export function resolveHttpErrorMessage(status?: number, responseData?: Record<string, any>, fallbackMessage?: string): string;',
    '  export function normalizeApiPayload<T>(payload: T): T;',
    '  const service: any;',
    '  export default service;',
    '}',
    '',
  ].join('\n');
}

function createPiniaPersistShim() {
  return [
    "import 'pinia';",
    '',
    "declare module 'pinia' {",
    '  export interface DefineStoreOptionsBase<S, Store> {',
    '    persist?: boolean | Record<string, any>;',
    '  }',
    '}',
    '',
  ].join('\n');
}

function createThirdPartyShims() {
  return [
    "declare module 'sm-crypto' {",
    '  export const sm2: any;',
    '  export const sm3: any;',
    '  export const sm4: any;',
    '}',
    '',
    "declare module 'vue-i18n' {",
    '  export function useI18n(): {',
    '    t: (key: string, ...args: any[]) => string;',
    '    locale: { value: string };',
    '  };',
    '}',
    '',
  ].join('\n');
}

function extractExportNames(entryPath) {
  if (!existsSync(entryPath)) {
    return [];
  }
  const text = readFileSync(entryPath, 'utf8');
  const names = new Set();
  for (const match of text.matchAll(/export\s+(?:declare\s+)?(?:async\s+)?(?:const|let|var|function|class|interface|type|enum)\s+([A-Za-z_$][\w$]*)/g)) {
    names.add(match[1]);
  }
  for (const match of text.matchAll(/export\s+(?:type\s+)?\{([^}]+)\}/g)) {
    for (const part of match[1].split(',')) {
      const cleaned = part
        .trim()
        .replace(/^type\s+/, '')
        .split(/\s+as\s+/)
        .pop()
        ?.trim();
      if (cleaned && /^[A-Za-z_$][\w$]*$/.test(cleaned) && cleaned !== 'default') {
        names.add(cleaned);
      }
    }
  }
  for (const match of text.matchAll(/export\s+\*\s+from\s+['"](.+)['"]/g)) {
    const childEntry = resolveExportEntry(entryPath, match[1]);
    extractExportNames(childEntry).forEach((name) => names.add(name));
  }
  return [...names].sort();
}

function resolveExportEntry(fromFile, specifier) {
  if (specifier.startsWith('.')) {
    return resolveRelativeEntry(fromFile, specifier);
  }
  return resolveWorkspaceModuleEntry(specifier);
}

function resolveWorkspaceModuleEntry(specifier) {
  const packageInfo = findWorkspacePackageForSpecifier(specifier);
  if (!packageInfo) {
    return specifier;
  }
  const { packageDir, packageJson, subpath } = packageInfo;
  if (subpath === '.') {
    return existsSync(join(packageDir, 'src/index.ts')) ? join(packageDir, 'src/index.ts') : join(packageDir, 'index.ts');
  }
  const exportTarget = packageJson.exports?.[subpath];
  return resolvePublicEntry(packageDir, packageJson, subpath, exportTarget);
}

function findWorkspacePackageForSpecifier(specifier) {
  const packageDirs = readdirSync(packagesRoot)
    .map((entry) => join(packagesRoot, entry))
    .filter((packageDir) => existsSync(join(packageDir, 'package.json')))
    .map((packageDir) => ({ packageDir, packageJson: readPackageJson(packageDir) }))
    .filter(({ packageJson }) => packageJson.name?.startsWith('@mango/'))
    .sort((a, b) => b.packageJson.name.length - a.packageJson.name.length);
  for (const packageInfo of packageDirs) {
    const packageName = packageInfo.packageJson.name;
    if (specifier === packageName) {
      return { ...packageInfo, subpath: '.' };
    }
    if (specifier.startsWith(`${packageName}/`)) {
      return {
        ...packageInfo,
        subpath: `./${specifier.slice(packageName.length + 1)}`,
      };
    }
  }
  return null;
}

function resolveRelativeEntry(fromFile, specifier) {
  const basePath = resolve(dirname(fromFile), specifier);
  const candidates = [
    `${basePath}.ts`,
    `${basePath}.vue`,
    join(basePath, 'index.ts'),
    basePath,
  ];
  return candidates.find((candidate) => existsSync(candidate) && !statIsDirectory(candidate)) || basePath;
}

function statIsDirectory(path) {
  try {
    return existsSync(path) && statSync(path).isDirectory();
  } catch {
    return false;
  }
}

function run(command, commandArgs, cwd, label) {
  const result = spawnSync(command, commandArgs, {
    cwd,
    stdio: 'inherit',
    shell: false,
  });
  if (result.status !== 0) {
    fail(`${label} failed: ${command} ${commandArgs.join(' ')}`);
  }
}

function normalizePath(path) {
  return path.split('\\').join('/');
}

function fail(message) {
  console.error(`Error: ${message}`);
  process.exit(1);
}
