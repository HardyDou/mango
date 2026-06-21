import { expect, test, type Download, type Locator, type Page } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import * as XLSX from 'xlsx';

type FieldType = '文本' | '数字' | '布尔' | '对象' | '数组' | '日期';

async function login(page: Page) {
  await page.goto('/#/login');
  const loginData = await page.evaluate(async () => {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: 'admin',
        password: 'admin123',
        tenantId: '1',
        tenantCode: 'default',
        realm: 'INTERNAL',
        actorType: 'INTERNAL_USER',
        partyType: 'INTERNAL_ORG',
        appCode: 'internal-admin',
      }),
    });
    const body = await response.json();
    if (!response.ok || !(body.success || body.code === 200) || !body.data?.accessToken) {
      throw new Error(`登录失败：${JSON.stringify(body)}`);
    }
    return body.data;
  });
  await page.evaluate((data) => {
    const userInfo = {
      ...data,
      tenantId: data.tenantId || '1',
      tenantCode: data.tenantCode || 'default',
      tenantName: data.tenantName || '芒果集团',
      realm: data.realm || 'INTERNAL',
      actorType: data.actorType || 'INTERNAL_USER',
      partyType: data.partyType || 'INTERNAL_ORG',
      partyId: data.partyId || '1',
      appCode: data.appCode || 'internal-admin',
    };
    sessionStorage.setItem('MANGO_TOKEN', data.accessToken);
    sessionStorage.setItem('MANGO_REFRESH_TOKEN', data.refreshToken || '');
    sessionStorage.setItem('MANGO_TOKEN_EXPIRES_AT', String(Date.now() + Number(data.expiresIn || 7200) * 1000));
    sessionStorage.setItem('userInfo', JSON.stringify(userInfo));
    sessionStorage.setItem('tenantId', String(userInfo.tenantId));
    document.cookie = `MANGO_TOKEN=${encodeURIComponent(data.accessToken)}; path=/; SameSite=Lax`;
  }, loginData);
  await page.goto('/#/home');
  await expect(page).toHaveURL(/#\/home$/, { timeout: 15000 });
}

