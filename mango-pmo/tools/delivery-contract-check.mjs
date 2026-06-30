#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';

const BASE_REQUIRED_COLUMNS = ['ID', '来源', '要求', '设计决策', '验收方式', '状态', '证据文件'];
const LEGACY_DELIVERY_COLUMN = '交付物';
const MATERIAL_COLUMNS = ['代码交付物', 'README/使用说明', '需求/设计文档', 'E2E 脚本', '测试结果基线'];
const E2E_COLUMN = 'E2E 脚本';
const BASELINE_COLUMN = '测试结果基线';
const EVIDENCE_CASE_COLUMN = '用例 ID';
const EVIDENCE_COLUMNS = [
  '台账 ID',
  EVIDENCE_CASE_COLUMN,
  '页面/接口',
  '功能点',
  '测试数据',
  '关键断言',
  'UI/交互检查',
  'console/network 结果',
  '截图/trace/日志',
  '结论'
];
const TEST_CASE_COLUMNS = [
  '用例 ID',
  '来源 AC',
  '场景',
  '优先级',
  '测试层级',
  '自动化判断',
  '测试数据',
  '稳定契约',
  '执行入口',
  '证据',
  '状态'
];
const BASELINE_COLUMNS = [
  '基线 ID',
  '覆盖台账 ID',
  'E2E 脚本',
  '测试命令',
  '环境/版本',
  '数据库或数据集',
  '账号/租户标识',
  '结果摘要',
  '失败/阻塞/例外',
  '报告/截图/日志路径',
  '行为变化'
];
const BASELINE_CASE_COLUMN = '覆盖用例 ID';
const DEFAULT_FORBIDDEN = ['TODO', 'FIXME', 'mock', 'Mock', 'virtual', 'Virtual', '模拟', '伪代码', '未来优化', '后续优化'];
const DONE_STATUSES = new Set(['DONE', 'EXCEPTION']);
const PLAN_STATUSES = new Set(['TODO', 'IN_PROGRESS', 'DONE', 'EXCEPTION']);
const TEST_CASE_STATUSES = new Set(['CANDIDATE', 'AUTOMATED', 'MANUAL', 'FLAKY', 'DEPRECATED']);
const TEST_PRIORITIES = new Set(['P0', 'P1', 'P2', 'P3']);
const TEST_LEVELS = new Set(['单元', 'API', '组件', 'E2E', '截图', '手工']);
const AUTOMATION_DECISIONS = new Set(['AUTO', 'MANUAL', 'EXCEPTION']);
const EMPTY_VALUES = new Set(['', '-']);
const NOT_APPLICABLE_VALUES = new Set(['n/a', 'na', 'none', 'not applicable', '不适用', '无需', '无']);

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

function parseMarkdownTables(content) {
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
  return tables;
}

function parseLedgerRows(content) {
  const tables = parseMarkdownTables(content);
  const ledgerTable = tables.find((table) => {
    const headers = table.headers;
    return BASE_REQUIRED_COLUMNS.every((column) => headers.includes(column))
      && (headers.includes(LEGACY_DELIVERY_COLUMN) || MATERIAL_COLUMNS.some((column) => headers.includes(column)));
  });
  return ledgerTable || { headers: [], rows: [] };
}

function parseTestCaseRows(content) {
  const tables = parseMarkdownTables(content);
  const testCaseTable = tables.find((table) => TEST_CASE_COLUMNS.every((column) => table.headers.includes(column)));
  return testCaseTable || { headers: [], rows: [] };
}

function parseBaselineRows(content) {
  const tables = parseMarkdownTables(content);
  const baselineTable = tables.find((table) => BASELINE_COLUMNS.every((column) => table.headers.includes(column)));
  return baselineTable || { headers: [], rows: [] };
}

