#!/usr/bin/env node
import { createHash } from 'node:crypto';
import { spawnSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  discoverTypecheckTargets,
  parseTypeScriptDiagnostics,
  resolveTypecheckCommand,
} from './typecheck-runner-lib.mjs';

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const defaultUiRoot = path.resolve(scriptDirectory, '../..');

function option(name, fallback) {
  const prefix = `--${name}=`;
  const argument = process.argv.slice(2).find((value) => value.startsWith(prefix));
  return argument ? argument.slice(prefix.length) : fallback;
}

const uiRoot = path.resolve(option('root', defaultUiRoot));
const outputFile = path.resolve(option('out', path.join(uiRoot, '../.runtime/frontend-quality/typecheck.json')));
const strict = process.argv.includes('--strict');
const { executable, shell } = resolveTypecheckCommand(uiRoot);
const { targets, skipped } = discoverTypecheckTargets(uiRoot);

if (targets.length === 0) {
  process.stderr.write('Typecheck failed: no workspace tsconfig inputs found\n');
  process.exit(1);
}
if (!fs.existsSync(executable)) {
  process.stderr.write(`Typecheck failed: vue-tsc executable not found at ${executable}\n`);
  process.exit(1);
}

const results = [];
for (const target of targets) {
  const started = process.hrtime.bigint();
  const child = spawnSync(executable, ['--noEmit', '--incremental', 'false', '-p', target.tsconfig], {
    cwd: uiRoot,
    encoding: 'utf8',
    env: { ...process.env, FORCE_COLOR: '0' },
    maxBuffer: 64 * 1024 * 1024,
    shell,
  });
  const output = `${child.stdout || ''}${child.stderr || ''}`;
  const diagnostics = parseTypeScriptDiagnostics(output, uiRoot, target.workspace);
  results.push({
    workspace: target.workspace,
    tsconfig: path.relative(uiRoot, target.tsconfig).split(path.sep).join('/'),
    status: child.status === 0 ? 'passed' : 'failed',
    exitCode: child.status ?? 1,
    signal: child.signal || null,
    durationMs: Number(process.hrtime.bigint() - started) / 1_000_000,
    diagnostics,
    diagnosticCount: diagnostics.length,
    outputSha256: createHash('sha256').update(output).digest('hex'),
    unmatchedOutput: output
      .split(/\r?\n/u)
      .filter(Boolean)
      .filter((line) => !/^(.*?)\((\d+),(\d+)\):\s+(error|warning)\s+TS\d+:/u.test(line)),
  });
  process.stderr.write(
    `${target.workspace}: ${child.status === 0 ? 'PASS' : `FAIL (${diagnostics.length} diagnostics)`}\n`,
  );
}

const report = {
  schemaVersion: 1,
  tool: 'vue-tsc',
  command: 'vue-tsc --noEmit --incremental false -p <workspace>/tsconfig.json',
  summary: {
    targetCount: results.length,
    passedCount: results.filter((item) => item.status === 'passed').length,
    failedCount: results.filter((item) => item.status === 'failed').length,
    skippedCount: skipped.length,
    diagnosticCount: results.reduce((total, item) => total + item.diagnosticCount, 0),
  },
  results,
  skipped,
};
fs.mkdirSync(path.dirname(outputFile), { recursive: true });
fs.writeFileSync(outputFile, `${JSON.stringify(report, null, 2)}\n`);
process.stdout.write(`${JSON.stringify({ outputFile, ...report.summary })}\n`);
if (strict && report.summary.failedCount > 0) process.exitCode = 1;
