import { createHash } from 'node:crypto';
import { existsSync, mkdirSync, readFileSync, renameSync, writeFileSync } from 'node:fs';
import { homedir } from 'node:os';
import { dirname, isAbsolute, join, resolve } from 'node:path';
import { spawnSync } from 'node:child_process';

export const RELEASE_STATES = [
  'source',
  'versions',
  'changelog',
  'readmes',
  'tests',
  'pr',
  'tag',
  'github-release',
  'maven',
  'npm',
  'cli-lock',
  'private-registry-publish',
  'private-registry-consume-verify',
  'docs-latest',
  'docs-snapshot',
  'post-verify',
  'cleanup'
];

export const RELEASE_STATUSES = new Set(['passed', 'failed', 'pending', 'not_applicable']);
export const RELEASE_KINDS = new Set(['mixed', 'maven', 'npm-only']);
export const ARTIFACT_MODES = new Set([
  'private-registry',
  'public-registry',
  'artifact-only',
  'disabled'
]);

const IMMUTABLE_STATES = new Set(['tag', 'github-release', 'maven', 'npm', 'docs-snapshot']);
const PUBLISH_ACTION_STATES = new Set(['tag', 'github-release', 'maven', 'npm', 'docs-snapshot']);
const DEFAULT_MANIFEST_DIRECTORY = '.mango/releases';
const CONFIG_SCHEMA_VERSION = 1;
const MANIFEST_SCHEMA_VERSION = 2;

function deepMerge(...values) {
  const result = {};
  for (const value of values) {
    if (!value || typeof value !== 'object' || Array.isArray(value)) {
      continue;
    }
    for (const [key, next] of Object.entries(value)) {
      if (next === undefined) {
        continue;
      }
      if (next && typeof next === 'object' && !Array.isArray(next)) {
        result[key] = deepMerge(result[key], next);
      } else {
        result[key] = next;
      }
    }
  }
  return result;
}

function readJsonIfPresent(file) {
  if (!file || !existsSync(file)) {
    return {};
  }
  try {
    return JSON.parse(readFileSync(file, 'utf8'));
  } catch (error) {
    throw new Error(`Cannot read release config ${file}: ${error.message}`);
  }
}

function parseBoolean(value) {
  if (value === undefined) {
    return undefined;
  }
  return ['1', 'true', 'yes', 'on'].includes(String(value).toLowerCase());
}

function environmentConfig(env) {
  return {
    manifestDirectory: env.MANGO_RELEASE_MANIFEST_DIR,
    tag: env.MANGO_RELEASE_TAG,
    releaseKind: env.MANGO_RELEASE_KIND,
    authorized: parseBoolean(env.MANGO_RELEASE_AUTHORIZED),
    artifacts: {
      maven: {
        mode: env.MANGO_RELEASE_MAVEN_MODE,
        disabledReason: env.MANGO_RELEASE_MAVEN_DISABLED_REASON,
        serverId: env.MANGO_RELEASE_MAVEN_SERVER_ID
      },
      npm: {
        mode: env.MANGO_RELEASE_NPM_MODE,
        disabledReason: env.MANGO_RELEASE_NPM_DISABLED_REASON,
        tokenEnv: env.MANGO_RELEASE_NPM_TOKEN_ENV,
        npmConfig: env.MANGO_RELEASE_NPM_CONFIG
      }
    },
    registries: {
      maven: {
        publish: env.MANGO_RELEASE_MAVEN_PUBLISH_REGISTRY,
        consume: env.MANGO_RELEASE_MAVEN_CONSUME_REGISTRY
      },
      npm: {
        publish: env.MANGO_RELEASE_NPM_PUBLISH_REGISTRY,
        consume: env.MANGO_RELEASE_NPM_CONSUME_REGISTRY
      }
    }
  };
}

function cliConfig(options) {
  return {
    manifestDirectory: options.manifestDirectory,
    tag: options.tag,
    pr: options.pr,
    releaseKind: options.releaseKind,
    authorized: options.authorized,
    artifacts: {
      maven: {
        mode: options.mavenMode,
        disabledReason: options.mavenDisabledReason,
        serverId: options.mavenServerId
      },
      npm: {
        mode: options.npmMode,
        disabledReason: options.npmDisabledReason,
        tokenEnv: options.npmTokenEnv,
        npmConfig: options.npmConfig
      }
    },
    registries: {
      maven: {
        publish: options.mavenPublishRegistry,
        consume: options.mavenConsumeRegistry
      },
      npm: {
        publish: options.npmPublishRegistry,
        consume: options.npmConsumeRegistry
      }
    }
  };
}

