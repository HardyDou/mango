#!/usr/bin/env node
import { execFileSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

/**
 * workflow 契约指纹发布块（Issue #1312）。
 *
 * 背景：`WorkflowApiSurfaceContractTest` 已经给公开 API 与 HTTP 端点算了 SHA-256 指纹，
 * 但业务仓看不到这些常量，每次升级只能人工逐个核对方法签名。1.12.0 发布时那组常量还是
 * 从未被执行验证过的陈旧值（当时 Surefire 2.17 不发现 JUnit 5 用例），业务侧因此完全
 * 无法判断"这次升级到底动没动契约"。
 *
 * 本工具把"发布说明里应该写什么"变成一条可执行命令：
 *
 * 1. 从 Java 契约测试里读 `API_FINGERPRINT` / `HTTP_FINGERPRINT` 常量（与门禁同一事实源）；
 * 2. 与上一发布批次的值比对，得出 `changed: none|api|web|both`；
 * 3. 扫描 `mango-workflow-api` 的 VO/Query/Request 与枚举 diff，抽出新增加字段与枚举值——
 *    这两类变更不改变指纹，但会让业务侧静默出错，必须单独列出；
 * 4. 输出可直接粘进 release notes 的 YAML 块，禁止手抄。
 */

const CONTRACT_TEST_RELATIVE =
  'mango/mango-platform/mango-workflow/mango-workflow-starter/src/test/java/io/mango/workflow/starter/controller/WorkflowApiSurfaceContractTest.java';

const API_SOURCE_ROOT = 'mango/mango-platform/mango-workflow/mango-workflow-api/src/main/java/io/mango/workflow/api';
const COMPATIBLE_SOURCE_DIRS = ['vo', 'query', 'request', 'enums'];

// 契约指纹只由「公开 API 接口的方法 + Controller 端点」决定。这两处改了而常量没改，
// 说明有人绕过了 `Workflow API contract sentinel`，发布说明里的指纹就不可信（1.12.0 正是这样）。
const CONTRACT_SOURCE_PATHS = [
  `${API_SOURCE_ROOT}`,
  'mango/mango-platform/mango-workflow/mango-workflow-starter/src/main/java/io/mango/workflow/starter/controller'
];

const FINGERPRINT_PATTERN = /(API|HTTP)_FINGERPRINT\s*=\s*"([0-9a-f]{64})"/g;
const FIELD_PATTERN =
  /^\s*(?:(?:private|protected|public|static|final|transient|volatile)\s+)*[A-Za-z_$][\w.$]*(?:\s*\[\s*\])?\s+([A-Za-z_$][\w$]*)\s*(?:=[^;]*)?;/;

/**
 * 字段声明可能被格式化拆成多行（`private List<@NotBlank String> businessTypes;` 的注解会跨行），
 * 所以先去掉泛型参数再取变量名，取不到就返回 null。
 */
export function extractFieldName(declaration) {
  // 去掉注解（`@Schema(...)`）与泛型参数，剩下 `private List businessTypes;` 这样的骨架。
  let normalized = declaration.replace(/@[\w.$]+(?:\s*\((?:[^()]|\([^()]*\))*\))?/g, ' ');
  for (let round = 0; round < 4 && normalized.includes('<'); round += 1) {
    normalized = normalized.replace(/<[^<>]*>/g, ' ');
  }
  const matched = FIELD_PATTERN.exec(normalized);
  return matched ? matched[1] : null;
}
const ENUM_CONSTANT_PATTERN = /^\+\s*([A-Z][A-Z0-9_]*)\s*(?:[(,;]|$)/;

function parseArgs(argv) {
  const args = { base: '', head: 'HEAD', root: '', json: false, changedOnly: false, verifyReleaseNotes: '' };
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--base') {
      args.base = argv[index + 1] || '';
      index += 1;
    } else if (arg === '--head') {
      args.head = argv[index + 1] || 'HEAD';
      index += 1;
    } else if (arg === '--root') {
      args.root = argv[index + 1] || '';
      index += 1;
    } else if (arg === '--json') {
      args.json = true;
    } else if (arg === '--changed-only') {
      args.changedOnly = true;
    } else if (arg === '--verify-release-notes') {
      args.verifyReleaseNotes = argv[index + 1] || '';
      index += 1;
    }
  }
  return args;
}

function resolveRepositoryRoot(explicitRoot) {
  if (explicitRoot) {
    return path.resolve(explicitRoot);
  }
  return path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
}

function git(repositoryRoot, args) {
  return execFileSync('git', args, { cwd: repositoryRoot, encoding: 'utf8', maxBuffer: 32 * 1024 * 1024 });
}

/**
 * 从契约测试源码里取出两个指纹。返回 null 表示该常量缺失（文件改过名或被删）。
 */
