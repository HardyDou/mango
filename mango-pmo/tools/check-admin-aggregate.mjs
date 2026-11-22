#!/usr/bin/env node
/**
 * 校验官方平台能力模块的装配点一致性（Issue #346）。
 *
 * 背景：官方平台能力模块（`mango/mango-platform/<domain>/`）有四个彼此独立的装配点——
 * ① starter 制品的 admin 聚合、② 单体应用（monolith）的 Flyway 模块开关、③ starter/core 里的
 * Resource 声明（`META-INF/mango/resources/*.json`）、④ 单体应用的 Resource 扫描位置。
 * 前三类在仓库里各自独立维护，漏配任何一项时应用仍然能启动：模块 jar 在 classpath 上但没被
 * `mango-admin-starter` 引入、DDL 没有被 Flyway 执行、Resource 声明没有被登记扫描。本工具把这类
 * 「静默漏配」变成机器校验。
 * （第四个装配点是前端 admin registry 与菜单 component key 的交叉验证，属于前端契约门禁的范围，
 * 不在本工具内，留待后续 issue。）
 *
 * 判定语义（只在「官方平台能力模块」范围内生效：`mango-platform/<domain>/<unit>/pom.xml`，
 * 子单元按自身 `artifactId` 识别；`src/test` 下的资源与迁移不计入）：
 * - A. starter 聚合：单元 `artifactId` 以 `-starter` 结尾时，`mango-admin-starter/pom.xml` 的
 *   `<dependencies>` 中必须出现该 `artifactId`；缺失 → `ERROR: STARTER_NOT_AGGREGATED`。
 * - B. Flyway 模块开关：单元带 `src/main/resources/db/migration/<键>/*.sql` 时，
 *   `mango-app/monolith/mango-monolith-app/src/main/resources/application.yml` 的
 *   `mango.persistence.flyway.modules.<键>` 必须存在且 `enabled: true`，或 `enabled: false` 时带非空
 *   `skip-reason`。键缺失 → `ERROR: FLYWAY_MODULE_MISSING`；`enabled` 不为 `true` 且没有
 *   `skip-reason`（含 `enabled` 整个缺失）→ `ERROR: FLYWAY_SKIP_REASON_MISSING`。
 *   反向孤儿（yml 有键、平台模块没有产出该键的迁移，例如 `kv`/`ai` 来自 infra/extension 模块）
 *   → `WARNING: FLYWAY_MODULE_ORPHAN`，允许存在。
 * - C. Resource 声明可达：单元带 `src/main/resources/META-INF/mango/resources/*.json` 时，
 *   ① 该单元的 starter 必须被 admin 聚合（复用 A 的结论，A 已报过同一 `artifactId` 时不重复报），
 *   ② 单体应用生效的 `mango.resource.registry.locations` 必须包含
 *   `classpath*:META-INF/mango/resources/*.json`（字符串包含即可）。不满足 →
 *   `ERROR: RESOURCE_NOT_REACHABLE`。
 *   yml 未声明 `locations` 时按 `ResourceRegistryProperties` 的框架默认值
 *   （`classpath*:META-INF/mango/resources/*.json|*.yml|*.yaml`）判定为可达，并给出一条
 *   `WARNING: RESOURCE_LOCATIONS_IMPLICIT`，提示显式声明；声明了但缺 `*.json` 才判失败。
 *
 * 覆盖边界：
 * - 不校验前端 admin registry 与菜单 `component` key 的交叉验证（前端契约门禁的范围，后续 issue）；
 * - 不解析 `spring.profiles` 分支或多份 profile yml，只看单体应用主文件 `application.yml`；
 * - 不判断 starter 内部是否真的提供了自动配置（那是架构规则与启动测试的范围）；
 * - 不判断 POM 依赖的生效结果（父链与版本出口由 `check-pom-inheritance.mjs` /
 *   `check-module-version-pins.mjs` 兜住），A 只比对字面 `artifactId`。
 *
 * 退出码：默认 `--fail-on=error`，只有 ERROR 失败；`--strict`（等价 `--fail-on=all`）把 WARNING
 * 也算失败。
 *
 * 白名单：默认 `mango-pmo/baselines/check-admin-aggregate.json`，
 * `{ "allow": [{ "rule": "<规则名>", "subject": "<artifactId 或迁移键>" , "reason": "<理由>" }] }`，
 * 命中白名单的条目不进入 error/warning；文件不存在即视为空白名单。
 *
 * 用法：
 *   node mango-pmo/tools/check-admin-aggregate.mjs [--root mango] [--allow-file <path>]
 *     [--strict | --fail-on error|all] [--json]
 */
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { dirname, join, relative } from 'node:path';
import { pathToFileURL } from 'node:url';

