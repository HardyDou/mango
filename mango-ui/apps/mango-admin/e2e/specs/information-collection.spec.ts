import { expect, test, type Page } from '@playwright/test';
import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const evidenceDir = resolve(
  __dirname,
  '../../../../../mango-docs/designs/issue-1055-information-collection/.runtime/evidence/issue-1055',
);

const formCode = process.env.MANGO_INFO_FORM_CODE || 'customer-contact';
const formId = process.env.MANGO_INFO_FORM_ID || '2111981833178341377';

async function shot(page: Page, filename: string, fullPage = true) {
  mkdirSync(evidenceDir, { recursive: true });
  await page.screenshot({ path: resolve(evidenceDir, filename), fullPage });
}

/** 表单列表按编码取主键，供深链用例定位目标表单。 */
async function loadFormId(page: Page, code: string): Promise<string> {
  const token = await page.evaluate(() => sessionStorage.getItem('MANGO_TOKEN') || '');
  const response = await page.request.get('/api/information/forms/page', {
    headers: { Authorization: `Bearer ${token}`, 'X-Mango-Tenant-Id': '1' },
    params: { page: '1', size: '1', keyword: code },
  });
  const body = await response.json();
  return String(body?.data?.list?.[0]?.id ?? '');
}

async function goBack(page: Page) {
  await page.getByRole('button', { name: '返回' }).first().click();
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

test.describe('@p0 @information 信息采集管理', () => {
  test('表单管理与详情、设计器、接入页面可用', async ({ page }) => {
    await login(page);

    // 表单管理：原型列与操作
    await page.goto('/#/information/forms');
    await expect(page.locator('[data-page="information.forms"]')).toBeVisible();
    await page.getByPlaceholder('名称/编码').first().fill(formCode);
    await page.getByRole('button', { name: '查询', exact: true }).click();
    for (const label of ['表单名称', '表单编码', '备注', '状态', '提交量', '创建日期', '更新日期', '创建人', '操作']) {
      await expect(page.getByRole('columnheader', { name: label, exact: true })).toBeVisible();
    }
    const targetRow = page.locator('.el-table__body tr', { hasText: formCode }).first();
    await expect(targetRow).toBeVisible();
    await shot(page, '01-form-list.png');

    // 表单详情
    await targetRow.getByRole('button', { name: '详情', exact: true }).click();
    await expect(page.locator('[data-page="information.form.detail"]')).toBeVisible();
    await expect(page.getByText('返回列表页')).toBeVisible();
    await expect(page.getByText('字段配置')).toBeVisible();
    await expect(page.locator('.information-section-title').filter({ hasText: '接入方式' })).toBeVisible();
    await expect(page.locator('[data-access-action="copy-hosted"]')).toBeVisible();
    await expect(page.locator('[data-access-action="open-hosted"]')).toBeVisible();
    await shot(page, '02-form-detail.png');

    // 表单设计：复用平台表单设计器
    await page.getByRole('button', { name: '配置表单' }).click();
    await expect(page.locator('[data-page="information.form.config"]')).toBeVisible();
    const designer = page.locator('[data-surface="information.form.designer"]');
    await expect(designer).toBeVisible({ timeout: 20000 });
    await expect(designer.getByRole('heading', { name: '基础组件' })).toBeVisible();
    await expect(designer.getByText('表单配置', { exact: true })).toBeVisible();
    await expect(designer.getByText('姓名', { exact: true }).first()).toBeVisible();
    await expect(designer.getByText('手机号', { exact: true }).first()).toBeVisible();
    await shot(page, '03-form-designer.png');

    // 初始无改动时保存按钮不可点，交互后出现未保存提示
    await expect(page.getByRole('button', { name: '保存配置' })).toBeDisabled();
    await page.locator('[data-surface="information.form.designer"]').click({ position: { x: 30, y: 30 } });
    await expect(page.getByText('有未保存的修改')).toBeVisible({ timeout: 20000 });

    // 保存后规则保持不变
    await page.getByRole('button', { name: '保存配置' }).click();
    await expect(page.getByText('配置已保存')).toBeVisible();
    await expect(page.getByRole('button', { name: '保存配置' })).toBeDisabled();
    await page.reload();
    const designerAfterReload = page.locator('[data-surface="information.form.designer"]');
    await expect(designerAfterReload).toBeVisible({ timeout: 20000 });
    await expect(designerAfterReload.getByText('需求描述', { exact: true }).first()).toBeVisible();

    // 接入方式
    await goBack(page);
    await expect(page.locator('[data-page="information.form.detail"]')).toBeVisible();
    await page.getByRole('button', { name: '接入方式', exact: true }).click();
    await expect(page.locator('[data-page="information.form.hosted"]')).toBeVisible();
    await expect(page.getByText('使用方式 ② · 托管页面')).toBeVisible();
    await expect(page.getByText('无需密钥')).toBeVisible();
    await expect(page.getByText('需要表单专属 SK')).toBeVisible();
    await expect(page.locator('.information-browser-url')).toContainText('/#/information/hosted?formCode=');
    await expect(page.locator('[data-secret-action="copy"]')).toBeVisible();
    await expect(page.locator('.information-api-value')).toContainText('••••');
    await page.locator('[data-secret-action="toggle"]').click();
    await expect(page.locator('.information-api-value')).not.toContainText('••••');
    await shot(page, '04-form-hosted.png');
  });

  test('提交记录是独立菜单入口且不依赖表单详情', async ({ page }) => {
    await login(page);
    await page.goto('/#/information/submissions');
    await expect(page.locator('[data-page="information.form.submissions"]')).toBeVisible();
    // 独立入口：没有返回详情页按钮
    await expect(page.getByText('返回详情页')).toHaveCount(0);
    // 通过表单筛选切换当前表单
    await expect(page.locator('[data-filter="form"]')).toBeVisible();
    // 深链：地址栏带 formId 时直接定位到该表单，刷新与分享保持同一视图
    await page.goto(`/#/information/submissions?formId=${formId}`);
    await expect(page).toHaveURL(new RegExp(`formId=${formId}`));
    await expect(page.locator('[data-filter="form"]')).toContainText(formCode, { timeout: 20000 });

    for (const label of ['记录ID', '提交时间', '状态', '来源页面', '姓名', '手机号']) {
      await expect(page.getByRole('columnheader', { name: label, exact: true })).toBeVisible();
    }
    await shot(page, '05-submission-list.png');

    // 行点击也能打开抽屉，并且筛选写入地址栏
    await expect(page).toHaveURL(/formId=/);
    await page.locator('.el-table__body tr').first().click();
    const drawer = page.locator('.el-drawer');
    await expect(drawer.getByText('提交详情')).toBeVisible();
    await expect(drawer.getByText('处理备注')).toBeVisible();
    await page.waitForTimeout(600);
    mkdirSync(evidenceDir, { recursive: true });
    await page.screenshot({ path: resolve(evidenceDir, '06-submission-detail.png') });
  });

  test('提交记录列随表单字段定义动态变化', async ({ page }) => {
    await login(page);
    const token = await page.evaluate(() => sessionStorage.getItem('MANGO_TOKEN') || '');
    const headers = { Authorization: `Bearer ${token}`, 'X-Mango-Tenant-Id': '1' };

    // 用设计器规则形状（form-create rules）准备一张字段完全不同的表单
    const definitionJson = JSON.stringify([
      {
        type: 'input',
        field: 'company',
        title: '公司名称',
        props: { placeholder: '请输入公司名称' },
        validate: [{ required: true, message: '请填写公司名称' }],
      },
      {
        type: 'select',
        field: 'scale',
        title: '企业规模',
        options: [
          { label: '小型', value: 's' },
          { label: '大型', value: 'l' },
        ],
      },
      { type: 'elDivider', title: '以下为展示区' },
    ]);
    const existing = await page.request.get('/api/information/forms/page', {
      headers,
      params: { page: '1', size: '1', keyword: 'e2e-dynamic-columns' },
    });
    const existingId = (await existing.json())?.data?.list?.[0]?.id;
    const save = await page.request.post('/api/information/forms/save', {
      headers,
      data: {
        ...(existingId ? { id: existingId } : {}),
        formCode: 'e2e-dynamic-columns',
        formName: '动态列验证表',
        definitionJson,
        status: 'PUBLISHED',
      },
    });
    expect(save.status()).toBe(200);

    const submit = await page.request.post('/api/information/open/forms/submit', {
      data: {
        formCode: 'e2e-dynamic-columns',
        values: { company: '动态列验证公司', scale: 'l' },
        sourcePage: 'dynamic-column-check',
      },
    });
    expect(submit.status()).toBe(200);
    expect((await submit.json()).success).toBeTruthy();

    const dynamicFormId = existingId || (await loadFormId(page, 'e2e-dynamic-columns'));
    await page.goto(`/#/information/submissions?formId=${dynamicFormId}`);

    for (const label of ['记录ID', '提交时间', '状态', '来源页面', '公司名称', '企业规模']) {
      await expect(page.getByRole('columnheader', { name: label, exact: true })).toBeVisible();
    }
    for (const label of ['手机号', '需求描述', '以下为展示区']) {
      await expect(page.getByRole('columnheader', { name: label, exact: true })).toHaveCount(0);
    }
    await expect(page.getByRole('cell', { name: '动态列验证公司' }).first()).toBeVisible();
    await expect(page.getByRole('cell', { name: '大型', exact: true }).first()).toBeVisible();
    await shot(page, '09-submission-dynamic-columns.png');
  });

  test('匿名访客可打开托管表单并提交', async ({ browser }) => {
    // 全新会话、无任何登录态，验证分享网址无需登录即可提交
    const context = await browser.newContext();
    const page = await context.newPage();
    await page.goto(`/#/information/hosted?formCode=${formCode}`);
    await expect(page.locator('[data-page="information.hosted"]')).toBeVisible();
    await expect(page).not.toHaveURL(/#\/login/);
    await expect(page.getByRole('heading', { name: '客户联系表' })).toBeVisible();
    await shot(page, '07-hosted-form.png');

    await page.getByPlaceholder('请输入姓名').fill('浏览器访客');
    await page.getByPlaceholder('请输入手机号').fill('13600136000');
    await page.getByPlaceholder('请输入需求描述').fill('来自托管表单页面的真实提交');
    // 设计器里的控件必须原样渲染：评分是星级而不是文本框
    await expect(page.locator('.el-rate')).toBeVisible();
    await expect(page.locator('.el-rate__item')).toHaveCount(5);
    await page.locator('.el-rate__item').nth(3).click();
    await page.getByRole('button', { name: '提交', exact: true }).click();
    await expect(page.getByText('提交成功', { exact: true })).toBeVisible();
    await page.waitForTimeout(600);
    mkdirSync(evidenceDir, { recursive: true });
    await page.screenshot({ path: resolve(evidenceDir, '08-hosted-submit-success.png') });
    await page.locator('[data-hosted-action="again"]').click();
    await expect(page.getByPlaceholder('请输入姓名')).toHaveValue('');
    await context.close();
  });

  test('表单详情入口可直达提交记录并预选当前表单', async ({ page }) => {
    await login(page);
    await page.goto(`/#/information/forms/${formId}/detail`);
    await page.getByRole('button', { name: '提交记录' }).click();
    await expect(page.locator('[data-page="information.form.submissions"]')).toBeVisible();
    await expect(page.locator('[data-filter="form"]')).toContainText(formCode);
  });
});
