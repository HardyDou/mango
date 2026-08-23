#!/usr/bin/env node
import { createHash } from 'node:crypto';
import {
  existsSync,
  mkdtempSync,
  mkdirSync,
  readFileSync,
  readdirSync,
  renameSync,
  rmSync,
  statSync,
  writeFileSync,
} from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { tmpdir } from 'node:os';
import { fileURLToPath } from 'node:url';
import { spawn, spawnSync } from 'node:child_process';
import { assertReleasePlanShape, sha256 } from './release-plan-lib.mjs';
import {
  advanceJournalEntry,
  assertPreparedCandidate,
  buildNormalReleaseBaseline,
  digestRelease,
} from './release-manifest-lib.mjs';
import {
  decideRegistryAction,
  markRemoteWriteIntent,
  recoverRemoteWriteAudit,
  validatePublicationPreflight,
} from './release-publication-lib.mjs';
import {
  createMavenConsumerPom,
  decideMavenCoordinateAction,
  mavenVerificationFiles,
  resolveMavenPublishConcurrency,
  verifyStagedMavenRepository,
} from './release-maven-lib.mjs';
import { assertCleanWorktree, gitValue } from './release-repository-lib.mjs';
import { acquireReleaseLock } from './release-lock-lib.mjs';
import { assertReleaseNodeVersion } from '../../packages/mango-cli/src/release-runtime.mjs';

const workspaceRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const repoRoot = resolve(workspaceRoot, '..');
assertReleaseNodeVersion({ manifestPath: join(workspaceRoot, 'package.json') });
const args = process.argv.slice(2);
const action = args[0] || 'status';
const planPath = resolveArg('--plan', join(workspaceRoot, '.changeset/release-plan.json'));
const plan = readJson(planPath);
assertReleasePlanShape(plan);
const runtimeBase = resolveArg('--runtime-dir', join(repoRoot, '.runtime/mango-release'));
const releaseRoot = join(runtimeBase, plan.planDigest);
const manifestPath = join(releaseRoot, 'manifest.json');
if (!existsSync(manifestPath)) throw new Error(`prepare manifest does not exist: ${manifestPath}`);
const manifest = readJson(manifestPath);
if (!['publish', 'status', 'repair'].includes(action))
  throw new Error(`unsupported prepared release action: ${action}`);
const publishRegistry = valueArg('--publish-registry') || process.env.MANGO_RELEASE_NPM_PUBLISH_REGISTRY || '';
const consumeRegistry = valueArg('--consume-registry') || process.env.MANGO_RELEASE_NPM_CONSUME_REGISTRY || '';
if (plan.order.length > 0) {
  assertRegistryUrl(publishRegistry, 'npm publish registry');
  assertRegistryUrl(consumeRegistry, 'npm consume registry');
}
const mavenPublishRegistry =
  valueArg('--maven-publish-registry') || process.env.MANGO_RELEASE_MAVEN_PUBLISH_REGISTRY || '';
const mavenConsumeRegistry =
  valueArg('--maven-consume-registry') || process.env.MANGO_RELEASE_MAVEN_CONSUME_REGISTRY || '';
const mavenPublishServerId =
  valueArg('--maven-publish-server-id') || process.env.MANGO_RELEASE_MAVEN_PUBLISH_SERVER_ID || '';
const mavenConsumeServerId =
  valueArg('--maven-consume-server-id') || process.env.MANGO_RELEASE_MAVEN_CONSUME_SERVER_ID || 'mango-release-consume';
const requestedMavenVerifyMode =
  valueArg('--maven-verify-mode') || process.env.MANGO_RELEASE_MAVEN_VERIFY_MODE || 'basic';
const mavenVerifyMode = manifest.maven ? requestedMavenVerifyMode : 'basic';
if (manifest.maven && !['basic', 'full'].includes(mavenVerifyMode)) {
  throw new Error(`invalid Maven verification mode: ${mavenVerifyMode}; expected basic or full`);
}
const requestedMavenPublishConcurrency =
  valueArg('--maven-publish-concurrency') || process.env.MANGO_RELEASE_MAVEN_PUBLISH_CONCURRENCY || 16;
const mavenPublishConcurrency = manifest.maven ? resolveMavenPublishConcurrency(requestedMavenPublishConcurrency) : 16;
if (manifest.maven) {
  assertRegistryUrl(mavenPublishRegistry, 'Maven publish registry');
  assertRegistryUrl(mavenConsumeRegistry, 'Maven consume registry');
  if (action !== 'status' && !mavenPublishServerId) throw new Error('Maven publish server id is required');
}
const visibilityTimeoutSeconds = numericArg('--visibility-timeout', 300, 0, 300);
const visibilityPollSeconds = numericArg('--visibility-poll', 5, 1, 30);
const gitCommonDir = resolve(repoRoot, gitValue(repoRoot, ['rev-parse', '--git-common-dir']));
const releaseLock = acquireReleaseLock(join(gitCommonDir, 'mango-release/release.lock'), {
  preparedCandidateId: manifest.preparedCandidateId,
});
process.on('exit', releaseLock);

if (action === 'status') {
  verifySourceAndArtifacts({ requireMergedSource: false });
  const remote = await inspectPublicationPreflight({ persist: false });
  printStatus(manifest, remote);
  process.exit(0);
}
if (!args.includes('--authorize') && process.env.MANGO_RELEASE_AUTHORIZED !== '1') {
  throw new Error('publish/repair requires current-turn --authorize or MANGO_RELEASE_AUTHORIZED=1');
}

if (action === 'repair' && manifest.status === 'COMPLETED') {
  verifyCompletedReleaseReadOnly();
  if (recoverRemoteWriteAudit(manifest)) writeManifest();
  printStatus(manifest);
  process.exit(0);
}

