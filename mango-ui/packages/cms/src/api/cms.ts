import { del, get, post, put } from '@mango/common/utils/request';

export type ApiId = string;

export interface PageResult<T> {
  list: T[];
  total: number;
  pageNum: number;
  pageSize: number;
  pages?: number;
}

type BackendPageResult<T> = {
  list?: T[];
  total?: number;
  page?: number;
  size?: number;
  pages?: number;
};

export type CmsStatus = 'ENABLED' | 'DISABLED';
export type CmsContentStatus = 'DRAFT' | 'PENDING_REVIEW' | 'PUBLISHED' | 'REJECTED' | 'OFFLINE';

export interface CmsBaseRecord {
  id?: ApiId;
  createdAt?: string;
  updatedAt?: string;
}

export interface CmsSite extends CmsBaseRecord {
  siteName?: string;
  siteCode?: string;
  logoFileId?: string;
  description?: string;
  domain?: string;
  status?: CmsStatus;
  defaultLanguage?: string;
  seoTitle?: string;
  seoKeywords?: string;
  seoDescription?: string;
  footerCopyright?: string;
  icpRecord?: string;
  contactInfo?: string;
}

export interface CmsContentCategory extends CmsBaseRecord {
  parentId?: ApiId;
  categoryCode?: string;
  categoryName?: string;
  sort?: number;
  status?: CmsStatus;
  remark?: string;
  children?: CmsContentCategory[];
}

export interface CmsContentTag extends CmsBaseRecord {
  tagCode?: string;
  tagName?: string;
  sort?: number;
  status?: CmsStatus;
  remark?: string;
}

export interface CmsSiteCategory extends CmsBaseRecord {
  siteId?: ApiId;
  parentId?: ApiId;
  categoryName?: string;
  categoryCode?: string;
  categoryType?: string;
  accessPath?: string;
  externalUrl?: string;
  sort?: number;
  visibleStatus?: CmsStatus;
  accessType?: string;
  roleCodes?: string;
  seoTitle?: string;
  seoKeywords?: string;
  seoDescription?: string;
  children?: CmsSiteCategory[];
}

export interface CmsContent extends CmsBaseRecord {
  title?: string;
  subtitle?: string;
  summary?: string;
  contentType?: string;
  coverFileId?: string;
  body?: string;
  externalUrl?: string;
  attachmentFileId?: string;
  videoFileId?: string;
  source?: string;
  author?: string;
  categoryId?: ApiId;
  categoryName?: string;
  tagIds?: ApiId[];
  tags?: CmsContentTag[];
  seoTitle?: string;
  seoKeywords?: string;
  seoDescription?: string;
  status?: CmsContentStatus;
  publishTime?: string;
  offlineTime?: string;
  reviewComment?: string;
}

export interface CmsContentPublish extends CmsBaseRecord {
  contentId?: ApiId;
  contentTitle?: string;
  siteId?: ApiId;
  siteName?: string;
  categoryId?: ApiId;
  categoryName?: string;
  publishStatus?: string;
  publishTime?: string;
  scheduledPublishTime?: string;
  offlineTime?: string;
  top?: boolean;
  topScope?: string;
  recommended?: boolean;
  recommendationType?: string;
  sort?: number;
}

export interface CmsNavigation extends CmsBaseRecord {
  siteId?: ApiId;
  navType?: string;
  navName?: string;
  jumpType?: string;
  categoryId?: ApiId;
  contentId?: ApiId;
  externalUrl?: string;
  openTarget?: string;
  sort?: number;
  status?: CmsStatus;
}

export interface CmsAdvertisement extends CmsBaseRecord {
  siteId?: ApiId;
  adCode?: string;
  adName?: string;
  position?: string;
  positionType?: string;
  supportedMaterialTypes?: string;
  width?: number;
  height?: number;
  remark?: string;
  sort?: number;
  status?: CmsStatus;
}

