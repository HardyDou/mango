/**
 * authFunction 单元测试
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { Session } from '../storage';

// Mock Session
vi.mock('../storage', () => ({
  Session: {
    get: vi.fn(),
    getToken: vi.fn(),
    set: vi.fn(),
    remove: vi.fn(),
    clear: vi.fn(),
  },
}));

import {
  hasPermission,
  hasAnyPermission,
  hasAllPermissions,
  getPermissions,
  getButtonRules,
  evaluateButtonDisplayRule,
  canShowButton,
  isLoggedIn,
  getUserInfo,
  getUsername,
  auth,
  auths,
  authAll,
} from '../authFunction';

describe('权限函数', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('hasPermission', () => {
    it('应该返回 false 当用户未登录', () => {
      vi.mocked(Session.get).mockReturnValue(null);
      expect(hasPermission('test:permission')).toBe(false);
    });

    it('应该返回 false 当用户没有该权限', () => {
      vi.mocked(Session.get).mockReturnValue({ permissions: ['other:permission'] });
      expect(hasPermission('test:permission')).toBe(false);
    });

    it('应该返回 true 当用户拥有该权限', () => {
      vi.mocked(Session.get).mockReturnValue({ permissions: ['test:permission'] });
      expect(hasPermission('test:permission')).toBe(true);
    });

    it('应该返回 false 当用户有 * 通配符权限（前端不信任 * 作为安全判断）', () => {
      vi.mocked(Session.get).mockReturnValue({ permissions: ['*'] });
      // Issue 009: 前端不再信任 '*' 作为超级权限标志
      // 安全控制在后端 API 层，前端仅用于 UI 渲染辅助
      expect(hasPermission('any:permission')).toBe(false);
    });
  });

  describe('hasAnyPermission', () => {
    it('应该返回 true 当用户拥有任意一个权限', () => {
      vi.mocked(Session.get).mockReturnValue({ permissions: ['test:read'] });
      expect(hasAnyPermission(['test:read', 'test:write'])).toBe(true);
    });

    it('应该返回 false 当用户没有任何权限', () => {
      vi.mocked(Session.get).mockReturnValue({ permissions: ['other:permission'] });
      expect(hasAnyPermission(['test:read', 'test:write'])).toBe(false);
    });
  });

  describe('hasAllPermissions', () => {
    it('应该返回 true 当用户拥有所有权限', () => {
      vi.mocked(Session.get).mockReturnValue({ permissions: ['test:read', 'test:write'] });
      expect(hasAllPermissions(['test:read', 'test:write'])).toBe(true);
    });

    it('应该返回 false 当用户缺少部分权限', () => {
      vi.mocked(Session.get).mockReturnValue({ permissions: ['test:read'] });
      expect(hasAllPermissions(['test:read', 'test:write'])).toBe(false);
    });
  });

  describe('getPermissions', () => {
    it('应该返回用户权限列表', () => {
      vi.mocked(Session.get).mockReturnValue({ permissions: ['a', 'b'] });
      expect(getPermissions()).toEqual(['a', 'b']);
    });

    it('应该返回空数组当用户信息为空', () => {
      vi.mocked(Session.get).mockReturnValue(null);
      expect(getPermissions()).toEqual([]);
    });

    it('应该返回空数组当 permissions 为 undefined', () => {
      vi.mocked(Session.get).mockReturnValue({});
      expect(getPermissions()).toEqual([]);
    });
  });

  describe('getButtonRules', () => {
    it('returns button display rules from userInfo', () => {
      const buttonRules = [{ code: 'test:edit', displayRule: 'row.status == 1' }];
      vi.mocked(Session.get).mockReturnValue({ permissions: [], buttonRules });
      expect(getButtonRules()).toEqual(buttonRules);
    });

    it('returns empty array without button rules', () => {
      vi.mocked(Session.get).mockReturnValue({ permissions: [] });
      expect(getButtonRules()).toEqual([]);
    });
  });

  describe('evaluateButtonDisplayRule', () => {
    it('returns true for empty rule', () => {
      expect(evaluateButtonDisplayRule('')).toBe(true);
      expect(evaluateButtonDisplayRule('   ')).toBe(true);
    });

    it('supports row comparison and && expressions', () => {
      const context = { row: { status: 1, age: 18, type: 'A' } };
      expect(evaluateButtonDisplayRule('row.status == 1 && row.age === 18', context)).toBe(true);
      expect(evaluateButtonDisplayRule('row.type != "B"', context)).toBe(true);
    });

    it('supports pageState query selectedRows and || expressions', () => {
      const context = {
        pageState: { locked: false },
        query: { keyword: 'demo' },
        selectedRows: [{ id: 1 }],
      };
      expect(evaluateButtonDisplayRule('!pageState.locked && (query.keyword === "demo" || selectedRows.length > 0)', context)).toBe(true);
    });

    it('returns false when rule throws', () => {
      expect(evaluateButtonDisplayRule('row.missing.value === 1', { row: {} })).toBe(false);
    });
  });

  describe('canShowButton', () => {
    it('keeps string permission compatibility', () => {
      vi.mocked(Session.get).mockReturnValue({ permissions: ['test:add'] });
      expect(canShowButton('test:add')).toBe(true);
    });

    it('hides when permission is missing', () => {
      vi.mocked(Session.get).mockReturnValue({ permissions: ['test:add'] });
      expect(canShowButton({ code: 'test:delete', displayRule: '' })).toBe(false);
    });

    it('supports inline displayRule', () => {
      vi.mocked(Session.get).mockReturnValue({ permissions: ['test:edit'] });
      expect(canShowButton({
        code: 'test:edit',
        displayRule: 'row.status == 1 && row.age == 18',
        row: { status: 1, age: 18 },
      })).toBe(true);
    });

    it('matches displayRule from buttonRules by code', () => {
      vi.mocked(Session.get).mockReturnValue({
        permissions: ['test:submit'],
        buttonRules: [{ code: 'test:submit', displayRule: 'pageState.status === "draft"' }],
      });
      expect(canShowButton({
        code: 'test:submit',
        pageState: { status: 'draft' },
      })).toBe(true);
      expect(canShowButton({
        code: 'test:submit',
        pageState: { status: 'done' },
      })).toBe(false);
    });
  });

  describe('isLoggedIn', () => {
    it('应该返回 true 当有 token', () => {
      vi.mocked(Session.getToken).mockReturnValue('valid-token');
      expect(isLoggedIn()).toBe(true);
    });

    it('应该返回 false 当没有 token', () => {
      vi.mocked(Session.getToken).mockReturnValue(null);
      expect(isLoggedIn()).toBe(false);
    });
  });

  describe('getUserInfo', () => {
    it('应该返回用户信息', () => {
      const userInfo = { username: 'test', permissions: [] };
      vi.mocked(Session.get).mockReturnValue(userInfo);
      expect(getUserInfo()).toEqual(userInfo);
    });

    it('应该返回 null 当没有用户信息', () => {
      vi.mocked(Session.get).mockReturnValue(null);
      expect(getUserInfo()).toBeNull();
    });
  });

  describe('getUsername', () => {
    it('应该返回用户名', () => {
      vi.mocked(Session.get).mockReturnValue({ username: 'testuser' });
      expect(getUsername()).toBe('testuser');
    });

    it('应该返回默认值当用户名为空', () => {
      vi.mocked(Session.get).mockReturnValue({ username: '' });
      expect(getUsername()).toBe('未知用户');
    });

    it('应该返回默认值当用户信息为空', () => {
      vi.mocked(Session.get).mockReturnValue(null);
      expect(getUsername()).toBe('未知用户');
    });
  });

  describe('别名导出 (auth directive 兼容)', () => {
    it('auth 应该是 hasPermission 的别名', () => {
      vi.mocked(Session.get).mockReturnValue({ permissions: ['test:permission'] });
      expect(auth('test:permission')).toBe(true);
      expect(hasPermission('test:permission')).toBe(true);
    });

    it('auths 应该是 hasAnyPermission 的别名', () => {
      vi.mocked(Session.get).mockReturnValue({ permissions: ['test:read'] });
      expect(auths(['test:read', 'test:write'])).toBe(true);
      expect(hasAnyPermission(['test:read', 'test:write'])).toBe(true);
    });

    it('authAll 应该是 hasAllPermissions 的别名', () => {
      vi.mocked(Session.get).mockReturnValue({ permissions: ['test:read', 'test:write'] });
      expect(authAll(['test:read', 'test:write'])).toBe(true);
      expect(hasAllPermissions(['test:read', 'test:write'])).toBe(true);
    });
  });
});
