<template>
  <div class="enterprise-site" data-testid="enterprise-site">
    <div v-if="loading" class="site-loading" data-testid="site-loading">站点加载中...</div>

    <section id="top" class="site-hero" :class="{ 'is-loading': loading, 'is-inner': currentRoute.view !== 'home' }">
      <header class="site-header">
        <a class="brand" href="/" :aria-label="siteTitle" @click="handleInternalLink('/', $event)">
          <span class="brand-mark">{{ brandInitial }}</span>
          <span>
            <strong>{{ siteTitle }}</strong>
            <small>{{ siteTagline }}</small>
          </span>
        </a>

        <nav v-if="topNavigations.length" class="top-nav" aria-label="主导航">
          <a
            v-for="item in topNavigations"
            :key="item.id || item.navName"
            :href="navigationHref(item)"
            :target="navigationTarget(item)"
            :rel="navigationTarget(item) === '_blank' ? 'noopener noreferrer' : undefined"
            :class="{ active: isNavigationActive(item) }"
            :aria-current="isNavigationActive(item) ? 'page' : undefined"
            @click="handleNavigationClick(item, $event)"
          >
            {{ item.navName }}
          </a>
        </nav>
        <div v-else class="nav-empty">导航未配置</div>
      </header>

      <div v-if="error" class="site-error" data-testid="site-error">
        <strong>站点加载失败</strong>
        <span>{{ error }}</span>
        <button type="button" @click="reload">重试</button>
      </div>

      <div v-if="!error && currentRoute.view === 'home'" class="hero-layout">
        <div class="hero-copy">
          <p class="section-kicker">Mango Technology</p>
          <template v-if="activeBanner">
            <h1>{{ adTitle(activeBanner) || siteTitle }}</h1>
            <p v-if="adPlainText(activeBanner)" class="hero-lead">{{ adPlainText(activeBanner) }}</p>
            <div class="hero-actions">
              <a :href="adHref(activeBanner, '#products')" class="primary-link" @click="handleInternalLink(adHref(activeBanner, '#products'), $event)">了解产品能力</a>
              <a v-if="newsRoute" :href="newsRoute" class="ghost-link" @click="handleInternalLink(newsRoute, $event)">查看新闻动态</a>
            </div>
          </template>
          <div v-else class="empty-block">
            <strong>首页 Banner 未配置</strong>
            <span>请在 CMS 广告位管理和广告投放管理中配置 HOME_HERO 展示位。</span>
          </div>
        </div>

        <div class="hero-media" aria-label="企业数字化平台能力">
          <div v-if="activeBanner" class="hero-panel">
            <a
              v-if="adPrimaryImage(activeBanner)"
              class="hero-banner-link"
              :href="adHref(activeBanner, '#products')"
              :target="adTarget(activeBanner)"
              :rel="adTarget(activeBanner) === '_blank' ? 'noopener noreferrer' : undefined"
              @click="handleAdClick(activeBanner, '#products', $event)"
            >
              <img :src="adPrimaryImage(activeBanner)" :alt="adTitle(activeBanner)" />
            </a>
            <div v-else class="platform-window">
              <div class="window-bar">
                <span />
                <span />
                <span />
              </div>
              <div class="window-body">
                <div class="platform-copy">
                  <strong>{{ siteTitle }}</strong>
                  <span>{{ siteDescription }}</span>
                </div>
                <div class="platform-metrics">
                  <span>
                    <strong>{{ flatCategories.length }}</strong>
                    公开栏目
                  </span>
                  <span>
                    <strong>{{ contents.length }}</strong>
                    发布内容
                  </span>
                  <span>
                    <strong>{{ routePromotions.length }}</strong>
                    投放内容
                  </span>
                </div>
                <div class="platform-lines" aria-hidden="true">
                  <i />
                  <i />
                  <i />
                  <i />
                </div>
              </div>
            </div>
          </div>
          <div v-if="heroBanners.length > 1" class="banner-dots" aria-label="Banner 切换">
            <button
              v-for="(banner, index) in heroBanners"
              :key="banner.id || banner.adCode || index"
              type="button"
              :class="{ active: index === activeBannerIndex }"
              :aria-label="`切换到第 ${index + 1} 个广告投放`"
              @click="activeBannerIndex = index"
            />
          </div>
        </div>
      </div>
    </section>

    <main v-if="!error">
      <template v-if="currentRoute.view === 'home'">
      <section class="site-info-section" aria-label="站点信息">
        <div class="section-shell site-info-grid">
          <article class="site-summary">
            <p class="section-kicker">企业官网</p>
            <h2>{{ siteTitle }}</h2>
            <p>{{ siteDescription }}</p>
          </article>

          <aside class="ad-panel" data-testid="advertisement-panel">
            <template v-if="primaryAdvertisement">
              <p class="section-kicker">运营公告</p>
              <AdContent :ad="primaryAdvertisement" variant="panel" />
              <a v-if="primaryAdvertisement.jumpUrl" :href="primaryAdvertisement.jumpUrl">立即咨询</a>
            </template>
            <div v-else class="empty-block compact">
              <strong>广告位未配置</strong>
              <span>请在 CMS 广告管理中配置 HOME_FLOAT 展示位。</span>
            </div>
          </aside>
        </div>
      </section>

      <section id="products" class="category-section">
        <div class="section-shell">
          <div class="section-heading">
            <p class="section-kicker">业务布局</p>
            <h2>围绕企业数字化的完整能力</h2>
            <p>栏目由后台站点管理维护，用于组织官网导航、内容分发和专题入口。</p>
          </div>

          <div v-if="displayCategories.length" class="category-grid">
            <article
              v-for="(category, index) in displayCategories"
              :id="`category-${category.id}`"
              :key="category.id || category.categoryCode || category.categoryName"
              class="category-card"
            >
              <span>{{ formatIndex(index + 1) }}</span>
              <h3>{{ category.categoryName }}</h3>
              <p>{{ productCategorySummary(category) }}</p>
            </article>
          </div>
          <div v-else class="empty-section">站点栏目未配置</div>
        </div>
      </section>

      <section id="contents" class="content-section">
        <div class="section-shell">
          <div class="section-heading">
            <p class="section-kicker">新闻动态</p>
            <h2>聚焦产品进展与企业动态</h2>
            <p>首页仅展示内容摘要，完整列表和富文本详情请进入独立栏目页面查看。</p>
          </div>
          <div v-if="contentsLoading" class="empty-section">内容加载中...</div>
          <div v-else-if="homeNews.length" class="home-news-grid">
            <article v-for="item in homeNews" :key="item.id || item.title" class="home-news-card">
              <span>{{ formatDate(item.publishTime) }}</span>
              <h3>{{ item.title }}</h3>
              <p>{{ item.summary || item.subtitle || '该内容暂无摘要。' }}</p>
              <a :href="contentHref(item, '/news')" @click="handleInternalLink(contentHref(item, '/news'), $event)">查看详情</a>
            </article>
          </div>
          <div v-else class="empty-section">当前暂无已发布内容</div>
          <div class="section-actions">
            <a v-if="newsNavigation" :href="newsRoute" @click="handleInternalLink(newsRoute, $event)">进入{{ newsNavigation.navName }}</a>
            <a v-if="companyNewsNavigation" :href="companyNewsRoute" @click="handleInternalLink(companyNewsRoute, $event)">进入{{ companyNewsNavigation.navName }}</a>
          </div>
        </div>
      </section>
      </template>

      <template v-else>
      <section class="page-title-section">
        <div class="section-shell page-title-layout">
          <div>
            <p class="section-kicker">Mango Technology</p>
            <h1>{{ activeBanner ? adTitle(activeBanner) : routeTitle }}</h1>
            <p>{{ activeBanner ? adPlainText(activeBanner) : routeDescription }}</p>
          </div>
          <a
            v-if="activeBanner && adPrimaryImage(activeBanner)"
            class="page-banner-media"
            :href="adHref(activeBanner, currentRoute.basePath)"
            :target="adTarget(activeBanner)"
            :rel="adTarget(activeBanner) === '_blank' ? 'noopener noreferrer' : undefined"
            @click="handleAdClick(activeBanner, currentRoute.basePath, $event)"
          >
            <img :src="adPrimaryImage(activeBanner)" :alt="adTitle(activeBanner)" />
          </a>
          <a href="/" class="back-home-link" @click="handleInternalLink('/', $event)">返回首页</a>
        </div>
      </section>

      <section v-if="currentRoute.view === 'list'" class="content-section">
        <div class="section-shell content-layout">
          <div class="content-main">
            <div v-if="advertisements.length" class="list-ad-strip">
              <a
                v-for="ad in advertisements"
                :key="ad.id || ad.adCode || ad.adName"
                :href="adHref(ad, currentRoute.basePath)"
                class="list-ad"
                @click="handleInternalLink(adHref(ad, currentRoute.basePath), $event)"
              >
                <AdContent :ad="ad" variant="strip" />
              </a>
            </div>

            <div v-if="contentsLoading" class="empty-section">内容加载中...</div>
            <div v-else-if="contents.length" class="news-list">
              <a
                v-for="item in contents"
                :key="item.id || item.title"
                :href="contentHref(item, currentRoute.path)"
                class="news-item"
                @click="handleInternalLink(contentHref(item, currentRoute.path), $event)"
              >
                <span class="news-date">{{ formatDate(item.publishTime) }}</span>
                <span class="news-cover" :class="{ empty: !item.coverUrl }">
                  <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.title" />
                  <span v-else>无图</span>
                </span>
                <span class="news-body">
                  <span class="news-tags">
                    <small class="content-type-tag">{{ contentTypeLabel(item.contentType) }}</small>
                    <small>{{ item.categoryName || item.source || item.author || 'CMS 内容' }}</small>
                  </span>
                  <strong>{{ item.title }}</strong>
                  <em>{{ item.summary || item.subtitle || '该内容暂无摘要。' }}</em>
                </span>
              </a>
            </div>
            <div v-else class="empty-section">当前栏目暂无已发布内容</div>

            <nav v-if="contentPage.total > contentPage.pageSize" class="content-pagination" aria-label="内容分页">
              <button type="button" :disabled="contentPage.pageNum <= 1 || contentsLoading" @click="changeContentPage(contentPage.pageNum - 1)">
                上一页
              </button>
              <button
                v-for="pageNo in contentPageNumbers"
                :key="pageNo"
                type="button"
                :class="{ active: pageNo === contentPage.pageNum }"
                :aria-current="pageNo === contentPage.pageNum ? 'page' : undefined"
                :disabled="contentsLoading"
                @click="changeContentPage(pageNo)"
              >
                {{ pageNo }}
              </button>
              <button type="button" :disabled="contentPage.pageNum >= contentPageTotalPages || contentsLoading" @click="changeContentPage(contentPage.pageNum + 1)">
                下一页
              </button>
              <span>共 {{ contentPage.total }} 条</span>
            </nav>
          </div>
        </div>
      </section>

      <section v-else class="detail-section">
        <div class="section-shell detail-layout">
          <article class="article-detail" data-testid="content-detail">
            <p class="section-kicker">{{ routeTitle }}</p>
            <template v-if="detailLoading">
              <div class="empty-section">详情加载中...</div>
            </template>
            <template v-else-if="selectedContent">
              <h2>{{ selectedContent.title }}</h2>
              <div class="article-meta">
                <span>{{ contentTypeLabel(selectedContent.contentType) }}</span>
                <span>{{ selectedContent.categoryName || 'CMS 内容' }}</span>
                <span>{{ formatDate(selectedContent.publishTime) }}</span>
                <span v-if="selectedContent.author">{{ selectedContent.author }}</span>
                <span v-if="selectedContent.source">{{ selectedContent.source }}</span>
              </div>
              <img
                v-if="selectedContent.coverUrl"
                class="article-cover"
                :src="selectedContent.coverUrl"
                :alt="selectedContent.title"
              />
              <video
                v-if="selectedContent.videoUrl"
                class="article-video"
                :src="selectedContent.videoUrl"
                :poster="selectedContent.coverUrl || undefined"
                controls
                playsinline
              />
              <div v-if="selectedContent.body" class="rich-text" v-html="renderRichText(selectedContent.body)" />
              <div v-else class="empty-block compact">
                <strong>正文未配置</strong>
                <span>请在内容管理中维护富文本正文。</span>
              </div>
              <a
                v-if="selectedContent.attachmentUrl"
                class="article-attachment"
                :href="selectedContent.attachmentUrl"
                target="_blank"
                rel="noopener noreferrer"
              >
                查看附件
              </a>
            </template>
            <div v-else class="empty-section">内容不存在或尚未发布</div>
          </article>

          <aside class="side-ad-list">
            <p class="section-kicker">推荐关注</p>
            <a
              v-for="ad in advertisements"
              :key="ad.id || ad.adCode || ad.adName"
              :href="adHref(ad, currentRoute.basePath)"
              class="side-ad"
              @click="handleInternalLink(adHref(ad, currentRoute.basePath), $event)"
            >
              <AdContent :ad="ad" variant="side" />
            </a>
            <div v-if="!advertisements.length" class="empty-block compact">
              <strong>广告列表为空</strong>
              <span>请配置公开广告位。</span>
            </div>
          </aside>
        </div>
      </section>
      </template>
    </main>

    <footer class="site-footer">
      <div class="section-shell footer-grid">
        <div>
          <strong>{{ siteTitle }}</strong>
          <p>{{ siteDescription }}</p>
        </div>
        <div>
          <span>站点导航</span>
          <a
            v-for="item in topNavigations"
            :key="item.id || item.navName"
            :href="navigationHref(item)"
            @click="handleNavigationClick(item, $event)"
          >
            {{ item.navName }}
          </a>
          <em v-if="!topNavigations.length">未配置</em>
        </div>
        <div>
          <span>栏目</span>
          <a
            v-for="item in flatCategories.slice(0, 5)"
            :key="item.id || item.categoryCode"
            :href="item.id ? `/#category-${item.id}` : '/#products'"
            @click="handleInternalLink(item.id ? `/#category-${item.id}` : '/#products', $event)"
          >
            {{ item.categoryName }}
          </a>
          <em v-if="!flatCategories.length">未配置</em>
        </div>
        <div>
          <span>友情链接</span>
          <a
            v-for="item in footerNavigations"
            :key="item.id || item.navName"
            :href="navigationHref(item)"
            :target="navigationTarget(item)"
            :rel="navigationTarget(item) === '_blank' ? 'noopener noreferrer' : undefined"
            @click="handleNavigationClick(item, $event)"
          >
            {{ item.navName }}
          </a>
          <em v-if="!footerNavigations.length">未配置</em>
        </div>
        <div>
          <span>联系方式</span>
          <p>{{ site?.contactInfo || '未配置' }}</p>
          <p>{{ site?.icpRecord || '未配置备案号' }}</p>
        </div>
      </div>
      <div class="footer-bottom section-shell">
        <span>{{ site?.footerCopyright || '未配置版权信息' }}</span>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import {
  applySiteSeo,
  cmsSiteApi,
  createSiteResolveQuery,
  type CmsAdvertisement,
  type CmsContent,
  type CmsNavigation,
  type CmsSiteCategory,
  type SiteResolve,
  type SiteResolveQuery,
} from '@mango/site-shell';

