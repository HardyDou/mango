#!/usr/bin/env node
import { spawnSync } from 'node:child_process';

const pnpmCommand = process.platform === 'win32' ? 'pnpm.cmd' : 'pnpm';

const packages = [
  '@mango/api-schema',
  '@mango/http-client',
  '@mango/common',
  '@mango/admin-extension',
  '@mango/auth',
  '@mango/app-runtime',
  '@mango/rbac',
  '@mango/grid-layout',
  '@mango/grid-widgets',
  '@mango/system',
  '@mango/admin-pages',
  '@mango/home',
  '@mango/job',
  '@mango/file',
  '@mango/notice',
  '@mango/workflow',
  '@mango/admin-shell',
  '@mango/cms',
  '@mango/link-openapi',
  '@mango/link',
  '@mango/template',
  '@mango/numgen',
  '@mango/calendar',
  '@mango/payment',
  '@mango/ai-api',
  '@mango/ai',
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
