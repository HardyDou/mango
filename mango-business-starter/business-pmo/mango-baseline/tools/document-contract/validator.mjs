import fs from 'node:fs';
import path from 'node:path';
import { collectBodyText, findLiteralLine, parseMarkdown, tableKey } from './markdown-ast.mjs';
import {
  containsPlaceholder,
  isDocumentId,
  isExactIdentifier,
  isSemver,
  isSha256,
  scanIdentifiers
} from './identifiers.mjs';
import { validateContractRuleLinks } from './contract-loader.mjs';

const STATUSES = new Set(['DRAFT', 'IN_REVIEW', 'APPROVED', 'BLOCKED', 'STALE']);
const ACTIONS = new Set(['STOP', 'ASK', 'WRITE', 'NEXT']);
const RISKS = new Set(['L0', 'L1', 'L2', 'L3']);
const NON_HUMAN_APPROVER = /(?:\bai\b|agent|codex|claude|gpt|模型|机器人|自动审批)/iu;
const INVALID_APPROVAL_EVIDENCE = /(?:invented|self[-_ ]?check|ai[-_ ]?generated|agent[-_ ]?generated|自评|自动生成)/iu;
const UPSTREAM_DOCUMENT_PREFIX = {
  'business-requirements': 'BRD',
  'system-requirements': 'SRS',
  'technical-design': 'TDD'
};

function addFinding(findings, ruleId, message, line = null) {
  findings.push({ severity: 'FAIL', ruleId, message, ...(line ? { line } : {}) });
}

function isEmpty(value) {
  const text = String(value ?? '').trim();
  return !text || text === '-' || text === '—';
}

function sameHeaders(left, right) {
  return tableKey(left) === tableKey(right);
}

function findTable(section, headers) {
  return section?.tables.find((table) => sameHeaders(table.headers, headers)) ?? null;
}

function findSection(ast, title) {
  return ast.sections.find((section) => section.logicalTitle === title) ?? null;
}

function validateMetadata(ast, contract, findings, options) {
  const ruleId = contract.metadata.ruleId;
  const values = ast.frontmatter.values;
  if (ast.frontmatter.endLine < 0) {
    addFinding(findings, ruleId, '缺少 YAML frontmatter');
    return;
  }
  for (const key of ast.frontmatter.duplicates) addFinding(findings, ruleId, `frontmatter 键重复：${key}`);
  for (const key of contract.metadata.required) {
    if (isEmpty(values[key])) addFinding(findings, ruleId, `frontmatter 缺少有效值：${key}`);
  }
  for (const [key, expected] of Object.entries(contract.metadata.fixed ?? {})) {
    if (values[key] !== expected) addFinding(findings, ruleId, `${key} 必须为 ${expected}，实际为 ${values[key] ?? '<缺失>'}`);
  }
  if (!isDocumentId(values.documentId, contract.documentIdPrefix)) {
    addFinding(findings, ruleId, `documentId 必须是 ${contract.documentIdPrefix}-... 格式`);
  }
  if (!isSemver(values.pmoVersion)) addFinding(findings, ruleId, 'pmoVersion 必须是三段数字版本，例如 2.3.0');
  if (!RISKS.has(values.riskLevel)) addFinding(findings, ruleId, 'riskLevel 必须是 L0、L1、L2 或 L3');
  if (!STATUSES.has(values.status)) addFinding(findings, ruleId, `status 非法：${values.status ?? '<缺失>'}`);
  if (!ACTIONS.has(values.action)) addFinding(findings, ruleId, `action 非法：${values.action ?? '<缺失>'}`);

  if (contract.upstreamDocumentType) {
    const prefix = UPSTREAM_DOCUMENT_PREFIX[contract.upstreamDocumentType];
    if (!isDocumentId(values.upstreamDocumentId, prefix)) {
      addFinding(findings, ruleId, `upstreamDocumentId 必须引用 ${prefix}-... 文档`);
    }
    if (!isSha256(values.upstreamDocumentHash)) addFinding(findings, ruleId, 'upstreamDocumentHash 必须是完整 SHA-256');
  }

  if (values.action === 'NEXT') {
    if (values.status !== 'APPROVED') addFinding(findings, ruleId, 'action=NEXT 时 status 必须为 APPROVED');
    if (isEmpty(values.approver) || isEmpty(values.approvalEvidence)) {
      addFinding(findings, ruleId, 'action=NEXT 时必须有审批人和审批证据');
    }
    if (NON_HUMAN_APPROVER.test(String(values.approver ?? ''))) {
      addFinding(findings, ruleId, 'action=NEXT 的 approver 必须是可追责的人工审批人，不能是 AI/Agent');
    }
    const evidence = String(values.approvalEvidence ?? '').trim();
    if (!isEmpty(evidence) && (INVALID_APPROVAL_EVIDENCE.test(evidence) || !/^(?:https?:\/\/|[A-Za-z0-9_.-]+\/)/u.test(evidence))) {
      addFinding(findings, ruleId, 'approvalEvidence 必须引用可核验的评审记录路径或 URL');
    } else if (!isEmpty(evidence) && !/^https?:\/\//iu.test(evidence)) {
      validateLocalApprovalEvidence(evidence, options.documentPath, ruleId, findings);
    }
  }
  if (values.status === 'APPROVED' && values.action !== 'NEXT') {
    addFinding(findings, ruleId, 'status=APPROVED 时 action 必须由门禁计算为 NEXT');
  }
  if (['STOP', 'ASK'].includes(values.action) && values.status === 'APPROVED') {
    addFinding(findings, ruleId, `${values.action} 状态不能标记 APPROVED`);
  }
}

