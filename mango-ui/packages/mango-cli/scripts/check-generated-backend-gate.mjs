#!/usr/bin/env node
import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync, mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';

const packageRoot = resolve(new URL('..', import.meta.url).pathname);
const cli = join(packageRoot, 'src/index.mjs');
const releaseVersions = JSON.parse(readFileSync(join(packageRoot, 'release-versions.json'), 'utf8'));
const mangoVersion = process.env.MANGO_BACKEND_GATE_VERSION || releaseVersions.maven.mangoBackend;
const mavenRepository = process.env.MANGO_BACKEND_GATE_REPOSITORY
  || 'https://nexus.inner.yunxinbaokeji.com/repository/maven-public/';
const tempRoot = mkdtempSync(join(tmpdir(), 'mango-generated-backend-gate-'));
const projectName = 'mango-backend-gate-acceptance';
const projectRoot = join(tempRoot, projectName);
const appJavaRoot = join(
  projectRoot,
  'backend/app/src/main/java/com/example/backendgate',
);
const coreJavaRoot = join(
  projectRoot,
  'backend/modules/order/order-core/src/main/java/com/example/backendgate/order/core',
);
const migrationPath = join(
  projectRoot,
  'backend/modules/order/order-core/src/main/resources/db/migration/order/V1__init_order.sql',
);
const entityPath = join(coreJavaRoot, 'entity/SalesOrderEntity.java');
const invalidApiModuleInfo = join(
  projectRoot,
  'backend/modules/order/order-api/src/main/resources/META-INF/mango/module.properties',
);
const invalidApiResources = join(
  projectRoot,
  'backend/modules/order/order-api/src/main/resources',
);
const globalEntityManifest = join(projectRoot, 'business-pmo/global-entity-exceptions.json');
const orderApiPom = join(projectRoot, 'backend/modules/order/order-api/pom.xml');

