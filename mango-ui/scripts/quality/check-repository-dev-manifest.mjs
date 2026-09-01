#!/usr/bin/env node
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(scriptDirectory, '../../..');
const backendName = 'mango-backend';
const sourceCliScript = 'node ./packages/mango-cli/src/index.mjs';

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
  if (Object.prototype.hasOwnProperty.call(backend, 'install')) {
    failures.push(`${backendName} must not declare a legacy install command`);
  }
  return failures;
}

export function validateRepositoryCliEntry(uiPackage) {
  if (uiPackage?.scripts?.mango === sourceCliScript) {
    return [];
  }
  return [
    `mango-ui package script mango must execute ${sourceCliScript} so source development cannot fall back to PATH`,
  ];
}

export function checkRepositoryDevManifest(root = repositoryRoot) {
  const manifestPath = path.join(root, 'mango.dev.json');
  const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
  const uiPackage = JSON.parse(fs.readFileSync(path.join(root, 'mango-ui/package.json'), 'utf8'));
  const failures = [...validateRepositoryDevManifest(manifest), ...validateRepositoryCliEntry(uiPackage)];
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
