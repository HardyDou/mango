/// <reference types="vitest" />

import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [vue()],
  build: {
    lib: {
      entry: 'index.ts',
      formats: ['es'],
      fileName: () => 'index.js',
    },
    rollupOptions: {
      external: [
        '@element-plus/icons-vue',
        '@mango/api-schema',
        '@microsoft/fetch-event-source',
        '@vueuse/core',
        '@wangeditor/editor',
        '@wangeditor/editor-for-vue',
        'axios',
        'codemirror',
        'crypto-js',
        'echarts',
        'element-plus',
        'js-cookie',
        'lodash',
        'mitt',
        'nprogress',
        'pinia',
        'qs',
        'screenfull',
        'sm-crypto',
        'vue',
        'vue-i18n',
        'vue-router',
        'xlsx',
      ],
    },
  },
  test: {
    environment: 'happy-dom',
  },
});