const IGNORED_DIRS = new Set(['target', 'node_modules', '.git', 'dist']);
const DEFAULT_ALLOW_FILE = 'mango-pmo/baselines/check-admin-aggregate.json';
const PLATFORM_DIR = 'mango-platform';
const ADMIN_AGGREGATE_POM = 'mango-admin-starter/pom.xml';
const MONOLITH_APP_YML = 'mango-app/monolith/mango-monolith-app/src/main/resources/application.yml';
const RESOURCE_LOCATION_PATTERN = 'classpath*:META-INF/mango/resources/*.json';
// 与 mango-platform/mango-resource/mango-resource-support 的 ResourceRegistryProperties 默认值一致
const DEFAULT_RESOURCE_LOCATIONS = [
  'classpath*:META-INF/mango/resources/*.json',
  'classpath*:META-INF/mango/resources/*.yml',
  'classpath*:META-INF/mango/resources/*.yaml',
];
const RULE_STARTER_NOT_AGGREGATED = 'STARTER_NOT_AGGREGATED';
const RULE_FLYWAY_MODULE_MISSING = 'FLYWAY_MODULE_MISSING';
const RULE_FLYWAY_SKIP_REASON_MISSING = 'FLYWAY_SKIP_REASON_MISSING';
const RULE_FLYWAY_MODULE_ORPHAN = 'FLYWAY_MODULE_ORPHAN';
const RULE_RESOURCE_NOT_REACHABLE = 'RESOURCE_NOT_REACHABLE';
const RULE_RESOURCE_LOCATIONS_IMPLICIT = 'RESOURCE_LOCATIONS_IMPLICIT';

function parseArgs(argv) {
  const options = { root: 'mango', allowFile: DEFAULT_ALLOW_FILE, strict: false, failOn: null, json: false };
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--strict') options.strict = true;
    else if (arg === '--json') options.json = true;
    else if (arg === '--root') options.root = argv[++index];
    else if (arg === '--allow-file') options.allowFile = argv[++index];
    else if (arg === '--fail-on') options.failOn = argv[++index];
    else if (arg === '--help' || arg === '-h') {
      console.log(
        '用法: node mango-pmo/tools/check-admin-aggregate.mjs [--root mango] [--allow-file <path>]'
          + ' [--strict | --fail-on error|all] [--json]',
      );
      process.exit(0);
    } else throw new Error(`未知参数: ${arg}`);
  }
  if (options.failOn !== null && options.failOn !== 'error' && options.failOn !== 'all') {
    throw new Error(`--fail-on 只支持 error 或 all，收到: ${options.failOn}`);
  }
  options.failOn = options.strict ? 'all' : (options.failOn ?? 'error');
  return options;
}

function sortedDirNames(dir) {
  return readdirSync(dir)
    .filter((entry) => statSync(join(dir, entry)).isDirectory())
    .sort((left, right) => left.localeCompare(right));
}

function toPosix(path) {
  return path.replaceAll('\\', '/');
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

/** 读取 POM 自身的 `artifactId`（先剥离 `<parent>`，否则会取到父 POM 的坐标）。 */
export function parseOwnArtifactId(xml) {
  return firstTag(xml.replace(/<parent>[\s\S]*?<\/parent>/, ''), 'artifactId');
}

function hasSqlFile(dir) {
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) {
      if (hasSqlFile(full)) return true;
    } else if (entry.toLowerCase().endsWith('.sql')) {
      return true;
    }
  }
  return false;
}

/** 迁移模块键 = `src/main/resources/db/migration/<键>/` 且目录内（递归）至少有一个 `.sql`。 */
export function listMigrationKeys(resourcesRoot) {
  const migrationRoot = join(resourcesRoot, 'db/migration');
  if (!existsSync(migrationRoot)) return [];
  return sortedDirNames(migrationRoot).filter((key) => hasSqlFile(join(migrationRoot, key)));
}

/** Resource 声明 = `src/main/resources/META-INF/mango/resources/*.json`（测试资源不计入）。 */
export function listResourceDeclarations(resourcesRoot) {
  const declarationRoot = join(resourcesRoot, 'META-INF/mango/resources');
  if (!existsSync(declarationRoot)) return [];
  return readdirSync(declarationRoot)
    .filter((entry) => entry.endsWith('.json') && statSync(join(declarationRoot, entry)).isFile())
    .sort((left, right) => left.localeCompare(right));
}

/**
 * 发现官方平台能力模块：`<platformRoot>/<domain>/<unit>/pom.xml`。
 * 每个单元记录自身 `artifactId`、是否 starter、迁移键与 Resource 声明。
 */
