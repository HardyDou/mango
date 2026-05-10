import { expect, test } from '@playwright/test';
import { writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { tmpdir } from 'node:os';
import * as XLSX from 'xlsx';

async function login(page: import('@playwright/test').Page) {
  await page.goto('/#/login');
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  await page.click('button:has-text("登 录")');
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

test.describe.serial('上传组件文件接口联调', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);

    await page.goto('/#/demo/upload');
    await expect(page.getByText('文件上传组件').first()).toBeVisible({ timeout: 10000 });
  });

  test('FileUpload 默认使用 /file/files 真实接口', async ({ page }) => {
    const filePath = join(tmpdir(), `mango-upload-component-${Date.now()}.txt`);
    writeFileSync(filePath, 'mango upload component e2e', 'utf-8');

    const uploadResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/file/files')
      && response.request().method() === 'POST'
      && response.status() === 200
    );

    await page.locator('.demo-card', { hasText: '文件上传 (FileUpload)' })
      .locator('input[type="file"]')
      .setInputFiles(filePath);

    const uploadResponse = await uploadResponsePromise;
    const body = await uploadResponse.json();
    expect(body.success || body.code === 200).toBeTruthy();
    expect(body.data?.fileName).toContain('mango-upload-component');
    await expect(page.locator('.demo-card', { hasText: '文件上传 (FileUpload)' })
      .locator('input[readonly]')
      .first()).toHaveValue(/mango-file:/, { timeout: 10000 });
    await expect(page.locator('.el-message--error')).toHaveCount(0);
  });

  test('ImageUpload 默认使用 /file/files 真实接口并返回文件标识', async ({ page }) => {
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

    await page.locator('.demo-card', { hasText: '图片上传 (ImageUpload)' })
      .locator('input[type="file"]')
      .setInputFiles(imagePath);

    const uploadResponse = await uploadResponsePromise;
    const body = await uploadResponse.json();
    expect(body.success || body.code === 200).toBeTruthy();
    expect(body.data?.fileName).toContain('mango-upload-image');
    await expect(page.locator('.demo-card', { hasText: '图片上传 (ImageUpload)' })
      .locator('input[readonly]')
      .first()).toHaveValue(/mango-file:/, { timeout: 10000 });
    await expect(page.locator('.el-message--error')).toHaveCount(0);
  });

  test('ExcelUpload 默认使用 /file/files 真实接口并保留文件记录', async ({ page }) => {
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

    await page.locator('.demo-card', { hasText: 'Excel上传 (ExcelUpload)' })
      .locator('input[type="file"]')
      .setInputFiles(excelPath);

    const uploadResponse = await uploadResponsePromise;
    const body = await uploadResponse.json();
    expect(body.success || body.code === 200).toBeTruthy();
    expect(body.data?.fileName).toContain('mango-upload-excel');
    await expect(page.locator('.demo-card', { hasText: 'Excel上传 (ExcelUpload)' })
      .locator('input[readonly]')
      .first()).toHaveValue(/mango-file:/, { timeout: 10000 });
    await expect(page.getByText(/已解析 2 条数据/)).toBeVisible({ timeout: 10000 });
    await expect(page.locator('.el-message--error')).toHaveCount(0);
  });
});
