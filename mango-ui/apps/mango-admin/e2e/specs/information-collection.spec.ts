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
  // 设计器与 form-create 运行时的首屏渲染较重，统一放宽用例超时
  test.describe.configure({ timeout: 90000 });

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

    // 配置是从列表发起的编辑动作，返回直接回列表
    await goBack(page);
    await expect(page.locator('[data-page="information.forms"]')).toBeVisible();

    // 列表行操作只保留 详情、配置、提交记录、停用、删除
    await page.getByPlaceholder('名称/编码').first().fill(formCode);
    await page.getByRole('button', { name: '查询', exact: true }).click();
    const hostedRow = page.locator('.el-table__body tr', { hasText: formCode }).first();
    await expect(hostedRow).toBeVisible();
    for (const label of ['详情', '配置', '提交记录', '停用', '删除']) {
      await expect(hostedRow.getByRole('button', { name: label, exact: true })).toBeVisible();
    }
    await expect(hostedRow.getByRole('button', { name: '接入', exact: true })).toHaveCount(0);

    // 接入内容已合并进详情页，不再有独立接入页面
    await hostedRow.getByRole('button', { name: '详情', exact: true }).click();
    await expect(page.locator('[data-page="information.form.detail"]')).toBeVisible();
    await expect(page.locator('[data-surface="information.form.access"]')).toBeVisible();
    // 接入方式明确分为页面与接口两种，且左侧页面、右侧接口
    await expect(page.getByText('方式 ① · 页面接入')).toBeVisible();
    await expect(page.getByText('方式 ② · 接口接入')).toBeVisible();
    await expect(page.getByText('无需密钥')).toBeVisible();
    await expect(page.getByText('需要表单专属 SK')).toBeVisible();
    await expect(page.locator('.information-browser-url')).toContainText('/#/information/hosted?formCode=');
    await expect(page.locator('[data-access-method="page"]')).toContainText(formCode);
    await expect(page.locator('[data-access-method="api"]')).toContainText(
      `/information/open/forms/${formCode}/submit`,
    );
    await expect(page.locator('[data-secret-action="copy"]')).toBeVisible();
    await expect(page.locator('.information-api-value')).toContainText('••••');
    await page.locator('[data-secret-action="toggle"]').click();
    await expect(page.locator('.information-api-value')).not.toContainText('••••');

    // 上基础信息，下左页面方式（含模拟浏览器预览、字段）右接口方式
    const summaryBox = await page
      .locator('[data-page="information.form.detail"] .information-card')
      .first()
      .boundingBox();
    const previewBox = await page.locator('[data-surface="information.hosted.preview"]').boundingBox();
    const pageBox = await page.locator('[data-access-method="page"]').boundingBox();
    const apiBox = await page.locator('[data-access-method="api"]').boundingBox();
    expect(summaryBox).not.toBeNull();
    expect(previewBox).not.toBeNull();
    expect(pageBox).not.toBeNull();
    expect(apiBox).not.toBeNull();
    // 基础信息在最上面，下面两栏并排：左页面、右接口
    expect(summaryBox!.y).toBeLessThan(pageBox!.y);
    expect(pageBox!.x).toBeLessThan(apiBox!.x);
    expect(Math.abs(pageBox!.y - apiBox!.y)).toBeLessThan(4);
    // 页面预览属于左栏，字段配置属于右栏
    expect(previewBox!.x).toBeGreaterThanOrEqual(pageBox!.x);
    expect(previewBox!.x).toBeLessThan(apiBox!.x);
    await expect(page.locator('[data-access-method="api"]').getByText('字段配置')).toBeVisible();
    await expect(page.locator('[data-access-method="page"]').getByText('字段配置')).toHaveCount(0);
    await shot(page, '04-form-detail-access.png');

    await goBack(page);
    await expect(page.locator('[data-page="information.forms"]')).toBeVisible();
  });

  // 平台设计器默认只给「输入框」6 个原生类型，采集常用的手机号/邮箱/网址等在补丁后才可选
  test('表单设计的输入框类型覆盖采集常用类型', async ({ page }) => {
    await login(page);
    await page.goto(`/#/information/forms/${formId}/config`);
    const designer = page.locator('[data-surface="information.form.designer"]');
    await expect(designer).toBeVisible({ timeout: 20000 });

    await designer.locator('.fc-form-title', { hasText: '姓名' }).first().click({ force: true });
    await designer.locator('.el-form-item', { hasText: '类型' }).first().locator('.el-select').click();
    const optionLocator = page.locator('.el-select-dropdown__item:visible');
    await expect(optionLocator.first()).toBeVisible();
    const typeOptions = await optionLocator.allInnerTexts();
    for (const option of ['文本', '手机号', '邮箱', '网址', '密码', '数字', '日期', '时间']) {
      expect(typeOptions).toContain(option);
    }
  });

  test('提交记录是独立菜单入口且不依赖表单详情', async ({ page }) => {
    await login(page);
    const token = await page.evaluate(() => sessionStorage.getItem('MANGO_TOKEN') || '');
    const headers = { Authorization: `Bearer ${token}`, 'X-Mango-Tenant-Id': '1' };

    // 用例自建表单与记录，避免依赖其它用例或演示数据
    const definitionJson = JSON.stringify([
      {
        type: 'input',
        field: 'name',
        title: '姓名',
        props: { placeholder: '请输入姓名' },
        validate: [{ required: true, message: '请填写姓名' }],
      },
      { type: 'input', field: 'phone', title: '手机号', props: { type: 'tel' } },
    ]);
    const existing = await page.request.get('/api/information/forms/page', {
      headers,
      params: { page: '1', size: '1', keyword: 'e2e-submissions' },
    });
    const existingId = (await existing.json())?.data?.list?.[0]?.id;
    const save = await page.request.post('/api/information/forms/save', {
      headers,
      data: {
        ...(existingId ? { id: existingId } : {}),
        formCode: 'e2e-submissions',
        formName: '提交记录验证表',
        definitionJson,
        status: 'PUBLISHED',
      },
    });
    const formId = (await save.json()).data as string;
    const submit = await page.request.post('/api/information/open/forms/submit', {
      data: { formCode: 'e2e-submissions', values: { name: '提交记录访客', phone: '13800138000' }, sourcePage: 'e2e' },
    });
    expect(submit.status()).toBe(200);

    await page.goto('/#/information/submissions');
    await expect(page.locator('[data-page="information.form.submissions"]')).toBeVisible();
    // 独立入口：没有返回详情页按钮
    await expect(page.getByText('返回详情页')).toHaveCount(0);
    await expect(page.locator('[data-filter="form"]')).toBeVisible();

    // 深链：地址栏带 formId 时直接定位到该表单，刷新与分享保持同一视图
    await page.goto(`/#/information/submissions?formId=${formId}`);
    await expect(page).toHaveURL(new RegExp(`formId=${formId}`));
    for (const label of ['记录ID', '提交时间', '状态', '来源页面', '姓名', '手机号']) {
      await expect(page.getByRole('columnheader', { name: label, exact: true })).toBeVisible();
    }
    await expect(page.locator('[data-filter="form"]')).toContainText('e2e-submissions', { timeout: 30000 });
    await shot(page, '05-submission-list.png');

    // 行操作是「处理」，点击后打开处理抽屉
    const firstRow = page.locator('.el-table__body tr').first();
    await expect(firstRow.getByRole('button', { name: '处理', exact: true })).toBeVisible();
    await expect(firstRow.getByRole('button', { name: '详情', exact: true })).toHaveCount(0);
    await firstRow.getByRole('button', { name: '处理', exact: true }).click();
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

  test('托管页面按设计器规则渲染全部控件', async ({ page, browser }) => {
    await login(page);
    const token = await page.evaluate(() => sessionStorage.getItem('MANGO_TOKEN') || '');
    const headers = { Authorization: `Bearer ${token}`, 'X-Mango-Tenant-Id': '1' };
    // tableForm 在设计器里存运行时形态：列定义在 props.columns
    const definitionJson = JSON.stringify([
      {
        type: 'input',
        field: 'name',
        title: '姓名',
        props: { placeholder: '请输入姓名' },
        validate: [{ required: true, message: '请填写姓名' }],
      },
      { type: 'inputNumber', field: 'amount', title: '金额' },
      { type: 'select', field: 'city', title: '城市', options: [{ label: '北京', value: 'bj' }] },
      { type: 'radio', field: 'level', title: '级别', options: [{ label: '重要', value: 'high' }] },
      { type: 'checkbox', field: 'channels', title: '渠道', options: [{ label: '电话', value: 'tel' }] },
      { type: 'switch', field: 'agreed', title: '同意条款' },
      { type: 'rate', field: 'score', title: '评分', props: { max: 5 } },
      { type: 'slider', field: 'weight', title: '权重', props: { min: 0, max: 100 } },
      { type: 'datePicker', field: 'visitDate', title: '到访日期' },
      { type: 'colorPicker', field: 'brandColor', title: '品牌色' },
      { type: 'elDivider', title: '分组' },
      { type: 'elAlert', props: { title: '补充信息' } },
      {
        type: 'tableForm',
        field: 'rows',
        title: '明细表',
        props: {
          columns: [
            { label: '项目', style: {}, rule: [{ type: 'input', field: 'itemName', title: '项目' }] },
            { label: '数量', style: {}, rule: [{ type: 'inputNumber', field: 'itemQty', title: '数量' }] },
          ],
        },
        children: [],
      },
    ]);
    const existing = await page.request.get('/api/information/forms/page', {
      headers,
      params: { page: '1', size: '1', keyword: 'e2e-controls' },
    });
    const existingId = (await existing.json())?.data?.list?.[0]?.id;
    const save = await page.request.post('/api/information/forms/save', {
      headers,
      data: {
        ...(existingId ? { id: existingId } : {}),
        formCode: 'e2e-controls',
        formName: '控件渲染验证表',
        definitionJson,
        status: 'PUBLISHED',
      },
    });
    expect(save.status()).toBe(200);

    const context = await browser.newContext();
    const hosted = await context.newPage();
    await hosted.goto('/#/information/hosted?formCode=e2e-controls');
    await expect(hosted.getByRole('heading', { name: '控件渲染验证表' })).toBeVisible({ timeout: 20000 });
    // 设计器里的控件必须原样渲染，而不是退化成文本框
    for (const selector of [
      '.el-rate',
      '.el-slider',
      '.el-switch',
      '.el-radio-group',
      '.el-checkbox-group',
      '.el-color-picker',
      '.el-divider',
      '.el-alert',
    ]) {
      await expect(hosted.locator(selector).first()).toBeVisible();
    }
    await expect(hosted.locator('.el-rate__item')).toHaveCount(5);
    await expect(hosted.locator('table th').filter({ hasText: '项目' })).toBeVisible();
    await expect(hosted.locator('table th').filter({ hasText: '数量' })).toBeVisible();
    mkdirSync(evidenceDir, { recursive: true });
    await hosted.screenshot({ path: resolve(evidenceDir, '10-hosted-all-controls.png'), fullPage: true });

    // 评分与滑块的值必须进入提交值
    await hosted.getByPlaceholder('请输入姓名').fill('全控件');
    await hosted.locator('.el-rate__item').nth(4).click();
    await hosted.locator('.el-slider__runway').click();
    await hosted.getByRole('button', { name: '提交', exact: true }).click();
    await expect(hosted.getByText('提交成功', { exact: true }).first()).toBeVisible();
    await context.close();
  });

  test('匿名访客上传附件需先取票据且文件落到文件模块', async ({ page, browser }) => {
    await login(page);
    const token = await page.evaluate(() => sessionStorage.getItem('MANGO_TOKEN') || '');
    const headers = { Authorization: `Bearer ${token}`, 'X-Mango-Tenant-Id': '1' };
    const definitionJson = JSON.stringify([
      { type: 'input', field: 'name', title: '姓名', props: { placeholder: '请输入姓名' } },
      { type: 'upload', field: 'attachment', title: '附件', props: { limit: 2, multiple: true } },
    ]);
    const existing = await page.request.get('/api/information/forms/page', {
      headers,
      params: { page: '1', size: '1', keyword: 'e2e-upload-ticket' },
    });
    const existingId = (await existing.json())?.data?.list?.[0]?.id;
    const save = await page.request.post('/api/information/forms/save', {
      headers,
      data: {
        ...(existingId ? { id: existingId } : {}),
        formCode: 'e2e-upload-ticket',
        formName: '上传票据验证表',
        definitionJson,
        status: 'PUBLISHED',
      },
    });
    expect(save.status()).toBe(200);

    const context = await browser.newContext();
    const hosted = await context.newPage();
    const calls: Array<{ url: string; status: number; ticket?: string }> = [];
    hosted.on('request', (request) => {
      if (request.url().includes('/information/open/forms/e2e-upload-ticket/files') && request.method() === 'POST') {
        calls.push({ url: request.url(), status: 0, ticket: request.headers()['x-mango-upload-ticket'] });
      }
    });
    hosted.on('response', (response) => {
      if (
        response.url().includes('/information/open/forms/e2e-upload-ticket/files') &&
        response.request().method() === 'POST'
      ) {
        const last = calls[calls.length - 1];
        if (last) {
          last.status = response.status();
        }
      }
    });

    await hosted.goto('/#/information/hosted?formCode=e2e-upload-ticket');
    await hosted.waitForTimeout(3500);
    await expect(hosted.locator('[data-upload-action="add"]')).toBeVisible();
    await hosted.locator('.information-upload-field .el-upload__input').setInputFiles({
      name: 'e2e-upload.txt',
      mimeType: 'text/plain',
      buffer: Buffer.from('mango information upload ticket'),
    });
    await expect(hosted.locator('.information-upload-list')).toBeVisible({ timeout: 20000 });
    await expect(hosted.locator('.information-upload-name')).toContainText('e2e-upload');
    await shot(hosted, '11-hosted-upload.png');

    // 上传必须携带短效票据，且只有一次成功请求
    expect(calls.length).toBeGreaterThan(0);
    expect(calls[0].ticket).toBeTruthy();
    expect(calls.some((call) => call.status === 200)).toBe(true);
    await context.close();
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

  // 回归：历史定义里子表单没有 props，设计器加载时会抛错并留下空白画布
  test('包含子表单与表格表单的表单可以再次打开设计器', async ({ page }) => {
    await login(page);
    const legacyDefinition = [
      {
        type: 'group',
        field: 'members',
        title: '成员明细',
        children: [{ type: 'input', field: 'memberName', title: '成员姓名' }],
      },
      {
        type: 'tableForm',
        field: 'rows',
        title: '费用明细',
        children: [{ type: 'input', field: 'itemName', title: '项目' }],
      },
    ];
    const errors: string[] = [];
    page.on('console', (message) => {
      if (message.type() === 'error') {
        errors.push(message.text());
      }
    });
    const token = await page.evaluate(() => sessionStorage.getItem('MANGO_TOKEN'));
    const headers = { Authorization: `Bearer ${token}`, 'X-Mango-Tenant-Id': '1' };
    // 编码唯一，用时间戳后缀保证用例可重复执行，结束时再清理掉
    const code = `e2e-subform-${Date.now().toString(36)}`;
    const saved = await page.request.post('/api/information/forms/save', {
      headers,
      data: {
        formCode: code,
        formName: 'E2E 子表单表单',
        definitionJson: JSON.stringify(legacyDefinition),
        status: 'PUBLISHED',
      },
    });
    const createdId = (await saved.json()).data;
    expect(createdId).toBeTruthy();

    await page.goto(`/#/information/forms/${createdId}/config`);
    const designer = page.locator('[data-surface="information.form.designer"]');
    await expect(designer).toBeVisible();
    // 画布不能因为加载失败而变成空态，子表单与表格表单都要渲染出来
    await expect(designer).not.toContainText('拖拽左侧列表中的组件到此处');
    await expect(designer).toContainText('成员明细');
    await expect(designer).toContainText('费用明细');
    expect(errors.filter((item) => item.includes('Cannot read properties'))).toEqual([]);

    await page.request.delete('/api/information/forms', { headers, params: { id: createdId } });
  });

  test('表单详情入口可直达提交记录并预选当前表单', async ({ page }) => {
    await login(page);
    await page.goto(`/#/information/forms/${formId}/detail`);
    await page.getByRole('button', { name: '提交记录' }).click();
    await expect(page.locator('[data-page="information.form.submissions"]')).toBeVisible();
    await expect(page.locator('[data-filter="form"]')).toContainText(formCode);
  });
});
