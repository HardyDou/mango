# @mango/pmo

## 1. 概览

`@mango/pmo` 是 Mango PMO baseline 和交付 Skills 的 npm 发布包。长期规则仍维护在仓库根目录 `mango-pmo`，本包把规则、角色、模板、文档契约、工具和 Skill 构建成一个可校验快照。包名表示发布与治理归属，不代表所有 Skill 都由产品经理执行。

业务项目通过 `@mango/cli` 消费本包，不直接依赖包内脚本作为运行时代码。

## 2. 功能清单

| 能力              | 入口                                                                        | 说明                                                                               |
| ----------------- | --------------------------------------------------------------------------- | ---------------------------------------------------------------------------------- |
| 构建 baseline     | `pnpm -F @mango/pmo build`                                                  | 复制 `mango-pmo` 到 `dist/baseline`                                                |
| 校验 baseline     | `pnpm -F @mango/pmo check`                                                  | 校验必备文件、manifest、preflight 及真实 `pnpm pack` 的 hash、size、mode           |
| 发布 manifest     | `dist/baseline.json`                                                        | 记录 package version、source commit、bundle hash、contract revision 和逐文件元数据 |
| Codex plugin 投影 | `.codex-plugin`、`skills`                                                   | npm 包根可安装插件；版本由 build 从 package metadata 生成                          |
| 业务同步          | `mango pmo sync/upgrade`                                                    | CLI 从本包安装业务仓 baseline，并同步 canonical PR 风险合同区段                    |
| 影响驱动门禁      | `dist/baseline/tools/risk-verification.mjs`、`classify-pmo-check-scope.mjs` | 校验需求/方案风险，并把 Java PR 限定到受影响 Maven 模块                            |
| 前端页面基线      | `dist/baseline/tools/check-frontend-page-baseline.mjs`                      | 阻断新增或修改页面继续使用旧列表骨架和原生标准 Dialog                              |

## 3. 接入方式

Mango 发布前执行：

```bash
pnpm -F @mango/pmo build
pnpm -F @mango/pmo check
```

业务项目使用：

```bash
npm view @mango/pmo@1.3.8 version --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/
npm view @mango/cli@1.0.94 version --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/
npm install -g @mango/cli@1.0.94 --registry http://nexus.inner.yunxinbaokeji.com/repository/npm-group/
mango pmo status --project-dir .
mango pmo upgrade --project-dir . --to 1.3.8 --dry-run
mango pmo upgrade --project-dir . --to 1.3.8 --sync-shell
mango pmo check --project-dir . --locked
```

升级会原子同步 `business-pmo/mango-baseline`、`business-pmo/pmo-lock.json`、项目 Agent 入口和 `.agents/skills`。项目级 Skill 与 PMO bundle 使用同一 manifest/hash，不需要逐个安装；用户级 Codex plugin 是独立的可选安装面，不由业务项目升级命令修改。

## 4. 配置说明

| 配置入口                                         | 字段                                                    | 含义                                                                   |
| ------------------------------------------------ | ------------------------------------------------------- | ---------------------------------------------------------------------- |
| `package.json`                                   | `files`                                                 | 发布 `dist`、`.codex-plugin`、`skills`、README 和 package metadata     |
| `package.json`                                   | `exports`                                               | 暴露 baseline manifest、baseline 文件、`./plugin.json` 和 `./skills/*` |
| `dist/baseline.json`                             | `packageVersion`                                        | 当前 baseline 包版本                                                   |
| `dist/baseline.json`                             | `sourceCommit`、`bundleSha256`                          | 可复现源码和整包身份                                                   |
| `dist/baseline.json`                             | `files[].sha256`                                        | 业务仓漂移检查依据                                                     |
| `dist/baseline.json`                             | `files[].kind`、`files[].mode`                          | 文件职责和发布权限                                                     |
| `dist/baseline.json`                             | `contracts[]`                                           | 文档 contract ID 和 schema revision                                    |
| `business-pmo/pmo-lock.json`                     | `packageVersion`、`bundleSha256`                        | 业务项目精确锁定的 PMO bundle                                          |
| 业务仓 `mango.config.json`                       | `paths.backend`、`paths.frontend`、`paths.businessDocs` | 标准 scope classifier 与 GitHub/Gitea workflow 使用的项目目录          |
| 消费仓库 `.github/branch-protection-policy.json` | `governanceMode` 与保护字段                             | 仓库自行声明的远端分支保护期望状态；不由 npm 包覆盖                    |

## 5. API 与扩展

