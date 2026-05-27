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
  });

  it('does not publish workspace dependency ranges', () => {
    const packageJson = JSON.parse(readFile('package.json')) as {
      dependencies?: Record<string, string>;
      peerDependencies?: Record<string, string>;
    };
    const dependencyRanges = [
      ...Object.values(packageJson.dependencies || {}),
      ...Object.values(packageJson.peerDependencies || {}),
    ];

    expect(dependencyRanges.some((range) => range.includes('workspace:'))).toBe(false);
  });

  it('does not depend on app-private source paths', () => {
    const sourceFiles = listFiles(join(packageRoot, 'src'))
      .filter((file) => /\.(ts|vue)$/.test(file));
    const forbiddenPatterns = [
      /apps\/mango-admin-shell/,
      /from ['"]@\//,
      /import\(['"]@\//,
    ];

    const violations = sourceFiles.flatMap((file) => {
      const content = readFileSync(file, 'utf-8');
      return forbiddenPatterns
        .filter((pattern) => pattern.test(content))
        .map((pattern) => `${file}:${pattern}`);
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
