/**
 * useTitle Hook
 *
 * Provides page title i18n support with automatic concatenation of page title + app name.
 * Listens for language changes and auto-updates the document title.
 */

import { watch, onUnmounted } from 'vue';
import { useI18n } from 'vue-i18n';

const APP_NAME = 'Mango Admin';

/**
 * Set document title with i18n support
 * @param titleKey i18n key for the page title (optional)
 * @param appName Custom app name override (optional)
 */
export function useTitle(titleKey?: string, customAppName?: string) {
  const { t, locale } = useI18n();

  const appName = customAppName || APP_NAME;

  /**
   * Update the document title
   */
  function updateTitle() {
    if (titleKey) {
      const title = t(titleKey);
      document.title = title ? `${title} - ${appName}` : appName;
    } else {
      document.title = appName;
    }
  }

  // Set initial title
  updateTitle();

  // Watch for language changes and update title
  const stopWatch = watch(
    locale,
    () => {
      updateTitle();
    },
    { immediate: false }
  );

  // Cleanup on unmount
  onUnmounted(() => {
    stopWatch();
  });

  return {
    /** Update title immediately */
    updateTitle,
    /** Current app name */
    appName,
  };
}

/**
 * Set document title directly (non-i18n)
 * @param title Page title
 * @param appName App name (optional, defaults to APP_NAME)
 */
export function setTitle(title: string, appName: string = APP_NAME) {
  document.title = title ? `${title} - ${appName}` : appName;
}

export default useTitle;
