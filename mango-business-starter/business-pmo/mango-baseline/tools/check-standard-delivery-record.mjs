#!/usr/bin/env node
import fs from 'node:fs';

const REQUIRED_HEADINGS = [
  '# 标准交付记录',
  '## 1. 元数据',
  '## 2. 目标与范围',
  '## 3. 可观察系统要求',
  '## 4. 技术决定',
  '## 5. 实施清单',
  '## 6. 验收映射与结果',
  '## 7. 例外与剩余风险',
];

export function validateStandardDeliveryRecord(markdown) {
  const failures = [];
  for (const heading of REQUIRED_HEADINGS) {
    if (!markdown.includes(heading)) failures.push(`missing heading: ${heading}`);
  }
  if (!/- 交付模式：STANDARD/u.test(markdown)) failures.push('delivery mode must be STANDARD');
  for (const field of ['需求影响', '方案风险', '最终风险', '工作区决策']) {
    const match = new RegExp(`^- ${field}：(.+)$`, 'mu').exec(markdown);
    if (!match || /(?:^|\s)(?:TBD|TODO|L2\s*-\s*$)/iu.test(match[1])) failures.push(`${field} must contain a concrete value`);
  }
  if (!/\|\s*ID\s*\|\s*参与者或入口/u.test(markdown)) failures.push('observable requirements table is missing');
  if (!/\|\s*要求 ID\s*\|\s*验证方式/u.test(markdown)) failures.push('acceptance mapping table is missing');
  return { failures };
}

function main(argv) {
  const file = argv[0];
  if (!file) throw new Error('usage: check-standard-delivery-record.mjs <file>');
  const result = validateStandardDeliveryRecord(fs.readFileSync(file, 'utf8'));
  if (result.failures.length > 0) {
    process.stderr.write(`${result.failures.join('\n')}\n`);
    process.exitCode = 1;
    return;
  }
  process.stdout.write(`Standard delivery record PASS: ${file}\n`);
}

if (process.argv[1]?.endsWith('check-standard-delivery-record.mjs')) main(process.argv.slice(2));
