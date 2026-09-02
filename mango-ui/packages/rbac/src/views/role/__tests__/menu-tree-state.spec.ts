import { describe, expect, it } from 'vitest';
import { assignedMenuIds, authorizedLeafMenuIds, type MenuTreeNode } from '../menu-tree-state';

const menus: MenuTreeNode[] = [
  {
    menuId: '1',
    children: [
      {
        menuId: '10',
        children: [{ menuId: '100' }, { menuId: '101' }],
      },
      { menuId: '11' },
    ],
  },
];

describe('role menu tree state', () => {
  it('hydrates only authorized leaves so ancestors become half checked without selecting siblings', () => {
    expect(authorizedLeafMenuIds(menus, ['1', '10', '100'])).toEqual(['100']);
  });

  it('keeps string ID semantics and removes duplicate authorized leaf IDs', () => {
    expect(authorizedLeafMenuIds(menus, [100, '100', '101', '999'])).toEqual(['100', '101']);
  });

  it('submits the stable deduplicated union of checked and half-checked nodes', () => {
    expect(assignedMenuIds(['100', 101], ['10', '1', 101])).toEqual(['100', '101', '10', '1']);
  });
});
