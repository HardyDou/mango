---
documentId: PLAN-ISSUES-824-826
documentType: implementation-plan
pmoVersion: 1.3.16
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: requirement=L3，身份误认与 WECOM 核心发送阻断同时影响用户主流程、租户和企业主体边界；solution=L3，方案跨 Identity、Notice、Auth 与 RBAC，修改持久化数据、公开同步契约、渠道选择和用户界面；final=max(requirement,solution)
status: APPROVED
action: NEXT
owner: Mango Identity 与 Notice 实施负责人
approver: HardyDou
approvalEvidence: review/APPROVAL.md
upstreamDocumentId: TDD-ISSUES-824-826
upstreamDocumentHash: 9d6f3842765499ca2d88bdcfa43c42f738882fb4751ce03d050570d1b39b5859
---

# Issue 824 / 826 第三方身份统一来源实施计划

## 1. 实施目标、范围与交付物

| 交付物ID | 技术设计ID | 交付物 | 路径或模块 | 完成状态定义 | 验收来源 | 不处理边界 |
|---|---|---|---|---|---|---|
| DEL-001 | DEC-001、DEC-004、MOD-001、DM-001、FLOW-001、API-001、DB-001、SEC-001 | Identity 外部身份完整昵称、头像文件 ID、查询和历史误值清理 | mango-platform/mango-identity | 不回退 Mango 昵称，只返回有效绑定，当前用户标识掩码，V3 仅清理明确误值，V4 追加可空头像文件字段 | SAC-001、TC-001、TC-003 | 不重写第三方授权协议，不保存外部头像 URL，不修改真实第三方同名显示名 |
| DEL-002 | DEC-002、DEC-003、DEC-004、MOD-002、DM-002、FLOW-002、FLOW-003、API-002、DB-002、SEC-002、ERR-001 至 ERR-003 | Notice WECOM 发送、通讯录昵称头像同步和单一身份来源 | mango-platform/mango-notice | 发送按 tenant/user/provider/CorpID/BOUND 选取；同步不写旧账户，导入可用头像且 unchanged 修复绑定 | SAC-002、TC-002、TC-003 | 邮件、短信和钉钉接收账户不变，不调用真实 WECOM |
| DEL-003 | MOD-003、MOD-004、FLOW-001、API-003、ERR-004、UI-001、UI-002 | Auth 单成员资料获取、profile 单账号同步与 RBAC 同步界面契约 | mango-platform/mango-auth、mango-ui/packages/auth、rbac、notice | 自助绑定后尝试获取当前成员昵称头像，失败不阻断绑定；个人列表显示 32px 可选头像、完整昵称、辅助尾号和 WECOM 同步图标；手动成功覆盖，失败提示并保留快照；管理员同步界面删除双写开关 | SAC-001、SAC-002、TC-001、TC-003、TC-004 | 不新增独立 WECOM 账号页面，不调用 NoticeWecomSyncService，不同步部门、组织、岗位或角色 |
| DEL-004 | IMP-001、IMP-002、TC-001 至 TC-004 | 测试资产、FULL 文档链和模块 README | 测试目录、mango-docs/designs/issues-824-826、四个 README | 定向自动化、本地服务 API/UI 与文档 checker 通过；说明反映统一来源和真实投递未验收项 | SAC-001、SAC-002 | 不调用真实 WECOM、不发布、不提交、不改 Issue 状态 |

## 2. 工作分解

