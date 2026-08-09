#!/usr/bin/env node
import { readFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const testRoot = dirname(fileURLToPath(import.meta.url));
const pmoRoot = resolve(testRoot, '../..');
const fixture = JSON.parse(readFileSync(join(testRoot, 'delivery-assurance-recommendation-cases.json'), 'utf8'));
const contract = JSON.parse(readFileSync(join(pmoRoot, 'contracts/delivery-assurance.json'), 'utf8'));

assert(contract.contractSchema === 2, 'delivery assurance contract schema must be 2');
assert(contract.schemaRevision === 5, 'delivery assurance schema revision must be 5');
assert(contract.selectionAuthority === 'policy-with-human-exceptions', 'selection authority drifted');
assert(contract.workspacePolicy.trackedChangeOnPrimary === 'CREATE', 'primary tracked changes must CREATE');
assert(contract.workspacePolicy.existingTaskWorktree === 'REUSE', 'existing task worktree must REUSE');
assert(contract.workspacePolicy.mainException === 'MAIN_EXCEPTION', 'main exception value drifted');
assert(contract.workspacePolicy.defaultHumanConfirmationRequired === false, 'default M01 confirmation must stay disabled');
assert(JSON.stringify(contract.riskToMode) === JSON.stringify({ L0: 'SIMPLE', L1: 'SIMPLE', L2: 'STANDARD', L3: 'FULL' }), 'risk-to-mode mapping drifted');
assert(contract.deliveryModes.map(mode => mode.id).join(',') === 'SIMPLE,STANDARD,FULL', 'delivery mode catalog drifted');
assert(contract.deliveryModes.find(mode => mode.id === 'STANDARD')?.artifactPolicy === 'SINGLE_DELIVERY_RECORD', 'STANDARD must use one record');

const catalog = new Map(contract.measures.map(measure => [measure.id, measure]));
const catalogIds = [...catalog.keys()];
assert(catalogIds.join(',') === 'M01,M02,M03,M04,M05,M06,M07,M08,M09,M10,M11,M12,M13,M14,M15,M16', 'capability IDs drifted');
assert(catalog.get('M01').allowedValues.includes('REUSE'), 'M01 must support REUSE');
assert(catalog.get('M01').allowedValues.includes('MAIN_EXCEPTION'), 'M01 must support MAIN_EXCEPTION');
for (const measure of catalog.values()) {
  assert(typeof measure.name === 'string' && measure.name.length > 0, `${measure.id}: name is required`);
  assert(typeof measure.triggerFact === 'string' && measure.triggerFact.length > 0, `${measure.id}: triggerFact is required`);
  assert(Array.isArray(measure.allowedValues) && measure.allowedValues.length >= 2, `${measure.id}: allowedValues are required`);
}

assert(fixture.schemaVersion === 1, 'unsupported fixture schemaVersion');
assert(Array.isArray(fixture.cases) && fixture.cases.length === 100, 'expected 100 assurance capability cases');
const ids = new Set();
let nonTrigger = 0;
for (const item of fixture.cases) {
  assert(/^da-rec-\d{3}$/u.test(item.id), `invalid case id ${item.id}`);
  assert(!ids.has(item.id), `duplicate case ${item.id}`);
  ids.add(item.id);
  assert(Array.isArray(item.requiredMeasures), `${item.id}: requiredMeasures must be an array`);
  assert(Array.isArray(item.forbiddenMeasures), `${item.id}: forbiddenMeasures must be an array`);
  const required = new Set();
  for (const expected of item.requiredMeasures) {
    const measure = catalog.get(expected.id);
    assert(measure, `${item.id}: unknown measure ${expected.id}`);
    assert(measure.allowedValues.includes(expected.value), `${item.id}: invalid ${expected.id} value ${expected.value}`);
    assert(!required.has(expected.id), `${item.id}: duplicate ${expected.id}`);
    required.add(expected.id);
  }
  const exactForbidden = catalogIds.filter(id => !required.has(id));
  assert(item.forbiddenMeasures.length === exactForbidden.length && exactForbidden.every(id => item.forbiddenMeasures.includes(id)), `${item.id}: forbidden measures must be exact complement`);
  if (!item.expectedSkill) {
    nonTrigger += 1;
    assert(required.size === 0, `${item.id}: non-trigger cannot activate capabilities`);
  }
}
assert(nonTrigger >= 15, 'need at least 15 non-trigger cases');

process.stdout.write(`Delivery assurance contract PASS: schema=2, modes=SIMPLE/STANDARD/FULL, cases=${fixture.cases.length}.\n`);

function assert(condition, message) {
  if (!condition) throw new Error(message);
}
