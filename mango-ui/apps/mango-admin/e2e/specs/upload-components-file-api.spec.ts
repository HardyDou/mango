import { expect, test } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { tmpdir } from 'node:os';
import * as XLSX from 'xlsx';

let uploadedFileIds: string[] = [];

async function login(page: import('@playwright/test').Page) {
  await page.goto('/#/login');
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  await page.click('button:has-text("登 录")');
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

async function apiHeaders(page: import('@playwright/test').Page) {
  return page.evaluate(() => {
    const token = sessionStorage.getItem('MANGO_TOKEN') || '';
    const userInfo = JSON.parse(sessionStorage.getItem('userInfo') || '{}');
    const tenantId = String(userInfo?.tenantId || '1');
    return {
      Authorization: token ? `Bearer ${token}` : '',
      'TENANT-ID': tenantId,
      'X-Mango-Tenant-Id': tenantId,
    };
  });
}

function cleanupUploadComponentFiles() {
  execFileSync('mysql', ['-uroot', 'mango', '-e',
    "DELETE FROM file_record WHERE file_name LIKE 'mango-upload-%' OR biz_id = 'DEMO-20260517'",
  ]);
}

test.describe.serial('上传组件文件接口联调', () => {
  test.beforeEach(async ({ page }) => {
    await page.route('**/api/file/files/batch', async (route) => {
      await new Promise(resolve => setTimeout(resolve, 300));
      await route.continue();
    });
  });

  test.beforeAll(() => {
    cleanupUploadComponentFiles();
  });

  test.beforeEach(async ({ page }) => {
    uploadedFileIds = [];
    await login(page);

    await page.goto('/#/components/upload');
    await expect(page.getByTestId('mixed-upload-panel')).toBeVisible({ timeout: 10000 });
  });

  test.afterEach(async ({ page }) => {
    const headers = await apiHeaders(page);
    for (const id of uploadedFileIds) {
      await page.request.delete(`/api/file/files?id=${encodeURIComponent(id)}&reason=e2e-cleanup`, { headers });
    }
    cleanupUploadComponentFiles();
    uploadedFileIds = [];
  });

  test('统一 Upload 混合附件使用 /file/files 真实接口', async ({ page }) => {
    const filePath = join(tmpdir(), `mango-upload-component-${Date.now()}.txt`);
    writeFileSync(filePath, 'mango upload component e2e', 'utf-8');

    const uploadResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/file/files')
      && response.request().method() === 'POST'
      && response.status() === 200
    );

    await page.getByTestId('mixed-upload-panel')
      .locator('input[type="file"]')
      .setInputFiles(filePath);

    const uploadResponse = await uploadResponsePromise;
    const body = await uploadResponse.json();
    expect(body.success || body.code === 200).toBeTruthy();
    if (body.data?.id) uploadedFileIds.push(String(body.data.id));
    expect(body.data?.fileName).toContain('mango-upload-component');
    await expect(page.getByTestId('mixed-upload-panel')
      .locator('input[readonly]')
      .first()).toHaveValue(/mango-file:/, { timeout: 10000 });
    await expect(page.locator('.el-message--error')).toHaveCount(0);
  });

  test('右侧文章目录点击只滚动页面不改变系统 hash 路由', async ({ page }) => {
    await expect(page).toHaveURL(/#\/components\/upload/);

    await page.getByRole('button', { name: '支持属性' }).click();

    await expect(page).toHaveURL(/#\/components\/upload/);
    await expect(page.locator('#props')).toBeInViewport({ timeout: 10000 });
  });

  test('服务端上传校验错误返回真实业务码而不是文件读取失败', async ({ page }) => {
    const headers = await apiHeaders(page);
    const response = await page.request.post('/api/file/files', {
      headers,
      multipart: {
        file: {
          name: 'mango-upload-blocked.exe',
          mimeType: 'application/x-msdownload',
          buffer: Buffer.from('blocked extension e2e'),
        },
        purpose: 'attachment',
        bizType: 'FILE_CENTER_DEMO',
        bizId: 'DEMO-20260517',
      },
    });
    const body = await response.json();

    expect(response.status()).toBe(200);
    expect(body.success).toBeFalsy();
    expect(body.code).toBe(3407);
    expect(body.msg).toBe('该文件类型禁止上传');
    expect(body.msg).not.toBe('文件读取失败');
  });

  test('统一 Upload 图片场景使用 /file/files 真实接口并返回文件标识', async ({ page }) => {
    const imagePath = join(tmpdir(), `mango-upload-image-${Date.now()}.png`);
    writeFileSync(imagePath, Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=',
      'base64',
    ));

    const uploadResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/file/files')
      && response.request().method() === 'POST'
      && response.status() === 200
    );

    await page.getByTestId('image-upload-panel')
      .locator('input[type="file"]')
      .setInputFiles(imagePath);

    const uploadResponse = await uploadResponsePromise;
    const body = await uploadResponse.json();
    expect(body.success || body.code === 200).toBeTruthy();
    if (body.data?.id) uploadedFileIds.push(String(body.data.id));
    expect(body.data?.fileName).toContain('mango-upload-image');
    expect(body.data?.directPreviewUrl || body.data?.url).toContain('http://file.mango.io:9000/');
    expect(body.data?.directDownloadUrl || body.data?.downloadUrl).toContain('http://file.mango.io:9000/');
    await expect(page.getByTestId('image-upload-panel')
      .locator('input[readonly]')
      .first()).toHaveValue(/mango-file:/, { timeout: 10000 });

    await page.getByTestId('image-upload-panel').locator('.el-upload-list__item').first().hover();
    const opened = page.waitForEvent('popup');
    await page.getByTestId('image-upload-panel')
      .locator('.el-upload-list__item-preview')
      .first()
      .click();
    const popup = await opened;
    await expect(popup).toHaveURL(/^http:\/\/file\.mango\.io:9000\//);
    await expect(popup).not.toHaveURL(/\/api\/file\/files\/download/);
    await popup.close();

    await expect(page.locator('.el-message--error')).toHaveCount(0);
  });

  test('统一 Upload 支持手动选择后统一上传', async ({ page }) => {
    const filePath = join(tmpdir(), `mango-upload-manual-${Date.now()}.txt`);
    writeFileSync(filePath, 'mango upload manual e2e', 'utf-8');

    await page.getByTestId('manual-upload-panel')
      .locator('input[type="file"]')
      .setInputFiles(filePath);
    await expect(page.getByTestId('manual-upload-panel').getByRole('button', { name: '选取文件' })).toBeVisible();
    await expect(page.getByTestId('manual-upload-panel').getByRole('button', { name: '上传到服务器' })).toBeVisible();
    await expect(page.getByTestId('manual-upload-panel')
      .locator('.upload-control.is-inline-manual')).toBeVisible();
    await expect(page.getByTestId('manual-upload-panel').getByText(/mango-upload-manual/)).toBeVisible({ timeout: 10000 });
    await expect(page.getByTestId('manual-upload-panel')
      .locator('input[readonly]')
      .first()).toHaveValue('', { timeout: 10000 });

    const uploadResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/file/files')
      && response.request().method() === 'POST'
      && response.status() === 200
    );
    await page.getByTestId('manual-upload-panel').getByRole('button', { name: '上传到服务器' }).click();
    const uploadResponse = await uploadResponsePromise;
    const body = await uploadResponse.json();
    expect(body.success || body.code === 200).toBeTruthy();
    if (body.data?.id) uploadedFileIds.push(String(body.data.id));
    expect(body.data?.fileName).toContain('mango-upload-manual');
    await expect(page.getByTestId('manual-upload-panel')
      .locator('input[readonly]')
      .first()).toHaveValue(/mango-file:/, { timeout: 10000 });
    await expect(page.locator('.el-message--error')).toHaveCount(0);
  });

  test('统一 Upload 手动模式一次选择多个文件时使用批量上传接口', async ({ page }) => {
    const firstPath = join(tmpdir(), `mango-upload-batch-a-${Date.now()}.txt`);
    const secondPath = join(tmpdir(), `mango-upload-batch-b-${Date.now()}.txt`);
    writeFileSync(firstPath, 'mango upload batch a', 'utf-8');
    writeFileSync(secondPath, 'mango upload batch b', 'utf-8');

    await page.getByTestId('manual-upload-panel')
      .locator('input[type="file"]')
      .setInputFiles([firstPath, secondPath]);
    await expect(page.getByTestId('manual-upload-panel').getByText(/mango-upload-batch-a/)).toBeVisible({ timeout: 10000 });
    await expect(page.getByTestId('manual-upload-panel').getByText(/mango-upload-batch-b/)).toBeVisible({ timeout: 10000 });
    await expect(page.getByTestId('manual-upload-panel')
      .locator('input[readonly]')
      .first()).toHaveValue('', { timeout: 10000 });

    const batchResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/file/files/batch')
      && response.request().method() === 'POST'
      && response.status() === 200
    );
    await page.getByTestId('manual-upload-panel').getByRole('button', { name: '上传到服务器' }).click();
    await expect(page.getByTestId('manual-upload-panel').locator('.el-progress').first()).toBeVisible({ timeout: 10000 });
    const batchResponse = await batchResponsePromise;
    const body = await batchResponse.json();
    expect(body.success || body.code === 200).toBeTruthy();
    expect(body.data).toHaveLength(2);
    for (const item of body.data || []) {
      if (item?.id) uploadedFileIds.push(String(item.id));
    }
    expect(body.data.map((item: { fileName: string }) => item.fileName).join(',')).toContain('mango-upload-batch-a');
    expect(body.data.map((item: { fileName: string }) => item.fileName).join(',')).toContain('mango-upload-batch-b');
    await expect(page.getByTestId('manual-upload-panel')
      .locator('input[readonly]')
      .first()).toHaveValue(/mango-file:/, { timeout: 10000 });
    await expect(page.locator('.el-message--error')).toHaveCount(0);
  });

  test('统一 Upload 图片场景会拦截非图片格式', async ({ page }) => {
    const textPath = join(tmpdir(), `mango-upload-not-image-${Date.now()}.txt`);
    writeFileSync(textPath, 'not image', 'utf-8');

    await page.getByTestId('image-upload-panel')
      .locator('input[type="file"]')
      .setInputFiles(textPath);

    await expect(page.getByText('该文件类型不允许上传')).toBeVisible({ timeout: 10000 });
    await expect(page.getByTestId('image-upload-panel')
      .locator('input[readonly]')
      .first()).toHaveValue('', { timeout: 10000 });
  });

  test('统一 Upload 办公文档场景使用 /file/files 真实接口并保留文件记录', async ({ page }) => {
    const excelPath = join(tmpdir(), `mango-upload-excel-${Date.now()}.xlsx`);
    const worksheet = XLSX.utils.json_to_sheet([
      { 名称: '测试数据', 数量: 1 },
      { 名称: '联调数据', 数量: 2 },
    ]);
    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Sheet1');
    XLSX.writeFile(workbook, excelPath);

    const uploadResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/file/files')
      && response.request().method() === 'POST'
      && response.status() === 200
    );

    await page.getByTestId('office-upload-panel')
      .locator('input[type="file"]')
      .setInputFiles(excelPath);

    const uploadResponse = await uploadResponsePromise;
    const body = await uploadResponse.json();
    expect(body.success || body.code === 200).toBeTruthy();
    if (body.data?.id) uploadedFileIds.push(String(body.data.id));
    expect(body.data?.fileName).toContain('mango-upload-excel');
    await expect(page.getByTestId('office-upload-panel')
      .locator('input[readonly]')
      .first()).toHaveValue(/mango-upload-excel/, { timeout: 10000 });
    await expect(page.getByText(/mango-upload-excel/).first()).toBeVisible({ timeout: 10000 });
    await expect(page.locator('.el-message--error')).toHaveCount(0);
  });
});
