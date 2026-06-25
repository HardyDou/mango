import { expect, type Page, test } from '@playwright/test';
import fs from 'node:fs/promises';
import path from 'node:path';

interface CmsMockContent {
  id: string;
  title: string;
  subtitle?: string;
  summary?: string;
  contentType: 'ARTICLE' | 'IMAGE_TEXT' | 'PAGE' | 'ATTACHMENT' | 'VIDEO';
  coverFileId?: string;
  body?: string;
  externalUrl?: string;
  attachmentFileId?: string;
  videoFileId?: string;
  source?: string;
  author?: string;
  categoryId: string;
  categoryName: string;
  publishTime: string;
}

const contents: CmsMockContent[] = [
  {
    id: 'content-solution-1',
    title: '集团门户统一建设方案',
    subtitle: '集团多站点统一内容运营',
    summary: '统一管理集团官网、子品牌站、专题站和帮助中心。',
    contentType: 'PAGE',
    source: '芒果科技解决方案中心',
    author: '方案顾问',
    categoryId: 'cat-solutions',
    categoryName: '解决方案',
    publishTime: '2026-06-23T09:00:00',
    body: '<h2>集团门户统一建设方案</h2><p>通过 CMS 站点、栏目、导航、内容和广告投放模型，支撑集团多站点统一运营。</p>',
  },
  {
    id: 'content-about-1',
    title: '关于芒果科技',
    subtitle: '专注企业数字化平台与可运营官网建设',
    summary: '芒果科技提供 CMS、微前端后台、权限租户和流程协同能力。',
    contentType: 'PAGE',
    source: '芒果科技',
    author: '品牌中心',
    categoryId: 'cat-about',
    categoryName: '关于我们',
    publishTime: '2026-06-23T08:30:00',
    body: '<h2>关于芒果科技</h2><p>芒果科技将官网建设、后台管理、权限租户和流程协同沉淀为可验证的企业数字化能力。</p>',
  },
  {
    id: 'content-article-1',
    title: '芒果科技发布统一门户建设方案',
    subtitle: '多站点统一内容运营',
    summary: '方案覆盖导航、Banner、广告、栏目、内容发布和富文本详情展示。',
    contentType: 'ARTICLE',
    coverFileId: '9201',
    source: '芒果科技',
    author: '内容运营部',
    categoryId: 'cat-news',
    categoryName: '新闻动态',
    publishTime: '2026-06-22T10:00:00',
    body: [
      '<h2>统一门户建设方案</h2>',
      '<p>这是来自 CMS 内容详情接口的 <strong>富文本正文</strong>。</p>',
      '<ul><li>导航由 CMS 导航管理维护</li><li>Banner 与广告由公开接口加载</li><li>栏目和内容支持分页与详情联动</li></ul>',
      '<blockquote>所有公开站点页面均通过域名解析站点，并按栏目读取内容。</blockquote>',
      '<table><thead><tr><th>能力</th><th>来源</th></tr></thead><tbody><tr><td>新闻列表</td><td>内容分页接口</td></tr><tr><td>新闻详情</td><td>内容详情接口</td></tr></tbody></table>',
      '<p><a href="/news">返回新闻列表</a></p>',
      '<script>window.__cms_xss = true</script><p onclick="window.__cms_click = true">清洗后的安全段落</p>',
    ].join(''),
  },
  {
    id: 'content-image-1',
    title: '数字化运营中心图文巡礼',
    summary: '通过图文内容展示统一运营中心、数据大屏和站点内容编排效果。',
    contentType: 'IMAGE_TEXT',
    coverFileId: '9202',
    source: '芒果科技',
    author: '品牌中心',
    categoryId: 'cat-news',
    categoryName: '新闻动态',
    publishTime: '2026-06-21T09:30:00',
    body: '<h2>图文展示</h2><p>图文正文用于展示产品界面、运营流程和项目现场。</p><p><img src="https://example.com/cms/news-center.png" alt="数字化运营中心" /></p>',
  },
  {
    id: 'content-video-1',
    title: '三分钟了解芒果科技门户平台',
    summary: '视频内容用于承载产品介绍、客户培训和发布会回放。',
    contentType: 'VIDEO',
    coverFileId: '9203',
    videoFileId: '9303',
    source: '芒果学院',
    author: '产品中心',
    categoryId: 'cat-news',
    categoryName: '新闻动态',
    publishTime: '2026-06-20T14:00:00',
    body: '<h2>视频内容说明</h2><p>视频正文可通过富文本说明播放内容、适用对象和后续行动。</p><video controls src="https://example.com/cms/portal-intro.mp4"></video>',
  },
  {
    id: 'content-company-page-1',
    title: '芒果科技完成华南研发中心升级',
    summary: '研发中心升级后将继续投入 CMS、流程协同和低代码平台能力建设。',
    contentType: 'PAGE',
    coverFileId: '9401',
    source: '芒果科技',
    author: '企业发展部',
    categoryId: 'cat-company',
    categoryName: '公司动态',
    publishTime: '2026-06-19T11:15:00',
    body: '<h2>华南研发中心</h2><p>研发中心升级聚焦工程效率、平台组件化和企业级交付质量。</p><ol><li>研发体系升级</li><li>客户共创中心开放</li></ol>',
  },
  {
    id: 'content-company-attachment-1',
    title: '芒果科技年度社会责任报告发布',
    summary: '报告以附件内容形式发布，正文提供下载说明和核心摘要。',
    contentType: 'ATTACHMENT',
    attachmentFileId: '9501',
    source: '芒果科技',
    author: '品牌中心',
    categoryId: 'cat-company',
    categoryName: '公司动态',
    publishTime: '2026-06-18T16:40:00',
    body: '<h2>年度社会责任报告</h2><p>附件内容用于承载白皮书、报告和资料下载说明。</p><p><a href="https://example.com/cms/esg-report.pdf">下载报告附件</a></p>',
  },
];

