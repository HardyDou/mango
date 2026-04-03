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
  authBtnList: string[];
  tenantId: string;
  tenantName: string;
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
