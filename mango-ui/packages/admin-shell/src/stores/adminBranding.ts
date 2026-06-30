import { defineStore } from 'pinia';
import { installMangoAuth } from '@mango/auth';
import { get } from '@mango/common/utils/request';
import { fileApi, fileRuntimeUrl, normalizeFileId } from '@mango/file';
import { usePreferencesStore } from './preferences';

export interface AdminBrandingConfig {
  enabled: boolean;
  title: string;
  shortTitle: string;
  subtitle: string;
  loginTitle: string;
  loginSubtitle: string;
  logoFile: string;
  faviconFile: string;
  loginImageFile: string;
  footerCopyright: string;
  icp: string;
  contact: string;
}

export interface AdminBrandingState extends AdminBrandingConfig {
  logoUrl: string;
  faviconUrl: string;
  loginImageUrl: string;
  loaded: boolean;
}

export const DEFAULT_ADMIN_BRANDING: AdminBrandingConfig = {
  enabled: true,
  title: 'Mango Admin',
  shortTitle: 'Mango',
  subtitle: '企业级管理平台',
  loginTitle: 'Mango Admin',
  loginSubtitle: '企业级管理平台',
  logoFile: '',
  faviconFile: '',
  loginImageFile: '',
  footerCopyright: '© Mango',
  icp: '',
  contact: '',
};

export const useAdminBrandingStore = defineStore('adminBranding', {
  state: (): AdminBrandingState => ({
    ...DEFAULT_ADMIN_BRANDING,
    logoUrl: '',
    faviconUrl: '',
    loginImageUrl: '',
    loaded: false,
  }),
  actions: {
    async loadPublicConfig() {
      const config = await fetchAdminBrandingPublic().catch(() => DEFAULT_ADMIN_BRANDING);
      const normalized = normalizeBrandingConfig(config);
      const [logoUrl, faviconUrl, loginImageUrl] = await Promise.all([
        resolveFilePreviewUrl(normalized.logoFile),
        resolveFilePreviewUrl(normalized.faviconFile),
        resolveFilePreviewUrl(normalized.loginImageFile),
      ]);

      this.$patch({
        ...normalized,
        logoUrl,
        faviconUrl,
        loginImageUrl,
        loaded: true,
      });
      applyBrandingToRuntime(this);
    },
  },
});

function fetchAdminBrandingPublic() {
  return get<AdminBrandingConfig>('/system/admin-branding/public', {
    ignoreToken: true,
    silentError: true,
  });
}

function normalizeBrandingConfig(config?: Partial<AdminBrandingConfig>): AdminBrandingConfig {
  return {
    enabled: config?.enabled !== false,
    title: normalizeText(config?.title, DEFAULT_ADMIN_BRANDING.title),
    shortTitle: normalizeText(config?.shortTitle, DEFAULT_ADMIN_BRANDING.shortTitle),
    subtitle: normalizeText(config?.subtitle, DEFAULT_ADMIN_BRANDING.subtitle),
    loginTitle: normalizeText(config?.loginTitle, DEFAULT_ADMIN_BRANDING.loginTitle),
    loginSubtitle: normalizeText(config?.loginSubtitle, DEFAULT_ADMIN_BRANDING.loginSubtitle),
    logoFile: normalizeText(config?.logoFile, ''),
    faviconFile: normalizeText(config?.faviconFile, ''),
    loginImageFile: normalizeText(config?.loginImageFile, ''),
    footerCopyright: normalizeText(config?.footerCopyright, DEFAULT_ADMIN_BRANDING.footerCopyright),
    icp: normalizeText(config?.icp, ''),
    contact: normalizeText(config?.contact, ''),
  };
}

function normalizeText(value: unknown, fallback: string): string {
  const text = typeof value === 'string' ? value.trim() : '';
  return text || fallback;
}

async function resolveFilePreviewUrl(value?: string): Promise<string> {
  const fileId = normalizeFileId(value);
  if (!fileId) {
    return '';
  }
  try {
    const preview = await fileApi.preview(fileId);
    return fileRuntimeUrl(preview);
  } catch {
    return '';
  }
}

function applyBrandingToRuntime(branding: AdminBrandingState) {
  if (!branding.enabled) {
    applyEnabledBrandingToRuntime({
      ...DEFAULT_ADMIN_BRANDING,
      logoUrl: '',
      faviconUrl: '',
      loginImageUrl: '',
      loaded: true,
    });
    return;
  }
  applyEnabledBrandingToRuntime(branding);
}

function applyEnabledBrandingToRuntime(branding: AdminBrandingState) {
  const preferencesStore = usePreferencesStore();
  preferencesStore.globalTitle = branding.title;
  preferencesStore.shortTitle = branding.shortTitle;
  preferencesStore.logoUrl = branding.logoUrl;
  preferencesStore.faviconUrl = branding.faviconUrl;
  preferencesStore.footerCopyright = branding.footerCopyright;
  preferencesStore.footerIcp = branding.icp;
  preferencesStore.footerContact = branding.contact;
  preferencesStore.footerAuthor = branding.footerCopyright || branding.title;

  installMangoAuth(undefined, {
    login: {
      brand: {
        title: branding.loginTitle,
        subtitle: branding.loginSubtitle,
        imageUrl: branding.loginImageUrl,
        logoUrl: branding.logoUrl,
      },
    },
  });
  applyFavicon(branding.faviconUrl);
}

function applyFavicon(url: string) {
  if (typeof document === 'undefined') {
    return;
  }
  let link = document.querySelector<HTMLLinkElement>('link[rel="icon"]');
  if (!link) {
    link = document.createElement('link');
    link.rel = 'icon';
    document.head.appendChild(link);
  }
  link.href = url || '/favicon.ico';
}
