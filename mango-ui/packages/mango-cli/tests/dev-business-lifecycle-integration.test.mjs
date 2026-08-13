import assert from 'node:assert/strict';
import test from 'node:test';
import { existsSync, mkdtempSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { homedir, tmpdir } from 'node:os';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const packageRoot = join(dirname(fileURLToPath(import.meta.url)), '..');
const cli = join(packageRoot, 'src/index.mjs');

test(
  'business project supports the complete Mango workspace and dev lifecycle without Maven install',
  { timeout: 180_000 },
  async () => {
    const fixtureId = `business-${process.pid}-${Date.now()}`;
    const root = mkdtempSync(join(tmpdir(), 'mango-business-lifecycle-'));
    const registry = join(root, '.runtime/workspaces.json');
    const groupId = 'io.mango.cli.businessfixture';
    const revision = '1.0.0-SNAPSHOT';
    const libraryArtifactId = `${fixtureId}-library`;
    const appArtifactId = `${fixtureId}-app`;
    const localCoordinateRoot = join(homedir(), '.m2/repository/io/mango/cli/businessfixture');
    const librarySource = join(root, 'backend/library/src/main/java/io/mango/fixture');
    const appSource = join(root, 'backend/app/src/main/java/io/mango/fixture');
    const env = { ...process.env, MANGO_WORKSPACE_REGISTRY: registry };
    let appStarted = false;

    mkdirSync(librarySource, { recursive: true });
    mkdirSync(appSource, { recursive: true });
    writeFixtureProject();

    try {
      const initialized = runCli(['workspace', 'init']);
      assert.match(initialized.stdout, /Workspace slot 1:/u);
      const workspace = JSON.parse(readFileSync(join(root, '.mango/workspace.json'), 'utf8'));
      const appUrl = `http://127.0.0.1:${workspace.backendPort}`;
      disableDatabaseAutoCreate();

      const workspaceStatus = runCli(['workspace', 'status']);
      assert.match(workspaceStatus.stdout, new RegExp(`business-app.*port=${workspace.backendPort}`, 'u'));
      assert.match(workspaceStatus.stdout, new RegExp(`Workspace ID:\\s+${workspace.workspaceId}`, 'u'));

      const doctor = runCli(['dev', 'doctor']);
      assert.match(doctor.stdout, /business-app pom/u);
      assert.match(doctor.stdout, new RegExp(`business-app port ${workspace.backendPort} is free`, 'u'));

      const plan = runCli(['dev', 'plan', 'backend']);
      assert.match(plan.stdout, new RegExp(`-pl :${appArtifactId}`, 'u'));
      assert.match(plan.stdout, /-am/u);
      assert.match(plan.stdout, /-DskipTests/u);
      assert.match(plan.stdout, / clean compile org\.springframework\.boot:spring-boot-maven-plugin:3\.5\.14:run/u);
      assert.doesNotMatch(plan.stdout, /(?:^|\s)(?:package|install)(?:\s|$)/u);
      assert.doesNotMatch(plan.stdout, /(?:^|\s)-cp(?:\s|$)/u);

      const started = runCli(['dev', 'start', 'backend'], 120_000);
      appStarted = true;
      assert.match(started.stdout, /business-app: ready/u);
      assert.equal(await readBusinessValue(appUrl), 'BUSINESS_SOURCE_V1');

      const runningStatus = runCli(['dev', 'status']);
      assert.match(runningStatus.stdout, /running\s+business-app/u);
      assert.match(runningStatus.stdout, new RegExp(`http://127.0.0.1:${workspace.backendPort}`, 'u'));

      const firstLogs = runCli(['dev', 'logs', 'business-app']);
      assert.match(firstLogs.stdout, /BUSINESS_VALUE=BUSINESS_SOURCE_V1/u);

      writeLibrarySource('BUSINESS_SOURCE_V2');
      const restarted = runCli(['dev', 'restart', 'backend'], 120_000);
      assert.match(restarted.stdout, /business-app: stopped/u);
      assert.match(restarted.stdout, /business-app: ready/u);
      assert.equal(await readBusinessValue(appUrl), 'BUSINESS_SOURCE_V2');
      const restartedLogs = runCli(['dev', 'logs', 'business-app']);
      assert.match(restartedLogs.stdout, /BUSINESS_VALUE=BUSINESS_SOURCE_V2/u);

      const unknownLogs = runCli(['dev', 'logs', 'missing-app'], 30_000, false);
      assert.notEqual(unknownLogs.status, 0);
      assert.match(`${unknownLogs.stdout}\n${unknownLogs.stderr}`, /unknown app: missing-app/u);

      const stopped = runCli(['dev', 'stop', 'backend']);
      appStarted = false;
      assert.match(stopped.stdout, /business-app: stopped/u);
      const stoppedStatus = runCli(['dev', 'status']);
      assert.match(stoppedStatus.stdout, /stopped\s+business-app/u);

      assertNoWorkspaceArtifactsInstalled();
      const released = runCli(['workspace', 'release', '--workspace', root, '--keep-db']);
      assert.match(released.stdout, /Released Mango workspace registration/u);
      assert.equal(
        readRegistryEntries().some((entry) => entry.root === root),
        false,
      );
    } finally {
      if (appStarted) {
        runCli(['dev', 'stop', 'backend'], 30_000, false);
      }
      rmSync(root, { recursive: true, force: true });
    }

    function runCli(args, timeout = 30_000, requireSuccess = true) {
      const result = spawnSync(process.execPath, [cli, ...args], {
        cwd: root,
        env,
        encoding: 'utf8',
        timeout,
      });
      if (requireSuccess) {
        assert.equal(result.status, 0, `${args.join(' ')} failed:\n${result.stdout}\n${result.stderr}`);
      }
      return result;
    }

    function disableDatabaseAutoCreate() {
      const envPath = join(root, '.mango/dev-workspace.env');
      const text = readFileSync(envPath, 'utf8').replace('MANGO_DB_AUTO_CREATE=true', 'MANGO_DB_AUTO_CREATE=false');
      writeFileSync(envPath, text);
    }

    function assertNoWorkspaceArtifactsInstalled() {
      assert.equal(existsSync(join(localCoordinateRoot, libraryArtifactId)), false);
      assert.equal(existsSync(join(localCoordinateRoot, appArtifactId)), false);
    }

    function readRegistryEntries() {
      if (!existsSync(registry)) {
        return [];
      }
      return JSON.parse(readFileSync(registry, 'utf8'));
    }

    function writeFixtureProject() {
      writeFileSync(
        join(root, 'mango.dev.json'),
        `${JSON.stringify(
          {
            version: 1,
            groups: { default: ['business-app'], backend: ['business-app'] },
            apps: {
              'business-app': {
                type: 'spring-boot-maven',
                processMode: 'runtime',
                cwd: 'backend',
                pom: 'app/pom.xml',
                goal: 'org.springframework.boot:spring-boot-maven-plugin:3.5.14:run',
                portEnv: 'MANGO_BACKEND_PORT',
                port: 5555,
                health: '/actuator/health',
                waitTimeoutMs: 90000,
                args: ['--server.port=${port}'],
              },
            },
          },
          null,
          2,
        )}\n`,
      );
      writeFileSync(
        join(root, 'backend/pom.xml'),
        `<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><groupId>${groupId}</groupId><artifactId>${fixtureId}-root</artifactId><version>\${revision}</version><packaging>pom</packaging><properties><revision>${revision}</revision><maven.compiler.release>17</maven.compiler.release><spring-boot.version>3.5.14</spring-boot.version></properties><modules><module>library</module><module>app</module></modules><build><plugins><plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId><version>\${spring-boot.version}</version><configuration><skip>true</skip></configuration></plugin></plugins></build></project>`,
      );
      writeFileSync(
        join(root, 'backend/library/pom.xml'),
        `<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><parent><groupId>${groupId}</groupId><artifactId>${fixtureId}-root</artifactId><version>\${revision}</version></parent><artifactId>${libraryArtifactId}</artifactId></project>`,
      );
      writeFileSync(
        join(root, 'backend/app/pom.xml'),
        `<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion><parent><groupId>${groupId}</groupId><artifactId>${fixtureId}-root</artifactId><version>\${revision}</version></parent><artifactId>${appArtifactId}</artifactId><dependencies><dependency><groupId>${groupId}</groupId><artifactId>${libraryArtifactId}</artifactId><version>\${revision}</version></dependency></dependencies><build><plugins><plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId><version>\${spring-boot.version}</version><configuration><skip>false</skip><mainClass>io.mango.fixture.BusinessApplication</mainClass></configuration></plugin></plugins></build></project>`,
      );
      writeLibrarySource('BUSINESS_SOURCE_V1');
      writeFileSync(
        join(appSource, 'BusinessApplication.java'),
        `package io.mango.fixture;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
public final class BusinessApplication {
  private BusinessApplication() {}
  public static void main(String[] args) throws Exception {
    int port = 5555;
    for (String arg : args) if (arg.startsWith("--server.port=")) port = Integer.parseInt(arg.substring(14));
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
    server.createContext("/actuator/health", exchange -> respond(exchange, "{\\"status\\":\\"UP\\"}"));
    server.createContext("/business/value", exchange -> respond(exchange, BusinessLibrary.value()));
    server.start();
    System.out.println("BUSINESS_VALUE=" + BusinessLibrary.value());
  }
  private static void respond(com.sun.net.httpserver.HttpExchange exchange, String body) throws java.io.IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.sendResponseHeaders(200, bytes.length);
    exchange.getResponseBody().write(bytes);
    exchange.close();
  }
}
`,
      );
    }

    function writeLibrarySource(value) {
      writeFileSync(
        join(librarySource, 'BusinessLibrary.java'),
        `package io.mango.fixture; public final class BusinessLibrary { private BusinessLibrary() {} public static String value() { return "${value}"; } }\n`,
      );
    }
  },
);

async function readBusinessValue(appUrl) {
  const response = await fetch(`${appUrl}/business/value`);
  assert.equal(response.status, 200);
  return response.text();
}
