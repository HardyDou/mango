import { computed, ref } from 'vue';
import { get } from '@mango/common/utils/request';
import type { MangoMenuPageType } from '@mango/app-runtime';
import type { RouteRecordRaw } from 'vue-router';

export enum MenuTypeEnum {
  DIRECTORY = 1,
  MENU = 2,
  BUTTON = 3,
}

export interface ShellMenu {
  menuId: string | number;
  appCode?: string;
  moduleCode?: string;
  parentId: string | number;
  menuType: MenuTypeEnum;
  menuName: string;
  menuCode: string;
  path: string;
  pageType?: MangoMenuPageType;
  component?: string;
  externalUrl?: string;
  icon?: string;
  sort: number;
  status: number;
  visible: number;
  keepAlive?: number;
  embedded?: number;
  redirect?: string;
  meta?: Record<string, any>;
  children?: ShellMenu[];
}

export interface ShellRouteMenu extends RouteRecordRaw {
  sourceMenu: ShellMenu;
  children?: ShellRouteMenu[];
}

export type ShellMenuTreeNode = {
  path: string;
  children?: ShellMenuTreeNode[];
};

export function useMenuHost() {
  const menuLoading = ref(false);
  const menus = ref<ShellRouteMenu[]>([]);
  const activeTopPath = ref('');
  const activeMenuPath = ref('');

  const topMenus = computed(() => menus.value.filter(menu => !menu.meta?.isHide));
  const activeTopMenu = computed(() => topMenus.value.find(menu => menu.path === activeTopPath.value) || topMenus.value[0]);
  const sideMenus = computed(() => {
    const activeTop = activeTopMenu.value;
    if (!activeTop) {
      return [];
    }
    return activeTop.children && activeTop.children.length > 0 ? activeTop.children : [activeTop];
  });
  const activeMenu = computed(() => findMenuByPath(menus.value, activeMenuPath.value));

  async function loadMenus() {
    menuLoading.value = true;
    try {
      const response = await get<ShellMenu[]>('/authorization/menus/user', {
        params: { fmt: 'tree', appCode: 'internal-admin' },
      });
      menus.value = [
        createHomeRouteMenu(),
        ...filterMenuForNav(response || []).map(toShellRouteMenu),
        ...createDevRouteMenus(),
        ...createAccountRouteMenus(),
      ];
      const first = resolveFirstMenu(menus.value[0]);
      activeTopPath.value = menus.value[0]?.path || '';
      activeMenuPath.value = first?.path || '';
      return first;
    } finally {
      menuLoading.value = false;
    }
  }

  function selectTop(menu: ShellRouteMenu) {
    activeTopPath.value = menu.path;
    const first = resolveFirstMenu(menu);
    if (first) {
      activeMenuPath.value = first.path;
    }
    return first;
  }

  function selectMenu(path: string) {
    const menu = findMenuByPath(menus.value, path);
    if (!menu) {
      return undefined;
    }
    activeMenuPath.value = menu.path;
    const top = findTopByPath(menus.value, menu.path);
    if (top) {
      activeTopPath.value = top.path;
    }
    return menu;
  }

  return {
    menuLoading,
    menus,
    topMenus,
    sideMenus,
    activeTopPath,
    activeMenuPath,
    activeMenu,
    loadMenus,
    selectTop,
    selectMenu,
  };
}

function filterMenuForNav(menus: ShellMenu[]): ShellMenu[] {
  return menus
    .filter(menu => menu.menuType !== MenuTypeEnum.BUTTON && menu.visible !== 0)
    .map(menu => ({
      ...menu,
      children: menu.children ? filterMenuForNav(menu.children) : [],
    }));
}

function toShellRouteMenu(menu: ShellMenu): ShellRouteMenu {
  const moduleCode = menu.moduleCode || inferModuleCode(menu.component, menu.path);
  const sourceMenu = {
    ...menu,
    moduleCode,
  };
  return {
    path: menu.path,
    name: menu.menuCode || menu.menuName,
    redirect: menu.redirect,
    meta: {
      ...(menu.meta || {}),
      title: menu.menuName,
      icon: menu.icon,
      isHide: menu.visible === 0,
      keepAlive: menu.keepAlive === 1,
      embedded: menu.embedded === 1,
    },
    sourceMenu,
    children: menu.children?.map(toShellRouteMenu),
  };
}

