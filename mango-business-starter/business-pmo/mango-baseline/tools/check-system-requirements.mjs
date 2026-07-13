#!/usr/bin/env node
import { runDocumentCli } from './document-contract/cli.mjs';

process.exitCode = runDocumentCli({
  contractPath: 'mango-pmo/contracts/system-requirements.json',
  label: '系统需求规格说明书检查'
});
