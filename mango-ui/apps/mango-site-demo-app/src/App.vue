<template>
  <div class="site" :class="{ 'is-home': view === 'home' }">
    <!-- 背景动效层 -->
    <div class="bg-fx" aria-hidden="true">
      <div class="bg-grid"></div>
      <div class="bg-glow bg-glow-1"></div>
      <div class="bg-glow bg-glow-2"></div>
      <div class="bg-glow bg-glow-3"></div>
    </div>

    <!-- 导航 -->
    <header class="nav" :class="{ 'is-scrolled': scrolled }">
      <div class="nav-shell">
        <a class="nav-logo" href="#/home" @click.prevent="go('home')">
          <span class="logo-mark">
            <svg viewBox="0 0 64 64" width="36" height="36" aria-hidden="true">
              <defs>
                <linearGradient id="logoGrad" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0" stop-color="#00D4FF" />
                  <stop offset="1" stop-color="#7B61FF" />
                </linearGradient>
              </defs>
              <path d="M32 6 L54 16 V32 C54 44 45 54 32 58 C19 54 10 44 10 32 V16 Z" fill="none" stroke="url(#logoGrad)" stroke-width="3" stroke-linejoin="round" />
              <path d="M22 32 L29 39 L43 25" fill="none" stroke="url(#logoGrad)" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </span>
          <span class="logo-text">{{ siteBrandName }}</span>
        </a>
        <nav class="nav-menu">
          <a
            v-for="item in topNavigations"
            :key="item.id || item.navName"
            :href="navigationHref(item)"
            :target="navigationTarget(item)"
            :class="{ active: isNavigationActive(item) }"
            @click="handleNavigationClick(item, $event)"
          >{{ item.navName }}</a>
        </nav>
        <a class="nav-cta" href="#/about" @click.prevent="go('about')">免费咨询</a>
      </div>
    </header>

    <main>
      <!-- 首页 -->
      <template v-if="view === 'home'">
        <!-- Hero -->
        <section class="hero">
          <div class="hero-shell">
            <div class="hero-copy">
              <p class="kicker"><span class="kicker-dot"></span>演示站点 · CONTENT PLATFORM</p>
              <h1 class="hero-title">
                <span class="grad-text">内容管理平台</span><br />
                让每一次发布<span class="grad-text-warm">可信可溯</span>
              </h1>
              <p class="hero-sub">{{ heroBanner?.subtitle || heroSubtitle }}</p>
              <div class="hero-actions">
                <button class="btn btn-primary" @click="go('products')">
                  探索产品能力
                  <span class="btn-arrow">→</span>
                </button>
                <button class="btn btn-ghost" @click="go('solutions')">查看解决方案</button>
              </div>
              <div class="hero-meta">
                <div class="hero-meta-item"><strong>8s</strong><span>平均发布时效</span></div>
                <div class="hero-meta-divider"></div>
                <div class="hero-meta-item"><strong>99.2%</strong><span>审核拦截率</span></div>
                <div class="hero-meta-divider"></div>
                <div class="hero-meta-item"><strong>200+</strong><span>服务站点</span></div>
              </div>
            </div>
            <div class="hero-visual" aria-hidden="true">
              <div class="orb orb-core"></div>
              <div class="orb orb-ring orb-ring-1"></div>
              <div class="orb orb-ring orb-ring-2"></div>
              <div class="orb orb-ring orb-ring-3"></div>
              <div class="hero-card hero-card-1">
                <div class="hc-icon" style="--c:#00D4FF">
                  <svg viewBox="0 0 24 24" width="22" height="22"><path fill="currentColor" d="M12 2 L4 5 V11 C4 16 7.5 20 12 21 C16.5 20 20 16 20 11 V5 Z" /></svg>
                </div>
                <div><div class="hc-title">内容发布</div><div class="hc-desc">秒级生效</div></div>
              </div>
              <div class="hero-card hero-card-2">
                <div class="hc-icon" style="--c:#7B61FF">
                  <svg viewBox="0 0 24 24" width="22" height="22"><path fill="currentColor" d="M12 2 L13.5 8.5 L20 10 L13.5 11.5 L12 18 L10.5 11.5 L4 10 L10.5 8.5 Z" /></svg>
                </div>
                <div><div class="hc-title">智能审核</div><div class="hc-desc">实时拦截</div></div>
              </div>
              <div class="hero-card hero-card-3">
                <div class="hc-icon" style="--c:#FF4D9D">
                  <svg viewBox="0 0 24 24" width="22" height="22"><path fill="currentColor" d="M3 12 L9 6 V9 H21 V15 H9 V18 Z" /></svg>
                </div>
                <div><div class="hc-title">数据穿透</div><div class="hc-desc">全链路可溯</div></div>
              </div>
            </div>
          </div>
          <div class="hero-scroll-hint"><span></span>向下滚动</div>
        </section>

        <!-- 核心能力 -->
        <section class="section" id="capabilities">
          <div class="section-shell">
            <div class="section-head">
              <p class="kicker"><span class="kicker-dot"></span>PRODUCTS</p>
              <h2 class="section-title">核心<span class="grad-text">产品能力</span></h2>
              <p class="section-desc">从内容发布到审核、数据、多站点，构建内容管理平台全栈能力</p>
            </div>
            <div class="cap-grid">
              <article v-for="(cap, i) in capabilities" :key="cap.code" class="cap-card reveal" :style="{ '--delay': i * 0.08 + 's' }">
                <div class="cap-icon">
                  <component :is="iconFor(cap.icon)" />
                </div>
                <h3 class="cap-name">{{ cap.name }}</h3>
                <p class="cap-tagline">{{ cap.tagline }}</p>
                <p class="cap-desc">{{ cap.description }}</p>
                <ul class="cap-features">
                  <li v-for="f in cap.features" :key="f"><span class="check">✓</span>{{ f }}</li>
                </ul>
              </article>
            </div>
          </div>
        </section>

        <!-- 解决方案 -->
        <section class="section section-alt" id="solutions-block">
          <div class="section-shell">
            <div class="section-head">
              <p class="kicker"><span class="kicker-dot"></span>SOLUTIONS</p>
              <h2 class="section-title">行业<span class="grad-text">解决方案</span></h2>
              <p class="section-desc">面向企业官网、帮助中心、行业门户、政务公开四大场景的差异化能力</p>
            </div>
            <div class="sol-grid">
              <article v-for="(sol, i) in solutions" :key="sol.code" class="sol-card reveal" :style="{ '--delay': i * 0.08 + 's' }">
                <div class="sol-industry">{{ sol.industry }}</div>
                <h3 class="sol-title">{{ sol.title }}</h3>
                <p class="sol-desc">{{ sol.description }}</p>
                <ul class="sol-highlights">
                  <li v-for="h in sol.highlights" :key="h">{{ h }}</li>
                </ul>
              </article>
            </div>
          </div>
        </section>

        <!-- 数据亮点 -->
        <section class="section" id="highlights">
          <div class="section-shell">
            <div class="hl-grid">
              <div v-for="(h, i) in highlights" :key="h.label" class="hl-item reveal" :style="{ '--delay': i * 0.1 + 's' }">
                <div class="hl-value">
                  <span class="hl-num" :data-target="h.value">0</span><span class="hl-suffix">{{ h.suffix }}</span>
                </div>
                <div class="hl-label">{{ h.label }}</div>
              </div>
            </div>
          </div>
        </section>

        <!-- 客户 -->
        <section class="section section-alt" id="clients">
          <div class="section-shell">
            <div class="section-head">
              <p class="kicker"><span class="kicker-dot"></span>CLIENTS</p>
              <h2 class="section-title">值得<span class="grad-text">信赖</span></h2>
              <p class="section-desc">服务 200+ 企业官网、帮助中心与行业门户</p>
            </div>
            <div class="client-grid">
              <div v-for="(c, i) in clients" :key="c" class="client-item reveal" :style="{ '--delay': i * 0.04 + 's' }">{{ c }}</div>
            </div>
          </div>
        </section>

        <!-- 新闻 -->
        <section class="section" id="news-block">
          <div class="section-shell">
            <div class="section-head">
              <p class="kicker"><span class="kicker-dot"></span>NEWS</p>
              <h2 class="section-title">新闻<span class="grad-text">动态</span></h2>
              <p class="section-desc">了解演示站点的最新进展</p>
            </div>
            <div class="news-grid">
              <article v-for="(item, i) in topNews" :key="item.id" class="news-card reveal" :style="{ '--delay': i * 0.08 + 's' }" @click="openNews(item.id)">
                <div class="news-cover" :style="{ background: newsCoverGradient(i) }">
                  <span class="news-cover-tag">{{ item.categoryName }}</span>
                </div>
                <div class="news-body">
                  <div class="news-date">{{ formatDate(item.publishTime) }}</div>
                  <h3 class="news-title">{{ item.title }}</h3>
                  <p class="news-summary">{{ item.summary }}</p>
                  <span class="news-more">阅读全文 →</span>
                </div>
              </article>
            </div>
            <div class="section-actions">
              <button class="btn btn-ghost" @click="go('news')">查看全部动态 →</button>
            </div>
          </div>
        </section>

        <!-- CTA -->
        <section class="section cta-section">
          <div class="section-shell">
            <div class="cta-box">
              <div class="cta-glow"></div>
              <h2 class="cta-title">开启你的<span class="grad-text-warm">内容管理</span>之旅</h2>
              <p class="cta-desc">预约一次产品演示，了解内容管理平台如何助力你的内容运营提效降险</p>
              <div class="cta-actions">
                <button class="btn btn-primary" @click="go('about')">预约演示</button>
                <button class="btn btn-ghost" @click="go('products')">了解产品</button>
              </div>
            </div>
          </div>
        </section>
      </template>

      <!-- 产品能力 -->
      <section v-else-if="view === 'products'" class="page">
        <div class="page-hero">
          <p class="kicker"><span class="kicker-dot"></span>PRODUCTS</p>
          <h1 class="page-title">核心<span class="grad-text">产品能力</span></h1>
          <p class="page-desc">构建内容管理平台全栈能力，覆盖发布、审核、数据与多站点协同</p>
        </div>
        <div class="section-shell">
          <div class="cap-grid cap-grid-page">
            <article v-for="cap in capabilities" :key="cap.code" class="cap-card">
              <div class="cap-icon"><component :is="iconFor(cap.icon)" /></div>
              <h3 class="cap-name">{{ cap.name }}</h3>
              <p class="cap-tagline">{{ cap.tagline }}</p>
              <p class="cap-desc">{{ cap.description }}</p>
              <ul class="cap-features">
                <li v-for="f in cap.features" :key="f"><span class="check">✓</span>{{ f }}</li>
              </ul>
            </article>
          </div>
        </div>
      </section>

      <!-- 解决方案 -->
      <section v-else-if="view === 'solutions'" class="page">
        <div class="page-hero">
          <p class="kicker"><span class="kicker-dot"></span>SOLUTIONS</p>
          <h1 class="page-title">行业<span class="grad-text">解决方案</span></h1>
          <p class="page-desc">面向四大场景的差异化内容管理方案</p>
        </div>
        <div class="section-shell">
          <div class="sol-list">
            <article v-for="sol in solutions" :key="sol.code" class="sol-row">
              <div class="sol-row-head">
                <span class="sol-industry-tag">{{ sol.industry }}</span>
                <h3 class="sol-title">{{ sol.title }}</h3>
              </div>
              <p class="sol-desc">{{ sol.description }}</p>
              <ul class="sol-highlights sol-highlights-row">
                <li v-for="h in sol.highlights" :key="h">{{ h }}</li>
              </ul>
            </article>
          </div>
        </div>
      </section>

      <!-- 新闻列表 -->
      <section v-else-if="view === 'news'" class="page">
        <div class="news-hero" :style="newsHeroStyle()">
          <p class="kicker"><span class="kicker-dot"></span>NEWS</p>
          <h1 class="page-title">新闻<span class="grad-text">动态</span></h1>
          <p class="page-desc">{{ newsHeroBanner?.subtitle || '了解演示站点的最新进展与行业洞察' }}</p>
        </div>
        <div class="section-shell">
          <div v-if="newsLoading && !newsList.length" class="news-empty">内容加载中...</div>
          <div v-else-if="newsList.length" class="news-list">
            <article v-for="(item, i) in newsList" :key="item.id" class="news-row reveal" :style="{ '--delay': i * 0.05 + 's' }" @click="openNews(item.id)">
              <div class="news-row-cover" :style="newsCoverStyle(item, i)"></div>
              <div class="news-row-body">
                <div class="news-date">{{ formatDate(item.publishTime) }} · {{ item.source }}</div>
                <h3 class="news-title">{{ item.title }}</h3>
                <p class="news-summary">{{ item.summary }}</p>
              </div>
              <span class="news-more">阅读 →</span>
            </article>
          </div>
          <div v-else class="news-empty">暂无已发布内容</div>
          <nav v-if="newsTotalPages > 1" class="news-pagination" aria-label="新闻分页">
            <button type="button" class="news-page-btn" :disabled="newsPage.pageNum <= 1 || newsLoading" @click="changeNewsPage(newsPage.pageNum - 1)">上一页</button>
            <button
              v-for="pageNo in newsPageNumbers"
              :key="pageNo"
              type="button"
              class="news-page-btn"
              :class="{ active: pageNo === newsPage.pageNum }"
              :aria-current="pageNo === newsPage.pageNum ? 'page' : undefined"
              :disabled="newsLoading"
              @click="changeNewsPage(pageNo)"
            >{{ pageNo }}</button>
            <button type="button" class="news-page-btn" :disabled="newsPage.pageNum >= newsTotalPages || newsLoading" @click="changeNewsPage(newsPage.pageNum + 1)">下一页</button>
            <span class="news-page-total">共 {{ newsPage.total }} 条</span>
          </nav>
        </div>
      </section>

      <!-- 新闻详情 -->
      <section v-else-if="view === 'news-detail'" class="detail-page">
        <div class="section-shell detail-layout">
          <article v-if="currentNews" class="detail-article">
            <a class="back-link" href="#/news" @click.prevent="go('news')">返回列表</a>
            <div class="detail-cover" :style="newsCoverStyle(currentNews, currentNewsIndex)">
              <span class="news-cover-tag">{{ currentNews.categoryName || '新闻动态' }}</span>
            </div>
            <div class="detail-content-card">
              <div class="detail-meta">{{ formatDate(currentNews.publishTime) }} · {{ currentNews.source }} · {{ currentNews.author }}</div>
              <h1 class="detail-title">{{ currentNews.title }}</h1>
              <div v-if="currentNews.summary" class="detail-summary">{{ currentNews.summary }}</div>
              <div class="detail-body" v-html="currentNews.body"></div>
            </div>
          </article>
          <div v-else class="news-empty">{{ newsLoading ? '详情加载中...' : '内容不存在或已下线' }}</div>
        </div>
      </section>

      <!-- 关于 -->
      <section v-else-if="view === 'about'" class="page">
        <div class="news-hero" :style="aboutHeroStyle()">
          <p class="kicker"><span class="kicker-dot"></span>ABOUT</p>
          <h1 class="page-title">关于<span class="grad-text">{{ siteBrandName }}</span></h1>
          <p class="page-desc">{{ aboutHeroAd?.title || site?.seoDescription || '让每一次发布可信可溯' }}</p>
        </div>
        <div class="section-shell">
          <div class="about-grid">
            <div class="about-block">
              <h3 class="about-h">公司简介</h3>
              <p class="about-p">{{ site?.seoDescription || '暂无公司简介，请在 CMS 站点配置中维护 SEO 描述。' }}</p>
            </div>
            <div class="about-block">
              <h3 class="about-h">使命与愿景</h3>
              <p class="about-p">{{ aboutHeroAd?.textContent || site?.seoKeywords || '让每一次发布可信可溯' }}</p>
            </div>
            <div class="about-block">
              <h3 class="about-h">联系我们</h3>
              <p class="about-p">{{ site?.contactInfo || '联系方式未配置，请在 CMS 站点配置中维护联系信息。' }}</p>
            </div>
          </div>
        </div>
      </section>
    </main>

    <!-- 页脚 -->
    <footer class="footer">
      <div class="section-shell footer-grid">
        <div class="footer-brand">
          <div class="nav-logo">
            <span class="logo-mark">
              <svg viewBox="0 0 64 64" width="32" height="32" aria-hidden="true">
                <path d="M32 6 L54 16 V32 C54 44 45 54 32 58 C19 54 10 44 10 32 V16 Z" fill="none" stroke="url(#logoGrad)" stroke-width="3" stroke-linejoin="round" />
                <path d="M22 32 L29 39 L43 25" fill="none" stroke="url(#logoGrad)" stroke-width="3.5" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </span>
            <span class="logo-text">{{ siteBrandName }}</span>
          </div>
          <p class="footer-desc">{{ site?.seoDescription || '站点描述未配置，请在 CMS 站点配置中维护。' }}</p>
        </div>
        <div v-for="group in footerNavigationGroups" :key="group.title" class="footer-col">
          <h4>{{ group.title }}</h4>
          <a
            v-for="item in group.items"
            :key="item.id || item.navName"
            :href="navigationHref(item)"
            :target="navigationTarget(item)"
            @click="handleNavigationClick(item, $event)"
          >{{ item.navName }}</a>
        </div>
      </div>
      <div class="section-shell footer-bottom">
        <span>{{ site?.footerCopyright || '版权信息未配置' }}</span>
        <span>{{ site?.icpRecord || '' }}</span>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, onUnmounted, ref, nextTick, type Component } from 'vue';
