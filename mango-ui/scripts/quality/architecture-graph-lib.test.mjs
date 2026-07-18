import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { analyzeArchitecture, assertArchitecture } from './architecture-graph-lib.mjs';

function writeJson(file, value) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, `${JSON.stringify(value, null, 2)}\n`);
}

function workspace(root, location, manifest, sources = {}) {
  const workspaceRoot = path.join(root, location);
  writeJson(path.join(workspaceRoot, 'package.json'), manifest);
  for (const [file, content] of Object.entries(sources)) {
    fs.mkdirSync(path.dirname(path.join(workspaceRoot, file)), { recursive: true });
    fs.writeFileSync(path.join(workspaceRoot, file), content);
  }
}

function metadata(architectureLayer, role, domain, sourceExports = {}) {
  return {
    architectureLayer,
    role,
    domain,
    ownerRole: 'Frontend Standards Owner',
    sourceExports,
    nonCodeExports: {},
  };
}

function fixture() {
  const repoRoot = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-architecture-'));
  const root = path.join(repoRoot, 'mango-ui');
  writeJson(path.join(root, 'architecture-exceptions.json'), { schemaVersion: 1, exceptions: [], legacySccs: [] });
  workspace(
    root,
    'packages/contracts',
    {
      name: '@mango/contracts',
      exports: { '.': { import: './dist/index.js' } },
      mangoArchitecture: metadata('FE0', 'contract', 'platform', { '.': { source: './src/index.ts', kind: 'code' } }),
    },
    { 'src/index.ts': 'export type ApiId = string;\n' },
  );
  workspace(
    root,
    'packages/domain',
    {
      name: '@mango/domain',
      dependencies: { '@mango/contracts': '1.0.0' },
      exports: { '.': { import: './dist/index.js' } },
      mangoArchitecture: metadata('FE2', 'domain', 'orders', { '.': { source: './src/index.ts', kind: 'code' } }),
    },
    { 'src/index.ts': "import type { ApiId } from '@mango/contracts';\nexport type OrderId = ApiId;\n" },
  );
  workspace(
    root,
    'apps/admin',
    {
      name: 'admin-app',
      private: true,
      dependencies: { '@mango/domain': '1.0.0' },
      mangoArchitecture: metadata('FE4', 'app', 'admin'),
    },
    { 'src/main.ts': "import '@mango/domain';\n" },
  );
  return root;
}

test('accepts complete metadata, public exports, and high-to-low dependencies', () => {
  const root = fixture();
  const report = analyzeArchitecture(root);
  assert.doesNotThrow(() => assertArchitecture(report));
  assert.equal(report.summary.workspaceCount, 3);
  assert.equal(report.summary.metadataCoverage, 3);
});

test('includes root non-code exports and CSS imports in the runtime graph', () => {
  const root = fixture();
  for (const packageName of ['contracts', 'domain']) {
    const manifestFile = path.join(root, `packages/${packageName}/package.json`);
    const manifest = JSON.parse(fs.readFileSync(manifestFile, 'utf8'));
    manifest.exports['./style.css'] = './style.css';
    manifest.mangoArchitecture.nonCodeExports['./style.css'] = {
      source: './style.css',
      dist: './style.css',
      generation: 'static',
    };
    writeJson(manifestFile, manifest);
    fs.writeFileSync(
      path.join(root, `packages/${packageName}/style.css`),
      packageName === 'domain' ? "@import './generated.css';\n" : ':root {}\n',
    );
    if (packageName === 'domain') {
      fs.writeFileSync(path.join(root, 'packages/domain/generated.css'), "@import '@mango/contracts/style.css';\n");
    }
  }
  const report = analyzeArchitecture(root);
  assert.doesNotThrow(() => assertArchitecture(report));
  assert.ok(
    report.edges['source-runtime'].some(
      (edge) => edge.from === '@mango/domain' && edge.to === '@mango/contracts' && edge.kind === 'runtime',
    ),
  );
});

