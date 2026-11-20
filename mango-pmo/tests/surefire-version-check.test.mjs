import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const testDir = path.dirname(fileURLToPath(import.meta.url));
const checker = path.resolve(testDir, '../tools/check-surefire-versions.mjs');

const ROOT_POM = `<?xml version="1.0" encoding="UTF-8"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>io.mango</groupId>
  <artifactId>mango</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>pom</packaging>
  <properties>
    <maven-surefire-plugin.version>3.2.5</maven-surefire-plugin.version>
  </properties>
  <modules>
    <module>child</module>
    <module>legacy</module>
    <module>bom</module>
  </modules>
  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-surefire-plugin</artifactId>
          <version>\${maven-surefire-plugin.version}</version>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-checkstyle-plugin</artifactId>
          <version>3.5.0</version>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>
</project>
`;

const CHILD_POM = `<?xml version="1.0" encoding="UTF-8"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>io.mango</groupId>
    <artifactId>mango</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
  </parent>
  <artifactId>child</artifactId>
</project>
`;

const LEGACY_POM = `<?xml version="1.0" encoding="UTF-8"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>io.mango</groupId>
    <artifactId>mango</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
  </parent>
  <artifactId>legacy</artifactId>
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>2.17</version>
      </plugin>
    </plugins>
  </build>
</project>
`;

const BOM_POM = `<?xml version="1.0" encoding="UTF-8"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>io.mango</groupId>
    <artifactId>mango</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath>../pom.xml</relativePath>
  </parent>
  <artifactId>bom</artifactId>
  <packaging>pom</packaging>
</project>
`;

function write(root, relativePath, source) {
  const target = path.join(root, relativePath);
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.writeFileSync(target, source, 'utf8');
}

function dropModule(root, name) {
  fs.rmSync(path.join(root, 'mango', name), { recursive: true, force: true });
  const rootPomPath = path.join(root, 'mango/pom.xml');
  const rootPom = fs.readFileSync(rootPomPath, 'utf8')
    .replace(`    <module>${name}</module>\n`, '');
  fs.writeFileSync(rootPomPath, rootPom, 'utf8');
}

function addModule(root, name, pom, withTests) {
  write(root, `mango/${name}/pom.xml`, pom);
  if (withTests) {
    write(root, `mango/${name}/src/test/java/example/${name}Test.java`,
      `class ${name}Test {\n  void example() {\n  }\n}\n`);
  }
  const rootPomPath = path.join(root, 'mango/pom.xml');
  const rootPom = fs.readFileSync(rootPomPath, 'utf8')
    .replace('  <modules>\n', `  <modules>\n    <module>${name}</module>\n`);
  fs.writeFileSync(rootPomPath, rootPom, 'utf8');
}

function createRepository() {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-surefire-'));
  write(root, 'mango/pom.xml', ROOT_POM);
  write(root, 'mango/child/pom.xml', CHILD_POM);
  write(root, 'mango/child/src/test/java/example/ChildTest.java',
    'class ChildTest {\n  void example() {\n  }\n}\n');
  write(root, 'mango/legacy/pom.xml', LEGACY_POM);
  write(root, 'mango/bom/pom.xml', BOM_POM);
  return root;
}

function run(root) {
  const result = spawnSync(process.execPath, [checker, '--root', root, '--json'], {
    encoding: 'utf8'
  });
  assert.equal(result.status === 0 || result.status === 1, true, result.stderr);
  return { status: result.status, report: JSON.parse(result.stdout) };
}

