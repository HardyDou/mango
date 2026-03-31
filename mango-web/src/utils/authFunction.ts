import { Session } from './storage';

/**
 * 检查是否有权限
 * @param permission 权限码
 */
export function hasPermission(permission: string): boolean {
  const userInfo = Session.get('userInfo');
  if (!userInfo) return false;

  const { permissions = [] } = userInfo;

  // admin 拥有所有权限
  if (permissions.includes('*')) return true;

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
  const userInfo = Session.get('userInfo');
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
export function getUserInfo(): any {
  return Session.get('userInfo');
}

/**
 * 获取用户名
 */
export function getUsername(): string {
  const userInfo = getUserInfo();
  return userInfo?.username || '未知用户';
}

// Alias exports for auth directive compatibility
export const auth = hasPermission;
export const auths = hasAnyPermission;
export const authAll = hasAllPermissions;
