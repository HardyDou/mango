#!/usr/bin/env node
import { existsSync, mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { spawnSync } from 'node:child_process';

export const trustedContractPaths = [
  'mango-pmo/tools/check-capability-docs.mjs',
  'mango-pmo/tools/risk-verification.mjs',
  'mango-pmo/contracts/delivery-assurance.json',
];

export function runTrustedPrContractCheck({ repoRoot, baseRef, headRef, prBodyPath }) {
  const resolvedBody = resolve(repoRoot, prBodyPath);
  if (!existsSync(resolvedBody)) {
    throw new Error(`trusted PR contract check requires a PR body: ${resolvedBody}`);
  }

  const baseCommit = gitValue(repoRoot, ['rev-parse', `${baseRef}^{commit}`]);
  const headCommit = gitValue(repoRoot, ['rev-parse', `${headRef}^{commit}`]);
  const worktree = mkdtempSync(join(tmpdir(), 'mango-pr-contract-'));
  let registered = false;

  try {
    run('git', ['worktree', 'add', '--detach', '--no-checkout', worktree, baseCommit], repoRoot);
    registered = true;
    run('git', ['sparse-checkout', 'set', '--no-cone', ...trustedContractPaths], worktree);
    run('git', ['checkout', '--detach', baseCommit], worktree);

    const env = {
      GITHUB_ACTIONS: 'true',
      PR_BODY_FILE: resolvedBody,
    };
    run(process.execPath, ['mango-pmo/tools/risk-verification.mjs'], worktree, { env });
    run(
      process.execPath,
      ['mango-pmo/tools/check-capability-docs.mjs', '--base', baseCommit, '--head', headCommit],
      worktree,
      { env },
    );
  } finally {
    if (registered) {
      const removal = spawnSync('git', ['worktree', 'remove', '--force', worktree], {
        cwd: repoRoot,
        encoding: 'utf8',
      });
      if (removal.status !== 0) {
        spawnSync('git', ['worktree', 'prune'], { cwd: repoRoot, encoding: 'utf8' });
        rmSync(worktree, { recursive: true, force: true });
      }
    } else {
      rmSync(worktree, { recursive: true, force: true });
    }
  }
}

function run(executable, args, cwd, options = {}) {
  const result = spawnSync(executable, args, {
    cwd,
    encoding: 'utf8',
    stdio: 'inherit',
    env: { ...process.env, ...(options.env || {}) },
  });
  if (result.status !== 0) {
    throw new Error(`${executable} ${args.join(' ')} failed with exit code ${result.status ?? 1}`);
  }
}

function gitValue(repoRoot, args) {
  const result = spawnSync('git', args, { cwd: repoRoot, encoding: 'utf8' });
  if (result.status !== 0) {
    throw new Error(`git ${args.join(' ')} failed: ${(result.stderr || result.stdout).trim()}`);
  }
  return result.stdout.trim();
}

function valueArg(args, name) {
  const inline = args.find((value) => value.startsWith(`${name}=`));
  if (inline) return inline.slice(name.length + 1);
  const index = args.indexOf(name);
  return index >= 0 ? args[index + 1] || '' : '';
}

function main() {
  const repoRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');
  const args = process.argv.slice(2);
  const baseRef = valueArg(args, '--base') || 'origin/main';
  const headRef = valueArg(args, '--head') || 'HEAD';
  const prBodyPath = valueArg(args, '--pr-body');
  if (!prBodyPath) throw new Error('trusted PR contract check requires --pr-body <path>');
  runTrustedPrContractCheck({ repoRoot, baseRef, headRef, prBodyPath });
  console.log(`Trusted base PR contract checks PASS for ${headRef}`);
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  main();
}
