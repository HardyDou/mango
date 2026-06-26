import { expect, test, type Locator, type Page } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { existsSync, mkdtempSync, readFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { deflateSync } from 'node:zlib';

type StepResult = {
  page: string;
  action: string;
  status: 'PASS' | 'FAIL';
  detail?: string;
};

const results: StepResult[] = [];
const unique = Date.now();
const keyword = `BTN${unique}`;

type UploadAsset = {
  name: string;
  mimeType: string;
  buffer: Buffer;
};

function record(page: string, action: string, status: 'PASS' | 'FAIL', detail?: string) {
  results.push({ page, action, status, detail });
}

function formItem(scope: Locator, label: string) {
  return scope.locator('.el-form-item').filter({
    has: scope.page().locator('.el-form-item__label').filter({ hasText: new RegExp(`^\\*?\\s*${escapeRegExp(label)}$`) }),
  });
}

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function firstDialog(page: Page) {
  return page.locator('.el-dialog:visible').last();
}

function rowByText(page: Page, text: string | RegExp) {
  return page.getByRole('row').filter({ hasText: text });
}

async function waitCmsReady(page: Page) {
  await expect(page.locator('.cms-panel')).toBeVisible({ timeout: 15000 });
  await expect.poll(
    async () => page.locator('.cms-panel .el-loading-mask:visible').count(),
    { timeout: 15000 },
  ).toBe(0);
}

async function closeDropdowns(page: Page) {
  const dropdowns = page.locator('.el-select-dropdown:visible, .el-tree-select__popper:visible');
  if (await dropdowns.count() === 0) {
    return;
  }
  const dialogHeader = page.locator('.el-dialog:visible .el-dialog__header').last();
  if (await dialogHeader.count()) {
    await dialogHeader.click({ position: { x: 12, y: 12 }, force: true });
  } else {
    await page.locator('main h2').first().click({ force: true });
  }
  await expect.poll(
    async () => dropdowns.count(),
    { timeout: 5000 },
  ).toBe(0);
}

async function selectValue(scope: Locator, label: string, option: string | RegExp) {
  const page = scope.page();
  await closeDropdowns(page);
  const select = formItem(scope, label).locator('.el-select, .el-tree-select').first();
  await expect(select, `${label} 下拉不存在`).toBeVisible({ timeout: 10000 });
  await select.scrollIntoViewIfNeeded();
  await select.click({ force: true });
  const dropdown = page.locator('.el-select-dropdown:visible, .el-tree-select__popper:visible').last();
  await expect(dropdown, `${label} 下拉面板未打开`).toBeVisible({ timeout: 10000 });
  const target = dropdown.getByRole('option', { name: option, exact: typeof option === 'string' }).first();
  await expect(target, `${label} 选项不存在: ${String(option)}`).toBeVisible({ timeout: 10000 });
  await target.scrollIntoViewIfNeeded();
  await target.click({ force: true });
  await closeDropdowns(page);
}

async function selectValues(scope: Locator, label: string, options: Array<string | RegExp>) {
  const page = scope.page();
  await closeDropdowns(page);
  const select = formItem(scope, label).locator('.el-select, .el-tree-select').first();
  await expect(select).toBeVisible({ timeout: 10000 });
  await select.click({ force: true });
  const dropdown = page.locator('.el-select-dropdown:visible, .el-tree-select__popper:visible').last();
  await expect(dropdown).toBeVisible({ timeout: 10000 });
  for (const option of options) {
    const target = dropdown.getByRole('option', { name: option, exact: typeof option === 'string' }).first();
    await expect(target).toBeVisible({ timeout: 10000 });
    await target.click({ force: true });
  }
  await closeDropdowns(page);
}

async function checkButton(scope: Locator, label: string, option: string) {
  const button = formItem(scope, label)
    .locator('.el-checkbox-button')
    .filter({ hasText: new RegExp(`^\\s*${escapeRegExp(option)}\\s*$`) })
    .first();
  await expect(button).toBeVisible({ timeout: 10000 });
  if (!(await button.evaluate(element => element.classList.contains('is-checked')))) {
    await button.click();
  }
}

async function fillInput(scope: Locator, label: string, value: string) {
  const input = formItem(scope, label).locator('input').first();
  await expect(input, `${label} 输入框不存在或未显示`).toBeVisible({ timeout: 10000 });
  await input.fill(value);
}

async function fillTextarea(scope: Locator, label: string, value: string) {
  const textarea = formItem(scope, label).locator('textarea').first();
  await expect(textarea, `${label} 文本域不存在或未显示`).toBeVisible({ timeout: 10000 });
  await textarea.fill(value);
}

async function fillNumber(scope: Locator, label: string, value: string) {
  const input = formItem(scope, label).locator('input').first();
  await expect(input, `${label} 数字输入框不存在或未显示`).toBeVisible({ timeout: 10000 });
  await input.fill(value);
}

async function fillRichText(scope: Locator, label: string, value: string) {
  const wrapper = formItem(scope, label).locator('.editor-wrapper').first();
  await expect(wrapper).toBeVisible({ timeout: 10000 });
  const html = normalizeRichTextHtml(value);
  const updated = await wrapper.evaluate((element, content) => {
    const component = (element as HTMLElement & { __vueParentComponent?: { exposed?: { setContent?: (value: string) => void } } }).__vueParentComponent;
    const setContent = component?.exposed?.setContent;
    if (typeof setContent === 'function') {
      setContent(content);
      return true;
    }
    return false;
  }, html);
  expect(updated, `${label} 富文本组件未暴露 setContent 方法`).toBeTruthy();
  await expect.poll(async () => {
    return wrapper.evaluate((element) => {
      const component = (element as HTMLElement & { __vueParentComponent?: { exposed?: { getHtml?: () => string } } }).__vueParentComponent;
      return component?.exposed?.getHtml?.() || '';
    });
  }, { timeout: 10000 }).toBe(html);
}

async function blurRichTextEditors(scope: Locator) {
  const page = scope.page();
  const wrappers = scope.locator('.editor-wrapper');
  const count = await wrappers.count();
  if (count === 0) {
    return;
  }
  for (let index = 0; index < count; index += 1) {
    await wrappers.nth(index).evaluate((element) => {
      const component = (element as HTMLElement & { __vueParentComponent?: { exposed?: { blur?: () => void } } }).__vueParentComponent;
      component?.exposed?.blur?.();
    });
  }
  await page.evaluate(() => {
    window.getSelection()?.removeAllRanges();
    if (document.activeElement instanceof HTMLElement) {
      document.activeElement.blur();
    }
  });
  await page.waitForTimeout(150);
}

async function uploadFile(scope: Locator, label: string, file: UploadAsset) {
  const item = formItem(scope, label);
  const input = item.locator('input[type="file"]').first();
  await expect(input, `${label} 上传控件不存在`).toBeAttached({ timeout: 10000 });
  const uploadResponsePromise = scope.page().waitForResponse(response =>
    response.request().method() === 'POST'
    && new URL(response.url()).pathname.endsWith('/file/files')
    && response.status() === 200,
  );
  await input.setInputFiles({
    name: uniqueAssetFileName(file.name),
    mimeType: file.mimeType,
    buffer: file.buffer,
  });
  const uploadResponse = await uploadResponsePromise;
  const responseBody = await uploadResponse.text().catch(() => '');
  expect(uploadResponse.ok(), `${label} 上传失败: ${uploadResponse.status()} ${responseBody}`).toBeTruthy();
  await expect.poll(async () => {
    const successCount = await item.locator('.el-upload-list__item.is-success').count();
    if (successCount > 0) return 'success';
    const messages = await scope.page().locator('.el-message:visible, .el-form-item__error:visible').allTextContents();
    return messages.join(' | ') || 'waiting';
  }, {
    timeout: 15000,
    message: `${label} 上传后未显示成功状态，接口响应：${responseBody.slice(0, 800)}`,
  }).toBe('success');
  return extractUploadedFileId(responseBody);
}

async function confirmDelete(page: Page) {
  const box = page.locator('.el-message-box').last();
  await expect(box).toBeVisible({ timeout: 10000 });
  await box.getByRole('button', { name: /^(确定|OK)$/ }).click();
  await expect(box).toBeHidden({ timeout: 10000 });
}

async function expectToast(page: Page, text: string | RegExp) {
  await expect(page.locator('.el-message:visible').filter({ hasText: text }).last()).toBeVisible({ timeout: 10000 });
}

async function openCmsPage(page: Page, path: string, title: string) {
  await page.goto(`/#${path}`);
  await page.waitForURL(`**/#${path}`, { timeout: 15000 });
  await expect(page.locator('main')).toContainText(title, { timeout: 15000 });
  await waitCmsReady(page);
}

async function login(page: Page) {
  await page.goto('/#/login');
  await page.getByPlaceholder('用户名').fill('admin');
  await page.getByPlaceholder('密码').fill('admin123');
  const tenants = page.waitForResponse(response => response.url().includes('/api/auth/login-institutions') && response.status() === 200);
  await page.getByPlaceholder('密码').blur();
  await tenants;
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: /芒果集团/ }).click();
  await page.getByRole('button', { name: /^登\s*录$/ }).click();
  await page.waitForURL('**/#/home', { timeout: 15000 });
  await expect(page.locator('main')).toContainText(/工作台|欢迎|首页/, { timeout: 15000 });
}

