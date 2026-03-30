import { createI18n } from 'vue-i18n';
import { usePreferencesStore } from '@/stores/preferences';

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
 * 初始化 i18n
 * @description 从 stores 获取当前语言设置并应用
 */
export function initI18n(): void {
  const preferencesStore = usePreferencesStore();
  const { language } = preferencesStore;

  if (language) {
    i18n.global.locale.value = language;
  }
}
