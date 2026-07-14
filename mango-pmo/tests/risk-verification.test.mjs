import assert from 'node:assert/strict';
import test from 'node:test';

import { validateRiskVerification } from '../tools/risk-verification.mjs';
import { assuranceCiScope } from '../tools/assurance-ci-scope.mjs';

function body(fields = {}) {
  return `## Risk / Verification

- Requirement impact: ${fields.requirement ?? 'L0 - only the existing button position changes; behavior and public contracts stay unchanged'}
- Solution risk: ${fields.solution ?? 'L0 - one local layout rule changes and reverting that rule restores the prior state'}
- Final risk: ${fields.finalRisk ?? 'L0'}
- Assurance baseline: ${fields.baseline ?? 'CONFIRMED - product owner native Ask User response task-123'}
- Assurance selections: ${fields.selections ?? 'M01=CREATE; M09=ENABLE; M13=ENABLE'}
- Assurance reasoning: ${fields.reasoning ?? 'the confirmed static and UI measures directly observe the requested position without adding unrelated tests'}
- Assurance evidence: ${fields.evidence ?? 'M01 - branch task/button-position; M09 - style checker passed; M13 - targeted viewport screenshot'}
- Residual risks: ${fields.residualRisks ?? 'None'}
`;
}

test('human-confirmed exact assurance measures pass without a fixed verification package', () => {
  const result = validateRiskVerification(body());
  assert.deepEqual(result.failures, []);
  assert.equal(result.assessment.finalRisk, 'L0');
  assert.deepEqual(result.assessment.assuranceSelections, {
    M01: 'CREATE',
    M09: 'ENABLE',
    M13: 'ENABLE',
  });
});

test('final risk remains the maximum of requirement impact and solution risk', () => {
  const result = validateRiskVerification(body({
    requirement: 'L1 - behavior stays local to one internal page',
    solution: 'L2 - the chosen implementation changes a shared persisted state and rollback needs a data repair',
    finalRisk: 'L1',
  }));
  assert.match(result.failures.join('\n'), /expected L2, got L1/);
});

test('confirmed disabled values require explicit residual risk instead of CI re-enabling them', () => {
  const result = validateRiskVerification(body({
    selections: 'M01=DO_NOT_CREATE; M10=DISABLE; M12=ENABLE',
    evidence: 'M12 - API normal and failure flow report',
    residualRisks: 'M01 - current main workspace carries the change; M10 - local rule branches lack isolated unit coverage',
  }));
  assert.deepEqual(result.failures, []);
});

test('enabled measures need evidence but unselected catalog measures need no skip explanation', () => {
  const result = validateRiskVerification(body({
    selections: 'M01=CREATE; M11=ENABLE',
    evidence: 'M01 - task worktree exists',
  }));
  assert.match(result.failures.join('\n'), /enabled M11/);
  assert.doesNotMatch(result.failures.join('\n'), /M03|M04|M10|M12|M13/);
});

test('invalid values and AI self-confirmation fail', () => {
  const result = validateRiskVerification(body({
    baseline: 'CONFIRMED - Codex confirmed automatically',
    selections: 'M01=CREATE; M02=KEEP_DATABASE; M17=ENABLE',
  }));
  assert.match(result.failures.join('\n'), /Assurance baseline/);
  assert.match(result.failures.join('\n'), /Assurance selections/);
});

test('release-only PR bypasses the delivery catalog and stays in the release workflow', () => {
  const result = validateRiskVerification(body({
    baseline: 'NOT_APPLICABLE - release-only task routed to mango-release',
    selections: 'None',
    reasoning: 'release authorization and recovery are governed independently',
    evidence: 'None',
  }));
  assert.deepEqual(result.failures, []);
  assert.deepEqual(result.assessment.assuranceSelections, {});
});

test('CI scope follows exact human-confirmed measures instead of changed paths', () => {
  const result = assuranceCiScope(body({
    selections: 'M07=ENABLE; M09=ENABLE; M10=DISABLE; M14=ENABLE',
    evidence: 'M07 - governance decision record; M09 - policy checker passed; M14 - PMO expert review',
    residualRisks: 'M10 - no isolated unit coverage was requested for the declarative rule change',
  }));
  assert.deepEqual(result.failures, []);
  assert.equal(result.outputs.m07, true);
  assert.equal(result.outputs.m09, true);
  assert.equal(result.outputs.m10, false);
  assert.equal(result.outputs.m14, true);
  assert.equal(result.outputs.m11, false);
});

test('CI scope rejects unconfirmed or invalid assurance baselines', () => {
  const result = assuranceCiScope(body({ baseline: 'CONFIRMED - AI decided the package' }));
  assert.match(result.failures.join('\n'), /Assurance baseline/);
  assert.equal(result.outputs, null);
});