if (recoverRemoteWriteAudit(manifest)) writeManifest();

assertCleanWorktree(repoRoot);
runChecked(process.execPath, [join(workspaceRoot, 'scripts/release/check-release-plan.mjs')], repoRoot);
verifySourceAndArtifacts({ requireMergedSource: true });
if (manifest.status === 'PREPARED') throw new Error('candidate verification has not passed; run mango release prepare');
if (manifest.status === 'FAILED' && manifest.states.CANDIDATE_VERIFIED?.status !== 'passed') {
  throw new Error('candidate verification failed; a new prepare is required');
}
if (manifest.states.READY?.status !== 'passed') throw new Error('sealed candidate READY record is missing');

let publishedCount = 0;
const publicationPreflight = await inspectPublicationPreflight();
try {
  validatePublicationPreflight({ action, ...publicationPreflight });
} catch (error) {
  manifest.status = error?.code === 'AMBIGUOUS' ? 'AMBIGUOUS' : manifest.status;
  manifest.publicationPreflight.result = 'STOP';
  manifest.publicationPreflight.reason = String(error?.message || error);
  writeManifest();
  throw error;
}
manifest.publicationPreflight.result = 'PASS';
manifest.status = action === 'repair' ? 'REPAIR' : 'PUBLISHING';
writeManifest();
if (manifest.maven) await publishMavenBatch();
for (const packageName of plan.order) {
  const artifact = manifest.artifacts.find((entry) => entry.name === packageName);
  const coordinate = `${artifact.name}@${artifact.version}`;
  const publication = manifest.packagePublications[packageName];
  const inspected = publicationPreflight.npm.find((entry) => entry.name === packageName);
  const { hosted, consume: group, decision } = inspected;
  const npmJournal = manifest.publicationJournal?.npm?.find((entry) => entry.identity === packageName);
  if (
    action === 'repair' &&
    npmJournal &&
    ['INTENT_RECORDED', 'REQUEST_DISPATCHED'].includes(npmJournal.state) &&
    decision.action === 'PUBLISH'
  ) {
    throw new Error(`${coordinate} is AMBIGUOUS; resolve the existing publication journal before retrying`);
  }
  if (decision.action === 'VERIFIED') {
    advancePublicationJournal('npm', packageName, 'REMOTE_OBSERVED', { target: coordinate, sha256: hosted.sha256 });
    advancePublicationJournal('npm', packageName, 'VERIFIED', { target: coordinate, sha256: group.sha256 });
    publication.status = 'PUBLISHED';
    publication.hostedSha256 = hosted.sha256;
    publication.consumeSha256 = group.sha256;
  } else if (decision.action === 'VERIFY_PENDING') {
    advancePublicationJournal('npm', packageName, 'REMOTE_OBSERVED', { target: coordinate, sha256: hosted.sha256 });
    publication.status = 'VERIFY_PENDING';
    publication.hostedSha256 = hosted.sha256;
  } else {
    const artifactPath = resolveArtifactPath(artifact);
    const startedAt = new Date().toISOString();
    advancePublicationJournal('npm', packageName, 'INTENT_RECORDED', {
      target: coordinate,
      requestDigest: digestRelease({ kind: 'npm-publish', target: coordinate, sha256: artifact.sha256 }),
    });
    markRemoteWriteIntent(manifest, { kind: 'npm-publish', target: coordinate, recordedAt: startedAt });
    writeManifest();
    const result = runCaptured(
      'npm',
      ['publish', artifactPath, `--registry=${publishRegistry}`, '--access=public'],
      repoRoot,
      20 * 60 * 1000,
    );
    publication.attempts.push(commandEvidence(result, startedAt, repoRoot));
    advancePublicationJournal('npm', packageName, 'REQUEST_DISPATCHED', {
      target: coordinate,
      requestDigest: digestRelease({ kind: 'npm-publish', target: coordinate, sha256: artifact.sha256 }),
    });
    publication.updatedAt = new Date().toISOString();
    writeManifest();
    if (result.status !== 0) {
      publication.status = 'FAILED';
      manifest.status = 'AMBIGUOUS';
      manifest.states.PUBLISHED = releaseState('failed', `npm publish failed for ${coordinate}`);
      writeManifest();
      throw new Error(`npm publish failed for ${coordinate}; inspect ${manifestPath}`);
    }
    const hostedAfter = registryVersion(coordinate, publishRegistry);
    if (hostedAfter.state === 'present') attachRegistryDigests(hostedAfter, coordinate, publishRegistry);
    const hostedDecision = decideRegistryAction({
      hosted: hostedAfter,
      consume: { state: 'absent' },
      expectedSha256: artifact.sha256,
      expectedIntegrity: artifact.sri,
    });
    if (hostedDecision.action !== 'VERIFY_PENDING') {
      failPublication(packageName, hostedDecision.reason, hostedAfter, null);
    }
    publication.status = 'VERIFY_PENDING';
    publication.hostedSha256 = hostedAfter.sha256;
    publication.hostedIntegrity = hostedAfter.integrity;
    advancePublicationJournal('npm', packageName, 'REMOTE_OBSERVED', {
      target: coordinate,
      sha256: hostedAfter.sha256,
      integrity: hostedAfter.integrity,
    });
  }

  const groupReady =
    publication.status === 'PUBLISHED' || waitForConsumeRegistry(coordinate, artifact.sha256, artifact.sri);
  if (!groupReady) {
    publication.status = 'VERIFY_PENDING';
    manifest.status = 'PARTIAL';
    manifest.states.PUBLISHED = releaseState(
      'pending',
      `${coordinate} is published but not yet visible from consume registry`,
    );
    writeManifest();
    throw new Error(`${coordinate} is awaiting consume-registry visibility; run repair without republishing`);
  }
  publication.status = 'PUBLISHED';
  publication.consumeSha256 = artifact.sha256;
  publication.consumeIntegrity = artifact.sri;
  advancePublicationJournal('npm', packageName, 'VERIFIED', { target: coordinate, sha256: artifact.sha256 });
  publication.updatedAt = new Date().toISOString();
  publishedCount += 1;
  manifest.status = 'PUBLISHING';
  writeManifest();
}

