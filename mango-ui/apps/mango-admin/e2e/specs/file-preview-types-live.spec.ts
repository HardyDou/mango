import { expect, test, type APIRequestContext, type Page, type Response } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import * as XLSX from 'xlsx';
import { resolveE2EApiBaseURL, resolveE2EBaseURL } from '../../../../playwright.workspace';

const uiRoot = resolve(__dirname, '../../../..');
const apiBaseURL = resolveE2EApiBaseURL({ uiRoot, defaultURL: 'http://127.0.0.1:5555' });
const frontendBaseURL = resolveE2EBaseURL({ uiRoot, defaultURL: 'http://127.0.0.1:7777' });
const resultFile = join(process.cwd(), 'e2e', '.tmp', 'file-preview-types-live-results.json');
const screenshotFile = join(process.cwd(), 'e2e', '.tmp', 'file-preview-types-live-zip.png');
const officeScreenshotFile = join(process.cwd(), 'e2e', '.tmp', 'file-preview-issue-563-docx.png');

type PreviewCase = {
  key: string;
  extension: string;
  mimeType: string;
  expected: 'preview' | 'unsupported-or-preview' | 'environment-blocked';
  create: () => Buffer | string;
  fileName?: string;
};

type CaseResult = {
  key: string;
  fileName: string;
  fileId?: string;
  uploadOk: boolean;
  previewLinkOk: boolean;
  previewPageOk: boolean;
  downloadOk: boolean;
  status: 'PASS' | 'FAIL' | 'BLOCKED';
  reason: string;
  previewUrl?: string;
  pageTitle?: string;
  pageText?: string;
  downloadContentType?: string;
  zipLayoutOk?: boolean;
  zipLayoutReason?: string;
  zipInnerPdfOk?: boolean;
  zipInnerPdfReason?: string;
  pdfPageCount?: number;
  pdfResourceUrl?: string;
  pdfBackingUrl?: string;
  pdfResponseStatus?: number;
  pdfResponseContentType?: string;
  doubleEncodedRequestCount?: number;
};

type BrowserResponse = {
  url: string;
  status: number;
  contentType: string;
};

type ApiResponseBody = {
  success?: boolean;
  code?: number;
  data?: unknown;
};

type LoginData = {
  accessToken: string;
};

type FileUploadData = {
  id: string | number;
};

type PreviewLinkData = {
  previewUrl: string;
};

const cases: PreviewCase[] = [
  {
    key: 'text',
    extension: 'txt',
    mimeType: 'text/plain',
    expected: 'preview',
    create: () => Buffer.from(`Mango file preview text ${Date.now()}`, 'utf-8'),
  },
  {
    key: 'image',
    extension: 'png',
    mimeType: 'image/png',
    expected: 'preview',
    create: () => Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=',
      'base64',
    ),
  },
  {
    key: 'pdf',
    extension: 'pdf',
    mimeType: 'application/pdf',
    expected: 'preview',
    create: () => createPdfFixture(),
  },
  {
    key: 'zip',
    extension: 'zip',
    mimeType: 'application/zip',
    expected: 'unsupported-or-preview',
    create: () => createZipFixture(),
  },
  {
    key: 'xlsx',
    extension: 'xlsx',
    mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    expected: 'environment-blocked',
    create: () => createXlsxFixture(),
  },
  {
    key: 'docx-mixed-name',
    extension: 'docx',
    mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    expected: process.env.MANGO_E2E_OFFICE_ENABLED === 'true' ? 'preview' : 'environment-blocked',
    create: () => createDocxFixture(),
    fileName: '中文 (1).docx',
  },
];

function api(path: string) {
  return `${apiBaseURL}${path}`;
}

function asApiResponseBody(body: unknown): ApiResponseBody {
  expect(body && typeof body === 'object', `接口响应结构异常: ${JSON.stringify(body)}`).toBeTruthy();
  return body as ApiResponseBody;
}

