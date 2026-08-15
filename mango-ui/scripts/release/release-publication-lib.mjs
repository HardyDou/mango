export function decideRegistryAction({ hosted, consume, expectedSha256 }) {
  if (hosted.state === 'unknown' || consume.state === 'unknown') {
    return { action: 'STOP', reason: 'registry state is unknown' };
  }
  if (hosted.state === 'absent' && consume.state === 'present') {
    return { action: 'STOP', reason: 'consume registry contains a coordinate absent from publish registry' };
  }
  if (hosted.state === 'present' && hosted.sha256 !== expectedSha256) {
    return { action: 'STOP', reason: 'publish-registry tarball differs from the sealed artifact' };
  }
  if (consume.state === 'present' && consume.sha256 !== expectedSha256) {
    return { action: 'STOP', reason: 'consume-registry tarball differs from the sealed artifact' };
  }
  if (hosted.state === 'present' && consume.state === 'present') {
    return { action: 'VERIFIED', reason: 'both registry roles contain the sealed artifact' };
  }
  if (hosted.state === 'present') {
    return { action: 'VERIFY_PENDING', reason: 'publish registry is complete; consume registry is not visible yet' };
  }
  return { action: 'PUBLISH', reason: 'both registry roles prove the coordinate is absent' };
}

export function markRemoteWriteIntent(manifest, { kind, target, recordedAt = new Date().toISOString() }) {
  if (!manifest || typeof manifest !== 'object') throw new Error('release manifest is required');
  if (!kind || !target) throw new Error('remote write kind and target are required');
  manifest.remoteWrites = true;
  manifest.remoteWriteAudit ??= [];
  manifest.remoteWriteAudit.push({ kind, target, recordedAt });
  return manifest;
}

export function recoverRemoteWriteAudit(manifest, { recordedAt = new Date().toISOString() } = {}) {
  if (!manifest || typeof manifest !== 'object') throw new Error('release manifest is required');
  if (manifest.remoteWrites === true) return false;
  const publications = [
    ...Object.values(manifest.packagePublications ?? {}),
    ...Object.values(manifest.mavenPublications ?? {}),
  ];
  const hasAttemptEvidence = publications.some((entry) => Array.isArray(entry.attempts) && entry.attempts.length > 0);
  if (!hasAttemptEvidence) return false;
  markRemoteWriteIntent(manifest, {
    kind: 'recovered-publication-audit',
    target: 'manifest-attempt-evidence',
    recordedAt,
  });
  return true;
}
