#!/usr/bin/env node
import { createHash } from 'node:crypto';
import { existsSync, mkdtempSync, mkdirSync, readFileSync, readdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { loadCodeTemplateIndex, renderCodeBaseline } from './code-baseline.mjs';

const currentFile = fileURLToPath(import.meta.url);
const pmoRoot = resolve(dirname(currentFile), '..');

export function evaluateCodeBaseline({ qualityEvidence } = {}) {
  const verifiedQualityEvidence = qualityEvidence ? validateQualityEvidence(qualityEvidence) : null;
  const root = mkdtempSync(join(tmpdir(), 'mango-code-baseline-eval-'));
  try {
    const empty = evaluateEmptyProject(join(root, 'empty'), verifiedQualityEvidence);
    const legacy = evaluateLegacyProject(join(root, 'legacy'), verifiedQualityEvidence);
    const mango = evaluateMangoDebtProject(join(root, 'mango'), verifiedQualityEvidence);
    const template = loadCodeTemplateIndex().templates.find((item) => item.id === 'business-module');
    return {
      schemaVersion: 1,
      template: 'business-module@1',
      profileSources: template.qualityProfiles.flatMap((profile) => profile.sources ?? [profile.source]),
      representedQualityRules: Object.fromEntries(
        template.qualityProfiles.map((profile) => [profile.id, profile.rules.length]),
      ),
      actualQualityEvidence: verifiedQualityEvidence,
      scenarios: { empty, legacy, mango },
      passed: [empty, legacy, mango].every((scenario) => scenario.passed),
      assessment: buildAssessment({ empty, legacy, mango, qualityEvidence: verifiedQualityEvidence }),
    };
  } finally {
    rmSync(root, { recursive: true, force: true });
  }
}

function evaluateEmptyProject(target, qualityEvidence) {
  const started = process.hrtime.bigint();
  const result = renderCodeBaseline({ templateId: 'business-module', targetDir: target, variables: variables('order') });
  const elapsedMs = Number(process.hrtime.bigint() - started) / 1_000_000;
  const contract = inspectRenderedContract(target, variables('order'));
  const generatedQuality = cleanQualityMetrics(qualityEvidence);
  return {
    passed: contract.violations.length === 0 && (!qualityEvidence || generatedQuality.passed),
    generatedFiles: result.files.length,
    elapsedMs: round(elapsedMs),
    unresolvedPlaceholders: countMatches(target, /\{\{[^}]+}}/g),
    contractViolations: contract.violations,
    representedConventions: contract.representedConventions,
    generatedQuality,
  };
}

function evaluateLegacyProject(target, qualityEvidence) {
  const fixtures = [
    {
      path: 'legacy/LegacyController.java',
      ruleId: 'MANGO-ARCH-PATH-001',
      content: `import org.springframework.web.bind.annotation.PathVariable;
class LegacyController {
    String detail(@PathVariable Long id) { return String.valueOf(id); }
}
`,
    },
    {
      path: 'legacy/LegacyDirectService.java',
      ruleId: 'MANGO-ARCH-SVC-014',
      content: `import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
class LegacyDirectService extends ServiceImpl<Object, Object> {
}
`,
    },
    {
      path: 'legacy/LegacyStyle.java',
      ruleId: 'CHECKSTYLE-FileTabCharacter',
      content: `class LegacyStyle {
\tint value() { return 42; }
}
`,
    },
    {
      path: 'legacy/LegacyFixedSuccess.java',
      ruleId: 'AI-REDLINE-FIXED-SUCCESS',
      content: `class LegacyFixedSuccess {
    boolean create() {
        try { return true; } catch (Exception ignored) { return true; }
    }
}
`,
    },
  ];
  const before = new Map();
  for (const fixture of fixtures) {
    const path = join(target, fixture.path);
    mkdirSync(dirname(path), { recursive: true });
    writeFileSync(path, fixture.content);
    before.set(fixture.path, digest(path));
  }
  const result = renderCodeBaseline({ templateId: 'business-module', targetDir: target, variables: variables('invoice') });
  const generatedFiles = result.files.filter((file) => !file.startsWith('legacy/'));
  const generatedRoots = ['backend', 'frontend'].map((path) => join(target, path));
  const forbiddenHits = generatedRoots.reduce(
    (total, path) => total + countMatches(
      path,
      /com\.baomidou\.mybatisplus\.extension\.service\.impl\.ServiceImpl|@PathVariable|\t|UnsupportedOperationException|TODO|FIXME|return\s+R\.ok\(true\)/g,
    ),
    0,
  );
  const contract = inspectRenderedContract(target, variables('invoice'));
  const changedLegacyFiles = fixtures.filter((fixture) => digest(join(target, fixture.path)) !== before.get(fixture.path));
  const generatedQuality = cleanQualityMetrics(qualityEvidence);
  const qualityControlsPassed = !qualityEvidence || (
    qualityEvidence.negativeControls.architectureViolationsRejected
    && qualityEvidence.negativeControls.checkstyleViolationRejected
  );
  const comparableViolationCount = fixtures.filter((fixture) => fixture.ruleId !== 'AI-REDLINE-FIXED-SUCCESS').length;
  return {
    passed: changedLegacyFiles.length === 0
      && forbiddenHits === 0
      && contract.violations.length === 0
      && qualityControlsPassed
      && (!qualityEvidence || generatedQuality.passed),
    generatedFiles: generatedFiles.length,
    legacyFiles: fixtures.length,
    legacyFileUnchanged: changedLegacyFiles.length === 0,
    legacyPatternCopies: forbiddenHits,
    nonTargetFileChanges: changedLegacyFiles.length,
    knownLegacyViolationRules: fixtures.map((fixture) => fixture.ruleId),
    comparableLegacyViolationCount: comparableViolationCount,
    generatedIssueCount: generatedQuality.totalIssueCount,
    comparableIssueReductionPercent: reductionPercent(comparableViolationCount, generatedQuality.totalIssueCount),
    qualityNegativeControlsPassed: qualityControlsPassed,
    contractViolations: contract.violations,
    generatedQuality,
  };
}

