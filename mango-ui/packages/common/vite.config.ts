/// <reference types="vitest" />

import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  plugins: [vue()],
  build: {
    lib: {
      entry: 'index.ts',
      formats: ['es'],
      cssFileName: 'style',
      fileName: () => 'index.js',
    },
    rollupOptions: {
      external: [
        '@element-plus/icons-vue',
        '@mango/api-schema',
        '@mango/http-client',
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
    setupFiles: ['./utils/__tests__/setupCanvasMock.ts'],
  },
});
