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
    alias: {
      '@mango/api-schema': resolve(repoRoot, 'packages/api-schema'),
      '@mango/admin-pages': resolve(repoRoot, 'packages/admin-pages'),
      '@mango/admin-pages/core': resolve(repoRoot, 'packages/admin-pages/src/core.ts'),
      '@mango/common': resolve(__dirname, 'src/__tests__/commonMock.ts'),
      '@mango/common/utils/request': resolve(repoRoot, 'packages/common/utils/request.ts'),
      '@mango/file': resolve(repoRoot, 'packages/file/src/index.ts'),
    },
  },
});
