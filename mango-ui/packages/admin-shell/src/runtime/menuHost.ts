import { computed, ref } from 'vue';
import { get } from '@mango/common/utils/request';
import { getRegisteredPageRoutes } from '@mango/admin-pages/core';
import { getMangoDevComponentPages } from '@mango/admin-pages/dev-pages';
import { DEV_COMPONENT_DEMO_REDIRECT } from '@mango/admin-pages/dev-component-pages';
import {
  isMangoAdminFeatureEnabled,
  resolveMangoAdminFeatures,
  resolveMangoAdminModuleFeature,
  type MangoAdminFeatureCode,
} from '@mango/admin-pages/features';
import type { MangoMenuPageType } from '@mango/app-runtime';
import type { RouteRecordRaw } from 'vue-router';
import { getMangoAdminShellOptions, type MangoAdminShellDevCenterOptions } from '../config';
import {
  containsMenuPath,
  findMenuByPath,
  findTopMenuByPath,
  resolveFirstMenu as resolveFirstMenuNode,
  type MangoMenuTreeNode,
} from '@mango/common/utils/menuTree';

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
  meta?: ShellMenuMeta;
  children?: ShellMenu[];
}

export type ShellMenuSource = 'backend' | 'shell' | 'custom' | 'fallback';

export interface ShellMenuMeta {
  source?: ShellMenuSource;
  diagnostics?: string[];
  [key: string]: any;
}

export interface ShellRouteMenu extends RouteRecordRaw {
  sourceMenu: ShellMenu;
  children?: ShellRouteMenu[];
}

export type ShellMenuTreeNode = MangoMenuTreeNode;

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
      await ensureFeatureRegistrars();
      const response = await get<ShellMenu[]>('/authorization/menus/user', {
        params: { fmt: 'tree', appCode: 'internal-admin' },
      });
      menus.value = [
        withMenuSource(createHomeRouteMenu(), 'shell'),
        ...filterMenuForRoute(response || []).map(menu => toShellRouteMenu(withMenuSource(menu, 'backend'))),
        ...createRegisteredHiddenRouteMenus(),
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

let featureRegistrarsPromise: Promise<void> | undefined;

function ensureFeatureRegistrars() {
  if (!featureRegistrarsPromise) {
    featureRegistrarsPromise = Promise.resolve().then(async () => {
      for (const registrar of getMangoAdminShellOptions().featureRegistrars || []) {
        await registrar();
      }
    });
  }
  return featureRegistrarsPromise;
}

function filterMenuForRoute(menus: ShellMenu[]): ShellMenu[] {
  const enabledFeatures = resolveMangoAdminFeatures(getMangoAdminShellOptions().features);
  return filterMenuForRouteByFeatures(menus, enabledFeatures);
}

export function filterMenuForRouteByFeatures(menus: ShellMenu[], enabledFeatures: Set<MangoAdminFeatureCode>): ShellMenu[] {
  return menus
    .filter(menu => menu.menuType !== MenuTypeEnum.BUTTON)
    .filter(menu => {
      const moduleCode = menu.moduleCode || inferModuleCode(menu.component, menu.path);
      return isMangoAdminFeatureEnabled(enabledFeatures, resolveMangoAdminModuleFeature(moduleCode));
    })
    .map(menu => ({
      ...menu,
      children: menu.children ? filterMenuForRouteByFeatures(menu.children, enabledFeatures) : [],
    }))
    .filter(menu => menu.menuType === MenuTypeEnum.MENU || (menu.children && menu.children.length > 0));
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
  if (target.includes('notice/')) {
    return 'mango-notice';
  }
  if (target.includes('file/')) {
    return 'mango-file';
  }
  if (target.includes('template/')) {
    return 'mango-template';
  }
  if (target.includes('numgen/')) {
    return 'mango-numgen';
  }
  if (target.includes('calendar/')) {
    return 'mango-calendar';
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
  return toShellRouteMenu(withMenuSource({
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
  }, 'fallback'));
}

export function shouldShowDevCenter(options: MangoAdminShellDevCenterOptions = getMangoAdminShellOptions().devCenter || {}) {
  if (typeof options.visible === 'boolean') {
    return options.visible;
  }
  const deployEnv = normalizeDeployEnv(options.deployEnv || import.meta.env.VITE_MANGO_DEPLOY_ENV || import.meta.env.MODE);
  if (isProductionLikeEnv(deployEnv)) {
    return false;
  }
  return import.meta.env.DEV || deployEnv === 'dev' || deployEnv === 'test';
}

function normalizeDeployEnv(value?: string) {
  return (value || '').trim().toLowerCase();
}

function isProductionLikeEnv(value: string) {
  return value === 'prod' || value === 'prd' || value === 'production';
}

function createDevRouteMenus(): ShellRouteMenu[] {
  if (!shouldShowDevCenter()) {
    return [];
  }
  const componentMenus = createComponentDemoMenus();
  const redirect = componentMenus[0]?.path || DEV_COMPONENT_DEMO_REDIRECT;

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
      meta: { source: 'shell' },
      pageType: 'LOCAL_ROUTE',
      redirect,
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
          meta: { source: 'shell' },
          pageType: 'LOCAL_ROUTE',
          redirect,
          children: componentMenus,
        },
      ],
    }),
  ];
}

