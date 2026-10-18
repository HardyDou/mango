import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [vue()],
  build: {
    lib: {
      entry: { index: 'src/index.ts', 'admin-pages': 'src/admin-pages.ts' },
      formats: ['es'],
      cssFileName: 'style',
    },
    rollupOptions: {
      external: [
        '@element-plus/icons-vue',
        '@form-create/element-ui',
        '@mango/admin-extension/core',
        '@mango/api-schema',
        '@mango/app-runtime',
        '@mango/common',
        'element-plus',
        'form-create-designer',
        'vue',
        'vue-router',
      ],
      output: { entryFileNames: '[name].js' },
    },
  },
});
