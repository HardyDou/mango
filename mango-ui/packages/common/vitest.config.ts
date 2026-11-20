import vue from '@vitejs/plugin-vue';
import { dirname } from 'path';
import { fileURLToPath } from 'url';
import { defineConfig } from 'vitest/config';

const configDir = dirname(fileURLToPath(new URL(import.meta.url)));
const packageRoot = configDir;

export default defineConfig({
  plugins: [vue()],
  test: {
    globals: true,
    environment: 'happy-dom',
  },
  resolve: {
    alias: [{ find: /^@mango\/common$/, replacement: packageRoot }],
  },
});