| API / 扩展点                | 输入                            | 输出                                                  |
| --------------------------- | ------------------------------- | ----------------------------------------------------- |
| `scripts/build-package.mjs` | `mango-pmo/**`                  | `dist/baseline/**`、manifest、包根 plugin/skills 投影 |
| `scripts/check-package.mjs` | `dist/baseline/**`              | 校验结果                                              |
| `exports["."]`              | npm import                      | `dist/baseline.json`                                  |
| `exports["./baseline/*"]`   | npm package path                | baseline 文件                                         |
| `exports["./plugin.json"]`  | npm package path                | package-root Codex plugin manifest                    |
| `exports["./skills/*"]`     | npm package path                | package-root 交付 Skill 投影                          |
| npm package root            | `.codex-plugin/**`、`skills/**` | Codex plugin 安装投影                                 |

## 6. 数据与初始化

本包不初始化数据库、菜单、权限、租户或业务数据。

| 类型              | 位置                          | 初始化方式                                           |
| ----------------- | ----------------------------- | ---------------------------------------------------- |
| PMO baseline      | `dist/baseline`               | build 脚本从 `mango-pmo` 复制                        |
| baseline manifest | `dist/baseline.json`          | build 脚本按文件内容生成 hash                        |
| 业务项目 baseline | `business-pmo/mango-baseline` | `@mango/cli` 安装或升级                              |
| 业务项目 PMO lock | `business-pmo/pmo-lock.json`  | CLI 原子切换成功后写入                               |
| 业务项目 Skill    | `.agents/skills/**`           | CLI 按 bundle manifest 同步；不修改用户级 Codex 配置 |

## 7. 管理入口

| 任务     | 命令                                |
| -------- | ----------------------------------- |
| 构建包   | `pnpm -F @mango/pmo build`          |
| 校验包   | `pnpm -F @mango/pmo check`          |
| 发布包   | `pnpm publish:pkg pmo --dry-run`    |
| 业务升级 | `mango pmo upgrade --project-dir .` |

## 8. 快速开始

1. 修改根目录 `mango-pmo/**`。
2. 执行 `pnpm -F @mango/pmo build`。
3. 执行 `pnpm -F @mango/pmo check`。
4. `check` 会执行真实 `pnpm pack` 并逐文件校验 tarball；发布后还必须从消费仓库下载并运行 package-root 校验。
5. 业务项目通过 `mango pmo upgrade --to <version>` 原子更新 baseline、项目锁和项目级 Skill。
6. 使用 `mango pmo check --locked` 校验项目锁；需要恢复时执行 `mango pmo rollback`。

## 9. 问题排查

| 问题                                       | 原因                                                | 处理方式                                                                                          |
| ------------------------------------------ | --------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| `dist/baseline.json` 不存在                | 未执行 build                                        | 执行 `pnpm -F @mango/pmo build`                                                                   |
| check 报 hash mismatch                     | dist 内容和 manifest 不一致                         | 重新 build 后再 check                                                                             |
| 业务项目 baseline changed                  | 业务仓锁定快照被修改                                | 执行 `mango pmo sync --project-dir .` 修复当前锁定版本                                            |
| `sync` 提示锁定版本不可用                  | 当前 CLI 解析的 PMO 版本与项目锁不同                | 使用项目内锁定 CLI，或显式执行 `upgrade --to`                                                     |
| 项目 Skill changed / extra                 | `.agents/skills` 中 PMO 管理文件被修改或残留        | 执行 `mango pmo sync` 修复当前锁定版本                                                            |
| PR template missing / differs              | 项目模板缺失或 Risk / Verification 与锁定合同不一致 | 执行项目内 `mango pmo sync --project-dir .`；重复区段先人工合并                                   |
| Codex 中未出现插件                         | 项目 Skill 已同步不等于用户级 plugin 已安装         | 从 npm 包根插件投影执行独立 Codex plugin 安装流程并新开会话                                       |
| npm tarball 缺 baseline                    | 发布前未 build 或 files 配置错误                    | 执行 pack dry-run 并检查 `package.json`                                                           |
| npm tarball 的工具 mode 与 manifest 不一致 | 打包器没有保留 manifest 声明的可执行权限            | 不发布该 tarball；同步 `publishConfig.executableFiles` 后重新执行 build/check，并使用新的补丁版本 |
| `npm view` 返回 404                        | 目标版本尚未进入消费仓库                            | 等待发布状态机和 npm-group 回查完成，不使用源码目录冒充已发布包                                   |

## 10. 相关文档

- [Mango PMO Baseline](../../../mango-pmo/README.md)
- [@mango/cli](../mango-cli/README.md)
- [PMO 总流程](../../../mango-pmo/rules/00-dev-flow.md)
- [开发环境规范](../../../mango-pmo/rules/02-dev-environment.md)