function validateLocalApprovalEvidence(evidence, documentPath, ruleId, findings) {
  const segments = evidence.replaceAll('\\', '/').split('/');
  if (evidence.includes('\\') || path.isAbsolute(evidence)
      || segments.some((segment) => !segment || segment === '.' || segment === '..')) {
    addFinding(findings, ruleId, '本地 approvalEvidence 必须是无路径穿越的相对文件路径');
    return;
  }
  if (!documentPath) return;
  const candidates = [];
  let base = path.dirname(path.resolve(documentPath));
  while (true) {
    candidates.push(path.resolve(base, evidence));
    const parent = path.dirname(base);
    if (parent === base) break;
    base = parent;
  }
  candidates.push(path.resolve(process.cwd(), evidence));
  const resolved = [...new Set(candidates)].find((candidate) => {
    try {
      const stat = fs.lstatSync(candidate);
      return stat.isFile() && !stat.isSymbolicLink();
    } catch {
      return false;
    }
  });
  if (!resolved) {
    addFinding(findings, ruleId, `本地审批证据不存在或不是普通文件：${evidence}`);
    return;
  }
  if (!fs.readFileSync(resolved, 'utf8').trim()) {
    addFinding(findings, ruleId, `本地审批证据不能为空：${evidence}`);
  }
}

function validateStructure(ast, contract, findings) {
  const h1 = ast.headings.filter((heading) => heading.level === 1);
  if (h1.length !== 1) addFinding(findings, contract.metadata.ruleId, `文档必须且只能有一个 H1，实际 ${h1.length} 个`);

  const actualTitles = ast.sections.map((section) => section.logicalTitle);
  const expectedTitles = contract.sections.map((section) => section.title);
  for (const sectionSpec of contract.sections) {
    const matches = ast.sections.filter((section) => section.logicalTitle === sectionSpec.title);
    if (matches.length === 0) addFinding(findings, sectionSpec.ruleId, `缺少章节：${sectionSpec.title}`);
    if (matches.length > 1) addFinding(findings, sectionSpec.ruleId, `章节重复：${sectionSpec.title}`);
  }
  for (const section of ast.sections) {
    if (!expectedTitles.includes(section.logicalTitle)) {
      addFinding(findings, contract.metadata.ruleId, `出现合同未定义的 H2 章节：${section.logicalTitle}`, section.line);
    }
  }
  const filteredActual = actualTitles.filter((title) => expectedTitles.includes(title));
  if (filteredActual.join('\u001f') !== expectedTitles.join('\u001f')) {
    addFinding(findings, contract.metadata.ruleId, 'H2 章节顺序与合同不一致');
  }
  for (const heading of ast.headings) {
    if (heading.level > 2) {
      const owner = [...contract.sections].reverse().find((section) => {
        const actual = findSection(ast, section.title);
        return actual && actual.line < heading.line;
      });
      addFinding(findings, owner?.ruleId ?? contract.metadata.ruleId, `不允许合同外的小章节：${heading.title}`, heading.line);
    }
  }

  for (const sectionSpec of contract.sections) {
    const section = findSection(ast, sectionSpec.title);
    if (!section) continue;
    for (const tableSpec of sectionSpec.tables) {
      const matches = section.tables.filter((table) => sameHeaders(table.headers, tableSpec.headers));
      if (matches.length === 0) addFinding(findings, sectionSpec.ruleId, `章节“${sectionSpec.title}”缺少表格：${tableSpec.headers.join(' | ')}`);
      if (matches.length > 1) addFinding(findings, sectionSpec.ruleId, `章节“${sectionSpec.title}”表格重复：${tableSpec.headers.join(' | ')}`);
    }
    const expectedKeys = new Set(sectionSpec.tables.map((table) => tableKey(table.headers)));
    for (const table of section.tables) {
      if (!expectedKeys.has(tableKey(table.headers))) {
        addFinding(findings, sectionSpec.ruleId, `章节“${sectionSpec.title}”出现合同外表格：${table.headers.join(' | ')}`, table.line);
      }
    }
  }
}

