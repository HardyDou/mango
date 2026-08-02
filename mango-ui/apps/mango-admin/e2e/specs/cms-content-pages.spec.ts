import { expect, test, type Page } from '@playwright/test';
import { openUnlabelledElementPlusCombobox } from '../support/element-plus';

const cmsPages = [
  ['/cms/sites', '站点管理'],
  ['/cms/site-categories', '站点栏目'],
  ['/cms/contents', '内容管理'],
  ['/cms/content-categories', '内容分类'],
  ['/cms/content-tags', '内容标签'],
  ['/cms/content-publishes', '内容发布'],
  ['/cms/navigations', '导航管理'],
  ['/cms/advertisements', '广告位管理'],
  ['/cms/ad-deliveries', '广告投放管理'],
] as const;

async function login(page: Page) {
  await page.goto('/#/login');
  await page.getByPlaceholder('请输入用户名').fill('admin');
  await page.getByPlaceholder('请输入密码').fill('admin123');
  await page.getByPlaceholder('请输入密码').blur();
  await openUnlabelledElementPlusCombobox(page);
  await page.getByRole('option', { name: /芒果集团/ }).click();
  await page.getByRole('button', { name: '登录', exact: true }).click();
  await page.waitForURL('**/#/home');
}

test('@p0 @cms 内容运营页面均能解析为有效 Vue 组件', async ({ page }) => {
  const invalidVnodeWarnings: string[] = [];
  page.on('console', (message) => {
    if (message.text().includes('Invalid vnode type')) invalidVnodeWarnings.push(message.text());
  });

  await login(page);

  for (const [path, title] of cmsPages) {
    await page.goto(`/#${path}`);
    await expect(page.getByRole('heading', { name: title, level: 2 })).toBeVisible();
  }

  expect(invalidVnodeWarnings).toEqual([]);
});