interface ContentPageState {
  total: number;
  pageNum: number;
  pageSize: number;
}

interface SiteRouteState {
  path: string;
  view: 'home' | 'list' | 'detail';
  basePath: string;
  contentId?: string;
}

const site = ref<SiteResolve | null>(null);
const topNavigations = ref<CmsNavigation[]>([]);
const footerNavigations = ref<CmsNavigation[]>([]);
const heroBanners = ref<CmsAdvertisement[]>([]);
const advertisements = ref<CmsAdvertisement[]>([]);
const categories = ref<CmsSiteCategory[]>([]);
const contents = ref<CmsContent[]>([]);
const selectedContent = ref<CmsContent | null>(null);
const selectedCategoryId = ref('');
const activeBannerIndex = ref(0);
const contentPage = ref<ContentPageState>({ total: 0, pageNum: 1, pageSize: 8 });
const loading = ref(true);
const contentsLoading = ref(false);
const detailLoading = ref(false);
const error = ref('');
const currentRoute = ref<SiteRouteState>(parseRoute(window.location.pathname));
const currentHash = ref(window.location.hash);

const siteQuery = computed<SiteResolveQuery>(() => createSiteResolveQuery(undefined, window.location.host));
const siteTitle = computed(() => site.value?.siteName || 'CMS 站点未解析');
const siteTagline = computed(() => site.value?.seoTitle || site.value?.siteCode || 'CMS SITE');
const siteDescription = computed(() => site.value?.seoDescription || '站点描述未配置，请在 CMS 站点配置中维护 SEO 描述。');
const brandInitial = computed(() => (siteTitle.value.trim().charAt(0) || 'M').toUpperCase());
const activeBanner = computed(() => heroBanners.value[activeBannerIndex.value] || null);
const primaryAdvertisement = computed(() => advertisements.value[0] || null);
const routePromotions = computed(() => [...heroBanners.value, ...advertisements.value]);
const flatCategories = computed(() => flattenCategories(categories.value).slice(0, 8));
const displayCategories = computed(() => flatCategories.value.slice(0, 6));
const contentPageTotalPages = computed(() => Math.max(1, Math.ceil(contentPage.value.total / contentPage.value.pageSize)));
const contentPageNumbers = computed(() => {
  const total = contentPageTotalPages.value;
  const current = contentPage.value.pageNum;
  const start = Math.max(1, Math.min(current - 2, total - 4));
  const end = Math.min(total, start + 4);
  return Array.from({ length: end - start + 1 }, (_, index) => start + index);
});
const currentCategory = computed(() => findCategoryByPath(currentRoute.value.basePath));
const currentNavigation = computed(() => findNavigationByPath(currentRoute.value.basePath));
const newsNavigation = computed(() => findNavigationByName(/新闻|资讯|动态/));
const companyNewsNavigation = computed(() => findNavigationByName(/公司动态|企业动态|公司新闻/));
const newsRoute = computed(() => newsNavigation.value ? navigationHref(newsNavigation.value) : '');
const companyNewsRoute = computed(() => companyNewsNavigation.value ? navigationHref(companyNewsNavigation.value) : '');
const routeTitle = computed(() => {
  return currentNavigation.value?.navName || currentCategory.value?.categoryName || '内容列表';
});
const routeDescription = computed(() => {
  if (currentRoute.value.view === 'detail') return '富文本正文来自 CMS 内容发布接口，支持分享、刷新和独立访问。';
  return currentCategory.value?.seoDescription || currentCategory.value?.seoTitle || '公开内容由 CMS 发布接口提供。';
});
const routeCategoryId = computed(() => {
  return currentCategory.value?.id || currentNavigation.value?.categoryId || '';
});
const homeNews = computed(() => {
  const newsItems = contents.value.filter(item => /新闻|动态/.test(item.categoryName || ''));
  return (newsItems.length ? newsItems : contents.value).slice(0, 3);
});
onMounted(() => {
  window.addEventListener('popstate', handlePopState);
  window.addEventListener('hashchange', handleHashChange);
  void loadSite();
});

