import assert from 'node:assert/strict';
import test from 'node:test';
import { analyzeE2eSpecSource } from './e2e-selector-governance-lib.mjs';

test('accepts business-semantic Playwright selectors', () => {
  const source = [
    "await page.getByRole('button', { name: '保存' }).click()",
    "await page.locator('[data-action=publish]').click()",
  ].join('\n');
  assert.deepEqual(analyzeE2eSpecSource(source, 'good.spec.ts'), []);
});

test('rejects vendor classes, positional selectors, fixed waits and forced clicks in specs', () => {
  const source = [
    "page.locator('.el-dialog')",
    'rows.nth(2)',
    'page.waitForTimeout(500)',
    'button.click({ force: true })',
  ].join('\n');
  assert.deepEqual(
    analyzeE2eSpecSource(source, 'bad.spec.ts').map(({ rule, line }) => ({ rule, line })),
    [
      { rule: 'element-plus-internal-class', line: 1 },
      { rule: 'positional-nth', line: 2 },
      { rule: 'fixed-timeout', line: 3 },
      { rule: 'forced-click', line: 4 },
    ],
  );
});
