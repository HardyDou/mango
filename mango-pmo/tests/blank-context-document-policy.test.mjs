import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

import { resolveLeanDocumentPolicy } from '../tools/resolve-lean-document-policy.mjs';

const fixture = JSON.parse(fs.readFileSync(new URL('./skills/blank-context-document-policy-cases.json', import.meta.url), 'utf8'));

test('空白上下文场景清单满足数量、等级和 ASK 覆盖要求', () => {
  assert.equal(fixture.schemaVersion, 1);
  assert.ok(fixture.cases.length >= 30);
  const ids = new Set();
  const coveredLevels = new Set();
  const actions = new Set();
  for (const item of fixture.cases) {
    assert.match(item.id, /^lean-blank-\d{3}$/u);
    assert.equal(ids.has(item.id), false, `duplicate ${item.id}`);
    ids.add(item.id);
    assert.ok(item.prompt.length >= 6, `${item.id}: prompt too short`);
    if (item.expect.finalLevel) coveredLevels.add(item.expect.finalLevel);
    actions.add(item.expect.action);
  }
  assert.deepEqual([...coveredLevels].sort(), ['L0', 'L1', 'L2', 'L3', 'L4', 'L5']);
  assert.deepEqual([...actions].sort(), ['ASK', 'DIRECT', 'STOP', 'WRITE']);
  assert.ok(fixture.cases.filter(item => item.expect.action === 'ASK').length >= 6);
  assert.ok(fixture.cases.filter(item => item.expect.finalLevel === 'L5').length >= 8);
});

for (const item of fixture.cases) {
  test(`${item.id} ${item.prompt}`, () => {
    const result = resolveLeanDocumentPolicy(item.facts);
    for (const [key, expected] of Object.entries(item.expect)) {
      if (key === 'finding') {
        assert.match(result.findings.join('\n'), new RegExp(expected), JSON.stringify(result));
      } else {
        assert.deepEqual(result[key], expected, `${key}: ${JSON.stringify(result)}`);
      }
    }
  });
}