| 任务ID | 技术设计ID | 交付物ID | 责任角色 | 路径或模块 | 前置任务 | 具体动作 | 完成标准 | 验证ID | 实施批次 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | DEC-001、DEC-004、MOD-001、MOD-003、DM-001、FLOW-001、API-001、API-003、DB-001、SEC-001、ERR-003、ERR-004、UI-001 | DEL-001、DEL-003 | Dev | mango-identity-api/core、mango-auth-api/core/starter/remote、@mango/auth | NONE | 落实昵称头像快照、有效绑定查询和方案 A 展示；自助绑定后按 userid 查询当前 WECOM 单成员；实现 API-003、头像导入补偿和列表同步图标 | 成功才覆盖快照；自助查询失败不阻断绑定；手动失败保留旧资料并返回明确原因；不调用通讯录或组织关系服务 | VAL-001、VAL-002、VAL-005 | B1 Identity/Auth | PLANNED |
| TASK-002 | DEC-002、MOD-002、DM-002、FLOW-002、API-001、DB-002、SEC-002、ERR-001、ERR-002 | DEL-002 | Dev | mango-notice-core | TASK-001 | 在渠道选择后查询 Identity WECOM 绑定，删除 WECOM 接收账户映射和 fallback | CorpID 隔离、失效失败与无旧账户发送路径进入测试资产 | VAL-003 | B2 Notice delivery | PLANNED |
| TASK-003 | DEC-003、DEC-004、FLOW-003、API-002、ERR-003、IMP-002 | DEL-002、DEL-003 | Dev | mango-notice-api/core、@mango/rbac | TASK-002 | 删除同步双写开关和旧账户写入；导入企业微信头像并补偿文件；unchanged 分支绑定 Identity；更新结果字段和页面 | 旧字段、方法和 WECOM 账户映射扫描为零，头像失败不阻断昵称且不记录完整 externalUserId | VAL-003、VAL-004 | B3 Sync consumers | PLANNED |
| TASK-004 | TC-001、TC-002、TC-003、TC-004 | DEL-004 | Dev、QA | Identity/Notice/Auth 测试目录 | TASK-001、TASK-002、TASK-003 | 增加完整昵称、可选头像、显式头像清空、自助绑定自动获取、权限与传输失败仍绑定成功、手动成功覆盖、手动失败保留、头像补偿、未登录 401、资料未同步指引、有效状态、CorpID、发送和通讯录同步修复测试资产 | 定向自动化和静态门禁通过；本地 UI/API 以路由拦截验证同步按钮成功失败交互；真实 WECOM 资料查询和投递保留为授权测试企业后续验收 | VAL-002、VAL-003、VAL-005 | B4 Test assets | PLANNED |
| TASK-005 | IMP-001、IMP-002、MOD-001 至 MOD-004 | DEL-004 | Dev、Tech Lead | 模块 README 与 FULL 文档目录 | TASK-001、TASK-002、TASK-003 | 更新能力说明，建立 BRD/SRS/TDD/Plan 并执行文档 checker | 说明无旧双写语义，文档集合与生命周期检查通过 | VAL-006 | B5 Docs | PLANNED |

## 3. 顺序、依赖与里程碑

| 里程碑ID | 包含任务ID | 进入条件 | 完成条件 | 依赖 | 可并行任务 | 阻塞升级 | 责任人 |
|---|---|---|---|---|---|---|---|
| MS-001 | TASK-001 | BRD、SRS、TDD、Plan 方向已获用户批准 | Identity/Auth 代码与迁移落地 | NONE | NONE | 无法区分真实第三方同名值时停止扩大 migration 条件 | Identity/Auth owner |
| MS-002 | TASK-002、TASK-003 | MS-001 的有效绑定语义可供 Notice 消费 | 发送和同步只使用 Identity，强类型消费者同步更新 | MS-001 | TASK-002、TASK-003 | 任一运行路径仍可读取或写入旧 WECOM 接收账户则阻断 | Notice/RBAC owner |
| MS-003 | TASK-004、TASK-005 | MS-002 完成 | 测试资产、静态验证、文档与剩余风险可交接 | MS-002 | TASK-004、TASK-005 | 编译失败、文档 checker 失败或出现未说明 diff 时阻断 | Dev、QA、Tech Lead |

## 4. 验证计划

