#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';

import { checkDocumentFile, parseDocumentArgs, printFindings } from './document-contract/cli.mjs';
import { parseMarkdown } from './document-contract/markdown-ast.mjs';

const args = parseDocumentArgs(process.argv.slice(2));
if (!args.document) {
  process.stderr.write('[FAIL] LEGACY-PRD-MIGRATION-001 缺少 --prd <path>\n');
  process.exitCode = 1;
} else {
  const resolved = path.resolve(args.document);
  if (!fs.existsSync(resolved)) {
    process.stderr.write(`[FAIL] LEGACY-PRD-MIGRATION-001 文档不存在：${resolved}\n`);
    process.exitCode = 1;
  } else {
    const source = fs.readFileSync(resolved, 'utf8');
    const documentType = parseMarkdown(source).frontmatter.values.documentType;
    const contracts = {
      'business-requirements': 'mango-pmo/contracts/business-requirements.json',
      'system-requirements': 'mango-pmo/contracts/system-requirements.json'
    };
    const contractPath = contracts[documentType];
    if (!contractPath) {
      process.stderr.write('[FAIL] LEGACY-PRD-MIGRATION-001 混合 PRD 已废弃；请拆分为 business-requirements 或 system-requirements\n');
      process.exitCode = 1;
    } else {
      const checked = checkDocumentFile(resolved, contractPath);
      printFindings(`旧 check-prd 兼容转发 (${documentType})`, checked, args.json);
      process.exitCode = checked.findings.length === 0 ? 0 : 1;
    }
  }
}
