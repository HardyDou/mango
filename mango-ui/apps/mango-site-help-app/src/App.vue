<template>
  <div class="help-site" data-testid="help-site">
    <header class="help-header">
      <a href="#top" class="help-brand" :aria-label="siteTitle">
        <span>{{ brandInitial }}</span>
        <strong>{{ siteTitle }}</strong>
      </a>
      <nav v-if="navigations.length" class="help-nav" aria-label="帮助站导航">
        <a
          v-for="item in navigations"
          :key="item.id || item.navName"
          :href="navigationHref(item)"
          :target="item.openTarget === 'BLANK' ? '_blank' : '_self'"
          :rel="item.openTarget === 'BLANK' ? 'noopener noreferrer' : undefined"
          @click="handleNavigationClick(item, $event)"
        >
          {{ item.navName }}
        </a>
      </nav>
      <form class="help-search" @submit.prevent="search">
        <input v-model="keyword" aria-label="搜索内容" placeholder="搜索内容" />
        <button type="submit">搜索</button>
      </form>
    </header>

    <main>
      <section id="top" class="help-hero">
        <div class="help-hero-inner">
          <div class="help-hero-copy">
            <p class="section-kicker">帮助中心</p>
            <template v-if="heroAd">
              <h1>{{ heroAd.title || heroAd.adName || siteTitle }}</h1>
              <p>{{ heroAd.textContent || siteDescription }}</p>
              <a
                :href="adHref(heroAd, '#contents')"
                :target="adTarget(heroAd)"
                :rel="adTarget(heroAd) === '_blank' ? 'noopener noreferrer' : undefined"
                @click="handleAdClick(heroAd, '#contents', $event)"
              >
                查看指南
              </a>
            </template>
            <template v-else>
              <h1>{{ siteTitle }}</h1>
              <p>{{ siteDescription }}</p>
            </template>
          </div>
          <aside class="help-hero-card" aria-label="帮助站 Banner">
            <a
              v-if="heroImage"
              :href="adHref(heroAd, '#contents')"
              :target="adTarget(heroAd)"
              :rel="adTarget(heroAd) === '_blank' ? 'noopener noreferrer' : undefined"
              @click="handleAdClick(heroAd, '#contents', $event)"
            >
              <img :src="heroImage" :alt="heroAd?.title || heroAd?.adName || siteTitle" />
            </a>
            <div v-else class="hero-card-copy">
              <span>常用入口</span>
              <strong>{{ heroAd?.title || heroAd?.adName || siteTitle }}</strong>
              <em>{{ siteDescription }}</em>
            </div>
          </aside>
        </div>
      </section>

      <section v-if="loadError" class="site-error" data-testid="site-error">
        <strong>帮助中心暂不可用</strong>
        <span>{{ loadError }}</span>
        <button type="button" @click="reload">重试</button>
      </section>

      <section v-if="!loadError" class="notice-section">
        <div class="section-shell notice-grid">
          <article class="site-summary">
            <p class="section-kicker">服务说明</p>
            <h2>{{ siteTitle }}</h2>
            <p>{{ siteDescription }}</p>
            <dl class="help-stats">
              <div>
                <dt>{{ contents.total || 0 }}</dt>
                <dd>公开文档</dd>
              </div>
              <div>
                <dt>{{ categoryOptions.length }}</dt>
                <dd>文档栏目</dd>
              </div>
              <div>
                <dt>{{ ads.length + heroAds.length }}</dt>
                <dd>投放内容</dd>
              </div>
            </dl>
          </article>
          <aside class="notice-card" data-testid="advertisement-panel">
            <p class="section-kicker">帮助公告</p>
            <template v-if="primaryAd">
              <strong>{{ primaryAd.title || primaryAd.adName }}</strong>
              <span>{{ primaryAd.textContent || primaryAd.adCode || '帮助中心公告' }}</span>
              <a
                v-if="primaryAd.jumpUrl"
                :href="primaryAd.jumpUrl"
                :target="adTarget(primaryAd)"
                :rel="adTarget(primaryAd) === '_blank' ? 'noopener noreferrer' : undefined"
              >
                查看公告
              </a>
            </template>
            <template v-else>
              <strong>公告位未配置</strong>
              <span>请在 CMS 广告管理中维护 HELP_TOP。</span>
            </template>
          </aside>
        </div>
      </section>

      <section v-if="!loadError" id="contents" class="content-section">
        <div class="section-shell help-layout">
          <aside class="category-sidebar">
            <p class="section-kicker">文档栏目</p>
            <button type="button" :class="{ active: !categoryId }" @click="selectCategory('')">全部文档</button>
            <button
              v-for="category in categoryOptions"
              :key="category.id || category.categoryCode"
              type="button"
              :class="{ active: categoryId === category.id }"
              @click="selectCategory(category.id || '')"
            >
              {{ category.categoryName }}
            </button>
          </aside>

          <div class="doc-main">
            <div class="doc-heading">
              <p class="section-kicker">使用指南</p>
              <h2>公开帮助文档</h2>
              <span>共 {{ contents.total }} 篇</span>
            </div>

            <div v-if="loading" class="empty-state">内容加载中...</div>
            <div v-else-if="contents.list.length" class="doc-list">
              <button
                v-for="item in contents.list"
                :key="item.id || item.title"
                type="button"
                class="doc-item"
                :class="{ active: selectedContent?.id === item.id }"
                @click="selectContent(item)"
              >
                <span>{{ item.categoryName || item.contentType || 'CMS' }}</span>
                <strong>{{ item.title }}</strong>
                <em>{{ item.summary || item.subtitle || '暂无摘要' }}</em>
              </button>
            </div>
            <div v-else class="empty-state">当前条件暂无公开内容</div>
          </div>
          <article class="doc-detail" data-testid="content-detail">
            <p class="section-kicker">文档详情</p>
            <template v-if="detailLoading">
              <div class="empty-state">详情加载中...</div>
            </template>
            <template v-else-if="selectedContent">
              <h2>{{ selectedContent.title }}</h2>
              <div class="doc-meta">
                <span>{{ selectedContent.source || 'CMS' }}</span>
                <span>{{ selectedContent.author || '帮助中心' }}</span>
                <span>{{ formatDate(selectedContent.publishTime) }}</span>
              </div>
              <div v-if="selectedContent.body" class="rich-text" v-html="selectedContent.body" />
              <div v-else class="empty-state">该文档暂未维护正文</div>
            </template>
            <div v-else class="empty-state">选择左侧文档后查看详情</div>
          </article>

          <aside class="ad-list">
            <p class="section-kicker">推荐关注</p>
            <a
              v-for="ad in ads"
              :key="ad.id || ad.adCode"
              :href="adHref(ad, '#contents')"
              :target="adTarget(ad)"
              :rel="adTarget(ad) === '_blank' ? 'noopener noreferrer' : undefined"
              @click="handleAdClick(ad, '#contents', $event)"
            >
              <span>{{ ad.title || ad.adCode || '帮助公告' }}</span>
              <strong>{{ ad.textContent || ad.adName }}</strong>
            </a>
            <em v-if="!ads.length">暂无公告</em>
          </aside>
        </div>
      </section>
    </main>

    <footer class="help-footer">
      <div class="section-shell footer-grid">
        <strong>{{ siteTitle }}</strong>
        <span>{{ site?.icpRecord || '未配置备案号' }}</span>
        <span>{{ site?.contactInfo || '未配置联系方式' }}</span>
        <span>{{ site?.footerCopyright || '未配置版权信息' }}</span>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import {
  applySiteSeo,
  cmsSiteApi,
  createSiteResolveQuery,
  type ApiId,
  type CmsAdvertisement,
  type CmsContent,
  type CmsNavigation,
  type CmsSiteCategory,
  type PageResult,
  type SiteResolve,
} from '@mango/site-shell';

