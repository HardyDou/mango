import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

import { validateLeanDocument } from '../tools/check-lean-document.mjs';

const pmoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const examplesRoot = path.join(pmoRoot, 'examples/lean-documents');
const templatesRoot = path.join(pmoRoot, 'templates');
const contract = JSON.parse(fs.readFileSync(path.join(pmoRoot, 'contracts/lean-documents.json'), 'utf8'));

const exampleNames = [
  'delivery-l2-json-error.md',
  'delivery-l3-workflow-withdraw.md',
  'delivery-l4-third-party-login.md',
  'l5-supplier-business-requirements.md',
  'l5-supplier-system-requirements.md',
  'l5-supplier-technical-design.md',
  'l5-supplier-implementation-plan.md',
];

const templateNames = [
  'delivery-l2.md',
  'delivery-l3.md',
  'delivery-l4.md',
  'l5-business-requirements.md',
  'l5-system-requirements.md',
  'l5-technical-design.md',
  'l5-implementation-plan.md',
];

function example(name) {
  return fs.readFileSync(path.join(examplesRoot, name), 'utf8');
}

function findings(source, options) {
  return validateLeanDocument(source, options).findings;
}

test('七个真实示例全部通过当前精简合同', () => {
  for (const name of exampleNames) assert.deepEqual(findings(example(name)), [], name);
});

test('七个模板全部通过模板结构检查', () => {
  for (const name of templateNames) {
    const source = fs.readFileSync(path.join(templatesRoot, name), 'utf8');
    assert.deepEqual(findings(source, { template: true }), [], name);
  }
});

test('合同固定 L0/L1 无文档、L2-L4 单文档和 L5 四文档', () => {
  assert.equal(contract.levels.L0.artifactPolicy, 'NONE');
  assert.equal(contract.levels.L1.artifactPolicy, 'NONE');
  assert.deepEqual(
    ['L2', 'L3', 'L4'].map(level => [contract.levels[level].artifactPolicy, contract.levels[level].maxA4Pages]),
    [['SINGLE', 1], ['SINGLE', 3], ['SINGLE', 5]],
  );
  assert.equal(contract.levels.L5.artifactPolicy, 'INDEPENDENT_DOCUMENTS');
  assert.equal(contract.levels.L5.templates.length, 4);
});

test('未知文档类型和错误等级被阻断', () => {
  const source = example('delivery-l2-json-error.md');
  assert.match(findings(source.replace('documentType: delivery-l2', 'documentType: unknown')).join('\n'), /未知 documentType/);
  assert.match(findings(source.replace('deliveryLevel: L2', 'deliveryLevel: L1')).join('\n'), /deliveryLevel 必须为 L2/);
});

test('章节缺失、乱序或增加无关 H2 被阻断', () => {
  const source = example('delivery-l2-json-error.md');
  assert.match(findings(source.replace('## 技术改动', '## 其它内容')).join('\n'), /H2 必须依次为/);
  assert.match(findings(source.replace('## 技术改动', '## 无关摘要\n\n重复说明。\n\n## 技术改动')).join('\n'), /H2 必须依次为/);
});

test('L2 超过一页以及藏在 Mermaid 中的超长内容都被页数门禁统计', () => {
  const l2 = example('delivery-l2-json-error.md').replace('当前参数类型错误', `${'超'.repeat(1900)}当前参数类型错误`);
  assert.match(findings(l2).join('\n'), /超过 1 张 A4/);
  const l3 = example('delivery-l3-workflow-withdraw.md').replace('flowchart LR', `flowchart LR\n  X[${'长'.repeat(5500)}]`);
  assert.match(findings(l3).join('\n'), /超过 3 张 A4/);
});

test('未替换模板占位符被阻断', () => {
  const source = example('delivery-l2-json-error.md').replace('当前参数类型错误', '{{当前问题}}');
  assert.match(findings(source).join('\n'), /未替换模板占位符/);
});