function evaluateMangoDebtProject(target, qualityEvidence) {
  const baselinePath = join(pmoRoot, 'baselines/mango-check/no-new-violations-baseline.json');
  const baseline = JSON.parse(readFileSync(baselinePath, 'utf8'));
  const moduleStats = new Map();
  for (const issue of baseline.issues ?? []) {
    const parts = String(issue.file ?? '').split('/');
    const moduleRoot = parts.length >= 2 ? `${parts[0]}/${parts[1]}` : parts[0];
    if (!moduleRoot) continue;
    const current = moduleStats.get(moduleRoot) ?? { issueCount: 0, files: new Set(), issuesBySource: {} };
    current.issueCount += 1;
    current.files.add(issue.file);
    const source = issue.source || 'unknown';
    current.issuesBySource[source] = (current.issuesBySource[source] || 0) + 1;
    moduleStats.set(moduleRoot, current);
  }
  const [moduleRoot, stats] = [...moduleStats.entries()].sort((left, right) => right[1].issueCount - left[1].issueCount)[0];
  const moduleKebab = moduleRoot.split('/').pop().replace(/^mango-/, '');
  const moduleVariables = variables(moduleKebab);
  const result = renderCodeBaseline({ templateId: 'business-module', targetDir: target, variables: moduleVariables });
  const contract = inspectRenderedContract(target, moduleVariables);
  const generatedQuality = cleanQualityMetrics(qualityEvidence);
  return {
    passed: contract.violations.length === 0 && (!qualityEvidence || generatedQuality.passed),
    sourceModule: moduleRoot,
    historicalIssues: stats.issueCount,
    historicalAffectedFiles: stats.files.size,
    historicalIssuesBySource: stats.issuesBySource,
    historicalIssuesPerAffectedFile: round(stats.issueCount / stats.files.size),
    generatedFiles: result.files.length,
    generatedContractViolations: contract.violations.length,
    generatedIssueCount: generatedQuality.totalIssueCount,
    issueReductionPercent: reductionPercent(stats.issueCount, generatedQuality.totalIssueCount),
    realModuleFilesModified: 0,
    generatedQuality,
  };
}

function validateQualityEvidence(evidence) {
  const clean = evidence?.cleanQualityEvidence;
  const controls = evidence?.negativeControls;
  if (
    evidence.schemaVersion !== 1
    || evidence.templateId !== 'business-module@1'
    || clean?.architecture?.mode !== 'full'
    || clean.architecture.blockingIssueCount !== 0
    || clean?.quality?.gate !== 'all'
    || clean.quality.passed !== true
    || clean.quality.totalIssueCount !== 0
    || clean.quality.newIssueCount !== 0
    || clean.quality.toolFailureCount !== 0
    || !controls
    || Object.values(controls).some((value) => value !== true)
  ) {
    throw new Error('invalid or incomplete generated backend quality evidence');
  }
  return evidence;
}

function cleanQualityMetrics(evidence) {
  if (!evidence) {
    return {
      evidence: 'not-provided',
      passed: null,
      totalIssueCount: null,
      newIssueCount: null,
      blockingArchitectureIssueCount: null,
      toolFailureCount: null,
    };
  }
  return {
    evidence: `Mango ${evidence.mangoVersion} full generated-backend gate`,
    passed: evidence.cleanQualityEvidence.quality.passed,
    totalIssueCount: evidence.cleanQualityEvidence.quality.totalIssueCount,
    newIssueCount: evidence.cleanQualityEvidence.quality.newIssueCount,
    blockingArchitectureIssueCount: evidence.cleanQualityEvidence.architecture.blockingIssueCount,
    toolFailureCount: evidence.cleanQualityEvidence.quality.toolFailureCount,
  };
}

