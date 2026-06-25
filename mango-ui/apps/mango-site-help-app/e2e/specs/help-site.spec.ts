import { expect, test } from '@playwright/test';

test.beforeEach(async ({ page }) => {
  await page.route('**/api/cms-api/**', async route => {
    const url = new URL(route.request().url());
    const path = url.pathname;
    expect(url.searchParams.get('domain')).toBe('127.0.0.1:5192');
    expect(url.searchParams.has('siteCode')).toBeFalsy();

    if (path.endsWith('/files/public-preview')) {
      await route.fulfill({
        status: 200,
        contentType: 'image/png',
        body: Buffer.from(
          'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAFgwJ/lwHh3QAAAABJRU5ErkJggg==',
          'base64',
        ),
      });
      return;
    }

    if (path.endsWith('/sites/resolve')) {
      await route.fulfill({
        json: {
          code: 200,
          success: true,
          data: {
            siteId: '2',
            siteCode: 'mango-help',
            siteName: '芒果科技帮助中心',
            seoTitle: '芒果科技帮助中心',
            seoDescription: '面向运营、实施和开发团队的产品帮助中心。',
            footerCopyright: 'Copyright 2026 芒果科技帮助中心',
            icpRecord: '沪ICP备20260214号-2',
            contactInfo: 'support@mango-tech.example',
          },
        },
      });
      return;
    }

    if (path.endsWith('/navigations/list')) {
      expect(url.searchParams.get('navType')).toBe('TOP');
      await route.fulfill({
        json: {
          code: 200,
          success: true,
          data: [
            { id: 'nav-1', navName: '快速入门', categoryId: '31', sort: 1 },
            { id: 'nav-2', navName: '后台管理', categoryId: '32', sort: 2 },
          ],
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
            { id: '31', parentId: '0', categoryName: '快速入门', categoryCode: 'start', seoDescription: '创建站点与发布内容。', sort: 1 },
            { id: '32', parentId: '0', categoryName: '后台管理', categoryCode: 'admin', seoDescription: '菜单、权限与配置。', sort: 2 },
          ],
        },
      });
      return;
    }

    if (path.endsWith('/advertisements/list')) {
      const position = url.searchParams.get('position');
      expect(['HELP_HERO', 'HELP_TOP']).toContain(position);
      await route.fulfill({
        json: {
          code: 200,
          success: true,
          data: position === 'HELP_HERO'
            ? [
                {
                  id: 'hero-1',
                  adCode: 'help-hero',
                  adName: '帮助中心首页 Banner',
                  position: 'HELP_HERO',
                  materialType: 'SINGLE_IMAGE',
                  title: '芒果科技帮助中心',
                  textContent: '从站点创建到内容发布，按真实后台能力组织操作指南。',
                  imageUrl: '/api/cms-api/files/public-preview?id=9301&domain=127.0.0.1%3A5192',
                  jumpUrl: '#contents',
                  sort: 1,
                },
              ]
            : [{ id: '1', adCode: 'help-top', adName: '服务公告', title: '服务公告', textContent: '帮助中心公告内容。', position: 'HELP_TOP', adType: 'TEXT', sort: 1 }],
        },
      });
      return;
    }

    if (path.endsWith('/contents/page')) {
      const keyword = url.searchParams.get('keyword');
      const categoryId = url.searchParams.get('categoryId');
      await route.fulfill({
        json: {
          code: 200,
          success: true,
          data: {
            list: [
              {
                id: keyword ? '43' : categoryId ? '42' : '41',
                title: keyword ? '搜索结果文档' : categoryId ? '栏目文档' : '如何创建一个可公开访问的官网站点',
                summary: '帮助摘要来自 CMS 内容分页接口。',
                contentType: 'ARTICLE',
                categoryId: categoryId || '31',
                categoryName: categoryId ? '快速入门' : '站点运营',
                source: '芒果科技帮助中心',
                author: '支持团队',
              },
            ],
            total: 1,
            pageNum: 1,
            pageSize: 10,
            pages: 1,
          },
        },
      });
      return;
    }

    if (path.endsWith('/contents/detail')) {
      expect(url.searchParams.get('contentId')).toBeTruthy();
      await route.fulfill({
        json: {
          code: 200,
          success: true,
          data: {
            id: url.searchParams.get('contentId'),
            title: '如何创建一个可公开访问的官网站点',
            summary: '帮助摘要来自 CMS 内容分页接口。',
            body: '<p>这是来自 CMS 内容详情接口的 <strong>富文本正文</strong>。</p><ol><li>维护站点域名</li><li>配置栏目导航</li><li>发布内容关系</li></ol>',
            source: '芒果科技帮助中心',
            author: '支持团队',
            publishTime: '2026-06-22T09:00:00',
          },
        },
      });
      return;
    }

    await route.fulfill({ status: 404, json: { code: 404, message: 'missing mock' } });
  });
});

test('loads help site CMS data and filters content', async ({ page }) => {
  const errors: string[] = [];
  const failedResponses: string[] = [];
  const cmsApiRequests: string[] = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => {
    if (message.type() === 'error') {
      errors.push(message.text());
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

  await page.goto('/');
  await expect(page.getByTestId('help-site')).toBeVisible();
  await expect(page.getByRole('navigation', { name: '帮助站导航' })).toContainText('快速入门');
  await expect(page.getByRole('heading', { name: '芒果科技帮助中心', level: 1 })).toBeVisible();
  await expect(page.getByTestId('advertisement-panel')).toContainText('服务公告');
  await expect(page.getByRole('heading', { name: '如何创建一个可公开访问的官网站点' })).toBeVisible();
  await expect(page.getByTestId('content-detail')).toContainText('富文本正文');
  await expect(page.getByTestId('site-error')).toBeHidden();

  await page.getByLabel('搜索内容').fill('权限');
  await page.getByRole('button', { name: '搜索', exact: true }).click();
  await expect(page.getByRole('button', { name: /搜索结果文档/ })).toBeVisible();
  await page.getByLabel('搜索内容').clear();
  await page.getByRole('button', { name: '搜索', exact: true }).click();
  await page.getByRole('button', { name: '快速入门' }).click();
  await expect(page.getByRole('button', { name: /栏目文档/ })).toBeVisible();
  await expect(page).toHaveTitle('芒果科技帮助中心');

  expect(cmsApiRequests.length).toBeGreaterThanOrEqual(7);
  expect(cmsApiRequests.some(request => request.includes('/banners/list'))).toBeFalsy();
  expect(cmsApiRequests.some(request => request.includes('/advertisements/list') && request.includes('HELP_HERO'))).toBeTruthy();
  expect(cmsApiRequests.some(request => request.includes('/advertisements/list') && request.includes('HELP_TOP'))).toBeTruthy();
  expect(failedResponses, `Failed CMS API responses:\n${failedResponses.join('\n')}`).toEqual([]);
  expect(errors, `Browser errors:\n${errors.join('\n')}`).toEqual([]);
});