| 验证ID | 测试或验收ID | 任务ID | 验证层级 | 命令或步骤 | 环境 | 测试数据 | 权限或租户边界 | 预期结果 | 证据路径 | 责任人 | 失败处理 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| VAL-001 | TC-001 | TASK-001 | 静态/编译 | IdentityUserServiceIntegrationTest 的 Maven reactor 编译与测试 | Java 21、workspace Maven repo | 当前源码与 V3/V4 SQL | tenant、BOUND 与正数头像文件 ID 条件复核 | Identity API/core 编译，V3 限定条件和 V4 追加列清晰 | 命令输出 | Dev | 修复编译、导入或 SQL 条件后重跑 |
| VAL-002 | TC-001 | TASK-001、TASK-004 | 自动化/API | IdentityUserServiceIntegrationTest；DefaultWecomLoginClientTest、ExternalIdentityAvatarServiceTest、ExternalAuthorizationServiceTest、AuthSecurityFlowTest；Auth externalIdentity Vitest；Auth vue-tsc/ESLint/Stylelint | Java 21、pnpm workspace、本地隔离服务 | 昵称不同、显示名为空、尾号 4826、新旧 avatarFileId、48002 权限错误、传输与 Identity 更新异常、BOUND/UNBOUND fixture | 当前 tenant/user/app/CorpID；刷新入口无客户端身份参数；私有文件下载 | Identity 既有定向测试、Auth Core 定向测试、Auth Starter 45/45、Auth 前端 14/14、Maven verify 和改动范围静态检查通过；自助查询失败不阻断绑定，手动失败不覆盖快照 | 命令输出与浏览器会话 | Dev | 修复实现或 fixture，不删除有效断言 |
| VAL-003 | TC-002、TC-003 | TASK-002、TASK-003、TASK-004 | 自动化/静态 | NoticeServiceIntegrationTest；`rg` 扫描旧字段、方法、WECOM 账户映射、外部头像 URL 持久化和敏感日志 | Java 21、workspace Maven repo | corp-a/corp-b、BOUND/UNBOUND、头像 URL stub 和无头像测试源码 | 当前 tenant 与渠道 CorpID | Notice 20/20，头像导入与清空通过，旧执行路径扫描为零 | 命令输出 | Dev | 发现旧路径、敏感日志或头像阻断昵称同步则修复后重跑 |
| VAL-004 | TC-003 | TASK-003 | 前端构建 | `pnpm -C mango-ui --filter @mango/rbac... build` | pnpm workspace | TypeScript 同步命令和结果类型 | system:user:add 语义不变 | RBAC 及依赖构建通过，页面无旧开关字段 | 命令输出 | Dev | 修正类型和消费者后重跑 |
| VAL-005 | TC-004 | TASK-001、TASK-004 | UI/API/人工 | 重启隔离服务；查看 profile、解绑确认、无头像和资料未同步状态；使用浏览器路由拦截 POST /auth/providers/wecom/profile/refresh，分别返回成功和 1505 失败，验证同步图标、loading、成功刷新、失败提示和旧快照保留；真实 WECOM 资料查询与投递后续执行 | 本地后端 18062、前端 30062、隔离数据库 `mango_dev_mango_issues_824_826_clean_062`、浏览器 | 专用绑定用户、真实文件中心记录；成功/失败路由响应 | 当前 tenant、CorpID、登录和文件下载；拦截资料刷新防止真实企业微信调用 | 页面显示 32px 可选头像、完整昵称、辅助尾号和同步图标；同步中不能重复点击；成功提示与刷新；失败提示后端原因且已有昵称头像不消失；真实企业微信结果仍待验收 | `.runtime/issues-824-826/browser/` | QA、业务 Owner | 真实资料查询和投递仅在授权测试企业验收 |
| VAL-006 | TC-003 | TASK-005 | 文档/差异 | 四个单文档 checker、check-lifecycle-handoff、check-document-set、`git diff --check` | 当前 worktree | FULL 文档与源代码 | 不涉及真实凭据 | checker 与差异检查通过 | 命令输出 | Dev、Tech Lead | 修正文档追踪、哈希或格式后重跑 |

## 5. 数据库实施步骤