export function resolveReleaseConfig(options, env = process.env) {
  const projectRoot = resolve(options.projectDir || process.cwd());
  const projectConfigPath = resolve(
    projectRoot,
    options.configPath || '.mango-release.json'
  );
  const userConfigPath = resolve(
    options.userConfigPath
      || env.MANGO_RELEASE_USER_CONFIG
      || join(env.XDG_CONFIG_HOME || join(homedir(), '.config'), 'mango/release.json')
  );
  const projectConfig = readJsonIfPresent(projectConfigPath);
  const userConfig = readJsonIfPresent(userConfigPath);
  const config = deepMerge(
    projectConfig,
    userConfig,
    environmentConfig(env),
    cliConfig(options)
  );
  config.authorized = options.authorized
    ?? parseBoolean(env.MANGO_RELEASE_AUTHORIZED)
    ?? false;
  config.schemaVersion = config.schemaVersion || CONFIG_SCHEMA_VERSION;
  config.projectRoot = projectRoot;
  config.projectConfigPath = projectConfigPath;
  config.userConfigPath = userConfigPath;
  config.manifestDirectory = resolve(
    projectRoot,
    config.manifestDirectory || DEFAULT_MANIFEST_DIRECTORY
  );
  config.stateAdapters = config.stateAdapters || {};
  config.notApplicable = config.notApplicable || {};
  return config;
}

function validateUrl(value, field, errors) {
  if (!value) {
    errors.push(`${field} is required`);
    return;
  }
  try {
    const url = new URL(value);
    if (!['http:', 'https:'].includes(url.protocol)) {
      errors.push(`${field} must use http or https`);
    }
    if (url.username || url.password) {
      errors.push(`${field} must not contain credentials`);
    }
  } catch {
    errors.push(`${field} must be an absolute URL`);
  }
}

function validateArtifact(name, artifact, registries, env, errors) {
  const mode = artifact?.mode;
  if (!ARTIFACT_MODES.has(mode)) {
    errors.push(`artifacts.${name}.mode must explicitly select ${[...ARTIFACT_MODES].join(', ')}`);
    return;
  }
  if (mode === 'disabled') {
    if (!artifact.disabledReason?.trim()) {
      errors.push(`artifacts.${name}.disabledReason is required when disabled`);
    }
    return;
  }
  if (mode === 'artifact-only') {
    return;
  }
  validateUrl(registries?.publish, `registries.${name}.publish`, errors);
  validateUrl(registries?.consume, `registries.${name}.consume`, errors);
  if (name === 'maven' && !artifact.serverId?.trim()) {
    errors.push('artifacts.maven.serverId is required for registry publication');
  }
  if (name === 'npm') {
    if (!artifact.tokenEnv?.trim() && !artifact.npmConfig?.trim()) {
      errors.push('artifacts.npm.tokenEnv or artifacts.npm.npmConfig is required for registry publication');
    }
    if (artifact.tokenEnv && !env[artifact.tokenEnv]) {
      errors.push(`npm credential environment variable is not set: ${artifact.tokenEnv}`);
    }
  }
}

