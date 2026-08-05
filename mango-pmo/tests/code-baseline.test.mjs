import assert from 'node:assert/strict';
import { mkdtempSync, readFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';
import { evaluateCodeBaseline } from '../tools/evaluate-code-baseline.mjs';
import { loadCodeTemplateIndex, renderCodeBaseline } from '../tools/code-baseline.mjs';

const testRoot = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(testRoot, '../..');

test('business module baseline represents Mango-owned code and quality rules', () => {
  const template = loadCodeTemplateIndex().templates.find((item) => item.id === 'business-module');
  assert.equal(template.qualityProfiles.some((profile) => profile.id === 'mango-java-checkstyle'), true);
  assert.equal(template.qualityProfiles.some((profile) => profile.id === 'mango-java-architecture'), true);
  assert.equal(template.conventions.some((rule) => rule.id === 'MANGO-CODE-ERROR-001'), true);
  assert.equal(template.conventions.some((rule) => rule.id === 'MANGO-CODE-REQUIRE-001'), true);
});

test('renderer rejects undeclared and path-unsafe template variables', () => {
  const root = mkdtempSync(join(tmpdir(), 'mango-code-baseline-inputs-'));
  const values = validVariables();
  try {
    assert.throws(
      () => renderCodeBaseline({ templateId: 'business-module', targetDir: root, variables: { ...values, surprise: 'x' } }),
      /unknown template variables/,
    );
    assert.throws(
      () => renderCodeBaseline({
        templateId: 'business-module',
        targetDir: root,
        variables: { ...values, basePackagePath: '../../outside' },
      }),
      /invalid template variable basePackagePath/,
    );
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
});

test('empty, non-compliant legacy and real Mango debt scenarios pass baseline isolation evaluation', () => {
  const result = evaluateCodeBaseline();
  assert.equal(result.passed, true, JSON.stringify(result, null, 2));
  assert.equal(result.scenarios.empty.unresolvedPlaceholders, 0);
  assert.equal(result.scenarios.legacy.legacyFileUnchanged, true);
  assert.equal(result.scenarios.legacy.legacyPatternCopies, 0);
  assert.equal(result.scenarios.legacy.nonTargetFileChanges, 0);
  assert.equal(result.scenarios.mango.historicalIssues > 0, true);
  assert.equal(result.scenarios.mango.generatedContractViolations, 0);
  assert.equal(result.scenarios.mango.realModuleFilesModified, 0);
  assert.equal(result.assessment.evidenceLevel, 'template-contract-only');
  assert.equal(result.assessment.improved, null);
});

test('actual Mango gate evidence proves measured quality improvement in all three scenarios', () => {
  const result = evaluateCodeBaseline({ qualityEvidence: validQualityEvidence() });
  assert.equal(result.passed, true, JSON.stringify(result, null, 2));
  assert.equal(result.assessment.evidenceLevel, 'mango-full-gate');
  assert.equal(result.assessment.improved, true);
  assert.equal(result.scenarios.empty.generatedQuality.totalIssueCount, 0);
  assert.equal(result.scenarios.legacy.qualityNegativeControlsPassed, true);
  assert.equal(result.scenarios.legacy.comparableIssueReductionPercent, 100);
  assert.equal(result.scenarios.mango.issueReductionPercent, 100);
});

test('evaluation rejects quality evidence that did not exercise every negative control', () => {
  const evidence = validQualityEvidence();
  evidence.negativeControls.checkstyleViolationRejected = false;
  assert.throws(
    () => evaluateCodeBaseline({ qualityEvidence: evidence }),
    /invalid or incomplete generated backend quality evidence/,
  );
});

test('checked-in evaluation evidence matches current baseline and remains improved', () => {
  const evidence = JSON.parse(readFileSync(
    join(repoRoot, 'mango-docs/evidence/governance/code-baseline-evaluation-2026-08-05.json'),
    'utf8',
  ));
  const current = evaluateCodeBaseline({ qualityEvidence: evidence.actualQualityEvidence });
  assert.equal(evidence.assessment.improved, true);
  assert.equal(evidence.scenarios.empty.generatedQuality.totalIssueCount, 0);
  assert.equal(evidence.scenarios.legacy.comparableIssueReductionPercent, 100);
  assert.equal(evidence.scenarios.mango.sourceModule, current.scenarios.mango.sourceModule);
  assert.equal(evidence.scenarios.mango.historicalIssues, current.scenarios.mango.historicalIssues);
  assert.equal(evidence.scenarios.mango.issueReductionPercent, current.scenarios.mango.issueReductionPercent);
});

function validVariables() {
  return {
    moduleKebab: 'order',
    aggregateKebab: 'record',
    basePackage: 'io.mango.generated',
    groupId: 'io.mango.generated',
    projectKebab: 'baseline-test',
    projectVersion: '1.0.0-SNAPSHOT',
    mangoAppRuntimeVersion: '1.0.2',
    moduleName: '订单模块',
    aggregateName: '业务记录',
    modulePackage: 'order',
    modulePascal: 'Order',
    moduleCamel: 'order',
    moduleBusinessDomainCode: 'ORDER',
    moduleKebabSnake: 'order',
    aggregatePascal: 'Record',
    aggregateCamel: 'record',
    aggregateKebabSnake: 'record',
    basePackagePath: 'io/mango/generated',
    businessResourceMenuId: '2690690000000000001',
  };
}

function validQualityEvidence() {
  return {
    schemaVersion: 1,
    templateId: 'business-module@1',
    mangoVersion: '1.0.35',
    mavenInvocationCount: 9,
    cleanQualityEvidence: {
      architecture: {
        mode: 'full',
        blockingIssueCount: 0,
        moduleCount: 8,
        reactorProjectCount: 8,
      },
      quality: {
        gate: 'all',
        passed: true,
        totalIssueCount: 0,
        newIssueCount: 0,
        toolFailureCount: 0,
        issuesBySource: {},
      },
    },
    negativeControls: {
      missingStaticReportRejected: true,
      architectureViolationsRejected: true,
      reservedNamespaceRejected: true,
      checkstyleViolationRejected: true,
      mangoCheckViolationsRejected: true,
      missingEntityManifestRejected: true,
      architectureBypassRejected: true,
    },
  };
}
