import {
  createMangoAdminApp,
  type MangoAdminAppInstance,
  type MangoAdminShellOptions,
} from '@mango/admin-shell';
import './style.css';
import type { ShellMenu } from '@mango/admin-shell';
import type { MangoCapabilityManifest, MangoCapabilityMenu, MangoPageRegistry, MangoShellPageLoaders } from '@mango/admin-pages/core';
import { registerMangoAdminPreset, type MangoAdminPreset } from './presets';

export interface MangoAdminOptions extends MangoAdminShellOptions {
  preset?: MangoAdminPreset;
  capabilities?: MangoCapabilityManifest[];
  registries?: MangoPageRegistry[];
  shellPages?: MangoShellPageLoaders;
}

export type MangoAdminInstance = MangoAdminAppInstance;

export function createMangoAdmin(options: MangoAdminOptions = {}): MangoAdminInstance {
  const {
    preset,
    capabilities,
    registries,
  shellPages,
  ...shellOptions
  } = options;

  const presetResolution = registerMangoAdminPreset({
    preset,
    capabilities,
    registries,
    shellPages,
  });

  return createMangoAdminApp({
    ...shellOptions,
    menu: {
      ...shellOptions.menu,
      capabilityMenus: [
        ...toShellMenus(presetResolution.capabilities),
        ...(shellOptions.menu?.capabilityMenus || []),
      ],
      businessMenus: shellOptions.menu?.businessMenus || [],
    },
  });
}

export type {
  MangoCapabilityManifest,
  MangoPageRegistry,
  MangoShellPageLoaders,
} from '@mango/admin-pages/core';
export type {
  MangoAdminPreset,
  MangoAdminPresetOptions,
  MangoAdminPresetResolution,
} from './presets';
export {
  mangoDefaultCapabilities,
} from '@mango/admin-pages/defaults';
export {
  registerMangoAdminPreset,
  resolveMangoAdminPreset,
} from './presets';
export {
  getMangoAdminShellOptions,
  type MangoAdminShellOptions,
} from '@mango/admin-shell';
export {
  getLastShellMenuMergeReport,
  mergeShellMenus,
  type ShellMenuMergeReport,
} from '@mango/admin-shell';
export type {
  MangoAdminMenu,
  ShellMenu,
  ShellMenuHost,
  ShellMenuTreeNode,
  ShellRouteMenu,
} from './menu';

function toShellMenus(capabilities: MangoCapabilityManifest[]): ShellMenu[] {
  const groups = new Map<string, ShellMenu>();
  for (const capability of capabilities) {
    for (const menu of capability.menus || []) {
      const group = groups.get(capability.capabilityCode) || createCapabilityGroupMenu(capability);
      group.children = group.children || [];
      group.children.push(toShellMenu(capability, menu, group.menuId));
      groups.set(capability.capabilityCode, group);
    }
  }
  return [...groups.values()];
}

function createCapabilityGroupMenu(capability: MangoCapabilityManifest): ShellMenu {
  return {
    appCode: 'internal-admin',
    moduleCode: capability.moduleCode,
    menuId: `capability:${capability.capabilityCode}`,
    menuName: capability.capabilityName,
    menuCode: `capability:${capability.capabilityCode}`,
    parentId: 0,
    menuType: 1,
    path: `/capabilities/${capability.capabilityCode}`,
    icon: 'Menu',
    sort: 9000,
    status: 1,
    visible: 1,
    pageType: 'LOCAL_ROUTE',
    children: [],
  };
}

function toShellMenu(capability: MangoCapabilityManifest, menu: MangoCapabilityMenu, parentId: string): ShellMenu {
  return {
    appCode: 'internal-admin',
    moduleCode: menu.moduleCode || capability.moduleCode,
    menuId: `capability:${menu.menuCode}`,
    menuName: menu.menuCode,
    menuCode: menu.menuCode,
    parentId,
    menuType: 2,
    path: `/${menu.component || menu.menuCode.replace(/:/g, '/')}`,
    component: menu.component,
    sort: 0,
    status: 1,
    visible: 1,
    pageType: 'LOCAL_ROUTE',
    permissions: menu.permissions || [],
    meta: {
      permissions: menu.permissions || [],
    },
  };
}
