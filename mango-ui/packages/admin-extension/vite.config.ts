import { defineConfig } from 'vite';

export default defineConfig({
  build: {
    lib: {
      entry: {
        index: 'src/index.ts',
        core: 'src/core.ts',
        features: 'src/features.ts',
        notice: 'src/notice.ts',
      },
      formats: ['es'],
    },
    rollupOptions: {
      external: ['@mango/common/utils/realtime/types', 'vue'],
      output: {
        entryFileNames: '[name].js',
      },
    },
  },
});
