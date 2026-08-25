import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'happy-dom',
  },
  build: {
    lib: {
      entry: {
        index: 'src/index.ts',
        'admin-pages': 'src/admin-pages.ts',
      },
      formats: ['es'],
      cssFileName: 'style',
    },
    rollupOptions: {
      external: [
        '@element-plus/icons-vue',
        '@mango/admin-extension/core',
        '@mango/ai-api',
        '@mango/api-schema',
        '@mango/app-runtime',
        '@mango/common',
        'element-plus',
        'vue',
      ],
      output: {
        entryFileNames: '[name].js',
      },
    },
  },
});
