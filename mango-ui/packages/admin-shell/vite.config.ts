import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

const externalPackages = [
  '@element-plus/icons-vue',
  '@mango/admin-pages',
  '@mango/admin-pages/core',
  '@mango/admin-pages/defaults',
  '@mango/admin-pages/features',
  '@mango/app-runtime',
  '@mango/auth',
  '@mango/common',
  '@mango/common/utils/mitt',
  '@mango/common/utils/realtime/types',
  '@mango/common/utils/request',
  '@mango/common/utils/storage',
  '@mango/common/utils/tagsView',
  '@mango/common/utils/theme',
  '@mango/common/utils/menuTree',
  '@mango/rbac',
  '@mango/workflow',
  'element-plus',
  'pinia',
  'vue',
  'vue-i18n',
  'vue-router',
];

export default defineConfig({
  plugins: [vue()],
  build: {
    lib: {
      entry: 'src/index.ts',
      formats: ['es'],
      fileName: () => 'index.js',
    },
    rollupOptions: {
      external: externalPackages,
    },
  },
});
