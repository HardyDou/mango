#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

/**
 * Surefire 版本基线校验（Issue #1300）。
 *
 * 背景：`mango-parent` 只被 12 个模块直接继承，其余模块此前落在 Maven 默认绑定的
 * Surefire 2.17 上。Surefire 2.x 无法发现 JUnit 5 用例，于是 `mvn test` 输出
 * `Tests run: 0` 并把整个测试套件静默跳过。Issue #1307 之后根 POM 统一声明
 * Surefire 3.2.5，本工具把这个事实固定成回归检查：
 *
 * 1. 只检查真正参与构建的模块：从 `mango/pom.xml` 逐级解析 `<modules>` 得到 reactor 图，
 *    `src/it`、`src/test/resources` 下的 fixture POM 不在范围内。
 * 2. 每个含测试源目录（或含测试源文件的 `packaging=pom` 聚合模块）的模块，最终解析到的
 *    Surefire 版本必须 >= 3.0.0；解析规则按 Maven 语义走：本模块 `build/plugins` →
 *    本模块 `build/pluginManagement` → 父 POM（`relativePath` 优先，其次按 GAV 回查）。
 * 3. 版本声明只允许出现在根 POM 与 `mango-parent`；其它模块本地声明视为漂移，
 *    避免"某个模块自己钉一个版本"重新把测试变成空跑。
 *
 * 输出统一为 `Surefire gate passed: ...` / `[FAIL] ...`，可用 `--json` 取机器可读结果。
 */

const SUREFIRE_MINIMUM = [3, 0, 0];
const ROOT_POM_RELATIVE = 'mango/pom.xml';
const PARENT_POM_RELATIVE = 'mango/mango-parent/pom.xml';
const DECLARATION_ALLOWED_POM_RELATIVES = new Set([ROOT_POM_RELATIVE, PARENT_POM_RELATIVE]);

