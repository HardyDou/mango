import { defineConfig } from 'vite';

const externalPackages = [
  '@mango/admin-shell',
  '@mango/admin-pages',
  '@mango/auth',
  '@mango/common',
  'vue',
  'vue-router',
  'pinia',
  'vue-i18n',
];

export default defineConfig({
  build: {
    lib: {
      entry: 'src/index.ts',
      formats: ['es'],
      fileName: () => 'index.js',
    },
    rollupOptions: {
      external: externalPackages,
    },
  },
});