const testPng = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAFgwJ/lwHh3QAAAABJRU5ErkJggg==',
  'base64',
);

const runtimeImageUrl = (fileId: string) => `/runtime-cms-assets/ads/${fileId}.png`;

test.beforeEach(async ({ page }) => {
  await page.route('**/runtime-cms-assets/ads/*.png', async route => {
    await route.fulfill({
      status: 200,
      contentType: 'image/png',
      body: testPng,
    });
  });

  await page.route('**/api/file/files/download**', async route => {
    await route.fulfill({ status: 401, json: { code: 401, success: false, msg: '官网不得直接访问受保护下载接口' } });
  });

  await page.route('**/api/cms-api/**', async route => {
    const url = new URL(route.request().url());
    const path = url.pathname;
    expect(url.searchParams.get('domain')).toBe('127.0.0.1:5191');
    expect(url.searchParams.has('siteCode')).toBeFalsy();

    if (path.endsWith('/sites/resolve')) {
      await route.fulfill({
        json: {
          code: 200,
          success: true,
          data: {
            siteId: '1',
            siteCode: 'mango-tech',
            siteName: '芒果科技',
            seoTitle: '芒果科技企业官网',
            seoKeywords: 'CMS,企业官网,数字化平台',
            seoDescription: '芒果科技面向政企客户提供内容运营、统一门户、流程协同和低代码交付能力。',
            footerCopyright: 'Copyright 2026 芒果科技 版权所有',
            icpRecord: '粤ICP备202600001号',
            contactInfo: '广州市天河区软件园 18 号 / 400-800-2140',
          },
        },
      });
      return;
    }

    if (path.endsWith('/navigations/list')) {
      const navType = url.searchParams.get('navType');
      await route.fulfill({
        json: {
          code: 200,
          success: true,
          data: navType === 'FOOTER'
            ? [
                { id: 'footer-github', navType: 'FOOTER', navName: 'GitHub', externalUrl: 'https://github.com/HardyDou/mango', openTarget: 'BLANK', sort: 1 },
                { id: 'footer-issue', navType: 'FOOTER', navName: 'Issue 214', externalUrl: 'https://github.com/HardyDou/mango/issues/214', openTarget: 'BLANK', sort: 2 },
                { id: 'footer-ui', navType: 'FOOTER', navName: 'Mango UI', externalUrl: 'https://github.com/HardyDou/mango/tree/main/mango-ui', openTarget: 'BLANK', sort: 3 },
              ]
            : [
                { id: 'nav-home', navType: 'TOP', navName: '首页', jumpType: 'URL', externalUrl: '/', sort: 1 },
                { id: 'nav-products', navType: 'TOP', navName: '产品能力', jumpType: 'URL', externalUrl: '#products', sort: 2 },
                { id: 'nav-solutions', navType: 'TOP', navName: '解决方案', jumpType: 'CONTENT', contentId: 'content-solution-1', sort: 3 },
                { id: 'nav-news', navType: 'TOP', navName: '新闻动态', jumpType: 'CATEGORY', categoryId: 'cat-news', sort: 4 },
                { id: 'nav-company', navType: 'TOP', navName: '公司动态', jumpType: 'CATEGORY', categoryId: 'cat-company', sort: 5 },
                { id: 'nav-about', navType: 'TOP', navName: '关于我们', jumpType: 'CONTENT', contentId: 'content-about-1', sort: 6 },
              ],
        },
      });
      return;
    }

    if (path.endsWith('/banners/list')) {
      await route.fulfill({
        status: 410,
        json: {
          code: 410,
          success: false,
          msg: 'Banner 已并入广告投放接口，官网不得再读取旧 Banner 接口。',
        },
      });
      return;
    }

    if (path.endsWith('/advertisements/list')) {
      const position = url.searchParams.get('position') || 'HOME_FLOAT';
      const ads: Record<string, Array<Record<string, unknown>>> = {
        HOME_HERO: [
          {
            id: 'ad-home-hero',
            adCode: 'mango-tech-home-hero',
            adName: '官网首页主视觉 Banner',
            position: 'HOME_HERO',
            materialType: 'SINGLE_IMAGE',
            imageFileId: '9601',
            imageUrl: runtimeImageUrl('9601'),
            title: '政企数字门户与内容运营平台',
            textContent: '以统一 CMS、站点栏目、导航、广告和富文本发布能力，支撑官网、专题站和帮助中心灵活交付。',
            jumpUrl: 'https://github.com/HardyDou/mango',
            openTarget: 'BLANK',
            sort: 1,
          },
        ],
        HOME_FLOAT: [
          {
            id: 'ad-home',
            adCode: 'mango-tech-home-float',
            adName: '官网首页咨询推荐位',
            position: 'HOME_FLOAT',
            materialType: 'TEXT',
            title: '预约芒果科技产品演示',
            textContent: '获取 CMS 官网、微前端后台和权限租户能力的完整演示。',
            jumpUrl: '#products',
            sort: 1,
          },
        ],
        SOLUTIONS_BANNER: [
          { id: 'ad-solutions-banner', adCode: 'mango-tech-solutions-banner', adName: '解决方案页 Banner', position: 'SOLUTIONS_BANNER', materialType: 'SINGLE_IMAGE', imageFileId: '9602', imageUrl: runtimeImageUrl('9602'), title: '集团门户统一建设方案', textContent: '统一管理集团官网、子品牌站、专题站和帮助中心。', jumpUrl: 'https://github.com/HardyDou/mango/tree/main/mango-docs', openTarget: 'BLANK', sort: 1 },
        ],
        SOLUTIONS_TEXT: [
          { id: 'ad-solutions', adCode: 'mango-tech-solutions-text', adName: '解决方案页文本推荐', position: 'SOLUTIONS_TEXT', materialType: 'TEXT', title: '预约方案顾问', textContent: '围绕集团官网、专题站、帮助中心和内容运营建立可落地方案。', jumpUrl: '/solutions', sort: 1 },
        ],
        NEWS_BANNER: [
          { id: 'ad-news-banner', adCode: 'mango-tech-news-banner', adName: '新闻动态页 Banner', position: 'NEWS_BANNER', materialType: 'SINGLE_IMAGE', imageFileId: '9603', imageUrl: runtimeImageUrl('9603'), title: '新闻动态', textContent: '持续发布芒果科技产品进展、项目实践和平台能力更新。', jumpUrl: 'https://github.com/HardyDou/mango/issues/214', openTarget: 'BLANK', sort: 1 },
        ],
        NEWS_TEXT: [
          { id: 'ad-news', adCode: 'mango-tech-news-text', adName: '新闻动态页文本推荐', position: 'NEWS_TEXT', materialType: 'TEXT', title: '订阅产品动态', textContent: '第一时间了解 Mango CMS 与微前端后台的功能升级。', jumpUrl: '/news', sort: 1 },
        ],
        COMPANY_NEWS_BANNER: [
          { id: 'ad-company-banner', adCode: 'mango-tech-company-news-banner', adName: '公司动态页 Banner', position: 'COMPANY_NEWS_BANNER', materialType: 'SINGLE_IMAGE', imageFileId: '9604', imageUrl: runtimeImageUrl('9604'), title: '公司动态', textContent: '记录芒果科技组织发展、客户共创和品牌活动。', jumpUrl: 'https://github.com/HardyDou/mango/tree/main/mango-ui', openTarget: 'BLANK', sort: 1 },
        ],
        COMPANY_NEWS_TEXT: [
          { id: 'ad-company', adCode: 'mango-tech-company-news-text', adName: '公司动态页文本推荐', position: 'COMPANY_NEWS_TEXT', materialType: 'TEXT', title: '加入客户共创计划', textContent: '和芒果科技一起打磨更适合企业长期运营的官网与后台方案。', jumpUrl: '/company-news', sort: 1 },
        ],
        ABOUT_BANNER: [
          { id: 'ad-about-banner', adCode: 'mango-tech-about-banner', adName: '关于我们页 Banner', position: 'ABOUT_BANNER', materialType: 'SINGLE_IMAGE', imageFileId: '9605', imageUrl: runtimeImageUrl('9605'), title: '关于芒果科技', textContent: '专注企业数字化平台与可运营官网建设。', jumpUrl: 'https://github.com/HardyDou/mango/tree/main/mango', openTarget: 'BLANK', sort: 1 },
        ],
        ABOUT_TEXT: [
          { id: 'ad-about', adCode: 'mango-tech-about-text', adName: '关于我们页文本推荐', position: 'ABOUT_TEXT', materialType: 'TEXT', title: '联系芒果科技', textContent: '工作日 09:00-18:00 提供产品演示、方案咨询和交付评估。', jumpUrl: '/about', sort: 1 },
        ],
      };
      await route.fulfill({
        json: {
          code: 200,
          success: true,
          data: ads[position] || [],
        },
      });
      return;
    }

    if (path.endsWith('/site-categories/tree')) {
      await route.fulfill({
        json: {
          code: 200,
          success: true,
          data: [
            {
              id: 'cat-products',
              parentId: '0',
              categoryName: '产品能力',
              categoryCode: 'products',
              categoryType: 'LIST',
              accessPath: '/#products',
              seoDescription: '门户、CMS、权限租户、流程协同等平台能力。',
              sort: 1,
            },
            {
              id: 'cat-solutions',
              parentId: '0',
              categoryName: '解决方案',
              categoryCode: 'solutions',
              categoryType: 'LIST',
              accessPath: '/solutions',
              seoDescription: '覆盖政务服务、企业门户、产业运营和知识服务场景。',
              sort: 2,
              children: [
                {
                  id: 'cat-enterprise-portal',
                  parentId: 'cat-solutions',
                  categoryName: '企业门户',
                  categoryCode: 'enterprise-portal',
                  categoryType: 'LIST',
                  seoDescription: '统一官网、专题站和帮助中心建设。',
                  sort: 1,
                },
              ],
            },
            {
              id: 'cat-news',
              parentId: '0',
              categoryName: '新闻动态',
              categoryCode: 'news',
              categoryType: 'LIST',
              accessPath: '/news',
              seoDescription: '发布产品进展、行业观察和内容运营实践。',
              sort: 3,
            },
            {
              id: 'cat-company',
              parentId: '0',
              categoryName: '公司动态',
              categoryCode: 'company-news',
              categoryType: 'LIST',
              accessPath: '/company-news',
              seoDescription: '发布公司新闻、组织发展和品牌资讯。',
              sort: 4,
            },
            {
              id: 'cat-about',
              parentId: '0',
              categoryName: '关于我们',
              categoryCode: 'about',
              categoryType: 'DETAIL',
              accessPath: '/about',
              seoDescription: '了解芒果科技的公司定位、团队能力和服务方式。',
              sort: 5,
            },
          ],
        },
      });
      return;
    }

    if (path.endsWith('/contents/page')) {
      expect(url.searchParams.get('pageNum')).toBe('1');
      expect(url.searchParams.get('pageSize')).toBe('8');
      const categoryId = url.searchParams.get('categoryId');
      const list = categoryId ? contents.filter(item => item.categoryId === categoryId) : contents.slice(0, 3);
      await route.fulfill({
        json: {
          code: 200,
          success: true,
          data: {
            list,
            total: list.length,
            pageNum: 1,
            pageSize: 8,
            pages: 1,
          },
        },
      });
      return;
    }

    if (path.endsWith('/contents/detail')) {
      const contentId = url.searchParams.get('contentId');
      expect(contentId).toBeTruthy();
      const content = contents.find(item => item.id === contentId);
      await route.fulfill({
        status: content ? 200 : 404,
        json: content
          ? { code: 200, success: true, data: content }
          : { code: 404, success: false, msg: '内容不存在或尚未发布' },
      });
      return;
    }

    await route.fulfill({ status: 404, json: { code: 404, success: false, msg: `missing mock: ${path}` } });
  });
});