function materializeTables(ast, contract) {
  const entries = [];
  for (const sectionSpec of contract.sections) {
    const section = findSection(ast, sectionSpec.title);
    if (!section) continue;
    for (const tableSpec of sectionSpec.tables) {
      const table = findTable(section, tableSpec.headers);
      if (table) entries.push({ section, sectionSpec, table, tableSpec });
    }
  }
  return entries;
}

function validateTables(entries, findings) {
  for (const { sectionSpec, table, tableSpec } of entries) {
    if (table.malformedRows.length > 0) {
      for (const row of table.malformedRows) addFinding(findings, sectionSpec.ruleId, '表格数据列数与表头不一致', row.line);
    }
    if (table.rows.length < tableSpec.minRows) {
      addFinding(findings, sectionSpec.ruleId, `表格至少需要 ${tableSpec.minRows} 行数据，实际 ${table.rows.length} 行`, table.line);
    }
    const optional = new Set(tableSpec.optionalColumns ?? []);
    for (const row of table.rows) {
      for (const header of table.headers) {
        if (!optional.has(header) && isEmpty(row.values[header])) {
          addFinding(findings, sectionSpec.ruleId, `必填单元格为空：${header}`, row.line);
        }
      }
      for (const [column, allowed] of Object.entries(tableSpec.enums ?? {})) {
        if (!allowed.includes(row.values[column])) {
          addFinding(findings, sectionSpec.ruleId, `${column} 必须是 ${allowed.join('/')}，实际为 ${row.values[column]}`, row.line);
        }
      }
    }
    for (const [column, requiredValues] of Object.entries(tableSpec.requiredEnumValues ?? {})) {
      const actual = new Set(table.rows.map((row) => row.values[column]));
      for (const value of requiredValues) {
        if (!actual.has(value)) addFinding(findings, sectionSpec.ruleId, `${column} 缺少必需值：${value}`, table.line);
      }
    }
    if (tableSpec.rowKeys) {
      const keyColumn = table.headers[0];
      const actual = new Set(table.rows.map((row) => row.values[keyColumn]));
      for (const key of tableSpec.rowKeys) {
        if (!actual.has(key)) addFinding(findings, sectionSpec.ruleId, `门禁表缺少检查项：${key}`, table.line);
      }
    }
  }
}

function validateColumnRules(entries, contract, findings) {
  for (const rule of contract.columnRules ?? []) {
    const entry = entries.find(({ sectionSpec, table }) =>
      sectionSpec.title === rule.section && sameHeaders(table.headers, rule.headers));
    if (!entry) continue;
    for (const row of entry.table.rows) {
      for (const [column, checks] of Object.entries(rule.columns ?? {})) {
        const value = row.values[column] ?? '';
        for (const pattern of checks.forbiddenPatterns ?? []) {
          if (new RegExp(pattern, 'iu').test(value)) {
            addFinding(findings, rule.ruleId, `${column} 包含禁止设计：${value}`, row.line);
          }
        }
        for (const pattern of checks.requiredPatterns ?? []) {
          if (!new RegExp(pattern, 'iu').test(value)) {
            addFinding(findings, rule.ruleId, `${column} 不满足必需契约：${value}`, row.line);
          }
        }
      }
    }
  }
}

function collectDefinitions(entries, contract, findings) {
  const definitions = new Map(contract.localPrefixes.map((prefix) => [prefix, new Map()]));
  for (const { sectionSpec, table, tableSpec } of entries) {
    for (const [column, prefix] of Object.entries(tableSpec.defines ?? {})) {
      for (const row of table.rows) {
        const value = row.values[column];
        if (value === 'NONE') continue;
        if (!isExactIdentifier(value, prefix)) {
          addFinding(findings, sectionSpec.ruleId, `${column} 必须是唯一 ${prefix}-xxx：${value}`, row.line);
          continue;
        }
        const bucket = definitions.get(prefix);
        if (bucket.has(value)) {
          addFinding(findings, sectionSpec.ruleId, `ID 重复定义：${value}`, row.line);
        } else {
          bucket.set(value, { line: row.line, section: sectionSpec.title });
        }
      }
    }
  }
  return definitions;
}

