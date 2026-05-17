import { computed, ref } from 'vue';
import { get } from '@mango/common';
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
      menus.value = filterMenuForNav(response || []).map(toShellRouteMenu);
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
    sourceMenu: menu,
    children: menu.children?.map(toShellRouteMenu),
  };
}

export function resolveFirstMenu(menu?: ShellRouteMenu): ShellRouteMenu | undefined {
  if (!menu) {
    return undefined;
  }
  if (menu.children && menu.children.length > 0) {
    return resolveFirstMenu(menu.children[0]);
  }
  return menu;
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
  return menus.find(menu => path === menu.path || path.startsWith(`${menu.path}/`));
}
