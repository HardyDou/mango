import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vitest/config';
import { resolve } from 'path';

const repoRoot = resolve(__dirname, '../..');
const adminNodeModules = resolve(repoRoot, 'apps/mango-admin/node_modules');

export default defineConfig({
  plugins: [vue()],
  test: {
    globals: true,
    environment: 'happy-dom',
  },
  resolve: {
    alias: {
      '@mango/workflow': resolve(repoRoot, 'packages/workflow'),
      '@mango/common': resolve(repoRoot, 'packages/common'),
      '@mango/file': resolve(repoRoot, 'packages/file/src/index.ts'),
      '@mango/rbac': resolve(repoRoot, 'packages/rbac'),
      '@mango/system': resolve(repoRoot, 'packages/system'),
      '@mango/api-schema': resolve(repoRoot, 'packages/api-schema'),
      '@mango/admin-pages/core': resolve(repoRoot, 'packages/admin-pages/src/core.ts'),
      vue: resolve(adminNodeModules, 'vue'),
      'vue-router': resolve(adminNodeModules, 'vue-router'),
      'element-plus': resolve(adminNodeModules, 'element-plus'),
      '@element-plus/icons-vue': resolve(adminNodeModules, '@element-plus/icons-vue'),
    },
  },
});
