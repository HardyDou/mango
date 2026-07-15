import { expect, test, type Page } from '@playwright/test';
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

type FileRecord = {
  id: string | number;
  fileName: string;
  fileExt?: string;
  fileSize?: number;
  contentType?: string;
};

type FilePreview = FileRecord & {
  previewable?: boolean;
  previewUrl?: string;
  downloadUrl?: string;
};

test('文件中心真实上传图片、合并 PDF、预览并下载有效结果 @p0 @fileproc', async ({ page }) => {
  test.setTimeout(90 * 1000);

  const createdFileIds: string[] = [];
  const serverErrors: string[] = [];
  const browserErrors: string[] = [];
  page.on('response', (response) => {
    if (response.status() >= 500 && response.url().includes('/api/file/')) {
      serverErrors.push(`${response.status()} ${response.request().method()} ${response.url()}`);
    }
  });
  page.on('pageerror', error => browserErrors.push(`pageerror: ${error.message}`));
  page.on('console', (message) => {
    if (message.type() === 'error') {
      browserErrors.push(`console: ${message.text()}`);
    }
  });

  await login(page);
  const headers = await apiHeaders(page);

  try {
    await page.goto('/#/file/files');
    await expect(page.getByPlaceholder('搜索文件名/业务信息')).toBeVisible({ timeout: 15_000 });

    const suffix = `${Date.now()}-${test.info().workerIndex}`;
    const first = await uploadImageFromFilePage(page, `fileproc-source-a-${suffix}.png`, firstPng());
    createdFileIds.push(String(first.id));
    const second = await uploadImageFromFilePage(page, `fileproc-source-b-${suffix}.png`, secondPng());
    createdFileIds.push(String(second.id));

    const outputName = `fileproc-merged-${suffix}.pdf`;
    const mergeResponse = await page.request.post(api('/file/files/merge-pdf'), {
      headers,
      data: {
        fileName: outputName,
        targetFormat: 'PDF',
        purpose: 'fileproc-e2e',
        accessLevel: 'PRIVATE',
        bizType: 'fileproc-e2e',
        bizId: suffix,
        rebuildBookmark: true,
        addPageNumber: false,
        entries: [
          { fileId: first.id, title: `source-a-${suffix}` },
          { fileId: second.id, title: `source-b-${suffix}` },
        ],
      },
    });
    const merged = await readBusinessResponse<FileRecord>(mergeResponse);
    createdFileIds.push(String(merged.id));
    expect(merged.fileName).toBe(outputName);
    expect(merged.fileExt).toBe('pdf');
    expect(merged.contentType).toBe('application/pdf');
    expect(Number(merged.fileSize)).toBeGreaterThan(500);

    await page.getByPlaceholder('搜索文件名/业务信息').fill(outputName);
    const pageResponsePromise = page.waitForResponse(response =>
      response.url().includes('/api/file/files/page')
      && response.request().method() === 'GET'
      && response.status() === 200
    );
    await page.getByRole('button', { name: '查询', exact: true }).click();
    await readBusinessResponse(await pageResponsePromise);

    const mergedRow = page.getByRole('row').filter({ hasText: outputName });
    await expect(mergedRow).toHaveCount(1);
    await expect(mergedRow).toContainText('application/pdf');

    const previewResponsePromise = page.waitForResponse(response =>
      response.url().includes('/api/file/files/preview')
      && response.url().includes(`id=${merged.id}`)
      && response.status() === 200
    );
    await mergedRow.getByRole('button', { name: '预览', exact: true }).click();
    const preview = await readBusinessResponse<FilePreview>(await previewResponsePromise);
    expect(preview.id).toEqual(merged.id);
    expect(preview.fileName).toBe(outputName);
    expect(preview.fileExt).toBe('pdf');
    expect(preview.contentType).toBe('application/pdf');
    expect(preview.previewUrl).toBeTruthy();
    expect(preview.downloadUrl).toBeTruthy();

    const previewDialog = page.getByRole('dialog', { name: outputName });
    await expect(previewDialog).toBeVisible();
    await expect(previewDialog.getByRole('button', { name: '下载' })).toBeEnabled();

    const downloadResponse = await page.request.get(api(`/file/files/download?id=${merged.id}`), { headers });
    expect(downloadResponse.status()).toBe(200);
    expect(downloadResponse.headers()['content-type']).toContain('application/pdf');
    expect(downloadResponse.headers()['content-disposition']).toContain(encodeURIComponent(outputName));
    const pdf = await downloadResponse.body();
    const pdfText = pdf.toString('latin1');
    expect(pdf.subarray(0, 5).toString('ascii')).toBe('%PDF-');
    expect(pdfText).toContain('%%EOF');
    expect(pdfText.match(/\/Type\s*\/Page\b/g) || []).toHaveLength(2);

    expect(serverErrors).toEqual([]);
    expect(browserErrors).toEqual([]);
  } finally {
    if (createdFileIds.length) {
      const cleanupResponse = await page.request.post(api('/file/files/delete'), {
        headers,
        data: { ids: createdFileIds },
      });
      expect(await readBusinessResponse<boolean>(cleanupResponse)).toBe(true);
    }
  }
});

async function uploadImageFromFilePage(page: Page, fileName: string, buffer: Buffer) {
  const uploadResponsePromise = page.waitForResponse(response =>
    response.url().endsWith('/api/file/files')
    && response.request().method() === 'POST'
    && response.status() === 200
  );
  await page.locator('input[type="file"]').setInputFiles({
    name: fileName,
    mimeType: 'image/png',
    // PNG readers stop at IEND. A per-test trailing marker keeps storage hashes isolated
    // when Playwright repeats run concurrently without changing the decoded image.
    buffer: Buffer.concat([buffer, Buffer.from(fileName)]),
  });
  const uploaded = await readBusinessResponse<FileRecord>(await uploadResponsePromise);
  expect(uploaded.fileName).toBe(fileName);
  expect(uploaded.contentType).toBe('image/png');
  return uploaded;
}

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

async function readBusinessResponse<T = unknown>(response: { json(): Promise<ApiBody<T>>; status(): number }) {
  const body = await response.json();
  expect(response.status(), body.msg || JSON.stringify(body)).toBe(200);
  expect(body.success || body.code === 200, body.msg || '业务响应失败').toBeTruthy();
  expect(body.data, body.msg || '业务响应缺少 data').toBeDefined();
  return body.data as T;
}

function firstPng() {
  return Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==',
    'base64',
  );
}

function secondPng() {
  return Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=',
    'base64',
  );
}
