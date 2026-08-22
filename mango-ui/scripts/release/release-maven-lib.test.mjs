import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import test from 'node:test';
import {
  createCandidateMavenConsumerPom,
  createMavenConsumerPom,
  createCandidateMavenSettings,
  decideMavenCoordinateAction,
  hasMavenReleaseImpact,
  inspectStagedMavenRepository,
  verifyStagedMavenRepository,
  mavenVerificationFiles,
  resolveMavenPublishConcurrency,
} from './release-maven-lib.mjs';

test('Maven impact includes production reactor files but excludes tests', () => {
  assert.equal(hasMavenReleaseImpact(['mango/mango-common/src/main/java/io/mango/A.java']), true);
  assert.equal(hasMavenReleaseImpact(['mango/mango-common/pom.xml']), true);
  assert.equal(hasMavenReleaseImpact(['mango/mango-common/src/test/java/io/mango/ATest.java']), false);
  assert.equal(hasMavenReleaseImpact(['mango/mango-common/README.md']), false);
  assert.equal(hasMavenReleaseImpact(['mango-docs/capabilities/README.md']), false);
});

test('staged Maven repository is sealed by exact POM and JAR hashes', () => {
  const root = mkdtempSync(join(tmpdir(), 'mango-maven-stage-'));
  try {
    const directory = join(root, 'io/mango/sample/1.2.3');
    mkdirSync(directory, { recursive: true });
    writeFileSync(join(directory, 'sample-1.2.3.pom'), '<project/>');
    writeFileSync(join(directory, 'sample-1.2.3.jar'), 'jar-bytes');
    const descriptor = inspectStagedMavenRepository(root, '1.2.3');
    assert.equal(descriptor.coordinateCount, 1);
    assert.equal(descriptor.coordinates[0].coordinate, 'io.mango:sample:1.2.3');
    assert.equal(descriptor.coordinates[0].packaging, 'jar');
    assert.equal(verifyStagedMavenRepository(root, descriptor).repositorySha256, descriptor.repositorySha256);
    writeFileSync(join(directory, 'sample-1.2.3.jar'), 'changed');
    assert.throws(() => verifyStagedMavenRepository(root, descriptor), /digest mismatch/u);
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});

test('candidate Maven settings preserve configured mirrors but exclude the sealed file repository', () => {
  const source = `<?xml version="1.0" encoding="UTF-8"?>
<settings>
  <servers>
    <server><id>private</id><password>must-not-be-copied</password></server>
  </servers>
  <mirrors>
    <mirror>
      <id>company-group</id>
      <mirrorOf>*</mirrorOf>
      <url>https://repo.example.test/maven-public/</url>
    </mirror>
  </mirrors>
</settings>`;
  const settings = createCandidateMavenSettings(source, 'mango-release-stage');
  assert.match(settings, /<id>company-group<\/id>/u);
  assert.match(settings, /<mirrorOf>\*,!mango-release-stage<\/mirrorOf>/u);
  assert.match(settings, /https:\/\/repo\.example\.test\/maven-public\//u);
  assert.doesNotMatch(settings, /must-not-be-copied/u);
  const secondPass = createCandidateMavenSettings(settings, 'mango-release-stage');
  assert.equal((secondPass.match(/!mango-release-stage/gu) ?? []).length, 1);
});

test('candidate Maven consumer declares every sealed coordinate in one repository-backed project', () => {
  const pom = createCandidateMavenConsumerPom(
    [
      { coordinate: 'io.mango:mango-bom:1.0.37', packaging: 'pom' },
      { coordinate: 'io.mango.common:mango-common:1.0.37', packaging: 'jar' },
    ],
    'mango-release-stage',
    'file:///tmp/mango-release-stage/',
  );
  assert.match(pom, /<id>mango-release-stage<\/id>/u);
  assert.match(pom, /<artifactId>mango-bom<\/artifactId>[\s\S]*?<type>pom<\/type>/u);
  assert.match(pom, /<artifactId>mango-common<\/artifactId>[\s\S]*?<type>jar<\/type>/u);
  assert.equal((pom.match(/<dependency>/gu) ?? []).length, 2);
});

test('basic Maven verification checks only POMs while full mode retains JAR checks', () => {
  const coordinate = {
    coordinate: 'io.mango:sample:1.2.3',
    files: [
      { path: 'io/mango/sample/1.2.3/sample-1.2.3.pom', sha256: 'a'.repeat(64) },
      { path: 'io/mango/sample/1.2.3/sample-1.2.3.jar', sha256: 'b'.repeat(64) },
    ],
  };
  assert.deepEqual(mavenVerificationFiles(coordinate, 'basic'), [coordinate.files[0]]);
  assert.deepEqual(mavenVerificationFiles(coordinate, 'full'), coordinate.files);
  assert.throws(() => mavenVerificationFiles(coordinate, 'invalid'), /expected basic or full/u);
});

test('Maven publish concurrency is bounded for controlled parallel deployment', () => {
  assert.equal(resolveMavenPublishConcurrency(8), 8);
  assert.equal(resolveMavenPublishConcurrency('16'), 16);
  assert.throws(() => resolveMavenPublishConcurrency(0), /integer from 1 to 16/u);
  assert.throws(() => resolveMavenPublishConcurrency(17), /integer from 1 to 16/u);
});

test('publish Maven consumer accepts a credential-free HTTP consume registry', () => {
  const pom = createMavenConsumerPom(
    [{ coordinate: 'io.mango:mango-common:1.2.3', packaging: 'jar' }],
    'mango-release-consume',
    'https://repo.example.test/maven-public/',
  );
  assert.match(pom, /<url>https:\/\/repo\.example\.test\/maven-public\/<\/url>/u);
  assert.throws(
    () =>
      createMavenConsumerPom(
        [{ coordinate: 'io.mango:mango-common:1.2.3', packaging: 'jar' }],
        'consume',
        'https://user:pass@example.test/repo',
      ),
    /credential-free/u,
  );
});

test('Maven recovery publishes only when both roles prove absence', () => {
  const expected = [
    { path: 'x.pom', sha256: 'a'.repeat(64) },
    { path: 'x.jar', sha256: 'b'.repeat(64) },
  ];
  const absent = expected.map((entry) => ({ path: entry.path, state: 'absent' }));
  const present = expected.map((entry) => ({ ...entry, state: 'present' }));
  assert.equal(
    decideMavenCoordinateAction({ publishFiles: absent, consumeFiles: absent, expectedFiles: expected }).action,
    'PUBLISH',
  );
  assert.equal(
    decideMavenCoordinateAction({ publishFiles: present, consumeFiles: absent, expectedFiles: expected }).action,
    'VERIFY_PENDING',
  );
  assert.equal(
    decideMavenCoordinateAction({ publishFiles: present, consumeFiles: present, expectedFiles: expected }).action,
    'VERIFIED',
  );
  assert.equal(
    decideMavenCoordinateAction({
      publishFiles: [present[0], absent[1]],
      consumeFiles: absent,
      expectedFiles: expected,
    }).action,
    'STOP',
  );
});