async function openTemplateList(page: Page) {
  await page.goto('/#/template/templates');
  await expect(page).toHaveURL(/#\/template\/templates$/);
  const searchInput = page.getByPlaceholder('搜索模板编码或名称');
  if (!(await searchInput.isVisible().catch(() => false))) {
    const backButton = page.getByRole('button', { name: '返回' });
    if (await backButton.isVisible().catch(() => false)) {
      await backButton.click();
    }
  }
  await expect(searchInput).toBeVisible({ timeout: 10000 });
}

async function createTemplateShell(page: Page, code: string, name: string) {
  await openTemplateList(page);
  await page.locator('.action-toolbar').getByRole('button', { name: '新增模板' }).click();
  const form = page.locator('.page-card').filter({ hasText: '模板维护' });
  await expect(form).toBeVisible({ timeout: 10000 });
  await form.getByPlaceholder('如 CONTRACT_NOTICE').fill(code);
  await form.getByPlaceholder('如 合同到期提醒').fill(name);
  await form.getByPlaceholder('记录适用场景、调用方或维护说明').fill('E2E 嵌套参数渲染覆盖');
  return form;
}

async function selectTemplateType(form: Locator, label: RegExp | string) {
  await form.locator('.content-format-bar .el-radio-button').filter({ hasText: label }).click();
}

async function selectDictOption(root: Locator, label: string, option: RegExp | string) {
  await root.locator('.el-form-item', { hasText: label }).locator('.el-select').click();
  await root.page().getByRole('option', { name: option }).last().click();
}

function variableRows(form: Locator) {
  return form.locator('.variable-tree-table tbody tr');
}

async function addRootVariable(form: Locator) {
  const rows = variableRows(form);
  const before = await rows.count();
  await form.getByRole('button', { name: '新增变量' }).click();
  await expect(rows).toHaveCount(before + 1, { timeout: 5000 });
}

async function fillVariableRow(form: Locator, index: number, name: string, type: FieldType = '文本') {
  const rows = variableRows(form);
  const row = rows.nth(index);
  await expect(row).toBeVisible({ timeout: 10000 });
  await row.getByPlaceholder('字段名').fill(name);
  if (type !== '文本') {
    const before = await rows.count();
    await row.locator('.el-select').click();
    await form.page().getByRole('option', { name: type, exact: true }).last().click();
    if (type === '对象' || type === '数组') {
      await expect(rows).toHaveCount(before + 1, { timeout: 5000 });
    }
  }
}

async function addChildVariable(form: Locator, parentRowIndex: number) {
  const rows = variableRows(form);
  const before = await rows.count();
  await rows.nth(parentRowIndex).locator('button[aria-label="添加子字段"]').click();
  await expect(rows).toHaveCount(before + 1, { timeout: 5000 });
}

async function setNestedVariables(form: Locator) {
  await addRootVariable(form);
  await fillVariableRow(form, 0, 'customer', '对象');
  await fillVariableRow(form, 1, 'name');
  await addChildVariable(form, 0);
  await fillVariableRow(form, 2, 'address', '对象');
  await fillVariableRow(form, 3, 'city');

  await addRootVariable(form);
  await fillVariableRow(form, 4, 'items', '数组');
  await fillVariableRow(form, 5, 'name');
  await addChildVariable(form, 4);
  await fillVariableRow(form, 6, 'qty', '数字');

  await addRootVariable(form);
  await fillVariableRow(form, 7, 'groups', '数组');
  await fillVariableRow(form, 8, 'title');
  await addChildVariable(form, 7);
  await fillVariableRow(form, 9, 'children', '数组');
  await fillVariableRow(form, 10, 'name');
}

async function setDocxVariables(form: Locator) {
  await addRootVariable(form);
  await fillVariableRow(form, 0, 'customer', '对象');
  await fillVariableRow(form, 1, 'name');

  await addRootVariable(form);
  await fillVariableRow(form, 2, 'amount', '数字');

  await addRootVariable(form);
  await fillVariableRow(form, 3, 'approved', '布尔');

  await addRootVariable(form);
  await fillVariableRow(form, 4, 'items', '数组');
  await fillVariableRow(form, 5, 'name');
  await addChildVariable(form, 4);
  await fillVariableRow(form, 6, 'count', '数字');
}

async function setCodeMirrorValue(page: Page, root: Locator, value: string) {
  const editor = root.locator('.CodeMirror').first();
  await expect(editor).toBeVisible({ timeout: 10000 });
  await editor.click();
  await page.keyboard.press(process.platform === 'darwin' ? 'Meta+A' : 'Control+A');
  await page.keyboard.insertText(value);
  await expect(editor.locator('.CodeMirror-code')).toContainText(value.split('\n')[0]);
}

async function uploadTemplateFile(form: Locator, format: RegExp | string, filePath: string) {
  await selectTemplateType(form, format);
  const uploadResponsePromise = form.page().waitForResponse((response) =>
    response.url().includes('/api/file/files')
    && response.request().method() === 'POST'
    && response.status() === 200
  );
  await form.locator('.document-upload-area input[type="file"]').setInputFiles(filePath);
  const uploadResponse = await uploadResponsePromise;
  const body = await uploadResponse.json();
  expect(body.success || body.code === 200, JSON.stringify(body)).toBeTruthy();
  await expect(form.locator('.document-preview-card').first()).toBeVisible({ timeout: 10000 });
}

async function publishFromMaintain(page: Page, form: Locator, code: string) {
  const publishResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/template/templates/versions')
    && response.request().method() === 'POST'
    && response.status() === 200
  );
  await form.getByRole('button', { name: '发布', exact: true }).click();
  const publishResponse = await publishResponsePromise;
  const body = await publishResponse.json();
  expect(body.success || body.code === 200, JSON.stringify(body)).toBeTruthy();
  await expect(page.getByText('发布成功')).toBeVisible({ timeout: 10000 });
  await expect(page.getByRole('row', { name: new RegExp(code) })).toContainText('已同步', { timeout: 10000 });
}

async function openPreview(page: Page, code: string) {
  await openTemplateList(page);
  await page.getByPlaceholder('搜索模板编码或名称').fill(code);
  await page.getByRole('button', { name: '查询' }).click();
  const row = page.getByRole('row', { name: new RegExp(code) });
  await expect(row).toBeVisible({ timeout: 10000 });
  await row.getByRole('button', { name: '预览' }).click();
  const preview = page.locator('.page-card').filter({ hasText: '模板预览' });
  await expect(preview).toBeVisible({ timeout: 10000 });
  await expect(preview.getByText('参数输入')).toBeVisible();
  return preview;
}

