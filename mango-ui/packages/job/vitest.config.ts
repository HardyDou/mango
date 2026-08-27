import { resolve } from 'node:path';
import { defineConfig } from 'vitest/config';

const repoRoot = resolve(__dirname, '../..');

export default defineConfig({
  test: {
    environment: 'node',
  },
  resolve: {
    alias: [
      {
        find: '@mango/common/utils/request',
        replacement: resolve(repoRoot, 'packages/common/utils/request.ts'),
      },
      {
        find: '@mango/common/utils/date-range',
        replacement: resolve(repoRoot, 'packages/common/utils/date-range.ts'),
      },
    ],
  },
});
