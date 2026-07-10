import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';

export const TEST_TYPES = new Set(['UNIT', 'API', 'UI']);
export const OBLIGATIONS = new Set(['STATIC_REVIEW', ...TEST_TYPES]);
export const RISKS = new Set(['R0', 'R1', 'R2', 'R3']);

const REQUIRED_OBLIGATIONS = {
  R0: ['STATIC_REVIEW'],
  R1: ['STATIC_REVIEW', 'UNIT'],
  R2: ['STATIC_REVIEW', 'UNIT', 'API'],
  R3: ['STATIC_REVIEW', 'UNIT', 'API', 'UI']
};

export function computeFilesDigest(artifacts) {
  const hash = crypto.createHash('sha256');
  for (const artifact of [...artifacts].sort((a, b) => a.path.localeCompare(b.path))) {
    hash.update(artifact.path);
    hash.update('\0');
    hash.update(artifact.content);
    hash.update('\0');
  }
  return `sha256:${hash.digest('hex')}`;
}

export function loadArtifacts(root, files) {
  return files.map((file) => {
    const normalized = normalizePath(file);
    return {
      path: normalized,
      content: fs.readFileSync(path.join(root, normalized), 'utf8')
    };
  });
}

export function analyzeArtifacts({ artifacts, contract = null, now = new Date() }) {
  const issues = [];
  if (contract) validateContract(contract, artifacts, now, issues);
  for (const artifact of artifacts) {
    const file = normalizePath(artifact.path);
    const text = artifact.content;
    if (isJavaTest(file)) analyzeJavaTest(file, text, contract, issues);
    if (file.endsWith('.java') && !isJavaTest(file)) analyzeJavaSource(file, text, issues);
    if (file.endsWith('pom.xml')) analyzePom(file, text, issues);
    if (isUiSpec(file)) analyzeUiSpec(file, text, issues);
    if (file.endsWith('.vue')) analyzeVuePage(file, text, issues);
  }
  analyzeBaselineLayout(artifacts, issues);
  return dedupeIssues(issues);
}

