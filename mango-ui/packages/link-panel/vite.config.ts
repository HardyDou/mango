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
        '@mango/link-page',
        '@mango/link-page/style.css',
      ],
      output: {
        entryFileNames: '[name].js',
      },
    },
  },
});