export function doctorReleaseConfig(config, env = process.env) {
  const errors = [];
  if (config.schemaVersion !== CONFIG_SCHEMA_VERSION) {
    errors.push(`schemaVersion must be ${CONFIG_SCHEMA_VERSION}`);
  }
  if (!RELEASE_KINDS.has(config.releaseKind)) {
    errors.push(`releaseKind must explicitly select ${[...RELEASE_KINDS].join(', ')}`);
  }
  validateArtifact('maven', config.artifacts?.maven, config.registries?.maven, env, errors);
  validateArtifact('npm', config.artifacts?.npm, config.registries?.npm, env, errors);
  for (const [state, configured] of Object.entries(config.stateAdapters || {})) {
    if (!RELEASE_STATES.includes(state)) {
      errors.push(`unknown stateAdapters key: ${state}`);
      continue;
    }
    for (const action of ['publish', 'verify']) {
      validateExecutableAdapters(
        configured?.[action],
        `stateAdapters.${state}.${action}`,
        errors
      );
    }
    if (!IMMUTABLE_STATES.has(state)) {
      validateExecutableAdapters(
        configured?.repair,
        `stateAdapters.${state}.repair`,
        errors
      );
    }
  }
  for (const state of IMMUTABLE_STATES) {
    const repair = config.stateAdapters?.[state]?.repair;
    if (repair !== undefined
      && (!repair
        || Array.isArray(repair)
        || repair.kind !== 'verify-existing'
        || Object.keys(repair).some((key) => key !== 'kind'))) {
      errors.push(
        `stateAdapters.${state}.repair must be exactly { kind: "verify-existing" }`
      );
    }
    if (repair?.kind === 'verify-existing'
      && normalizeAdapters(config.stateAdapters?.[state]?.verify).length === 0) {
      errors.push(`stateAdapters.${state}.repair requires a non-empty verify adapter`);
    }
  }
  for (const [state, reason] of Object.entries(config.notApplicable || {})) {
    if (!RELEASE_STATES.includes(state)) {
      errors.push(`unknown notApplicable state: ${state}`);
    } else if (typeof reason !== 'string' || !reason.trim()) {
      errors.push(`notApplicable.${state} requires a non-empty reason`);
    }
  }
  return { passed: errors.length === 0, errors };
}

function validateExecutableAdapters(value, field, errors) {
  if (value === undefined) {
    return;
  }
  const adapters = normalizeAdapters(value);
  if (adapters.length === 0) {
    errors.push(`${field} must not be empty`);
    return;
  }
  for (const adapter of adapters) {
    if (!adapter || typeof adapter.command !== 'string' || !adapter.command.trim()) {
      errors.push(`${field} requires a non-empty command`);
    }
  }
}

function artifactMode(config, name) {
  return config.artifacts?.[name]?.mode;
}

export function releaseApplicability(config) {
  const result = {};
  for (const state of RELEASE_STATES) {
    result[state] = { applicable: true, reason: 'required release state' };
  }
  for (const [state, reason] of Object.entries(config.notApplicable || {})) {
    result[state] = { applicable: false, reason };
  }

  for (const name of ['maven', 'npm']) {
    if (artifactMode(config, name) === 'disabled') {
      result[name] = {
        applicable: false,
        reason: config.artifacts[name].disabledReason
      };
    }
  }

  const privateArtifacts = ['maven', 'npm'].filter(
    (name) => artifactMode(config, name) === 'private-registry'
  );
  if (privateArtifacts.length === 0) {
    result['private-registry-publish'] = {
      applicable: false,
      reason: 'no artifact uses private-registry mode'
    };
    result['private-registry-consume-verify'] = {
      applicable: false,
      reason: 'no artifact uses private-registry mode'
    };
  }

  const mavenFormal = artifactMode(config, 'maven') !== 'disabled'
    && artifactMode(config, 'maven') !== undefined
    && config.releaseKind !== 'npm-only';
  if (!mavenFormal) {
    result['docs-snapshot'] = {
      applicable: false,
      reason: 'npm-only or Maven-disabled release updates Latest without a Maven snapshot'
    };
  }
  return result;
}

function now() {
  return new Date().toISOString();
}

function initialState(state, applicability) {
  if (!applicability.applicable) {
    return {
      status: 'not_applicable',
      reason: applicability.reason,
      attempts: 0,
      immutableAttempted: false,
      immutableCompleted: false,
      updatedAt: now(),
      evidence: []
    };
  }
  return {
    status: 'pending',
    reason: 'not executed',
    attempts: 0,
    immutableAttempted: false,
    immutableCompleted: false,
    updatedAt: now(),
    evidence: []
  };
}

function sanitizedConfig(config) {
  return {
    schemaVersion: config.schemaVersion,
    releaseKind: config.releaseKind || 'mixed',
    tag: config.tag || null,
    pr: config.pr || null,
    manifestDirectory: config.manifestDirectory,
    artifacts: {
      maven: {
        mode: config.artifacts?.maven?.mode || null,
        disabledReason: config.artifacts?.maven?.disabledReason || null,
        serverId: config.artifacts?.maven?.serverId || null
      },
      npm: {
        mode: config.artifacts?.npm?.mode || null,
        disabledReason: config.artifacts?.npm?.disabledReason || null,
        tokenEnv: config.artifacts?.npm?.tokenEnv || null,
        npmConfig: config.artifacts?.npm?.npmConfig || null
      }
    },
    registries: config.registries || {},
    stateAdapters: config.stateAdapters,
    notApplicable: config.notApplicable
  };
}

