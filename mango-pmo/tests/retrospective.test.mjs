import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const tool = path.join(root, 'tools/check-retrospective.mjs');
const valid = `---\nretrospectiveId: RETRO-2026-001\ndocumentType: delivery-retrospective\nstatus: DRAFT\nowner: delivery-owner\nreviewedCommitOrVersion: abc1234\nsourceDeliveryRecord: evidence/delivery.md\n---\n\n# Delivery retro\n\n> **先读：这次复盘要回答什么**\n> - **一句话结论：** 明确入口降低了澄清次数\n> - **真实场景：** 新贡献者先看上传预览\n> - **关键证据：** evidence/delivery.md\n> - **成功与失败：** 入口命中，旧术语仍造成一次误解\n> - **明确不做：** 不重写结果\n> - **阅读顺序：** 先看结论，再看动作\n\n## 1. 保留\n| 项目 | 观察到的事实 | 证据 |\n|---|---|---|\n| KEEP-1 | 入口命中 | evidence/delivery.md |\n\n## 2. 改变\n| 项目 | 观察到的事实 | 下次改变 | 证据 |\n|---|---|---|---|\n| CHANGE-1 | 一次澄清 | 补充场景 | evidence/review.md |\n\n## 3. 停止\n| 项目 | 停止的行为或假设 | 原因 | 证据 |\n|---|---|---|---|\n| STOP-1 | 猜测 | 无来源 | evidence/review.md |\n\n## 4. 理解与路由复盘\n| 场景 | 用户或 Agent 原先卡在哪里 | 首个有效来源 | 是否一次命中 | 下次入口 |\n|---|---|---|---|---|\n| file flow | URL 与 fileId 混淆 | guide.md | 否 | guide.md |\n\n## 5. 后续动作\n| 动作ID | 责任人 | 目标路径或 Skill | 完成条件 | 截止里程碑 | 状态 |\n|---|---|---|---|---|---|\n| ACTION-1 | owner | guide.md | check passes | next | OPEN |\n\n## 6. 证据索引\n| 证据 | 类型 | 用途 |\n|---|---|---|\n| evidence/delivery.md | record | delivery facts |\n`;

function run(content) {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-retro-'));
  const documentPath = path.join(directory, 'retro.md');
  fs.writeFileSync(documentPath, content);
  return spawnSync(process.execPath, [tool, '--document', documentPath], { encoding: 'utf8' });
}

test('retrospective checker accepts evidence-backed record', () => {
  const result = run(valid);
  assert.equal(result.status, 0, result.stderr);
});

test('retrospective checker rejects unresolved placeholders', () => {
  const result = run(valid.replace('明确入口降低了澄清次数', '{{CONCLUSION}}'));
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /占位符/);
});
