import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { MangoAdminShellOptions } from '@mango/admin-shell';
import { getMangoAdminShellOptions } from '@mango/admin-shell';
import { createMangoAdmin } from '../index';
import type { MangoCapabilityManifest } from '@mango/admin-pages/core';

let shellOptions: MangoAdminShellOptions = {};

vi.mock('@mango/admin-shell', () => {
  return {
    configureMangoAdminShell: vi.fn((options: MangoAdminShellOptions) => {
      shellOptions = options;
    }),
    getMangoAdminShellOptions: vi.fn(() => shellOptions),
    createMangoAdminApp: vi.fn((options: MangoAdminShellOptions) => {
      shellOptions = options;
      return {
        app: {} as never,
        router: {} as never,
        mount: vi.fn(),
      };
    }),
    mergeShellMenus: vi.fn(),
    getLastShellMenuMergeReport: vi.fn(),
  };
});

describe('@mango/admin createMangoAdmin menu integration', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    shellOptions = {};
  });

  it('injects resolved capability menus into shell options', () => {
    createMangoAdmin({
      preset: 'custom',
      capabilities: [createCapability('orders')],
      menu: {
        businessMenus: [
          {
            menuId: 'business:reports',
            menuName: '业务报表',
            menuCode: 'business:reports',
            parentId: 0,
            menuType: 2,
            path: '/business/reports',
            component: 'business/reports/index',
            sort: 1,
            status: 1,
            visible: 1,
          },
        ],
      },
    });

    const options = getMangoAdminShellOptions();
    expect(options.menu?.capabilityMenus?.some(menu =>
      menu.children?.some(child => child.menuCode === 'orders:list'),
    )).toBe(true);
    expect(options.menu?.businessMenus?.some(menu => menu.menuCode === 'business:reports')).toBe(true);
  });
});

function createCapability(capabilityCode: string): MangoCapabilityManifest {
  return {
    moduleCode: `mango-${capabilityCode}`,
    packageName: `@mango/${capabilityCode}-admin`,
    capabilityCode,
    capabilityName: capabilityCode,
    requires: [],
    optional: [],
    backend: {
      moduleCode: `mango-${capabilityCode}`,
      menuSource: 'backend',
      requiredApis: [],
    },
    pages: [],
    menus: [
      {
        menuCode: `${capabilityCode}:list`,
        moduleCode: `mango-${capabilityCode}`,
        component: `${capabilityCode}/list/index`,
        permissions: [`${capabilityCode}:list`],
        source: 'capability',
      },
    ],
    permissions: [`${capabilityCode}:list`],
    styles: [],
    runtime: {
      modes: ['local'],
      defaultMode: 'local',
    },
    e2e: {
      smoke: [],
      screenshots: [],
      dataChecks: [],
    },
  };
}
