#!/usr/bin/env node
import { createHash } from 'node:crypto';
import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

const SCHEMA_VERSION = 3;
const REPORT_SCHEMA_VERSION = 1;
const ENGINE_FIELDS = {
  dependency: 'dependencyIssues',
  archunit: 'archUnitIssues',
  pmd: 'pmdIssues'
};
let cachedRepositoryRoot;

function parseArgs(argv) {
  const args = {
    report: '',
    baseline: '',
    baseRef: '',
    baseBudget: '',
    write: false,
    acceptIncrease: false,
    reason: '',
    json: false
  };
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--write') {
      args.write = true;
    } else if (arg === '--accept-increase') {
      args.acceptIncrease = true;
    } else if (arg === '--json') {
      args.json = true;
    } else if (['--report', '--baseline', '--base-ref', '--base-budget', '--reason'].includes(arg)) {
      const key = arg.slice(2).replace(/-([a-z])/g, (_, letter) => letter.toUpperCase());
      args[key] = argv[index + 1] || '';
      index += 1;
    } else {
      throw new Error(`unknown argument: ${arg}`);
    }
  }
  args.report ||= 'mango/target/mango-architecture-report.json';
  args.baseline ||= 'mango-pmo/baselines/architecture/debt-budget.json';
  if (args.baseRef && args.baseBudget) {
    throw new Error('--base-ref and --base-budget are mutually exclusive');
  }
  if (args.acceptIncrease && !args.write) {
    throw new Error('--accept-increase requires --write');
  }
  if (args.acceptIncrease && !args.reason.trim()) {
    throw new Error('--accept-increase requires a non-empty --reason');
  }
  return args;
}

function parseJson(text, label) {
  try {
    return JSON.parse(text);
  } catch (error) {
    throw new Error(`cannot parse ${label}: ${error.message}`);
  }
}

function readJsonDocument(file, label) {
  try {
    const text = fs.readFileSync(file, 'utf8');
    return { value: parseJson(text, `${label} ${file}`), text };
  } catch (error) {
    if (error.code === 'ENOENT') {
      throw new Error(`cannot read ${label} ${file}: file does not exist`);
    }
    throw error;
  }
}

function sha256(text) {
  return createHash('sha256').update(text).digest('hex');
}

function repositoryRoot() {
  if (cachedRepositoryRoot) {
    return cachedRepositoryRoot;
  }
  const repository = runGit(process.cwd(), ['rev-parse', '--show-toplevel'], 'cannot locate Git root');
  if (repository.status !== 0) {
    throw new Error(`cannot locate Git root: ${repository.stderr.trim()}`);
  }
  cachedRepositoryRoot = fs.realpathSync(repository.stdout.trim());
  return cachedRepositoryRoot;
}

function repositoryRelativeSource(file) {
  const absoluteFile = fs.realpathSync(path.resolve(file));
  const relative = path.relative(repositoryRoot(), absoluteFile).split(path.sep).join('/');
  if (!relative || relative === '..' || relative.startsWith('../')) {
    throw new Error(`architecture issue source must be inside the Git repository: ${file}`);
  }
  return relative;
}

function normalizedIssueSubject(subject) {
  const normalized = String(subject).replaceAll('\\', '/');
  const source = normalized.match(/^(.*\.java):(\d+)$/u);
  if (!source) {
    const mangoPath = normalized.indexOf('/mango/');
    return mangoPath >= 0 ? normalized.slice(mangoPath + 1) : normalized;
  }
  const [, file, rawLine] = source;
  let sourceLine;
  try {
    sourceLine = fs.readFileSync(file, 'utf8').split(/\r?\n/u)[Number(rawLine) - 1];
  } catch (error) {
    throw new Error(`cannot read architecture issue source ${subject}: ${error.message}`);
  }
  if (sourceLine === undefined) {
    throw new Error(`architecture issue source line is outside the file: ${subject}`);
  }
  const relative = repositoryRelativeSource(file);
  const normalizedLine = sourceLine.trim().replace(/\s+/gu, ' ');
  return `${relative}|source-line-sha256=${sha256(normalizedLine)}`;
}

function issueIdentity(issue) {
  return sha256(`${issue.ruleId}\0${normalizedIssueSubject(issue.subject)}\0${issue.message}`);
}

