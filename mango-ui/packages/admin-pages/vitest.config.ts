import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vitest/config';

const fromPackage = (path: string) => fileURLToPath(new URL(`../${path}`, import.meta.url));

export default defineConfig({
  test: {
    environment: 'node',
  },
  resolve: {
    alias: {
      '@mango/auth-admin/capability': fromPackage('auth-admin/src/capability.ts'),
      '@mango/calendar-admin/capability': fromPackage('calendar-admin/src/capability.ts'),
      '@mango/file-admin/capability': fromPackage('file-admin/src/capability.ts'),
      '@mango/notice-admin/capability': fromPackage('notice-admin/src/capability.ts'),
      '@mango/numgen-admin/capability': fromPackage('numgen-admin/src/capability.ts'),
      '@mango/rbac-admin/capability': fromPackage('rbac-admin/src/capability.ts'),
      '@mango/system-admin/capability': fromPackage('system-admin/src/capability.ts'),
      '@mango/template-admin/capability': fromPackage('template-admin/src/capability.ts'),
      '@mango/workflow-admin/capability': fromPackage('workflow-admin/src/capability.ts'),
      '@mango/auth/capability': fromPackage('auth/src/capability.ts'),
      '@mango/calendar/capability': fromPackage('calendar/src/capability.ts'),
      '@mango/file/capability': fromPackage('file/src/capability.ts'),
      '@mango/notice/capability': fromPackage('notice/src/capability.ts'),
      '@mango/numgen/capability': fromPackage('numgen/src/capability.ts'),
      '@mango/rbac/capability': fromPackage('rbac/src/capability.ts'),
      '@mango/system/capability': fromPackage('system/src/capability.ts'),
      '@mango/template/capability': fromPackage('template/src/capability.ts'),
      '@mango/workflow/capability': fromPackage('workflow/src/capability.ts'),
    },
  },
});
