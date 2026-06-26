import { cmsApi, type CmsPageQuery } from '../api/cms';
import type { CmsResourceConfig } from './CmsResourceView.vue';

type CmsPageData = Awaited<ReturnType<typeof cmsApi.pageSites>>;

function createCategoryCode(categoryName: unknown, accessPath: unknown) {
  const pathCode = String(accessPath || '')
    .trim()
    .split('/')
    .filter(Boolean)
    .pop();
  if (pathCode) {
    return pathCode.replace(/[^A-Za-z0-9_.:-]/g, '-').replace(/-+/g, '-');
  }
  const nameCode = String(categoryName || '')
    .trim()
    .replace(/[^A-Za-z0-9_.:-]+/g, '-')
    .replace(/^-+|-+$/g, '');
  return nameCode || `category-${Date.now()}`;
}

export const siteConfig: CmsResourceConfig = {
  key: 'sites',
  title: '站点管理',
  description: '维护官网、帮助中心等站点基础信息、域名和 SEO 配置。',
  statusField: true,
  canToggleStatus: true,
  columns: [
    { prop: 'siteName', subProp: 'siteCode', label: '站点', type: 'name', minWidth: 240 },
    { prop: 'domain', label: '域名', minWidth: 180 },
    { prop: 'defaultLanguage', label: '默认语言', width: 110 },
    { prop: 'status', label: '状态', type: 'status', width: 100 },
  ],
  page: query => cmsApi.pageSites(query) as Promise<CmsPageData>,
  create: data => cmsApi.createSite(data),
  update: data => cmsApi.updateSite(data),
  updateStatus: cmsApi.updateSiteStatus,
  remove: cmsApi.deleteSite,
  editor: {
    defaults: { status: 'ENABLED', defaultLanguage: 'zh-CN' },
    fields: [
      { prop: 'siteName', label: '站点名称', required: true },
      { prop: 'siteCode', label: '站点编码', required: true },
      { prop: 'domain', label: '域名' },
      { prop: 'defaultLanguage', label: '默认语言' },
      { prop: 'logoFileId', label: 'Logo 文件ID' },
      { prop: 'status', label: '状态', type: 'select', optionKey: 'status', required: true },
      { prop: 'description', label: '站点描述', type: 'textarea', span: 24 },
      { prop: 'seoTitle', label: 'SEO 标题' },
      { prop: 'seoKeywords', label: 'SEO 关键词' },
      { prop: 'seoDescription', label: 'SEO 描述', type: 'textarea', span: 24 },
      { prop: 'footerCopyright', label: '版权信息' },
      { prop: 'icpRecord', label: '备案号' },
      { prop: 'contactInfo', label: '联系方式', type: 'textarea', span: 24 },
    ],
  },
  detail: {
    groups: [
      {
        title: '基础信息',
        fields: [
          { prop: 'siteName', label: '站点名称' },
          { prop: 'siteCode', label: '站点编码' },
          { prop: 'domain', label: '域名' },
          { prop: 'defaultLanguage', label: '默认语言' },
          { prop: 'status', label: '状态', optionKey: 'status' },
          { prop: 'logoFileId', label: 'Logo 文件ID' },
          { prop: 'description', label: '站点描述', type: 'textarea', detailSpan: 2 },
        ],
      },
      {
        title: 'SEO 配置',
        fields: [
          { prop: 'seoTitle', label: 'SEO 标题' },
          { prop: 'seoKeywords', label: 'SEO 关键词' },
          { prop: 'seoDescription', label: 'SEO 描述', type: 'textarea', detailSpan: 2 },
        ],
      },
      {
        title: '页脚与联系信息',
        fields: [
          { prop: 'footerCopyright', label: '版权信息' },
          { prop: 'icpRecord', label: '备案号' },
          { prop: 'contactInfo', label: '联系方式', type: 'textarea', detailSpan: 2 },
        ],
      },
    ],
  },
};