import { cmsSiteApi, createSiteResolveQuery, applySiteSeo, type SiteResolve, type SiteNavigation, type SiteBanner, type SiteContent, type SiteAdvertisement } from '@mango/site-shell';
import { CAPABILITIES, SOLUTIONS, HIGHLIGHTS, CLIENTS } from './mock';

type View = 'home' | 'products' | 'solutions' | 'news' | 'news-detail' | 'about';
type FooterNavigationGroup = { title: string; items: SiteNavigation[] };

const site = ref<SiteResolve | null>(null);
const topNavigations = ref<SiteNavigation[]>([]);
const footerNavigations = ref<SiteNavigation[]>([]);
const banners = ref<SiteBanner[]>([]);
const newsHeroBanner = ref<SiteBanner | null>(null);
const aboutHeroAd = ref<SiteAdvertisement | null>(null);
const homeNews = ref<SiteContent[]>([]);
const newsList = ref<SiteContent[]>([]);
const newsPage = ref({ pageNum: 1, pageSize: 6, total: 0 });
const newsLoading = ref(false);
const currentNews = ref<SiteContent | null>(null);
const view = ref<View>('home');
const currentNewsId = ref<string>('');
const scrolled = ref(false);

const capabilities = CAPABILITIES;
const solutions = SOLUTIONS;
const highlights = HIGHLIGHTS;
const clients = CLIENTS;

