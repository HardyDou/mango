/**
 * 验证码请求工具
 * @description 处理需要验证码的接口请求，自动获取验证码并添加Header
 */
import { ElMessage } from 'element-plus';
import type { CaptchaResponse } from '../api/captcha';

/**
 * 验证码Header配置
 */
const CAPTCHA_HEADERS = {
  key: 'X-Captcha-Key',
  code: 'X-Captcha-Code',
};

/**
 * 最大重试次数，防止无限循环
 */
const MAX_RETRIES = 3;

/**
 * 验证码类型对应的API路径
 */
const CAPTCHA_API_MAP: Record<string, string> = {
  ARITHMETIC: '/captcha/arithmetic',
  BLOCK_PUZZLE: '/captcha/block-puzzle',
  SMS: '/captcha/sms/send',
  EMAIL: '/captcha/email/send',
};

/**
 * 验证码配置接口
 */
interface CaptchaConfig {
  type: string;
  required: boolean;
}

export interface CaptchaRequestOptions extends RequestInit {
  getCaptchaConfig?: (path: string) => CaptchaConfig | null;
}

function toMutableHeaders(headers?: HeadersInit): Record<string, string> {
  if (!headers) {
    return {};
  }
  if (headers instanceof Headers) {
    return Object.fromEntries(headers.entries());
  }
  if (Array.isArray(headers)) {
    return Object.fromEntries(headers);
  }
  return { ...headers };
}

/**
 * 从菜单配置中查找验证码配置
 * @param menuItems 菜单数组
 * @param path 请求路径
 * @returns 验证码配置或null
 */
export function findCaptchaConfig(menuItems: any[], path: string): CaptchaConfig | null {
  // 递归查找匹配路径的菜单项
  function findMenuConfig(
    menus: any[],
    targetPath: string
  ): any | null {
    for (const menu of menus) {
      if (menu.path === targetPath && menu.meta?.captcha) {
        return menu.meta.captcha;
      }
      if (menu.children && menu.children.length > 0) {
        const found = findMenuConfig(menu.children, targetPath);
        if (found) return found;
      }
    }
    return null;
  }

  const menuConfig = findMenuConfig(menuItems, path);
  if (menuConfig?.required) {
    return menuConfig;
  }
  return null;
}

/**
 * 显示验证码弹窗
 * @param captchaData 验证码数据
 * @param type 验证码类型
 * @returns 用户输入的验证码或null
 */
async function showCaptchaModal(
  captchaData: CaptchaResponse,
  type: string
): Promise<string | null> {
  return new Promise((resolve) => {
    // 简化实现：实际项目中应该弹出验证码组件
    // 这里暂时返回null，让用户手动处理
    ElMessage.warning('请完成验证码');
    resolve(null);
  });
}

/**
 * 带验证码自动处理的请求
 * @param url 请求URL
 * @param options fetch选项
 * @param retries 当前重试次数
 * @returns 响应数据
 */
export async function captchaRequest<T>(
  url: string,
  options: CaptchaRequestOptions = {},
  retries = 0
): Promise<T> {
  // 1. 检查菜单配置是否需要验证码
  const captchaConfig = options.getCaptchaConfig?.(url) || null;

  const initialHeaders = toMutableHeaders(options.headers);
  if (captchaConfig?.required && !initialHeaders[CAPTCHA_HEADERS.key]) {
    // 获取验证码
    const captchaApiPath = CAPTCHA_API_MAP[captchaConfig.type] || '/captcha/arithmetic';

    try {
      const captchaRes = await fetch(captchaApiPath, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
      });

      if (!captchaRes.ok) {
        throw new Error('获取验证码失败');
      }

      const captchaData = await captchaRes.json();

      if (captchaData.code !== 200 || !captchaData.data?.key) {
        throw new Error('验证码数据无效');
      }

      // 显示验证码弹窗获取用户输入
      const userCode = await showCaptchaModal(captchaData.data, captchaConfig.type);

      if (!userCode) {
        throw new Error('CAPTCHA_REQUIRED');
      }

      // 添加验证码Header
      options.headers = {
        ...toMutableHeaders(options.headers),
        [CAPTCHA_HEADERS.key]: captchaData.data.key,
        [CAPTCHA_HEADERS.code]: userCode,
      };
    } catch (error: any) {
      if (error.message === 'CAPTCHA_REQUIRED') {
        throw error;
      }
      console.error('获取验证码失败:', error);
      throw error;
    }
  }

  // 2. 发起实际请求
  const res = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...toMutableHeaders(options.headers),
    },
  });

  // 3. 处理 428 响应（需要验证码）
  if (res.status === 428) {
    if (retries >= MAX_RETRIES) {
      throw new Error('CAPTCHA_MAX_RETRIES_EXCEEDED');
    }

    try {
      const data = await res.json();
      const captchaType = data.data?.type || 'ARITHMETIC';
      const captchaApiPath = CAPTCHA_API_MAP[captchaType] || '/captcha/arithmetic';

      // 获取新验证码
      const captchaRes = await fetch(captchaApiPath);
      const captchaData = await captchaRes.json();

      const userCode = await showCaptchaModal(captchaData.data, captchaType);

      if (!userCode) {
        throw new Error('CAPTCHA_REQUIRED');
      }

      // 添加验证码Header并重试
      options.headers = {
        ...toMutableHeaders(options.headers),
        [CAPTCHA_HEADERS.key]: captchaData.data.key,
        [CAPTCHA_HEADERS.code]: userCode,
      };

      return captchaRequest(url, options, retries + 1);
    } catch (error: any) {
      if (error.message === 'CAPTCHA_REQUIRED') {
        throw error;
      }
      // 重试
      return captchaRequest(url, options, retries + 1);
    }
  }

  // 4. 处理 400（验证码错误）
  if (res.status === 400) {
    if (retries >= MAX_RETRIES) {
      throw new Error('CAPTCHA_ERROR');
    }

    try {
      // 获取新验证码
      const captchaRes = await fetch('/captcha/arithmetic');
      const captchaData = await captchaRes.json();

      const userCode = await showCaptchaModal(captchaData.data, 'ARITHMETIC');

      if (!userCode) {
        throw new Error('CAPTCHA_REQUIRED');
      }

      options.headers = {
        ...toMutableHeaders(options.headers),
        [CAPTCHA_HEADERS.key]: captchaData.data.key,
        [CAPTCHA_HEADERS.code]: userCode,
      };

      return captchaRequest(url, options, retries + 1);
    } catch (error: any) {
      if (error.message === 'CAPTCHA_REQUIRED') {
        throw error;
      }
      return captchaRequest(url, options, retries + 1);
    }
  }

  // 5. 处理其他错误
  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.msg || `请求失败: ${res.status}`);
  }

  return res.json();
}

/**
 * 导出常量供外部使用
 */
export { CAPTCHA_HEADERS, MAX_RETRIES };
