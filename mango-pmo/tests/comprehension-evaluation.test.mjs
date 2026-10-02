import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const generator = path.join(root, 'tools/create-comprehension-evaluation-packet.mjs');
const checker = path.join(root, 'tools/check-comprehension-evaluation.mjs');

function packet() {
  const generated = spawnSync(process.execPath, [generator], { encoding: 'utf8' });
  assert.equal(generated.status, 0, generated.stderr);
  return generated.stdout
    .replace('|  |  |  |  |', '| human participant | human evaluator | 2026-10-01 | abc1234 |')
    .replaceAll(/\| (\d) \| ([^\n]+) \|  \|  \|/gu, '| $1 | $2 | 1 | source evidence |')
    .replaceAll('- **本题得分：** /6', '- **本题得分：** 6/6')
    .replaceAll('- **澄清次数：**', '- **澄清次数：** 0')
    .replaceAll('- **参与者首次指出的源码入口：**', '- **参与者首次指出的源码入口：** current/source.md')
    .replaceAll('- **未解释术语数量：**', '- **未解释术语数量：** 0')
    .replaceAll('| 总分达标（每题至少 5/6） |  |', '| 总分达标（每题至少 5/6） | PASS |')
    .replaceAll('| 首次源码命中率达标 |  |', '| 首次源码命中率达标 | PASS |')
    .replaceAll('| 澄清次数较基线下降 |  |', '| 澄清次数较基线下降 | PASS |')
    .replaceAll('| 结论 |  |', '| 结论 | PASS |');
}

function run(content) {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-comprehension-evaluation-'));
  const documentPath = path.join(directory, 'evaluation.md');
  fs.writeFileSync(documentPath, content);
  return spawnSync(process.execPath, [checker, '--document', documentPath], { encoding: 'utf8' });
}

test('completed evaluation packet passes the mechanical gate', () => {
  const result = run(packet());
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.stdout, /8\/8/);
});

test('blank evaluation packet is blocked', () => {
  const generated = spawnSync(process.execPath, [generator], { encoding: 'utf8' });
  const result = run(generated.stdout);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /必须填写|缺少本题得分/);
});
