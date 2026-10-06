import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const checker = path.join(root, 'tools/check-decision-review.mjs');

const validReview = `---
decisionId: DEC-001
status: REVIEWED
---
# Decision review

## 1. 决定摘要
- **状态：** REVIEWED

## 2. 第一轮：事实与用户视角
| 参与者/角色 | 意见 |
|---|---|
| PM reviewer | 当前事实支持该场景。 |

## 3. 第二轮：技术与风险视角
| 参与者/角色 | 意见 |
|---|---|
| Tech reviewer | 方案保留权限和租户边界。 |

## 4. 第三轮：验证与交付视角
| 参与者/角色 | 意见 |
|---|---|
| QA reviewer | API 和文档检查可以证明结果。 |

## 5. 方案整理
采用方案：按当前范围实现。

## 6. 评审选择与记录
- **评审选择：** EXTERNAL_PEER_REVIEW
- **询问记录：** 用户确认需要外部同行评审。
- **用户决定人：** Owner
| 评审方式 | 评审人/Agent 角色 | 阻断问题 | 建议 | 结论 |
|---|---|---|---|---|
| EXTERNAL_PEER_REVIEW | Peer reviewer | None | Keep source evidence. | PASS |

## 7. 审批与后续动作
- **审批人：** Pending owner
- **审批证据：** review record
`;

function run(content) {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-decision-review-'));
  const file = path.join(dir, 'review.md');
  fs.writeFileSync(file, content);
  return spawnSync(process.execPath, [checker, '--document', file], { encoding: 'utf8' });
}

test('decision review requires three perspectives and a recorded review choice', () => {
  const result = run(validReview);
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.stdout, /three perspectives/);
});

test('decision review accepts Agent-only analysis after user choice is recorded', () => {
  const result = run(validReview
    .replace('EXTERNAL_PEER_REVIEW', 'AGENT_ONLY')
    .replace('Peer reviewer', 'Agent role analysis')
    .replace('PASS |', 'RECORDED |')
    .replace('用户确认需要外部同行评审。', '用户确认只保留 Agent 角色视角分析。'));
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.stdout, /AGENT_ONLY/);
});

test('decision review rejects placeholders and simulated external reviewers', () => {
  const result = run(validReview.replace('Peer reviewer', 'Agent dry run').replace('review record', '{{APPROVAL_EVIDENCE}}'));
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /占位符|真实同行/);
});
