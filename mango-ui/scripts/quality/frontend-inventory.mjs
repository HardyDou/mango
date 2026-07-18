#!/usr/bin/env node
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { createFrontendInventory, writeFrontendInventory } from './frontend-inventory-lib.mjs';

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const defaultUiRoot = path.resolve(scriptDirectory, '../..');

function option(name, fallback) {
  const prefix = `--${name}=`;
  const argument = process.argv.slice(2).find(value => value.startsWith(prefix));
  return argument ? argument.slice(prefix.length) : fallback;
}

const uiRoot = path.resolve(option('root', defaultUiRoot));
const outputFile = path.resolve(option('out', path.join(uiRoot, '../.runtime/frontend-quality/inventory.json')));

try {
  const report = createFrontendInventory(uiRoot);
  writeFrontendInventory(report, outputFile);
  process.stdout.write(`${JSON.stringify({ outputFile, inventorySha256: report.inventorySha256, ...report.summary })}\n`);
} catch (error) {
  process.stderr.write(`Frontend inventory failed: ${error.message}\n`);
  process.exitCode = 1;
}
