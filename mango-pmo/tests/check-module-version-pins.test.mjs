import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { mkdirSync, mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';

import {
  collectDependencyPins,
  evaluate,
  parseBom,
  parseProperties,
  resolveVersion,
} from '../tools/check-module-version-pins.mjs';

const TOOL = new URL('../tools/check-module-version-pins.mjs', import.meta.url).pathname;

function writeTree(files) {
  const root = mkdtempSync(join(tmpdir(), 'module-version-pins-'));
  for (const [path, content] of Object.entries(files)) {
    mkdirSync(join(root, path, '..'), { recursive: true });
    writeFileSync(join(root, path), content, 'utf8');
  }
  return root;
}

function dependencyXml({ groupId, artifactId, version }, indent) {
  const pad = ' '.repeat(indent);
  const versionTag = version ? `<version>${version}</version>` : '';
  return `${pad}<dependency><groupId>${groupId}</groupId><artifactId>${artifactId}</artifactId>${versionTag}</dependency>`;
}

function bomPom(properties, managed) {
  const props = Object.entries(properties)
    .map(([key, value]) => `        <${key}>${value}</${key}>`)
    .join('\n');
  const dependencies = managed.map((item) => dependencyXml(item, 12)).join('\n');
  return `<?xml version="1.0"?>
<project>
  <groupId>io.mango</groupId>
  <artifactId>mango-bom</artifactId>
  <version>\${revision}</version>
  <properties>
    <revision>1.0.0-SNAPSHOT</revision>
${props}
  </properties>
  <dependencyManagement>
    <dependencies>
${dependencies}
    </dependencies>
  </dependencyManagement>
</project>
`;
}

function modulePom(dependencies, { managed = [] } = {}) {
  const deps = dependencies.map((item) => dependencyXml(item, 8)).join('\n');
  const management = managed.length
    ? `  <dependencyManagement>\n    <dependencies>\n${managed.map((item) => dependencyXml(item, 6)).join('\n')}\n    </dependencies>\n  </dependencyManagement>\n`
    : '';
  return `<?xml version="1.0"?>
<project>
  <groupId>io.mango</groupId>
  <artifactId>svc</artifactId>
${management}  <dependencies>
${deps}
  </dependencies>
</project>
`;
}

const LUNAR = { groupId: 'cn.6tail', artifactId: 'lunar', version: '1.7.7' };

function fixture(moduleContent, { managed = [{ groupId: 'cn.6tail', artifactId: 'lunar', version: '${lunar.version}' }], properties = { 'lunar.version': '1.7.7' }, allow = null } = {}) {
  const files = {
    'mango-bom/pom.xml': bomPom(properties, managed),
    'svc/pom.xml': moduleContent,
  };
  if (allow) files['allow.json'] = JSON.stringify(allow);
  return writeTree(files);
}

function runTool(root, args = []) {
  return spawnSync(
    process.execPath,
    [TOOL, '--root', root, '--bom', join(root, 'mango-bom/pom.xml'), '--allow-file', join(root, 'allow.json'), ...args],
    { encoding: 'utf8' },
  );
}

test('BOM 生效值一致时记 WARNING，默认退出 0、--strict 退出 1', () => {
  const root = fixture(modulePom([LUNAR]));
  const result = evaluate(collectDependencyPins(root), parseBom(join(root, 'mango-bom/pom.xml')), []);
  assert.equal(result.errors.length, 0);
  assert.deepEqual(result.warnings.map((item) => item.rule), ['REDUNDANT_VERSION']);
  assert.equal(result.warnings[0].module, 'svc/pom.xml');
  assert.equal(result.total, 1);

  assert.equal(runTool(root).status, 0);
  assert.equal(runTool(root, ['--fail-on', 'drift']).status, 0);
  assert.equal(runTool(root, ['--strict']).status, 1);
  assert.equal(runTool(root, ['--fail-on', 'all']).status, 1);
});

test('BOM 生效值不一致时记 ERROR 且默认退出 1', () => {
  const root = fixture(modulePom([{ ...LUNAR, version: '1.7.6' }]));
  const result = evaluate(collectDependencyPins(root), parseBom(join(root, 'mango-bom/pom.xml')), []);
  assert.deepEqual(result.errors.map((item) => item.rule), ['VERSION_DRIFT']);
  assert.equal(result.errors[0].version, '1.7.6');
  assert.equal(result.errors[0].expected, '1.7.7');
  assert.equal(result.warnings.length, 0);

  const cli = runTool(root, ['--json']);
  assert.equal(cli.status, 1);
  const payload = JSON.parse(cli.stdout);
  assert.equal(payload.total, 1);
  assert.deepEqual(payload.errors.map((item) => item.artifactId), ['lunar']);
  assert.deepEqual(payload.warnings, []);
});

test('坐标不在 BOM 时忽略，不算违规', () => {
  const root = fixture(
    modulePom([{ groupId: 'org.example', artifactId: 'outside', version: '9.9.9' }, LUNAR]),
  );
  const result = evaluate(collectDependencyPins(root), parseBom(join(root, 'mango-bom/pom.xml')), []);
  assert.equal(result.total, 2);
  assert.deepEqual(result.errors, []);
  assert.deepEqual(result.warnings.map((item) => item.artifactId), ['lunar']);
  assert.equal(runTool(root).status, 0);
});

test('${xxx.version} 属性替换解析出 BOM 生效值，未定义属性视为不可比对', () => {
  const root = fixture(
    modulePom([
      { groupId: 'cn.6tail', artifactId: 'lunar', version: '1.7.7' },
      { groupId: 'io.mango', artifactId: 'mango-common', version: '1.0.0-SNAPSHOT' },
    ]),
    {
      properties: { 'lunar.version': '1.7.7', 'base.version': '2.0.0', 'nested.version': '${base.version}' },
      managed: [
        { groupId: 'cn.6tail', artifactId: 'lunar', version: '${lunar.version}' },
        { groupId: 'io.mango', artifactId: 'mango-common', version: '${project.version}' },
        { groupId: 'org.example', artifactId: 'nested', version: '${nested.version}' },
      ],
    },
  );
  const bom = parseBom(join(root, 'mango-bom/pom.xml'));
  assert.equal(bom.managed.get('cn.6tail:lunar'), '1.7.7');
  assert.equal(bom.managed.get('org.example:nested'), '2.0.0');
  assert.equal(bom.managed.get('io.mango:mango-common'), null);

  const result = evaluate(collectDependencyPins(root), bom, []);
  // mango-common 的 BOM 版本是 ${project.version}，不可比对，直接跳过
  assert.deepEqual(result.errors, []);
  assert.deepEqual(result.warnings.map((item) => item.artifactId), ['lunar']);
  assert.equal(parseProperties('<properties><a.version>1</a.version></properties>')['a.version'], '1');
  assert.equal(resolveVersion('${missing.version}', {}), null);
});

test('白名单命中的条目不计入 error/warning', () => {
  const allow = {
    allow: [
      {
        groupId: 'cn.6tail',
        artifactId: 'lunar',
        module: 'svc/pom.xml',
        reason: '故意保留旧版本用于兼容性测试',
      },
    ],
  };
  const root = fixture(modulePom([{ ...LUNAR, version: '1.7.6' }]), { allow });
  const result = evaluate(collectDependencyPins(root), parseBom(join(root, 'mango-bom/pom.xml')), allow.allow);
  assert.deepEqual(result.errors, []);
  assert.deepEqual(result.warnings, []);
  assert.equal(runTool(root).status, 0);
  assert.equal(runTool(root, ['--strict']).status, 0);
});

test('dependencyManagement 段的依赖不算模块 pin（BOM 自身不报）', () => {
  const root = fixture(
    modulePom([LUNAR], { managed: [{ ...LUNAR, version: '1.7.7' }] }),
  );
  const pins = collectDependencyPins(root);
  assert.deepEqual(pins.map((item) => ({ artifactId: item.artifactId, version: item.version })), [
    { artifactId: 'lunar', version: '1.7.7' },
  ]);
});
