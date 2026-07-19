const EXPECTED_STAGES = ['pilot', 'affected', 'repository'];

export function validateAdoptionContract(contract, context) {
  const failures = [];
  if (contract.schemaVersion !== 1) failures.push('schemaVersion must be 1');
  if (!contract.owner) failures.push('owner is required');
  if (contract.approvalMode !== 'single-owner') failures.push('approvalMode must be single-owner');
  if (contract.scope !== 'standards-adoption') failures.push('scope must be standards-adoption');

  const stages = contract.adoptionStages ?? [];
  if (
    stages.length !== EXPECTED_STAGES.length ||
    stages.some((stage, index) => stage.name !== EXPECTED_STAGES[index])
  ) {
    failures.push('adoption stages must be exactly pilot, affected, repository');
  }
  for (const stage of stages) {
    const checks = stage.requiredChecks ?? [];
    if (checks.length === 0 || new Set(checks).size !== checks.length) {
      failures.push(`adoption stage '${stage.name || '<missing>'}' requires unique non-empty checks`);
    }
  }

  const enforcement = contract.enforcement ?? {};
  if (enforcement.newAndChangedCode !== 'blocking') {
    failures.push('enforcement.newAndChangedCode must be blocking');
  }
  if (enforcement.historicalDebt !== 'exact-identities-only-decrease') {
    failures.push('enforcement.historicalDebt must be exact-identities-only-decrease');
  }

  const candidateNames = Object.keys(context.candidateVersions).sort();
  const localNames = Object.keys(context.localVersions).sort();
  if (JSON.stringify(candidateNames) !== JSON.stringify(localNames)) {
    failures.push('candidate lock must cover the complete local public package set');
  }
  const recoveryNames = Object.keys(contract.dependencyRecovery ?? {}).sort();
  if (JSON.stringify(candidateNames) !== JSON.stringify(recoveryNames)) {
    failures.push('dependencyRecovery must cover the complete npm candidate lock');
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
    const recovery = contract.dependencyRecovery?.[name];
    const baseVersion = context.baseVersions[name] ?? null;
    if (!recovery) continue;
    if (baseVersion === null) {
      if (recovery.version !== null || recovery.action !== 'remove') {
        failures.push(`${name}: a new package must recover by removal`);
      }
    } else if (candidateDiffersFromBase && (recovery.version !== baseVersion || recovery.action !== 'pin')) {
      failures.push(`${name}: recovery must pin ${baseVersion}`);
    }
  }

  return {
    failures,
    mode: candidateDiffersFromBase ? 'candidate' : 'stable',
    packageCount: candidateNames.length,
    stageCount: stages.length,
  };
}
