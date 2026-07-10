#!/usr/bin/env node
import crypto from 'node:crypto';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawn, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const toolDir = path.dirname(fileURLToPath(import.meta.url));
const baselineRoot = path.resolve(toolDir, '..');

function findRepositoryRoot(start) {
  let current = start;
  while (current !== path.dirname(current)) {
    if (fs.existsSync(path.join(current, '.git'))) return current;
    current = path.dirname(current);
  }
  throw new Error(`Cannot find Git repository root from ${start}`);
}

const repoRoot = findRepositoryRoot(baselineRoot);
const baselinePrefix = path.relative(repoRoot, baselineRoot).split(path.sep).join('/');
const catalogFile = path.join(baselineRoot, 'fixtures/agent-classification/cases.json');
const policyFiles = [
  { label: 'AGENTS.md', local: path.join(repoRoot, 'AGENTS.md'), git: 'AGENTS.md' },
  ...[
    'rules/00-dev-flow.md',
    'rules/03-ai-coding-redlines.md',
    'rules/04-test-assets.md',
    'rules/05-ai-delivery-quality.md',
    'rules/backend/08-test.md',
    'rules/frontend/04-test.md',
    'rules/10-executable-quality-contract.md'
  ].map((file) => ({ label: file, local: path.join(baselineRoot, file), git: `${baselinePrefix}/${file}` }))
];

function parseArgs(argv) {
  const args = {
    base: 'main',
    repeats: 3,
    concurrency: 4,
    model: 'gpt-5.6-sol',
    report: '.runtime/pmo/isolated-agent-classification.json',
    markdown: '.runtime/pmo/isolated-agent-classification.md',
    caseId: '',
    validateOnly: false
  };
  const allowed = new Set(['base', 'repeats', 'concurrency', 'model', 'report', 'markdown', 'case']);
  for (let index = 0; index < argv.length; index += 1) {
    const option = argv[index];
    if (option === '--validate-only') {
      args.validateOnly = true;
      continue;
    }
    if (!option.startsWith('--') || !allowed.has(option.slice(2))) throw new Error(`Unknown option: ${option}`);
    const value = argv[index + 1];
    if (value === undefined || value.startsWith('--')) throw new Error(`Missing value: ${option}`);
    const key = option.slice(2);
    if (key === 'repeats' || key === 'concurrency') args[key] = Number(value);
    else if (key === 'case') args.caseId = value;
    else args[key] = value;
    index += 1;
  }
  if (!Number.isInteger(args.repeats) || args.repeats < 1 || args.repeats > 5) throw new Error('--repeats must be an integer from 1 to 5');
  if (!Number.isInteger(args.concurrency) || args.concurrency < 1 || args.concurrency > 8) throw new Error('--concurrency must be an integer from 1 to 8');
  args.report = path.resolve(repoRoot, args.report);
  args.markdown = path.resolve(repoRoot, args.markdown);
  return args;
}

function command(commandArgs, options = {}) {
  return spawnSync(commandArgs[0], commandArgs.slice(1), {
    cwd: options.cwd || repoRoot,
    env: options.env || process.env,
    encoding: 'utf8',
    stdio: 'pipe',
    timeout: options.timeout || 30000
  });
}

function commandAsync(commandArgs, options = {}) {
  return new Promise((resolve) => {
    const child = spawn(commandArgs[0], commandArgs.slice(1), {
      cwd: options.cwd || repoRoot,
      env: options.env || process.env,
      stdio: ['ignore', 'pipe', 'pipe']
    });
    let stdout = '';
    let stderr = '';
    let timedOut = false;
    child.stdout.on('data', (chunk) => { stdout += chunk.toString(); });
    child.stderr.on('data', (chunk) => { stderr += chunk.toString(); });
    const timer = setTimeout(() => {
      timedOut = true;
      child.kill('SIGKILL');
    }, options.timeout || 180000);
    child.on('error', (error) => {
      clearTimeout(timer);
      resolve({ status: null, stdout, stderr: `${stderr}\n${error.message}`, timedOut });
    });
    child.on('close', (status) => {
      clearTimeout(timer);
      resolve({ status, stdout, stderr, timedOut });
    });
  });
}

function gitShow(ref, file) {
  const result = command(['git', 'show', `${ref}:${file}`]);
  return result.status === 0 ? result.stdout : '';
}

