export interface ActiveMenuNode {
  path?: string;
  meta?: {
    activePaths?: string[];
    isHide?: boolean;
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

export function resolveActiveMenuPath<T extends ActiveMenuNode>(menus: T[], path: string): string {
  let activePath = '';

  const visit = (menu: T) => {
    const matchesMenuPath = menu.path && isSameOrChildPath(menu.path, path);
    const matchesHiddenDescendant = menu.meta?.activePaths?.some((activePath) => isSameOrChildPath(activePath, path));
    if (menu.path && (matchesMenuPath || matchesHiddenDescendant) && menu.path.length >= activePath.length) {
      activePath = menu.path;
    }
    for (const child of (menu.children || []) as T[]) {
      visit(child);
    }
  };

  for (const menu of menus) {
    visit(menu);
  }
  return activePath;
}

function isSameOrChildPath(menuPath: string, path: string): boolean {
  return path === menuPath || path.startsWith(`${menuPath}/`);
}
