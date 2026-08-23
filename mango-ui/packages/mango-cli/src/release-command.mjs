import { existsSync, readFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';
import { assertReleaseNodeVersion } from './release-runtime.mjs';

export const RELEASE_STATES = [
  'VALIDATED',
  'PREPARED',
  'READY',
  'PUBLISHING',
  'PARTIAL',
  'AMBIGUOUS',
  'REPAIR',
  'COMPLETED',
];
export const RELEASE_COMMAND_MAX_BUFFER_BYTES = 64 * 1024 * 1024;

const RELEASE_COMMAND_SCRIPTS = {
  plan: 'create-release-plan.mjs',
  prepare: 'prepare-release.mjs',
  publish: 'publish-prepared-release.mjs',
  repair: 'publish-prepared-release.mjs',
  status: 'publish-prepared-release.mjs',
};

export function redactReleaseText(value, env = process.env) {
  let redacted = String(value || '');
  for (const [key, secret] of Object.entries(env)) {
    if (
      !/token|password|secret|credential|private_key/iu.test(key) ||
      typeof secret !== 'string' ||
      secret.length < 4
    ) {
      continue;
    }
    redacted = redacted.split(secret).join('[REDACTED]');
  }
  return redacted
    .replace(/(https?:\/\/)[^\s/@:]+:[^\s/@]+@/giu, '$1[REDACTED]@')
    .replace(/(Bearer\s+)[A-Za-z0-9._~+/-]+/giu, '$1[REDACTED]')
    .replace(/((?:token|password|secret|credential)[=:]\s*)[^\s]+/giu, '$1[REDACTED]')
    .replace(/(_authToken=)[^\s]+/giu, '$1[REDACTED]');
}

export async function runReleaseCli(argv, runtime = {}) {
  const command = argv[0] || 'status';
  const env = runtime.env || process.env;
  if (command === 'verify') {
    throw new Error('mango release verify was removed; use status or repair against the prepared batch manifest');
  }
  const registryDoctor = command === 'registry' && argv[1] === 'doctor';
  if (command === 'registry' && !registryDoctor) throw new Error('mango release registry supports only doctor');
  const script = RELEASE_COMMAND_SCRIPTS[command];
  if (!script && !registryDoctor) throw new Error(`unknown release command: ${command}`);
  const projectRoot = findMangoRepository(runtime.cwd || process.cwd());
  assertReleaseNodeVersion({
    manifestPath: join(projectRoot, 'mango-ui/package.json'),
    nodeVersion: runtime.nodeVersion,
  });
  if (registryDoctor) return runRegistryDoctor(argv.slice(2), runtime, env);
  const scriptArgs = ['plan', 'prepare'].includes(command) ? argv.slice(1) : argv;
  const execute = runtime.spawnSync || spawnSync;
  const result = execute(process.execPath, [join(projectRoot, 'mango-ui/scripts/release', script), ...scriptArgs], {
    cwd: projectRoot,
    env,
    encoding: 'utf8',
    maxBuffer: RELEASE_COMMAND_MAX_BUFFER_BYTES,
  });
  writeResult(result, runtime, env);
  if (result.status !== 0) throw new Error(`mango release ${command} failed with exit code ${result.status ?? 1}`);
  return { command, projectRoot, exitCode: 0 };
}

export function findMangoRepository(start) {
  let current = resolve(start);
  while (true) {
    if (existsSync(join(current, 'mango-ui/scripts/release/create-release-plan.mjs'))) return current;
    const parent = dirname(current);
    if (parent === current) break;
    current = parent;
  }
  throw new Error('mango release commands must run inside a Mango source repository');
}

function runRegistryDoctor(argv, runtime, env) {
  const publishRegistry = argument(argv, '--publish-registry') || env.MANGO_RELEASE_NPM_PUBLISH_REGISTRY || '';
  const consumeRegistry = argument(argv, '--consume-registry') || env.MANGO_RELEASE_NPM_CONSUME_REGISTRY || '';
  const mavenPublishRegistry =
    argument(argv, '--maven-publish-registry') || env.MANGO_RELEASE_MAVEN_PUBLISH_REGISTRY || '';
  const mavenConsumeRegistry =
    argument(argv, '--maven-consume-registry') || env.MANGO_RELEASE_MAVEN_CONSUME_REGISTRY || '';
  const mavenPublishServerId =
    argument(argv, '--maven-publish-server-id') || env.MANGO_RELEASE_MAVEN_PUBLISH_SERVER_ID || '';
  const mavenConsumeServerId =
    argument(argv, '--maven-consume-server-id') || env.MANGO_RELEASE_MAVEN_CONSUME_SERVER_ID || '';
  const errors = [];
  for (const [label, value] of [
    ['npm publish registry', publishRegistry],
    ['npm consume registry', consumeRegistry],
  ]) {
    try {
      const url = new URL(value);
      if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password) {
        errors.push(`${label} must be an HTTP(S) URL without credentials`);
      }
    } catch {
      errors.push(`${label} must be an absolute URL`);
    }
  }
  const mavenConfigured = [mavenPublishRegistry, mavenConsumeRegistry, mavenPublishServerId, mavenConsumeServerId].some(
    (value) => Boolean(value),
  );
  if (mavenConfigured) {
    for (const [label, value] of [
      ['Maven publish registry', mavenPublishRegistry],
      ['Maven consume registry', mavenConsumeRegistry],
    ]) {
      try {
        const url = new URL(value);
        if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password) {
          errors.push(`${label} must be an HTTP(S) URL without credentials`);
        }
      } catch {
        errors.push(`${label} must be an absolute URL`);
      }
    }
    if (!mavenPublishServerId) errors.push('Maven publish settings.xml server id is required');
    if (!mavenConsumeServerId) errors.push('Maven consume settings.xml server id is required');
  }
  let mavenProbe = null;
  if (mavenConfigured && errors.length === 0) {
    try {
      mavenProbe = resolveMavenRegistryProbe(runtime.cwd || process.cwd());
    } catch (error) {
      errors.push(error instanceof Error ? error.message : String(error));
    }
  }
  const checks = [];
  if (errors.length === 0) {
    checks.push(
      runCheck('npm', ['whoami', `--registry=${publishRegistry}`], runtime.cwd || process.cwd(), env, runtime),
    );
    checks.push(
      runCheck(
        'npm',
        ['view', '@mango/cli', 'version', `--registry=${consumeRegistry}`],
        runtime.cwd || process.cwd(),
        env,
        runtime,
      ),
    );
    for (const check of checks) {
      if (check.exitCode !== 0) errors.push(`${check.command} failed: ${check.output}`);
    }
    if (mavenConfigured) {
      checks.push(
        runCheck(
          'curl',
          [
            '-fsSL',
            '--max-time',
            '20',
            '--output',
            '/dev/null',
            `${mavenPublishRegistry.replace(/\/$/u, '')}/${mavenProbe.path}`,
          ],
          runtime.cwd || process.cwd(),
          env,
          runtime,
        ),
      );
      checks.push(
        runCheck(
          'curl',
          [
            '-fsSL',
            '--max-time',
            '20',
            '--output',
            '/dev/null',
            `${mavenConsumeRegistry.replace(/\/$/u, '')}/${mavenProbe.path}`,
          ],
          runtime.cwd || process.cwd(),
          env,
          runtime,
        ),
      );
      for (const check of checks.slice(-2)) {
        if (check.exitCode !== 0) errors.push(`${check.command} failed: ${check.output}`);
      }
    }
  }
  const output = {
    command: 'registry-doctor',
    passed: errors.length === 0,
    publishRegistry,
    consumeRegistry,
    maven: mavenConfigured
      ? {
          publishRegistry: mavenPublishRegistry,
          consumeRegistry: mavenConsumeRegistry,
          publishServerId: mavenPublishServerId,
          consumeServerId: mavenConsumeServerId,
          probe: mavenProbe,
        }
      : null,
    checks,
    errors,
  };
  const json = argv.includes('--json');
  const stdout = runtime.stdout || process.stdout;
  const stderr = runtime.stderr || process.stderr;
  if (json) stdout.write(`${JSON.stringify(output, null, 2)}\n`);
  else if (output.passed) stdout.write('Mango release registry doctor PASS\n');
  else stderr.write(`Mango release registry doctor FAIL\n${errors.map((entry) => `- ${entry}`).join('\n')}\n`);
  if (!output.passed) throw new Error('release registry doctor failed');
  return output;
}

