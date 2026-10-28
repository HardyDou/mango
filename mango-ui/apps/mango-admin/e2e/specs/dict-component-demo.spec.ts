import { expect, test, type Page } from '@playwright/test';

async function loginPage(page: Page) {
  await page.goto('/#/login');
  await page.getByPlaceholder('请输入用户名', { exact: true }).fill('admin');
  await page.getByPlaceholder('请输入密码', { exact: true }).fill('admin123');
  await page.getByPlaceholder('请输入密码', { exact: true }).blur();
  await page.getByRole('button', { name: /^登\s*录$/ }).click();
  await page.waitForURL('**/#/home', { timeout: 15000 });
}

test('@p1 @system 字典组件直接使用统一 DictSelect 展示 FLAT 和 TREE', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 1200 });
  await loginPage(page);
  await page.goto('/#/components/dict');

  await expect(page.locator('[data-page="develop-dict-component"]')).toBeVisible({ timeout: 15000 });
  await expect(page.locator('[data-action="flat-single-select"]')).toBeVisible();
  await expect(page.locator('[data-action="flat-multiple-select"]')).toBeVisible();
  await expect(page.locator('[data-action="tree-single-select"]')).toBeVisible();
  await expect(page.locator('[data-action="tree-multiple-select"]')).toBeVisible();
  await expect(page.locator('[data-action="tree-single-select"]')).toContainText('纸张', { timeout: 15000 });
  await expect(page.locator('[data-action="tree-display-select"]')).toContainText('办公用品 / 纸张', { timeout: 15000 });
  await expect(page.locator('[data-action="display-flat-single"]')).toHaveText('启用', { timeout: 15000 });
  await expect(page.locator('[data-action="display-flat-multi"]')).toContainText('禁用');
  await expect(page.locator('[data-action="display-flat-tag"] .el-tag')).toHaveCount(2);
  await expect(page.locator('[data-action="display-flat-empty"]')).toHaveText('-');
  await expect(page.locator('[data-action="display-tree-label"]')).toHaveText('纸张');
  await expect(page.locator('[data-action="display-tree-path"]')).toContainText('办公用品 / 纸张');

  await expect(page.locator('[data-action="group-order-display"]')).toHaveText('值1', { timeout: 15000 });
  await expect(page.locator('[data-action="group-after-sale-display"]')).toHaveText('值4');
  await expect(page.locator('[data-action="group-narrow-display"]')).toHaveText('值2');

  await page.locator('[data-action="group-order-select"] .el-select__wrapper').click();
  await expect(page.locator('.el-select-dropdown:visible .el-select-dropdown__item')).toHaveCount(3);
  await expect(page.locator('.el-select-dropdown:visible .el-select-dropdown__item').nth(2)).toHaveText('值3');
  await page.keyboard.press('Escape');

  await expect(page.getByText('仅叶子节点可选', { exact: true })).toBeVisible();
  await expect(page.getByText('system_demo_tree', { exact: true })).toBeVisible();

  // 切换选择策略时会重置已选值，便于从空选择开始验证半选中状态
  await page.locator('[data-action="toggle-tree-leaf-only"]').click();
  await page.waitForTimeout(300);
  await page.locator('[data-action="tree-multiple-select"] .el-select__wrapper').click();
  const penCheckbox = page.locator('[role="treeitem"][data-key="pen"]:visible .el-checkbox');
  await penCheckbox.scrollIntoViewIfNeeded();
  const checkboxBox = await penCheckbox.boundingBox();
  expect(checkboxBox).not.toBeNull();
  await page.mouse.click(checkboxBox!.x + checkboxBox!.width / 2, checkboxBox!.y + checkboxBox!.height / 2);
  await expect(
    page.locator('[role="treeitem"][data-key="office_supplies"] .el-checkbox__input.is-indeterminate').first(),
  ).toBeAttached({ timeout: 5000 });
  await page.keyboard.press('Escape');
});
