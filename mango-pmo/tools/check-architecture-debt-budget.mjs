#!/usr/bin/env node
import { createHash } from 'node:crypto';
import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';

const SCHEMA_VERSION = 4;
const LEGACY_SCHEMA_VERSION = 3;
const REPORT_SCHEMA_VERSION = 2;
const GIT_OUTPUT_MAX_BYTES = 64 * 1024 * 1024;
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
    modules: [],
    write: false,
    acceptIncrease: false,
    baselineOnly: false,
    reason: '',
    json: false
  };
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--write') {
      args.write = true;
    } else if (arg === '--accept-increase') {
      args.acceptIncrease = true;
    } else if (arg === '--baseline-only') {
      args.baselineOnly = true;
    } else if (arg === '--json') {
      args.json = true;
    } else if (arg === '--module') {
      args.modules.push(argv[index + 1] || '');
      index += 1;
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
  if (args.modules.some(module => !module.trim())) {
    throw new Error('--module requires a non-empty selector');
  }
  if (args.acceptIncrease && !args.write) {
    throw new Error('--accept-increase requires --write');
  }
  if (args.acceptIncrease && !args.reason.trim()) {
    throw new Error('--accept-increase requires a non-empty --reason');
  }
  if (args.acceptIncrease && args.modules.length > 0) {
    throw new Error('--module cannot be combined with --accept-increase');
  }
  if (args.baselineOnly && (args.write || args.acceptIncrease || args.modules.length > 0)) {
    throw new Error('--baseline-only cannot be combined with --write, --accept-increase, or --module');
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

function validModuleKey(moduleKey) {
  if (moduleKey === '.') {
    return true;
  }
  if (typeof moduleKey !== 'string'
    || !moduleKey
    || moduleKey.startsWith('/')
    || moduleKey.endsWith('/')
    || moduleKey.includes('\\')) {
    return false;
  }
  return moduleKey.split('/').every(segment => segment && segment !== '.' && segment !== '..');
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
  if (!Array.isArray(report.modules) || report.modules.length !== report.reactorProjectCount) {
    throw new Error('architecture report modules must describe every Reactor project');
  }
  const moduleKeys = new Set();
  const coordinates = new Set();
  for (const module of report.modules) {
    if (!validModuleKey(module?.moduleKey)
      || typeof module.groupId !== 'string'
      || !module.groupId.trim()
      || typeof module.artifactId !== 'string'
      || !module.artifactId.trim()) {
      throw new Error('architecture report contains invalid module metadata');
    }
    if (!moduleKeys.add(module.moduleKey)) {
      throw new Error(`architecture report contains duplicate moduleKey: ${module.moduleKey}`);
    }
    const coordinatesKey = `${module.groupId}:${module.artifactId}`;
    if (!coordinates.add(coordinatesKey)) {
      throw new Error(`architecture report contains duplicate Maven coordinates: ${coordinatesKey}`);
    }
  }
  return moduleKeys;
}

function emptyCounters() {
  return {
    totalIssueCount: 0,
    engines: Object.fromEntries(Object.keys(ENGINE_FIELDS).map(engine => [engine, 0])),
    rules: {},
    identities: {}
  };
}

function sortedCounts(counts) {
  return Object.fromEntries(
    Object.entries(counts).sort(([left], [right]) => left.localeCompare(right))
  );
}

function normalizeCounters(counters) {
  return {
    totalIssueCount: counters.totalIssueCount,
    engines: Object.fromEntries(
      Object.keys(ENGINE_FIELDS).map(engine => [engine, counters.engines[engine] || 0])
    ),
    rules: sortedCounts(counters.rules),
    identities: sortedCounts(counters.identities)
  };
}

function incrementIssue(counters, engine, issue) {
  counters.totalIssueCount += 1;
  counters.engines[engine] += 1;
  counters.rules[issue.ruleId] = (counters.rules[issue.ruleId] || 0) + 1;
  const identity = issueIdentity(issue);
  counters.identities[identity] = (counters.identities[identity] || 0) + 1;
}

function aggregateModules(modules) {
  const aggregate = emptyCounters();
  for (const module of Object.values(modules)) {
    aggregate.totalIssueCount += module.totalIssueCount;
    for (const engine of Object.keys(ENGINE_FIELDS)) {
      aggregate.engines[engine] += module.engines[engine];
    }
    for (const [ruleId, count] of Object.entries(module.rules)) {
      aggregate.rules[ruleId] = (aggregate.rules[ruleId] || 0) + count;
    }
    for (const [identity, count] of Object.entries(module.identities)) {
      aggregate.identities[identity] = (aggregate.identities[identity] || 0) + count;
    }
  }
  return normalizeCounters(aggregate);
}

function budgetWithModules(modules, sourceReport, acceptedIncreaseReason = null, acceptedIncreaseFromSha256 = null) {
  const sortedModules = Object.fromEntries(
    Object.entries(modules)
      .sort(([left], [right]) => left.localeCompare(right))
      .map(([moduleKey, module]) => [moduleKey, {
        groupId: module.groupId,
        artifactId: module.artifactId,
        ...normalizeCounters(module)
      }])
  );
  return {
    schemaVersion: SCHEMA_VERSION,
    generatedAt: new Date().toISOString(),
    sourceReport,
    acceptedIncreaseReason,
    acceptedIncreaseFromSha256,
    ...aggregateModules(sortedModules),
    modules: sortedModules
  };
}

function budgetFromReport(report, reportPath) {
  const moduleKeys = validateReport(report);
  const modules = {};
  for (const module of report.modules) {
    modules[module.moduleKey] = {
      groupId: module.groupId,
      artifactId: module.artifactId,
      ...emptyCounters()
    };
  }
  for (const [engine, field] of Object.entries(ENGINE_FIELDS)) {
    if (!Array.isArray(report[field])) {
      throw new Error(`architecture report is missing array ${field}`);
    }
    for (const issue of report[field]) {
      if (typeof issue?.ruleId !== 'string' || !issue.ruleId.trim()) {
        throw new Error(`${field} contains an issue without ruleId`);
      }
      if (typeof issue.subject !== 'string' || typeof issue.message !== 'string') {
        throw new Error(`${field} contains an issue without subject or message`);
      }
      if (!moduleKeys.has(issue.moduleKey)) {
        throw new Error(`${field} contains an issue with unknown moduleKey: ${issue.moduleKey}`);
      }
      incrementIssue(modules[issue.moduleKey], engine, issue);
    }
  }
  return budgetWithModules(modules, path.basename(reportPath));
}

function withAcceptedIncrease(budget, reason, previousSha256) {
  return {
    ...budget,
    acceptedIncreaseReason: reason.trim(),
    acceptedIncreaseFromSha256: previousSha256
  };
}

function validateAcceptedIncrease(budget, label) {
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
}

function validateCounters(counters, label) {
  if (!Number.isInteger(counters?.totalIssueCount) || counters.totalIssueCount < 0) {
    throw new Error(`${label} totalIssueCount must be a non-negative integer`);
  }
  const engineKeys = Object.keys(counters.engines || {}).sort();
  const expectedEngineKeys = Object.keys(ENGINE_FIELDS).sort();
  if (JSON.stringify(engineKeys) !== JSON.stringify(expectedEngineKeys)) {
    throw new Error(`${label} engines must contain exactly ${expectedEngineKeys.join(', ')}`);
  }
  for (const engine of expectedEngineKeys) {
    if (!Number.isInteger(counters.engines[engine]) || counters.engines[engine] < 0) {
      throw new Error(`${label} engines.${engine} must be a non-negative integer`);
    }
  }
  for (const [ruleId, count] of Object.entries(counters.rules || {})) {
    if (!ruleId || !Number.isInteger(count) || count < 0) {
      throw new Error(`${label} contains invalid rule budget: ${ruleId}`);
    }
  }
  for (const [identity, count] of Object.entries(counters.identities || {})) {
    if (!/^[a-f0-9]{64}$/u.test(identity) || !Number.isInteger(count) || count < 0) {
      throw new Error(`${label} contains invalid issue identity budget: ${identity}`);
    }
  }
  const engineTotal = Object.values(counters.engines).reduce((sum, count) => sum + count, 0);
  const ruleTotal = Object.values(counters.rules || {}).reduce((sum, count) => sum + count, 0);
  const identityTotal = Object.values(counters.identities || {}).reduce((sum, count) => sum + count, 0);
  if (engineTotal !== counters.totalIssueCount
    || ruleTotal !== counters.totalIssueCount
    || identityTotal !== counters.totalIssueCount) {
    throw new Error(`${label} totals are inconsistent`);
  }
}

function sameCounts(left, right) {
  const keys = new Set([...Object.keys(left || {}), ...Object.keys(right || {})]);
  return [...keys].every(key => (left?.[key] || 0) === (right?.[key] || 0));
}

function validateBudget(budget, label, options = {}) {
  if (budget?.schemaVersion === LEGACY_SCHEMA_VERSION && options.allowLegacy) {
    validateCounters(budget, label);
    validateAcceptedIncrease(budget, label);
    return LEGACY_SCHEMA_VERSION;
  }
  if (budget?.schemaVersion !== SCHEMA_VERSION) {
    throw new Error(`${label} schemaVersion must be ${SCHEMA_VERSION}`);
  }
  validateCounters(budget, label);
  validateAcceptedIncrease(budget, label);
  if (!budget.modules || typeof budget.modules !== 'object' || Array.isArray(budget.modules)) {
    throw new Error(`${label} modules must be an object`);
  }
  const coordinates = new Set();
  for (const [moduleKey, module] of Object.entries(budget.modules)) {
    if (!validModuleKey(moduleKey)
      || typeof module?.groupId !== 'string'
      || !module.groupId.trim()
      || typeof module.artifactId !== 'string'
      || !module.artifactId.trim()) {
      throw new Error(`${label} contains invalid module metadata: ${moduleKey}`);
    }
    const coordinatesKey = `${module.groupId}:${module.artifactId}`;
    if (!coordinates.add(coordinatesKey)) {
      throw new Error(`${label} contains duplicate Maven coordinates: ${coordinatesKey}`);
    }
    validateCounters(module, `${label} module ${moduleKey}`);
  }
  const aggregate = aggregateModules(budget.modules);
  if (aggregate.totalIssueCount !== budget.totalIssueCount
    || !sameCounts(aggregate.engines, budget.engines)
    || !sameCounts(aggregate.rules, budget.rules)
    || !sameCounts(aggregate.identities, budget.identities)) {
    throw new Error(`${label} module aggregates do not match the global budget`);
  }
  return SCHEMA_VERSION;
}

function compareCounters(previousBudget, nextBudget) {
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

function countersForMissingModule(reference = {}) {
  return {
    groupId: reference.groupId || '',
    artifactId: reference.artifactId || '',
    ...emptyCounters()
  };
}

function hasCounterIncrease(comparison) {
  return comparison.increases.length > 0 || comparison.identityIncreases.length > 0;
}

function hasCounterReduction(comparison) {
  return comparison.reductions.length > 0 || comparison.identityReductions.length > 0;
}

function compareBudgets(previousBudget, nextBudget) {
  const comparison = compareCounters(previousBudget, nextBudget);
  const moduleComparisons = [];
  if (previousBudget.schemaVersion === SCHEMA_VERSION && nextBudget.schemaVersion === SCHEMA_VERSION) {
    const moduleKeys = new Set([
      ...Object.keys(previousBudget.modules),
      ...Object.keys(nextBudget.modules)
    ]);
    for (const moduleKey of [...moduleKeys].sort()) {
      const previousModule = previousBudget.modules[moduleKey]
        || countersForMissingModule(nextBudget.modules[moduleKey]);
      const nextModule = nextBudget.modules[moduleKey]
        || countersForMissingModule(previousBudget.modules[moduleKey]);
      const moduleComparison = compareCounters(previousModule, nextModule);
      const metadataChanged = Boolean(
        previousBudget.modules[moduleKey]
        && nextBudget.modules[moduleKey]
        && (previousModule.groupId !== nextModule.groupId
          || previousModule.artifactId !== nextModule.artifactId)
      );
      if (hasCounterIncrease(moduleComparison)
        || hasCounterReduction(moduleComparison)
        || metadataChanged) {
        moduleComparisons.push({ moduleKey, metadataChanged, ...moduleComparison });
      }
    }
  }
  return { ...comparison, moduleComparisons };
}

function hasBudgetIncrease(comparison) {
  return hasCounterIncrease(comparison)
    || comparison.moduleComparisons.some(item => hasCounterIncrease(item) || item.metadataChanged);
}

function hasBudgetReduction(comparison) {
  return hasCounterReduction(comparison)
    || comparison.moduleComparisons.some(hasCounterReduction);
}

function writeBudget(file, budget) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  const temporary = `${file}.tmp`;
  fs.writeFileSync(temporary, `${JSON.stringify(budget, null, 2)}\n`);
  fs.renameSync(temporary, file);
}

function runGit(cwd, args, label) {
  const result = spawnSync('git', ['-C', cwd, ...args], {
    encoding: 'utf8',
    maxBuffer: GIT_OUTPUT_MAX_BYTES
  });
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

  validateBudget(baseDocument.value, 'base architecture budget', { allowLegacy: true });
  const baseComparison = compareBudgets(baseDocument.value, baseline);
  if (hasBudgetIncrease(baseComparison)) {
    const expectedDigest = sha256(baseDocument.text);
    if (!baseline.acceptedIncreaseReason
      || baseline.acceptedIncreaseFromSha256 !== expectedDigest) {
      return {
        passed: false,
        action: 'base-budget-increase',
        message: 'The committed budget increased or moved debt relative to the PR base without a fresh reason bound to the base budget.',
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
      passed: true,
      action: 'historical-increase-recorded',
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

function resolveModuleSelectors(current, selectors) {
  const selected = new Set();
  const entries = Object.entries(current.modules);
  for (const rawSelector of selectors) {
    const selector = rawSelector.trim().replace(/\/+$/u, '') || '.';
    if (!validModuleKey(selector)) {
      throw new Error(`invalid --module selector: ${rawSelector}`);
    }
    let matches = entries
      .filter(([moduleKey]) => moduleKey === selector || moduleKey.startsWith(`${selector}/`))
      .map(([moduleKey]) => moduleKey);
    if (matches.length === 0) {
      matches = entries
        .filter(([, module]) => module.artifactId === selector)
        .map(([moduleKey]) => moduleKey);
      if (matches.length > 1) {
        throw new Error(`ambiguous --module artifactId ${selector}: ${matches.join(', ')}`);
      }
    }
    if (matches.length === 0) {
      throw new Error(`unknown --module selector: ${selector}`);
    }
    const overlap = matches.find(moduleKey => selected.has(moduleKey));
    if (overlap) {
      throw new Error(`overlapping --module selectors include ${overlap}`);
    }
    matches.forEach(moduleKey => selected.add(moduleKey));
  }
  return [...selected].sort();
}

function projectBudget(budget, moduleKeys, catalog = {}) {
  const modules = {};
  for (const moduleKey of moduleKeys) {
    modules[moduleKey] = budget.modules?.[moduleKey]
      || countersForMissingModule(catalog[moduleKey]);
  }
  return budgetWithModules(modules, budget.sourceReport || 'module-selection');
}

function mergeSelectedModules(baseline, current, moduleKeys) {
  const modules = { ...baseline.modules };
  for (const moduleKey of moduleKeys) {
    modules[moduleKey] = current.modules[moduleKey];
  }
  return budgetWithModules(modules, current.sourceReport);
}

function resultForModules(args, baselineDocument, current, baseline, baselinePath) {
  const selectedModules = resolveModuleSelectors(current, args.modules);
  const selectedBaseline = projectBudget(baseline, selectedModules, current.modules);
  const selectedCurrent = projectBudget(current, selectedModules, current.modules);
  const comparison = compareBudgets(selectedBaseline, selectedCurrent);
  if (hasBudgetIncrease(comparison)) {
    return {
      passed: false,
      action: args.write ? 'write-refused' : 'check',
      message: 'Architecture debt increased or moved in the selected modules.',
      selectedModules,
      baseline: selectedBaseline,
      current: selectedCurrent,
      comparison
    };
  }
  if (args.write) {
    const updated = mergeSelectedModules(baseline, current, selectedModules);
    validateBudget(updated, 'updated architecture debt budget');
    writeBudget(baselinePath, updated);
    return {
      passed: true,
      action: 'module-ratcheted',
      selectedModules,
      baselinePath,
      current: projectBudget(updated, selectedModules, current.modules),
      comparison
    };
  }
  if (hasBudgetReduction(comparison)) {
    return {
      passed: false,
      action: 'ratchet-required',
      message: 'Architecture debt decreased in the selected modules; lower their committed budget with --write.',
      selectedModules,
      baseline: selectedBaseline,
      current: selectedCurrent,
      comparison
    };
  }
  const baseDocument = readBaseBudget(args, baselinePath);
  if (baseDocument && !baseDocument.missing) {
    const baseSchema = validateBudget(
      baseDocument.value,
      'base architecture budget',
      { allowLegacy: true }
    );
    if (baseSchema !== SCHEMA_VERSION) {
      throw new Error('module --base-ref requires a schemaVersion 4 base budget');
    }
    baseDocument.value = projectBudget(baseDocument.value, selectedModules, current.modules);
  }
  const result = checkAgainstBase(baseDocument, selectedBaseline, selectedCurrent);
  return { ...result, selectedModules };
}

function resultForLegacyMigration(args, baselineDocument, current, baselinePath) {
  if (args.modules.length > 0) {
    throw new Error('--module requires a schemaVersion 4 current budget');
  }
  const baseline = baselineDocument.value;
  const comparison = compareBudgets(baseline, current);
  const hasIncrease = hasBudgetIncrease(comparison);
  if (hasIncrease && !args.acceptIncrease) {
    return {
      passed: false,
      action: args.write ? 'write-refused' : 'migration-required',
      message: 'Schema migration also increases architecture debt; explicit global governance is required.',
      baseline,
      current,
      comparison
    };
  }
  if (!args.write) {
    return {
      passed: false,
      action: 'migration-required',
      message: 'Architecture debt budget schemaVersion 3 must be migrated with a full report and --write.',
      baseline,
      current,
      comparison
    };
  }
  const updated = hasIncrease
    ? withAcceptedIncrease(current, args.reason, sha256(baselineDocument.text))
    : current;
  writeBudget(baselinePath, updated);
  return {
    passed: true,
    action: hasIncrease ? 'increase-accepted' : 'migrated',
    baselinePath,
    current: updated,
    comparison
  };
}

function resultForGlobal(args, baselineDocument, current, baseline, baselinePath) {
  const comparison = compareBudgets(baseline, current);
  const hasIncrease = hasBudgetIncrease(comparison);
  if (hasIncrease && !args.acceptIncrease) {
    return {
      passed: false,
      action: args.write ? 'write-refused' : 'check',
      message: 'Architecture debt increased or moved; existing debt cannot authorize new violations.',
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
  if (hasBudgetReduction(comparison)) {
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

function resultFor(args) {
  const baselinePath = path.resolve(args.baseline);
  if (args.baselineOnly) {
    if (!fs.existsSync(baselinePath)) {
      return {
        passed: false,
        action: 'initialize',
        message: `Architecture debt budget is missing: ${baselinePath}`
      };
    }
    const baselineDocument = readJsonDocument(baselinePath, 'architecture debt budget');
    const baseline = baselineDocument.value;
    validateBudget(baseline, 'architecture debt budget', { allowLegacy: true });
    const baseDocument = readBaseBudget(args, baselinePath);
    if (!baseDocument) {
      throw new Error('--baseline-only requires --base-ref or --base-budget');
    }
    return checkAgainstBase(baseDocument, baseline, baseline);
  }

  const reportPath = path.resolve(args.report);
  const report = readJsonDocument(reportPath, 'architecture report').value;
  const current = budgetFromReport(report, reportPath);
  validateBudget(current, 'current architecture budget');

  if (!fs.existsSync(baselinePath)) {
    if (!args.write || args.modules.length > 0) {
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
  const schemaVersion = validateBudget(
    baseline,
    'architecture debt budget',
    { allowLegacy: true }
  );
  if (schemaVersion === LEGACY_SCHEMA_VERSION) {
    return resultForLegacyMigration(args, baselineDocument, current, baselinePath);
  }
  if (args.modules.length > 0) {
    return resultForModules(args, baselineDocument, current, baseline, baselinePath);
  }
  return resultForGlobal(args, baselineDocument, current, baseline, baselinePath);
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
  for (const moduleComparison of comparison?.moduleComparisons || []) {
    if (moduleComparison.metadataChanged) {
      console.error(`- ${prefix}MODULE METADATA CHANGED ${moduleComparison.moduleKey}`);
    }
    printComparison(moduleComparison, `${prefix}MODULE ${moduleComparison.moduleKey} `);
  }
}

function printText(result) {
  const summary = result.current
    ? `current=${result.current.totalIssueCount}`
    : 'current=unknown';
  const moduleSummary = result.selectedModules?.length
    ? `, modules=${result.selectedModules.join(',')}`
    : '';
  if (result.passed) {
    console.log(`Architecture debt budget PASS: ${summary}${moduleSummary}, action=${result.action}`);
    return;
  }
  console.error(`Architecture debt budget FAIL: ${summary}${moduleSummary}, action=${result.action}`);
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
  process.exitCode = result.passed ? 0 : 1;
} catch (error) {
  console.error(`Architecture debt budget check failed: ${error.message}`);
  process.exitCode = 2;
}
