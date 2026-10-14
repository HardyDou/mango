/**
 * Issue #1041 真实链路验收脚本（一次执行副本）。
 *
 * 前置：Mango 开发工作区前后端已通过 `mango dev start` 启动，且前端已加载修复后的
 * `@mango/file` 源码。脚本用真实 Chrome 登录管理端，在文件管理页上传一个 8 KB PDF，
 * 断言请求 `Content-Type` 为 multipart/form-data、后端返回 200，并保存截图与报告。
 *
 * 运行：node real-link-upload.mjs
 * 可选环境变量：E2E_BASE（默认 http://127.0.0.1:30045）、EVIDENCE_DIR（默认脚本所在目录）。
 */
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import { dirname, join, resolve } from 'node:path';
import { mkdirSync, writeFileSync } from 'node:fs';

const here = dirname(fileURLToPath(import.meta.url));
const require = createRequire(resolve(here, '../../../mango-ui/package.json'));
const { chromium } = require('@playwright/test');

const BASE = process.env.E2E_BASE || 'http://127.0.0.1:30045';
const OUT_DIR = process.env.EVIDENCE_DIR || here;
mkdirSync(OUT_DIR, { recursive: true });

const PDF_PATH = join('/tmp', `mango-1041-small-${Date.now()}.pdf`);
const bytes = Buffer.concat([
  Buffer.from('%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n'),
  Buffer.alloc(8 * 1024, 'A'),
  Buffer.from('\n%%EOF\n'),
]);
writeFileSync(PDF_PATH, bytes);

const browser = await chromium.launch({ channel: 'chrome' });
const page = await browser.newPage();

try {
  await page.goto(`${BASE}/#/login`, { waitUntil: 'domcontentloaded' });
  await page.getByPlaceholder('请输入用户名').fill('admin');
  await page.getByPlaceholder('请输入密码').fill('admin123');
  await page.getByPlaceholder('请输入密码').blur();
  const tenantSelect = page.locator('.tenant-select');
  if (await tenantSelect.isVisible({ timeout: 5000 }).catch(() => false)) {
    await tenantSelect.click();
    await page.getByRole('option', { name: /芒果集团/ }).click();
  }
  await page.locator('.login-btn').click();
  await page.waitForURL('**/#/home', { timeout: 20000 });

  await page.goto(`${BASE}/#/file/files`, { waitUntil: 'domcontentloaded' });
  const input = page.locator('input[type="file"]').first();
  await input.waitFor({ state: 'attached', timeout: 20000 });

  const responsePromise = page.waitForResponse(
    (response) =>
      response.request().method() === 'POST'
      && response.url().includes('/api/file/files')
      && !response.url().includes('/batch'),
    { timeout: 40000 },
  );
  await input.setInputFiles(PDF_PATH);
  const response = await responsePromise;
  const body = await response.json().catch(() => null);
  await page.waitForTimeout(1500);

  const screenshot = 'real-link-upload.png';
  await page.screenshot({ path: join(OUT_DIR, screenshot), fullPage: true });

  const contentType = response.request().headers()['content-type'] ?? null;
  const multipart = /multipart\/form-data/i.test(contentType || '');
  const passed = response.status() === 200 && (body?.success === true || body?.code === 200) && multipart;

  const report = {
    generatedAt: new Date().toISOString(),
    environment: {
      frontend: BASE,
      backend: process.env.E2E_API_BASE || 'http://127.0.0.1:18045',
      database: process.env.MANGO_DB_NAME || 'mango_dev_mango_045',
      tenant: '芒果集团',
      user: 'admin',
    },
    scenario: '文件管理页上传 8 KB PDF，低于 20 MiB 分片阈值，走普通上传 fileApi.upload',
    file: { path: PDF_PATH, sizeBytes: bytes.length },
    request: { method: 'POST', url: response.request().url(), contentType },
    response: {
      status: response.status(),
      success: body?.success ?? null,
      code: body?.code ?? null,
      fileId: body?.data?.id ?? null,
      fileName: body?.data?.fileName ?? null,
      contentType: body?.data?.contentType ?? null,
      fileSize: body?.data?.fileSize ?? null,
    },
    screenshot,
    result: passed ? 'PASS' : 'FAIL',
  };
  writeFileSync(join(OUT_DIR, 'real-link-upload.json'), `${JSON.stringify(report, null, 2)}\n`);
  console.log(JSON.stringify(report, null, 2));
  process.exitCode = passed ? 0 : 1;
} finally {
  await browser.close();
}