test('loads all enterprise site sections from cms public APIs', async ({ page }) => {
  const monitor = monitorBrowser(page);

  await page.goto('/');

  await expect(page.getByTestId('enterprise-site')).toBeVisible();
  await expect(page.getByTestId('site-error')).toBeHidden();
  await expect(page).toHaveTitle('芒果科技企业官网');
  await expect(page.getByLabel('主导航').getByRole('link', { name: '首页' })).toHaveAttribute('aria-current', 'page');
  await expect(page.getByLabel('主导航').getByRole('link', { name: '新闻动态' })).toBeVisible();
  await expect(page.getByLabel('主导航').getByRole('link', { name: '公司动态' })).toBeVisible();
  await expect(page.getByRole('heading', { name: '政企数字门户与内容运营平台' })).toBeVisible();
  await expect(page.locator('.hero-banner-link img')).toHaveAttribute('src', runtimeImageUrl('9601'));
  await expect(page.locator('.hero-banner-link')).toHaveAttribute('href', 'https://github.com/HardyDou/mango');
  await expect(page.locator('.hero-banner-link')).toHaveAttribute('target', '_blank');
  await expect(page.getByTestId('advertisement-panel')).toContainText('预约芒果科技产品演示');
  await expect(page.locator('#products')).toContainText('产品能力');
  await expect(page.getByRole('heading', { name: '聚焦产品进展与企业动态' })).toBeVisible();
  await expect(page.getByRole('link', { name: /查看详情/ }).first()).toHaveAttribute('href', '/news/content-article-1');
  await expect(page.getByText('广州市天河区软件园 18 号 / 400-800-2140')).toBeVisible();
  await expect(page.getByText('粤ICP备202600001号')).toBeVisible();
  await expect(page.getByRole('link', { name: 'GitHub' })).toHaveAttribute('target', '_blank');
  await expect(page.getByRole('link', { name: 'Issue 214' })).toHaveAttribute('href', 'https://github.com/HardyDou/mango/issues/214');

  expectRequested(monitor.cmsApiRequests, [
    '/sites/resolve',
    '/navigations/list',
    '/advertisements/list',
    '/site-categories/tree',
    '/contents/page',
  ]);
  expectPositionRequest(monitor.cmsApiRequests, '/advertisements/list', 'HOME_HERO');
  expectPositionRequest(monitor.cmsApiRequests, '/advertisements/list', 'HOME_FLOAT');
  expectNoRequest(monitor.cmsApiRequests, '/banners/list');
  expectNoProtectedFileDownload(monitor.protectedFileRequests);
  expect(monitor.failedResponses, `Failed CMS API responses:\n${monitor.failedResponses.join('\n')}`).toEqual([]);
  expect(monitor.errors, `Browser errors:\n${monitor.errors.join('\n')}`).toEqual([]);
});

