/**
 * 菜单加载器
 * @description 从后端 API 加载菜单，并与前端配置合并
 * 支持前端模式（纯静态）和后端模式（API + 前端配置合并）
 */
import type { RouteRecordRaw } from 'vue-router';
import menuJson from './menu.json';
import { menuApi, type SysMenuVO } from '@/api/admin/menu';
import { componentsMap } from './componentsMap';

/**
 * 验证码配置
 */
export interface CaptchaConfig {
  type: 'ARITHMETIC' | 'BLOCK_PUZZLE' | 'SMS' | 'EMAIL';
  required: boolean;
}

/**
 * 菜单配置项（JSON 格式）
 */
export interface MenuConfigItem {
  path: string;
  name: string;
  meta?: {
    title?: string;
    icon?: string;
    isAffix?: boolean;
    permissions?: string[];
    keepAlive?: boolean;
    embedded?: boolean;
    isHide?: boolean;
    captcha?: CaptchaConfig;
    [key: string]: any;
  };
  component?: string;
  redirect?: string;
  children?: MenuConfigItem[];
}

/**
 * 过滤菜单，移除按钮类型（menuType === 3），用于左侧导航
 */
function filterMenuForNav(menus: SysMenuVO[]): SysMenuVO[] {
  return menus
    .filter(menu => menu.menuType !== 3)
    .map(menu => ({
      ...menu,
      children: menu.children ? filterMenuForNav(menu.children) : [],
    })) as SysMenuVO[];
}

function filterDevOnlyMenus(menus: MenuConfigItem[]): MenuConfigItem[] {
  return menus
    .filter(menu => import.meta.env.DEV || !menu.meta?.devOnly)
    .map(menu => ({
      ...menu,
      children: menu.children ? filterDevOnlyMenus(menu.children) : undefined,
    }));
}

function createHomeMenuItem(): MenuItem {
  return {
    path: '/home',
    name: 'Home',
    meta: {
      title: '首页',
      icon: 'HomeFilled',
      isAffix: true,
      keepAlive: true,
    },
    component: () => import('@/views/home/index.vue'),
  };
}

/**
 * 菜单元数据
 */
export interface MenuMeta {
  title: string;
  icon: string;
  isAffix?: boolean;
  permissions?: string[];
  keepAlive?: boolean;
  embedded?: boolean;
  isHide?: boolean;
  captcha?: CaptchaConfig;
}

/**
 * 菜单项
 */
export interface MenuItem {
  path: string;
  name: string;
  meta: MenuMeta;
  component?: () => Promise<any>;
  redirect?: string;
  children?: MenuItem[];
}

const appViewModules = import.meta.glob('../views/**/*.vue');
const appViewMap = new Map(
  Object.entries(appViewModules).map(([key, loader]) => [
    key.replace('../views/', 'views/').replace(/\.vue$/, ''),
    loader,
  ])
);

export class MenuLoader {
  private static instance: MenuLoader;
  private homeMenuItem: MenuItem;
  private frontendMenuItems: MenuItem[] = [];
  private backendMenuItems: MenuItem[] = [];
  private mergedMenuItems: MenuItem[] = [];
  private backendMode = false;

  private constructor() {
    // 初始化时加载前端配置。开发中心只在开发环境展示，不进入测试/生产菜单。
    this.homeMenuItem = createHomeMenuItem();
    this.frontendMenuItems = this.jsonToMenuItem(filterDevOnlyMenus(menuJson as MenuConfigItem[]));
  }

  /**
   * 获取单例实例
   */
  static getInstance(): MenuLoader {
    if (!MenuLoader.instance) {
      MenuLoader.instance = new MenuLoader();
    }
    return MenuLoader.instance;
  }

  /**
   * 将 JSON 配置转换为菜单项
   */
  private jsonToMenuItem(config: MenuConfigItem[]): MenuItem[] {
    return config.map((item) => this.convertToMenuItem(item));
  }

