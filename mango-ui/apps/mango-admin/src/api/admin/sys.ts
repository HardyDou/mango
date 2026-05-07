import { get, post } from '@mango/common';
import { generateArithmetic } from './captcha';

/**
 * 登录
 */
export function login(data: { username: string; password: string; captchaCode?: string; captchaKey?: string }) {
  return post('/auth/login', data);
}

/**
 * 获取用户信息
 * Issue B Fix: 路径更新为 /auth/info（根据 Plan 011 auth 域收敛）
 */
export function getUserInfo() {
  return get('/auth/info');
}

/**
 * 登出
 */
export function logout() {
  return post('/auth/logout');
}

/**
 * 获取验证码
 */
export function getCaptcha() {
  return generateArithmetic();
}

/**
 * 修改密码
 */
export function updatePassword(data: { oldPassword: string; newPassword: string }) {
  return post('/user/password', data);
}

/**
 * 获取系统配置
 */
export function getSystemConfig() {
  return get('/system/config/type', { params: { type: 'SYSTEM' } });
}