onBeforeUnmount(() => {
  window.removeEventListener('popstate', handlePopState);
  window.removeEventListener('hashchange', handleHashChange);
});

watch(() => currentRoute.value.path, () => {
  selectedContent.value = null;
  selectedCategoryId.value = routeCategoryId.value;
  contentPage.value = { ...contentPage.value, pageNum: 1 };
  void loadContents();
});

function findNavigationByName(pattern: RegExp) {
  return topNavigations.value.find(item => pattern.test(item.navName || '')) || null;
}

function findNavigationByPath(path: string) {
  return topNavigations.value.find(item => navigationHref(item) === path) || null;
}

function findCategoryByPath(path: string) {
  return flatCategories.value.find(item => normalizeCategoryPath(item) === path) || null;
}

function parseRoute(pathname: string): SiteRouteState {
  const normalized = pathname.split('#')[0].replace(/\/+$/, '') || '/';
  const sectionPath = `/${normalized.split('/').filter(Boolean)[0] || ''}`;
  const supportedSections = ['/products', '/solutions', '/news', '/company-news', '/about'];
  if (supportedSections.includes(sectionPath) && normalized !== sectionPath) {
    return { path: normalized, view: 'detail', basePath: sectionPath, contentId: normalized.split('/')[2] };
  }
  if (supportedSections.includes(normalized)) {
    return { path: normalized, view: 'list', basePath: normalized };
  }
  return { path: '/', view: 'home', basePath: '/news' };
}

