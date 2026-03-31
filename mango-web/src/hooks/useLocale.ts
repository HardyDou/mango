/**
 * useLocale Hook
 *
 * Provides locale switching functionality with loading state support.
 */

import { ref, computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { usePreferencesStore } from '@/stores/preferences';
import { storeToRefs } from 'pinia';
import { ElMessage } from 'element-plus';
import { fetchI18n } from '@/i18n';

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

export function useLocale() {
  const { locale } = useI18n();
  const preferencesStore = usePreferencesStore();
  const { language } = storeToRefs(preferencesStore);

  // Loading state
  const loading = ref(false);

  // Current locale (from i18n)
  const currentLocale = computed(() => locale.value as SupportedLocale);

  // Check if locale is supported
  const isSupported = (loc: string): loc is SupportedLocale => {
    return SUPPORTED_LOCALES.includes(loc as SupportedLocale);
  };

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
      locale.value = newLocale;

      // Persist to store
      preferencesStore.setLanguage(newLocale);

      // Persist to localStorage
      localStorage.setItem('locale', newLocale);

      // 重新加载后端语言包（运行时切换模式）
      await fetchI18n(false);

      ElMessage.success(`语言已切换至 ${LOCALE_NAMES[newLocale as SupportedLocale]}`);
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
