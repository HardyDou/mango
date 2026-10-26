#!/usr/bin/env node
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';

export const UPGRADE_GUIDE_MARKER = '<!-- mango-release-upgrade-guide: generated -->';
export const MANUAL_UPGRADE_GUIDE_MARKER = '<!-- mango-release-upgrade-guide: manual -->';

export function upgradeGuideRelativePath(plan) {
  const sourceVersion = plan.maven?.sourceVersion || inferOldestSourceVersion(plan);
  const targetVersion = plan.maven?.targetVersion || inferNewestTargetVersion(plan);
  if (!sourceVersion || !targetVersion) {
    throw new Error('automatic upgrade guide requires a Maven source and target version');
  }
  return `mango-docs/guides/operations/mango-${sourceVersion}-to-${targetVersion}-upgrade.md`;
}

export function renderUpgradeGuideFor({ plan, notes }) {
  const sourceVersion = plan.maven?.sourceVersion || inferOldestSourceVersion(plan);
  const targetVersion = plan.maven?.targetVersion || inferNewestTargetVersion(plan);
  return renderUpgradeGuide({ plan, notes, sourceVersion, targetVersion });
}

export function assertUpgradeGuide({ repoRoot, plan, notes }) {
  const relativePath = upgradeGuideRelativePath(plan);
  const outputPath = join(repoRoot, relativePath);
  if (!existsSync(outputPath)) {
    throw new Error(`release plan requires the generated upgrade guide: ${relativePath}`);
  }
  const existing = readFileSync(outputPath, 'utf8');
  if (existing.includes(MANUAL_UPGRADE_GUIDE_MARKER)) return relativePath;
  if (!existing.includes(UPGRADE_GUIDE_MARKER)) {
    throw new Error(`upgrade guide ${relativePath} is hand-written; add the manual marker or regenerate it`);
  }
  const expected = renderUpgradeGuideFor({ plan, notes });
  if (existing !== expected) {
    throw new Error(`upgrade guide ${relativePath} does not match the release plan; rerun the release plan`);
  }
  return relativePath;
}

export function generateUpgradeGuide({ repoRoot, plan, notes }) {
  const sourceVersion = plan.maven?.sourceVersion || inferOldestSourceVersion(plan);
  const targetVersion = plan.maven?.targetVersion || inferNewestTargetVersion(plan);
  if (!sourceVersion || !targetVersion) {
    throw new Error('automatic upgrade guide requires a Maven source and target version');
  }

  const relativePath = upgradeGuideRelativePath(plan);
  const outputPath = join(repoRoot, relativePath);
  const content = renderUpgradeGuide({ plan, notes, sourceVersion, targetVersion });

  if (existsSync(outputPath)) {
    const existing = readFileSync(outputPath, 'utf8');
    if (existing.includes(UPGRADE_GUIDE_MARKER)) {
      writeFileSync(outputPath, content);
      return { relativePath, action: 'updated' };
    }
    return { relativePath, action: 'preserved' };
  }

  mkdirSync(dirname(outputPath), { recursive: true });
  writeFileSync(outputPath, content);
  return { relativePath, action: 'created' };
}

