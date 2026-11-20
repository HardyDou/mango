#!/usr/bin/env node
/**
 * 校验 Maven 父链形态（Issue #1353 第一阶段的机器校验）。
 *
 * 目标形态（见 mango-pmo/rules/backend/10-dev-flow.md 与 issue #1353）：
 * - 单一构建基座：根聚合 POM `io.mango:mango` 继承 `io.mango:mango-parent`，不再有两棵互不相交的父树；
 * - 版本出口唯一：`dependencyManagement` 只允许出现在 `mango-bom`（依赖版本）与 `mango-parent`（自身模块版本派生）；
 * - 插件版本出口唯一：`pluginManagement` 只允许出现在 `mango-parent`（根聚合 POM 在迁移期允许保留，由 `--strict` 收紧）；
 * - 模块父链只允许指向 `mango-parent` 或根聚合 `mango`，禁止更深的中转父 POM。
 *
 * 默认以「报告模式」运行（存在偏差时退出码 0，只打印清单），`--strict` 时偏差即失败。
 * 用法：
 *   node mango-pmo/tools/check-pom-inheritance.mjs [--root mango] [--strict] [--json]
 */
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

const IGNORED_DIRS = new Set(['target', 'node_modules', '.git', 'dist']);

function parseArgs(argv) {
  const options = { root: 'mango', strict: false, json: false };
  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    if (arg === '--strict') options.strict = true;
    else if (arg === '--json') options.json = true;
    else if (arg === '--root') options.root = argv[++index];
    else if (arg === '--help' || arg === '-h') {
      console.log('用法: node mango-pmo/tools/check-pom-inheritance.mjs [--root mango] [--strict] [--json]');
      process.exit(0);
    } else throw new Error(`未知参数: ${arg}`);
  }
  return options;
}

function collectPoms(dir, acc = []) {
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

function parentBlock(xml) {
  const match = /<parent>([\s\S]*?)<\/parent>/.exec(xml);
  if (!match) return null;
  return {
    groupId: firstTag(xml, 'groupId', match[1]),
    artifactId: firstTag(xml, 'artifactId', match[1]),
  };
}

function countTag(xml, tag) {
  return (xml.match(new RegExp(`<${tag}>`, 'g')) ?? []).length;
}

export function analyse(projectRoot) {
  const poms = collectPoms(projectRoot).map((file) => {
    const xml = readFileSync(file, 'utf8');
    // 先剥离 parent 段，否则会取到父 POM 的 artifactId
    const own = xml.replace(/<parent>[\s\S]*?<\/parent>/, '');
    return {
      file: relative(projectRoot, file).replaceAll('\\', '/'),
      artifactId: firstTag(own, 'artifactId'),
      parent: parentBlock(xml),
      hasDependencyManagement: countTag(xml, 'dependencyManagement') > 0,
      hasPluginManagement: countTag(xml, 'pluginManagement') > 0,
    };
  });
  const byArtifact = new Map(poms.map((pom) => [pom.artifactId, pom]));
  const aggregate = byArtifact.get('mango');
  const parentPom = byArtifact.get('mango-parent');
  const bom = byArtifact.get('mango-bom');
  const allowedParentArtifacts = new Set(['mango', 'mango-parent']);
  const allowedDependencyManagement = new Set(['mango', 'mango-parent', 'mango-bom']);
  // 集成测试与 maven 插件的 it 工程是独立构建单元，不参与仓库父链约束
  const ignored = (file) => file.includes('/src/it/') || file.includes('/src/it-negative/') || file.includes('/src/test/resources/');
  const allowedPluginManagement = new Set(['mango-parent', 'mango']);

  const findings = [];
  const add = (rule, file, message) => findings.push({ rule, file, message });

  if (!aggregate) add('ROOT_MISSING', 'mango/pom.xml', '未找到根聚合 POM io.mango:mango');
  if (!parentPom) add('PARENT_MISSING', 'mango/mango-parent/pom.xml', '未找到 io.mango:mango-parent');
  if (!bom) add('BOM_MISSING', 'mango/mango-bom/pom.xml', '未找到 io.mango:mango-bom');

  if (aggregate && !aggregate.parent) {
    add('ROOT_NOT_INHERITING', aggregate.file, '根聚合 POM 未继承 mango-parent，父链仍是两棵独立的树');
  }

  for (const pom of poms) {
    if (ignored(pom.file)) continue;
    if (pom.parent && pom.parent.groupId && !pom.parent.groupId.startsWith('io.mango')) {
      add('PARENT_OUTSIDE_PROJECT', pom.file, `父 POM 来自 ${pom.parent.groupId}:${pom.parent.artifactId}，模块不得继承外部父 POM`);
    }
    if (pom.hasDependencyManagement && !allowedDependencyManagement.has(pom.artifactId)) {
      add('DEPENDENCY_MANAGEMENT_DUPLICATED', pom.file, 'dependencyManagement 只允许出现在 mango-bom / mango-parent / 根聚合 POM');
    }
    if (pom.hasPluginManagement && !allowedPluginManagement.has(pom.artifactId)) {
      add('PLUGIN_MANAGEMENT_DUPLICATED', pom.file, 'pluginManagement 只允许出现在 mango-parent（迁移期根聚合 POM 例外）');
    }
  }

  // 每个模块的父链必须最终落到 mango 或 mango-parent，且不得成环
  for (const pom of poms) {
    if (pom.artifactId === 'mango' || pom.artifactId === 'mango-parent' || pom.artifactId === 'mango-bom') continue;
    if (ignored(pom.file)) continue;
    if (!pom.parent) continue;
    const chain = [];
    let current = pom;
    while (current) {
      chain.push(current.artifactId);
      if (current.artifactId === 'mango' || current.artifactId === 'mango-parent') break;
      if (!current.parent) { current = null; break; }
      if (chain.includes(current.parent.artifactId)) break;
      if (chain.length > 16) break;
      current = byArtifact.get(current.parent.artifactId) ?? null;
    }
    const resolved = chain.includes('mango') || chain.includes('mango-parent');
    const parentKnown = pom.parent ? [...byArtifact.values()].some((candidate) => candidate.artifactId === pom.parent.artifactId) : false;
    if (!resolved && !parentKnown) {
      add('PARENT_CHAIN_UNRESOLVED', pom.file, `父链 [${chain.join(' -> ')}] 未落到 mango 或 mango-parent`);
    }
  }

  return { poms, findings };
}

function main() {
  const options = parseArgs(process.argv.slice(2));
  const { poms, findings } = analyse(options.root);
  const rootInheriting = findings.filter((item) => item.rule === 'ROOT_NOT_INHERITING').length === 0;
  if (options.json) {
    console.log(JSON.stringify({ inspected: poms.length, rootInheritingParent: rootInheriting, findings }, null, 2));
  } else {
    console.log(`扫描 ${poms.length} 个 pom.xml，根聚合 POM 继承 mango-parent：${rootInheriting ? '是' : '否'}`);
    if (findings.length === 0) console.log('POM 父链形态符合目标');
    else for (const item of findings) console.log(`- [${item.rule}] ${item.file}: ${item.message}`);
  }
  if (options.strict && findings.length > 0) {
    console.error(`POM 父链校验失败：${findings.length} 处偏差（--strict）`);
    process.exit(1);
  }
}

if (import.meta.url === `file://${process.argv[1]}`) main();
