#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const toolDir = path.dirname(fileURLToPath(import.meta.url));
const contract = JSON.parse(fs.readFileSync(path.resolve(toolDir, '../contracts/lean-documents.json'), 'utf8'));

function parseArgs(argv) {
  const args = { document: '', template: false, json: false };
  for (let index = 0; index < argv.length; index += 1) {
    const value = argv[index];
    if (value === '--document') args.document = argv[++index] ?? '';
    else if (value === '--template') args.template = true;
    else if (value === '--json') args.json = true;
    else if (!value.startsWith('-') && !args.document) args.document = value;
  }
  return args;
}

function parseFrontmatter(source) {
  const match = /^---\r?\n([\s\S]*?)\r?\n---\r?\n/u.exec(source);
  if (!match) return { values: {}, body: source };
  const values = {};
  for (const line of match[1].split(/\r?\n/u)) {
    const pair = /^([A-Za-z][A-Za-z0-9_-]*):\s*(.*?)\s*$/u.exec(line);
    if (pair) values[pair[1]] = pair[2];
  }
  return { values, body: source.slice(match[0].length) };
}

function contentUnits(body) {
  const plain = body
    .replace(/^```.*$/gmu, '')
    .replace(/^#{1,6}\s+.*$/gmu, '')
    .replace(/[`*_>|\-]/gu, ' ');
  const chinese = (plain.match(/[\u3400-\u9fff]/gu) ?? []).length;
  const asciiWords = (plain.match(/[A-Za-z0-9_./:-]+/gu) ?? []).length;
  return chinese + Math.ceil(asciiWords * 0.6);
}

