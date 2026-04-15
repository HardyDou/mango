/**
 * useLocale Hook
 *
 * Provides locale switching functionality with loading state support.
 */

import { ref, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage } from 'element-plus';

/**
 * Supported locales
 */
export type SupportedLocale = 'zh-cn' | 'en';

const SUPPORTED_LOCALES: SupportedLocale[] = ['zh-cn', 'en'];

/**
 * Locale display names
 */
const LOCALE_NAMES: Record<SupportedLocale, string> = {
  'zh-cn': '简体中文',
  en: 'English',
};

export interface UseLocaleOptions {
  initialLocale?: string;
  onPersistLanguage?: (locale: SupportedLocale) => void | Promise<void>;
  onLocaleChanged?: (locale: SupportedLocale) => void | Promise<void>;
}

export function useLocale(options: UseLocaleOptions = {}) {
  const { locale } = useI18n();

  // Loading state
  const loading = ref(false);

  // Current locale (from i18n)
  const currentLocale = computed(() => locale.value as SupportedLocale);

  // Check if locale is supported
  const isSupported = (loc: string): loc is SupportedLocale => {
    return SUPPORTED_LOCALES.includes(loc as SupportedLocale);
  };

  if (options.initialLocale && isSupported(options.initialLocale)) {
    locale.value = options.initialLocale;
  }

  /**
   * Switch locale
   * @param newLocale Target locale
   */
  async function switchLocale(newLocale: string): Promise<void> {
    if (!isSupported(newLocale)) {
      ElMessage.warning(`不支持的语言: ${newLocale}`);
      return;
    }

    if (locale.value === newLocale) {
      return;
    }

    loading.value = true;
    try {
      // Update i18n locale
      const targetLocale = newLocale as SupportedLocale;
      locale.value = targetLocale;

      // Persist to localStorage
      localStorage.setItem('locale', targetLocale);

      await options.onPersistLanguage?.(targetLocale);
      await options.onLocaleChanged?.(targetLocale);

      ElMessage.success(`语言已切换至 ${LOCALE_NAMES[targetLocale]}`);
    } catch (error) {
      console.error('[useLocale] Failed to switch locale:', error);
      ElMessage.error('语言切换失败');
    } finally {
      loading.value = false;
    }
  }

  /**
   * Get current locale display name
   */
  const currentLocaleName = computed(() => {
    return LOCALE_NAMES[currentLocale.value] || currentLocale.value;
  });

  /**
   * Get all supported locales
   */
  const supportedLocales = computed(() => {
    return SUPPORTED_LOCALES.map((loc) => ({
      code: loc,
      name: LOCALE_NAMES[loc],
    }));
  });

  return {
    locale: currentLocale,
    localeName: currentLocaleName,
    loading,
    switchLocale,
    supportedLocales,
    isSupported,
  };
}
