import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const proxyTarget = env.VITE_ADMIN_PROXY_PATH || `http://127.0.0.1:${env.MANGO_BACKEND_PORT || '5555'}`;

  return {
    plugins: [vue()],
    optimizeDeps: {
      include: [
        'dayjs',
        'dayjs/plugin/advancedFormat.js',
        'dayjs/plugin/customParseFormat.js',
        'dayjs/plugin/dayOfYear.js',
        'dayjs/plugin/isSameOrAfter.js',
        'dayjs/plugin/isSameOrBefore.js',
        'dayjs/plugin/localeData.js',
        'dayjs/plugin/weekOfYear.js',
        'dayjs/plugin/weekYear.js',
        'element-plus/es/locale/lang/zh-cn',
        'element-plus/es/locale/lang/en',
      ],
    },
    server: {
      port: Number(env.MANGO_FRONTEND_PORT || 5173),
      proxy: {
        '/api': {
          target: proxyTarget,
          changeOrigin: true,
          rewrite: path => path.replace(/^\/api/, ''),
        },
      },
    },
    preview: {
      port: Number(env.MANGO_FRONTEND_PORT || 4173),
    },
  };
});
