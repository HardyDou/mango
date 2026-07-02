import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

export default defineConfig({
  plugins: [vue()],
  build: {
    lib: {
      entry: {
        index: 'src/index.ts',
        calendar: 'src/system/calendar/calendar.ts',
        'my-task': 'src/system/my-task/my-task.ts',
        'my-todo': 'src/system/my-todo/my-todo.ts',
        'my-process': 'src/system/my-process/my-process.ts',
        'quick-entry': 'src/system/quick-entry/quick-entry.ts',
        'message-center': 'src/system/message-center/message-center.ts',
        'user-profile': 'src/system/user-profile/user-profile.ts',
      },
      formats: ['es'],
    },
    rollupOptions: {
      external: [
        '@element-plus/icons-vue',
        '@mango/calendar',
        '@mango/common',
        '@mango/grid-layout',
        '@mango/notice',
        '@mango/workflow',
        'element-plus',
        'vue',
      ],
      output: {
        entryFileNames: '[name].js',
      },
    },
  },
});