test('fails closed for unknown roles and bad export sources', () => {
  const root = fixture();
  const file = path.join(root, 'packages/domain/package.json');
  const manifest = JSON.parse(fs.readFileSync(file, 'utf8'));
  manifest.mangoArchitecture.role = 'unknown';
  manifest.mangoArchitecture.sourceExports['.'].source = './src/missing.ts';
  writeJson(file, manifest);
  const report = analyzeArchitecture(root);
  assert.throws(() => assertArchitecture(report), /unknown role|source does not resolve/u);
});

test('rejects reverse dependencies without a registered exception', () => {
  const root = fixture();
  const contractFile = path.join(root, 'packages/contracts/package.json');
  const manifest = JSON.parse(fs.readFileSync(contractFile, 'utf8'));
  manifest.dependencies = { '@mango/domain': '1.0.0' };
  writeJson(contractFile, manifest);
  const report = analyzeArchitecture(root);
  assert.match(report.errors.join('\n'), /reverse layer dependency FE0->FE2/u);
});

test('explicitly rejects package to app dependencies', () => {
  const root = fixture();
  const contractFile = path.join(root, 'packages/contracts/package.json');
  const manifest = JSON.parse(fs.readFileSync(contractFile, 'utf8'));
  manifest.dependencies = { 'admin-app': '1.0.0' };
  writeJson(contractFile, manifest);
  const report = analyzeArchitecture(root);
  assert.match(report.errors.join('\n'), /package depends on app/u);
});

test('rejects runtime imports declared only as devDependencies', () => {
  const root = fixture();
  const domainFile = path.join(root, 'packages/domain/package.json');
  const manifest = JSON.parse(fs.readFileSync(domainFile, 'utf8'));
  delete manifest.dependencies;
  manifest.devDependencies = { '@mango/contracts': '1.0.0' };
  writeJson(domainFile, manifest);
  fs.writeFileSync(path.join(root, 'packages/domain/src/index.ts'), "import '@mango/contracts';\n");
  const report = analyzeArchitecture(root);
  assert.match(report.errors.join('\n'), /runtime dependency @mango\/contracts is declared only for development/u);
});

test('rejects tooling dependency cycles', () => {
  const root = fixture();
  for (const packageName of ['contracts', 'domain']) {
    const file = path.join(root, `packages/${packageName}/package.json`);
    const manifest = JSON.parse(fs.readFileSync(file, 'utf8'));
    manifest.devDependencies =
      packageName === 'contracts' ? { '@mango/domain': '1.0.0' } : { '@mango/contracts': '1.0.0' };
    writeJson(file, manifest);
  }
  const report = analyzeArchitecture(root);
  assert.match(report.errors.join('\n'), /tooling-scc:@mango\/contracts,@mango\/domain/u);
});

test('fails closed for non-literal dynamic imports without treating code samples as imports', () => {
  const root = fixture();
  const sourceFile = path.join(root, 'packages/domain/src/index.ts');
  fs.writeFileSync(
    sourceFile,
    [
      "const sample = `import '@mango/not-a-workspace';`;",
      'const target = getTarget();',
      'void import(target);',
      '',
    ].join('\n'),
  );
  const report = analyzeArchitecture(root);
  assert.match(report.errors.join('\n'), /non-literal import is not analyzable/u);
  assert.doesNotMatch(report.errors.join('\n'), /not-a-workspace/u);
});

test('detects missing workspace manifests and preserves deterministic report hashes', () => {
  const root = fixture();
  fs.mkdirSync(path.join(root, 'packages/missing-manifest'));
  const first = analyzeArchitecture(root);
  const second = analyzeArchitecture(root);
  assert.match(first.errors.join('\n'), /packages\/missing-manifest: missing package.json/u);
  assert.equal(first.reportSha256, second.reportSha256);
});

