---
documentId: TDD-ISSUE-690-WEEKLY-REGRESSION
documentType: technical-design
pmoVersion: 1.3.8
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: requirement=L3，Issue #690 的 13 个 P0 与 7 个 P1 阻断标准生成项目启动、升级、模块追加、生产制品、空库登录重启、真实 BSQL 消费和可信浏览器验收；solution=L3，修复横跨 CLI、生成模板、pnpm workspace、Maven 架构插件、Bootstrap 生命周期、Resource、认证边界、Persistence/BSQL 和浏览器 E2E，并需要一次性真实 MySQL 消费链；final=max(requirement,solution)
status: APPROVED
action: NEXT
owner: Mango Development Agent
approver: HardyDou
approvalEvidence: mango-docs/designs/issue-690-weekly-regression/review/APPROVAL.md
upstreamDocumentId: NONE
upstreamDocumentHash: NONE
---

# Issue #690 周回归投产阻断修复技术设计文档

## 1. 设计输入、约束与决策

本设计以 [GitHub Issue #690](https://github.com/HardyDou/mango/issues/690)、[2026-08-02 追加回归记录](https://github.com/HardyDou/mango/issues/690#issuecomment-5154203321) 及其 [第二次补充探索](https://github.com/HardyDou/mango/issues/690#issuecomment-5154523050) 为唯一需求源，基线为 `main@5b4f51092`。20 项要求规范化为：FR-001 标准 dev start、FR-002 verify/install 架构一致性、FR-003 flatten 与独立消费、FR-004 KV locker、FR-005 Bootstrap 隔离、FR-006 release tuple、FR-007 空库 Resource/菜单、FR-008 真实 monolith BSQL、FR-009 cold baseline 性能门禁、FR-010 Boot packaging Invoker、FR-011 自包含 E2E、FR-012 module add lockfile/格式事务、FR-013 module add 后 install/dev start、FR-014 可执行 Boot JAR、FR-015 空库最低可登录数据与 PUBLIC API、FR-016 stable generation 重启、FR-017 generated page 完整工程门禁、FR-018 API fallback 仅拦截真实 `/api/` pathname、FR-019 Notice 结构化通知与 `total <= 10`/`total > 10` 浏览器合同、FR-020 Playwright reporter/artifacts 位于 Vite root 外；SAC-001 至 SAC-020 与上述顺序一一对应。任务采用 L3/FULL、M01=`CREATE`、M08=`ENABLE`、M09 至 M14=`ENABLE`。Issue #687 与 #688 已由其他人接手，明确不在本任务范围；发布、提交、推送、PR、Issue 更新或关闭也不属于当前授权。

| 决策ID | 问题 | 候选方案 | 选择 | 理由 | 来源ID或路径 | 是否推断 | 影响 | 风险 | 回退条件 |
|---|---|---|---|---|---|---|---|---|---|
| DEC-001 | 11 个故障如何组织 | 各点绕过；整链重写；统一契约并分批验收 | 统一进程、版本、资源、schema 与 fixture 契约，按四批验收 | 避免 verify/install、bootstrap/runtime、reactor/consumer 双重语义，同时控制重写范围 | FR-001 至 FR-011 | 否 | 全任务 | 跨模块失败互相掩盖 | 每批独立证明，不能通过时回修契约而非跳过门禁 |
| DEC-002 | Spring Boot mode 如何传递 | app.args 顺序；CLI 隐式前插；manifest 显式字段 | 显式 `processMode`，dev 最终首参固定 `runtime` | 生成模板和兼容解析共享唯一合同 | FR-001 | 否 | CLI 与模板 | 旧项目参数冲突 | 缺字段默认 runtime；重复或冲突 fail-closed |
| DEC-003 | 生成 Maven 投产合同 | 只补 install；删除 `${revision}`；统一 inventory 并 flatten | 保留 CI-friendly version，修正 inventory，加入 flatten 与 reactor 外 consumer | 问题发生在 install 和独立消费，Issue 明确不得删除 revision | FR-002、FR-003 | 否 | CLI POM 与 Maven plugin | qualifier 漂移 | installed POM 或 consumer 失败即阻断 |
| DEC-004 | 升级如何同步版本 | 分文件更新；只更 Maven；release manifest 原子投影 | 当前 CLI 随包 manifest 生成 Maven/CLI/PMO/npm tuple plan | 一个可信源、dry-run 可见、可恢复 | FR-006 | 否 | `pmo upgrade` | 部分写入 | 预检失败不写；写失败恢复全部原文件 |
| DEC-005 | Bootstrap 如何隔离 worker | 禁用 starter；只移除 runner；统一 runtime condition | Job、Notice、Payment 等 worker 仅 runtime，Bootstrap handler/service 保留 | event listener/scheduler 不能靠 runner removal 隔离 | FR-005 | 否 | 自动配置与写集 | 漏关 worker或误关 handler | bean inventory 或 MySQL 写集失败即阻断 |
| DEC-006 | 业务 Resource 如何进入 Bootstrap | 旧 runner；旧 manifest 特判；typed declaration | 生成 `META-INF/mango/resources/` ResourceDeclaration | 与平台资源共用 plan/apply/verify 生命周期 | FR-007 | 否 | module template 与 Registry | identity 不稳 | 声明冲突或空库计数不符即阻断，不启用旧 runner |
| DEC-007 | BSQL 如何比较 schema | 删 DDL 片段；正则扩展；结构化语义 snapshot | table/column/index/constraint 结构化，view/trigger 受控 canonical | MySQL 会规范化 charset、collation 与 default，字符串相等不代表语义 | FR-008 | 否 | baseline plugin | metadata 不完整导致假相等 | 未知对象或差异均 fail-closed 并输出对象路径 |
| DEC-008 | E2E 如何适配空库 | 固定 seed；只改中文名；自建 fixture 与 stable code | API 自建/清理 tenant/domain，按 code 或 data 属性定位，共享 admin 状态串行 | 展示文案和预置数据不是稳定合同 | FR-011 | 否 | Playwright 与必要 UI anchor | 清理污染 | setup/cleanup 失败中止并保留精确 handle |
| DEC-009 | module add 如何保持项目可立即验证 | 只写源码；提示人工 install/format；受管文件事务 | 在任何写入前校验显式模块/聚合中文显示名，再预渲染全部产物、运行 Prettier、同步 workspace lockfile，并把源码/集成文件/lockfile 纳入同一补偿边界 | 非中文显示名立即失败且项目零变更；命令成功后 CI 的 frozen install 与完整前端门禁必须立即成立 | FR-012、FR-017 | 否 | CLI、pnpm workspace、业务页面模板 | 无效显示名或包管理/格式化中断留下半模块 | 输入预检失败零写入；后续任一步失败恢复命令前文件与 lockfile，长短命名 packed fixture 均验证 |
| DEC-010 | 生成 app 如何形成生产制品 | 只支持 spring-boot:run；手工 repackage；模板绑定 repackage | 在 app POM 显式绑定 Spring Boot repackage，生产门禁独立 `java -jar` 启动 | package 成功必须等价于可部署 Boot JAR | FR-014 | 否 | full backend app POM、generated gate | 普通 jar 假成功 | 缺 Main-Class/Start-Class/BOOT-INF、health/Flyway/resource 任一失败即阻断 |
| DEC-011 | runtime 如何获得 stable generation | 默认 0；人工 env；读取已验证 receipt/control 状态 | workspace init/dev start 从 Bootstrap receipt/control manifest 解析 generation、revision、fingerprint并显式传递 | 完成 Bootstrap 后默认 0 必然陈旧，重启必须复用稳定代际 | FR-016 | 否 | CLI workspace、Bootstrap control plane | 读取错库或旧 receipt | 缺失/歧义/不匹配 fail-closed，不回退 0 |
| DEC-012 | 空库登录如何定义最低可用 | health 即通过；手工 seed；Bootstrap typed declarations + public auth contract | apply 初始化最低租户/管理员/角色/授权和平台/业务资源，并让 PUBLIC 注解与匿名过滤链一致 | 登录页依赖公开 branding/login-options，菜单依赖真实身份与资源 | FR-015 | 否 | Bootstrap、Identity/System/Auth/Resource、E2E | 假健康或越权匿名 | 数据计数、401 负例边界、真实登录/菜单/CRUD 联合验收 |
| DEC-013 | E2E API fallback 如何避免吞掉 Vite 源码 | glob 包含匹配；逐文件排除；按 URL pathname 分类 | 仅当 pathname 以 `/api/` 开头时 fulfill，并记录实际 `.ts/.vue` API 模块请求作为负面证据 | Vite `/@fs/.../api/*.ts` 与 `/src/api/*.ts` 是源码而非后端 API，硬编码例外会继续漏新路径 | FR-018 | 否 | Admin Playwright fixtures | 源码被 JSON fulfill 造成白屏 | 任一源码模块进入 fallback 立即失败，不降级为路径白名单 |
| DEC-014 | Notice 浏览器合同如何跟随结构化展示 | 保留旧 title 断言；只测组件；浏览器覆盖当前协议 | E2E 按消息类型/内容/时间/点击查看断言通知与详情，同时覆盖 `total <= 10` 列表、`total > 10` 分类和业务跳转 | 组件单测不能证明 admin shell 路由、popover 与真实浏览器整合，旧 title 已不是展示标题 | FR-019 | 否 | Notice client、admin shell、E2E fixture | API fixture 漂移导致假空态 | unread-category-stats、channel route tags、reference impact 与敏感字段拆分均按当前公开合同 mock |
| DEC-015 | Playwright 产物如何避免触发 Vite reload | 默认 app 内目录；Vite ignore；worktree runtime 根 | reporter、outputDir 和定向截图统一写入 worktree `.runtime/playwright/mango-admin` | 从源头隔离所有默认产物，CI 与 external-webserver 行为一致且不依赖 Vite watch 配置 | FR-020 | 否 | Playwright config、E2E artifacts | HTML 报告写入 Vite root 制造假 reload | 发现 app/Vite root 内新增报告或默认 artifact 即阻断 |

## 2. 模块与依赖边界

| 模块设计ID | 模块或包 | 职责 | 改动类型 | 依赖方向 | 公开能力 | 系统需求ID | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|
| MOD-001 | `mango-ui/packages/mango-cli` | processMode、release tuple、module add 事务、full template、generation 解析、packed 生成入口 | 扩展/修复 | CLI 读取随包 manifest/Bootstrap receipt；生成物不反向依赖源码 | dev start、pmo upgrade、module add、full template | FR-001、FR-003、FR-004、FR-006、FR-007、FR-012、FR-013、FR-014、FR-016、FR-017 | `rules/03-ai-coding-redlines.md` | Node、packed CLI、frozen install、generated smoke |
| MOD-002 | `mango-tools/mango-maven-plugin` | reactor inventory、schema snapshot、Invoker packaging | 修复/重构 | 只依赖 Maven reactor 与 MySQL metadata | Maven goals 和报告 | FR-002、FR-008、FR-010 | `rules/backend/04-db.md`、`rules/backend/07-persistence.md` | JUnit、Invoker、monolith golden |
| MOD-003 | Bootstrap 与 runtime provider starters | mode 条件、plan/apply/verify 隔离、stable generation receipt | 修复 | worker 依赖 runtime condition；handler 保持装配；runtime 只接受稳定代际 | Bootstrap 生命周期 | FR-005、FR-016 | `rules/backend/07-persistence.md` | context、真实 MySQL 写集与两次重启 |
| MOD-004 | Resource Registry、认证公共边界与生成业务模板 | typed declaration、Bootstrap 最低数据和 PUBLIC 登录前接口 | 迁移/修复 | template 依赖 Registry schema；匿名过滤链消费 API access contract | 菜单/API resource 声明、登录前公共查询 | FR-007、FR-015 | `rules/backend/11-module-menu.md` | schema、空库计数、PUBLIC API、登录菜单 |
| MOD-005 | Persistence cold baseline tests | 生产 mode、B1 history、状态、行数和性能断言 | 修复 | 调用真实 importer 与 MySQL | 性能门禁 | FR-009 | `rules/backend/08-test.md` | 定向 integration/performance |
| MOD-006 | Admin E2E | fixture、stable selector、shared state isolation | 修复 | 只依赖公开 API 与 UI | Chromium regression | FR-011 | `rules/frontend/04-test.md` | 定向串行 Playwright |
| MOD-007 | Admin Notice E2E 与 Playwright config | pathname 路由隔离、结构化通知阈值/跳转、渠道 fixture、外置 reporter/artifacts | 修复 | E2E 只消费 Notice/Auth/System 公开 API；产物只写 worktree runtime 根 | Notice Chromium regression 与无干扰报告 | FR-018、FR-019、FR-020 | `rules/frontend/04-test.md` | ESLint/Prettier、Notice 单测、Playwright list、Chromium specs、Vite 日志 |

## 3. 技术对象与状态模型

| 模型ID | 上游ID | 模型职责 | 标识 | 关系 | 状态编码 | 审计或历史 | 归属或租户 | 一致性约束 |
|---|---|---|---|---|---|---|---|---|
| DM-001 | FR-001 | Spring Boot process contract | app key | app 对应一个 processMode | bootstrap/runtime | CLI 命令摘要 | workspace | 最终业务首参恰好一个 mode，dev 必为 runtime |
| DM-002 | FR-006 | release tuple plan | CLI package version | Maven、CLI、PMO、受管 npm 投影 | PLANNED/APPLIED/ROLLED_BACK/FAILED | dry-run 与 diff | workspace | 单 manifest、不可部分成功 |
| DM-003 | FR-005 | Bootstrap process boundary | mode + command | mode 决定 bean 集与允许写集 | plan/apply/verify/runtime | step journal | 专用数据库 | plan/verify 无业务写，apply 仅 step allowlist |
| DM-004 | FR-007 | generated ResourceDeclaration | resource id + version | bizKey、targetModule、type、fields | DECLARED/APPLIED/VERIFIED | Registry source/version | 声明租户 | identity 稳定且可追踪到模块 |
| DM-005 | FR-008 | schema semantic snapshot | object type + name | tables、columns、indexes、constraints、canonical definitions | EQUIVALENT/DIFFERENT/UNSUPPORTED | 对象级 diff | 临时 schema | 排除物理噪声但不丢语义，unknown 不得相等 |
| DM-006 | FR-012、FR-017 | module add project transaction | module code + project root | generated files、managed integrations、workspace lockfile 同属一次变更 | PLANNED/APPLIED/ROLLED_BACK/FAILED | 文件 hash 与命令日志 | workspace | 成功后 frozen install 和完整前端门禁立即通过，失败恢复原字节 |
| DM-007 | FR-016 | stable runtime release identity | generation + revision + fingerprint | receipt/control manifest 对应专用 DB 和 runtime app | BOOTSTRAPPED/STABLE/RUNNING/STALE | Bootstrap receipt 与 runtime state | workspace/database | generation>0 且三元组一致；缺失或冲突不启动 |
| DM-008 | FR-018 | E2E request classification | origin + pathname + resourceType | backend `/api/` 与 Vite source module 互斥 | BACKEND_API/SOURCE_MODULE/PASSTHROUGH | fallback/source request evidence | browser context | 只有 BACKEND_API 可由 JSON fallback fulfill |
| DM-009 | FR-019 | Notice bell presentation state | unread total + category | total 决定 list/category，message 决定 structured detail/action | LIST/GROUPED/DETAIL/NAVIGATED | API request 与 browser trace | current user/tenant | `total <= 10` 为列表，`total > 10` 为非空分类，动作保留业务参数 |
| DM-010 | FR-020 | Playwright artifact location | absolute runtime path | report、trace、screenshot 属于同一外置根 | CONFIGURED/WRITING/FINALIZED | reporter output 与 Vite log | worktree | artifact path 不得位于 `mango-ui` Vite root 内 |

| 模型ID | 当前状态 | 触发 | 目标状态 | 前置条件 | 副作用 | 失败处理 | 上游ID |
|---|---|---|---|---|---|---|---|
| DM-002 | PLANNED | 全部受管字段预检通过 | APPLIED | manifest 完整且目标唯一 | 原子更新版本 | 任一失败恢复原内容并进入 ROLLED_BACK | FR-006 |
| DM-003 | bootstrap plan | 声明解析完成 | plan complete | 不访问未创建业务表 | 只输出计划 | 发现查询或写入立即失败 | FR-005 |
| DM-003 | bootstrap apply | 声明步骤全部成功 | finalized | 写集属于 allowlist | 初始化 schema/resource/基础数据 | 既有 journal 记录失败且不启动 worker | FR-005、FR-007 |
| DM-003 | bootstrap verify | 验证器只读通过 | verified | 已存在目标状态 | 无业务写 | 发现写集或 worker 注册立即失败 | FR-005 |
| DM-005 | EQUIVALENT | 对象集合或语义属性不同 | DIFFERENT | 两侧 metadata 完整 | 生成对象级 diff | 阻止 baseline 产出并保留安全报告 | FR-008 |
| DM-006 | PLANNED | 生成、格式化、lockfile 同步与预检全部通过 | APPLIED | pnpm 可解析且目标路径无冲突 | 写入完整业务模块 | 任一步失败按 snapshot 恢复并进入 ROLLED_BACK | FR-012、FR-017 |
| DM-007 | STABLE | dev start 读取匹配 receipt/control identity | RUNNING | generation 为正且 revision/fingerprint 匹配 | 传递显式 runtime identity | stale/missing/ambiguous 时启动前失败，不默认 0 | FR-016 |
| DM-008 | PASSTHROUGH | pathname 以 `/api/` 开头 | BACKEND_API | URL 分类完成 | 允许 fixture fulfill | `.ts/.vue` source module 被 fulfill 时测试失败 | FR-018 |
| DM-009 | LIST | unread total 变为大于 10 | GROUPED | category stats 可用 | 清空列表并展示非零分类 | stats 缺失/失败时显示受控空态并使浏览器合同失败 | FR-019 |
| DM-010 | CONFIGURED | Playwright 开始执行 | WRITING | output/report 路径位于 worktree runtime 根 | 写 artifacts/report | 产物进入 Vite root 时配置合同失败 | FR-020 |

## 4. 系统流程、事务与一致性

| 流程设计ID | 系统需求ID | 调用入口 | 参与模块 | 处理顺序 | 事务边界 | 状态变化 | 幂等键 | 并发策略 | 外部失败与补偿 | 用户可见结果 |
|---|---|---|---|---|---|---|---|---|---|---|
| FLOW-001 | FR-001、FR-004 | packed dev start | MOD-001 | parse→validate mode→prepend runtime→append args→start→health | 无跨进程事务 | stopped→healthy | workspace+app | app process lock | 参数冲突启动前失败；context 失败停止子进程 | 无手工绕过即可 healthy |
| FLOW-002 | FR-002、FR-003 | generated verify/install/consumer | MOD-001、MOD-002 | generate→verify→install→report→installed POM→consumer | Maven lifecycle | pending→accepted | artifact coordinate | 独立 local repo | 任一步失败保留 `.runtime` 报告并阻断 | verify/install 一致且独立消费成功 |
| FLOW-003 | FR-006 | pmo upgrade/dry-run | MOD-001 | manifest→scan→plan→precheck→atomic writes→reread | 全计划补偿边界 | DM-002 转换 | project+tuple | workspace lock | 失败恢复全部受管文件 | dry-run 可见且 apply 后 tuple 一致 |
| FLOW-004 | FR-005 | bootstrap plan/apply/verify | MOD-003 | set mode before refresh→conditional beans→command→capture writes→close | step 既有事务 | DM-003 转换 | run/step | Bootstrap lock | 越界 bean/query/write fail-closed | 可证明只读或 allowlist 写入 |
| FLOW-005 | FR-007 | full empty-db bootstrap | MOD-001、MOD-003、MOD-004 | scan declarations→plan→apply→verify→runtime→login→menu | handler/step 事务 | DM-004→VERIFIED | resource id+version | Registry lease | 冲突或基础实体为空停止 | tenant/user/role/menu/API/business resource 非零且菜单可用 |
| FLOW-006 | FR-008、FR-009 | baseline-generate/performance | MOD-002、MOD-005 | V migrations→snapshot→B1 rebuild→snapshot→diff→history/state/rows/time | 临时数据库 | pending→verified | module+checksum | 专用 schema | 差异输出 object path；失败清理临时库 | monolith BSQL 可生成和消费 |
| FLOW-007 | FR-010 | Maven Invoker Boot IT | MOD-002 | build→repackage→list entries→root manifest assertion→missing negative | IT project | unverified→verified/failed | coordinates | case isolation | hook/entry/负例任一异常导致 Invoker 失败 | 明确证明 packaging 合同 |
| FLOW-008 | FR-011 | Playwright Chromium | MOD-006 | login→create tenant/domain→locate by code→flows→assert→cleanup | API 命令自身事务 | fixture creating→ready→removed | run prefix | shared admin suites serial | 部分创建按 handle 逆序清理 | 空库菜单、租户、Workflow 稳定通过 |
| FLOW-009 | FR-012、FR-017 | packed module add | MOD-001 | validate Chinese display names→plan files→render→Prettier→stage managed integrations→pnpm install/lockfile-only→frozen install/check→commit transaction | 输入预检零写入；其后为 workspace 文件补偿事务 | DM-006 转换 | project+module | project command lock | 显示名无中文立即失败；formatter/pnpm/check 任一失败恢复所有目标 | 无效输入不污染项目；成功后 CI 可直接 frozen install 与完整 check |
| FLOW-010 | FR-014 | generated production package | MOD-001、MOD-002 | clean package→repackage→entry inspection→external java -jar→health→Flyway/resource assertion→stop | Maven artifact lifecycle | plain jar→boot jar→verified | app coordinates | isolated process/ports | entry/启动/业务状态失败保留安全日志并阻断 | 生产 JAR 可独立运行 |
| FLOW-011 | FR-015、FR-016 | cold apply 后 runtime restart | MOD-001、MOD-003、MOD-004 | apply→read stable identity→start→public APIs→login→stop→reread identity→start→menu/CRUD→rolling generation | Bootstrap control + process state | STABLE↔RUNNING | workspace+generation | app process lock + release generation | stale/default 0、401 或基础数据空均失败 | 空库可登录且可重复重启 |
| FLOW-012 | FR-018 | Notice announcement mock routing | MOD-007 | observe request→classify pathname→specific route 或 `/api/` fallback→source passthrough→page visible | browser route lifecycle | DM-008 转换 | request URL | Playwright route ordering | source fulfill/MIME error/空 `#app` 立即失败 | 真实 observed API source modules 不进入 fallback |
| FLOW-013 | FR-019 | structured Notice browser flow | MOD-007 | login→realtime event→structured alert→detail→action route→bell list→bell grouped→category route→message action/detail | UI/API state | DM-009 转换 | message/category/action | current session | fixture 缺接口、旧 title 或跳转参数丢失失败 | 结构化展示和两阈值在 Chromium 通过 |
| FLOW-014 | FR-020 | Playwright report/artifact flow | MOD-007 | resolve worktree runtime root→write artifacts/report→finish→inspect Vite log | filesystem artifact lifecycle | DM-010 转换 | run/project | per-run outputDir | Vite root 内 report/reload 即失败 | local/CI/external server 均无 report reload |

## 5. API 与远程契约设计

不新增业务 HTTP API；这里固定真实消费链和 E2E 使用的既有服务入口，CLI/manifest 行为由 FLOW-001 至 FLOW-003 描述。

| 接口ID | 系统需求ID | 调用方 | 所属模块 | 入口类型 | 方法与路径 | Command Query或VO | 返回契约 | 校验 | 权限租户或数据权限 | 幂等分页或排序 | 错误码 | 兼容策略 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| API-001 | FR-007 | packed consumer/Playwright | MOD-004 | HTTP | POST /auth/login | LoginCommand/LoginVO | R<LoginVO> | tenant、username、password | 既有登录边界 | 登录非幂等会话 | 既有 auth errors | 不改契约 | `rules/backend/03-api.md` | 空库真实登录 |
| API-002 | FR-011 | Playwright fixture | MOD-006 | HTTP | POST /system/tenant | SaveSysTenantCommand/Long | R<Long> | unique tenant code | admin 与系统租户规则 | tenant code 幂等身份 | 既有 tenant errors | 不改契约 | `rules/backend/03-api.md` | 自建/清理租户 fixture |
| API-003 | FR-011 | Playwright fixture | MOD-006 | HTTP | POST /workflow/categories | SaveWorkflowCategoryCommand/String | R<String> | categoryCode 与 domainCode | workflow admin/tenant | category code 稳定 | 既有 workflow errors | 不改契约 | `rules/backend/03-api.md` | 自建/清理业务域分类 |
| API-004 | FR-015 | 登录页 | MOD-004 | HTTP | GET /api/system/admin-branding/public | public branding query/VO | R<BrandingVO> | 无用户输入 | `@ApiAccess(PUBLIC)` 与匿名过滤链一致 | 只读幂等 | 既有 system errors | 修正实现与声明不一致，不扩大其它匿名面 | `rules/backend/03-api.md` | 未登录 200 与受保护接口 401 对照 |
| API-005 | FR-015 | 登录页 | MOD-004 | HTTP | GET /api/system/tenant/login-options | login options query/VO | R<LoginOptionsVO> | 请求域名/tenant hint | PUBLIC 仅返回登录前必要字段 | 只读幂等 | 既有 tenant errors | 同 API-004 | `rules/backend/03-api.md` | 未登录 200、最低租户可选 |
| API-006 | FR-019 | Notice bell | MOD-007 | HTTP | GET /api/notice/site/my/unread-category-stats | current user context | R<NoticeUnreadCategoryStats> | 无显式业务输入 | 登录用户/当前租户 | 只读幂等 | 既有 Notice errors | fixture 同时提供 total 与三类计数 | `rules/backend/03-api.md` | `total <= 10` 与 `total > 10` 浏览器分支 |

## 6. 持久化与数据迁移设计

| 数据设计ID | 上游或模型ID | 表或实体 | 字段变化 | 约束 | 索引 | 租户审计 | Mapper边界 | 数据来源 | migration或回填 | 回滚或补偿 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| DB-001 | DM-003 | Bootstrap 写集证据 | 无生产字段；测试记录 before/after 和 SQL 分类 | plan/verify 业务写集空，apply 属于 allowlist | 沿用 schema | 专用数据库 | 不新增业务 Mapper | datasource proxy/metadata | 无生产 migration | 删除一次性库，不改现有业务库 | `rules/backend/04-db.md` | MySQL 写集断言 |
| DB-002 | DM-005 | SchemaSnapshot 内存模型 | charset、collation、default、index、constraint 等语义字段 | type+name 唯一，排序确定，unknown fail-closed | 不适用 | 无业务租户 | store 映射 metadata，generator 消费 diff | information_schema 与受控 SHOW CREATE | 无生产 migration | 失败不产 baseline并清理临时 schema | `rules/backend/07-persistence.md` | canonical 正例、真实差异负例、monolith |
| DB-003 | DM-004 | Registry 与平台基础表 | 不改 schema；新增 generated declarations | stable id/version/bizKey，重入幂等 | 既有唯一约束 | 既有 handler 规则 | 仅 Resource handler 写 | classpath declarations | 空库 apply，无回填 | 既有 journal/事务，runtime 不补写 | `rules/backend/11-module-menu.md` | 空库计数、来源、重入 |
| DB-004 | FR-011 | E2E tenant/workflow data | 不改 schema | 只删除当前 run 持有 id/code 的记录 | 既有索引 | suite-owned tenant/code | 仅公开 API | fixture setup | before 创建、after 逆序清理 | 输出 handle 定向恢复，禁止宽条件删除 | `rules/frontend/04-test.md` | fixture ownership 测试 |

## 7. 安全、权限、租户与数据边界

| 安全设计ID | 系统需求ID | 能力 | 权限资源 | 默认授权 | 后端校验入口 | 租户边界 | 数据归属断言 | 前端反馈 | 审计 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|
| SEC-001 | FR-005、FR-007 | 空库 Bootstrap | 既有运维权限与本地 DB 凭据 | 不新增 | command/mode conditions | 仅任务专用库或派生临时库 | 不连接现有业务库 | CLI step/result | 非敏感写集与计数 | `rules/backend/04-db.md` | URL 与库名预检、写集 |
| SEC-002 | FR-006 | project upgrade | workspace 文件权限 | 不新增 | managed-field selector | 无运行时租户 | 只改 Mango 管理字段 | dry-run/rollback | 不输出 registry Secret | `rules/03-ai-coding-redlines.md` | fixture diff、敏感扫描 |
| SEC-003 | FR-011 | E2E fixture | 既有 test admin | 测试环境 | API helper 保存 id/code | suite tenant 与 system tenant 区分 | cleanup 同时匹配 run handle | setup failure 中止 | 不记录 token/cookie/password | `rules/frontend/04-test.md` | cross-run 保护 |
| SEC-004 | FR-015、FR-016 | cold empty-db login/restart | 最低管理员角色资源；仅两条登录前 PUBLIC API 匿名 | Bootstrap 声明定义 | API access registry + auth filter | default/system tenant 明确 | 登录后资源与业务写均在当前租户 | 公开接口失败显示可诊断错误 | receipt、登录与菜单来源可审计 | `rules/backend/03-api.md` | 未登录 PUBLIC 200、相邻 protected 401、登录后权限正例 |
| SEC-005 | FR-018、FR-019、FR-020 | Notice browser fixture/artifacts | test login state；仅 mock 当前页面公开 API | 不新增产品权限 | pathname 精确分类、secretValues 与 configJson 分离 | 当前测试用户/租户 | 不把源码或非 API 请求当成受信后端响应 | fixture 漂移显式失败 | traces/screenshots 不记录 token/password | `rules/frontend/04-test.md` | source-module 负例、敏感字段 request body、外置 artifacts |

## 8. 错误码、异常与可观测性

| 错误设计ID | 系统需求ID | 失败场景 | 触发条件 | 错误码 | 异常类型 | 用户反馈 | 日志上下文 | 指标或告警 | 重试或补偿 | 敏感信息处理 |
|---|---|---|---|---|---|---|---|---|---|---|
| ERR-001 | FR-001 | mode 非法或冲突 | 字段和 app.args 冲突 | INVALID/CONFLICTING_PROCESS_MODE | CLI config failure | app key、字段、允许值 | workspace/app/mode | CLI gate | 修 manifest 重试 | 不输出完整 env |
| ERR-002 | FR-002、FR-003 | lifecycle/consumer 不一致 | inventory 漂移、blockingIssues、未 flatten、解析失败 | Maven/plugin failure | build failure | lifecycle/module/coordinate | report path | generated gate | 保留 runtime 证据后全链重跑 | registry credential 不入报告 |
| ERR-003 | FR-006 | tuple 缺失/冲突/写中断 | manifest 或 target 预检失败 | RELEASE_TUPLE_* | upgrade failure | 明确未写或已恢复 | ecosystem/file/expected/actual | upgrade gate | 自动恢复原文件 | 不输出 token |
| ERR-004 | FR-005 | Bootstrap 越界 | worker bean、业务表查询或写集越界 | BOOTSTRAP_ISOLATION_FAILURE | context/test failure | bean/table 分类 | mode/command/step | regression gate | 不放过，修 provider 条件 | SQL 参数脱敏 |
| ERR-005 | FR-008、FR-010 | schema/packaging 不等价 | 语义 diff、metadata unknown、hook 或 entry 缺失 | MANGO-BASELINE-019/Invoker failure | build failure | object path 或 JAR entry | module/object/property/IT | baseline gate | 不产制品，清理临时库 | 不记录数据与凭据 |
| ERR-006 | FR-011 | fixture/anchor 失败 | setup、cleanup、code query 或 selector 非唯一 | Playwright failure | test failure | suite/code/step | trace/screenshot | E2E gate | 精确清理后重跑 | 不记录 auth state |
| ERR-007 | FR-012、FR-017 | module add 半成功 | format、lockfile、frozen install 或完整 check 失败 | MODULE_ADD_TRANSACTION_FAILED | CLI transaction failure | module/阶段/已恢复状态 | project/module/tool/exit | packed generator gate | 自动恢复原文件，修根因后重试 | 不输出 registry token |
| ERR-008 | FR-014 | package 假成功 | Boot entries 或独立启动缺失 | GENERATED_BOOT_ARTIFACT_INVALID | Maven/artifact failure | artifact 与缺失 entry/health | coordinate/JAR/report | generated artifact gate | 不接受普通 jar，修 POM 后 clean package | 日志脱敏 |
| ERR-009 | FR-015、FR-016 | 空库不可登录或 stale runtime | 基础数据为空、PUBLIC 401、identity 缺失/默认0/不匹配 | EMPTY_DB_LOGIN_UNAVAILABLE/STALE_RUNTIME_GENERATION | Bootstrap/runtime failure | API/identity/expected/actual | workspace/db/generation/source | restart E2E | 不手工 seed、不回退 generation 0 | 密码/token 不入日志 |
| ERR-010 | FR-018 | Vite source module 被 API fallback fulfill | 非 `/api/` pathname 的 `.ts/.vue` 请求进入 JSON fallback | PLAYWRIGHT_SOURCE_REQUEST_INTERCEPTED | Playwright contract failure | request URL 与分类 | pathname/resourceType/fallback set | Notice E2E gate | 修 route predicate 后重跑 | URL 不含凭据 |
| ERR-011 | FR-019 | Notice structured/list/grouped 合同漂移 | 缺 category stats、旧 title、动作或渠道 fixture 不完整 | NOTICE_BROWSER_CONTRACT_DRIFT | Playwright failure | 缺失字段/接口/可见业务语义 | message/category/action/path | Notice E2E gate | 同步公开 API fixture，不降级断言 | secret 仅断言进入 secretValues |
| ERR-012 | FR-020 | reporter/artifact 触发 Vite reload | output 位于 Vite root | PLAYWRIGHT_ARTIFACT_WATCH_INTERFERENCE | configuration/test failure | artifact path 与 reload 文件 | config/root/output | Playwright config gate | 移到 worktree runtime 根并重跑 | artifact 不上传凭据 |

## 9. 前端结构与交互实现映射

生产页面不做功能或视觉重设计；仅在真实组件缺少稳定语义入口时增加不改变可见行为的 `data-*` 属性。

| 前端设计ID | 系统需求ID | 页面或动作 | 页面key或路由 | 区域与组件 | 状态来源 | API依赖 | 权限或不可操作 | 空加载或失败态 | 语义测试锚点 | 复用判断 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|
| UI-001 | FR-007 | 登录并访问 generated menu | generated module route | sidebar 与 generated CRUD | Registry/router | API-001 | 既有 menu permission | resource 为空即失败 | route/resource code；必要时 data-resource-code | 复用 menu component | `rules/frontend/04-test.md` |
| UI-002 | FR-011 | tenant switch/menu navigation | admin tenant/menu routes | tenant selector/sidebar/page | self-created tenant | API-002 | test admin | fixture 缺失中止 | tenant code、route name | 复用现有组件 | `rules/frontend/04-test.md` |
| UI-003 | FR-011 | Workflow domain/definition/actions | workflow routes | domain selector/list/actions | self-created domain code | API-003 | workflow admin | setup 失败中止 | domain/category code；必要时 data-record-key | 仅缺 code DOM 时补 anchor | `rules/frontend/04-test.md` |
| UI-004 | FR-015、FR-017 | 空库登录与 generated CRUD | login→generated route | branding/tenant selector/login/sidebar/list/dialog | Bootstrap 最低数据与 typed resources | API-004、API-005、API-001 | 未登录仅两条 PUBLIC；登录后按角色 | 登录前接口或菜单为空即失败 | tenant code、resource code、data-page/data-action | 复用生成页面与现有登录组件 | `rules/frontend/04-test.md` |
| UI-005 | FR-019 | Notice realtime/bell/message center | home→message-center routes | structured alert、detail dialog、bell list/categories、message action | realtime detail、category stats、message state | API-006 与既有 message detail/action API | `notice:site:view` 与目标菜单权限 | API 失败进入受控空态但验收失败 | role/text 与 `data-test=notice-category-*` | 复用 NoticeClientBell/NoticeDetailDialog/admin shell | `rules/frontend/04-test.md` |

## 10. 测试设计与验收映射

M09 静态验证观察 manifest/POM/config/JAR/docs；M10 单元测试观察 mode、upgrade plan、conditions 和 schema diff；M11 集成测试观察 Maven/Spring/MySQL/Invoker/consumer；M12 API 验证观察 health/login/menu/fixture；M13 UI 验证观察登录、generated menu、tenant 与 Workflow；M14 专家复核观察 L3 跨模块设计盲区和证据充分性。六项均由用户批准的推荐方案启用。

| 测试用例ID | 系统验收ID | 设计项ID | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 权限或租户边界 | 稳定契约 | 执行入口 | 证据 | 失败处理 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| TC-001 | SAC-001、SAC-004 | DEC-002、FLOW-001 | mode compatibility/conflict、runtime first arg、locker context | P0 | M09/M10/M11 | AUTO | old/new manifest | workspace | unique runtime + locker bean | CLI tests + packed context | report/log | 手工参数依赖阻断 | `rules/09-test-case-automation-flow.md` |
| TC-002 | SAC-002 | DEC-003、FLOW-002 | same generated backend verify/install | P0 | M11 | AUTO | packed generated module | runtime project | inventory equal、no blockingIssues | backend gate | architecture report | lifecycle drift 阻断 | `rules/backend/08-test.md` |
| TC-003 | SAC-003 | DEC-003、FLOW-002 | flatten/qualifier/external consumer | P0 | M09/M11 | AUTO | isolated local repo/consumer | no production registry write | installed POM no literal revision | install + consumer verify | POM/log | reactor-only 不算通过 | `rules/backend/08-test.md` |
| TC-004 | SAC-006 | DEC-004、FLOW-003 | dry-run/apply/reentry/rollback | P0 | M10/M11 | AUTO | old release fixture | temp project | tuple consistent、user fields unchanged | CLI suite | before/after diff | partial write 阻断 | `rules/09-test-case-automation-flow.md` |
| TC-005 | SAC-005 | DEC-005、FLOW-004、DB-001 | mode beans and plan/apply/verify writes | P0 | M10/M11 | AUTO | dedicated empty DB | task DB only | workers runtime-only、read/write boundaries | bootstrap IT | bean/write inventory | any violation 阻断 | `rules/backend/08-test.md` |
| TC-006 | SAC-007 | DEC-006、FLOW-005、API-001 | declaration/empty DB/reentry/login/menu | P0 | M10/M11/M12/M13 | AUTO | full generated project | system/default tenant | all base/business counts nonzero and menu clickable | Bootstrap + API + Playwright | counts/trace | fake healthy 阻断 | `rules/backend/11-module-menu.md` |
| TC-007 | SAC-008、SAC-009、SAC-010 | DEC-007、FLOW-006、FLOW-007、DB-002 | canonical diff/monolith/performance/Boot positive-negative | P0 | M10/M11 | AUTO | all migrations + performance fixture | temporary DB | equivalent schema、B1/COMPLETED/rows/time、hook fail-closed | plugin tests/Invoker/monolith goal | diff/history/time/entries | skipped assertion 阻断 | `rules/backend/08-test.md` |
| TC-008 | SAC-001、SAC-002、SAC-003、SAC-004、SAC-005、SAC-006、SAC-007、SAC-008 | DEC-001、MOD-001、MOD-002、MOD-003、MOD-004、MOD-005 | packed CLI empty-db true consumption chain | P0 | M11/M12/M13 | AUTO | runtime project/repo/dedicated DB | no existing business DB | generate→install→upgrade→bootstrap→runtime→login→menu→BSQL consumer | real chain commands | runtime reports/counts/artifacts | any manual bypass blocks completion | `rules/11-delivery-assurance.md` |
| TC-009 | SAC-011 | DEC-008、FLOW-008、API-002、API-003、UI-002、UI-003 | self-created tenant/domain and serial Chromium | P1 | M12/M13 | AUTO | run-unique codes | suite-owned fixtures | no display-copy dependency、safe cleanup | targeted Playwright | trace/API log | fixture instability blocks suite | `rules/frontend/04-test.md` |
| TC-010 | SAC-001、SAC-005、SAC-008、SAC-011 | DEC-001、SEC-001、SEC-002、SEC-003 | independent L3 design/evidence review | P0 | M14 | MANUAL | design/diff/gates/real-chain evidence | no expanded external write | blockers、non-blocking advice、PASS/FAIL | Tech Lead/QA review | review artifact | open blocker prevents completion | `rules/05-ai-delivery-quality.md` |
| TC-011 | SAC-012、SAC-017 | DEC-009、DM-006、FLOW-009、ERR-007 | 中英文显示名预检、short/long names module add、failure rollback、frozen install 和完整 frontend check | P0 | M09/M10/M11 | AUTO | invalid display names + two rendered projects | workspace-owned files only | invalid input zero-write；success immediately CI-ready；failure byte-exact rollback | packed CLI generator fixture | lockfile/diff/check logs | 只过页面字符串基线不算通过 | `rules/frontend/04-test.md` |
| TC-012 | SAC-013、SAC-014 | DEC-003、DEC-010、FLOW-002、FLOW-010、ERR-002、ERR-008 | module add→install→new-process dev start；clean package→java -jar | P0 | M09/M11/M12 | AUTO | qualified generated project | isolated repo/ports/DB | installed POM无revision；Boot entries完整；外部进程health/Flyway/resource通过 | generated backend/artifact gate | POM/JAR/log/counts | reactor 内或 spring-boot:run 单独通过不算完成 | `rules/backend/08-test.md` |
| TC-013 | SAC-015 | DEC-012、FLOW-011、API-004、API-005、SEC-004、UI-004 | empty-db minimum data、PUBLIC boundary、browser login/menu/CRUD | P0 | M11/M12/M13 | AUTO | dedicated empty DB | system/default tenant + minimum admin | PUBLIC 200/protected 401；真实登录和业务操作成功 | Bootstrap/API/Chromium | counts/network/trace | health-only 或手工 seed 阻断 | `rules/09-test-case-automation-flow.md` |
| TC-014 | SAC-016 | DEC-011、DM-007、FLOW-011、ERR-009 | cold apply→start→stop→start 与 rolling generation | P0 | M10/M11/M12 | AUTO | stable receipt/control state | dedicated DB/workspace | 两次 start 使用稳定正 generation；rolling identity 一致 | CLI tests + real process E2E | receipt/state/log | 默认0或人工参数依赖阻断 | `rules/backend/08-test.md` |
| TC-015 | SAC-018 | DEC-013、DM-008、FLOW-012、ERR-010 | Vite API 源码请求与 backend fallback 互斥 | P1 | M09/M13 | AUTO | observed `/@fs` 与 `/src/api` modules | browser-only fixture | fallback request 全为 `/api/`；observed `.ts/.vue` source 不被 fulfill；页面非白屏 | notice-announcement Chromium | request sets/trace | 零 observed source 或任一交集均失败 | `rules/frontend/04-test.md` |
| TC-016 | SAC-019 | DEC-014、DM-009、FLOW-013、API-006、UI-005、ERR-011 | structured realtime/detail/action 与 list/group thresholds | P1 | M10/M13 | AUTO | message、category stats、channel contracts | current test user/tenant | type/content/time/click、详情与 query 跳转；`<=10` list、`>10` categories | Notice unit + notice-site-message Chromium | assertions/trace | 旧 title、缺 API 或 action drift 阻断 | `rules/frontend/04-test.md` |
| TC-017 | SAC-020 | DEC-015、DM-010、FLOW-014、ERR-012 | reporter/outputDir 位于 Vite root 外且无 reload | P1 | M09/M13 | AUTO | Playwright config/runtime root | worktree only | config 可 list；report/artifacts 位于 `.runtime`；Vite 无 report reload | Playwright list + Notice Chromium | config/path/server log | app root 内输出或 reload 阻断 | `rules/frontend/04-test.md` |

## 11. 兼容与已启用能力说明影响

M08=`ENABLE`：实现批次必须同步 CLI、Bootstrap/provider、Resource/module menu、Persistence/BSQL 和前端 E2E 的 README 或能力地图，写清真实用法、失败语义和验收入口。

| 影响ID | 设计项ID | 影响对象 | 当前行为 | 目标行为 | 兼容策略 | 升级或补偿 | 已启用能力说明 | 验证 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|
| IMP-001 | DEC-002、DEC-004 | existing CLI projects | no mode、partial locks | default runtime and atomic tuple | missing mode compatible、conflict rejected | dry-run + automatic rollback | CLI README mode/tuple/recovery | TC-001、TC-004 | CLI owner |
| IMP-002 | DEC-003 | generated backend/consumer | reactor works、external may fail | verify/install/flatten/consumer | retain revision/qualifier | old project follows POM upgrade guide | Maven/generated README | TC-002、TC-003 | CLI/plugin owner |
| IMP-003 | DEC-005 | Bootstrap/providers | runtime worker can activate | worker runtime-only、handlers retained | runtime default unchanged | no data migration | Bootstrap/Job/Notice/Payment mode docs | TC-005 | provider owners |
| IMP-004 | DEC-006 | generated Resource | old disabled runner | typed declaration in Bootstrap | new template stops old file | existing projects migrate by guide | Resource/menu docs and capability map | TC-006、TC-008 | Resource/CLI owners |
| IMP-005 | DEC-007 | BSQL | string diff、gates can skip | semantic diff and fail-closed gates | root manifest layout retained | regenerate unpublished baseline | Persistence/BSQL equivalence and packaging docs | TC-007 | Persistence owner |
| IMP-006 | DEC-008 | Admin E2E | seeded copy/concurrent shared state | owned fixtures/stable code/serial suites | visible copy unchanged | no production migration | E2E fixture/cleanup/serial docs | TC-009 | Frontend QA owner |
| IMP-007 | DEC-009、DEC-010 | generated business projects | module add 后需隐式安装/人工格式化；package 产普通 jar | module add 事务后 CI-ready；package 产可执行 Boot JAR | 旧项目按升级指南补 lockfile/format/repackage | dry-run/失败自动恢复；旧项目 clean package 复验 | CLI/generated project README | TC-011、TC-012 | CLI owner |
| IMP-008 | DEC-011、DEC-012 | bootstrapped empty databases | generation 默认0且登录前接口/最低数据未闭环 | 从 receipt 传稳定 identity，最低数据和 PUBLIC 登录合同可用 | 未 Bootstrap 项目保持明确失败；不兼容回退0 | 重新 plan/apply/verify 生成 receipt；按指南验证两次重启 | Bootstrap/Auth/System/CLI docs | TC-013、TC-014 | Bootstrap/Auth owners |
| IMP-009 | DEC-013、DEC-014、DEC-015 | Admin Notice E2E | glob 可吞源码、旧 title/缺 fixture、默认 report 在 Vite root | pathname 分类、当前结构化/阈值合同、外置 artifacts | 不改变 Notice 生产 UI/API；只同步测试与配置 | 老命令继续可用，报告新位置为 worktree `.runtime` | Admin E2E 使用说明/Playwright config | TC-015、TC-016、TC-017 | Frontend QA owner |

## 12. 技术追踪矩阵

| 上游ID | 设计项ID | 测试用例ID | 覆盖说明 |
|---|---|---|---|
| FR-001、FR-004、SAC-001、SAC-004 | DEC-001、DEC-002、MOD-001、DM-001、FLOW-001、ERR-001、IMP-001 | TC-001、TC-008、TC-010 | mode、locker、真实启动 |
| FR-002、SAC-002 | DEC-003、MOD-002、FLOW-002、ERR-002、IMP-002 | TC-002、TC-008 | verify/install inventory 与报告 |
| FR-003、SAC-003 | DEC-003、MOD-001、MOD-002、FLOW-002、ERR-002、IMP-002 | TC-003、TC-008 | flatten、qualifier、external consumer |
| FR-005、SAC-005 | DEC-005、MOD-003、DM-003、FLOW-004、DB-001、SEC-001、ERR-004、IMP-003 | TC-005、TC-008、TC-010 | worker 与读写边界 |
| FR-006、SAC-006 | DEC-004、MOD-001、DM-002、FLOW-003、SEC-002、ERR-003、IMP-001 | TC-004、TC-008 | tuple、dry-run、rollback |
| FR-007、SAC-007 | DEC-006、MOD-004、DM-004、FLOW-005、API-001、DB-003、UI-001、IMP-004 | TC-006、TC-008 | Resource、基础数据、登录菜单 |
| FR-008、SAC-008 | DEC-007、MOD-002、DM-005、FLOW-006、DB-002、ERR-005、IMP-005 | TC-007、TC-008、TC-010 | semantic snapshot 与 monolith |
| FR-009、SAC-009 | MOD-005、FLOW-006 | TC-007 | production mode、history/state/rows/time |
| FR-010、SAC-010 | MOD-002、FLOW-007、ERR-005 | TC-007 | Invoker hook、root entry、negative case |
| FR-011、SAC-011 | DEC-008、MOD-006、FLOW-008、API-002、API-003、DB-004、SEC-003、ERR-006、UI-002、UI-003、IMP-006 | TC-009、TC-010 | owned fixtures、stable code、serial Chromium |
| FR-012、FR-017、SAC-012、SAC-017 | DEC-009、MOD-001、DM-006、FLOW-009、ERR-007、IMP-007 | TC-011 | module add 事务、lockfile、真实渲染全工程门禁 |
| FR-013、FR-014、SAC-013、SAC-014 | DEC-003、DEC-010、MOD-001、MOD-002、FLOW-002、FLOW-010、ERR-002、ERR-008、IMP-002、IMP-007 | TC-012 | install 后新进程 dev start 与可执行 Boot JAR |
| FR-015、SAC-015 | DEC-012、MOD-004、FLOW-011、API-004、API-005、SEC-004、ERR-009、UI-004、IMP-008 | TC-013 | 最低登录数据、PUBLIC 边界、菜单 CRUD |
| FR-016、SAC-016 | DEC-011、MOD-001、MOD-003、DM-007、FLOW-011、SEC-004、ERR-009、IMP-008 | TC-014 | stable generation 解析、两次重启、rolling identity |
| FR-018、SAC-018 | DEC-013、MOD-007、DM-008、FLOW-012、SEC-005、ERR-010、IMP-009 | TC-015 | API pathname fallback 与 Vite source module 负面证据 |
| FR-019、SAC-019 | DEC-014、MOD-007、DM-009、FLOW-013、API-006、SEC-005、ERR-011、UI-005、IMP-009 | TC-016 | structured Notice、两种总数阈值、详情与业务跳转 |
| FR-020、SAC-020 | DEC-015、MOD-007、DM-010、FLOW-014、SEC-005、ERR-012、IMP-009 | TC-017 | 外置 reporter/artifacts 与无 Vite reload |
| FR-001、FR-005、FR-008、FR-011、FR-012、FR-013、FR-014、FR-015、FR-016、FR-017、FR-018、FR-019、FR-020 | DEC-001、SEC-001、SEC-002、SEC-003、SEC-004、SEC-005 | TC-008、TC-010、TC-011、TC-012、TC-013、TC-014、TC-015、TC-016、TC-017 | 全批次汇合、20 项真实消费链、独立复核；#687/#688 与外部写保持排除 |

## 13. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 技术设计 checker | PASS | `node mango-pmo/tools/check-technical-design.mjs --document mango-docs/designs/2026-08-02-issue-690-weekly-regression-remediation.md` |
| 生命周期 handoff | PASS | Issue #690 是唯一需求源；用户已批准推荐总体方案；preflight 判定 L3/FULL、M01=CREATE |
| 专项规范检查计划 | PASS | TC-001 至 TC-017 覆盖 M09-M14、MySQL、packed CLI、frozen install、external consumer、Boot JAR、Bootstrap writes/restart、PUBLIC API、monolith BSQL、Notice Chromium 和 reporter isolation |
| 未关闭阻断数量 | 0 | DEC-001 至 DEC-015 均有选择与失败语义；发现新架构选择必须回修本文 |
| Tech Lead 审批 | APPROVED | HardyDou 于 2026-08-02 批准原方案，并在两次追加回归后要求 Agent 自行吸收、持续目标模式推进；记录见 `mango-docs/designs/issue-690-weekly-regression/review/APPROVAL.md` |
