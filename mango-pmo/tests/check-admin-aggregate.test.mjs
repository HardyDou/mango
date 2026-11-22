import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';

import {
  discoverModules,
  evaluate,
  loadAllowList,
  parseAggregatedStarters,
  parseFlywayModules,
  parseResourceLocations,
  parseYamlPaths,
} from '../tools/check-admin-aggregate.mjs';

const TOOL = new URL('../tools/check-admin-aggregate.mjs', import.meta.url).pathname;
const YML = 'mango-app/monolith/mango-monolith-app/src/main/resources/application.yml';

function writeTree(files) {
  const root = mkdtempSync(join(tmpdir(), 'check-admin-aggregate-'));
  for (const [path, content] of Object.entries(files)) {
    mkdirSync(join(root, path, '..'), { recursive: true });
    writeFileSync(join(root, path), content, 'utf8');
  }
  return root;
}

function unitPom(artifactId, parentArtifactId = 'mango-domain') {
  return `<?xml version="1.0"?>
<project>
  <parent>
    <groupId>io.mango.platform</groupId>
    <artifactId>${parentArtifactId}</artifactId>
  </parent>
  <artifactId>${artifactId}</artifactId>
</project>
`;
}

function adminPom(starters) {
  const deps = starters
    .map((artifactId) => `    <dependency><groupId>io.mango.platform</groupId><artifactId>${artifactId}</artifactId></dependency>`)
    .join('\n');
  return `<?xml version="1.0"?>
<project>
  <parent><groupId>io.mango</groupId><artifactId>mango</artifactId></parent>
  <artifactId>mango-admin-starter</artifactId>
  <dependencies>
${deps}
  </dependencies>
</project>
`;
}

function appYml({ flyway = {}, locations = null } = {}) {
  const lines = ['mango:', '  persistence:', '    flyway:', '      modules:'];
  for (const [key, config] of Object.entries(flyway)) {
    lines.push(`        ${key}:`);
    if (config.enabled !== undefined) lines.push(`          enabled: ${config.enabled}`);
    if (config.skipReason !== undefined) lines.push(`          skip-reason: ${config.skipReason}`);
  }
  if (locations) {
    lines.push('  resource:', '    registry:', '      locations:');
    for (const location of locations) lines.push(`        - ${location}`);
  }
  return `${lines.join('\n')}\n`;
}

const MIGRATION = 'V1__init.sql';
const RESOURCE = '{"kind":"MENU"}';

/** 最小目录树：`mango-platform/<domain>/<unit>` + admin 聚合 POM + 单体 application.yml。 */
function fixture({ starters, admin, flyway, locations, resources = {}, migrations = {}, allow = null }) {
  const files = {
    'mango-admin-starter/pom.xml': adminPom(admin),
    [YML]: appYml({ flyway, locations }),
  };
  for (const [domain, units] of Object.entries(starters)) {
    for (const artifactId of units) {
      files[`mango-platform/${domain}/${artifactId}/pom.xml`] = unitPom(artifactId, domain);
      for (const key of migrations[artifactId] ?? []) {
        files[`mango-platform/${domain}/${artifactId}/src/main/resources/db/migration/${key}/${MIGRATION}`] = 'select 1;\n';
      }
      for (const name of resources[artifactId] ?? []) {
        files[`mango-platform/${domain}/${artifactId}/src/main/resources/META-INF/mango/resources/${name}`] = RESOURCE;
      }
    }
  }
  if (allow) files['allow.json'] = JSON.stringify({ allow });
  return writeTree(files);
}

function evaluateRoot(root) {
  return evaluate(discoverModules(join(root, 'mango-platform')), {
    aggregated: parseAggregatedStarters(join(root, 'mango-admin-starter/pom.xml')),
    flywayModules: parseFlywayModules(join(root, YML)),
    resourceLocations: parseResourceLocations(join(root, YML)),
    allowList: loadAllowList(join(root, 'allow.json')),
  });
}

function runTool(root, args = []) {
  return spawnSync(
    process.execPath,
    [TOOL, '--root', root, '--allow-file', join(root, 'allow.json'), ...args],
    { encoding: 'utf8' },
  );
}

