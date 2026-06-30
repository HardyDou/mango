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
  shortTitle: string;
  logoUrl: string;
  faviconUrl: string;
  footerCopyright: string;
  footerIcp: string;
  footerContact: string;
  footerAuthor: string;
}

export const TAGS_STYLE_ALIASES: Record<string, string> = {
  'tags-style-one': 'tags-style-capsule',
  'tags-style-four': 'tags-style-card',
  'tags-style-five': 'tags-style-classic',
};

export const DEFAULT_TAGS_STYLE = 'tags-style-classic';

export function normalizeTagsStyle(style?: string) {
  if (!style) {
    return DEFAULT_TAGS_STYLE;
  }
  return TAGS_STYLE_ALIASES[style] || style;
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
    tagsStyle: DEFAULT_TAGS_STYLE,
    animation: 'slide-right',
    isRequestRoutes: true,
    language: 'zh-cn',
    size: 'default',
    globalI18n: 'zh-cn',
    globalComponentSize: 'default',
    globalTitle: import.meta.env.VITE_APP_TITLE || 'Mango',
    shortTitle: 'Mango',
    logoUrl: '',
    faviconUrl: '',
    footerCopyright: '© Mango',
    footerIcp: '',
    footerContact: '',
    footerAuthor: 'Mango',
  }),
  actions: {
    setLanguage(lang: string) {
      this.language = lang;
    },
  },
  persist: false,
});
