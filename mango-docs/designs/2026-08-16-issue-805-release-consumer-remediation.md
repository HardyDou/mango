---
documentId: TDD-ISSUE-805-RELEASE-CONSUMER
documentType: technical-design
pmoVersion: 1.3.15
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: requirement=L3，发布 tuple 的 full 单体消费者有 10 个授权菜单失败且运行证据包含框架告警，完整发布闭包还存在三包运行时循环，阻断平台升级；solution=L3，修复跨越 npm 构建边界、CLI 模板、后端默认配置、FE1/FE2/FE3 依赖方向和真实发布消费者门禁；final=max(requirement,solution)
status: APPROVED
action: NEXT
owner: Mango Development Agent
approver: HardyDou
approvalEvidence: mango-docs/designs/issue-805-release-consumer/review/APPROVAL.md
upstreamDocumentId: NONE
upstreamDocumentHash: NONE
---

# Issue #805 发布 tuple 全模块消费者修复技术设计文档

## 1. 设计输入、约束与决策

需求源为 [Issue #805](https://github.com/HardyDou/mango/issues/805)，代码基线为 `origin/main@ff76c28db3d1a7d106e978e793163effc0d0ce16`。系统要求为：FR-001 CMS 九个菜单共享唯一页面注册表；FR-002 full 单体生成项目默认启用领域事件 outbox，系统事件 API 不再 404；FR-003 CLI 与生成 README 的 CLI/PMO tuple 一致；FR-004 已确认的 Element Plus prop 与弃用告警清零；FR-005 favicon 不再 404；FR-006 发布候选和纯消费仓门禁能阻断注册表分裂及模板漂移；FR-007 全新消费者不修改源码、不追加参数时全部实际授权叶子菜单通过；FR-008 完整发布闭包不存在运行时循环并能生成确定性拓扑顺序。SAC-001 至 SAC-008 与 FR-001 至 FR-008 一一对应。发布质量预防治理另建 Issue，不在本次实现；Push、PR、合并、发布和关闭 Issue 不在当前授权范围。

| 决策ID  | 问题                           | 候选方案                                             | 选择                                                                                            | 理由                                                                           | 来源ID或路径                   | 是否推断 | 影响                      | 风险                           | 回退条件                                                   |
| ------- | ------------------------------ | ---------------------------------------------------- | ----------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ | ------------------------------ | -------- | ------------------------- | ------------------------------ | ---------------------------------------------------------- |
| DEC-001 | CMS registrar 注册到私有 Map   | 消费者手工注册；Admin external；重写注册表           | 将 `@mango/cms/admin-pages` 纳入 Admin external，并对 packed `dist/full.js` 建合同              | 与其它官方 registrar 一致，保证唯一 `@mango/admin-pages` 实例                  | FR-001                         | 否       | `@mango/admin`            | external 漏声明再次内联        | packed 合同或页面探针失败即阻断                            |
| DEC-002 | 菜单可见但事件 Controller 关闭 | 隐藏菜单；full 默认启用；Controller 无条件启用       | full 模板显式配置 `mango.event.outbox.enabled: true`                                            | full 已启用 KV outbox 且公开系统事件菜单，配置应与能力集合一致                 | FR-002                         | 否       | CLI full backend template | 改变 full 默认运行能力         | API 装配测试和新项目验收失败即回修                         |
| DEC-003 | 消费验收只做类型和构建         | 保持现状；只增源码单测；增加制品合同和 full 模板回归 | 候选 tarball 检查 Admin external import、CLI full 配置/README/favicon，并保留发布后纯仓消费验证 | 直接覆盖本次“源码正确、发布包错误”的失效模式，运行级全菜单验收作为发布完成门禁 | FR-003、FR-005、FR-006、FR-007 | 否       | release consumer gate     | 低成本合同不能代替真实运行验收 | 合同与运行结果不一致时以真实消费者失败为准                 |
| DEC-004 | 前端告警修复边界               | 全仓迁移；只忽略日志；修复已确认调用链               | 仅修复 Link 空 Tag、Common Pagination `small`、RBAC radio `label`、System/RBAC 数字转换         | 清除 Issue 证据中的告警，不扩大为 Element Plus 3 全仓迁移                      | FR-004                         | 否       | common/link/rbac/system   | 公共 Pagination API 兼容       | 保留 `small` 入参并映射为 `size`，组件和页面测试失败即回修 |
| DEC-005 | 完整发布闭包存在三包运行时循环 | 把 SCC 当发布批次；忽略 optional peer；拆除反向依赖  | 新增 FE1 `@mango/admin-extension` 承接中立扩展契约，先把 `@mango/file` registrar 从 FE3 `@mango/admin-pages/core` 迁出；旧 subpath 受控 re-export | npm 发布没有原子 SCC 事务；FE2 不能反向依赖 FE3，迁移 `file` 边即可恢复 DAG 且保持仓外 API 兼容 | FR-008 | 否 | admin-extension/admin-pages/file/system | 新包和兼容窗口扩大 tuple | 架构图、发布闭包或兼容入口测试失败即阻断 |

## 2. 模块与依赖边界

| 模块设计ID | 模块或包                                     | 职责                      | 改动类型           | 依赖方向                             | 公开能力             | 系统需求ID             | 适用规范ruleId                    | 验证方式                               |
| ---------- | -------------------------------------------- | ------------------------- | ------------------ | ------------------------------------ | -------------------- | ---------------------- | --------------------------------- | -------------------------------------- |
| MOD-001    | `mango-ui/packages/admin`                    | full 官方模块聚合         | 构建修复           | Admin -> 官方 registrar external     | `@mango/admin/full`  | FR-001                 | frontend/06-monorepo-architecture | build、packed contract、consumer build |
| MOD-002    | `mango-ui/packages/mango-cli/templates/full` | full 单体生成物           | 配置/文档/资源修复 | CLI -> generated project             | full preset          | FR-002、FR-003、FR-005 | 08-capability-docs                | CLI tests、fresh generate              |
| MOD-003    | `common/link/rbac/system`                    | 管理页面组件与 API 归一化 | 缺陷修复           | domain pages -> common/Element Plus  | 页面既有 API 不变    | FR-004                 | frontend/01-vue-code              | unit/typecheck/browser console         |
| MOD-004    | `mango-ui/scripts`、`release-contracts.json` | 发布消费者门禁            | 测试增强           | sealed tarballs -> isolated consumer | release verification | FR-006、FR-007         | 09-test-case-automation-flow      | release tests、candidate consumer      |
| MOD-005    | `admin-extension/admin-pages/file/system`    | 管理端扩展契约与领域依赖  | 架构拆环           | FE3 -> FE2 -> FE1，禁止 FE2 -> FE3    | page registry contract | FR-008                 | frontend/06-monorepo-architecture | architecture、release plan、pack tests |

## 3. 技术对象与状态模型

| 模型ID | 上游ID | 模型职责              | 标识                                     | 关系                         | 状态编码               | 审计或历史             | 归属或租户   | 一致性约束                                             |
| ------ | ------ | --------------------- | ---------------------------------------- | ---------------------------- | ---------------------- | ---------------------- | ------------ | ------------------------------------------------------ |
| DM-001 | FR-001 | 页面 registrar 注册表 | package instance + moduleCode + page key | registrar 写入、Shell 读取   | registered/ready/error | packed hash 与测试报告 | 前端进程实例 | 写入和读取必须解析到同一 external `@mango/admin-pages` |
| DM-002 | FR-002 | full 事件能力配置     | generated application.yml                | KV outbox 支撑 domain outbox | enabled/disabled       | 生成物 diff            | 应用级       | 默认菜单可见时 Controller 必须注册                     |

| 模型ID | 当前状态         | 触发             | 目标状态                | 前置条件              | 副作用                    | 失败处理                     | 上游ID |
| ------ | ---------------- | ---------------- | ----------------------- | --------------------- | ------------------------- | ---------------------------- | ------ |
| DM-001 | registrar 被内联 | Admin full build | registrar 保持 external | CMS 依赖存在          | packed import 保留        | package contract fail closed | FR-001 |
| DM-002 | full 默认未启用  | CLI init full    | outbox enabled          | KV capability enabled | 注册事件服务和 Controller | 启动/API 失败阻断            | FR-002 |

## 4. 系统流程、事务与一致性

| 流程设计ID | 系统需求ID     | 调用入口                         | 参与模块                           | 处理顺序                                                                                                         | 事务边界             | 状态变化              | 幂等键                       | 并发策略             | 外部失败与补偿                           | 用户可见结果              |
| ---------- | -------------- | -------------------------------- | ---------------------------------- | ---------------------------------------------------------------------------------------------------------------- | -------------------- | --------------------- | ---------------------------- | -------------------- | ---------------------------------------- | ------------------------- |
| FLOW-001   | FR-001         | `bootstrapMangoAdminApp`         | MOD-001、MOD-004                   | import external registrar -> register -> resolve menu page                                                       | 前端启动             | unregistered -> ready | moduleCode/page key          | 单进程幂等 registrar | loader/registry 失败显示诊断并使验收失败 | 9 个 CMS 菜单显示真实页面 |
| FLOW-002   | FR-002         | generated backend runtime        | MOD-002                            | load config -> create outbox beans -> register controller -> GET events                                          | JDBC outbox 自有事务 | disabled -> enabled   | message id                   | 现有 outbox 语义     | 装配失败启动失败，API 非 200 阻断        | 系统事件页面正常加载      |
| FLOW-003   | FR-006、FR-007 | release prepare/publish consumer | MOD-001、MOD-002、MOD-003、MOD-004 | candidate pack -> isolated install/build/contract -> publish -> pure registry install -> full runtime acceptance | 无业务事务           | pending -> verified   | plan digest + tarball sha256 | 同批次串行           | 任一步失败禁止完成发布                   | 不合格 tuple 不能通知升级 |
| FLOW-004   | FR-008         | release plan                      | MOD-005                           | load package graph -> resolve exact-version closure -> topological order -> sealed candidate                         | 无业务事务           | cyclic -> ordered     | package graph hash              | 确定性排序           | 任一 SCC 立即阻断，不采用人工包顺序       | 发布计划可生成           |

## 5. API 与远程契约设计

| 接口ID  | 系统需求ID | 调用方            | 所属模块 | 入口类型 | 方法与路径         | Command Query或VO             | 返回契约             | 校验         | 权限租户或数据权限                 | 幂等分页或排序 | 错误码     | 兼容策略                         | 适用规范ruleId | 验证方式             |
| ------- | ---------- | ----------------- | -------- | -------- | ------------------ | ----------------------------- | -------------------- | ------------ | ---------------------------------- | -------------- | ---------- | -------------------------------- | -------------- | -------------------- |
| API-001 | FR-002     | System event page | MOD-002  | HTTP     | GET /system/events | pageNum/pageSize/abnormalOnly | R<SystemEventPageVO> | 既有参数校验 | `system:event:list` 与租户边界不变 | 既有分页       | 既有错误码 | 不改变 API，只保证 full 默认装配 | backend/03-api | 真实登录 API/browser |

## 6. 持久化与数据迁移设计

| 数据设计ID | 上游或模型ID | 表或实体                 | 字段变化 | 约束            | 索引   | 租户审计          | Mapper边界 | 数据来源         | migration或回填 | 回滚或补偿       | 适用规范ruleId         | 验证方式               |
| ---------- | ------------ | ------------------------ | -------- | --------------- | ------ | ----------------- | ---------- | ---------------- | --------------- | ---------------- | ---------------------- | ---------------------- |
| DB-001     | DM-002       | 现有 KV/domain outbox 表 | 无       | 复用当前 schema | 无变化 | 现有租户/审计语义 | 不变       | event/kv starter | 无 migration    | 关闭配置即可回退 | backend/07-persistence | 空库 bootstrap/runtime |

## 7. 安全、权限、租户与数据边界

| 安全设计ID | 系统需求ID     | 能力               | 权限资源                       | 默认授权 | 后端校验入口             | 租户边界 | 数据归属断言   | 前端反馈                   | 审计     | 适用规范ruleId        | 验证方式             |
| ---------- | -------------- | ------------------ | ------------------------------ | -------- | ------------------------ | -------- | -------------- | -------------------------- | -------- | --------------------- | -------------------- |
| SEC-001    | FR-001、FR-002 | CMS 与系统事件管理 | 既有 `cms:*`、`system:event:*` | 不变     | 现有 Security/Controller | 不变     | 不扩大查询范围 | 404 消失，401/403 语义不变 | 既有日志 | 03-ai-coding-redlines | 管理员正例与匿名 401 |

## 8. 错误码、异常与可观测性

| 错误设计ID | 系统需求ID     | 失败场景        | 触发条件                    | 错误码                               | 异常类型                | 用户反馈     | 日志上下文                  | 指标或告警             | 重试或补偿           | 敏感信息处理 |
| ---------- | -------------- | --------------- | --------------------------- | ------------------------------------ | ----------------------- | ------------ | --------------------------- | ---------------------- | -------------------- | ------------ |
| ERR-001    | FR-001、FR-006 | 注册表分裂      | packed Admin 内联 registrar | PAGE_NOT_REGISTERED                  | route diagnostic        | 404          | module/page/package version | consumer gate failure  | 修复 external 后重建 | 无敏感数据   |
| ERR-002    | FR-002         | 事件 API 未装配 | full 配置缺失/错误          | HTTP 404                             | missing controller      | 页面错误提示 | request path/config mode    | acceptance failure     | 修复模板后重建消费者 | 不记录凭据   |
| ERR-003    | FR-004、FR-005 | console 污染    | prop 类型/弃用/favicon      | Vue/Element Plus warning 或 HTTP 404 | browser console/network | 不声明通过   | route + message             | zero-warning assertion | 修复来源包/模板      | 不记录 token |
| ERR-004    | FR-008         | 发布拓扑不可解  | FE2 反向依赖 FE3 并闭合 SCC | RELEASE_DEPENDENCY_CYCLE                | release planning       | 发布停止     | package/edge/dependency type | release plan failure   | 拆除反向边后重算     | 无敏感数据 |

## 9. 前端结构与交互实现映射

| 前端设计ID | 系统需求ID | 页面或动作               | 页面key或路由  | 区域与组件                       | 状态来源 | API依赖 | 权限或不可操作 | 空加载或失败态 | 语义测试锚点                      | 复用判断   | 适用规范ruleId                    |
| ---------- | ---------- | ------------------------ | -------------- | -------------------------------- | -------- | ------- | -------------- | -------------- | --------------------------------- | ---------- | --------------------------------- |
| UI-001     | FR-001     | CMS 九个管理页面         | `cms/**/index` | 已有 CMS pages                   | CMS API  | API-001 | 既有权限       | 既有状态       | route + main content              | 不新增页面 | frontend/06-monorepo-architecture |
| UI-002     | FR-004     | Link/Post/Login Log 页面 | 既有 route     | ElTag/Pagination/Radio/Statistic | 真实 API | API-001 | 不变           | 不变           | console warning + visible content | 修复原组件 | frontend/01-vue-code              |

## 10. 测试设计与验收映射

| 测试用例ID | 系统验收ID                | 设计项ID                   | 场景                                                        | 优先级 | 测试层级     | 自动化判断 | 测试数据           | 权限或租户边界   | 稳定契约                                  | 执行入口                       | 证据                  | 失败处理     | 适用规范ruleId               |
| ---------- | ------------------------- | -------------------------- | ----------------------------------------------------------- | ------ | ------------ | ---------- | ------------------ | ---------------- | ----------------------------------------- | ------------------------------ | --------------------- | ------------ | ---------------------------- |
| TC-001     | SAC-001                   | DEC-001、FLOW-001          | packed Admin full 保留 CMS external 且注册表探针 ready      | P0     | 集成         | AUTO       | sealed tarballs    | 无新增权限       | import specifier/page keys                | package consumer gate          | `.runtime/issue-805/` | 阻断候选     | 09-test-case-automation-flow |
| TC-002     | SAC-002                   | DEC-002、FLOW-002、API-001 | 全新 full 项目无附加参数访问系统事件                        | P0     | API/入口流程 | AUTO       | 空库 full monolith | 管理员+匿名负例  | HTTP 200/401                              | fresh consumer                 | `.runtime/issue-805/` | 阻断验收     | 09-test-case-automation-flow |
| TC-003     | SAC-003、SAC-005、SAC-006 | DEC-003                    | packed CLI 生成 README/config/favicon 与 release tuple 一致 | P0     | 集成         | AUTO       | packed CLI         | 不适用           | exact versions/files                      | CLI tests + candidate consumer | `.runtime/issue-805/` | 阻断候选     | 04-test-assets               |
| TC-004     | SAC-004                   | DEC-004、UI-002            | 受影响页面真实数据无 Vue/Element Plus warning               | P0     | 组件/UI      | AUTO       | 管理员授权数据     | 既有权限         | zero matching console warning             | package tests + browser巡检    | `.runtime/issue-805/` | 阻断验收     | frontend/04-test             |
| TC-005     | SAC-007                   | FLOW-003                   | fresh caches 全模块 85/85 菜单通过                          | P0     | UI/E2E       | AUTO       | 新库+生成 CRUD     | 实际授权叶子菜单 | 0 404/4xx/5xx/pageerror/framework warning | release consumer suite         | `.runtime/issue-805/` | 不得完成发布 | frontend/04-test             |
| TC-006     | SAC-008                   | DEC-005、MOD-005、FLOW-004 | 三包 manifest/source-runtime/combined SCC 清零，完整发布闭包生成稳定顺序 | P0 | 静态/集成 | AUTO | 当前 package graph | 不适用 | 0 SCC + deterministic topology | architecture check + release plan tests | `.runtime/issue-805/` | 阻断候选 | frontend/06-monorepo-architecture |

## 11. 兼容与已启用能力说明影响

| 影响ID  | 设计项ID         | 影响对象                   | 当前行为                      | 目标行为              | 兼容策略                                       | 升级或补偿               | 已启用能力说明                              | 验证              | 责任人 |
| ------- | ---------------- | -------------------------- | ----------------------------- | --------------------- | ---------------------------------------------- | ------------------------ | ------------------------------------------- | ----------------- | ------ |
| IMP-001 | DEC-001          | `@mango/admin/full` 消费者 | CMS registrar 被内联          | external 共享注册表   | 公开导出与调用方式不变                         | 升级精确 Admin/CLI tuple | Admin/CMS/能力地图                          | packed consumer   | Dev    |
| IMP-002 | DEC-002、DEC-003 | 新 full 项目               | 系统事件默认 404、README 漂移 | 默认可用且 tuple 一致 | 只改变 full preset，新旧项目按升级说明显式配置 | CLI 生成/升级说明        | CLI/Event/Business Starter/能力地图         | fresh generate    | Dev    |
| IMP-003 | DEC-004          | 页面消费者                 | 产生框架告警                  | 相同行为无告警        | props/API 兼容                                 | 随对应包升级             | Common/Link/RBAC/System README 说明行为不变 | component/browser | Dev    |
| IMP-004 | DEC-005          | 管理端扩展包消费者         | FE2 registrar 依赖 FE3 subpath | FE2 依赖 FE1 扩展契约 | `@mango/admin-pages/core|features|notice` 在兼容窗口内 re-export，不改变现有仓外 import | 新增并发布 `@mango/admin-extension`，精确 tuple 由 release plan 生成 | Admin Pages/Admin Extension/File/能力地图 | architecture/pack/consumer | Dev |

## 12. 技术追踪矩阵

| 上游ID                 | 设计项ID                                                                       | 测试用例ID     | 覆盖说明                      |
| ---------------------- | ------------------------------------------------------------------------------ | -------------- | ----------------------------- |
| FR-001                 | DEC-001、MOD-001、DM-001、FLOW-001、UI-001、ERR-001、IMP-001                   | TC-001、TC-005 | 制品合同和真实页面双重覆盖    |
| FR-002                 | DEC-002、MOD-002、DM-002、FLOW-002、API-001、DB-001、SEC-001、ERR-002、IMP-002 | TC-002、TC-005 | 配置、API 和 UI 覆盖          |
| FR-003、FR-005、FR-006 | DEC-003、MOD-002、MOD-004、FLOW-003、ERR-003、IMP-002                          | TC-003、TC-005 | 生成合同与发布消费覆盖        |
| FR-004                 | DEC-004、MOD-003、UI-002、ERR-003、IMP-003                                     | TC-004、TC-005 | 定向组件和全菜单 console 覆盖 |
| FR-007                 | FLOW-003、MOD-004                                                              | TC-005         | 全新消费者最终结论            |
| FR-008                 | DEC-005、MOD-005、FLOW-004、ERR-004、IMP-004                                   | TC-006         | 依赖图清零与确定性发布顺序    |

## 13. 阶段判定与审批

| 检查项           | 结果     | 证据                                                                             |
| ---------------- | -------- | -------------------------------------------------------------------------------- |
| 技术设计 checker | PASS     | 按 PMO 1.3.15 technical-design schema 编写，实施前执行 checker                   |
| 生命周期 handoff | PASS     | Issue #805 为唯一需求源，用户明确要求开始修复                                    |
| 专项规范检查计划 | PASS     | M08-M13 启用；M14 在交付前做独立高影响复核；M15/M16 不属于当前未授权外部发布阶段 |
| 未关闭阻断数量   | 0        | 根因、范围和回退条件均已明确                                                     |
| Tech Lead 审批   | APPROVED | `mango-docs/designs/issue-805-release-consumer/review/APPROVAL.md`               |
