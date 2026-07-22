import { resolve } from 'path';
import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vitest/config';

const repoRoot = resolve(__dirname, '../..');

export default defineConfig({
  plugins: [vue()],
  test: {
    globals: true,
    environment: 'happy-dom',
    include: ['src/**/*.spec.ts'],
  },
  resolve: {
    alias: {
      '@mango/admin-pages/core': resolve(repoRoot, 'packages/admin-pages/src/core.ts'),
      '@mango/common': resolve(repoRoot, 'packages/common'),
      '@mango/http-client': resolve(repoRoot, 'packages/http-client/src/index.ts'),
      '@mango/notice/admin': resolve(repoRoot, 'packages/notice/src/admin.ts'),
      '@mango/notice/client': resolve(repoRoot, 'packages/notice/src/client.ts'),
      '@mango/notice/realtime': resolve(repoRoot, 'packages/notice/src/realtime.ts'),
      '@mango/notice': resolve(repoRoot, 'packages/notice/src'),
    },
  },
});