export const contentCategoryConfig: CmsResourceConfig = {
  key: 'contentCategories',
  title: '内容分类',
  description: '维护内容中心的通用分类，用于文章、单页和附件归类。',
  statusField: true,
  canToggleStatus: true,
  tree: true,
  hidePagination: true,
  columns: [
    { prop: 'categoryName', label: '分类名称', type: 'treeName', minWidth: 220 },
    { prop: 'categoryCode', label: '分类编码', minWidth: 160 },
    { prop: 'sort', label: '排序', width: 90 },
    { prop: 'status', label: '状态', type: 'status', width: 100 },
    { prop: 'remark', label: '备注', minWidth: 180 },
  ],
  page: async (query) => {
    const list = await cmsApi.treeContentCategories(query);
    return { list, total: list.length, pageNum: 1, pageSize: list.length || 10 };
  },
  create: data => cmsApi.createContentCategory(data),
  update: data => cmsApi.updateContentCategory(data),
  updateStatus: cmsApi.updateContentCategoryStatus,
  remove: cmsApi.deleteContentCategory,
  editor: {
    defaults: { status: 'ENABLED', sort: 0 },
    fields: [
      { prop: 'categoryName', label: '分类名称', required: true },
      { prop: 'categoryCode', label: '分类编码', required: true },
      { prop: 'parentId', label: '父级分类', type: 'treeSelect', optionKey: 'contentCategories' },
      { prop: 'sort', label: '排序', type: 'number' },
      { prop: 'status', label: '状态', type: 'select', optionKey: 'status', required: true },
      { prop: 'remark', label: '备注', type: 'textarea', span: 24 },
    ],
  },
};

export const contentTagConfig: CmsResourceConfig = {
  key: 'contentTags',
  title: '内容标签',
  description: '维护可复用内容标签，支持内容运营筛选和展示。',
  statusField: true,
  canToggleStatus: true,
  columns: [
    { prop: 'tagName', subProp: 'tagCode', label: '标签', type: 'name', minWidth: 260 },
    { prop: 'sort', label: '排序', width: 90 },
    { prop: 'status', label: '状态', type: 'status', width: 100 },
    { prop: 'remark', label: '备注', minWidth: 180 },
  ],
  page: query => cmsApi.pageContentTags(query) as Promise<CmsPageData>,
  create: data => cmsApi.createContentTag(data),
  update: data => cmsApi.updateContentTag(data),
  updateStatus: cmsApi.updateContentTagStatus,
  remove: cmsApi.deleteContentTag,
  editor: {
    defaults: { status: 'ENABLED', sort: 0 },
    fields: [
      { prop: 'tagName', label: '标签名称', required: true },
      { prop: 'tagCode', label: '标签编码', required: true },
      { prop: 'sort', label: '排序', type: 'number' },
      { prop: 'status', label: '状态', type: 'select', optionKey: 'status', required: true },
      { prop: 'remark', label: '备注', type: 'textarea', span: 24 },
    ],
  },
};