function expectApiSuccess(body: unknown, message: string): ApiResponseBody {
  const responseBody = asApiResponseBody(body);
  expect(responseBody.success || responseBody.code === 200, `${message}: ${JSON.stringify(body)}`).toBeTruthy();
  return responseBody;
}

function expectData<T>(data: unknown, message: string): T {
  expect(data && typeof data === 'object', message).toBeTruthy();
  return data as T;
}

async function loginToken(request: APIRequestContext) {
  const response = await request.post(api('/auth/login'), {
    data: {
      username: 'admin',
      password: 'admin123',
      tenantId: '1',
    },
  });
  expect(response.status()).toBe(200);
  const body = await response.json();
  const responseBody = expectApiSuccess(body, '登录失败');
  return expectData<LoginData>(responseBody.data, '登录响应缺少 token').accessToken;
}

function authHeaders(token: string) {
  return {
    Authorization: `Bearer ${token}`,
    'X-Tenant-Id': '1',
    'TENANT-ID': '1',
    'X-Mango-Tenant-Id': '1',
  };
}

function createZipFixture() {
  const dir = join(tmpdir(), `mango-preview-zip-${Date.now()}`);
  mkdirSync(dir, { recursive: true });
  const textSource = join(dir, 'readme.txt');
  const pdfSource = join(dir, 'sample.pdf');
  const zipPath = join(dir, 'fixture.zip');
  writeFileSync(textSource, 'Mango zip preview fixture', 'utf-8');
  writeFileSync(pdfSource, createPdfFixture());
  execFileSync('zip', ['-q', zipPath, 'readme.txt', 'sample.pdf'], { cwd: dir });
  const content = Buffer.from(readFileSync(zipPath));
  rmSync(dir, { recursive: true, force: true });
  return content;
}

function createPdfFixture() {
  return Buffer.from([
    '%PDF-1.4',
    '1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj',
    '2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj',
    '3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 200 200] /Contents 4 0 R >> endobj',
    '4 0 obj << /Length 44 >> stream',
    'BT /F1 12 Tf 30 120 Td (Mango PDF Preview) Tj ET',
    'endstream endobj',
    'xref',
    '0 5',
    '0000000000 65535 f ',
    'trailer << /Root 1 0 R /Size 5 >>',
    'startxref',
    '310',
    '%%EOF',
  ].join('\n'), 'utf-8');
}

function createXlsxFixture() {
  const workbook = XLSX.utils.book_new();
  const worksheet = XLSX.utils.json_to_sheet([
    { name: 'Mango', value: 'file-preview-e2e' },
  ]);
  XLSX.utils.book_append_sheet(workbook, worksheet, 'Sheet1');
  return XLSX.write(workbook, { type: 'buffer', bookType: 'xlsx' }) as Buffer;
}

function createDocxFixture() {
  const dir = join(tmpdir(), `mango-preview-docx-${Date.now()}`);
  const relsDir = join(dir, '_rels');
  const wordDir = join(dir, 'word');
  mkdirSync(relsDir, { recursive: true });
  mkdirSync(wordDir, { recursive: true });
  writeFileSync(join(dir, '[Content_Types].xml'), `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>`, 'utf-8');
  writeFileSync(join(relsDir, '.rels'), `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>`, 'utf-8');
  writeFileSync(join(wordDir, 'document.xml'), `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    <w:p><w:r><w:t>Mango Issue 563 mixed encoded filename preview</w:t></w:r></w:p>
    <w:sectPr><w:pgSz w:w="11906" w:h="16838"/><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/></w:sectPr>
  </w:body>
</w:document>`, 'utf-8');
  const docxPath = join(dir, 'fixture.docx');
  execFileSync('zip', ['-q', '-r', docxPath, '[Content_Types].xml', '_rels', 'word'], { cwd: dir });
  const content = Buffer.from(readFileSync(docxPath));
  rmSync(dir, { recursive: true, force: true });
  return content;
}