function createRegisteredHiddenRouteMenus(): ShellRouteMenu[] {
  const enabledFeatures = resolveMangoAdminFeatures(getMangoAdminShellOptions().features);
  const moduleCodes = Array.from(new Set(Array.from(enabledFeatures).map(feature => FEATURE_MODULE_MAP[feature]).filter(Boolean)));
  return getRegisteredPageRoutes(moduleCodes)
    .map((route, index) => toShellRouteMenu(withMenuSource({
      appCode: 'internal-admin',
      moduleCode: route.moduleCode,
      menuId: `registered-hidden:${route.moduleCode}:${route.path}`,
      menuName: route.menuName || route.path,
      menuCode: route.menuCode || `registered-hidden:${route.moduleCode}:${route.path}`,
      parentId: 0,
      menuType: MenuTypeEnum.MENU,
      path: route.path,
      component: route.component,
      icon: route.icon,
      sort: route.sort ?? (900000 + index),
      status: 1,
      visible: route.visible ?? 0,
      keepAlive: route.keepAlive ?? 0,
      pageType: 'LOCAL_ROUTE',
      children: [],
    }, 'shell')));
}

function createComponentDemoMenus(): ShellMenu[] {
  const enabledFeatures = resolveMangoAdminFeatures(getMangoAdminShellOptions().features);
  return getMangoDevComponentPages()
    .filter(page => !page.feature || enabledFeatures.has(page.feature))
    .map(page => ({
      ...page,
      appCode: 'internal-admin',
      moduleCode: 'mango-shell',
      parentId: 'shell-develop-components',
      menuType: MenuTypeEnum.MENU,
      status: 1,
      visible: 1,
      meta: { source: 'shell' },
      keepAlive: 1,
      pageType: 'LOCAL_ROUTE',
      children: [],
    }));
}

const FEATURE_MODULE_MAP = {
  authorization: 'mango-authorization',
  system: 'mango-system',
  workflow: 'mango-workflow',
  file: 'mango-file',
  template: 'mango-template',
  notice: 'mango-notice',
  numgen: 'mango-numgen',
  calendar: 'mango-calendar',
} as const;

function withMenuSource(menu: ShellMenu, source: ShellMenuSource): ShellMenu {
  return {
    ...menu,
    meta: {
      ...(menu.meta || {}),
      source: menu.meta?.source || source,
    },
    children: menu.children?.map(child => withMenuSource(child, source)),
  };
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
  return toShellRouteMenu(withMenuSource({
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
  }, 'shell'));
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

export function findUnexpectedTopLevelMenus(uiTopMenus: string[], backendTopMenus: string[], allowedShellMenus: string[]) {
  return uiTopMenus.filter(name => !backendTopMenus.includes(name) && !allowedShellMenus.includes(name));
}