function buildAssessment({ empty, legacy, mango, qualityEvidence }) {
  const hasActualQualityEvidence = Boolean(qualityEvidence);
  const improved = hasActualQualityEvidence
    && empty.passed
    && legacy.passed
    && mango.passed
    && legacy.comparableIssueReductionPercent === 100
    && mango.issueReductionPercent === 100;
  return {
    evidenceLevel: hasActualQualityEvidence ? 'mango-full-gate' : 'template-contract-only',
    improved: hasActualQualityEvidence ? improved : null,
    conclusion: hasActualQualityEvidence
      ? (improved
          ? 'Improved: generated code is clean under Mango gates and remains isolated from non-compliant legacy code.'
          : 'Not improved: at least one measured quality or isolation condition failed.')
      : 'Actual Mango quality evidence is required before deciding whether quality improved.',
    limitations: [
      'The baseline improves newly generated code; it does not claim to repair untouched legacy files.',
      'Historical Mango issue counts and generated-code issue counts use the same Mango quality sources, but different code sizes.',
    ],
  };
}

function inspectRenderedContract(root, values) {
  const template = loadCodeTemplateIndex().templates.find((item) => item.id === 'business-module');
  const violations = [];
  let representedConventions = 0;
  for (const convention of template.conventions) {
    const evidence = render(convention.evidence, values);
    const marker = render(convention.contains, values);
    const path = join(root, evidence);
    if (!existsSync(path) || !readFileSync(path, 'utf8').includes(marker)) {
      violations.push(convention.id);
    } else {
      representedConventions += 1;
    }
  }
  return { violations, representedConventions };
}

function variables(moduleKebab) {
  const modulePascal = pascal(moduleKebab);
  const moduleCamel = modulePascal.charAt(0).toLowerCase() + modulePascal.slice(1);
  return {
    moduleKebab,
    aggregateKebab: 'record',
    basePackage: 'io.mango.generated',
    groupId: 'io.mango.generated',
    projectKebab: 'baseline-evaluation',
    projectVersion: '1.0.0-SNAPSHOT',
    mangoAppRuntimeVersion: '1.0.2',
    moduleName: `${modulePascal}模块`,
    aggregateName: '业务记录',
    modulePackage: moduleCamel,
    modulePascal,
    moduleCamel,
    moduleBusinessDomainCode: moduleKebab.replaceAll('-', '_').toUpperCase(),
    moduleKebabSnake: moduleKebab.replaceAll('-', '_'),
    aggregatePascal: 'Record',
    aggregateCamel: 'record',
    aggregateKebabSnake: 'record',
    basePackagePath: 'io/mango/generated',
    businessResourceMenuId: '2690690000000000001',
  };
}

function pascal(value) {
  return value.split('-').map((part) => part.charAt(0).toUpperCase() + part.slice(1)).join('');
}

function render(value, values) {
  return Object.entries(values).reduce(
    (output, [name, replacement]) => output.replaceAll(`{{${name}}}`, replacement),
    value,
  );
}

function countMatches(root, pattern) {
  if (!existsSync(root)) return 0;
  let count = 0;
  for (const entry of readdirSync(root, { withFileTypes: true })) {
    const path = join(root, entry.name);
    if (entry.isDirectory()) count += countMatches(path, pattern);
    else if (entry.isFile()) count += readFileSync(path, 'utf8').match(pattern)?.length ?? 0;
  }
  return count;
}

function digest(path) {
  return createHash('sha256').update(readFileSync(path)).digest('hex');
}

function round(value) {
  return Math.round(value * 100) / 100;
}

function reductionPercent(before, after) {
  if (!Number.isFinite(after) || before <= 0) return null;
  return round(((before - after) / before) * 100);
}

function parseArgs(argv) {
  const args = { qualitySummary: '', output: '' };
  for (let index = 0; index < argv.length; index += 1) {
    const option = argv[index];
    const value = argv[index + 1];
    if (!['--quality-summary', '--output'].includes(option) || !value || value.startsWith('--')) {
      throw new Error(`usage: evaluate-code-baseline.mjs [--quality-summary <json>] [--output <json>]`);
    }
    args[option === '--quality-summary' ? 'qualitySummary' : 'output'] = value;
    index += 1;
  }
  return args;
}

if (process.argv[1] && resolve(process.argv[1]) === currentFile) {
  try {
    const args = parseArgs(process.argv.slice(2));
    const qualityEvidence = args.qualitySummary
      ? JSON.parse(readFileSync(resolve(args.qualitySummary), 'utf8'))
      : undefined;
    const result = evaluateCodeBaseline({ qualityEvidence });
    const content = `${JSON.stringify(result, null, 2)}\n`;
    if (args.output) {
      const output = resolve(args.output);
      mkdirSync(dirname(output), { recursive: true });
      writeFileSync(output, content);
    }
    process.stdout.write(content);
    process.exitCode = result.passed && result.assessment.improved !== false ? 0 : 1;
  } catch (error) {
    process.stderr.write(`Code baseline evaluation failed: ${error.message}\n`);
    process.exitCode = 1;
  }
}
