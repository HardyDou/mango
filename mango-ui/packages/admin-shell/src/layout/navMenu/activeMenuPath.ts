export interface ActiveMenuNode {
  path?: string;
  children?: ActiveMenuNode[];
}

export function resolveActiveMenuPath<T extends ActiveMenuNode>(menus: T[], path: string): string {
  let activePath = '';

  const visit = (menu: T) => {
    if (menu.path && isSameOrChildPath(menu.path, path) && menu.path.length >= activePath.length) {
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
