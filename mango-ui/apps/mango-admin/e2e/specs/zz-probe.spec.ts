import { test, type Page } from '@playwright/test';
async function login(page: Page) {
  await page.goto('/#/login');
  await page.getByPlaceholder('请输入用户名').fill('admin');
  await page.getByPlaceholder('请输入密码').fill('admin123');
  await page.getByPlaceholder('请输入密码').blur();
  await page.getByRole('button', { name: '登录', exact: true }).click();
  await page.waitForURL('**/#/home');
}
test('probe save roundtrip', async ({ page }) => {
  await login(page);
  await page.goto('/#/information/forms/2111714759159914497/config');
  await page.waitForTimeout(2500);
  const before = await page.locator('.information-canvas-field').evaluateAll((els) =>
    els.map((e) => (e as HTMLElement).innerText.replace(/\s+/g, ' ').trim()),
  );
  console.log('CANVAS_BEFORE:', JSON.stringify(before));
  // add a select field
  await page.locator('.information-field-option', { hasText: '下拉选择' }).click();
  await page.waitForTimeout(300);
  // edit the new field label
  const label = page.locator('input[data-field-config="label"]');
  await label.fill('学历');
  const code = page.locator('input[data-field-config="code"]');
  await code.fill('education');
  const options = page.locator('.information-config-section textarea');
  await options.last().fill('本科=bachelor\n硕士=master');
  await page.waitForTimeout(200);
  const after = await page.locator('.information-canvas-field').evaluateAll((els) =>
    els.map((e) => (e as HTMLElement).innerText.replace(/\s+/g, ' ').trim()),
  );
  console.log('CANVAS_AFTER_EDIT:', JSON.stringify(after));
  await page.getByRole('button', { name: '保存配置' }).click();
  await page.waitForTimeout(2500);
  const saved = await page.locator('.information-canvas-field').evaluateAll((els) =>
    els.map((e) => (e as HTMLElement).innerText.replace(/\s+/g, ' ').trim()),
  );
  console.log('CANVAS_AFTER_SAVE:', JSON.stringify(saved));
  const bodyText = (await page.locator('.information-builder__config').innerText()).replace(/\s+/g, ' ').slice(0, 300);
  console.log('CONFIG_PANEL_AFTER_SAVE:', bodyText);
});