function previewEntryUrl(previewUrl: string) {
  const url = previewUrl.startsWith('http')
    ? new URL(previewUrl)
    : new URL(previewUrl, frontendBaseURL);
  const path = url.pathname.startsWith('/api/') ? url.pathname : `/api${url.pathname}`;
  return `${frontendBaseURL}${path}${url.search}`;
}

function pageLooksLikePreview(text: string, title: string) {
  const combined = `${title}\n${text}`;
  return !/Whitelabel Error Page|HTTP Status 500|系统异常|登录|404 Not Found/i.test(combined)
    && /Mango|文件|图片预览|普通文本预览|压缩包预览|preview|pdf|image|txt|zip|xlsx|在线预览|暂不支持|无法在线预览/i.test(combined);
}

function classifyResult(item: PreviewCase, pageText: string, pageOk: boolean, downloadOk: boolean) {
  if (!downloadOk) {
    return { status: 'FAIL' as const, reason: '下载接口未返回可读取的二进制文件' };
  }
  if (item.key === 'zip' && /目录加载失败|目录读取失败|暂时无法读取压缩包目录结构/.test(pageText)) {
    return { status: 'FAIL' as const, reason: 'zip 预览页打开但压缩包目录读取失败' };
  }
  if (item.expected === 'environment-blocked' && /暂未启用 Office 转换能力|找不到office组件|Office/.test(pageText)) {
    return { status: 'BLOCKED' as const, reason: '当前验证环境禁用了 Office 插件，Office 文档预览被环境阻塞' };
  }
  if (pageOk) {
    return { status: 'PASS' as const, reason: '预览入口可打开，下载返回二进制内容' };
  }
  return { status: 'FAIL' as const, reason: '预览页未呈现可识别的预览或不可预览状态' };
}

async function inspectPreviewPage(page: Page, url: string) {
  const response = await page.goto(url, { waitUntil: 'domcontentloaded', timeout: 30000 });
  await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => undefined);
  const title = await page.title().catch(() => '');
  const text = await page.locator('body').innerText({ timeout: 5000 }).catch(() => '');
  return {
    status: response?.status() || 0,
    title,
    text: text.slice(0, 1000),
    ok: Boolean(response && response.status() < 500 && pageLooksLikePreview(text, title)),
  };
}

async function inspectOfficePdf(
  page: Page,
  fileId: string,
  requestUrls: string[],
  responses: BrowserResponse[],
) {
  const frame = page.frameLocator('#pdfFrame');
  const iframeSrc = await page.locator('#pdfFrame').getAttribute('src');
  expect(iframeSrc, 'PDF.js iframe 缺少资源地址').toBeTruthy();
  const pdfResourceUrl = new URL(iframeSrc || '', frontendBaseURL).searchParams.get('file') || '';
  const decodedPdfResourceUrl = decodeURIComponent(pdfResourceUrl);
  const pdfBackingUrl = unwrapCorsResourceUrl(decodedPdfResourceUrl);
  const findPdfResponse = () => responses.find(
    response => unwrapCorsResourceUrl(response.url) === pdfBackingUrl,
  );
  await expect.poll(
    findPdfResponse,
    {
      message: `未捕获 PDF 资源响应: ${decodedPdfResourceUrl}`,
      timeout: 30000,
    },
  ).toBeTruthy();
  const pdfResponse = findPdfResponse();
  await expect(frame.locator('#viewer .page').first(), JSON.stringify({
    decodedPdfResourceUrl,
    pdfBackingUrl,
    pdfResponse,
  })).toBeVisible({ timeout: 30000 });
  const pageCount = await frame.locator('#viewer .page').count();
  const canvasSize = await frame.locator('#viewer .page canvas').first().evaluate((canvas) => ({
    width: (canvas as HTMLCanvasElement).width,
    height: (canvas as HTMLCanvasElement).height,
  }));
  const inspectedUrls = requestUrls.flatMap(url => [url, unwrapCorsResourceUrl(url)]);
  const doubleEncodedRequests = inspectedUrls.filter(url => /%25(?:e4|e5|e6|e7|e8|e9)/i.test(url));

  expect(pageCount, 'PDF.js 未渲染任何页').toBeGreaterThan(0);
  expect(canvasSize.width, 'PDF.js 首页 canvas 宽度为 0').toBeGreaterThan(0);
  expect(canvasSize.height, 'PDF.js 首页 canvas 高度为 0').toBeGreaterThan(0);
  expect(pdfBackingUrl).toContain('/file-preview/generated');
  expect(pdfBackingUrl).toContain(`file-${fileId}`);
  expect(pdfResponse?.status, `PDF 资源请求失败: ${JSON.stringify(pdfResponse)}`).toBe(200);
  expect(pdfResponse?.contentType).toContain('application/pdf');
  expect(doubleEncodedRequests, '出现中文文件名双重 URL 编码请求').toEqual([]);

  return {
    pageCount,
    pdfResourceUrl,
    pdfBackingUrl,
    pdfResponseStatus: pdfResponse?.status,
    pdfResponseContentType: pdfResponse?.contentType,
    doubleEncodedRequestCount: doubleEncodedRequests.length,
  };
}

