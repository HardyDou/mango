#!/usr/bin/env node
// AC-1002-06 blank-context semantic evaluation harness.
//
// Each case runs in a fresh `pi --no-session` process so the answering Agent
// cannot read the implementation session, then a deterministic grader checks the
// answer against the case requiredAssertions and forbid list. The grader is
// pattern-based: it detects the rule-id citations, the required disposition, and
// the forbidden behaviors. It does not judge general answer quality, so a PASS
// still needs the human review recorded in the packet.
import { execFileSync } from 'node:child_process';
import { cpSync, mkdirSync, readFileSync, readdirSync, rmSync, statSync, writeFileSync } from 'node:fs';
import os from 'node:os';
import path from 'node:path';

const root = process.cwd();
const evalPath = path.join(root, 'mango-pmo/tests/skills/evals.json');
const outDir = path.join(root, 'mango-docs/evidence/issue-1002/eval-packet');
const model = process.env.PI_MODEL;
if (!model) {
  console.error('[FAIL] PI_MODEL is not set; refusing to guess the evaluated model');
  process.exit(1);
}

const cases = JSON.parse(readFileSync(evalPath, 'utf8')).cases.filter(item => item.id.startsWith('issue-1002'));
if (cases.length === 0) {
  console.error('[FAIL] no issue-1002 cases found in eval set');
  process.exit(1);
}

// --regrade re-scores the captured answers without new model calls. It exists so a
// grader-pattern fix does not require paying for another identical model run.
const regradeOnly = process.argv.includes('--regrade');

// Patterns that decide one assertion. Each entry is [case-scoped label, regex].
const ASSERTION_PATTERNS = {
  'issue-1002-design-reuse-first': [
    ['cites REDLINE-REUSE-010', /REDLINE-REUSE-010/u],
    ['rejects the consumerless addition', /REDLINE-MINIMAL-020|当前消费者|无消费者|未来扩展|future/iu],
    ['rejects future-only extension', /未来|future/iu],
  ],
  'issue-1002-engineering-no-fixed-questionnaire': [
    ['cites REDLINE-QUEST-050', /REDLINE-QUEST-050/u],
    ['cites REDLINE-MINIMAL-020', /REDLINE-MINIMAL-020/u],
    ['keeps the mode unchanged for a small change', /SIMPLE|不升级|无需升级|保持.{0,6}模式/iu],
  ],
  'issue-1002-engineering-ambiguous-term-ask': [
    ['asks instead of renaming', /ASK|询问|先确认|需要确认|无法确定/iu],
    ['protects the canonical term', /主术语|canonical|已有术语|RESOURCE|targetId/iu],
  ],
  'issue-1002-defect-root-cause-minimal-fix': [
    ['cites REDLINE-REUSE-010', /REDLINE-REUSE-010/u],
    ['fixes the shared root cause', /根因|共享能力|root cause/iu],
    ['refuses scope expansion', /不扩大|不重构|无关重构|范围|scope/iu],
  ],
  'issue-1002-pr-review-backstop': [
    ['cites REDLINE-OVERDESIGN-060', /REDLINE-OVERDESIGN-060/u],
    ['requires file and line evidence', /文件|行号|file|line/iu],
    ['does not modify code', /不修改|不直接改|只报告|review only/iu],
  ],
  'issue-1002-plan-no-silent-design': [
    ['keeps the approved design', /批准|approved|保持/iu],
    ['refuses to add the unapproved design', /不能|不得|拒绝|不按|退回|夹带|多余|凭空/iu],
    ['cites REDLINE-MINIMAL-020', /REDLINE-(?:MINIMAL-020|OVERDESIGN-060|TERMS-030)/u],
  ],
};

