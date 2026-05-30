import { describe, expect, it } from 'vitest';
import {
  createNotFoundRouteMenu,
  findUnexpectedTopLevelMenus,
  shouldShowDevCenter,
} from '../runtime/menuHost';

describe('admin-shell menu contract', () => {
  it('shows development center for dev and test deploy environments by default', () => {
    expect(shouldShowDevCenter({ deployEnv: 'dev' })).toBe(true);
    expect(shouldShowDevCenter({ deployEnv: 'test' })).toBe(true);
  });

  it('hides development center for production-like deploy environments by default', () => {
    expect(shouldShowDevCenter({ deployEnv: 'prod' })).toBe(false);
    expect(shouldShowDevCenter({ deployEnv: 'prd' })).toBe(false);
    expect(shouldShowDevCenter({ deployEnv: 'production' })).toBe(false);
  });

  it('lets explicit configuration override deploy environment defaults', () => {
    expect(shouldShowDevCenter({ deployEnv: 'prod', visible: true })).toBe(true);
    expect(shouldShowDevCenter({ deployEnv: 'dev', visible: false })).toBe(false);
  });

  it('marks fallback routes so full-mode verification can reject them', () => {
    const route = createNotFoundRouteMenu('/missing-page');

    expect(route.sourceMenu.meta?.source).toBe('fallback');
    expect(route.path).toBe('/missing-page');
  });

  it('rejects top-level menus that are neither backend menus nor explicit shell menus', () => {
    const backendTopMenus = ['系统管理', '审批中心', '平台能力', '通知中心'];
    const uiTopMenus = ['首页', '系统管理', '审批中心', '平台能力', '通知中心', '开发中心', '自造菜单'];
    const allowedShellMenus = ['首页', '开发中心'];

    expect(findUnexpectedTopLevelMenus(uiTopMenus, backendTopMenus, allowedShellMenus)).toEqual(['自造菜单']);
  });
});