test('supports url category and content navigation types from cms navigation data', async ({ page }) => {
  const monitor = monitorBrowser(page);

  await page.goto('/');

  await page.getByLabel('主导航').getByRole('link', { name: '产品能力' }).click();
  await expect(page).toHaveURL('/#products');
  await expect(page.getByLabel('主导航').getByRole('link', { name: '产品能力' })).toHaveAttribute('aria-current', 'page');
  await expect(page.locator('#products')).toBeVisible();

  await page.getByLabel('主导航').getByRole('link', { name: '解决方案' }).click();
  await expect(page).toHaveURL('/solutions/content-solution-1');
  await expect(page.getByRole('heading', { name: '集团门户统一建设方案' }).first()).toBeVisible();
  await expect(page.locator('.page-banner-media img')).toHaveAttribute('src', runtimeImageUrl('9602'));
  await expect(page.getByTestId('content-detail')).toContainText('集团门户统一建设方案');
  await expect(page.locator('.side-ad-list')).toContainText('预约方案顾问');

  await page.getByLabel('主导航').getByRole('link', { name: '新闻动态' }).click();
  await expect(page).toHaveURL('/news');
  await expect(page.getByRole('heading', { name: '新闻动态' })).toBeVisible();
  await expect(page.locator('.page-banner-media img')).toHaveAttribute('src', runtimeImageUrl('9603'));
  await expect(page.locator('.category-menu')).toHaveCount(0);
  await expect(page.locator('.list-ad-strip')).toContainText('订阅产品动态');

  await page.getByLabel('主导航').getByRole('link', { name: '关于我们' }).click();
  await expect(page).toHaveURL('/about/content-about-1');
  await expect(page.locator('.page-banner-media img')).toHaveAttribute('src', runtimeImageUrl('9605'));
  await expect(page.getByTestId('content-detail')).toContainText('关于芒果科技');
  await expect(page.locator('.side-ad-list')).toContainText('联系芒果科技');

  expectDetailRequest(monitor.cmsApiRequests, 'content-solution-1');
  expectDetailRequest(monitor.cmsApiRequests, 'content-about-1');
  expectPositionRequest(monitor.cmsApiRequests, '/advertisements/list', 'SOLUTIONS_BANNER');
  expectPositionRequest(monitor.cmsApiRequests, '/advertisements/list', 'SOLUTIONS_TEXT');
  expectPositionRequest(monitor.cmsApiRequests, '/advertisements/list', 'NEWS_BANNER');
  expectPositionRequest(monitor.cmsApiRequests, '/advertisements/list', 'NEWS_TEXT');
  expectPositionRequest(monitor.cmsApiRequests, '/advertisements/list', 'ABOUT_BANNER');
  expectPositionRequest(monitor.cmsApiRequests, '/advertisements/list', 'ABOUT_TEXT');
  expectNoRequest(monitor.cmsApiRequests, '/banners/list');
  expectNoProtectedFileDownload(monitor.protectedFileRequests);
  expect(monitor.failedResponses, `Failed CMS API responses:\n${monitor.failedResponses.join('\n')}`).toEqual([]);
  expect(monitor.errors, `Browser errors:\n${monitor.errors.join('\n')}`).toEqual([]);
});

