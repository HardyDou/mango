import { fileURLToPath, URL } from 'node:url';
import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vitest/config';

const fromPackage = (path: string) => fileURLToPath(new URL(`../${path}`, import.meta.url));

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'node',
  },
  resolve: {
    alias: [
      { find: /^@mango\/common\/(.+)$/, replacement: fromPackage('common/$1') },
      { find: /^@mango\/auth-api\/(.+)$/, replacement: fromPackage('auth-api/src/$1') },
      { find: /^@mango\/rbac-api\/(.+)$/, replacement: fromPackage('rbac-api/src/$1') },
      { find: '@mango/admin-pages/defaults', replacement: fromPackage('admin-pages/src/defaults.ts') },
      { find: '@mango/admin-pages/core', replacement: fromPackage('admin-pages/src/core.ts') },
      { find: '@mango/admin-pages', replacement: fromPackage('admin-pages/src/index.ts') },
      { find: '@mango/app-runtime', replacement: fromPackage('app-runtime/src/index.ts') },
      { find: '@mango/auth-api', replacement: fromPackage('auth-api/src/index.ts') },
      { find: '@mango/auth', replacement: fromPackage('auth/src/index.ts') },
      { find: '@mango/common', replacement: fromPackage('common/index.ts') },
      { find: '@mango/rbac-api', replacement: fromPackage('rbac-api/src/index.ts') },
      { find: '@mango/rbac', replacement: fromPackage('rbac/src/index.ts') },
      { find: '@mango/auth-admin/capability', replacement: fromPackage('auth-admin/src/capability.ts') },
      { find: '@mango/calendar-admin/capability', replacement: fromPackage('calendar-admin/src/capability.ts') },
      { find: '@mango/file-admin/capability', replacement: fromPackage('file-admin/src/capability.ts') },
      { find: '@mango/notice-admin/capability', replacement: fromPackage('notice-admin/src/capability.ts') },
      { find: '@mango/numgen-admin/capability', replacement: fromPackage('numgen-admin/src/capability.ts') },
      { find: '@mango/rbac-admin/capability', replacement: fromPackage('rbac-admin/src/capability.ts') },
      { find: '@mango/system-admin/capability', replacement: fromPackage('system-admin/src/capability.ts') },
      { find: '@mango/template-admin/capability', replacement: fromPackage('template-admin/src/capability.ts') },
      { find: '@mango/workflow-admin/capability', replacement: fromPackage('workflow-admin/src/capability.ts') },
    ],
  },
});
