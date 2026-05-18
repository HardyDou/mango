export interface MangoMenuTreeNode {
  path?: string;
  redirect?: string;
  children?: MangoMenuTreeNode[];
  meta?: {
    isHide?: boolean;
    [key: string]: unknown;
  };
}

export type MangoMenuRunnablePredicate<T extends MangoMenuTreeNode> = (menu: T) => boolean;

export function containsMenuPath<T extends MangoMenuTreeNode>(menu: T, path: string): boolean {
  if (isSameOrChildPath(menu.path, path)) {
    return true;
  }
  return Boolean(menu.children?.some(child => containsMenuPath(child, path)));
}

export function findMenuByPath<T extends MangoMenuTreeNode>(menus: T[], path: string): T | undefined {
  for (const menu of menus) {
    if (menu.path === path) {
      return menu;
    }
    const child = findMenuByPath((menu.children || []) as T[], path);
    if (child) {
      return child;
    }
  }
  return undefined;
}

export function findTopMenuByPath<T extends MangoMenuTreeNode>(menus: T[], path: string): T | undefined {
  return menus.find(menu => containsMenuPath(menu, path));
}

export function resolveFirstMenu<T extends MangoMenuTreeNode>(
  menu?: T,
  isRunnable: MangoMenuRunnablePredicate<T> = defaultIsRunnableMenu
): T | undefined {
  if (!menu) {
    return undefined;
  }
  if (isRunnable(menu)) {
    return menu;
  }
  for (const child of (menu.children || []) as T[]) {
    const first = resolveFirstMenu(child, isRunnable);
    if (first) {
      return first;
    }
  }
  return undefined;
}

export function resolveFirstMenuPath<T extends MangoMenuTreeNode>(
  menu?: T,
  isRunnable?: MangoMenuRunnablePredicate<T>
): string {
  if (!menu) {
    return '';
  }
  if (typeof menu.redirect === 'string' && menu.redirect) {
    return menu.redirect;
  }
  return resolveFirstMenu(menu, isRunnable)?.path || menu.path || '';
}

export function filterVisibleMenus<T extends MangoMenuTreeNode>(menus: T[]): T[] {
  return menus.filter(menu => !menu.meta?.isHide);
}

function defaultIsRunnableMenu<T extends MangoMenuTreeNode>(menu: T): boolean {
  return Boolean(menu.path);
}

function isSameOrChildPath(menuPath: string | undefined, path: string): boolean {
  if (!menuPath) {
    return false;
  }
  return path === menuPath || path.startsWith(`${menuPath}/`);
}
