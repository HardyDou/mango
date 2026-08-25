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
    manifest.status === 'READY' &&
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

export function orderExactMavenCoordinateSet(expectedOrder, stagedCoordinates) {
  if (!Array.isArray(expectedOrder) || !Array.isArray(stagedCoordinates)) {
    throw new Error('staged Maven coordinate comparison requires arrays');
  }
  const duplicateExpected = duplicates(expectedOrder);
  const stagedIdentities = stagedCoordinates.map((entry) => entry?.coordinate);
  const duplicateStaged = duplicates(stagedIdentities);
  const stagedByCoordinate = new Map(stagedCoordinates.map((entry) => [entry?.coordinate, entry]));
  const expected = new Set(expectedOrder);
  const missing = expectedOrder.filter((coordinate) => !stagedByCoordinate.has(coordinate));
  const extra = stagedIdentities.filter((coordinate) => !expected.has(coordinate));
  if (duplicateExpected.length || duplicateStaged.length || missing.length || extra.length) {
    throw new Error(
      `staged Maven coordinate set differs from plan; missing=${missing.join(',')} extra=${extra.join(',')} duplicatePlan=${duplicateExpected.join(',')} duplicateStaged=${duplicateStaged.join(',')}`,
    );
  }
  return expectedOrder.map((coordinate) => stagedByCoordinate.get(coordinate));
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

function duplicates(values) {
  const seen = new Set();
  const duplicate = new Set();
  for (const value of values) {
    if (seen.has(value)) duplicate.add(value);
    seen.add(value);
  }
  return [...duplicate].sort();
}