function renderUpgradeGuide({ plan, notes, sourceVersion, targetVersion }) {
  const packages = plan.packages ?? [];
  const versions = packages
    .map((entry) => `| \`${entry.name}\` | \`${entry.sourceVersion}\` | \`${entry.targetVersion}\` |`)
    .join('\n');
  const changedSections = extractReleaseSections(notes);
  const releaseTag = plan.release?.tag || 'pending-release';
  const npmNames = packages.map((entry) => `\`${entry.name}@${entry.targetVersion}\``).join('、');

  return `${UPGRADE_GUIDE_MARKER}
# Mango ${sourceVersion} → ${targetVersion} 升级指南

**适用对象：使用 Mango Maven ${sourceVersion} 或更早版本的业务项目。** 本页由 Mango 发布计划自动生成；人按章节执行，Agent 按第 8 节逐项执行。

发布批次：\`${releaseTag}\`。发布状态、完整制品校验和与不可变 Tag 以对应 GitHub Release 为准。

## 1. 本次升级范围

### Maven

- Maven / docs bundle：\`${sourceVersion}\` → \`${targetVersion}\`
- 目标版本包含当前发布计划的全部 Maven 非应用坐标；不要只替换单个 starter。

### npm

${npmNames || '本批次无 npm 包变更。'}

| 包 | 升级前 | 升级后 |
|---|---:|---:|
${versions || '| 无 | — | — |'}

## 2. 发布变更摘要

以下内容来自本批次发布说明；模块 README 是运行时事实源，若与本页冲突，以当前模块 README 为准。

${changedSections || '- 详见 GitHub Release 与本批次各模块 README。'}

## 3. 升级前检查

1. 确认数据库备份可恢复，尤其是 Resource Registry、Flyway history、Bootstrap receipt 和业务目标表。
2. 阅读当前版本对应的模块 README、能力地图和本文引用的专项指南。
3. 检查业务项目是否引用本批次删除或收紧的 API、配置、枚举、菜单或前端导出。
4. 记录当前 Maven、npm、CLI、PMO 版本以及构建锁文件；不要混用不同发布批次。
5. 先在预发布数据库执行只读检查，再进入维护窗口。

## 4. 依赖升级

### Maven

将业务项目 \`backend/pom.xml\` 的 Mango 版本锁定为：

\`\`\`xml
<mango.version>${targetVersion}</mango.version>
\`\`\`

同时确认 \`io.mango:mango-docs-bundle:${targetVersion}\` 可从 consume registry 解析。

### npm / CLI

按发布计划整体升级 npm 闭包，不要只升级聚合包：


${npmNames || '本批次无 npm 包变更。'}

\`\`\`bash
pnpm install
pnpm exec mango pmo check --locked
pnpm exec mango docs pull
\`\`\`

CLI 生成项目使用本批次的 \`@mango/cli\`；不要用旧的全局 CLI 覆盖项目内锁定版本。

## 5. 数据库与启动验证

1. 备份后启动一次升级候选，等待 Flyway 与 Resource Bootstrap 完成。
2. 确认所有新增迁移成功，Bootstrap receipt 最终为 \`FINALIZED\`。
3. 对 Resource Registry 检查唯一键、同步模式、归属字段和冲突诊断。
4. 对业务模块执行本文第 2 节涉及的专项验证；迁移失败或身份冲突时停止，不要通过手工修改 Flyway history 绕过。

## 6. 业务回归

- 后端 API、配置、权限、菜单、初始化数据和租户边界。
- 前端构建、路由、页面注册、公开站点和已变更的 npm 导出。
- CLI 生成项目、PMO baseline、docs bundle 和 clean consumer。
- 对本批次 release notes 的 \`Fixed\`、\`Added\`、\`Changed\` 项逐项保留验证证据。

## 7. 回滚边界

- 已发布的 Maven/npm 坐标不可覆盖，只能发布新版本或回退消费者依赖。
- 数据库迁移是否可逆以对应模块 README 为准；不可逆数据操作需要在升级前完成归档或备份。
- 代码回退不等于数据回滚；保留升级前数据库快照、Bootstrap receipt、迁移日志和前端构建产物。

## 8. Agent 执行清单

| 步骤 | 动作 | 通过判据 | 停止条件 |
|---|---|---|---|
| 1 | 读取能力地图、模块 README、本文和专项升级指南 | 当前事实源与目标版本一致 | 文档冲突，先报告，不套用历史路径 |
| 2 | 盘点 API、配置、枚举、前端导出和数据库迁移影响 | 形成命中清单 | 发现破坏性引用未给出迁移方案 |
| 3 | 备份数据库和构建锁文件 | 可恢复证据已保存 | 无可验证备份 |
| 4 | 升级 Maven/npm/CLI 全闭包 | 依赖解析、构建与锁文件通过 | peer 冲突、坐标解析失败或混批次 |
| 5 | 执行迁移并启动 Bootstrap | Flyway 成功，receipt=FINALIZED | 迁移失败、冲突、ownership 未证明项未处理 |
| 6 | 执行 API、前端、CLI 和业务回归 | release notes 变更项均有证据 | 任一关键回归失败 |
| 7 | 保存升级证据并决定放行 | 命令、版本、日志、结果齐全 | 缺证据不要宣告完成 |

## 9. 关联记录

- GitHub Release：<https://github.com/HardyDou/mango/releases/tag/${releaseTag}>
- 发布说明：仓库根目录 \`CHANGELOG.md\` 的本批次章节。
- 计划文件：
  - Release plan：\`mango-ui/.changeset/release-plan.json\`
  - Catalog：\`mango-catalog/catalog.lock.json\`
- 版本事实源：\`mango-ui/packages/mango-cli/release-versions.json\`
`;
}

function extractReleaseSections(notes = '') {
  const sections = [];
  for (const heading of ['Fixed', 'Added', 'Changed']) {
    const start = notes.indexOf(`# ${heading}`);
    if (start < 0) continue;
    const bodyStart = notes.indexOf('\n', start) + 1;
    const nextHeading = notes.slice(bodyStart).match(/\n#{1,6} /u);
    const next = nextHeading ? bodyStart + nextHeading.index : -1;
    const body = notes.slice(bodyStart, next < 0 ? notes.length : next).trim();
    if (body) sections.push(`### ${heading}\n\n${body}`);
  }
  return sections.join('\n\n');
}

function inferOldestSourceVersion(plan) {
  const versions = (plan.packages ?? []).map((entry) => entry.sourceVersion).filter(Boolean);
  return versions.sort(compareVersions)[0] || '';
}

function inferNewestTargetVersion(plan) {
  const versions = (plan.packages ?? []).map((entry) => entry.targetVersion).filter(Boolean);
  return versions.sort(compareVersions).at(-1) || '';
}

function compareVersions(left, right) {
  return left.localeCompare(right, undefined, { numeric: true });
}