function digest(value) {
  return createHash('sha256').update(JSON.stringify(value)).digest('hex');
}

function createManifest(version, config) {
  const applicability = releaseApplicability(config);
  const states = Object.fromEntries(
    RELEASE_STATES.map((state) => [state, initialState(state, applicability[state])])
  );
  const createdAt = now();
  return {
    schemaVersion: MANIFEST_SCHEMA_VERSION,
    version,
    tag: config.tag || null,
    projectRoot: config.projectRoot,
    configDigest: digest(sanitizedConfig(config)),
    createdAt,
    updatedAt: createdAt,
    completed: false,
    applicability,
    states
  };
}

function safeVersion(value) {
  if (!value || !/^[0-9A-Za-z][0-9A-Za-z._-]*$/.test(value)) {
    throw new Error('release --version must contain only letters, numbers, dot, underscore, and hyphen');
  }
  return value;
}

function manifestPath(config, version) {
  return join(config.manifestDirectory, safeVersion(version), 'manifest.json');
}

function readManifest(config, version, create = false) {
  const file = manifestPath(config, version);
  if (!existsSync(file)) {
    return { file, manifest: create ? createManifest(version, config) : null };
  }
  const manifest = JSON.parse(readFileSync(file, 'utf8'));
  if (manifest.schemaVersion !== MANIFEST_SCHEMA_VERSION || manifest.version !== version) {
    throw new Error(`Unsupported release manifest: ${file}`);
  }
  const expectedApplicability = releaseApplicability(config);
  for (const state of RELEASE_STATES) {
    const recordedApplicability = manifest.applicability?.[state];
    const expected = expectedApplicability[state];
    if (recordedApplicability?.applicable !== expected.applicable
      || recordedApplicability?.reason !== expected.reason) {
      throw new Error(`Release manifest applicability ${state} does not match release config`);
    }
    if (!stateIsAuditable(manifest.states?.[state], recordedApplicability)) {
      throw new Error(`Release manifest state ${state} is invalid`);
    }
  }
  if (manifest.completed !== manifestIsComplete(manifest)) {
    throw new Error(`Release manifest completion flag is inconsistent: ${file}`);
  }
  return { file, manifest };
}

function writeManifest(file, manifest) {
  manifest.updatedAt = now();
  manifest.completed = manifestIsComplete(manifest);
  mkdirSync(dirname(file), { recursive: true });
  const temporary = `${file}.tmp`;
  writeFileSync(temporary, `${JSON.stringify(manifest, null, 2)}\n`, { mode: 0o600 });
  renameSync(temporary, file);
}

function validTimestamp(value) {
  return typeof value === 'string' && Number.isFinite(Date.parse(value));
}

function evidenceIsComplete(evidence) {
  return typeof evidence?.command === 'string'
    && evidence.command.trim().length > 0
    && typeof evidence?.cwd === 'string'
    && evidence.cwd.trim().length > 0
    && validTimestamp(evidence.startedAt)
    && validTimestamp(evidence.completedAt)
    && Number.isInteger(evidence.exitCode)
    && typeof evidence.output === 'string'
    && evidence.output.trim().length > 0;
}

function stateIsAuditable(item, applicability) {
  if (!RELEASE_STATUSES.has(item?.status)
    || typeof item.reason !== 'string'
    || !item.reason.trim()
    || !validTimestamp(item.updatedAt)
    || !Array.isArray(item.evidence)) {
    return false;
  }
  if (!applicability?.applicable) {
    return item.status === 'not_applicable'
      && item.reason === applicability.reason
      && item.attempts === 0
      && item.evidence.length === 0;
  }
  if (item.status === 'not_applicable') {
    return false;
  }
  if (item.status === 'pending') {
    return item.attempts === 0;
  }
  return Number.isInteger(item.attempts)
    && item.attempts > 0
    && item.evidence.length > 0
    && item.evidence.every(evidenceIsComplete);
}

function manifestIsComplete(manifest) {
  return RELEASE_STATES.every((state) => {
    const item = manifest.states?.[state];
    return ['passed', 'not_applicable'].includes(item?.status)
      && stateIsAuditable(item, manifest.applicability?.[state]);
  });
}