async function queryAndReset(page: Page, pageName: string, keywordValue: string) {
  await page.getByPlaceholder('名称/编码').fill(keywordValue);
  await page.getByRole('button', { name: '查询' }).click();
  await waitCmsReady(page);
  record(pageName, '查询', 'PASS');
  await page.getByRole('button', { name: '重置' }).click();
  await waitCmsReady(page);
  record(pageName, '重置', 'PASS');
}

async function saveCurrentDialog(page: Page, pageName: string, action: string) {
  const dialog = firstDialog(page);
  if (await dialog.locator('.editor-wrapper').count()) {
    await blurRichTextEditors(dialog);
  }
  const saveResponsePromise = page.waitForResponse(response => {
    const method = response.request().method();
    return response.url().includes('/api/cms/') && (method === 'POST' || method === 'PUT');
  }, { timeout: 15000 });
  await dialog.getByRole('button', { name: '保存' }).click();
  const saveResponse = await saveResponsePromise;
  expect(
    saveResponse.status(),
    `${pageName} ${action} 写请求失败: ${saveResponse.request().method()} ${saveResponse.url()} ${await saveResponse.text().catch(() => '')}`,
  ).toBeLessThan(400);
  await expectToast(page, '保存成功');
  await expect(dialog).toBeHidden({ timeout: 15000 });
  await waitCmsReady(page);
  record(pageName, action, 'PASS');
}

