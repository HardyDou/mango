import assert from 'node:assert/strict';
import test from 'node:test';

import {
  inspectProjectPullRequestTemplate,
  synchronizeProjectPullRequestTemplate,
} from '../src/pmo-project-template.mjs';

const canonicalTemplate = `## Summary

-

## Risk / Verification

- Requirement impact: current
- Solution risk: current

## Validation

- Result:
`;

test('missing project template installs the complete canonical template', () => {
  const result = synchronizeProjectPullRequestTemplate('', canonicalTemplate, { targetExists: false });
  assert.equal(result.action, 'add');
  assert.equal(result.content, canonicalTemplate);
});

test('sync replaces only the managed risk section and preserves business content', () => {
  const existing = `## Summary

- business-specific summary

## Risk / Verification

- Selected verification: STATIC

## Deployment

- business rollout instructions
`;
  const result = synchronizeProjectPullRequestTemplate(existing, canonicalTemplate, { targetExists: true });
  assert.equal(result.action, 'update');
  assert.match(result.content, /business-specific summary/);
  assert.match(result.content, /business rollout instructions/);
  assert.match(result.content, /- Requirement impact: current/);
  assert.doesNotMatch(result.content, /Selected verification/);
});

test('sync inserts a missing managed section before Validation', () => {
  const existing = `## Summary

- business summary

## Validation

- business validation
`;
  const result = synchronizeProjectPullRequestTemplate(existing, canonicalTemplate, { targetExists: true });
  assert.equal(result.action, 'update');
  assert.ok(result.content.indexOf('## Risk / Verification') < result.content.indexOf('## Validation'));
  assert.match(result.content, /business validation/);
});

test('duplicate managed sections fail closed without replacement content', () => {
  const existing = `## Risk / Verification

- first

## Notes

## Risk / Verification

- second
`;
  const result = synchronizeProjectPullRequestTemplate(existing, canonicalTemplate, { targetExists: true });
  assert.equal(result.action, 'error');
  assert.equal(result.content, undefined);
  assert.match(result.reason, /exactly one/);
});

test('inspection reports missing and drifted project contract sections', () => {
  assert.match(
    inspectProjectPullRequestTemplate('', canonicalTemplate, { targetExists: false }).errors.join('\n'),
    /missing/,
  );
  assert.match(
    inspectProjectPullRequestTemplate('## Risk / Verification\n\n- old\n', canonicalTemplate).errors.join('\n'),
    /differs/,
  );
  assert.deepEqual(inspectProjectPullRequestTemplate(canonicalTemplate, canonicalTemplate).errors, []);
});

test('sync recognizes CRLF headings and preserves the project line-ending style', () => {
  const existing = `## Summary

- business

## Risk / Verification

- Selected verification: STATIC

## Validation

- result
`.replace(/\n/gu, '\r\n');
  const result = synchronizeProjectPullRequestTemplate(existing, canonicalTemplate, { targetExists: true });
  assert.equal(result.action, 'update');
  assert.doesNotMatch(result.content, /Selected verification/);
  assert.equal(result.content.replace(/\r\n/gu, '').includes('\n'), false);
  assert.deepEqual(inspectProjectPullRequestTemplate(result.content, canonicalTemplate).errors, []);
});
