#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';

const REQUIRED_COLUMNS = [
  '台账 ID',
  '页面/接口',
  '功能点',
  '测试数据',
  '关键断言',
  'UI/交互检查',
  'console/network 结果',
  '截图/trace/日志',
  '结论',
];
const DONE_STATUSES = new Set(['DONE', 'PASS', '通过']);
const EXCEPTION_STATUSES = new Set(['EXCEPTION', 'BLOCKED', '未验证']);
const ALLOWED_STATUSES = new Set([...DONE_STATUSES, ...EXCEPTION_STATUSES]);
const WEAK_PHRASES = [
  '接口 200',
  '接口返回 200',
  '页面无异常',
  '页面正常',
  '页面能打开',
  '无报错',
  '截图正常',
  '主流程通过',
];

function parseArgs(argv) {
  const args = {
    evidence: '',
    minRows: 1,
    json: false,
  };
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--json') {
      args.json = true;
      continue;
    }
    if (arg === '--min-rows') {
      args.minRows = Number(argv[index + 1] || '1');
      index += 1;
      continue;
    }
    if (arg === '--evidence') {
      args.evidence = argv[index + 1] || '';
      index += 1;
      continue;
    }
  }
  return args;
}

function normalizeCell(cell) {
  return cell.trim().replaceAll('<br>', '\n').replaceAll('<br/>', '\n').replaceAll('<br />', '\n');
}

function splitMarkdownRow(line) {
  const trimmed = line.trim();
  if (!trimmed.startsWith('|') || !trimmed.endsWith('|')) {
    return [];
  }
  return trimmed.slice(1, -1).split('|').map(normalizeCell);
}

function isSeparatorRow(cells) {
  return cells.length > 0 && cells.every((cell) => /^:?-{3,}:?$/.test(cell.trim()));
}

function parseTables(content) {
  const lines = content.split(/\r?\n/);
  const tables = [];
  let headers = [];
  let rows = [];
  for (let index = 0; index < lines.length; index += 1) {
    const cells = splitMarkdownRow(lines[index]);
    if (cells.length === 0) {
      if (headers.length > 0) {
        tables.push({ headers, rows });
        headers = [];
        rows = [];
      }
      continue;
    }
    const nextCells = splitMarkdownRow(lines[index + 1] || '');
    if (headers.length === 0 && isSeparatorRow(nextCells)) {
      headers = cells;
      index += 1;
      continue;
    }
    if (headers.length > 0 && cells.length === headers.length && !isSeparatorRow(cells)) {
      const row = {};
      headers.forEach((header, columnIndex) => {
        row[header] = cells[columnIndex] || '';
      });
      rows.push(row);
    }
  }
  if (headers.length > 0) {
    tables.push({ headers, rows });
  }
  return tables;
}

function findEvidenceTable(tables) {
  return tables.find((table) => REQUIRED_COLUMNS.every((column) => table.headers.includes(column)));
}

function isPlaceholder(value) {
  const normalized = String(value || '').trim();
  return !normalized || normalized === '-' || normalized === 'TODO' || normalized === 'TBD' || normalized === '无';
}

function rowText(row) {
  return Object.values(row).join(' ');
}

function checkRow(row, index, errors) {
  const rowNo = index + 1;
  for (const column of REQUIRED_COLUMNS) {
    if (isPlaceholder(row[column])) {
      errors.push(`Row ${rowNo} missing concrete value for column: ${column}`);
    }
  }

  const status = String(row['结论'] || '').trim();
  if (!ALLOWED_STATUSES.has(status)) {
    errors.push(`Row ${rowNo} has invalid conclusion "${status}", expected DONE, PASS, 通过, EXCEPTION, BLOCKED, or 未验证`);
  }

  const text = rowText(row);
  const hasWeakPhrase = WEAK_PHRASES.some((phrase) => text.includes(phrase));
  if (DONE_STATUSES.has(status) && hasWeakPhrase) {
    errors.push(`Row ${rowNo} uses weak acceptance wording but is marked complete`);
  }

  if (DONE_STATUSES.has(status)) {
    for (const column of ['关键断言', 'UI/交互检查', 'console/network 结果', '截图/trace/日志']) {
      if (String(row[column] || '').length < 6) {
        errors.push(`Row ${rowNo} has insufficient evidence in column: ${column}`);
      }
    }
  }

  if (EXCEPTION_STATUSES.has(status) && !String(row['关键断言'] || '').includes('原因')) {
    errors.push(`Row ${rowNo} is not complete but does not record a reason in 关键断言`);
  }
}

function printText(result) {
  console.log('Acceptance Evidence Check');
  console.log(`Evidence: ${result.evidence}`);
  console.log(`Rows: ${result.rows}`);
  if (result.errors.length > 0) {
    console.log('');
    console.log('Errors:');
    result.errors.forEach((error) => console.log(`- ${error}`));
  }
}

const args = parseArgs(process.argv.slice(2));
const errors = [];

if (!args.evidence) {
  errors.push('Missing --evidence');
}
if (Number.isNaN(args.minRows) || args.minRows < 1) {
  errors.push('--min-rows must be a positive number');
}

let content = '';
if (args.evidence) {
  if (!fs.existsSync(args.evidence)) {
    errors.push(`Evidence file not found: ${args.evidence}`);
  } else {
    content = fs.readFileSync(args.evidence, 'utf8');
  }
}

const tables = content ? parseTables(content) : [];
const evidenceTable = findEvidenceTable(tables);
if (!evidenceTable) {
  errors.push(`Evidence table missing required columns: ${REQUIRED_COLUMNS.join(', ')}`);
}

const rows = evidenceTable?.rows || [];
if (rows.length < args.minRows) {
  errors.push(`Evidence table has ${rows.length} rows, expected at least ${args.minRows}`);
}
rows.forEach((row, index) => checkRow(row, index, errors));

const result = {
  evidence: args.evidence ? path.resolve(args.evidence) : '',
  rows: rows.length,
  errors,
};

if (args.json) {
  console.log(JSON.stringify(result, null, 2));
} else {
  printText(result);
}

process.exit(errors.length > 0 ? 1 : 0);