test('accepts a reactor whose modules inherit the root pluginManagement version', () => {
  const root = createRepository();
  try {
    dropModule(root, 'legacy');
    const result = run(root);
    assert.deepEqual(result.report.failures, []);
    assert.equal(result.status, 0);
    assert.deepEqual(result.report.summary.versions, { '3.2.5': 3 });
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('rejects a module that pins its own Surefire version', () => {
  const root = createRepository();
  try {
    const result = run(root);
    assert.equal(result.status, 1);
    assert.equal(result.report.failures.length, 2);
    assert.ok(result.report.failures.some((failure) => failure.includes('mango/legacy/pom.xml 本地声明了')));
    assert.ok(result.report.failures.some((failure) => failure.includes('解析到 Surefire 2.17，低于 3.0.0')));
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('fails a module with tests but no resolvable Surefire version', () => {
  const root = createRepository();
  try {
    dropModule(root, 'legacy');
    addModule(root, 'orphan', `<?xml version="1.0" encoding="UTF-8"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>io.mango</groupId>
  <artifactId>orphan</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</project>
`, true);
    const result = run(root);
    assert.equal(result.status, 1);
    assert.ok(result.report.failures.some((failure) =>
      failure.includes('mango/orphan/pom.xml 未解析到 Surefire 版本')));
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('exempts pom-packaging modules without tests and no version', () => {
  const root = createRepository();
  try {
    dropModule(root, 'legacy');
    addModule(root, 'aggregator', `<?xml version="1.0" encoding="UTF-8"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>io.mango</groupId>
  <artifactId>aggregator</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>pom</packaging>
</project>
`, false);
    const result = run(root);
    assert.deepEqual(result.report.failures, []);
    assert.equal(result.report.summary.exempt, 1);
    assert.deepEqual(result.report.summary.versions, { '3.2.5': 3 });
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('ignores POMs outside the reactor graph', () => {
  const root = createRepository();
  try {
    dropModule(root, 'legacy');
    write(root, 'mango/child/src/it/fixture/pom.xml', `<?xml version="1.0" encoding="UTF-8"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>io.mango.it</groupId>
  <artifactId>fixture</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</project>
`);
    const result = run(root);
    assert.deepEqual(result.report.failures, []);
    assert.equal(result.report.summary.modules, 3);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('resolves parent versions through GAV when relativePath is omitted', () => {
  const root = createRepository();
  try {
    dropModule(root, 'legacy');
    write(root, 'mango/child/pom.xml', `<?xml version="1.0" encoding="UTF-8"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>io.mango</groupId>
    <artifactId>mango</artifactId>
    <version>1.0.0-SNAPSHOT</version>
  </parent>
  <artifactId>child</artifactId>
</project>
`);
    const result = run(root);
    assert.deepEqual(result.report.failures, []);
    assert.deepEqual(result.report.summary.versions, { '3.2.5': 3 });
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('reports a missing root POM instead of passing silently', () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-surefire-empty-'));
  try {
    const result = run(root);
    assert.equal(result.status, 1);
    assert.match(result.report.failures[0], /mango\/pom\.xml 不存在/);
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});

test('resolves a parent POM that is not part of the reactor modules', () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-surefire-detached-parent-'));
  try {
    write(root, 'mango/pom.xml', `<?xml version="1.0" encoding="UTF-8"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>io.mango</groupId>
  <artifactId>mango</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>pom</packaging>
  <modules>
    <module>child</module>
  </modules>
</project>
`);
    // 父 POM 在磁盘上，但不在 <modules> 里（对应 mango-parent 退出根聚合的情形）。
    write(root, 'mango/mango-parent/pom.xml', `<?xml version="1.0" encoding="UTF-8"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>io.mango</groupId>
  <artifactId>mango-parent</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>pom</packaging>
  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-surefire-plugin</artifactId>
          <version>3.2.5</version>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>
</project>
`);
    write(root, 'mango/child/pom.xml', `<?xml version="1.0" encoding="UTF-8"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>io.mango</groupId>
    <artifactId>mango-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath>../mango-parent/pom.xml</relativePath>
  </parent>
  <artifactId>child</artifactId>
</project>
`);
    const result = run(root);
    assert.deepEqual(result.report.failures, []);
    assert.deepEqual(result.report.summary.versions, { '3.2.5': 1 });
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
});
