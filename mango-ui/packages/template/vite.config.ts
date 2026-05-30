import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [vue()],
  build: {
    lib: {
      entry: 'src/index.ts',
      formats: ['es'],
      fileName: () => 'index.js',
    },
    rollupOptions: {
      external: [
        '@element-plus/icons-vue',
        '@mango/common',
        '@mango/common/utils/request',
        '@mango/file',
        'element-plus',
        'vue',
        'vue-router',
      ],
    },
  },
});
