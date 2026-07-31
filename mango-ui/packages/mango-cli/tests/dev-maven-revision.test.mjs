import assert from 'node:assert/strict';
import test from 'node:test';
import {
  buildWorkspaceMavenRevisionQualifier,
  injectMavenRevisionArgs,
  isMavenCommand,
  qualifyWorkspaceMavenRevision,
  readCiFriendlyMavenRevision,
} from '../src/dev-maven-revision.mjs';

test('workspace id becomes a Maven-safe stable qualifier', () => {
  assert.equal(buildWorkspaceMavenRevisionQualifier('mango_010'), 'mango-010');
  assert.equal(buildWorkspaceMavenRevisionQualifier(' Feature / A '), 'feature-a');
  assert.throws(() => buildWorkspaceMavenRevisionQualifier('---'), /workspace id is required/);
});

test('workspace qualifier is inserted before SNAPSHOT and remains idempotent', () => {
  assert.equal(qualifyWorkspaceMavenRevision('1.0.0-SNAPSHOT', 'mango-010'), '1.0.0-mango-010-SNAPSHOT');
  assert.equal(qualifyWorkspaceMavenRevision('1.0.0', 'mango-010'), '1.0.0-mango-010-SNAPSHOT');
  assert.equal(
    qualifyWorkspaceMavenRevision('1.0.0-mango-010-SNAPSHOT', 'mango-010'),
    '1.0.0-mango-010-SNAPSHOT',
  );
  assert.throws(() => qualifyWorkspaceMavenRevision('${revision}', 'mango-010'), /concrete value/);
});

test('CI-friendly Maven root exposes a concrete revision', () => {
  const pom =
    '<artifactId>business-backend</artifactId><version>${revision}</version>' +
    '<properties><revision>1.2.3-SNAPSHOT</revision></properties>';
  assert.equal(readCiFriendlyMavenRevision(pom), '1.2.3-SNAPSHOT');
  assert.equal(
    readCiFriendlyMavenRevision(
      '<parent><artifactId>parent</artifactId><version>${revision}</version></parent>' +
        '<artifactId>child</artifactId><version>1.2.3-SNAPSHOT</version>' +
        '<properties><revision>1.2.3-SNAPSHOT</revision></properties>',
    ),
    '',
  );
  assert.equal(readCiFriendlyMavenRevision('<artifactId>root</artifactId><version>${revision}</version>'), '');
});

test('Maven invocations receive exactly one workspace revision property', () => {
  assert.deepEqual(injectMavenRevisionArgs(['-f', 'pom.xml', 'install'], '1.0.0-mango-010-SNAPSHOT'), [
    '-Drevision=1.0.0-mango-010-SNAPSHOT',
    '-f',
    'pom.xml',
    'install',
  ]);
  assert.deepEqual(
    injectMavenRevisionArgs(['-Drevision=old', 'verify'], '1.0.0-mango-010-SNAPSHOT'),
    ['-Drevision=1.0.0-mango-010-SNAPSHOT', 'verify'],
  );
  assert.equal(isMavenCommand('mvn'), true);
  assert.equal(isMavenCommand('./mvnw'), true);
  assert.equal(isMavenCommand('C:\\project\\mvnw.cmd'), process.platform === 'win32');
  assert.equal(isMavenCommand('pnpm'), false);
});
