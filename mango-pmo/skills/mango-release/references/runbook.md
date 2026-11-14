# Mango 发布批次 Runbook

本文件是 `mango-release` Skill 的执行手册：把一次 Maven/docs 或 npm 发布批次从 `plan` 做到 `closeout` 的固定顺序、每步产物和已验证的失败处置记录下来。它不替代 `SKILL.md` 的边界、授权和状态机定义，也不进入 `@mango/pmo` 包或业务基线。

适用：Mango 主仓的一次不可变组件发布批次。不适用：普通开发 PR、业务项目 PR、业务应用发布与部署。

## 1. 批次执行清单

按顺序执行，每步通过后再进入下一步；任一步失败先按第 4 节处置，不跳步。

| 步 | 动作 | 命令 | 通过判据 |
|---|---|---|---|
| 1 | 同步与基线确认 | `git fetch origin --prune --tags && git merge --ff-only origin/main` | `HEAD == origin/main`，工作区干净 |
| 2 | 主仓一致性与改动盘点 | `node mango-ui/scripts/catalog/compile-catalog.mjs --check`、`git diff --name-only <上一层Tag>..HEAD` | Catalog PASS；列出本批次 PR 与受影响包 |
| 3 | 建发布工作树 | `git worktree add -b release/<...> /Users/hardy/<...> origin/main` | 工作树在仓库外，`pmo-preflight` 通过 |
| 4 | 补 Changeset 并固定 plan 源 | 提交 `.changeset/*.md` 后执行 `pnpm --dir mango-ui release:plan -- --tag <tag> --maven-version <v>` | `release-plan.json` 的 `source.commit` 指向 Changeset 提交 |
| 5 | 写发布物料 | `release-notes.txt`、根 `CHANGELOG.md`、`mango-pmo/CHANGELOG.md`、`packages/mango-cli/CHANGELOG.md`、升级指南 | `release:plan:check`、`release:check-notes`、`catalog:check`、`release:impact` 全 PASS |
| 6 | 封存候选 | `pnpm --dir mango-ui release:prepare -- --consume-registry <npm-group>` | manifest `READY`，`preparedCandidateId` 与 npm/Maven 数量确定 |
| 7 | Release PR 与发布 | `classify-release-pr` 为 release-only → PR → 合并 → `release:publish --authorize` | 合并后 `origin/main` tree 等于候选 tree；manifest `COMPLETED` |
| 8 | 收尾与清理 | 回填 `CHANGELOG` 为 `PUBLISHED_AND_VERIFIED`、推进 `release-baseline.json`、删除已消费 Changeset、closeout PR、清理工作树 | 收尾 PR 合并，`main` 同步，Tag 未移动 |

批次开始前必须同时满足：Catalog 在 `main` 上一致；本批次 PR 都已合并；Registry 凭证可用（`~/.m2/settings.xml` 的 `maven-releases`、npm token）。

## 2. 版本与 Changeset 判定

| 事实 | 版本动作 |
|---|---|
| 新增平台模块、新能力、新公开配置或接口字段 | Maven minor，受影响 npm 包 minor |
| 缺陷修复、规则阈值调整、文档/模板修正 | Maven patch，受影响 npm 包 patch |
| 只有 `mango/**` 源码变化、无前端包变化 | Maven-only 批次；`@mango/cli` 由版本矩阵自动 patch，不需要 Changeset |
| 改动 `mango-ui/packages/**` 已发布源码（含 README、templates、scripts） | 必须有该包的 Changeset，否则 `release plan` 报 `missing Changeset declaration` |
| 改动 `mango-pmo/rules|contracts|templates|agents|skills` | 需要 `@mango/pmo` 新版本与 CLI 对应 patch |
| 只改 `mango-release`（repository-only） | 不需要 PMO 版本，因为它不进入 `@mango/pmo` 包 |

版本号必须由用户在批次开始前确认；工具只校验单调递增和目标一致性，不替人决定 minor/patch。

## 3. 发布物料写法

- `release-notes.txt`：`Pull Requests` 每个 PR 必须带 `Fixed|Added|Changed`、`Packages:`、`Business Adaptation:`；`Versions` 和 `Published Packages` 必须列出本批次**每一个** npm 包名与目标版本（`release:prepare` 会逐包运行 notes 检查）。
- 根 `CHANGELOG.md`：批次标题行必须含 Tag 和 Maven 目标版本；内部章节用 `###`，不能用 `##`，否则发布说明解析会在第一个 `##` 处截断。
- 升级指南由 `release plan` 生成，不手写；需要人工说明时加 `<!-- mango-release-upgrade-guide: manual -->` 后自行维护。
- 全部文本禁止占位符形态：`<...>`、`TODO`、`TBD`、`待补充`、`待确认` 都会被检查器拒绝。
- npm 包涉及 `@mango/pmo` 时，`mango-pmo/CHANGELOG.md` 必须有对应版本条目；`mango-cli` 的 `CHANGELOG.md` 同理。

