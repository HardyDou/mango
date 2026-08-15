import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { spawnSync } from 'node:child_process';
import test from 'node:test';
import { runTrustedPrContractCheck, trustedContractPaths } from './run-trusted-pr-contract-check.mjs';

test('runs the trusted base contract check instead of the candidate checker', () => {
  const repoRoot = mkdtempSync(join(tmpdir(), 'mango-pr-contract-fixture-'));
  try {
    git(repoRoot, ['init']);
    git(repoRoot, ['config', 'user.email', 'test@example.test']);
    git(repoRoot, ['config', 'user.name', 'Mango Test']);
    mkdirSync(join(repoRoot, 'mango-pmo/tools'), { recursive: true });
    mkdirSync(join(repoRoot, 'mango-pmo/contracts'), { recursive: true });
    writeFileSync(
      join(repoRoot, 'mango-pmo/tools/risk-verification.mjs'),
      "import { existsSync } from 'node:fs';\nif (process.env.GITHUB_ACTIONS !== 'true' || !existsSync(process.env.PR_BODY_FILE)) process.exit(2);\n",
    );
    writeFileSync(
      join(repoRoot, 'mango-pmo/tools/check-capability-docs.mjs'),
      "if (process.env.GITHUB_ACTIONS !== 'true' || !process.argv.includes('--base') || !process.argv.includes('--head')) process.exit(3);\n",
    );
    writeFileSync(join(repoRoot, 'mango-pmo/contracts/delivery-assurance.json'), '{}\n');
    writeFileSync(join(repoRoot, 'pr-body.md'), '## PMO / Scope\n');
    git(repoRoot, ['add', ...trustedContractPaths, 'pr-body.md']);
    git(repoRoot, ['commit', '-m', 'base checker']);
    const baseCommit = git(repoRoot, ['rev-parse', 'HEAD']).stdout.trim();

    writeFileSync(join(repoRoot, 'mango-pmo/tools/check-capability-docs.mjs'), 'process.exit(9);\n');
    git(repoRoot, ['add', 'mango-pmo/tools/check-capability-docs.mjs']);
    git(repoRoot, ['commit', '-m', 'candidate checker']);
    const headCommit = git(repoRoot, ['rev-parse', 'HEAD']).stdout.trim();

    assert.doesNotThrow(() =>
      runTrustedPrContractCheck({
        repoRoot,
        baseRef: baseCommit,
        headRef: headCommit,
        prBodyPath: 'pr-body.md',
      }),
    );
  } finally {
    rmSync(repoRoot, { recursive: true, force: true });
  }
});

function git(cwd, args) {
  const result = spawnSync('git', args, { cwd, encoding: 'utf8' });
  assert.equal(result.status, 0, result.stderr || result.stdout);
  return result;
}
