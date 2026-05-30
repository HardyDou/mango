import { del, get, post, put } from '@mango/common/utils/request';
import type { ApiId } from '@mango/api-schema';
import type { MangoMenuPageType } from './runtimeTypes';

/**
 * 菜单类型枚举
 */
export enum MenuTypeEnum {
  DIRECTORY = 1,
  MENU = 2,
  BUTTON = 3,
}

export type MenuFormat = 'list' | 'tree';

/**
 * 验证码配置
 */
export interface CaptchaConfig {
  type: 'ARITHMETIC' | 'BLOCK_PUZZLE' | 'SMS' | 'EMAIL';
  required: boolean;
}

/**
 * 菜单元数据
 */
export interface MenuMeta {
  title?: string;
  icon?: string;
  isAffix?: boolean;
  permissions?: string[];
  keepAlive?: boolean;
  embedded?: boolean;
  captcha?: CaptchaConfig;
  [key: string]: any;
}

/**
 * 菜单VO
 */
export interface SysMenuVO {
  menuId: ApiId;
  appCode?: string;
  moduleCode?: string;
  parentId: ApiId;
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
  permissions?: string;
  meta?: MenuMeta;
  children?: SysMenuVO[];
}

/**
 * 菜单响应
 */
export interface MenuResponse {
  menus: SysMenuVO[];
  currentStorage?: string;
}

export interface MenuQuery {
  appCode?: string;
  moduleCode?: string;
  fmt?: MenuFormat;
  type?: number;
  parentId?: ApiId;
  menuName?: string;
  status?: number;
}

export interface MenuDetailQuery {
  menuId: ApiId;
}

type MenuPayload = Pick<
  SysMenuVO,
  | 'menuId'
  | 'parentId'
  | 'menuType'
  | 'menuName'
  | 'menuCode'
  | 'path'
  | 'pageType'
  | 'icon'
  | 'sort'
  | 'status'
  | 'visible'
  | 'keepAlive'
  | 'embedded'
  | 'redirect'
  | 'permissions'
  | 'component'
> & {
  appCode?: string;
  moduleCode?: string;
  externalUrl?: string;
};

function toBackendPayload(data: Partial<SysMenuVO> & { appCode?: string }): Partial<MenuPayload> {
  return {
    menuId: data.menuId,
    appCode: data.appCode,
    moduleCode: data.moduleCode,
    parentId: data.parentId ?? '0',
    menuType: data.menuType,
    menuName: data.menuName,
    menuCode: data.menuCode,
    path: data.path,
    pageType: data.pageType,
    icon: data.icon,
    sort: data.sort ?? 0,
    status: data.status ?? 1,
    visible: data.visible ?? 1,
    keepAlive: data.keepAlive ?? 0,
    embedded: data.embedded ?? 0,
    redirect: data.redirect,
    permissions: data.permissions,
    component: data.component,
    externalUrl: data.externalUrl,
  };
}

/**
 * 菜单API
 * 菜单归属授权域，对接后端 /authorization/menus。
 */
export const menuApi = {
  /**
   * 查询当前用户菜单
   */
  getUserMenus: (params: MenuQuery = {}) =>
    get<SysMenuVO[]>('/authorization/menus/user', {
      params: { fmt: 'tree', ...params },
    }).then((menus) => ({
      menus,
      currentStorage: 'backend',
    })),

  /**
   * 查询菜单资源列表
   */
  getMenus: (params: MenuQuery = {}) =>
    get<SysMenuVO[]>('/authorization/menus', {
      params: { fmt: 'list', ...params },
    }),

  /**
   * 查询菜单资源树
   */
  getMenuTree: (params: MenuQuery = {}) =>
    get<SysMenuVO[]>('/authorization/menus', {
      params: { fmt: 'tree', ...params },
    }),

  /**
   * 查询菜单详情
   */
  getMenuDetail: (menuId: ApiId) =>
    get<SysMenuVO>('/authorization/menus/detail', { params: { menuId } }),

  /**
   * 新增菜单
   */
  createMenu: (data: Partial<SysMenuVO>) =>
    post('/authorization/menus', toBackendPayload(data)),

  /**
   * 修改菜单
   */
  updateMenu: (data: Partial<SysMenuVO>) =>
    put('/authorization/menus', toBackendPayload(data)),

  /**
   * 删除菜单
   */
  deleteMenu: (menuId: ApiId) =>
    del('/authorization/menus', { params: { menuId } }),
};
