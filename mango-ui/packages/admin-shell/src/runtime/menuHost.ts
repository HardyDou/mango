import { computed, ref, type ComputedRef, type Ref } from 'vue';
import { get, type RequestConfig } from '@mango/common/utils/request';
import { DEV_COMPONENT_DEMO_PAGES, DEV_COMPONENT_DEMO_REDIRECT } from '@mango/admin-pages';
import type { MangoMenuPageType } from '@mango/app-runtime';
import type { RouteRecordRaw } from 'vue-router';
import {
  containsMenuPath,
  findMenuByPath,
  findTopMenuByPath,
  resolveFirstMenu as resolveFirstMenuNode,
  type MangoMenuTreeNode,
} from '@mango/common/utils/menuTree';
import { getMangoAdminShellOptions } from '../config';
import { mergeShellMenus, type ShellMenuMergeReport } from './menuMerge';

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
  permissions?: string[];
  redirect?: string;
  meta?: Record<string, any>;
  children?: ShellMenu[];
}

export type ShellRouteMenu = RouteRecordRaw & MangoMenuTreeNode & {
  sourceMenu: ShellMenu;
  children?: ShellRouteMenu[];
};

export type ShellMenuTreeNode = MangoMenuTreeNode;

export interface ShellMenuHost {
  menuLoading: Ref<boolean>;
  menus: Ref<ShellRouteMenu[]>;
  topMenus: ComputedRef<ShellRouteMenu[]>;
  sideMenus: ComputedRef<ShellRouteMenu[]>;
  activeTopPath: Ref<string>;
  activeMenuPath: Ref<string>;
  activeMenu: ComputedRef<ShellRouteMenu | undefined>;
  loadMenus: () => Promise<ShellRouteMenu | undefined>;
  selectTop: (menu: ShellRouteMenu) => ShellRouteMenu | undefined;
  selectMenu: (path: string) => ShellRouteMenu | undefined;
}

let lastShellMenuMergeReport: ShellMenuMergeReport | undefined;

export function getLastShellMenuMergeReport() {
  return lastShellMenuMergeReport;
}

export function useMenuHost(): ShellMenuHost {
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
      const options = getMangoAdminShellOptions();
      const appCode = options.menu?.appCode || options.login?.defaults?.appCode || 'internal-admin';
      const response = options.menu?.loader
        ? await options.menu.loader({ appCode })
        : await get('/authorization/menus/user', {
          params: { fmt: 'tree', appCode },
        } as RequestConfig) as ShellMenu[];
      const merged = mergeShellMenus({
        backendMenus: response || [],
        capabilityMenus: options.menu?.capabilityMenus || [],
        businessMenus: options.menu?.businessMenus || [],
        permissions: options.menu?.permissions,
      });
      lastShellMenuMergeReport = merged.report;
      options.menu?.onMergeReport?.(merged.report);
      menus.value = [
        createHomeRouteMenu(),
        ...filterMenuForRoute(merged.menus).map(toShellRouteMenu),
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
    const top = findTopMenuByPath(menus.value, menu.path);
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

function filterMenuForRoute(menus: ShellMenu[]): ShellMenu[] {
  return menus
    .filter(menu => menu.menuType !== MenuTypeEnum.BUTTON)
    .map(menu => ({
      ...menu,
      children: menu.children ? filterMenuForRoute(menu.children) : [],
    }));
}

function toShellRouteMenu(menu: ShellMenu): ShellRouteMenu {
  const moduleCode = menu.moduleCode || inferModuleCode(menu.component, menu.path);
  const sourceMenu = {
    ...menu,
    moduleCode,
  };
  const route = {
    path: menu.path,
    name: menu.menuCode || menu.menuName,
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
  } as ShellRouteMenu;
  if (menu.redirect) {
    route.redirect = menu.redirect;
  }
  return route;
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

export function createNotFoundRouteMenu(path = '/404'): ShellRouteMenu {
  return toShellRouteMenu({
    appCode: 'internal-admin',
    moduleCode: 'mango-shell',
    menuId: 'shell-not-found',
    menuName: '404',
    menuCode: 'shell:not-found',
    parentId: 0,
    menuType: MenuTypeEnum.MENU,
    path,
    component: 'error/404',
    icon: 'Warning',
    sort: -999,
    status: 1,
    visible: 0,
    keepAlive: 0,
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
      redirect: DEV_COMPONENT_DEMO_REDIRECT,
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
          redirect: DEV_COMPONENT_DEMO_REDIRECT,
          children: createComponentDemoMenus(),
        },
      ],
    }),
  ];
}

function createComponentDemoMenus(): ShellMenu[] {
  return DEV_COMPONENT_DEMO_PAGES.map((page: Pick<ShellMenu, 'menuId' | 'menuName' | 'menuCode' | 'path' | 'component' | 'icon' | 'sort'>) => ({
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

export { containsMenuPath };

export function resolveFirstMenu(menu?: ShellRouteMenu): ShellRouteMenu | undefined {
  return resolveFirstMenuNode(menu, isRunnableMenu);
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
