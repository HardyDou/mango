import assert from 'node:assert/strict';
import { createHash } from 'node:crypto';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import {
  assertCatalogProjection,
  assertFileCopyProjection,
  canonicalJsonBytes,
  CatalogValidationError,
  compileCatalog,
} from './catalog-lib.mjs';

const repositoryRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');

test('compiles authored package, module, Maven and docs facts deterministically', () => {
  const fixture = createFixture();
  const first = compileFixture(fixture);
  const second = compileFixture(fixture);
  assert.equal(first.catalogDigest, second.catalogDigest);
  assert.equal(first.packages.length, 2);
  assert.equal(first.modules[0].owner, 'module:system');
  assert.equal(first.modules[0].backendStarters[0], ':mango-system-starter');
  assert.equal(first.projections.adminModules.defaultPackages[0].packageName, '@mango/system');
  assert.equal(first.projections.cliModules.modules[0].backendStarters[0].groupId, 'io.mango.platform.system');
  assert.equal(first.releaseArtifacts[0].owner, 'release-artifact:mango-docs-bundle');
  assert.equal(first.maven.sourceModel, 'maven-effective-pom');
});

test('projects default config and required resources deterministically from the module manifest', () => {
  const fixture = createFixture();
  fs.writeFileSync(path.join(fixture, 'system-default.yml'), '  event:\n    outbox:\n      enabled: true\n');
  fs.writeFileSync(path.join(fixture, 'favicon.ico'), Buffer.from([0, 1, 2, 3]));
  const systemFile = path.join(fixture, 'mango-catalog/modules/system.json');
  const system = readJson(systemFile);
  system.configFragments = [
    { source: 'system-default.yml', targetOwner: '@mango/admin', targetPath: 'templates/application.yml' },
  ];
  system.resourceCopies = [{ source: 'favicon.ico', targetOwner: '@mango/admin', targetPath: 'templates/favicon.ico' }];
  writeJson(systemFile, system);

  const first = compileFixture(fixture);
  const second = compileFixture(fixture);
  const projection = first.projections.cliModules.modules[0];
  assert.deepEqual(first, second);
  assert.equal(projection.configFragments[0].content, '  event:\n    outbox:\n      enabled: true\n');
  assert.match(projection.configFragments[0].sha256, /^[a-f0-9]{64}$/u);
  assert.match(projection.resourceCopies[0].sha256, /^[a-f0-9]{64}$/u);
});

test('rejects schema drift, duplicate owners and unknown references in one report', () => {
  const fixture = createFixture();
  const systemFile = path.join(fixture, 'mango-catalog/modules/system.json');
  const system = readJson(systemFile);
  system.unexpected = true;
  system.backendStarters = [':missing-starter'];
  system.adminIntegration.registrarExports[0].subpath = './missing';
  writeJson(systemFile, system);
  writeJson(path.join(fixture, 'mango-catalog/modules/copy.json'), {
    schemaVersion: 1,
    moduleId: 'system-copy',
    frontendPackages: ['@mango/system'],
    presetMembership: 'full',
  });

  assert.throws(
    () => compileFixture(fixture),
    (error) => {
      assert.ok(error instanceof CatalogValidationError);
      assert.match(error.message, /unexpected is not allowed/u);
      assert.match(error.message, /duplicate frontend package owner @mango\/system/u);
      assert.match(error.message, /unknown publishable Maven starter :missing-starter/u);
      assert.match(error.message, /registrar subpath is not a public authored export/u);
      assert.match(error.message, /filename must be system-copy\.json/u);
      return true;
    },
  );
});

