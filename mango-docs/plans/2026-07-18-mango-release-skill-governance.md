# Mango Release Skill 治理记录

## 目标与范围

本次治理把仓库内 `mango-pmo/skills/mango-release` 固定为 Mango 发布、恢复、验证和收尾的唯一 Skill 入口，并把同一版本化投影交付到 `@mango/pmo`、CLI full template 和业务 baseline。

范围包括发布说明预检、结构化 adapter 语义检查、不可变制品恢复、只读验证恢复、CHANGELOG 状态回填、GitHub Release 一致性和服务/worktree/分支/数据库清理。不改变 Maven 运行时、业务 API、数据库、菜单、权限、租户、路由或前端业务行为。

## 触发事实

- CLI 1.0.81 首次 npm 状态在真正上传前因 GitHub Release 正文缺少 `Published Packages` 被阻断。
- 补齐正文后 CLI 成功发布，但最终消费者验证把 YAML 单引号误判为失败；已发布制品不能因此重发。
- 发布事实最终由原失败清单和完整只读验证清单共同保留，说明既有 Skill 缺少发布说明预检、语义断言和验证恢复指引。
- 用户明确要求 Mango 后续不得使用外部通用 release skill。

## 决策

长期约束仍以 [发布制品与版本同步规范](../../mango-pmo/rules/10-release-artifacts.md) 为源；仓库和生成项目 `AGENTS.md` 只维护 Skill 路由。`mango-release` 负责可执行步骤和关闭条件，不复制 Maven/npm 发布脚本。

PMO/Skill bundle 从 1.3.0 升到 1.3.1；CLI 因精确依赖 PMO，从 1.0.81 升到 1.0.82。Mango Maven保持 1.0.22，运行时前端包保持原版本。

## 实施计划

1. 强化仓库入口、发布规则、Skill 和 Skill prompt。
2. 增加本地 Skill 优先、Release 正文缺项、远端写入前失败、发布后验证器误判和收尾遗漏的正反例 eval。
3. 升版 PMO/CLI，更新契约固定版本、README、CHANGELOG、能力地图和 full template。
4. 机械构建并同步业务 PMO baseline，校验 package/plugin/Skill 哈希和权限。
5. 通过 PR 合并后，使用仓库内 `mango-release` 发布 PMO 1.3.1 和 CLI 1.0.82；发布后另行回填状态并清理工作区。

## 验证与复核

- M09：Skill quick validator、JSON/规则索引、diff check、workspace layout、版本影响、README source facts。
- M10：PMO 文档合同、风险/工作区/发布路由测试和 CLI release state 单元测试。
- M11：PMO 真实 pack、业务 baseline 投影、CLI PMO 安装/升级/回滚和 package contracts。
- M14：从发布安全、PMO 唯一规范源、不可变制品和业务消费者四个视角复核；阻断项为“不得静默替换 PMO 1.3.0”和“不得因验证器格式误判重发”，均由 1.3.1/1.0.82 版本化发布与只读恢复关闭。
- M15：PR required checks、分支保护、Nexus hosted/group、tag 和 GitHub Release 回读在发布阶段执行。

## 回滚与关闭

PR 合并前可整体回退该任务分支。制品发布后不删除或覆盖不可变版本；业务项目可继续锁定 PMO 1.3.0/CLI 1.0.81。只有发布清单完成、CHANGELOG 通过 PR 回填、任务服务与 workspace/数据库/分支已清理且本地 `main` 等于 `origin/main` 时才关闭本次治理。
