import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

import { checkDocumentSet } from '../../tools/check-document-set.mjs';
import { sha256 } from '../../tools/document-contract/lifecycle.mjs';

const pmoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const lifecycle = JSON.parse(fs.readFileSync(path.join(pmoRoot, 'contracts/document-lifecycle.json'), 'utf8'));

test('当前生命周期只把 lean-documents 作为新文档合同', () => {
  assert.equal(lifecycle.currentDocumentContract, 'mango-pmo/contracts/lean-documents.json');
  assert.equal(lifecycle.stagesAreLegacy, true);
  assert.equal(lifecycle.legacyStageContracts, true);
  assert.deepEqual(lifecycle.riskLevels, ['L0', 'L1', 'L2', 'L3', 'L4', 'L5']);
  assert.deepEqual(lifecycle.deliveryModes, ['SIMPLE', 'L2', 'L3', 'L4', 'L5']);
});

test('当前等级文档数量和模板路径与生命周期合同一致', () => {
  assert.deepEqual(lifecycle.currentArtifacts.L0, []);
  assert.deepEqual(lifecycle.currentArtifacts.L1, []);
  assert.equal(lifecycle.currentArtifacts.L2.length, 1);
  assert.equal(lifecycle.currentArtifacts.L3.length, 1);
  assert.equal(lifecycle.currentArtifacts.L4.length, 1);
  assert.equal(lifecycle.currentArtifacts.L5.length, 4);
  for (const files of Object.values(lifecycle.currentArtifacts)) {
    for (const file of files) assert.equal(fs.existsSync(path.resolve(pmoRoot, '..', file)), true, file);
  }
});

test('旧四阶段合同、模板和检查器作为历史兼容资产仍然存在', () => {
  const legacyAssets = [
    'contracts/business-requirements.json',
    'contracts/system-requirements.json',
    'contracts/technical-design.json',
    'contracts/implementation-plan.json',
    'templates/business-requirements.md',
    'templates/system-requirements.md',
    'templates/technical-design.md',
    'templates/implementation-plan.md',
    'tools/check-business-requirements.mjs',
    'tools/check-system-requirements.mjs',
    'tools/check-technical-design.mjs',
    'tools/check-implementation-plan.mjs',
  ];
  for (const file of legacyAssets) assert.equal(fs.existsSync(path.join(pmoRoot, file)), true, file);
  assert.equal(lifecycle.stages.length, 4);
});

test('哈希锁定的历史文档可读取，内容变化后立即失效', t => {
  const root = fs.mkdtempSync(path.join(process.env.TMPDIR || '/tmp', 'mango-pmo-legacy-docs-'));
  t.after(() => fs.rmSync(root, { recursive: true, force: true }));
  const legacyPath = path.join(root, 'legacy-plan.md');
  const source = '# 历史实施计划\n\n存量内容。\n';
  fs.writeFileSync(legacyPath, source);
  fs.writeFileSync(
    path.join(root, '.mango-pmo-legacy-documents.json'),
    `${JSON.stringify({
      schemaVersion: 1,
      documents: [{ path: 'legacy-plan.md', sha256: sha256(source), reason: '旧合同形成，只读保留' }],
    }, null, 2)}\n`,
  );

  const pinned = checkDocumentSet(root);
  assert.deepEqual(pinned.findings, []);
  assert.equal(pinned.legacyDocuments.length, 1);

  fs.writeFileSync(legacyPath, `${source}\n未锁定变化\n`);
  const changed = checkDocumentSet(root);
  assert.ok(changed.findings.some(item => item.ruleId === 'LIFE-HASH-020' && item.file === legacyPath));
});

test('越界或失效的历史基线项被阻断', t => {
  const root = fs.mkdtempSync(path.join(process.env.TMPDIR || '/tmp', 'mango-pmo-stale-docs-'));
  t.after(() => fs.rmSync(root, { recursive: true, force: true }));
  fs.writeFileSync(
    path.join(root, '.mango-pmo-legacy-documents.json'),
    `${JSON.stringify({
      schemaVersion: 1,
      documents: [
        { path: 'missing-plan.md', sha256: '0'.repeat(64), reason: '待迁移' },
        { path: '../outside.md', sha256: '0'.repeat(64), reason: '非法路径' },
      ],
    }, null, 2)}\n`,
  );
  const result = checkDocumentSet(root);
  assert.ok(result.findings.some(item => item.message.includes('历史文档基线路径非法')));
  assert.ok(result.findings.some(item => item.message.includes('历史文档基线项已失效')));
});