function templateValue(value, context) {
  return String(value)
    .replaceAll('{version}', context.version)
    .replaceAll('{tag}', context.config.tag || '')
    .replaceAll('{projectRoot}', context.config.projectRoot)
    .replaceAll('{manifest}', context.manifestFile)
    .replaceAll('{mavenPublishRegistry}', context.config.registries?.maven?.publish || '')
    .replaceAll('{mavenConsumeRegistry}', context.config.registries?.maven?.consume || '')
    .replaceAll('{npmPublishRegistry}', context.config.registries?.npm?.publish || '')
    .replaceAll('{npmConsumeRegistry}', context.config.registries?.npm?.consume || '');
}

function normalizeAdapters(value) {
  if (!value) {
    return [];
  }
  return Array.isArray(value) ? value : [value];
}

function secretValues(env) {
  return Object.entries(env)
    .filter(([key, value]) => /token|password|secret|credential|private_key/i.test(key)
      && typeof value === 'string'
      && value.length >= 4)
    .map(([, value]) => value);
}

export function redactReleaseText(value, env = process.env) {
  let redacted = String(value || '');
  for (const secret of secretValues(env)) {
    redacted = redacted.split(secret).join('[REDACTED]');
  }
  return redacted
    .replace(/(https?:\/\/)[^\s/@:]+:[^\s/@]+@/gi, '$1[REDACTED]@')
    .replace(/(Bearer\s+)[A-Za-z0-9._~+\/-]+/gi, '$1[REDACTED]')
    .replace(/((?:token|password|secret|credential)[=:]\s*)[^\s]+/gi, '$1[REDACTED]')
    .replace(/(_authToken=)[^\s]+/gi, '$1[REDACTED]');
}

function executeAdapter(adapter, context) {
  if (!adapter || typeof adapter.command !== 'string' || !adapter.command.trim()) {
    return { passed: false, reason: 'adapter command is missing', evidence: [] };
  }
  const command = templateValue(adapter.command, context);
  const args = (adapter.args || []).map((arg) => templateValue(arg, context));
  const cwdValue = adapter.cwd ? templateValue(adapter.cwd, context) : context.config.projectRoot;
  const cwd = isAbsolute(cwdValue) ? cwdValue : resolve(context.config.projectRoot, cwdValue);
  const childEnv = { ...context.env };
  for (const [key, value] of Object.entries(adapter.env || {})) {
    if (typeof value === 'string' && value.startsWith('env:')) {
      childEnv[key] = context.env[value.slice(4)] || '';
    } else {
      childEnv[key] = templateValue(value, context);
    }
  }
  const startedAt = now();
  const result = spawnSync(command, args, {
    cwd,
    env: childEnv,
    encoding: 'utf8',
    stdio: ['ignore', 'pipe', 'pipe'],
    timeout: adapter.timeoutMs || 30 * 60 * 1000
  });
  const output = redactReleaseText(`${result.stdout || ''}${result.stderr || ''}`, childEnv).trim();
  const commandText = redactReleaseText([command, ...args].join(' '), childEnv);
  const evidence = [{
    command: commandText,
    cwd,
    startedAt,
    completedAt: now(),
    exitCode: result.status,
    output: output.slice(-12000)
  }];
  if (result.error) {
    return { passed: false, reason: redactReleaseText(result.error.message, childEnv), evidence };
  }
  return {
    passed: result.status === 0,
    reason: result.status === 0 ? `adapter passed: ${commandText}` : `adapter failed: ${commandText}`,
    evidence
  };
}

function executeAdapters(adapters, context) {
  const evidence = [];
  for (const adapter of normalizeAdapters(adapters)) {
    const result = executeAdapter(adapter, context);
    evidence.push(...result.evidence);
    if (!result.passed) {
      return { ...result, evidence };
    }
  }
  return {
    passed: true,
    reason: `${evidence.length} adapter command(s) passed`,
    evidence
  };
}