test('rejects unsafe and missing sources plus conflicting projection targets', () => {
  const fixture = createFixture();
  writePackage(fixture, 'job', '@mango/job', {
    '.': { source: './src/index.ts', kind: 'code' },
  });
  fs.mkdirSync(path.join(fixture, 'mango-ui/packages/job/src'), { recursive: true });
  fs.writeFileSync(path.join(fixture, 'mango-ui/packages/job/src/index.ts'), 'export {};\n');
  fs.writeFileSync(path.join(fixture, 'owned.json'), '{}\n');
  const systemFile = path.join(fixture, 'mango-catalog/modules/system.json');
  const system = readJson(systemFile);
  system.configFragments = [
    { source: '../outside.json', targetOwner: '@mango/admin', targetPath: 'generated/shared.json' },
    { source: 'missing.json', targetOwner: '@mango/admin', targetPath: 'generated/missing.json' },
    { source: 'owned.json', targetOwner: '@mango/admin', targetPath: 'generated/shared.json' },
  ];
  writeJson(systemFile, system);
  writeJson(path.join(fixture, 'mango-catalog/modules/job.json'), {
    schemaVersion: 1,
    moduleId: 'job',
    frontendPackages: ['@mango/job'],
    presetMembership: 'full',
    resourceCopies: [{ source: 'owned.json', targetOwner: '@mango/admin', targetPath: 'generated/shared.json' }],
  });
  fs.rmSync(path.join(fixture, 'mango-docs/capabilities/README.md'));

  assert.throws(
    () => compileFixture(fixture),
    (error) => {
      assert.match(error.message, /unsafe source path \.\.\/outside\.json/u);
      assert.match(error.message, /source path does not exist: missing\.json/u);
      assert.match(error.message, /duplicate projection target @mango\/admin:file:generated\/shared\.json/u);
      assert.match(error.message, /required file is missing: capabilities\/README\.md/u);
      return true;
    },
  );
});

test('detects a hand-edited tracked projection', () => {
  const fixture = createFixture();
  const bytes = canonicalJsonBytes(compileFixture(fixture));
  const projection = path.join(fixture, 'mango-catalog/catalog.lock.json');
  fs.writeFileSync(projection, bytes);
  assert.doesNotThrow(() => assertCatalogProjection(projection, bytes));
  fs.appendFileSync(projection, ' ');
  assert.throws(() => assertCatalogProjection(projection, bytes), /tracked Catalog projection differs/u);
});

test('rejects missing and hand-edited resource projections', () => {
  const fixture = createFixture();
  const source = path.join(fixture, 'source.ico');
  const target = path.join(fixture, 'target.ico');
  fs.writeFileSync(source, Buffer.from([0, 1, 2, 3]));
  assert.throws(() => assertFileCopyProjection(source, target), /tracked resource projection is missing/u);
  fs.writeFileSync(target, Buffer.from([3, 2, 1, 0]));
  assert.throws(() => assertFileCopyProjection(source, target), /differs from source/u);
  fs.copyFileSync(source, target);
  assert.doesNotThrow(() => assertFileCopyProjection(source, target));
});

test('rejects package projection drift and missing source files', () => {
  const fixture = createFixture();
  const packageFile = path.join(fixture, 'mango-ui/packages/system/package.json');
  const packageJson = readJson(packageFile);
  packageJson.exports = {};
  packageJson.mangoArchitecture.sourceExports['./missing'] = {
    source: './src/missing.ts',
    kind: 'code',
  };
  writeJson(packageFile, packageJson);
  assert.throws(
    () => compileFixture(fixture),
    (error) => {
      assert.match(error.message, /source export \.\/admin-pages is missing from package\.json exports/u);
      assert.match(error.message, /source path does not exist: \.\/src\/missing\.ts/u);
      return true;
    },
  );
});

test('uses unsigned UTF-8 byte order and a fixed canonical digest fixture', () => {
  const bytes = canonicalJsonBytes({ ä: 1, z: 2, a: 3 });
  assert.equal(bytes.toString('utf8'), '{\n  "a": 3,\n  "z": 2,\n  "ä": 1\n}\n');
  assert.equal(
    createHash('sha256').update(bytes).digest('hex'),
    '254daf6de51147c9c5ebbd44e45b29f9fe2870a8c0b085a138de270a1ff96294',
  );
});

