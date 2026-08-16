import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const testDir = path.dirname(fileURLToPath(import.meta.url));
const sourceTools = path.resolve(testDir, '../tools');
const moduleAudit = path.join(sourceTools, 'audit-module-readmes.mjs');
const sourceFactsAudit = path.join(sourceTools, 'audit-readme-source-facts.mjs');

function write(root, relativePath, content = '') {
  const target = path.join(root, relativePath);
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.writeFileSync(target, content);
}

function run(script, { cwd, args = [] } = {}) {
  return spawnSync(process.execPath, [script, ...args], {
    cwd,
    encoding: 'utf8'
  });
}

function createBusinessConsumer() {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-readme-business-'));
  write(root, 'mango.config.json', `${JSON.stringify({
    paths: {
      backend: 'service',
      frontend: 'web',
      businessDocs: 'docs'
    },
    mangoFrontendVersions: {
      '@mango/common': '1.0.0'
    }
  }, null, 2)}\n`);
  write(root, 'business-pmo/mango-baseline/rules/08-capability-docs.md', '# Capability docs\n');
  write(root, 'docs/capabilities/README.md', `# Demo capability map

- [Backend module](../../service/orders/README.md)
- [Frontend component](../../web/packages/orders/src/components/README.md)
`);
  write(root, 'service/orders/README.md', `# Orders

The service endpoint is \`GET /orders/items/page\`.

- [Capability map](../../docs/capabilities/README.md)
`);
  write(root, 'service/orders/pom.xml', `
<project>
  <artifactId>orders-service</artifactId>
  <dependencies>
    <dependency><artifactId>mango-notice-starter</artifactId></dependency>
  </dependencies>
</project>
`);
  write(root, 'service/orders/src/main/java/example/OrdersController.java', `
@RequestMapping("/orders/items")
class OrdersController {
  @GetMapping("/page")
  void page() {}
}
`);
  write(root, 'web/packages/orders/package.json', `${JSON.stringify({
    name: '@example/orders',
    dependencies: { '@mango/common': '1.0.0' }
  }, null, 2)}\n`);
  write(root, 'web/packages/orders/src/components/README.md', `# Orders component

The component uses \`@mango/common\`.

- [Capability map](../../../../../docs/capabilities/README.md)
`);
  const packagedTools = path.join(root, 'business-pmo/mango-baseline/tools');
  fs.mkdirSync(path.join(packagedTools, 'lib'), { recursive: true });
  fs.copyFileSync(moduleAudit, path.join(packagedTools, 'audit-module-readmes.mjs'));
  fs.copyFileSync(sourceFactsAudit, path.join(packagedTools, 'audit-readme-source-facts.mjs'));
  fs.copyFileSync(
    path.join(sourceTools, 'lib/readme-audit-scope.mjs'),
    path.join(packagedTools, 'lib/readme-audit-scope.mjs')
  );
  return root;
}

function createMangoSourceSkeleton() {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-readme-source-'));
  write(root, 'mango/pom.xml', '<project/>\n');
  write(root, 'mango-pmo/rules/index.json', '{}\n');
  write(root, 'mango-ui/packages/mango-pmo/package.json', '{}\n');
  fs.mkdirSync(path.join(root, 'mango-business-starter/business-pmo'), { recursive: true });
  return root;
}

test('packaged business baseline resolves the consumer root and audits declared repository assets', () => {
  const root = createBusinessConsumer();
  try {
    const packagedTools = path.join(root, 'business-pmo/mango-baseline/tools');
    const moduleResult = run(path.join(packagedTools, 'audit-module-readmes.mjs'), { cwd: root });
    assert.equal(moduleResult.status, 0, moduleResult.stderr);
    assert.match(moduleResult.stdout, /Module README audit scope: business-consumer/);
    assert.match(moduleResult.stdout, /service\/orders\/README\.md/);
    assert.match(moduleResult.stdout, /web\/packages\/orders\/src\/components\/README\.md/);
    assert.doesNotMatch(moduleResult.stdout, /mango-business-starter\/README\.md/);

    const factsResult = run(path.join(packagedTools, 'audit-readme-source-facts.mjs'), { cwd: root });
    assert.equal(factsResult.status, 0, factsResult.stderr);
    assert.match(factsResult.stdout, /README source facts audit scope: business-consumer/);
    assert.match(factsResult.stdout, /docs\/capabilities\/README\.md/);
    assert.match(factsResult.stdout, /service\/orders\/README\.md/);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('business consumer audit fails instead of passing with an empty managed README set', () => {
  const root = createBusinessConsumer();
  try {
    write(root, 'docs/capabilities/README.md', '# Capability map without module links\n');
    const script = path.join(root, 'business-pmo/mango-baseline/tools/audit-module-readmes.mjs');
    const result = run(script, { cwd: root });
    assert.equal(result.status, 1);
    assert.match(result.stderr, /does not reference a README/);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('business source facts audit reports a declared endpoint that is absent from repository source', () => {
  const root = createBusinessConsumer();
  try {
    write(root, 'service/orders/README.md', `# Orders

The service endpoint is \`GET /missing/items/page\`.

- [Capability map](../../docs/capabilities/README.md)
`);
    const script = path.join(root, 'business-pmo/mango-baseline/tools/audit-readme-source-facts.mjs');
    const result = run(script, { cwd: root });
    assert.equal(result.status, 1);
    assert.match(result.stdout, /api:\/missing\/items\/page/);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('explicit wrong root fails closed', () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-readme-wrong-root-'));
  try {
    const result = run(moduleAudit, {
      cwd: root,
      args: ['--project-root', root]
    });
    assert.equal(result.status, 1);
    assert.match(result.stderr, /neither the Mango source repository nor a Mango business consumer/);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('Mango source audit still fails when a fixed source README is missing', () => {
  const root = createMangoSourceSkeleton();
  try {
    const result = run(moduleAudit, {
      cwd: root,
      args: ['--project-root', root]
    });
    assert.equal(result.status, 1);
    assert.match(result.stdout, /mango\/mango-admin-starter\/README\.md/);
    assert.match(result.stdout, /README file/);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('business configured paths cannot escape the repository root', () => {
  const root = createBusinessConsumer();
  try {
    write(root, 'mango.config.json', `${JSON.stringify({
      paths: {
        backend: '../outside',
        frontend: 'web',
        businessDocs: 'docs'
      }
    }, null, 2)}\n`);
    const script = path.join(root, 'business-pmo/mango-baseline/tools/audit-module-readmes.mjs');
    const result = run(script, { cwd: root });
    assert.equal(result.status, 1);
    assert.match(result.stderr, /must stay inside the project root/);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('business capability README links cannot escape through a symlink', () => {
  const root = createBusinessConsumer();
  const outside = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-readme-outside-'));
  try {
    write(outside, 'README.md', '# Outside\n');
    fs.rmSync(path.join(root, 'service/orders/README.md'));
    fs.symlinkSync(path.join(outside, 'README.md'), path.join(root, 'service/orders/README.md'));
    const script = path.join(root, 'business-pmo/mango-baseline/tools/audit-module-readmes.mjs');
    const result = run(script, { cwd: root });
    assert.equal(result.status, 1);
    assert.match(result.stderr, /outside the project root/);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
    fs.rmSync(outside, { recursive: true, force: true });
  }
});
