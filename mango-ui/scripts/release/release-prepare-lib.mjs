import { existsSync, renameSync } from 'node:fs';

export function isRetryablePrepareFailure(manifest, releasePlan, currentSource) {
  return (
    manifest.schemaVersion === 1 &&
    manifest.status === 'FAILED' &&
    manifest.remoteWrites === false &&
    manifest.planDigest === releasePlan.planDigest &&
    typeof manifest.source?.tree === 'string' &&
    typeof currentSource?.tree === 'string'
  );
}

export function isSupersededLocalCandidate(manifest, releasePlan, currentSource) {
  return (
    manifest.schemaVersion === 1 &&
    manifest.status === 'CANDIDATE_VERIFIED' &&
    manifest.remoteWrites === false &&
    manifest.planDigest === releasePlan.planDigest &&
    typeof manifest.source?.commit === 'string' &&
    typeof manifest.source?.tree === 'string' &&
    typeof currentSource?.commit === 'string' &&
    typeof currentSource?.tree === 'string' &&
    (manifest.source.commit !== currentSource.commit || manifest.source.tree !== currentSource.tree)
  );
}

export function archiveFailedPrepare(path, now = new Date()) {
  return archivePrepare(path, 'failed', now);
}

export function archiveSupersededPrepare(path, now = new Date()) {
  return archivePrepare(path, 'superseded', now);
}

function archivePrepare(path, reason, now) {
  const suffix = now.toISOString().replaceAll(/[:.]/gu, '-');
  let archived = `${path}.${reason}-${suffix}`;
  let sequence = 1;
  while (existsSync(archived)) {
    archived = `${path}.${reason}-${suffix}-${sequence}`;
    sequence += 1;
  }
  renameSync(path, archived);
  return archived;
}