function inferModuleCode(component?: string, path?: string) {
  const target = `${component || ''} ${path || ''}`;
  if (target.includes('components/') || target.includes('demo/') || target.includes('debug/')) {
    return 'mango-shell';
  }
  if (target.includes('profile') || target.includes('password')) {
    return 'mango-authorization';
  }
  if (target.includes('home')) {
    return 'mango-shell';
  }
  if (target.includes('workflow/')) {
    return 'mango-workflow';
  }
  if (target.includes('system/dict')
    || target.includes('system/operation-log')
    || target.includes('system/login-log')
    || target.includes('system/tenant')
    || target.includes('system/config')
    || target.includes('system/route')
    || target.includes('system/public-path')
    || target.includes('system/area')
    || target.includes('system/file')) {
    return 'mango-system';
  }
  if (target.includes('system/')) {
    return 'mango-authorization';
  }
  return undefined;
}

function createHomeRouteMenu(): ShellRouteMenu {
  return toShellRouteMenu({
    appCode: 'internal-admin',
    moduleCode: 'mango-shell',
    menuId: 'shell-home',
    menuName: '首页',
    menuCode: 'shell:home',
    parentId: 0,
    menuType: MenuTypeEnum.MENU,
    path: '/home',
    component: 'home/index',
    icon: 'HomeFilled',
    sort: -1000,
    status: 1,
    visible: 1,
    keepAlive: 1,
    pageType: 'LOCAL_ROUTE',
    children: [],
  });
}

function createDevRouteMenus(): ShellRouteMenu[] {
  if (!import.meta.env.DEV) {
    return [];
  }

  return [
    toShellRouteMenu({
      appCode: 'internal-admin',
      moduleCode: 'mango-shell',
      menuId: 'shell-develop',
      menuName: '开发中心',
      menuCode: 'shell:develop',
      parentId: 0,
      menuType: MenuTypeEnum.DIRECTORY,
      path: '/develop',
      icon: 'Monitor',
      sort: 999999,
      status: 1,
      visible: 1,
      pageType: 'LOCAL_ROUTE',
      redirect: '/components/editor',
      children: [
        {
          appCode: 'internal-admin',
          moduleCode: 'mango-shell',
          menuId: 'shell-develop-components',
          menuName: '组件库',
          menuCode: 'shell:develop:components',
          parentId: 'shell-develop',
          menuType: MenuTypeEnum.DIRECTORY,
          path: '/develop/components',
          icon: 'Box',
          sort: 1,
          status: 1,
          visible: 1,
          pageType: 'LOCAL_ROUTE',
          redirect: '/components/editor',
          children: createComponentDemoMenus(),
        },
      ],
    }),
  ];
}

function createComponentDemoMenus(): ShellMenu[] {
  const pages: Array<Pick<ShellMenu, 'menuId' | 'menuName' | 'menuCode' | 'path' | 'component' | 'icon'> & { sort: number }> = [
    {
      menuId: 'shell-components-editor',
      menuName: '富文本编辑器',
      menuCode: 'shell:components:editor',
      path: '/components/editor',
      component: 'demo/components/EditorView',
      icon: 'Edit',
      sort: 1,
    },
    {
      menuId: 'shell-components-code-editor',
      menuName: '代码编辑器',
      menuCode: 'shell:components:code-editor',
      path: '/components/code-editor',
      component: 'demo/components/CodeEditorView',
      icon: 'Code',
      sort: 2,
    },
    {
      menuId: 'shell-components-upload',
      menuName: '文件上传',
      menuCode: 'shell:components:upload',
      path: '/components/upload',
      component: 'demo/components/UploadView',
      icon: 'Upload',
      sort: 3,
    },
    {
      menuId: 'shell-components-charts',
      menuName: '数据图表',
      menuCode: 'shell:components:charts',
      path: '/components/charts',
      component: 'demo/components/ChartsView',
      icon: 'TrendCharts',
      sort: 4,
    },
    {
      menuId: 'shell-components-directive',
      menuName: '功能指令',
      menuCode: 'shell:components:directive',
      path: '/components/directive',
      component: 'demo/components/DirectiveView',
      icon: 'Pointer',
      sort: 5,
    },
    {
      menuId: 'shell-demo-chat',
      menuName: 'AI 对话',
      menuCode: 'shell:demo:chat',
      path: '/demo/chat',
      component: 'demo/components/ChatView',
      icon: 'ChatDotRound',
      sort: 6,
    },
    {
      menuId: 'shell-demo-sse',
      menuName: '服务端推送',
      menuCode: 'shell:demo:sse',
      path: '/demo/sse',
      component: 'demo/components/SSEView',
      icon: 'Connection',
      sort: 7,
    },
    {
      menuId: 'shell-demo-websocket',
      menuName: 'WebSocket',
      menuCode: 'shell:demo:websocket',
      path: '/demo/websocket',
      component: 'demo/components/WebsocketView',
      icon: 'Connection',
      sort: 8,
    },
    {
      menuId: 'shell-demo-china-area',
      menuName: '中国行政区划',
      menuCode: 'shell:demo:china-area',
      path: '/demo/china-area',
      component: 'demo/components/ChinaAreaView',
      icon: 'MapLocation',
      sort: 9,
    },
    {
      menuId: 'shell-demo-org-selector',
      menuName: '机构选择器',
      menuCode: 'shell:demo:org-selector',
      path: '/demo/org-selector',
      component: 'demo/components/OrgSelectorView',
      icon: 'Management',
      sort: 10,
    },
    {
      menuId: 'shell-demo-captcha',
      menuName: '验证码',
      menuCode: 'shell:demo:captcha',
      path: '/demo/captcha',
      component: 'demo/components/CaptchaView',
      icon: 'Key',
      sort: 11,
    },
    {
      menuId: 'shell-debug-test',
      menuName: '测试页面',
      menuCode: 'shell:debug:test',
      path: '/debug/test',
      component: 'demo/components/CaptchaView',
      icon: 'Bug',
      sort: 12,
    },
  ];

  return pages.map(page => ({
    ...page,
    appCode: 'internal-admin',
    moduleCode: 'mango-shell',
    parentId: 'shell-develop-components',
    menuType: MenuTypeEnum.MENU,
    status: 1,
    visible: 1,
    keepAlive: 1,
    pageType: 'LOCAL_ROUTE',
    children: [],
  }));
}

