import fs from 'node:fs';
import path from 'node:path';
import { loadContract } from './contract-loader.mjs';
import { validateDocument } from './validator.mjs';

export function parseDocumentArgs(argv) {
  const args = { document: '', json: false };
  for (let index = 0; index < argv.length; index += 1) {
    const value = argv[index];
    if (value === '--document' || value === '--prd' || value === '--design' || value === '--plan') {
      args.document = argv[index + 1] ?? '';
      index += 1;
    } else if (value === '--json') {
      args.json = true;
    } else if (!value.startsWith('-') && !args.document) {
      args.document = value;
    }
  }
  return args;
}

export function checkDocumentFile(documentPath, contractPath, options = {}) {
  const contract = typeof contractPath === 'string' ? loadContract(contractPath) : contractPath;
  if (!documentPath) {
    return {
      contract,
      result: null,
      findings: [{ severity: 'FAIL', ruleId: contract.metadata.ruleId, message: '缺少 --document <path>' }]
    };
  }
  const resolved = path.resolve(documentPath);
  if (!fs.existsSync(resolved)) {
    return {
      contract,
      result: null,
      findings: [{ severity: 'FAIL', ruleId: contract.metadata.ruleId, message: `文档不存在：${resolved}` }]
    };
  }
  const source = fs.readFileSync(resolved, 'utf8');
  const result = validateDocument(source, contract, { ...options, documentPath: resolved });
  return { contract, result, findings: result.findings, source, resolved };
}

export function printFindings(label, checked, json = false) {
  if (json) {
    process.stdout.write(`${JSON.stringify({ documentType: checked.contract.documentType, file: checked.resolved ?? null, findings: checked.findings }, null, 2)}\n`);
    return;
  }
  process.stdout.write(`\n=== ${label} ===\n`);
  if (checked.resolved) process.stdout.write(`文件：${checked.resolved}\n`);
  if (checked.findings.length === 0) {
    process.stdout.write('结果：PASS\n');
    return;
  }
  for (const finding of checked.findings) {
    const location = finding.line ? ` (line ${finding.line})` : '';
    process.stdout.write(`[FAIL] ${finding.ruleId}${location} ${finding.message}\n`);
  }
  process.stdout.write(`结果：FAIL (${checked.findings.length})\n`);
}

export function runDocumentCli({ argv = process.argv.slice(2), contractPath, label }) {
  const args = parseDocumentArgs(argv);
  const checked = checkDocumentFile(args.document, contractPath);
  printFindings(label, checked, args.json);
  return checked.findings.length === 0 ? 0 : 1;
}
