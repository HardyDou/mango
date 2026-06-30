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
        '@form-create/element-ui',
        '@mango/api-schema',
        '@mango/common',
        '@mango/common/utils/request',
        '@mango/file',
        '@mango/rbac',
        'element-plus',
        'form-create-designer',
        'vue',
        'vue-router',
      ],
    },
  },
});
