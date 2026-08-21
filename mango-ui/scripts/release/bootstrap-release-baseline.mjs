#!/usr/bin/env node
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { buildBootstrapBaseline } from './release-manifest-lib.mjs';

const args = process.argv.slice(2);
const catalogPath = value('--catalog', 'mango-catalog/catalog.lock.json');
const evidencePath = value('--evidence', '');
const outputPath = value('--output', 'mango-ui/.changeset/release-baseline.bootstrap.json');
const sourceCommit = value('--source-commit', '');
const sourceTree = value('--source-tree', '');
if (!evidencePath) throw new Error('bootstrap requires --evidence; registry/history evidence cannot be inferred');
if (!/^[0-9a-f]{40}$/u.test(sourceCommit) || !/^[0-9a-f]{40}$/u.test(sourceTree)) {
  throw new Error('bootstrap requires valid --source-commit and --source-tree');
}
const catalog = readJson(catalogPath);
const evidence = readJson(evidencePath);
const baseline = buildBootstrapBaseline({ catalog, evidence, source: { commit: sourceCommit, tree: sourceTree } });
writeFileSync(resolve(outputPath), `${JSON.stringify(baseline, null, 2)}\n`);
console.log(`bootstrap baseline written: ${resolve(outputPath)}`);

function value(name, fallback) {
  const inline = args.find((arg) => arg.startsWith(`${name}=`));
  if (inline) return inline.slice(name.length + 1);
  const index = args.indexOf(name);
  return index >= 0 ? args[index + 1] || fallback : fallback;
}

function readJson(file) {
  const path = resolve(file);
  if (!existsSync(path)) throw new Error(`missing JSON input: ${path}`);
  return JSON.parse(readFileSync(path, 'utf8'));
}