test('renders news list with article image text and video content types then opens independent detail page', async ({ page }) => {
  const monitor = monitorBrowser(page);

  await page.goto('/news');

  await expect(page).toHaveURL('/news');
  await expect(page.getByLabel('主导航').getByRole('link', { name: '新闻动态' })).toHaveAttribute('aria-current', 'page');
  await expect(page.getByRole('heading', { name: '新闻动态' })).toBeVisible();
  await expect(page.locator('.page-banner-media img')).toHaveAttribute('src', runtimeImageUrl('9603'));
  await expect(page.locator('.detail-section')).toHaveCount(0);
  await expect(page.locator('.category-menu')).toHaveCount(0);
  await expect(page.locator('.list-ad-strip')).toContainText('订阅产品动态');

  const newsList = page.locator('.news-list');
  await expect(newsList).toBeVisible();
  await expect(newsList.locator('.news-item')).toHaveCount(3);

  await expect(newsList.getByRole('link', { name: /芒果科技发布统一门户建设方案/ })).toContainText('文章');
  await expect(newsList.getByRole('link', { name: /数字化运营中心图文巡礼/ })).toContainText('图文');
  await expect(newsList.getByRole('link', { name: /三分钟了解芒果科技门户平台/ })).toContainText('视频');
  await expect(newsList).toContainText('2026-06-22');
  await expect(newsList).toContainText('新闻动态');

  await newsList.getByRole('link', { name: /数字化运营中心图文巡礼/ }).click();
  await expect(page).toHaveURL('/news/content-image-1');
  await expect(page.locator('.news-list')).toHaveCount(0);
  await expect(page.locator('.detail-section')).toBeVisible();
  await expect(page.getByTestId('content-detail')).toContainText('图文');
  await expect(page.getByTestId('content-detail')).toContainText('图文展示');
  await expect(page.getByTestId('content-detail').locator('.rich-text img')).toHaveAttribute('alt', '数字化运营中心');
  await captureEvidence(page, 'enterprise-news-image-detail.png');

  expectDetailRequest(monitor.cmsApiRequests, 'content-image-1');
  expectPositionRequest(monitor.cmsApiRequests, '/advertisements/list', 'NEWS_BANNER');
  expectPositionRequest(monitor.cmsApiRequests, '/advertisements/list', 'NEWS_TEXT');
  expectNoRequest(monitor.cmsApiRequests, '/banners/list');
  expectNoProtectedFileDownload(monitor.protectedFileRequests);
  expect(monitor.failedResponses, `Failed CMS API responses:\n${monitor.failedResponses.join('\n')}`).toEqual([]);
  expect(monitor.errors, `Browser errors:\n${monitor.errors.join('\n')}`).toEqual([]);
});

