import assert from 'node:assert/strict';
import test from 'node:test';

import {
  addedWorkflowCheckers,
  extractWorkflowCheckers,
  releaseSectionForVersion,
  validateRequiredCheckCoverage,
  validateRootReleaseNotes,
} from './check-release-notes.mjs';

const frontendChecker = 'check-frontend-page-baseline.mjs';
const completeEntry = `
### PMO Required Checks

- \`${frontendChecker}\`
  - Migration: migrate changed management pages to the current shells.
  - Exception: record a typed and reviewable exception reason.
  - Verify: \`node business-pmo/mango-baseline/tools/${frontendChecker} --base base --head head\`.
`;

test('extracts required-check tools from generated PMO workflows', () => {
  const workflow = `
run: >-
  node business-pmo/mango-baseline/tools/check-document-set.mjs
run: >-
  node business-pmo/mango-baseline/tools/${frontendChecker}
`;
  assert.deepEqual([...extractWorkflowCheckers(workflow)].sort(), ['check-document-set.mjs', frontendChecker]);
});

test('identifies checkers added since the previous PMO release', () => {
  const previous = ['node business-pmo/mango-baseline/tools/check-document-set.mjs'];
  const current = [
    `node business-pmo/mango-baseline/tools/check-document-set.mjs\n` +
      `node business-pmo/mango-baseline/tools/${frontendChecker}`,
  ];
  assert.deepEqual(addedWorkflowCheckers(previous, current), [frontendChecker]);
});

test('accepts complete migration, exception and verification coverage', () => {
  assert.deepEqual(
    validateRequiredCheckCoverage(completeEntry, {
      label: 'fixture changelog',
      checkers: [frontendChecker],
    }),
    [],
  );
});

test('fails when a new required check omits consumer adaptation fields', () => {
  const errors = validateRequiredCheckCoverage(
    `
### PMO Required Checks

- \`${frontendChecker}\`
  - Migration: migrate changed pages.
`,
    {
      label: 'fixture changelog',
      checkers: [frontendChecker],
    },
  );
  assert.equal(errors.length, 2);
  assert.ok(errors.some((error) => error.includes('Exception')));
  assert.ok(errors.some((error) => error.includes('Verify')));
});

test('fails when consumer notes omit the PMO required-check section', () => {
  const errors = validateRequiredCheckCoverage('### Upgrade Notes\n\n- upgrade PMO.', {
    label: 'fixture changelog',
    checkers: [frontendChecker],
  });
  assert.deepEqual(errors, [
    'fixture changelog must contain "### PMO Required Checks" for newly added PMO required checks.',
  ]);
});

test('selects an exact package version section without prefix collisions', () => {
  const changelog = `
## 1.0.94 - 2026-08-01

current

## 1.0.9 - 2026-06-01

old
`;
  assert.match(releaseSectionForVersion(changelog, '1.0.94'), /current/u);
  assert.match(releaseSectionForVersion(changelog, '1.0.9'), /old/u);
});

test('validates the target root release after a newer release is added', () => {
  const changelog = `
## v2026.08.01-cli-1.0.95-release

@mango/cli 1.0.95

### Published Packages
### Upgrade Notes
### Verification

## v2026.08.01-pmo-1.3.8-cli-1.0.94-release

@mango/cli 1.0.94

### Published Packages
${completeEntry}
### Upgrade Notes
### Verification
`;
  assert.deepEqual(
    validateRootReleaseNotes(changelog, {
      packageName: '@mango/cli',
      version: '1.0.94',
      requiredPmoChecks: [frontendChecker],
    }),
    [],
  );
});
