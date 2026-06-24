import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [vue()],
  build: {
    lib: {
      entry: {
        index: 'src/index.ts',
        'quick-entry': 'src/system/quick-entry/quick-entry.ts',
        'message-center': 'src/system/message-center/message-center.ts',
        'user-profile': 'src/system/user-profile/user-profile.ts',
      },
      formats: ['es'],
    },
    rollupOptions: {
      external: [
        '@element-plus/icons-vue',
        '@mango/common',
        '@mango/grid-layout',
        '@mango/notice',
        'element-plus',
        'vue',
      ],
      output: {
        entryFileNames: '[name].js',
      },
    },
  },
});