const siteQuery = computed(() => createSiteResolveQuery(undefined, window.location.host));
const site = ref<SiteResolve | null>(null);
const categories = ref<CmsSiteCategory[]>([]);
const navigations = ref<CmsNavigation[]>([]);
const heroAds = ref<CmsAdvertisement[]>([]);
const ads = ref<CmsAdvertisement[]>([]);
const contents = ref<PageResult<CmsContent>>({ list: [], total: 0, page: 1, size: 10, pageNum: 1, pageSize: 10 });
const selectedContent = ref<CmsContent | null>(null);
const keyword = ref('');
const categoryId = ref<ApiId>('');
const loadError = ref('');
const loading = ref(false);
const detailLoading = ref(false);

const siteTitle = computed(() => site.value?.siteName || '帮助中心');
const siteDescription = computed(() => site.value?.seoDescription || '帮助中心描述未配置。');
const brandInitial = computed(() => (siteTitle.value.trim().charAt(0) || 'H').toUpperCase());
const heroAd = computed(() => heroAds.value[0] || null);
const heroImage = computed(() => adPrimaryImage(heroAd.value));
const primaryAd = computed(() => ads.value[0] || null);
const categoryOptions = computed(() => uniqueCategories(flattenCategories(categories.value)).slice(0, 12));