test('renders company news list with page and attachment types and detail metadata', async ({ page }) => {
  const monitor = monitorBrowser(page);

  await page.goto('/company-news');

  await expect(page).toHaveURL('/company-news');
  await expect(page.getByLabel('主导航').getByRole('link', { name: '公司动态' })).toHaveAttribute('aria-current', 'page');
  await expect(page.getByRole('heading', { name: '公司动态' })).toBeVisible();
  await expect(page.locator('.page-banner-media img')).toHaveAttribute('src', runtimeImageUrl('9604'));
  await expect(page.locator('.news-list .news-item')).toHaveCount(2);
  await expect(page.locator('.news-list')).toContainText('单页');
  await expect(page.locator('.news-list')).toContainText('附件');
  await expect(page.locator('.news-list')).toContainText('芒果科技年度社会责任报告发布');
  await captureEvidence(page, 'enterprise-company-news-types.png');

  await page.getByRole('link', { name: /芒果科技年度社会责任报告发布/ }).click();
  await expect(page).toHaveURL('/company-news/content-company-attachment-1');
  await expect(page.getByTestId('content-detail')).toContainText('附件');
  await expect(page.getByTestId('content-detail')).toContainText('公司动态');
  await expect(page.getByTestId('content-detail')).toContainText('品牌中心');
  await expect(page.getByTestId('content-detail')).toContainText('芒果科技');
  await expect(page.getByTestId('content-detail').locator('.rich-text a', { hasText: '下载报告附件' })).toHaveAttribute('href', 'https://example.com/cms/esg-report.pdf');

  expectDetailRequest(monitor.cmsApiRequests, 'content-company-attachment-1');
  expectPositionRequest(monitor.cmsApiRequests, '/advertisements/list', 'COMPANY_NEWS_BANNER');
  expectPositionRequest(monitor.cmsApiRequests, '/advertisements/list', 'COMPANY_NEWS_TEXT');
  expectNoRequest(monitor.cmsApiRequests, '/banners/list');
  expectNoProtectedFileDownload(monitor.protectedFileRequests);
  expect(monitor.failedResponses, `Failed CMS API responses:\n${monitor.failedResponses.join('\n')}`).toEqual([]);
  expect(monitor.errors, `Browser errors:\n${monitor.errors.join('\n')}`).toEqual([]);
});