function handlePopState() {
  currentRoute.value = parseRoute(window.location.pathname);
  currentHash.value = window.location.hash;
  scrollToCurrentHash();
}

function handleHashChange() {
  currentHash.value = window.location.hash;
}

function navigateTo(path: string) {
  const normalized = path || '/';
  const nextRoute = parseRoute(normalized);
  window.history.pushState({}, '', normalized);
  currentRoute.value = nextRoute;
  currentHash.value = window.location.hash;
  scrollToCurrentHash();
}

function handleInternalLink(path: string, event: MouseEvent) {
  if (path.startsWith('http')) {
    return;
  }
  event.preventDefault();
  navigateTo(path.startsWith('#') ? `/${path}` : path);
}

function handleAdClick(ad: CmsAdvertisement, fallback: string, event: MouseEvent) {
  const href = adHref(ad, fallback);
  if (href.startsWith('http')) {
    return;
  }
  handleInternalLink(href, event);
}

function scrollToCurrentHash() {
  void nextTick(() => {
    if (!window.location.hash) {
      window.scrollTo({ top: 0 });
      return;
    }
    document.querySelector(window.location.hash)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  });
}

function contentHref(content: CmsContent, fallbackBasePath = currentRoute.value.basePath) {
  const basePath = categoryPathById(content.categoryId) || fallbackBasePath;
  return content.id ? `${basePath}/${content.id}` : basePath;
}

