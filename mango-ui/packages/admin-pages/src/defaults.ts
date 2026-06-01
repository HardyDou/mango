import { registerModulePages, registerShellPages, type MangoPageRegistry, type MangoShellPageLoaders } from './core';
import {
  isMangoAdminFeatureEnabled,
  resolveMangoAdminFeatures,
  resolveMangoAdminModuleFeature,
  type MangoAdminFeatures,
} from './features';

let shellPagesRegistered = false;
const registeredFeatures = new Set<string>();

export type RegisterDefaultAdminPagesOptions = {
  shellPages?: MangoShellPageLoaders;
  registries?: MangoPageRegistry[];
  features?: MangoAdminFeatures;
};

export function registerDefaultAdminPages(options: RegisterDefaultAdminPagesOptions = {}) {
  if (!shellPagesRegistered) {
    shellPagesRegistered = true;
    registerShellPages(options.shellPages || {});
  }

  const registries: MangoPageRegistry[] = [
    {
      moduleCode: 'mango-authorization',
      pages: {
        'profile/index': () => import('@mango/auth').then(m => m.ProfileView),
        'password/index': () => import('@mango/auth').then(m => m.PasswordView),
        'system/menu-package/index': () => import('@mango/rbac').then(m => m.MenuPackageView),
        'system/menu/index': () => import('@mango/rbac').then(m => m.MenuView),
        'system/role/index': () => import('@mango/rbac').then(m => m.RoleView),
        'system/user/index': () => import('@mango/rbac').then(m => m.UserView),
        'system/org/index': () => import('@mango/rbac').then(m => m.OrgView),
        'system/post/index': () => import('@mango/rbac').then(m => m.PostView),
        'system/app/index': () => import('@mango/rbac').then(m => m.AppView),
        'system/permission/index': () => import('@mango/rbac').then(m => m.PermissionView),
      },
    },
    {
      moduleCode: 'mango-system',
      pages: {
        'system/dict/index': () => import('@mango/system').then(m => m.DictView),
        'system/operation-log/index': () => import('@mango/system').then(m => m.OperationLogView),
        'system/login-log/index': () => import('@mango/system').then(m => m.LoginLogView),
        'system/tenant/index': () => import('@mango/system').then(m => m.TenantView),
        'system/config/index': () => import('@mango/system').then(m => m.ConfigView),
        'system/route/index': () => import('@mango/system').then(m => m.RouteView),
        'system/public-path/index': () => import('@mango/system').then(m => m.PublicPathView),
        'system/area/index': () => import('@mango/system').then(m => m.AreaView),
      },
    },
  ];

  const enabledFeatures = resolveMangoAdminFeatures(options.features);
  registries
    .filter(registry =>
      isMangoAdminFeatureEnabled(
        enabledFeatures,
        resolveMangoAdminModuleFeature(registry.moduleCode),
      ),
    )
    .filter((registry) => {
      const feature = resolveMangoAdminModuleFeature(registry.moduleCode) || registry.moduleCode;
      if (registeredFeatures.has(feature)) {
        return false;
      }
      registeredFeatures.add(feature);
      return true;
    })
    .forEach(registerModulePages);
  (options.registries || []).forEach(registerModulePages);
}
