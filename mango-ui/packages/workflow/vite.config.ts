import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [vue()],
  build: {
    lib: {
      entry: {
        index: 'src/index.ts',
        'admin-pages': 'src/admin-pages.ts',
        'widgets/my-process': 'src/widgets/my-process/my-process.ts',
        'widgets/my-task': 'src/widgets/my-task/my-task.ts',
        'widgets/my-todo': 'src/widgets/my-todo/my-todo.ts',
      },
      formats: ['es'],
    },
    rollupOptions: {
      external: [
        '@element-plus/icons-vue',
        '@form-create/element-ui',
        '@mango/admin-pages/core',
        '@mango/api-schema',
        '@mango/common',
        '@mango/common/utils/request',
        '@mango/file',
        '@mango/grid-widgets',
        '@mango/system',
        'element-plus',
        'form-create-designer',
        'vue',
        'vue-router',
      ],
      output: {
        entryFileNames: '[name].js',
      },
    },
  },
});
