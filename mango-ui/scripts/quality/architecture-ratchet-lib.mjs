function edgeIdentity(edge) {
  return `${edge.from}->${edge.to}:${edge.kind}`;
}

function wildcardSourceExports(manifest) {
  return Object.entries(manifest.mangoArchitecture?.sourceExports || {}).filter(([, config]) =>
    (config.sourcePattern || config.source || '').includes('*'),
  );
}

export function compareWildcardSourceExports(currentManifests, baseManifests) {
  const failures = [];
  const baseByName = new Map(baseManifests.map((manifest) => [manifest.name, manifest]));
  for (const manifest of currentManifests) {
    for (const [exportKey, config] of wildcardSourceExports(manifest)) {
      const identity = `${manifest.name}:${exportKey}`;
      const baseManifest = baseByName.get(manifest.name);
      if (!exportKey.includes('*') || !Object.hasOwn(baseManifest?.exports || {}, exportKey)) {
        failures.push(`new wildcard source export is not allowed: ${identity}`);
        continue;
      }
      const baseConfig = baseManifest.mangoArchitecture?.sourceExports?.[exportKey];
      if (!baseConfig) {
        continue;
      }
      if (config.sourcePattern !== baseConfig.sourcePattern) {
        failures.push(`wildcard source pattern changed: ${identity}`);
      }
      if (Date.parse(config.expiresAt) > Date.parse(baseConfig.expiresAt)) {
        failures.push(`wildcard source export expiry was extended: ${identity}`);
      }
    }
  }
  return failures;
}

export function compareArchitectureBaselines(current, base) {
  const failures = [];
  const baseExceptions = new Map((base.exceptions || []).map((item) => [`${item.from}->${item.to}`, item]));
  for (const exception of current.exceptions || []) {
    const identity = `${exception.from}->${exception.to}`;
    const baseException = baseExceptions.get(identity);
    if (!baseException) {
      failures.push(`new architecture exception is not allowed: ${identity}`);
      continue;
    }
    for (const field of ['reason', 'ownerRole', 'adr', 'decisionEvidence']) {
      if (exception[field] !== baseException[field]) {
        failures.push(`architecture exception ${field} changed: ${identity}`);
      }
    }
    if (Date.parse(exception.expiresAt) > Date.parse(baseException.expiresAt)) {
      failures.push(`architecture exception expiry was extended: ${identity}`);
    }
  }

  const baseSccs = new Map((base.legacySccs || []).map((item) => [item.id, item]));
  for (const currentScc of current.legacySccs || []) {
    const baseScc = baseSccs.get(currentScc.id);
    if (!baseScc) {
      failures.push(`new legacy SCC baseline is not allowed: ${currentScc.id}`);
      continue;
    }
    if (currentScc.graphKind !== baseScc.graphKind) {
      failures.push(`legacy SCC graph kind changed: ${currentScc.id}`);
    }
    for (const field of ['ownerRole', 'adr']) {
      if (currentScc[field] !== baseScc[field]) {
        failures.push(`legacy SCC ${field} changed: ${currentScc.id}`);
      }
    }
    if (Number(currentScc.targetPhase) > Number(baseScc.targetPhase)) {
      failures.push(`legacy SCC target phase was delayed: ${currentScc.id}`);
    }
    const baseMembers = new Set(baseScc.members || []);
    for (const member of currentScc.members || []) {
      if (!baseMembers.has(member)) {
        failures.push(`legacy SCC gained member: ${currentScc.id}:${member}`);
      }
    }
    const baseEdges = new Set((baseScc.edges || []).map(edgeIdentity));
    for (const edge of currentScc.edges || []) {
      const identity = edgeIdentity(edge);
      if (!baseEdges.has(identity)) {
        failures.push(`legacy SCC gained edge: ${currentScc.id}:${identity}`);
      }
    }
  }
  return failures;
}
