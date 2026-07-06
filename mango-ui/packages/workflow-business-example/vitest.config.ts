import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vitest/config';
import { dirname, resolve } from 'path';
import { fileURLToPath } from 'url';

const configDir = dirname(fileURLToPath(import.meta.url));
const repoRoot = resolve(configDir, '../..');
const adminNodeModules = resolve(repoRoot, 'apps/mango-admin/node_modules');

export default defineConfig({
  plugins: [vue()],
  test: {
    globals: true,
    environment: 'happy-dom',
  },
  resolve: {
    alias: [
      { find: /^@mango\/workflow$/, replacement: resolve(repoRoot, 'packages/workflow/src/index.ts') },
      { find: /^@mango\/common$/, replacement: resolve(repoRoot, 'packages/common/index.ts') },
      { find: /^@mango\/common\/(.*)$/, replacement: `${resolve(repoRoot, 'packages/common')}/$1` },
      { find: /^@mango\/file$/, replacement: resolve(repoRoot, 'packages/file/src/index.ts') },
      { find: /^@mango\/rbac$/, replacement: resolve(repoRoot, 'packages/rbac/src/index.ts') },
      { find: /^@mango\/system$/, replacement: resolve(repoRoot, 'packages/system/src/index.ts') },
      { find: /^@mango\/api-schema$/, replacement: resolve(repoRoot, 'packages/api-schema/src/index.ts') },
      { find: /^@mango\/admin-pages\/core$/, replacement: resolve(repoRoot, 'packages/admin-pages/src/core.ts') },
      { find: /^vue$/, replacement: resolve(adminNodeModules, 'vue') },
      { find: /^vue-router$/, replacement: resolve(adminNodeModules, 'vue-router') },
      { find: /^element-plus$/, replacement: resolve(adminNodeModules, 'element-plus') },
      { find: /^@element-plus\/icons-vue$/, replacement: resolve(adminNodeModules, '@element-plus/icons-vue') },
    ],
  },
});
