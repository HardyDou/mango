import assert from 'node:assert/strict';
import test from 'node:test';
import { mkdtempSync, mkdirSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import {
  buildSpringBootReactorArgs,
  readMavenModules,
  readProjectArtifactId,
  resolveSpringBootMavenReactor,
} from '../src/dev-maven-reactor.mjs';

function fixture() {
  const root = mkdtempSync(join(tmpdir(), 'mango-reactor-'));
  mkdirSync(join(root, 'library'), { recursive: true });
  mkdirSync(join(root, 'app'), { recursive: true });
  writeFileSync(
    join(root, 'pom.xml'),
    `<project><artifactId>fixture-root</artifactId><version>\${revision}</version><packaging>pom</packaging><properties><revision>1.0.0-SNAPSHOT</revision></properties><build><plugins><plugin><artifactId>spring-boot-maven-plugin</artifactId><configuration><skip>true</skip></configuration></plugin></plugins></build><modules><module>library</module><module>app</module></modules></project>`,
  );
  writeFileSync(join(root, 'library/pom.xml'), '<project><artifactId>fixture-library</artifactId></project>');
  writeFileSync(
    join(root, 'app/pom.xml'),
    '<project><parent><artifactId>fixture-root</artifactId></parent><artifactId>fixture-app</artifactId><build><plugins><plugin><artifactId>spring-boot-maven-plugin</artifactId><configuration><skip>false</skip></configuration></plugin></plugins></build></project>',
  );
  return { root, appPomPath: join(root, 'app/pom.xml') };
}

test('reactor resolution selects one app and validates the boot skip contract', () => {
  const context = fixture();
  const reactor = resolveSpringBootMavenReactor({ workspaceRoot: context.root, appPomPath: context.appPomPath });
  assert.equal(reactor.artifactId, 'fixture-app');
  assert.equal(reactor.selector, ':fixture-app');
  assert.equal(reactor.cwd, reactor.rootPom.slice(0, -'/pom.xml'.length));
  assert.deepEqual(readMavenModules('<modules><module>a</module><module>b</module></modules>'), ['a', 'b']);
  assert.equal(
    readProjectArtifactId('<parent><artifactId>parent</artifactId></parent><artifactId>child</artifactId>'),
    'child',
  );
  assert.equal(
    readProjectArtifactId(
      '<project><dependencies><dependency><artifactId>not-the-project</artifactId></dependency></dependencies></project>',
    ),
    '',
  );
});

test('reactor startup command cleans before compile without install, package, or manual classpath', () => {
  const args = buildSpringBootReactorArgs({
    rootPom: '/tmp/backend/pom.xml',
    selector: ':fixture-app',
    revision: '1.0.0-mango-001-SNAPSHOT',
    springArgs: ['runtime', '--server.port=5555'],
    goal: 'org.springframework.boot:spring-boot-maven-plugin:3.5.14:run',
  });
  assert.deepEqual(args, [
    '-f',
    'pom.xml',
    '-pl',
    ':fixture-app',
    '-am',
    '-DskipTests',
    '-Drevision=1.0.0-mango-001-SNAPSHOT',
    '-Dspring-boot.run.arguments=runtime --server.port=5555',
    'clean',
    'compile',
    'org.springframework.boot:spring-boot-maven-plugin:3.5.14:run',
  ]);
  assert.equal(
    args.some((arg) => /(?:install|package|(^|\s)-cp(?:=|$))/u.test(arg)),
    false,
  );
});