function validateReport(report) {
  if (report?.schemaVersion !== REPORT_SCHEMA_VERSION) {
    throw new Error(`architecture report schemaVersion must be ${REPORT_SCHEMA_VERSION}`);
  }
  if (report.inventoryScope !== 'full-reactor') {
    throw new Error('architecture report inventoryScope must be full-reactor');
  }
  if (report.issueInventory !== 'all-detected-issues') {
    throw new Error('architecture report issueInventory must be all-detected-issues');
  }
  if (!Number.isInteger(report.reactorProjectCount) || report.reactorProjectCount <= 0) {
    throw new Error('architecture report reactorProjectCount must be a positive integer');
  }
  if (report.reactorProjectCount !== report.expectedProjectCount) {
    throw new Error(
      `architecture report Reactor is incomplete: ${report.reactorProjectCount}/${report.expectedProjectCount}`
    );
  }
}

function budgetFromReport(report, reportPath) {
  validateReport(report);
  const engines = {};
  const rules = {};
  const identities = {};
  for (const [engine, field] of Object.entries(ENGINE_FIELDS)) {
    if (!Array.isArray(report[field])) {
      throw new Error(`architecture report is missing array ${field}`);
    }
    engines[engine] = report[field].length;
    for (const issue of report[field]) {
      if (typeof issue?.ruleId !== 'string' || !issue.ruleId.trim()) {
        throw new Error(`${field} contains an issue without ruleId`);
      }
      rules[issue.ruleId] = (rules[issue.ruleId] || 0) + 1;
      const identity = issueIdentity(issue);
      identities[identity] = (identities[identity] || 0) + 1;
    }
  }
  return {
    schemaVersion: SCHEMA_VERSION,
    generatedAt: new Date().toISOString(),
    sourceReport: path.basename(reportPath),
    acceptedIncreaseReason: null,
    acceptedIncreaseFromSha256: null,
    totalIssueCount: Object.values(engines).reduce((sum, count) => sum + count, 0),
    engines,
    rules: Object.fromEntries(
      Object.entries(rules).sort(([left], [right]) => left.localeCompare(right))
    ),
    identities: Object.fromEntries(
      Object.entries(identities).sort(([left], [right]) => left.localeCompare(right))
    )
  };
}

function withAcceptedIncrease(budget, reason, previousSha256) {
  return {
    ...budget,
    acceptedIncreaseReason: reason.trim(),
    acceptedIncreaseFromSha256: previousSha256
  };
}

function validateBudget(budget, label) {
  if (budget?.schemaVersion !== SCHEMA_VERSION) {
    throw new Error(`${label} schemaVersion must be ${SCHEMA_VERSION}`);
  }
  if (!Number.isInteger(budget.totalIssueCount) || budget.totalIssueCount < 0) {
    throw new Error(`${label} totalIssueCount must be a non-negative integer`);
  }
  for (const engine of Object.keys(ENGINE_FIELDS)) {
    if (!Number.isInteger(budget.engines?.[engine]) || budget.engines[engine] < 0) {
      throw new Error(`${label} engines.${engine} must be a non-negative integer`);
    }
  }
  for (const [ruleId, count] of Object.entries(budget.rules || {})) {
    if (!ruleId || !Number.isInteger(count) || count < 0) {
      throw new Error(`${label} contains invalid rule budget: ${ruleId}`);
    }
  }
  for (const [identity, count] of Object.entries(budget.identities || {})) {
    if (!/^[a-f0-9]{64}$/u.test(identity) || !Number.isInteger(count) || count < 0) {
      throw new Error(`${label} contains invalid issue identity budget: ${identity}`);
    }
  }
  const reason = budget.acceptedIncreaseReason;
  const previousSha = budget.acceptedIncreaseFromSha256;
  if ((reason === null) !== (previousSha === null)) {
    throw new Error(`${label} accepted increase reason and source digest must both be null or set`);
  }
  if (reason !== null && (typeof reason !== 'string' || !reason.trim())) {
    throw new Error(`${label} acceptedIncreaseReason must be non-empty when set`);
  }
  if (previousSha !== null && !/^[a-f0-9]{64}$/u.test(previousSha)) {
    throw new Error(`${label} acceptedIncreaseFromSha256 must be a SHA-256 digest`);
  }
  const engineTotal = Object.values(budget.engines).reduce((sum, count) => sum + count, 0);
  const ruleTotal = Object.values(budget.rules || {}).reduce((sum, count) => sum + count, 0);
  const identityTotal = Object.values(budget.identities || {}).reduce((sum, count) => sum + count, 0);
  if (engineTotal !== budget.totalIssueCount
    || ruleTotal !== budget.totalIssueCount
    || identityTotal !== budget.totalIssueCount) {
    throw new Error(`${label} totals are inconsistent`);
  }
}

