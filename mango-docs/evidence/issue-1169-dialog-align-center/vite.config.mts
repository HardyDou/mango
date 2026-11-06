import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

const dialogDir = process.env.DIALOG_DIR;
const commonNodeModules = process.env.COMMON_NODE_MODULES;
if (!dialogDir || !commonNodeModules) throw new Error('DIALOG_DIR and COMMON_NODE_MODULES are required');

export default defineConfig({
  root: fileURLToPath(new URL('.', import.meta.url)),
  plugins: [vue()],
  resolve: {
    alias: [
      { find: /^@dialog$/, replacement: `${dialogDir}/index.vue` },
      { find: /^element-plus\/dist\/index\.css$/, replacement: `${commonNodeModules}/element-plus/dist/index.css` },
      { find: /^element-plus$/, replacement: `${commonNodeModules}/element-plus/es/index.mjs` },
      { find: /^@element-plus\/icons-vue$/, replacement: `${commonNodeModules}/@element-plus/icons-vue/dist/index.js` },
    ],
    dedupe: ['vue', 'element-plus'],
  },
  server: {
    port: Number(process.env.LAB_PORT ?? 5199),
    strictPort: true,
    fs: { allow: ['/'] },
  },
});