const FORBID_PATTERNS = {
  'future-only abstraction': /保留.{0,10}(预留|未来).{0,10}抽象|可以新增该抽象/u,
  'duplicate capability': /重复实现.{0,6}可以|允许重复/u,
  'unnecessary dependency': /可以新增.{0,4}依赖|允许新增依赖/u,
  'fixed questionnaire': /填写问卷|问卷清单|逐项问卷/u,
  'new dependency': /新增依赖.{0,6}(可以|允许)/u,
  'test without observation target': /补一个测试即可|无观察目标.{0,6}可以/u,
  'invented synonym': /我选择.{0,6}新名称|自行命名/u,
  'blind rename': /直接重命名.{0,6}(可以|执行)/u,
  'surface-only workaround': /只改调用方|局部 workaround.{0,6}可以/u,
  'broad refactor': /顺手重构.{0,6}(可以|执行)/u,
  'scope expansion': /扩大范围.{0,6}(可以|执行)/u,
  'code modification': /我来修改代码|现在就去改|已经修改了代码|直接改代码/u,
  'line-count-only deletion': /减少行数.{0,6}(即可|足够)/u,
  NEXT: /^NEXT\b/mu,
};

mkdirSync(outDir, { recursive: true });

function grade(item, answer) {
  const assertionPatterns = ASSERTION_PATTERNS[item.id] ?? [];
  const assertionResults = assertionPatterns.map(([label, pattern]) => ({
    assertion: label,
    passed: pattern.test(answer),
  }));
  const forbiddenHits = (item.expect.forbid ?? [])
    .map(entry => [entry, FORBID_PATTERNS[entry]])
    .filter(([, pattern]) => pattern)
    .filter(([, pattern]) => pattern.test(answer))
    .map(([label]) => label);
  const actionMatched = item.expect.action
    ? new RegExp(item.expect.action.replace(/[.*+?^${}()|[\]\\]/gu, '\\$&'), 'u').test(answer)
      || new RegExp(item.expect.action.toLowerCase().replace(/[.*+?^${}()|[\]\\]/gu, '\\$&'), 'u').test(answer)
    : true;
  const failed = assertionResults.filter(result => !result.passed).map(result => result.assertion);
  return {
    assertionResults,
    forbiddenHits,
    actionMatched,
    verdict: failed.length === 0 && forbiddenHits.length === 0 ? 'PASS' : 'FAIL',
  };
}

function regradeCase(item) {
  const answer = readFileSync(path.join(outDir, `${item.id}.answer.md`), 'utf8');
  const graded = grade(item, answer);
  return {
    id: item.id,
    skill: item.expect.skill,
    expectedAction: item.expect.action ?? null,
    actionMatched: graded.actionMatched,
    assertionResults: graded.assertionResults,
    forbiddenHits: graded.forbiddenHits,
    verdict: graded.verdict,
    durationMs: 0,
    attempts: 0,
    sandboxModified: false,
    error: null,
    answer,
  };
}

function runCase(item) {
  const prompt = `${item.prompt}\n\n上下文事实（评测器注入）：${JSON.stringify(item.context)}`;
  const started = Date.now();
  let answer = '';
  let error = '';
  let attempts = 0;
  // Each case runs inside a throwaway copy of the repository. The evaluator must
  // not be able to read the implementation session, and it must also not be able
  // to mutate the task worktree; a modified copy is reported and then discarded.
  const sandbox = path.join(os.tmpdir(), `mango-1002-eval-${item.id}-${process.pid}`);
  rmSync(sandbox, { recursive: true, force: true });
  cpSync(root, sandbox, {
    recursive: true,
    filter: source => {
      const rel = path.relative(root, source);
      if (!rel) return true;
      const top = rel.split(path.sep)[0];
      return top !== 'node_modules' && top !== '.git' && top !== '.runtime' && top !== '.mango';
    },
  });
  const before = sandboxFingerprint(sandbox);
  let sandboxDirty = false;
  try {
    while (attempts < 3) {
      attempts += 1;
      try {
        answer = execFileSync('pi', [
          '-p', '--no-session',
          '--no-builtin-tools', '-t', 'read,bash',
          '--model', model,
          '--append-system-prompt', 'This is a read-only evaluation. Do not edit, create or delete any file; answer with your decision and reasoning only.',
          prompt,
        ], { cwd: sandbox, encoding: 'utf8', maxBuffer: 32 * 1024 * 1024, stdio: ['ignore', 'pipe', 'pipe'] });
        error = '';
        break;
      } catch (caught) {
        const stdout = caught.stdout?.toString() || '';
        error = stdout || caught.message || 'unknown execution error';
        answer = stdout;
        const transient = /invalid `?signature`? in `?thinking`?/iu.test(error)
          || /invalid_request_error/iu.test(error)
          || /overloaded|rate limit|529|503/iu.test(error);
        if (!transient) break;
      }
    }
    sandboxDirty = sandboxFingerprint(sandbox) !== before;
  } finally {
    rmSync(sandbox, { recursive: true, force: true });
  }
  const durationMs = Date.now() - started;
  const graded = grade(item, answer);
  const { assertionResults, forbiddenHits, actionMatched, verdict } = graded;
  return {
    id: item.id,
    skill: item.expect.skill,
    expectedAction: item.expect.action ?? null,
    actionMatched,
    assertionResults,
    forbiddenHits,
    verdict,
    durationMs,
    attempts,
    sandboxModified: sandboxDirty,
    error: error || null,
    answer,
  };
}