async function createRecord(page: Page, pageName: string, expectedText: string | RegExp, fill: (dialog: Locator) => Promise<void>) {
  await page.getByRole('button', { name: '新增' }).click();
  const dialog = firstDialog(page);
  await expect(dialog).toBeVisible({ timeout: 10000 });
  await fill(dialog);
  await saveCurrentDialog(page, pageName, '新增/保存');
  if (typeof expectedText === 'string' && await rowByText(page, expectedText).count() === 0) {
    await page.getByPlaceholder('名称/编码').fill(expectedText);
    await page.getByRole('button', { name: '查询' }).click();
    await waitCmsReady(page);
  }
  await expect(rowByText(page, expectedText).first()).toBeVisible({ timeout: 10000 });
}

async function editRecord(page: Page, pageName: string, rowText: string | RegExp, fill: (dialog: Locator) => Promise<void>) {
  const row = rowByText(page, rowText).first();
  await expect(row).toBeVisible({ timeout: 10000 });
  await row.getByRole('button', { name: '编辑' }).click();
  const dialog = firstDialog(page);
  await expect(dialog).toBeVisible({ timeout: 10000 });
  await fill(dialog);
  await saveCurrentDialog(page, pageName, '编辑/保存');
}

async function toggleStatus(page: Page, pageName: string, rowText: string | RegExp) {
  const row = rowByText(page, rowText).first();
  await expect(row).toBeVisible({ timeout: 10000 });
  if (await row.getByRole('button', { name: '禁用' }).count()) {
    await row.getByRole('button', { name: '禁用' }).click();
    await expectToast(page, '状态已更新');
    record(pageName, '禁用', 'PASS');
  }
  const nextRow = rowByText(page, rowText).first();
  if (await nextRow.getByRole('button', { name: '启用' }).count()) {
    await nextRow.getByRole('button', { name: '启用' }).click();
    await expectToast(page, '状态已更新');
    record(pageName, '启用', 'PASS');
  }
}

async function deleteRecord(page: Page, pageName: string, rowText: string | RegExp) {
  const row = rowByText(page, rowText).first();
  await expect(row).toBeVisible({ timeout: 10000 });
  const deleteResponsePromise = page.waitForResponse(response =>
    response.url().includes('/api/cms/')
    && response.request().method() === 'DELETE',
  { timeout: 15000 });
  await row.getByRole('button', { name: '删除' }).click();
  await confirmDelete(page);
  const deleteResponse = await deleteResponsePromise;
  expect(
    deleteResponse.status(),
    `${pageName} 删除请求失败: ${deleteResponse.url()} ${await deleteResponse.text().catch(() => '')}`,
  ).toBeLessThan(400);
  await expectToast(page, '删除成功');
  await waitCmsReady(page);
  record(pageName, '删除', 'PASS');
}

function normalizeRichTextHtml(value: string) {
  if (/<[a-z][\s\S]*>/i.test(value)) {
    return value;
  }
  return `<p>${escapeHtml(value)}</p>`;
}

function richTextToPlainText(value: string) {
  return value
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/p>\s*<p>/gi, '\n')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&amp;/g, '&');
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function uniqueAssetFileName(fileName: string) {
  const index = fileName.lastIndexOf('.');
  const suffix = `${unique}-${Math.random().toString(36).slice(2, 8)}`;
  if (index < 0) return `${fileName}-${suffix}`;
  return `${fileName.slice(0, index)}-${suffix}${fileName.slice(index)}`;
}