export const siteCategoryConfig: CmsResourceConfig = {
  key: 'siteCategories',
  title: '站点栏目',
  description: '维护官网栏目树、访问路径、显示状态和排序。',
  filterSite: true,
  requireSiteForPage: true,
  statusField: true,
  statusProp: 'visibleStatus',
  canToggleStatus: true,
  tree: true,
  hidePagination: true,
  columns: [
    { prop: 'categoryName', label: '栏目', type: 'treeName', minWidth: 240 },
    { prop: 'siteId', label: '站点', type: 'site', minWidth: 160 },
    { prop: 'accessPath', label: '访问路径', minWidth: 180 },
    { prop: 'visibleStatus', label: '显示状态', type: 'status', width: 110 },
    { prop: 'sort', label: '排序', width: 90 },
  ],
  page: async (query: CmsPageQuery) => {
    const list = await cmsApi.treeSiteCategories({ siteId: query.siteId, status: query.status });
    return { list, total: list.length, pageNum: query.pageNum || 1, pageSize: query.pageSize || 10 };
  },
  create: data => cmsApi.createSiteCategory(data),
  update: data => cmsApi.updateSiteCategory(data),
  updateStatus: cmsApi.updateSiteCategoryStatus,
  remove: cmsApi.deleteSiteCategory,
  normalizePayload: (payload, form) => ({
    ...payload,
    categoryCode: String(form.categoryCode || payload.categoryCode || createCategoryCode(payload.categoryName, payload.accessPath)),
    categoryType: String(form.categoryType || (payload.externalUrl ? 'LINK' : 'LIST')),
    accessType: String(form.accessType || 'PUBLIC'),
    visibleStatus: String(payload.visibleStatus || form.visibleStatus || 'ENABLED'),
  }),
  editor: {
    defaults: { visibleStatus: 'ENABLED', sort: 0 },
    fields: [
      { prop: 'siteId', label: '所属站点', type: 'select', optionKey: 'sites', required: true },
      { prop: 'categoryName', label: '栏目名称', required: true },
      { prop: 'parentId', label: '父级栏目', type: 'treeSelect', optionKey: 'siteCategories' },
      { prop: 'accessPath', label: '访问路径' },
      { prop: 'externalUrl', label: '外链地址' },
      { prop: 'visibleStatus', label: '显示状态', type: 'select', optionKey: 'status', required: true },
      { prop: 'sort', label: '排序', type: 'number' },
    ],
  },
};

export const contentConfig: CmsResourceConfig = {
  key: 'contents',
  title: '内容管理',
  description: '维护文章、单页、图文、视频和附件内容，支持审核发布状态流转。',
  statusField: true,
  columns: [
    { prop: 'coverFileId', label: '封面', type: 'cover', width: 122 },
    { prop: 'title', subProp: 'summary', label: '内容', type: 'name', minWidth: 320 },
    { prop: 'contentType', subProp: 'categoryName', label: '类型/分类', type: 'meta', optionKey: 'contentType', minWidth: 160 },
    { prop: 'status', label: '状态', type: 'contentStatus', width: 110 },
    { prop: 'author', label: '作者', width: 120 },
  ],
  page: query => cmsApi.pageContents(query) as Promise<CmsPageData>,
  create: data => cmsApi.createContent(data),
  update: data => cmsApi.updateContent(data),
  remove: cmsApi.deleteContent,
  editor: {
    defaults: { contentType: 'ARTICLE' },
    fields: [
      { prop: 'title', label: '标题', required: true, span: 24 },
      { prop: 'subtitle', label: '副标题', span: 24 },
      { prop: 'contentType', label: '内容类型', type: 'select', optionKey: 'contentType', required: true },
      { prop: 'categoryId', label: '内容分类', type: 'treeSelect', optionKey: 'contentCategories', required: true },
      {
        prop: 'coverFileId',
        label: '封面图片',
        type: 'upload',
        span: 24,
        upload: {
          display: 'thumbnail',
          count: 1,
          fmt: 'jpg,png,jpeg,webp',
          bizType: 'cms-content-cover',
          purpose: 'cms-content-cover',
          accessLevel: 'PUBLIC_READ',
          buttonText: '上传封面',
        },
      },
      { prop: 'source', label: '来源', span: 24 },
      { prop: 'author', label: '作者', span: 24 },
      { prop: 'externalUrl', label: '外部链接', span: 24 },
      { prop: 'summary', label: '摘要', type: 'textarea', span: 24 },
      { prop: 'body', label: '正文', type: 'richText', height: 420, span: 24 },
    ],
  },
  detail: {
    groups: [
      {
        title: '基础信息',
        fields: [
          { prop: 'title', label: '标题', detailSpan: 2 },
          { prop: 'subtitle', label: '副标题', detailSpan: 2 },
          { prop: 'contentType', label: '内容类型', optionKey: 'contentType' },
          { prop: 'categoryName', label: '内容分类' },
          { prop: 'status', label: '状态', optionKey: 'contentStatus' },
          { prop: 'author', label: '作者' },
          { prop: 'source', label: '来源' },
          { prop: 'externalUrl', label: '外部链接' },
          { prop: 'summary', label: '摘要', type: 'textarea', detailSpan: 2 },
        ],
      },
      {
        title: '正文',
        fields: [
          { prop: 'body', label: '正文', type: 'richText', detailSpan: 2 },
        ],
      },
    ],
  },
};