function parseArgs(argv) {
  const args = { json: false, root: '' };
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--json') {
      args.json = true;
    } else if (arg === '--root') {
      args.root = argv[index + 1] || '';
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

function readText(filePath) {
  return fs.readFileSync(filePath, 'utf8');
}

function stripComments(xml) {
  return xml.replace(/<!--[\s\S]*?-->/g, '');
}

function firstMatch(xml, pattern) {
  const matched = pattern.exec(xml);
  return matched ? matched[1].trim() : '';
}

function parseProperties(xml) {
  const properties = {};
  const block = firstMatch(xml, /<properties>([\s\S]*?)<\/properties>/);
  if (!block) {
    return properties;
  }
  const pattern = /<([A-Za-z0-9_.\-]+)>([\s\S]*?)<\/\1>/g;
  let matched = pattern.exec(block);
  while (matched) {
    properties[matched[1]] = matched[2].trim();
    matched = pattern.exec(block);
  }
  return properties;
}

function parseParent(xml) {
  const block = firstMatch(xml, /<parent>([\s\S]*?)<\/parent>/);
  if (!block) {
    return null;
  }
  return {
    groupId: firstMatch(block, /<groupId>([\s\S]*?)<\/groupId>/),
    artifactId: firstMatch(block, /<artifactId>([\s\S]*?)<\/artifactId>/),
    version: firstMatch(block, /<version>([\s\S]*?)<\/version>/),
    relativePath: block.includes('<relativePath>')
      ? firstMatch(block, /<relativePath>([\s\S]*?)<\/relativePath>/) : null
  };
}

function parseModules(xml) {
  const block = firstMatch(xml, /<modules>([\s\S]*?)<\/modules>/);
  if (!block) {
    return [];
  }
  const modules = [];
  const pattern = /<module>([\s\S]*?)<\/module>/g;
  let matched = pattern.exec(block);
  while (matched) {
    modules.push(matched[1].trim());
    matched = pattern.exec(block);
  }
  return modules;
}

function parseSurefireDeclaration(xml) {
  const declared = { main: '', management: '' };
  const blocks = [
    { key: 'main', pattern: /<build>([\s\S]*?)<\/build>/ },
    { key: 'management', pattern: /<pluginManagement>([\s\S]*?)<\/pluginManagement>/ }
  ];
  for (const block of blocks) {
    const body = firstMatch(xml, block.pattern);
    if (!body) {
      continue;
    }
    const pluginPattern = /<plugin>([\s\S]*?)<\/plugin>/g;
    let matched = pluginPattern.exec(body);
    while (matched) {
      const plugin = matched[1];
      if (firstMatch(plugin, /<artifactId>([\s\S]*?)<\/artifactId>/) === 'maven-surefire-plugin') {
        const version = firstMatch(plugin, /<version>([\s\S]*?)<\/version>/);
        if (version) {
          declared[block.key] = version;
        }
      }
      matched = pluginPattern.exec(body);
    }
  }
  return declared;
}

/**
 * 解析 POM 关键字段。解析失败抛错让调用方按"无法判定即失败"处理。
 */
export function parsePom(filePath) {
  const xml = stripComments(readText(filePath));
  const projectBlock = firstMatch(xml, /<project[^>]*>([\s\S]*)<\/project>/) || xml;
  const withoutParent = projectBlock.replace(/<parent>[\s\S]*?<\/parent>/, '');
  const parent = parseParent(projectBlock);
  return {
    filePath,
    groupId: firstMatch(withoutParent, /<groupId>([\s\S]*?)<\/groupId>/),
    artifactId: firstMatch(withoutParent, /<artifactId>([\s\S]*?)<\/artifactId>/),
    version: firstMatch(withoutParent, /<version>([\s\S]*?)<\/version>/),
    packaging: firstMatch(withoutParent, /<packaging>([\s\S]*?)<\/packaging>/) || 'jar',
    parent,
    modules: parseModules(withoutParent),
    properties: parseProperties(projectBlock),
    surefire: parseSurefireDeclaration(projectBlock)
  };
}

function interpolate(value, pom, pomsByPath, seen = new Set()) {
  if (!value) {
    return value;
  }
  const pattern = /\$\{([A-Za-z0-9_.\-]+)\}/g;
  let resolved = value;
  let matched = pattern.exec(value);
  while (matched) {
    const key = matched[1];
    let replacement = pom.properties[key];
    if (replacement === undefined && pom.parent) {
      const parentPath = resolveParentPath(pom, pomsByPath);
      if (parentPath && !seen.has(parentPath)) {
        seen.add(parentPath);
        replacement = interpolateAll(
          pomsByPath.get(parentPath).properties[key], pomsByPath.get(parentPath), pomsByPath, seen);
      }
    }
    if (replacement !== undefined) {
      resolved = resolved.replace(matched[0], replacement);
    }
    matched = pattern.exec(value);
  }
  return resolved;
}

function interpolateAll(value, pom, pomsByPath, seen) {
  if (!value) {
    return value;
  }
  return interpolate(value, pom, pomsByPath, seen);
}

function resolveParentPath(pom, pomsByPath) {
  if (!pom.parent) {
    return null;
  }
  const relativePath = pom.parent.relativePath;
  // 未显式声明 relativePath 时，Maven 默认按 ../pom.xml 查找父 POM。
  if (relativePath === null) {
    const defaultCandidate = path.join(path.dirname(path.dirname(pom.filePath)), 'pom.xml');
    if (fs.existsSync(defaultCandidate) && !pomsByPath.has(defaultCandidate)) {
      const onDisk = parsePom(defaultCandidate);
      onDisk.detachedParent = true;
      pomsByPath.set(defaultCandidate, onDisk);
    }
  }
  if (relativePath !== null) {
    const candidate = path.resolve(path.dirname(pom.filePath), relativePath || '..');
    const candidateFile = candidate.endsWith('.xml') ? candidate : path.join(candidate, 'pom.xml');
    if (pomsByPath.has(candidateFile)) {
      return candidateFile;
    }
    // 父 POM 不一定在 reactor 里（例如 mango-parent 从根聚合的 <modules> 退出后），
    // Maven 本身仍按 relativePath 从文件系统解析父链，这里保持一致。
    if (fs.existsSync(candidateFile)) {
      if (pomsByPath.has(candidateFile)) {
        return candidateFile;
      }
      const onDisk = parsePom(candidateFile);
      // 标记为「仅父链解析用」，不计入 reactor 模块统计。
      onDisk.detachedParent = true;
      pomsByPath.set(candidateFile, onDisk);
      return candidateFile;
    }
    if (!relativePath) {
      return null;
    }
  }
  for (const [candidatePath, candidate] of pomsByPath) {
    if (candidate.groupId === pom.parent.groupId && candidate.artifactId === pom.parent.artifactId) {
      return candidatePath;
    }
  }
  return null;
}

/**
 * 按 Maven 语义解析某个 POM 的有效 Surefire 版本；返回 null 表示整条父链都没有声明。
 */
export function resolveEffectiveSurefireVersion(pom, pomsByPath) {
  const visited = new Set();
  let current = pom;
  while (current && !visited.has(current.filePath)) {
    visited.add(current.filePath);
    for (const declared of [current.surefire.main, current.surefire.management]) {
      if (declared) {
        return interpolateAll(declared, current, pomsByPath, new Set(visited));
      }
    }
    const parentPath = resolveParentPath(current, pomsByPath);
    current = parentPath ? pomsByPath.get(parentPath) : null;
  }
  return null;
}

function compareVersions(left, right) {
  const parse = (value) => String(value).split('.').map((part) => Number.parseInt(part, 10) || 0);
  const leftParts = parse(left);
  const rightParts = parse(right);
  const length = Math.max(leftParts.length, rightParts.length);
  for (let index = 0; index < length; index += 1) {
    const diff = (leftParts[index] || 0) - (rightParts[index] || 0);
    if (diff !== 0) {
      return diff;
    }
  }
  return 0;
}

function hasTestSources(moduleDir) {
  const testDir = path.join(moduleDir, 'src', 'test', 'java');
  if (!fs.existsSync(testDir)) {
    return false;
  }
  const stack = [testDir];
  while (stack.length > 0) {
    const current = stack.pop();
    for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
      if (entry.isDirectory()) {
        stack.push(path.join(current, entry.name));
      } else if (entry.isFile() && entry.name.endsWith('.java')) {
        return true;
      }
    }
  }
  return false;
}