manifest.status = 'PUBLISHING';
manifest.states.PUBLISHED = releaseState(
  'passed',
  `all sealed ${plan.order.length} npm tarball(s)${manifest.maven ? ` and ${manifest.maven.coordinateCount} Maven coordinate(s)` : ''} resolve identically from publish and consume registries`,
);
writeManifest();

if (manifest.states.CONSUMER_VERIFIED.status !== 'passed') {
  const startedAt = new Date().toISOString();
  const npmConsumer =
    plan.order.length > 0
      ? runCaptured(
          'pnpm',
          [
            'package-consumer:typecheck',
            '--',
            '--release-candidate-matrix',
            '--pure-registry',
            '--reuse-build',
            `--registry=${consumeRegistry}`,
          ],
          workspaceRoot,
          60 * 60 * 1000,
        )
      : null;
  const mavenConsumer = manifest.maven ? verifyMavenConsumer() : [];
  manifest.evidence.consumer = {
    npm: npmConsumer ? commandEvidence(npmConsumer, startedAt, workspaceRoot) : null,
    maven: mavenConsumer,
    mavenVerificationMode: manifest.maven ? mavenVerifyMode : null,
  };
  if ((npmConsumer && npmConsumer.status !== 0) || mavenConsumer.some((entry) => entry.exitCode !== 0)) {
    manifest.status = 'PARTIAL';
    manifest.states.CONSUMER_VERIFIED = releaseState('failed', 'pure consume-registry consumer failed');
    writeManifest();
    throw new Error(`pure consume-registry consumer failed; immutable packages remain published: ${manifestPath}`);
  }
  manifest.status = 'PUBLISHING';
  manifest.states.CONSUMER_VERIFIED = releaseState(
    'passed',
    'clean consumer installed only the consume-registry tuple',
  );
  writeManifest();
}

completeTagAndRelease();
manifest.status = 'COMPLETED';
manifest.states.COMPLETED = releaseState(
  'passed',
  `tag and GitHub Release verified after consumer success: ${plan.release.tag}`,
);
manifest.nextBaseline = buildNormalReleaseBaseline({
  plan,
  manifest,
});
writeJson(join(releaseRoot, 'release-baseline.json'), manifest.nextBaseline);
applyCloseoutProjection();
writeManifest();
printStatus(manifest);

function verifySourceAndArtifacts({ requireMergedSource }) {
  if (manifest.schemaVersion !== 1 || manifest.planDigest !== plan.planDigest) {
    throw new Error('prepare manifest and release plan differ');
  }
  if (requireMergedSource) {
    const headCommit = gitValue(repoRoot, ['rev-parse', 'HEAD^{commit}']);
    const headTree = gitValue(repoRoot, ['rev-parse', 'HEAD^{tree}']);
    const mainTree = gitValue(repoRoot, ['rev-parse', 'origin/main^{tree}']);
    if (headTree !== mainTree) throw new Error('publish requires HEAD tree to equal origin/main tree');
    if (headTree !== manifest.source.tree) throw new Error('merged source tree differs from the prepared source tree');
    if (headCommit !== manifest.source.commit) manifest.source.mergedCommit = headCommit;
    const notesPath = join(workspaceRoot, plan.release.notesFile);
    if (sha256(Buffer.from(readFileSync(notesPath, 'utf8'), 'utf8')) !== plan.release.notesSha256) {
      throw new Error('release notes changed after planning');
    }
  }
  for (const artifact of manifest.artifacts) {
    const path = resolveArtifactPath(artifact);
    if (!existsSync(path) || sha256File(path) !== artifact.sha256) {
      throw new Error(`sealed artifact changed after prepare: ${artifact.name}@${artifact.version}`);
    }
  }
  const sourceArchive = resolveReleasePath(manifest.source.archive, 'source archive');
  if (
    !existsSync(sourceArchive) ||
    sha256File(sourceArchive) !== manifest.source.archiveSha256 ||
    statSize(sourceArchive) !== manifest.source.archiveSize
  ) {
    throw new Error('sealed source archive changed after prepare');
  }
  if (manifest.maven) {
    verifyStagedMavenRepository(resolve(releaseRoot, manifest.maven.repository), manifest.maven);
  }
  if (manifest.preparedCandidateId) {
    assertPreparedCandidate({
      manifest,
      planDigest: plan.planDigest,
      source: manifest.source,
      artifacts: manifest.artifacts,
      mavenArtifacts: manifest.maven,
      sourceArchive: {
        path: manifest.source.archive,
        size: manifest.source.archiveSize,
        sha256: manifest.source.archiveSha256,
      },
    });
  }
}

