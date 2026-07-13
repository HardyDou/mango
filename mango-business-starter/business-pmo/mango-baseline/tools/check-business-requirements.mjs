#!/usr/bin/env node
import { runDocumentCli } from './document-contract/cli.mjs';

process.exitCode = runDocumentCli({
  contractPath: 'mango-pmo/contracts/business-requirements.json',
  label: '业务需求说明书检查'
});