function createAccountRouteMenus(): ShellRouteMenu[] {
  return [
    createAccountRouteMenu({
      menuId: 'account-profile',
      menuName: '个人中心',
      menuCode: 'account:profile',
      path: '/profile',
      component: 'profile/index',
      icon: 'User',
    }),
    createAccountRouteMenu({
      menuId: 'account-password',
      menuName: '修改密码',
      menuCode: 'account:password',
      path: '/password',
      component: 'password/index',
      icon: 'Lock',
    }),
  ];
}

function createAccountRouteMenu(menu: Pick<ShellMenu, 'menuId' | 'menuName' | 'menuCode' | 'path' | 'component' | 'icon'>): ShellRouteMenu {
  return toShellRouteMenu({
    ...menu,
    appCode: 'internal-admin',
    moduleCode: 'mango-authorization',
    parentId: 0,
    menuType: MenuTypeEnum.MENU,
    sort: 0,
    status: 1,
    visible: 0,
    pageType: 'LOCAL_ROUTE',
    children: [],
  });
}

export function resolveFirstMenu(menu?: ShellRouteMenu): ShellRouteMenu | undefined {
  if (!menu) {
    return undefined;
  }
  if (isRunnableMenu(menu)) {
    return menu;
  }
  for (const child of menu.children || []) {
    const first = resolveFirstMenu(child);
    if (first) {
      return first;
    }
  }
  return undefined;
}

export function isRunnableMenu(menu?: ShellRouteMenu): boolean {
  const source = menu?.sourceMenu;
  if (!source || source.menuType === MenuTypeEnum.BUTTON || source.visible === 0) {
    return false;
  }
  if (source.pageType === 'IFRAME' || source.pageType === 'EXTERNAL_LINK') {
    return Boolean(source.externalUrl);
  }
  return source.menuType === MenuTypeEnum.MENU && Boolean(source.component || source.path);
}

function findMenuByPath(menus: ShellRouteMenu[], path: string): ShellRouteMenu | undefined {
  for (const menu of menus) {
    if (menu.path === path) {
      return menu;
    }
    const child = findMenuByPath(menu.children || [], path);
    if (child) {
      return child;
    }
  }
  return undefined;
}

function findTopByPath(menus: ShellRouteMenu[], path: string): ShellRouteMenu | undefined {
  return menus.find(menu => containsMenuPath(menu, path));
}

export function containsMenuPath(menu: ShellMenuTreeNode, path: string): boolean {
  if (path === menu.path || path.startsWith(`${menu.path}/`)) {
    return true;
  }
  return Boolean(menu.children?.some(child => containsMenuPath(child, path)));
}
