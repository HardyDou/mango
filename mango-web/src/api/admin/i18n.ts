import { get } from '@/utils/request';

/**
 * 获取国际化语言包
 * @param lang 语言代码
 */
export function getI18n(lang: string) {
  return get(`/admin/i18n/${lang}`);
}

/**
 * 获取支持的语言列表
 */
export function getSupportedLanguages() {
  return get('/admin/i18n/languages');
}
