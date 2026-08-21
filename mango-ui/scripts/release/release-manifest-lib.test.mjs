import assert from 'node:assert/strict';
import test from 'node:test';
import {
  advanceJournalEntry,
  assertPreparedCandidate,
  buildBootstrapBaseline,
  buildReleasePlanIdentity,
  buildSealedArtifactManifest,
  buildNormalReleaseBaseline,
  createPublicationJournal,
  digestRelease,
  preparedCandidateId,
  releasePlanDigest,
} from './release-manifest-lib.mjs';

const source = { commit: 'a'.repeat(40), tree: 'b'.repeat(40) };

test('release plan identity changes when tuple or publish order changes', () => {
  const input = {
    sourceCommitOid: source.commit,
    sourceTreeOid: source.tree,
    catalogDigest: 'c'.repeat(64),
    previousBaselineDigest: 'd'.repeat(64),
    changesetsDigest: 'e'.repeat(64),
    releaseNotesDigest: 'f'.repeat(64),
    versionPolicyDigest: '1'.repeat(64),
    resolvedTuple: [{ name: '@mango/base', version: '1.0.1' }],
    releaseClosure: ['@mango/base'],
    publishOrder: ['@mango/base'],
  };
  const first = buildReleasePlanIdentity(input);
  assert.notEqual(
    releasePlanDigest(first),
    releasePlanDigest({ ...first, resolvedTuple: [{ name: '@mango/base', version: '1.0.2' }] }),
  );
  assert.notEqual(
    releasePlanDigest(first),
    releasePlanDigest({ ...first, publishOrder: ['@mango/cli', '@mango/base'] }),
  );
});

test('prepared candidate identity binds source, plan and sealed bytes', () => {
  const sealed = buildSealedArtifactManifest({
    releasePlanDigest: '1'.repeat(64),
    source,
    npmArtifacts: [
      { name: '@mango/base', version: '1.0.1', file: 'artifacts/base.tgz', size: 10, sha256: '2'.repeat(64) },
    ],
  });
  const first = preparedCandidateId(sealed);
  assert.equal(first, preparedCandidateId(sealed));
  assert.notEqual(first, preparedCandidateId({ ...sealed, npm: [{ ...sealed.npm[0], sha256: '3'.repeat(64) }] }));
  assert.throws(
    () =>
      assertPreparedCandidate({
        manifest: {
          schemaVersion: 1,
          planDigest: '1'.repeat(64),
          source,
          preparedCandidateId: first,
          sealedArtifactManifest: sealed,
        },
        planDigest: '1'.repeat(64),
        source,
        artifacts: [
          { name: '@mango/base', version: '1.0.1', file: 'artifacts/base.tgz', size: 10, sha256: '3'.repeat(64) },
        ],
      }),
    /differs from the current sealed bytes/u,
  );
});

test('normal baseline retains the merged release commit and sealed npm/Maven identities', () => {
  const sealed = buildSealedArtifactManifest({
    releasePlanDigest: '1'.repeat(64),
    source,
    npmArtifacts: [
      {
        name: '@mango/base',
        version: '1.0.1',
        file: 'artifacts/base.tgz',
        size: 10,
        sha256: '2'.repeat(64),
        sri: `sha512-${'x'.repeat(88)}`,
        publishedRanges: [{ name: '@mango/common', range: '1.0.0', section: 'dependencies' }],
      },
    ],
    mavenArtifacts: {
      scope: 'all-non-app',
      version: '1.0.2',
      coordinates: [
        {
          coordinate: 'io.mango:mango-base:1.0.2',
          files: [{ path: 'base.pom', size: 10, sha256: '3'.repeat(64) }],
        },
      ],
    },
  });
  const manifest = {
    preparedCandidateId: preparedCandidateId(sealed),
    source: { ...source, mergedCommit: 'c'.repeat(40) },
    sealedArtifactManifest: sealed,
  };
  const baseline = buildNormalReleaseBaseline({
    plan: {
      planDigest: '1'.repeat(64),
      release: { tag: 'v1' },
      baseline: {
        packages: {
          '@mango/untouched': {
            version: '1.0.0',
            tarballSha256: '4'.repeat(64),
            sri: `sha512-${'y'.repeat(88)}`,
            publishedRanges: [],
          },
        },
      },
    },
    manifest,
  });
  assert.equal(baseline.commit, 'c'.repeat(40));
  assert.equal(baseline.packages['@mango/base'].sri, `sha512-${'x'.repeat(88)}`);
  assert.deepEqual(baseline.packages['@mango/base'].publishedRanges, [
    { name: '@mango/common', range: '1.0.0', section: 'dependencies' },
  ]);
  assert.equal(baseline.maven.version, '1.0.2');
  assert.equal(baseline.packages['@mango/untouched'].version, '1.0.0');
});

test('publication journal rejects skipping intent and preserves ambiguous requests', () => {
  const [entry] = createPublicationJournal(['@mango/base']);
  assert.throws(
    () => advanceJournalEntry(entry, 'REQUEST_DISPATCHED', { requestDigest: digestRelease({ a: 1 }) }),
    /cannot transition/u,
  );
  advanceJournalEntry(entry, 'INTENT_RECORDED', { target: '@mango/base@1.0.1' });
  advanceJournalEntry(entry, 'REQUEST_DISPATCHED', { target: '@mango/base@1.0.1', requestDigest: '4'.repeat(64) });
  assert.equal(entry.state, 'REQUEST_DISPATCHED');
});

test('publication journal can observe sealed remote bytes after a crash at the intent boundary', () => {
  const [entry] = createPublicationJournal(['@mango/base']);
  advanceJournalEntry(entry, 'INTENT_RECORDED', { target: '@mango/base@1.0.1' });
  advanceJournalEntry(entry, 'REMOTE_OBSERVED', { target: '@mango/base@1.0.1', sha256: '4'.repeat(64) });
  advanceJournalEntry(entry, 'VERIFIED', { target: '@mango/base@1.0.1', sha256: '4'.repeat(64) });
  assert.equal(entry.state, 'VERIFIED');
});

test('bootstrap baseline requires exact Catalog coordinate evidence', () => {
  const catalog = {
    schemaVersion: 1,
    catalogDigest: '5'.repeat(64),
    packages: [{ name: '@mango/base' }],
    maven: { publishableCoordinates: [{ groupId: 'io.mango', artifactId: 'mango-base-starter', version: '1.0.0' }] },
    releaseArtifacts: [{ groupId: 'io.mango', artifactId: 'mango-docs-bundle' }],
  };
  const evidence = {
    npm: { '@mango/base': { version: '1.0.0', tarballSha256: '6'.repeat(64), publishedRanges: [] } },
    maven: { 'io.mango:mango-base-starter:1.0.0': { files: [{ path: 'base.pom', sha256: '7'.repeat(64) }] } },
    docs: { 'io.mango:mango-docs-bundle:1.0.0': { files: [{ path: 'docs.jar', sha256: '8'.repeat(64) }] } },
  };
  const baseline = buildBootstrapBaseline({ catalog, evidence, source });
  assert.equal(baseline.baselineKind, 'bootstrap');
  assert.equal(baseline.packages['@mango/base'].tarballSha256, '6'.repeat(64));
  assert.ok(baseline.docs['io.mango:mango-docs-bundle:1.0.0']);
  assert.throws(
    () => buildBootstrapBaseline({ catalog, evidence: { ...evidence, npm: {} }, source }),
    /coordinate set mismatch/u,
  );
  assert.throws(
    () => buildBootstrapBaseline({ catalog, evidence: { ...evidence, docs: {} }, source }),
    /coordinate set mismatch/u,
  );
});