function compareBudgets(previousBudget, nextBudget) {
  const increases = [];
  const reductions = [];
  const ruleIds = new Set([
    ...Object.keys(previousBudget.rules),
    ...Object.keys(nextBudget.rules)
  ]);
  for (const ruleId of [...ruleIds].sort()) {
    const previous = previousBudget.rules[ruleId] || 0;
    const current = nextBudget.rules[ruleId] || 0;
    if (current > previous) {
      increases.push({ ruleId, previous, current, delta: current - previous });
    } else if (current < previous) {
      reductions.push({ ruleId, previous, current, delta: current - previous });
    }
  }
  const identityIncreases = [];
  const identityReductions = [];
  const identities = new Set([
    ...Object.keys(previousBudget.identities),
    ...Object.keys(nextBudget.identities)
  ]);
  for (const identity of [...identities].sort()) {
    const previous = previousBudget.identities[identity] || 0;
    const current = nextBudget.identities[identity] || 0;
    if (current > previous) {
      identityIncreases.push({ identity, previous, current, delta: current - previous });
    } else if (current < previous) {
      identityReductions.push({ identity, previous, current, delta: current - previous });
    }
  }
  return {
    increases,
    reductions,
    identityIncreases,
    identityReductions,
    totalDelta: nextBudget.totalIssueCount - previousBudget.totalIssueCount
  };
}

function writeBudget(file, budget) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  const temporary = `${file}.tmp`;
  fs.writeFileSync(temporary, `${JSON.stringify(budget, null, 2)}\n`);
  fs.renameSync(temporary, file);
}

function runGit(cwd, args, label) {
  const result = spawnSync('git', ['-C', cwd, ...args], { encoding: 'utf8' });
  if (result.error) {
    throw new Error(`${label}: ${result.error.message}`);
  }
  return result;
}

function readBaseFromGit(baseRef, baselinePath) {
  if (!/^[A-Za-z0-9][A-Za-z0-9._/-]*$/u.test(baseRef) || baseRef.includes('..')) {
    throw new Error(`invalid --base-ref: ${baseRef}`);
  }
  const repository = runGit(process.cwd(), ['rev-parse', '--show-toplevel'], 'cannot locate Git root');
  if (repository.status !== 0) {
    throw new Error(`cannot locate Git root: ${repository.stderr.trim()}`);
  }
  const root = fs.realpathSync(repository.stdout.trim());
  const normalizedBaselinePath = fs.realpathSync(baselinePath);
  const relative = path.relative(root, normalizedBaselinePath).split(path.sep).join('/');
  if (!relative || relative === '..' || relative.startsWith('../')) {
    throw new Error(`baseline must be inside the Git repository when using --base-ref: ${baselinePath}`);
  }
  const commit = runGit(root, ['cat-file', '-e', `${baseRef}^{commit}`], 'cannot resolve base ref');
  if (commit.status !== 0) {
    throw new Error(`cannot resolve base ref ${baseRef}: ${commit.stderr.trim()}`);
  }
  const listing = runGit(root, ['ls-tree', '-r', '--name-only', baseRef, '--', relative], 'cannot inspect base budget');
  if (listing.status !== 0) {
    throw new Error(`cannot inspect base budget at ${baseRef}: ${listing.stderr.trim()}`);
  }
  if (!listing.stdout.trim()) {
    return { missing: true, source: `${baseRef}:${relative}` };
  }
  const shown = runGit(root, ['show', `${baseRef}:${relative}`], 'cannot read base budget');
  if (shown.status !== 0) {
    throw new Error(`cannot read base budget at ${baseRef}:${relative}: ${shown.stderr.trim()}`);
  }
  return {
    missing: false,
    source: `${baseRef}:${relative}`,
    text: shown.stdout,
    value: parseJson(shown.stdout, `base architecture budget ${baseRef}:${relative}`)
  };
}

function readBaseBudget(args, baselinePath) {
  if (args.baseBudget) {
    const file = path.resolve(args.baseBudget);
    const document = readJsonDocument(file, 'base architecture budget');
    return { missing: false, source: file, ...document };
  }
  if (args.baseRef) {
    return readBaseFromGit(args.baseRef, baselinePath);
  }
  return null;
}

