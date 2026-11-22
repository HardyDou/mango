import vue from '@vitejs/plugin-vue';
import { dirname, resolve } from 'path';
import { fileURLToPath } from 'url';
import { defineConfig } from 'vitest/config';

const configDir = dirname(fileURLToPath(new URL(import.meta.url)));
const packageRoot = configDir;
const repoRoot = resolve(configDir, '../..');

export default defineConfig({
  plugins: [vue()],
  test: {
    globals: true,
    environment: 'happy-dom',
  },
  resolve: {
    alias: [
      { find: /^@mango\/common$/, replacement: packageRoot },
      // 与 notice 包一致：直接指向源码，避免依赖尚未构建的 dist。
      { find: /^@mango\/http-client$/, replacement: resolve(repoRoot, 'packages/http-client/src/index.ts') },
    ],
  },
});
