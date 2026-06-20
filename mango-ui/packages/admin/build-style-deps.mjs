#!/usr/bin/env node
import { spawnSync } from 'node:child_process';

const packages = [
  '@mango/admin-pages',
  '@mango/admin-shell',
  '@mango/app-runtime',
  '@mango/auth',
  '@mango/common',
  '@mango/grid-layout',
  '@mango/job',
  '@mango/rbac',
  '@mango/system',
  '@mango/file',
  '@mango/template',
  '@mango/notice',
  '@mango/numgen',
  '@mango/calendar',
  '@mango/payment',
  '@mango/workflow',
  '@mango/workflow-business-example',
];

for (const packageName of packages) {
  const result = spawnSync('pnpm', ['--dir', '../..', '-F', packageName, 'build'], {
    stdio: 'inherit',
    shell: process.platform === 'win32',
  });

  if (result.status !== 0) {
    process.exit(result.status || 1);
  }
}

// Generated from packages/admin/admin-modules.json.
