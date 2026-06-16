import { defineConfig } from 'vite';

const externalPackages = [
  '@mango/admin-shell',
  '@mango/admin-pages',
  '@mango/auth',
  '@mango/calendar/admin-pages',
  '@mango/common',
  '@mango/file/admin-pages',
  '@mango/grid-layout',
  '@mango/job/admin-pages',
  '@mango/notice/admin-pages',
  '@mango/notice/admin-shell',
  '@mango/numgen/admin-pages',
  '@mango/template/admin-pages',
  '@mango/workflow/admin-pages',
  '@mango/workflow-business-example/admin-pages',
  'vue',
  'vue-router',
  'pinia',
  'vue-i18n',
];

export default defineConfig({
  build: {
    lib: {
      entry: {
        index: 'src/index.ts',
        full: 'src/full.ts',
      },
      formats: ['es'],
    },
    rollupOptions: {
      external: externalPackages,
      output: {
        entryFileNames: '[name].js',
      },
    },
  },
});
