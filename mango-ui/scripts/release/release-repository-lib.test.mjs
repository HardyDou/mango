import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';
import test from 'node:test';
import {
  gitChangedFiles,
  resolveGitSource,
  resolveRepositoryInputPath,
  verifyReleasePlanSource,
} from './release-repository-lib.mjs';

test('resolves command input paths from the repository root', () => {
  const repoRoot = resolve('/tmp/mango-release-repository');

  assert.equal(
    resolveRepositoryInputPath(repoRoot, '.runtime/pr-body.md', '.runtime/mango-release/pr-body.md'),
    resolve(repoRoot, '.runtime/pr-body.md'),
  );
  assert.equal(
    resolveRepositoryInputPath(repoRoot, '', '.runtime/mango-release/pr-body.md'),
    resolve(repoRoot, '.runtime/mango-release/pr-body.md'),
  );
});

test('preserves absolute command input paths', () => {
  const repoRoot = resolve('/tmp/mango-release-repository');
  const absolute = resolve('/tmp/mango-release-pr-body.md');

  assert.equal(resolveRepositoryInputPath(repoRoot, absolute, '.runtime/mango-release/pr-body.md'), absolute);
});

test('binds source files to a committed source snapshot while returning the final projection', (t) => {
  const repoRoot = createRepository(t);
  write(repoRoot, 'baseline.txt', 'baseline\n');
  commit(repoRoot, 'baseline');
  const baselineCommit = git(repoRoot, ['rev-parse', 'HEAD']);
  write(repoRoot, 'mango-ui/packages/base/src/index.ts', 'export const value = 1;\n');
  commit(repoRoot, 'source');
  const source = resolveGitSource(repoRoot);
  const sourceFiles = gitChangedFiles(repoRoot, baselineCommit, source.commit);
  write(repoRoot, 'mango-ui/.changeset/release-plan.json', '{}\n');
  write(repoRoot, 'mango-ui/packages/base/package.json', '{"name":"@mango/base","version":"1.0.1"}\n');
  commit(repoRoot, 'release projection');

  const verified = verifyReleasePlanSource({ repoRoot, baselineCommit, source, sourceFiles });
  assert.deepEqual(verified.sourceFiles, ['mango-ui/packages/base/src/index.ts']);
  assert.deepEqual(verified.projectionFiles, [
    'mango-ui/.changeset/release-plan.json',
    'mango-ui/packages/base/package.json',
  ]);
  assert.throws(
    () => verifyReleasePlanSource({ repoRoot, baselineCommit, source, sourceFiles: [] }),
    /sourceFiles does not match/u,
  );
  assert.throws(
    () =>
      verifyReleasePlanSource({ repoRoot, baselineCommit, source: { ...source, tree: 'f'.repeat(40) }, sourceFiles }),
    /commit\/tree does not match/u,
  );
});

test('rejects a source snapshot that is not an ancestor of final HEAD', (t) => {
  const repoRoot = createRepository(t);
  write(repoRoot, 'baseline.txt', 'baseline\n');
  commit(repoRoot, 'baseline');
  const baselineCommit = git(repoRoot, ['rev-parse', 'HEAD']);
  git(repoRoot, ['switch', '-c', 'source']);
  write(repoRoot, 'source.txt', 'source\n');
  commit(repoRoot, 'source');
  const source = resolveGitSource(repoRoot);
  const sourceFiles = gitChangedFiles(repoRoot, baselineCommit, source.commit);
  git(repoRoot, ['switch', 'main']);
  write(repoRoot, 'final.txt', 'final\n');
  commit(repoRoot, 'final');

  assert.throws(
    () => verifyReleasePlanSource({ repoRoot, baselineCommit, source, sourceFiles }),
    /source is not an ancestor/u,
  );
});

function createRepository(t) {
  const repoRoot = mkdtempSync(join(tmpdir(), 'mango-release-repository-'));
  t.after(() => rmSync(repoRoot, { recursive: true, force: true }));
  git(repoRoot, ['init', '-b', 'main']);
  git(repoRoot, ['config', 'user.email', 'release-test@example.com']);
  git(repoRoot, ['config', 'user.name', 'Release Test']);
  return repoRoot;
}

function write(repoRoot, path, content) {
  mkdirSync(resolve(repoRoot, path, '..'), { recursive: true });
  writeFileSync(join(repoRoot, path), content);
}

function commit(repoRoot, message) {
  git(repoRoot, ['add', '.']);
  git(repoRoot, ['commit', '-m', message]);
}

function git(repoRoot, args) {
  const result = spawnSync('git', args, { cwd: repoRoot, encoding: 'utf8' });
  if (result.status !== 0) throw new Error(result.stderr || result.stdout);
  return result.stdout.trim();
}
