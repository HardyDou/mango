import { expect, test } from '@playwright/test';
import { resolveE2EApiBaseURL } from '../../../../playwright.workspace';

const apiBaseURL = resolveE2EApiBaseURL({
  uiRoot: process.cwd(),
  defaultURL: 'http://127.0.0.1:5555',
});

test('file preview task is queued, reusable, and renders a PDF page', async ({ request, page }) => {
  test.skip(process.env.MANGO_E2E_ASYNC_PREVIEW !== 'true', 'set MANGO_E2E_ASYNC_PREVIEW=true for a live backend');
  const login = await request.post(`${apiBaseURL}/auth/login`, {
    data: { username: 'admin', password: 'admin123', tenantId: 1 },
  });
  expect(login.ok()).toBeTruthy();
  const token = (await login.json()).data.accessToken as string;
  const headers = { Authorization: `Bearer ${token}` };
  const upload = await request.post(`${apiBaseURL}/file/files`, {
    headers,
    multipart: {
      file: { name: `async-preview-${Date.now()}.txt`, mimeType: 'text/plain', buffer: Buffer.from('async preview') },
      purpose: 'preview-e2e',
      bizType: 'FILE_PREVIEW_ASYNC_E2E',
    },
  });
  expect(upload.ok()).toBeTruthy();
  const fileId = String((await upload.json()).data.id);
  const first = await request.get(`${apiBaseURL}/file-preview/files/preview-status?fileId=${fileId}`, { headers });
  expect(first.ok()).toBeTruthy();
  expect(['QUEUED', 'PROCESSING', 'SUCCEEDED']).toContain((await first.json()).data.status);
  await expect.poll(async () => {
    const response = await request.get(`${apiBaseURL}/file-preview/files/preview-status?fileId=${fileId}`, { headers });
    return (await response.json()).data.status;
  }, { timeout: 30000 }).toBe('SUCCEEDED');
  const second = await request.get(`${apiBaseURL}/file-preview/files/preview-status?fileId=${fileId}`, { headers });
  const secondData = (await second.json()).data;
  expect(secondData.previewFileId).toBeTruthy();
  const linkResponse = await request.get(`${apiBaseURL}/file-preview/files/preview-link?fileId=${fileId}`, { headers });
  expect(linkResponse.ok()).toBeTruthy();
  const previewUrl = new URL((await linkResponse.json()).data.previewUrl, apiBaseURL).toString();
  await page.goto(previewUrl);
  await expect(page).toHaveTitle('PDF预览');
  const pdfFrame = page.frames().find((frame) => frame.url().includes('/pdfjs/web/viewer.html'));
  expect(pdfFrame).toBeTruthy();
  await expect(pdfFrame!.locator('#viewer .page')).toHaveCount(1, { timeout: 30000 });
  await expect(pdfFrame!.locator('#viewer canvas')).toHaveCount(1, { timeout: 30000 });
});