function checkAgainstBase(baseDocument, baseline, current) {
  if (!baseDocument) {
    return { passed: true, action: 'check', baseline, current };
  }
  if (baseDocument.missing) {
    if (baseline.acceptedIncreaseReason !== null) {
      return {
        passed: false,
        action: 'invalid-initial-budget',
        message: 'An initial debt budget cannot contain accepted-increase metadata.',
        baseline,
        current
      };
    }
    return {
      passed: true,
      action: 'initialized-against-base',
      baseSource: baseDocument.source,
      baseline,
      current
    };
  }

  validateBudget(baseDocument.value, 'base architecture budget');
  const baseComparison = compareBudgets(baseDocument.value, baseline);
  if (baseComparison.increases.length > 0 || baseComparison.identityIncreases.length > 0) {
    const expectedDigest = sha256(baseDocument.text);
    if (!baseline.acceptedIncreaseReason
      || baseline.acceptedIncreaseFromSha256 !== expectedDigest) {
      return {
        passed: false,
        action: 'base-budget-increase',
        message: 'The committed budget increased relative to the PR base without a fresh reason bound to the base budget.',
        base: baseDocument.value,
        baseline,
        current,
        baseComparison
      };
    }
    return {
      passed: true,
      action: 'governed-increase-recorded',
      base: baseDocument.value,
      baseline,
      current,
      baseComparison
    };
  }
  if (baseline.acceptedIncreaseReason !== null) {
    return {
      passed: false,
      action: 'stale-increase-reason',
      message: 'Accepted-increase metadata is stale because the budget did not increase relative to the PR base.',
      base: baseDocument.value,
      baseline,
      current,
      baseComparison
    };
  }
  return {
    passed: true,
    action: 'check',
    base: baseDocument.value,
    baseline,
    current,
    baseComparison
  };
}

function resultFor(args) {
  const reportPath = path.resolve(args.report);
  const baselinePath = path.resolve(args.baseline);
  const report = readJsonDocument(reportPath, 'architecture report').value;
  const current = budgetFromReport(report, reportPath);
  validateBudget(current, 'current architecture budget');

  if (!fs.existsSync(baselinePath)) {
    if (!args.write) {
      return {
        passed: false,
        action: 'initialize',
        message: `Architecture debt budget is missing: ${baselinePath}`,
        current
      };
    }
    if (args.acceptIncrease) {
      throw new Error('--accept-increase cannot initialize a missing budget');
    }
    writeBudget(baselinePath, current);
    return { passed: true, action: 'initialized', baselinePath, current };
  }

  const baselineDocument = readJsonDocument(baselinePath, 'architecture debt budget');
  const baseline = baselineDocument.value;
  validateBudget(baseline, 'architecture debt budget');
  const comparison = compareBudgets(baseline, current);

  const hasIncrease = comparison.increases.length > 0 || comparison.identityIncreases.length > 0;
  if (hasIncrease && !args.acceptIncrease) {
    return {
      passed: false,
      action: args.write ? 'write-refused' : 'check',
      message: 'Architecture debt increased; existing debt cannot authorize new violations.',
      baseline,
      current,
      comparison
    };
  }

  if (args.write) {
    const updated = hasIncrease
      ? withAcceptedIncrease(current, args.reason, sha256(baselineDocument.text))
      : current;
    writeBudget(baselinePath, updated);
    return {
      passed: true,
      action: hasIncrease ? 'increase-accepted' : 'ratcheted',
      baselinePath,
      current: updated,
      comparison
    };
  }

  if (comparison.reductions.length > 0 || comparison.identityReductions.length > 0) {
    return {
      passed: false,
      action: 'ratchet-required',
      message: 'Architecture debt decreased; lower the committed budget with --write.',
      baseline,
      current,
      comparison
    };
  }

  const baseDocument = readBaseBudget(args, baselinePath);
  return checkAgainstBase(baseDocument, baseline, current);
}

function printComparison(comparison, prefix = '') {
  for (const item of comparison?.increases || []) {
    console.error(`- ${prefix}INCREASE ${item.ruleId}: ${item.previous} -> ${item.current}`);
  }
  for (const item of comparison?.reductions || []) {
    console.error(`- ${prefix}REDUCTION ${item.ruleId}: ${item.previous} -> ${item.current}`);
  }
  for (const item of comparison?.identityIncreases || []) {
    console.error(`- ${prefix}IDENTITY INCREASE ${item.identity}: ${item.previous} -> ${item.current}`);
  }
  for (const item of comparison?.identityReductions || []) {
    console.error(`- ${prefix}IDENTITY REDUCTION ${item.identity}: ${item.previous} -> ${item.current}`);
  }
}

function printText(result) {
  const summary = result.current
    ? `current=${result.current.totalIssueCount}`
    : 'current=unknown';
  if (result.passed) {
    console.log(`Architecture debt budget PASS: ${summary}, action=${result.action}`);
    return;
  }
  console.error(`Architecture debt budget FAIL: ${summary}, action=${result.action}`);
  if (result.message) {
    console.error(result.message);
  }
  printComparison(result.comparison);
  printComparison(result.baseComparison, 'BASE ');
}

try {
  const args = parseArgs(process.argv.slice(2));
  const result = resultFor(args);
  if (args.json) {
    console.log(JSON.stringify(result, null, 2));
  } else {
    printText(result);
  }
  process.exit(result.passed ? 0 : 1);
} catch (error) {
  console.error(`Architecture debt budget check failed: ${error.message}`);
  process.exit(2);
}
