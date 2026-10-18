import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const evidenceDir = resolve(
  __dirname,
  '../../../../../mango-docs/designs/issue-1055-information-collection/.runtime/evidence/issue-1055',
);

const formCode = process.env.MANGO_INFO_FORM_CODE || 'customer-contact';

async function shot(page: Page, filename: string) {
  mkdirSync(evidenceDir, { recursive: true });
  await page.screenshot({ path: resolve(evidenceDir, filename), fullPage: true });
}

async function login(page: Page) {
  await page.goto('/#/login');
  await page.getByPlaceholder('请输入用户名').fill('admin');
  await page.getByPlaceholder('请输入密码').fill('admin123');
  await page.getByPlaceholder('请输入密码').blur();
  const tenantSelect = page.locator('.tenant-select');
  if (await tenantSelect.count()) {
    await tenantSelect.click();
    await page.getByRole('option', { name: /芒果集团/ }).click();
  }
  await page.getByRole('button', { name: '登录', exact: true }).click();
  await page.waitForURL('**/#/home');
}

test.describe('@p0 @information 信息采集中心', () => {
  test('后台表单管理与提交记录页面可用', async ({ page }) => {
    await login(page);

    // 表单列表
    await page.goto('/#/information/forms');
    await expect(page.locator('[data-page="information.forms"]')).toBeVisible();
    await expect(page.getByText(formCode)).toBeVisible();
    await shot(page, '01-form-list.png');

    // 表单详情
    await page.getByRole('button', { name: '详情' }).first().click();
    const detailDialog = page.getByRole('dialog', { name: '表单详情' });
    await expect(detailDialog).toBeVisible();
    await expect(detailDialog.getByText('托管入口')).toBeVisible();
    await shot(page, '02-form-detail.png');
    await page.keyboard.press('Escape');

    // 表单配置
    await page.getByRole('button', { name: '配置' }).first().click();
    await expect(page.getByRole('dialog', { name: '配置表单' })).toBeVisible();
    await shot(page, '03-form-config.png');
    await page.keyboard.press('Escape');

    // 提交记录
    await page.goto('/#/information/submissions');
    await expect(page.locator('[data-page="information.submissions"]')).toBeVisible();
    await expect(page.getByText('提交编号')).toBeVisible();
    await shot(page, '04-submission-list.png');

    // 提交详情
    await page.getByRole('button', { name: '详情' }).first().click();
    await expect(page.getByRole('dialog', { name: '提交详情' })).toBeVisible();
    await shot(page, '05-submission-detail.png');
  });

  test('匿名访客可打开托管表单并提交', async ({ page }) => {
    await page.goto(`/#/information/hosted?formCode=${formCode}`);
    await expect(page.getByRole('heading', { name: '客户联系表' })).toBeVisible();
    await shot(page, '06-hosted-form.png');

    await page.getByLabel('姓名').fill('浏览器访客');
    await page.getByLabel('手机号').fill('13600136000');
    await page.getByLabel('需求描述').fill('来自托管表单页面的真实提交');
    await page.getByRole('button', { name: '提交' }).click();
    await expect(page.getByText('提交成功', { exact: true })).toBeVisible();
    await shot(page, '07-hosted-submit-success.png');
  });
});