test('keeps rich text detail stable and sanitizes unsafe html', async ({ page }) => {
  const monitor = monitorBrowser(page);

  await page.goto('/news/content-article-1');

  await expect(page).toHaveURL('/news/content-article-1');
  await expect(page.getByLabel('主导航').getByRole('link', { name: '新闻动态' })).toHaveAttribute('aria-current', 'page');
  await expect(page.locator('.news-list')).toHaveCount(0);

  const detail = page.getByTestId('content-detail');
  await expect(detail).toBeVisible();
  await expect(detail).toContainText('文章');
  await expect(detail.getByRole('heading', { name: '芒果科技发布统一门户建设方案' })).toBeVisible();
  await expect(detail.locator('.rich-text h2')).toHaveText('统一门户建设方案');
  await expect(detail.locator('.rich-text strong')).toHaveText('富文本正文');
  await expect(detail.locator('.rich-text li')).toHaveCount(3);
  await expect(detail.locator('.rich-text blockquote')).toContainText('所有公开站点页面均通过域名解析站点');
  await expect(detail.locator('.rich-text table')).toContainText('新闻列表');
  await expect(detail.locator('.rich-text script')).toHaveCount(0);
  await expect(detail.locator('.rich-text p', { hasText: '清洗后的安全段落' })).not.toHaveAttribute('onclick', /.+/);
  expect(await page.evaluate(() => window.__cms_xss)).toBeUndefined();
  await captureEvidence(page, 'enterprise-news-rich-text-detail.png');

  const minHeight = await detail.evaluate(element => Number.parseFloat(window.getComputedStyle(element).minHeight));
  expect(minHeight).toBeGreaterThanOrEqual(420);
  expectDetailRequest(monitor.cmsApiRequests, 'content-article-1');
  expectPositionRequest(monitor.cmsApiRequests, '/advertisements/list', 'NEWS_BANNER');
  expectPositionRequest(monitor.cmsApiRequests, '/advertisements/list', 'NEWS_TEXT');
  expectNoRequest(monitor.cmsApiRequests, '/banners/list');
  expectNoProtectedFileDownload(monitor.protectedFileRequests);
  expect(monitor.failedResponses, `Failed CMS API responses:\n${monitor.failedResponses.join('\n')}`).toEqual([]);
  expect(monitor.errors, `Browser errors:\n${monitor.errors.join('\n')}`).toEqual([]);
});

test('renders video detail through direct route without embedding detail in list', async ({ page }) => {
  const monitor = monitorBrowser(page);

  await page.goto('/news/content-video-1');

  await expect(page).toHaveURL('/news/content-video-1');
  await expect(page.locator('.news-list')).toHaveCount(0);
  await expect(page.getByTestId('content-detail')).toContainText('视频');
  await expect(page.getByTestId('content-detail')).toContainText('视频内容说明');
  await expect(page.getByTestId('content-detail').locator('.rich-text video')).toHaveAttribute('controls', '');
  await captureEvidence(page, 'enterprise-news-video-detail.png');

  expectDetailRequest(monitor.cmsApiRequests, 'content-video-1');
  expectPositionRequest(monitor.cmsApiRequests, '/advertisements/list', 'NEWS_BANNER');
  expectPositionRequest(monitor.cmsApiRequests, '/advertisements/list', 'NEWS_TEXT');
  expectNoRequest(monitor.cmsApiRequests, '/banners/list');
  expectNoProtectedFileDownload(monitor.protectedFileRequests);
  expect(monitor.failedResponses, `Failed CMS API responses:\n${monitor.failedResponses.join('\n')}`).toEqual([]);
  expect(monitor.errors, `Browser errors:\n${monitor.errors.join('\n')}`).toEqual([]);
});

