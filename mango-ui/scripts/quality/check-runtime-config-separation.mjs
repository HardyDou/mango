#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { validateRuntimeConfigSeparation } from './runtime-config-separation-lib.mjs';

const uiRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const appRoot = path.join(uiRoot, 'apps/mango-admin-shell');
const deployPath = path.join(appRoot, 'public/runtime-config.json');
const developmentPath = path.join(appRoot, 'runtime-config.dev.json');
const distRoot = path.join(appRoot, 'dist');
const distSources = fs.existsSync(distRoot) ? readDistSources(distRoot) : [];
const failures = validateRuntimeConfigSeparation(readJson(deployPath), readJson(developmentPath), distSources);

if (failures.length > 0) {
  process.stderr.write(`runtime config separation FAIL\n${failures.map((failure) => `- ${failure}`).join('\n')}\n`);
  process.exit(1);
}
process.stdout.write(`runtime config separation PASS distFiles=${distSources.length}\n`);

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

function readDistSources(directory) {
  const sources = [];
  for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
    const absolute = path.join(directory, entry.name);
    if (entry.isDirectory()) sources.push(...readDistSources(absolute));
    else if (entry.isFile() && /\.(?:html|js|json)$/u.test(entry.name)) {
      sources.push({
        path: path.relative(distRoot, absolute).split(path.sep).join('/'),
        content: fs.readFileSync(absolute, 'utf8'),
      });
    }
  }
  return sources;
}