test('A：starter 未被 admin 聚合时记 ERROR，并给出插入位置建议', () => {
  const root = fixture({
    starters: { 'mango-demo': ['mango-demo-core', 'mango-demo-starter'] },
    admin: [],
    flyway: {},
    locations: ['classpath*:META-INF/mango/resources/*.json'],
  });
  const result = evaluateRoot(root);
  assert.deepEqual(result.errors.map((item) => item.rule), ['STARTER_NOT_AGGREGATED']);
  assert.equal(result.errors[0].artifactId, 'mango-demo-starter');
  assert.equal(result.errors[0].module, 'mango-platform/mango-demo/mango-demo-starter');
  assert.match(result.errors[0].hint, /mango-admin-starter\/pom\.xml/);
  assert.equal(result.total, 2);

  const cli = runTool(root, ['--json']);
  assert.equal(cli.status, 1);
  const payload = JSON.parse(cli.stdout);
  assert.deepEqual(payload.errors.map((item) => item.subject), ['mango-demo-starter']);
  assert.deepEqual(payload.warnings, []);
});

test('A：starter 已聚合时通过，默认退出 0', () => {
  const root = fixture({
    starters: { 'mango-demo': ['mango-demo-starter'] },
    admin: ['mango-demo-starter'],
    flyway: {},
    locations: ['classpath*:META-INF/mango/resources/*.json'],
  });
  const result = evaluateRoot(root);
  assert.deepEqual(result.errors, []);
  assert.deepEqual(result.warnings, []);
  assert.equal(runTool(root).status, 0);
});

test('B：迁移键在 application.yml 中没有开关时记 ERROR', () => {
  const root = fixture({
    starters: { 'mango-demo': ['mango-demo-starter'] },
    admin: ['mango-demo-starter'],
    flyway: {},
    locations: ['classpath*:META-INF/mango/resources/*.json'],
    migrations: { 'mango-demo-starter': ['demo'] },
  });
  const result = evaluateRoot(root);
  assert.deepEqual(result.errors.map((item) => item.rule), ['FLYWAY_MODULE_MISSING']);
  assert.equal(result.errors[0].subject, 'demo');
  assert.equal(result.errors[0].ymlKey, 'mango.persistence.flyway.modules.demo');
  assert.equal(runTool(root).status, 1);
});

test('B：enabled: false 且无 skip-reason 时记 ERROR，带 skip-reason 时通过', () => {
  const broken = fixture({
    starters: { 'mango-demo': ['mango-demo-starter'] },
    admin: ['mango-demo-starter'],
    flyway: { demo: { enabled: 'false' } },
    locations: ['classpath*:META-INF/mango/resources/*.json'],
    migrations: { 'mango-demo-starter': ['demo'] },
  });
  const brokenResult = evaluateRoot(broken);
  assert.deepEqual(brokenResult.errors.map((item) => item.rule), ['FLYWAY_SKIP_REASON_MISSING']);
  assert.equal(brokenResult.errors[0].subject, 'demo');
  assert.equal(runTool(broken).status, 1);

  const skipped = fixture({
    starters: { 'mango-demo': ['mango-demo-starter'] },
    admin: ['mango-demo-starter'],
    flyway: { demo: { enabled: 'false', skipReason: 'demo 表由业务应用自带' } },
    locations: ['classpath*:META-INF/mango/resources/*.json'],
    migrations: { 'mango-demo-starter': ['demo'] },
  });
  assert.deepEqual(evaluateRoot(skipped).errors, []);
  assert.equal(runTool(skipped).status, 0);
});

