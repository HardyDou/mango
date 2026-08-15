import { spawnSync } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';
import { join } from 'node:path';

export function runGit(repoRoot, args, { allowFailure = false } = {}) {
  const result = spawnSync('git', args, { cwd: repoRoot, encoding: 'utf8' });
  if (!allowFailure && result.status !== 0) {
    throw new Error(`git ${args.join(' ')} failed: ${(result.stderr || result.stdout).trim()}`);
  }
  return result;
}

export function gitValue(repoRoot, args) {
  return runGit(repoRoot, args).stdout.trim();
}

export function gitChangedFiles(repoRoot, baseRef, headRef = 'HEAD', includeWorkingTree = false) {
  const mergeBase = gitValue(repoRoot, ['merge-base', baseRef, headRef]);
  const files = new Set(
    runGit(repoRoot, ['diff', '--name-only', `${mergeBase}..${headRef}`]).stdout.split(/\r?\n/u).filter(Boolean),
  );
  if (includeWorkingTree) {
    for (const args of [
      ['diff', '--name-only'],
      ['diff', '--cached', '--name-only'],
      ['ls-files', '--others', '--exclude-standard'],
    ]) {
      for (const file of runGit(repoRoot, args).stdout.split(/\r?\n/u).filter(Boolean)) files.add(file);
    }
  }
  return [...files].sort();
}

export function resolveBaseline(repoRoot, workspaceRoot, legacy = null) {
  if (legacy) {
    const actualCommit = gitValue(repoRoot, ['rev-parse', `${legacy.from.tag}^{commit}`]);
    const actualTree = gitValue(repoRoot, ['rev-parse', `${legacy.from.tag}^{tree}`]);
    if (actualCommit !== legacy.from.commit || actualTree !== legacy.from.tree) {
      throw new Error(`legacy baseline ${legacy.from.tag} does not match its recorded commit/tree`);
    }
    return { kind: 'legacy-release', tag: legacy.from.tag, commit: actualCommit, tree: actualTree };
  }
  const baselinePath = join(workspaceRoot, '.changeset/release-baseline.json');
  if (!existsSync(baselinePath)) {
    throw new Error('release baseline is missing; only the approved one-time legacy reconciliation may bootstrap it');
  }
  const baseline = JSON.parse(readFileSync(baselinePath, 'utf8'));
  if (baseline.schemaVersion !== 1 || !baseline.commit || !baseline.tree) {
    throw new Error('release baseline has an unsupported structure');
  }
  const actualTree = gitValue(repoRoot, ['rev-parse', `${baseline.commit}^{tree}`]);
  if (actualTree !== baseline.tree) throw new Error('release baseline commit/tree mismatch');
  return { kind: 'successful-release', ...baseline };
}

export function restoredPublishedBaselines({ repoRoot, packageIndex, legacy, headRef = 'HEAD', includeWorkingTree = false }) {
  const restored = [];
  for (const descriptor of legacy?.restoredPublishedBaselines ?? []) {
    const entry = packageIndex.get(descriptor.name);
    if (!entry || entry.packageJson.version !== descriptor.version) continue;
    const packagePath = `mango-ui/packages/${entry.dir}`;
    const committed = runGit(repoRoot, ['diff', '--quiet', descriptor.gitCommit, headRef, '--', packagePath], {
      allowFailure: true,
    });
    const working = includeWorkingTree
      ? runGit(repoRoot, ['diff', '--quiet', '--', packagePath], { allowFailure: true })
      : { status: 0 };
    const untracked = includeWorkingTree
      ? runGit(repoRoot, ['ls-files', '--others', '--exclude-standard', '--', packagePath]).stdout.trim()
      : '';
    if (committed.status === 0 && working.status === 0 && !untracked) restored.push(descriptor);
  }
  return restored;
}

export function assertCleanWorktree(repoRoot) {
  const dirty = gitValue(repoRoot, ['status', '--porcelain']);
  if (dirty) throw new Error(`release preparation requires a clean worktree:\n${dirty}`);
}