  /**
   * 转换单个菜单项
   */
  private convertToMenuItem(item: MenuConfigItem): MenuItem {
    const menuItem: MenuItem = {
      path: item.path,
      name: item.name,
      meta: {
        title: item.meta?.title || item.name,
        icon: item.meta?.icon || '',
        isAffix: item.meta?.isAffix,
        permissions: item.meta?.permissions,
        keepAlive: item.meta?.keepAlive,
        embedded: item.meta?.embedded,
        isHide: item.meta?.isHide,
        captcha: item.meta?.captcha,
      },
    };

    if (item.component) {
      menuItem.component = this.resolveComponent(item.component);
    }

    if (item.redirect) {
      menuItem.redirect = item.redirect;
    }

    if (item.children && item.children.length > 0) {
      menuItem.children = item.children.map((child) => this.convertToMenuItem(child));
    }

    return menuItem;
  }

  /**
   * 解析组件路径为动态导入
   */
  private resolveComponent(componentPath: string): () => Promise<any> {
    const cleanPath = componentPath
      .replace(/^@\//, '')
      .replace(/^\//, '')
      .replace(/^src\//, '')
      .replace(/\.vue$/, '');

    if (componentsMap[cleanPath]) {
      return componentsMap[cleanPath];
    }

    const appViewLoader = appViewMap.get(cleanPath);
    if (appViewLoader) {
      return appViewLoader as () => Promise<any>;
    }

    console.warn(`[MenuLoader] 未找到菜单组件，使用 404 页面: ${componentPath}`);
    return () => import('@/views/error/404.vue');
  }

  /**
   * 后端菜单结构转换为前端配置格式
   */
  private backendToConfig(menus: SysMenuVO[]): MenuConfigItem[] {
    return menus.map((menu) => ({
      path: menu.path,
      name: menu.menuCode || menu.menuName,
      meta: {
        title: menu.menuName,
        icon: menu.icon,
        isAffix: menu.meta?.isAffix,
        permissions: menu.meta?.permissions,
        keepAlive: menu.keepAlive === 1,
        embedded: menu.embedded === 1,
        captcha: menu.meta?.captcha,
      },
      component: menu.component,
      redirect: menu.redirect,
      children: menu.children?.length > 0 ? this.backendToConfig(menu.children) : undefined,
    }));
  }

  /**
   * 从后端加载菜单配置
   * @description 直接调用用户菜单接口，与前端配置合并
   * 合并策略：前端配置（组件库、示例页面）在前，后端配置（系统管理）在后
   */
  async loadFromBackend(): Promise<MenuItem[]> {
    const response = await menuApi.getUserMenus({ fmt: 'tree' });
    const backendMenus = filterMenuForNav(response.menus || []);
    if (!backendMenus || backendMenus.length === 0) {
      this.resetBackendCache();
      throw new Error('后端菜单数据为空');
    }

    // 将后端菜单转换为配置格式
    const backendConfig = this.backendToConfig(backendMenus);
    this.backendMenuItems = this.jsonToMenuItem(backendConfig);

    // 合并：固定首页在前，后端业务菜单居中，本地开发菜单在最后。
    // 去重：后端或本地追加菜单以后配置 /home 时，以固定首页为准。
    const localPaths = new Set([this.homeMenuItem.path, ...this.frontendMenuItems.map(m => m.path)]);
    const uniqueBackendItems = this.backendMenuItems.filter(m => !localPaths.has(m.path));
    const uniqueFrontendItems = this.frontendMenuItems.filter(m => m.path !== this.homeMenuItem.path);
    this.mergedMenuItems = [this.homeMenuItem, ...uniqueBackendItems, ...uniqueFrontendItems];

    this.backendMode = true;
    if (import.meta.env.DEV) console.log('[MenuLoader] 从后端加载菜单成功，共', this.mergedMenuItems.length, '个菜单（前端:', this.frontendMenuItems.length, '后端:', this.backendMenuItems.length, '）');
    return this.mergedMenuItems;
  }

  /**
   * 重置后端菜单缓存。
   * 登录用户、机构或应用上下文变化时必须重新加载后端菜单，避免沿用上一身份的导航权限。
   */
  resetBackendCache(): void {
    this.backendMenuItems = [];
    this.mergedMenuItems = [];
    this.backendMode = false;
  }

  /**
   * 获取前端菜单配置（不含后端）
   */
  getFrontendMenuItems(): MenuItem[] {
    return [this.homeMenuItem, ...this.frontendMenuItems.filter(m => m.path !== this.homeMenuItem.path)];
  }

  /**
   * 获取合并后的菜单配置
   */
  getMenuConfig(): MenuItem[] {
    return this.mergedMenuItems.length > 0 ? this.mergedMenuItems : this.getFrontendMenuItems();
  }

  /**
   * 获取静态路由配置
   */
  getStaticRoutes(): RouteRecordRaw[] {
    const menuItems = this.getMenuConfig();

    const routes: RouteRecordRaw[] = [
      {
        path: '/login',
        name: 'Login',
        component: () => import('@mango/auth').then(m => m.LoginView),
        meta: { title: '登录', isAffix: true },
      },
      {
        path: '/404',
        name: 'NotFound',
        component: () => import('@/views/error/404.vue'),
        meta: { title: '404', isAffix: true },
      },
      {
        path: '/401',
        name: 'NoPermission',
        component: () => import('@/views/error/401.vue'),
        meta: { title: '401', isAffix: true },
      },
      {
        path: '/',
        name: 'Layout',
        component: () => import('@/layout/index.vue'),
        redirect: '/home',
        children: menuItems.map((item) => this.menuItemToRoute(item)),
      },
    ];

    return routes;
  }

  /**
   * 将菜单项转换为路由记录
   */
  private menuItemToRoute(item: MenuItem): RouteRecordRaw {
    const route: RouteRecordRaw = {
      path: item.path,
      name: item.name,
      meta: item.meta,
    };

    if (item.component) {
      route.component = item.component;
    }

    if (item.redirect) {
      route.redirect = item.redirect;
    }

    if (item.children && item.children.length > 0) {
      (route as any).children = item.children.map((child) => this.menuItemToRoute(child));
    }

    return route;
  }

  /**
   * 生成后端路由模式需要的路由配置
   */
  generateBackEndRoutes(): RouteRecordRaw[] {
    const menuItems = this.getMenuConfig();

    function menuToRouteWithPermissions(item: MenuItem): RouteRecordRaw {
      const route: RouteRecordRaw = {
        path: item.path,
        name: item.name,
        meta: {
          ...item.meta,
          permissions: ['admin'],
        },
      };

      if (item.component) {
        route.component = item.component;
      }

      if (item.redirect) {
        route.redirect = item.redirect;
      }

      if (item.children && item.children.length > 0) {
        (route as any).children = item.children.map(menuToRouteWithPermissions);
      }

      return route;
    }

    return [
      {
        path: '/',
        name: 'Layout',
        meta: { title: '主布局', permissions: ['admin'] },
        children: menuItems.map(menuToRouteWithPermissions),
      },
    ] as RouteRecordRaw[];
  }

  /**
   * 是否为后端模式
   */
  isBackendMode(): boolean {
    return this.backendMode;
  }
}

/**
 * 导出单例便捷方法
 */
export const menuLoader = MenuLoader.getInstance();

/**
 * 获取静态路由
 */
export function getStaticRoutes(): RouteRecordRaw[] {
  return menuLoader.getStaticRoutes();
}

/**
 * 获取菜单配置
 */
export function getMenuConfig(): MenuItem[] {
  return menuLoader.getMenuConfig();
}

/**
 * 生成后端路由
 */
export function generateBackEndRoutes(): RouteRecordRaw[] {
  return menuLoader.generateBackEndRoutes();
}
