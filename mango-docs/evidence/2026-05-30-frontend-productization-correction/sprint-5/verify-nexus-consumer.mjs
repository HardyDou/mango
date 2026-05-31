import { cpSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { execFileSync } from 'node:child_process';
import { tmpdir } from 'node:os';

const repoRoot = process.cwd();
const evidenceDir = join(repoRoot, 'mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-5');
const consumerTemplateDir = join(evidenceDir, 'consumer-app');
const reportPath = join(evidenceDir, 'nexus-consumer-report.json');
const workspaceRoot = join(tmpdir(), `mango-sprint-5-nexus-consumer-${Date.now()}`);
const appDir = join(workspaceRoot, 'app');
const groupRegistry = 'http://nexus.inner.yunxinbaokeji.com/repository/npm-group/';
const packageVersion = '1.0.3';
const commands = [];
const errors = [];

function run(command, args, options = {}) {
  const startedAt = new Date().toISOString();
  try {
    const stdout = execFileSync(command, args, {
      cwd: options.cwd || repoRoot,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe'],
      env: { ...process.env, ...options.env },
    });
    commands.push({ command, args, cwd: options.cwd || repoRoot, ok: true, startedAt, stdout });
    return stdout;
  } catch (error) {
    commands.push({
      command,
      args,
      cwd: options.cwd || repoRoot,
      ok: false,
      startedAt,
      stdout: error.stdout?.toString() || '',
      stderr: error.stderr?.toString() || '',
      status: error.status,
    });
    throw error;
  }
}

try {
  rmSync(workspaceRoot, { recursive: true, force: true });
  mkdirSync(workspaceRoot, { recursive: true });
  cpSync(consumerTemplateDir, appDir, { recursive: true });

  const packageJsonPath = join(appDir, 'package.json');
  const packageJson = JSON.parse(run('node', ['-e', `console.log(JSON.stringify(require(${JSON.stringify(packageJsonPath)}), null, 2))`]));
  packageJson.dependencies['@mango/admin'] = packageVersion;
  writeFileSync(packageJsonPath, `${JSON.stringify(packageJson, null, 2)}\n`);

  run('npm', ['view', '@mango/admin', 'version', `--registry=${groupRegistry}`]);
  run('pnpm', ['install', '--ignore-workspace', `--registry=${groupRegistry}`], { cwd: appDir });
  run('pnpm', ['run', 'typecheck'], { cwd: appDir });
  run('pnpm', ['run', 'build'], { cwd: appDir });
} catch (error) {
  errors.push(error.message);
} finally {
  const report = {
    ok: errors.length === 0,
    checkedAt: new Date().toISOString(),
    registry: groupRegistry,
    packageVersion,
    workspaceRoot,
    appDir,
    errors,
    commands,
  };
  writeFileSync(reportPath, JSON.stringify(report, null, 2));
  console.log(`Nexus consumer report: ${reportPath}`);
  if (errors.length > 0) {
    process.exit(1);
  }
}
