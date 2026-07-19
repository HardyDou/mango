const EXPECTED_STAGE_PERCENTAGES = [0, 5, 25, 100];

export function evaluateRolloutSample(thresholds, sample) {
  const breaches = [];
  for (const [signal, maximum] of Object.entries(thresholds)) {
    const observed = sample[signal];
    if (!Number.isFinite(observed)) {
      breaches.push({ signal, maximum, observed: null, reason: 'missing' });
    } else if (observed > maximum) {
      breaches.push({ signal, maximum, observed, reason: 'threshold' });
    }
  }
  return {
    decision: breaches.length === 0 ? 'promote' : 'rollback',
    breaches,
  };
}

export function validateRolloutContract(contract, context) {
  const failures = [];
  if (contract.schemaVersion !== 1) failures.push('schemaVersion must be 1');
  if (!contract.owner) failures.push('owner is required');
  if (contract.approvalMode !== 'single-owner') failures.push('approvalMode must be single-owner');
  if (contract.scope !== 'release-lock') failures.push('scope must be release-lock');

  const stages = contract.trafficStages ?? [];
  if (
    stages.length !== EXPECTED_STAGE_PERCENTAGES.length ||
    stages.some((stage, index) => stage.percent !== EXPECTED_STAGE_PERCENTAGES[index])
  ) {
    failures.push('traffic stages must be exactly 0%, 5%, 25%, 100%');
  }
  if (
    stages.some((stage) => !Number.isFinite(stage.minimumObservationMinutes) || stage.minimumObservationMinutes <= 0)
  ) {
    failures.push('every traffic stage requires a positive observation window');
  }

  const policy = contract.rollbackPolicy ?? {};
  for (const field of ['automaticOnAnyThresholdBreach', 'retainImmutableStableAssets', 'restoreStableRuntimeEntries']) {
    if (policy[field] !== true) failures.push(`rollbackPolicy.${field} must be true`);
  }
  if (policy.databaseRollbackRequired !== false || policy.apiCompatibility !== 'backward-compatible') {
    failures.push('frontend rollout must declare backward-compatible APIs and no database rollback');
  }

  const candidateNames = Object.keys(context.candidateVersions).sort();
  const localNames = Object.keys(context.localVersions).sort();
  if (JSON.stringify(candidateNames) !== JSON.stringify(localNames)) {
    failures.push('candidate lock must cover the complete local public package set');
  }
  const rollbackNames = Object.keys(contract.rollbackPackages ?? {}).sort();
  if (JSON.stringify(candidateNames) !== JSON.stringify(rollbackNames)) {
    failures.push('rollbackPackages must cover the complete npm candidate lock');
  }
  const candidateDiffersFromBase = candidateNames.some(
    (name) => context.baseVersions[name] !== context.candidateVersions[name],
  );
  for (const name of candidateNames) {
    const candidate = context.candidateVersions[name];
    if (context.localVersions[name] !== candidate) {
      failures.push(
        `${name}: candidate lock ${candidate} != local package ${context.localVersions[name] ?? '<missing>'}`,
      );
    }
    const rollback = contract.rollbackPackages?.[name];
    const baseVersion = context.baseVersions[name] ?? null;
    if (!rollback) continue;
    if (baseVersion === null) {
      if (rollback.version !== null || rollback.action !== 'remove') {
        failures.push(`${name}: a new package must roll back by removal`);
      }
    } else if (candidateDiffersFromBase && (rollback.version !== baseVersion || rollback.action !== 'pin')) {
      failures.push(`${name}: rollback must pin ${baseVersion}`);
    }
  }

  const healthy = evaluateRolloutSample(contract.healthThresholds ?? {}, contract.exerciseSamples?.healthy ?? {});
  const breach = evaluateRolloutSample(contract.healthThresholds ?? {}, contract.exerciseSamples?.breach ?? {});
  if (healthy.decision !== 'promote') failures.push('healthy exercise sample must promote');
  if (breach.decision !== 'rollback') failures.push('breach exercise sample must roll back');

  return {
    failures,
    mode: candidateDiffersFromBase ? 'candidate' : 'stable',
    packageCount: candidateNames.length,
    healthy,
    breach,
  };
}
