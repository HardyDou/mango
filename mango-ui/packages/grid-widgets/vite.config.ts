import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [vue()],
  build: {
    lib: {
      entry: {
        index: 'src/index.ts',
      },
      formats: ['es'],
    },
    rollupOptions: {
      external: [
        '@mango/grid-layout',
        'vue',
      ],
      output: {
        entryFileNames: '[name].js',
      },
    },
  },
});