function categoryPathById(categoryId?: string) {
  if (!categoryId) return '';
  const category = flatCategories.value.find(item => item.id === categoryId);
  const path = category ? normalizeCategoryPath(category) : '';
  return path.startsWith('/') && !path.includes('#') ? path : '';
}

async function loadSite() {
  loading.value = true;
  error.value = '';
  selectedContent.value = null;
  try {
    site.value = await cmsSiteApi.resolveSite(siteQuery.value);
    applySiteSeo(site.value);
    const [navigationData, footerNavigationData, categoryData] = await Promise.all([
      cmsSiteApi.listNavigations({ ...siteQuery.value, navType: 'TOP' }),
      cmsSiteApi.listNavigations({ ...siteQuery.value, navType: 'FOOTER' }),
      cmsSiteApi.treeCategories(siteQuery.value),
    ]);
    topNavigations.value = sortBySort(navigationData);
    footerNavigations.value = sortBySort(footerNavigationData);
    categories.value = categoryData || [];
    selectedCategoryId.value = routeCategoryId.value;
    await loadContents();
  } catch (err) {
    error.value = err instanceof Error ? err.message : '站点加载失败';
  } finally {
    loading.value = false;
  }
}

async function loadContents() {
  await loadRoutePromotions();
  if (currentRoute.value.view === 'detail' && currentRoute.value.contentId) {
    await loadContentDetailById(currentRoute.value.contentId);
    return;
  }
  contentsLoading.value = true;
  try {
    const categoryId = selectedCategoryId.value || (currentRoute.value.view === 'home' ? '' : routeCategoryId.value);
    const pageNum = currentRoute.value.view === 'home' ? 1 : contentPage.value.pageNum;
    const pageSize = contentPage.value.pageSize;
    const page = await cmsSiteApi.pageContents({
      ...siteQuery.value,
      page: pageNum,
      size: pageSize,
      pageNum,
      pageSize,
      categoryId: categoryId || undefined,
    });
    contents.value = page.list || [];
    contentPage.value = {
      total: Number(page.total || 0),
      pageNum: Number(page.pageNum || page.page || 1),
      pageSize: Number(page.pageSize || page.size || contentPage.value.pageSize),
    };
    selectedContent.value = null;
  } catch (err) {
    error.value = err instanceof Error ? err.message : '内容加载失败';
  } finally {
    contentsLoading.value = false;
  }
}