function policySnapshot(group, base) {
  const sections = [];
  for (const file of policyFiles) {
    const content = group === 'current' ? gitShow(base, file.git) : (fs.existsSync(file.local) ? fs.readFileSync(file.local, 'utf8') : '');
    if (content) sections.push(`\n===== ${file.label} =====\n${content.trim()}\n`);
  }
  return sections.join('');
}

function hash(value) {
  return crypto.createHash('sha256').update(value).digest('hex');
}

function outputSchema() {
  return {
    type: 'object',
    additionalProperties: false,
    required: ['risk', 'staticReviewRequired', 'requiredTests', 'decision', 'reasonCode'],
    properties: {
      risk: { enum: ['R0', 'R1', 'R2', 'R3'] },
      staticReviewRequired: { type: 'boolean' },
      requiredTests: { type: 'array', items: { enum: ['UNIT', 'API', 'UI'] } },
      decision: { enum: ['PASS', 'BLOCK'] },
      reasonCode: { enum: ['MECHANICAL_STATIC', 'USELESS_TEST', 'KEY_LOGIC_UNIT', 'COMPLEX_FLOW_API', 'USER_FLOW_UI', 'PROTECTED_PATH_MOCK', 'API_ENTRY_BYPASS', 'INTERNAL_API_MOCK', 'EXTERNAL_BOUNDARY_MOCK', 'UI_ASSERTION_MISSING'] }
    }
  };
}

function evaluationPrompt() {
  return [
    '你是受控 PMO 分类执行器。',
    '只读取当前目录的 POLICY.md 和 TASK.md；不得读取其他目录，不得联网检索。',
    '把 TASK.md 描述的方案当作待验收提交，根据 POLICY.md 给出最小充分验证义务和是否放行。',
    'requiredTests 只能按 UNIT、API、UI 顺序；UI 与 E2E 同义；STATIC_REVIEW 不是测试。',
    '输出必须严格符合 schema，不要添加解释文本。'
  ].join('\n');
}

function sameClassification(actual, expected) {
  if (!actual || typeof actual !== 'object') return false;
  return actual.risk === expected.risk
    && actual.staticReviewRequired === expected.staticReviewRequired
    && JSON.stringify(actual.requiredTests) === JSON.stringify(expected.requiredTests)
    && actual.decision === expected.decision
    && actual.reasonCode === expected.reasonCode;
}

function copyAuth(targetCodexHome) {
  const sourceHome = process.env.CODEX_HOME || path.join(os.homedir(), '.codex');
  const source = path.join(sourceHome, 'auth.json');
  if (!fs.existsSync(source) || !fs.statSync(source).isFile()) throw new Error(`Codex auth file not found: ${source}`);
  fs.mkdirSync(targetCodexHome, { recursive: true });
  fs.copyFileSync(source, path.join(targetCodexHome, 'auth.json'));
}

async function runCase({ group, policy, item, repeat, model }) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), `mango-agent-eval-${item.id}-`));
  const home = path.join(root, 'home');
  const codexHome = path.join(root, 'codex-home');
  const output = path.join(root, 'output.json');
  try {
    fs.mkdirSync(home, { recursive: true });
    copyAuth(codexHome);
    fs.writeFileSync(path.join(root, 'POLICY.md'), policy);
    fs.writeFileSync(path.join(root, 'TASK.md'), `${item.task}\n`);
    fs.writeFileSync(path.join(root, 'schema.json'), `${JSON.stringify(outputSchema(), null, 2)}\n`);
    const prompt = evaluationPrompt();
    const env = {
      PATH: process.env.PATH || '/usr/bin:/bin',
      HOME: home,
      CODEX_HOME: codexHome,
      LANG: 'C.UTF-8',
      LC_ALL: 'C.UTF-8',
      TZ: 'UTC'
    };
    for (const name of ['HTTPS_PROXY', 'HTTP_PROXY', 'ALL_PROXY', 'NO_PROXY', 'SSL_CERT_FILE', 'SSL_CERT_DIR']) {
      if (process.env[name]) env[name] = process.env[name];
    }
    const result = await commandAsync([
      'codex', 'exec', '--ephemeral', '--ignore-user-config', '--ignore-rules',
      '--sandbox', 'read-only', '--skip-git-repo-check', '--color', 'never',
      '--model', model, '--cd', root, '--output-schema', path.join(root, 'schema.json'),
      '--output-last-message', output, prompt
    ], { cwd: root, env, timeout: 180000 });
    let actual = null;
    try { actual = JSON.parse(fs.readFileSync(output, 'utf8')); } catch { /* reported below */ }
    const exactMatch = sameClassification(actual, item.expected);
    const sessionId = (result.stderr.match(/session id:\s*([0-9a-f-]+)/i) || [])[1] || '';
    return {
      group,
      caseId: item.id,
      critical: item.critical,
      repeat,
      processExit: result.status,
      timedOut: result.timedOut,
      sessionId,
      exactMatch,
      expected: item.expected,
      actual,
      stderrTail: result.status === 0 ? '' : (result.stderr || '').split('\n').slice(-20).join('\n')
    };
  } finally {
    fs.rmSync(root, { recursive: true, force: true });
  }
}

