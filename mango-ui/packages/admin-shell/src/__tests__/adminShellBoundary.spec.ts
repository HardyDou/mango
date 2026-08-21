import { describe, expect, it } from 'vitest';
import { existsSync, readdirSync, readFileSync, statSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const packageRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../..');

describe('@mango/admin-shell package boundary', () => {
  it('exports the official app creation API', () => {
    const indexSource = readFile('src/index.ts');

    expect(indexSource).toContain('createMangoAdminApp');
    expect(indexSource).toContain('MangoAdminShellOptions');
    expect(indexSource).toContain('isElementPlusMessageBoxCancellation');
  });

  it('installs the shared error boundary for every Shell-created Vue app', () => {
    const bootstrapSource = readFile('src/appBootstrap.ts');
    const runtimeHostSource = readFile('src/runtime/runtimeHost.ts');

    expect(bootstrapSource).toContain('installShellErrorHandler(app)');
    expect(runtimeHostSource.match(/installShellApp\(mountedLocalPage\)/gu)).toHaveLength(2);
  });

  it('uses only exact workspace dependency pins in the development manifest', () => {
    const packageJson = JSON.parse(readFile('package.json')) as {
      dependencies?: Record<string, string>;
      peerDependencies?: Record<string, string>;
    };
    const dependencyRanges = [
      ...Object.values(packageJson.dependencies || {}),
      ...Object.values(packageJson.peerDependencies || {}),
    ];

    const workspaceRanges = dependencyRanges.filter((range) => range.startsWith('workspace:'));
    expect(workspaceRanges.every((range) => /^workspace:\d+\.\d+\.\d+$/u.test(range))).toBe(true);
  });

  it('keeps published subpath imports on built files instead of source files', () => {
    const packageJson = JSON.parse(readFile('package.json')) as {
      exports?: Record<string, string | { import?: string; types?: string }>;
      files?: string[];
    };
    const productSubpaths = ['.', './runtime', './menu', './stores', './router', './home'];

    for (const subpath of productSubpaths) {
      const entry = packageJson.exports?.[subpath];
      const importPath = typeof entry === 'string' ? entry : entry?.import;
      expect(importPath, subpath).toMatch(/^\.\/dist\/.+\.js$/);
    }
    expect(packageJson.exports?.['./style.css']).toBe('./style.css');
    expect(packageJson.files).toContain('README.md');
  });

  it('documents admin shell product APIs and extension contracts', () => {
    const readme = readFile('README.md');

    for (const expected of [
      'createMangoAdminApp',
      'Feature Registrars',
      'Runtime Modules',
      'Menu Contract',
      'Theme',
      'I18n',
      'Directives',
      'Migration From App-Local Shell Code',
      'Compatibility Matrix',
    ]) {
      expect(readme).toContain(expected);
    }
  });

  it('keeps common dependency aligned with the released CLI material version', () => {
    const packageJson = JSON.parse(readFile('package.json')) as {
      dependencies?: Record<string, string>;
    };

    const releaseVersions = JSON.parse(readFile('../mango-cli/release-versions.json')) as {
      npm?: Record<string, string>;
    };

    expect(packageJson.dependencies?.['@mango/common']).toBe(`workspace:${releaseVersions.npm?.['@mango/common']}`);
  });

  it('does not depend on app-private source paths', () => {
    const sourceFiles = listFiles(join(packageRoot, 'src')).filter((file) => /\.(ts|vue)$/.test(file));
    const forbiddenPatterns = [/apps\/mango-admin-shell/, /from ['"]@\//, /import\(['"]@\//];

    const violations = sourceFiles.flatMap((file) => {
      const content = readFileSync(file, 'utf-8');
      return forbiddenPatterns.filter((pattern) => pattern.test(content)).map((pattern) => `${file}:${pattern}`);
    });

    expect(violations).toEqual([]);
  });
});

function readFile(relativePath: string) {
  return readFileSync(join(packageRoot, relativePath), 'utf-8');
}

function listFiles(dir: string): string[] {
  if (!existsSync(dir)) {
    return [];
  }
  return readdirSync(dir).flatMap((name) => {
    const path = join(dir, name);
    if (statSync(path).isDirectory()) {
      return listFiles(path);
    }
    return path;
  });
}
