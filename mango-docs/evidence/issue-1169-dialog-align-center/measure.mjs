import { chromium } from '@playwright/test';
import { mkdirSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const label = process.env.LAB_LABEL ?? 'unlabeled';
const port = Number(process.env.LAB_PORT ?? 5199);
const shotDir = new URL('./shots/', import.meta.url).pathname;
mkdirSync(shotDir, { recursive: true });

const round = (n) => Math.round(n * 100) / 100;

const measure = () => {
  const round = (n) => Math.round(n * 100) / 100;
  const dialog = [...document.querySelectorAll('.mango-dialog.el-dialog')].find(
    (el) => el.getBoundingClientRect().width > 0 && el.getBoundingClientRect().height > 0,
  );
  const overlay = dialog?.closest('.el-overlay-dialog');
  const body = dialog?.querySelector('.mango-dialog__body');
  if (!overlay || !dialog || !body) return null;
  const o = overlay.getBoundingClientRect();
  const d = dialog.getBoundingClientRect();
  const cs = getComputedStyle(dialog);
  return {
    overlay: { w: o.width, h: o.height, x: o.left, y: o.top, display: getComputedStyle(overlay).display },
    dialog: {
      w: round(d.width),
      h: round(d.height),
      x: round(d.left),
      y: round(d.top),
      margin: cs.margin,
      maxHeight: cs.maxHeight,
      offsetFromOverlayLeft: round(d.left - o.left),
      offsetFromOverlayRight: round(o.right - d.right),
      offsetFromOverlayTop: round(d.top - o.top),
      offsetFromOverlayBottom: round(o.bottom - d.bottom),
    },
    body: {
      clientHeight: body.clientHeight,
      scrollHeight: body.scrollHeight,
      contentFits: body.scrollHeight <= body.clientHeight + 1,
    },
  };
};

const browser = await chromium.launch({ channel: 'chrome' });
const results = {};

for (const viewport of [
  { width: 2560, height: 1415 },
  { width: 1500, height: 1000 },
]) {
  const context = await browser.newContext({ viewport });
  const page = await context.newPage();
  const consoleErrors = [];
  page.on('console', (msg) => {
    if (msg.type() === 'error' || msg.type() === 'warning') consoleErrors.push(msg.text());
  });
  await page.goto(`http://localhost:${port}/`, { waitUntil: 'load' });

  const key = `${viewport.width}x${viewport.height}`;

  for (const [scenario, buttonId] of [
    ['short', '#open-short'],
    ['tall', '#open-tall'],
  ]) {
    await page.click(buttonId);
    await page.waitForSelector('.mango-dialog.el-dialog:visible');
    await page.waitForTimeout(600);
    const data = await page.evaluate(measure);
    await page.screenshot({
      path: join(shotDir, `${label}-${scenario}-${key}.png`),
      fullPage: false,
    });
    results[`${scenario}-${key}`] = data;
    await page.keyboard.press('Escape');
    await page.waitForSelector('.mango-dialog.el-dialog:visible', { state: 'detached' }).catch(() => {});
    await page.waitForTimeout(300);
  }

  // draggable dialog: verify drag still works and keeps inline margin 0
  await page.click('#open-drag');
  await page.waitForSelector('.mango-dialog.el-dialog:visible');
  await page.waitForTimeout(600);
  const before = await page.evaluate(measure);
  const header = await page.locator('.mango-dialog.el-dialog:visible .mango-dialog__header').first().elementHandle();
  const box = await header.boundingBox();
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
  await page.mouse.down();
  await page.mouse.move(box.x + box.width / 2 - 120, box.y + box.height / 2 - 80, { steps: 8 });
  await page.mouse.up();
  await page.waitForTimeout(300);
  const after = await page.evaluate(measure);
  await page.screenshot({ path: join(shotDir, `${label}-drag-${key}.png`) });
  results[`drag-${key}`] = { before, after };
  results[`console-${key}`] = consoleErrors;

  await context.close();
}

await browser.close();
writeFileSync(new URL(`./result-${label}.json`, import.meta.url), JSON.stringify(results, null, 2));
console.log(JSON.stringify(results, null, 2));