async function runWithConcurrency(jobs, limit) {
  const results = new Array(jobs.length);
  let next = 0;
  async function worker() {
    while (true) {
      const index = next;
      next += 1;
      if (index >= jobs.length) return;
      const job = jobs[index];
      console.log(`Agent classification ${job.group} ${job.item.id} repeat ${job.repeat}`);
      results[index] = await runCase(job);
    }
  }
  await Promise.all(Array.from({ length: Math.min(limit, jobs.length) }, () => worker()));
  return results;
}

function metrics(results) {
  const completed = results.filter((item) => item.processExit === 0 && item.actual !== null);
  const exact = results.filter((item) => item.exactMatch);
  const critical = results.filter((item) => item.critical);
  const criticalExact = critical.filter((item) => item.exactMatch);
  return {
    agentClassificationCount: results.length,
    completedCount: completed.length,
    agentClassificationExactMatches: exact.length,
    agentClassificationExactMatchRate: results.length === 0 ? 0 : exact.length / results.length,
    criticalClassificationCount: critical.length,
    criticalClassificationExactMatches: criticalExact.length,
    criticalClassificationExactMatchRate: critical.length === 0 ? 1 : criticalExact.length / critical.length
  };
}

function validateCases(cases) {
  const schema = outputSchema().properties;
  const ids = new Set();
  for (const item of cases) {
    if (!item.id || ids.has(item.id)) throw new Error(`Invalid or duplicate case id: ${item.id}`);
    ids.add(item.id);
    if (!item.task || !item.expected) throw new Error(`${item.id}: task and expected are required`);
    const expected = item.expected;
    const valid = schema.risk.enum.includes(expected.risk)
      && typeof expected.staticReviewRequired === 'boolean'
      && Array.isArray(expected.requiredTests)
      && expected.requiredTests.every((value) => schema.requiredTests.items.enum.includes(value))
      && schema.decision.enum.includes(expected.decision)
      && schema.reasonCode.enum.includes(expected.reasonCode);
    if (!valid) throw new Error(`${item.id}: invalid expected classification`);
  }
}

function percent(value) {
  return `${(value * 100).toFixed(2)}%`;
}

function markdown(report) {
  const lines = [
    '# PMO 受控隔离新会话分类实验', '',
    `- 结论：**${report.status}**`,
    `- 时间：${report.generatedAt}`,
    `- Codex：${report.codexVersion}`,
    `- 模型：${report.model}`,
    `- 案例：${report.caseCount}，每组每案例重复 ${report.repeats} 次`,
    '- 隔离：每案例独立进程、临时目录、临时 HOME、仅含认证的临时 CODEX_HOME、ephemeral、忽略用户配置和项目 rules。',
    '- 口径：只测 Agent 对 PMO 测试义务的结构化分类，不证明代码实现、测试运行或业务正确性。', '',
    '| 指标 | Current PMO | Candidate PMO |', '|---|---:|---:|',
    `| Agent 分类精确匹配率 | ${percent(report.current.agentClassificationExactMatchRate)} | ${percent(report.candidate.agentClassificationExactMatchRate)} |`,
    `| 关键案例精确匹配率 | ${percent(report.current.criticalClassificationExactMatchRate)} | ${percent(report.candidate.criticalClassificationExactMatchRate)} |`,
    `| 完成运行 | ${report.current.completedCount}/${report.current.agentClassificationCount} | ${report.candidate.completedCount}/${report.candidate.agentClassificationCount} |`, '',
    '## 未精确匹配', ''
  ];
  const failures = report.results.filter((item) => !item.exactMatch);
  if (failures.length === 0) lines.push('无。');
  else for (const item of failures) lines.push(`- ${item.group}/${item.caseId}/#${item.repeat}: expected=${JSON.stringify(item.expected)}, actual=${JSON.stringify(item.actual)}`);
  return `${lines.join('\n')}\n`;
}