async function inspectPublicationPreflight({ persist = true } = {}) {
  const npm = plan.order.map((packageName) => {
    const artifact = manifest.artifacts.find((entry) => entry.name === packageName);
    const identity = `${artifact.name}@${artifact.version}`;
    const hosted = registryVersion(identity, publishRegistry);
    const consume = registryVersion(identity, consumeRegistry);
    if (hosted.state === 'present') attachRegistryDigests(hosted, identity, publishRegistry);
    if (consume.state === 'present') attachRegistryDigests(consume, identity, consumeRegistry);
    const journal = manifest.publicationJournal?.npm?.find((entry) => entry.identity === packageName);
    return {
      name: packageName,
      identity,
      hosted,
      consume,
      journalState: journal?.state,
      decision: decideRegistryAction({
        hosted,
        consume,
        expectedSha256: artifact.sha256,
        expectedIntegrity: artifact.sri,
      }),
    };
  });
  const maven = await mapConcurrent(manifest.maven?.coordinates ?? [], mavenPublishConcurrency, async (coordinate) => {
    const expectedFiles = mavenVerificationFiles(coordinate, mavenVerifyMode);
    const [publishFiles, consumeFiles] = await Promise.all([
      Promise.all(expectedFiles.map((file) => remoteMavenFileStateAsync(mavenPublishRegistry, file))),
      Promise.all(expectedFiles.map((file) => remoteMavenFileStateAsync(mavenConsumeRegistry, file))),
    ]);
    const journal = manifest.publicationJournal?.maven?.find((entry) => entry.identity === coordinate.coordinate);
    return {
      identity: coordinate.coordinate,
      publishFiles,
      consumeFiles,
      journalState: journal?.state,
      decision: decideMavenCoordinateAction({ publishFiles, consumeFiles, expectedFiles }),
    };
  });
  const summary = {
    action,
    inspectedAt: new Date().toISOString(),
    mavenVerificationMode: manifest.maven ? mavenVerifyMode : null,
    mavenPublishConcurrency: manifest.maven ? mavenPublishConcurrency : null,
    npm: npm.map(({ identity, journalState, decision }) => ({ identity, journalState, decision })),
    maven: maven.map(({ identity, journalState, decision }) => ({ identity, journalState, decision })),
  };
  if (persist) {
    manifest.publicationPreflight = summary;
    writeManifest();
  }
  return { npm, maven };
}

function advancePublicationJournal(kind, identity, state, evidence) {
  const entry = manifest.publicationJournal?.[kind]?.find((candidate) => candidate.identity === identity);
  if (!entry || entry.state === state) return;
  if (entry.state === 'VERIFIED') return;
  advanceJournalEntry(entry, state, evidence);
  writeManifest();
}

async function mapConcurrent(items, concurrency, task) {
  if (items.length === 0) return [];
  const results = new Array(items.length);
  let nextIndex = 0;
  const workerCount = Math.min(concurrency, items.length);
  async function worker() {
    for (;;) {
      const index = nextIndex++;
      if (index >= items.length) return;
      results[index] = await task(items[index], index);
    }
  }
  await Promise.all(Array.from({ length: workerCount }, () => worker()));
  return results;
}

async function publishMavenBatch() {
  const repository = resolve(releaseRoot, manifest.maven.repository);
  const pending = [];
  for (const coordinate of manifest.maven.coordinates) {
    const publication = manifest.mavenPublications[coordinate.coordinate];
    const inspected = publicationPreflight.maven.find((entry) => entry.identity === coordinate.coordinate);
    const { decision } = inspected;
    if (decision.action === 'VERIFIED') {
      advancePublicationJournal('maven', coordinate.coordinate, 'REMOTE_OBSERVED', {
        target: coordinate.coordinate,
        sha256: digestRelease(coordinate),
      });
      advancePublicationJournal('maven', coordinate.coordinate, 'VERIFIED', {
        target: coordinate.coordinate,
        sha256: digestRelease(coordinate),
      });
      publication.status = 'PUBLISHED';
    } else if (decision.action === 'VERIFY_PENDING') {
      advancePublicationJournal('maven', coordinate.coordinate, 'REMOTE_OBSERVED', {
        target: coordinate.coordinate,
        sha256: digestRelease(coordinate),
      });
      publication.status = 'VERIFY_PENDING';
    } else {
      pending.push({ coordinate, publication, repository });
    }
  }

  const results = await runMavenDeployments(pending);
  const failures = [];
  for (const { coordinate, publication, startedAt, result } of results) {
    publication.attempts.push(commandEvidence(result, startedAt, repoRoot));
    if (result.status !== 0) {
      publication.status = 'FAILED';
      publication.reason = `Maven deploy failed for ${coordinate.coordinate}`;
      failures.push(publication.reason);
      continue;
    }
    const expectedFiles = mavenVerificationFiles(coordinate, mavenVerifyMode);
    const hostedAfter = await Promise.all(
      expectedFiles.map((file) => remoteMavenFileStateAsync(mavenPublishRegistry, file)),
    );
    const hostedDecision = decideMavenCoordinateAction({
      publishFiles: hostedAfter,
      consumeFiles: expectedFiles.map((file) => ({ path: file.path, state: 'absent' })),
      expectedFiles,
    });
    if (!['VERIFY_PENDING', 'VERIFIED'].includes(hostedDecision.action)) {
      publication.status = 'FAILED';
      publication.reason = 'published Maven files differ from the sealed coordinate';
      publication.registryEvidence = { publishFiles: hostedAfter, consumeFiles: [] };
      failures.push(`${coordinate.coordinate}: ${publication.reason}`);
      continue;
    }
    publication.status = 'VERIFY_PENDING';
    advancePublicationJournal('maven', coordinate.coordinate, 'REMOTE_OBSERVED', {
      target: coordinate.coordinate,
      sha256: digestRelease(coordinate),
    });
  }
  writeManifest();
  if (failures.length > 0) {
    manifest.status = 'AMBIGUOUS';
    manifest.states.PUBLISHED = releaseState('failed', failures[0]);
    writeManifest();
    throw new Error(`${failures[0]}; inspect ${manifestPath}`);
  }

  const pendingConsume = [];
  for (const coordinate of manifest.maven.coordinates) {
    const publication = manifest.mavenPublications[coordinate.coordinate];
    if (publication.status !== 'PUBLISHED') pendingConsume.push(coordinate);
  }
  const consumeResults = await waitForMavenConsumeBatch(pendingConsume);
  for (const coordinate of manifest.maven.coordinates) {
    const publication = manifest.mavenPublications[coordinate.coordinate];
    const consumeResult = consumeResults.get(coordinate.coordinate);
    if (publication.status !== 'PUBLISHED' && consumeResult?.error) {
      throw new Error(`${coordinate.coordinate}: ${consumeResult.error}; inspect ${manifestPath}`);
    }
    if (publication.status !== 'PUBLISHED' && !consumeResult?.ready) {
      publication.status = 'VERIFY_PENDING';
      manifest.status = 'PARTIAL';
      manifest.states.PUBLISHED = releaseState(
        'pending',
        `${coordinate.coordinate} is awaiting Maven consume-registry visibility`,
      );
      writeManifest();
      throw new Error(
        `${coordinate.coordinate} is awaiting Maven consume-registry visibility; run repair without rebuilding`,
      );
    }
    publication.status = 'PUBLISHED';
    advancePublicationJournal('maven', coordinate.coordinate, 'VERIFIED', {
      target: coordinate.coordinate,
      sha256: digestRelease(coordinate),
    });
    publication.updatedAt = new Date().toISOString();
    writeManifest();
  }
}

