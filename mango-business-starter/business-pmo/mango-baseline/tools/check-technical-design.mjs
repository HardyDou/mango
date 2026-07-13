#!/usr/bin/env node
import { runDocumentCli } from './document-contract/cli.mjs';

process.exitCode = runDocumentCli({
  contractPath: 'mango-pmo/contracts/technical-design.json',
  label: '技术设计文档检查'
});
