import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';
import { defineConfig } from 'vitest/config';

const adminNodeModules = resolve(__dirname, 'apps/mango-admin/node_modules');
const workspaceNodeModules = resolve(__dirname, 'node_modules/.pnpm/node_modules');

export default defineConfig({
  plugins: [vue()],
  test: {
    globals: true,
    environment: 'happy-dom',
  },
  resolve: {
    alias: {
      '@mango/workflow': resolve(__dirname, 'packages/workflow'),
      '@mango/common': resolve(__dirname, 'packages/common'),
      '@mango/file': resolve(__dirname, 'packages/file/src/index.ts'),
      '@mango/system': resolve(__dirname, 'packages/system/src/index.ts'),
      '@mango/rbac': resolve(__dirname, 'packages/rbac/src/index.ts'),
      '@mango/app-runtime': resolve(__dirname, 'packages/app-runtime/src/index.ts'),
      '@mango/api-schema': resolve(__dirname, 'packages/api-schema'),
      '@mango/admin-pages/core': resolve(__dirname, 'packages/admin-pages/src/core.ts'),
      vitest: resolve(workspaceNodeModules, 'vitest/dist/index.js'),
      vue: resolve(adminNodeModules, 'vue'),
      'vue-router': resolve(adminNodeModules, 'vue-router'),
      'element-plus': resolve(adminNodeModules, 'element-plus'),
      '@element-plus/icons-vue': resolve(adminNodeModules, '@element-plus/icons-vue'),
    },
  },
});
