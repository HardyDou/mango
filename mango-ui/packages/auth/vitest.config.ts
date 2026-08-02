import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';
import { defineConfig } from 'vitest/config';

const repoRoot = resolve(__dirname, '../..');

export default defineConfig({
  plugins: [vue()],
  test: {
    globals: true,
    environment: 'happy-dom',
  },
  resolve: {
    alias: [
      {
        find: '@mango/common/utils/request',
        replacement: resolve(repoRoot, 'packages/common/utils/request.ts'),
      },
      {
        find: '@mango/common/utils/storage',
        replacement: resolve(repoRoot, 'packages/common/utils/storage.ts'),
      },
      {
        find: '@mango/common/api/captcha',
        replacement: resolve(repoRoot, 'packages/common/api/captcha.ts'),
      },
      {
        find: '@mango/api-schema',
        replacement: resolve(repoRoot, 'packages/api-schema'),
      },
      {
        find: /^@mango\/common$/,
        replacement: resolve(repoRoot, 'packages/common/src/index.ts'),
      },
    ],
  },
});
