#!/usr/bin/env node
import { createHash } from 'node:crypto';
import { existsSync, mkdirSync, readFileSync, readdirSync, renameSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';

const workspaceRoot = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const repoRoot = resolve(workspaceRoot, '..');
const args = process.argv.slice(2);
const base = valueArg('--base') || 'origin/main';
const head = valueArg('--head') || 'HEAD';
const prBodyPath = resolve(valueArg('--pr-body') || join(repoRoot, '.runtime/mango-release/pr-body.md'));
const cliTests = readdirSync(join(workspaceRoot, 'packages/mango-cli/tests'))
  .filter((file) => file.endsWith('.test.mjs'))
  .sort()
  .map((file) => `mango-ui/packages/mango-cli/tests/${file}`);
const nodeVersion = process.versions.node;
if (!isSupportedNode(nodeVersion)) {
  throw new Error(`local release checks require Node >=22.23.1 <23; current ${nodeVersion}`);
}
const status = run('git', ['status', '--porcelain'], repoRoot, { capture: true });
if (status.stdout.trim()) throw new Error('local release checks require a clean final candidate worktree');
if (!existsSync(prBodyPath)) {
  throw new Error(`local release checks require the final PR body at --pr-body or ${prBodyPath}`);
}
const prBodyEnv = { PR_BODY_FILE: prBodyPath };
const scope = JSON.parse(
  run('node', ['mango-pmo/tools/classify-pmo-check-scope.mjs', '--base', base, '--head', head], repoRoot, {
    capture: true,
  }).stdout,
);
const assurance = parseKeyValues(
  run('node', ['mango-pmo/tools/assurance-ci-scope.mjs', '--body', prBodyPath], repoRoot, {
    capture: true,
  }).stdout,
);

const checks = [
  command('git', ['diff', '--check', base, head], repoRoot),
  command('node', ['mango-pmo/tools/assurance-ci-scope.mjs', '--body', prBodyPath], repoRoot),
  command('node', ['mango-pmo/tools/risk-verification.mjs'], repoRoot, { env: prBodyEnv }),
  command(
    'node',
    [
      '--test',
      'mango-pmo/tests/architecture-debt-budget.test.mjs',
      'mango-pmo/tests/branch-protection-policy.test.mjs',
      'mango-pmo/tests/document-contract/document-contract.test.mjs',
      'mango-pmo/tests/frontend-page-baseline.test.mjs',
      'mango-pmo/tests/pmo-check-scope.test.mjs',
      'mango-pmo/tests/pmo-preflight.test.mjs',
      'mango-pmo/tests/risk-verification.test.mjs',
      'mango-pmo/tests/standard-delivery-record.test.mjs',
      'mango-pmo/tests/test-quality-check.test.mjs',
      'mango-pmo/tests/workspace-layout-check.test.mjs',
    ],
    repoRoot,
  ),
  command('node', ['mango-pmo/tests/skills/check-skill-evals.mjs'], repoRoot),
  command('node', ['mango-pmo/tests/skills/check-delivery-mode-history-cases.mjs'], repoRoot),
  command('node', ['mango-pmo/tools/check-pmo-preflight.mjs'], repoRoot),
  command('node', ['mango-pmo/tools/check-governance-intent.mjs'], repoRoot),
  command('node', ['mango-pmo/tests/skills/check-delivery-assurance-recommendations.mjs'], repoRoot),
  command('pnpm', ['-F', '@mango/pmo', 'build'], workspaceRoot),
  command('pnpm', ['-F', '@mango/pmo', 'check'], workspaceRoot),
  command('node', ['mango-business-starter/scripts/sync-pmo-baseline.mjs', '--check'], repoRoot),
  command('node', ['mango-business-starter/scripts/check-template.mjs'], repoRoot),
  command('node', ['mango-ui/packages/mango-cli/scripts/check-business-module-template.mjs'], repoRoot),
  command('node', ['mango-ui/packages/mango-cli/scripts/check-release-versions.mjs'], repoRoot),
  command('node', ['mango-ui/packages/mango-cli/scripts/check-cli.mjs'], repoRoot),
  command('node', ['--test', ...cliTests], repoRoot),
];
appendJavaRunnerChecks(checks, scope, assurance);
checks.push(
  command('pnpm', ['release:test'], workspaceRoot),
  command('pnpm', ['release:change-check', '--', `--base=${base}`, `--head=${head}`], workspaceRoot),
  command('pnpm', ['release:plan:check'], workspaceRoot),
  command('node', ['mango-ui/scripts/quality/check-repository-dev-manifest.mjs'], repoRoot),
  command(
    'node',
    ['mango-pmo/tools/check-frontend-page-baseline.mjs', '--base', base, '--head', head, '--frontend-root', 'mango-ui'],
    repoRoot,
  ),
  command('pnpm', ['install', '--frozen-lockfile', '--registry=https://registry.npmjs.org/'], workspaceRoot),
  command('pnpm', ['check:pr', '--', `--base=${base}`, `--head=${head}`], workspaceRoot),
  command('pnpm', ['admin:styles:check'], workspaceRoot),
  command('pnpm', ['admin:module-styles:check'], workspaceRoot),
  command('node', ['mango-pmo/tools/workspace-layout-check.mjs', '--root', '.'], repoRoot),
  command('node', ['mango-pmo/tools/audit-module-readmes.mjs'], repoRoot),
  command('node', ['mango-pmo/tools/audit-readme-source-facts.mjs'], repoRoot),
  command('node', ['mango-pmo/tools/check-business-guides.mjs'], repoRoot),
  command('node', ['mango-pmo/tools/check-capability-docs.mjs', '--base', base, '--head', head], repoRoot, {
    env: prBodyEnv,
  }),
);

const evidence = {
  schemaVersion: 1,
  base,
  head,
  headCommit: run('git', ['rev-parse', `${head}^{commit}`], repoRoot, { capture: true }).stdout.trim(),
  headTree: run('git', ['rev-parse', `${head}^{tree}`], repoRoot, { capture: true }).stdout.trim(),
  nodeVersion,
  pnpmVersion: run('pnpm', ['--version'], workspaceRoot, { capture: true }).stdout.trim(),
  prBody: prBodyPath,
  prBodySha256: createHash('sha256').update(readFileSync(prBodyPath)).digest('hex'),
  scope,
  assurance,
  startedAt: new Date().toISOString(),
  checks: [],
};

for (const check of checks) {
  const startedAt = new Date().toISOString();
  const result = run(check.executable, check.args, check.cwd, check.options);
  evidence.checks.push({
    command: [check.executable, ...check.args].join(' '),
    cwd: check.cwd,
    startedAt,
    completedAt: new Date().toISOString(),
    exitCode: result.status,
  });
  writeEvidence(evidence);
  if (result.status !== 0) throw new Error(`local release check failed: ${check.executable} ${check.args.join(' ')}`);
}
evidence.completedAt = new Date().toISOString();
evidence.status = 'PASS';
writeEvidence(evidence);
console.log(`Local release checks PASS for ${evidence.headCommit}`);

function command(executable, commandArgs, cwd, options = {}) {
  return { executable, args: commandArgs, cwd, options };
}

function run(executable, commandArgs, cwd, options = {}) {
  const result = spawnSync(executable, commandArgs, {
    cwd,
    encoding: 'utf8',
    stdio: options.capture ? 'pipe' : 'inherit',
    shell: options.shell === true,
    env: { ...process.env, ...(options.env || {}) },
  });
  if (options.capture && result.status !== 0) {
    throw new Error(`${executable} ${commandArgs.join(' ')} failed: ${(result.stderr || result.stdout).trim()}`);
  }
  return { status: result.status ?? 1, stdout: result.stdout || '', stderr: result.stderr || '' };
}

function appendJavaRunnerChecks(target, classification, selections) {
  const { scope: affected, maven } = classification;
  const javaSelected = selections.m09 || selections.m10 || selections.m11;
  const runJava = javaSelected && (affected.backend || affected.generated_backend || maven.mode === 'governance');
  if (!runJava) return;

  target.push(
    command(
      'mvn',
      ['-f', 'mango/pom.xml', '-pl', ':mango-bom,:mango-parent,:mango-common,:mango-tools', '-DskipTests', 'install'],
      repoRoot,
    ),
    command(
      'mvn',
      [
        '-f',
        'mango/pom.xml',
        '-pl',
        ':mango-architecture-rules,:mango-maven-plugin',
        `-DskipTests=${!selections.m10}`,
        'install',
      ],
      repoRoot,
    ),
  );

  const dependencyProjects = maven.projects.filter(
    (project) =>
      ![':mango-architecture-verification', 'architecture-verification', 'mango-architecture-verification'].includes(
        project,
      ),
  );
  if (selections.m09 && maven.mode === 'partial' && dependencyProjects.length > 0) {
    target.push(
      command(
        'mvn',
        [
          '-f',
          'mango/pom.xml',
          '-pl',
          dependencyProjects.join(','),
          '-am',
          '-DskipTests',
          '-Denforcer.skip=true',
          '-Dcheckstyle.skip=true',
          '-Dpmd.skip=true',
          '-Dspotbugs.skip=true',
          'install',
        ],
        repoRoot,
      ),
    );
  }
  if (selections.m11 && affected.generated_backend) {
    target.push(
      command(
        'pnpm',
        [
          'install',
          '--frozen-lockfile',
          '--filter',
          '@mango/cli...',
          '--ignore-scripts',
          '--registry=https://registry.npmjs.org/',
        ],
        workspaceRoot,
      ),
      command(
        'mvn',
        [
          '-f',
          'mango/pom.xml',
          '-pl',
          ':mango,:mango-parent,:mango-common,:mango-infra,:mango-infra-bootstrap,:mango-infra-bootstrap-api,:mango-infra-module,:mango-infra-module-api,:mango-infra-kv,:mango-infra-kv-api,:mango-infra-persistence,:mango-infra-persistence-api,:mango-infra-context,:mango-infra-context-api,:mango-infra-context-support,:mango-infra-context-starter,:mango-infra-persistence-starter,:mango-infra-web,:mango-infra-web-api,:mango-resource,:mango-resource-api,:mango-authorization,:mango-authorization-api,:mango-infra-web-support,:mango-infra-web-starter,:mango-infra-persistence-web-starter,:mango-infra-feign-starter,:mango-admin-starter',
          '-am',
          '-DskipTests',
          '-Dcheckstyle.skip=true',
          '-Dpmd.skip=true',
          '-Dspotbugs.skip=true',
          'install',
        ],
        repoRoot,
      ),
      command('node', ['mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs'], repoRoot, {
        env: { MANGO_BACKEND_GATE_VERSION: '1.0.0-SNAPSHOT' },
      }),
    );
  }
  if (selections.m09 && maven.mode === 'partial') {
    target.push(
      command(
        'mvn',
        [
          '-f',
          'mango/pom.xml',
          '-pl',
          maven.projects.join(','),
          '-DskipTests',
          '-Dmango.architecture.skip=false',
          '-Dmango.architecture.mode=changed',
          '-Dmango.architecture.requireFullReactor=false',
          `-Dmango.architecture.base=${base}`,
          '-Dmango.check.changedOnly=true',
          '-Dmango.check.gate=no-new-violations',
          `-Dmango.check.baseRef=${base}`,
          'verify',
        ],
        repoRoot,
      ),
    );
  }
  if (selections.m09 && maven.mode === 'governance') {
    target.push(
      command(
        'mvn',
        [
          '-f',
          'mango/pom.xml',
          '-DskipTests',
          '-Dmango.architecture.skip=false',
          '-Dmango.architecture.mode=changed',
          '-Dmango.architecture.requireFullReactor=true',
          '-Dmango.architecture.inventoryOnly=true',
          `-Dmango.architecture.base=${base}`,
          '-Dmango.check.changedOnly=false',
          '-Dmango.check.gate=no-new-violations',
          `-Dmango.check.baseRef=${base}`,
          'verify',
        ],
        repoRoot,
      ),
    );
  }
  if (selections.m09 && (affected.backend || maven.mode === 'governance')) {
    const debtArgs = maven.mode === 'governance' ? ['--base-ref', base] : ['--baseline-only', '--base-ref', base];
    target.push(command('node', ['mango-pmo/tools/check-architecture-debt-budget.mjs', ...debtArgs], repoRoot));
  }
  if (selections.m09 && affected.backend) {
    target.push(
      command(
        'mvn',
        [
          '-f',
          'mango/pom.xml',
          '-pl',
          ':mango-architecture-verification',
          'io.mango.tools.maven.plugin:mango-maven-plugin:1.0.0-SNAPSHOT:check',
          '-Drule=module-info',
          `-DbaseDir=${repoRoot}`,
          '-Dmango.check.changedOnly=true',
          '-Dmango.check.gate=no-new-violations',
          `-Dmango.check.baseRef=${base}`,
          '-Doutput=json',
          `-DreportFile=${join(repoRoot, '.runtime/pmo/backend-module-info.json')}`,
        ],
        repoRoot,
      ),
    );
  }
}

function parseKeyValues(output) {
  return Object.fromEntries(
    output
      .split(/\r?\n/u)
      .filter(Boolean)
      .map((line) => {
        const [key, value] = line.split('=', 2);
        return [key, value === 'true'];
      }),
  );
}

function writeEvidence(value) {
  const path = join(repoRoot, '.runtime/mango-release/local-checks.json');
  mkdirSync(dirname(path), { recursive: true });
  const temporary = `${path}.tmp`;
  writeFileSync(temporary, `${JSON.stringify(value, null, 2)}\n`, { mode: 0o600 });
  renameSync(temporary, path);
}

function isSupportedNode(version) {
  const match = version.match(/^(\d+)\.(\d+)\.(\d+)/u);
  if (!match) return false;
  const [, major, minor, patch] = match.map(Number);
  return major === 22 && (minor > 23 || (minor === 23 && patch >= 1));
}

function valueArg(name) {
  const inline = args.find((value) => value.startsWith(`${name}=`));
  if (inline) return inline.slice(name.length + 1);
  const index = args.indexOf(name);
  return index >= 0 ? args[index + 1] || '' : '';
}
