import { defineConfig } from 'vite';

export default defineConfig({
  build: {
    lib: {
      entry: {
        index: 'src/index.ts',
        'vue-micro': 'src/vue-micro.ts',
      },
      formats: ['es'],
    },
    rollupOptions: {
      external: [
        'vue',
        'vue-router',
        'wujie',
      ],
      output: {
        entryFileNames: '[name].js',
      },
    },
  },
});