const siteBrandName = computed(() => site.value?.siteName || 'CMS 站点');
const footerNavigationGroups = computed(() => {
  const groups = new Map<string, SiteNavigation[]>();
  footerNavigations.value.forEach((item) => {
    const title = footerGroupTitle(item);
    groups.set(title, [...(groups.get(title) || []), item]);
  });
  return Array.from(groups, ([title, items]) => ({ title, items })) as FooterNavigationGroup[];
});
const heroBanner = computed(() => banners.value.find(b => b.position === 'HOME_HERO') || banners.value[0] || null);
const heroSubtitle = '全线上秒级发布 · 智能审核 · 数据穿透 · 服务企业官网、帮助中心与行业门户';
const topNews = computed(() => homeNews.value);
const newsTotalPages = computed(() => Math.max(1, Math.ceil(newsPage.value.total / newsPage.value.pageSize)));
const newsPageNumbers = computed(() => {
  const total = newsTotalPages.value;
  const current = newsPage.value.pageNum;
  const start = Math.max(1, Math.min(current - 2, total - 4));
  const end = Math.min(total, start + 4);
  return Array.from({ length: end - start + 1 }, (_, index) => start + index);
});
const currentNewsIndex = computed(() => Math.max(0, newsList.value.findIndex(n => n.id === currentNewsId.value)));

