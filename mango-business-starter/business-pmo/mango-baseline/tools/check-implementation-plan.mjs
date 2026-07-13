#!/usr/bin/env node
import { runDocumentCli } from './document-contract/cli.mjs';

process.exitCode = runDocumentCli({
  contractPath: 'mango-pmo/contracts/implementation-plan.json',
  label: '实施计划检查'
});
