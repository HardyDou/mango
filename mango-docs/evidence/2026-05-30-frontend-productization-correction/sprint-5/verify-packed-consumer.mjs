import { cpSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { isAbsolute, join } from 'node:path';
import { execFileSync } from 'node:child_process';
import { tmpdir } from 'node:os';

const repoRoot = process.cwd();
const evidenceDir = join(repoRoot, 'mango-docs/evidence/2026-05-30-frontend-productization-correction/sprint-5');
const consumerTemplateDir = join(evidenceDir, 'consumer-app');
const reportPath = join(evidenceDir, 'packed-consumer-report.json');
const workspaceRoot = join(tmpdir(), `mango-sprint-5-consumer-${Date.now()}`);
const packDir = join(workspaceRoot, 'packs');
const appDir = join(workspaceRoot, 'app');

const packages = [
  'api-schema',
  'common',
  'app-runtime',
  'auth',
  'calendar',
  'file',
  'numgen',
  'rbac',
  'system',
  'template',
  'workflow',
  'workflow-business-example',
  'notice',
  'admin-pages',
  'admin-shell',
  'admin',
];

const commands = [];
const errors = [];
const tarballs = {};

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
  mkdirSync(packDir, { recursive: true });
  cpSync(consumerTemplateDir, appDir, { recursive: true });

  run('pnpm', ['--filter', '@mango/admin...', 'build'], { cwd: join(repoRoot, 'mango-ui') });

  for (const packageName of packages) {
    const packageDir = join(repoRoot, 'mango-ui/packages', packageName);
    const output = run('pnpm', ['pack', '--pack-destination', packDir], { cwd: packageDir });
    const tarballName = output.trim().split('\n').at(-1);
    tarballs[`@mango/${packageName}`] = isAbsolute(tarballName) ? tarballName : join(packDir, tarballName);
  }

  const packageJsonPath = join(appDir, 'package.json');
  const consumerPackageJson = JSON.parse(run('node', ['-e', `console.log(JSON.stringify(require(${JSON.stringify(packageJsonPath)}), null, 2))`]));
  consumerPackageJson.pnpm = {
    ...(consumerPackageJson.pnpm || {}),
    overrides: {
      ...(consumerPackageJson.pnpm?.overrides || {}),
    },
  };
  for (const [packageName, tarballPath] of Object.entries(tarballs)) {
    consumerPackageJson.dependencies[packageName] = `file:${tarballPath}`;
    consumerPackageJson.pnpm.overrides[packageName] = `file:${tarballPath}`;
  }
  writeFileSync(packageJsonPath, `${JSON.stringify(consumerPackageJson, null, 2)}\n`);

  run('pnpm', ['install', '--ignore-workspace'], { cwd: appDir });
  run('pnpm', ['run', 'typecheck'], { cwd: appDir });
  run('pnpm', ['run', 'build'], { cwd: appDir });
} catch (error) {
  errors.push(error.message);
} finally {
  const report = {
    ok: errors.length === 0,
    checkedAt: new Date().toISOString(),
    workspaceRoot,
    appDir,
    packDir,
    tarballs,
    errors,
    commands,
  };
  writeFileSync(reportPath, JSON.stringify(report, null, 2));
  console.log(`Packed consumer report: ${reportPath}`);
  if (errors.length > 0) {
    process.exit(1);
  }
}
