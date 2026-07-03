import { defineConfig } from 'vite';

export default defineConfig({
  build: {
    lib: {
      entry: {
        index: 'src/index.ts',
      },
      formats: ['es'],
    },
    rollupOptions: {
      external: [
        '@mango/common',
        '@mango/common/utils/request',
      ],
      output: {
        entryFileNames: '[name].js',
      },
    },
  },
});
