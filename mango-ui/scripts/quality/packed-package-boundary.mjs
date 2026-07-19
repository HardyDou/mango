function wildcardToRegExp(pattern) {
  const escaped = pattern.replace(/[.+?^${}()|[\]\\]/g, '\\$&');
  return new RegExp(`^${escaped.replaceAll('*', '(.+)')}$`, 'u');
}

function collectStringTargets(value, label, targets) {
  if (typeof value === 'string') {
    targets.push({ label, target: value });
    return;
  }
  if (Array.isArray(value)) {
    value.forEach((entry, index) => collectStringTargets(entry, `${label}[${index}]`, targets));
    return;
  }
  if (value && typeof value === 'object') {
    for (const [key, entry] of Object.entries(value)) {
      collectStringTargets(entry, `${label}.${key}`, targets);
    }
  }
}

export function packedExportTargets(packageJson) {
  const targets = [];
  for (const field of ['main', 'module', 'types', 'style']) {
    if (packageJson[field]) {
      targets.push({ label: field, target: packageJson[field] });
    }
  }
  collectStringTargets(packageJson.exports, 'exports', targets);
  return targets.filter(
    ({ target }, index, entries) =>
      entries.findIndex((entry) => entry.label === entries[index].label && entry.target === target) === index,
  );
}

function assertPublishedTarget(packageName, files, label, target) {
  if (!target.startsWith('./') || target.includes('../')) {
    throw new Error(`${packageName} ${label} must stay inside the published package: ${target}`);
  }
  const packedTarget = `package/${target.slice(2)}`;
  if (packedTarget.includes('*')) {
    const pattern = wildcardToRegExp(packedTarget);
    if (!files.some((file) => pattern.test(file))) {
      throw new Error(`${packageName} tarball is missing published export pattern: ${label} -> ${target}`);
    }
    return;
  }
  if (!files.includes(packedTarget)) {
    throw new Error(`${packageName} tarball is missing published export: ${label} -> ${target}`);
  }
}

function wildcardCaptures(files, target) {
  const pattern = wildcardToRegExp(`package/${target.slice(2)}`);
  return new Set(
    files.flatMap((file) => {
      const match = pattern.exec(file);
      return match ? [match.slice(1).join('\0')] : [];
    }),
  );
}

function assertWildcardConditionParity(packageName, files, exportKey, exportConfig) {
  if (!exportKey.includes('*') || !exportConfig || typeof exportConfig !== 'object') {
    return;
  }
  const targets = [];
  collectStringTargets(exportConfig, `exports.${exportKey}`, targets);
  const wildcardTargets = targets.filter(({ target }) => target.includes('*'));
  if (wildcardTargets.length < 2) {
    return;
  }
  const [first, ...rest] = wildcardTargets.map(({ label, target }) => ({
    label,
    captures: wildcardCaptures(files, target),
  }));
  for (const candidate of rest) {
    const missing = [...first.captures].filter((capture) => !candidate.captures.has(capture));
    const extra = [...candidate.captures].filter((capture) => !first.captures.has(capture));
    if (missing.length > 0 || extra.length > 0) {
      throw new Error(
        `${packageName} wildcard export conditions do not publish the same subpaths: ` +
          `${first.label} vs ${candidate.label}`,
      );
    }
  }
}

export function assertPackedPackageBoundary(packageJson, files) {
  if (!packageJson.name?.startsWith('@mango/') || packageJson.name === '@mango/cli') {
    return;
  }
  const sourceFile = files.find(
    (file) =>
      /^package\/src\//u.test(file) ||
      /^package\/(?:api|components|hooks|types|utils|views)\//u.test(file) ||
      /^package\/index\.ts$/u.test(file),
  );
  if (sourceFile) {
    throw new Error(`${packageJson.name} tarball must not publish source file: ${sourceFile}`);
  }
  for (const section of ['dependencies', 'devDependencies', 'optionalDependencies', 'peerDependencies']) {
    for (const [dependency, version] of Object.entries(packageJson[section] || {})) {
      if (typeof version === 'string' && version.startsWith('workspace:')) {
        throw new Error(
          `${packageJson.name} packed ${section}.${dependency} must not expose workspace protocol: ${version}`,
        );
      }
    }
  }
  for (const { label, target } of packedExportTargets(packageJson)) {
    assertPublishedTarget(packageJson.name, files, label, target);
  }
  if (packageJson.exports && typeof packageJson.exports === 'object') {
    for (const [exportKey, exportConfig] of Object.entries(packageJson.exports)) {
      assertWildcardConditionParity(packageJson.name, files, exportKey, exportConfig);
    }
  }
}
