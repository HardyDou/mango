#!/usr/bin/env node
import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, mkdtempSync, readdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { homedir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { assertPnpmLockfileFixtureInvocations, createPnpmLockfileFixture } from './support/pnpm-lockfile-fixture.mjs';

const packageRoot = resolve(fileURLToPath(new URL('..', import.meta.url)));
const cli = join(packageRoot, 'src/index.mjs');
const releaseVersions = JSON.parse(readFileSync(join(packageRoot, 'release-versions.json'), 'utf8'));
const mangoVersion = process.env.MANGO_BACKEND_GATE_VERSION || releaseVersions.maven.mangoBackend;
const mangoPluginVersion = process.env.MANGO_BACKEND_GATE_PLUGIN_VERSION || mangoVersion;
const runtimeProjectsRoot = resolve(packageRoot, '../../../.runtime/projects');
mkdirSync(runtimeProjectsRoot, { recursive: true });
const tempRoot = mkdtempSync(join(runtimeProjectsRoot, 'mango-generated-backend-gate-'));
const localMangoRepository = join(tempRoot, 'mango-repository');
mkdirSync(localMangoRepository, { recursive: true });
const mavenRepository = process.env.MANGO_BACKEND_GATE_REPOSITORY || `${pathToFileURL(localMangoRepository).href}/`;
const mavenLocalRepository = process.env.MANGO_BACKEND_GATE_LOCAL_REPOSITORY || join(homedir(), '.m2/repository');
const projectName = 'mango-backend-gate-acceptance';
const MAX_MAVEN_INVOCATIONS = 9;
let mavenInvocationCount = 0;
const projectRoot = join(tempRoot, projectName);
const starterJavaRoot = join(
  projectRoot,
  'backend/modules/order/order-starter/src/main/java/com/example/backendgate/order/starter',
);
const coreJavaRoot = join(
  projectRoot,
  'backend/modules/order/order-core/src/main/java/com/example/backendgate/order/core',
);
const migrationPath = join(
  projectRoot,
  'backend/modules/order/order-core/src/main/resources/db/migration/order/V1__init_order.sql',
);
const invalidApiModuleInfo = join(
  projectRoot,
  'backend/modules/order/order-api/src/main/resources/META-INF/mango/module.properties',
);
const invalidApiResources = join(projectRoot, 'backend/modules/order/order-api/src/main/resources');
const globalEntityManifest = join(projectRoot, 'business-pmo/global-entity-exceptions.json');
const architectureDebtBudget = join(projectRoot, 'business-pmo/architecture-debt-budget.json');
const githubGovernanceWorkflow = join(projectRoot, '.github/workflows/pmo-doc-check.yml');
const orderApiPom = join(projectRoot, 'backend/modules/order/order-api/pom.xml');
const architectureVerificationPom = join(projectRoot, 'backend/architecture-verification/pom.xml');
const globalReferenceEntity = join(coreJavaRoot, 'entity/GlobalReferenceEntity.java');
const summaryPath = process.env.MANGO_BACKEND_GATE_SUMMARY_FILE
  ? resolve(process.env.MANGO_BACKEND_GATE_SUMMARY_FILE)
  : '';
let cleanQualityEvidence;
const negativeControls = {
  missingStaticReportRejected: false,
  architectureViolationsRejected: false,
  reservedNamespaceRejected: false,
  checkstyleViolationRejected: false,
  mangoCheckViolationsRejected: false,
  missingEntityManifestRejected: false,
  architectureBypassRejected: false,
};

try {
  runNode(
    [
      cli,
      'init',
      projectName,
      '--preset',
      'custom',
      '--modules',
      'none',
      '--package',
      'com.example.backendgate',
      '--group-id',
      'com.example',
      '--mango-version',
      mangoVersion,
      '--maven-repository',
      mavenRepository,
    ],
    tempRoot,
    'generate acceptance project',
  );
  const moduleAddPnpmFixture = createPnpmLockfileFixture(join(tempRoot, 'module-add-lockfile'), [
    'packages/order',
    'packages/order-api',
  ]);
  runNode(
    [
      cli,
      'module',
      'add',
      'order',
      '--aggregate',
      'sales-order',
      '--aggregate-name',
      '销售订单',
      '--module-name',
      '订单模块',
      '--project-dir',
      '.',
    ],
    projectRoot,
    'generate four-layer business module',
    moduleAddPnpmFixture.env,
  );
  assertPnpmLockfileFixtureInvocations(moduleAddPnpmFixture.logPath);
  configureMangoPluginVersion();
  addApprovedGlobalEntityFixture();
  assertGeneratedPolicyContract();
  const projectBudget = readFileSync(architectureDebtBudget, 'utf8');
  const managedWorkflow = readFileSync(githubGovernanceWorkflow, 'utf8');
  const legacyWorkflow = managedWorkflow.replace(/^# Managed by Mango CLI\.[^\n]*\n/u, '');
  if (legacyWorkflow === managedWorkflow) {
    throw new Error('generated GitHub governance workflow is missing its Mango ownership marker');
  }
  rmSync(architectureDebtBudget);
  writeFileSync(githubGovernanceWorkflow, legacyWorkflow);
  runGit(['init', '-q'], 'initialize generated legacy project Git repository');
  runGit(['add', '.'], 'stage generated legacy project base');
  runGit(['commit', '-qm', 'generated legacy project base'], 'commit generated legacy project base');
  writeFileSync(githubGovernanceWorkflow, managedWorkflow);

  runMaven(
    ['-Dmango.architecture.inventoryOnly=true', 'install'],
    true,
    'generated executable backend install and full-Reactor inventory-only governance path',
  );
  cleanQualityEvidence = assertPassingReports();
  assertInventoryOnlyReport();
  assertFlattenedInstalledPoms();
  assertExecutableBootArtifact();
  runExternalConsumer();
  runNode(
    [
      'business-pmo/mango-baseline/tools/check-architecture-debt-budget.mjs',
      '--report',
      'backend/target/mango-architecture-report.json',
      '--baseline',
      'business-pmo/architecture-debt-budget.json',
      '--base-ref',
      'HEAD',
      '--allow-missing-for-governance-upgrade',
    ],
    projectRoot,
    'generated legacy project PMO/workflow upgrade transition',
  );
  writeFileSync(architectureDebtBudget, projectBudget);
  runNode(
    [
      'business-pmo/mango-baseline/tools/check-architecture-debt-budget.mjs',
      '--report',
      'backend/target/mango-architecture-report.json',
      '--baseline',
      'business-pmo/architecture-debt-budget.json',
    ],
    projectRoot,
    'generated project architecture debt budget',
  );

  const originalOrderApiPom = readFileSync(orderApiPom, 'utf8');
  const pmdSkippedOrderApiPom = originalOrderApiPom.replace(
    '</project>',
    `    <properties>\n        <pmd.skip>true</pmd.skip>\n    </properties>\n</project>`,
  );
  if (pmdSkippedOrderApiPom === originalOrderApiPom) {
    throw new Error('unable to inject child-module PMD skip acceptance fixture');
  }
  writeFileSync(orderApiPom, pmdSkippedOrderApiPom);
  rmSync(join(projectRoot, 'backend/modules/order/order-api/target'), {
    recursive: true,
    force: true,
  });
  const missingChildReportFailure = runMaven(['verify'], false, 'missing child-module PMD report');
  assertIncludes(
    missingChildReportFailure,
    'Static analysis missing pmd report for Java Reactor module(s)',
    'full-scope static report coverage failure',
  );
  assertIncludes(missingChildReportFailure, 'order-api', 'missing PMD report module identity');
  negativeControls.missingStaticReportRejected = true;
  writeFileSync(orderApiPom, originalOrderApiPom);

  const badController = join(starterJavaRoot, 'BadController.java');
  writeFileSync(
    badController,
    `package com.example.backendgate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BadController {

    @GetMapping("/bad/{id}")
    public String detail(@PathVariable Long id) {
        return String.valueOf(id);
    }
}
`,
  );
  const suppressedServiceContract = join(coreJavaRoot, 'service/ISuppressedArchitectureService.java');
  writeFileSync(
    suppressedServiceContract,
    `package com.example.backendgate.order.core.service;

public interface ISuppressedArchitectureService {

    String ping();
}
`,
  );
  const suppressedService = join(coreJavaRoot, 'service/impl/SuppressedArchitectureService.java');
  mkdirSync(dirname(suppressedService), { recursive: true });
  writeFileSync(
    suppressedService,
    `package com.example.backendgate.order.core.service.impl;

import com.example.backendgate.order.core.service.ISuppressedArchitectureService;
import io.mango.common.result.R;

@SuppressWarnings("PMD.MangoJavaArchitecture")
public final class SuppressedArchitectureService implements ISuppressedArchitectureService {

    @Override
    public String ping() {
        return "ok";
    }

    public R<String> leakResult() {
        return null;
    }
}
`,
  );
  const directService = join(coreJavaRoot, 'service/impl/DirectServiceImpl.java');
  writeFileSync(
    directService,
    `package com.example.backendgate.order.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backendgate.order.core.entity.SalesOrderEntity;
import com.example.backendgate.order.core.mapper.SalesOrderMapper;
import org.springframework.stereotype.Service;

@Service
public class DirectServiceImpl extends ServiceImpl<SalesOrderMapper, SalesOrderEntity> {
}
`,
  );
  const unregisteredGlobalEntity = join(coreJavaRoot, 'entity/UnregisteredGlobalEntity.java');
  writeFileSync(unregisteredGlobalEntity, globalEntitySource('UnregisteredGlobalEntity', 'order_unregistered_global'));
  const approvedMigration = readFileSync(migrationPath, 'utf8');
  writeFileSync(migrationPath, `${approvedMigration}${globalEntityTableSql('order_unregistered_global')}`);
  writeFileSync(globalEntityManifest, globalEntityManifestJson('wrong_global_table'));

  const architectureFailure = runMaven(['verify'], false, 'combined architecture violations');
  assertIncludes(architectureFailure, 'Mango architecture gate found', 'architecture failure');
  assertArchitectureModuleOwnership(readJson(join(projectRoot, 'backend/target/mango-architecture-report.json')));
  for (const ruleId of [
    'MANGO-ARCH-PATH-001',
    'MANGO-ARCH-PATH-002',
    'MANGO-ARCH-SVC-001',
    'MANGO-ARCH-SVC-014',
    'MANGO-ARCH-ENTITY-003',
    'MANGO-ARCH-ENTITY-004',
  ]) {
    assertArchitectureRule(ruleId, architectureFailure);
  }
  negativeControls.architectureViolationsRejected = true;
  for (const source of [
    badController,
    suppressedService,
    suppressedServiceContract,
    directService,
    unregisteredGlobalEntity,
  ]) {
    removeJavaFixture(source);
  }
  writeFileSync(migrationPath, approvedMigration);
  writeFileSync(globalEntityManifest, globalEntityManifestJson('order_global_reference'));

  const reservedNamespaceSource = join(
    projectRoot,
    'backend/modules/order/order-api/src/main/java/io/mango/common/result/BusinessShadow.java',
  );
  mkdirSync(dirname(reservedNamespaceSource), { recursive: true });
  writeFileSync(
    reservedNamespaceSource,
    `package io.mango.common.result;

public final class BusinessShadow {
}
`,
  );
  const namespaceFailure = runMaven(['verify'], false, 'reserved namespace shadow');
  assertIncludes(namespaceFailure, 'MANGO-ARCH-ENGINE-017', 'reserved namespace failure');
  negativeControls.reservedNamespaceRejected = true;
  removeJavaFixture(reservedNamespaceSource);

  const staticViolation = join(starterJavaRoot, 'StaticViolation.java');
  writeFileSync(
    staticViolation,
    `package com.example.backendgate;

public final class StaticViolation {

\tpublic int value() {
        return 42;
    }
}
`,
  );
  const staticFailure = runMaven(['verify'], false, 'generic Java style violation');
  assertStaticAnalysisFailure('checkstyle', staticFailure);
  negativeControls.checkstyleViolationRejected = true;
  removeJavaFixture(staticViolation);

  const originalMigration = readFileSync(migrationPath, 'utf8');
  const missingTenantMigration = originalMigration.replace('    tenant_id VARCHAR(64) NULL,\n', '');
  if (missingTenantMigration === originalMigration) {
    throw new Error('generated migration does not contain the expected tenant_id declaration');
  }
  writeFileSync(migrationPath, missingTenantMigration);
  mkdirSync(dirname(invalidApiModuleInfo), { recursive: true });
  writeFileSync(invalidApiModuleInfo, 'module-name=order\nmodule-path=order\n');
  runMaven(['verify'], false, 'combined project-quality violations');
  assertMangoCheckRule('PERSISTENCE_SCHEMA');
  assertMangoCheckRule('MODULE_INFO');
  negativeControls.mangoCheckViolationsRejected = true;
  writeFileSync(migrationPath, originalMigration);
  rmSync(invalidApiResources, { recursive: true, force: true });

  const originalManifest = readFileSync(globalEntityManifest, 'utf8');
  rmSync(globalEntityManifest);
  const manifestFailure = runMaven(['verify'], false, 'missing global entity manifest');
  assertIncludes(manifestFailure, 'MANGO-ARCH-ENGINE-014', 'missing global entity manifest failure');
  negativeControls.missingEntityManifestRejected = true;
  writeFileSync(globalEntityManifest, originalManifest);

  const architecturePolicyFailure = runMaven(
    [
      '-pl',
      'architecture-verification',
      '-Dmango.architecture.skip=true',
      '-Denforcer.skip=true',
      `io.mango.tools.maven.plugin:mango-maven-plugin:${mangoVersion}:architecture`,
    ],
    false,
    'mango.architecture.skip override',
  );
  assertIncludes(architecturePolicyFailure, 'MANGO-ARCH-ENGINE-015', 'mango.architecture.skip override failure');
  negativeControls.architectureBypassRejected = true;
  assertMavenInvocationBudget();

  if (summaryPath) {
    mkdirSync(dirname(summaryPath), { recursive: true });
    writeFileSync(summaryPath, `${JSON.stringify({
      schemaVersion: 1,
      templateId: 'business-module@1',
      mangoVersion,
      mavenInvocationCount,
      cleanQualityEvidence,
      negativeControls,
    }, null, 2)}\n`);
  }

  process.stdout.write(
    `Generated backend gate PASS with Mango ${mangoVersion} in ${mavenInvocationCount} Maven invocations: ` +
      'clean project install and executable Boot JAR accepted; Mango Start-Class reached through java -jar; ' +
      'flattened installed POMs and reactor-external consumer resolved; ' +
      'PathVariable, direct ServiceImpl, tenant schema, module info, Checkstyle, manifest, ' +
      'PMD suppression, reserved namespace shadowing, ' +
      'module-aware architecture report ownership, ' +
      'full-Reactor inventory-only project budget verification, ' +
      'missing-budget legacy workflow migration, ' +
      'per-Java-module static report coverage, ' +
      'unregistered/mismatched global Entity cases, approved global Entity acceptance, ' +
      'generated policy wiring locked, and representative ' +
      'architecture policy bypass rejected.\n',
  );
} finally {
  if (process.env.MANGO_BACKEND_GATE_KEEP_TEMP === 'true') {
    process.stdout.write(`Generated backend gate diagnostics retained at ${tempRoot}\n`);
  } else {
    rmSync(tempRoot, { recursive: true, force: true });
  }
}

function runNode(args, cwd, label, env = process.env) {
  const result = spawnSync(process.execPath, args, {
    cwd,
    encoding: 'utf8',
    maxBuffer: 20 * 1024 * 1024,
    env,
  });
  if (result.status !== 0) {
    throw new Error(`${label} failed:\n${combinedOutput(result)}`);
  }
}

function configureMangoPluginVersion() {
  if (mangoPluginVersion === mangoVersion) return;
  const original = readFileSync(architectureVerificationPom, 'utf8');
  const marker = '<artifactId>mango-maven-plugin</artifactId>\n                <version>${mango.version}</version>';
  const replacement = `<artifactId>mango-maven-plugin</artifactId>\n                <version>${mangoPluginVersion}</version>`;
  const configured = original.replace(marker, replacement);
  if (configured === original) {
    throw new Error('unable to configure workspace-qualified Mango Maven plugin version');
  }
  writeFileSync(architectureVerificationPom, configured);
}

function addApprovedGlobalEntityFixture() {
  writeFileSync(globalReferenceEntity, globalEntitySource('GlobalReferenceEntity', 'order_global_reference'));
  writeFileSync(
    migrationPath,
    `${readFileSync(migrationPath, 'utf8')}${globalEntityTableSql('order_global_reference')}`,
  );
  writeFileSync(globalEntityManifest, globalEntityManifestJson('order_global_reference'));
}

function assertFlattenedInstalledPoms() {
  const coordinates = [
    ['com/example', `${projectName}-backend`],
    ['com/example', 'order-starter'],
  ];
  for (const [groupPath, artifactId] of coordinates) {
    const pomPath = join(
      mavenLocalRepository,
      groupPath,
      artifactId,
      '1.0.0-SNAPSHOT',
      `${artifactId}-1.0.0-SNAPSHOT.pom`,
    );
    if (!existsSync(pomPath)) {
      throw new Error(`generated backend install did not create ${pomPath}`);
    }
    const pom = readFileSync(pomPath, 'utf8');
    if (pom.includes('${revision}')) {
      throw new Error(`installed POM still contains unresolved ci-friendly revision: ${pomPath}`);
    }
  }
}

function assertExecutableBootArtifact() {
  const target = join(projectRoot, 'backend/app/target');
  const jars = readdirSync(target).filter((name) => name.endsWith('.jar') && !name.endsWith('.jar.original'));
  if (jars.length !== 1) {
    throw new Error(`generated backend package must produce one primary app JAR: ${jars.join(', ')}`);
  }
  const jarPath = join(target, jars[0]);
  const listed = spawnSync('jar', ['tf', jarPath], {
    encoding: 'utf8',
    maxBuffer: 20 * 1024 * 1024,
  });
  if (listed.status !== 0) {
    throw new Error(`unable to inspect generated Boot JAR:\n${combinedOutput(listed)}`);
  }
  for (const entry of [
    'BOOT-INF/classes/com/example/backendgate/MangoBackendGateAcceptanceApplication.class',
    'BOOT-INF/lib/',
    'org/springframework/boot/loader/launch/JarLauncher.class',
  ]) {
    assertIncludes(listed.stdout, entry, 'generated executable Boot JAR');
  }
  const launched = spawnSync('java', ['-jar', jarPath], {
    cwd: projectRoot,
    encoding: 'utf8',
    maxBuffer: 20 * 1024 * 1024,
    timeout: 20_000,
  });
  if (launched.status === 0 || launched.error?.code === 'ETIMEDOUT') {
    throw new Error('generated Boot JAR must reach MangoApplication and reject a missing process mode');
  }
  assertIncludes(
    combinedOutput(launched),
    'Mango process mode is required: bootstrap or runtime',
    'generated Boot JAR Start-Class',
  );
}

function runExternalConsumer() {
  const consumerRoot = join(tempRoot, 'reactor-external-consumer');
  const javaPath = join(consumerRoot, 'src/main/java/com/example/consumer/OrderConsumer.java');
  mkdirSync(dirname(javaPath), { recursive: true });
  writeFileSync(
    join(consumerRoot, 'pom.xml'),
    `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example.consumer</groupId>
  <artifactId>order-consumer</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <properties>
    <maven.compiler.release>21</maven.compiler.release>
  </properties>
  <dependencies>
    <dependency>
      <groupId>com.example</groupId>
      <artifactId>order-starter</artifactId>
      <version>1.0.0-SNAPSHOT</version>
    </dependency>
  </dependencies>
</project>
`,
  );
  writeFileSync(
    javaPath,
    `package com.example.consumer;

import com.example.backendgate.order.api.OrderApi;

public final class OrderConsumer {
    private final OrderApi orderApi;

    public OrderConsumer(OrderApi orderApi) {
        this.orderApi = orderApi;
    }
}
`,
  );
  mavenInvocationCount += 1;
  const result = spawnSync(
    'mvn',
    ['-B', '-ntp', `-Dmaven.repo.local=${mavenLocalRepository}`, '-f', 'pom.xml', 'package'],
    {
      cwd: consumerRoot,
      encoding: 'utf8',
      maxBuffer: 50 * 1024 * 1024,
    },
  );
  if (result.status !== 0) {
    throw new Error(`reactor-external generated starter consumer failed:\n${combinedOutput(result)}`);
  }
}

function assertGeneratedPolicyContract() {
  const pom = readFileSync(architectureVerificationPom, 'utf8');
  for (const expected of [
    '<skip>${mango.architecture.skip}</skip>',
    '<rule>${mango.check.rule}</rule>',
    '<baseDir>${mango.check.baseDir}</baseDir>',
    '<staticFailurePolicy>${mango.check.staticFailurePolicy}</staticFailurePolicy>',
    '<requireExecutionRoot>true</requireExecutionRoot>',
    '<requiredRule>all</requiredRule>',
    '<requireBlockingStaticFailures>true</requireBlockingStaticFailures>',
    '<requireFullScope>${mango.check.requireFullScope}</requireFullScope>',
  ]) {
    assertIncludes(pom, expected, 'generated fail-closed policy contract');
  }
  if (pom.includes('<codeLevelExcludedModules>')) {
    throw new Error('generated policy contract must not allow code-level module exclusions');
  }
}

function removeJavaFixture(sourcePath) {
  rmSync(sourcePath, { force: true });
  const sourceMarker = '/src/main/java/';
  const markerIndex = sourcePath.indexOf(sourceMarker);
  if (markerIndex < 0) {
    throw new Error(`Java fixture is outside src/main/java: ${sourcePath}`);
  }
  const moduleRoot = sourcePath.slice(0, markerIndex);
  const classRelativePath = sourcePath.slice(markerIndex + sourceMarker.length).replace(/\.java$/u, '.class');
  rmSync(join(moduleRoot, 'target/classes', classRelativePath), { force: true });
}

function runMaven(goals, shouldPass, label) {
  mavenInvocationCount += 1;
  if (mavenInvocationCount > MAX_MAVEN_INVOCATIONS) {
    throw new Error(
      `Maven invocation budget exceeded before ${label}: ` + `${mavenInvocationCount} > ${MAX_MAVEN_INVOCATIONS}`,
    );
  }
  const defaultProperties = [
    '-Dmango.architecture.mode=full',
    '-Dmango.architecture.skip=false',
    '-Dmango.check.gate=all',
    '-Dmango.check.staticFailurePolicy=block',
    '-Denforcer.skip=false',
  ];
  const overriddenProperties = new Set(
    goals
      .filter((argument) => argument.startsWith('-D') && argument.includes('='))
      .map((argument) => argument.slice(2, argument.indexOf('='))),
  );
  const result = spawnSync(
    'mvn',
    [
      '-B',
      '-ntp',
      `-Dmaven.repo.local=${mavenLocalRepository}`,
      '-f',
      'backend/pom.xml',
      ...defaultProperties.filter((argument) => {
        const property = argument.slice(2, argument.indexOf('='));
        return !overriddenProperties.has(property);
      }),
      ...goals,
    ],
    {
      cwd: projectRoot,
      encoding: 'utf8',
      maxBuffer: 50 * 1024 * 1024,
    },
  );
  if (shouldPass && result.status !== 0) {
    throw new Error(`${label} unexpectedly failed:\n${combinedOutput(result)}`);
  }
  if (!shouldPass && result.status === 0) {
    throw new Error(`${label} unexpectedly passed`);
  }
  return combinedOutput(result);
}

function runGit(args, label) {
  const result = spawnSync('git', args, {
    cwd: projectRoot,
    encoding: 'utf8',
    env: {
      ...process.env,
      GIT_AUTHOR_NAME: 'Mango Generated Gate',
      GIT_AUTHOR_EMAIL: 'mango-generated-gate@example.invalid',
      GIT_COMMITTER_NAME: 'Mango Generated Gate',
      GIT_COMMITTER_EMAIL: 'mango-generated-gate@example.invalid',
    },
  });
  if (result.status !== 0) {
    throw new Error(`${label} failed:\n${combinedOutput(result)}`);
  }
}

function assertMavenInvocationBudget() {
  if (mavenInvocationCount !== MAX_MAVEN_INVOCATIONS) {
    throw new Error(
      `generated backend gate Maven invocation contract changed: ` +
        `${mavenInvocationCount} != ${MAX_MAVEN_INVOCATIONS}`,
    );
  }
}

function assertPassingReports() {
  const architecturePath = join(projectRoot, 'backend/target/mango-architecture-report.json');
  const qualityPath = join(projectRoot, 'backend/target/mango-quality-report.json');
  if (!existsSync(architecturePath) || !existsSync(qualityPath)) {
    throw new Error('generated backend verify did not create both architecture and quality reports');
  }
  const architecture = readJson(architecturePath);
  const quality = readJson(qualityPath);
  assertArchitectureModuleOwnership(architecture);
  if (architecture.mode !== 'full' || (architecture.blockingIssues || []).length !== 0) {
    throw new Error(`generated backend architecture report did not pass in full mode: ${architecturePath}`);
  }
  if (
    !quality.passed ||
    quality.gate !== 'all' ||
    quality.staticFailurePolicy !== 'block' ||
    quality.toolFailureCount !== 0
  ) {
    throw new Error(`generated backend quality report is not fail-closed: ${qualityPath}`);
  }
  return {
    architecture: {
      mode: architecture.mode,
      blockingIssueCount: (architecture.blockingIssues || []).length,
      moduleCount: (architecture.modules || []).length,
      reactorProjectCount: architecture.reactorProjectCount,
    },
    quality: {
      gate: quality.gate,
      passed: quality.passed,
      totalIssueCount: quality.totalIssueCount ?? (quality.issues || []).length,
      newIssueCount: quality.newIssueCount ?? (quality.newIssues || []).length,
      toolFailureCount: quality.toolFailureCount,
      issuesBySource: quality.issuesBySource || {},
    },
  };
}

function assertInventoryOnlyReport() {
  const architecturePath = join(projectRoot, 'backend/target/mango-architecture-report.json');
  const architecture = readJson(architecturePath);
  assertArchitectureModuleOwnership(architecture);
  if (architecture.inventoryOnly !== true || architecture.inventoryScope !== 'full-reactor') {
    throw new Error(`generated backend inventory-only report is not full-Reactor: ${architecturePath}`);
  }
}

function assertArchitectureModuleOwnership(report) {
  if (report.schemaVersion !== 2 || !Array.isArray(report.modules)) {
    throw new Error('generated backend architecture report must use module-aware schema v2');
  }
  const moduleKeys = new Set(report.modules.map((module) => module.moduleKey));
  if (
    moduleKeys.size !== report.modules.length ||
    report.reactorProjectCount !== report.expectedProjectCount ||
    report.reactorProjectCount !== report.modules.length
  ) {
    throw new Error('generated backend architecture report has an incomplete Reactor module catalog');
  }
  for (const field of ['dependencyIssues', 'archUnitIssues', 'pmdIssues', 'blockingIssues']) {
    if (!Array.isArray(report[field])) {
      throw new Error(`generated backend architecture report is missing ${field}`);
    }
    const unowned = report[field].find((issue) => !moduleKeys.has(issue.moduleKey));
    if (unowned) {
      throw new Error(`${field} contains an issue without valid module ownership: ${JSON.stringify(unowned)}`);
    }
  }
}

function assertArchitectureRule(ruleId, failureOutput = '') {
  const report = readJson(join(projectRoot, 'backend/target/mango-architecture-report.json'));
  const rules = new Set((report.blockingIssues || []).map((issue) => issue.ruleId));
  if (!rules.has(ruleId)) {
    throw new Error(
      `architecture report missing ${ruleId}: ${JSON.stringify([...rules])}` +
        (failureOutput ? `\nMaven failure:\n${failureOutput}` : ''),
    );
  }
}

function assertMangoCheckRule(ruleId) {
  const report = readJson(join(projectRoot, 'backend/target/mango-quality-report.json'));
  const rules = new Set((report.issues || []).map((issue) => issue.rule));
  if (!rules.has(ruleId)) {
    throw new Error(`Mango quality report missing ${ruleId}: ${JSON.stringify([...rules])}`);
  }
}

function assertStaticAnalysisFailure(source, failureOutput = '') {
  const report = readJson(join(projectRoot, 'backend/target/mango-quality-report.json'));
  const matchingIssues = (report.newIssues || []).filter((issue) => issue.source === source);
  if (report.passed || report.toolFailureCount !== 0 || matchingIssues.length === 0) {
    throw new Error(
      `Mango quality report missing blocking ${source} issues: ${JSON.stringify(report)}` +
        (failureOutput ? `\nMaven failure:\n${failureOutput}` : ''),
    );
  }
}

function assertIncludes(content, expected, label) {
  if (!content.includes(expected)) {
    throw new Error(`${label} missing ${expected}:\n${content}`);
  }
}

function globalEntityManifestJson(table) {
  return `${JSON.stringify(
    {
      contractId: 'global-entity-exceptions',
      schemaRevision: 1,
      version: 1,
      exceptions: [
        {
          entity: 'com.example.backendgate.order.core.entity.GlobalReferenceEntity',
          table,
          owner: 'order-team',
          reason: 'Approved platform-wide sales order reference data',
          approvalRef: 'ARCH-APPROVAL-001',
          approvedBy: 'architecture-owner',
          expiresOn: '2099-12-31',
        },
      ],
    },
    null,
    2,
  )}\n`;
}

function globalEntitySource(className, table) {
  return `package com.example.backendgate.order.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.mango.infra.persistence.api.entity.AuditableEntity;

/**
 * Global reference data acceptance fixture.
 */
@TableName("${table}")
public class ${className} extends AuditableEntity {

    private static final long serialVersionUID = 1L;

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
`;
}

function globalEntityTableSql(table) {
  return `
CREATE TABLE ${table} (
    id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    created_by BIGINT NULL,
    created_at DATETIME NULL,
    updated_by BIGINT NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (id)
);
`;
}

function combinedOutput(result) {
  return `${result.stdout || ''}\n${result.stderr || ''}`;
}

function readJson(path) {
  return JSON.parse(readFileSync(path, 'utf8'));
}