function normalizedStateEvidence(state, action, result, context, startedAt, completedAt) {
  const rawEvidence = result.evidence?.length > 0 ? result.evidence : [{}];
  return rawEvidence.map((evidence) => {
    const fallbackOutput = Object.keys(evidence).length > 0
      ? JSON.stringify(evidence)
      : result.reason;
    return {
      ...evidence,
      command: redactReleaseText(
        evidence.command || `builtin:${state}:${action}`,
        context.env
      ),
      cwd: evidence.cwd || context.config.projectRoot,
      startedAt: evidence.startedAt || startedAt,
      completedAt: evidence.completedAt || completedAt,
      exitCode: Number.isInteger(evidence.exitCode)
        ? evidence.exitCode
        : (result.passed ? 0 : 1),
      output: redactReleaseText(evidence.output || fallbackOutput || '[no output]', context.env)
        .trim() || '[no output]'
    };
  });
}

function builtinSource(context) {
  const result = spawnSync('git', ['status', '--porcelain'], {
    cwd: context.config.projectRoot,
    encoding: 'utf8'
  });
  if (result.status !== 0) {
    return { passed: false, reason: 'cannot read Git source state', evidence: [] };
  }
  const dirty = result.stdout.trim();
  return dirty
    ? { passed: false, reason: 'source worktree is not clean', evidence: [{ output: dirty.slice(0, 12000) }] }
    : { passed: true, reason: 'source worktree is clean', evidence: [] };
}

function builtinPrivatePublish(context) {
  const required = ['maven', 'npm'].filter(
    (name) => artifactMode(context.config, name) === 'private-registry'
  );
  const missing = required.filter((state) => context.manifest.states[state].status !== 'passed');
  return missing.length === 0
    ? { passed: true, reason: `private registry artifact states passed: ${required.join(', ')}`, evidence: [] }
    : { passed: false, reason: `private registry artifact states not passed: ${missing.join(', ')}`, evidence: [] };
}

function builtinCleanup() {
  return {
    passed: true,
    reason: 'release cleanup preserves source and immutable artifacts; no destructive cleanup configured',
    evidence: []
  };
}

function builtinPullRequest(context) {
  const args = ['pr', 'view'];
  if (context.config.pr) {
    args.push(String(context.config.pr));
  }
  args.push('--json', 'number,state,mergedAt,url');
  const result = spawnSync('gh', args, {
    cwd: context.config.projectRoot,
    env: context.env,
    encoding: 'utf8'
  });
  if (result.status !== 0) {
    return { passed: false, reason: 'release PR cannot be resolved', evidence: [] };
  }
  const pullRequest = JSON.parse(result.stdout);
  return pullRequest.state === 'MERGED' && pullRequest.mergedAt
    ? { passed: true, reason: `release PR #${pullRequest.number} merged`, evidence: [{ url: pullRequest.url }] }
    : { passed: false, reason: `release PR #${pullRequest.number} is not merged`, evidence: [{ url: pullRequest.url }] };
}

function builtinTag(context) {
  if (!context.config.tag) {
    return { passed: false, reason: 'release tag is not configured', evidence: [] };
  }
  const result = spawnSync('git', ['rev-parse', '--verify', `refs/tags/${context.config.tag}`], {
    cwd: context.config.projectRoot,
    encoding: 'utf8'
  });
  return result.status === 0
    ? { passed: true, reason: `tag exists: ${context.config.tag}`, evidence: [{ sha: result.stdout.trim() }] }
    : { passed: false, reason: `tag does not exist: ${context.config.tag}`, evidence: [] };
}

function builtinGithubRelease(context) {
  if (!context.config.tag) {
    return { passed: false, reason: 'release tag is not configured', evidence: [] };
  }
  const result = spawnSync('gh', ['release', 'view', context.config.tag, '--json', 'url,tagName'], {
    cwd: context.config.projectRoot,
    env: context.env,
    encoding: 'utf8'
  });
  if (result.status !== 0) {
    return { passed: false, reason: `GitHub Release does not exist: ${context.config.tag}`, evidence: [] };
  }
  const release = JSON.parse(result.stdout);
  return {
    passed: release.tagName === context.config.tag,
    reason: release.tagName === context.config.tag
      ? `GitHub Release exists: ${context.config.tag}`
      : `GitHub Release tag mismatch: ${release.tagName}`,
    evidence: [{ url: release.url }]
  };
}