try {
  runNode([
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
  ], tempRoot, 'generate acceptance project');
  runNode([
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
  ], projectRoot, 'generate four-layer business module');

  runMaven(['clean', 'verify'], true, 'generated four-layer backend');
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
  const missingChildReportFailure = runMaven(
    ['clean', 'verify'],
    false,
    'missing child-module PMD report',
  );
  assertIncludes(
    missingChildReportFailure,
    'Static analysis missing pmd report for Java Reactor module(s)',
    'full-scope static report coverage failure',
  );
  assertIncludes(missingChildReportFailure, 'order-api', 'missing PMD report module identity');
  writeFileSync(orderApiPom, originalOrderApiPom);

  const badController = join(appJavaRoot, 'BadController.java');
  writeFileSync(badController, `package com.example.backendgate;

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
`);
  const pathFailure = runMaven(['clean', 'verify'], false, '@PathVariable violation');
  assertIncludes(pathFailure, 'Mango architecture gate found', 'path violation failure');
  const architectureReport = readJson(join(projectRoot, 'backend/target/mango-architecture-report.json'));
  assertArchitectureModuleOwnership(architectureReport);
  const pathRules = new Set((architectureReport.blockingIssues || []).map(issue => issue.ruleId));
  if (!pathRules.has('MANGO-ARCH-PATH-001') || !pathRules.has('MANGO-ARCH-PATH-002')) {
    throw new Error(`path violation report missing PATH-001/002: ${JSON.stringify([...pathRules])}`);
  }
  rmSync(badController);

  const staticViolation = join(appJavaRoot, 'StaticViolation.java');
  writeFileSync(staticViolation, `package com.example.backendgate;

public final class StaticViolation {

\tpublic int value() {
        return 42;
    }
}
`);
  const staticFailure = runMaven(['clean', 'verify'], false, 'generic Java style violation');
  assertIncludes(staticFailure, 'Checkstyle violations', 'static-analysis failure');
  rmSync(staticViolation);

  const suppressedServiceContract = join(
    coreJavaRoot,
    'service/ISuppressedArchitectureService.java',
  );
  writeFileSync(suppressedServiceContract, `package com.example.backendgate.order.core.service;

public interface ISuppressedArchitectureService {

    String ping();
}
`);
  const suppressedService = join(coreJavaRoot, 'service/impl/SuppressedArchitectureService.java');
  mkdirSync(dirname(suppressedService), { recursive: true });
  writeFileSync(suppressedService, `package com.example.backendgate.order.core.service.impl;

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
`);
  const suppressionFailure = runMaven(
    ['clean', 'verify'],
    false,
    'suppressed PMD architecture violation',
  );
  assertArchitectureRule('MANGO-ARCH-SVC-001', suppressionFailure);
  rmSync(suppressedService);
  rmSync(suppressedServiceContract);

  const reservedNamespaceSource = join(
    projectRoot,
    'backend/app/src/main/java/io/mango/common/result/BusinessShadow.java',
  );
  mkdirSync(dirname(reservedNamespaceSource), { recursive: true });
  writeFileSync(reservedNamespaceSource, `package io.mango.common.result;

public final class BusinessShadow {
}
`);
  const namespaceFailure = runMaven(['clean', 'verify'], false, 'reserved namespace shadow');
  assertIncludes(namespaceFailure, 'MANGO-ARCH-ENGINE-017', 'reserved namespace failure');
  rmSync(reservedNamespaceSource);

  const directService = join(coreJavaRoot, 'service/impl/DirectServiceImpl.java');
  writeFileSync(directService, `package com.example.backendgate.order.core.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.backendgate.order.core.entity.SalesOrderEntity;
import com.example.backendgate.order.core.mapper.SalesOrderMapper;
import org.springframework.stereotype.Service;

@Service
public class DirectServiceImpl extends ServiceImpl<SalesOrderMapper, SalesOrderEntity> {
}
`);
  runMaven(['clean', 'verify'], false, 'direct MyBatis ServiceImpl violation');
  assertArchitectureRule('MANGO-ARCH-SVC-014');
  rmSync(directService);

  const originalMigration = readFileSync(migrationPath, 'utf8');
  const missingTenantMigration = originalMigration.replace('    tenant_id VARCHAR(64) NULL,\n', '');
  if (missingTenantMigration === originalMigration) {
    throw new Error('generated migration does not contain the expected tenant_id declaration');
  }
  writeFileSync(migrationPath, missingTenantMigration);
  runMaven(['clean', 'verify'], false, 'missing tenant migration violation');
  assertMangoCheckRule('PERSISTENCE_SCHEMA');
  writeFileSync(migrationPath, originalMigration);

  mkdirSync(dirname(invalidApiModuleInfo), { recursive: true });
  writeFileSync(invalidApiModuleInfo, 'module-name=order\nmodule-path=order\n');
  runMaven(['clean', 'verify'], false, 'invalid module info violation');
  assertMangoCheckRule('MODULE_INFO');
  rmSync(invalidApiResources, { recursive: true, force: true });

  const originalEntity = readFileSync(entityPath, 'utf8');
  const globalEntity = originalEntity
    .replace('import io.mango.infra.persistence.api.entity.TenantEntity;',
      'import io.mango.infra.persistence.api.entity.AuditableEntity;')
    .replace('extends TenantEntity', 'extends AuditableEntity');
  const globalMigration = originalMigration
    .replace('    tenant_id VARCHAR(64) NULL,\n', '')
    .replace('    org_id BIGINT NULL,\n', '');
  if (globalEntity === originalEntity || globalMigration === originalMigration) {
    throw new Error('unable to transform generated tenant entity into the global Entity acceptance fixture');
  }
  writeFileSync(entityPath, globalEntity);
  writeFileSync(migrationPath, globalMigration);
  runMaven(['clean', 'verify'], false, 'unregistered global Entity violation');
  assertArchitectureRule('MANGO-ARCH-ENTITY-003');

  writeFileSync(globalEntityManifest, globalEntityManifestJson('order_sales_order'));
  runMaven(['clean', 'verify'], true, 'approved global Entity');
  assertPassingReports();

  writeFileSync(globalEntityManifest, globalEntityManifestJson('wrong_global_table'));
  runMaven(['clean', 'verify'], false, 'global Entity table mismatch');
  assertArchitectureRule('MANGO-ARCH-ENTITY-004');
  writeFileSync(entityPath, originalEntity);
  writeFileSync(migrationPath, originalMigration);
  writeFileSync(globalEntityManifest, emptyGlobalEntityManifestJson());

  const originalManifest = readFileSync(globalEntityManifest, 'utf8');
  rmSync(globalEntityManifest);
  const manifestFailure = runMaven(['clean', 'verify'], false, 'missing global entity manifest');
  assertIncludes(manifestFailure, 'MANGO-ARCH-ENGINE-014', 'missing global entity manifest failure');
  writeFileSync(globalEntityManifest, originalManifest);

  for (const [property, value, message] of [
    ['mango.architecture.skip', 'true', 'MANGO-ARCH-ENGINE-015'],
    ['mango.check.rule', 'static', 'Governed mango:check rule must remain all, actual=static'],
    ['mango.check.baseDir', 'backend/app', 'Governed mango:check baseDir must equal Maven execution root'],
    ['mango.check.gate', 'no-new-violations', 'Governed mango:check gate must remain all'],
    ['mango.check.staticFailurePolicy', 'report', 'Governed mango:check staticFailurePolicy must remain block'],
    ['mango.check.changedOnly', 'true', 'Governed mango:check requires full scope; changedOnly=true is forbidden'],
    ['mango.check.codeLevelExcludedModules', 'x', 'Governed mango:check requires full scope; codeLevelExcludedModules is forbidden'],
  ]) {
    const policyFailure = runMaven([
      `-D${property}=${value}`,
      '-Denforcer.skip=true',
      'verify',
    ], false, `${property} override`);
    assertIncludes(policyFailure, message, `${property} override failure`);
  }
  runMaven([
    '-pl',
    'modules/order/order-core,architecture-verification',
    '-Dmango.architecture.mode=changed',
    '-Dmango.architecture.requireFullReactor=false',
    '-Denforcer.skip=false',
    'validate',
  ], true, 'affected-module architecture mode');

  process.stdout.write(
    `Generated backend gate PASS with Mango ${mangoVersion}: clean project accepted; `
      + 'PathVariable, direct ServiceImpl, tenant schema, module info, Checkstyle, manifest, '
      + 'PMD suppression, reserved namespace shadowing, '
      + 'module-aware architecture report ownership, '
      + 'per-Java-module static report coverage, '
      + 'unregistered/mismatched global Entity cases, approved global Entity acceptance, '
      + 'affected-module mode accepted, and seven fail-closed policy overrides rejected.\n',
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

function runMaven(goals, shouldPass, label) {
  const defaultProperties = [
    '-Dmango.architecture.mode=full',
    '-Dmango.architecture.skip=false',
    '-Dmango.check.gate=all',
    '-Dmango.check.staticFailurePolicy=block',
    '-Denforcer.skip=false',
  ];
  const overriddenProperties = new Set(goals
    .filter(argument => argument.startsWith('-D') && argument.includes('='))
    .map(argument => argument.slice(2, argument.indexOf('='))));
  const result = spawnSync('mvn', [
    '-B',
    '-ntp',
    '-f',
    'backend/pom.xml',
    ...defaultProperties.filter(argument => {
      const property = argument.slice(2, argument.indexOf('='));
      return !overriddenProperties.has(property);
    }),
    ...goals,
  ], {
    cwd: projectRoot,
    encoding: 'utf8',
    maxBuffer: 50 * 1024 * 1024,
  });
  if (shouldPass && result.status !== 0) {
    throw new Error(`${label} unexpectedly failed:\n${combinedOutput(result)}`);
  }
  if (!shouldPass && result.status === 0) {
    throw new Error(`${label} unexpectedly passed`);
  }
  return combinedOutput(result);
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
  if (!quality.passed
      || quality.gate !== 'all'
      || quality.staticFailurePolicy !== 'block'
      || quality.toolFailureCount !== 0) {
    throw new Error(`generated backend quality report is not fail-closed: ${qualityPath}`);
  }
}

function assertArchitectureModuleOwnership(report) {
  if (report.schemaVersion !== 2 || !Array.isArray(report.modules)) {
    throw new Error('generated backend architecture report must use module-aware schema v2');
  }
  const moduleKeys = new Set(report.modules.map(module => module.moduleKey));
  if (moduleKeys.size !== report.modules.length
      || report.reactorProjectCount !== report.expectedProjectCount
      || report.reactorProjectCount !== report.modules.length) {
    throw new Error('generated backend architecture report has an incomplete Reactor module catalog');
  }
  for (const field of ['dependencyIssues', 'archUnitIssues', 'pmdIssues', 'blockingIssues']) {
    if (!Array.isArray(report[field])) {
      throw new Error(`generated backend architecture report is missing ${field}`);
    }
    const unowned = report[field].find(issue => !moduleKeys.has(issue.moduleKey));
    if (unowned) {
      throw new Error(`${field} contains an issue without valid module ownership: ${JSON.stringify(unowned)}`);
    }
  }
}

function assertArchitectureRule(ruleId, failureOutput = '') {
  const report = readJson(join(projectRoot, 'backend/target/mango-architecture-report.json'));
  const rules = new Set((report.blockingIssues || []).map(issue => issue.ruleId));
  if (!rules.has(ruleId)) {
    throw new Error(
      `architecture report missing ${ruleId}: ${JSON.stringify([...rules])}`
        + (failureOutput ? `\nMaven failure:\n${failureOutput}` : ''),
    );
  }
}

function assertMangoCheckRule(ruleId) {
  const report = readJson(join(projectRoot, 'backend/target/mango-quality-report.json'));
  const rules = new Set((report.issues || []).map(issue => issue.rule));
  if (!rules.has(ruleId)) {
    throw new Error(`Mango quality report missing ${ruleId}: ${JSON.stringify([...rules])}`);
  }
}

function assertIncludes(content, expected, label) {
  if (!content.includes(expected)) {
    throw new Error(`${label} missing ${expected}:\n${content}`);
  }
}

function globalEntityManifestJson(table) {
  return `${JSON.stringify({
    contractId: 'global-entity-exceptions',
    schemaRevision: 1,
    version: 1,
    exceptions: [{
      entity: 'com.example.backendgate.order.core.entity.SalesOrderEntity',
      table,
      owner: 'order-team',
      reason: 'Approved platform-wide sales order reference data',
      approvalRef: 'ARCH-APPROVAL-001',
      approvedBy: 'architecture-owner',
      expiresOn: '2099-12-31',
    }],
  }, null, 2)}\n`;
}

function emptyGlobalEntityManifestJson() {
  return `${JSON.stringify({
    contractId: 'global-entity-exceptions',
    schemaRevision: 1,
    version: 1,
    exceptions: [],
  }, null, 2)}\n`;
}

function combinedOutput(result) {
  return `${result.stdout || ''}\n${result.stderr || ''}`;
}

function readJson(path) {
  return JSON.parse(readFileSync(path, 'utf8'));
}
