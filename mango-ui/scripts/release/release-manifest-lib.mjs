import { createHash } from 'node:crypto';

const JOURNAL_STATES = ['NOT_ATTEMPTED', 'INTENT_RECORDED', 'REQUEST_DISPATCHED', 'REMOTE_OBSERVED', 'VERIFIED'];

export function canonicalReleaseBytes(value) {
  return Buffer.from(JSON.stringify(canonicalize(value)), 'utf8');
}

export function canonicalize(value) {
  if (Array.isArray(value)) return value.map(canonicalize);
  if (!value || typeof value !== 'object') return value;
  return Object.fromEntries(
    Object.keys(value)
      .sort((left, right) => Buffer.compare(Buffer.from(left, 'utf8'), Buffer.from(right, 'utf8')))
      .map((key) => [key, canonicalize(value[key])]),
  );
}

export function digestRelease(value) {
  return createHash('sha256').update(canonicalReleaseBytes(value)).digest('hex');
}

export function buildReleasePlanIdentity({
  schemaVersion = 1,
  sourceCommitOid,
  sourceTreeOid,
  catalogDigest,
  previousBaselineDigest,
  changesetsDigest,
  releaseNotesDigest,
  versionPolicyDigest,
  resolvedTuple,
  releaseClosure,
  publishOrder,
}) {
  return {
    schemaVersion,
    sourceCommitOid,
    sourceTreeOid,
    catalogDigest,
    previousBaselineDigest,
    changesetsDigest,
    releaseNotesDigest,
    versionPolicyDigest,
    resolvedTuple,
    releaseClosure,
    publishOrder,
  };
}

export function releasePlanDigest(identity) {
  return digestRelease(identity);
}

export function buildSealedArtifactManifest({
  releasePlanDigest: planDigest,
  source,
  npmArtifacts = [],
  mavenArtifacts = null,
  sourceArchive = null,
}) {
  const manifest = {
    schemaVersion: 1,
    releasePlanDigest: planDigest,
    source: { commit: source.commit, tree: source.tree },
    npm: npmArtifacts.map((artifact) => sealedNpmArtifact(artifact)),
    maven: mavenArtifacts ? sealedMavenArtifacts(mavenArtifacts) : null,
    sourceArchive: sourceArchive ? sealedFile(sourceArchive) : null,
  };
  return manifest;
}

export function preparedCandidateId(sealedArtifactManifest) {
  return digestRelease({ schemaVersion: 1, sealedArtifactManifest });
}

export function assertPreparedCandidate({
  manifest,
  planDigest: expectedPlanDigest,
  source,
  artifacts = [],
  mavenArtifacts = null,
  sourceArchive = null,
}) {
  if (!manifest || manifest.schemaVersion !== 1) throw new Error('sealed candidate manifest is invalid');
  if (manifest.planDigest !== expectedPlanDigest && manifest.releasePlanDigest !== expectedPlanDigest) {
    throw new Error('sealed candidate release plan digest differs from the current plan');
  }
  if (source && (manifest.source?.tree !== source.tree || manifest.source?.commit !== source.commit)) {
    throw new Error('sealed candidate source differs from the current source');
  }
  const expectedId = preparedCandidateId(manifest.sealedArtifactManifest);
  if (manifest.preparedCandidateId !== expectedId) throw new Error('prepared candidate identity mismatch');
  const expectedSealed = buildSealedArtifactManifest({
    releasePlanDigest: expectedPlanDigest,
    source: source || manifest.source,
    npmArtifacts: artifacts,
    mavenArtifacts,
    sourceArchive,
  });
  if (digestRelease(expectedSealed) !== digestRelease(manifest.sealedArtifactManifest)) {
    throw new Error('sealed candidate manifest differs from the current sealed bytes');
  }
  return true;
}

export function createPublicationJournal(order, artifactsByIdentity = new Map()) {
  return order.map((identity, index) => {
    const artifact = artifactsByIdentity.get(identity);
    return {
      sequence: index + 1,
      identity,
      preparedCandidateId: artifact?.preparedCandidateId,
      sealedSha256: artifact?.sha256,
      state: 'NOT_ATTEMPTED',
      attempts: [],
    };
  });
}

export function buildNormalReleaseBaseline({ plan, manifest }) {
  if (!manifest?.preparedCandidateId) throw new Error('normal baseline requires a prepared candidate identity');
  const previousPackages = Object.fromEntries(
    Object.entries(plan.baseline?.packages || {}).map(([name, entry]) => [
      name,
      typeof entry === 'string' ? { version: entry } : structuredClone(entry),
    ]),
  );
  const packages = {
    ...previousPackages,
    ...Object.fromEntries(
      (manifest.sealedArtifactManifest?.npm || []).map((artifact) => [
        artifact.name,
        {
          version: artifact.version,
          tarballSha256: artifact.sha256,
          sri: artifact.sri,
          publishedRanges: artifact.publishedRanges || [],
        },
      ]),
    ),
  };
  return {
    schemaVersion: 1,
    baselineKind: 'normal',
    tag: plan.release?.tag,
    commit: manifest.source.mergedCommit || manifest.source.commit,
    tree: manifest.source.tree,
    planDigest: plan.planDigest,
    preparedCandidateId: manifest.preparedCandidateId,
    packages: canonicalize(packages),
    maven: manifest.sealedArtifactManifest?.maven || plan.baseline?.maven || null,
    docs: manifest.sealedArtifactManifest?.maven ? null : plan.baseline?.docs || null,
  };
}

