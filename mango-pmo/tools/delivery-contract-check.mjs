#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';

const BASE_REQUIRED_COLUMNS = ['ID', '来源', '要求', '设计决策', '验收方式', '状态', '证据文件'];
const LEGACY_DELIVERY_COLUMN = '交付物';
const MATERIAL_COLUMNS = ['代码交付物', 'README/使用说明', '需求/设计文档', 'E2E 脚本', '测试结果基线'];
const DEFAULT_FORBIDDEN = ['TODO', 'FIXME', 'mock', 'Mock', 'virtual', 'Virtual', '模拟', '伪代码', '未来优化', '后续优化'];
const DONE_STATUSES = new Set(['DONE', 'EXCEPTION']);
const PLAN_STATUSES = new Set(['TODO', 'IN_PROGRESS', 'DONE', 'EXCEPTION']);
const EMPTY_VALUES = new Set(['', '-']);

function parseArgs(argv) {
  const args = {
    design: '',
    ledger: '',
    mode: 'plan',
    require: '',
    scan: '',
    forbidden: DEFAULT_FORBIDDEN.join(','),
    requireMaterials: false,
    json: false
  };
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === '--json') {
      args.json = true;
      continue;
    }
    if (arg === '--require-materials') {
      args.requireMaterials = true;
      continue;
    }
    if (arg.startsWith('--')) {
      const key = arg.slice(2);
      args[key] = argv[i + 1] ?? '';
      i += 1;
    }
  }
  return args;
}

