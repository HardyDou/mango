import { registerMangoDevComponentPages } from '@mango/admin-pages/dev-pages';

let registered = false;

export function registerMangoAdminShellBaseDevPages() {
  if (registered) {
    return;
  }
  registered = true;
  registerMangoDevComponentPages([
    {
      menuId: 'shell-components-editor',
      menuName: '富文本编辑器',
      menuCode: 'shell:components:editor',
      path: '/components/editor',
      component: 'demo/components/EditorView',
      icon: 'Edit',
      sort: 2,
      loader: () => import('./components/EditorView.vue'),
    },
    {
      menuId: 'shell-components-code-editor',
      menuName: '代码编辑器',
      menuCode: 'shell:components:code-editor',
      path: '/components/code-editor',
      component: 'demo/components/CodeEditorView',
      icon: 'Code',
      sort: 3,
      loader: () => import('./components/CodeEditorView.vue'),
    },
    {
      menuId: 'shell-components-org-selector',
      menuName: '组织架构选择器',
      menuCode: 'shell:components:org-selector',
      path: '/components/org-selector',
      component: 'demo/components/OrgSelectorView',
      icon: 'Management',
      sort: 4,
      loader: () => import('./components/OrgSelectorView.vue'),
    },
    {
      menuId: 'shell-components-captcha',
      menuName: '验证码',
      menuCode: 'shell:components:captcha',
      path: '/components/captcha',
      component: 'demo/components/CaptchaView',
      icon: 'Key',
      sort: 6,
      loader: () => import('./components/CaptchaView.vue'),
    },
    {
      menuId: 'shell-components-charts',
      menuName: '数据图表',
      menuCode: 'shell:components:charts',
      path: '/components/charts',
      component: 'demo/components/ChartsView',
      icon: 'TrendCharts',
      sort: 7,
      loader: () => import('./components/ChartsView.vue'),
    },
    {
      menuId: 'shell-components-directive',
      menuName: '功能指令',
      menuCode: 'shell:components:directive',
      path: '/components/directive',
      component: 'demo/components/DirectiveView',
      icon: 'Pointer',
      sort: 8,
      loader: () => import('./components/DirectiveView.vue'),
    },
    {
      menuId: 'shell-components-china-area',
      menuName: '省市区选择器',
      menuCode: 'shell:components:china-area',
      path: '/components/china-area',
      component: 'demo/components/ChinaAreaView',
      icon: 'MapLocation',
      sort: 9,
      loader: () => import('./components/ChinaAreaView.vue'),
    },
    {
      menuId: 'shell-components-chat',
      menuName: 'AI 对话',
      menuCode: 'shell:components:chat',
      path: '/components/chat',
      component: 'demo/components/ChatView',
      icon: 'ChatDotRound',
      sort: 10,
      loader: () => import('./components/ChatView.vue'),
    },
    {
      menuId: 'shell-components-realtime',
      menuName: '实时通信',
      menuCode: 'shell:components:realtime',
      path: '/components/realtime',
      component: 'demo/components/RealtimeView',
      icon: 'Connection',
      sort: 11,
      loader: () => import('./components/RealtimeView.vue'),
    },
  ]);
}
