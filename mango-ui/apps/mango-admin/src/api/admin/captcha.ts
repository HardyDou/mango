import { get, post } from '@mango/common';

/**
 * 验证码类型
 */
export enum CaptchaType {
  /** 算术验证码 */
  ARITHMETIC = 'ARITHMETIC',
  /** 滑块验证码（图片） */
  BLOCK_PUZZLE = 'BLOCK_PUZZLE',
  /** 点选文字验证码 */
  CLICK_WORD = 'CLICK_WORD',
  /** 无感行为验证 */
  BEHAVIOR = 'BEHAVIOR',
  /** 滑块验证码（Canvas） */
  CANVAS_SLIDER = 'CANVAS_SLIDER',
  /** 短信验证码 */
  SMS = 'SMS',
  /** 邮件验证码 */
  EMAIL = 'EMAIL',
}

/**
 * 验证码响应
 */
export interface CaptchaResponse {
  /** 验证码key - 用于后续验证 */
  key: string;
  /** 验证码类型 */
  type: CaptchaType;
  /** 图片Base64（算术/滑块） */
  image?: string;
  /** 滑块背景图 */
  backgroundImage?: string;
  /** 滑块图片 */
  sliderImage?: string;
  /** 过期时间（秒） */
  expireTime: number;
  /** 目标（手机号/邮箱） */
  target?: string;
  /** 额外数据（算术验证码答案等） */
  extra?: string;
}

/**
 * 验证码校验请求
 */
export interface CaptchaVerifyRequest {
  /** 验证码key */
  key: string;
  /** 验证码类型 */
  type: CaptchaType;
  /** 用户输入的验证码（算术/短信/邮件） */
  code?: string;
  /** 滑块验证参数（滑块验证码） */
  pointJson?: string;
}

/**
 * 无感行为验证评分结果
 */
export interface BehaviorCaptchaVerifyResult {
  /** 验证码key */
  key: string;
  /** 0.0 到 1.0 的行为评分，分数越高越像真人 */
  score: number;
  /** 是否通过 */
  passed: boolean;
  /** 风险等级 */
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH';
  /** 建议业务动作 */
  suggestAction: 'ALLOW' | 'SECONDARY_VERIFY' | 'DENY';
  /** 评分原因 */
  reason: string;
}

/**
 * 验证码类型响应
 */
export interface CaptchaTypesResponse {
  /** 支持的验证码类型列表 */
  types: CaptchaType[];
  /** 当前存储方式 */
  currentStorage: string;
}

/**
 * 获取支持的验证码类型
 * Issue B Fix: 公共接口保持 /captcha/*，需要认证的接口使用 /auth/captcha/*
 */
export function getCaptchaTypes() {
  return get<CaptchaTypesResponse>('/captcha/types');
}

/**
 * 生成算术验证码
 */
export function generateArithmetic() {
  return get<CaptchaResponse>('/captcha/arithmetic');
}

/**
 * 生成滑块验证码
 */
export function generateBlockPuzzle() {
  return get<CaptchaResponse>('/captcha/block-puzzle');
}

/**
 * 生成点选文字验证码
 */
export function generateClickWord() {
  return get<CaptchaResponse>('/captcha/click-word');
}

/**
 * 生成无感行为验证
 */
export function generateBehavior() {
  return get<CaptchaResponse>('/captcha/behavior');
}

/**
 * 校验无感行为验证
 */
export function verifyBehaviorCaptcha(request: CaptchaVerifyRequest) {
  return post<BehaviorCaptchaVerifyResult>('/captcha/behavior/verify', request);
}

/**
 * 发送短信验证码（需认证）
 */
export function sendSms(mobile: string) {
  return post<string>('/auth/captcha/send', { type: CaptchaType.SMS, target: mobile, businessType: 'LOGIN' });
}

/**
 * 发送邮件验证码（需认证）
 */
export function sendEmail(email: string) {
  return post<string>('/auth/captcha/send', { type: CaptchaType.EMAIL, target: email, businessType: 'LOGIN' });
}

/**
 * 校验验证码
 */
export function verifyCaptcha(request: CaptchaVerifyRequest) {
  return post<boolean>('/captcha/verify', request);
}
