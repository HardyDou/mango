#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { readAffectedWorkspaces, selectAffectedWorkspaces } from './affected-selector-lib.mjs';
import { createQualityCommandPlan } from './quality-command-plan-lib.mjs';

const uiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const repositoryRoot = path.resolve(uiRoot, '..');
const reportFile = path.join(repositoryRoot, '.runtime/frontend-quality/affected.json');
const base = process.argv.find((value) => value.startsWith('--base='))?.slice('--base='.length);
const head = process.argv.find((value) => value.startsWith('--head='))?.slice('--head='.length) || 'HEAD';
const profile = process.argv.find((value) => value.startsWith('--profile='))?.slice('--profile='.length) || 'deep';
let report;

function git(arguments_) {
  return spawnSync('git', arguments_, { cwd: repositoryRoot, encoding: 'utf8' });
}

function resolveChanges() {
  if (!base) return { scopeKnown: false, reason: 'local run has no explicit --base' };
  const baseCommit = git(['rev-parse', '--verify', `${base}^{commit}`]);
  const headCommit = git(['rev-parse', '--verify', `${head}^{commit}`]);
  if (baseCommit.status !== 0 || headCommit.status !== 0) {
    return { scopeKnown: false, reason: `base/head cannot be resolved: ${base}..${head}` };
  }
  const mergeBase = git(['merge-base', base, head]);
  if (mergeBase.status !== 0) return { scopeKnown: false, reason: `merge-base cannot be resolved: ${base}..${head}` };
  const diff = git(['diff', '--name-status', '--find-renames', `${mergeBase.stdout.trim()}..${head}`]);
  if (diff.status !== 0) return { scopeKnown: false, reason: `git diff failed: ${base}..${head}` };
  const changedPaths = diff.stdout
    .split('\n')
    .filter(Boolean)
    .flatMap((line) => line.split('\t').slice(1));
  return { scopeKnown: true, changedPaths, mergeBase: mergeBase.stdout.trim() };
}

function run(command, arguments_) {
  const result = spawnSync(command, arguments_, { cwd: uiRoot, stdio: 'inherit' });
  if (result.status !== 0)
    throw new Error(`command failed (${result.status ?? 'signal'}): ${command} ${arguments_.join(' ')}`);
}

try {
  const records = readAffectedWorkspaces(uiRoot);
  const changes = resolveChanges();
  const selection = selectAffectedWorkspaces(records, changes.changedPaths || [], changes);
  const pnpmVersion = spawnSync('pnpm', ['--version'], { cwd: uiRoot, encoding: 'utf8' });
  if (pnpmVersion.status !== 0) throw new Error('cannot resolve the active pnpm version');
  report = {
    schemaVersion: 1,
    base: base || null,
    head,
    mergeBase: changes.mergeBase || null,
    changedPathCount: changes.changedPaths?.length || 0,
    changedPaths: changes.changedPaths || [],
    ...selection,
    profile,
    scannedWorkspaceCount: records.length,
    toolchain: {
      node: process.version,
      pnpm: pnpmVersion.stdout.trim(),
    },
    executedCommands: [],
    startedAt: new Date().toISOString(),
  };
  fs.mkdirSync(path.dirname(reportFile), { recursive: true });
  const persist = () => fs.writeFileSync(reportFile, `${JSON.stringify(report, null, 2)}\n`);
  const execute = (command, arguments_) => {
    report.executedCommands.push([command, ...arguments_].join(' '));
    persist();
    run(command, arguments_);
  };
  persist();

  if (changes.scopeKnown && changes.changedPaths.length > 0) {
    execute(process.execPath, [
      './scripts/quality/run-changed-static.mjs',
      `--base-ref=${changes.mergeBase}`,
      ...changes.changedPaths,
    ]);
  }

  for (const [command, arguments_] of createQualityCommandPlan(records, selection, profile)) {
    execute(command, arguments_);
  }
  report.completedAt = new Date().toISOString();
  report.status = 'passed';
  persist();
  console.log(`affected frontend check PASS profile=${profile} ${JSON.stringify(selection)}`);
} catch (error) {
  const message = error instanceof Error ? error.message : String(error);
  if (report) {
    report.completedAt = new Date().toISOString();
    report.status = 'failed';
    report.error = message;
    fs.mkdirSync(path.dirname(reportFile), { recursive: true });
    fs.writeFileSync(reportFile, `${JSON.stringify(report, null, 2)}\n`);
  }
  console.error(message);
  process.exit(1);
}