function parseHash(): { view: View; id?: string } {
  const raw = window.location.hash.replace(/^#\/?/, '');
  const [path, id] = raw.split('/');
  if (path === 'products') return { view: 'products' };
  if (path === 'solutions') return { view: 'solutions' };
  if (path === 'news') return { view: 'news' };
  if (path === 'about') return { view: 'about' };
  if (path === 'news-detail' && id) return { view: 'news-detail', id };
  return { view: 'home' };
}

function applyHash() {
  const parsed = parseHash();
  view.value = parsed.view;
  if (parsed.id) currentNewsId.value = parsed.id;
  window.scrollTo({ top: 0, behavior: 'instant' as ScrollBehavior });
  nextTick(() => {
    initReveal();
    if (parsed.view === 'home') initCounters();
  });
  if (parsed.view === 'news') {
    void loadNewsList(newsPage.value.pageNum);
  } else if (parsed.view === 'news-detail' && parsed.id) {
    void loadNewsDetail(parsed.id);
  }
}

function go(target: View) {
  window.location.hash = '#/' + target;
}

function openNews(id?: string) {
  if (!id) return;
  window.location.hash = '#/news-detail/' + id;
}

async function loadNewsList(pageNum: number) {
  newsLoading.value = true;
  try {
    const query = siteResolveQuery();
    const next = Math.max(1, Math.min(pageNum, newsTotalPages.value || 1));
    const page = await cmsSiteApi.pageContents({ ...query, page: next, size: newsPage.value.pageSize });
    newsList.value = page?.list || [];
    newsPage.value = {
      pageNum: Number(page?.pageNum || page?.page || next),
      pageSize: newsPage.value.pageSize,
      total: Number(page?.total || 0),
    };
  } catch {
    newsList.value = [];
    newsPage.value = { pageNum: 1, pageSize: newsPage.value.pageSize, total: 0 };
  } finally {
    newsLoading.value = false;
    nextTick(() => { initReveal(); updateReveal(); });
  }
}

function changeNewsPage(pageNum: number) {
  const next = Math.max(1, Math.min(pageNum, newsTotalPages.value));
  if (next === newsPage.value.pageNum || newsLoading.value) return;
  void loadNewsList(next).then(() => window.scrollTo({ top: 0, behavior: 'smooth' }));
}

async function loadNewsDetail(id: string) {
  try {
    const query = siteResolveQuery();
    const detail = await cmsSiteApi.detailContent({ ...query, contentId: id });
    currentNews.value = detail || null;
  } catch {
    currentNews.value = null;
  }
}

function navigationHref(item: SiteNavigation) {
  if (item.externalUrl) return normalizeNavigationUrl(item.externalUrl);
  const viewCode = viewForNavigation(item);
  return viewCode ? `#/${viewCode}` : '#/home';
}

function normalizeNavigationUrl(url: string) {
  if (url.startsWith('http')) return url;
  if (url.startsWith('#/')) return url;
  if (url.startsWith('#')) return `#/${url.slice(1).replace(/^\/+/, '')}`;
  if (url.startsWith('/')) return `#${url}`;
  return `#/${url}`;
}

function navigationTarget(item: SiteNavigation) {
  return item.openTarget === 'BLANK' ? '_blank' : '_self';
}

function isNavigationActive(item: SiteNavigation) {
  return viewForNavigation(item) === view.value;
}

function handleNavigationClick(item: SiteNavigation, event: MouseEvent) {
  const href = navigationHref(item);
  if (!href.startsWith('http')) {
    event.preventDefault();
    window.location.hash = href;
  }
}

function footerGroupTitle(item: SiteNavigation) {
  const name = item.navName || '';
  if (/产品|签发|风控|数据|银担/.test(name)) return '产品';
  if (/方案|官网|门户|政务|帮助/.test(name)) return '解决方案';
  if (/关于|公司|新闻|联系/.test(name)) return '关于';
  return '导航';
}

function viewForNavigation(item: SiteNavigation): View | '' {
  const name = item.navName || '';
  const externalUrl = item.externalUrl || '';
  if (/产品|能力/.test(name) || externalUrl.includes('products')) return 'products';
  if (/方案|解决/.test(name) || externalUrl.includes('solutions')) return 'solutions';
  if (/新闻|资讯|动态/.test(name) || externalUrl.includes('news')) return 'news';
  if (/关于|我们|联系/.test(name) || externalUrl.includes('about')) return 'about';
  if (/首页|主页|home/i.test(name) || externalUrl === '/' || externalUrl.includes('home')) return 'home';
  return '';
}

function onHashChange() { applyHash(); }
function onScroll() {
  scrolled.value = window.scrollY > 40;
  updateReveal();
}

function sortBySort<T extends { sort?: number }>(items: T[] = []) {
  return [...items].sort((left, right) => Number(left.sort || 0) - Number(right.sort || 0));
}

function formatDate(s?: string) {
  if (!s) return '';
  return s.slice(0, 10);
}

function newsCoverGradient(i: number) {
  const palettes = [
    'linear-gradient(135deg, #00D4FF 0%, #7B61FF 100%)',
    'linear-gradient(135deg, #7B61FF 0%, #FF4D9D 100%)',
    'linear-gradient(135deg, #00D4FF 0%, #2E9BFF 100%)',
  ];
  return palettes[i % palettes.length];
}

function newsCoverStyle(item: SiteContent, index: number) {
  const fallback = newsCoverGradient(index);
  if (!item.coverUrl) return { background: fallback };
  return {
    backgroundImage: `linear-gradient(180deg, rgba(6,8,26,0.08), rgba(6,8,26,0.72)), url(${item.coverUrl})`,
    backgroundSize: 'cover',
    backgroundPosition: 'center',
  };
}

function bannerMediaUrl(banner: SiteBanner | null): string {
  if (!banner?.mediaFileId) return '';
  return `/api/cms/open/files/public-preview?id=${encodeURIComponent(banner.mediaFileId)}&domain=${encodeURIComponent(window.location.host)}`;
}

function newsHeroStyle(): Record<string, string> {
  const url = bannerMediaUrl(newsHeroBanner.value);
  if (url) {
    return {
      backgroundImage: `linear-gradient(180deg, rgba(6,8,26,0.45), rgba(6,8,26,0.82)), url(${url})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
    };
  }
  return { background: 'linear-gradient(135deg, #00D4FF 0%, #7B61FF 100%)' };
}

function aboutHeroStyle(): Record<string, string> {
  const url = aboutHeroAd.value?.imageUrl || '';
  if (url) {
    return {
      backgroundImage: `linear-gradient(180deg, rgba(6,8,26,0.5), rgba(6,8,26,0.85)), url(${url})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
    };
  }
  return { background: 'linear-gradient(135deg, #7B61FF 0%, #FF4D9D 100%)' };
}

// ===== 图标（内联 SVG，避免外部依赖） =====
const IconShield = () => h('svg', { viewBox: '0 0 24 24', width: '28', height: '28' }, [h('path', { fill: 'currentColor', d: 'M12 2 L4 5 V11 C4 16 7.5 20 12 21 C16.5 20 20 16 20 11 V5 Z' })]);
const IconRadar = () => h('svg', { viewBox: '0 0 24 24', width: '28', height: '28' }, [
  h('circle', { cx: '12', cy: '12', r: '9', fill: 'none', stroke: 'currentColor', 'stroke-width': '1.6' }),
  h('circle', { cx: '12', cy: '12', r: '5', fill: 'none', stroke: 'currentColor', 'stroke-width': '1.6' }),
  h('circle', { cx: '12', cy: '12', r: '1.6', fill: 'currentColor' }),
  h('path', { d: 'M12 12 L20 6', stroke: 'currentColor', 'stroke-width': '1.6', 'stroke-linecap': 'round' }),
]);
const IconLink = () => h('svg', { viewBox: '0 0 24 24', width: '28', height: '28' }, [
  h('path', { d: 'M9 15 L15 9', stroke: 'currentColor', 'stroke-width': '1.8', 'stroke-linecap': 'round' }),
  h('path', { d: 'M7 14 a4 4 0 0 1 0-6 l2-2 a4 4 0 0 1 6 0', fill: 'none', stroke: 'currentColor', 'stroke-width': '1.8', 'stroke-linecap': 'round' }),
  h('path', { d: 'M17 10 a4 4 0 0 1 0 6 l-2 2 a4 4 0 0 1-6 0', fill: 'none', stroke: 'currentColor', 'stroke-width': '1.8', 'stroke-linecap': 'round' }),
]);
const IconNetwork = () => h('svg', { viewBox: '0 0 24 24', width: '28', height: '28' }, [
  h('circle', { cx: '12', cy: '5', r: '2.4', fill: 'currentColor' }),
  h('circle', { cx: '5', cy: '18', r: '2.4', fill: 'currentColor' }),
  h('circle', { cx: '19', cy: '18', r: '2.4', fill: 'currentColor' }),
  h('path', { d: 'M12 7 V12 M12 12 L6 16 M12 12 L18 16', stroke: 'currentColor', 'stroke-width': '1.6', 'stroke-linecap': 'round' }),
]);

function iconFor(name: string): Component {
  switch (name) {
    case 'shield': return IconShield;
    case 'radar': return IconRadar;
    case 'link': return IconLink;
    case 'network': return IconNetwork;
    default: return IconShield;
  }
}

// ===== 滚动渐入 =====
let revealEls: HTMLElement[] = [];
function initReveal() {
  revealEls = Array.from(document.querySelectorAll('.reveal'));
  updateReveal();
}
function updateReveal() {
  const vh = window.innerHeight;
  revealEls.forEach(el => {
    const rect = (el as HTMLElement).getBoundingClientRect();
    if (rect.top < vh - 80) el.classList.add('is-visible');
  });
}

// ===== 数字滚动 =====
function initCounters() {
  const nums = document.querySelectorAll<HTMLElement>('.hl-num');
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        animateCount(entry.target as HTMLElement);
        observer.unobserve(entry.target);
      }
    });
  }, { threshold: 0.4 });
  nums.forEach(n => observer.observe(n));
}
function animateCount(el: HTMLElement) {
  const target = Number(el.dataset.target || '0');
  const duration = 1600;
  const start = performance.now();
  function step(now: number) {
    const p = Math.min((now - start) / duration, 1);
    const eased = 1 - Math.pow(1 - p, 3);
    el.textContent = String(Math.round(target * eased));
    if (p < 1) requestAnimationFrame(step);
  }
  requestAnimationFrame(step);
}

