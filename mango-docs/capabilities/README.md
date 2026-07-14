# Mango 能力地图

## 1. 定位

本文用于帮助 Mango 开发者、业务开发者和 AI Agent 快速定位 Mango 能力、模块 README、关联 PMO 规则和排障入口。

长期规则仍以 `mango-pmo` 为唯一来源；本文只做能力索引，不复制规范正文。

## 2. 使用方式

1. 先按任务关键词找到涉及能力。
2. 阅读能力对应的模块 README。
3. 查看关联 PMO 链接；正式交付规则以 preflight 输出和 `mango-pmo/rules/**` 为准。
4. 能力说明维护要求见 [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)。

处理 Mango 升级、初始化数据、字典、菜单、角色、工作流、Flyway 或 demo 数据时，先看本页“近期能力变更”，再看对应模块 README。历史 migration、历史设计和旧会话上下文只能作为历史证据；如果它们和当前 README 或本页冲突，以当前 README、本页和明确标注为当前口径的设计为准。

## 3. 组合接入入口

| 目标 | 阅读顺序 | 排障入口 |
|------|----------|----------|
| 登录到菜单闭环 | [Identity](../../mango/mango-platform/mango-identity/README.md) -> [Auth](../../mango/mango-platform/mango-auth/README.md) -> [Authorization](../../mango/mango-platform/mango-authorization/README.md) -> [Access](../../mango/mango-platform/mango-access/README.md) -> [Admin Shell](../../mango-ui/packages/admin-shell/README.md) | [Auth README](../../mango/mango-platform/mango-auth/README.md)、[菜单页面打不开排障](../guides/business-integration/rbac-menu-page-troubleshooting.md) |
| 按钮权限闭环 | [Authorization](../../mango/mango-platform/mango-authorization/README.md) -> [Access](../../mango/mango-platform/mango-access/README.md) -> [RBAC Frontend](../../mango-ui/packages/rbac/README.md) -> [Admin Shell](../../mango-ui/packages/admin-shell/README.md) | [Authorization README](../../mango/mango-platform/mango-authorization/README.md)、[按钮权限不显示排障](../guides/business-integration/permission-button-troubleshooting.md) |
| 数据权限闭环 | [Authorization](../../mango/mango-platform/mango-authorization/README.md) -> [Persistence](../../mango/mango-infra/mango-infra-persistence/README.md) -> [RBAC Frontend](../../mango-ui/packages/rbac/README.md) | [Authorization README](../../mango/mango-platform/mango-authorization/README.md)、[Persistence README](../../mango/mango-infra/mango-infra-persistence/README.md) |
| 文件上传到预览闭环 | [File](../../mango/mango-platform/mango-file/README.md) -> [Fileproc](../../mango/mango-infra/mango-infra-fileproc/README.md) -> [File Preview](../../mango/mango-platform/mango-file-preview/README.md) -> [Frontend File](../../mango-ui/packages/file/README.md) | [File README](../../mango/mango-platform/mango-file/README.md)、[文件上传表单接入](../guides/business-integration/file-upload-form.md) |
| 业务审批闭环 | [Workflow](../../mango/mango-platform/mango-workflow/README.md) -> [Workflow Frontend](../../mango-ui/packages/workflow/README.md) -> [Workflow Example](../../mango-ui/packages/workflow-business-example/README.md) | [Workflow README](../../mango/mango-platform/mango-workflow/README.md)、[业务审批接入](../guides/business-integration/workflow-business-approval.md) |
| 租户基础数据和字典闭环 | [Issue #184 数据治理设计](../designs/2026-07-01-issue-184-data-governance-design.md) -> [Identity](../../mango/mango-platform/mango-identity/README.md) -> [Org](../../mango/mango-platform/mango-org/README.md) -> [System](../../mango/mango-platform/mango-system/README.md) -> [Resource Registry](../../mango/mango-platform/mango-resource/README.md) -> [Access](../../mango/mango-platform/mango-access/README.md) | [System README](../../mango/mango-platform/mango-system/README.md)、[租户字典配置为空排障](../guides/business-integration/tenant-dict-config-empty.md)；新增字典优先确认当前 Resource handler 和 `sync-mode`，不要照搬历史 Flyway seed |
| 定时任务闭环 | [Job](../../mango/mango-platform/mango-job/README.md) -> [Job Frontend](../../mango-ui/packages/job/README.md) -> [Notice](../../mango/mango-platform/mango-notice/README.md) | [Job README](../../mango/mango-platform/mango-job/README.md)、[Job Frontend README](../../mango-ui/packages/job/README.md) |
| 业务项目创建到 PR | [CLI](../../mango-ui/packages/mango-cli/README.md) -> [Business Starter](../../mango-business-starter/README.md) -> [Business PMO](../../mango-business-starter/business-pmo/README.md) -> [Topology](../../mango-business-starter/topologies/monolith/README.md) | [CLI README](../../mango-ui/packages/mango-cli/README.md)、[Business Starter README](../../mango-business-starter/README.md) |
| 类型化业务 CRUD | [Persistence CRUD Service](../../mango/mango-infra/mango-infra-persistence/README.md#73-crud-service) -> [后端 API 规范](../../mango-pmo/rules/backend/03-api.md) -> [模块分层规范](../../mango-pmo/rules/backend/05-module.md) -> [Business Starter](../../mango-business-starter/README.md) | [Persistence 迁移说明](../../mango/mango-infra/mango-infra-persistence/README.md#731-从宽类型-crud-迁移) |
| 业务配置资源注入 | [Resource Registry](../../mango/mango-platform/mango-resource/README.md) -> 目标模块 README | [Resource README](../../mango/mango-platform/mango-resource/README.md)，支持授权、组织、身份等基线资源类型 |
| 数据初始化与停机升级治理 | [Issue #184 数据治理设计](../designs/2026-07-01-issue-184-data-governance-design.md) -> [Resource Registry](../../mango/mango-platform/mango-resource/README.md) -> [Persistence](../../mango/mango-infra/mango-infra-persistence/README.md) -> [S5 数据物料清单](../plans/2026-07-01-issue-184-s5-data-material-audit.md) | Resource 负责正式/demo/`INIT_ONLY`；Persistence 负责 DDL、外部 SQL、baseline pack |

## 3.1 近期能力变更

| 日期 | 能力 | 入口 | 设计与交付 |
|------|------|------|------------|
| 2026-07-14 | Worktree Maven 缓存复用：`mango workspace init` 默认把 .mango/m2/repository 链接到用户公共 ~/.m2/repository，业务清单保留 worktree 本地路径写法即可避免重复下载；已有真实目录或其它链接会保留，显式隔离仓库不受影响 | [CLI README](../../mango-ui/packages/mango-cli/README.md) | [Issue #507](https://github.com/HardyDou/mango/issues/507)、[根 CHANGELOG](../../CHANGELOG.md)；目标包 `@mango/cli@1.0.78`，Mango Maven 保持 `1.0.20` |
| 2026-07-14 | Notice 架构债务最终态：服务按职责拆分，API/HTTP/Feign 和仓内消费者同批对齐固定路径；事务后通知事件显式携带租户；新数据库使用单一纯 DDL V1，正式资源不再初始化个人联系方式或运行态数据 | [Notice README](../../mango/mango-platform/mango-notice/README.md)、[Notice Frontend README](../../mango-ui/packages/notice/README.md)、[Mango Tools README](../../mango/mango-tools/README.md) | [Notice 技术设计](../designs/notice-architecture-debt/technical-design.md)、[验证报告](../evidence/baselines/notice-architecture/latest/report.md)、PR #497 |
| 2026-07-14 | PR 门禁加速：PR 描述编辑只运行秒级 `pr-contract-check`；代码 SHA 由 PMO、文档、CLI 与 Java 并行验证并汇总为稳定 `pmo-doc-check`；新 SHA 取消旧运行，generated-backend 验收从 19 次 Maven 启动降为 9 次且不执行 `clean` | [PMO README](../../mango-pmo/README.md)、[CLI README](../../mango-ui/packages/mango-cli/README.md)、[主仓 Workflow](../../.github/workflows/pmo-doc-check.yml) | [根 CHANGELOG](../../CHANGELOG.md)；目标包 `@mango/pmo@1.2.4` / `@mango/cli@1.0.74`，Mango Maven 保持 `1.0.18` |
| 2026-07-14 | partial Maven PR 门禁冷缓存修复：依赖准备阶段用 `maven_dependency_projects` 和 `-am install` 补齐未发布 SNAPSHOT，质量阶段继续只扫描直接模块；`mango:check` 嵌套静态分析排除架构治理聚合模块，外层架构门禁保持启用 | [Mango Tools README](../../mango/mango-tools/README.md)、[Business Starter README](../../mango-business-starter/README.md) | [Issue #480](https://github.com/HardyDou/mango/issues/480)、[Issue #481](https://github.com/HardyDou/mango/issues/481)、[根 CHANGELOG](../../CHANGELOG.md)；目标批次 Mango Maven `1.0.18` / `@mango/pmo@1.2.3` / `@mango/cli@1.0.73` |
| 2026-07-14 | 业务仓目录与 Gitea 标准门禁：`pmo-doc-check` 从 `mango.config.json` 的 `paths` 对象读取后端、前端和业务文档根目录；`baohan-backend/` 等非默认布局可复用同一 scope classifier 和标准 GitHub/Gitea workflow，不再维护治理脚本分叉 | [PMO README](../../mango-pmo/README.md)、[CLI README](../../mango-ui/packages/mango-cli/README.md) | [Issue #470](https://github.com/HardyDou/mango/issues/470)、[根 CHANGELOG](../../CHANGELOG.md)；目标包 `@mango/pmo@1.2.2` / `@mango/cli@1.0.72`，Mango Maven 保持 `1.0.17` |
| 2026-07-14 | GitHub Release 到内网 Jenkins 主动发布能力保留；当前运行入口调整为发布负责人在主仓本机清理 Maven 构建目录后执行统一非 app 批量脚本，以减少 watcher、内网 Git 同步和 Jenkins 调度等待；Jenkins 的重新启用条件与双入口边界记录在操作 README | [Maven 发布入口与 Jenkins 主动发布 README](../../scripts/ci/README.md)、[GitHub Workflow](../../.github/workflows/maven-release.yml)、[Watcher Jenkinsfile](../../jenkins/mango-github-release-watcher.Jenkinsfile)、[Maven Jenkinsfile](../../jenkins/mango-maven-release.Jenkinsfile) | GitHub 仍是版本事实源；GitHub 不持有 Jenkins/Nexus 凭据；本机批量脚本和 Nexus 回查构成当前发布证据 |
| 2026-07-14 | PMO 发布物权限完整性：PMO 包在发布前用真实 `pnpm pack` 对 manifest 的 hash、size、mode 做全量校验，发布后对消费端 tarball 再校验文件 mode；业务升级使用 `@mango/pmo@1.2.1` / `@mango/cli@1.0.71`，不再使用存在权限缺陷的 `1.2.0` / `1.0.70` | [PMO package README](../../mango-ui/packages/mango-pmo/README.md)、[CLI README](../../mango-ui/packages/mango-cli/README.md) | [Issue #464](https://github.com/HardyDou/mango/issues/464)、[根 CHANGELOG](../../CHANGELOG.md)；Mango Maven 保持 `1.0.17` |
| 2026-07-13 | Payment 最终态初始化：未发布且只支持新数据库，Flyway 归并为纯 DDL V1；65 条正式必需配置由 payment starter `resources/` 默认登记，73 条演示数据由 `demo/` 显式开启，运行态数据与商户密钥不初始化 | [Payment README](../../mango/mango-platform/mango-payment/README.md)、[Resource README](../../mango/mango-platform/mango-resource/README.md) | [支付技术设计](../designs/payment-architecture-debt/technical-design.md)、[验证报告](../evidence/baselines/payment-architecture/latest/report.md) |
| 2026-07-13 | 影响驱动 PMO 门禁：需求影响与解决方案风险分别评估，最终等级取最大值；验证从 `STATIC`、`UNIT`、`API`、`UI` 中选择最低成本充分集合。稳定 `pmo-doc-check` 按改动范围执行，普通 Java 质量门禁只验证直接修改的 Maven 模块，构建与消费者兼容性独立验证；全量 212 模块历史债务盘点转为定时/手工任务 | [PMO README](../../mango-pmo/README.md)、[测试流程规范](../../mango-pmo/rules/09-test-case-automation-flow.md)、[CLI README](../../mango-ui/packages/mango-cli/README.md) | [影响驱动风险设计](../designs/2026-07-13-impact-driven-risk-and-verification-design.md)、[根 CHANGELOG](../../CHANGELOG.md)；目标包 Mango Maven `1.0.17` / `@mango/pmo@1.2.0` / `@mango/cli@1.0.70` |
| 2026-07-13 | CLI 可审计发布状态机：`mango release publish/status/verify/repair` 和 `registry doctor` 固定发布状态；已发布的 `@mango/cli@1.0.69` 修复断点恢复边界，恢复路径按是否执行过发布分流为首次 publish、`verify-existing` 或保持 passed | [CLI README](../../mango-ui/packages/mango-cli/README.md)、[发布规范](../../mango-pmo/rules/10-release-artifacts.md) | [根 CHANGELOG](../../CHANGELOG.md)、[发布证据](../evidence/governance/release-v2026.07.13-pmo-1.1.1-cli-1.0.69-release.json)；`1.0.68` 完成首批发布后发现恢复边界，`1.0.69` 为已验证修复版 |
| 2026-07-12 | 类型化业务 CRUD 与架构门禁：`MangoTypedCrudService<E,C,U,Q,V,ID>` 绑定业务 Service 契约，迁移后的 Controller/Feign 显式实现同一 API；Maven backend `1.0.16` 已发布并通过生成业务后端消费验证 | [Persistence README](../../mango/mango-infra/mango-infra-persistence/README.md#73-crud-service)、[后端 API 规范](../../mango-pmo/rules/backend/03-api.md) | [根 CHANGELOG](../../CHANGELOG.md)；`1.0.15` 不兼容新 manifest，业务项目升级到 `1.0.16` |
| 2026-07-13 | PMO 可复现 bundle：已发布的 `@mango/pmo@1.1.1` 携带规范、合同、模板、Agent、项目 Skill 和 Codex plugin 投影，增加单 Owner/多人维护分支保护、212 模块架构债务递减、大型 Git 基线读取和发布恢复边界；`@mango/cli@1.0.69` 提供项目锁、显式升级、本地回滚和修复后的发布状态机，Maven backend 锁定 `1.0.16` | [PMO package README](../../mango-ui/packages/mango-pmo/README.md)、[架构债务 README](../../mango-pmo/baselines/architecture/README.md)、[CLI README](../../mango-ui/packages/mango-cli/README.md) | [模块债务设计](../designs/2026-07-13-module-architecture-debt-governance-design.md)、[根 CHANGELOG](../../CHANGELOG.md)、[发布证据](../evidence/governance/release-v2026.07.13-pmo-1.1.1-cli-1.0.69-release.json) |
| 2026-07-11 | Excel 导入默认实现：title/idx 字段映射、字典和自定义 Converter、分层校验、事务模式、失败工作簿与 classpath 模板 | [Persistence README](../../mango/mango-infra/mango-infra-persistence/README.md) | [Issue #431 设计](../designs/2026-07-11-issue-431-excel-import-design.md)、[实施计划](../plans/2026-07-11-issue-431-excel-import-plan.md) |
| 2026-07-08 | 投产升级 SQL 约定目录：未显式配置模块 `locations` 时，Persistence 自动追加 `${MANGO_HOME:-/opt/mango}/upgrade/<module>`，执行记录仍进入模块 Flyway history | [Persistence README](../../mango/mango-infra/mango-infra-persistence/README.md) | [Issue #184 设计](../designs/2026-07-01-issue-184-data-governance-design.md) |
| 2026-07-04 | 首页管理返工：修复 `@mango/home` 集成，模板发布后可继续编辑未发布草稿，首页列表支持用户选择、所见即所得编辑及批量删除 | [Home README](../../mango/mango-platform/mango-home/README.md)、[Home Frontend README](../../mango-ui/packages/home/README.md)、[Admin Shell README](../../mango-ui/packages/admin-shell/README.md) | GitHub Issue #372 |
| 2026-07-04 | 通知中心站内信动作协议：业务消息可声明业务对象、隐藏参数、命名目标和动作按钮，支持 `ROUTE`、`FLOW`、`EVENT` 三类交互及命令型动作幂等回写 | [Notice README](../../mango/mango-platform/mango-notice/README.md)、[Notice Frontend README](../../mango-ui/packages/notice/README.md) | GitHub Issue #387 |
| 2026-07-03 | 首页管理：平台级首页模板、草稿复制、发布生效、个人/部门/角色授权、部门继承和用户最终首页视图 | [Home README](../../mango/mango-platform/mango-home/README.md)、[Home Frontend README](../../mango-ui/packages/home/README.md)、[Admin Shell README](../../mango-ui/packages/admin-shell/README.md) | GitHub Issue #372 |
| 2026-07-02 | 用户多首页工作台：个人首页列表、默认首页、带 `homeId` 参数的指定首页、布局 JSON 持久化 | [Home README](../../mango/mango-platform/mango-home/README.md)、[Home Frontend README](../../mango-ui/packages/home/README.md)、[Admin Shell README](../../mango-ui/packages/admin-shell/README.md) | GitHub Issue #368 |
| 2026-07-01 | 数据治理第一版：Resource demo 隔离、`INIT_ONLY`、Flyway 外部 locations、schema baseline pack | [Resource README](../../mango/mango-platform/mango-resource/README.md)、[Persistence README](../../mango/mango-infra/mango-infra-persistence/README.md) | [Issue #184 设计](../designs/2026-07-01-issue-184-data-governance-design.md)、[S5 清单](../plans/2026-07-01-issue-184-s5-data-material-audit.md) |
| 2026-06-29 | File 支持按目录结构清单打包多个文件为 ZIP，生成后写回存储层并返回新的 `FileRecordVO` | [File README](../../mango/mango-platform/mango-file/README.md) | [计划](../plans/2026-06-29-issue-316-file-package-plan.md)、[详细设计](../designs/2026-06-29-issue-316-file-package-design.md)、[交付台账](../plans/2026-06-29-issue-316-file-package-ledger.md) |

## 4. 后端平台能力

| 能力 | 模块 | README | 排障入口 |
|------|------|--------|----------|
| 访问控制 Access | `mango/mango-platform/mango-access` | [README](../../mango/mango-platform/mango-access/README.md) | [README](../../mango/mango-platform/mango-access/README.md) |
| 认证 Auth | `mango/mango-platform/mango-auth` | [README](../../mango/mango-platform/mango-auth/README.md) | [README](../../mango/mango-platform/mango-auth/README.md) |
| 授权 Authorization | `mango/mango-platform/mango-authorization` | [README](../../mango/mango-platform/mango-authorization/README.md) | [README](../../mango/mango-platform/mango-authorization/README.md) |
| 日历 Calendar | `mango/mango-platform/mango-calendar` | [README](../../mango/mango-platform/mango-calendar/README.md) | [README](../../mango/mango-platform/mango-calendar/README.md) |
| 验证码 Captcha | `mango/mango-platform/mango-captcha` | [README](../../mango/mango-platform/mango-captcha/README.md) | [README](../../mango/mango-platform/mango-captcha/README.md) |
| 业务域 Domain | `mango/mango-platform/mango-domain` | [README](../../mango/mango-platform/mango-domain/README.md) | [README](../../mango/mango-platform/mango-domain/README.md) |
| 文件 File | `mango/mango-platform/mango-file` | [README](../../mango/mango-platform/mango-file/README.md) | [README](../../mango/mango-platform/mango-file/README.md) |
| 文件预览 File Preview | `mango/mango-platform/mango-file-preview` | [README](../../mango/mango-platform/mango-file-preview/README.md) | [README](../../mango/mango-platform/mango-file-preview/README.md) |
| 自定义栅格布局 Grid Layout | `mango/mango-platform/mango-grid-layout` | [README](../../mango/mango-platform/mango-grid-layout/README.md) | [README](../../mango/mango-platform/mango-grid-layout/README.md) |
| 用户首页工作台 Home | `mango/mango-platform/mango-home` | [README](../../mango/mango-platform/mango-home/README.md) | [README](../../mango/mango-platform/mango-home/README.md) |
| 身份 Identity | `mango/mango-platform/mango-identity` | [README](../../mango/mango-platform/mango-identity/README.md) | [README](../../mango/mango-platform/mango-identity/README.md) |
| 任务调度 Job | `mango/mango-platform/mango-job` | [README](../../mango/mango-platform/mango-job/README.md) | [README](../../mango/mango-platform/mango-job/README.md) |
| 网址导航 Link | `mango/mango-platform/mango-link` | [README](../../mango/mango-platform/mango-link/README.md) | [README](../../mango/mango-platform/mango-link/README.md) |
| 通知 Notice | `mango/mango-platform/mango-notice` | [README](../../mango/mango-platform/mango-notice/README.md) | [README](../../mango/mango-platform/mango-notice/README.md) |
| 编号生成 Numgen | `mango/mango-platform/mango-numgen` | [README](../../mango/mango-platform/mango-numgen/README.md) | [README](../../mango/mango-platform/mango-numgen/README.md) |
| 组织 Org | `mango/mango-platform/mango-org` | [README](../../mango/mango-platform/mango-org/README.md) | [README](../../mango/mango-platform/mango-org/README.md) |
| 支付 Payment | `mango/mango-platform/mango-payment` | [README](../../mango/mango-platform/mango-payment/README.md) | [README](../../mango/mango-platform/mango-payment/README.md) |
| 资源注册中心 Resource Registry | `mango/mango-platform/mango-resource` | [README](../../mango/mango-platform/mango-resource/README.md) | [README](../../mango/mango-platform/mango-resource/README.md) |
| 系统 System | `mango/mango-platform/mango-system` | [README](../../mango/mango-platform/mango-system/README.md) | [README](../../mango/mango-platform/mango-system/README.md) |
| 模板 Template | `mango/mango-platform/mango-template` | [README](../../mango/mango-platform/mango-template/README.md) | [README](../../mango/mango-platform/mango-template/README.md) |
| 工作流 Workflow | `mango/mango-platform/mango-workflow` | [README](../../mango/mango-platform/mango-workflow/README.md) | [README](../../mango/mango-platform/mango-workflow/README.md) |

## 5. 后端基础设施能力

| 能力 | 模块 | README | 排障入口 |
|------|------|--------|----------|
| 上下文 Context | `mango/mango-infra/mango-infra-context` | [README](../../mango/mango-infra/mango-infra-context/README.md) | [README](../../mango/mango-infra/mango-infra-context/README.md) |
| 加密 Crypto | `mango/mango-infra/mango-infra-crypto` | [README](../../mango/mango-infra/mango-infra-crypto/README.md) | [README](../../mango/mango-infra/mango-infra-crypto/README.md) |
| 文档 Doc | `mango/mango-infra/mango-infra-doc` | [README](../../mango/mango-infra/mango-infra-doc/README.md) | [README](../../mango/mango-infra/mango-infra-doc/README.md) |
| 事件 Event | `mango/mango-infra/mango-infra-event` | [README](../../mango/mango-infra/mango-infra-event/README.md) | [README](../../mango/mango-infra/mango-infra-event/README.md) |
| Feign | `mango/mango-infra/mango-infra-feign` | [README](../../mango/mango-infra/mango-infra-feign/README.md) | [README](../../mango/mango-infra/mango-infra-feign/README.md) |
| 文件处理 Fileproc | `mango/mango-infra/mango-infra-fileproc` | [README](../../mango/mango-infra/mango-infra-fileproc/README.md) | [README](../../mango/mango-infra/mango-infra-fileproc/README.md) |
| Aspose License | `mango-infra-fileproc/resources/aspose` | [README](../../mango/mango-infra/mango-infra-fileproc/mango-infra-fileproc-core/src/main/resources/aspose/README.md) | [README](../../mango/mango-infra/mango-infra-fileproc/mango-infra-fileproc-core/src/main/resources/aspose/README.md) |
| IP 归属地 | `mango/mango-infra/mango-infra-ip-location` | [README](../../mango/mango-infra/mango-infra-ip-location/README.md) | [README](../../mango/mango-infra/mango-infra-ip-location/README.md) |
| KV | `mango/mango-infra/mango-infra-kv` | [README](../../mango/mango-infra/mango-infra-kv/README.md) | [README](../../mango/mango-infra/mango-infra-kv/README.md) |
| 日志 Log | `mango/mango-infra/mango-infra-log` | [README](../../mango/mango-infra/mango-infra-log/README.md) | [README](../../mango/mango-infra/mango-infra-log/README.md) |
| 模块服务 Module | `mango/mango-infra/mango-infra-module` | [README](../../mango/mango-infra/mango-infra-module/README.md) | [README](../../mango/mango-infra/mango-infra-module/README.md) |
| 持久化 Persistence | `mango/mango-infra/mango-infra-persistence` | [README](../../mango/mango-infra/mango-infra-persistence/README.md) | [README](../../mango/mango-infra/mango-infra-persistence/README.md) |
| 实时 Realtime | `mango/mango-infra/mango-infra-realtime` | [README](../../mango/mango-infra/mango-infra-realtime/README.md) | [README](../../mango/mango-infra/mango-infra-realtime/README.md) |
| 敏感数据 Sensitive | `mango/mango-infra/mango-infra-sensitive` | [README](../../mango/mango-infra/mango-infra-sensitive/README.md) | [README](../../mango/mango-infra/mango-infra-sensitive/README.md) |
| Infra Test | `mango/mango-infra/mango-infra-test` | [README](../../mango/mango-infra/mango-infra-test/README.md) | [README](../../mango/mango-infra/mango-infra-test/README.md) |
| Web | `mango/mango-infra/mango-infra-web` | [README](../../mango/mango-infra/mango-infra-web/README.md) | [README](../../mango/mango-infra/mango-infra-web/README.md) |

## 6. 前端与 CLI 能力

Mango 前端包默认服务管理后台。标记为 `Admin Shell` 或 `Admin Pages` 的包不适合作为官网、营销站、C 端门户的页面组件直接集成；这类站点只应评估 `通用能力`、`混合能力` 或 CLI，并单独确认样式、依赖、接口和权限边界。

| 能力 | 包 | 适用端 / 集成形态 | 官网类站点建议 | README | 排障入口 |
|------|----|-------------------|----------------|--------|----------|
| 单体管理端 | `@mango/admin` | Admin Shell，后台应用聚合入口 | 不使用 | [README](../../mango-ui/packages/admin/README.md) | [README](../../mango-ui/packages/admin/README.md) |
| 后台 Shell | `@mango/admin-shell` | Admin Shell，后台布局、菜单、路由、运行时和首页业务小组件自动注册 | 不使用，除非官网就是内部后台 | [README](../../mango-ui/packages/admin-shell/README.md) | [README](../../mango-ui/packages/admin-shell/README.md) |
| 页面注册表 | `@mango/admin-pages` | Admin Pages，后台页面注册和 component key 映射 | 不使用 | [README](../../mango-ui/packages/admin-pages/README.md) | [README](../../mango-ui/packages/admin-pages/README.md) |
| 认证前端 | `@mango/auth` | Admin Pages，后台登录、登录流程 hook、用户与认证页面 | 不直接复用官网登录页；自定义后台登录页可复用 `useMangoLoginFlow()` | [README](../../mango-ui/packages/auth/README.md) | [README](../../mango-ui/packages/auth/README.md) |
| 日历前端 | `@mango/calendar` | Admin Pages，后台日历管理页面 | 不直接复用整页 | [README](../../mango-ui/packages/calendar/README.md) | [README](../../mango-ui/packages/calendar/README.md) |
| 任务前端 | `@mango/job` | Admin Pages，后台任务管理页面 | 不使用 | [README](../../mango-ui/packages/job/README.md) | [README](../../mango-ui/packages/job/README.md) |
| 网址导航前端 | `@mango/link` | Admin Pages，后台网址导航和网址管理页面 | 不直接复用整页；门户导航使用 `@mango/link-page` | [README](../../mango-ui/packages/link/README.md) | [README](../../mango-ui/packages/link/README.md) |
| 网址导航 Open API | `@mango/link-openapi` | 通用能力，网址导航 API client | 可评估使用，需确认登录态和 `/api` 前缀 | [README](../../mango-ui/packages/link-openapi/README.md) | [README](../../mango-ui/packages/link-openapi/README.md) |
| 网址导航页面 | `@mango/link-page` | 通用页面，分组展示网址并支持个人操作 | 可评估使用，需确认 Element Plus、登录态和后端 `mango-link` | [README](../../mango-ui/packages/link-page/README.md) | [README](../../mango-ui/packages/link-page/README.md) |
| 通知前端 | `@mango/notice` | Admin Pages，后台通知管理页面 | 不直接复用整页 | [README](../../mango-ui/packages/notice/README.md) | [README](../../mango-ui/packages/notice/README.md) |
| 编号前端 | `@mango/numgen` | Admin Pages，后台编号规则管理页面 | 不使用 | [README](../../mango-ui/packages/numgen/README.md) | [README](../../mango-ui/packages/numgen/README.md) |
| 支付前端 | `@mango/payment` | Admin Pages，后台支付配置、订单和对账页面 | 不直接复用后台管理页；收银台另按业务评估 | [README](../../mango-ui/packages/payment/README.md) | [README](../../mango-ui/packages/payment/README.md) |
| RBAC API | `@mango/rbac` | Admin Pages/API，后台菜单、权限和页面注册辅助 | 不使用后台页面；API 封装需按权限模型评估 | [README](../../mango-ui/packages/rbac/README.md) | [README](../../mango-ui/packages/rbac/README.md) |
| 系统前端 | `@mango/system` | Admin Pages，后台系统配置页面与组件 | 不直接复用整页 | [README](../../mango-ui/packages/system/README.md) | [README](../../mango-ui/packages/system/README.md) |
| 模板前端 | `@mango/template` | Admin Pages，后台模板管理页面 | 不使用 | [README](../../mango-ui/packages/template/README.md) | [README](../../mango-ui/packages/template/README.md) |
| 工作流前端 | `@mango/workflow` | Admin Pages，后台流程设计、审批和运行页面 | 不直接复用整页；表单/流程组件需单独评估 | [README](../../mango-ui/packages/workflow/README.md) | [README](../../mango-ui/packages/workflow/README.md) |
| 工作流示例 | `@mango/workflow-business-example` | Example，后台业务审批示例 | 不作为生产站点依赖 | [README](../../mango-ui/packages/workflow-business-example/README.md) | [README](../../mango-ui/packages/workflow-business-example/README.md) |
| API Schema | `@mango/api-schema` | 通用能力，接口类型和 schema | 可评估使用 | [README](../../mango-ui/packages/api-schema/README.md) | [README](../../mango-ui/packages/api-schema/README.md) |
| 应用运行时 | `@mango/app-runtime` | 通用/运行时能力，应用装配基础 | 可评估使用，但需确认是否绑定后台运行模型 | [README](../../mango-ui/packages/app-runtime/README.md) | [README](../../mango-ui/packages/app-runtime/README.md) |
| 公共组件 | `@mango/common` | 通用能力，请求、消息、选择器、编辑器等 | 可评估使用，需核对 Element Plus、主题和后台依赖 | [README](../../mango-ui/packages/common/README.md) | [README](../../mango-ui/packages/common/README.md) |
| 文件前端 | `@mango/file` | 混合能力，包含后台页面和上传/预览组件 | 只评估组件级能力，不直接复用后台页面 | [README](../../mango-ui/packages/file/README.md) | [README](../../mango-ui/packages/file/README.md) |
| 用户首页 API | `@mango/home` | Admin Shell/API，用户多首页、模板管理、授权、默认首页和布局持久化接口封装 | 不直接复用为官网页面；API 封装需配合后端 `mango-home` 和登录态 | [README](../../mango-ui/packages/home/README.md) | [README](../../mango-ui/packages/home/README.md) |
| 自定义栅格布局前端 | `@mango/grid-layout` | 通用能力，自定义栅格展示与编辑器，支持失效组件查看态隐藏和编辑态清理 | 可评估使用，需确认 Element Plus、主题和个人布局接口边界 | [README](../../mango-ui/packages/grid-layout/README.md) | [README](../../mango-ui/packages/grid-layout/README.md) |
| 栅格系统小组件 | `@mango/grid-widgets` | 通用能力，系统小组件集合、网址导航、日历、用户信息、快捷入口、消息中心与业务小组件注册聚合 | 可评估使用，需确认运行时用户、菜单、跳转适配和小组件数据权限边界 | [README](../../mango-ui/packages/grid-widgets/README.md) | [README](../../mango-ui/packages/grid-widgets/README.md) |
| PMO bundle | `@mango/pmo` | 开发治理，版本化 baseline、文档合同、专用 Agent、项目 Skill 和 Codex plugin 发布物料 | 可用于业务仓治理，不是运行时组件 | [README](../../mango-ui/packages/mango-pmo/README.md) | [README](../../mango-ui/packages/mango-pmo/README.md) |
| CLI | `@mango/cli` | 开发工具，项目生成、模块追加和 PMO baseline 同步 | 可用于生成项目，不是运行时组件 | [README](../../mango-ui/packages/mango-cli/README.md) | [README](../../mango-ui/packages/mango-cli/README.md) |

## 7. 后端装配与工具

| 能力 | 模块 | README | 排障入口 |
|------|------|--------|----------|
| 后端聚合 Starter | `mango/mango-admin-starter` | [README](../../mango/mango-admin-starter/README.md) | [README](../../mango/mango-admin-starter/README.md) |
| 应用拓扑 | `mango/mango-app` | [README](../../mango/mango-app/README.md) | [README](../../mango/mango-app/README.md) |
| 后端公共契约 | `mango/mango-common` | [README](../../mango/mango-common/README.md) | [README](../../mango/mango-common/README.md) |
| 可选扩展 | `mango/mango-extension` | [README](../../mango/mango-extension/README.md) | [README](../../mango/mango-extension/README.md) |
| Maven Parent | `mango/mango-parent` | [README](../../mango/mango-parent/README.md) | [README](../../mango/mango-parent/README.md) |
| 构建工具 | `mango/mango-tools` | [README](../../mango/mango-tools/README.md) | [README](../../mango/mango-tools/README.md) |

## 8. 业务项目与 PMO 基线

| 能力 | 入口 | README | 排障入口 |
|------|------|--------|----------|
| Business Starter | `mango-business-starter` | [README](../../mango-business-starter/README.md) | [README](../../mango-business-starter/README.md) |
| Business PMO | `mango-business-starter/business-pmo` | [README](../../mango-business-starter/business-pmo/README.md) | [README](../../mango-business-starter/business-pmo/README.md) |
| Baseline | `mango-business-starter/business-pmo/mango-baseline` | [README](../../mango-business-starter/business-pmo/mango-baseline/README.md) | [README](../../mango-business-starter/business-pmo/mango-baseline/README.md) |
| 单体拓扑 | `mango-business-starter/topologies/monolith` | [README](../../mango-business-starter/topologies/monolith/README.md) | [README](../../mango-business-starter/topologies/monolith/README.md) |
| 微服务拓扑 | `mango-business-starter/topologies/microservice` | [README](../../mango-business-starter/topologies/microservice/README.md) | [README](../../mango-business-starter/topologies/microservice/README.md) |

## 9. 维护入口

能力说明维护规则见 [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)。

模块 README 模板见 [module-readme.md](../../mango-pmo/templates/module-readme.md)。
