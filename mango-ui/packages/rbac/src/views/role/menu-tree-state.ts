import type { ApiId } from '@mango/api-schema';

export interface MenuTreeNode {
  menuId: ApiId;
  children?: MenuTreeNode[];
}

function normalizeMenuIds(ids: ApiId[]): string[] {
  return Array.from(new Set(ids.map(String)));
}

export function authorizedLeafMenuIds(menus: MenuTreeNode[], authorizedMenuIds: ApiId[]): string[] {
  const authorizedIds = new Set(normalizeMenuIds(authorizedMenuIds));
  const leafIds: string[] = [];

  const visit = (nodes: MenuTreeNode[]) => {
    nodes.forEach((node) => {
      const children = node.children || [];
      if (children.length > 0) {
        visit(children);
        return;
      }
      const menuId = String(node.menuId);
      if (authorizedIds.has(menuId)) {
        leafIds.push(menuId);
      }
    });
  };

  visit(menus);
  return normalizeMenuIds(leafIds);
}

export function assignedMenuIds(checkedMenuIds: ApiId[], halfCheckedMenuIds: ApiId[]): string[] {
  return normalizeMenuIds([...checkedMenuIds, ...halfCheckedMenuIds]);
}
