#!/usr/bin/env node
import fs from 'node:fs';
import crypto from 'node:crypto';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const defaultCatalog = path.join(repoRoot, 'mango-pmo/fixtures/mutations/cases.json');

function parseArgs(argv) {
  const args = {
    head: 'HEAD',
    catalog: defaultCatalog,
    report: '.runtime/pmo/targeted-mutations.json',
    caseId: ''
  };
  const allowed = new Set(['head', 'catalog', 'report', 'case']);
  for (let index = 0; index < argv.length; index += 1) {
    const option = argv[index];
    if (!option.startsWith('--') || !allowed.has(option.slice(2))) {
      throw new Error(`Unknown option: ${option}`);
    }
    const value = argv[index + 1];
    if (value === undefined || value.startsWith('--')) {
      throw new Error(`Missing value: ${option}`);
    }
    if (option === '--case') args.caseId = value;
    else args[option.slice(2)] = value;
    index += 1;
  }
  args.catalog = path.resolve(repoRoot, args.catalog);
  args.report = path.resolve(repoRoot, args.report);
  return args;
}

function run(command, cwd, timeout = 180000) {
  const result = spawnSync(command[0], command.slice(1), {
    cwd,
    encoding: 'utf8',
    stdio: 'pipe',
    timeout,
    env: {
      ...process.env,
      TZ: 'UTC'
    }
  });
  return {
    status: result.status,
    signal: result.signal,
    timedOut: Boolean(result.error?.code === 'ETIMEDOUT'),
    output: `${result.stdout || ''}\n${result.stderr || ''}`.trim()
  };
}

function tail(text, lines = 30) {
  return text.split('\n').slice(-lines).join('\n');
}

function hashFile(file) {
  return crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
}

function xmlAttribute(source, name) {
  const match = source.match(new RegExp(`\\b${name}="([^"]*)"`));
  return match ? match[1] : '';
}

function parseSurefireReport(file, item) {
  if (!fs.existsSync(file)) return { valid: false, reason: `Surefire report not found: ${file}` };
  const source = fs.readFileSync(file, 'utf8');
  const suiteTag = source.match(/<testsuite\b[^>]*>/)?.[0] || '';
  const tests = Number(xmlAttribute(suiteTag, 'tests'));
  const failures = Number(xmlAttribute(suiteTag, 'failures'));
  const errors = Number(xmlAttribute(suiteTag, 'errors'));
  const skipped = Number(xmlAttribute(suiteTag, 'skipped'));
  const testcasePattern = new RegExp(`<testcase\\b(?=[^>]*\\bname="${item.expectedTestMethod}")(?=[^>]*\\bclassname="${item.expectedTestClass.replaceAll('.', '\\.')}")[^>]*(?:/>|>([\\s\\S]*?)</testcase>)`);
  const testcase = source.match(testcasePattern);
  const body = testcase?.[1] || '';
  const failureTag = body.match(/<failure\b[^>]*>/)?.[0] || '';
  const failureMessage = xmlAttribute(failureTag, 'message');
  const failureType = xmlAttribute(failureTag, 'type');
  return {
    valid: Boolean(suiteTag) && Number.isFinite(tests),
    tests,
    failures,
    errors,
    skipped,
    expectedTestFound: Boolean(testcase),
    expectedTestFailed: Boolean(failureTag),
    failureType,
    failureMessage
  };
}

function loadCases(file, selected) {
  const payload = JSON.parse(fs.readFileSync(file, 'utf8'));
  if (payload.version !== 1 || !Array.isArray(payload.cases) || payload.cases.length === 0) {
    throw new Error(`Invalid mutation catalog: ${file}`);
  }
  const cases = selected ? payload.cases.filter((item) => item.id === selected) : payload.cases;
  if (cases.length === 0) throw new Error(`Unknown mutation case: ${selected}`);
  return cases;
}

