import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import test from 'node:test';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const checker = path.join(root, 'tools/check-decision-review.mjs');
const headers = ['评审方式', '评审人/Agent 角色', '检查范围', '阻断问题', '非阻断建议', '结论'];
const question = '已询问是否需要外部同行评审；用户选择按记录的方式复核。';

function reviewRow(mode, overrides = {}) {
  return {
    '评审方式': mode,
    '评审人/Agent 角色': mode === 'AGENT_ONLY' ? 'Agent role analysis' : 'Peer reviewer',
    '检查范围': '事实 / 范围 / 风险 / 验证 / 文本',
    '阻断问题': 'None',
    '非阻断建议': 'Keep source evidence.',
    '结论': mode === 'AGENT_ONLY' ? 'RECORDED' : 'PASS',
    ...overrides,
  };
}

function reviewDocument(mode = 'AGENT_ONLY', rows = [reviewRow(mode)], columns = headers) {
  const table = [
    `| ${columns.join(' | ')} |`,
    `| ${columns.map(() => '---').join(' | ')} |`,
    ...rows.map(row => `| ${columns.map(column => row[column]).join(' | ')} |`),
  ].join('\n');
  return `---
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
- **评审选择：** ${mode}
- **询问记录：** ${question}
- **用户决定人：** Owner
${table}

## 7. 审批与后续动作
- **审批人：** Pending owner
- **审批证据：** review record
`;
}

function run(content) {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'mango-decision-review-'));
  try {
    const file = path.join(dir, 'review.md');
    fs.writeFileSync(file, content);
    return spawnSync(process.execPath, [checker, '--document', file], { encoding: 'utf8' });
  } finally {
    fs.rmSync(dir, { recursive: true, force: true });
  }
}

function rejects(content, diagnostic) {
  const result = run(content);
  assert.notEqual(result.status, 0, result.stdout);
  assert.match(result.stderr, diagnostic);
}

for (const mode of ['AGENT_ONLY', 'EXTERNAL_PEER_REVIEW']) {
  test(`${mode}: accepts completed review with a recorded choice`, () => {
    const result = run(reviewDocument(mode));
    assert.equal(result.status, 0, result.stderr);
    assert.match(result.stdout, new RegExp(mode));
  });

  for (const conclusion of ['BLOCKED', '', 'AGENT_ONLY', 'NOT_PASS', 'PASS / BLOCKED']) {
    test(`${mode}: rejects conclusion ${JSON.stringify(conclusion)} even if another cell says PASS`, () => {
      rejects(reviewDocument(mode, [reviewRow(mode, {
        '结论': conclusion,
        '非阻断建议': 'PASS is not a conclusion in this cell.',
      })]), /结论/);
    });
  }

  for (const blockers of ['授权边界尚未确认，阻断未解决', '']) {
    test(`${mode}: rejects unresolved or unspecified blockers ${JSON.stringify(blockers)}`, () => {
      rejects(reviewDocument(mode, [reviewRow(mode, { '阻断问题': blockers })]), /阻断问题/);
    });
  }

  test(`${mode}: rejects mismatched row mode`, () => {
    rejects(reviewDocument(mode, [reviewRow(mode, {
      '评审方式': mode === 'AGENT_ONLY' ? 'EXTERNAL_PEER_REVIEW' : 'AGENT_ONLY',
    })]), /评审方式.*评审选择/);
  });

  test(`${mode}: checks later rows, not just the first`, () => {
    rejects(reviewDocument(mode, [reviewRow(mode), reviewRow(mode, { '结论': 'BLOCKED' })]), /结论/);
  });

  for (const inquiry of ['', '   ', 'TBD']) {
    test(`${mode}: rejects empty or placeholder inquiry ${JSON.stringify(inquiry)}`, () => {
      rejects(reviewDocument(mode).replace(question, inquiry).replace('- **用户决定人：** Owner\n', ''), /询问记录/);
    });
  }

  test(`${mode}: requires inquiry in section 6, not another section`, () => {
    const line = `- **询问记录：** ${question}\n`;
    rejects(reviewDocument(mode).replace(line, '').replace('## 1. 决定摘要', `## 1. 决定摘要\n${line}`), /询问记录/);
  });

  test(`${mode}: reads cells by header, including reordered columns and escaped pipes`, () => {
    const result = run(reviewDocument(mode, [reviewRow(mode, {
      '非阻断建议': '保留 Agent 分析来源，比较 A \\| B 和 `X|Y`。',
    })], [...headers].reverse()).replaceAll('\n', '\r\n'));
    assert.equal(result.status, 0, result.stderr);
  });

  test(`${mode}: rejects duplicate inquiry fields`, () => {
    rejects(reviewDocument(mode).replace(`- **询问记录：** ${question}`, `- **询问记录：**\n- **询问记录：** ${question}`), /询问记录/);
  });
}

test('accepts AGENT_ONLY PASS with explicit no-blocker wording', () => {
  const result = run(reviewDocument('AGENT_ONLY', [reviewRow('AGENT_ONLY', { '结论': 'PASS', '阻断问题': '无' })]));
  assert.equal(result.status, 0, result.stderr);
});

test('accepts a recorded unanswered inquiry without demanding a human reply', () => {
  const result = run(reviewDocument().replace(question, '已询问是否需要外部同行评审；未指定，按 AGENT_ONLY 继续。')
    .replace('- **用户决定人：** Owner\n', ''));
  assert.equal(result.status, 0, result.stderr);
});

test('rejects missing inquiry, even if choice wording occurs elsewhere', () => {
  rejects(reviewDocument().replace(`- **询问记录：** ${question}\n`, '') + '\n用户确认采用方案。\n', /询问记录/);
});

test('rejects inquiry present only inside a code example', () => {
  rejects(reviewDocument().replace(`- **询问记录：** ${question}`, `\`\`\`md\n- **询问记录：** ${question}\n\`\`\``), /询问记录/);
});

test('rejects ambiguous review choice', () => {
  rejects(reviewDocument().replace('**评审选择：** AGENT_ONLY', '**评审选择：** AGENT_ONLY / EXTERNAL_PEER_REVIEW'), /评审选择/);
});

for (const header of ['评审方式', '阻断问题', '结论']) {
  test(`rejects missing ${header} column`, () => {
    rejects(reviewDocument('AGENT_ONLY', [reviewRow('AGENT_ONLY')], headers.filter(item => item !== header)), new RegExp(header));
  });
}

test('rejects duplicate table headers', () => {
  rejects(reviewDocument('AGENT_ONLY', [reviewRow('AGENT_ONLY')], [...headers, '结论']), /重复/);
});

test('rejects a header-only table', () => {
  rejects(reviewDocument('AGENT_ONLY', []), /记录/);
});

test('rejects malformed rows rather than ignoring them', () => {
  rejects(reviewDocument().replace('| RECORDED |', '| RECORDED | extra |'), /列数/);
});

test('rejects placeholders independently of reviewer type', () => {
  rejects(reviewDocument().replace('review record', '{{APPROVAL_EVIDENCE}}'), /占位符/);
});

test('rejects Agent identity only when claiming external human review', () => {
  for (const reviewer of ['Agent dry run', 'OpenAI', 'GPT4']) {
    rejects(reviewDocument('EXTERNAL_PEER_REVIEW').replace('Peer reviewer', reviewer), /真实同行/);
  }
});

test('Agent-only review still cannot impersonate a formal approver', () => {
  rejects(reviewDocument().replaceAll('REVIEWED', 'APPROVED').replace('Pending owner', 'Agent approver'), /真实人工审批人/);
});
