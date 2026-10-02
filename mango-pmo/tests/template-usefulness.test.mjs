import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const templates = [
  'business-requirements.md',
  'system-requirements.md',
  'technical-design.md',
  'implementation-plan.md',
  'standard-delivery-record.md'
];

test('stage templates explain their downstream use and provide a minimum example', () => {
  for (const name of templates) {
    const content = fs.readFileSync(path.join(root, 'templates', name), 'utf8');
    assert.match(content, /使用合同/u, `${name} must explain its usage contract`);
    assert.match(content, /下游用途/u, `${name} must name downstream consumers`);
    assert.match(content, /最小合格示例/u, `${name} must provide a minimum usable example`);
    assert.match(content, /来源/u, `${name} must identify source of facts`);
    assert.match(content, /校验方式/u, `${name} must identify validation`);
  }
});

test('template selector explains when to choose each stage', () => {
  const content = fs.readFileSync(path.join(root, 'templates', 'README.md'), 'utf8');
  for (const stage of ['BRD', 'SRS', 'TDD', 'Plan', 'STANDARD']) {
    assert.match(content, new RegExp(stage, 'u'), `selector must mention ${stage}`);
  }
});
