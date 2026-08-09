#!/usr/bin/env node
import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = dirname(fileURLToPath(import.meta.url));
const fixture = JSON.parse(readFileSync(join(root, 'delivery-mode-history-cases.json'), 'utf8'));
const modeByRisk = { L0: 'SIMPLE', L1: 'SIMPLE', L2: 'STANDARD', L3: 'FULL' };
const artifactByMode = { SIMPLE: 'NONE', STANDARD: 'SINGLE_DELIVERY_RECORD', FULL: 'APPLICABLE_FULL_LIFECYCLE' };

assert(fixture.schemaVersion === 1, 'unsupported schema version');
assert(fixture.source === 'paraphrased-and-sanitized-codex-user-history', 'history source declaration is required');
assert(Array.isArray(fixture.cases) && fixture.cases.length >= 30, 'at least 30 historical cases are required');

const ids = new Set();
const counts = new Map([['NONE', 0], ['SIMPLE', 0], ['STANDARD', 0], ['FULL', 0]]);
for (const item of fixture.cases) {
  assert(/^dm-hist-\d{3}$/u.test(item.id), `${item.id}: invalid id`);
  assert(!ids.has(item.id), `${item.id}: duplicate id`);
  ids.add(item.id);
  assert(typeof item.sourcePattern === 'string' && item.sourcePattern.length > 0, `${item.id}: sourcePattern required`);
  assert(typeof item.prompt === 'string' && item.prompt.length >= 10, `${item.id}: prompt too short`);
  assert(counts.has(item.expectedMode), `${item.id}: invalid mode`);
  counts.set(item.expectedMode, counts.get(item.expectedMode) + 1);
  assert(['NO_WORKTREE', 'CREATE', 'REUSE', 'MAIN_EXCEPTION'].includes(item.workspace), `${item.id}: invalid workspace decision`);
  assert(Array.isArray(item.tags) && item.tags.length > 0, `${item.id}: tags required`);
  if (item.expectedMode === 'NONE') {
    assert(item.finalRisk === null && item.artifact === 'NONE' && item.workspace === 'NO_WORKTREE', `${item.id}: read-only case must have no delivery flow`);
  } else {
    assert(modeByRisk[item.finalRisk] === item.expectedMode, `${item.id}: risk-to-mode mismatch`);
    assert(artifactByMode[item.expectedMode] === item.artifact, `${item.id}: artifact policy mismatch`);
  }
  assert(!/(?:token|password|secret|127\.0\.0\.1|\/Users\/|https?:\/\/)/iu.test(item.prompt), `${item.id}: prompt is not sufficiently sanitized`);
}

const deliveryCount = fixture.cases.length - counts.get('NONE');
assert(counts.get('SIMPLE') / deliveryCount >= 0.5, 'SIMPLE must represent the majority of delivery history');
assert(counts.get('STANDARD') >= 8, 'STANDARD needs at least 8 cases');
assert(counts.get('FULL') >= 4 && counts.get('FULL') / deliveryCount <= 0.2, 'FULL must be present but remain a minority');
assert(fixture.cases.some(item => item.workspace === 'MAIN_EXCEPTION'), 'main exception boundary is missing');
assert(fixture.cases.some(item => item.tags.includes('simple-to-standard')), 'simple-to-standard escalation is missing');
assert(fixture.cases.some(item => item.tags.includes('standard-to-full')), 'standard-to-full escalation is missing');

process.stdout.write(`Delivery mode history cases PASS: total=${fixture.cases.length}, SIMPLE=${counts.get('SIMPLE')}, STANDARD=${counts.get('STANDARD')}, FULL=${counts.get('FULL')}, read-only=${counts.get('NONE')}.\n`);

function assert(condition, message) {
  if (!condition) throw new Error(message);
}
