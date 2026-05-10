import { expect, test } from '@playwright/test';
import { writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { tmpdir } from 'node:os';

async function login(page: import('@playwright/test').Page) {
  await page.goto('/#/login');
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  await page.click('button:has-text("登 录")');
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

test.describe('文件管理联调', () => {
  test('上传、分页、详情、下载、归档使用真实文件接口', async ({ page }) => {
    await login(page);

    await page.goto('/#/system/file');
    await expect(page.getByText('文件管理').first()).toBeVisible({ timeout: 10000 });

    const filePath = join(tmpdir(), `mango-file-e2e-${Date.now()}.txt`);
    writeFileSync(filePath, `mango file e2e ${Date.now()}`);

    const uploadResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/file/files')
      && response.request().method() === 'POST'
      && response.status() === 200
    );
    await page.setInputFiles('input[type="file"]', filePath);
    const uploadResponse = await uploadResponsePromise;
    const uploadBody = await uploadResponse.json();
    expect(uploadBody.success || uploadBody.code === 200).toBeTruthy();
    expect(uploadBody.data?.fileName).toContain('mango-file-e2e');
    await expect(page.getByText('上传成功')).toBeVisible({ timeout: 10000 });

    const fileName = uploadBody.data.fileName;
    await expect(page.getByText(fileName).first()).toBeVisible({ timeout: 10000 });

    const row = page.locator('.el-table__body-wrapper tr', { hasText: fileName }).first();
    const previewResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/file/files/preview')
      && response.url().includes('/preview')
      && response.status() === 200
    );
    await row.getByRole('button', { name: '详情' }).click();
    const previewResponse = await previewResponsePromise;
    const previewBody = await previewResponse.json();
    expect(previewBody.success || previewBody.code === 200).toBeTruthy();
    await expect(page.getByRole('dialog', { name: '文件详情' })).toBeVisible();
    await page.keyboard.press('Escape');

    const downloadResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/file/files/download')
      && response.url().includes('/download')
      && response.request().method() === 'GET'
      && response.status() === 200
    );
    await row.getByRole('button', { name: '下载' }).click();
    await downloadResponsePromise;

    const archiveResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/file/files')
      && response.request().method() === 'DELETE'
      && response.status() === 200
    );
    await row.getByRole('button', { name: '归档' }).click();
    await page.getByRole('button', { name: '确定' }).last().click();
    const archiveResponse = await archiveResponsePromise;
    const archiveBody = await archiveResponse.json();
    expect(archiveBody.success || archiveBody.code === 200).toBeTruthy();
    await expect(page.getByText('归档成功')).toBeVisible({ timeout: 10000 });

    await expect(page.locator('.el-message--error')).toHaveCount(0);
    await expect(page.locator('text=/401|403|未授权|拒绝访问|登录已过期|请重新登录/')).toHaveCount(0);
  });
});
