import { expect, type Locator, type Page } from '@playwright/test';

// Element Plus teleports overlays outside their business container and does not expose
// stable roles for every state. Keep the unavoidable vendor selectors in this adapter.
export function formItem(scope: Locator, label: string) {
  return scope.locator('.el-form-item').filter({
    has: scope
      .page()
      .locator('.el-form-item__label')
      .filter({ hasText: new RegExp(`^\\*?\\s*${escapeRegExp(label)}$`) }),
  });
}

export function firstDialog(page: Page) {
  return page.locator('.el-dialog:visible').last();
}

export async function waitCmsReady(page: Page) {
  await expect(page.locator('.cms-panel')).toBeVisible({ timeout: 15000 });
  await expect
    .poll(async () => page.locator('.cms-panel .el-loading-mask:visible').count(), { timeout: 15000 })
    .toBe(0);
}

async function closeDropdowns(page: Page) {
  const dropdowns = page.locator('.el-select-dropdown:visible, .el-tree-select__popper:visible');
  if ((await dropdowns.count()) === 0) return;
  const dialogHeader = page.locator('.el-dialog:visible .el-dialog__header').last();
  if (await dialogHeader.count()) {
    await clickThroughTransientOverlay(dialogHeader, { position: { x: 12, y: 12 } });
  } else {
    await clickThroughTransientOverlay(page.locator('main h2').first());
  }
  await expect.poll(async () => dropdowns.count(), { timeout: 5000 }).toBe(0);
}

export async function selectValue(scope: Locator, label: string, option: string | RegExp) {
  const page = scope.page();
  await closeDropdowns(page);
  const select = formItem(scope, label).locator('.el-select, .el-tree-select').first();
  await expect(select, `${label} 下拉不存在`).toBeVisible({ timeout: 10000 });
  await select.scrollIntoViewIfNeeded();
  await clickThroughTransientOverlay(select);
  const dropdown = page.locator('.el-select-dropdown:visible, .el-tree-select__popper:visible').last();
  await expect(dropdown, `${label} 下拉面板未打开`).toBeVisible({ timeout: 10000 });
  const target = dropdown.getByRole('option', { name: option, exact: typeof option === 'string' }).first();
  await expect(target, `${label} 选项不存在: ${String(option)}`).toBeVisible({ timeout: 10000 });
  await target.scrollIntoViewIfNeeded();
  await clickThroughTransientOverlay(target);
  await closeDropdowns(page);
}

export async function selectValues(scope: Locator, label: string, options: Array<string | RegExp>) {
  const page = scope.page();
  await closeDropdowns(page);
  const select = formItem(scope, label).locator('.el-select, .el-tree-select').first();
  await expect(select, `${label} 下拉不存在`).toBeVisible({ timeout: 10000 });
  await clickThroughTransientOverlay(select);
  const dropdown = page.locator('.el-select-dropdown:visible, .el-tree-select__popper:visible').last();
  await expect(dropdown, `${label} 下拉面板未打开`).toBeVisible({ timeout: 10000 });
  for (const option of options) {
    const target = dropdown.getByRole('option', { name: option, exact: typeof option === 'string' }).first();
    await expect(target, `${label} 选项不存在: ${String(option)}`).toBeVisible({ timeout: 10000 });
    await clickThroughTransientOverlay(target);
  }
  await closeDropdowns(page);
}

export async function checkButton(scope: Locator, label: string, option: string) {
  const button = formItem(scope, label)
    .locator('.el-checkbox-button')
    .filter({ hasText: new RegExp(`^\\s*${escapeRegExp(option)}\\s*$`) })
    .first();
  await expect(button).toBeVisible({ timeout: 10000 });
  if (!(await button.evaluate((element) => element.classList.contains('is-checked')))) await button.click();
}

export async function expectUploadSuccess(item: Locator, label: string, responseBody: string) {
  await expect
    .poll(
      async () => {
        const successCount = await item.locator('.el-upload-list__item.is-success').count();
        if (successCount > 0) return 'success';
        const messages = await item
          .page()
          .locator('.el-message:visible, .el-form-item__error:visible')
          .allTextContents();
        return messages.join(' | ') || 'waiting';
      },
      {
        timeout: 15000,
        message: `${label} 上传后未显示成功状态，接口响应：${responseBody.slice(0, 800)}`,
      },
    )
    .toBe('success');
}

export async function confirmDelete(page: Page) {
  const box = page.locator('.el-message-box').last();
  await expect(box).toBeVisible({ timeout: 10000 });
  await box.getByRole('button', { name: /^(确定|OK)$/ }).click();
  await expect(box).toBeHidden({ timeout: 10000 });
}

export async function expectToast(page: Page, text: string | RegExp) {
  await expect(page.locator('.el-message:visible').filter({ hasText: text }).last()).toBeVisible({ timeout: 10000 });
}

export async function clickThroughTransientOverlay(
  locator: Locator,
  options: { position?: { x: number; y: number } } = {},
) {
  await locator.click({ ...options, force: true });
}

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
