import assert from 'node:assert/strict';
import test from 'node:test';
import { projectPmoPluginVersion } from './release-pmo-plugin-lib.mjs';

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