| 数据步骤ID | 技术设计ID | 环境 | 前置检查 | 动作 | 顺序 | 数据备份或回填 | 验证 | 失败停止条件 | 补偿 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| DATA-001 | DEC-001、DEC-004、DB-001、IMP-001 | 发布前测试库与生产升级窗口 | 确认 identity V1/V2 已执行，统计满足 WECOM+SELF+display_name=nickname 的记录并人工抽样 | 由 Flyway 依次执行 V3 清理限定 display_name、V4 追加 avatar_file_id | 后端制品升级时按模块 migration 顺序执行 | 生产执行前完成数据库备份；不做扩大条件的批量回填 | 升级后确认 V3 目标为空、其它 Provider/SYNC/不同显示名不变，V4 字段可空且可保存有效 File ID | 统计范围异常、备份不可用或 SQL 影响超出限定条件 | 停止发布并从备份恢复；本任务本地只在隔离库执行 | DBA、Identity owner |

## 6. 已启用说明与资产同步计划

| 文档项ID | 技术设计或交付物ID | 目标文档 | 变化 | 责任人 | 完成条件 | 检查命令 | 不适用依据 |
|---|---|---|---|---|---|---|---|
| DOC-001 | IMP-001、DEL-001 | mango-platform/mango-identity/README.md | 说明 displayName 完整第三方来源、空值、掩码回读和 avatarFileId | Identity owner | 与实现一致且不包含 nickname fallback 或外部头像 URL | `rg 'displayName|avatarFileId' mango/mango-platform/mango-identity/README.md` | NONE |
| DOC-002 | IMP-001、IMP-002、DEL-002 | mango-platform/mango-notice/README.md | 说明 WECOM 发送和同步统一使用 Identity，头像导入文件中心 | Notice owner | 明确 CorpID/BOUND 匹配、头像补偿和无旧账户 fallback | `rg 'Identity|WECOM|头像' mango/mango-platform/mango-notice/README.md` | NONE |
| DOC-003 | IMP-002、DEL-003 | @mango/notice 与 @mango/rbac README | 说明无独立 WECOM 账号入口及同步结果语义 | Frontend owner | 文档和 TypeScript 字段一致 | `rg 'WECOM|Identity|身份绑定' mango-ui/packages/{notice,rbac}/README.md` | NONE |
| DOC-004 | DEL-004、TASK-005 | mango-docs/designs/issues-824-826 | 建立 FULL 产品文档链和审批证据 | Product、Tech Lead | 单文档、生命周期和集合 checker 通过 | `node mango-pmo/tools/check-document-set.mjs --root mango-docs/designs/issues-824-826` | NONE |
| DOC-005 | IMP-001、DEL-003 | mango-platform/mango-auth/README.md | 说明自助绑定后按需读取单成员资料、当前用户手动刷新、成功覆盖失败保留与不触发组织同步 | Auth owner | 与 Auth API 和 profile 实现一致，明确企业微信权限和可见范围前提 | `rg '单成员|资料刷新|通讯录' mango/mango-platform/mango-auth/README.md` | NONE |

## 7. 风险、阻塞与例外

