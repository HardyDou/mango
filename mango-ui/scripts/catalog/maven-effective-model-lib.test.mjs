import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { exportMavenInventory, indexMavenModulePaths } from './maven-effective-model-lib.mjs';

test('exports resolved coordinates, dependency management, scope and optional from the Effective Model', () => {
  const modulePaths = new Map([
    ['mango-system-starter', 'mango-platform/mango-system/mango-system-starter'],
    ['mango-system-core', 'mango-platform/mango-system/mango-system-core'],
    ['mango-bom', 'mango-bom'],
    ['mango-monolith-app', 'mango-app/monolith/mango-monolith-app'],
    ['mango-infra-test', 'mango-infra/mango-infra-test'],
  ]);
  const inventory = exportMavenInventory(EFFECTIVE_POM, modulePaths);
  assert.equal(inventory.reactorModuleCount, 5);
  assert.equal(inventory.publishableCoordinateCount, 3);
  assert.equal(inventory.excludedCoordinateCount, 2);
  assert.deepEqual(inventory.excludedCoordinates.map((entry) => entry.reason).sort(), [
    'deployment-app',
    'internal-test-module',
  ]);
  const starter = inventory.publishableCoordinates.find((entry) => entry.artifactId === 'mango-system-starter');
  assert.equal(starter.version, '1.2.3');
  assert.deepEqual(starter.dependencies[0], {
    groupId: 'io.mango.platform.system',
    artifactId: 'mango-system-core',
    version: '1.2.3',
    type: 'jar',
    classifier: null,
    scope: 'runtime',
    optional: true,
  });
  const managed = inventory.dependencyManagementSets.find(
    (entry) => entry.digest === starter.dependencyManagementDigest,
  ).dependencies[0];
  assert.equal(managed.artifactId, 'mango-bom');
  assert.equal(managed.type, 'pom');
  assert.equal(managed.scope, 'import');
});

test('fails closed for unresolved revision and unsupported packaging', () => {
  const unresolved = EFFECTIVE_POM.replaceAll('1.2.3', '${revision}');
  assert.throws(
    () => exportMavenInventory(unresolved, new Map([['mango-system-starter', 'starter']])),
    /is unresolved/u,
  );
  const unsupported = EFFECTIVE_POM.replace('<packaging>jar</packaging>', '<packaging>war</packaging>');
  assert.throws(
    () => exportMavenInventory(unsupported, new Map([['mango-system-starter', 'starter']])),
    /unsupported packaging war/u,
  );
});

test('indexes real Reactor module paths and rejects duplicate artifactIds', () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-maven-index-'));
  writePom(path.join(root, 'pom.xml'), 'root');
  writePom(path.join(root, 'module/pom.xml'), 'module');
  assert.deepEqual(
    [...indexMavenModulePaths(root)],
    [
      ['module', 'module'],
      ['root', '.'],
    ],
  );
  writePom(path.join(root, 'duplicate/pom.xml'), 'module');
  assert.throws(() => indexMavenModulePaths(root), /duplicate Maven reactor artifactId module/u);
});

function writePom(file, artifactId) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(
    file,
    `<project><modelVersion>4.0.0</modelVersion><groupId>io.mango</groupId><artifactId>${artifactId}</artifactId><version>1</version></project>\n`,
  );
}

const EFFECTIVE_POM = `
<projects>
  <project>
    <groupId>io.mango.platform.system</groupId>
    <artifactId>mango-system-starter</artifactId>
    <version>1.2.3</version>
    <packaging>jar</packaging>
    <dependencyManagement>
      <dependencies>
        <dependency>
          <groupId>io.mango</groupId>
          <artifactId>mango-bom</artifactId>
          <version>1.2.3</version>
          <type>pom</type>
          <scope>import</scope>
        </dependency>
      </dependencies>
    </dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>io.mango.platform.system</groupId>
        <artifactId>mango-system-core</artifactId>
        <version>1.2.3</version>
        <scope>runtime</scope>
        <optional>true</optional>
      </dependency>
    </dependencies>
  </project>
  <project>
    <groupId>io.mango.platform.system</groupId>
    <artifactId>mango-system-core</artifactId>
    <version>1.2.3</version>
    <packaging>jar</packaging>
  </project>
  <project>
    <groupId>io.mango</groupId>
    <artifactId>mango-bom</artifactId>
    <version>1.2.3</version>
    <packaging>pom</packaging>
  </project>
  <project>
    <groupId>io.mango.app</groupId>
    <artifactId>mango-monolith-app</artifactId>
    <version>1.2.3</version>
    <packaging>jar</packaging>
  </project>
  <project>
    <groupId>io.mango.infra</groupId>
    <artifactId>mango-infra-test</artifactId>
    <version>1.2.3</version>
    <packaging>jar</packaging>
  </project>
</projects>
`;
