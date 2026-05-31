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
      entry: 'src/index.ts',
      formats: ['es'],
      fileName: () => 'index.js',
    },
    rollupOptions: {
      external: externalPackages,
    },
  },
});
