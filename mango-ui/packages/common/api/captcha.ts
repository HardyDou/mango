import { get, post } from '../utils/request';

export enum CaptchaType {
  ARITHMETIC = 'ARITHMETIC',
  BLOCK_PUZZLE = 'BLOCK_PUZZLE',
  CLICK_WORD = 'CLICK_WORD',
  CANVAS_SLIDER = 'CANVAS_SLIDER',
  SMS = 'SMS',
  EMAIL = 'EMAIL',
}

export interface CaptchaResponse {
  key: string;
  type: CaptchaType;
  image?: string;
  backgroundImage?: string;
  sliderImage?: string;
  x?: number;
  y?: number;
  expireTime: number;
  target?: string;
  extra?: string;
}

export interface CaptchaVerifyRequest {
  key: string;
  type: CaptchaType;
  code?: string;
  pointJson?: string;
}

export interface CaptchaTypesResponse {
  types: CaptchaType[];
  currentStorage: string;
}

export function getCaptchaTypes() {
  return get<CaptchaTypesResponse>('/captcha/types');
}

export function generateArithmetic() {
  return get<CaptchaResponse>('/captcha/arithmetic');
}

export function generateBlockPuzzle() {
  return get<CaptchaResponse>('/captcha/block-puzzle');
}

export function generateClickWord() {
  return get<CaptchaResponse>('/captcha/click-word');
}

export function sendSms(mobile: string) {
  return post<string>('/auth/captcha/send', { type: CaptchaType.SMS, target: mobile, businessType: 'LOGIN' });
}

export function sendEmail(email: string) {
  return post<string>('/auth/captcha/send', { type: CaptchaType.EMAIL, target: email, businessType: 'LOGIN' });
}

export function verifyCaptcha(request: CaptchaVerifyRequest) {
  return post<boolean>('/captcha/verify', request);
}
