import { readFileSync } from 'node:fs';
import { defineConfig } from 'vite';

const packageJson = JSON.parse(readFileSync(new URL('./package.json', import.meta.url), 'utf8'));

export default defineConfig({
  define: {
    __MANGO_ADMIN_EXTENSION_VERSION__: JSON.stringify(packageJson.version),
  },
  build: {
    lib: {
      entry: {
        index: 'src/index.ts',
        core: 'src/core.ts',
        features: 'src/features.ts',
        notice: 'src/notice.ts',
      },
      formats: ['es'],
    },
    rollupOptions: {
      external: ['@mango/common/utils/realtime/types', 'vue'],
      output: {
        entryFileNames: '[name].js',
      },
    },
  },
});
