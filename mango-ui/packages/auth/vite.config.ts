import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

const externalPackages = [
  '@element-plus/icons-vue',
  '@mango/api-schema',
  '@mango/common',
  '@mango/common/api/captcha',
  '@mango/common/api/upload',
  '@mango/common/utils/request',
  '@mango/common/utils/storage',
  'element-plus',
  'pinia',
  'vue',
  'vue-router',
];

export default defineConfig({
  plugins: [vue()],
  build: {
    lib: {
      entry: {
        index: 'src/index.ts',
        config: 'src/config.ts',
      },
      formats: ['es'],
      cssFileName: 'style',
    },
    rollupOptions: {
      external: externalPackages,
      output: {
        entryFileNames: '[name].js',
      },
    },
  },
});
