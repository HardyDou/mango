import { expect, type Locator, type Page } from '@playwright/test';

/**
 * Domain 页面当前由 Element Plus 表格和全局消息组件渲染，尚未暴露业务语义锚点。
 * 将不可避免的组件内部定位集中在 support，避免业务用例直接依赖布局结构。
 */
export async function selectLoginTenant(page: Page, tenantName: RegExp) {
  await page.locator('.tenant-select').click();
  await page.getByRole('option', { name: tenantName }).click();
}

export function domainTable(page: Page): Locator {
  return page.locator('.el-table');
}

export function domainRow(page: Page, text: string): Locator {
  return domainTable(page).locator('.el-table__row', { hasText: text }).first();
}

export async function expectLatestMessage(page: Page, message: string) {
  await expect(page.locator('.el-message__content', { hasText: message }).last())
    .toBeVisible({ timeout: 10000 });
}

export async function confirmLatestMessageBox(page: Page) {
  const messageBox = page.locator('.el-message-box').last();
  await messageBox.getByRole('button', { name: /^(OK|确定)$/ }).click();
}
