#!/usr/bin/env node
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { loadFixtureCases, materializeFixture } from './lib/quality-fixtures.mjs';

const toolDir = path.dirname(fileURLToPath(import.meta.url));
const fixtureFile = path.join(toolDir, '../fixtures/executable-quality/cases.json');
const gateFile = path.join(toolDir, 'quality-gate.mjs');
const repoRoot = path.resolve(toolDir, '../..');

function parseArgs(argv) {
  const args = { reportJson: '.runtime/pmo/executable-quality-eval.json', reportMd: '.runtime/pmo/executable-quality-eval.md' };
  const allowed = new Set(['report-json', 'report-md']);
  for (let index = 0; index < argv.length; index += 1) {
    const option = argv[index];
    if (!option.startsWith('--') || !allowed.has(option.slice(2))) throw new Error(`Unknown option: ${option}`);
    const value = argv[index + 1];
    if (value === undefined || value.startsWith('--')) throw new Error(`Missing value: ${option}`);
    if (option === '--report-json') args.reportJson = value;
    else args.reportMd = value;
    index += 1;
  }
  return args;
}

function isolatedEnvironment(root) {
  const home = path.join(root, 'home');
  const codexHome = path.join(root, 'codex-home');
  fs.mkdirSync(home, { recursive: true });
  fs.mkdirSync(codexHome, { recursive: true });
  return {
    PATH: process.env.PATH || '/usr/bin:/bin',
    HOME: home,
    CODEX_HOME: codexHome,
    LANG: 'C.UTF-8',
    LC_ALL: 'C.UTF-8',
    TZ: 'UTC',
    NO_PROXY: '*',
    no_proxy: '*',
    HTTP_PROXY: '',
    HTTPS_PROXY: '',
    ALL_PROXY: ''
  };
}

function runEngine(root, item, engine, contractPath) {
  const args = [gateFile, '--root', root, '--files', item.files.map((file) => file.path).join(','), '--engine', engine, '--json'];
  if (contractPath) args.push('--contract', path.relative(root, contractPath));
  const result = spawnSync(process.execPath, args, {
    cwd: root,
    env: isolatedEnvironment(root),
    encoding: 'utf8',
    stdio: 'pipe',
    timeout: 15000
  });
  let payload = null;
  try { payload = JSON.parse(result.stdout.trim()); } catch { /* recorded below */ }
  if (![0, 1].includes(result.status) || !payload) {
    return { outcome: 'ERROR', status: result.status, stderr: result.stderr.trim(), stdout: result.stdout.trim(), rules: [] };
  }
  return { outcome: result.status === 0 ? 'PASS' : 'BLOCK', status: result.status, rules: payload.issues.map((issue) => issue.rule) };
}

function metrics(rows, engine) {
  const predictions = rows.flatMap((row) => row.runs.map((run) => run[engine].outcome));
  const expected = rows.flatMap((row) => row.runs.map(() => row.expected));
  const correct = predictions.filter((prediction, index) => prediction === expected[index]).length;
  const criticalRows = rows.filter((row) => row.critical && row.expected === 'BLOCK');
  const criticalRuns = criticalRows.flatMap((row) => row.runs.map((run) => run[engine].outcome));
  const positiveRows = rows.filter((row) => row.expected === 'PASS');
  const positiveRuns = positiveRows.flatMap((row) => row.runs.map((run) => run[engine].outcome));
  const consistentRows = rows.filter((row) => new Set(row.runs.map((run) => run[engine].outcome)).size === 1).length;
  return {
    runs: predictions.length,
    correct,
    accuracy: predictions.length === 0 ? 0 : correct / predictions.length,
    criticalRuns: criticalRuns.length,
    criticalDetected: criticalRuns.filter((outcome) => outcome === 'BLOCK').length,
    criticalDetection: criticalRuns.length === 0 ? 1 : criticalRuns.filter((outcome) => outcome === 'BLOCK').length / criticalRuns.length,
    positiveRuns: positiveRuns.length,
    falsePositives: positiveRuns.filter((outcome) => outcome !== 'PASS').length,
    consistency: rows.length === 0 ? 0 : consistentRows / rows.length
  };
}

function percent(value) {
  return `${(value * 100).toFixed(2)}%`;
}

