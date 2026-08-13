import assert from 'node:assert/strict';
import test from 'node:test';
import { existsSync, mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { homedir, tmpdir } from 'node:os';
import { join } from 'node:path';
import { spawnSync } from 'node:child_process';
import { buildSpringBootReactorArgs, resolveSpringBootMavenReactor } from '../src/dev-maven-reactor.mjs';

test(
  'real Maven reactor runs newly compiled upstream code without installing workspace artifacts',
  { timeout: 120_000 },
  () => {
    const fixtureId = `fixture-${process.pid}-${Date.now()}`;
    const root = mkdtempSync(join(tmpdir(), 'mango-real-reactor-'));
    const groupId = 'io.mango.cli.reactorfixture';
    const revision = '1.0.0-SNAPSHOT';
    const libraryArtifactId = `${fixtureId}-library`;
    const appArtifactId = `${fixtureId}-app`;
    const localCoordinateRoot = join(homedir(), '.m2/repository/io/mango/cli/reactorfixture');
    const librarySource = join(root, 'library/src/main/java/io/mango/fixture');
    const appSource = join(root, 'app/src/main/java/io/mango/fixture');
    mkdirSync(librarySource, { recursive: true });
    mkdirSync(appSource, { recursive: true });

    writeFileSync(
      join(root, 'pom.xml'),
      `<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><groupId>${groupId}</groupId><artifactId>${fixtureId}-root</artifactId><version>\${revision}</version><packaging>pom</packaging><properties><revision>${revision}</revision><maven.compiler.release>17</maven.compiler.release><spring-boot.version>3.5.14</spring-boot.version></properties><modules><module>library</module><module>app</module></modules><build><plugins><plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId><version>\${spring-boot.version}</version><configuration><skip>true</skip></configuration></plugin></plugins></build></project>`,
    );
    writeFileSync(
      join(root, 'library/pom.xml'),
      `<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><parent><groupId>${groupId}</groupId><artifactId>${fixtureId}-root</artifactId><version>\${revision}</version></parent><artifactId>${libraryArtifactId}</artifactId></project>`,
    );
    writeFileSync(
      join(root, 'app/pom.xml'),
      `<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><parent><groupId>${groupId}</groupId><artifactId>${fixtureId}-root</artifactId><version>\${revision}</version></parent><artifactId>${appArtifactId}</artifactId><dependencies><dependency><groupId>${groupId}</groupId><artifactId>${libraryArtifactId}</artifactId><version>\${revision}</version></dependency></dependencies><build><plugins><plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId><version>\${spring-boot.version}</version><configuration><skip>false</skip><mainClass>io.mango.fixture.FixtureApplication</mainClass></configuration></plugin></plugins></build></project>`,
    );
    writeFileSync(
      join(librarySource, 'FixtureLibrary.java'),
      'package io.mango.fixture; public final class FixtureLibrary { private FixtureLibrary() {} public static String value() { return "REACTOR_SOURCE_V2"; } }\n',
    );
    writeFileSync(
      join(appSource, 'FixtureApplication.java'),
      'package io.mango.fixture; public final class FixtureApplication { private FixtureApplication() {} public static void main(String[] args) { System.out.println(FixtureLibrary.value()); } }\n',
    );

    try {
      const reactor = resolveSpringBootMavenReactor({ workspaceRoot: root, appPomPath: join(root, 'app/pom.xml') });
      const args = buildSpringBootReactorArgs({
        rootPom: reactor.rootPom,
        selector: reactor.selector,
        revision,
        springArgs: [],
        goal: 'org.springframework.boot:spring-boot-maven-plugin:3.5.14:run',
      });
      const result = spawnSync('mvn', ['-q', ...args], { cwd: reactor.cwd, encoding: 'utf8', timeout: 110_000 });
      assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`);
      assert.match(`${result.stdout}\n${result.stderr}`, /REACTOR_SOURCE_V2/u);
      assert.equal(existsSync(join(localCoordinateRoot, libraryArtifactId)), false);
      assert.equal(existsSync(join(localCoordinateRoot, appArtifactId)), false);
    } finally {
      rmSync(root, { recursive: true, force: true });
    }
  },
);
