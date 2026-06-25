import { get } from '@mango/common/utils/request';
import { inject, provide, readonly, ref, type InjectionKey, type Ref } from 'vue';

export type ApiId = string;

export interface SiteResolveQuery {
  siteCode?: string;
  domain?: string;
}

export interface SiteResolve {
  siteId?: ApiId;
  siteCode?: string;
  siteName?: string;
  status?: string;
  seoTitle?: string;
  seoKeywords?: string;
  seoDescription?: string;
  footerCopyright?: string;
  icpRecord?: string;
  contactInfo?: string;
}

export interface CmsSite {
  id?: ApiId;
  siteName?: string;
  siteCode?: string;
  logoFileId?: string;
  logoUrl?: string;
  description?: string;
  domain?: string;
  defaultLanguage?: string;
  seoTitle?: string;
  seoKeywords?: string;
  seoDescription?: string;
  footerCopyright?: string;
  icpRecord?: string;
  contactInfo?: string;
}

export interface SiteCategory {
  id?: ApiId;
  parentId?: ApiId;
  categoryName?: string;
  categoryCode?: string;
  categoryType?: string;
  accessPath?: string;
  externalUrl?: string;
  sort?: number;
  seoTitle?: string;
  seoKeywords?: string;
  seoDescription?: string;
  children?: SiteCategory[];
}

export interface SiteNavigation {
  id?: ApiId;
  navType?: string;
  navName?: string;
  jumpType?: string;
  categoryId?: ApiId;
  contentId?: ApiId;
  externalUrl?: string;
  openTarget?: string;
  sort?: number;
}

export interface SiteBanner {
  id?: ApiId;
  position?: string;
  title?: string;
  subtitle?: string;
  mediaType?: string;
  mediaFileId?: string;
  jumpUrl?: string;
  startTime?: string;
  endTime?: string;
  sort?: number;
}

export interface SiteAdvertisement {
  id?: ApiId;
  adCode?: string;
  adName?: string;
  position?: string;
  positionType?: string;
  adType?: string;
  materialType?: string;
  materialFileId?: string;
  title?: string;
  textContent?: string;
  richContent?: string;
  htmlContent?: string;
  imageFileId?: string;
  imageFileIds?: string;
  imageUrl?: string;
  imageUrls?: string;
  videoFileId?: string;
  coverFileId?: string;
  videoUrl?: string;
  coverUrl?: string;
  jumpUrl?: string;
  openTarget?: string;
  startTime?: string;
  endTime?: string;
  sort?: number;
}

export interface SiteContent {
  id?: ApiId;
  title?: string;
  subtitle?: string;
  summary?: string;
  contentType?: string;
  coverFileId?: string;
  coverUrl?: string;
  body?: string;
  externalUrl?: string;
  attachmentFileId?: string;
  attachmentUrl?: string;
  videoFileId?: string;
  videoUrl?: string;
  source?: string;
  author?: string;
  categoryId?: ApiId;
  categoryName?: string;
  seoTitle?: string;
  seoKeywords?: string;
  seoDescription?: string;
  publishTime?: string;
}

export type CmsSiteCategory = SiteCategory;
export type CmsNavigation = SiteNavigation;
export type CmsBanner = SiteBanner;
export type CmsAdvertisement = SiteAdvertisement;
export type CmsContent = SiteContent;

export interface PageResult<T> {
  list: T[];
  total: number;
  page: number;
  size: number;
  pages?: number;
  pageNum?: number;
  pageSize?: number;
}

export interface SiteShellState {
  site: Ref<SiteResolve | null>;
  loading: Ref<boolean>;
  error: Ref<string>;
  resolve: (query?: SiteResolveQuery) => Promise<SiteResolve>;
}

const SiteShellKey: InjectionKey<SiteShellState> = Symbol('MangoSiteShell');

export const cmsSiteApi = {
  resolveSite: (params: SiteResolveQuery) => get<SiteResolve>('/cms/open/sites/resolve', publicRequest(params)),
  detailSite: (params: SiteResolveQuery) => get<CmsSite>('/cms/open/sites/detail', publicRequest(params)),
  treeCategories: (params: SiteResolveQuery) => get<SiteCategory[]>('/cms/open/site-categories/tree', publicRequest(params)),
  listNavigations: (params: SiteResolveQuery & { navType?: string }) => get<SiteNavigation[]>('/cms/open/navigations/list', publicRequest(params)),
  listBanners: (params: SiteResolveQuery & { position?: string }) => get<SiteBanner[]>('/cms/open/banners/list', publicRequest(params)),
  listAdvertisements: (params: SiteResolveQuery & { position?: string }) => get<SiteAdvertisement[]>('/cms/open/advertisements/list', publicRequest(params)),
  pageContents: (params: SiteResolveQuery & { page?: number; size?: number; pageNum?: number; pageSize?: number; categoryId?: ApiId; keyword?: string; recommendationType?: string }) =>
    get<PageResult<SiteContent>>('/cms/open/contents/page', publicRequest(params)),
  detailContent: (params: SiteResolveQuery & { contentId: ApiId; categoryId?: ApiId }) =>
    get<SiteContent>('/cms/open/contents/detail', publicRequest(params)),
};

function publicRequest<T extends Record<string, unknown>>(params: T) {
  return { params, ignoreToken: true, silentError: true };
}

export function createSiteShellState(defaultQuery: SiteResolveQuery = {}): SiteShellState {
  const site = ref<SiteResolve | null>(null);
  const loading = ref(false);
  const error = ref('');

  async function resolve(query: SiteResolveQuery = defaultQuery) {
    loading.value = true;
    error.value = '';
    try {
      const nextSite = await cmsSiteApi.resolveSite(normalizeSiteQuery(query));
      site.value = nextSite;
      applySiteSeo(nextSite);
      return nextSite;
    } catch (err) {
      error.value = err instanceof Error ? err.message : '站点加载失败';
      throw err;
    } finally {
      loading.value = false;
    }
  }

  return {
    site: readonly(site) as Ref<SiteResolve | null>,
    loading: readonly(loading) as Ref<boolean>,
    error: readonly(error) as Ref<string>,
    resolve,
  };
}

export function provideSiteShell(state: SiteShellState) {
  provide(SiteShellKey, state);
  return state;
}

export function useSiteShell() {
  const state = inject(SiteShellKey);
  if (!state) {
    throw new Error('Site shell state is not provided.');
  }
  return state;
}

export function normalizeSiteQuery(query: SiteResolveQuery = {}): SiteResolveQuery {
  if (query.siteCode || query.domain) {
    return query;
  }
  if (typeof window === 'undefined') {
    return query;
  }
  return { ...query, domain: window.location.host };
}

export function createSiteResolveQuery(siteCode?: string, domain?: string): SiteResolveQuery {
  if (siteCode && siteCode.trim()) {
    return { siteCode: siteCode.trim() };
  }
  if (domain && domain.trim()) {
    return { domain: domain.trim() };
  }
  return normalizeSiteQuery();
}

export function applySiteSeo(site: SiteResolve) {
  if (typeof document === 'undefined') {
    return;
  }
  document.title = site.seoTitle || site.siteName || 'Mango Site';
  setMeta('keywords', site.seoKeywords || '');
  setMeta('description', site.seoDescription || '');
}

function setMeta(name: string, content: string) {
  let meta = document.querySelector(`meta[name="${name}"]`);
  if (!meta) {
    meta = document.createElement('meta');
    meta.setAttribute('name', name);
    document.head.appendChild(meta);
  }
  meta.setAttribute('content', content);
}
