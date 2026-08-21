export function decideRegistryAction({ hosted, consume, expectedSha256, expectedIntegrity = null }) {
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
  if (expectedIntegrity && hosted.state === 'present' && hosted.integrity !== expectedIntegrity) {
    return { action: 'STOP', reason: 'publish-registry integrity differs from the sealed artifact' };
  }
  if (expectedIntegrity && consume.state === 'present' && consume.integrity !== expectedIntegrity) {
    return { action: 'STOP', reason: 'consume-registry integrity differs from the sealed artifact' };
  }
  if (hosted.state === 'present' && consume.state === 'present') {
    return { action: 'VERIFIED', reason: 'both registry roles contain the sealed artifact' };
  }
  if (hosted.state === 'present') {
    return { action: 'VERIFY_PENDING', reason: 'publish registry is complete; consume registry is not visible yet' };
  }
  return { action: 'PUBLISH', reason: 'both registry roles prove the coordinate is absent' };
}

export function validatePublicationPreflight({ action, npm = [], maven = [] }) {
  if (!['publish', 'repair'].includes(action)) throw new Error(`unsupported publication preflight action: ${action}`);
  const entries = [...npm, ...maven];
  for (const entry of entries) {
    if (!entry?.identity || !entry.decision?.action) throw new Error('publication preflight entry is invalid');
    const decision = entry.decision.action;
    const journalState = entry.journalState || 'NOT_ATTEMPTED';
    if (decision === 'STOP') {
      throw publicationError('REMOTE_CONFLICT', `${entry.identity}: ${entry.decision.reason}`);
    }
    if (action === 'publish' && decision !== 'PUBLISH') {
      throw publicationError(
        'REMOTE_CONFLICT',
        `${entry.identity}: first publish requires both registry roles to prove absence; use status/repair for recovery`,
      );
    }
    if (action === 'repair' && decision === 'PUBLISH' && journalState !== 'NOT_ATTEMPTED') {
      throw publicationError(
        'AMBIGUOUS',
        `${entry.identity}: publication may have been dispatched but the remote coordinate is absent`,
      );
    }
    if (action === 'repair' && ['VERIFIED', 'VERIFY_PENDING'].includes(decision) && journalState === 'NOT_ATTEMPTED') {
      throw publicationError(
        'REMOTE_CONFLICT',
        `${entry.identity}: remote content exists without this candidate's publication journal`,
      );
    }
  }
  return entries;
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

function publicationError(code, message) {
  const error = new Error(message);
  error.code = code;
  return error;
}
