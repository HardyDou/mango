import { Session } from './storage';

/**
 * User information interface
 */
export interface UserInfo {
  username: string;
  nickname: string;
  photo: string;
  time: number;
  roles: string[];
  permissions: string[];
  buttonRules?: ButtonDisplayRule[];
  authBtnList: string[];
  tenantId: string;
  tenantName: string;
}

export interface ButtonRuleContext {
  row?: unknown;
  pageState?: unknown;
  query?: unknown;
  selectedRows?: unknown[];
}

export interface ButtonDisplayRule {
  code: string;
  displayRule?: string;
  buttonType?: 'TABLE' | 'NON_TABLE' | string;
}

export interface AuthRuleBindingValue extends ButtonRuleContext {
  code: string;
  permissions?: string[];
  buttonRules?: ButtonDisplayRule[];
  displayRule?: string;
}

/**
 * 检查是否有权限
 * @param permission 权限码
 *
 * IMPORTANT: 前端权限检查仅用于 UI 渲染，不作为安全判断。
 * 所有 API 操作的安全校验必须在后端完成。
 * 后端返回的 permissions 列表应包含用户所有有效权限（非 '*' 通配符）。
 */
export function hasPermission(permission: string): boolean {
  const userInfo = Session.get('userInfo') as UserInfo | null;
  if (!userInfo) return false;

  const { permissions = [] } = userInfo;

  // Issue 009: 不再信任 '*' 作为超级权限标志
  // 后端应返回具体权限码列表，而非 '*' 通配符
  // 前端 UI 显示仅作为辅助，真正的安全控制在后端 API 层

  return permissions.includes(permission);
}

/**
 * 检查是否有任意一个权限
 * @param permissionList 权限码列表
 */
export function hasAnyPermission(permissionList: string[]): boolean {
  return permissionList.some((permission) => hasPermission(permission));
}

/**
 * 检查是否有所有权限
 * @param permissionList 权限码列表
 */
export function hasAllPermissions(permissionList: string[]): boolean {
  return permissionList.every((permission) => hasPermission(permission));
}

/**
 * 获取用户权限列表
 */
export function getPermissions(): string[] {
  const userInfo = Session.get('userInfo') as UserInfo | null;
  return userInfo?.permissions || [];
}

/**
 * 获取当前登录人的按钮展示规则。
 */
export function getButtonRules(): ButtonDisplayRule[] {
  const userInfo = Session.get('userInfo') as UserInfo | null;
  return Array.isArray(userInfo?.buttonRules) ? userInfo.buttonRules : [];
}

/**
 * 解析按钮展示表达式。
 *
 * 注意：这里仅用于前端 UI 显示控制，不是安全边界；真实权限仍必须由后端接口校验。
 * 空规则默认显示，表达式异常时隐藏，避免错误配置把不该展示的按钮暴露出来。
 */
export function evaluateButtonDisplayRule(rule?: string, context: ButtonRuleContext = {}): boolean {
  const expression = (rule || '').trim();
  if (!expression) {
    return true;
  }

  try {
    const runner = new Function(
      'row',
      'pageState',
      'query',
      'selectedRows',
      `return Boolean(${expression});`,
    );
    return Boolean(runner(
      context.row,
      context.pageState,
      context.query,
      context.selectedRows || [],
    ));
  } catch {
    return false;
  }
}

function isRuleBindingValue(value: unknown): value is AuthRuleBindingValue {
  return Boolean(value)
    && typeof value === 'object'
    && !Array.isArray(value)
    && typeof (value as AuthRuleBindingValue).code === 'string';
}

function resolveButtonRule(value: AuthRuleBindingValue): string {
  if (typeof value.displayRule === 'string') {
    return value.displayRule;
  }

  const rules = Array.isArray(value.buttonRules) ? value.buttonRules : getButtonRules();
  return rules.find(item => item.code === value.code)?.displayRule || '';
}

/**
 * 按钮是否展示：先校验权限码，再校验展示规则。
 */
export function canShowButton(value: string | AuthRuleBindingValue): boolean {
  if (typeof value === 'string') {
    return hasPermission(value);
  }
  if (!isRuleBindingValue(value)) {
    return false;
  }

  const permissions = Array.isArray(value.permissions) ? value.permissions : getPermissions();
  const hasCodePermission = permissions.includes(value.code);
  if (!hasCodePermission) {
    return false;
  }

  return evaluateButtonDisplayRule(resolveButtonRule(value), {
    row: value.row,
    pageState: value.pageState,
    query: value.query,
    selectedRows: value.selectedRows,
  });
}

/**
 * 检查是否登录
 */
export function isLoggedIn(): boolean {
  return !!Session.getToken();
}

/**
 * 获取用户信息
 */
export function getUserInfo(): UserInfo | null {
  return Session.get('userInfo') as UserInfo | null;
}

/**
 * 获取用户名
 */
export function getUsername(): string {
  const userInfo = getUserInfo();
  return userInfo?.username || '未知用户';
}

// 权限检查别名（供 directive 使用）
export const auth = hasPermission;
export const auths = hasAnyPermission;
export const authAll = hasAllPermissions;
