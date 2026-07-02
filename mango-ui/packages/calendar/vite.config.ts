import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [vue()],
  build: {
    lib: {
      entry: {
        index: 'src/index.ts',
        'admin-pages': 'src/admin-pages.ts',
        'widgets/calendar': 'src/widgets/calendar/calendar.ts',
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
        '@mango/grid-widgets',
        'element-plus',
        'vue',
      ],
      output: {
        entryFileNames: '[name].js',
      },
    },
  },
});
