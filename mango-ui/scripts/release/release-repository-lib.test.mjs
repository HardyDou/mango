import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';
import test from 'node:test';
import {
  gitChangedFiles,
  readGitFile,
  resolveReleaseBaselineAnchor,
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
  const baselineTree = git(repoRoot, ['rev-parse', 'HEAD^{tree}']);
  write(repoRoot, 'mango-ui/packages/base/src/index.ts', 'export const value = 1;\n');
  commit(repoRoot, 'source');
  const source = resolveGitSource(repoRoot);
  assert.equal(
    readGitFile(repoRoot, source.commit, 'mango-ui/packages/base/src/index.ts'),
    'export const value = 1;\n',
  );
  const sourceFiles = gitChangedFiles(repoRoot, baselineCommit, source.commit);
  write(repoRoot, 'mango-ui/.changeset/release-plan.json', '{}\n');
  write(repoRoot, 'mango-ui/packages/base/package.json', '{"name":"@mango/base","version":"1.0.1"}\n');
  commit(repoRoot, 'release projection');

  const verified = verifyReleasePlanSource({ repoRoot, baselineCommit, baselineTree, source, sourceFiles });
  assert.equal(verified.baselineAnchor, baselineCommit);
  assert.deepEqual(verified.sourceFiles, ['mango-ui/packages/base/src/index.ts']);
  assert.deepEqual(verified.projectionFiles, [
    'mango-ui/.changeset/release-plan.json',
    'mango-ui/packages/base/package.json',
  ]);
  assert.throws(
    () => verifyReleasePlanSource({ repoRoot, baselineCommit, baselineTree, source, sourceFiles: [] }),
    /sourceFiles does not match/u,
  );
  assert.throws(
    () =>
      verifyReleasePlanSource({
        repoRoot,
        baselineCommit,
        baselineTree,
        source: { ...source, tree: 'f'.repeat(40) },
        sourceFiles,
      }),
    /commit\/tree does not match/u,
  );
});

test('anchors a prepared baseline to its tree-equivalent squash commit on the first-parent history', (t) => {
  const repoRoot = createRepository(t);
  write(repoRoot, 'base.txt', 'base\n');
  commit(repoRoot, 'base');
  git(repoRoot, ['switch', '-c', 'prepared']);
  write(repoRoot, 'release.txt', 'released\n');
  commit(repoRoot, 'prepared release');
  const baselineCommit = git(repoRoot, ['rev-parse', 'HEAD']);
  const baselineTree = git(repoRoot, ['rev-parse', 'HEAD^{tree}']);

  git(repoRoot, ['switch', 'main']);
  write(repoRoot, 'release.txt', 'released\n');
  commit(repoRoot, 'squash release');
  const squashCommit = git(repoRoot, ['rev-parse', 'HEAD']);
  write(repoRoot, 'next.txt', 'next\n');
  commit(repoRoot, 'next release source');
  const source = resolveGitSource(repoRoot);
  const sourceFiles = ['next.txt'];

  assert.equal(
    resolveReleaseBaselineAnchor({ repoRoot, baselineCommit, baselineTree, sourceCommit: source.commit }),
    squashCommit,
  );
  const verified = verifyReleasePlanSource({ repoRoot, baselineCommit, baselineTree, source, sourceFiles });
  assert.equal(verified.baselineAnchor, squashCommit);
  assert.deepEqual(verified.sourceFiles, sourceFiles);
});

test('rejects a non-ancestor baseline when no first-parent commit has the successful tree', (t) => {
  const repoRoot = createRepository(t);
  write(repoRoot, 'base.txt', 'base\n');
  commit(repoRoot, 'base');
  git(repoRoot, ['switch', '-c', 'prepared']);
  write(repoRoot, 'release.txt', 'prepared only\n');
  commit(repoRoot, 'prepared release');
  const baselineCommit = git(repoRoot, ['rev-parse', 'HEAD']);
  const baselineTree = git(repoRoot, ['rev-parse', 'HEAD^{tree}']);

  git(repoRoot, ['switch', 'main']);
  write(repoRoot, 'source.txt', 'different source\n');
  commit(repoRoot, 'source');
  const source = resolveGitSource(repoRoot);

  assert.throws(
    () => resolveReleaseBaselineAnchor({ repoRoot, baselineCommit, baselineTree, sourceCommit: source.commit }),
    /no tree-equivalent first-parent ancestor/u,
  );
});

test('rejects a missing or mismatched recorded baseline tree', (t) => {
  const repoRoot = createRepository(t);
  write(repoRoot, 'baseline.txt', 'baseline\n');
  commit(repoRoot, 'baseline');
  const baselineCommit = git(repoRoot, ['rev-parse', 'HEAD']);

  assert.throws(
    () => resolveReleaseBaselineAnchor({ repoRoot, baselineCommit, sourceCommit: baselineCommit }),
    /commit\/tree does not match/u,
  );
  assert.throws(
    () =>
      resolveReleaseBaselineAnchor({
        repoRoot,
        baselineCommit,
        baselineTree: 'f'.repeat(40),
        sourceCommit: baselineCommit,
      }),
    /commit\/tree does not match/u,
  );
});

test('rejects a source snapshot that is not an ancestor of final HEAD', (t) => {
  const repoRoot = createRepository(t);
  write(repoRoot, 'baseline.txt', 'baseline\n');
  commit(repoRoot, 'baseline');
  const baselineCommit = git(repoRoot, ['rev-parse', 'HEAD']);
  const baselineTree = git(repoRoot, ['rev-parse', 'HEAD^{tree}']);
  git(repoRoot, ['switch', '-c', 'source']);
  write(repoRoot, 'source.txt', 'source\n');
  commit(repoRoot, 'source');
  const source = resolveGitSource(repoRoot);
  const sourceFiles = gitChangedFiles(repoRoot, baselineCommit, source.commit);
  git(repoRoot, ['switch', 'main']);
  write(repoRoot, 'final.txt', 'final\n');
  commit(repoRoot, 'final');

  assert.throws(
    () => verifyReleasePlanSource({ repoRoot, baselineCommit, baselineTree, source, sourceFiles }),
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
