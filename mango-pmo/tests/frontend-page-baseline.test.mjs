import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';

import {
  evaluateVuePageBaseline,
  formatFrontendPageBaselineFailures,
  runFrontendPageBaselineCli,
} from '../tools/check-frontend-page-baseline.mjs';

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

test('accepts a whole-page exception for every page baseline check', () => {
  const content = `
    <!-- mango-page-baseline-exception all: embedded workflow canvas owns its complete page layout -->
    <template><el-table /><el-dialog /><el-descriptions /><el-form /></template>`;
  assert.deepEqual(
    evaluateVuePageBaseline('frontend/packages/workflow/src/views/detail/form/index.vue', content, 'M'),
    [],
  );
});

test('rejects whole-page exceptions with a missing or short reason', () => {
  for (const exception of [
    '<!-- mango-page-baseline-exception all: -->',
    '<!-- mango-page-baseline-exception all: too short -->',
  ]) {
    const content = `${exception}
      <template><el-table /><el-dialog /><el-descriptions /><el-form /></template>`;
    assert.equal(
      evaluateVuePageBaseline('frontend/packages/workflow/src/views/detail/form/index.vue', content, 'M').length,
      9,
    );
  }
});

test('failure output explains typed and whole-page exceptions', () => {
  const output = formatFrontendPageBaselineFailures(['frontend/views/index.vue: list page must use MangoListPage']);
  assert.match(output, /mango-page-baseline-exception <list\|detail\|form\|dialog>:/u);
  assert.match(output, /mango-page-baseline-exception all:/u);
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

test('explicit project configuration disables only the frontend page baseline checker', t => {
  const project = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-disabled-page-baseline-'));
  t.after(() => fs.rmSync(project, { recursive: true, force: true }));
  fs.writeFileSync(path.join(project, 'mango.config.json'), JSON.stringify({
    pmoChecks: { frontendPageBaseline: false },
  }));

  assert.equal(
    runFrontendPageBaselineCli(['--base', 'missing-base', '--head', 'missing-head'], { repositoryRoot: project }),
    0,
  );
});
