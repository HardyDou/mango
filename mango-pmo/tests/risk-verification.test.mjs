import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

import {
  validateRiskVerification,
  validateRiskVerificationTemplate,
} from '../tools/risk-verification.mjs';
import { assuranceCiScope } from '../tools/assurance-ci-scope.mjs';

const canonicalTemplate = readFileSync(
  new URL('../templates/business-pull-request-template.md', import.meta.url),
  'utf8',
);

function body(fields = {}) {
  return `## Risk / Verification

- Requirement impact: ${fields.requirement ?? 'L0 - only the existing button position changes; behavior and public contracts stay unchanged'}
- Solution risk: ${fields.solution ?? 'L0 - one local layout rule changes and reverting that rule restores the prior state'}
- Final risk: ${fields.finalRisk ?? 'L0'}
- Delivery mode: ${fields.deliveryMode ?? 'SIMPLE'}
- Workspace decision: ${fields.workspaceDecision ?? 'CREATE'}
- Non-downgradable facts: ${fields.nonDowngradableFacts ?? 'None'}
- Assurance baseline: ${fields.baseline ?? 'RESOLVED - repository policy and task facts task-123'}
- Assurance selections: ${fields.selections ?? 'M01=CREATE; M09=ENABLE; M13=ENABLE'}
- Assurance reasoning: ${fields.reasoning ?? 'the confirmed static and UI measures directly observe the requested position without adding unrelated tests'}
- Assurance evidence: ${fields.evidence ?? 'M01 - branch task/button-position; M09 - style checker passed; M13 - targeted viewport screenshot'}
- Residual risks: ${fields.residualRisks ?? 'None'}
`;
}

test('policy-resolved delivery mode and assurance measures pass', () => {
  const result = validateRiskVerification(body());
  assert.deepEqual(result.failures, []);
  assert.equal(result.assessment.finalRisk, 'L0');
  assert.equal(result.assessment.deliveryMode, 'SIMPLE');
  assert.deepEqual(result.assessment.assuranceSelections, {
    M01: 'CREATE',
    M09: 'ENABLE',
    M13: 'ENABLE',
  });
});

test('canonical business PR template matches the delivery-assurance contract', () => {
  assert.deepEqual(validateRiskVerificationTemplate(canonicalTemplate).failures, []);
});

test('canonical business PR template accepts CRLF line endings', () => {
  assert.deepEqual(validateRiskVerificationTemplate(canonicalTemplate.replace(/\n/gu, '\r\n')).failures, []);
});

test('template validation rejects legacy fields and missing current fields', () => {
  const legacyTemplate = `## Risk / Verification

- Requirement impact: L0-L5 - facts
- Solution risk: L0-L5 - facts
- Final risk: L0-L5
- Selected verification: STATIC, UNIT, API, UI
- Why sufficient:
- Skipped verification: None
`;
  const failures = validateRiskVerificationTemplate(legacyTemplate).failures.join('\n');
  assert.match(failures, /legacy field.*Selected verification/);
  assert.match(failures, /missing field.*Delivery mode/);
  assert.match(failures, /missing field.*Assurance selections/);
});

test('template validation rejects duplicate sections and reordered fields', () => {
  const duplicate = `${canonicalTemplate}\n${canonicalTemplate}`;
  assert.match(validateRiskVerificationTemplate(duplicate).failures.join('\n'), /exactly one section/);

  const reordered = canonicalTemplate
    .replace(
      /- Requirement impact:([^\n]*)\n- Solution risk:([^\n]*)/u,
      '- Solution risk:$2\n- Requirement impact:$1',
    );
  assert.match(validateRiskVerificationTemplate(reordered).failures.join('\n'), /field order/);
});

test('final risk remains the maximum of requirement impact and solution risk', () => {
  const result = validateRiskVerification(body({
    requirement: 'L1 - behavior stays local to one internal page',
    solution: 'L2 - the chosen implementation changes a shared persisted state and rollback needs a data repair',
    finalRisk: 'L1',
    deliveryMode: 'SIMPLE',
  }));
  assert.match(result.failures.join('\n'), /expected L2, got L1/);
});

