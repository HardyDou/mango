#!/usr/bin/env node
import { resolve } from 'node:path';
import { verifyPmoPackageRoot } from './pmo-package-verifier-lib.mjs';

const packageRoot = process.argv[2];
if (!packageRoot || process.argv.length !== 3) {
  throw new Error('usage: verify-pmo-package-root.mjs <extracted-package-root>');
}
const result = verifyPmoPackageRoot(resolve(packageRoot));
console.log(`Verified ${result.packageName}@${result.packageVersion} package root`);