/**
 * 收集 reactor 图：从根 POM 沿 `<modules>` 递归，返回 filePath → POM 映射。
 */
export function collectReactorPoms(rootPomPath) {
  const poms = new Map();
  const queue = [path.resolve(rootPomPath)];
  while (queue.length > 0) {
    const currentPath = queue.shift();
    if (poms.has(currentPath)) {
      continue;
    }
    const pom = parsePom(currentPath);
    poms.set(currentPath, pom);
    for (const module of pom.modules) {
      const modulePom = path.resolve(path.dirname(currentPath), module, 'pom.xml');
      if (fs.existsSync(modulePom)) {
        queue.push(modulePom);
      }
    }
  }
  return poms;
}

/**
 * 校验入口：返回 { failures, summary }。
 */
export function checkSurefireVersions(repositoryRoot) {
  const rootPomPath = path.join(repositoryRoot, ROOT_POM_RELATIVE);
  if (!fs.existsSync(rootPomPath)) {
    return {
      failures: [`${ROOT_POM_RELATIVE} 不存在，无法解析 reactor`],
      summary: { modules: 0, versions: {}, exempt: 0 }
    };
  }
  const poms = collectReactorPoms(rootPomPath);
  const versions = {};
  const failures = [];
  let exempt = 0;
  for (const pom of poms.values()) {
    if (pom.detachedParent) {
      continue;
    }
    const relativePath = path.relative(repositoryRoot, pom.filePath).split(path.sep).join('/');
    const moduleDir = path.dirname(pom.filePath);
    const declarationAllowed = DECLARATION_ALLOWED_POM_RELATIVES.has(relativePath);
    const localDeclarations = [pom.surefire.main, pom.surefire.management].filter(Boolean);
    if (!declarationAllowed && localDeclarations.length > 0) {
      failures.push(`${relativePath} 本地声明了 Surefire 版本 ${localDeclarations.join('/')}；`
        + '版本只能声明在 mango/pom.xml 或 mango/mango-parent/pom.xml，避免单模块漂移');
    }
    const effective = resolveEffectiveSurefireVersion(pom, poms);
    if (!effective) {
      if (pom.packaging === 'pom' && !hasTestSources(moduleDir)) {
        exempt += 1;
        continue;
      }
      failures.push(`${relativePath} 未解析到 Surefire 版本；2.x 无法发现 JUnit 5 用例，`
        + '请继承 mango/pom.xml 或 mango/mango-parent/pom.xml 的 pluginManagement');
      continue;
    }
    versions[effective] = (versions[effective] || 0) + 1;
    if (compareVersions(effective, SUREFIRE_MINIMUM.join('.')) < 0) {
      failures.push(`${relativePath} 解析到 Surefire ${effective}，低于 3.0.0；`
        + '2.x 无法发现 JUnit 5 用例，测试会静默空跑');
    }
  }
  const summary = {
    modules: poms.size,
    versions,
    minimum: SUREFIRE_MINIMUM.join('.'),
    exempt
  };
  return { failures, summary };
}

export function formatSurefireReport(result) {
  if (result.failures.length > 0) {
    return [
      ...result.failures.map((failure) => `[FAIL] ${failure}`),
      `[FAIL] Surefire gate failed: ${result.failures.length} issue(s)`
    ].join('\n');
  }
  const versions = Object.entries(result.summary.versions)
    .map(([version, count]) => `${version}×${count}`)
    .join(', ');
  return `Surefire gate passed: modules=${result.summary.modules} versions=${versions}`
    + ` minimum=${result.summary.minimum} exempt=${result.summary.exempt}`;
}

export function runCli(argv = process.argv.slice(2)) {
  const args = parseArgs(argv);
  const repositoryRoot = resolveRepositoryRoot(args.root);
  const result = checkSurefireVersions(repositoryRoot);
  if (args.json) {
    process.stdout.write(`${JSON.stringify(result, null, 2)}\n`);
  } else {
    process.stdout.write(`${formatSurefireReport(result)}\n`);
  }
  return result.failures.length === 0 ? 0 : 1;
}

if (process.argv[1] && path.resolve(process.argv[1]) === path.resolve(fileURLToPath(import.meta.url))) {
  process.exit(runCli());
}
