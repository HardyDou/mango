#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { analyzeArtifacts, analyzeLegacyArtifacts, loadArtifacts } from './lib/quality-analyzer.mjs';
import { fixtureArtifacts, fixtureContract, loadFixtureCases } from './lib/quality-fixtures.mjs';

const toolDir = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(toolDir, '../..');
const defaultFixtureFile = path.join(toolDir, '../fixtures/executable-quality/cases.json');

function parseArgs(argv) {
  const args = { root: repoRoot, base: 'origin/main', head: 'HEAD', files: '', contract: '', report: '', engine: 'candidate', json: false, reportOnly: false, selfTest: false };
  const flags = new Map([['--json', 'json'], ['--report-only', 'reportOnly'], ['--self-test', 'selfTest']]);
  const values = new Set(['root', 'base', 'head', 'files', 'contract', 'report', 'engine']);
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (flags.has(arg)) { args[flags.get(arg)] = true; continue; }
    if (!arg.startsWith('--') || !values.has(arg.slice(2))) throw new Error(`Unknown option: ${arg}`);
    const value = argv[index + 1];
    if (value === undefined || value.startsWith('--')) throw new Error(`Missing value: ${arg}`);
    args[arg.slice(2)] = value;
    index += 1;
  }
  if (!['candidate', 'legacy'].includes(args.engine)) throw new Error('--engine must be candidate or legacy');
  args.root = path.resolve(args.root);
  return args;
}

function git(root, ...args) {
  return execFileSync('git', args, { cwd: root, encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] }).trim();
}

function resolveFiles(args) {
  if (args.files) return [...new Set(args.files.split(',').map((item) => item.trim()).filter(Boolean))];
  let base = args.base;
  try { base = git(args.root, 'merge-base', args.base, args.head); } catch { base = `${args.head}^`; }
  const outputs = [];
  try { outputs.push(git(args.root, 'diff', '--name-only', `${base}...${args.head}`, '--')); } catch { outputs.push(git(args.root, 'diff', '--name-only', args.head, '--')); }
  if (args.head === 'HEAD') {
    outputs.push(git(args.root, 'diff', '--name-only', 'HEAD', '--'));
    outputs.push(git(args.root, 'ls-files', '--others', '--exclude-standard'));
  }
  return [...new Set(outputs.join('\n').split('\n').map((item) => item.trim()).filter(Boolean))];
}

function runSelfTest() {
  const cases = loadFixtureCases(defaultFixtureFile);
  const failures = [];
  for (const item of cases) {
    const artifacts = fixtureArtifacts(item);
    const issues = analyzeArtifacts({ artifacts, contract: fixtureContract(item), now: new Date('2026-07-10T00:00:00Z') });
    const actual = issues.length > 0 ? 'BLOCK' : 'PASS';
    if (actual !== item.expected) failures.push(`${item.id}: expected ${item.expected}, got ${actual} (${issues.map((issue) => issue.rule).join(', ')})`);
    if (item.rule && !issues.some((issue) => issue.rule === item.rule)) failures.push(`${item.id}: expected rule ${item.rule}, got ${issues.map((issue) => issue.rule).join(', ') || '<none>'}`);
  }
  if (failures.length > 0) throw new Error(`self-test failed:\n${failures.map((failure) => `- ${failure}`).join('\n')}`);
  console.log(`Executable quality gate self-test passed: ${cases.length} cases`);
}

function printText(result) {
  console.log(`Executable quality gate: ${result.status}; engine=${result.engine}; files=${result.files.length}; issues=${result.issues.length}`);
  for (const issue of result.issues) {
    console.log(`- [${issue.rule}] ${issue.file}:${issue.line} ${issue.message}；修复：${issue.fix}`);
  }
}

try {
  const args = parseArgs(process.argv.slice(2));
  if (args.selfTest) {
    runSelfTest();
    process.exit(0);
  }
  const files = resolveFiles(args).filter((file) => fs.existsSync(path.join(args.root, file)) && fs.statSync(path.join(args.root, file)).isFile());
  const contract = args.contract ? JSON.parse(fs.readFileSync(path.resolve(args.root, args.contract), 'utf8')) : null;
  const contractFiles = contract?.change?.files || [];
  const scanFiles = [...new Set([...files, ...contractFiles])].filter((file) => fs.existsSync(path.join(args.root, file)));
  const artifacts = loadArtifacts(args.root, scanFiles);
  const issues = args.engine === 'legacy' ? analyzeLegacyArtifacts({ artifacts }) : analyzeArtifacts({ artifacts, contract });
  const result = {
    status: issues.length > 0 ? 'BLOCK' : 'PASS',
    engine: args.engine,
    root: args.root,
    files: scanFiles,
    issueCount: issues.length,
    issues
  };
  if (args.report) {
    const output = path.resolve(args.root, args.report);
    fs.mkdirSync(path.dirname(output), { recursive: true });
    fs.writeFileSync(output, `${JSON.stringify(result, null, 2)}\n`);
  }
  if (args.json) console.log(JSON.stringify(result));
  else printText(result);
  if (issues.length > 0 && !args.reportOnly) process.exit(1);
} catch (error) {
  console.error(`Executable quality gate failed: ${error.message}`);
  process.exit(2);
}