export function discoverModules(platformRoot) {
  if (!existsSync(platformRoot)) return [];
  const displayRoot = dirname(platformRoot);
  const modules = [];
  for (const domain of sortedDirNames(platformRoot)) {
    if (IGNORED_DIRS.has(domain)) continue;
    const domainDir = join(platformRoot, domain);
    const units = [];
    for (const unit of sortedDirNames(domainDir)) {
      if (IGNORED_DIRS.has(unit)) continue;
      const unitDir = join(domainDir, unit);
      const pomPath = join(unitDir, 'pom.xml');
      if (!existsSync(pomPath)) continue;
      const artifactId = parseOwnArtifactId(readFileSync(pomPath, 'utf8'));
      if (!artifactId) continue;
      const resourcesRoot = join(unitDir, 'src/main/resources');
      units.push({
        domain,
        artifactId,
        path: toPosix(relative(displayRoot, unitDir)),
        isStarter: artifactId.endsWith('-starter'),
        migrations: listMigrationKeys(resourcesRoot),
        resources: listResourceDeclarations(resourcesRoot),
      });
    }
    if (units.length > 0) modules.push({ domain, path: toPosix(relative(displayRoot, domainDir)), units });
  }
  return modules;
}

/** 读取 `mango-admin-starter/pom.xml` 中按声明顺序聚合的 `<artifactId>`。 */
export function parseAggregatedStarters(pomPath) {
  if (!existsSync(pomPath)) throw new Error(`未找到 admin 聚合 POM：${pomPath}`);
  const xml = readFileSync(pomPath, 'utf8')
    .replace(/<parent>[\s\S]*?<\/parent>/g, '')
    .replace(/<dependencyManagement>[\s\S]*?<\/dependencyManagement>/g, '')
    .replace(/<pluginManagement>[\s\S]*?<\/pluginManagement>/g, '')
    .replace(/<build>[\s\S]*?<\/build>/g, '')
    .replace(/<profiles>[\s\S]*?<\/profiles>/g, '');
  const artifactIds = [];
  eachTag(xml, 'dependency', (block) => {
    const artifactId = firstTag(block, 'artifactId');
    if (artifactId) artifactIds.push(artifactId);
  });
  return artifactIds;
}

function stripYamlComment(line) {
  const index = line.indexOf('#');
  return index < 0 ? line : line.slice(0, index);
}

function unquote(value) {
  const trimmed = value.trim();
  if (trimmed.length >= 2 && ((trimmed.startsWith('"') && trimmed.endsWith('"'))
    || (trimmed.startsWith("'") && trimmed.endsWith("'")))) {
    return trimmed.slice(1, -1);
  }
  return trimmed;
}

/**
 * 足够本门禁使用的 YAML 子集解析：返回 `path → 标量` 与 `path → 列表` 两张表。
 * 只依赖缩进，不处理锚点、多文档与流式映射；块标量（`|`/`>`）记为「有值」。
 */
export function parseYamlPaths(text) {
  const scalars = new Map();
  const lists = new Map();
  const stack = [];
  for (const rawLine of text.split(/\r?\n/)) {
    const line = stripYamlComment(rawLine);
    if (line.trim() === '') continue;
    const indent = line.length - line.trimStart().length;
    const trimmed = line.trim();
    if (trimmed.startsWith('-')) {
      const value = unquote(trimmed.replace(/^-\s*/, ''));
      const path = stack.map((item) => item.key).join('.');
      if (!lists.has(path)) lists.set(path, []);
      lists.get(path).push(value);
      continue;
    }
    const separator = trimmed.indexOf(':');
    if (separator <= 0) continue;
    const key = trimmed.slice(0, separator).trim();
    const value = trimmed.slice(separator + 1).trim();
    while (stack.length > 0 && stack[stack.length - 1].indent >= indent) stack.pop();
    const path = [...stack.map((item) => item.key), key].join('.');
    if (value === '') {
      stack.push({ indent, key });
      continue;
    }
    if (value.startsWith('[') && value.endsWith(']')) {
      lists.set(
        path,
        value.slice(1, -1).split(',').map((item) => unquote(item)).filter((item) => item !== ''),
      );
      continue;
    }
    scalars.set(path, value.startsWith('|') || value.startsWith('>') ? '' : unquote(value));
  }
  return { scalars, lists };
}