test('用户故事必须使用序号并保持一项一行', () => {
  const source = example('delivery-l3-workflow-withdraw.md').replace('2. US-001 -> BR-001：', 'US-001 -> BR-001：');
  const result = findings(source).join('\n');
  assert.match(result, /用户故事必须使用|缺少直接追踪格式：US/);
});

for (const [id, from, to] of [
  ['US', '2. US-001 -> BR-001：', '2. US-001：'],
  ['SR', '1. SR-001 -> US-001：', '1. SR-001 -> TD-001：'],
  ['TD', '1. TD-001 -> SR-001：', '1. TD-001 -> BR-001：'],
  ['TASK', '1. TASK-001 -> TD-001：', '1. TASK-001 -> SR-001：'],
  ['VAL', '2. VAL-001 -> SR-001：', '2. VAL-001 -> TD-001：'],
]) {
  test(`${id} 的错误直接追踪被阻断`, () => {
    const source = example('delivery-l3-workflow-withdraw.md').replace(from, to);
    assert.match(findings(source).join('\n'), new RegExp(`直接追踪格式错误.*${id}-001`));
  });
}

test('同一类型中任意一项断链都会失败，不会被另一条正确记录掩盖', () => {
  const source = example('l5-supplier-system-requirements.md').replace('2. SR-002 -> US-002：', '2. SR-002 -> TD-002：');
  assert.match(findings(source).join('\n'), /直接追踪格式错误：2\. SR-002/);
});

test('一个条目不得使用斜杠同时指向多个直接上游', () => {
  const source = example('l5-supplier-implementation-plan.md').replace('1. VAL-001 -> TASK-002：', '1. VAL-001 -> TASK-002/SR-001：');
  assert.match(findings(source).join('\n'), /直接追踪格式错误：1\. VAL-001/);
});

test('重复的直接追踪 ID 被阻断', () => {
  const source = example('l5-supplier-system-requirements.md').replace('2. SR-002 -> US-002：', '2. SR-001 -> US-002：');
  assert.match(findings(source).join('\n'), /直接追踪 ID 重复：SR-001/);
});

test('实施计划引用 TD 不会被误判为必须重新定义 TD', () => {
  assert.deepEqual(findings(example('l5-supplier-implementation-plan.md')), []);
});

test('规范和代码引用必须包含精确版本或提交', () => {
  const base = example('delivery-l2-json-error.md');
  const noSpecVersion = base.replace('rules/backend/03-api.md@1.4.0', 'rules/backend/03-api.md@current');
  assert.match(findings(noSpecVersion).join('\n'), /精确版本/);
  const noCodeVersion = base.replace('GlobalExceptionHandler.java@8f2c1ad', 'GlobalExceptionHandler.java@latest');
  assert.match(findings(noCodeVersion).join('\n'), /精确提交或版本/);
});

test('L3/L4/SRS 缺少适用图被阻断', () => {
  for (const name of ['delivery-l3-workflow-withdraw.md', 'delivery-l4-third-party-login.md', 'l5-supplier-system-requirements.md']) {
    const source = example(name).replace(/```mermaid[\s\S]*?```/gu, '');
    assert.match(findings(source).join('\n'), /缺少必需内容：```mermaid/, name);
  }
});

test('L5 技术设计必须包含枚举和字段字典', () => {
  const source = example('l5-supplier-technical-design.md')
    .replace(/枚举/gu, '类型代码')
    .replace(/字段/gu, '属性项');
  const result = findings(source).join('\n');
  assert.match(result, /缺少必需内容：枚举/);
  assert.match(result, /缺少必需内容：字段/);
});

for (const filler of contract.forbiddenFiller) {
  test(`空话“${filler}”被阻断`, () => {
    const source = example('delivery-l2-json-error.md').replace('只改统一异常转换', `${filler}；只改统一异常转换`);
    assert.match(findings(source).join('\n'), new RegExp(`包含禁止空话：${filler}`));
  });
}
