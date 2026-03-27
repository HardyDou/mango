import { get, post } from '@/utils/request';

/**
 * 登录
 */
export function login(data: { username: string; password: string; captcha?: string }) {
  return post('/admin/sys/login', data);
}

/**
 * 获取用户信息
 */
export function getUserInfo() {
  return get('/admin/sys/userInfo');
}

/**
 * 获取路由列表
 */
export function getRoutes() {
  return get('/admin/sys/routes');
}

/**
 * 获取菜单列表
 */
export function getMenus() {
  return get('/admin/sys/menus');
}

/**
 * 登出
 */
export function logout() {
  return post('/admin/sys/logout');
}

/**
 * 获取验证码
 */
export function getCaptcha() {
  return get('/admin/sys/captcha');
}

/**
 * 修改密码
 */
export function updatePassword(data: { oldPassword: string; newPassword: string }) {
  return post('/admin/sys/password', data);
}

/**
 * 获取系统配置
 */
export function getSystemConfig() {
  return get('/admin/sys/config');
}