try {
  const args = parseArgs(process.argv.slice(2));
  const catalog = JSON.parse(fs.readFileSync(catalogFile, 'utf8'));
  let cases = catalog.cases;
  if (args.caseId) cases = cases.filter((item) => item.id === args.caseId);
  if (cases.length === 0) throw new Error(`No agent classification cases selected: ${args.caseId}`);
  validateCases(cases);
  const policies = {
    current: policySnapshot('current', args.base),
    candidate: policySnapshot('candidate', args.base)
  };
  if (!policies.current || !policies.candidate) throw new Error('Current and candidate policy snapshots must both be non-empty');
  if (args.validateOnly) {
    console.log(`Agent classification fixtures valid: cases=${cases.length}; currentPolicy=${hash(policies.current)}; candidatePolicy=${hash(policies.candidate)}`);
    process.exit(0);
  }
  const versionResult = command(['codex', '--version']);
  if (versionResult.status !== 0) throw new Error(`Cannot execute codex: ${versionResult.stderr}`);
  const jobs = [];
  for (const item of cases) {
    for (let repeat = 1; repeat <= args.repeats; repeat += 1) {
      const groups = repeat % 2 === 0 ? ['candidate', 'current'] : ['current', 'candidate'];
      for (const group of groups) {
        jobs.push({ group, policy: policies[group], item, repeat, model: args.model });
      }
    }
  }
  const results = await runWithConcurrency(jobs, args.concurrency);
  const current = metrics(results.filter((item) => item.group === 'current'));
  const candidate = metrics(results.filter((item) => item.group === 'candidate'));
  const thresholds = {
    allRunsCompleted: candidate.completedCount === candidate.agentClassificationCount,
    candidateExactMatchRate: candidate.agentClassificationExactMatchRate >= 0.95,
    candidateCriticalExactMatchRate: candidate.criticalClassificationExactMatchRate === 1
  };
  const report = {
    reportKind: 'isolated_codex_classification',
    status: Object.values(thresholds).every(Boolean) ? 'PASS' : 'FAIL',
    generatedAt: new Date().toISOString(),
    codexVersion: versionResult.stdout.trim(),
    model: args.model,
    baseRef: args.base,
    baseCommit: command(['git', 'rev-parse', args.base]).stdout.trim(),
    candidateCommit: command(['git', 'rev-parse', 'HEAD']).stdout.trim(),
    policyHashes: { current: hash(policies.current), candidate: hash(policies.candidate) },
    promptHash: hash(evaluationPrompt()),
    isolation: { processPerCase: true, ephemeral: true, ignoreUserConfig: true, ignoreProjectRules: true, temporaryHome: true, authOnlyCodexHome: true, sandbox: 'read-only' },
    caseCount: cases.length,
    repeats: args.repeats,
    concurrency: args.concurrency,
    current,
    candidate,
    thresholds,
    results
  };
  fs.mkdirSync(path.dirname(args.report), { recursive: true });
  fs.mkdirSync(path.dirname(args.markdown), { recursive: true });
  fs.writeFileSync(args.report, `${JSON.stringify(report, null, 2)}\n`);
  fs.writeFileSync(args.markdown, markdown(report));
  console.log(`Isolated Codex classification: ${report.status}; current=${percent(current.agentClassificationExactMatchRate)}; candidate=${percent(candidate.agentClassificationExactMatchRate)}`);
  console.log(`Reports: ${path.relative(repoRoot, args.report)}, ${path.relative(repoRoot, args.markdown)}`);
  if (report.status !== 'PASS') process.exit(1);
} catch (error) {
  console.error(`Isolated Codex classification failed: ${error.message}`);
  process.exit(2);
}