function unwrapCorsResourceUrl(resourceUrl: string) {
  try {
    const parsedUrl = new URL(resourceUrl, frontendBaseURL);
    if (!parsedUrl.pathname.endsWith('/getCorsFile')) {
      return resourceUrl;
    }
    const urlPath = parsedUrl.searchParams.get('urlPath');
    return urlPath ? Buffer.from(urlPath, 'base64').toString('utf-8') : resourceUrl;
  } catch {
    return resourceUrl;
  }
}

async function inspectZipInnerPdfPreview(page: Page) {
  const entry = page.locator('#treeDemo a', { hasText: 'sample.pdf' });
  await expect(entry).toBeVisible({ timeout: 15000 });
  const responsePromise = page.waitForResponse(response => {
    const responseUrl = response.url();
    if (responseUrl.includes('/compressed-file') && responseUrl.includes('sample.pdf')) {
      return true;
    }
    if (!responseUrl.includes('/getCorsFile')) {
      return false;
    }
    const parsedUrl = new URL(responseUrl);
    const urlPath = parsedUrl.searchParams.get('urlPath');
    return Boolean(urlPath && Buffer.from(urlPath, 'base64').toString('utf-8').includes('/compressed-file'));
  }, { timeout: 20000 }).catch(() => null);

  await entry.click();

  const response = await responsePromise;
  if (!response) {
    return { ok: false, reason: '未捕获到 zip 内 PDF 文件读取请求' };
  }
  const contentType = response.headers()['content-type'] || '';
  return {
    ok: response.status() === 200 && contentType.includes('application/pdf'),
    reason: `status=${response.status()}, content-type=${contentType}`,
  };
}

async function inspectZipInnerTextPreview(page: Page) {
  const entry = page.locator('#treeDemo a', { hasText: 'readme.txt' });
  await expect(entry).toBeVisible({ timeout: 15000 });
  await entry.click();
  await expect(page.locator('.preview-panel.is-compact')).toBeVisible({ timeout: 10000 });
  await expect(page.locator('.preview-panel .panel-header')).toBeHidden({ timeout: 10000 });
  return { ok: true, reason: '非 PDF 文件预览已隐藏右侧 header' };
}