export function analyzeLegacyArtifacts({ artifacts }) {
  const issues = [];
  for (const artifact of artifacts) {
    const file = normalizePath(artifact.path);
    if (!isJavaTest(file)) continue;
    const text = stripComments(artifact.content);
    const className = path.posix.basename(file, '.java');
    const testedName = className
      .replace(/IntegrationTest$/, '')
      .replace(/E2ETest$/, '')
      .replace(/UnitTest$/, '')
      .replace(/Test$/, '');
    if (testedName && new RegExp(`\\b(?:mock|spy)\\s*\\([^)]*\\b${escapeRegex(testedName)}\\b`).test(text)) {
      pushIssue(issues, 'LEGACY-MOCK-001', file, text, text.search(/\b(?:mock|spy)\s*\(/),
        `测试 mock 了按类名推断的被测目标 ${testedName}`, '让被测目标真实执行');
    }
    if (/\/(?:service\/impl|resource)\//.test(file) && /\bmock\s*\(\s*[\w.]*\w+Mapper\.class/.test(text)) {
      pushIssue(issues, 'LEGACY-MOCK-002', file, text, text.search(/\bmock\s*\(/),
        'service/resource 测试 mock Mapper', '使用隔离数据库和真实 Mapper');
    }
  }
  return dedupeIssues(issues);
}

function validateContract(contract, artifacts, now, issues) {
  const file = 'quality-contract.json';
  if (contract.schemaVersion !== 1) {
    pushIssue(issues, 'PQT-CONTRACT-001', file, '', 0, 'schemaVersion 必须为 1', '按当前 schema 重新生成契约');
  }
  if (!contract.change || !Array.isArray(contract.change.files) || contract.change.files.length === 0) {
    pushIssue(issues, 'PQT-CONTRACT-002', file, '', 0, 'change.files 缺失或为空', '绑定真实 Git 变更文件');
  } else {
    if (!/^[0-9a-f]{7,40}$/.test(contract.change.base || '') || !/^[0-9a-f]{7,40}$/.test(contract.change.head || '')) {
      pushIssue(issues, 'PQT-CONTRACT-003', file, '', 0, 'base/head 必须是已解析的 Git 提交哈希', '由生成器解析 Git 引用');
    }
    const requested = new Set(contract.change.files.map(normalizePath));
    const boundArtifacts = artifacts.filter((artifact) => requested.has(normalizePath(artifact.path)));
    if (boundArtifacts.length !== requested.size) {
      pushIssue(issues, 'PQT-CONTRACT-004', file, '', 0, '质量契约引用了不存在或未参与校验的文件', '补齐文件并重新生成契约');
    } else if (contract.change.filesDigest !== computeFilesDigest(boundArtifacts)) {
      pushIssue(issues, 'PQT-CONTRACT-005', file, '', 0, 'filesDigest 与真实文件内容不一致', '变更后重新生成质量契约');
    }
  }
  if (!Array.isArray(contract.capabilities) || contract.capabilities.length === 0) {
    pushIssue(issues, 'PQT-CONTRACT-006', file, '', 0, '至少需要一个能力契约', '登记能力、验收结果和证明路径');
    return;
  }
  for (const capability of contract.capabilities) {
    const id = capability?.id || '<unknown>';
    if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(id)) {
      pushIssue(issues, 'PQT-CONTRACT-007', file, '', 0, `能力 ID 非 kebab-case: ${id}`, '使用稳定的 kebab-case 能力 ID');
    }
    if (!Array.isArray(capability.acceptance) || capability.acceptance.length === 0 || capability.acceptance.some(isWeakAcceptance)) {
      pushIssue(issues, 'PQT-CONTRACT-008', file, '', 0, `能力 ${id} 缺少可观察验收结果`, '描述输入、业务状态变化或用户可见结果');
    }
    if (!RISKS.has(capability.risk)) {
      pushIssue(issues, 'PQT-CONTRACT-009', file, '', 0, `能力 ${id} 使用未知风险 ${capability.risk}`, '仅使用 R0/R1/R2/R3');
      continue;
    }
    const obligations = Array.isArray(capability.obligations) ? capability.obligations : [];
    const unknown = obligations.filter((item) => !OBLIGATIONS.has(item));
    if (unknown.length > 0) {
      pushIssue(issues, 'PQT-CONTRACT-010', file, '', 0, `能力 ${id} 使用未知测试/义务类型: ${unknown.join(', ')}`, '正式测试只使用 UNIT/API/UI');
    }
    const missing = REQUIRED_OBLIGATIONS[capability.risk].filter((item) => !obligations.includes(item));
    if (missing.length > 0) {
      pushIssue(issues, 'PQT-CONTRACT-011', file, '', 0, `能力 ${id} 缺少 ${capability.risk} 最低义务: ${missing.join(', ')}`, '由生成器恢复最低义务');
    }
    if (capability.risk === 'R0' && obligations.some((item) => TEST_TYPES.has(item))) {
      pushIssue(issues, 'PQT-NOOP-001', file, '', 0, `R0 能力 ${id} 不应要求自动化测试`, '使用静态 Review，删除机械测试义务');
    }
    const proofPath = Array.isArray(capability.protectedProofPath) ? capability.protectedProofPath : [];
    if (capability.risk !== 'R0' && proofPath.length < 2) {
      pushIssue(issues, 'PQT-PROOF-001', file, '', 0, `能力 ${id} 的受保护证明路径不足两个节点`, '登记真实入口、关键决策和结果节点');
    }
    for (const double of capability.allowedExternalDoubles || []) {
      if (!double?.target || !double?.reason || double.reason.trim().length < 8) {
        pushIssue(issues, 'PQT-MOCK-001', file, '', 0, `能力 ${id} 的外部替身缺少精确目标或理由`, '登记外部目标和稳定测试所需理由');
      }
      if (proofPath.some((node) => sameTarget(node, double?.target))) {
        pushIssue(issues, 'PQT-MOCK-002', file, '', 0, `外部替身 ${double.target} 命中受保护证明路径`, '移除替身，让受保护节点真实执行');
      }
    }
    for (const exception of capability.exceptions || []) validateException(id, exception, now, issues);
  }
}

function validateException(id, exception, now, issues) {
  const required = ['rule', 'target', 'reason', 'alternativeEvidence', 'owner', 'approver', 'expiresAt'];
  const missing = required.filter((key) => !String(exception?.[key] || '').trim());
  if (missing.length > 0) {
    pushIssue(issues, 'PQT-EXCEPTION-001', 'quality-contract.json', '', 0,
      `能力 ${id} 的例外缺少字段: ${missing.join(', ')}`, '补齐责任、批准、替代证据和到期日');
    return;
  }
  const expires = new Date(`${exception.expiresAt}T23:59:59Z`);
  if (Number.isNaN(expires.getTime()) || expires < now) {
    pushIssue(issues, 'PQT-EXCEPTION-002', 'quality-contract.json', '', 0,
      `能力 ${id} 的例外已到期或日期无效: ${exception.expiresAt}`, '删除例外或重新审批新的短期例外');
  }
  if (/PQT-(?:MOCK-002|BASELINE-003|PROOF-001)/.test(exception.rule)) {
    pushIssue(issues, 'PQT-EXCEPTION-003', 'quality-contract.json', '', 0,
      `规则 ${exception.rule} 不允许例外`, '恢复受保护路径或基线写保护');
  }
}

function analyzeJavaTest(file, rawText, contract, issues) {
  const text = stripComments(rawText);
  const className = path.posix.basename(file, '.java');
  const testedName = className.replace(/(?:Integration|E2E|Unit|Api)?Test$/, '');
  const literalAssert = /\bassertEquals\s*\(\s*([^,()]+)\s*,\s*\1\s*\)/g;
  for (const match of text.matchAll(literalAssert)) {
    pushIssue(issues, 'PQT-NOOP-002', file, text, match.index, '断言把同一表达式与自身比较', '删除测试或断言可观察业务结果');
  }
  const constants = new Map();
  for (const match of text.matchAll(/\b(?:static\s+)?final\s+[\w<>?,.]+\s+(\w+)\s*=\s*([^;]+);/g)) {
    constants.set(match[1], match[2].trim());
  }
  for (const [name, value] of constants) {
    const patterns = [
      new RegExp(`\\bassertEquals\\s*\\(\\s*${escapeRegex(value)}\\s*,\\s*${escapeRegex(name)}\\s*\\)`),
      new RegExp(`\\bassertThat\\s*\\(\\s*${escapeRegex(name)}\\s*\\)\\s*\\.isEqualTo\\s*\\(\\s*${escapeRegex(value)}\\s*\\)`)
    ];
    const found = patterns.map((pattern) => text.search(pattern)).find((index) => index >= 0);
    if (found !== undefined) {
      pushIssue(issues, 'PQT-NOOP-003', file, text, found, `测试只证明常量 ${name} 等于其声明值`, '由静态 Review 覆盖常量声明');
    }
  }
  for (const setter of text.matchAll(/\b(\w+)\.set([A-Z]\w*)\s*\(\s*([^;)]+)\s*\)\s*;/g)) {
    const [, objectName, property, value] = setter;
    const getterAssert = new RegExp(`assertEquals\\s*\\(\\s*${escapeRegex(value.trim())}\\s*,\\s*${escapeRegex(objectName)}\\.get${property}\\s*\\(\\s*\\)\\s*\\)`);
    const index = text.search(getterAssert);
    if (index >= 0 && countBusinessCalls(text, objectName, property) <= 2) {
      pushIssue(issues, 'PQT-NOOP-004', file, text, index, `测试只复述 ${property} 的 setter/getter`, '删除机械访问器测试');
    }
  }
  if (/\bassertDoesNotThrow\s*\(/.test(text) && !hasMeaningfulJavaAssertion(text.replace(/assertDoesNotThrow/g, ''))) {
    pushIssue(issues, 'PQT-NOOP-005', file, text, text.search(/\bassertDoesNotThrow/), '测试只断言不抛异常', '断言业务状态、返回值或副作用');
  }
  if (/\bassertNotNull\s*\(/.test(text) && !hasMeaningfulJavaAssertion(text.replace(/assertNotNull/g, ''))) {
    pushIssue(issues, 'PQT-NOOP-006', file, text, text.search(/\bassertNotNull/), '测试只断言对象非空', '断言关键业务语义');
  }
  if (testedName) {
    const selfMock = new RegExp(`(?:\\b(?:mock|spy)\\s*\\([^)]*\\b${escapeRegex(testedName)}\\b|@(?:Mock|MockBean|Spy|SpyBean)\\b[\\s\\S]{0,120}\\b${escapeRegex(testedName)}\\b)`);
    const index = text.search(selfMock);
    if (index >= 0) {
      pushIssue(issues, 'PQT-MOCK-003', file, text, index, `测试替换了被测目标 ${testedName}`, '实例化并执行真实被测目标');
    }
  }
  for (const node of protectedNodes(contract)) {
    const target = simpleName(node);
    if (target.length < 3) continue;
    const mockPattern = new RegExp(`(?:\\b(?:mock|spy)\\s*\\(\\s*(?:new\\s+)?(?:[\\w.]+\\.)?${escapeRegex(target)}\\b|@(?:Mock|MockBean|Spy|SpyBean)\\b[\\s\\S]{0,100}\\b${escapeRegex(target)}\\b)`);
    const index = text.search(mockPattern);
    if (index >= 0) {
      pushIssue(issues, 'PQT-MOCK-004', file, text, index, `测试替换了受保护证明节点 ${node}`, '让证明路径节点真实执行');
    }
  }
  const apiClaim = /(?:Api|API|E2E)Test\.java$/.test(file) || /@Tag\s*\(\s*"(?:api|e2e)"/.test(text);
  if (apiClaim) {
    const hasRealEntry = /\b(?:MockMvc|WebTestClient|TestRestTemplate|RestAssured|HttpClient|KafkaTemplate|ApplicationEventPublisher|JobLauncher)\b/.test(text);
    if (!hasRealEntry && /\b\w+Service\b/.test(text)) {
      pushIssue(issues, 'PQT-API-001', file, text, text.search(/\b\w+Service\b/), 'API 测试直接调用 Service，未经过真实入口', '使用 HTTP/Event/Consumer/Job/CLI 测试入口');
    }
    const internalMock = /@(?:Mock|MockBean|Spy|SpyBean)\b[\s\S]{0,120}\b\w+(?:Service|Mapper)\b|\bmock\s*\(\s*[\w.]*\w+(?:Service|Mapper)\.class/.exec(text);
    if (internalMock) {
      pushIssue(issues, 'PQT-API-002', file, text, internalMock.index, 'API 测试替换了 Mango 内部 Service/Mapper 链路', '使用隔离环境执行真实内部链路');
    }
  }
  if (/\bverify\s*\(/.test(text) && !hasMeaningfulJavaAssertion(text)) {
    pushIssue(issues, 'PQT-NOOP-007', file, text, text.search(/\bverify\s*\(/), '测试只验证 Mock 调用次数', '断言可观察业务结果');
  }
}

function analyzeJavaSource(file, rawText, issues) {
  const text = stripComments(rawText);
  if (/(?:class|record)\s+\w*Controller\b|@RestController\b/.test(text)) {
    const mapper = /\b(?:private|protected|public)?\s*(?:final\s+)?[\w.]*\w+Mapper\s+\w+\s*;|\([^)]*\b\w+Mapper\s+\w+/.exec(text);
    if (mapper) pushIssue(issues, 'PQT-JAVA-001', file, text, mapper.index, 'Controller 直接依赖 Mapper', '通过应用 Service/UseCase 访问持久化');
    for (const mapping of text.matchAll(/@(Post|Put|Patch)Mapping\b[\s\S]{0,500}?\([^)]*@RequestBody\s+(?!@Valid\b)(?!@Validated\b)[^)]*\)/g)) {
      pushIssue(issues, 'PQT-JAVA-002', file, text, mapping.index, '写接口的 RequestBody 缺少参数校验', '在请求参数上增加 @Valid 或等价校验');
    }
  }
  if (/(?:class\s+\w*ServiceImpl\b|@Service\b)/.test(text) && !/@Transactional\b/.test(text)) {
    const writeCall = /\.(?:save|saveBatch|insert|update|updateById|delete|deleteById|remove|removeById)\s*\(/.exec(text);
    if (writeCall) pushIssue(issues, 'PQT-JAVA-003', file, text, writeCall.index, 'Service 写链路缺少可识别事务边界', '在写方法或服务类上声明 @Transactional');
  }
  for (const sql of text.matchAll(/@(Update|Delete)\s*\(\s*"([^"]+)"/gi)) {
    if (!/\bwhere\b/i.test(sql[2])) {
      pushIssue(issues, 'PQT-JAVA-004', file, text, sql.index, `${sql[1]} SQL 缺少 WHERE`, '增加明确且可验证的条件');
    }
  }
}

function analyzePom(file, text, issues) {
  const moduleKind = file.match(/mango-[^/]+-(api|core|starter|starter-remote)\/pom\.xml$/)?.[1];
  if (!moduleKind) return;
  const artifacts = [...text.matchAll(/<dependency>([\s\S]*?)<\/dependency>/g)]
    .map((match) => ({
      name: match[1].match(/<artifactId>\s*([^<]+)\s*<\/artifactId>/)?.[1]?.trim() || '',
      scope: match[1].match(/<scope>\s*([^<]+)\s*<\/scope>/)?.[1]?.trim() || 'compile',
      index: match.index
    }))
    .filter((artifact) => artifact.name && artifact.scope !== 'test');
  for (const artifact of artifacts) {
    const dependencyKind = artifact.name.match(/-(api|core|starter|starter-remote)$/)?.[1];
    const invalid = (moduleKind === 'api' && ['core', 'starter', 'starter-remote'].includes(dependencyKind))
      || (moduleKind === 'core' && ['starter', 'starter-remote'].includes(dependencyKind))
      || (moduleKind === 'starter-remote' && dependencyKind === 'core');
    if (invalid) {
      pushIssue(issues, 'PQT-JAVA-005', file, text, artifact.index,
        `${moduleKind} 模块非法依赖 ${artifact.name}`, '依赖公开 api 契约并保持装配/实现方向单向');
    }
  }
}

function analyzeUiSpec(file, rawText, issues) {
  const text = stripComments(rawText);
  const ownApi = /\b(?:page|context)\.route\s*\(\s*(['"`])([^'"`]*(?:\/api\/|\/mango\/|\/admin-api\/)[^'"`]*)\1[\s\S]{0,500}?\b(?:fulfill|json|body)\b/g;
  for (const match of text.matchAll(ownApi)) {
    pushIssue(issues, 'PQT-UI-001', file, text, match.index, `UI/E2E 伪造 Mango 自有 API: ${match[2]}`, '启动真实后端并保留第三方边界替身');
  }
  const forbidden = [
    ['PQT-UI-002', /(['"`])\.el-[^'"`]+\1/, 'spec 直接依赖 Element Plus 内部 class', '使用角色、标签或业务语义锚点'],
    ['PQT-UI-003', /\.waitForTimeout\s*\(/, 'spec 使用固定等待', '等待可观察页面或网络状态'],
    ['PQT-UI-004', /\bforce\s*:\s*true\b/, 'spec 使用 force 绕过真实可交互性', '修复遮挡、禁用或定位问题'],
    ['PQT-UI-005', /\.(?:nth|first|last)\s*\(/, 'spec 依赖 DOM 顺序', '使用稳定业务记录或字段锚点']
  ];
  for (const [rule, pattern, message, fix] of forbidden) {
    const index = text.search(pattern);
    if (index >= 0) pushIssue(issues, rule, file, text, index, message, fix);
  }
  if (/\btoHaveScreenshot\s*\(/.test(text) && !hasUiBusinessAssertion(text.replace(/toHaveScreenshot/g, ''))) {
    pushIssue(issues, 'PQT-UI-006', file, text, text.search(/\btoHaveScreenshot/), 'UI 用例只有截图，没有业务结果断言', '先断言用户可见业务结果，再保存截图');
  }
}

function analyzeVuePage(file, rawText, issues) {
  if (!/\/(?:views|pages)\//.test(file)) return;
  const text = stripComments(rawText);
  if (!/\bdata-page\s*=/.test(text)) {
    pushIssue(issues, 'PQT-WEB-001', file, text, 0, '页面缺少稳定 data-page 能力锚点', '在真实页面根节点声明业务能力 data-page');
  }
  const hidden = /(?:display\s*:\s*none|visibility\s*:\s*hidden|v-show\s*=\s*["']false["'])[\s\S]{0,240}data-(?:page|surface|field)|data-(?:page|surface|field)[\s\S]{0,240}(?:display\s*:\s*none|visibility\s*:\s*hidden|v-show\s*=\s*["']false["'])/.exec(text);
  if (hidden) pushIssue(issues, 'PQT-WEB-002', file, text, hidden.index, '隐藏语义锚点不能证明页面真实合规', '让标准组件在实际页面承担可见职责');
  if (/<el-form\b/.test(text) && !/\bdata-surface\s*=/.test(text)) {
    pushIssue(issues, 'PQT-WEB-003', file, text, text.search(/<el-form\b/), '表单缺少稳定 data-surface 业务区域锚点', '为真实表单声明业务语义 surface');
  }
  if (/<el-form-item\b/.test(text) && !/\bdata-field\s*=/.test(text)) {
    pushIssue(issues, 'PQT-WEB-004', file, text, text.search(/<el-form-item\b/), '表单字段缺少稳定 data-field 锚点', '为关键字段声明业务语义 field');
  }
}

function analyzeBaselineLayout(artifacts, issues) {
  const roots = new Map();
  for (const artifact of artifacts) {
    const file = normalizePath(artifact.path);
    const match = file.match(/^mango-docs\/evidence\/test-baseline\/([^/]+)\/(unit|api|ui)\/([^/]+)\//);
    if (!match) continue;
    const key = `${match[1]}/${match[2]}`;
    if (!roots.has(key)) roots.set(key, new Set());
    roots.get(key).add(match[3]);
  }
  for (const [key, versions] of roots) {
    if (versions.size > 1 || !versions.has('latest')) {
      pushIssue(issues, 'PQT-BASELINE-001', `mango-docs/evidence/test-baseline/${key}`, '', 0,
        `正式树必须只保留 latest，实际为: ${[...versions].join(', ')}`, '删除过程版本，只通过 baseline promote 替换 latest');
    }
  }
}

function protectedNodes(contract) {
  if (!contract?.capabilities) return [];
  return contract.capabilities.flatMap((capability) => capability.protectedProofPath || []);
}

function isWeakAcceptance(value) {
  const text = String(value || '').trim();
  return text.length < 8 || /^(?:通过|正常|可用|完成|无报错|TODO|TBD|待补充)$/i.test(text);
}

function sameTarget(left, right) {
  const a = simpleName(left).toLowerCase();
  const b = simpleName(right).toLowerCase();
  return a && b && (a === b || a.includes(b) || b.includes(a));
}

function simpleName(value) {
  return String(value || '').split(/[.#/:]/).filter(Boolean).pop()?.replace(/\(.*$/, '') || '';
}

function hasMeaningfulJavaAssertion(text) {
  return /\bassert(?:Equals|True|False|Throws|Same|ArrayEquals)\s*\(|\bassertThat\s*\(/.test(text);
}

function hasUiBusinessAssertion(text) {
  return /\bexpect\s*\([^)]*(?:data-state|data-record-key|data-field|getByRole|getByLabel|getByText)[\s\S]{0,240}?\.(?:toHaveText|toContainText|toHaveValue|toHaveAttribute|toBeVisible|toBeEnabled|toBeDisabled)\s*\(/.test(text)
    || /\bexpect\.poll\s*\(/.test(text);
}

function countBusinessCalls(text, objectName, property) {
  const matches = text.match(new RegExp(`\\b${escapeRegex(objectName)}\\.(?:set|get)${escapeRegex(property)}\\s*\\(`, 'g'));
  return matches?.length || 0;
}

function isJavaTest(file) {
  return file.endsWith('.java') && file.includes('/src/test/');
}

function isUiSpec(file) {
  return /(?:e2e|playwright|specs|tests|__tests__)\//.test(file) && /\.(?:spec|test)\.[cm]?[jt]sx?$/.test(file);
}

function stripComments(text) {
  return text
    .replace(/\/\*[\s\S]*?\*\//g, (match) => match.replace(/[^\n]/g, ' '))
    .replace(/(^|[^:])\/\/.*$/gm, '$1');
}

function pushIssue(issues, rule, file, text, index, message, fix) {
  issues.push({
    severity: 'BLOCK',
    rule,
    file,
    line: lineNumber(text, Math.max(0, index || 0)),
    message,
    fix
  });
}

function lineNumber(text, index) {
  return text ? text.slice(0, index).split('\n').length : 1;
}

function dedupeIssues(issues) {
  const seen = new Set();
  return issues.filter((issue) => {
    const key = `${issue.rule}:${issue.file}:${issue.line}:${issue.message}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function normalizePath(value) {
  return String(value).replaceAll('\\', '/').replace(/^\.\//, '');
}

function escapeRegex(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
