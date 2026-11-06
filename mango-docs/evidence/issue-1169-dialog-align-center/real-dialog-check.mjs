import { chromium } from '@playwright/test';
import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const baseUrl = process.env.APP_URL ?? 'http://localhost:30017';
const shotDir = new URL('./shots-real/', import.meta.url).pathname;
mkdirSync(shotDir, { recursive: true });

const geometry = () => {
  const dialog = [...document.querySelectorAll('.mango-dialog.el-dialog')].find(
    (el) => el.getBoundingClientRect().width > 0 && el.getBoundingClientRect().height > 0,
  );
  if (!dialog) return null;
  const overlay = dialog.closest('.el-overlay-dialog');
  const o = overlay.getBoundingClientRect();
  const d = dialog.getBoundingClientRect();
  const cs = getComputedStyle(dialog);
  const body = dialog.querySelector('.mango-dialog__body');
  const round = (n) => Math.round(n * 100) / 100;
  return {
    title: dialog.querySelector('.mango-dialog__title')?.textContent?.trim(),
    overlayDisplay: getComputedStyle(overlay).display,
    dialog: { w: round(d.width), h: round(d.height), x: round(d.left), y: round(d.top), margin: cs.margin },
    centeredX: Math.abs(d.left - o.left - (o.right - d.right)) <= 1,
    centeredY: Math.abs(d.top - o.top - (o.bottom - d.bottom)) <= 1,
    body: body ? { clientHeight: body.clientHeight, scrollHeight: body.scrollHeight } : null,
  };
};

async function login(page) {
  await page.goto(`${baseUrl}/#/login`, { waitUntil: 'load' });
  await page.getByPlaceholder('请输入用户名', { exact: true }).fill('admin');
  await page.getByPlaceholder('请输入密码', { exact: true }).fill('admin123');
  await page.getByRole('button', { name: '登录' }).first().click();
  await page.waitForFunction(() => !location.hash.includes('/login'), null, { timeout: 30000 });
  await page.waitForTimeout(2500);
}

const browser = await chromium.launch({ channel: 'chrome' });
const results = {};

for (const viewport of [
  { width: 1500, height: 1000 },
  { width: 2560, height: 1415 },
]) {
  const context = await browser.newContext({ viewport });
  const page = await context.newPage();
  const consoleMessages = [];
  page.on('console', (msg) => {
    if (msg.type() === 'error' || msg.type() === 'warning') consoleMessages.push(`${msg.type()}: ${msg.text()}`);
  });
  const key = `${viewport.width}x${viewport.height}`;

  await login(page);
  await page.goto(`${baseUrl}/#/system/role`, { waitUntil: 'load' });
  await page.waitForTimeout(3000);
  const pageReady = await page.getByRole('button', { name: '新增角色' }).isVisible().catch(() => false);
  if (!pageReady) {
    results[key] = { error: 'role page not ready', url: page.url(), bodyText: (await page.textContent('body'))?.slice(0, 400) };
    await page.screenshot({ path: join(shotDir, `real-role-page-${key}.png`) });
    await context.close();
    continue;
  }

  await page.getByRole('button', { name: '新增角色' }).click();
  await page.waitForSelector('.mango-dialog.el-dialog:visible');
  await page.waitForTimeout(900);
  const data = await page.evaluate(geometry);
  await page.screenshot({ path: join(shotDir, `real-role-create-dialog-${key}.png`) });
  results[key] = { dialog: data, console: consoleMessages.slice(0, 5) };
  await context.close();
}

await browser.close();
writeFileSync(new URL('./result-real.json', import.meta.url), JSON.stringify(results, null, 2));
console.log(JSON.stringify(results, null, 2));
