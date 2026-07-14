#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

import { loadContract } from './document-contract/contract-loader.mjs';
import { sha256 } from './document-contract/lifecycle.mjs';
import { parseMarkdown } from './document-contract/markdown-ast.mjs';
import { validateDocument } from './document-contract/validator.mjs';

const STAGES = [
  ['business-requirements', 'contracts/business-requirements.json'],
  ['system-requirements', 'contracts/system-requirements.json'],
  ['technical-design', 'contracts/technical-design.json'],
  ['implementation-plan', 'contracts/implementation-plan.json'],
];
const CONTRACTS = new Map(STAGES.map(([type, contractPath]) => [type, loadContract(contractPath)]));
const STAGE_INDEX = new Map(STAGES.map(([type], index) => [type, index]));
const DOCUMENT_ID_MARKER = /^(?:BRD|SRS|TDD|PLAN)-/u;
const TITLE_MARKER = /(?:业务需求说明书|系统需求规格说明书|技术设计文档|实施计划|business requirements|system requirements|technical design|implementation plan)/iu;
const LEGACY_BASELINE_FILE = '.mango-pmo-legacy-documents.json';
const SHA256_PATTERN = /^[a-f0-9]{64}$/u;

function finding(ruleId, message, file = null, line = null) {
  return {
    severity: 'FAIL',
    ruleId,
    message,
    ...(file ? { file } : {}),
    ...(line ? { line } : {}),
  };
}

function markdownFiles(root, findings) {
  const files = [];
  function walk(current) {
    for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
      const resolved = path.join(current, entry.name);
      if (entry.isSymbolicLink()) {
        findings.push(finding('LIFE-ORDER-010', `业务文档目录禁止符号链接：${resolved}`, resolved));
      } else if (entry.isDirectory()) {
        walk(resolved);
      } else if (entry.isFile() && path.extname(entry.name).toLowerCase() === '.md') {
        files.push(resolved);
      }
    }
  }
  walk(root);
  return files.sort((left, right) => left.localeCompare(right, 'en'));
}

function looksLikeLifecycleDocument(ast) {
  const documentId = String(ast.frontmatter.values.documentId ?? '');
  if (DOCUMENT_ID_MARKER.test(documentId)) return true;
  return ast.headings.some((heading) => heading.level === 1 && TITLE_MARKER.test(heading.title));
}

function loadLegacyBaseline(root, findings) {
  const baselinePath = path.join(root, LEGACY_BASELINE_FILE);
  const entries = new Map();
  if (!fs.existsSync(baselinePath)) return { baselinePath, entries };
  let baseline;
  try {
    baseline = JSON.parse(fs.readFileSync(baselinePath, 'utf8'));
  } catch (error) {
    findings.push(finding('LIFE-ORDER-010', `历史文档基线不是合法 JSON：${error.message}`, baselinePath));
    return { baselinePath, entries };
  }
  if (baseline?.schemaVersion !== 1 || !Array.isArray(baseline.documents)) {
    findings.push(finding('LIFE-ORDER-010', '历史文档基线必须使用 schemaVersion=1 和 documents 数组', baselinePath));
    return { baselinePath, entries };
  }
  for (const item of baseline.documents) {
    const relativePath = String(item?.path ?? '').replaceAll('\\', '/').trim();
    const expectedHash = String(item?.sha256 ?? '').trim().toLowerCase();
    const reason = String(item?.reason ?? '').trim();
    const resolved = path.resolve(root, relativePath);
    const insideRoot = resolved.startsWith(`${root}${path.sep}`);
    if (!relativePath || path.isAbsolute(relativePath) || !insideRoot) {
      findings.push(finding('LIFE-ORDER-010', `历史文档基线路径非法：${relativePath || '<缺失>'}`, baselinePath));
      continue;
    }
    if (!SHA256_PATTERN.test(expectedHash) || !reason) {
      findings.push(finding('LIFE-ORDER-010', `历史文档基线必须提供 sha256 和 reason：${relativePath}`, baselinePath));
      continue;
    }
    if (entries.has(resolved)) {
      findings.push(finding('LIFE-ORDER-010', `历史文档基线路径重复：${relativePath}`, baselinePath));
      continue;
    }
    entries.set(resolved, { relativePath, expectedHash, reason });
  }
  return { baselinePath, entries };
}