export function buildBootstrapBaseline({ catalog, evidence, source }) {
  if (!catalog || catalog.schemaVersion !== 1) throw new Error('bootstrap Catalog is invalid');
  if (!source || !/^[0-9a-f]{40}$/u.test(source.commit) || !/^[0-9a-f]{40}$/u.test(source.tree)) {
    throw new Error('bootstrap source commit/tree is invalid');
  }
  const expectedNpm = new Set((catalog.packages || []).map((entry) => entry.name));
  const actualNpm = new Set(Object.keys(evidence?.npm || {}));
  assertExactCoordinateSet('npm', expectedNpm, actualNpm);
  const expectedMaven = new Set(
    (catalog.maven?.publishableCoordinates || []).map(
      (entry) => `${entry.groupId}:${entry.artifactId}:${entry.version}`,
    ),
  );
  const actualMaven = new Set(Object.keys(evidence?.maven || {}));
  assertExactCoordinateSet('Maven', expectedMaven, actualMaven);
  const mavenVersions = new Set((catalog.maven?.publishableCoordinates || []).map((entry) => entry.version));
  if (mavenVersions.size !== 1) throw new Error('bootstrap Catalog must have one Maven platform version');
  const [mavenVersion] = mavenVersions;
  const expectedDocs = new Set(
    (catalog.releaseArtifacts || []).map((entry) => `${entry.groupId}:${entry.artifactId}:${mavenVersion}`),
  );
  const actualDocs = new Set(Object.keys(evidence?.docs || {}));
  assertExactCoordinateSet('docs', expectedDocs, actualDocs);
  const npm = Object.fromEntries(
    [...expectedNpm].sort().map((name) => {
      const item = evidence.npm[name];
      if (!item?.version || !/^[0-9a-f]{64}$/u.test(item.tarballSha256 || '')) {
        throw new Error(`bootstrap npm evidence is incomplete: ${name}`);
      }
      return [name, { ...item }];
    }),
  );
  const maven = Object.fromEntries(
    [...expectedMaven].sort().map((coordinate) => {
      const item = evidence.maven[coordinate];
      if (!Array.isArray(item?.files) || item.files.length === 0) {
        throw new Error(`bootstrap Maven evidence is incomplete: ${coordinate}`);
      }
      return [coordinate, item];
    }),
  );
  const docs = Object.fromEntries(
    [...expectedDocs].sort().map((coordinate) => {
      const item = evidence.docs[coordinate];
      if (!Array.isArray(item?.files) || item.files.length === 0) {
        throw new Error(`bootstrap docs evidence is incomplete: ${coordinate}`);
      }
      return [coordinate, item];
    }),
  );
  return {
    schemaVersion: 1,
    baselineKind: 'bootstrap',
    source,
    catalogDigest: catalog.catalogDigest,
    packages: npm,
    maven,
    docs,
    evidenceDigest: digestRelease(evidence),
  };
}

function assertExactCoordinateSet(kind, expected, actual) {
  const missing = [...expected].filter((value) => !actual.has(value)).sort();
  const extra = [...actual].filter((value) => !expected.has(value)).sort();
  if (missing.length || extra.length) {
    throw new Error(`${kind} bootstrap coordinate set mismatch; missing=${missing.join(',')} extra=${extra.join(',')}`);
  }
}

export function advanceJournalEntry(entry, nextState, evidence = null) {
  if (!entry || !JOURNAL_STATES.includes(entry.state) || !JOURNAL_STATES.includes(nextState)) {
    throw new Error('publication journal state is invalid');
  }
  const currentIndex = JOURNAL_STATES.indexOf(entry.state);
  const nextIndex = JOURNAL_STATES.indexOf(nextState);
  const readOnlyObservation =
    ['NOT_ATTEMPTED', 'INTENT_RECORDED', 'REQUEST_DISPATCHED'].includes(entry.state) && nextState === 'REMOTE_OBSERVED';
  if (nextIndex < currentIndex || (!readOnlyObservation && nextIndex > currentIndex + 1)) {
    throw new Error(`publication journal cannot transition ${entry.state} -> ${nextState}`);
  }
  if (nextState === 'INTENT_RECORDED' && !evidence?.target) {
    throw new Error('journal intent requires an exact target');
  }
  if (nextState === 'REQUEST_DISPATCHED' && !evidence?.requestDigest) {
    throw new Error('journal dispatch requires a request digest');
  }
  if (nextState === 'VERIFIED' && !evidence?.sha256) {
    throw new Error('journal verification requires an observed sha256');
  }
  entry.state = nextState;
  if (evidence) entry.attempts.push({ recordedAt: new Date().toISOString(), ...evidence });
  return entry;
}

function sealedNpmArtifact(artifact) {
  return {
    name: artifact.name,
    version: artifact.version,
    file: artifact.file,
    size: artifact.size,
    sha256: artifact.sha256,
    sri: artifact.sri || null,
    fileListDigest: artifact.fileListDigest || null,
    publishedRanges: artifact.publishedRanges || [],
  };
}

function sealedMavenArtifacts(descriptor) {
  return {
    scope: descriptor.scope,
    version: descriptor.version,
    coordinates: (descriptor.coordinates || []).map((coordinate) => ({
      coordinate: coordinate.coordinate,
      files: (coordinate.files || []).map((file) => ({ path: file.path, size: file.size, sha256: file.sha256 })),
    })),
  };
}

function sealedFile(file) {
  return { path: file.path, size: file.size, sha256: file.sha256 };
}
