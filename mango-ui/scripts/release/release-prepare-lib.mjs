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

export function archiveFailedPrepare(path, now = new Date()) {
  const suffix = now.toISOString().replaceAll(/[:.]/gu, '-');
  let archived = `${path}.failed-${suffix}`;
  let sequence = 1;
  while (existsSync(archived)) {
    archived = `${path}.failed-${suffix}-${sequence}`;
    sequence += 1;
  }
  renameSync(path, archived);
  return archived;
}
