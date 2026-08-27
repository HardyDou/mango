import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';
import { resolve } from 'node:path';

const commonPackage = resolve(__dirname, '../common');

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@mango/common/utils/date-range': resolve(commonPackage, 'utils/date-range.ts'),
    },
  },
  build: {
    lib: {
      entry: {
        index: 'src/index.ts',
        'admin-pages': 'src/admin-pages.ts',
        'widgets/quick-entry': 'src/widgets/quick-entry/quick-entry.ts',
        'widgets/user-profile': 'src/widgets/user-profile/user-profile.ts',
      },
      formats: ['es'],
      cssFileName: 'style',
    },
    rollupOptions: {
      external: [
        '@element-plus/icons-vue',
        '@form-create/element-ui',
        '@mango/api-schema',
        '@mango/common',
        '@mango/common/utils/request',
        '@mango/common/utils/date-range',
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