function builtinFor(state, context) {
  if (state === 'source') {
    return builtinSource(context);
  }
  if (state === 'private-registry-publish') {
    return builtinPrivatePublish(context);
  }
  if (state === 'pr') {
    return builtinPullRequest(context);
  }
  if (state === 'tag') {
    return builtinTag(context);
  }
  if (state === 'github-release') {
    return builtinGithubRelease(context);
  }
  if (state === 'cleanup') {
    return builtinCleanup();
  }
  return null;
}

function adapterFor(state, action, config) {
  const configured = config.stateAdapters?.[state] || {};
  if (action === 'repair'
    && IMMUTABLE_STATES.has(state)
    && configured.repair?.kind === 'verify-existing') {
    return configured.verify;
  }
  if (configured[action]) {
    return configured[action];
  }
  if (action === 'repair' && configured.publish) {
    return configured.publish;
  }
  if (action === 'publish' && !PUBLISH_ACTION_STATES.has(state) && configured.verify) {
    return configured.verify;
  }
  return configured.verify;
}

function executeState(state, action, context) {
  const current = context.manifest.states[state];
  if (current.status === 'not_applicable') {
    return current;
  }
  if (current.status === 'passed' && action !== 'verify') {
    return current;
  }
  if (action === 'repair'
      && IMMUTABLE_STATES.has(state)
      && current.immutableAttempted
      && !current.immutableCompleted
      && !context.config.stateAdapters?.[state]?.repair) {
    return {
      ...current,
      status: 'failed',
      reason: 'repair adapter required after an immutable publish attempt; automatic republish is forbidden',
      updatedAt: now()
    };
  }

  let effectiveAction = action;
  if (action === 'repair' && IMMUTABLE_STATES.has(state)) {
    if (!current.immutableAttempted) {
      effectiveAction = 'publish';
    } else if (current.immutableCompleted) {
      effectiveAction = 'verify';
    }
  }
  const startedAt = now();
  const adapter = adapterFor(state, effectiveAction, context.config);
  let result;
  if (adapter) {
    result = executeAdapters(adapter, context);
  } else {
    result = builtinFor(state, context);
  }
  if (!result) {
    result = {
      passed: false,
      reason: `no ${action} or verify adapter configured for required state ${state}`,
      evidence: []
    };
  }
  const completedAt = now();
  const evidence = normalizedStateEvidence(
    state,
    effectiveAction,
    result,
    context,
    startedAt,
    completedAt
  );
  return {
    ...current,
    status: result.passed ? 'passed' : 'failed',
    reason: result.reason || (result.passed ? 'state passed' : 'state failed'),
    attempts: current.attempts + 1,
    immutableAttempted: current.immutableAttempted
      || (IMMUTABLE_STATES.has(state) && ['publish', 'repair'].includes(action)),
    immutableCompleted: current.immutableCompleted
      || (IMMUTABLE_STATES.has(state)
        && ['publish', 'repair'].includes(action)
        && result.passed),
    updatedAt: completedAt,
    evidence: [...current.evidence, ...evidence]
  };
}

function executionPlan(manifest) {
  return RELEASE_STATES.filter((state) =>
    !['passed', 'not_applicable'].includes(manifest.states[state].status)
  );
}

function runStateMachine(command, version, config, options, env) {
  const create = command !== 'status';
  const loaded = readManifest(config, version, create);
  const manifest = loaded.manifest || createManifest(version, config);
  const currentDigest = digest(sanitizedConfig(config));
  if (command === 'status') {
    return { command, manifestFile: loaded.file, manifest, plan: executionPlan(manifest) };
  }
  if (existsSync(loaded.file) && manifest.configDigest !== currentDigest) {
    throw new Error('release config changed after manifest creation; use the original config or start a new version');
  }
  if (options.dryRun) {
    return { command, dryRun: true, manifestFile: loaded.file, manifest, plan: executionPlan(manifest) };
  }

  const action = command === 'verify' ? 'verify' : command;
  const context = {
    version,
    config,
    manifest,
    manifestFile: loaded.file,
    env
  };
  for (const state of RELEASE_STATES) {
    manifest.states[state] = executeState(state, action, context);
    writeManifest(loaded.file, manifest);
    if (manifest.states[state].status === 'failed') {
      break;
    }
  }
  return { command, manifestFile: loaded.file, manifest, plan: executionPlan(manifest) };
}