test('renders real cms news pagination covers and rich media detail', async ({ page }) => {
  const monitor = monitorBrowser(page);

  await page.unroute('**/runtime-cms-assets/ads/*.png').catch(() => undefined);
  await page.unroute('**/api/file/files/download**').catch(() => undefined);
  await page.unroute('**/api/cms-api/**').catch(() => undefined);
  await page.goto('/news');

  await expect(page.getByTestId('site-error')).toBeHidden();
  await expect(page.getByLabel('主导航').getByRole('link', { name: '新闻动态' })).toHaveAttribute('aria-current', 'page');
  await expect(page.getByRole('heading', { name: '新闻动态' })).toBeVisible();
  await expect(page.locator('.page-banner-media img')).toHaveAttribute('src', /\/api\/cms-api\/files\/public-preview\?id=/);

  const newsItems = page.locator('.news-list .news-item');
  await expect(newsItems).toHaveCount(8);
  await expect(page.locator('.news-cover img')).toHaveCount(8);
  await expect(page.locator('.content-pagination')).toContainText('共 9 条');
  await expect(page.locator('.content-pagination button.active')).toHaveText('1');

  await page.locator('.content-pagination button', { hasText: '2' }).click();
  await expect(page.locator('.content-pagination button.active')).toHaveText('2');
  await expect(page.locator('.news-list .news-item')).toHaveCount(1);
  await expect(page.locator('.news-cover img')).toHaveCount(1);

  await page.locator('.content-pagination button', { hasText: '1' }).click();
  await expect(page.locator('.content-pagination button.active')).toHaveText('1');
  await expect(page.locator('.news-list .news-item')).toHaveCount(8);
  const firstNewsTitle = '芒果科技发布企业数字化官网套件';
  const firstNews = page.locator('.news-list .news-item', { hasText: firstNewsTitle }).first();
  await expect(firstNews).toBeVisible();
  await firstNews.click();

  await expect(page).toHaveURL(/\/news\/\d+/);
  await expect(page.locator('.news-list')).toHaveCount(0);
  const detail = page.getByTestId('content-detail');
  await expect(detail.getByRole('heading', { name: firstNewsTitle })).toBeVisible();
  await expect(detail.locator('.article-cover')).toHaveAttribute('src', /\/api\/cms-api\/files\/public-preview\?id=/);
  await expect(detail.locator('.rich-text img')).toHaveAttribute('src', /\/api\/cms-api\/files\/public-preview\?id=/);
  await expect(detail.locator('.rich-text video source')).toHaveAttribute('src', /\/api\/cms-api\/files\/public-preview\?id=/);
  await expect(detail.locator('.rich-text a[href*="/api/cms-api/files/public-preview"]')).toBeVisible();
  await expect(detail.locator('.rich-text table')).toBeVisible();
  await expect(detail.locator('.rich-text li')).toHaveCount(3);
  await expect(detail.locator('.rich-text blockquote')).toContainText('CMS 后台上传组件');
  await captureEvidence(page, 'enterprise-real-news-pagination-rich-media.png');

  expectRequested(monitor.cmsApiRequests, [
    '/sites/resolve',
    '/navigations/list',
    '/advertisements/list',
    '/site-categories/tree',
    '/contents/page',
    '/contents/detail',
  ]);
  expectPositionRequest(monitor.cmsApiRequests, '/advertisements/list', 'NEWS_BANNER');
  expectPositionRequest(monitor.cmsApiRequests, '/advertisements/list', 'NEWS_TEXT');
  expectNoRequest(monitor.cmsApiRequests, '/banners/list');
  expect(monitor.failedResponses, `Failed CMS API responses:\n${monitor.failedResponses.join('\n')}`).toEqual([]);
  expect(monitor.errors, `Browser errors:\n${monitor.errors.join('\n')}`).toEqual([]);
});

function monitorBrowser(page: Page) {
  const errors: string[] = [];
  const failedResponses: string[] = [];
  const cmsApiRequests: string[] = [];
  const protectedFileRequests: string[] = [];

  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => {
    if (message.type() === 'error') {
      errors.push(message.text());
    }
  });
  page.on('request', request => {
    if (request.url().includes('/api/file/files/download')) {
      protectedFileRequests.push(request.url());
    }
  });
  page.on('response', response => {
    if (response.url().includes('/cms-api/')) {
      cmsApiRequests.push(response.url());
      if (!response.ok()) {
        failedResponses.push(`${response.status()} ${response.url()}`);
      }
    }
  });

  return { errors, failedResponses, cmsApiRequests, protectedFileRequests };
}

function expectRequested(requests: string[], pathSuffixes: string[]) {
  const requestedPaths = requests.map(item => new URL(item).pathname);
  for (const suffix of pathSuffixes) {
    expect(requestedPaths.some(path => path.endsWith(suffix)), `missing request ${suffix}`).toBeTruthy();
  }
}

function expectDetailRequest(requests: string[], contentId: string) {
  expect(requests.some(item => {
    const url = new URL(item);
    return url.pathname.endsWith('/contents/detail') && url.searchParams.get('contentId') === contentId;
  }), `missing detail request for ${contentId}`).toBeTruthy();
}

function expectPositionRequest(requests: string[], pathSuffix: string, position: string) {
  expect(requests.some(item => {
    const url = new URL(item);
    return url.pathname.endsWith(pathSuffix) && url.searchParams.get('position') === position;
  }), `missing ${pathSuffix} request for ${position}`).toBeTruthy();
}

function expectNoRequest(requests: string[], pathSuffix: string) {
  expect(requests.some(item => new URL(item).pathname.endsWith(pathSuffix)), `unexpected request ${pathSuffix}`).toBeFalsy();
}

function expectNoProtectedFileDownload(requests: string[]) {
  expect(requests, `官网不得直接请求受保护文件下载接口:\n${requests.join('\n')}`).toEqual([]);
}

async function captureEvidence(page: Page, filename: string) {
  const evidenceDir = process.env.CMS_EVIDENCE_DIR;
  if (!evidenceDir) {
    return;
  }
  await fs.mkdir(evidenceDir, { recursive: true });
  await page.screenshot({ path: path.join(evidenceDir, filename), fullPage: true });
}
