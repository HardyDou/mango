import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const evidenceDir = resolve(
  __dirname,
  '../../../../../mango-docs/designs/issue-1055-information-collection/.runtime/evidence/issue-1055',
);

const formCode = process.env.MANGO_INFO_FORM_CODE || 'customer-contact';

async function goBack(page: Page) {
  await page.getByRole('button', { name: '返回' }).first().click();
}

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
  test('表单管理与详情、配置、接入、提交记录页面可用', async ({ page }) => {
    await login(page);

    // 表单管理：菜单只有“表单管理”，列表列与原型一致
    await page.goto('/#/information/forms');
    await expect(page.locator('[data-page="information.forms"]')).toBeVisible();
    await expect(page.getByText(formCode)).toBeVisible();
    for (const label of ['表单名称', '表单编码', '备注', '状态', '提交量', '创建日期', '更新日期', '创建人', '操作']) {
      await expect(page.getByRole('columnheader', { name: label, exact: true })).toBeVisible();
    }
    await expect(page.getByRole('button', { name: '提交记录' }).first()).toBeVisible();
    await expect(page.getByRole('button', { name: '接入' }).first()).toBeVisible();
    await shot(page, '01-form-list.png');

    // 表单详情
    await page.getByRole('button', { name: '详情', exact: true }).first().click();
    await expect(page.locator('[data-page="information.form.detail"]')).toBeVisible();
    await expect(page.getByText('返回列表页')).toBeVisible();
    await expect(page.getByText('字段配置')).toBeVisible();
    await expect(page.locator('.information-section-title').filter({ hasText: '接入方式' })).toBeVisible();
    await shot(page, '02-form-detail.png');

    // 表单配置（可视化搭建）
    await page.getByRole('button', { name: '配置表单' }).click();
    await expect(page.locator('[data-page="information.form.config"]')).toBeVisible();
    await expect(page.getByRole('heading', { name: '添加控件' })).toBeVisible();
    await expect(page.getByText('基础字段', { exact: true })).toBeVisible();
    await expect(page.getByRole('heading', { name: '字段属性' })).toBeVisible();
    await expect(page.getByRole('heading', { name: '当前字段' })).toBeVisible();
    await shot(page, '03-form-config.png');

    // 接入方式
    await goBack(page);
    await expect(page.locator('[data-page="information.form.detail"]')).toBeVisible();
    await page.getByRole('button', { name: '接入方式' }).click();
    await expect(page.locator('[data-page="information.form.hosted"]')).toBeVisible();
    await expect(page.getByText('使用方式 ② · 托管页面')).toBeVisible();
    await expect(page.getByText('无需密钥')).toBeVisible();
    await expect(page.getByText('需要表单专属 SK')).toBeVisible();
    await shot(page, '04-form-hosted.png');

    // 提交记录
    await goBack(page);
    await expect(page.locator('[data-page="information.form.detail"]')).toBeVisible();
    await page.getByRole('button', { name: '提交记录' }).click();
    await expect(page.locator('[data-page="information.form.submissions"]')).toBeVisible();
    await expect(page.getByText('返回详情页')).toBeVisible();
    for (const label of ['提交编号', '提交时间', '状态', '来源页面']) {
      await expect(page.getByRole('columnheader', { name: label, exact: true })).toBeVisible();
    }
    await shot(page, '05-submission-list.png');

    await page.getByRole('button', { name: '详情', exact: true }).first().click();
    const drawer = page.locator('.el-drawer');
    await expect(drawer.getByText('提交详情')).toBeVisible();
    await expect(drawer.getByText('处理备注')).toBeVisible();
    await expect(drawer).toBeVisible();
    await page.waitForTimeout(600);
    mkdirSync(evidenceDir, { recursive: true });
    await page.screenshot({ path: resolve(evidenceDir, '06-submission-detail.png') });
  });

  test('匿名访客可打开托管表单并提交', async ({ page }) => {
    await page.goto(`/#/information/hosted?formCode=${formCode}`);
    await expect(page.locator('[data-page="information.hosted"]')).toBeVisible();
    await expect(page.getByRole('heading', { name: '客户联系表' })).toBeVisible();
    await shot(page, '07-hosted-form.png');

    await page.getByPlaceholder('请输入姓名').fill('浏览器访客');
    await page.getByPlaceholder('请输入手机号').fill('13600136000');
    await page.getByPlaceholder('请输入需求描述').fill('来自托管表单页面的真实提交');
    await page.getByRole('button', { name: '提交', exact: true }).click();
    await expect(page.getByText('提交成功', { exact: true })).toBeVisible();
    await page.waitForTimeout(600);
    mkdirSync(evidenceDir, { recursive: true });
    await page.screenshot({ path: resolve(evidenceDir, '08-hosted-submit-success.png') });
  });
});