function siteResolveQuery() {
  return createSiteResolveQuery(undefined, window.location.host);
}
// ===== 数据加载（优先真实 CMS，失败回退仿真） =====
async function loadData() {
  const query = siteResolveQuery();
  try {
    const resolved = await cmsSiteApi.resolveSite(query);
    if (resolved?.siteName) {
      site.value = resolved;
      applySiteSeo(resolved);
    }
  } catch {
    site.value = null;
  }
  try {
    const [topNavs, footerNavs, bnrs, newsBnrs] = await Promise.all([
      cmsSiteApi.listNavigations({ ...query, navType: 'TOP' }),
      cmsSiteApi.listNavigations({ ...query, navType: 'FOOTER' }),
      cmsSiteApi.listBanners(query),
      cmsSiteApi.listBanners({ ...query, position: 'NEWS_HERO' }),
    ]);
    topNavigations.value = sortBySort(topNavs);
    footerNavigations.value = sortBySort(footerNavs);
    banners.value = sortBySort(bnrs);
    newsHeroBanner.value = sortBySort(newsBnrs)[0] || null;
    const aboutAds = await cmsSiteApi.listAdvertisements({ ...query, position: 'ABOUT_HERO' });
    aboutHeroAd.value = sortBySort(aboutAds)[0] || null;
  } catch {
    topNavigations.value = [];
    footerNavigations.value = [];
    banners.value = [];
    newsHeroBanner.value = null;
    aboutHeroAd.value = null;
  }
  try {
    const page = await cmsSiteApi.pageContents({ ...query, page: 1, size: 3 });
    homeNews.value = page?.list || [];
  } catch {
    homeNews.value = [];
  }
}

onMounted(async () => {
  applyHash();
  window.addEventListener('hashchange', onHashChange);
  window.addEventListener('scroll', onScroll, { passive: true });
  await loadData();
  nextTick(() => { initReveal(); if (view.value === 'home') initCounters(); });
});

onUnmounted(() => {
  window.removeEventListener('hashchange', onHashChange);
  window.removeEventListener('scroll', onScroll);
});
</script>