export interface CmsAdDelivery extends CmsBaseRecord {
  siteId?: ApiId;
  adId?: ApiId;
  adName?: string;
  adCode?: string;
  position?: string;
  positionType?: string;
  deliveryName?: string;
  materialType?: string;
  title?: string;
  textContent?: string;
  richContent?: string;
  htmlContent?: string;
  imageFileId?: string;
  imageFileIds?: string | string[];
  videoFileId?: string;
  coverFileId?: string;
  jumpUrl?: string;
  openTarget?: string;
  startTime?: string;
  endTime?: string;
  sort?: number;
  status?: CmsStatus;
}

export interface CmsSiteSetting {
  id?: ApiId;
  siteId?: ApiId;
  seoTitle?: string;
  seoKeywords?: string;
  seoDescription?: string;
  footerCopyright?: string;
  icpRecord?: string;
  contactInfo?: string;
}

export interface CmsPageQuery {
  pageNum?: number;
  pageSize?: number;
  keyword?: string;
  status?: string;
  siteId?: ApiId | '';
  categoryId?: ApiId | '';
  contentId?: ApiId | '';
  contentType?: string;
  navType?: string;
  position?: string;
  adId?: ApiId | '';
  materialType?: string;
}

export function requestErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return fallback;
}

function toPageParams(params: CmsPageQuery) {
  const next: Record<string, unknown> = {};
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') {
      return;
    }
    if (key === 'pageNum') {
      next.page = value;
      return;
    }
    if (key === 'pageSize') {
      next.size = value;
      return;
    }
    next[key] = value;
  });
  return next;
}

function normalizePageResult<T>(result: BackendPageResult<T>): PageResult<T> {
  return {
    list: result.list || [],
    total: Number(result.total || 0),
    pageNum: Number(result.page || 1),
    pageSize: Number(result.size || 10),
    pages: Number(result.pages || 0),
  };
}

function pageGet<T>(url: string, params: CmsPageQuery) {
  return get<BackendPageResult<T>>(url, { params: toPageParams(params) }).then(normalizePageResult);
}