export const publishConfig: CmsResourceConfig = {
  key: 'contentPublishes',
  title: '内容发布',
  description: '查看内容与站点栏目的发布关系、定时发布和下线状态。',
  filterSite: true,
  statusField: true,
  columns: [
    { prop: 'contentTitle', subProp: 'categoryName', label: '发布内容', type: 'name', minWidth: 320 },
    { prop: 'siteId', label: '站点', type: 'site', minWidth: 160 },
    { prop: 'publishStatus', label: '发布状态', optionKey: 'publishStatus', width: 110 },
    { prop: 'scheduledPublishTime', label: '定时发布时间', minWidth: 170 },
    { prop: 'offlineTime', label: '下线时间', minWidth: 170 },
  ],
  page: query => cmsApi.pagePublishes(query) as Promise<CmsPageData>,
  create: data => cmsApi.publishContents({
    contentIds: (data.contentIds as string[]) || [],
    siteId: String(data.siteId || ''),
    categoryIds: (data.categoryIds as string[]) || [],
    scheduledPublishTime: data.scheduledPublishTime as string | undefined,
    offlineTime: data.offlineTime as string | undefined,
    top: Boolean(data.top),
    topScope: data.topScope as string | undefined,
    recommended: Boolean(data.recommended),
    recommendationType: data.recommendationType as string | undefined,
    sort: Number(data.sort || 0),
  }),
  update: data => cmsApi.publishContents({
    contentIds: (data.contentIds as string[]) || [],
    siteId: String(data.siteId || ''),
    categoryIds: (data.categoryIds as string[]) || [],
    scheduledPublishTime: data.scheduledPublishTime as string | undefined,
    offlineTime: data.offlineTime as string | undefined,
    top: Boolean(data.top),
    topScope: data.topScope as string | undefined,
    recommended: Boolean(data.recommended),
    recommendationType: data.recommendationType as string | undefined,
    sort: Number(data.sort || 0),
  }),
  remove: cmsApi.deletePublish,
  editor: {
    defaults: { contentIds: [], categoryIds: [], top: false, recommended: false, topScope: 'CATEGORY', recommendationType: 'HOME', sort: 0 },
    fields: [
      { prop: 'siteId', label: '所属站点', type: 'select', optionKey: 'sites', required: true },
      { prop: 'contentIds', label: '发布内容', type: 'multiSelect', optionKey: 'contents', required: true, span: 24 },
      { prop: 'categoryIds', label: '发布栏目', type: 'treeSelect', optionKey: 'siteCategories', required: true, multiple: true },
      { prop: 'top', label: '置顶', type: 'switch' },
      { prop: 'topScope', label: '置顶范围' },
      { prop: 'recommended', label: '推荐', type: 'switch' },
      { prop: 'recommendationType', label: '推荐类型' },
      { prop: 'scheduledPublishTime', label: '定时发布时间' },
      { prop: 'offlineTime', label: '下线时间' },
      { prop: 'sort', label: '排序', type: 'number' },
    ],
  },
};