function extractUploadedFileId(responseBody: string) {
  const body = JSON.parse(responseBody || '{}') as { data?: { id?: string | number }; id?: string | number };
  const id = body.data?.id ?? body.id;
  expect(id, `上传接口未返回文件 ID：${responseBody.slice(0, 500)}`).toBeTruthy();
  return String(id);
}

function createPng(accent: [number, number, number]) {
  const width = 960;
  const height = 540;
  const raw = Buffer.alloc((width * 4 + 1) * height);
  for (let y = 0; y < height; y += 1) {
    const row = y * (width * 4 + 1);
    raw[row] = 0;
    for (let x = 0; x < width; x += 1) {
      const offset = row + 1 + x * 4;
      const shade = Math.round(245 - (y / height) * 30);
      raw[offset] = blend(shade, accent[0], x / width * 0.45);
      raw[offset + 1] = blend(shade, accent[1], x / width * 0.45);
      raw[offset + 2] = blend(255, accent[2], y / height * 0.25);
      raw[offset + 3] = 255;
    }
  }
  drawPanel(raw, width, 80, 88, 430, 42, accent, 255);
  drawPanel(raw, width, 80, 160, 560, 24, [32, 55, 90], 230);
  drawPanel(raw, width, 80, 210, 480, 18, [88, 112, 146], 220);
  drawPanel(raw, width, 650, 110, 220, 220, [255, 255, 255], 235);
  drawPanel(raw, width, 690, 152, 140, 30, accent, 255);
  drawPanel(raw, width, 690, 218, 120, 20, [70, 96, 132], 230);
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    pngChunk('IHDR', Buffer.concat([uint32(width), uint32(height), Buffer.from([8, 6, 0, 0, 0])])),
    pngChunk('IDAT', deflateSync(raw)),
    pngChunk('IEND', Buffer.alloc(0)),
  ]);
}

function blend(base: number, accent: number, ratio: number) {
  return Math.max(0, Math.min(255, Math.round(base * (1 - ratio) + accent * ratio)));
}

function drawPanel(buffer: Buffer, imageWidth: number, x: number, y: number, width: number, height: number, color: [number, number, number], alpha = 255) {
  const stride = imageWidth * 4 + 1;
  const [red, green, blue] = color;
  for (let row = y; row < y + height; row += 1) {
    const rowOffset = row * stride + 1;
    for (let column = x; column < x + width; column += 1) {
      const offset = rowOffset + column * 4;
      buffer[offset] = blend(buffer[offset], red, alpha / 255);
      buffer[offset + 1] = blend(buffer[offset + 1], green, alpha / 255);
      buffer[offset + 2] = blend(buffer[offset + 2], blue, alpha / 255);
      buffer[offset + 3] = 255;
    }
  }
}

function uint32(value: number) {
  const buffer = Buffer.alloc(4);
  buffer.writeUInt32BE(value >>> 0, 0);
  return buffer;
}

function pngChunk(type: string, data: Buffer) {
  const typeBuffer = Buffer.from(type);
  return Buffer.concat([uint32(data.length), typeBuffer, data, uint32(crc32(Buffer.concat([typeBuffer, data])))]);
}

