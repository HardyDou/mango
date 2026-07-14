#!/usr/bin/env node
import { readFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const testRoot = dirname(fileURLToPath(import.meta.url));
const pmoRoot = resolve(testRoot, '../..');
const fixturePath = join(testRoot, 'delivery-assurance-recommendation-cases.json');
const contractPath = join(pmoRoot, 'contracts/delivery-assurance.json');
const fixture = JSON.parse(readFileSync(fixturePath, 'utf8'));
const contract = JSON.parse(readFileSync(contractPath, 'utf8'));
const ruleText = readFileSync(join(pmoRoot, 'rules/11-delivery-assurance.md'), 'utf8');

const FIXED_CATALOG = [
  ['M01', 'Worktree 隔离', ['CREATE', 'DO_NOT_CREATE'], '需要修改受版本控制文件', '隔离任务改动，避免污染错误分支或其它并行工作。'],
  ['M02', '数据库重建', ['REBUILD', 'DO_NOT_REBUILD'], '涉及数据库结构、初始化数据、空库启动或数据库验收', '证明目标数据库可从受管输入形成预期状态。'],
  ['M03', 'BRD', ['ENABLE', 'DISABLE'], '业务目标/范围/参与者责任/业务规则/业务验收变化', '固化业务承诺和验收边界。'],
  ['M04', 'SRS', ['ENABLE', 'DISABLE'], '用户或外部系统可观察行为/状态/输入输出/失败语义变化', '固化可观察系统行为。'],
  ['M05', 'TDD', ['ENABLE', 'DISABLE'], '接口/数据/安全/架构/兼容性/关键技术取舍', '保存关键技术决定、边界和恢复依据。'],
  ['M06', '实施计划', ['ENABLE', 'DISABLE'], '实施顺序/多人协作/跨模块依赖/复杂步骤', '协调任务依赖、责任和完成顺序。'],
  ['M07', '治理决策记录', ['ENABLE', 'DISABLE'], '长期PR/CI/权限/审批责任/仓库治理语义变化', '保存治理意图、适用条件和责任边界。'],
  ['M08', '能力说明', ['ENABLE', 'DISABLE'], '公开能力/配置/接入/使用方式变化', '让能力消费者获得与实现一致的使用、配置和接入事实。'],
  ['M09', '静态验证', ['ENABLE', 'DISABLE'], '目标可由结构/格式/配置/代码规则/编译证明', '以最低成本证明可静态观察的约束。'],
  ['M10', '单元测试', ['ENABLE', 'DISABLE'], '独立规则/算法/状态转换/函数行为变化', '隔离验证局部决策和边界分支。'],
  ['M11', '集成测试', ['ENABLE', 'DISABLE'], '结果依赖模块/数据库/框架装配/组件协作', '证明真实协作、装配和持久化边界。'],
  ['M12', 'API 验证', ['ENABLE', 'DISABLE'], 'API或服务入口行为变化', '从服务入口证明契约、权限、数据和失败行为。'],
  ['M13', 'UI 验证', ['ENABLE', 'DISABLE'], '用户界面/交互变化', '从用户入口证明可见结果、交互和浏览器运行态。'],
  ['M14', '专家复核', ['ENABLE', 'DISABLE'], '专业不确定性/高影响决定/治理系统修改自身', '使用独立相关视角发现知识盲区和自证偏差。'],
  ['M15', '外部状态回读', ['ENABLE', 'DISABLE'], '目标涉及GitHub/Gitea等外部平台配置或运行状态', '证明真实外部状态已生效并发现配置漂移。'],
  ['M16', '人工验收', ['ENABLE', 'DISABLE'], '自动化不足以证明实际目标', '由责任人确认自动化无法覆盖的实际结果。'],
];

assert(fixture.schemaVersion === 1, 'unsupported fixture schemaVersion');
assert(fixture.skill === 'mango-design-delivery-assurance', 'fixture skill must be mango-design-delivery-assurance');
assert(Array.isArray(fixture.cases), 'cases must be an array');
assert(fixture.cases.length === 100, `expected exactly 100 cases, got ${fixture.cases.length}`);
assert(contract.releaseIncluded === false, 'delivery assurance contract must exclude release');
assert(contract.measures.length === 16, `expected 16 catalog measures, got ${contract.measures.length}`);

const catalog = new Map(contract.measures.map(measure => [measure.id, measure]));
const catalogIds = [...catalog.keys()];
assert(catalogIds.join(',') === 'M01,M02,M03,M04,M05,M06,M07,M08,M09,M10,M11,M12,M13,M14,M15,M16', 'catalog IDs must remain fixed M01-M16');
for (const [id, name, allowedValues, triggerFact, purpose] of FIXED_CATALOG) {
  const measure = catalog.get(id);
  assert(measure.name === name, `${id} fixed name drifted: expected ${name}, got ${measure.name}`);
  assert(measure.allowedValues.join(',') === allowedValues.join(','), `${id} allowed values drifted`);
  assert(measure.triggerFact === triggerFact, `${id} trigger fact drifted`);
  assert((measure.purpose ?? measure.value) === purpose, `${id} purpose drifted`);
  assert(ruleText.includes(`| ${id} | ${name} |`), `${id} name is not projected into the rule table`);
  assert(ruleText.includes(triggerFact), `${id} trigger fact is not projected into the rule`);
  assert(ruleText.includes(purpose.replace(/。$/u, '')), `${id} purpose is not projected into the rule`);
}

const ids = new Set();
const measureCoverage = new Map(catalogIds.map(id => [id, 0]));
const valueCoverage = new Map(catalogIds.map(id => [id, new Set()]));
const kindCounts = new Map();
let positiveWithoutWorktree = 0;
let releaseCases = 0;
let nonTriggerCases = 0;

for (const item of fixture.cases) {
  assert(typeof item.id === 'string' && /^da-rec-\d{3}$/u.test(item.id), 'case id must use da-rec-NNN');
  assert(!ids.has(item.id), `duplicate case id: ${item.id}`);
  ids.add(item.id);
  assert(typeof item.prompt === 'string' && item.prompt.trim().length >= 12, `${item.id}: prompt is required`);
  assert(typeof item.expectedSkill === 'boolean', `${item.id}: expectedSkill must be boolean`);
  assert(Array.isArray(item.requiredMeasures), `${item.id}: requiredMeasures must be an array`);
  assert(Array.isArray(item.forbiddenMeasures), `${item.id}: forbiddenMeasures must be an array`);
  assert(typeof item.reason === 'string' && item.reason.trim().length >= 8, `${item.id}: reason is required`);
  assert(['single', 'combination', 'boundary', 'non-trigger'].includes(item.kind), `${item.id}: unsupported kind ${item.kind}`);
  assert(Array.isArray(item.tags) && item.tags.length > 0, `${item.id}: tags must be a non-empty array`);
  kindCounts.set(item.kind, (kindCounts.get(item.kind) ?? 0) + 1);

  const requiredIds = new Set();
  for (const expected of item.requiredMeasures) {
    assert(expected && typeof expected === 'object' && !Array.isArray(expected), `${item.id}: required measure must be an object`);
    assert(catalog.has(expected.id), `${item.id}: unknown required measure ${expected.id}`);
    assert(!requiredIds.has(expected.id), `${item.id}: duplicate required measure ${expected.id}`);
    requiredIds.add(expected.id);
    assert(catalog.get(expected.id).allowedValues.includes(expected.value), `${item.id}: invalid recommended value ${expected.value} for ${expected.id}`);
    measureCoverage.set(expected.id, measureCoverage.get(expected.id) + 1);
    valueCoverage.get(expected.id).add(expected.value);
  }

  const forbiddenIds = new Set();
  for (const id of item.forbiddenMeasures) {
    assert(catalog.has(id), `${item.id}: unknown forbidden measure ${id}`);
    assert(!forbiddenIds.has(id), `${item.id}: duplicate forbidden measure ${id}`);
    forbiddenIds.add(id);
    assert(!requiredIds.has(id), `${item.id}: ${id} cannot be both required and forbidden`);
  }
  const exactForbidden = catalogIds.filter(id => !requiredIds.has(id));
  assert(
    exactForbidden.every(id => forbiddenIds.has(id)) && forbiddenIds.size === exactForbidden.length,
    `${item.id}: forbiddenMeasures must be the exact complement of requiredMeasures`,
  );

  if (item.expectedSkill) {
    assert(item.requiredMeasures.length > 0, `${item.id}: triggered Skill must recommend at least one measure`);
    if (!requiredIds.has('M01')) positiveWithoutWorktree += 1;
  } else {
    nonTriggerCases += 1;
    assert(item.requiredMeasures.length === 0, `${item.id}: non-trigger case cannot recommend measures`);
  }

  if (item.kind === 'single') assert(item.requiredMeasures.length === 1, `${item.id}: single case must recommend exactly one measure`);
  if (item.kind === 'combination') assert(item.requiredMeasures.length >= 2, `${item.id}: combination case must recommend at least two measures`);

  if (item.tags.includes('release')) {
    releaseCases += 1;
    assert(item.expectedSkill === false, `${item.id}: release case must not trigger delivery assurance`);
    assert(item.requiredMeasures.length === 0, `${item.id}: release case cannot recommend delivery measures`);
    assert(catalogIds.every(id => forbiddenIds.has(id)), `${item.id}: release case must forbid all delivery-assurance measures`);
  }
  if (item.tags.includes('read-only') || item.tags.includes('status')) {
    assert(!requiredIds.has('M01'), `${item.id}: read-only/status case must not recommend M01`);
  }
}

for (const [id, count] of measureCoverage) assert(count > 0, `missing recommendation coverage for ${id}`);
assert(valueCoverage.get('M01').has('CREATE') && valueCoverage.get('M01').has('DO_NOT_CREATE'), 'M01 cases must cover both allowed values');
assert(valueCoverage.get('M02').has('REBUILD') && valueCoverage.get('M02').has('DO_NOT_REBUILD'), 'M02 cases must cover both allowed values');
assert((kindCounts.get('single') ?? 0) >= 16, 'need at least 16 single-measure cases');
assert((kindCounts.get('combination') ?? 0) >= 40, 'need at least 40 combination cases');
assert((kindCounts.get('boundary') ?? 0) >= 15, 'need at least 15 boundary cases');
assert(nonTriggerCases >= 15, 'need at least 15 non-trigger cases');
assert(releaseCases >= 10, 'need at least 10 release non-trigger cases');
assert(positiveWithoutWorktree >= 20, 'recommendation set is mechanically adding M01; need at least 20 triggered cases without it');

const coverageText = catalogIds.map(id => `${id}=${measureCoverage.get(id)}`).join(', ');
process.stdout.write(`Delivery assurance recommendation cases PASS: ${fixture.cases.length} cases.\n`);
process.stdout.write(`Kinds: ${[...kindCounts].map(([kind, count]) => `${kind}=${count}`).join(', ')}.\n`);
process.stdout.write(`Coverage: ${coverageText}.\n`);
process.stdout.write(`Non-trigger=${nonTriggerCases}, release=${releaseCases}, triggered-without-M01=${positiveWithoutWorktree}.\n`);

const predictionsIndex = process.argv.indexOf('--predictions');
if (predictionsIndex >= 0) {
  const predictionsPath = process.argv[predictionsIndex + 1];
  assert(predictionsPath, '--predictions requires a JSON path');
  const predictionFile = JSON.parse(readFileSync(resolve(predictionsPath), 'utf8'));
  const predictions = Array.isArray(predictionFile) ? predictionFile : predictionFile.predictions;
  assert(Array.isArray(predictions), 'predictions must be an array or an object with predictions[]');
  const byId = new Map();
  for (const prediction of predictions) {
    assert(typeof prediction.id === 'string', 'prediction id is required');
    assert(!byId.has(prediction.id), `duplicate prediction: ${prediction.id}`);
    assert(typeof prediction.skillTriggered === 'boolean', `${prediction.id}: skillTriggered must be boolean`);
    assert(Array.isArray(prediction.measures), `${prediction.id}: measures must be an array`);
    byId.set(prediction.id, prediction);
  }
  assert(byId.size === fixture.cases.length, `expected ${fixture.cases.length} predictions, got ${byId.size}`);

  const failures = [];
  for (const item of fixture.cases) {
    const prediction = byId.get(item.id);
    if (!prediction) {
      failures.push(`${item.id}: missing prediction`);
      continue;
    }
    if (prediction.skillTriggered !== item.expectedSkill) {
      failures.push(`${item.id}: skillTriggered expected ${item.expectedSkill}, got ${prediction.skillTriggered}`);
    }
    const actual = new Map();
    for (const measure of prediction.measures) {
      if (!catalog.has(measure.id)) {
        failures.push(`${item.id}: unknown measure ${measure.id}`);
        continue;
      }
      if (actual.has(measure.id)) failures.push(`${item.id}: duplicate measure ${measure.id}`);
      if (!catalog.get(measure.id).allowedValues.includes(measure.value)) {
        failures.push(`${item.id}: invalid value ${measure.value} for ${measure.id}`);
      }
      actual.set(measure.id, measure.value);
    }
    const expected = new Map(item.requiredMeasures.map(measure => [measure.id, measure.value]));
    for (const [id, value] of expected) {
      if (actual.get(id) !== value) failures.push(`${item.id}: expected ${id}=${value}, got ${actual.get(id) ?? '<missing>'}`);
    }
    for (const [id, value] of actual) {
      if (!expected.has(id)) failures.push(`${item.id}: unexpected ${id}=${value}`);
    }
  }
  if (failures.length > 0) {
    for (const failure of failures) process.stderr.write(`[FAIL] ${failure}\n`);
    throw new Error(`recommendation predictions failed: ${failures.length} mismatches`);
  }
  process.stdout.write(`AI recommendation predictions PASS: ${fixture.cases.length}/${fixture.cases.length} exact matches.\n`);
}

function assert(condition, message) {
  if (!condition) throw new Error(message);
}
