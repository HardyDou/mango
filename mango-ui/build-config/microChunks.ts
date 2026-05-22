export function mangoMicroManualChunks(id: string) {
  if (!id.includes('node_modules') && !id.includes('/packages/')) {
    return undefined;
  }
  if (id.includes('@element-plus/icons-vue')) {
    return 'vendor-icons';
  }
  if (id.includes('element-plus')) {
    return 'vendor-element-plus';
  }
  if (id.includes('/node_modules/vue') || id.includes('/node_modules/@vue') || id.includes('/node_modules/vue-router') || id.includes('/node_modules/pinia')) {
    return 'vendor-vue';
  }
  if (id.includes('/node_modules/wujie')) {
    return 'vendor-wujie';
  }
  if (id.includes('form-create') || id.includes('@form-create')) {
    return 'vendor-form-create';
  }
  if (id.includes('wangeditor') || id.includes('@wangeditor')) {
    return 'vendor-editor';
  }
  if (id.includes('codemirror') || id.includes('highlight.js')) {
    return 'vendor-code-editor';
  }
  if (id.includes('echarts') || id.includes('zrender')) {
    return 'vendor-chart';
  }
  if (id.includes('axios') || id.includes('nprogress')) {
    return 'vendor-request';
  }
  if (id.includes('lodash') || id.includes('lodash-es')) {
    return 'vendor-lodash';
  }
  if (id.includes('/packages/rbac/')) {
    return 'mango-rbac';
  }
  if (id.includes('/packages/system/')) {
    return 'mango-system';
  }
  if (id.includes('/packages/template/')) {
    return 'mango-template';
  }
  if (id.includes('/packages/workflow/')) {
    return 'mango-workflow';
  }
  if (id.includes('/packages/auth/')) {
    return 'mango-auth';
  }
  if (id.includes('/packages/common/index.ts')) {
    return 'mango-common-barrel';
  }
  if (id.includes('/packages/common/utils/') || id.includes('/packages/common/api/') || id.includes('/packages/common/hooks/')) {
    return 'mango-common-core';
  }
  if (id.includes('/packages/common/components/CodeEditor/')) {
    return 'mango-common-code-editor';
  }
  if (id.includes('/packages/common/components/ECharts/')) {
    return 'mango-common-chart';
  }
  if (id.includes('/packages/common/components/Editor/')) {
    return 'mango-common-editor';
  }
  if (id.includes('/packages/common/components/FormCreate/')) {
    return 'mango-common-form-create';
  }
  if (id.includes('/packages/common/components/ExcelUpload/')) {
    return 'mango-common-excel-upload';
  }
  if (id.includes('/packages/common/components/')) {
    return 'mango-common-components';
  }
  if (id.includes('/packages/common/')) {
    return 'mango-common';
  }
  if (id.includes('/packages/app-runtime/') || id.includes('/packages/admin-pages/')) {
    return 'mango-runtime';
  }
  if (id.includes('node_modules')) {
    return 'vendor';
  }
  return undefined;
}
