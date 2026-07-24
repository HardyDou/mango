import { expect, type Locator, type Page } from '@playwright/test';

/**
 * Element Plus 的 select、radio 和 switch 会把语义化 input 隐藏在可点击包装层中，
 * 动画弹层也可能拦截 Playwright 的标准点击。将这些不可避免的组件内部兼容定位
 * 集中在 support，避免业务用例依赖 Element Plus 的 DOM 结构。
 */
export async function openElementPlusSelect(scope: Locator | Page, label: string): Promise<void> {
  const selectInput = scope.getByLabel(label, { exact: true });
  await selectInput.locator('xpath=ancestor::div[contains(@class, "el-select__wrapper")][1]').click();
}

export async function chooseElementPlusRadio(scope: Locator, name: string): Promise<void> {
  const radio = scope.getByRole('radio', { name });
  await radio.locator('xpath=ancestor::label[1]').click();
  await expect(radio).toBeChecked();
}

export async function setElementPlusSwitch(scope: Locator | Page, name: string, checked: boolean): Promise<void> {
  const switchInput = scope.getByRole('switch', { name });
  const current = (await switchInput.getAttribute('aria-checked')) === 'true';
  if (current !== checked) {
    await switchInput
      .locator('xpath=ancestor::*[contains(concat(" ", normalize-space(@class), " "), " el-switch ")][1]')
      .click();
  }
  await expect(switchInput).toHaveAttribute('aria-checked', String(checked));
}

export async function chooseElementPlusOption(option: Locator): Promise<void> {
  await expect(option).toBeVisible();
  await option.click({ force: true });
}