function verifyCase(item, root) {
  const target = path.join(root, item.file);
  if (!fs.existsSync(target)) throw new Error(`${item.id}: target not found: ${item.file}`);
  const original = fs.readFileSync(target, 'utf8');
  const sourceHash = hashFile(target);
  const testPath = path.join(root, item.testFile);
  if (!fs.existsSync(testPath)) throw new Error(`${item.id}: test file not found: ${item.testFile}`);
  const testHash = hashFile(testPath);
  const reportPath = path.join(root, item.surefireReport);
  const occurrences = original.split(item.find).length - 1;
  if (occurrences !== 1) {
    throw new Error(`${item.id}: expected exactly one mutation target, found ${occurrences}`);
  }
  const cwd = path.join(root, item.cwd || '.');
  const baseline = run(item.command, cwd);
  const baselineReport = parseSurefireReport(reportPath, item);
  if (baseline.status !== 0 || !baselineReport.valid || baselineReport.tests < 1
      || baselineReport.failures !== 0 || baselineReport.errors !== 0 || !baselineReport.expectedTestFound) {
    return {
      id: item.id,
      status: 'FAIL',
      reason: 'baseline implementation did not pass before mutation',
      baselineExit: baseline.status,
      baselineReport,
      baselineTail: tail(baseline.output)
    };
  }
  fs.writeFileSync(target, original.replace(item.find, item.replace));
  const mutated = run(item.command, cwd);
  const mutatedReport = parseSurefireReport(reportPath, item);
  const pattern = new RegExp(item.expectedFailurePattern, 'm');
  const expectedFailure = mutatedReport.valid
    && mutatedReport.tests > 0
    && mutatedReport.failures === 1
    && mutatedReport.errors === 0
    && mutatedReport.expectedTestFound
    && mutatedReport.expectedTestFailed
    && mutatedReport.failureType === item.expectedFailureType
    && pattern.test(mutatedReport.failureMessage);
  const killed = mutated.status !== 0 && !mutated.timedOut && !mutated.signal && expectedFailure;
  return {
    id: item.id,
    status: killed ? 'PASS' : 'FAIL',
    baselineExit: baseline.status,
    baselineReport,
    mutatedExit: mutated.status,
    mutatedTimedOut: mutated.timedOut,
    mutatedSignal: mutated.signal,
    mutatedReport,
    sourceHash,
    testHash,
    command: item.command,
    expectedFailurePattern: item.expectedFailurePattern,
    expectedFailureObserved: expectedFailure,
    curatedSeedKilled: killed,
    mutatedTail: tail(mutated.output)
  };
}

function createDetachedWorktree(head) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-mutation-'));
  fs.rmSync(root, { recursive: true, force: true });
  const result = run(['git', 'worktree', 'add', '--detach', root, head], repoRoot, 120000);
  if (result.status !== 0) throw new Error(`Cannot create mutation worktree:\n${result.output}`);
  return root;
}

function removeWorktree(root) {
  const result = run(['git', 'worktree', 'remove', '--force', root], repoRoot, 120000);
  if (result.status !== 0 && fs.existsSync(root)) {
    throw new Error(`Cannot remove mutation worktree ${root}:\n${result.output}`);
  }
}

try {
  const args = parseArgs(process.argv.slice(2));
  const cases = loadCases(args.catalog, args.caseId);
  const results = [];
  for (const item of cases) {
    const worktree = createDetachedWorktree(args.head);
    try {
      results.push(verifyCase(item, worktree));
    } finally {
      removeWorktree(worktree);
    }
  }
  const resolvedCommit = run(['git', 'rev-parse', args.head], repoRoot).output.trim();
  const workingTreeStatus = run(['git', 'status', '--porcelain'], repoRoot).output.trim();
  const report = {
    status: results.every((item) => item.status === 'PASS') ? 'PASS' : 'FAIL',
    head: args.head,
    resolvedCommit,
    workingTreeIncluded: false,
    workingTreeWasDirty: Boolean(workingTreeStatus),
    catalogHash: hashFile(args.catalog),
    generatedAt: new Date().toISOString(),
    isolation: 'detached temporary git worktree',
    caseCount: results.length,
    curatedSeedCount: results.length,
    curatedSeedsKilled: results.filter((item) => item.curatedSeedKilled).length,
    curatedSeedKillRate: results.length === 0 ? 0 : results.filter((item) => item.curatedSeedKilled).length / results.length,
    results
  };
  fs.mkdirSync(path.dirname(args.report), { recursive: true });
  fs.writeFileSync(args.report, `${JSON.stringify(report, null, 2)}\n`);
  if (workingTreeStatus) console.warn('Working tree changes are not included; detached verification uses the selected committed head only.');
  console.log(`Targeted mutation verification: ${report.status}; curated seeds killed=${report.curatedSeedsKilled}/${report.curatedSeedCount}; report=${path.relative(repoRoot, args.report)}`);
  for (const item of results) console.log(`- ${item.id}: ${item.status}`);
  if (report.status !== 'PASS') process.exit(1);
} catch (error) {
  console.error(`Targeted mutation verification failed: ${error.message}`);
  process.exit(2);
}
