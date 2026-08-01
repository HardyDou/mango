import assert from 'node:assert/strict';
import test from 'node:test';

import { evaluateVuePageBaseline } from '../tools/check-frontend-page-baseline.mjs';

const listPage = `
<template>
  <MangoListPage>
    <template #search><MangoSearchPanel /></template>
    <MangoListPanel><el-table /><Pagination /></MangoListPanel>
  </MangoListPage>
</template>`;

test('accepts the current list page baseline', () => {
  assert.deepEqual(evaluateVuePageBaseline('frontend/packages/orders/src/views/orders/index.vue', listPage, 'A'), []);
});

test('rejects legacy list layouts and raw standard dialogs', () => {
  const failures = evaluateVuePageBaseline(
    'frontend/packages/orders/src/views/orders/index.vue',
    '<template><el-table /><el-dialog /></template>',
    'A',
  );
  assert.equal(failures.length, 5);
  assert.ok(failures.some(failure => failure.includes('MangoListPage')));
  assert.ok(failures.some(failure => failure.includes('MangoDialog')));
});

test('accepts typed exceptions with reviewable reasons', () => {
  const content = `
    <!-- mango-page-baseline-exception list: embedded comparison table is not a management list -->
    <!-- mango-page-baseline-exception dialog: third-party editor requires the native dialog contract -->
    <template><el-table /><el-dialog /></template>`;
  assert.deepEqual(evaluateVuePageBaseline('frontend/packages/report/src/views/report/index.vue', content, 'M'), []);
});

test('requires page shells for new or modified independent detail and form pages', () => {
  const detailFailures = evaluateVuePageBaseline(
    'frontend/packages/orders/src/views/orders/detail/index.vue',
    '<template><el-descriptions /></template>',
    'A',
  );
  assert.deepEqual(detailFailures.map(item => item.split('use ')[1]), ['MangoDetailPage', 'MangoPageSection']);

  const formFailures = evaluateVuePageBaseline(
    'frontend/packages/orders/src/views/orders/edit/index.vue',
    '<template><el-form /></template>',
    'M',
  );
  assert.deepEqual(formFailures.map(item => item.split('use ')[1]), ['MangoFormPage', 'MangoPageSection']);
});

test('does not force independent page shells onto modified drawers or ordinary pages', () => {
  assert.deepEqual(
    evaluateVuePageBaseline(
      'frontend/packages/orders/src/views/orders/index.vue',
      '<template><el-drawer><el-descriptions /></el-drawer></template>',
      'M',
    ),
    [],
  );
});
