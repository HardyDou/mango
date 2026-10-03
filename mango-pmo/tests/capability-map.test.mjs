import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const mapPath = path.join(root, 'mango-docs/capabilities/README.md');

test('capability map is module-first and points to module documentation', () => {
  const content = fs.readFileSync(mapPath, 'utf8');
  for (const heading of ['基础设施能力', '平台后端能力', '前端能力', '业务项目和治理能力']) {
    assert.match(content, new RegExp(`## .*${heading}`, 'u'));
  }
  for (const moduleName of ['Auth', 'Identity', 'Authorization', 'File', 'Workflow', 'Resource Registry', 'Admin Shell', 'Mango CLI']) {
    assert.match(content, new RegExp(`\\| ${moduleName} \\|`, 'u'), `${moduleName} must be indexed`);
  }
  assert.match(content, /能做什么/u);
  assert.match(content, /入口/u);
  assert.doesNotMatch(content, /^## .*Issue|^## .*Release/mu);
});
