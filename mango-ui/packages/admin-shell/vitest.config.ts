import vue from '@vitejs/plugin-vue';
import { dirname, resolve } from 'path';
import { fileURLToPath } from 'url';
import { defineConfig } from 'vitest/config';

const configDir = dirname(fileURLToPath(new URL(import.meta.url)));
const workspaceRoot = resolve(configDir, '../..');

// 注意：本包部分用例会导入 @mango/home，其 package.json 只暴露 dist 产物；
// 首次运行前先 `pnpm -F @mango/home build`（或 `pnpm -r build`），否则会出现
// `Failed to resolve entry for package "@mango/home"`。不使用测试期 alias 掩盖该依赖。
export default defineConfig({
  plugins: [vue()],
  test: {
    globals: true,
    environment: 'happy-dom',
    include: ['src/**/__tests__/**/*.spec.ts'],
  },
  resolve: {
    alias: [
      { find: /^@mango\/common$/, replacement: resolve(workspaceRoot, 'packages/common') },
    ],
  },
});
