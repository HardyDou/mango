#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

import { loadContract } from './document-contract/contract-loader.mjs';
import { sha256 } from './document-contract/lifecycle.mjs';
import { parseMarkdown } from './document-contract/markdown-ast.mjs';

const LEGACY_BASELINE_FILE = '.mango-pmo-legacy-documents.json';
const STAGES = [
  ['business-requirements', 'contracts/business-requirements.json'],
  ['system-requirements', 'contracts/system-requirements.json'],
  ['technical-design', 'contracts/technical-design.json'],
  ['implementation-plan', 'contracts/implementation-plan.json'],
];
const CONTRACTS = new Map(STAGES.map(([type, contractPath]) => [type, loadContract(contractPath)]));
const SHA256_PATTERN = /^[a-f0-9]{64}$/u;

function readBaseline(baselinePath) {
  if (!fs.existsSync(baselinePath)) return { schemaVersion: 1, documents: [] };
  const baseline = JSON.parse(fs.readFileSync(baselinePath, 'utf8'));
  if (baseline?.schemaVersion !== 1 || !Array.isArray(baseline.documents)) {
    throw new Error('历史文档基线必须使用 schemaVersion=1 和 documents 数组');
  }
  return baseline;
}

function walkMarkdownFiles(root) {
  const files = [];
  function walk(current) {
    for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
      const resolved = path.join(current, entry.name);
      if (entry.isSymbolicLink()) throw new Error(`业务文档目录禁止符号链接：${resolved}`);
      if (entry.isDirectory()) walk(resolved);
      else if (entry.isFile() && path.extname(entry.name).toLowerCase() === '.md') files.push(resolved);
    }
  }
  if (fs.existsSync(root)) walk(root);
  return files.sort((left, right) => left.localeCompare(right, 'en'));
}

function normalizeExistingEntries(root, documents) {
  const entries = new Map();
  for (const item of documents) {
    const relativePath = String(item?.path ?? '').replaceAll('\\', '/').trim();
    const resolved = path.resolve(root, relativePath);
    const insideRoot = resolved.startsWith(`${root}${path.sep}`);
    if (!relativePath || path.isAbsolute(relativePath) || !insideRoot) {
      throw new Error(`历史文档基线路径非法：${relativePath || '<缺失>'}`);
    }
    if (!SHA256_PATTERN.test(String(item?.sha256 ?? '')) || !String(item?.reason ?? '').trim()) {
      throw new Error(`历史文档基线必须提供 sha256 和 reason：${relativePath}`);
    }
    if (entries.has(resolved)) throw new Error(`历史文档基线路径重复：${relativePath}`);
    entries.set(resolved, item);
  }
  return entries;
}

export function pinHistoricalPmoVersionDocuments(rootPath, { write = true } = {}) {
  const root = path.resolve(rootPath || 'business-docs');
  if (!fs.existsSync(root)) return { root, added: [], baselinePath: path.join(root, LEGACY_BASELINE_FILE) };
  if (!fs.statSync(root).isDirectory()) throw new Error(`业务文档目录不是目录：${root}`);

  const baselinePath = path.join(root, LEGACY_BASELINE_FILE);
  const baseline = readBaseline(baselinePath);
  const entries = normalizeExistingEntries(root, baseline.documents);
  const added = [];

  for (const file of walkMarkdownFiles(root)) {
    const source = fs.readFileSync(file, 'utf8');
    const ast = parseMarkdown(source);
    const contract = CONTRACTS.get(String(ast.frontmatter.values.documentType ?? '').trim());
    if (!contract) continue;
    const pmoVersion = String(ast.frontmatter.values.pmoVersion ?? '').trim();
    if (contract.metadata.historicalPmoVersions?.includes(pmoVersion) !== true) continue;

    const existing = entries.get(file);
    if (existing) {
      if (existing.pmoVersion !== pmoVersion || String(existing.sha256).toLowerCase() !== sha256(source)) {
        throw new Error(`已有历史文档基线与当前版本或内容不一致：${path.relative(root, file)}`);
      }
      continue;
    }
    const relativePath = path.relative(root, file).split(path.sep).join('/');
    const entry = {
      path: relativePath,
      sha256: sha256(source),
      pmoVersion,
      reason: `PMO ${pmoVersion} 升级前形成，保留原文审批和上游摘要`,
    };
    baseline.documents.push(entry);
    entries.set(file, entry);
    added.push(entry);
  }

  if (write && added.length > 0) {
    baseline.documents.sort((left, right) => String(left.path).localeCompare(String(right.path), 'en'));
    fs.writeFileSync(baselinePath, `${JSON.stringify(baseline, null, 2)}\n`);
  }
  return { root, added, baselinePath };
}

function parseArgs(argv) {
  const args = { root: 'business-docs', dryRun: false, json: false };
  for (let index = 0; index < argv.length; index += 1) {
    const value = argv[index];
    if (value === '--root') {
      args.root = argv[index + 1] ?? '';
      index += 1;
    } else if (value === '--dry-run') args.dryRun = true;
    else if (value === '--json') args.json = true;
    else if (!value.startsWith('-')) args.root = value;
  }
  return args;
}

export function runHistoricalPmoVersionPinCli(argv = process.argv.slice(2)) {
  const args = parseArgs(argv);
  const result = pinHistoricalPmoVersionDocuments(args.root, { write: !args.dryRun });
  if (args.json) {
    process.stdout.write(`${JSON.stringify(result, null, 2)}\n`);
  } else {
    process.stdout.write(`${args.dryRun ? '计划' : '已'}登记 ${result.added.length} 份历史 PMO 版本文档：${result.root}\n`);
    for (const entry of result.added) process.stdout.write(`- ${entry.path} (${entry.pmoVersion})\n`);
  }
  return 0;
}

if (import.meta.url === pathToFileURL(process.argv[1] ?? '').href) {
  try {
    process.exitCode = runHistoricalPmoVersionPinCli();
  } catch (error) {
    process.stderr.write(`[FAIL] ${error.message}\n`);
    process.exitCode = 1;
  }
}