function sandboxFingerprint(sandbox) {
  const entries = [];
  const walk = directory => {
    for (const entry of readdirSync(directory, { withFileTypes: true }).sort(byName)) {
      const full = path.join(directory, entry.name);
      if (entry.isDirectory()) {
        walk(full);
        continue;
      }
      if (!entry.isFile()) continue;
      const stat = statSync(full);
      entries.push(`${path.relative(sandbox, full)}\u0000${stat.size}\u0000${stat.mtimeMs}`);
    }
  };
  walk(sandbox);
  return entries.join('\n');
}

function byName(left, right) {
  return left.name < right.name ? -1 : left.name > right.name ? 1 : 0;
}

const results = [];
for (const item of cases) {
  if (regradeOnly) {
    process.stderr.write(`Regrading ${item.id}...\n`);
    results.push(regradeCase(item));
    continue;
  }
  process.stderr.write(`Running ${item.id}...\n`);
  results.push(runCase(item));
}

const passed = results.filter(result => result.verdict === 'PASS').length;
const report = {
  schemaVersion: 1,
  suite: 'issue-1002-ac-1002-06-blank-context',
  evaluatedAt: new Date().toISOString(),
  evaluatedModel: model,
  harness: 'pi --no-session (fresh context per case)',
  grader: 'deterministic pattern grader over requiredAssertions and forbid',
  graderLimitations: 'Pattern grading confirms rule citations, disposition and forbidden behaviors. It does not judge general answer quality; human review remains required before replacing the baseline.',
  caseCount: results.length,
  passed,
  failed: results.length - passed,
  result: passed === results.length ? 'PASS' : 'FAIL',
  cases: results.map(({ answer, ...rest }) => rest),
};

writeFileSync(path.join(outDir, 'ac-1002-06-semantic-report.json'), `${JSON.stringify(report, null, 2)}\n`);
for (const result of results) {
  writeFileSync(path.join(outDir, `${result.id}.answer.md`), result.answer);
}

process.stdout.write(`${report.result}: ${passed}/${results.length} cases passed (model ${model})\n`);
for (const result of results) {
  const failedAssertions = result.assertionResults.filter(item => !item.passed).map(item => item.assertion);
  process.stdout.write(`- ${result.verdict} ${result.id}${failedAssertions.length ? ` failed: ${failedAssertions.join(', ')}` : ''}${result.forbiddenHits.length ? ` forbidden: ${result.forbiddenHits.join(', ')}` : ''}${result.sandboxModified ? ' sandbox-modified: evaluator tried to write' : ''}\n`);
}
const anyWriteAttempt = results.some(result => result.sandboxModified);
if (anyWriteAttempt) {
  process.stdout.write('NOTE: at least one evaluator session attempted a repository write; the write was confined to a discarded copy.\n');
}
process.exit(report.result === 'PASS' && !anyWriteAttempt ? 0 : 1);
