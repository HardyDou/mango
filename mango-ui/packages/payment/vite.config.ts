import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [vue()],
  build: {
    lib: {
      entry: {
        index: 'src/index.ts',
        'admin-pages': 'src/admin-pages.ts',
      },
      formats: ['es'],
    },
    rollupOptions: {
      external: [
        '@element-plus/icons-vue',
        '@mango/admin-pages/core',
        '@mango/api-schema',
        '@mango/common',
        '@mango/common/utils/request',
        '@mango/file',
        'element-plus',
        'qrcode',
        'vue',
        'vue-router',
      ],
      output: {
        entryFileNames: '[name].js',
      },
    },
  },
});