function parseEvidenceRows(content) {
  const tables = parseMarkdownTables(content);
  const evidenceTable = tables.find((table) => EVIDENCE_COLUMNS.every((column) => table.headers.includes(column)));
  return evidenceTable || { headers: [], rows: [] };
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

function isPlaceholderCell(value) {
  const normalized = String(value || '').trim();
  return isEmptyCell(normalized) || normalized === 'TODO' || normalized === 'TBD';
}

function hasExceptionText(value) {
  return String(value || '').toUpperCase().includes('EXCEPTION');
}

function hasPathLikeText(value) {
  const text = String(value || '').trim();
  return /[`'"]?\/?[\w.@-]+\/[\w./@-]+[`'"]?/.test(text) || /\b[\w.@-]+\.(md|mjs|js|ts|tsx|vue|java|xml|json|yaml|yml|sql|sh|feature|spec)\b/.test(text);
}

function extractPathLikeText(value) {
  const text = String(value || '').trim();
  const match = text.match(/`?(\/?[\w.@-]+\/[\w./@-]+)`?/) || text.match(/`?([\w.@-]+\.(?:md|mjs|js|ts|tsx|vue|java|xml|json|yaml|yml|sql|sh|feature|spec))`?/);
  return match ? match[1] : '';
}

function isNotApplicableText(value) {
  const normalized = String(value || '')
    .trim()
    .toLowerCase()
    .replace(/[。；;,.，\s]+$/g, '');
  return NOT_APPLICABLE_VALUES.has(normalized);
}

function isFormalTestPath(filePath) {
  return /(^|\/)(tests?|__tests__|e2e|playwright|cypress|src\/test)(\/|$)/.test(filePath);
}

function checkMaterialException(rowNo, column, value, errors) {
  if (!hasExceptionText(value)) {
    return;
  }
  const reason = String(value || '').replace(/EXCEPTION:?/i, '').trim();
  if (reason.length < 8 || isNotApplicableText(reason)) {
    errors.push(`Row ${rowNo} material column ${column} has EXCEPTION but no concrete reason`);
  }
}

function checkMaterialPath(rowNo, column, value, errors) {
  if (hasExceptionText(value)) {
    return;
  }
  if (isNotApplicableText(value)) {
    errors.push(`Row ${rowNo} material column ${column} is not applicable but does not contain EXCEPTION`);
    return;
  }
  if (!hasPathLikeText(value)) {
    errors.push(`Row ${rowNo} material column ${column} must contain a path or EXCEPTION reason`);
  }
}

function checkE2ePath(rowNo, value, errors) {
  if (hasExceptionText(value)) {
    return;
  }
  const filePath = extractPathLikeText(value);
  if (!filePath) {
    return;
  }
  if (!isFormalTestPath(filePath)) {
    errors.push(`Row ${rowNo} material column ${E2E_COLUMN} must point to a formal test directory or use EXCEPTION`);
  }
  if (!fs.existsSync(filePath)) {
    errors.push(`Row ${rowNo} material column ${E2E_COLUMN} path does not exist: ${filePath}`);
  }
}

function splitCellIds(value) {
  return String(value || '')
    .split(/[,，、\s]+/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function checkTestCaseTable(testCaseRows, evidenceRows, baselineRows, errors) {
  if (testCaseRows.length === 0) {
    return;
  }
  const referencedCaseIds = new Set([
    ...evidenceRows.flatMap((row) => splitCellIds(row[EVIDENCE_CASE_COLUMN])),
    ...baselineRows.flatMap((row) => splitCellIds(row[BASELINE_CASE_COLUMN]))
  ]);
  const seenCaseIds = new Set();
  testCaseRows.forEach((row, index) => {
    const rowNo = index + 1;
    for (const column of TEST_CASE_COLUMNS) {
      if (isPlaceholderCell(row[column])) {
        errors.push(`Test case row ${rowNo} missing concrete value for column: ${column}`);
      }
    }

    const caseId = String(row['用例 ID'] || '').trim();
    if (!/^TC-\d{3,}$/.test(caseId)) {
      errors.push(`Test case row ${rowNo} has invalid 用例 ID "${caseId}", expected TC-001 style`);
    }
    if (seenCaseIds.has(caseId)) {
      errors.push(`Test case row ${rowNo} duplicates 用例 ID: ${caseId}`);
    }
    seenCaseIds.add(caseId);

    const sourceAc = String(row['来源 AC'] || '').trim();
    if (!/^AC-\d{3,}$/.test(sourceAc)) {
      errors.push(`Test case row ${rowNo} has invalid 来源 AC "${sourceAc}", expected AC-001 style`);
    }

    const priority = String(row['优先级'] || '').trim();
    if (!TEST_PRIORITIES.has(priority)) {
      errors.push(`Test case row ${rowNo} has invalid priority "${priority}", expected P0, P1, P2, or P3`);
    }

    const levels = splitCellIds(row['测试层级']);
    if (levels.length === 0 || levels.some((level) => !TEST_LEVELS.has(level))) {
      errors.push(`Test case row ${rowNo} has invalid 测试层级 "${row['测试层级']}", expected 单元/API/组件/E2E/截图/手工`);
    }

    const automationDecision = String(row['自动化判断'] || '').trim();
    if (!AUTOMATION_DECISIONS.has(automationDecision)) {
      errors.push(`Test case row ${rowNo} has invalid 自动化判断 "${automationDecision}", expected AUTO, MANUAL, or EXCEPTION`);
    }

    const status = String(row['状态'] || '').trim();
    if (!TEST_CASE_STATUSES.has(status)) {
      errors.push(`Test case row ${rowNo} has invalid status "${status}", expected CANDIDATE, AUTOMATED, MANUAL, FLAKY, or DEPRECATED`);
    }

    if (automationDecision === 'AUTO' && isNotApplicableText(row['执行入口'])) {
      errors.push(`Test case row ${rowNo} is AUTO but 执行入口 is not applicable`);
    }
    if (automationDecision === 'EXCEPTION' && !hasExceptionText(row['证据'])) {
      errors.push(`Test case row ${rowNo} is EXCEPTION but 证据 does not record an EXCEPTION reason`);
    }
    if (caseId && referencedCaseIds.size > 0 && !referencedCaseIds.has(caseId)) {
      errors.push(`Test case row ${rowNo} 用例 ID ${caseId} is not referenced by evidence or baseline tables`);
    }
  });
}

function checkBaselineTable(baselineRows, mode, testCaseRows, errors) {
  if (baselineRows.length === 0) {
    errors.push('Test result baseline table has no rows');
    return;
  }
  const knownCaseIds = new Set(testCaseRows.map((row) => String(row['用例 ID'] || '').trim()).filter(Boolean));
  const baselineHasCaseColumn = baselineRows.some((row) => Object.prototype.hasOwnProperty.call(row, BASELINE_CASE_COLUMN));
  baselineRows.forEach((row, index) => {
    const rowNo = index + 1;
    for (const column of BASELINE_COLUMNS) {
      if (isEmptyCell(row[column])) {
        errors.push(`Baseline row ${rowNo} missing value for column: ${column}`);
      }
    }
    if (baselineHasCaseColumn) {
      if (isPlaceholderCell(row[BASELINE_CASE_COLUMN])) {
        errors.push(`Baseline row ${rowNo} missing value for column: ${BASELINE_CASE_COLUMN}`);
      }
      const caseIds = splitCellIds(row[BASELINE_CASE_COLUMN]);
      for (const caseId of caseIds) {
        if (!/^TC-\d{3,}$/.test(caseId)) {
          errors.push(`Baseline row ${rowNo} has invalid ${BASELINE_CASE_COLUMN} "${caseId}", expected TC-001 style`);
        } else if (knownCaseIds.size > 0 && !knownCaseIds.has(caseId)) {
          errors.push(`Baseline row ${rowNo} references unknown test case: ${caseId}`);
        }
      }
    }
    if (mode === 'verify' && !String(row['报告/截图/日志路径'] || '').trim().includes('EXCEPTION')) {
      const evidencePath = extractPathLikeText(row['报告/截图/日志路径']);
      if (!evidencePath) {
        errors.push(`Baseline row ${rowNo} report column must contain a path or EXCEPTION reason`);
      }
    }
  });
}

function checkRows(rows, mode, requireMaterials, baselineRows, testCaseRows, errors) {
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
        if (!(column in row)) {
          continue;
        }
        if (isEmptyCell(row[column])) {
          errors.push(`Row ${rowNo} missing value for material column: ${column}`);
          continue;
        }
        checkMaterialException(rowNo, column, row[column], errors);
        checkMaterialPath(rowNo, column, row[column], errors);
        if (column === E2E_COLUMN) {
          checkE2ePath(rowNo, row[column], errors);
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
  if (requireMaterials && rows.some((row) => row[BASELINE_COLUMN] !== undefined && !hasExceptionText(row[BASELINE_COLUMN]))) {
    checkBaselineTable(baselineRows, mode, testCaseRows, errors);
  }
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
const baselineParsed = ledgerText ? parseBaselineRows(ledgerText) : { headers: [], rows: [] };
const testCaseParsed = ledgerText ? parseTestCaseRows(ledgerText) : { headers: [], rows: [] };
const evidenceParsed = ledgerText ? parseEvidenceRows(ledgerText) : { headers: [], rows: [] };
if (ledgerText) {
  checkRequiredColumns(parsed.headers, args.requireMaterials, errors);
  checkTestCaseTable(testCaseParsed.rows, evidenceParsed.rows, baselineParsed.rows, errors);
  checkRows(parsed.rows, args.mode, args.requireMaterials, baselineParsed.rows, testCaseParsed.rows, errors);
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
