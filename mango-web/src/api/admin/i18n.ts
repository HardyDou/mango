import { get } from '@/utils/request';

/**
 * 获取公开的国际化语言包（所有语言）
 * @description 调用 /i18n/public，返回格式 { "zh-cn": [...], "en": [...] }
 * 用于应用启动时初始加载
 */
export function getI18nPublic() {
  return get('/i18n/public');
}

/**
 * 获取单一语言的国际化语言包
 * @param lang 语言代码，如 'zh-cn', 'en'
 * @description 调用 /i18n?lang=xxx，用于运行时语言切换
 */
export function getI18nBff(lang: string) {
  return get(`/i18n?lang=${lang}`);
}

/**
 * 获取支持的语言列表
 * @description 调用 /i18n/languages，返回 ["zh-cn", "en"]
 */
export function getSupportedLanguages() {
  return get('/i18n/languages');
}

/**
 * 获取单一语言的国际化语言包（直接从 i18n 模块）
 * @param lang 语言代码
 * @description 调用 /i18n/public/lang?lang=xxx，由 i18n 模块直接返回
 */
export function getI18n(lang: string) {
  return get(`/i18n/public/lang?lang=${lang}`);
}