async function runMavenDeployments(pending) {
  if (pending.length === 0) return [];
  const results = [];
  let nextIndex = 0;
  const workerCount = Math.min(mavenPublishConcurrency, pending.length);
  async function worker() {
    for (;;) {
      const index = nextIndex++;
      if (index >= pending.length) return;
      const { coordinate, repository } = pending[index];
      const pom = coordinate.files.find((file) => file.path.endsWith('.pom'));
      const main = coordinate.packaging === 'jar' ? coordinate.files.find((file) => file.path.endsWith('.jar')) : pom;
      if (!pom || !main) {
        results[index] = {
          coordinate,
          startedAt: new Date().toISOString(),
          requestDigest: digestRelease({
            kind: 'maven-deploy',
            target: coordinate.coordinate,
            files: coordinate.files,
          }),
          result: {
            command: 'maven deploy',
            status: 1,
            stdout: '',
            stderr: 'sealed Maven coordinate is missing POM or main artifact',
          },
        };
        continue;
      }
      const startedAt = new Date().toISOString();
      const requestDigest = digestRelease({
        kind: 'maven-deploy',
        target: coordinate.coordinate,
        files: coordinate.files,
      });
      advancePublicationJournal('maven', coordinate.coordinate, 'INTENT_RECORDED', {
        target: coordinate.coordinate,
        requestDigest,
      });
      markRemoteWriteIntent(manifest, {
        kind: 'maven-deploy',
        target: coordinate.coordinate,
        recordedAt: startedAt,
      });
      writeManifest();
      const result = await runCapturedAsync(
        'mvn',
        [
          '-q',
          'org.apache.maven.plugins:maven-deploy-plugin:3.1.4:deploy-file',
          `-Dfile=${join(repository, main.path)}`,
          `-DpomFile=${join(repository, pom.path)}`,
          `-DgroupId=${coordinate.groupId}`,
          `-DartifactId=${coordinate.artifactId}`,
          `-Dversion=${coordinate.version}`,
          `-Dpackaging=${coordinate.packaging}`,
          '-DgeneratePom=false',
          `-DrepositoryId=${mavenPublishServerId}`,
          `-Durl=${mavenPublishRegistry}`,
        ],
        repoRoot,
        20 * 60 * 1000,
      );
      advancePublicationJournal('maven', coordinate.coordinate, 'REQUEST_DISPATCHED', {
        target: coordinate.coordinate,
        requestDigest,
      });
      writeManifest();
      results[index] = { coordinate, startedAt, requestDigest, result };
    }
  }
  await Promise.all(Array.from({ length: workerCount }, () => worker()));
  return results;
}

function remoteMavenFileState(registry, file) {
  const temporary = mkdtempSync(join(tmpdir(), 'mango-release-maven-'));
  try {
    const destination = join(temporary, 'artifact');
    const url = `${registry.replace(/\/$/u, '')}/${file.path}`;
    const result = runCaptured(
      'curl',
      ['-sS', '-L', '--max-time', '60', '-o', destination, '-w', '%{http_code}', url],
      repoRoot,
      70_000,
    );
    const statusCode = result.stdout.trim().slice(-3);
    if (result.status !== 0) return { path: file.path, state: 'unknown', output: result.stderr.trim() };
    if (statusCode === '404') return { path: file.path, state: 'absent' };
    if (statusCode !== '200') return { path: file.path, state: 'unknown', output: `HTTP ${statusCode}` };
    return { path: file.path, state: 'present', sha256: sha256File(destination) };
  } finally {
    rmSync(temporary, { recursive: true, force: true });
  }
}

async function remoteMavenFileStateAsync(registry, file) {
  const temporary = mkdtempSync(join(tmpdir(), 'mango-release-maven-'));
  try {
    const destination = join(temporary, 'artifact');
    const url = `${registry.replace(/\/$/u, '')}/${file.path}`;
    const result = await runCapturedAsync(
      'curl',
      ['-sS', '-L', '--max-time', '60', '-o', destination, '-w', '%{http_code}', url],
      repoRoot,
      70_000,
    );
    const statusCode = result.stdout.trim().slice(-3);
    if (result.status !== 0) return { path: file.path, state: 'unknown', output: result.stderr.trim() };
    if (statusCode === '404') return { path: file.path, state: 'absent' };
    if (statusCode !== '200') return { path: file.path, state: 'unknown', output: `HTTP ${statusCode}` };
    return { path: file.path, state: 'present', sha256: sha256File(destination) };
  } finally {
    rmSync(temporary, { recursive: true, force: true });
  }
}