function parseReleaseArgs(argv) {
  const options = {
    command: argv[0] || 'status',
    version: '',
    projectDir: '',
    configPath: '',
    userConfigPath: '',
    manifestDirectory: undefined,
    tag: undefined,
    releaseKind: undefined,
    authorized: undefined,
    dryRun: false,
    json: false
  };
  let start = 1;
  if (options.command === 'registry') {
    if (argv[1] !== 'doctor') {
      throw new Error('release registry supports only doctor');
    }
    options.command = 'registry-doctor';
    start = 2;
  }
  for (let index = start; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--dry-run') {
      options.dryRun = true;
      continue;
    }
    if (arg === '--json') {
      options.json = true;
      continue;
    }
    if (arg === '--authorize') {
      options.authorized = true;
      continue;
    }
    const mapping = {
      '--version': 'version',
      '--project-dir': 'projectDir',
      '--config': 'configPath',
      '--user-config': 'userConfigPath',
      '--manifest-dir': 'manifestDirectory',
      '--tag': 'tag',
      '--release-kind': 'releaseKind',
      '--pr': 'pr',
      '--maven-mode': 'mavenMode',
      '--npm-mode': 'npmMode',
      '--maven-disabled-reason': 'mavenDisabledReason',
      '--npm-disabled-reason': 'npmDisabledReason',
      '--maven-publish-registry': 'mavenPublishRegistry',
      '--maven-consume-registry': 'mavenConsumeRegistry',
      '--npm-publish-registry': 'npmPublishRegistry',
      '--npm-consume-registry': 'npmConsumeRegistry',
      '--maven-server-id': 'mavenServerId',
      '--npm-token-env': 'npmTokenEnv',
      '--npm-config': 'npmConfig'
    };
    const key = mapping[arg];
    if (!key) {
      throw new Error(`unknown release option: ${arg}`);
    }
    if (!argv[index + 1] || argv[index + 1].startsWith('--')) {
      throw new Error(`missing value for ${arg}`);
    }
    options[key] = argv[index + 1];
    index += 1;
  }
  return options;
}

function statusLines(result) {
  const lines = [
    `Release ${result.manifest.version}`,
    `Manifest: ${result.manifestFile}`,
    `Complete: ${result.manifest.completed ? 'yes' : 'no'}`
  ];
  for (const state of RELEASE_STATES) {
    const item = result.manifest.states[state];
    lines.push(`${state.padEnd(34)} ${item.status.padEnd(14)} ${item.reason}`);
  }
  return lines.join('\n');
}

export async function runReleaseCli(argv, runtime = {}) {
  const env = runtime.env || process.env;
  const options = parseReleaseArgs(argv);
  if (runtime.cwd && !options.projectDir) {
    options.projectDir = runtime.cwd;
  }
  const config = resolveReleaseConfig(options, env);
  const doctor = doctorReleaseConfig(config, env);
  if (options.command === 'registry-doctor') {
    const result = {
      command: options.command,
      passed: doctor.passed,
      errors: doctor.errors,
      config: sanitizedConfig(config)
    };
    if (options.json) {
      (runtime.stdout || process.stdout).write(`${JSON.stringify(result, null, 2)}\n`);
    } else if (result.passed) {
      (runtime.stdout || process.stdout).write('Mango release registry doctor PASS\n');
    } else {
      (runtime.stderr || process.stderr).write(`Mango release registry doctor FAIL\n${result.errors.map((error) => `- ${error}`).join('\n')}\n`);
    }
    if (!result.passed) {
      throw new Error('release registry configuration is invalid');
    }
    return result;
  }
  if (!['publish', 'status', 'verify', 'repair'].includes(options.command)) {
    throw new Error(`unknown release command: ${options.command}`);
  }
  safeVersion(options.version);
  if (options.command !== 'status' && !doctor.passed) {
    throw new Error(`release registry configuration is invalid: ${doctor.errors.join('; ')}`);
  }
  if (['publish', 'repair'].includes(options.command) && !config.authorized) {
    throw new Error('release publish/repair requires --authorize or MANGO_RELEASE_AUTHORIZED=1');
  }
  const result = runStateMachine(options.command, options.version, config, options, env);
  const output = options.json ? JSON.stringify(result, null, 2) : statusLines(result);
  (runtime.stdout || process.stdout).write(`${output}\n`);
  if (!['status'].includes(options.command) && !result.manifest.completed) {
    throw new Error(`release ${options.command} stopped before completion; inspect ${result.manifestFile}`);
  }
  return result;
}