export function parseFingerprints(source) {
  const fingerprints = { api: null, web: null };
  FINGERPRINT_PATTERN.lastIndex = 0;
  let matched = FINGERPRINT_PATTERN.exec(source);
  while (matched) {
    const [, kind, value] = matched;
    fingerprints[kind === 'API' ? 'api' : 'web'] = value;
    matched = FINGERPRINT_PATTERN.exec(source);
  }
  if (fingerprints.api && fingerprints.web) {
    return fingerprints;
  }
  // 1.13.0 之前指纹写在内联断言里（`assertThat(apiFingerprint()).isEqualTo("...")`），
  // 没有常量可读。按文件出现顺序兜底：第一个 64 位十六进制是 API，第二个是 HTTP。
  const literals = [...source.matchAll(/"([0-9a-f]{64})"/g)].map((entry) => entry[1]);
  fingerprints.api = fingerprints.api ?? literals[0] ?? null;
  fingerprints.web = fingerprints.web ?? literals[1] ?? null;
  return fingerprints;
}

export function describeChange(baseFingerprints, headFingerprints) {
  const apiChanged = baseFingerprints.api !== headFingerprints.api;
  const webChanged = baseFingerprints.web !== headFingerprints.web;
  if (apiChanged && webChanged) {
    return 'both';
  }
  if (apiChanged) {
    return 'api';
  }
  return webChanged ? 'web' : 'none';
}

/**
 * 从 unified diff 里抽「类名 → 新增字段 / 新增枚举值」。
 *
 * 只看 `+++ b/...` 目标路径落在 workflow API 的 VO/Query/Request 与 enums 目录下的文件；
 * 删除的字段与枚举值不列出——业务侧关心的是"我还能不能编译/分支要不要补"，
 * 删除会直接编译失败，不需要靠发布说明发现。
 */