function markdown(report) {
  const lines = [
    '# PMO 可执行质量空白上下文 A/B 实验报告',
    '',
    `- 时间：${report.generatedAt}`,
    `- 场景数：${report.caseCount}`,
    `- 隔离运行数：${report.candidate.runs}（普通场景 3 次，关键场景 5 次）`,
    '- 隔离条件：每次使用独立临时目录、空 HOME、空 CODEX_HOME、固定 UTC、代理禁用、无会话历史。',
    '',
    '## 结果',
    '',
    '| 指标 | Current executable checks | Candidate quality gate | 阈值 |',
    '|---|---:|---:|---:|',
    `| 总体正确率 | ${percent(report.current.accuracy)} | ${percent(report.candidate.accuracy)} | ≥ 95% |`,
    `| 关键红线检出率 | ${percent(report.current.criticalDetection)} | ${percent(report.candidate.criticalDetection)} | 100% |`,
    `| 合法正例误报 | ${report.current.falsePositives} | ${report.candidate.falsePositives} | 0 |`,
    `| 重复结论一致率 | ${percent(report.current.consistency)} | ${percent(report.candidate.consistency)} | ≥ 95% |`,
    `| 相对提升 | - | ${(report.improvement * 100).toFixed(2)} 个百分点 | ≥ 30 个百分点 |`,
    '',
    `结论：**${report.status}**`,
    '',
    '## 场景明细',
    '',
    '| 场景 | 期望 | 关键 | Current | Candidate | Candidate 规则 |',
    '|---|---|---:|---|---|---|'
  ];
  for (const row of report.rows) {
    const current = [...new Set(row.runs.map((run) => run.current.outcome))].join('/');
    const candidate = [...new Set(row.runs.map((run) => run.candidate.outcome))].join('/');
    const rules = [...new Set(row.runs.flatMap((run) => run.candidate.rules))].join(', ');
    lines.push(`| ${row.id} | ${row.expected} | ${row.critical ? '是' : '否'} | ${current} | ${candidate} | ${rules || '-'} |`);
  }
  lines.push('');
  return `${lines.join('\n')}\n`;
}

try {
  const args = parseArgs(process.argv.slice(2));
  const cases = loadFixtureCases(fixtureFile);
  const rows = [];
  for (const item of cases) {
    const runs = [];
    const repeat = item.critical ? 5 : 3;
    for (let index = 0; index < repeat; index += 1) {
      const root = fs.mkdtempSync(path.join(os.tmpdir(), `mango-quality-${item.id}-`));
      try {
        const contractPath = materializeFixture(root, item);
        runs.push({
          current: runEngine(root, item, 'legacy', contractPath),
          candidate: runEngine(root, item, 'candidate', contractPath)
        });
      } finally {
        fs.rmSync(root, { recursive: true, force: true });
      }
    }
    rows.push({ id: item.id, expected: item.expected, critical: item.critical, expectedRule: item.rule || null, runs });
  }
  const current = metrics(rows, 'current');
  const candidate = metrics(rows, 'candidate');
  const improvement = candidate.accuracy - current.accuracy;
  const thresholds = {
    minimumCases: cases.length >= 30,
    accuracy: candidate.accuracy >= 0.95,
    criticalDetection: candidate.criticalDetection === 1,
    falsePositives: candidate.falsePositives === 0,
    consistency: candidate.consistency >= 0.95,
    improvement: improvement >= 0.30,
    expectedRules: rows.every((row) => !row.expectedRule || row.runs.every((run) => run.candidate.rules.includes(row.expectedRule)))
  };
  const report = {
    status: Object.values(thresholds).every(Boolean) ? 'PASS' : 'FAIL',
    generatedAt: new Date().toISOString(),
    fixtureVersion: 1,
    caseCount: cases.length,
    isolation: { emptyHome: true, emptyCodexHome: true, timezone: 'UTC', networkProxyDisabled: true, freshDirectoryPerRun: true },
    current,
    candidate,
    improvement,
    thresholds,
    rows
  };
  const jsonPath = path.resolve(repoRoot, args.reportJson);
  const mdPath = path.resolve(repoRoot, args.reportMd);
  fs.mkdirSync(path.dirname(jsonPath), { recursive: true });
  fs.mkdirSync(path.dirname(mdPath), { recursive: true });
  fs.writeFileSync(jsonPath, `${JSON.stringify(report, null, 2)}\n`);
  fs.writeFileSync(mdPath, markdown(report));
  console.log(`Executable quality A/B evaluation: ${report.status}`);
  console.log(`Cases=${cases.length}; current=${percent(current.accuracy)}; candidate=${percent(candidate.accuracy)}; improvement=${(improvement * 100).toFixed(2)}pp`);
  console.log(`Critical=${percent(candidate.criticalDetection)}; falsePositives=${candidate.falsePositives}; consistency=${percent(candidate.consistency)}`);
  console.log(`Reports: ${path.relative(repoRoot, jsonPath)}, ${path.relative(repoRoot, mdPath)}`);
  if (report.status !== 'PASS') process.exit(1);
} catch (error) {
  console.error(`Executable quality evaluation failed: ${error.message}`);
  process.exit(2);
}