function changeContentPage(pageNo: number) {
  const next = Math.max(1, Math.min(pageNo, contentPageTotalPages.value));
  if (next === contentPage.value.pageNum || contentsLoading.value) {
    return;
  }
  contentPage.value = { ...contentPage.value, pageNum: next };
  void loadContents().then(() => window.scrollTo({ top: 0, behavior: 'smooth' }));
}

async function loadRoutePromotions() {
  activeBannerIndex.value = 0;
  const [bannerData, advertisementData] = await Promise.all([
    cmsSiteApi.listAdvertisements({ ...siteQuery.value, position: routeBannerPosition() }),
    cmsSiteApi.listAdvertisements({ ...siteQuery.value, position: routeAdPosition() }),
  ]);
  heroBanners.value = sortBySort(bannerData);
  advertisements.value = sortBySort(advertisementData);
}

async function loadContentDetailById(contentId: string) {
  detailLoading.value = true;
  try {
    selectedContent.value = await cmsSiteApi.detailContent({
      ...siteQuery.value,
      contentId,
      categoryId: routeCategoryId.value || undefined,
    });
  } catch {
    selectedContent.value = null;
  } finally {
    detailLoading.value = false;
  }
}

function reload() {
  void loadSite();
}

function navigationHref(item: CmsNavigation) {
  if (item.externalUrl) return item.externalUrl.startsWith('#') ? `/${item.externalUrl}` : item.externalUrl;
  if (item.contentId) return contentNavigationHref(item);
  if (item.categoryId) {
    const category = flatCategories.value.find(entry => entry.id === item.categoryId);
    const categoryPath = category ? normalizeCategoryPath(category) : '';
    return categoryPath || `/#category-${item.categoryId}`;
  }
  return '/';
}

