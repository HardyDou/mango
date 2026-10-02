#!/usr/bin/env node
import { readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const pmoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const datasetPath = path.join(pmoRoot, 'tests/skills/comprehension-evals.json');
const outputPath = valueAfter('--output');
const includeRubric = process.argv.includes('--with-rubric');
const suite = JSON.parse(readFileSync(datasetPath, 'utf8'));
const questions = suite.scoring.questions;
const lines = [
  '# Mango Understanding-first 人工评测包',
  '',
  '> 本评测包必须由未参与实现的真实参与者执行。Agent 或模拟角色的结果只能作为 dry run，不能替代人工门禁。',
  '',
  `- **目标分数：** ${suite.scoring.targetScore}/6`,
  `- **首次源码命中率目标：** ${suite.scoring.firstCorrectSourceRate * 100}%`,
  `- **澄清减少目标：** ${suite.scoring.relativeClarificationReduction * 100}%`,
  '- **评测方式：** 不先展示 expected facts；先让参与者回答，再由评测者依据当前源码复核。',
  '',
  '## 评测记录',
  '',
  '| 参与者 | 评测者 | 日期 | 版本/提交 |',
  '|---|---|---|---|',
  '|  |  |  |  |',
  '',
];

for (const item of suite.cases) {
  lines.push(`## ${item.id} · ${item.audience}`, '', `**场景请求：** ${item.prompt}`, `**推荐表达形式：** ${item.recommendedFormat ?? '受控文字'}`, '');
  lines.push('**当前来源：**');
  for (const reference of item.sourceRefs) lines.push('- `' + reference + '`');
  lines.push('- 下一入口：`' + item.nextSource + '`', '', '**六问记录：**', '', '| # | 参与者回答摘要 | 得分（0/1） | 评测证据 |', '|---|---|---:|---|');
  questions.forEach((question, index) => lines.push(`| ${index + 1} | ${question} |  |  |`));
  lines.push('', '- **本题得分：** /6', '- **澄清次数：**', '- **参与者首次指出的源码入口：**', '- **未解释术语数量：**', '');
  if (includeRubric) {
    lines.push('**评测者 rubric（回答完成后再展开）：**');
    for (const fact of item.expectedFacts) lines.push(`- ${fact}`);
    lines.push('');
  }
}

lines.push('## 汇总', '', '| 项目 | 结果 |', '|---|---|', '| 总分达标（每题至少 5/6） |  |', '| 首次源码命中率达标 |  |', '| 澄清次数较基线下降 |  |', '| 结论 |  |', '');
const packet = `${lines.join('\n')}\n`;
if (outputPath) {
  const resolved = path.resolve(process.cwd(), outputPath);
  writeFileSync(resolved, packet);
  console.log(`Wrote evaluation packet: ${resolved}`);
} else {
  process.stdout.write(packet);
}

function valueAfter(flag) {
  const index = process.argv.indexOf(flag);
  return index >= 0 ? process.argv[index + 1] : null;
}
function fail(message) {
  console.error(`[FAIL] ${message}`);
  process.exit(1);
}
