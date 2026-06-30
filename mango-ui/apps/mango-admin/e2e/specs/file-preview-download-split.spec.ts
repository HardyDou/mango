import { expect, test, type Page, type Route } from '@playwright/test';
import { Buffer } from 'node:buffer';
import { api } from '../support/api';

type ApiBody<T> = {
  code?: number;
  success?: boolean;
  data?: T;
  msg?: string;
};

type LoginData = {
  accessToken: string;
  refreshToken?: string;
  expiresIn?: number;
  tenantId?: string | number;
  tenantCode?: string;
  tenantName?: string;
  realm?: string;
  actorType?: string;
  partyType?: string;
  partyId?: string | number;
  appCode?: string;
};

type FilePreviewResponse = {
  id: string | number;
  fileName: string;
  fileExt?: string;
  fileSize?: number;
  contentType?: string;
  previewable?: boolean;
  previewUrl?: string;
  downloadUrl?: string;
  directAccess?: boolean;
  directPreviewUrl?: string;
  directDownloadUrl?: string;
};

test.describe('文件预览下载地址分离 @p1 @file', () => {
  test('预览接口异常返回下载端点时不触发自动下载，下载按钮仍可下载', async ({ page }) => {
    test.setTimeout(60 * 1000);

    await login(page);
    await page.goto('/#/file/files');
    await expect(page.locator('.file-container')).toBeVisible({ timeout: 15000 });

    const fileName = `mango-file-preview-download-split-${Date.now()}.png`;
    let uploadedFileId = '';

    try {
      const uploadResponsePromise = page.waitForResponse(response =>
        response.url().includes('/api/file/files')
        && response.request().method() === 'POST'
        && response.status() === 200
      );
      await page.locator('input[type="file"]').setInputFiles({
        name: fileName,
        mimeType: 'image/png',
        buffer: createTinyPng(),
      });
      const uploadBody = await readBusinessResponse<Record<string, unknown>>(await uploadResponsePromise);
      uploadedFileId = String(uploadBody.data?.id || '');
      expect(uploadedFileId, '上传接口必须返回文件 id，用于构造预览回归场景').toBeTruthy();

      await expect(page.locator('.el-table__body-wrapper tr', { hasText: fileName }).first()).toBeVisible({
        timeout: 10000,
      });

      await routePreviewAsDownloadEndpoint(page, uploadedFileId);

      const row = page.locator('.el-table__body-wrapper tr', { hasText: fileName }).first();
      const previewResponsePromise = page.waitForResponse(response =>
        response.url().includes('/api/file/files/preview')
        && response.url().includes(`id=${uploadedFileId}`)
        && response.status() === 200
      );
      const unexpectedDownloadPromise = page.waitForEvent('download', { timeout: 1500 })
        .then(() => true)
        .catch(() => false);

      await row.getByRole('button').filter({ hasText: '预览' }).click();
      await readBusinessResponse<FilePreviewResponse>(await previewResponsePromise);

      const previewDialog = page.locator('.file-preview-dialog').filter({ hasText: fileName }).first();
      await expect(previewDialog).toBeVisible({ timeout: 10000 });
      await expect(previewDialog.locator('.preview-placeholder')).toBeVisible();
      await expect(previewDialog.locator('img[src*="/file/files/download"]')).toHaveCount(0);
      await expect(previewDialog.locator('iframe[src*="/file/files/download"]')).toHaveCount(0);
      await expect(previewDialog.locator('video[src*="/file/files/download"]')).toHaveCount(0);
      await expect(previewDialog.locator('audio[src*="/file/files/download"]')).toHaveCount(0);
      await expect(previewDialog.locator('.preview-dialog-actions button').first()).toBeDisabled();
      expect(await unexpectedDownloadPromise, '点击预览不应该触发浏览器下载事件').toBe(false);

      const downloadPromise = page.waitForEvent('download');
      await previewDialog.locator('.preview-dialog-actions button').nth(1).click();
      const download = await downloadPromise;
      expect(download.suggestedFilename()).toContain(fileName);

      await expect(page.locator('.el-message--error')).toHaveCount(0);
    } finally {
      if (uploadedFileId) {
        await cleanupUploadedFile(page, uploadedFileId);
      }
    }
  });
});

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
    return body.data as LoginData;
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
}

async function routePreviewAsDownloadEndpoint(page: Page, fileId: string) {
  await page.route('**/api/file/files/preview**', async (route: Route) => {
    const url = new URL(route.request().url());
    if (url.searchParams.get('id') !== fileId) {
      await route.continue();
      return;
    }

    const originResponse = await route.fetch();
    const originBody = await originResponse.json() as ApiBody<FilePreviewResponse>;
    const downloadEndpoint = `/api/file/files/download?id=${encodeURIComponent(fileId)}`;
    await route.fulfill({
      status: originResponse.status(),
      headers: originResponse.headers(),
      contentType: 'application/json',
      body: JSON.stringify({
        ...originBody,
        data: {
          ...originBody.data,
          id: fileId,
          previewable: true,
          previewUrl: downloadEndpoint,
          directPreviewUrl: '',
          downloadUrl: downloadEndpoint,
          directDownloadUrl: downloadEndpoint,
        },
      }),
    });
  });
}

async function cleanupUploadedFile(page: Page, fileId: string) {
  const headers = await apiHeaders(page);
  await page.request.post(api('/file/files/delete'), {
    headers,
    data: { ids: [fileId] },
  });
}

async function apiHeaders(page: Page) {
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

async function readBusinessResponse<T>(response: { json(): Promise<ApiBody<T>>; status(): number }) {
  const body = await response.json();
  expect(response.status(), body.msg || JSON.stringify(body)).toBe(200);
  expect(body.success || body.code === 200, body.msg || '业务响应失败').toBeTruthy();
  return body;
}

function createTinyPng() {
  return Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
    'base64',
  );
}
