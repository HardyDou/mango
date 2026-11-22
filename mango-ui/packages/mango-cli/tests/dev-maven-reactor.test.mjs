import assert from 'node:assert/strict';
import test from 'node:test';
import { mkdtempSync, mkdirSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import {
  buildSpringBootReactorArgs,
  readMavenModules,
  resolveReactorParentPomInstall,
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

test('reactor parent install targets an out-of-reactor parent POM at the development revision', () => {
  const root = mkdtempSync(join(tmpdir(), 'mango-parent-install-'));
  mkdirSync(join(root, 'parent'), { recursive: true });
  mkdirSync(join(root, 'library'), { recursive: true });
  writeFileSync(
    join(root, 'parent/pom.xml'),
    '<project><artifactId>fixture-parent</artifactId><version>${revision}</version><packaging>pom</packaging></project>',
  );
  writeFileSync(
    join(root, 'pom.xml'),
    '<project><parent><groupId>io.mango</groupId><artifactId>fixture-parent</artifactId><version>${revision}</version><relativePath>parent/pom.xml</relativePath></parent><artifactId>fixture-root</artifactId><version>${revision}</version><modules><module>library</module></modules></project>',
  );
  writeFileSync(join(root, 'library/pom.xml'), '<project><artifactId>fixture-library</artifactId></project>');

  const install = resolveReactorParentPomInstall({
    rootPom: join(root, 'pom.xml'),
    revision: '1.0.0-mango-001-SNAPSHOT',
  });
  assert.equal(install.pomPath, join(root, 'parent/pom.xml'));
  assert.deepEqual(install.steps, [
    {
      pomPath: join(root, 'parent/pom.xml'),
      args: ['-f', join(root, 'parent/pom.xml'), '-N', '-DskipTests', '-Drevision=1.0.0-mango-001-SNAPSHOT', 'install'],
    },
  ]);
});

test('reactor parent install installs imported reactor BOM POMs before the parent', () => {
  const root = mkdtempSync(join(tmpdir(), 'mango-parent-bom-'));
  mkdirSync(join(root, 'parent'), { recursive: true });
  mkdirSync(join(root, 'bom'), { recursive: true });
  writeFileSync(
    join(root, 'bom/pom.xml'),
    '<project><artifactId>fixture-bom</artifactId><version>${revision}</version><packaging>pom</packaging></project>',
  );
  writeFileSync(
    join(root, 'parent/pom.xml'),
    '<project><artifactId>fixture-parent</artifactId><version>${revision}</version><packaging>pom</packaging><dependencyManagement><dependencies><dependency><groupId>io.mango</groupId><artifactId>fixture-bom</artifactId><version>${revision}</version><type>pom</type><scope>import</scope></dependency></dependencies></dependencyManagement></project>',
  );
  writeFileSync(
    join(root, 'pom.xml'),
    '<project><parent><artifactId>fixture-parent</artifactId><version>${revision}</version><relativePath>parent/pom.xml</relativePath></parent><artifactId>fixture-root</artifactId><modules><module>bom</module></modules></project>',
  );
  const install = resolveReactorParentPomInstall({ rootPom: join(root, 'pom.xml'), revision: '2.0.0-dev' });
  assert.deepEqual(
    install.steps.map((step) => step.pomPath),
    [join(root, 'bom/pom.xml'), join(root, 'parent/pom.xml')],
  );
});

test('reactor parent install is skipped when the parent is a reactor module or unresolvable', () => {
  const root = mkdtempSync(join(tmpdir(), 'mango-parent-skip-'));
  mkdirSync(join(root, 'parent'), { recursive: true });
  writeFileSync(
    join(root, 'parent/pom.xml'),
    '<project><artifactId>fixture-parent</artifactId><version>${revision}</version><packaging>pom</packaging></project>',
  );
  writeFileSync(
    join(root, 'pom.xml'),
    '<project><parent><artifactId>fixture-parent</artifactId><version>${revision}</version><relativePath>parent/pom.xml</relativePath></parent><artifactId>fixture-root</artifactId><modules><module>parent</module></modules></project>',
  );
  assert.equal(resolveReactorParentPomInstall({ rootPom: join(root, 'pom.xml'), revision: '1.0.0' }), null);

  const plainRoot = mkdtempSync(join(tmpdir(), 'mango-parent-plain-'));
  writeFileSync(join(plainRoot, 'pom.xml'), '<project><artifactId>plain-root</artifactId></project>');
  assert.equal(resolveReactorParentPomInstall({ rootPom: join(plainRoot, 'pom.xml'), revision: '1.0.0' }), null);
});