function headings(body) {
  return [...body.matchAll(/^##\s+(.+?)\s*$/gmu)].map(match => match[1]);
}

function hasVersionedReference(body, label) {
  const line = new RegExp(`^- ${label}：(.+)$`, 'gmu');
  return [...body.matchAll(line)].some(match => {
    const value = match[1].replace(/`/gu, '');
    return /@[A-Za-z0-9][A-Za-z0-9._:-]*\d[A-Za-z0-9._:-]*/u.test(value)
      || /(?:版本|提交|commit|SHA)\s*[:：=]?\s*[A-Za-z0-9][A-Za-z0-9._:-]*\d[A-Za-z0-9._:-]*/iu.test(value);
  });
}

function hasReferenceLine(body, label) {
  return new RegExp(`^- ${label}：.+$`, 'mu').test(body);
}

function validateDirectTrace(documentType, body, findings) {
  const ownedDefinitions = {
    'delivery-l2': ['SR', 'TD', 'VAL'],
    'delivery-l3': ['US', 'SR', 'TD', 'TASK', 'VAL'],
    'delivery-l4': ['US', 'SR', 'TD', 'TASK', 'VAL'],
    'business-requirements': ['US'],
    'system-requirements': ['SR', 'TD', 'VAL'],
    'technical-design': ['TD', 'VAL'],
    'implementation-plan': ['TASK', 'VAL']
  };
  const required = new Set(ownedDefinitions[documentType] ?? []);
  const rules = [
    { id: 'US', pattern: /^\d+\.\s+US-\d{3}\s*->\s*BR-\d{3}\s*[：:]\s*\S.+$/u },
    { id: 'SR', pattern: /^\d+\.\s+SR-\d{3}\s*->\s*(?:US|BR)-\d{3}\s*[：:]\s*\S.+$/u },
    { id: 'TD', pattern: /^\d+\.\s+TD-\d{3}\s*->\s*SR-\d{3}\s*[：:]\s*\S.+$/u },
    { id: 'TASK', pattern: /^\d+\.\s+TASK-\d{3}\s*->\s*TD-\d{3}\s*[：:]\s*\S.+$/u },
    { id: 'VAL', pattern: /^\d+\.\s+VAL-\d{3}\s*->\s*(?:SR|TASK)-\d{3}\s*[：:]\s*\S.+$/u }
  ];
  for (const rule of rules) {
    if (!required.has(rule.id)) continue;
    const definitions = [...body.matchAll(new RegExp(`^\\d+\\.\\s+${rule.id}-\\d{3}.*$`, 'gmu'))]
      .map(match => match[0]);
    if (definitions.length === 0) {
      findings.push(`缺少直接追踪格式：${rule.id}`);
      continue;
    }
    const ids = new Set();
    for (const definition of definitions) {
      const id = new RegExp(`${rule.id}-\\d{3}`, 'u').exec(definition)?.[0];
      if (id && ids.has(id)) findings.push(`直接追踪 ID 重复：${id}`);
      if (id) ids.add(id);
      if (!rule.pattern.test(definition)) findings.push(`直接追踪格式错误：${definition}`);
    }
  }
}

export function validateLeanDocument(source, options = {}) {
  const findings = [];
  const parsed = parseFrontmatter(source);
  const documentType = parsed.values.documentType;
  const spec = contract.documents[documentType];
  if (!spec) return { findings: [`未知 documentType：${documentType || '<缺失>'}`] };
  if (parsed.values.deliveryLevel !== spec.deliveryLevel) findings.push(`deliveryLevel 必须为 ${spec.deliveryLevel}`);

  const actualHeadings = headings(parsed.body);
  if (actualHeadings.join('\u001f') !== spec.headings.join('\u001f')) {
    findings.push(`H2 必须依次为：${spec.headings.join(' / ')}`);
  }
  for (const pattern of spec.requiredPatterns) {
    if (!new RegExp(pattern, 'mu').test(parsed.body)) findings.push(`缺少必需内容：${pattern}`);
  }

  if (!options.template && ['delivery-l2', 'delivery-l3', 'delivery-l4', 'business-requirements'].includes(documentType)) {
    const stories = [...parsed.body.matchAll(/^\d+\.\s+US-\d{3}.+$/gmu)];
    if (stories.length === 0) findings.push('用户故事必须使用“序号 + US-xxx”且一项一行');
  }

  if (contract.referencePolicy.requiredDocumentTypes.includes(documentType)) {
    if (options.template) {
      if (!hasReferenceLine(parsed.body, '规范')) findings.push('模板必须提供规范引用行');
      if (!hasReferenceLine(parsed.body, '代码')) findings.push('模板必须提供代码引用行');
    } else {
      if (!hasVersionedReference(parsed.body, '规范')) findings.push('必须引用带精确版本的实际采用规范');
      if (!hasVersionedReference(parsed.body, '代码')) findings.push('必须引用带精确提交或版本的代码示例');
    }
  }

  if (!options.template) {
    for (const filler of contract.forbiddenFiller) {
      if (parsed.body.includes(filler)) findings.push(`包含禁止空话：${filler}`);
    }
    validateDirectTrace(documentType, parsed.body, findings);
  }

  if (spec.pageBudget) {
    if (Number(parsed.values.pageBudget) !== spec.pageBudget) findings.push(`pageBudget 必须为 ${spec.pageBudget}`);
    const units = contentUnits(parsed.body);
    const limit = contract.a4EquivalentContentUnits * spec.pageBudget;
    if (!options.template && units > limit) findings.push(`超过 ${spec.pageBudget} 张 A4 等效内容：${units}/${limit}`);
  }

  if (!options.template && /\{\{[^}]+\}\}/u.test(source)) findings.push('存在未替换模板占位符');
  return { findings, documentType, contentUnits: contentUnits(parsed.body) };
}

function main(argv) {
  const args = parseArgs(argv);
  if (!args.document) throw new Error('usage: check-lean-document.mjs --document <path> [--template] [--json]');
  const source = fs.readFileSync(path.resolve(args.document), 'utf8');
  const result = validateLeanDocument(source, { template: args.template });
  if (args.json) process.stdout.write(`${JSON.stringify(result, null, 2)}\n`);
  else if (result.findings.length === 0) process.stdout.write(`Lean document PASS: ${args.document}; contentUnits=${result.contentUnits}\n`);
  else process.stderr.write(`${result.findings.map(item => `[FAIL] ${item}`).join('\n')}\n`);
  process.exitCode = result.findings.length === 0 ? 0 : 1;
}

if (process.argv[1]?.endsWith('check-lean-document.mjs')) main(process.argv.slice(2));
