#!/usr/bin/env node
import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const packageRoot = resolve(fileURLToPath(new URL('..', import.meta.url)));
const cli = join(packageRoot, 'src/index.mjs');
const releaseVersions = JSON.parse(readFileSync(join(packageRoot, 'release-versions.json'), 'utf8'));
const mangoVersion = process.env.MANGO_BACKEND_GATE_VERSION || releaseVersions.maven.mangoBackend;
const tempRoot = mkdtempSync(join(tmpdir(), 'mango-generated-backend-gate-'));
const localMangoRepository = join(tempRoot, 'mango-repository');
mkdirSync(localMangoRepository, { recursive: true });
const mavenRepository = process.env.MANGO_BACKEND_GATE_REPOSITORY || `${pathToFileURL(localMangoRepository).href}/`;
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
const orderApiPom = join(projectRoot, 'backend/modules/order/order-api/pom.xml');
const architectureVerificationPom = join(projectRoot, 'backend/architecture-verification/pom.xml');
const globalReferenceEntity = join(coreJavaRoot, 'entity/GlobalReferenceEntity.java');

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
  );
  keepOnlyFourLayerBusinessReactor();
  addApprovedGlobalEntityFixture();
  assertGeneratedPolicyContract();

  runMaven(['verify'], true, 'generated four-layer backend');
  assertPassingReports();

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
  writeFileSync(migrationPath, originalMigration);
  rmSync(invalidApiResources, { recursive: true, force: true });

  const originalManifest = readFileSync(globalEntityManifest, 'utf8');
  rmSync(globalEntityManifest);
  const manifestFailure = runMaven(['verify'], false, 'missing global entity manifest');
  assertIncludes(manifestFailure, 'MANGO-ARCH-ENGINE-014', 'missing global entity manifest failure');
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
  runMaven(
    [
      '-pl',
      'modules/order/order-core,architecture-verification',
      '-Dmango.architecture.mode=changed',
      '-Dmango.architecture.requireFullReactor=false',
      '-Dmango.check.gate=no-new-violations',
      '-Dmango.check.changedOnly=true',
      '-Dmango.check.requireFullScope=false',
      '-Denforcer.skip=false',
      'validate',
    ],
    true,
    'affected-module architecture mode',
  );

  assertMavenInvocationBudget();

  process.stdout.write(
    `Generated backend gate PASS with Mango ${mangoVersion} in ${mavenInvocationCount} Maven invocations: ` +
      'clean project accepted; ' +
      'PathVariable, direct ServiceImpl, tenant schema, module info, Checkstyle, manifest, ' +
      'PMD suppression, reserved namespace shadowing, ' +
      'module-aware architecture report ownership, ' +
      'per-Java-module static report coverage, ' +
      'unregistered/mismatched global Entity cases, approved global Entity acceptance, ' +
      'affected-module mode accepted, generated policy wiring locked, and representative ' +
      'architecture policy bypass rejected.\n',
  );
} finally {
  rmSync(tempRoot, { recursive: true, force: true });
}

function runNode(args, cwd, label) {
  const result = spawnSync(process.execPath, args, {
    cwd,
    encoding: 'utf8',
    maxBuffer: 20 * 1024 * 1024,
  });
  if (result.status !== 0) {
    throw new Error(`${label} failed:\n${combinedOutput(result)}`);
  }
}

function keepOnlyFourLayerBusinessReactor() {
  const backendPom = join(projectRoot, 'backend/pom.xml');
  const original = readFileSync(backendPom, 'utf8');
  const withoutPlatformApp = original.replace(/^\s*<module>app<\/module>\s*$/mu, '');
  if (withoutPlatformApp === original) {
    throw new Error('generated backend does not contain the expected platform app module');
  }
  writeFileSync(backendPom, withoutPlatformApp);
  rmSync(join(projectRoot, 'backend/app'), { recursive: true, force: true });
}

function addApprovedGlobalEntityFixture() {
  writeFileSync(globalReferenceEntity, globalEntitySource('GlobalReferenceEntity', 'order_global_reference'));
  writeFileSync(
    migrationPath,
    `${readFileSync(migrationPath, 'utf8')}${globalEntityTableSql('order_global_reference')}`,
  );
  writeFileSync(globalEntityManifest, globalEntityManifestJson('order_global_reference'));
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
