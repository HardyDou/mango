import { defineConfig } from 'vite';

const externalPackages = [
  '@mango/auth',
  '@mango/common/utils/realtime/types',
  '@mango/rbac',
  '@mango/system',
  'vue',
];

export default defineConfig({
  build: {
    lib: {
      entry: {
        index: 'src/index.ts',
        core: 'src/core.ts',
        defaults: 'src/defaults.ts',
        features: 'src/features.ts',
        notice: 'src/notice.ts',
        'dev-pages': 'src/dev-pages.ts',
        'dev-component-pages': 'src/devComponentPages.ts',
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