function splitList(value) {
  return String(value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

function readFile(filePath, label, errors) {
  if (!filePath) {
    errors.push(`Missing --${label}`);
    return '';
  }
  if (!fs.existsSync(filePath)) {
    errors.push(`${label} file not found: ${filePath}`);
    return '';
  }
  return fs.readFileSync(filePath, 'utf8');
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

function parseLedgerRows(content) {
  const lines = content.split(/\r?\n/);
  const tables = [];
  for (let i = 0; i < lines.length; i += 1) {
    const cells = splitMarkdownRow(lines[i]);
    if (cells.length === 0) {
      continue;
    }
    const nextCells = splitMarkdownRow(lines[i + 1] || '');
    if (isSeparatorRow(nextCells)) {
      const headers = cells;
      const rows = [];
      i += 1;
      for (let rowIndex = i + 1; rowIndex < lines.length; rowIndex += 1) {
        const rowCells = splitMarkdownRow(lines[rowIndex]);
        if (rowCells.length === 0) {
          break;
        }
        if (rowCells.length === headers.length && !isSeparatorRow(rowCells)) {
          const row = {};
          headers.forEach((header, index) => {
            row[header] = rowCells[index] || '';
          });
          rows.push(row);
        }
        i = rowIndex;
      }
      tables.push({ headers, rows });
      continue;
    }
  }
  const ledgerTable = tables.find((table) => {
    const headers = table.headers;
    return BASE_REQUIRED_COLUMNS.every((column) => headers.includes(column))
      && (headers.includes(LEGACY_DELIVERY_COLUMN) || MATERIAL_COLUMNS.some((column) => headers.includes(column)));
  });
  return ledgerTable || { headers: [], rows: [] };
}

function rowText(row) {
  return Object.values(row).join(' ');
}

function checkRequiredColumns(headers, requireMaterials, errors) {
  for (const column of BASE_REQUIRED_COLUMNS) {
    if (!headers.includes(column)) {
      errors.push(`Ledger missing required column: ${column}`);
    }
  }
  if (requireMaterials) {
    for (const column of MATERIAL_COLUMNS) {
      if (!headers.includes(column)) {
        errors.push(`Ledger missing required material column: ${column}`);
      }
    }
    return;
  }
  if (!headers.includes(LEGACY_DELIVERY_COLUMN) && !MATERIAL_COLUMNS.some((column) => headers.includes(column))) {
    errors.push(`Ledger missing required column: ${LEGACY_DELIVERY_COLUMN} or material columns`);
  }
}

function isEmptyCell(value) {
  return EMPTY_VALUES.has(String(value || '').trim());
}

function hasExceptionText(value) {
  return String(value || '').toUpperCase().includes('EXCEPTION');
}

function checkRows(rows, mode, requireMaterials, errors) {
  if (rows.length === 0) {
    errors.push('Ledger has no delivery rows');
    return;
  }
  rows.forEach((row, index) => {
    const rowNo = index + 1;
    for (const column of BASE_REQUIRED_COLUMNS) {
      if (isEmptyCell(row[column])) {
        errors.push(`Row ${rowNo} missing value for column: ${column}`);
      }
    }
    if (requireMaterials) {
      for (const column of MATERIAL_COLUMNS) {
        if (isEmptyCell(row[column])) {
          errors.push(`Row ${rowNo} missing value for material column: ${column}`);
        }
      }
    } else if (row[LEGACY_DELIVERY_COLUMN] !== undefined && isEmptyCell(row[LEGACY_DELIVERY_COLUMN])) {
      errors.push(`Row ${rowNo} missing value for column: ${LEGACY_DELIVERY_COLUMN}`);
    }
    const status = String(row['状态'] || '').trim();
    if (!PLAN_STATUSES.has(status)) {
      errors.push(`Row ${rowNo} has invalid status "${status}", expected TODO, IN_PROGRESS, DONE, or EXCEPTION`);
    }
    if (mode === 'verify' && !DONE_STATUSES.has(status)) {
      errors.push(`Row ${rowNo} is not complete in verify mode: ${status}`);
    }
    if (mode === 'verify' && status === 'EXCEPTION') {
      const evidence = String(row['证据文件'] || '').trim();
      if (isEmptyCell(evidence)) {
        errors.push(`Row ${rowNo} is EXCEPTION but has no evidence or user confirmation`);
      }
    }
    if (mode === 'verify' && requireMaterials && status !== 'EXCEPTION') {
      for (const column of MATERIAL_COLUMNS) {
        if (hasExceptionText(row[column]) && isEmptyCell(row['证据文件'])) {
          errors.push(`Row ${rowNo} material column ${column} is EXCEPTION but has no evidence or user confirmation`);
        }
      }
    }
  });
}

function checkRequiredItems(rows, sourceText, requiredItems, errors) {
  for (const item of requiredItems) {
    if (sourceText && !sourceText.includes(item)) {
      errors.push(`Required item not found in design source: ${item}`);
    }
    const matched = rows.some((row) => rowText(row).includes(item));
    if (!matched) {
      errors.push(`Ledger missing required delivery item: ${item}`);
    }
  }
}

function collectFiles(targetPath, files) {
  if (!fs.existsSync(targetPath)) {
    return;
  }
  const stat = fs.statSync(targetPath);
  if (stat.isFile()) {
    files.push(targetPath);
    return;
  }
  if (!stat.isDirectory()) {
    return;
  }
  for (const entry of fs.readdirSync(targetPath, { withFileTypes: true })) {
    if (entry.name === 'node_modules' || entry.name === 'target' || entry.name === '.git') {
      continue;
    }
    collectFiles(path.join(targetPath, entry.name), files);
  }
}

function checkForbidden(scanPaths, forbiddenTerms, errors) {
  if (scanPaths.length === 0 || forbiddenTerms.length === 0) {
    return;
  }
  const files = [];
  scanPaths.forEach((scanPath) => collectFiles(scanPath, files));
  for (const filePath of files) {
    let content = '';
    try {
      content = fs.readFileSync(filePath, 'utf8');
    } catch {
      continue;
    }
    const lines = content.split(/\r?\n/);
    lines.forEach((line, index) => {
      for (const term of forbiddenTerms) {
        if (line.includes(term)) {
          errors.push(`Forbidden marker "${term}" found at ${filePath}:${index + 1}`);
        }
      }
    });
  }
}

function buildSummary(rows) {
  const total = rows.length;
  const done = rows.filter((row) => row['状态'] === 'DONE').length;
  const exception = rows.filter((row) => row['状态'] === 'EXCEPTION').length;
  const incomplete = rows.filter((row) => !DONE_STATUSES.has(row['状态'])).length;
  return { total, done, exception, incomplete };
}

function printText(result) {
  console.log('Delivery Contract Check');
  console.log(`Mode: ${result.mode}`);
  console.log(`Design: ${result.design}`);
  console.log(`Ledger: ${result.ledger}`);
  console.log(`Rows: ${result.summary.total}`);
  console.log(`DONE: ${result.summary.done}`);
  console.log(`EXCEPTION: ${result.summary.exception}`);
  console.log(`Incomplete: ${result.summary.incomplete}`);
  if (result.errors.length > 0) {
    console.log('');
    console.log('Errors:');
    result.errors.forEach((error) => console.log(`- ${error}`));
  }
}

const args = parseArgs(process.argv.slice(2));
const errors = [];
const designText = readFile(args.design, 'design', errors);
const ledgerText = readFile(args.ledger, 'ledger', errors);
const requiredItems = splitList(args.require);
const forbiddenTerms = splitList(args.forbidden);
const scanPaths = splitList(args.scan);

if (!['plan', 'verify'].includes(args.mode)) {
  errors.push(`Invalid --mode "${args.mode}", expected plan or verify`);
}

const parsed = ledgerText ? parseLedgerRows(ledgerText) : { headers: [], rows: [] };
if (ledgerText) {
  checkRequiredColumns(parsed.headers, args.requireMaterials, errors);
  checkRows(parsed.rows, args.mode, args.requireMaterials, errors);
  checkRequiredItems(parsed.rows, designText, requiredItems, errors);
}

checkForbidden(scanPaths, forbiddenTerms, errors);

const result = {
  mode: args.mode,
  requireMaterials: args.requireMaterials,
  design: args.design,
  ledger: args.ledger,
  summary: buildSummary(parsed.rows),
  errors
};

if (args.json) {
  console.log(JSON.stringify(result, null, 2));
} else {
  printText(result);
}

process.exit(errors.length > 0 ? 1 : 0);