export function checkDocumentSet(rootPath) {
  const root = path.resolve(rootPath || 'business-docs');
  const findings = [];
  const documents = [];
  const legacyDocuments = [];
  if (!fs.existsSync(root) || !fs.statSync(root).isDirectory()) {
    return {
      root,
      documents,
      legacyDocuments,
      findings: [finding('LIFE-ORDER-010', `业务文档目录不存在或不是目录：${root}`, root)],
    };
  }

  const legacyBaseline = loadLegacyBaseline(root, findings);
  const consumedLegacyEntries = new Set();

  for (const file of markdownFiles(root, findings)) {
    const source = fs.readFileSync(file, 'utf8');
    const ast = parseMarkdown(source);
    const type = String(ast.frontmatter.values.documentType ?? '').trim();
    if (!type) {
      if (looksLikeLifecycleDocument(ast)) {
        const legacy = legacyBaseline.entries.get(path.resolve(file));
        if (!legacy) {
          findings.push(finding('LIFE-ORDER-010', '生命周期文档缺少 frontmatter documentType，不能绕过合同检查', file));
        } else if (sha256(source) !== legacy.expectedHash) {
          findings.push(finding('LIFE-HASH-020', '历史生命周期文档内容已变化，必须迁移为正式合同文档或更新经批准的基线', file));
        } else {
          consumedLegacyEntries.add(path.resolve(file));
          legacyDocuments.push({ file, sha256: legacy.expectedHash, reason: legacy.reason });
        }
      }
      continue;
    }
    const contract = CONTRACTS.get(type);
    if (!contract) {
      findings.push(finding('LIFE-ORDER-010', `不支持的生命周期 documentType：${type}`, file));
      continue;
    }
    const result = validateDocument(source, contract, { documentPath: file });
    const document = { file, source, type, contract, result, meta: result.ast.frontmatter.values };
    documents.push(document);
    for (const item of result.findings) {
      findings.push({ ...item, file });
    }
  }

  for (const [resolved, legacy] of legacyBaseline.entries) {
    if (!consumedLegacyEntries.has(resolved)) {
      findings.push(finding(
        'LIFE-ORDER-010',
        `历史文档基线项已失效，目标必须仍是缺少 documentType 的生命周期文档：${legacy.relativePath}`,
        legacyBaseline.baselinePath,
      ));
    }
  }

  const byId = new Map();
  for (const document of documents) {
    const id = String(document.meta.documentId ?? '').trim();
    if (!id) continue;
    if (byId.has(id)) {
      findings.push(finding(
        'LIFE-TRACE-030',
        `documentId 重复：${id}；首次出现于 ${byId.get(id).file}`,
        document.file,
      ));
    } else {
      byId.set(id, document);
    }
  }

  for (const document of documents) {
    const index = STAGE_INDEX.get(document.type);
    if (index === 0) continue;
    const upstreamId = String(document.meta.upstreamDocumentId ?? '').trim();
    const upstreamHash = String(document.meta.upstreamDocumentHash ?? '').trim();
    if (upstreamId === 'NONE' || upstreamHash === 'NONE') continue;
    const upstream = byId.get(upstreamId);
    if (!upstream) {
      findings.push(finding(
        'LIFE-ORDER-010',
        `${document.type} 引用的上游文档不在业务文档目录中：${upstreamId || '<缺失>'}`,
        document.file,
      ));
      continue;
    }
    const expectedType = STAGES[index - 1][0];
    if (upstream.type !== expectedType) {
      findings.push(finding(
        'LIFE-ORDER-010',
        `${document.type} 的上游必须是 ${expectedType}，实际为 ${upstream.type}`,
        document.file,
      ));
    }
    const actualHash = sha256(upstream.source);
    if (upstreamHash !== actualHash) {
      findings.push(finding(
        'LIFE-HASH-020',
        `${document.type} 的上游摘要已失效：期望 ${actualHash}，实际 ${upstreamHash || '<缺失>'}`,
        document.file,
      ));
    }
  }

  return { root, documents, legacyDocuments, findings };
}

function parseArgs(argv) {
  const args = { root: 'business-docs', json: false };
  for (let index = 0; index < argv.length; index += 1) {
    if (argv[index] === '--root') {
      args.root = argv[index + 1] ?? '';
      index += 1;
    } else if (argv[index] === '--json') {
      args.json = true;
    } else if (!argv[index].startsWith('-')) {
      args.root = argv[index];
    }
  }
  return args;
}

export function runDocumentSetCli(argv = process.argv.slice(2)) {
  const args = parseArgs(argv);
  const result = checkDocumentSet(args.root);
  if (args.json) {
    process.stdout.write(`${JSON.stringify({
      root: result.root,
      checkedDocuments: result.documents.map((document) => document.file),
      pinnedLegacyDocuments: result.legacyDocuments,
      findings: result.findings,
    }, null, 2)}\n`);
  } else {
    process.stdout.write('\n=== 业务文档集合检查 ===\n');
    process.stdout.write(`目录：${result.root}\n`);
    process.stdout.write(`生命周期文档：${result.documents.length}\n`);
    process.stdout.write(`哈希锁定的历史文档：${result.legacyDocuments.length}\n`);
    for (const item of result.findings) {
      const location = [item.file, item.line ? `line ${item.line}` : ''].filter(Boolean).join(':');
      process.stdout.write(`[FAIL] ${item.ruleId}${location ? ` (${location})` : ''} ${item.message}\n`);
    }
    process.stdout.write(result.findings.length === 0 ? '结果：PASS\n' : `结果：FAIL (${result.findings.length})\n`);
  }
  return result.findings.length === 0 ? 0 : 1;
}

if (import.meta.url === pathToFileURL(process.argv[1] ?? '').href) {
  process.exitCode = runDocumentSetCli();
}
