import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [vue()],
  build: {
    lib: {
      entry: {
        index: 'src/index.ts',
        'quick-entry': 'src/system/quick-entry/quick-entry.ts',
        'user-profile': 'src/system/user-profile/user-profile.ts',
      },
      formats: ['es'],
    },
    rollupOptions: {
      external: [
        '@element-plus/icons-vue',
        '@mango/common',
        '@mango/grid-layout',
        'element-plus',
        'vue',
      ],
      output: {
        entryFileNames: '[name].js',
      },
    },
  },
});