onMounted(() => {
  void loadSite();
});

async function loadSite() {
  loading.value = true;
  loadError.value = '';
  try {
    site.value = await cmsSiteApi.resolveSite(siteQuery.value);
    applySiteSeo(site.value);
    const [navRows, heroRows, categoryRows, adRows] = await Promise.all([
      cmsSiteApi.listNavigations({ ...siteQuery.value, navType: 'TOP' }),
      cmsSiteApi.listAdvertisements({ ...siteQuery.value, position: 'HELP_HERO' }),
      cmsSiteApi.treeCategories(siteQuery.value),
      cmsSiteApi.listAdvertisements({ ...siteQuery.value, position: 'HELP_TOP' }),
    ]);
    navigations.value = sortBySort(navRows);
    heroAds.value = sortBySort(heroRows);
    categories.value = categoryRows || [];
    ads.value = sortBySort(adRows);
    await loadContents();
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : '数据加载失败';
  } finally {
    loading.value = false;
  }
}

async function loadContents() {
  loading.value = true;
  try {
    const nextPage = await cmsSiteApi.pageContents({
      ...siteQuery.value,
      pageNum: 1,
      pageSize: 10,
      keyword: keyword.value || undefined,
      categoryId: categoryId.value || undefined,
    });
    contents.value = nextPage;
    if (nextPage.list?.[0]) {
      await loadDetail(nextPage.list[0]);
    } else {
      selectedContent.value = null;
    }
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : '内容加载失败';
  } finally {
    loading.value = false;
  }
}

async function loadDetail(content: CmsContent) {
  if (!content.id) {
    selectedContent.value = content;
    return;
  }
  detailLoading.value = true;
  try {
    selectedContent.value = await cmsSiteApi.detailContent({
      ...siteQuery.value,
      contentId: content.id,
      categoryId: categoryId.value || undefined,
    });
  } catch {
    selectedContent.value = content;
  } finally {
    detailLoading.value = false;
  }
}

function search() {
  void loadContents();
}

function selectCategory(id: ApiId) {
  categoryId.value = id;
  void loadContents();
}

function selectContent(content: CmsContent) {
  void loadDetail(content);
}

function reload() {
  void loadSite();
}

function navigationHref(item: CmsNavigation) {
  if (item.externalUrl) return item.externalUrl;
  if (item.contentId) return '#contents';
  if (item.categoryId) return `#contents`;
  return '#top';
}

function handleNavigationClick(item: CmsNavigation, event: MouseEvent) {
  if (item.categoryId) {
    event.preventDefault();
    selectCategory(item.categoryId);
    document.querySelector('#contents')?.scrollIntoView({ behavior: 'smooth' });
  }
}

function handleAdClick(ad: CmsAdvertisement | null, fallback: string, event: MouseEvent) {
  const href = adHref(ad, fallback);
  if (href.startsWith('#')) {
    event.preventDefault();
    document.querySelector(href)?.scrollIntoView({ behavior: 'smooth' });
  }
}

function adHref(ad: CmsAdvertisement | null, fallback: string) {
  return ad?.jumpUrl || fallback;
}

function adTarget(ad: CmsAdvertisement | null) {
  return ad?.openTarget === 'BLANK' ? '_blank' : '_self';
}

function adPrimaryImage(ad: CmsAdvertisement | null) {
  if (!ad) return '';
  if (ad.imageUrl) return ad.imageUrl;
  if (ad.coverUrl) return ad.coverUrl;
  if (ad.imageUrls) return ad.imageUrls.split(',').map(item => item.trim()).filter(Boolean)[0] || '';
  return '';
}

function flattenCategories(items: CmsSiteCategory[], level = 0): CmsSiteCategory[] {
  return items.flatMap(item => [
    { ...item, categoryName: level > 0 ? `${'　'.repeat(level)}${item.categoryName || ''}` : item.categoryName },
    ...flattenCategories(item.children || [], level + 1),
  ]);
}

function uniqueCategories(items: CmsSiteCategory[]) {
  const used = new Set<string>();
  return items.filter(item => {
    const key = (item.categoryName || item.categoryCode || item.accessPath || '').trim();
    if (!key || used.has(key)) {
      return false;
    }
    used.add(key);
    return true;
  });
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
</script>