async function renderWithJson(page: Page, preview: Locator, code: string, outputFormat: RegExp | string, variables: unknown) {
  await selectDictOption(preview, '输出格式', outputFormat);
  await preview.getByRole('tab', { name: 'JSON 输入' }).click();
  await preview.locator('.render-tabs textarea').last().fill(JSON.stringify(variables, null, 2));
  const renderResponsePromise = page.waitForResponse((response) => {
    const postData = response.request().postData() || '';
    return response.url().includes('/api/template/templates/render')
      && response.request().method() === 'POST'
      && postData.includes(code);
  });
  await preview.getByRole('button', { name: '生成预览' }).click();
  const renderResponse = await renderResponsePromise;
  return renderResponse.json();
}

async function cleanupTemplateViaUi(page: Page, code: string) {
  await openTemplateList(page);
  await page.getByPlaceholder('搜索模板编码或名称').fill(code);
  await page.getByRole('button', { name: '查询' }).click();
  const row = page.getByRole('row', { name: new RegExp(code) }).first();
  if (!(await row.isVisible().catch(() => false))) return;
  await row.getByRole('button', { name: '删除' }).click();
  await page.locator('.el-message-box').getByRole('button', { name: /确定|OK/ }).click();
  await expect(page.getByText('模板已删除')).toBeVisible({ timeout: 10000 });
}

async function downloadRenderedFileFromFileCenter(page: Page, fileNamePart: string) {
  await page.goto('/#/file/files');
  await expect(page.getByPlaceholder('搜索文件名/业务信息')).toBeVisible({ timeout: 10000 });
  await page.getByPlaceholder('搜索文件名/业务信息').fill(fileNamePart);
  await page.getByRole('button', { name: '查询' }).click();
  const row = page.getByRole('row', { name: new RegExp(fileNamePart) }).first();
  await expect(row).toBeVisible({ timeout: 10000 });
  const downloadPromise = page.waitForEvent('download');
  await row.getByRole('button', { name: '下载' }).click();
  return downloadPromise;
}

async function saveDownload(download: Download, suffix: string) {
  const path = join(tmpdir(), `mango-template-rendered-${Date.now()}-${Math.random().toString(36).slice(2)}.${suffix}`);
  await download.saveAs(path);
  return path;
}

function createDocxFixture(unique: string) {
  const output = join(tmpdir(), `mango-template-${unique}.docx`);
  writeFileSync(output, Buffer.from(DOCX_TEMPLATE_BASE64, 'base64'));
  return output;
}

function createXlsxFixture(unique: string) {
  const output = join(tmpdir(), `mango-template-${unique}.xlsx`);
  const worksheet = XLSX.utils.aoa_to_sheet([
    ['客户', '{{customer.name}}'],
    ['城市', '{{customer.address.city}}'],
    ['数组直出', '{{items}}'],
  ]);
  const workbook = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(workbook, worksheet, 'Sheet1');
  XLSX.writeFile(workbook, output);
  return output;
}

function unzipText(path: string, entry: string) {
  return execFileSync('unzip', ['-p', path, entry], { encoding: 'utf-8' });
}

const nestedVariables = {
  customer: { name: '张三', address: { city: '上海' } },
  items: [
    { name: '身份证', qty: 1 },
    { name: '营业执照', qty: 2 },
  ],
  groups: [
    { title: 'A组', children: [{ name: '甲' }, { name: '乙' }] },
  ],
};

const DOCX_TEMPLATE_BASE64 = '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';