async function inspectZipPreviewLayout(page: Page) {
  const layout = await page.evaluate(() => {
    const workspace = document.querySelector('.workspace');
    const treePanel = document.querySelector('.tree-panel');
    const previewPanel = document.querySelector('.preview-panel');
    const title = document.querySelector('.preview-title-row .panel-title');
    const treeTitle = document.querySelector('.tree-panel .panel-title');
    if (!workspace || !treePanel || !previewPanel || !title || !treeTitle) {
      return { ok: false, reason: 'zip 预览布局元素缺失' };
    }
    const treeRect = treePanel.getBoundingClientRect();
    const previewRect = previewPanel.getBoundingClientRect();
    return {
      ok: Math.abs(treeRect.top - previewRect.top) < 8
        && previewRect.left >= treeRect.right - 2
        && previewRect.width > 320
        && treeTitle.textContent?.trim() === '目录'
        && title.textContent?.trim() === 'FileView',
      reason: `treeTitle=${treeTitle.textContent?.trim()}, title=${title.textContent?.trim()}, tree=${Math.round(treeRect.width)}x${Math.round(treeRect.height)}@${Math.round(treeRect.left)},${Math.round(treeRect.top)}, preview=${Math.round(previewRect.width)}x${Math.round(previewRect.height)}@${Math.round(previewRect.left)},${Math.round(previewRect.top)}`,
    };
  });
  return layout;
}

async function uploadFixture(request: APIRequestContext, token: string, item: PreviewCase) {
  const fileName = item.fileName || `mango-preview-live-${item.key}-${Date.now()}.${item.extension}`;
  const response = await request.post(api('/file/files'), {
    headers: authHeaders(token),
    multipart: {
      file: {
        name: fileName,
        mimeType: item.mimeType,
        buffer: item.create(),
      },
      purpose: 'preview-e2e',
      bizType: 'FILE_PREVIEW_E2E',
      bizId: `FILE_PREVIEW_E2E_${Date.now()}`,
    },
  });
  const body = await response.json();
  expect(response.status()).toBe(200);
  const responseBody = expectApiSuccess(body, `${item.key} 上传失败`);
  const data = expectData<FileUploadData>(responseBody.data, `${item.key} 上传响应缺少文件 ID`);
  return { fileName, fileId: String(data.id), body };
}

