import { get, post } from '@mango/common/utils/request';
import { generateArithmetic } from '@mango/common/api/captcha';

/**
 * 登录
 */
export function login(data: {
  username: string;
  password: string;
  tenantId?: string | number;
  tenantCode?: string;
  realm?: string;
  actorType?: string;
  partyType?: string;
  partyId?: string | number;
  appCode?: string;
  captchaCode?: string;
  captchaKey?: string;
}) {
  return post('/auth/login', data);
}

export function wecomLogin(data: {
  code: string;
  channelConfigId?: string | number;
  tenantId?: string | number;
  tenantCode?: string;
  appCode?: string;
}) {
  return post('/auth/wecom/login', data, { ignoreToken: true });
}

export interface WecomLoginConfig {
  channelConfigId?: string | number;
  corpId?: string;
  agentId?: string;
  redirectUri?: string;
}

export function getWecomLoginConfig(tenantId: string | number) {
  return get<WecomLoginConfig>('/auth/wecom/login-config', {
    params: { tenantId },
    ignoreToken: true,
  });
}

export function getAccountLoginTenantOptions(data: {
  username: string;
  password: string;
  realm?: string;
  appCode?: string;
}) {
  return post<LoginTenantOption[]>('/auth/login-institutions', data, { ignoreToken: true });
}

export interface LoginTenantOption {
  tenantId: string;
  tenantCode: string;
  tenantName: string;
}

/**
 * 获取登录机构选项
 */
export function getLoginTenantOptions() {
  return get<LoginTenantOption[]>('/system/tenant/login-options', { ignoreToken: true });
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
