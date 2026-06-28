#!/usr/bin/env node
import { spawnSync } from 'node:child_process';

const pnpmCommand = process.platform === 'win32' ? 'pnpm.cmd' : 'pnpm';

const packages = [
  '@mango/common',
  '@mango/auth',
  '@mango/app-runtime',
  '@mango/rbac',
  '@mango/system',
  '@mango/admin-pages',
  '@mango/grid-layout',
  '@mango/calendar',
  '@mango/notice',
  '@mango/file',
  '@mango/workflow',
  '@mango/grid-widgets',
  '@mango/admin-shell',
  '@mango/cms',
  '@mango/job',
  '@mango/template',
  '@mango/numgen',
  '@mango/payment',
  '@mango/workflow-business-example',
];

for (const packageName of packages) {
  const result = spawnSync(pnpmCommand, ['--dir', '../..', '-F', packageName, 'build'], {
    stdio: 'inherit',
    shell: process.platform === 'win32',
  });

  if (result.status !== 0) {
    process.exit(result.status || 1);
  }
}

// Generated from packages/admin/admin-modules.json.