function contentNavigationHref(item: CmsNavigation) {
  const matchedContent = contents.value.find(content => content.id === item.contentId);
  const matchedCategoryPath = matchedContent ? categoryPathById(matchedContent.categoryId) : '';
  if (matchedCategoryPath) return `${matchedCategoryPath}/${item.contentId}`;
  const normalizedName = item.navName || '';
  if (/解决方案/.test(normalizedName)) return `/solutions/${item.contentId}`;
  if (/关于/.test(normalizedName)) return `/about/${item.contentId}`;
  if (/产品/.test(normalizedName)) return `/products/${item.contentId}`;
  return `/news/${item.contentId}`;
}

function routeBannerPosition() {
  if (currentRoute.value.view === 'home') return 'HOME_HERO';
  return `${positionPrefix(currentRoute.value.basePath)}_BANNER`;
}

function routeAdPosition() {
  if (currentRoute.value.view === 'home') return 'HOME_FLOAT';
  return `${positionPrefix(currentRoute.value.basePath)}_TEXT`;
}

function positionPrefix(path: string) {
  const normalized = path.replace(/^\/+/, '').replace(/-/g, '_').toUpperCase();
  return normalized || 'HOME';
}

function normalizeCategoryPath(category: CmsSiteCategory) {
  const rawPath = category.externalUrl || category.accessPath || '';
  if (!rawPath) return '';
  if (rawPath.startsWith('http') || rawPath.startsWith('#')) return rawPath;
  return rawPath.startsWith('/') ? rawPath : `/${rawPath}`;
}

function navigationTarget(item: CmsNavigation) {
  return item.openTarget === 'BLANK' ? '_blank' : '_self';
}

function isNavigationActive(item: CmsNavigation) {
  const href = navigationHref(item);
  if (href.startsWith('http')) return false;
  const [path, hash = ''] = href.split('#');
  const normalizedPath = path.replace(/\/+$/, '') || '/';
  const normalizedCurrentPath = currentRoute.value.path;
  if (hash) {
    return normalizedCurrentPath === '/' && currentHash.value === `#${hash}`;
  }
  if (normalizedPath === '/') {
    return normalizedCurrentPath === '/' && !currentHash.value;
  }
  return normalizedCurrentPath === normalizedPath || normalizedCurrentPath.startsWith(`${normalizedPath}/`);
}

function handleNavigationClick(item: CmsNavigation, event: MouseEvent) {
  const href = navigationHref(item);
  if (href.startsWith('/') || href.startsWith('#')) {
    handleInternalLink(href, event);
  }
}

function flattenCategories(items: CmsSiteCategory[], level = 0): CmsSiteCategory[] {
  return items.flatMap((item) => [
    { ...item, categoryName: level > 0 ? `${'　'.repeat(level)}${item.categoryName || ''}` : item.categoryName },
    ...flattenCategories(item.children || [], level + 1),
  ]);
}

function sortBySort<T extends { sort?: number }>(items: T[] = []) {
  return [...items].sort((left, right) => Number(left.sort || 0) - Number(right.sort || 0));
}

function formatDate(value?: string) {
  if (!value) return '未发布';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value.slice(0, 10);
  return date.toISOString().slice(0, 10);
}

function formatIndex(value: number) {
  return String(value).padStart(2, '0');
}

function contentTypeLabel(value?: string) {
  const labels: Record<string, string> = {
    ARTICLE: '文章',
    IMAGE_TEXT: '图文',
    PAGE: '单页',
    ATTACHMENT: '附件',
    VIDEO: '视频',
  };
  return labels[String(value || '')] || '内容';
}