export function resolveMavenRegistryProbe(start) {
  const projectRoot = findMangoRepository(start);
  const plan = readJsonIfPresent(join(projectRoot, 'mango-ui/.changeset/release-plan.json'));
  const versions = readJsonIfPresent(join(projectRoot, 'mango-ui/packages/mango-cli/release-versions.json'));
  const version = plan?.maven?.sourceVersion || versions?.maven?.mangoBackend || '';
  if (!/^\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.-]+)?$/u.test(version)) {
    throw new Error('cannot resolve a published Maven baseline version for registry doctor');
  }
  return {
    coordinate: `io.mango:mango-bom:${version}`,
    path: `io/mango/mango-bom/${version}/mango-bom-${version}.pom`,
  };
}

function readJsonIfPresent(file) {
  if (!existsSync(file)) return null;
  return JSON.parse(readFileSync(file, 'utf8'));
}

function runCheck(command, args, cwd, env, runtime = {}) {
  const execute = runtime.spawnSync || spawnSync;
  const result = execute(command, args, { cwd, env, encoding: 'utf8', timeout: 60_000 });
  return {
    command: [command, ...args].join(' '),
    exitCode: result.status ?? 1,
    output: redactReleaseText(`${result.stdout || ''}${result.stderr || result.error?.message || ''}`, env).trim(),
  };
}

function writeResult(result, runtime, env) {
  if (result.stdout) (runtime.stdout || process.stdout).write(redactReleaseText(result.stdout, env));
  if (result.stderr) (runtime.stderr || process.stderr).write(redactReleaseText(result.stderr, env));
}

function argument(args, name) {
  const inline = args.find((value) => value.startsWith(`${name}=`));
  if (inline) return inline.slice(name.length + 1);
  const index = args.indexOf(name);
  return index >= 0 ? args[index + 1] || '' : '';
}