export const navigationConfig: CmsResourceConfig = {
  key: 'navigations',
  title: '导航管理',
  description: '维护站点顶部、底部和快捷导航，支持栏目、内容和链接跳转。',
  filterSite: true,
  statusField: true,
  canToggleStatus: true,
  columns: [
    { prop: 'navName', label: '导航名称', type: 'name', minWidth: 220 },
    { prop: 'siteId', label: '站点', type: 'site', minWidth: 160 },
    { prop: 'navType', label: '导航位置', optionKey: 'navType', width: 110 },
    { prop: 'jumpType', label: '导航类型', optionKey: 'jumpType', width: 100 },
    { prop: 'openTarget', label: '打开方式', optionKey: 'openTarget', width: 110 },
    { prop: 'status', label: '状态', type: 'status', width: 100 },
  ],
  page: query => cmsApi.pageNavigations(query) as Promise<CmsPageData>,
  create: data => cmsApi.createNavigation(data),
  update: data => cmsApi.updateNavigation(data),
  updateStatus: cmsApi.updateNavigationStatus,
  remove: cmsApi.deleteNavigation,
  normalizePayload: (payload, form) => {
    const navType = String(payload.navType || form.navType || 'TOP');
    const jumpType = String(payload.jumpType || form.jumpType || 'CATEGORY');
    const next = {
      ...payload,
      navType,
      jumpType,
      status: String(payload.status || form.status || 'ENABLED'),
      sort: Number(payload.sort || form.sort || 0),
      openTarget: jumpType === 'URL'
        ? String(payload.openTarget || form.openTarget || 'SELF')
        : 'SELF',
      categoryId: jumpType === 'CATEGORY' ? payload.categoryId || form.categoryId : undefined,
      contentId: jumpType === 'CONTENT' ? payload.contentId || form.contentId : undefined,
      externalUrl: jumpType === 'URL' ? payload.externalUrl || form.externalUrl : undefined,
    };
    return next;
  },
  editor: {
    defaults: { navType: 'TOP', jumpType: 'CATEGORY', openTarget: 'SELF', status: 'ENABLED', sort: 0 },
    fields: [
      { prop: 'siteId', label: '所属站点', type: 'select', optionKey: 'sites', required: true },
      { prop: 'navName', label: '导航名称', required: true },
      { prop: 'navType', label: '导航位置', type: 'select', optionKey: 'navType', required: true },
      { prop: 'jumpType', label: '导航类型', type: 'select', optionKey: 'jumpType', required: true },
      { prop: 'categoryId', label: '栏目', type: 'treeSelect', optionKey: 'siteCategories', required: true, visibleWhen: { jumpType: 'CATEGORY' } },
      { prop: 'contentId', label: '内容', type: 'select', optionKey: 'contents', required: true, visibleWhen: { jumpType: 'CONTENT' } },
      { prop: 'externalUrl', label: '链接地址', required: true, visibleWhen: { jumpType: 'URL' } },
      { prop: 'openTarget', label: '打开方式', type: 'select', optionKey: 'openTarget', visibleWhen: { jumpType: 'URL' } },
      { prop: 'sort', label: '排序', type: 'number' },
      { prop: 'status', label: '状态', type: 'select', optionKey: 'status', required: true },
    ],
  },
};