async function waitForMavenConsumeBatch(coordinates) {
  const results = new Map();
  if (coordinates.length === 0) return results;
  let nextIndex = 0;
  const workerCount = Math.min(mavenPublishConcurrency, coordinates.length);
  async function worker() {
    for (;;) {
      const index = nextIndex++;
      if (index >= coordinates.length) return;
      const coordinate = coordinates[index];
      try {
        results.set(coordinate.coordinate, { ready: await waitForMavenConsumeAsync(coordinate) });
      } catch (error) {
        results.set(coordinate.coordinate, { ready: false, error: String(error?.message || error) });
      }
    }
  }
  await Promise.all(Array.from({ length: workerCount }, () => worker()));
  return results;
}

async function waitForMavenConsumeAsync(coordinate) {
  const deadline = Date.now() + visibilityTimeoutSeconds * 1000;
  const expectedFiles = mavenVerificationFiles(coordinate, mavenVerifyMode);
  for (;;) {
    const consumeFiles = await Promise.all(
      expectedFiles.map((file) => remoteMavenFileStateAsync(mavenConsumeRegistry, file)),
    );
    const decision = decideMavenCoordinateAction({
      publishFiles: expectedFiles.map((file) => ({ ...file, state: 'present' })),
      consumeFiles,
      expectedFiles,
    });
    if (decision.action === 'VERIFIED') return true;
    if (decision.action === 'STOP') throw new Error(decision.reason);
    if (Date.now() >= deadline) return false;
    await new Promise((resolveDelay) => setTimeout(resolveDelay, visibilityPollSeconds * 1000));
  }
}

function verifyMavenConsumer() {
  const localRepository = mkdtempSync(join(tmpdir(), 'mango-release-maven-consumer-'));
  const consumerRoot = mkdtempSync(join(tmpdir(), 'mango-release-maven-consumer-project-'));
  const consumerPom = join(consumerRoot, 'pom.xml');
  const outputDirectory = join(consumerRoot, 'resolved-artifacts');
  try {
    writeFileSync(
      consumerPom,
      createMavenConsumerPom(manifest.maven.coordinates, mavenConsumeServerId, mavenConsumeRegistry),
      { mode: 0o600 },
    );
    const startedAt = new Date().toISOString();
    const result = runCaptured(
      'mvn',
      [
        '-q',
        '-U',
        '-f',
        consumerPom,
        'org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies',
        `-Dmaven.repo.local=${localRepository}`,
        '-DexcludeTransitive=true',
        '-Dmdep.useRepositoryLayout=true',
        `-DoutputDirectory=${outputDirectory}`,
      ],
      repoRoot,
      60 * 60 * 1000,
    );
    return [commandEvidence(result, startedAt, repoRoot)];
  } finally {
    rmSync(localRepository, { recursive: true, force: true });
    rmSync(consumerRoot, { recursive: true, force: true });
  }
}

function failMavenPublication(coordinate, reason, publishFiles = null, consumeFiles = null) {
  const publication = manifest.mavenPublications[coordinate];
  publication.status = 'FAILED';
  publication.reason = reason;
  publication.registryEvidence = { publishFiles, consumeFiles };
  manifest.status = 'AMBIGUOUS';
  manifest.states.PUBLISHED = releaseState('failed', `${coordinate}: ${reason}`);
  writeManifest();
  throw new Error(`${coordinate}: ${reason}`);
}

