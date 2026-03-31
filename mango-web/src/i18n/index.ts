import { createI18n } from 'vue-i18n';
import { usePreferencesStore } from '@/stores/preferences';
import { getI18nPublic, getI18nBff } from '@/api/admin/i18n';

// 导入语言文件
import zhCn from './lang/zh-cn';
import en from './lang/en';

export const i18n = createI18n({
  legacy: false, // 使用 Composition API 模式
  locale: 'zh-cn', // 默认语言
  fallbackLocale: 'zh-cn', // 回退语言
  messages: {
    'zh-cn': zhCn,
    en: en,
  },
  // 启用 Number.format 和 Date.timezone 设置
  numberFormats: {},
  datetimeFormats: {},
});

/**
 * 从后端获取国际化语言包并合并到 i18n
 * @param initialLoad 是否为初始加载
 * - true: 调用 /admin/i18n/public 获取所有语言（应用启动时）
 * - false: 调用 /bff/admin/i18n?lang=xxx 获取单一语言（运行时切换）
 */
export async function fetchI18n(initialLoad = true): Promise<void> {
  const lang = i18n.global.locale.value || localStorage.getItem('locale') || 'zh-cn';

  try {
    const messages: Record<string, unknown> = {};

    if (initialLoad) {
      // 初始加载：使用 /admin/i18n/public（返回所有语言）
      const res = await getI18nPublic();
      const langMessages = res.data[lang] || [];
      langMessages.forEach((item: Record<string, string>) => {
        Object.assign(messages, item);
      });
      i18n.global.mergeLocaleMessage(lang, messages);
    } else {
      // 运行时切换：使用 /bff/admin/i18n?lang=xxx（返回单一语言）
      const res = await getI18nBff(lang);
      const langMessages = res.data[lang] || [];
      langMessages.forEach((item: Record<string, string>) => {
        Object.assign(messages, item);
      });
      i18n.global.mergeLocaleMessage(lang, messages);
    }
  } catch (error) {
    console.warn('[i18n] Failed to fetch i18n from backend, using fallback:', error);
  }
}

/**
 * 初始化 i18n
 * @description 从 stores 获取当前语言设置并应用，同时从后端加载语言包
 */
export async function initI18n(): Promise<void> {
  const preferencesStore = usePreferencesStore();
  const { language } = preferencesStore;

  if (language) {
    i18n.global.locale.value = language;
  }

  // 从后端加载语言包
  await fetchI18n(true);
}
