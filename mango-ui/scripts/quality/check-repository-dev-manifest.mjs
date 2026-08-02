#!/usr/bin/env node
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(scriptDirectory, '../../..');
const backendName = 'mango-backend';
const backendModule = 'mango-app/monolith/mango-monolith-app';
const requiredInstallSelectors = [':mango-bom', backendModule];

function projectSelectors(args) {
  const projectListIndex = args.indexOf('-pl');
  if (projectListIndex < 0 || typeof args[projectListIndex + 1] !== 'string') {
    return [];
  }
  return args[projectListIndex + 1]
    .split(',')
    .map((selector) => selector.trim())
    .filter(Boolean);
}

export function validateRepositoryDevManifest(manifest) {
  const failures = [];
  const backend = manifest?.apps?.[backendName];
  if (!backend || backend.type !== 'spring-boot-maven') {
    return [`${backendName} must be a spring-boot-maven app`];
  }
  if (backend.processMode !== 'runtime') {
    failures.push(`${backendName} must explicitly declare processMode=runtime`);
  }
  const positionalModes = (backend.args || []).filter((arg) => ['bootstrap', 'runtime'].includes(arg));
  if (positionalModes.length > 0) {
    failures.push(`${backendName} args must not declare a process mode: ${positionalModes.join(', ')}`);
  }
  if (backend.install?.command !== 'mvn' || !Array.isArray(backend.install.args)) {
    failures.push(`${backendName} must declare a Maven install command`);
    return failures;
  }
  const selectors = projectSelectors(backend.install.args);
  for (const selector of requiredInstallSelectors) {
    if (!selectors.includes(selector)) {
      failures.push(`${backendName} install -pl must include ${selector}`);
    }
  }
  if (!backend.install.args.includes('-am')) {
    failures.push(`${backendName} install must include -am`);
  }
  if (!backend.install.args.includes('install')) {
    failures.push(`${backendName} install command must execute the Maven install phase`);
  }
  return failures;
}

export function checkRepositoryDevManifest(root = repositoryRoot) {
  const manifestPath = path.join(root, 'mango.dev.json');
  const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
  const failures = validateRepositoryDevManifest(manifest);
  if (failures.length > 0) {
    throw new Error(`Repository development manifest check failed:\n- ${failures.join('\n- ')}`);
  }
  execFileSync(process.execPath, [path.join(root, 'mango-ui/packages/mango-cli/src/index.mjs'), 'plan', backendName], {
    cwd: root,
    stdio: 'inherit',
  });
  process.stdout.write('Repository development manifest check passed.\n');
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  checkRepositoryDevManifest();
}