export const advertisementConfig: CmsResourceConfig = {
  key: 'advertisements',
  title: '广告位管理',
  description: '维护站点可投放位置、位置类型、尺寸和支持的物料类型。',
  filterSite: true,
  statusField: true,
  canToggleStatus: true,
  columns: [
    { prop: 'adName', subProp: 'adCode', label: '广告位', type: 'name', minWidth: 260 },
    { prop: 'siteId', label: '站点', type: 'site', minWidth: 160 },
    { prop: 'positionType', label: '位置类型', optionKey: 'positionType', width: 120 },
    { prop: 'position', label: '位置编码', minWidth: 130 },
    { prop: 'supportedMaterialTypes', label: '支持物料', optionKey: 'materialType', minWidth: 180 },
    { prop: 'status', label: '状态', type: 'status', width: 100 },
  ],
  page: query => cmsApi.pageAdvertisements(query) as Promise<CmsPageData>,
  create: data => cmsApi.createAdvertisement(data),
  update: data => cmsApi.updateAdvertisement(data),
  updateStatus: cmsApi.updateAdvertisementStatus,
  remove: cmsApi.deleteAdvertisement,
  normalizePayload: (payload, form) => {
    const supportedMaterialTypes = Array.isArray(payload.supportedMaterialTypes)
      ? payload.supportedMaterialTypes.join(',')
      : String(payload.supportedMaterialTypes || form.supportedMaterialTypes || '');
    return {
      ...payload,
      positionType: String(payload.positionType || form.positionType || 'CUSTOM'),
      supportedMaterialTypes,
      width: payload.width === undefined || payload.width === '' ? undefined : Number(payload.width),
      height: payload.height === undefined || payload.height === '' ? undefined : Number(payload.height),
      status: String(payload.status || form.status || 'ENABLED'),
      sort: Number(payload.sort || form.sort || 0),
    };
  },
  editor: {
    defaults: { positionType: 'CUSTOM', supportedMaterialTypes: ['SINGLE_IMAGE'], status: 'ENABLED', sort: 0 },
    fields: [
      { prop: 'siteId', label: '所属站点', type: 'select', optionKey: 'sites', required: true },
      { prop: 'adName', label: '广告位名称', required: true },
      { prop: 'adCode', label: '广告位编码', required: true },
      { prop: 'positionType', label: '位置类型', type: 'select', optionKey: 'positionType', required: true },
      { prop: 'position', label: '位置编码', required: true },
      { prop: 'supportedMaterialTypes', label: '支持物料', type: 'checkButtonGroup', optionKey: 'materialType', required: true, span: 24 },
      { prop: 'width', label: '推荐宽度', type: 'number' },
      { prop: 'height', label: '推荐高度', type: 'number' },
      { prop: 'sort', label: '排序', type: 'number' },
      { prop: 'status', label: '状态', type: 'select', optionKey: 'status', required: true },
      { prop: 'remark', label: '备注', type: 'textarea', span: 24 },
    ],
  },
};