export function extractCompatibleAdditions(diffText) {
  const additions = new Map();
  let currentFile = null;
  let currentClass = null;
  let currentKind = null;
  let pendingDeclaration = '';

  const lines = diffText.split('\n');
  for (const line of lines) {
    if (line.startsWith('+++ ')) {
      const target = line.slice(4).replace(/^b\//, '').trim();
      currentFile = target === '/dev/null' ? null : target;
      currentClass = null;
      currentKind = null;
      pendingDeclaration = '';
      if (!currentFile) {
        continue;
      }
      const inScope = COMPATIBLE_SOURCE_DIRS.some(
        (directory) => currentFile.includes(`/${directory}/`) && currentFile.endsWith('.java')
      );
      if (!inScope) {
        currentFile = null;
        continue;
      }
      currentClass = path.basename(currentFile, '.java');
      currentKind = currentFile.includes('/enums/') ? 'enum' : 'field';
      additions.set(currentClass, new Set());
      continue;
    }
    if (!currentFile || !currentClass) {
      continue;
    }
    if (currentKind === 'enum') {
      const enumMatched = ENUM_CONSTANT_PATTERN.exec(line);
      if (enumMatched) {
        additions.get(currentClass).add(enumMatched[1]);
      }
      continue;
    }
    if (!line.startsWith('+')) {
      // 非新增行（含 hunk 头）一律打断续行缓冲：声明不会跨 hunk 拼接。
      pendingDeclaration = '';
      continue;
    }
    pendingDeclaration += ` ${line.slice(1).trim()}`;
    if (!pendingDeclaration.includes(';')) {
      if (pendingDeclaration.length > 600) {
        pendingDeclaration = '';
      }
      continue;
    }
    const fieldName = extractFieldName(pendingDeclaration);
    if (fieldName) {
      additions.get(currentClass).add(fieldName);
    }
    pendingDeclaration = '';
  }

  return [...additions.entries()]
    .filter(([, names]) => names.size > 0)
    .map(([className, names]) => ({ className, names: [...names].sort() }))
    .sort((left, right) => left.className.localeCompare(right.className));
}

/**
 * 找出「契约源码变了但指纹常量没变」的范围——门禁没跑过时的可观测信号。
 */
export function detectUnverifiedContractChanges(changedFiles, baseFingerprints, headFingerprints) {
  const contractChanged = changedFiles.some((file) => {
    if (file.includes('/controller/') && file.endsWith('Controller.java')) {
      return true;
    }
    return /\/api\/[A-Za-z]+Api\.java$/.test(file);
  });
  if (!contractChanged) {
    return [];
  }
  const fingerprintsUnchanged = baseFingerprints.api === headFingerprints.api
    && baseFingerprints.web === headFingerprints.web;
  return fingerprintsUnchanged ? changedFiles.filter((file) => {
    if (file.includes('/controller/') && file.endsWith('Controller.java')) {
      return true;
    }
    return /\/api\/[A-Za-z]+Api\.java$/.test(file);
  }) : [];
}

function collectChangedContractFiles(repositoryRoot, base, head) {
  if (!base) {
    return [];
  }
  try {
    return git(repositoryRoot, ['diff', '--name-only', `${base}..${head}`, '--', ...CONTRACT_SOURCE_PATHS])
      .split('\n')
      .map((file) => file.trim())
      .filter(Boolean);
  } catch {
    return [];
  }
}

export function formatReleaseNotesBlock(model) {
  const lines = ['workflow-contract-fingerprint:'];
  if (model.unverifiedContractChanges && model.unverifiedContractChanges.length > 0) {
    lines.push('  # WARN 契约源码有改动但指纹常量没变，确认 Workflow API contract sentinel 已在该提交跑绿：');
    for (const file of model.unverifiedContractChanges) {
      lines.push(`  #   ${file}`);
    }
  }
  lines.push(`  api:    ${model.api ?? 'n/a'}`);
  lines.push(`  web:    ${model.web ?? 'n/a'}`);
  lines.push(`  changed: ${model.changed}`);
  lines.push(`  since:  ${model.since}`);
  lines.push('');
  if (model.additions.length === 0) {
    lines.push('workflow-compatible-additions: []');
  } else {
    lines.push('workflow-compatible-additions:');
    for (const addition of model.additions) {
      lines.push(`  - ${addition.className}: + ${addition.names.join(', ')}`);
    }
  }
  return lines.join('\n');
}

function resolvePreviousRelease(repositoryRoot, head) {
  // 发布 tag 打在 release 分支的提交上，随后以 squash 提交进 main，因此 tag 的提交
  // 不是 main 的祖先，`git describe` / `--merged` 都找不到它们。按创建时间倒序挑第一个
  // 与当前提交不同的批次 tag。
  let tags = [];
  try {
    tags = git(repositoryRoot, ['tag', '--list', 'v*maven-*-release', '--sort=-creatordate'])
      .split('\n')
      .map((tag) => tag.trim())
      .filter(Boolean);
  } catch {
    return '';
  }
  let headCommit = '';
  try {
    headCommit = git(repositoryRoot, ['rev-parse', `${head}^{commit}`]).trim();
  } catch {
    headCommit = '';
  }
  for (const tag of tags) {
    let tagCommit = '';
    try {
      tagCommit = git(repositoryRoot, ['rev-parse', `${tag}^{commit}`]).trim();
    } catch {
      tagCommit = '';
    }
    if (!tagCommit || tagCommit !== headCommit) {
      return tag;
    }
  }
  return '';
}

function readContractSource(repositoryRoot, ref) {
  const relative = CONTRACT_TEST_RELATIVE;
  try {
    return git(repositoryRoot, ['show', `${ref}:${relative}`]);
  } catch {
    return null;
  }
}

function readWorkingTreeContract(repositoryRoot) {
  const absolute = path.join(repositoryRoot, CONTRACT_TEST_RELATIVE);
  return fs.existsSync(absolute) ? fs.readFileSync(absolute, 'utf8') : null;
}

function collectCompatibleAdditions(repositoryRoot, base, head) {
  if (!base) {
    return [];
  }
  let diffText = '';
  try {
    diffText = git(repositoryRoot, [
      'diff',
      '--unified=0',
      '--no-color',
      `${base}..${head}`,
      '--',
      `${API_SOURCE_ROOT}/vo`,
      `${API_SOURCE_ROOT}/query`,
      `${API_SOURCE_ROOT}/request`,
      `${API_SOURCE_ROOT}/enums`
    ]);
  } catch {
    return [];
  }
  return extractCompatibleAdditions(diffText);
}

export function buildFingerprintModel({ headSource, baseSource, base, additions = [] }) {
  const headFingerprints = headSource ? parseFingerprints(headSource) : { api: null, web: null };
  const baseFingerprints = baseSource ? parseFingerprints(baseSource) : { api: null, web: null };
  const comparable = Boolean(baseFingerprints.api && baseFingerprints.web && base);
  return {
    api: headFingerprints.api,
    web: headFingerprints.web,
    changed: comparable ? describeChange(baseFingerprints, headFingerprints) : 'none',
    since: comparable ? `${baseFingerprints.api} / ${baseFingerprints.web}` : 'n/a',
    base,
    additions
  };
}

/**
 * 取出 `CHANGELOG.md` 第一个发布段落（`## ` 到下一个 `## ` 之间），校验它带着指纹块且数值与当前代码一致。
 */
export function firstReleaseSection(changelog) {
  const lines = changelog.split('\n');
  const start = lines.findIndex((line) => line.startsWith('## '));
  if (start < 0) {
    return '';
  }
  const rest = lines.slice(start + 1);
  const end = rest.findIndex((line) => line.startsWith('## '));
  return (end < 0 ? rest : rest.slice(0, end)).join('\n');
}

export function verifyReleaseNotesSection(section, model) {
  const problems = [];
  const blockMatch = /workflow-contract-fingerprint:\s*\n([\s\S]*?)(?:\n\s*\n|$)/.exec(section);
  if (!blockMatch) {
    problems.push('发布段落里没有 workflow-contract-fingerprint 块');
  } else {
    const block = blockMatch[1];
    const read = (key) => {
      const matched = new RegExp(`^[ \\t]*${key}:[ \\t]*(.*?)[ \\t]*$`, 'm').exec(block);
      return matched ? matched[1] : '';
    };
    const api = read('api');
    const web = read('web');
    const changed = read('changed');
    const since = read('since');
    if (api !== model.api) {
      problems.push(`api 指纹与当前代码不一致：发布说明 ${api || '(缺失)'}，当前 ${model.api}`);
    }
    if (web !== model.web) {
      problems.push(`web 指纹与当前代码不一致：发布说明 ${web || '(缺失)'}，当前 ${model.web}`);
    }
    if (changed !== model.changed) {
      problems.push(`changed 应为 ${model.changed}，发布说明写的是 ${changed || '(缺失)'}`);
    }
    if (since !== model.since) {
      problems.push(`since 应为 ${model.since}，发布说明写的是 ${since || '(缺失)'}`);
    }
  }
  if (!/workflow-compatible-additions:/i.test(section)) {
    problems.push('发布段落里没有 workflow-compatible-additions 块（没有新增时写 `[]`）');
  }
  if (model.unverifiedContractChanges.length > 0) {
    problems.push('契约源码有改动但指纹常量未更新，先让 Workflow API contract sentinel 跑绿');
  }
  return problems;
}

export function runCli(argv = process.argv.slice(2)) {
  const args = parseArgs(argv);
  const repositoryRoot = resolveRepositoryRoot(args.root);
  const headSource = readContractSource(repositoryRoot, args.head) ?? readWorkingTreeContract(repositoryRoot);
  if (!headSource) {
    process.stderr.write(`[FAIL] 找不到契约测试：${CONTRACT_TEST_RELATIVE}\n`);
    return 1;
  }
  const headFingerprints = parseFingerprints(headSource);
  if (!headFingerprints.api || !headFingerprints.web) {
    process.stderr.write('[FAIL] 契约测试里缺少 API_FINGERPRINT 或 HTTP_FINGERPRINT 常量\n');
    return 1;
  }
  const base = args.base || resolvePreviousRelease(repositoryRoot, args.head);
  const baseSource = base ? readContractSource(repositoryRoot, base) : null;
  const additions = args.changedOnly ? [] : collectCompatibleAdditions(repositoryRoot, base, args.head);
  const model = buildFingerprintModel({ headSource, baseSource, base, additions });
  const changedContractFiles = collectChangedContractFiles(repositoryRoot, base, args.head);
  model.unverifiedContractChanges = detectUnverifiedContractChanges(
    changedContractFiles,
    parseFingerprints(baseSource ?? ''),
    headFingerprints
  );
  if (model.unverifiedContractChanges.length > 0) {
    model.warnings = [
      '契约源码有改动但指纹常量未变：确认 Workflow API contract sentinel 已在该提交跑绿，否则发布说明里的指纹不可信。'
    ];
  } else {
    model.warnings = [];
  }

  if (args.verifyReleaseNotes) {
    const changelogPath = path.resolve(repositoryRoot, args.verifyReleaseNotes);
    if (!fs.existsSync(changelogPath)) {
      process.stderr.write(`[FAIL] 找不到发布说明文件：${args.verifyReleaseNotes}\n`);
      return 1;
    }
    const section = firstReleaseSection(fs.readFileSync(changelogPath, 'utf8'));
    const problems = verifyReleaseNotesSection(section, model);
    if (problems.length > 0) {
      for (const problem of problems) {
        process.stderr.write(`[FAIL] ${problem}\n`);
      }
      return 1;
    }
    process.stdout.write('Release notes contract block verified.\n');
    return 0;
  }

  if (args.json) {
    process.stdout.write(`${JSON.stringify(model, null, 2)}\n`);
  } else {
    process.stdout.write(`${formatReleaseNotesBlock(model)}\n`);
  }
  return 0;
}

if (process.argv[1] && path.resolve(process.argv[1]) === path.resolve(fileURLToPath(import.meta.url))) {
  process.exit(runCli());
}