function createFixture() {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-catalog-'));
  fs.mkdirSync(path.join(root, 'mango-catalog/schema'), { recursive: true });
  for (const schema of ['module.schema.json', 'release-artifact.schema.json']) {
    fs.copyFileSync(
      path.join(repositoryRoot, `mango-catalog/schema/${schema}`),
      path.join(root, `mango-catalog/schema/${schema}`),
    );
  }
  writePackage(
    root,
    'system',
    '@mango/system',
    {
      '.': { source: './src/index.ts', kind: 'code' },
      './admin-pages': { source: './src/admin-pages.ts', kind: 'code' },
    },
    {
      './style.css': { source: './src/style.css', dist: './dist/style.css', generation: 'build' },
    },
  );
  writePackage(root, 'admin', '@mango/admin', {
    '.': { source: './src/index.ts', kind: 'code' },
  });
  for (const file of ['src/index.ts', 'src/admin-pages.ts', 'src/style.css']) {
    const target = path.join(root, 'mango-ui/packages/system', file);
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.writeFileSync(target, 'export {};\n');
  }
  fs.mkdirSync(path.join(root, 'mango-ui/packages/admin/src'), { recursive: true });
  fs.writeFileSync(path.join(root, 'mango-ui/packages/admin/src/index.ts'), 'export {};\n');
  writeJson(path.join(root, 'mango-catalog/modules/system.json'), {
    schemaVersion: 1,
    moduleId: 'system',
    frontendPackages: ['@mango/system'],
    adminIntegration: {
      aggregateOwner: '@mango/admin',
      registrarExports: [
        { package: '@mango/system', subpath: './admin-pages', symbol: 'registerMangoSystemAdminPages' },
      ],
      styleEntries: [{ package: '@mango/system', subpath: './style.css' }],
    },
    backendStarters: [':mango-system-starter'],
    presetMembership: 'default',
  });
  writeJson(path.join(root, 'mango-catalog/release-artifacts/mango-docs-bundle.json'), {
    schemaVersion: 1,
    groupId: 'io.mango',
    artifactId: 'mango-docs-bundle',
    sourceRoots: ['mango-docs'],
    requiredFiles: ['README.md', 'capabilities/README.md', 'versions/manifest.json'],
    packaging: 'jar',
    extension: 'jar',
    classifier: null,
    versionSource: 'maven-platform',
  });
  for (const file of ['README.md', 'capabilities/README.md', 'versions/manifest.json']) {
    const target = path.join(root, 'mango-docs', file);
    fs.mkdirSync(path.dirname(target), { recursive: true });
    fs.writeFileSync(target, file.endsWith('.json') ? '{}\n' : '# docs\n');
  }
  return root;
}

function writePackage(root, directory, name, sourceExports, nonCodeExports = {}) {
  const exports = Object.fromEntries([
    ...Object.keys(sourceExports).map((subpath) => [subpath, { import: './dist/index.js' }]),
    ...Object.keys(nonCodeExports).map((subpath) => [subpath, './dist/style.css']),
  ]);
  writeJson(path.join(root, `mango-ui/packages/${directory}/package.json`), {
    name,
    version: '1.0.0',
    private: false,
    exports,
    mangoArchitecture: { sourceExports, nonCodeExports },
  });
}

function compileFixture(root) {
  return compileCatalog({
    repositoryRoot: root,
    architectureReport: {
      schemaVersion: 1,
      reportSha256: 'a'.repeat(64),
      summary: { workspaceCount: 2 },
      errors: [],
    },
    mavenInventory: {
      schemaVersion: 1,
      sourceModel: 'maven-effective-pom',
      reactorModuleCount: 1,
      publishableCoordinateCount: 1,
      excludedCoordinateCount: 0,
      dependencyManagementSets: [{ digest: '0'.repeat(64), dependencies: [] }],
      publishableCoordinates: [
        {
          groupId: 'io.mango.platform.system',
          artifactId: 'mango-system-starter',
          version: '1.0.0',
          packaging: 'jar',
          extension: 'jar',
          classifier: null,
          modulePath: 'mango-platform/mango-system/mango-system-starter',
          dependencies: [],
          dependencyManagementDigest: '0'.repeat(64),
        },
      ],
      excludedCoordinates: [],
    },
  });
}

function writeJson(file, value) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, `${JSON.stringify(value, null, 2)}\n`);
}

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}
