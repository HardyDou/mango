import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [vue()],
  build: {
    lib: {
      entry: {
        index: 'src/index.ts',
        'admin-pages': 'src/admin-pages.ts',
        'widgets/quick-entry': 'src/widgets/quick-entry/quick-entry.ts',
        'widgets/user-profile': 'src/widgets/user-profile/user-profile.ts',
      },
      formats: ['es'],
    },
    rollupOptions: {
      external: [
        '@element-plus/icons-vue',
        '@form-create/element-ui',
        '@mango/api-schema',
        '@mango/common',
        '@mango/common/utils/request',
        '@mango/file',
        '@mango/grid-widgets',
        '@mango/rbac',
        'element-plus',
        'form-create-designer',
        'vue',
        'vue-router',
      ],
      output: {
        entryFileNames: '[name].js',
      },
    },
  },
});
