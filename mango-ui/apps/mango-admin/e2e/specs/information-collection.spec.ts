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
    // 列表按更新时间排序，先按编码过滤，保证用例只作用于目标表单
    await page.getByPlaceholder('名称/编码').first().fill(formCode);
    await page.getByRole('button', { name: '查询' }).click();
    await expect(page.locator('.el-table__body tr')).toHaveCount(1);
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

    // 画布中选中字段后，右侧字段属性跟随切换（对齐原型）
    await expect(page.locator('.information-canvas-field.is-selected')).toContainText('姓名');
    await page.locator('.information-canvas-field[data-field-code="phone"]').click();
    await expect(page.locator('.information-canvas-field.is-selected')).toContainText('手机号');
    await expect(page.locator('input[data-field-config="label"]')).toHaveValue('手机号');
    await expect(page.locator('input[data-field-config="code"]')).toHaveValue('phone');
    await expect(page.locator('.information-canvas-field.is-selected')).toHaveAttribute('data-field-code', 'phone');
    await shot(page, '03-form-config-selected.png');
    await page.locator('.information-canvas-field[data-field-code="name"]').click();
    await expect(page.locator('input[data-field-config="label"]')).toHaveValue('姓名');
    await shot(page, '03-form-config.png');

    // 保存后右侧字段属性仍停留在当前编辑字段（不丢内容）
    await page.getByRole('button', { name: '保存配置' }).click();
    await expect(page.getByText('配置已保存')).toBeVisible();
    await expect(page.locator('input[data-field-config="label"]')).toHaveValue('姓名');
    await expect(page.locator('.information-canvas-field')).toHaveCount(3);

    // 接入方式
    await goBack(page);
    await expect(page.locator('[data-page="information.form.detail"]')).toBeVisible();
    await page.getByRole('button', { name: '接入方式' }).click();
    await expect(page.locator('[data-page="information.form.hosted"]')).toBeVisible();
    await expect(page.getByText('使用方式 ② · 托管页面')).toBeVisible();
    await expect(page.getByText('无需密钥')).toBeVisible();
    await expect(page.getByText('需要表单专属 SK')).toBeVisible();
    await expect(page.locator('.information-browser-url')).toContainText('/#/information/hosted?formCode=');
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

  test('提交记录列随表单字段定义动态变化', async ({ page }) => {
    await login(page);
    const token = await page.evaluate(() => sessionStorage.getItem('MANGO_TOKEN') || '');
    const headers = { Authorization: `Bearer ${token}`, 'X-Mango-Tenant-Id': '1' };

    // 准备一张字段与 customer-contact 完全不同的表单，保证用例独立于其他数据
    const definitionJson = JSON.stringify([
      { fieldCode: 'company', fieldName: '公司名称', fieldType: 'text', required: true, sort: 1 },
      {
        fieldCode: 'scale',
        fieldName: '企业规模',
        fieldType: 'select',
        required: true,
        sort: 2,
        options: [
          { label: '小型', value: 's' },
          { label: '大型', value: 'l' },
        ],
      },
      { fieldCode: 'tip', fieldName: '说明', fieldType: 'static', content: '仅展示', sort: 3 },
    ]);
    const existing = await page.request.get('/api/information/forms/page', {
      headers,
      params: { page: '1', size: '1', keyword: 'e2e-dynamic-columns' },
    });
    const existingBody = await existing.json();
    const existingId = existingBody?.data?.list?.[0]?.id;
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

    await page.goto('/#/information/forms');
    await page.getByPlaceholder('名称/编码').first().fill('e2e-dynamic-columns');
    await page.getByRole('button', { name: '查询' }).click();
    await page.getByRole('button', { name: '提交记录' }).first().click();

    await expect(page.locator('[data-page="information.form.submissions"]')).toBeVisible();
    for (const label of ['提交编号', '提交时间', '状态', '来源页面', '公司名称', '企业规模']) {
      await expect(page.getByRole('columnheader', { name: label, exact: true })).toBeVisible();
    }
    // 其它表单的字段不得出现；展示型控件不生成列
    for (const label of ['手机号', '需求描述', '说明']) {
      await expect(page.getByRole('columnheader', { name: label, exact: true })).toHaveCount(0);
    }
    await expect(page.getByRole('cell', { name: '动态列验证公司' }).first()).toBeVisible();
    // 选择型控件展示选项标签而不是存储值
    await expect(page.getByRole('cell', { name: '大型', exact: true }).first()).toBeVisible();
    await shot(page, '09-submission-dynamic-columns.png');
  });

  test('匿名访客可打开托管表单并提交', async ({ browser }) => {
    // 全新会话、无任何登录态，验证分享网址无需登录即可提交
    const context = await browser.newContext();
    const page = await context.newPage();
    await page.goto(`/#/information/hosted?formCode=${formCode}`);
    await expect(page.locator('[data-page="information.hosted"]')).toBeVisible();
    await expect(page).not.toHaveURL(/\/#\/login/);
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
    await context.close();
  });
});