test('allows only cycles contained by the exact legacy SCC baseline', () => {
  const root = fixture();
  const contractFile = path.join(root, 'packages/contracts/package.json');
  const manifest = JSON.parse(fs.readFileSync(contractFile, 'utf8'));
  manifest.dependencies = { '@mango/domain': '1.0.0' };
  writeJson(contractFile, manifest);
  const baselineFile = path.join(root, 'architecture-exceptions.json');
  writeJson(baselineFile, {
    schemaVersion: 1,
    exceptions: [
      {
        from: '@mango/contracts',
        to: '@mango/domain',
        reason: 'fixture legacy edge',
        ownerRole: 'Frontend Standards Owner',
        adr: 'ADR-FE-005',
        decisionEvidence: 'decision.md',
        expiresAt: '2099-01-01',
      },
    ],
    legacySccs: [
      {
        id: 'fixture-manifest-cycle',
        graphKind: 'manifest',
        members: ['@mango/contracts', '@mango/domain'],
        edges: [
          { from: '@mango/contracts', to: '@mango/domain', kind: 'dependencies' },
          { from: '@mango/domain', to: '@mango/contracts', kind: 'dependencies' },
        ],
        ownerRole: 'Frontend Standards Owner',
        adr: 'ADR-FE-005',
        targetPhase: 2,
      },
      {
        id: 'fixture-combined-cycle',
        graphKind: 'combined',
        members: ['@mango/contracts', '@mango/domain'],
        edges: [
          { from: '@mango/contracts', to: '@mango/domain', kind: 'dependencies' },
          { from: '@mango/domain', to: '@mango/contracts', kind: 'contract' },
          { from: '@mango/domain', to: '@mango/contracts', kind: 'dependencies' },
        ],
        ownerRole: 'Frontend Standards Owner',
        adr: 'ADR-FE-005',
        targetPhase: 2,
      },
    ],
  });
  fs.writeFileSync(path.join(path.dirname(root), 'decision.md'), 'fixture decision\n');
  const report = analyzeArchitecture(root);
  assert.equal(report.errors.filter((error) => error.startsWith('scc:')).length, 0);
});

test('rejects invalid exception expiry dates', () => {
  const root = fixture();
  const baselineFile = path.join(root, 'architecture-exceptions.json');
  writeJson(baselineFile, {
    schemaVersion: 1,
    exceptions: [
      {
        from: '@mango/domain',
        to: '@mango/contracts',
        reason: 'invalid fixture expiry',
        ownerRole: 'Frontend Standards Owner',
        adr: 'ADR-FE-005',
        decisionEvidence: 'decision.md',
        expiresAt: 'never',
      },
    ],
    legacySccs: [],
  });
  fs.writeFileSync(path.join(path.dirname(root), 'decision.md'), 'fixture decision\n');
  const report = analyzeArchitecture(root);
  assert.match(report.errors.join('\n'), /expiresAt must be a valid YYYY-MM-DD date/u);
});

test('rejects invalid and expired wildcard source exports', () => {
  const root = fixture();
  const manifestFile = path.join(root, 'packages/domain/package.json');
  const manifest = JSON.parse(fs.readFileSync(manifestFile, 'utf8'));
  manifest.exports['./features/*'] = {
    types: './dist/features/*.d.ts',
    import: './dist/features/*.js',
  };
  manifest.mangoArchitecture.sourceExports['./features/*'] = {
    sourcePattern: './src/features/*.ts',
    kind: 'code',
    expiresAt: 'never',
  };
  writeJson(manifestFile, manifest);
  fs.mkdirSync(path.join(root, 'packages/domain/src/features'), { recursive: true });
  fs.writeFileSync(path.join(root, 'packages/domain/src/features/legacy.ts'), 'export {};\n');
  assert.match(analyzeArchitecture(root).errors.join('\n'), /expiresAt must be a valid YYYY-MM-DD date/u);

  manifest.mangoArchitecture.sourceExports['./features/*'].expiresAt = '2000-01-01';
  writeJson(manifestFile, manifest);
  assert.match(analyzeArchitecture(root).errors.join('\n'), /wildcard source expired on 2000-01-01/u);
});

test('fails closed when workspace discovery has zero input', () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-architecture-empty-'));
  assert.throws(() => analyzeArchitecture(root), /No frontend workspaces/u);
});
