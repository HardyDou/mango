#!/usr/bin/env node
/**
 * 校验模块内依赖版本 pin（Issue #1404，配套 #1353 的「版本出口唯一」）。
 *
 * 背景：`#1353` 把依赖版本出口收敛到 `mango-bom` 的 `dependencyManagement`。模块 POM 里再写一次
 * `<version>` 会让 `dependencyManagement` 对该坐标失效——同名 jar 可能因此出现两个版本，
 * 且升级 `mango-bom` 时该模块不会跟随。本工具把这条契约变成机器校验。
 *
 * 判定语义（只针对「坐标同时被 `mango-bom` 管理」的依赖）：
 * - 模块声明的版本与 BOM 生效值不一致 → ERROR（真漂移，会改变运行时 classpath）；
 * - 模块声明的版本与 BOM 生效值完全一致 → WARNING（纯冗余，允许存量存在）；
 * - 坐标不在 BOM 里 → 忽略，不算违规（那类依赖没有 BOM 出口可收敛）。
 *
 * BOM 生效值来自 `mango-bom/pom.xml` 的 `<dependencyManagement>`，支持 `${xxx.version}` 属性替换；
 * `${project.version}`、`${revision}` 等解析不到具体值的版本视为「不可比对」，直接跳过，
 * 避免把自身模块版本误判为漂移。
 *
 * 退出码：默认 `--fail-on=drift`，只有 ERROR 才失败（WARNING 只打印）；
 * `--strict`（等价 `--fail-on=all`）把 WARNING 也算失败，用于希望冗余归零的场景。
 *
 * 白名单：`--allow-file` 指向 `{ "allow": [{ "groupId", "artifactId", "module", "reason" }] }`，
 * 命中白名单的条目不计入 error/warning，用于「故意测试旧版本」这类有意保留的 pin。
 * 默认 `mango-pmo/baselines/check-module-version-pins.json`，文件不存在即视为空白名单。
 *
 * 用法：
 *   node mango-pmo/tools/check-module-version-pins.mjs [--root mango] [--bom <path>]
 *     [--allow-file <path>] [--strict | --fail-on drift|all] [--json]
 */
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';
import { pathToFileURL } from 'node:url';

const IGNORED_DIRS = new Set(['target', 'node_modules', '.git', 'dist']);
const DEFAULT_ALLOW_FILE = 'mango-pmo/baselines/check-module-version-pins.json';
const RULE_DRIFT = 'VERSION_DRIFT';
const RULE_REDUNDANT = 'REDUNDANT_VERSION';

function parseArgs(argv) {
  const options = { root: 'mango', bom: null, allowFile: DEFAULT_ALLOW_FILE, strict: false, failOn: null, json: false };
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--strict') options.strict = true;
    else if (arg === '--json') options.json = true;
    else if (arg === '--root') options.root = argv[++index];
    else if (arg === '--bom') options.bom = argv[++index];
    else if (arg === '--allow-file') options.allowFile = argv[++index];
    else if (arg === '--fail-on') options.failOn = argv[++index];
    else if (arg === '--help' || arg === '-h') {
      console.log(
        '用法: node mango-pmo/tools/check-module-version-pins.mjs [--root mango] [--bom <path>]'
          + ' [--allow-file <path>] [--strict | --fail-on drift|all] [--json]',
      );
      process.exit(0);
    } else throw new Error(`未知参数: ${arg}`);
  }
  if (options.failOn !== null && options.failOn !== 'drift' && options.failOn !== 'all') {
    throw new Error(`--fail-on 只支持 drift 或 all，收到: ${options.failOn}`);
  }
  options.failOn = options.strict ? 'all' : (options.failOn ?? 'drift');
  options.bom = options.bom ?? join(options.root, 'mango-bom/pom.xml');
  return options;
}

export function collectPoms(dir, acc = []) {
  for (const entry of readdirSync(dir)) {
    if (IGNORED_DIRS.has(entry)) continue;
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) collectPoms(full, acc);
    else if (entry === 'pom.xml') acc.push(full);
  }
  return acc;
}

function firstTag(xml, tag, scope) {
  const source = scope ?? xml;
  const match = new RegExp(`<${tag}>([\\s\\S]*?)</${tag}>`).exec(source);
  return match ? match[1].trim() : null;
}

function eachTag(xml, tag, callback) {
  const pattern = new RegExp(`<${tag}>([\\s\\S]*?)</${tag}>`, 'g');
  let match = pattern.exec(xml);
  while (match) {
    callback(match[1]);
    match = pattern.exec(xml);
  }
}

/** 解析 `<properties>`；键名按 Maven 习惯原样保留。 */
export function parseProperties(xml) {
  const properties = {};
  const block = /<properties>([\s\S]*?)<\/properties>/.exec(xml);
  if (!block) return properties;
  const pattern = /<([A-Za-z0-9_.-]+)>([^<]*)<\/\1>/g;
  let match = pattern.exec(block[1]);
  while (match) {
    properties[match[1]] = match[2].trim();
    match = pattern.exec(block[1]);
  }
  return properties;
}

/**
 * 展开 `${xxx.version}` 形式的属性引用；展开后仍含 `${...}` 的值（`project.version`、
 * `revision`、未定义属性）返回 null，表示不可比对。
 */
