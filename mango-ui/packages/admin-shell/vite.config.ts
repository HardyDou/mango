import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';
import { createMangoWorkspaceAliases } from '../../build-config/mangoAliases';

const externalPackages = [
  '@element-plus/icons-vue',
  '@mango/admin-pages',
  '@mango/admin-pages/core',
  '@mango/admin-pages/defaults',
  '@mango/admin-pages/dev-component-pages',
  '@mango/admin-pages/dev-pages',
  '@mango/admin-pages/features',
  '@mango/admin-pages/notice',
  '@mango/app-runtime',
  '@mango/auth',
  '@mango/auth/config',
  '@mango/common',
  '@mango/common/utils/authFunction',
  '@mango/common/utils/errorHandling',
  '@mango/common/utils/iconConfig',
  '@mango/common/utils/message',
  '@mango/common/utils/mitt',
  '@mango/common/utils/realtime/types',
  '@mango/common/utils/request',
  '@mango/common/utils/storage',
  '@mango/common/utils/tagsView',
  '@mango/common/utils/theme',
  '@mango/common/utils/menuTree',
  '@mango/common/utils/webCrypto',
  '@mango/file',
  '@mango/grid-layout',
  '@mango/grid-widgets',
  '@mango/home',
  '@mango/job/style.css',
  '@mango/rbac',
  '@mango/workflow',
  'element-plus',
  'pinia',
  'vue',
  'vue-i18n',
  'vue-router',
];

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: createMangoWorkspaceAliases({
      appDir: __dirname,
      includeStyles: false,
    }),
  },
  build: {
    minify: false,
    lib: {
      entry: {
        index: 'src/index.ts',
        runtime: 'src/runtime/runtimeHost.ts',
        menu: 'src/runtime/menuHost.ts',
        stores: 'src/stores/index.ts',
        router: 'src/router.ts',
        home: 'src/views/home/index.vue',
        'home-management': 'src/views/home/management/index.vue',
        'home-templates': 'src/views/home/templates/index.vue',
        'home-list': 'src/views/home/list/index.vue',
        'home-user': 'src/views/home/user/index.vue',
        'dev-pages': 'src/views/demo/registerDevPages.ts',
        'dev-base-pages': 'src/views/demo/registerBaseDevPages.ts',
        'dev-upload-page': 'src/views/demo/components/UploadView.vue',
        'dev-workflow-page': 'src/views/demo/components/WorkflowComponentsView.vue',
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