/** 读取单体应用 `application.yml` 的 `mango.persistence.flyway.modules.<键>` 开关。 */
export function parseFlywayModules(ymlPath) {
  if (!existsSync(ymlPath)) throw new Error(`未找到单体应用配置：${ymlPath}`);
  const { scalars } = parseYamlPaths(readFileSync(ymlPath, 'utf8'));
  const prefix = 'mango.persistence.flyway.modules.';
  const modules = new Map();
  for (const [path, value] of scalars) {
    if (!path.startsWith(prefix)) continue;
    const rest = path.slice(prefix.length);
    const dot = rest.indexOf('.');
    if (dot <= 0) continue;
    const key = rest.slice(0, dot);
    const field = rest.slice(dot + 1);
    if (!modules.has(key)) modules.set(key, { enabled: null, skipReason: null });
    const entry = modules.get(key);
    if (field === 'enabled') entry.enabled = value === 'true';
    else if (field === 'skip-reason') entry.skipReason = value === '' ? null : value;
  }
  return modules;
}

/** 读取单体应用的 `mango.resource.registry.locations`；未声明时 `declared` 为 false。 */
export function parseResourceLocations(ymlPath) {
  if (!existsSync(ymlPath)) throw new Error(`未找到单体应用配置：${ymlPath}`);
  const { scalars, lists } = parseYamlPaths(readFileSync(ymlPath, 'utf8'));
  const path = 'mango.resource.registry.locations';
  const locations = lists.get(path) ?? (scalars.has(path) ? [scalars.get(path)] : null);
  if (locations === null) return { declared: false, locations: [] };
  return { declared: true, locations };
}

/** 读取白名单文件；文件缺失或缺少 `allow` 数组时返回空名单。 */
export function loadAllowList(filePath) {
  if (!filePath || !existsSync(filePath)) return [];
  const parsed = JSON.parse(readFileSync(filePath, 'utf8'));
  return Array.isArray(parsed?.allow) ? parsed.allow : [];
}

function insertHint(artifactId, aggregated) {
  const sorted = [...aggregated].sort((left, right) => left.localeCompare(right));
  const next = sorted.find((item) => item.localeCompare(artifactId) > 0) ?? null;
  const previous = [...sorted].reverse().find((item) => item.localeCompare(artifactId) < 0) ?? null;
  const anchor = next ? `插入到 ${next} 之前` : (previous ? `插入到 ${previous} 之后` : '插入到 <dependencies> 任意位置');
  const tail = next && previous ? `（按 artifactId 字母序接在 ${previous} 之后、${next} 之前）` : '（按 artifactId 字母序）';
  return `${ADMIN_AGGREGATE_POM} 的 <dependencies> 建议${anchor}${tail}`;
}

/**
 * 判定三条装配点一致性。
 * `context`：`{ aggregated: string[], flywayModules: Map, resourceLocations, allowList }`。
 */
