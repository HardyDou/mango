import { expect, test } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { unlinkSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { tmpdir } from 'node:os';

const realisticAllowedExtensions = [
  'txt', 'csv', 'json', 'xml',
  'png', 'jpg', 'jpeg', 'gif', 'webp', 'svg',
  'pdf', 'ofd',
  'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx',
  'odt', 'ods', 'odp',
  'zip', 'rar', '7z',
].join(', ');

async function login(page: import('@playwright/test').Page) {
  await page.goto('/#/login');
  await page.fill('input[placeholder="用户名"]', 'admin');
  await page.fill('input[placeholder="密码"]', 'admin123');
  const accountTenantsResponsePromise = page.waitForResponse((response) =>
    response.url().includes('/api/auth/login-institutions') && response.status() === 200
  );
  await page.locator('input[placeholder="密码"]').blur();
  await accountTenantsResponsePromise;
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: /芒果集团/ }).click();
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 10000 });
}

async function routeMinioDirectAccess(
  page: import('@playwright/test').Page,
  request: import('@playwright/test').APIRequestContext,
) {
  await page.route('http://file.mango.io:9000/**', async (route) => {
    const url = new URL(route.request().url());
    const response = await request.get(`http://127.0.0.1:9000${url.pathname}${url.search}`, {
      headers: { Host: url.host },
    });
    await route.fulfill({
      status: response.status(),
      headers: response.headers(),
      body: await response.body(),
    });
  });
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

function cleanupE2EFiles() {
  execFileSync('mysql', ['-uroot', process.env.MANGO_DB_NAME || 'mango', '-e', [
    "DELETE FROM file_record WHERE file_name LIKE 'mango-file-e2e%' OR biz_id LIKE 'mango-file-e2e%' OR biz_type LIKE 'mango-file-e2e%'",
    "DELETE FROM file_directory WHERE directory_name LIKE 'mango-file-e2e%'",
  ].join('; ')]);
}

test.describe('文件管理联调', () => {
  test.describe.configure({ mode: 'serial' });

  test.afterEach(() => {
    cleanupE2EFiles();
  });

  test('文件配置支持维护上传访问和预览策略', async ({ page }) => {
    await login(page);

    await page.goto('/#/file/settings');
    await expect(page.getByText('文件配置').first()).toBeVisible({ timeout: 10000 });

    const settingsResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/file/settings')
      && response.request().method() === 'GET'
      && response.status() === 200
    );
    await page.reload();
    await settingsResponsePromise;

    await page.getByRole('spinbutton', { name: /单文件大小/ }).fill('100');
    await page.getByLabel('允许扩展名').fill('');
    await page.getByLabel('允许扩展名').pressSequentially(realisticAllowedExtensions);
    await expect(page.getByLabel('允许扩展名')).toHaveValue(realisticAllowedExtensions);
    await page.getByLabel('禁止扩展名').fill('');
    await page.getByLabel('禁止扩展名').pressSequentially('exe, bat, cmd, sh, jar');
    await expect(page.getByLabel('禁止扩展名')).toHaveValue('exe, bat, cmd, sh, jar');
    await page.getByLabel('允许 Content-Type').fill('');
    await page.getByLabel('禁止 Content-Type').fill('');
    await page.getByLabel('禁止 Content-Type').pressSequentially('application/x-msdownload, application/x-sh');
    await expect(page.getByLabel('禁止 Content-Type')).toHaveValue('application/x-msdownload, application/x-sh');
    await page.getByRole('spinbutton', { name: /直传有效期/ }).fill('900');
    await page.getByRole('spinbutton', { name: /访问有效期/ }).fill('600');
    await page.locator('.el-radio-button__inner', { hasText: '存储直连' }).click();
    await page.getByLabel('文档预览服务').fill('');
    await page.getByRole('spinbutton', { name: /预览有效期/ }).fill('600');
    await page.getByLabel('外部预览类型').fill('');
    await page.getByLabel('外部预览类型').pressSequentially(realisticAllowedExtensions);
    await expect(page.getByLabel('外部预览类型')).toHaveValue(realisticAllowedExtensions);
    const saveResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/file/settings')
      && response.request().method() === 'PUT'
    );
    await page.getByRole('button', { name: '保存配置' }).click();
    const saveResponse = await saveResponsePromise;
    expect(saveResponse.status()).toBe(200);
    const saveBody = await saveResponse.json();
    expect(saveBody.success || saveBody.code === 200).toBeTruthy();
    await expect(page.getByText('保存成功')).toBeVisible({ timeout: 10000 });

    const persisted = await page.request.get('/api/file/settings', { headers: await apiHeaders(page) });
    const persistedBody = await persisted.json();
    expect(persistedBody.success || persistedBody.code === 200).toBeTruthy();
    expect(persistedBody.data.allowedExtensions).toEqual(realisticAllowedExtensions.split(', '));
    expect(persistedBody.data.blockedExtensions).toEqual(['exe', 'bat', 'cmd', 'sh', 'jar']);
    expect(persistedBody.data.blockedContentTypes).toEqual(['application/x-msdownload', 'application/x-sh']);
    expect(persistedBody.data.previewExternalExtensions).toEqual(realisticAllowedExtensions.split(', '));
    expect(persistedBody.data.accessMode).toBe('DIRECT');

    await expect(page.locator('.el-message--error')).toHaveCount(0);
    await expect(page.locator('text=/401|403|未授权|拒绝访问|登录已过期|请重新登录/')).toHaveCount(0);
  });

  test('目录、上传、分页、详情、下载、归档使用真实文件接口', async ({ page, request }) => {
    await routeMinioDirectAccess(page, request);
    await login(page);

    await page.goto('/#/file/files');
    await expect(page.getByText('文件管理').first()).toBeVisible({ timeout: 10000 });

    const directoryName = `mango-file-e2e-dir-${Date.now()}`;
    const directoryResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/file/directories')
      && response.request().method() === 'POST'
      && response.status() === 200
    );
    await page.getByRole('button', { name: '新建' }).click();
    await page.getByLabel('目录名称').fill(directoryName);
    await page.getByRole('button', { name: '保存' }).click();
    const directoryResponse = await directoryResponsePromise;
    const directoryBody = await directoryResponse.json();
    expect(directoryBody.success || directoryBody.code === 200).toBeTruthy();
    await expect(page.getByText('目录创建成功')).toBeVisible({ timeout: 10000 });
    await page.getByText(directoryName).click();
    await expect(page.getByText(directoryName).first()).toBeVisible({ timeout: 10000 });

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
    await row.getByRole('button', { name: '预览' }).click();
    const previewResponse = await previewResponsePromise;
    const previewBody = await previewResponse.json();
    expect(previewBody.success || previewBody.code === 200).toBeTruthy();
    const previewDialog = page.getByRole('dialog', { name: new RegExp(fileName) });
    await expect(previewDialog).toBeVisible();
    await expect(previewDialog.getByText(fileName).first()).toBeVisible();
    await expect(previewDialog.getByRole('button', { name: '新窗口预览' })).toBeVisible();
    await expect(previewDialog.getByRole('button', { name: '下载' })).toBeVisible();
    await expect(previewDialog.getByRole('button', { name: '关闭' })).toBeVisible();
    await expect(previewDialog.getByText('文件大小')).toHaveCount(0);
    await expect(previewDialog.getByText('内容类型')).toHaveCount(0);
    await expect(previewDialog.getByText('扩展名')).toHaveCount(0);
    await page.keyboard.press('Escape');

    const downloadUrl = previewBody.data?.directDownloadUrl || previewBody.data?.downloadUrl;
    expect(downloadUrl).toBeTruthy();
    const resolvedDownloadUrl = String(downloadUrl).startsWith('/api')
      ? String(downloadUrl)
      : new URL(String(downloadUrl), process.env.PLAYWRIGHT_API_BASE_URL || 'http://127.0.0.1:5555').toString();
    const downloadResponse = await request.get(resolvedDownloadUrl, { headers: await apiHeaders(page) });
    expect(downloadResponse.status()).toBe(200);
    expect((await downloadResponse.body()).byteLength).toBeGreaterThan(0);
    await row.getByRole('button', { name: '下载' }).click();
    await expect(page.locator('.el-message--error')).toHaveCount(0);

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

    await page.getByRole('button', { name: '更多' }).click();
    const deleteDirectoryResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/file/directories')
      && response.request().method() === 'DELETE'
      && response.status() === 200
    );
    await page.getByRole('menuitem', { name: '删除' }).click();
    await page.getByRole('button', { name: '确定' }).last().click();
    await deleteDirectoryResponsePromise;
    await expect(page.getByText('目录删除成功')).toBeVisible({ timeout: 10000 });

    await expect(page.locator('.el-message--error')).toHaveCount(0);
    await expect(page.locator('text=/401|403|未授权|拒绝访问|登录已过期|请重新登录/')).toHaveCount(0);
    unlinkSync(filePath);
  });

  test('文件支持单个删除和批量删除', async ({ page }) => {
    await login(page);

    await page.goto('/#/file/files');
    await expect(page.getByText('文件管理').first()).toBeVisible({ timeout: 10000 });

    const firstPath = join(tmpdir(), `mango-file-e2e-delete-${Date.now()}-a.txt`);
    const secondPath = join(tmpdir(), `mango-file-e2e-delete-${Date.now()}-b.txt`);
    const thirdPath = join(tmpdir(), `mango-file-e2e-delete-${Date.now()}-c.txt`);
    writeFileSync(firstPath, `mango delete e2e a ${Date.now()}`);
    writeFileSync(secondPath, `mango delete e2e b ${Date.now()}`);
    writeFileSync(thirdPath, `mango delete e2e c ${Date.now()}`);

    async function uploadOne(filePath: string) {
      const uploadResponsePromise = page.waitForResponse((response) =>
        response.url().includes('/api/file/files')
        && response.request().method() === 'POST'
        && response.status() === 200
      );
      await page.setInputFiles('input[type="file"]', filePath);
      const uploadResponse = await uploadResponsePromise;
      const uploadBody = await uploadResponse.json();
      expect(uploadBody.success || uploadBody.code === 200).toBeTruthy();
      await expect(page.locator('.el-message__content', { hasText: '上传成功' }).last()).toBeVisible({ timeout: 10000 });
      return uploadBody.data.fileName as string;
    }

    const firstName = await uploadOne(firstPath);
    const secondName = await uploadOne(secondPath);
    const thirdName = await uploadOne(thirdPath);
    await expect(page.getByText(firstName).first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(secondName).first()).toBeVisible({ timeout: 10000 });
    await expect(page.getByText(thirdName).first()).toBeVisible({ timeout: 10000 });

    const firstRow = page.locator('.el-table__body-wrapper tr', { hasText: firstName }).first();
    const singleDeleteResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/file/files/delete')
      && response.request().method() === 'POST'
      && response.status() === 200
    );
    await firstRow.getByRole('button', { name: '删除' }).click();
    await page.getByRole('button', { name: '确定' }).last().click();
    const singleDeleteResponse = await singleDeleteResponsePromise;
    const singleDeleteBody = await singleDeleteResponse.json();
    expect(singleDeleteBody.success || singleDeleteBody.code === 200).toBeTruthy();
    await expect(page.getByText('删除成功')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('.el-table__body-wrapper tr', { hasText: firstName })).toHaveCount(0);

    for (const name of [secondName, thirdName]) {
      const row = page.locator('.el-table__body-wrapper tr', { hasText: name }).first();
      await row.locator('.el-checkbox__input').click();
    }
    const batchDeleteResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/file/files/delete')
      && response.request().method() === 'POST'
      && response.status() === 200
    );
    await page.getByRole('button', { name: '批量删除' }).click();
    await page.getByRole('button', { name: '确定' }).last().click();
    const batchDeleteResponse = await batchDeleteResponsePromise;
    const batchDeleteBody = await batchDeleteResponse.json();
    expect(batchDeleteBody.success || batchDeleteBody.code === 200).toBeTruthy();
    await expect(page.getByText('批量删除成功')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('.el-table__body-wrapper tr', { hasText: secondName })).toHaveCount(0);
    await expect(page.locator('.el-table__body-wrapper tr', { hasText: thirdName })).toHaveCount(0);

    await expect(page.locator('.el-message--error')).toHaveCount(0);
    unlinkSync(firstPath);
    unlinkSync(secondPath);
    unlinkSync(thirdPath);
  });

  test('上传 PNG 图片后使用当前存储访问地址预览', async ({ page, request }) => {
    await routeMinioDirectAccess(page, request);
    await login(page);

    await page.goto('/#/file/files');
    await expect(page.getByText('文件管理').first()).toBeVisible({ timeout: 10000 });

    const filePath = join(tmpdir(), `mango-file-e2e-${Date.now()}.png`);
    writeFileSync(filePath, Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAwAAAAKCAIAAAAPTiitAAAAFElEQVR4nGP8z4AEMGJpGNGjBgD2cQITX3Z5mQAAAABJRU5ErkJggg==',
      'base64',
    ));

    const uploadResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/file/files')
      && response.request().method() === 'POST'
      && response.status() === 200
    );
    await page.setInputFiles('input[type="file"]', filePath);
    const uploadResponse = await uploadResponsePromise;
    const uploadBody = await uploadResponse.json();
    expect(uploadBody.success || uploadBody.code === 200).toBeTruthy();

    const fileName = uploadBody.data.fileName;
    await expect(page.getByText(fileName).first()).toBeVisible({ timeout: 10000 });
    const row = page.locator('.el-table__body-wrapper tr', { hasText: fileName }).first();

    const previewResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/file/files/preview')
      && response.status() === 200
    );
    await row.getByRole('button', { name: '预览' }).click();
    const previewResponse = await previewResponsePromise;
    const previewBody = await previewResponse.json();
    expect(previewBody.success || previewBody.code === 200).toBeTruthy();
    expect(previewBody.data?.directPreviewUrl).toBeTruthy();
    expect(previewBody.data?.directDownloadUrl).toBeTruthy();
    expect(String(previewBody.data.directPreviewUrl)).toMatch(
      /^(http:\/\/file\.mango\.io:9000\/|https?:\/\/127\.0\.0\.1:\d+\/file\/local-objects\/|\/(?:api\/)?file\/local-objects\/)/,
    );
    expect(String(previewBody.data.directDownloadUrl)).toMatch(
      /^(http:\/\/file\.mango\.io:9000\/|https?:\/\/127\.0\.0\.1:\d+\/file\/local-objects\/|\/(?:api\/)?file\/local-objects\/)/,
    );
    expect(previewBody.data?.directAccess).toBeTruthy();

    const previewImage = page.locator('.preview-image .el-image__inner').first();
    await expect(previewImage).toBeVisible({ timeout: 10000 });
    await expect(previewImage).toHaveJSProperty('naturalWidth', 12);
    await expect(previewImage).toHaveJSProperty('naturalHeight', 10);

    await page.keyboard.press('Escape');
    const archiveResponsePromise = page.waitForResponse((response) =>
      response.url().includes('/api/file/files')
      && response.request().method() === 'DELETE'
      && response.status() === 200
    );
    await row.getByRole('button', { name: '归档' }).click();
    await page.getByRole('button', { name: '确定' }).last().click();
    await archiveResponsePromise;

    await expect(page.locator('.el-message--error')).toHaveCount(0);
    unlinkSync(filePath);
  });
});
