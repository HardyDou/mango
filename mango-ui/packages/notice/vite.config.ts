import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [vue()],
  build: {
    lib: {
      entry: {
        index: 'src/index.ts',
        admin: 'src/admin.ts',
        'admin-pages': 'src/admin-pages.ts',
        'admin-shell': 'src/admin-shell.ts',
        client: 'src/client.ts',
        realtime: 'src/realtime.ts',
      },
      formats: ['es'],
    },
    rollupOptions: {
      external: [
        '@element-plus/icons-vue',
        '@mango/admin-pages/core',
        '@mango/admin-pages/notice',
        '@mango/common',
        '@mango/common/utils/realtime',
        '@mango/common/utils/realtime/types',
        '@mango/common/utils/request',
        '@mango/system',
        'element-plus',
        'vue',
        'vue-router',
      ],
      output: {
        entryFileNames: '[name].js',
      },
    },
  },
});
