#!/usr/bin/env node
// 影子评测 runner：在任务基线提交上注入新治理规则，用空白上下文 Agent 重做方案。
// 用法：node run-shadow-eval.mjs [--only <id>]
import { execFileSync } from 'node:child_process';
import { cpSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const outDir = path.join(root, 'mango-docs/evidence/issue-1002/shadow-eval');
const worktreeBase = '/tmp/mango-shadow';
const model = process.env.PI_MODEL || 'claude-fable-5-1';
const only = process.argv.includes('--only') ? process.argv[process.argv.indexOf('--only') + 1] : null;

const tasks = JSON.parse(readFileSync(path.join(outDir, 'shadow-tasks.json'), 'utf8'));

const prompt = requirement => `你是 Mango 仓库的开发 Agent，正在处理一个新任务。请先阅读 mango-pmo/rules/03-ai-coding-redlines.md 和 mango-pmo/agents/03-dev-agent.md，然后只输出实现方案（不要修改任何文件、不要写完整代码、不要执行任何写入命令）。

需求：${requirement}

请按以下五项逐条回答（不适用项明确写“不适用”，不要跳过）：
1. 复用判断（REDLINE-REUSE-010）：按 Mango 能力→当前模块实现→标准库→已装依赖→新增结构的顺序，说明应复用哪些现有能力，是否真的需要新增任何结构。
2. 术语（REDLINE-TERMS-030）：是否引入新的公共类型、API、配置、权限码、事件或概念名？命名是否与既有术语一致、是否产生同义词？不确定时是否应 ASK？
3. 最小实现（REDLINE-MINIMAL-020）：改动边界、当前消费者、验证入口分别是什么？
4. 简单任务问卷（REDLINE-QUEST-050）：本任务是否需要生成固定问卷或术语表？
5. 过度设计（REDLINE-OVERDESIGN-060）：有无无消费者抽象、单一实现接口、仅内部使用的公共类型、无必要新依赖？`;

function runOnce(cwd, requirement) {
  return execFileSync('pi', [
    '-p', '--no-session', '--no-builtin-tools', '-t', 'read,bash',
    '--model', model,
    '--append-system-prompt', 'This is a read-only planning evaluation. Do not edit, create or delete any file; answer with your plan only.',
    prompt(requirement),
  ], { cwd, encoding: 'utf8', maxBuffer: 64 * 1024 * 1024, stdio: ['ignore', 'pipe', 'pipe'] });
}

function transient(text) {
  return /invalid `?signature`? in `?thinking`?/iu.test(text)
    || /invalid_request_error/iu.test(text)
    || /overloaded|rate limit|529|503/iu.test(text);
}

function runTask(task) {
  const dir = `${worktreeBase}-${task.id}`;
  rmSync(dir, { recursive: true, force: true });
  execFileSync('git', ['worktree', 'add', '--detach', dir, task.baseline], { cwd: root, stdio: 'ignore' });
  cpSync(path.join(root, 'mango-pmo/rules/03-ai-coding-redlines.md'), path.join(dir, 'mango-pmo/rules/03-ai-coding-redlines.md'));
  cpSync(path.join(root, 'mango-pmo/agents/03-dev-agent.md'), path.join(dir, 'mango-pmo/agents/03-dev-agent.md'));

  let answer = '';
  let error = '';
  let attempts = 0;
  while (attempts < 3) {
    attempts += 1;
    try {
      answer = runOnce(dir, task.requirement);
      error = '';
      break;
    } catch (caught) {
      const stdout = caught.stdout?.toString() || '';
      const stderr = caught.stderr?.toString() || '';
      error = `${stdout}${stderr}` || caught.message || 'unknown error';
      answer = stdout;
      if (!transient(error)) break;
    }
  }

  execFileSync('git', ['worktree', 'remove', '--force', dir], { cwd: root, stdio: 'ignore' });

  const result = {
    id: task.id,
    pr: task.pr,
    type: task.type,
    baseline: task.baseline,
    actualAdditions: task.actualAdditions,
    actualDeletions: task.actualDeletions,
    actualFiles: task.actualFiles,
    model,
    attempts,
    error: error || null,
    answer,
  };
  writeFileSync(path.join(outDir, `shadow-eval-${task.id}.answer.md`), answer);
  return result;
}

mkdirSync(outDir, { recursive: true });
const selected = only ? tasks.filter(t => t.id === only) : tasks;
const results = [];
for (const task of selected) {
  process.stderr.write(`[${task.id}] ${task.type} ... `);
  const result = runTask(task);
  results.push(result);
  process.stderr.write(`attempts=${result.attempts} error=${result.error ? 'YES' : 'no'} chars=${result.answer.length}\n`);
}
const summaryPath = path.join(outDir, 'shadow-run-summary.json');
const previous = readFileSync(summaryPath, 'utf8');
let accumulated = [];
try { accumulated = JSON.parse(previous); } catch { /* first run */ }
const merged = new Map(accumulated.map(item => [item.id, item]));
for (const result of results) {
  merged.set(result.id, {
    id: result.id, pr: result.pr, type: result.type, model: result.model,
    baseline: result.baseline, actualAdditions: result.actualAdditions,
    actualDeletions: result.actualDeletions, actualFiles: result.actualFiles,
    attempts: result.attempts, error: result.error, answerChars: result.answer.length,
  });
}
writeFileSync(summaryPath, JSON.stringify([...merged.values()], null, 2) + '\n');
process.stderr.write('done\n');