## 4. 失败处置矩阵

以下问题在本仓真实发生过，处置已执行验证。

| 现象 | 根因 | 处置 | 预防 |
|---|---|---|---|
| `classify-release-pr` 报 `PMO plugin source version X != Y` | 在投影提交上重跑了 `release plan`，`source.commit` 漂移 | `git reset --soft <Changeset提交>` → 重跑 plan → 同步 `CHANGELOG` digest → 只做一次投影提交 | plan 只在 Changeset 提交上跑一次；投影后不再跑 plan |
| `release notes check failed for @mango/<pkg>` | 说明没列出全部包名+目标版本 | 从 `release-plan.json` 生成包清单写进 `Versions` 与 `Published Packages` | 先写清单再跑 plan |
| `Root CHANGELOG.md section ... does not mention <pkg>/<version>` | 章节用了 `##` 而非 `###`，解析被截断 | 章节降为 `###` 后重建该段 | 复制上一批次章节结构 |
| 说明报 `unresolved placeholder` | 文本含 `<path>` 之类尖括号 | 改为自然语言描述 | 写完后 `rg '<[^>]+>'` 自查 |
| `package-exports:check` 报 `dist/index.js` 缺失 | 未在批次里的包没有构建产物 | `pnpm --dir mango-ui -r --filter "@mango/*" run build` 后重跑 prepare | 进入 prepare 前先全量构建包 |
| `package-exports:check` 报样式入口缺失 | 已发布样式入口的 `@import` 没有扩展名（#1221 后检查器更严） | 在源文件补 `.scss` 扩展名，单独提修复 PR 后再发布 | 发布前在 `main` 上跑一次该检查 |
| `catalog:check` 在 `main` 上失败 | 生成投影落后于源（漏跑 `catalog -- --write`） | 先提修复 PR（投影修复 + 片段对齐），合并后重新规划批次 | 批次开始前先跑 `catalog:check` |
| `release plan` 报 `Changeset declares unrelated package` | 在修复 PR 里声明了该 PR 未改动的包 | 把 Changeset 放到真正改动该包的 PR 或发布分支 | 先看 `git diff --name-only` 再声明 |
| `release prepare` 报某 npm 包 404 | Nexus 代理未缓存第三方 tarball | `curl -o /dev/null -w '%{http_code}'` 校验后重试 `prepare` | 首次失败不要改版本、不要重建候选目录之外的配置 |
| closeout PR `CONFLICTING` / 无 checks | closeout 分支基于发布分支，squash 合并后与 `main` 历史冲突 | `git diff origin/main <closeout提交>` 导出补丁，重置到 `origin/main` 后重新提交并强推 | closeout 分支直接从最新 `origin/main` 起 |
| closeout PR 合同检查失败 | `Final risk: L1.`、`Workspace decision: REUSE - path` 等带后缀 | 值必须是裸标记：`L1`、`SIMPLE`、`REUSE`；`Assurance selections` 结尾不加句号；`Assurance evidence` 用 `Mxx - 事实` 分号分隔；补 `Loaded PMO files` | 复用上一批次已验证的 closeout body |

## 5. 收尾与清理

1. `closeout` 只改三个位置：根 `CHANGELOG.md`（`PENDING` → `PUBLISHED_AND_VERIFIED` 并写入 digest 与 candidate）、`mango-ui/.changeset/release-baseline.json`（推进到 sealed source commit、plan digest、candidate）、删除已消费 Changeset。
2. Tag 永远指向制品提交，不移动到 closeout 提交。
3. 保留 manifest 副本到主仓 `.runtime/mango-release/<planDigest>/manifest.json` 后再删除工作树。
4. 清理顺序：`git worktree remove --force` → 删本地分支 → 删远端分支 → `git fetch --prune` → 回 `main` → `git merge --ff-only origin/main`，并证明 `HEAD == origin/main`。
5. 保留其他任务的 worktree、分支和未提交改动，不做全局清理。

## 6. 禁止事项

- 禁止跳过 `plan`/`prepare` 直接手写发布包清单或复用旧候选。
- 禁止在已有远端写入的情况下重建候选、改版本或改 plan。
- 禁止在说明缺失、门禁失败、Registry 状态未知时合并 Release PR 或发布。
- 禁止把本 Runbook 的处置经验改写成业务项目发布流程。
