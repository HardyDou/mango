export interface ActiveMenuNode {
  path?: string;
  meta?: {
    activePaths?: string[];
    isHide?: boolean;
    [key: string]: unknown;
  };
  children?: ActiveMenuNode[];
}

export function filterMenusForNavigation<T extends ActiveMenuNode>(menus: T[]): T[] {
  return menus
    .filter((menu) => !menu.meta?.isHide)
    .map((menu) => {
      const activePaths = collectHiddenDescendantPaths(menu.children || []);
      return {
        ...menu,
        meta: activePaths.length > 0 ? { ...menu.meta, activePaths } : menu.meta,
        children: menu.children ? filterMenusForNavigation(menu.children as T[]) : menu.children,
      } as T;
    });
}

function collectHiddenDescendantPaths(menus: ActiveMenuNode[]): string[] {
  return menus.flatMap((menu) => [
    ...(menu.meta?.isHide && menu.path ? [menu.path] : []),
    ...collectHiddenDescendantPaths(menu.children || []),
  ]);
}

export function resolveActiveMenuPaths<T extends ActiveMenuNode>(menus: T[], path: string): string[] {
  let activePaths: string[] = [];
  let matchedPathLength = -1;

  const visit = (menu: T, ancestorPaths: string[]) => {
    const menuPath = menu.path;
    const matchesMenuPath = Boolean(menuPath && isSameOrChildPath(menuPath, path));
    const matchesHiddenDescendant = Boolean(
      menu.meta?.activePaths?.some((activePath) => isSameOrChildPath(activePath, path)),
    );
    const menuPaths = menuPath ? [...ancestorPaths, menuPath] : ancestorPaths;

    if (menuPath && (matchesMenuPath || matchesHiddenDescendant) && menuPath.length >= matchedPathLength) {
      activePaths = menuPaths;
      matchedPathLength = menuPath.length;
    }

    for (const child of (menu.children || []) as T[]) {
      visit(child, menuPaths);
    }
  };

  for (const menu of menus) {
    visit(menu, []);
  }
  return activePaths;
}

export function resolveActiveMenuPath<T extends ActiveMenuNode>(menus: T[], path: string): string {
  const activePaths = resolveActiveMenuPaths(menus, path);
  return activePaths[activePaths.length - 1] || '';
}

function isSameOrChildPath(menuPath: string, path: string): boolean {
  return path === menuPath || path.startsWith(`${menuPath}/`);
}