function adTitle(ad: CmsAdvertisement) {
  return ad.title || ad.adName || ad.adCode || '广告投放';
}

function adPlainText(ad: CmsAdvertisement) {
  const text = ad.textContent || stripHtml(ad.richContent) || stripHtml(ad.htmlContent);
  return text || '由 CMS 广告投放接口提供';
}

function adHref(ad: CmsAdvertisement, fallback = '/') {
  return ad.jumpUrl || fallback;
}

function adTarget(ad: CmsAdvertisement) {
  return ad.openTarget === 'BLANK' || adHref(ad).startsWith('http') ? '_blank' : '_self';
}

function stripHtml(value?: string) {
  return String(value || '').replace(/<[^>]*>/g, '').replace(/\s+/g, ' ').trim();
}

function adImageUrls(ad: CmsAdvertisement) {
  if (ad.imageUrl) {
    return [String(ad.imageUrl)];
  }
  if (ad.imageUrls) {
    return String(ad.imageUrls)
      .split(',')
      .map(item => item.trim())
      .filter(Boolean);
  }
  return [];
}

function adPrimaryImage(ad: CmsAdvertisement) {
  return adImageUrls(ad)[0] || '';
}

function productCategorySummary(category: CmsSiteCategory) {
  return category.seoDescription || category.seoTitle || '该产品能力由 CMS 站点栏目维护，并通过官网首页锚点展示。';
}

function sanitizeRichText(html: string) {
  const template = document.createElement('template');
  template.innerHTML = html;
  template.content.querySelectorAll('script, style, iframe, object, embed, link, meta').forEach(element => element.remove());
  template.content.querySelectorAll('*').forEach((element) => {
    Array.from(element.attributes).forEach((attribute) => {
      const name = attribute.name.toLowerCase();
      const value = attribute.value.trim();
      if (name.startsWith('on') || name === 'style') {
        element.removeAttribute(attribute.name);
        return;
      }
      if ((name === 'href' || name === 'src') && /^(javascript|data):/i.test(value)) {
        element.removeAttribute(attribute.name);
      }
    });
  });
  return template.innerHTML;
}

function renderRichText(html: string) {
  return sanitizeRichText(html).replace(/mango-file:([1-9]\d*)/g, (_match, fileId: string) => {
    return `/api/cms-api/files/public-preview?id=${encodeURIComponent(fileId)}&domain=${encodeURIComponent(window.location.host)}`;
  });
}

const AdContent = defineComponent({
  name: 'AdContent',
  props: {
    ad: {
      type: Object as () => CmsAdvertisement,
      required: true,
    },
    variant: {
      type: String,
      default: 'panel',
    },
  },
  setup(props) {
    return () => {
      const materialType = props.ad.materialType || 'TEXT';
      const title = adTitle(props.ad);
      const text = adPlainText(props.ad);
      const images = adImageUrls(props.ad);
      const children = [
        h('strong', title),
      ];

      if (materialType === 'RICH_TEXT' && props.ad.richContent) {
        children.push(h('div', {
          class: 'ad-rich-text',
          innerHTML: sanitizeRichText(props.ad.richContent),
        }));
      } else if (materialType === 'HTML' && props.ad.htmlContent) {
        children.push(h('div', {
          class: 'ad-rich-text',
          innerHTML: sanitizeRichText(props.ad.htmlContent),
        }));
      } else if (materialType === 'VIDEO') {
        const videoUrl = props.ad.videoUrl || '';
        const coverUrl = props.ad.coverUrl || '';
        children.push(videoUrl
          ? h('video', { class: 'ad-video', src: videoUrl, poster: coverUrl || undefined, controls: true, playsinline: true })
          : h('em', '视频素材已投放，请检查文件访问权限。'));
        if (text) children.push(h('em', text));
      } else if (['IMAGE', 'SINGLE_IMAGE', 'MULTI_IMAGE'].includes(materialType)) {
        if (images.length) {
          children.push(h('div', { class: 'ad-image-grid' }, images.map((url, index) =>
            h('img', { src: url, alt: `${title} ${index + 1}` }),
          )));
        } else {
          children.push(h('em', '图片素材已投放，请检查文件访问权限。'));
        }
        if (text) children.push(h('em', text));
      } else {
        children.push(h('em', text));
      }

      return h('div', { class: ['ad-content', `is-${props.variant}`] }, children);
    };
  },
});
</script>
