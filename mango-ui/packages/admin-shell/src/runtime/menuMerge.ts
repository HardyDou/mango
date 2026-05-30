import type { ShellMenu } from './menuHost';

const MENU_TYPE_DIRECTORY = 1;
const MENU_TYPE_MENU = 2;

export type ShellMenuSource = 'backend' | 'capability' | 'business' | 'shell';

export interface ShellMenuMergeReportItem {
  menuCode: string;
  path: string;
  source: ShellMenuSource;
  action: 'added' | 'merged' | 'kept' | 'filtered' | 'conflict';
  reason: string;
}

export interface ShellMenuMergeReport {
  items: ShellMenuMergeReportItem[];
  diagnostics: string[];
}

export interface MergeShellMenusOptions {
  backendMenus?: ShellMenu[];
  capabilityMenus?: ShellMenu[];
  businessMenus?: ShellMenu[];
  permissions?: string[];
  report?: ShellMenuMergeReport;
}

export interface MergeShellMenusResult {
  menus: ShellMenu[];
  report: ShellMenuMergeReport;
}

export function createShellMenuMergeReport(): ShellMenuMergeReport {
  return {
    items: [],
    diagnostics: [],
  };
}

export function mergeShellMenus(options: MergeShellMenusOptions): MergeShellMenusResult {
  const report = options.report || createShellMenuMergeReport();
  const permissions = options.permissions ? new Set(options.permissions) : undefined;
  const indexed = new Map<string, ShellMenu>();
  const pathIndex = new Map<string, string>();
  const roots: ShellMenu[] = [];

  addMenus(roots, options.backendMenus || [], 'backend', indexed, pathIndex, report, permissions);
  addMenus(roots, options.capabilityMenus || [], 'capability', indexed, pathIndex, report, permissions);
  addMenus(roots, options.businessMenus || [], 'business', indexed, pathIndex, report, permissions);

  return {
    menus: sortMenus(roots),
    report,
  };
}

function addMenus(
  target: ShellMenu[],
  menus: ShellMenu[],
  source: ShellMenuSource,
  indexed: Map<string, ShellMenu>,
  pathIndex: Map<string, string>,
  report: ShellMenuMergeReport,
  permissions?: Set<string>,
) {
  for (const menu of menus) {
    const cloned = cloneMenu(menu);
    const filteredChildren = cloned.children || [];
    cloned.children = [];
    addMenus(cloned.children, filteredChildren, source, indexed, pathIndex, report, permissions);
    if (!isMenuAllowed(cloned, permissions)) {
      report.items.push(toReportItem(cloned, source, 'filtered', 'missing permission'));
      continue;
    }
    if (isEmptyDirectory(cloned)) {
      report.items.push(toReportItem(cloned, source, 'filtered', 'empty directory'));
      continue;
    }
    const existing = indexed.get(cloned.menuCode);
    if (existing) {
      mergeMissingMenuFields(existing, cloned);
      report.items.push(toReportItem(cloned, source, source === 'backend' ? 'kept' : 'merged', 'menuCode already exists; existing menu kept'));
      continue;
    }
    const pathConflictCode = cloned.path ? pathIndex.get(cloned.path) : undefined;
    if (pathConflictCode && pathConflictCode !== cloned.menuCode) {
      const message = `${source} menu ${cloned.menuCode} path ${cloned.path} conflicts with existing menu ${pathConflictCode}`;
      report.diagnostics.push(message);
      report.items.push(toReportItem(cloned, source, 'conflict', message));
      continue;
    }
    indexed.set(cloned.menuCode, cloned);
    if (cloned.path) {
      pathIndex.set(cloned.path, cloned.menuCode);
    }
    target.push(cloned);
    report.items.push(toReportItem(cloned, source, 'added', 'added to menu tree'));
  }
}

function mergeMissingMenuFields(target: ShellMenu, source: ShellMenu) {
  target.moduleCode ||= source.moduleCode;
  target.component ||= source.component;
  target.pageType ||= source.pageType;
  target.externalUrl ||= source.externalUrl;
  target.icon ||= source.icon;
  target.redirect ||= source.redirect;
  target.keepAlive ??= source.keepAlive;
  target.embedded ??= source.embedded;
  target.meta = {
    ...(source.meta || {}),
    ...(target.meta || {}),
  };
  const targetCodes = new Set((target.children || []).map(child => child.menuCode));
  for (const child of source.children || []) {
    if (!targetCodes.has(child.menuCode)) {
      target.children = target.children || [];
      target.children.push(child);
    }
  }
}

function isMenuAllowed(menu: ShellMenu, permissions?: Set<string>) {
  if (!permissions || permissions.size === 0 || menu.menuType !== MENU_TYPE_MENU) {
    return true;
  }
  const requiredPermissions = extractMenuPermissions(menu);
  return requiredPermissions.length === 0 || requiredPermissions.some(permission => permissions.has(permission));
}

function extractMenuPermissions(menu: ShellMenu) {
  const metaPermissions = Array.isArray(menu.meta?.permissions) ? menu.meta.permissions : [];
  const sourcePermissions = Array.isArray((menu as ShellMenu & { permissions?: string[] }).permissions)
    ? (menu as ShellMenu & { permissions?: string[] }).permissions || []
    : [];
  return [...new Set([...metaPermissions, ...sourcePermissions])];
}

function isEmptyDirectory(menu: ShellMenu) {
  return menu.menuType === MENU_TYPE_DIRECTORY && (!menu.children || menu.children.length === 0);
}

function cloneMenu(menu: ShellMenu): ShellMenu {
  return {
    ...menu,
    meta: menu.meta ? { ...menu.meta } : undefined,
    children: menu.children?.map(cloneMenu) || [],
  };
}

function sortMenus(menus: ShellMenu[]): ShellMenu[] {
  return menus
    .map(menu => ({
      ...menu,
      children: menu.children ? sortMenus(menu.children) : [],
    }))
    .sort((left, right) => (left.sort || 0) - (right.sort || 0));
}

function toReportItem(
  menu: ShellMenu,
  source: ShellMenuSource,
  action: ShellMenuMergeReportItem['action'],
  reason: string,
): ShellMenuMergeReportItem {
  return {
    menuCode: menu.menuCode,
    path: menu.path,
    source,
    action,
    reason,
  };
}