function validateReferences(entries, contract, definitions, findings) {
  const local = new Set(contract.localPrefixes);
  for (const { sectionSpec, table, tableSpec } of entries) {
    for (const reference of tableSpec.references ?? []) {
      for (const row of table.rows) {
        const value = row.values[reference.column];
        if (reference.allowNone && value === 'NONE') continue;
        const tokens = scanIdentifiers(value);
        if (reference.required && tokens.length === 0) {
          addFinding(findings, sectionSpec.ruleId, `${reference.column} 必须引用 ${reference.prefixes.join('/')} ID`, row.line);
          continue;
        }
        for (const token of tokens) {
          if (!reference.prefixes.includes(token.prefix)) {
            addFinding(findings, sectionSpec.ruleId, `${reference.column} 不允许引用 ${token.id}`, row.line);
          } else if (local.has(token.prefix) && !definitions.get(token.prefix)?.has(token.id)) {
            addFinding(findings, sectionSpec.ruleId, `引用了未定义的本地 ID：${token.id}`, row.line);
          }
        }
      }
    }
  }

  const bodyTokens = scanIdentifiers(collectBodyText({ nodes: entries.flatMap(({ section }) => section.nodes) }));
  for (const token of bodyTokens) {
    if (local.has(token.prefix) && !definitions.get(token.prefix)?.has(token.id)) {
      addFinding(findings, contract.metadata.ruleId, `文档出现未定义的本地 ID：${token.id}`);
    }
  }
}

function validateTraceability(ast, contract, definitions, findings) {
  const trace = contract.traceability;
  if (!trace) return;
  const section = findSection(ast, trace.section);
  if (!section) return;
  const tokens = new Set();
  for (const table of section.tables) {
    for (const row of table.rows) scanIdentifiers(row.cells.join(' ')).forEach((token) => tokens.add(token.id));
  }
  for (const prefix of trace.localCoveragePrefixes) {
    for (const id of definitions.get(prefix)?.keys() ?? []) {
      if (!tokens.has(id)) addFinding(findings, trace.ruleId, `追踪矩阵未覆盖本地 ID：${id}`, section.line);
    }
  }
}

function validateBlocking(ast, contract, findings) {
  const blocking = contract.blocking;
  if (!blocking || ast.frontmatter.values.action !== 'NEXT') return;
  const section = findSection(ast, blocking.section);
  const table = findTable(section, blocking.headers);
  if (!table) return;
  for (const row of table.rows) {
    const state = row.values[blocking.stateColumn];
    const closed = blocking.closedValues.includes(state);
    const blockingValue = row.values[blocking.blockingColumn];
    const isBlocking = blocking.blockingValues
      ? blocking.blockingValues.includes(blockingValue)
      : blockingValue === '是';
    if ((isBlocking || state === 'BLOCKED') && !closed) {
      addFinding(findings, blocking.ruleId, `存在未关闭阻断项：${row.cells[0]}`, row.line);
    }
  }
}

function validateDependencyGraph(ast, contract, findings) {
  const graphSpec = contract.dependencyGraph;
  if (!graphSpec) return;
  const section = findSection(ast, graphSpec.section);
  const table = findTable(section, graphSpec.headers);
  if (!table) return;
  const graph = new Map();
  for (const row of table.rows) {
    const node = row.values[graphSpec.nodeColumn];
    const deps = row.values[graphSpec.dependencyColumn] === 'NONE'
      ? []
      : scanIdentifiers(row.values[graphSpec.dependencyColumn]).map((token) => token.id);
    graph.set(node, deps);
  }
  for (const [node, dependencies] of graph) {
    for (const dependency of dependencies) {
      if (!graph.has(dependency)) addFinding(findings, graphSpec.ruleId, `${node} 依赖不存在的任务 ${dependency}`);
      if (dependency === node) addFinding(findings, graphSpec.ruleId, `${node} 不能依赖自身`);
    }
  }
  const visiting = new Set();
  const visited = new Set();
  function visit(node, trail) {
    if (visiting.has(node)) {
      addFinding(findings, graphSpec.ruleId, `依赖形成循环：${[...trail, node].join(' -> ')}`);
      return;
    }
    if (visited.has(node) || !graph.has(node)) return;
    visiting.add(node);
    for (const dependency of graph.get(node)) visit(dependency, [...trail, node]);
    visiting.delete(node);
    visited.add(node);
  }
  for (const node of graph.keys()) visit(node, []);
}