function crc32(buffer: Buffer) {
  let crc = 0xffffffff;
  for (const byte of buffer) {
    crc ^= byte;
    for (let index = 0; index < 8; index += 1) {
      crc = (crc >>> 1) ^ (0xedb88320 & -(crc & 1));
    }
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function createDemoVideoBuffer() {
  const directory = mkdtempSync(join(tmpdir(), 'mango-cms-buttons-video-'));
  const output = join(directory, `cms-buttons-${unique}.mp4`);
  execFileSync('ffmpeg', [
    '-y',
    '-f',
    'lavfi',
    '-i',
    'testsrc2=size=640x360:rate=12:duration=1',
    '-pix_fmt',
    'yuv420p',
    '-movflags',
    'faststart',
    output,
  ], { stdio: 'ignore' });
  expect(existsSync(output), `演示视频生成失败：${output}`).toBeTruthy();
  return readFileSync(output);
}

test.describe('内容运营页面按钮浏览器验收', () => {
  test.setTimeout(1200000);

  test('逐页操作内容运营下所有主要按钮', async ({ page }) => {
    const consoleErrors: string[] = [];
    const failedResponses: string[] = [];
    page.on('console', message => {
      if (message.type() === 'error') {
        const location = message.location();
        consoleErrors.push(`console:${location.url}:${location.lineNumber}:${location.columnNumber} ${message.text()}`);
      }
    });
    page.on('pageerror', error => consoleErrors.push(`pageerror:${error.stack || error.message}`));
    page.on('response', async (response) => {
      if (response.url().includes('/api/') && response.status() >= 400) {
        failedResponses.push(`${response.status()} ${response.request().method()} ${response.url()} ${await response.text().catch(() => '')}`);
      }
    });

    const siteName = `${keyword} 站点`;
    const siteNameUpdated = `${keyword} 站点更新`;
    const siteCode = `site-${keyword.toLowerCase()}`;
    const contentCategoryName = `${keyword} 内容分类`;
    const contentCategoryUpdated = `${keyword} 内容分类更新`;
    const contentCategoryCode = `content-${keyword.toLowerCase()}`;
    const tagName = `${keyword} 标签`;
    const tagUpdated = `${keyword} 标签更新`;
    const tagCode = `tag-${keyword.toLowerCase()}`;
    const siteCategoryName = `${keyword} 栏目`;
    const siteCategoryUpdated = `${keyword} 栏目更新`;
    const contentTitle = `${keyword} 内容`;
    const contentUpdated = `${keyword} 内容更新`;
    const navName = `${keyword} 导航`;
    const navUpdated = `${keyword} 导航更新`;
    const adName = `${keyword} 广告位`;
    const adUpdated = `${keyword} 广告位更新`;
    const adCode = `ad-${keyword.toLowerCase()}`;
    const adDeliveryName = `${keyword} 广告投放`;
    const adDeliveryUpdated = `${keyword} 广告投放更新`;
    const imageAsset: UploadAsset = { name: 'cms-buttons-image.png', mimeType: 'image/png', buffer: createPng([0, 91, 216]) };
    const secondImageAsset: UploadAsset = { name: 'cms-buttons-second.png', mimeType: 'image/png', buffer: createPng([0, 132, 204]) };
    const videoAsset: UploadAsset = { name: 'cms-buttons-video.mp4', mimeType: 'video/mp4', buffer: createDemoVideoBuffer() };

    await login(page);

    await openCmsPage(page, '/cms/sites', '站点管理');
    await queryAndReset(page, '站点列表', keyword);
    await createRecord(page, '站点列表', siteName, async (dialog) => {
      await fillInput(dialog, '站点名称', siteName);
      await fillInput(dialog, '站点编码', siteCode);
      await fillInput(dialog, '域名', `${siteCode}.example.test`);
      await fillInput(dialog, '默认语言', 'zh-CN');
      await selectValue(dialog, '状态', '启用');
      await fillTextarea(dialog, '站点描述', `${keyword} 站点描述`);
      await fillInput(dialog, 'SEO 标题', `${keyword} SEO`);
      await fillInput(dialog, '版权信息', `${keyword} copyright`);
    });
    await editRecord(page, '站点列表', siteName, async dialog => fillInput(dialog, '站点名称', siteNameUpdated));
    await expect(rowByText(page, siteNameUpdated).first()).toBeVisible({ timeout: 10000 });
    await rowByText(page, siteNameUpdated).first().getByRole('button', { name: '详情' }).click();
    await expect(firstDialog(page)).toContainText(`${keyword} SEO`, { timeout: 10000 });
    await firstDialog(page).getByRole('button', { name: '关闭' }).click();
    record('站点列表', '详情/关闭', 'PASS');
    await toggleStatus(page, '站点列表', siteNameUpdated);

    await openCmsPage(page, '/cms/content-categories', '内容分类');
    await queryAndReset(page, '内容分类', keyword);
    await createRecord(page, '内容分类', contentCategoryName, async (dialog) => {
      await fillInput(dialog, '分类名称', contentCategoryName);
      await fillInput(dialog, '分类编码', contentCategoryCode);
      await fillNumber(dialog, '排序', '1');
      await selectValue(dialog, '状态', '启用');
      await fillTextarea(dialog, '备注', `${keyword} 内容分类备注`);
    });
    await editRecord(page, '内容分类', contentCategoryName, async dialog => fillInput(dialog, '分类名称', contentCategoryUpdated));
    await toggleStatus(page, '内容分类', contentCategoryUpdated);

    await openCmsPage(page, '/cms/content-tags', '内容标签');
    await queryAndReset(page, '标签管理', keyword);
    await createRecord(page, '标签管理', tagName, async (dialog) => {
      await fillInput(dialog, '标签名称', tagName);
      await fillInput(dialog, '标签编码', tagCode);
      await fillNumber(dialog, '排序', '1');
      await selectValue(dialog, '状态', '启用');
      await fillTextarea(dialog, '备注', `${keyword} 标签备注`);
    });
    await editRecord(page, '标签管理', tagName, async dialog => fillInput(dialog, '标签名称', tagUpdated));
    await toggleStatus(page, '标签管理', tagUpdated);

    await openCmsPage(page, '/cms/site-categories', '站点栏目');
    await queryAndReset(page, '栏目管理', keyword);
    await createRecord(page, '栏目管理', siteCategoryName, async (dialog) => {
      await selectValue(dialog, '所属站点', siteNameUpdated);
      await fillInput(dialog, '栏目名称', siteCategoryName);
      await fillInput(dialog, '访问路径', `/btn/${keyword.toLowerCase()}`);
      await selectValue(dialog, '显示状态', '启用');
      await fillNumber(dialog, '排序', '1');
    });
    await editRecord(page, '栏目管理', siteCategoryName, async dialog => fillInput(dialog, '栏目名称', siteCategoryUpdated));
    await toggleStatus(page, '栏目管理', siteCategoryUpdated);

    await openCmsPage(page, '/cms/contents', '内容管理');
    await queryAndReset(page, '内容中心', keyword);
    await createRecord(page, '内容中心', contentTitle, async (dialog) => {
      await fillInput(dialog, '标题', contentTitle);
      await fillInput(dialog, '副标题', `${keyword} 副标题`);
      await selectValue(dialog, '内容类型', '文章');
      await selectValue(dialog, '内容分类', contentCategoryUpdated);
      await uploadFile(dialog, '封面图片', imageAsset);
      await fillInput(dialog, '来源', '按钮验收');
      await fillInput(dialog, '作者', 'QA');
      await fillTextarea(dialog, '摘要', `${keyword} 摘要`);
      await fillRichText(dialog, '正文', `<h2>${keyword} 富文本标题</h2><p><strong>${keyword}</strong> 富文本正文</p><ul><li>格式稳定性验证</li><li>列表内容验证</li></ul>`);
    });
    await editRecord(page, '内容中心', contentTitle, async dialog => fillInput(dialog, '标题', contentUpdated));
    await rowByText(page, contentUpdated).first().getByRole('button', { name: '详情' }).click();
    await expect(firstDialog(page)).toContainText(`${keyword} 富文本标题`, { timeout: 10000 });
    await expect(firstDialog(page)).toContainText('格式稳定性验证', { timeout: 10000 });
    await firstDialog(page).getByRole('button', { name: '关闭' }).click();
    record('内容中心', '详情/关闭', 'PASS');
    await rowByText(page, contentUpdated).first().getByRole('button', { name: '提交' }).click();
    await expect(rowByText(page, contentUpdated).first()).toContainText('待审核', { timeout: 10000 });
    record('内容中心', '提交', 'PASS');
    await rowByText(page, contentUpdated).first().getByRole('button', { name: '通过' }).click();
    await expect(rowByText(page, contentUpdated).first()).toContainText('已发布', { timeout: 10000 });
    record('内容中心', '通过', 'PASS');

    await openCmsPage(page, '/cms/content-publishes', '内容发布');
    await queryAndReset(page, '发布管理', keyword);
    await createRecord(page, '发布管理', contentUpdated, async (dialog) => {
      await selectValues(dialog, '发布内容', [contentUpdated]);
      await selectValue(dialog, '所属站点', siteNameUpdated);
      await selectValues(dialog, '发布栏目', [siteCategoryUpdated]);
      await fillNumber(dialog, '排序', '1');
    });
    await rowByText(page, contentUpdated).first().getByRole('button', { name: '下线' }).click();
    await expectToast(page, '发布关系已下线');
    record('发布管理', '下线', 'PASS');

    await openCmsPage(page, '/cms/navigations', '导航管理');
    await queryAndReset(page, '导航管理', keyword);
    await createRecord(page, '导航管理', navName, async (dialog) => {
      await selectValue(dialog, '所属站点', siteNameUpdated);
      await fillInput(dialog, '导航名称', navName);
      await selectValue(dialog, '导航位置', '顶部导航');
      await selectValue(dialog, '导航类型', '栏目');
      await selectValue(dialog, '栏目', siteCategoryUpdated);
      await fillNumber(dialog, '排序', '1');
      await selectValue(dialog, '状态', '启用');
    });
    await editRecord(page, '导航管理', navName, async dialog => fillInput(dialog, '导航名称', navUpdated));
    await toggleStatus(page, '导航管理', navUpdated);

    await openCmsPage(page, '/cms/advertisements', '广告位管理');
    await queryAndReset(page, '广告位管理', keyword);
    await createRecord(page, '广告位管理', adName, async (dialog) => {
      await selectValue(dialog, '所属站点', siteNameUpdated);
      await fillInput(dialog, '广告位名称', adName);
      await fillInput(dialog, '广告位编码', adCode);
      await selectValue(dialog, '位置类型', '自定义');
      await fillInput(dialog, '位置编码', `BTN_${keyword}`);
      await checkButton(dialog, '支持物料', '单图');
      await checkButton(dialog, '支持物料', '多图');
      await checkButton(dialog, '支持物料', '视频');
      await checkButton(dialog, '支持物料', '文本');
      await checkButton(dialog, '支持物料', '富文本');
      await checkButton(dialog, '支持物料', 'HTML');
      await fillNumber(dialog, '排序', '1');
      await selectValue(dialog, '状态', '启用');
      await fillTextarea(dialog, '备注', `${keyword} 广告位备注`);
    });
    await editRecord(page, '广告位管理', adName, async dialog => fillInput(dialog, '广告位名称', adUpdated));
    await toggleStatus(page, '广告位管理', adUpdated);

    await openCmsPage(page, '/cms/ad-deliveries', '广告投放管理');
    await queryAndReset(page, '广告投放管理', keyword);
    await createRecord(page, '广告投放管理', adDeliveryName, async (dialog) => {
      await selectValue(dialog, '所属站点', siteNameUpdated);
      await selectValue(dialog, '广告位', new RegExp(escapeRegExp(adUpdated)));
      await fillInput(dialog, '投放名称', adDeliveryName);
      await selectValue(dialog, '物料类型', '文本');
      await fillInput(dialog, '标题', `${keyword} 投放标题`);
      await fillTextarea(dialog, '文本内容', `${keyword} 广告投放文本`);
      await fillInput(dialog, '跳转链接', 'https://github.com/HardyDou/mango');
      await selectValue(dialog, '打开方式', '新窗口');
      await fillNumber(dialog, '排序', '1');
      await selectValue(dialog, '状态', '启用');
    });
    await editRecord(page, '广告投放管理', adDeliveryName, async dialog => fillInput(dialog, '投放名称', adDeliveryUpdated));
    await toggleStatus(page, '广告投放管理', adDeliveryUpdated);

    const extraDeliveries = [
      `${keyword} 单图投放`,
      `${keyword} 多图投放`,
      `${keyword} 视频投放`,
      `${keyword} 富文本投放`,
      `${keyword} HTML投放`,
    ];

    await createRecord(page, '广告投放管理', extraDeliveries[0], async (dialog) => {
      await selectValue(dialog, '所属站点', siteNameUpdated);
      await selectValue(dialog, '广告位', new RegExp(escapeRegExp(adUpdated)));
      await fillInput(dialog, '投放名称', extraDeliveries[0]);
      await selectValue(dialog, '物料类型', '单图');
      await fillInput(dialog, '标题', `${keyword} 单图标题`);
      await uploadFile(dialog, '图片素材', imageAsset);
      await fillInput(dialog, '跳转链接', 'https://github.com/HardyDou/mango');
      await selectValue(dialog, '打开方式', '新窗口');
      await selectValue(dialog, '状态', '启用');
    });

    await createRecord(page, '广告投放管理', extraDeliveries[1], async (dialog) => {
      await selectValue(dialog, '所属站点', siteNameUpdated);
      await selectValue(dialog, '广告位', new RegExp(escapeRegExp(adUpdated)));
      await fillInput(dialog, '投放名称', extraDeliveries[1]);
      await selectValue(dialog, '物料类型', '多图');
      await fillInput(dialog, '标题', `${keyword} 多图标题`);
      await uploadFile(dialog, '多图素材', imageAsset);
      await uploadFile(dialog, '多图素材', secondImageAsset);
      await fillInput(dialog, '跳转链接', 'https://github.com/HardyDou/mango/tree/main/mango-ui');
      await selectValue(dialog, '打开方式', '新窗口');
      await selectValue(dialog, '状态', '启用');
    });

    await createRecord(page, '广告投放管理', extraDeliveries[2], async (dialog) => {
      await selectValue(dialog, '所属站点', siteNameUpdated);
      await selectValue(dialog, '广告位', new RegExp(escapeRegExp(adUpdated)));
      await fillInput(dialog, '投放名称', extraDeliveries[2]);
      await selectValue(dialog, '物料类型', '视频');
      await fillInput(dialog, '标题', `${keyword} 视频标题`);
      await uploadFile(dialog, '视频素材', videoAsset);
      await uploadFile(dialog, '视频封面', secondImageAsset);
      await fillInput(dialog, '跳转链接', 'https://github.com/HardyDou/mango/tree/main/mango');
      await selectValue(dialog, '打开方式', '新窗口');
      await selectValue(dialog, '状态', '启用');
    });

    await createRecord(page, '广告投放管理', extraDeliveries[3], async (dialog) => {
      await selectValue(dialog, '所属站点', siteNameUpdated);
      await selectValue(dialog, '广告位', new RegExp(escapeRegExp(adUpdated)));
      await fillInput(dialog, '投放名称', extraDeliveries[3]);
      await selectValue(dialog, '物料类型', '富文本');
      await fillInput(dialog, '标题', `${keyword} 富文本标题`);
      await fillRichText(dialog, '富文本内容', `<p><strong>${keyword}</strong> 富文本广告内容</p>`);
      await fillInput(dialog, '跳转链接', 'https://github.com/HardyDou/mango/issues/214');
      await selectValue(dialog, '打开方式', '新窗口');
      await selectValue(dialog, '状态', '启用');
    });

    await createRecord(page, '广告投放管理', extraDeliveries[4], async (dialog) => {
      await selectValue(dialog, '所属站点', siteNameUpdated);
      await selectValue(dialog, '广告位', new RegExp(escapeRegExp(adUpdated)));
      await fillInput(dialog, '投放名称', extraDeliveries[4]);
      await selectValue(dialog, '物料类型', 'HTML');
      await fillInput(dialog, '标题', `${keyword} HTML标题`);
      await fillTextarea(dialog, 'HTML 内容', `<div class="cms-ad-html">${keyword} HTML 广告</div>`);
      await fillInput(dialog, '跳转链接', 'https://github.com/HardyDou/mango');
      await selectValue(dialog, '打开方式', '新窗口');
      await selectValue(dialog, '状态', '启用');
    });

    await openCmsPage(page, '/cms/contents', '内容管理');
    await page.getByPlaceholder('名称/编码').fill(keyword);
    await page.getByRole('button', { name: '查询' }).click();
    await waitCmsReady(page);
    await rowByText(page, contentUpdated).first().getByRole('button', { name: '下线' }).click();
    await expect(rowByText(page, contentUpdated).first()).toContainText('已下线', { timeout: 10000 });
    record('内容中心', '下线', 'PASS');

    await openCmsPage(page, '/cms/ad-deliveries', '广告投放管理');
    await page.getByPlaceholder('名称/编码').fill(keyword);
    await page.getByRole('button', { name: '查询' }).click();
    await waitCmsReady(page);
    for (const delivery of extraDeliveries.reverse()) {
      await deleteRecord(page, '广告投放管理', delivery);
    }
    await deleteRecord(page, '广告投放管理', adDeliveryUpdated);

    await openCmsPage(page, '/cms/advertisements', '广告位管理');
    await page.getByPlaceholder('名称/编码').fill(keyword);
    await page.getByRole('button', { name: '查询' }).click();
    await waitCmsReady(page);
    await deleteRecord(page, '广告位管理', adUpdated);

    await openCmsPage(page, '/cms/navigations', '导航管理');
    await page.getByPlaceholder('名称/编码').fill(keyword);
    await page.getByRole('button', { name: '查询' }).click();
    await waitCmsReady(page);
    await deleteRecord(page, '导航管理', navUpdated);

    await openCmsPage(page, '/cms/content-publishes', '内容发布');
    await page.getByPlaceholder('名称/编码').fill(keyword);
    await page.getByRole('button', { name: '查询' }).click();
    await waitCmsReady(page);
    await deleteRecord(page, '发布管理', contentUpdated);

    await openCmsPage(page, '/cms/contents', '内容管理');
    await page.getByPlaceholder('名称/编码').fill(keyword);
    await page.getByRole('button', { name: '查询' }).click();
    await waitCmsReady(page);
    await deleteRecord(page, '内容中心', contentUpdated);

    await openCmsPage(page, '/cms/site-categories', '站点栏目');
    await page.getByPlaceholder('名称/编码').fill(keyword);
    await page.getByRole('button', { name: '查询' }).click();
    await waitCmsReady(page);
    await deleteRecord(page, '栏目管理', siteCategoryUpdated);

    await openCmsPage(page, '/cms/content-tags', '内容标签');
    await page.getByPlaceholder('名称/编码').fill(keyword);
    await page.getByRole('button', { name: '查询' }).click();
    await waitCmsReady(page);
    await deleteRecord(page, '标签管理', tagUpdated);

    await openCmsPage(page, '/cms/content-categories', '内容分类');
    await page.getByPlaceholder('名称/编码').fill(keyword);
    await page.getByRole('button', { name: '查询' }).click();
    await waitCmsReady(page);
    await deleteRecord(page, '内容分类', contentCategoryUpdated);

    await openCmsPage(page, '/cms/sites', '站点管理');
    await page.getByPlaceholder('名称/编码').fill(keyword);
    await page.getByRole('button', { name: '查询' }).click();
    await waitCmsReady(page);
    await deleteRecord(page, '站点列表', siteNameUpdated);

    const failed = results.filter(item => item.status === 'FAIL');
    console.log(`CMS_BUTTON_RESULTS ${JSON.stringify(results, null, 2)}`);
    expect(failed, `按钮验收失败:\n${JSON.stringify(failed, null, 2)}`).toEqual([]);
    expect(failedResponses, `存在失败请求:\n${failedResponses.join('\n')}`).toEqual([]);
    expect(consoleErrors, `存在控制台错误:\n${consoleErrors.join('\n')}`).toEqual([]);
  });
});