export const cmsApi = {
  pageSites: (params: CmsPageQuery) => pageGet<CmsSite>('/cms/sites/page', params),
  detailSite: (id: ApiId) => get<CmsSite>('/cms/sites/detail', { params: { id } }),
  createSite: (data: CmsSite) => post<ApiId>('/cms/sites', data),
  updateSite: (data: CmsSite) => put<boolean>('/cms/sites', data),
  updateSiteStatus: (id: ApiId, status: CmsStatus) => put<boolean>('/cms/sites/status', { id, status }),
  deleteSite: (id: ApiId) => del<boolean>('/cms/sites', { params: { id } }),

  pageContentCategories: (params: CmsPageQuery) => pageGet<CmsContentCategory>('/cms/content-categories/page', params),
  listContentCategories: (params: CmsPageQuery) => get<CmsContentCategory[]>('/cms/content-categories/list', { params }),
  treeContentCategories: (params: CmsPageQuery) => get<CmsContentCategory[]>('/cms/content-categories/tree', { params }),
  createContentCategory: (data: CmsContentCategory) => post<ApiId>('/cms/content-categories', data),
  updateContentCategory: (data: CmsContentCategory) => put<boolean>('/cms/content-categories', data),
  updateContentCategoryStatus: (id: ApiId, status: CmsStatus) => put<boolean>('/cms/content-categories/status', { id, status }),
  deleteContentCategory: (id: ApiId) => del<boolean>('/cms/content-categories', { params: { id } }),

  pageContentTags: (params: CmsPageQuery) => pageGet<CmsContentTag>('/cms/content-tags/page', params),
  listContentTags: (params: CmsPageQuery) => get<CmsContentTag[]>('/cms/content-tags/list', { params }),
  createContentTag: (data: CmsContentTag) => post<ApiId>('/cms/content-tags', data),
  updateContentTag: (data: CmsContentTag) => put<boolean>('/cms/content-tags', data),
  updateContentTagStatus: (id: ApiId, status: CmsStatus) => put<boolean>('/cms/content-tags/status', { id, status }),
  deleteContentTag: (id: ApiId) => del<boolean>('/cms/content-tags', { params: { id } }),

  treeSiteCategories: (params: { siteId?: ApiId | ''; status?: string }) => get<CmsSiteCategory[]>('/cms/site-categories/tree', { params }),
  createSiteCategory: (data: CmsSiteCategory) => post<ApiId>('/cms/site-categories', data),
  updateSiteCategory: (data: CmsSiteCategory) => put<boolean>('/cms/site-categories', data),
  updateSiteCategoryStatus: (id: ApiId, status: CmsStatus) => put<boolean>('/cms/site-categories/status', { id, status }),
  deleteSiteCategory: (id: ApiId) => del<boolean>('/cms/site-categories', { params: { id } }),

  pageContents: (params: CmsPageQuery) => pageGet<CmsContent>('/cms/contents/page', params),
  detailContent: (id: ApiId) => get<CmsContent>('/cms/contents/detail', { params: { id } }),
  createContent: (data: CmsContent) => post<ApiId>('/cms/contents', data),
  updateContent: (data: CmsContent) => put<boolean>('/cms/contents', data),
  submitContent: (id: ApiId) => post<boolean>('/cms/contents/submit', { id }),
  approveContent: (id: ApiId, reviewComment?: string) => post<boolean>('/cms/contents/approve', { id, reviewComment }),
  rejectContent: (id: ApiId, reviewComment?: string) => post<boolean>('/cms/contents/reject', { id, reviewComment }),
  offlineContent: (id: ApiId) => post<boolean>('/cms/contents/offline', { id }),
  deleteContent: (id: ApiId) => del<boolean>('/cms/contents', { params: { id } }),

  pagePublishes: (params: CmsPageQuery) => pageGet<CmsContentPublish>('/cms/content-publishes/page', params),
  publishContents: (data: {
    contentIds: ApiId[];
    siteId: ApiId;
    categoryIds: ApiId[];
    publishTime?: string;
    scheduledPublishTime?: string;
    offlineTime?: string;
    top?: boolean;
    topScope?: string;
    recommended?: boolean;
    recommendationType?: string;
    sort?: number;
  }) => post<boolean>('/cms/content-publishes/publish', data),
  offlinePublish: (id: ApiId) => post<boolean>('/cms/content-publishes/offline', { id }),
  deletePublish: (id: ApiId) => del<boolean>('/cms/content-publishes', { params: { id } }),

  pageNavigations: (params: CmsPageQuery) => pageGet<CmsNavigation>('/cms/navigations/page', params),
  createNavigation: (data: CmsNavigation) => post<ApiId>('/cms/navigations', data),
  updateNavigation: (data: CmsNavigation) => put<boolean>('/cms/navigations', data),
  updateNavigationStatus: (id: ApiId, status: CmsStatus) => put<boolean>('/cms/navigations/status', { id, status }),
  deleteNavigation: (id: ApiId) => del<boolean>('/cms/navigations', { params: { id } }),

  pageAdvertisements: (params: CmsPageQuery) => pageGet<CmsAdvertisement>('/cms/advertisements/page', params),
  createAdvertisement: (data: CmsAdvertisement) => post<ApiId>('/cms/advertisements', data),
  updateAdvertisement: (data: CmsAdvertisement) => put<boolean>('/cms/advertisements', data),
  updateAdvertisementStatus: (id: ApiId, status: CmsStatus) => put<boolean>('/cms/advertisements/status', { id, status }),
  deleteAdvertisement: (id: ApiId) => del<boolean>('/cms/advertisements', { params: { id } }),

  pageAdDeliveries: (params: CmsPageQuery) => pageGet<CmsAdDelivery>('/cms/ad-deliveries/page', params),
  createAdDelivery: (data: CmsAdDelivery) => post<ApiId>('/cms/ad-deliveries', data),
  updateAdDelivery: (data: CmsAdDelivery) => put<boolean>('/cms/ad-deliveries', data),
  updateAdDeliveryStatus: (id: ApiId, status: CmsStatus) => put<boolean>('/cms/ad-deliveries/status', { id, status }),
  deleteAdDelivery: (id: ApiId) => del<boolean>('/cms/ad-deliveries', { params: { id } }),

  detailSiteSetting: (siteId: ApiId) => get<CmsSiteSetting>('/cms/site-settings/detail', { params: { siteId } }),
  saveSiteSetting: (data: CmsSiteSetting) => put<boolean>('/cms/site-settings', data),
};