export function resolveVersion(rawVersion, properties = {}) {
  if (!rawVersion) return null;
  let value = rawVersion.trim();
  for (let depth = 0; depth < 8 && value.includes('${'); depth += 1) {
    const expanded = value.replace(/\$\{([^}]+)\}/g, (placeholder, key) => properties[key] ?? placeholder);
    if (expanded === value) break;
    value = expanded;
  }
  return value.includes('${') ? null : value;
}

/** 读取 `mango-bom` 的生效版本：`groupId:artifactId` → version（不可比对时为 null）。 */
export function parseBom(bomPath) {
  if (!existsSync(bomPath)) throw new Error(`未找到 BOM：${bomPath}`);
  const xml = readFileSync(bomPath, 'utf8');
  const properties = parseProperties(xml);
  const bomGroupId = firstTag(xml.replace(/<parent>[\s\S]*?<\/parent>/, ''), 'groupId') ?? firstTag(xml, 'groupId');
  const managed = new Map();
  const management = /<dependencyManagement>([\s\S]*?)<\/dependencyManagement>/.exec(xml);
  if (management) {
    eachTag(management[1], 'dependency', (block) => {
      const groupId = firstTag(block, 'groupId') ?? bomGroupId;
      const artifactId = firstTag(block, 'artifactId');
      if (!groupId || !artifactId) return;
      managed.set(`${groupId}:${artifactId}`, resolveVersion(firstTag(block, 'version'), properties));
    });
  }
  return { bomPath, properties, managed };
}

/**
 * 收集模块内带显式 `<version>` 的 `<dependency>`。
 * `dependencyManagement` / `pluginManagement` / `build` 段不参与（那些不是模块引入依赖时的 pin，
 * 且 BOM 自身就定义在 `dependencyManagement` 里）。
 */
export function collectDependencyPins(rootDir) {
  const pins = [];
  for (const file of collectPoms(rootDir)) {
    const xml = readFileSync(file, 'utf8');
    const module = relative(rootDir, file).replaceAll('\\', '/');
    const scoped = xml
      .replace(/<dependencyManagement>[\s\S]*?<\/dependencyManagement>/g, '')
      .replace(/<pluginManagement>[\s\S]*?<\/pluginManagement>/g, '')
      .replace(/<build>[\s\S]*?<\/build>/g, '');
    eachTag(scoped, 'dependency', (block) => {
      const groupId = firstTag(block, 'groupId');
      const artifactId = firstTag(block, 'artifactId');
      const version = firstTag(block, 'version');
      if (!groupId || !artifactId || !version) return;
      pins.push({ groupId, artifactId, version, module });
    });
  }
  return pins;
}

/** 读取白名单文件；文件缺失或缺少 `allow` 数组时返回空名单。 */
export function loadAllowList(filePath) {
  if (!filePath || !existsSync(filePath)) return [];
  const parsed = JSON.parse(readFileSync(filePath, 'utf8'));
  return Array.isArray(parsed?.allow) ? parsed.allow : [];
}

function allowEntryMatches(entry, pin) {
  if (entry?.groupId !== pin.groupId || entry?.artifactId !== pin.artifactId) return false;
  if (!entry.module) return true;
  return String(entry.module).replaceAll('\\', '/').replace(/^\.\//, '') === pin.module;
}

/** 判定：BOM 已管理的坐标 + 显式版本 → ERROR（漂移）或 WARNING（纯冗余）。 */
export function evaluate(pins, bom, allowList = []) {
  const errors = [];
  const warnings = [];
  for (const pin of pins) {
    const key = `${pin.groupId}:${pin.artifactId}`;
    if (!bom.managed.has(key)) continue;
    const expected = bom.managed.get(key);
    if (!expected || !resolveVersion(pin.version)) continue;
    if (allowList.some((entry) => allowEntryMatches(entry, pin))) continue;
    const item = {
      groupId: pin.groupId,
      artifactId: pin.artifactId,
      module: pin.module,
      version: pin.version,
      expected,
    };
    if (pin.version === expected) {
      warnings.push({ rule: RULE_REDUNDANT, ...item, message: `版本与 mango-bom 一致（${expected}），属冗余声明` });
    } else {
      errors.push({ rule: RULE_DRIFT, ...item, message: `模块声明 ${pin.version}，mango-bom 管理 ${expected}` });
    }
  }
  return { total: pins.length, errors, warnings };
}

function main() {
  const options = parseArgs(process.argv.slice(2));
  const bom = parseBom(options.bom);
  const pins = collectDependencyPins(options.root);
  const allowList = loadAllowList(options.allowFile);
  const { total, errors, warnings } = evaluate(pins, bom, allowList);

  if (options.json) {
    console.log(JSON.stringify({ total, errors, warnings }, null, 2));
  } else {
    console.log(`扫描 ${total} 处显式版本依赖，mango-bom 管理 ${bom.managed.size} 个坐标（白名单 ${allowList.length} 条）`);
    console.log(`版本漂移（ERROR）：${errors.length} 处；纯冗余声明（WARNING）：${warnings.length} 处`);
    for (const item of errors) console.log(`- [ERROR] ${item.groupId}:${item.artifactId} ${item.message}（${item.module}）`);
    for (const item of warnings) console.log(`- [WARNING] ${item.groupId}:${item.artifactId} ${item.message}（${item.module}）`);
  }

  const failing = options.failOn === 'all' ? errors.length + warnings.length : errors.length;
  if (failing > 0) {
    console.error(
      `依赖版本 pin 校验失败：${errors.length} 处漂移、${warnings.length} 处冗余（--fail-on=${options.failOn}）`,
    );
    process.exit(1);
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) main();