test.describe.serial('模板中心嵌套参数浏览器 E2E', () => {
  test('通过页面操作验证 TEXT、Word、Excel 与 PDF 输出边界', async ({ page }) => {
    test.setTimeout(240000);
    const unique = `${Date.now()}`;
    const textCode = `E2E_TEXT_${unique}`;
    const docxCode = `E2E_DOCX_${unique}`;
    const xlsxCode = `E2E_XLSX_${unique}`;
    const docxFixture = createDocxFixture(unique);
    const xlsxFixture = createXlsxFixture(unique);

    await login(page);

    try {
      const textForm = await createTemplateShell(page, textCode, `E2E 文本嵌套 ${unique}`);
      await setCodeMirrorValue(page, textForm, [
        '客户：${customer.name}',
        '城市：${customer.address.city}',
        '清单：<#list items as item>${item.name}:${item.qty};</#list>',
        '分组：<#list groups as group>${group.title}-<#list group.children as child>${child.name},</#list></#list>',
      ].join('\n'));
      await setNestedVariables(textForm);
      await publishFromMaintain(page, textForm, textCode);

      const docxForm = await createTemplateShell(page, docxCode, `E2E Word 嵌套 ${unique}`);
      await uploadTemplateFile(docxForm, /Word DOCX|DOCX/, docxFixture);
      await setDocxVariables(docxForm);
      await publishFromMaintain(page, docxForm, docxCode);

      const xlsxForm = await createTemplateShell(page, xlsxCode, `E2E Excel 嵌套 ${unique}`);
      await uploadTemplateFile(xlsxForm, /Excel XLSX|XLSX/, xlsxFixture);
      await setNestedVariables(xlsxForm);
      await publishFromMaintain(page, xlsxForm, xlsxCode);

      const textPreview = await openPreview(page, textCode);
      const textRenderBody = await renderWithJson(page, textPreview, textCode, '纯文本', nestedVariables);
      expect(textRenderBody.success || textRenderBody.code === 200, JSON.stringify(textRenderBody)).toBeTruthy();
      await expect(page.getByText('渲染完成').first()).toBeVisible({ timeout: 10000 });
      await expect(textPreview.locator('.preview-result')).toContainText('客户：张三');
      await expect(textPreview.locator('.preview-result')).toContainText('城市：上海');
      await expect(textPreview.locator('.preview-result')).toContainText('清单：身份证:1;营业执照:2;');
      await expect(textPreview.locator('.preview-result')).toContainText('分组：A组-甲,乙,');

      const pdfBody = await renderWithJson(page, textPreview, textCode, 'PDF', nestedVariables);
      expect(pdfBody.success || pdfBody.code === 200, JSON.stringify(pdfBody)).toBeFalsy();
      await expect(textPreview.locator('.preview-result')).toContainText(
        pdfBody.msg || pdfBody.message || /不支持|转换/,
        { timeout: 10000 },
      );

      const docxPreview = await openPreview(page, docxCode);
      const docxBody = await renderWithJson(page, docxPreview, docxCode, /Word DOCX|DOCX/, {
        customer: { name: '张三' },
        amount: 128,
        approved: true,
        items: [
          { name: '身份证', count: 1 },
          { name: '营业执照', count: 2 },
        ],
      });
      expect(docxBody.success || docxBody.code === 200, JSON.stringify(docxBody)).toBeTruthy();
      await expect(page.getByText('渲染完成').first()).toBeVisible({ timeout: 10000 });
      await expect(docxPreview.getByText(/文件ID：/)).toBeVisible({ timeout: 10000 });
      const docxDownload = await downloadRenderedFileFromFileCenter(page, `mango-template-${unique}-rendered.docx`);
      const renderedDocxPath = await saveDownload(docxDownload, 'docx');
      const docxXml = unzipText(renderedDocxPath, 'word/document.xml');
      expect(docxXml).toContain('张三');
      expect(docxXml).toContain('128');
      expect(docxXml).toContain('true');
      expect(docxXml).toContain('身份证');
      expect(docxXml).toContain('营业执照');
      expect(docxXml).not.toContain('{{items}}');

      const xlsxPreview = await openPreview(page, xlsxCode);
      const xlsxBody = await renderWithJson(page, xlsxPreview, xlsxCode, /Excel XLSX|XLSX/, nestedVariables);
      expect(xlsxBody.success || xlsxBody.code === 200, JSON.stringify(xlsxBody)).toBeTruthy();
      await expect(page.getByText('渲染完成').first()).toBeVisible({ timeout: 10000 });
      await expect(xlsxPreview.getByText(/文件ID：/)).toBeVisible({ timeout: 10000 });
      const xlsxDownload = await downloadRenderedFileFromFileCenter(page, `mango-template-${unique}-rendered.xlsx`);
      const renderedXlsxPath = await saveDownload(xlsxDownload, 'xlsx');
      const renderedWorkbook = XLSX.readFile(renderedXlsxPath);
      const renderedRows = XLSX.utils.sheet_to_json<string[]>(renderedWorkbook.Sheets.Sheet1, { header: 1 });
      expect(renderedRows).toEqual(expect.arrayContaining([
        ['客户', '张三'],
        ['城市', '上海'],
      ]));
      expect(String(renderedRows[2]?.[1] || '')).toContain('身份证');
      expect(String(renderedRows[2]?.[1] || '')).toContain('营业执照');
    } finally {
      await cleanupTemplateViaUi(page, textCode).catch(() => undefined);
      await cleanupTemplateViaUi(page, docxCode).catch(() => undefined);
      await cleanupTemplateViaUi(page, xlsxCode).catch(() => undefined);
    }
  });
});
