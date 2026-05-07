import { get } from '@mango/common';

/**
 * 菜单类型枚举
 */
export enum MenuTypeEnum {
  DIRECTORY = 0,
  MENU = 1,
  BUTTON = 2,
}

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
  menuId: number;
  parentId: number;
  menuType: MenuTypeEnum;
  menuName: string;
  menuCode: string;
  path: string;
  component?: string;
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

/**
 * 菜单API
 * 菜单归属授权域，对接后端 /authorization/menus。
 */
export const menuApi = {
  /**
   * 获取当前用户的菜单
   * @param type 菜单类型 null=全部, 0=目录, 1=菜单, 2=按钮
   * @param parentId 父菜单ID，0=根节点
   */
  getUserMenus: (type?: number, parentId = 0) =>
    get<SysMenuVO[]>('/authorization/menus', { params: { type, parentId } }).then((menus) => ({
      menus,
      currentStorage: 'backend',
    })),

  /**
   * 获取所有菜单（管理员用）
   * @param parentId 父菜单ID
   * @param menuName 菜单名称（模糊搜索）
   */
  getTreeMenus: (parentId?: number, menuName?: string) =>
    get<SysMenuVO[]>('/authorization/menus/tree', { params: { parentId, menuName } }),
};
