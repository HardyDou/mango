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

/**
 * 登录页机构选择器当前没有关联 label，且 Element Plus 的已选值会遮挡原生 combobox。
 * 该组件内部兼容细节仅在 support 层处理，业务用例不依赖其 DOM 结构。
 */
export async function openUnlabelledElementPlusCombobox(scope: Locator | Page): Promise<void> {
  const combobox = scope.getByRole('combobox');
  await combobox.locator('xpath=ancestor::div[contains(@class, "el-select__wrapper")][1]').click();
}

export async function chooseElementPlusRadio(scope: Locator, name: string): Promise<void> {
  const radio = scope.getByRole('radio', { name });
  await radio.locator('xpath=ancestor::label[1]').click();
  await expect(radio).toBeChecked();
}

/** Element Plus 会隐藏 checkbox input；使用可点击的 label 保持真实用户交互。 */
export async function setElementPlusCheckbox(scope: Locator | Page, name: string, checked: boolean): Promise<void> {
  const checkbox = scope.getByRole('checkbox', { name, exact: true });
  if ((await checkbox.isChecked()) !== checked) {
    await checkbox.locator('xpath=ancestor::label[1]').click();
  }
  if (checked) await expect(checkbox).toBeChecked();
  else await expect(checkbox).not.toBeChecked();
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

/**
 * Element Plus 的父 treeitem 包含所有后代节点，只按 role 定位会同时命中后代 checkbox。
 * 限定到节点自身的 content 容器，避免业务用例依赖这段组件内部结构。
 */
export function elementPlusTreeItemCheckbox(tree: Locator, name: string): Locator {
  return tree
    .getByRole('treeitem', { name, exact: true })
    .locator(':scope > .el-tree-node__content')
    .getByRole('checkbox');
}

export async function chooseElementPlusOption(option: Locator): Promise<void> {
  await expect(option).toBeVisible();
  await option.click({ force: true });
}