function validateGate(ast, contract, findings) {
  if (ast.frontmatter.values.action !== 'NEXT') return;
  const gateSpec = contract.sections.at(-1);
  const section = findSection(ast, gateSpec.title);
  const tableSpec = gateSpec.tables[0];
  const table = findTable(section, tableSpec.headers);
  if (!table) return;
  for (const row of table.rows) {
    const key = row.values['检查项'];
    const result = row.values['结果'];
    if ((key.includes('checker') || key.includes('handoff') || key.includes('专项') || key.includes('依赖图')) && result !== 'PASS') {
      addFinding(findings, gateSpec.ruleId, `${key} 在 NEXT 前必须为 PASS`, row.line);
    }
    if (key.includes('未关闭阻断') && result !== '0') {
      addFinding(findings, gateSpec.ruleId, 'NEXT 前未关闭阻断数量必须为 0', row.line);
    }
    if (key.includes('审批') && result !== 'APPROVED') {
      addFinding(findings, gateSpec.ruleId, `${key} 在 NEXT 前必须为 APPROVED`, row.line);
    }
  }
}

function validateForbidden(ast, contract, findings) {
  const forbidden = contract.forbidden;
  if (!forbidden) return;
  if (forbidden.codeBlocks && ast.codeBlocks.length > 0) {
    for (const block of ast.codeBlocks) addFinding(findings, forbidden.ruleId, '本阶段不允许代码块', block.line);
  }
  const body = collectBodyText(ast).toLocaleLowerCase('en-US');
  const rawBody = ast.source.split(/\r?\n/u).slice(Math.max(ast.frontmatter.endLine + 1, 0)).join('\n');
  const compactBody = normalizeForbiddenText(`${body}\n${rawBody}`);
  for (const literal of forbidden.literals ?? []) {
    if (body.includes(literal.toLocaleLowerCase('en-US')) || compactBody.includes(normalizeForbiddenText(literal))) {
      addFinding(findings, forbidden.ruleId, `发现阶段禁止内容：${literal}`, findLiteralLine(ast, literal));
    }
  }
  for (const pattern of forbidden.patterns ?? []) {
    if (new RegExp(pattern, 'iu').test(rawBody)) {
      addFinding(findings, forbidden.ruleId, `发现阶段禁止模式：${pattern}`);
    }
  }
}

function normalizeForbiddenText(value) {
  return String(value ?? '')
    .normalize('NFKC')
    .toLocaleLowerCase('en-US')
    .replace(/[*_`~\s]+/gu, '');
}

function validatePlaceholders(ast, contract, findings) {
  if (containsPlaceholder(ast.source)) addFinding(findings, contract.metadata.ruleId, '文档仍包含 {{...}} 模板占位符');
}

export function validateDocument(source, contract, options = {}) {
  const ast = parseMarkdown(source);
  const findings = [];
  if (options.checkAssets !== false) findings.push(...validateContractRuleLinks(contract));
  for (const error of ast.errors) addFinding(findings, contract.metadata.ruleId, error.message, error.line);
  validateMetadata(ast, contract, findings, options);
  validatePlaceholders(ast, contract, findings);
  validateStructure(ast, contract, findings);
  const entries = materializeTables(ast, contract);
  validateTables(entries, findings);
  validateColumnRules(entries, contract, findings);
  const definitions = collectDefinitions(entries, contract, findings);
  validateReferences(entries, contract, definitions, findings);
  validateTraceability(ast, contract, definitions, findings);
  validateBlocking(ast, contract, findings);
  validateDependencyGraph(ast, contract, findings);
  validateGate(ast, contract, findings);
  validateForbidden(ast, contract, findings);
  return { ast, definitions, findings };
}

export function definedIds(result, prefixes = null) {
  const allowed = prefixes ? new Set(prefixes) : null;
  const ids = new Set();
  for (const [prefix, bucket] of result.definitions) {
    if (!allowed || allowed.has(prefix)) for (const id of bucket.keys()) ids.add(id);
  }
  return ids;
}

export function traceSectionIds(result, contract, prefixes) {
  const section = findSection(result.ast, contract.traceability.section);
  const allowed = new Set(prefixes);
  const ids = new Set();
  for (const table of section?.tables ?? []) {
    for (const row of table.rows) {
      for (const token of scanIdentifiers(row.cells.join(' '))) {
        if (allowed.has(token.prefix)) ids.add(token.id);
      }
    }
  }
  return ids;
}

export function documentIds(result, prefixes) {
  const allowed = new Set(prefixes);
  const ids = new Set();
  for (const token of scanIdentifiers(collectBodyText(result.ast))) {
    if (allowed.has(token.prefix)) ids.add(token.id);
  }
  return ids;
}
