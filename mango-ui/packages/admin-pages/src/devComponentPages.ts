export interface DevComponentDemoPage {
  menuId: string;
  menuName: string;
  menuCode: string;
  path: string;
  component: string;
  icon: string;
  sort: number;
}

export const DEV_COMPONENT_DEMO_REDIRECT = '/components/upload';

export const DEV_COMPONENT_DEMO_PAGES: DevComponentDemoPage[] = [
  {
    menuId: 'shell-components-upload',
    menuName: '文件上传',
    menuCode: 'shell:components:upload',
    path: '/components/upload',
    component: 'demo/components/UploadView',
    icon: 'Upload',
    sort: 1,
  },
  {
    menuId: 'shell-components-editor',
    menuName: '富文本编辑器',
    menuCode: 'shell:components:editor',
    path: '/components/editor',
    component: 'demo/components/EditorView',
    icon: 'Edit',
    sort: 2,
  },
  {
    menuId: 'shell-components-code-editor',
    menuName: '代码编辑器',
    menuCode: 'shell:components:code-editor',
    path: '/components/code-editor',
    component: 'demo/components/CodeEditorView',
    icon: 'Code',
    sort: 3,
  },
  {
    menuId: 'shell-components-org-selector',
    menuName: '组织架构选择器',
    menuCode: 'shell:components:org-selector',
    path: '/components/org-selector',
    component: 'demo/components/OrgSelectorView',
    icon: 'Management',
    sort: 4,
  },
  {
    menuId: 'shell-components-captcha',
    menuName: '验证码',
    menuCode: 'shell:components:captcha',
    path: '/components/captcha',
    component: 'demo/components/CaptchaView',
    icon: 'Key',
    sort: 5,
  },
  {
    menuId: 'shell-components-charts',
    menuName: '数据图表',
    menuCode: 'shell:components:charts',
    path: '/components/charts',
    component: 'demo/components/ChartsView',
    icon: 'TrendCharts',
    sort: 6,
  },
  {
    menuId: 'shell-components-directive',
    menuName: '功能指令',
    menuCode: 'shell:components:directive',
    path: '/components/directive',
    component: 'demo/components/DirectiveView',
    icon: 'Pointer',
    sort: 7,
  },
  {
    menuId: 'shell-components-china-area',
    menuName: '省市区选择器',
    menuCode: 'shell:components:china-area',
    path: '/components/china-area',
    component: 'demo/components/ChinaAreaView',
    icon: 'MapLocation',
    sort: 8,
  },
  {
    menuId: 'shell-components-chat',
    menuName: 'AI 对话',
    menuCode: 'shell:components:chat',
    path: '/components/chat',
    component: 'demo/components/ChatView',
    icon: 'ChatDotRound',
    sort: 9,
  },
  {
    menuId: 'shell-components-realtime',
    menuName: '实时通信',
    menuCode: 'shell:components:realtime',
    path: '/components/realtime',
    component: 'demo/components/RealtimeView',
    icon: 'Connection',
    sort: 10,
  },
];
