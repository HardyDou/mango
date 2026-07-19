import assert from 'node:assert/strict';
import { mkdirSync, mkdtempSync, readFileSync, rmSync, symlinkSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';
import {
  applyTarballMappings,
  assertGeneratedProjectBoundary,
  assertInstalledSymlinkBoundary,
  readJson,
} from './business-lab-lib.mjs';

function fixture() {
  const root = mkdtempSync(join(tmpdir(), 'mango-business-lab-'));
  const frontend = join(root, 'projects/lab/frontend');
  const tarballs = join(root, 'artifacts');
  mkdirSync(frontend, { recursive: true });
  mkdirSync(tarballs, { recursive: true });
  writeFileSync(
    join(frontend, 'package.json'),
    `${JSON.stringify(
      {
        dependencies: { '@mango/admin': '1.0.0', vue: '3.5.13' },
        devDependencies: { '@mango/cli': '1.0.0' },
      },
      null,
      2,
    )}\n`,
  );
  return { root, frontend, tarballs };
}

test('maps every declared Mango dependency to a portable tarball override', () => {
  const { root, frontend, tarballs } = fixture();
  try {
    const admin = join(tarballs, 'mango-admin.tgz');
    const cli = join(tarballs, 'mango-cli.tgz');
    writeFileSync(admin, 'admin');
    writeFileSync(cli, 'cli');
    applyTarballMappings(
      frontend,
      new Map([
        ['@mango/admin', admin],
        ['@mango/cli', cli],
      ]),
      'https://registry.example/',
    );

    const packageJson = readJson(join(frontend, 'package.json'));
    assert.match(packageJson.dependencies['@mango/admin'], /^file:\.\.\/\.\.\/\.\.\/artifacts\//u);
    assert.match(packageJson.devDependencies['@mango/cli'], /^file:\.\.\/\.\.\/\.\.\/artifacts\//u);
    assert.equal(packageJson.dependencies.vue, '3.5.13');
    assert.match(readFileSync(join(frontend, 'pnpm-workspace.yaml'), 'utf8'), /'@mango\/admin': 'file:/u);
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});

test('fails closed when a declared Mango dependency has no packed artifact', () => {
  const { root, frontend } = fixture();
  try {
    assert.throws(() => applyTarballMappings(frontend, new Map(), 'https://registry.example/'), /no local tarball/u);
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});

test('rejects workspace aliases, host paths, and registry credentials', () => {
  const { root, frontend } = fixture();
  try {
    writeFileSync(
      join(frontend, '.npmrc'),
      'registry=https://registry.example/\n//registry.example/:_authToken=secret\n',
    );
    writeFileSync(join(frontend, 'pnpm-lock.yaml'), 'dependency: workspace:*\nsource: /repo/mango-ui/packages/admin\n');
    assert.throws(
      () => assertGeneratedProjectBoundary(root, ['/repo']),
      /forbidden source reference|host repository path|registry credentials/u,
    );
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});

test('allows only an exact workspace link to a package inside the generated project', () => {
  const { root, frontend } = fixture();
  try {
    const apiRoot = join(frontend, 'packages/orders-api');
    const uiRoot = join(frontend, 'packages/orders');
    mkdirSync(apiRoot, { recursive: true });
    mkdirSync(uiRoot, { recursive: true });
    writeFileSync(
      join(apiRoot, 'package.json'),
      `${JSON.stringify({ name: '@lab/orders-api', version: '1.0.0-SNAPSHOT' }, null, 2)}\n`,
    );
    writeFileSync(
      join(uiRoot, 'package.json'),
      `${JSON.stringify(
        {
          name: '@lab/orders',
          version: '1.0.0-SNAPSHOT',
          dependencies: { '@lab/orders-api': 'workspace:1.0.0-SNAPSHOT' },
        },
        null,
        2,
      )}\n`,
    );
    writeFileSync(join(frontend, 'pnpm-lock.yaml'), 'version: link:../orders-api\n');
    assert.doesNotThrow(() => assertGeneratedProjectBoundary(root, ['/repo']));
    writeFileSync(
      join(uiRoot, 'package.json'),
      `${JSON.stringify(
        {
          name: '@lab/orders',
          version: '1.0.0-SNAPSHOT',
          dependencies: { '@lab/orders-api': 'workspace:*' },
        },
        null,
        2,
      )}\n`,
    );
    assert.throws(() => assertGeneratedProjectBoundary(root, ['/repo']), /invalid local workspace dependency/u);
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});

test('does not interpret governance source text as an installed dependency reference', () => {
  const { root, frontend } = fixture();
  try {
    writeFileSync(join(frontend, 'governance-rule.mjs'), "export const forbiddenExample = 'mango-ui/packages/';\n");
    assert.doesNotThrow(() => assertGeneratedProjectBoundary(root, ['/repo']));
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});

test('rejects installed dependency symlinks that escape node_modules', () => {
  const { root, frontend } = fixture();
  try {
    const nodeModules = join(frontend, 'node_modules');
    const external = join(root, 'external-package');
    mkdirSync(nodeModules, { recursive: true });
    mkdirSync(external, { recursive: true });
    symlinkSync(external, join(nodeModules, 'escaped'));
    assert.throws(() => assertInstalledSymlinkBoundary(nodeModules), /escapes node_modules/u);
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});
