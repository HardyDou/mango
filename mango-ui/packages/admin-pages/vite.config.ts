import { defineConfig } from 'vite';

const externalPackages = [
  '@mango/auth',
  '@mango/calendar',
  '@mango/file',
  '@mango/numgen',
  '@mango/rbac',
  '@mango/system',
  '@mango/template',
  '@mango/workflow',
  '@mango/workflow-business-example',
  'vue',
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