export function evaluate(modules, context = {}) {
  const {
    aggregated = [],
    flywayModules = new Map(),
    resourceLocations = { declared: false, locations: [] },
    allowList = [],
  } = context;
  const aggregatedSet = new Set(aggregated);
  const errors = [];
  const warnings = [];
  const allowed = (rule, subject) => allowList.some((entry) => entry?.rule === rule && entry?.subject === subject);
  const report = (target, rule, subject, item, message) => {
    if (allowed(rule, subject)) return;
    target.push({ rule, subject, ...item, message });
  };

  const units = modules.flatMap((module) => module.units);
  const migrationOwners = new Map();
  for (const unit of units) {
    for (const key of unit.migrations) {
      if (!migrationOwners.has(key)) migrationOwners.set(key, []);
      migrationOwners.get(key).push(unit);
    }
  }

  const starterMisses = new Set();
  for (const unit of units) {
    if (!unit.isStarter || aggregatedSet.has(unit.artifactId)) continue;
    starterMisses.add(unit.artifactId);
    report(errors, RULE_STARTER_NOT_AGGREGATED, unit.artifactId, {
      artifactId: unit.artifactId,
      module: unit.path,
      hint: insertHint(unit.artifactId, aggregated),
    }, `starter ${unit.artifactId} 未在 ${ADMIN_AGGREGATE_POM} 聚合（模块 ${unit.path}）`);
  }

  for (const [key, owners] of migrationOwners) {
    const owner = owners[0];
    const entry = flywayModules.get(key);
    if (!entry) {
      report(errors, RULE_FLYWAY_MODULE_MISSING, key, {
        module: owner.path,
        owners: owners.map((unit) => unit.artifactId),
        ymlKey: `mango.persistence.flyway.modules.${key}`,
      }, `迁移键 ${key} 在 ${MONOLITH_APP_YML} 的 mango.persistence.flyway.modules 中没有开关（模块 ${owner.path}）`);
      continue;
    }
    if (entry.enabled !== true && !entry.skipReason) {
      report(errors, RULE_FLYWAY_SKIP_REASON_MISSING, key, {
        module: owner.path,
        enabled: entry.enabled,
        ymlKey: `mango.persistence.flyway.modules.${key}`,
      }, `迁移键 ${key} 的开关既不是 enabled: true，也没有非空 skip-reason（${MONOLITH_APP_YML}）`);
    }
  }

  for (const key of flywayModules.keys()) {
    if (migrationOwners.has(key)) continue;
    report(warnings, RULE_FLYWAY_MODULE_ORPHAN, key, {
      ymlKey: `mango.persistence.flyway.modules.${key}`,
    }, `迁移键 ${key} 已声明开关，但官方平台能力模块里没有产出该键的迁移（允许，例如来自 infra/extension 模块）`);
  }

  const locationReachable = (resourceLocations.declared ? resourceLocations.locations : DEFAULT_RESOURCE_LOCATIONS)
    .some((location) => location.includes(RESOURCE_LOCATION_PATTERN));
  if (!resourceLocations.declared) {
    const subject = 'mango.resource.registry.locations';
    report(warnings, RULE_RESOURCE_LOCATIONS_IMPLICIT, subject, {
      ymlKey: subject,
    }, `${MONOLITH_APP_YML} 未声明 ${subject}，按 ResourceRegistryProperties 默认值判定 Resource 声明可达`);
  }

  for (const unit of units) {
    if (unit.resources.length === 0) continue;
    const reachableStarter = unit.isStarter
      ? (aggregatedSet.has(unit.artifactId) ? unit.artifactId : null)
      : modules
        .find((module) => module.domain === unit.domain)
        ?.units.find((item) => item.isStarter && aggregatedSet.has(item.artifactId))?.artifactId;
    if (!reachableStarter && !starterMisses.has(unit.artifactId)) {
      report(errors, RULE_RESOURCE_NOT_REACHABLE, unit.artifactId, {
        artifactId: unit.artifactId,
        module: unit.path,
        resources: unit.resources,
      }, `${unit.path} 的 Resource 声明没有可达的已聚合 starter（模块内 ${unit.artifactId} 未进入 ${ADMIN_AGGREGATE_POM}）`);
      continue;
    }
    if (!locationReachable) {
      report(errors, RULE_RESOURCE_NOT_REACHABLE, unit.artifactId, {
        artifactId: unit.artifactId,
        module: unit.path,
        resources: unit.resources,
        locations: resourceLocations.locations,
      }, `${MONOLITH_APP_YML} 的 mango.resource.registry.locations 不包含 ${RESOURCE_LOCATION_PATTERN}，${unit.artifactId} 的 Resource 声明不可达`);
    }
  }

  return { total: units.length, errors, warnings };
}

function main() {
  const options = parseArgs(process.argv.slice(2));
  const modules = discoverModules(join(options.root, PLATFORM_DIR));
  const aggregated = parseAggregatedStarters(join(options.root, ADMIN_AGGREGATE_POM));
  const flywayModules = parseFlywayModules(join(options.root, MONOLITH_APP_YML));
  const resourceLocations = parseResourceLocations(join(options.root, MONOLITH_APP_YML));
  const allowList = loadAllowList(options.allowFile);
  const { total, errors, warnings } = evaluate(modules, {
    aggregated,
    flywayModules,
    resourceLocations,
    allowList,
  });

  if (options.json) {
    console.log(JSON.stringify({ total, errors, warnings }, null, 2));
  } else {
    const starterCount = modules.flatMap((module) => module.units).filter((unit) => unit.isStarter).length;
    console.log(
      `扫描 ${modules.length} 个官方平台能力域、${total} 个模块单元（starter ${starterCount} 个，admin 聚合 ${aggregated.length} 个，`
        + `Flyway 键 ${flywayModules.size} 个，白名单 ${allowList.length} 条）`,
    );
    console.log(`装配点不一致（ERROR）：${errors.length} 处；提示（WARNING）：${warnings.length} 处`);
    for (const item of errors) console.log(`- [ERROR] ${item.rule} ${item.subject} ${item.message}`);
    for (const item of warnings) console.log(`- [WARNING] ${item.rule} ${item.subject} ${item.message}`);
  }

  const failing = options.failOn === 'all' ? errors.length + warnings.length : errors.length;
  if (failing > 0) {
    console.error(
      `官方能力模块装配点校验失败：${errors.length} 处 ERROR、${warnings.length} 处 WARNING（--fail-on=${options.failOn}）`,
    );
    process.exit(1);
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) main();
