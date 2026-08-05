import { expect, test } from '@playwright/test';

test('@p0 @admin-shell 缺少原生 randomUUID 时由框架在启动阶段安全补齐', async ({ page }) => {
  await page.addInitScript(() => {
    Object.defineProperty(Crypto.prototype, 'randomUUID', {
      configurable: true,
      value: undefined,
      writable: true,
    });
  });
  await page.route('**://*/api/**', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, success: true, data: [] }),
    }),
  );

  const randomUuidErrors: string[] = [];
  page.on('pageerror', (error) => {
    if (error.message.includes('randomUUID')) {
      randomUuidErrors.push(error.message);
    }
  });

  await page.goto('/#/login');
  await expect(page.locator('#app')).toBeVisible();

  const compatibility = await page.evaluate(() => {
    const randomUUID = window.crypto.randomUUID;
    return {
      available: typeof randomUUID === 'function',
      uuid: typeof randomUUID === 'function' ? randomUUID.call(window.crypto) : '',
    };
  });

  expect(compatibility.available).toBe(true);
  expect(compatibility.uuid).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/u);
  expect(randomUuidErrors).toEqual([]);
});