| 风险ID | 风险等级 | 类型 | 触发条件 | 影响 | 预防 | 应对 | 责任人 | 截止时间 | 状态 | 例外ruleId | 例外批准与到期 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| RISK-001 | L3 | RISK | Identity 查询缺少 tenant、CorpID 或 BOUND 条件 | 跨租户、跨企业主体错发 | gateway 参数和 Identity service 双重条件，负向测试资产 | 阻断交付，复核全部 WECOM 入口 | Identity/Notice owner | 交付前 | CLOSED | NONE | NONE |
| RISK-002 | L2 | RISK | V3 把真实第三方显示名与 Mango 昵称恰好相同的 SELF 记录清空 | 页面显示资料未同步而非真实显示名 | 仅处理旧实现确实可能回填的 SELF+WECOM+精确相等记录，不删除绑定 | 发布前统计抽样；无法确认时将 migration 从发布批次移除并另行数据治理 | Identity owner、DBA | 发布前 | CLOSED | NONE | NONE |
| RISK-003 | L2 | RISK | 删除同步字段导致旧强类型消费者编译失败 | 消费应用需同步升级 | 后端与 @mango/rbac 同批升级，README 说明字段变化 | 发布影响评估后升版，不恢复旧业务 fallback | Notice/RBAC owner | 发布前 | CLOSED | NONE | NONE |
| RISK-004 | L2 | RISK | 尚未在授权测试企业执行真实 WECOM 消息投递 | 不能证明第三方网络、凭据和真实接收端到端成功 | 已完成自动化、本地数据库/API/UI 与无配置失败路径验证，保持禁止真实厂商调用 | QA 在授权测试企业完成 VAL-005 的真实投递部分后再决定发布 | QA、业务 Owner | 发布前 | OPEN | NONE | NONE |
| RISK-005 | L2 | RISK | 头像导入、替换、删除或页面下载失败 | 头像缺失、文件残留或旧异步响应覆盖新头像 | 只保存 Mango 文件 ID；跨服务补偿清理；request generation 与 Object URL revoke；失败不阻断昵称 | 保留 warning 与后续同步重试，核查孤儿文件并按文件中心生命周期治理 | Notice/Auth owner | 交付前 | CLOSED | NONE | NONE |
| RISK-006 | L2 | RISK | 自助绑定或单账号刷新所用企业微信应用无成员资料读取权限、成员不在可见范围或可信 IP 未配置 | 无法获取或更新昵称头像 | 初次查询失败不阻断绑定；手动失败精确映射48002/60020/60111 和其他企业微信错误；保留旧快照；不启用通讯录同步兜底 | 在授权测试企业配置应用成员可见范围、读取权限和可信 IP 后执行真实单账号验收 | Auth owner、QA | 发布前 | OPEN | NONE | NONE |

## 8. 实施追踪矩阵

| 上游设计ID | 交付物ID | 任务ID | 验证ID | 里程碑数据文档或风险项ID | 覆盖说明 |
|---|---|---|---|---|---|
| DEC-001、DEC-004、MOD-001、MOD-003、DM-001、FLOW-001、API-001、API-003、DB-001、SEC-001、ERR-003、ERR-004、UI-001、TC-001、IMP-001 | DEL-001、DEL-003、DEL-004 | TASK-001、TASK-004、TASK-005 | VAL-001、VAL-002、VAL-005、VAL-006 | MS-001、MS-003、DATA-001、DOC-001、DOC-004、DOC-005、RISK-002、RISK-005、RISK-006 | 覆盖自助绑定后单成员获取、当前账号手动同步、成功覆盖、失败保留、组织架构隔离、头像补偿、迁移、辅助尾号和解绑识别 |
| DEC-002、DEC-003、MOD-002、MOD-004、DM-002、FLOW-002、FLOW-003、API-002、DB-002、SEC-002、ERR-001、ERR-002、UI-002、TC-002、IMP-002 | DEL-002、DEL-003、DEL-004 | TASK-002、TASK-003、TASK-004 | VAL-003、VAL-004 | MS-002、DOC-002、DOC-003、RISK-001、RISK-003 | 覆盖 WECOM 身份隔离、发送、同步、消费者和旧路径清理 |
| TC-003、TC-004 | DEL-004 | TASK-004、TASK-005 | VAL-005、VAL-006 | MS-003、DOC-004、RISK-004 | 覆盖自动化、本地服务、FULL 文档和真实 WECOM 投递剩余验收 |

## 9. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 实施计划 checker | PASS | `node mango-pmo/tools/check-implementation-plan.mjs --document mango-docs/designs/issues-824-826/implementation-plan.md` |
| 生命周期 handoff | PASS | BRD、SRS、TDD、Plan 的 ID、状态、审批与 SHA-256 精确匹配 |
| 依赖图 | PASS | MS-001 → MS-002 → MS-003；同一里程碑内只并行独立任务 |
| 未关闭阻断数量 | 0 | RISK-004 是发布前真实 WECOM 投递剩余风险；本地自动化、数据库、API 与 UI 已通过 |
| 实施审批 | APPROVED | `review/APPROVAL.md` |