test('main exception and disabled values require explicit residual risk', () => {
  const result = validateRiskVerification(body({
    workspaceDecision: 'MAIN_EXCEPTION',
    baseline: 'RESOLVED - MAIN_EXCEPTION_CONFIRMED by repository owner for this task',
    selections: 'M01=MAIN_EXCEPTION; M10=DISABLE; M12=ENABLE',
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

test('invalid values and unresolved baseline fail', () => {
  const result = validateRiskVerification(body({
    baseline: 'PENDING - automatic choice',
    selections: 'M01=CREATE; M02=KEEP_DATABASE; M17=ENABLE',
  }));
  assert.match(result.failures.join('\n'), /Assurance baseline/);
  assert.match(result.failures.join('\n'), /Assurance selections/);
});

test('a human CONFIRMED baseline remains valid during the trusted-base contract migration', () => {
  const result = validateRiskVerification(body({
    workspaceDecision: 'CREATE',
    baseline: 'CONFIRMED - repository owner approved the recorded assurance set',
    selections: 'M01=CREATE; M09=ENABLE; M10=ENABLE',
    evidence: 'M01 - isolated task worktree; M09 - static checks passed; M10 - contract tests passed',
  }));
  assert.deepEqual(result.failures, []);
});


test('trusted-base schema migration temporarily accepts legacy FULL as L3', () => {
  const result = validateRiskVerification(body({
    requirement: 'L3 - repository-wide governance contract changes',
    solution: 'L3 - trusted contract and packaged projections change together',
    finalRisk: 'L3',
    deliveryMode: 'FULL',
    baseline: 'RESOLVED - trusted-base schema migration from the current main branch',
  }));
  assert.deepEqual(result.failures, []);
  assert.equal(result.assessment.deliveryMode, 'FULL');
  assert.equal(result.assessment.effectiveDeliveryMode, 'L3');
});

test('legacy delivery modes remain blocked outside the exact trusted-base migration', () => {
  const missingMarker = validateRiskVerification(body({
    requirement: 'L3 - repository-wide governance contract changes',
    solution: 'L3 - packaged projections change together',
    finalRisk: 'L3',
    deliveryMode: 'FULL',
  }));
  assert.match(missingMarker.failures.join('\n'), /trusted-base schema migration/);

  const wrongRisk = validateRiskVerification(body({
    requirement: 'L4 - cross-service governance contract changes',
    solution: 'L4 - packaged projections change together',
    finalRisk: 'L4',
    deliveryMode: 'FULL',
    baseline: 'RESOLVED - trusted-base schema migration from the current main branch',
  }));
  assert.match(wrongRisk.failures.join('\n'), /trusted-base schema migration/);

  const standard = validateRiskVerification(body({
    requirement: 'L2 - bounded governance contract change',
    solution: 'L2 - local checker change',
    finalRisk: 'L2',
    deliveryMode: 'STANDARD',
    baseline: 'RESOLVED - trusted-base schema migration from the current main branch',
  }));
  assert.match(standard.failures.join('\n'), /trusted-base schema migration/);
});

test('release-only PR bypasses the delivery catalog and stays in the release workflow', () => {
  const result = validateRiskVerification(body({
    deliveryMode: 'NOT_APPLICABLE',
    baseline: 'NOT_APPLICABLE - release-only task routed to mango-release',
    selections: 'None',
    reasoning: 'release authorization and recovery are governed independently',
    evidence: 'None',
  }));
  assert.deepEqual(result.failures, []);
  assert.deepEqual(result.assessment.assuranceSelections, {});
});

test('CI scope follows exact resolved capabilities instead of changed paths', () => {
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
  const result = assuranceCiScope(body({ baseline: 'CONFIRMED - old questionnaire' }));
  assert.match(result.failures.join('\n'), /Assurance baseline/);
  assert.equal(result.outputs, null);
});

test('risk level must map to the declared delivery mode', () => {
  const result = validateRiskVerification(body({
    requirement: 'L2 - public API behavior changes',
    solution: 'L2 - reversible shared service update',
    finalRisk: 'L2',
    deliveryMode: 'SIMPLE',
  }));
  assert.match(result.failures.join('\n'), /minimum L2, got SIMPLE/);
});

test('a higher delivery mode is allowed without weakening the risk baseline', () => {
  const result = validateRiskVerification(body({
    requirement: 'L1 - local behavior change',
    solution: 'L1 - reversible implementation',
    finalRisk: 'L1',
    deliveryMode: 'L5',
  }));
  assert.deepEqual(result.failures, []);
});

test('a lower delivery mode requires explicit exception evidence and residual risk', () => {
  const result = validateRiskVerification(body({
    requirement: 'L2 - shared behavior changes',
    solution: 'L2 - reversible shared implementation',
    finalRisk: 'L2',
    deliveryMode: 'SIMPLE',
    baseline: 'RESOLVED - MODE_DOWNGRADE_CONFIRMED by task owner for bounded rollout',
    residualRisks: '交付模式降级后不建立标准交付记录，由定向验证承担剩余追踪风险',
  }));
  assert.deepEqual(result.failures, []);
});

test('security tenant and authorization facts cannot be downgraded', () => {
  const result = validateRiskVerification(body({
    requirement: 'L3 - cross-tenant authorization and security boundary changes',
    solution: 'L3 - tenant permission enforcement changes across services',
    finalRisk: 'L3',
    deliveryMode: 'SIMPLE',
    nonDowngradableFacts: 'SECURITY, TENANT, AUTHORIZATION',
    baseline: 'RESOLVED - MODE_DOWNGRADE_CONFIRMED by repository owner',
    residualRisks: '交付模式降级风险已由负责人接受',
  }));
  assert.match(result.failures.join('\n'), /cannot be downgraded.*SECURITY.*TENANT.*AUTHORIZATION/);
});

test('detected non-downgradable facts must be declared', () => {
  const result = validateRiskVerification(body({
    requirement: 'L3 - cross-tenant access changes',
    solution: 'L3 - tenant isolation implementation',
    finalRisk: 'L3',
    deliveryMode: 'L3',
    nonDowngradableFacts: 'None',
  }));
  assert.match(result.failures.join('\n'), /omits detected facts: TENANT/);
});

test('AI cannot provide main-worktree exception confirmation', () => {
  const result = validateRiskVerification(body({
    workspaceDecision: 'MAIN_EXCEPTION',
    baseline: 'RESOLVED - MAIN_EXCEPTION_CONFIRMED by Codex Agent',
    selections: 'M01=MAIN_EXCEPTION; M09=ENABLE',
    evidence: 'M09 - static checker passed',
    residualRisks: 'M01 - main workspace pollution risk',
  }));
  assert.match(result.failures.join('\n'), /requires human evidence/);
});