export const adDeliveryConfig: CmsResourceConfig = {
  key: 'adDeliveries',
  title: '广告投放管理',
  description: '维护广告位上的投放素材、内容、跳转方式和有效期。',
  filterSite: true,
  statusField: true,
  canToggleStatus: true,
  columns: [
    { prop: 'deliveryName', subProp: 'title', label: '投放', type: 'name', minWidth: 260 },
    { prop: 'siteId', label: '站点', type: 'site', minWidth: 160 },
    { prop: 'adName', subProp: 'position', label: '广告位', type: 'name', minWidth: 220 },
    { prop: 'materialType', label: '物料', optionKey: 'materialType', width: 110 },
    { prop: 'startTime', label: '开始时间', minWidth: 170 },
    { prop: 'status', label: '状态', type: 'status', width: 100 },
  ],
  page: query => cmsApi.pageAdDeliveries(query) as Promise<CmsPageData>,
  create: data => cmsApi.createAdDelivery(data),
  update: data => cmsApi.updateAdDelivery(data),
  updateStatus: cmsApi.updateAdDeliveryStatus,
  remove: cmsApi.deleteAdDelivery,
  normalizePayload: (payload, form) => {
    const materialType = String(payload.materialType || form.materialType || 'SINGLE_IMAGE');
    const imageFileIds = Array.isArray(payload.imageFileIds)
      ? payload.imageFileIds.join(',')
      : String(payload.imageFileIds || form.imageFileIds || '');
    return {
      ...payload,
      materialType,
      imageFileId: ['IMAGE', 'SINGLE_IMAGE'].includes(materialType) ? payload.imageFileId || form.imageFileId : undefined,
      imageFileIds: materialType === 'MULTI_IMAGE' ? imageFileIds : undefined,
      videoFileId: materialType === 'VIDEO' ? payload.videoFileId || form.videoFileId : undefined,
      coverFileId: materialType === 'VIDEO' ? payload.coverFileId || form.coverFileId : undefined,
      textContent: materialType === 'TEXT' ? payload.textContent || form.textContent : undefined,
      richContent: materialType === 'RICH_TEXT' ? payload.richContent || form.richContent : undefined,
      htmlContent: materialType === 'HTML' ? payload.htmlContent || form.htmlContent : undefined,
      openTarget: String(payload.openTarget || form.openTarget || 'SELF'),
      status: String(payload.status || form.status || 'ENABLED'),
      sort: Number(payload.sort || form.sort || 0),
    };
  },
  editor: {
    defaults: { materialType: 'SINGLE_IMAGE', openTarget: 'SELF', status: 'ENABLED', sort: 0 },
    fields: [
      { prop: 'siteId', label: '所属站点', type: 'select', optionKey: 'sites', required: true },
      { prop: 'adId', label: '广告位', type: 'select', optionKey: 'advertisements', required: true },
      { prop: 'deliveryName', label: '投放名称', required: true },
      { prop: 'materialType', label: '物料类型', type: 'select', optionKey: 'materialType', required: true },
      { prop: 'title', label: '标题', span: 24 },
      { prop: 'textContent', label: '文本内容', type: 'textarea', required: true, span: 24, visibleWhen: { materialType: 'TEXT' } },
      { prop: 'richContent', label: '富文本内容', type: 'richText', required: true, height: 320, span: 24, visibleWhen: { materialType: 'RICH_TEXT' } },
      { prop: 'htmlContent', label: 'HTML 内容', type: 'textarea', required: true, rows: 6, span: 24, visibleWhen: { materialType: 'HTML' } },
      {
        prop: 'imageFileId',
        label: '图片素材',
        type: 'upload',
        required: true,
        span: 24,
        visibleWhenAny: [{ materialType: 'IMAGE' }, { materialType: 'SINGLE_IMAGE' }],
        upload: {
          display: 'thumbnail',
          count: 1,
          fmt: 'jpg,png,jpeg,webp',
          bizType: 'cms-ad-delivery',
          purpose: 'cms-ad-image',
          accessLevel: 'PUBLIC_READ',
          buttonText: '上传图片',
        },
      },
      {
        prop: 'imageFileIds',
        label: '多图素材',
        type: 'upload',
        required: true,
        span: 24,
        visibleWhen: { materialType: 'MULTI_IMAGE' },
        upload: {
          display: 'thumbnail',
          count: 6,
          fmt: 'jpg,png,jpeg,webp',
          bizType: 'cms-ad-delivery',
          purpose: 'cms-ad-images',
          accessLevel: 'PUBLIC_READ',
          buttonText: '上传图片',
        },
      },
      {
        prop: 'videoFileId',
        label: '视频素材',
        type: 'upload',
        required: true,
        span: 24,
        visibleWhen: { materialType: 'VIDEO' },
        upload: {
          display: 'list',
          count: 1,
          fmt: 'mp4,webm,mov',
          bizType: 'cms-ad-delivery',
          purpose: 'cms-ad-video',
          accessLevel: 'PUBLIC_READ',
          buttonText: '上传视频',
        },
      },
      {
        prop: 'coverFileId',
        label: '视频封面',
        type: 'upload',
        required: true,
        span: 24,
        visibleWhen: { materialType: 'VIDEO' },
        upload: {
          display: 'thumbnail',
          count: 1,
          fmt: 'jpg,png,jpeg,webp',
          bizType: 'cms-ad-delivery',
          purpose: 'cms-ad-video-cover',
          accessLevel: 'PUBLIC_READ',
          buttonText: '上传封面',
        },
      },
      { prop: 'jumpUrl', label: '跳转链接', span: 24 },
      { prop: 'openTarget', label: '打开方式', type: 'select', optionKey: 'openTarget' },
      { prop: 'startTime', label: '开始时间', type: 'datetime' },
      { prop: 'endTime', label: '结束时间', type: 'datetime' },
      { prop: 'sort', label: '排序', type: 'number' },
      { prop: 'status', label: '状态', type: 'select', optionKey: 'status', required: true },
    ],
  },
};
