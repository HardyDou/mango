import assert from 'node:assert/strict';
import test from 'node:test';

import { validateRiskVerification } from '../tools/risk-verification.mjs';

function body(fields = {}) {
  return `## Risk / Verification

- Requirement impact: ${fields.requirement ?? 'L0 - only the existing button position changes; behavior and public contracts stay unchanged'}
- Solution risk: ${fields.solution ?? 'L0 - one local layout rule changes and reverting that rule restores the prior state'}
- Final risk: ${fields.finalRisk ?? 'L0'}
- Selected verification: ${fields.selected ?? 'STATIC, UI'}
- Why sufficient: ${fields.sufficient ?? 'diff review proves no behavior changed and a targeted screenshot proves the requested placement'}
- Skipped verification: ${fields.skipped ?? 'UNIT - no logic changed; API - no API or server behavior changed'}
`;
}

test('button position change accepts lightweight static and UI evidence', () => {
  const result = validateRiskVerification(body());
  assert.deepEqual(result.failures, []);
  assert.equal(result.assessment.finalRisk, 'L0');
  assert.deepEqual(result.assessment.selectedVerification, ['STATIC', 'UI']);
});

test('final risk is the maximum of requirement impact and solution risk', () => {
  const result = validateRiskVerification(body({
    requirement: 'L1 - behavior stays local to one internal page',
    solution: 'L2 - the chosen implementation changes a shared persisted state and rollback needs a data repair',
    finalRisk: 'L1',
  }));
  assert.match(result.failures.join('\n'), /expected L2, got L1/);
});

test('backend L3 can omit UI when API proves the outcome', () => {
  const result = validateRiskVerification(body({
    requirement: 'L3 - tenant isolation failure can expose records across customers',
    solution: 'L3 - authorization and transaction boundaries change across backend modules',
    finalRisk: 'L3',
    selected: 'STATIC, UNIT, API',
    sufficient: 'static checks cover architecture, unit tests cover the rule, and API tests exercise the real tenant and transaction entry',
    skipped: 'UI - there is no browser entry and the API is the real user-facing boundary',
  }));
  assert.deepEqual(result.failures, []);
});

test('every unselected verification type needs a concrete reason', () => {
  const result = validateRiskVerification(body({ skipped: 'UNIT - no logic changed' }));
  assert.match(result.failures.join('\n'), /why API is not needed/);
});

test('placeholder evidence and unsupported verification types fail', () => {
  const result = validateRiskVerification(body({
    requirement: 'L0 - TODO',
    selected: 'STATIC, E2E',
  }));
  assert.match(result.failures.join('\n'), /Requirement impact/);
  assert.match(result.failures.join('\n'), /unsupported verification type: E2E/);
});
