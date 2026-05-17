import type { MangoMenu, MangoMenuPageType } from '@mango/app-runtime';

export enum MenuTypeEnum {
  DIRECTORY = 1,
  MENU = 2,
  BUTTON = 3,
}

export interface RbacMenu extends MangoMenu {
  menuId?: string | number;
  moduleCode?: string;
  parentId?: string | number;
  menuType?: MenuTypeEnum;
  menuName: string;
  menuCode: string;
  path?: string;
  pageType?: MangoMenuPageType;
  component?: string;
  icon?: string;
  sort?: number;
  status?: number;
  visible?: number;
  children?: RbacMenu[];
}
