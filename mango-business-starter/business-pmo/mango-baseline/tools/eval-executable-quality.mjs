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
  const exactRows = rows.filter((row) => row.runs.every((run) => run[engine].outcome === row.expected)).length;
  return {
    gateCaseCount: rows.length,
    gateCaseExactMatches: exactRows,
    gateCaseExactMatchRate: rows.length === 0 ? 0 : exactRows / rows.length,
    gateDecisionCount: predictions.length,
    gateDecisionExactMatches: correct,
    gateDecisionExactMatchRate: predictions.length === 0 ? 0 : correct / predictions.length,
    criticalDecisionCount: criticalRuns.length,
    criticalBlocks: criticalRuns.filter((outcome) => outcome === 'BLOCK').length,
    criticalBlockRecall: criticalRuns.length === 0 ? 1 : criticalRuns.filter((outcome) => outcome === 'BLOCK').length / criticalRuns.length,
    legalDecisionCount: positiveRuns.length,
    legalFalseBlocks: positiveRuns.filter((outcome) => outcome !== 'PASS').length,
    repeatConsistencyRate: rows.length === 0 ? 0 : consistentRows / rows.length
  };
}

function percent(value) {
  return `${(value * 100).toFixed(2)}%`;
}

function markdown(report) {
  const lines = [
    '# PMO 可执行质量隔离门禁判定 A/B 实验报告',
    '',
    `- 时间：${report.generatedAt}`,
    '- 受测对象：旧版与候选版可执行门禁，不是 AI 生成代码。',
    `- 场景数：${report.caseCount}`,
    `- 隔离运行数：${report.candidate.gateDecisionCount}（普通场景 3 次，关键场景 5 次）`,
    '- 隔离条件：每次使用独立临时目录、空 HOME、空 CODEX_HOME、固定 UTC、代理禁用。',
    '- 口径限制：这里的“门禁判定精确匹配率”只表示冻结样本的 PASS/BLOCK 是否符合标签，不能解释为项目质量或 AI 编码正确率。',
    '',
    '## 结果',
    '',
    '| 指标 | Current executable checks | Candidate quality gate | 阈值 |',
    '|---|---:|---:|---:|',
    `| 场景级精确匹配率 | ${percent(report.current.gateCaseExactMatchRate)} | ${percent(report.candidate.gateCaseExactMatchRate)} | 参考 |`,
    `| 重复运行加权精确匹配率 | ${percent(report.current.gateDecisionExactMatchRate)} | ${percent(report.candidate.gateDecisionExactMatchRate)} | ≥ 95% |`,
    `| 关键红线阻断召回率 | ${percent(report.current.criticalBlockRecall)} | ${percent(report.candidate.criticalBlockRecall)} | 100% |`,
    `| 合法正例错误阻断数 | ${report.current.legalFalseBlocks} | ${report.candidate.legalFalseBlocks} | 0 |`,
    `| 重复结论一致率 | ${percent(report.current.repeatConsistencyRate)} | ${percent(report.candidate.repeatConsistencyRate)} | ≥ 95% |`,
    `| 门禁精确匹配率提升 | - | ${report.gateDecisionImprovementPoints.toFixed(2)} 个百分点 | ≥ 30 个百分点 |`,
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
  return `${lines.join('\n').trimEnd()}\n`;
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
  const gateDecisionImprovementPoints = (candidate.gateDecisionExactMatchRate - current.gateDecisionExactMatchRate) * 100;
  const thresholds = {
    minimumCases: cases.length >= 30,
    gateDecisionExactMatchRate: candidate.gateDecisionExactMatchRate >= 0.95,
    criticalBlockRecall: candidate.criticalBlockRecall === 1,
    legalFalseBlocks: candidate.legalFalseBlocks === 0,
    repeatConsistencyRate: candidate.repeatConsistencyRate >= 0.95,
    gateDecisionImprovementPoints: gateDecisionImprovementPoints >= 30,
    expectedRules: rows.every((row) => !row.expectedRule || row.runs.every((run) => run.candidate.rules.includes(row.expectedRule)))
  };
  const report = {
    reportKind: 'deterministic_gate_ab',
    status: Object.values(thresholds).every(Boolean) ? 'PASS' : 'FAIL',
    generatedAt: new Date().toISOString(),
    evaluationSubject: 'deterministic executable gate classification; not AI artifact generation',
    fixtureVersion: 1,
    caseCount: cases.length,
    isolation: { emptyHome: true, emptyCodexHome: true, timezone: 'UTC', networkProxyDisabled: true, freshDirectoryPerRun: true },
    current,
    candidate,
    gateDecisionImprovementPoints,
    thresholds,
    rows
  };
  const jsonPath = path.resolve(repoRoot, args.reportJson);
  const mdPath = path.resolve(repoRoot, args.reportMd);
  fs.mkdirSync(path.dirname(jsonPath), { recursive: true });
  fs.mkdirSync(path.dirname(mdPath), { recursive: true });
  fs.writeFileSync(jsonPath, `${JSON.stringify(report, null, 2)}\n`);
  fs.writeFileSync(mdPath, markdown(report));
  console.log(`Executable quality isolated gate A/B evaluation: ${report.status}`);
  console.log(`Cases=${cases.length}; current gate exact-match=${percent(current.gateDecisionExactMatchRate)}; candidate=${percent(candidate.gateDecisionExactMatchRate)}; improvement=${gateDecisionImprovementPoints.toFixed(2)}pp`);
  console.log(`Critical block recall=${percent(candidate.criticalBlockRecall)}; legal false blocks=${candidate.legalFalseBlocks}; consistency=${percent(candidate.repeatConsistencyRate)}`);
  console.log(`Reports: ${path.relative(repoRoot, jsonPath)}, ${path.relative(repoRoot, mdPath)}`);
  if (report.status !== 'PASS') process.exit(1);
} catch (error) {
  console.error(`Executable quality evaluation failed: ${error.message}`);
  process.exit(2);
}
