#!/usr/bin/env node
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { analyzeArtifacts } from './lib/quality-analyzer.mjs';

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
let activeRepoRoot = repoRoot;
let baselineRoot = path.join(activeRepoRoot, 'mango-docs/evidence/test-baseline');

function parseArgs(argv) {
  const command = argv[0] || '';
  if (!['check', 'promote', 'self-test'].includes(command)) throw new Error('usage: quality-baseline.mjs check|promote|self-test');
  const args = { command };
  const allowed = new Set(['root', 'capability', 'type', 'source', 'owner', 'approver', 'reason']);
  for (let index = 1; index < argv.length; index += 1) {
    const option = argv[index];
    if (!option.startsWith('--') || !allowed.has(option.slice(2))) throw new Error(`Unknown option: ${option}`);
    const value = argv[index + 1];
    if (value === undefined || value.startsWith('--')) throw new Error(`Missing value: ${option}`);
    args[option.slice(2)] = value;
    index += 1;
  }
  return args;
}

function walk(dir, files = []) {
  if (!fs.existsSync(dir)) return files;
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const target = path.join(dir, entry.name);
    if (entry.isDirectory()) walk(target, files);
    else files.push(target);
  }
  return files;
}

function check() {
  const files = walk(baselineRoot).map((file) => path.relative(activeRepoRoot, file).replaceAll('\\', '/'));
  const artifacts = files.map((file) => ({ path: file, content: fs.readFileSync(path.join(activeRepoRoot, file), 'utf8') }));
  const issues = analyzeArtifacts({ artifacts }).filter((issue) => issue.rule.startsWith('PQT-BASELINE'));
  const latestRoots = new Set(files.map((file) => file.match(/^(mango-docs\/evidence\/test-baseline\/[^/]+\/(?:unit|api|ui)\/latest)\//)?.[1]).filter(Boolean));
  for (const latest of latestRoots) {
    if (!files.includes(`${latest}/report.json`) || !files.includes(`${latest}/README.md`)) {
      issues.push({ rule: 'PQT-BASELINE-002', file: latest, line: 1, message: 'latest 缺少 report.json 或 README.md', fix: '补齐机器报告和复现摘要' });
    }
    if (/\/ui\/latest$/.test(latest) && !files.some((file) => file.startsWith(`${latest}/`) && /\.(?:png|webp)$/.test(file))) {
      issues.push({ rule: 'PQT-BASELINE-003', file: latest, line: 1, message: 'UI latest 缺少关键截图', fix: '保存固定视口和主题的截图' });
    }
  }
  if (issues.length > 0) throw new Error(issues.map((issue) => `[${issue.rule}] ${issue.file}: ${issue.message}`).join('\n'));
  console.log(`Quality baseline check passed: ${latestRoots.size} latest baseline(s)`);
}

function promote(args) {
  for (const field of ['capability', 'type', 'source', 'owner', 'approver', 'reason']) {
    if (!String(args[field] || '').trim()) throw new Error(`promote requires --${field}`);
  }
  if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(args.capability)) throw new Error('invalid --capability');
  if (!['unit', 'api', 'ui'].includes(args.type)) throw new Error('--type must be unit, api or ui');
  if (args.reason.trim().length < 8) throw new Error('--reason must explain the baseline change');
  const source = path.resolve(activeRepoRoot, args.source);
  const reportPath = path.join(source, 'report.json');
  if (!fs.existsSync(reportPath) || !fs.existsSync(path.join(source, 'README.md'))) throw new Error('source requires report.json and README.md');
  const report = JSON.parse(fs.readFileSync(reportPath, 'utf8'));
  if (report.status !== 'PASS' || report.businessAssertionsPassed !== true) throw new Error('source report must be PASS with businessAssertionsPassed=true');
  if (args.type === 'ui' && !walk(source).some((file) => /\.(?:png|webp)$/.test(file))) throw new Error('UI source requires a screenshot');
  const parent = path.join(baselineRoot, args.capability, args.type);
  const latest = path.join(parent, 'latest');
  const incoming = path.join(parent, `.incoming-${process.pid}`);
  const backup = path.join(parent, `.backup-${process.pid}`);
  fs.mkdirSync(parent, { recursive: true });
  fs.cpSync(source, incoming, { recursive: true });
  fs.writeFileSync(path.join(incoming, 'promotion.json'), `${JSON.stringify({ owner: args.owner, approver: args.approver, reason: args.reason, promotedAt: new Date().toISOString() }, null, 2)}\n`);
  if (fs.existsSync(latest)) fs.renameSync(latest, backup);
  try {
    fs.renameSync(incoming, latest);
    if (fs.existsSync(backup)) fs.rmSync(backup, { recursive: true, force: true });
  } catch (error) {
    if (fs.existsSync(latest)) fs.rmSync(latest, { recursive: true, force: true });
    if (fs.existsSync(backup)) fs.renameSync(backup, latest);
    throw error;
  }
  check();
  console.log(`Quality baseline promoted: ${path.relative(activeRepoRoot, latest)}`);
}

function selfTest() {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-quality-baseline-'));
  activeRepoRoot = root;
  baselineRoot = path.join(root, 'mango-docs/evidence/test-baseline');
  try {
    const source = path.join(root, 'candidate');
    fs.mkdirSync(source, { recursive: true });
    fs.writeFileSync(path.join(source, 'README.md'), '# Candidate\n');
    fs.writeFileSync(path.join(source, 'report.json'), '{"status":"FAIL","businessAssertionsPassed":false}\n');
    assertFails(() => promote({ capability: 'demo', type: 'unit', source: 'candidate', owner: 'dev', approver: 'lead', reason: '验证失败报告不能提升' }), 'FAIL report');
    fs.writeFileSync(path.join(source, 'report.json'), '{"status":"PASS","businessAssertionsPassed":true}\n');
    assertFails(() => promote({ capability: 'demo', type: 'unit', source: 'candidate', owner: 'dev', reason: '验证缺少批准人不能提升' }), 'missing approver');
    promote({ capability: 'demo', type: 'unit', source: 'candidate', owner: 'dev', approver: 'lead', reason: '验证受控基线原子提升流程' });
    const latest = path.join(baselineRoot, 'demo/unit/latest');
    if (!fs.existsSync(path.join(latest, 'promotion.json'))) throw new Error('self-test: promotion metadata missing');
    const before = fs.readFileSync(path.join(latest, 'report.json'), 'utf8');
    check();
    const after = fs.readFileSync(path.join(latest, 'report.json'), 'utf8');
    if (before !== after) throw new Error('self-test: baseline check modified latest');
    const stale = path.join(baselineRoot, 'demo/unit/2026-01-01');
    fs.mkdirSync(stale, { recursive: true });
    fs.writeFileSync(path.join(stale, 'report.json'), '{}\n');
    assertFails(() => check(), 'multiple baseline versions');
    console.log('Quality baseline self-test passed: read-only check, approval, PASS report, atomic latest and stale-version rejection');
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
}

function assertFails(action, label) {
  try {
    action();
  } catch {
    return;
  }
  throw new Error(`self-test expected failure: ${label}`);
}

try {
  const args = parseArgs(process.argv.slice(2));
  if (args.root) {
    activeRepoRoot = path.resolve(args.root);
    baselineRoot = path.join(activeRepoRoot, 'mango-docs/evidence/test-baseline');
  }
  if (args.command === 'check') check();
  else if (args.command === 'promote') promote(args);
  else selfTest();
} catch (error) {
  console.error(`Quality baseline failed: ${error.message}`);
  process.exit(1);
}