function applyCloseoutProjection() {
  const baselinePath = join(workspaceRoot, '.changeset/release-baseline.json');
  writeJson(baselinePath, manifest.nextBaseline);
  const removed = [];
  for (const file of [plan.legacyReconciliation?.file, ...(plan.changesets ?? []).map((entry) => entry.file)].filter(
    Boolean,
  )) {
    const path = resolve(workspaceRoot, file.replace(/^\.changeset\//u, '.changeset/'));
    if (!path.startsWith(`${join(workspaceRoot, '.changeset')}/`)) {
      throw new Error(`closeout path escapes .changeset: ${file}`);
    }
    if (existsSync(path)) {
      rmSync(path);
      removed.push(file);
    }
  }
  manifest.closeoutProjection = {
    status: 'PENDING_PR',
    baseline: '.changeset/release-baseline.json',
    removed,
    reason: 'submit the generated successful baseline and consumed intent cleanup through mango-submit-pr',
  };
}

function waitForConsumeRegistry(coordinate, expectedHash, expectedIntegrity) {
  const deadline = Date.now() + visibilityTimeoutSeconds * 1000;
  for (;;) {
    const state = registryVersion(coordinate, consumeRegistry);
    if (state.state === 'unknown') throw new Error(`${coordinate}: consume registry state is unknown`);
    if (state.state === 'present') {
      attachRegistryDigests(state, coordinate, consumeRegistry);
      const decision = decideRegistryAction({
        hosted: { state: 'present', sha256: expectedHash, integrity: expectedIntegrity },
        consume: state,
        expectedSha256: expectedHash,
        expectedIntegrity,
      });
      if (decision.action !== 'VERIFIED') throw new Error(`${coordinate}: ${decision.reason}`);
      return true;
    }
    if (Date.now() >= deadline) return false;
    Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, visibilityPollSeconds * 1000);
  }
}

function registryVersion(coordinate, registry) {
  const result = runCaptured(
    'npm',
    ['view', coordinate, 'version', `--registry=${registry}`, '--json'],
    repoRoot,
    60_000,
  );
  const output = `${result.stdout}\n${result.stderr}`;
  if (result.status === 0) {
    const value = JSON.parse(result.stdout);
    const version = Array.isArray(value) ? value.at(-1) : value;
    return { state: 'present', version: String(version), output: output.trim() };
  }
  if (/E404|404 Not Found|is not in this registry/iu.test(output)) return { state: 'absent', output: output.trim() };
  return { state: 'unknown', output: output.trim() };
}

function registryTarballSha256(coordinate, registry) {
  const temporary = mkdtempSync(join(tmpdir(), 'mango-release-registry-'));
  try {
    const result = runCaptured(
      'npm',
      ['pack', coordinate, `--registry=${registry}`, '--pack-destination', temporary],
      repoRoot,
      120_000,
    );
    if (result.status !== 0) throw new Error(`cannot download ${coordinate} from ${registry}: ${result.stderr}`);
    const tarballs = readdirSync(temporary).filter((file) => file.endsWith('.tgz'));
    if (tarballs.length !== 1)
      throw new Error(`expected one downloaded tarball for ${coordinate}, found ${tarballs.length}`);
    return sha256File(join(temporary, tarballs[0]));
  } finally {
    rmSync(temporary, { recursive: true, force: true });
  }
}

function attachRegistryDigests(state, coordinate, registry) {
  state.sha256 = registryTarballSha256(coordinate, registry);
  const result = runCaptured(
    'npm',
    ['view', coordinate, 'dist.integrity', `--registry=${registry}`, '--json'],
    repoRoot,
    60_000,
  );
  if (result.status !== 0) {
    state.state = 'unknown';
    state.output = `${state.output || ''}\n${result.stderr || result.stdout}`.trim();
    return state;
  }
  const integrity = JSON.parse(result.stdout);
  if (typeof integrity !== 'string' || !integrity) {
    state.state = 'unknown';
    state.output = `${state.output || ''}\nmissing dist.integrity`.trim();
    return state;
  }
  state.integrity = integrity;
  return state;
}

function completeTagAndRelease() {
  const tag = plan.release.tag;
  const intendedCommit = manifest.source.mergedCommit || manifest.source.commit;
  const localTag = runCaptured('git', ['rev-parse', '--verify', `refs/tags/${tag}^{commit}`], repoRoot);
  if (localTag.status === 0) {
    if (localTag.stdout.trim() !== intendedCommit) throw new Error(`existing tag ${tag} points to a different commit`);
  } else {
    runChecked('git', ['tag', '-a', tag, intendedCommit, '-m', plan.release.title], repoRoot);
  }
  const remoteTag = runCaptured('git', ['ls-remote', '--tags', 'origin', `refs/tags/${tag}^{}`], repoRoot);
  if (!remoteTag.stdout.trim()) {
    markRemoteWriteIntent(manifest, { kind: 'git-tag-push', target: tag });
    writeManifest();
    runChecked('git', ['push', 'origin', `refs/tags/${tag}`], repoRoot);
  }
  const release = runCaptured('gh', ['release', 'view', tag, '--json', 'tagName,url'], repoRoot);
  if (release.status !== 0) {
    markRemoteWriteIntent(manifest, { kind: 'github-release-create', target: tag });
    writeManifest();
    runChecked(
      'gh',
      [
        'release',
        'create',
        tag,
        '--verify-tag',
        '--title',
        plan.release.title,
        '--notes-file',
        join(workspaceRoot, plan.release.notesFile),
      ],
      repoRoot,
    );
  }
  const verified = runCaptured('gh', ['release', 'view', tag, '--json', 'tagName,url'], repoRoot);
  if (verified.status !== 0 || JSON.parse(verified.stdout).tagName !== tag)
    throw new Error(`GitHub Release verification failed: ${tag}`);
}

function verifyCompletedReleaseReadOnly() {
  for (const artifact of manifest.artifacts) {
    const coordinate = `${artifact.name}@${artifact.version}`;
    const hosted = registryVersion(coordinate, publishRegistry);
    const consume = registryVersion(coordinate, consumeRegistry);
    if (hosted.state === 'present') attachRegistryDigests(hosted, coordinate, publishRegistry);
    if (consume.state === 'present') attachRegistryDigests(consume, coordinate, consumeRegistry);
    const decision = decideRegistryAction({
      hosted,
      consume,
      expectedSha256: artifact.sha256,
      expectedIntegrity: artifact.sri,
    });
    if (decision.action !== 'VERIFIED') {
      throw new Error(`${coordinate}: completed release registry verification failed: ${decision.reason}`);
    }
  }
  if (manifest.maven) {
    for (const coordinate of manifest.maven.coordinates) {
      const expectedFiles = mavenVerificationFiles(coordinate, mavenVerifyMode);
      const publishFiles = expectedFiles.map((file) => remoteMavenFileState(mavenPublishRegistry, file));
      const consumeFiles = expectedFiles.map((file) => remoteMavenFileState(mavenConsumeRegistry, file));
      const decision = decideMavenCoordinateAction({ publishFiles, consumeFiles, expectedFiles });
      if (decision.action !== 'VERIFIED') {
        throw new Error(`${coordinate.coordinate}: completed Maven release verification failed: ${decision.reason}`);
      }
    }
  }
  const tag = plan.release.tag;
  const intendedCommit = manifest.source.mergedCommit || manifest.source.commit;
  const remoteTag = runCaptured('git', ['ls-remote', '--tags', 'origin', `refs/tags/${tag}^{}`], repoRoot);
  if (remoteTag.status !== 0 || remoteTag.stdout.trim().split(/\s+/u)[0] !== intendedCommit) {
    throw new Error(`completed release tag verification failed: ${tag}`);
  }
  const release = runCaptured('gh', ['release', 'view', tag, '--json', 'tagName,url'], repoRoot);
  if (release.status !== 0 || JSON.parse(release.stdout).tagName !== tag) {
    throw new Error(`completed GitHub Release verification failed: ${tag}`);
  }
}

function failPublication(packageName, reason, hosted = null, group = null) {
  const publication = manifest.packagePublications[packageName];
  publication.status = 'FAILED';
  publication.reason = reason;
  publication.registryEvidence = { hosted, group };
  manifest.status = 'AMBIGUOUS';
  manifest.states.PUBLISHED = releaseState('failed', `${packageName}: ${reason}`);
  writeManifest();
  throw new Error(`${packageName}: ${reason}`);
}

function resolveArtifactPath(artifact) {
  return resolveReleasePath(artifact.file, `artifact ${artifact.name}`);
}

function resolveReleasePath(relativePath, label) {
  const path = resolve(releaseRoot, relativePath);
  if (!path.startsWith(`${releaseRoot}/`)) throw new Error(`${label} path escapes release root: ${relativePath}`);
  return path;
}

function printStatus(value, remote = null) {
  console.log(`Release plan: ${value.planDigest}`);
  console.log(`Status: ${value.status}`);
  for (const name of value.packageOrder ?? []) {
    const artifact = value.artifacts.find((entry) => entry.name === name);
    console.log(`${name}@${artifact.version} ${value.packagePublications[name].status}`);
  }
  for (const coordinate of value.maven?.coordinates ?? []) {
    console.log(`${coordinate.coordinate} ${value.mavenPublications[coordinate.coordinate].status}`);
  }
  for (const entry of remote?.npm ?? []) {
    console.log(
      `${entry.identity}: journal=${entry.journalState || 'MISSING'} hosted=${entry.hosted.state} consume=${entry.consume.state} action=${entry.decision.action}`,
    );
  }
  for (const entry of remote?.maven ?? []) {
    console.log(`${entry.identity}: journal=${entry.journalState || 'MISSING'} action=${entry.decision.action}`);
  }
}

function releaseState(status, reason) {
  return { status, reason, updatedAt: new Date().toISOString() };
}

function commandEvidence(result, startedAt, cwd) {
  return {
    command: result.command,
    cwd,
    startedAt,
    completedAt: new Date().toISOString(),
    exitCode: result.status,
    output: `${result.stdout}${result.stderr}`.trim().slice(-20000) || '[no output]',
  };
}

function runChecked(command, commandArgs, cwd) {
  const result = spawnSync(command, commandArgs, { cwd, stdio: 'inherit', encoding: 'utf8' });
  if (result.status !== 0) throw new Error(`${command} ${commandArgs.join(' ')} failed with ${result.status ?? 1}`);
}

function runCaptured(command, commandArgs, cwd, timeout = 30 * 60 * 1000) {
  const result = spawnSync(command, commandArgs, { cwd, encoding: 'utf8', timeout });
  return {
    command: [command, ...commandArgs].join(' '),
    status: result.status ?? 1,
    stdout: result.stdout ?? '',
    stderr: result.stderr ?? result.error?.message ?? '',
  };
}

function runCapturedAsync(command, commandArgs, cwd, timeout = 30 * 60 * 1000) {
  return new Promise((resolveResult) => {
    const child = spawn(command, commandArgs, { cwd, encoding: 'utf8' });
    let stdout = '';
    let stderr = '';
    let settled = false;
    const timer = setTimeout(() => {
      child.kill('SIGTERM');
      finish(1, `${stderr}\ncommand timed out after ${timeout}ms`);
    }, timeout);
    child.stdout?.on('data', (chunk) => {
      stdout += chunk;
    });
    child.stderr?.on('data', (chunk) => {
      stderr += chunk;
    });
    child.on('error', (error) => finish(1, `${stderr}\n${error.message}`));
    child.on('close', (status) => finish(status ?? 1, stderr));

    function finish(status, errorOutput = stderr) {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      resolveResult({
        command: [command, ...commandArgs].join(' '),
        status,
        stdout,
        stderr: errorOutput || stderr,
      });
    }
  });
}

function writeManifest() {
  manifest.updatedAt = new Date().toISOString();
  writeJson(manifestPath, manifest);
}

function sha256File(path) {
  return createHash('sha256').update(readFileSync(path)).digest('hex');
}

function statSize(path) {
  return statSync(path).size;
}

function readJson(path) {
  return JSON.parse(readFileSync(path, 'utf8'));
}

function writeJson(path, value) {
  mkdirSync(dirname(path), { recursive: true });
  const temporary = `${path}.tmp`;
  writeFileSync(temporary, `${JSON.stringify(value, null, 2)}\n`, { mode: 0o600 });
  renameSync(temporary, path);
}

function valueArg(name) {
  const inline = args.find((arg) => arg.startsWith(`${name}=`));
  if (inline) return inline.slice(name.length + 1);
  const index = args.indexOf(name);
  return index >= 0 ? (args[index + 1] ?? '') : '';
}

function resolveArg(name, fallback) {
  return resolve(valueArg(name) || fallback);
}

function numericArg(name, fallback, minimum, maximum) {
  const raw = valueArg(name);
  const value = raw === '' ? fallback : Number(raw);
  if (!Number.isInteger(value) || value < minimum || value > maximum) {
    throw new Error(`${name} must be an integer from ${minimum} to ${maximum}`);
  }
  return value;
}

function assertRegistryUrl(value, label) {
  if (!value) throw new Error(`${label} is required`);
  const url = new URL(value);
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password) {
    throw new Error(`${label} must be an HTTP(S) URL without credentials`);
  }
}
