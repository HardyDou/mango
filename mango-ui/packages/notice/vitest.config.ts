import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';
import { defineConfig } from 'vitest/config';

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
      '@mango/common': resolve(repoRoot, 'packages/common'),
      '@mango/notice': resolve(repoRoot, 'packages/notice/src'),
      '@vue/test-utils': resolve(adminNodeModules, '@vue/test-utils'),
      vitest: resolve(adminNodeModules, 'vitest'),
      vue: resolve(adminNodeModules, 'vue'),
      'element-plus': resolve(adminNodeModules, 'element-plus'),
      '@element-plus/icons-vue': resolve(adminNodeModules, '@element-plus/icons-vue'),
    },
  },
});