async function runPreviewCase(page: Page, request: APIRequestContext, token: string, item: PreviewCase): Promise<CaseResult> {
  const result: CaseResult = {
    key: item.key,
    fileName: '',
    uploadOk: false,
    previewLinkOk: false,
    previewPageOk: false,
    downloadOk: false,
    status: 'FAIL',
    reason: '',
  };
  const requestUrls: string[] = [];
  const responses: BrowserResponse[] = [];
  const requestListener = (browserRequest: { url: () => string }) => requestUrls.push(browserRequest.url());
  const responseListener = (response: Response) => responses.push({
    url: response.url(),
    status: response.status(),
    contentType: response.headers()['content-type'] || '',
  });
  page.on('request', requestListener);
  page.on('response', responseListener);
  try {
    const uploaded = await uploadFixture(request, token, item);
    result.fileName = uploaded.fileName;
    result.fileId = uploaded.fileId;
    result.uploadOk = true;

    const previewResponse = await request.get(api(`/file-preview/files/preview-link?fileId=${encodeURIComponent(uploaded.fileId)}`), {
      headers: authHeaders(token),
    });
    const previewBody = await previewResponse.json();
    expect(previewResponse.status()).toBe(200);
    const previewResponseBody = expectApiSuccess(previewBody, `${item.key} 获取预览链接失败`);
    result.previewLinkOk = true;
    const previewData = expectData<PreviewLinkData>(previewResponseBody.data, `${item.key} 预览响应缺少链接`);
    result.previewUrl = previewData.previewUrl;

    const pageInspection = await inspectPreviewPage(page, previewEntryUrl(result.previewUrl || ''));
    result.previewPageOk = pageInspection.ok;
    result.pageTitle = pageInspection.title;
    result.pageText = pageInspection.text;
    if (item.key === 'docx-mixed-name' && process.env.MANGO_E2E_OFFICE_ENABLED === 'true') {
      const pdfInspection = await inspectOfficePdf(page, uploaded.fileId, requestUrls, responses);
      result.pdfPageCount = pdfInspection.pageCount;
      result.pdfResourceUrl = pdfInspection.pdfResourceUrl;
      result.pdfBackingUrl = pdfInspection.pdfBackingUrl;
      result.pdfResponseStatus = pdfInspection.pdfResponseStatus;
      result.pdfResponseContentType = pdfInspection.pdfResponseContentType;
      result.doubleEncodedRequestCount = pdfInspection.doubleEncodedRequestCount;
      await page.screenshot({ path: officeScreenshotFile, fullPage: true });
    }
    if (item.key === 'zip') {
      const zipLayout = await inspectZipPreviewLayout(page);
      result.zipLayoutOk = zipLayout.ok;
      result.zipLayoutReason = zipLayout.reason;
      if (!zipLayout.ok) {
        result.previewPageOk = false;
        result.pageText = `${result.pageText || ''}\nzipLayout: ${zipLayout.reason}`;
      }
      const zipInnerPdf = await inspectZipInnerPdfPreview(page);
      result.zipInnerPdfOk = zipInnerPdf.ok;
      result.zipInnerPdfReason = zipInnerPdf.reason;
      if (!zipInnerPdf.ok) {
        result.previewPageOk = false;
        result.pageText = `${result.pageText || ''}\nzipInnerPdf: ${zipInnerPdf.reason}`;
      }
      const zipInnerText = await inspectZipInnerTextPreview(page);
      if (!zipInnerText.ok) {
        result.previewPageOk = false;
        result.pageText = `${result.pageText || ''}\nzipInnerText: ${zipInnerText.reason}`;
      }
      await page.screenshot({ path: screenshotFile, fullPage: true });
    }

    const downloadResponse = await request.get(api(`/file/files/download?id=${encodeURIComponent(uploaded.fileId)}`), {
      headers: authHeaders(token),
    });
    result.downloadOk = downloadResponse.status() === 200
      && !String(downloadResponse.headers()['content-type'] || '').includes('application/json')
      && (await downloadResponse.body()).length > 0;
    result.downloadContentType = downloadResponse.headers()['content-type'];

    const classified = classifyResult(item, result.pageText || '', result.previewPageOk, result.downloadOk);
    result.status = classified.status;
    result.reason = classified.reason;
  } catch (error) {
    result.status = item.expected === 'environment-blocked' ? 'BLOCKED' : 'FAIL';
    result.reason = error instanceof Error ? error.message : String(error);
  } finally {
    page.off('request', requestListener);
    page.off('response', responseListener);
    if (result.fileId) {
      await request.delete(api(`/file/files?id=${encodeURIComponent(result.fileId)}&reason=e2e-cleanup`), {
        headers: authHeaders(token),
      }).catch(() => undefined);
    }
  }
  return result;
}

test.describe.serial('文件预览多类型真实联调', () => {
  test('@p0 @file-preview txt、png、pdf、zip、xlsx、混合编码 docx 预览入口和下载链路', async ({ page, request }) => {
    test.setTimeout(120000);
    await page.setViewportSize({ width: 960, height: 720 });
    const token = await loginToken(request);
    const results: CaseResult[] = [];
    const selectedCase = process.env.MANGO_E2E_PREVIEW_CASE;
    const selectedCases = selectedCase ? cases.filter(item => item.key === selectedCase) : cases;
    expect(selectedCases, `未知预览用例: ${selectedCase}`).not.toHaveLength(0);
    for (const item of selectedCases) {
      results.push(await runPreviewCase(page, request, token, item));
    }

    mkdirSync(join(process.cwd(), 'e2e', '.tmp'), { recursive: true });
    writeFileSync(resultFile, `${JSON.stringify({
      apiBaseURL,
      frontendBaseURL,
      generatedAt: new Date().toISOString(),
      officePluginEnabled: process.env.MANGO_E2E_OFFICE_ENABLED === 'true',
      selectedCase: selectedCase || 'all',
      results,
    }, null, 2)}\n`, 'utf-8');

    const blockingFailures = results.filter(item => item.status === 'FAIL');
    expect(blockingFailures, JSON.stringify(results, null, 2)).toEqual([]);
  });
});
