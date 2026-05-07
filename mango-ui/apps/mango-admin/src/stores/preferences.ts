import { defineStore } from 'pinia';

export interface PreferencesState {
  isDrawer: boolean;
  isLockScreen: boolean;
  lockScreenTime: number;
  isGrayscale: boolean;
  isInvert: boolean;
  isWartermark: boolean;
  wartermarkText: string;
  tagsStyle: string;
  animation: string;
  isRequestRoutes: boolean;
  language: string;
  size: 'large' | 'default' | 'small';
  globalI18n: string;
  globalComponentSize: string;
  globalTitle: string;
  footerAuthor: string;
}

export const usePreferencesStore = defineStore('preferences', {
  state: (): PreferencesState => ({
    isDrawer: false,
    isLockScreen: false,
    lockScreenTime: 30,
    isGrayscale: false,
    isInvert: false,
    isWartermark: true,
    wartermarkText: 'Mango',
    tagsStyle: 'tags-style-five',
    animation: 'slide-right',
    isRequestRoutes: true,
    language: 'zh-cn',
    size: 'default',
    globalI18n: 'zh-cn',
    globalComponentSize: 'default',
    globalTitle: import.meta.env.VITE_APP_TITLE || 'Mango',
    footerAuthor: 'Mango',
  }),
  actions: {
    setLanguage(lang: string) {
      this.language = lang;
    },
  },
  persist: false,
});
