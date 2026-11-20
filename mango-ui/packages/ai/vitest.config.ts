import vue from '@vitejs/plugin-vue';
import { dirname, resolve } from 'path';
import { fileURLToPath } from 'url';
import { defineConfig } from 'vitest/config';

const configDir = dirname(fileURLToPath(new URL(import.meta.url)));
const workspaceRoot = resolve(configDir, '../..');

// 工作区内被 @mango/ai 引用的包都以 dist 作为入口，测试期直接解析源码，
// 避免必须先执行 `pnpm -r build` 才能跑单测。
export default defineConfig({
  plugins: [vue()],
  test: {
    globals: true,
    environment: 'happy-dom',
  },
  resolve: {
    alias: [
      { find: /^@mango\/ai-api$/, replacement: resolve(workspaceRoot, 'packages/ai-api/src/index.ts') },
      { find: /^@mango\/common$/, replacement: resolve(workspaceRoot, 'packages/common') },
      { find: /^@mango\/api-schema$/, replacement: resolve(workspaceRoot, 'packages/api-schema') },
      { find: /^@mango\/app-runtime$/, replacement: resolve(workspaceRoot, 'packages/app-runtime/src/index.ts') },
      { find: /^@mango\/admin-extension$/, replacement: resolve(workspaceRoot, 'packages/admin-extension/src/index.ts') },
    ],
  },
});
