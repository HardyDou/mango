import assert from 'node:assert/strict';
import test from 'node:test';
import { projectPmoPluginVersion, projectPmoVersionedFile } from './release-pmo-plugin-lib.mjs';

test('projects the PMO plugin manifest version deterministically', () => {
  const source = `${JSON.stringify({ name: 'mango-pmo', version: '1.3.16', skills: './skills/' }, null, 2)}\n`;
  assert.equal(
    projectPmoPluginVersion(source, '1.3.16', '1.4.0'),
    `${JSON.stringify({ name: 'mango-pmo', version: '1.4.0', skills: './skills/' }, null, 2)}\n`,
  );
});

test('rejects a stale PMO plugin source version', () => {
  assert.throws(
    () => projectPmoPluginVersion('{"name":"mango-pmo","version":"1.3.15"}\n', '1.3.16', '1.4.0'),
    /source version 1\.3\.15 != 1\.3\.16/u,
  );
});

test('projects contract current and historical PMO versions', () => {
  const source = `${JSON.stringify({ metadata: { fixed: { pmoVersion: '1.3.16' }, historicalPmoVersions: ['1.3.15'] } }, null, 2)}\n`;
  assert.equal(
    projectPmoVersionedFile('mango-pmo/contracts/business-requirements.json', source, '1.3.16', '1.4.0'),
    `${JSON.stringify({ metadata: { fixed: { pmoVersion: '1.4.0' }, historicalPmoVersions: ['1.3.15', '1.3.16'] } }, null, 2)}\n`,
  );
});

test('projects the lifecycle example and advances its historical ceiling', () => {
  const source = 'PMO `1.3.16`, history `1.3.6` 至 `1.3.15` 为历史版本, current `1.3.16`\n';
  assert.equal(
    projectPmoVersionedFile('mango-pmo/rules/product/05-document-lifecycle.md', source, '1.3.16', '1.4.0'),
    'PMO `1.4.0`, history `1.3.6` 至 `1.3.16` 为历史版本, current `1.4.0`\n',
  );
});
