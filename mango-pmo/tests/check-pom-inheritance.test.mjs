import assert from 'node:assert/strict';
import { mkdirSync, mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';
import { analyse } from '../tools/check-pom-inheritance.mjs';

function pom({ groupId = 'io.mango', artifactId, parent = null, dependencyManagement = false, pluginManagement = false }) {
  const parentBlock = parent
    ? `  <parent>\n    <groupId>${parent.groupId ?? 'io.mango'}</groupId>\n    <artifactId>${parent.artifactId}</artifactId>\n    <version>1.0.0</version>\n  </parent>\n`
    : '';
  const dm = dependencyManagement ? '  <dependencyManagement><dependencies/></dependencyManagement>\n' : '';
  const pm = pluginManagement ? '  <pluginManagement><plugins/></pluginManagement>\n' : '';
  return `<?xml version="1.0"?>\n<project>\n${parentBlock}  <groupId>${groupId}</groupId>\n  <artifactId>${artifactId}</artifactId>\n  <version>1.0.0</version>\n  <packaging>pom</packaging>\n${dm}${pm}</project>\n`;
}

function fixture(files) {
  const root = mkdtempSync(join(tmpdir(), 'pom-check-'));
  for (const [path, content] of Object.entries(files)) {
    mkdirSync(join(root, path, '..'), { recursive: true });
    writeFileSync(join(root, path), content);
  }
  return root;
}

test('根聚合继承 mango-parent 时无偏差', () => {
  const root = fixture({
    'pom.xml': pom({ artifactId: 'mango', parent: { artifactId: 'mango-parent' } }),
    'mango-parent/pom.xml': pom({ artifactId: 'mango-parent', dependencyManagement: true, pluginManagement: true }),
    'mango-bom/pom.xml': pom({ artifactId: 'mango-bom', dependencyManagement: true }),
    'svc/pom.xml': pom({ artifactId: 'svc', parent: { artifactId: 'mango-parent' } }),
  });
  assert.deepEqual(analyse(root).findings, []);
});

test('根聚合未继承 mango-parent 时报告 ROOT_NOT_INHERITING', () => {
  const root = fixture({
    'pom.xml': pom({ artifactId: 'mango' }),
    'mango-parent/pom.xml': pom({ artifactId: 'mango-parent' }),
    'mango-bom/pom.xml': pom({ artifactId: 'mango-bom' }),
  });
  const rules = analyse(root).findings.map((item) => item.rule);
  assert.deepEqual(rules, ['ROOT_NOT_INHERITING']);
});

test('模块重复声明 dependencyManagement 或 pluginManagement 时报告', () => {
  const root = fixture({
    'pom.xml': pom({ artifactId: 'mango', parent: { artifactId: 'mango-parent' } }),
    'mango-parent/pom.xml': pom({ artifactId: 'mango-parent', dependencyManagement: true, pluginManagement: true }),
    'mango-bom/pom.xml': pom({ artifactId: 'mango-bom', dependencyManagement: true }),
    'dup/pom.xml': pom({ artifactId: 'dup', parent: { artifactId: 'mango-parent' }, dependencyManagement: true, pluginManagement: true }),
  });
  const rules = analyse(root).findings.map((item) => item.rule).sort();
  assert.deepEqual(rules, ['DEPENDENCY_MANAGEMENT_DUPLICATED', 'PLUGIN_MANAGEMENT_DUPLICATED']);
});

test('测试夹具目录不参与父链约束', () => {
  const root = fixture({
    'pom.xml': pom({ artifactId: 'mango', parent: { artifactId: 'mango-parent' } }),
    'mango-parent/pom.xml': pom({ artifactId: 'mango-parent', dependencyManagement: true, pluginManagement: true }),
    'mango-bom/pom.xml': pom({ artifactId: 'mango-bom', dependencyManagement: true }),
    'plugin/src/it/consumer/pom.xml': pom({ artifactId: 'consumer', parent: { artifactId: 'external-parent' }, dependencyManagement: true }),
    'plugin/src/test/resources/probe/pom.xml': pom({ artifactId: 'probe', pluginManagement: true }),
  });
  assert.deepEqual(analyse(root).findings, []);
});

test('父链成环时报告 PARENT_CHAIN_CYCLE', () => {
  const root = fixture({
    'pom.xml': pom({ artifactId: 'mango', parent: { artifactId: 'mango-parent' } }),
    'mango-parent/pom.xml': pom({ artifactId: 'mango-parent', dependencyManagement: true, pluginManagement: true }),
    'mango-bom/pom.xml': pom({ artifactId: 'mango-bom', dependencyManagement: true }),
    'loop-a/pom.xml': pom({ artifactId: 'loop-a', parent: { artifactId: 'loop-b' } }),
    'loop-b/pom.xml': pom({ artifactId: 'loop-b', parent: { artifactId: 'loop-a' } }),
  });
  const findings = analyse(root).findings;
  assert.deepEqual(findings.map((item) => item.rule).sort(), ['PARENT_CHAIN_CYCLE', 'PARENT_CHAIN_CYCLE']);
});

test('父链落到非基座 POM（如 mango-bom）时报告 PARENT_CHAIN_UNRESOLVED', () => {
  const root = fixture({
    'pom.xml': pom({ artifactId: 'mango', parent: { artifactId: 'mango-parent' } }),
    'mango-parent/pom.xml': pom({ artifactId: 'mango-parent', dependencyManagement: true, pluginManagement: true }),
    'mango-bom/pom.xml': pom({ artifactId: 'mango-bom', dependencyManagement: true }),
    'orphan/pom.xml': pom({ artifactId: 'orphan', parent: { artifactId: 'mango-bom' } }),
    'dangling/pom.xml': pom({ artifactId: 'dangling', parent: { artifactId: 'not-in-repo' } }),
  });
  const findings = analyse(root).findings;
  assert.deepEqual(findings.map((item) => item.rule).sort(), ['PARENT_CHAIN_UNRESOLVED', 'PARENT_CHAIN_UNRESOLVED']);
  assert.deepEqual(findings.map((item) => item.file).sort(), ['dangling/pom.xml', 'orphan/pom.xml']);
});