test('B：仅有开关没有对应迁移时记 WARNING，--strict 下才失败；测试资源不计入迁移键', () => {
  const root = fixture({
    starters: { 'mango-demo': ['mango-demo-starter'] },
    admin: ['mango-demo-starter'],
    flyway: { demo: { enabled: 'true' }, kv: { enabled: 'true' } },
    locations: ['classpath*:META-INF/mango/resources/*.json'],
    migrations: { 'mango-demo-starter': ['demo'] },
  });
  const testOnly = join(root, 'mango-platform/mango-demo/mango-demo-starter/src/test/resources/db/migration/test-only');
  mkdirSync(testOnly, { recursive: true });
  writeFileSync(join(testOnly, MIGRATION), 'select 1;\n', 'utf8');
  const result = evaluateRoot(root);
  assert.deepEqual(result.errors, []);
  assert.deepEqual(result.warnings.map((item) => item.subject), ['kv']);
  assert.equal(result.warnings[0].rule, 'FLYWAY_MODULE_ORPHAN');
  assert.deepEqual(
    discoverModules(join(root, 'mango-platform'))[0].units[0].migrations,
    ['demo'],
  );
  assert.equal(runTool(root).status, 0);
  assert.equal(runTool(root, ['--strict']).status, 1);
});

test('C：Resource 声明所在 starter 已聚合但 locations 不含 *.json 时记 ERROR', () => {
  const root = fixture({
    starters: { 'mango-demo': ['mango-demo-starter'] },
    admin: ['mango-demo-starter'],
    flyway: {},
    locations: ['classpath*:META-INF/mango/resources/*.yml'],
    resources: { 'mango-demo-starter': ['demo-common-menu.json'] },
  });
  const result = evaluateRoot(root);
  assert.deepEqual(result.errors.map((item) => item.rule), ['RESOURCE_NOT_REACHABLE']);
  assert.equal(result.errors[0].artifactId, 'mango-demo-starter');
  assert.deepEqual(result.errors[0].resources, ['demo-common-menu.json']);
  assert.equal(runTool(root).status, 1);
});

test('C：locations 未声明时按框架默认值判为可达，只给 WARNING', () => {
  const root = fixture({
    starters: { 'mango-demo': ['mango-demo-starter'] },
    admin: ['mango-demo-starter'],
    flyway: {},
    locations: null,
    resources: { 'mango-demo-starter': ['demo-common-menu.json'] },
  });
  const result = evaluateRoot(root);
  assert.deepEqual(result.errors, []);
  assert.deepEqual(result.warnings.map((item) => item.rule), ['RESOURCE_LOCATIONS_IMPLICIT']);
  assert.equal(runTool(root).status, 0);
  assert.equal(runTool(root, ['--strict']).status, 1);
});

test('白名单命中时不计入 ERROR，CLI 退出 0', () => {
  const root = fixture({
    starters: { 'mango-demo': ['mango-demo-starter'] },
    admin: [],
    flyway: {},
    locations: ['classpath*:META-INF/mango/resources/*.json'],
    allow: [{ rule: 'STARTER_NOT_AGGREGATED', subject: 'mango-demo-starter', reason: '部署形态互斥' }],
  });
  const result = evaluateRoot(root);
  assert.deepEqual(result.errors, []);
  assert.deepEqual(result.warnings, []);
  assert.equal(runTool(root).status, 0);

  const other = fixture({
    starters: { 'mango-demo': ['mango-demo-starter'] },
    admin: [],
    flyway: {},
    locations: ['classpath*:META-INF/mango/resources/*.json'],
    allow: [{ rule: 'FLYWAY_MODULE_MISSING', subject: 'mango-demo-starter', reason: '规则不同不命中' }],
  });
  assert.equal(runTool(other).status, 1);
});

test('YAML 子集解析能读出嵌套开关与非空 skip-reason', () => {
  const { scalars, lists } = parseYamlPaths(
    [
      'mango:',
      '  persistence:',
      '    flyway:',
      '      modules:',
      '        demo:',
      '          enabled: false   # 关闭',
      '          skip-reason: "由业务应用维护"',
      '  resource:',
      '    registry:',
      '      locations:',
      '        - classpath*:META-INF/mango/resources/*.json',
      '        - classpath*:META-INF/mango/resources/*.yml',
    ].join('\n'),
  );
  assert.equal(scalars.get('mango.persistence.flyway.modules.demo.enabled'), 'false');
  assert.equal(scalars.get('mango.persistence.flyway.modules.demo.skip-reason'), '由业务应用维护');
  assert.deepEqual(lists.get('mango.resource.registry.locations'), [
    'classpath*:META-INF/mango/resources/*.json',
    'classpath*:META-INF/mango/resources/*.yml',
  ]);
});
