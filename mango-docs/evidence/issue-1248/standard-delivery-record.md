# 标准交付记录

## 1. 元数据

- 任务 ID：Issue #1248
- 交付模式：STANDARD
- 需求影响：L2 - 给已发布的 `POST /workflow/tasks/page-by-business-key` 增加可选过滤参数和排序字段，并新增一个写入接口，改变业务列表可观察的筛选能力和领域事件载荷；影响面限定在 Workflow 任务查询与申请写入
- 方案风险：L2 - 新增表、迁移、查询 SQL 形态、事件载荷字段和多处写入路径；失败会返回错误或不完整的分页结果，回退需同时撤掉接口与 SQL 片段
- 最终风险：L2
- 工作区决策：CREATE（`/data/Mango/mango-issue-1248`，分支 `feat/issue-1248-apply-attribute-filter`）

## 2. 目标与范围

- 目标：业务按自己的字段（保函种类、受益人、被保证人、审核类型、版本状态等）在平台侧筛选审批任务，不再先查业务表拼 `businessKeys`；同一批字段还能用于任务变动消息内容。
- 成功条件：`applyAttributeFilters` 命中「条件间且、条件内或」；`total` 与分页行同源一致；不再受业务侧候选版本数量上限影响；版本状态这类提交后变化的值可通过 `PUT` 刷新；不传新参数时行为与 1.8.0 完全一致；属性可作为 `applyAttribute_<key>` 消息变量。
- 处理范围：`mango-workflow-api`（查询/命令/事件载荷/Api 契约）、`mango-workflow-core`（实体、Mapper、Service、查询条件、事件发布、Flyway `V5`、测试）、`mango-workflow-starter`（Controller、默认通知参数展开）、`mango-workflow-starter-remote`（Feign 契约）、workflow README、业务接入指南、业务接入变更索引、本记录。
- 不处理范围：不做业务字段的区间/模糊/聚合/排序；不引入搜索引擎；不让平台回读业务表；不按项目维度跨轮次回刷属性；不新增权限码；不改 `businessKeys`、`applyKeyword`、`scope`、租户隔离和 `total` 口径。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-1 | `POST /workflow/tasks/page-by-business-key` | `applyAttributeFilters=[{key:guaranteeTypeCode,values:[PERF]}]` | 只返回该属性命中的申请下的任务 | 键非法或值为空返回 `TASK_QUERY_INVALID` | 结果任务全部属于登记了 `guaranteeTypeCode=PERF` 的申请 |
| AC-2 | 同上 | 两个属性条件 | 条件之间为「且」 | 同上 | 只返回同时满足两个条件的申请 |
| AC-3 | 同上 | 同一条件多个值 | 值之间为「或」 | 同上 | 命中任一值即返回 |
| AC-4 | 同上 | 申请没有登记被筛选属性 | 不匹配 | — | 该申请的任务不出现 |
| AC-5 | 同上 | 同一申请同一属性多个值（多值字段） | 多条属性值都参与匹配 | — | 命中任一属性值即返回 |
| AC-6 | 同上 | 不传 `applyAttributeFilters` | 行为与 1.8.0 一致 | — | 既有任务查询用例全部保持通过 |
| AC-7 | 同上 | `applyTimeBegin` / `applyTimeEnd` | 按申请创建时间闭区间过滤 | 起止颠倒返回 `TASK_QUERY_INVALID` | 只返回区间内创建的申请任务 |
| AC-8 | 同上 | `sortBy=applyTime` | 按申请创建时间排序 | 非法字段被校验拒绝 | 顺序按申请创建时间，时间相同按任务 ID |
| AC-9 | 同上 | 多个属性和分页 | `total` 与逐页结果一致 | — | 翻页不重不漏，`total` 等于全量命中任务数 |
| AC-10 | 同上 | 跨租户数据 | 只返回当前租户申请的任务 | 缺租户上下文报错 | 其他租户同属性任务不可见 |
| AC-11 | `POST /workflow/business-applies` | 传 `filterAttributes`（单值/多值） | 与申请同事务写入，按「一行一个值」保存 | 键非法、值超长、属性超 20 个返回 `APPLY_INVALID` | 属性能被查询命中，重复值被去重 |
| AC-12 | `PUT /workflow/business-applies/{applyId}/filter-attributes` | 新属性集合 / 空集合 | 整体覆盖；空集合清空 | 申请不存在或跨租户返回 `APPLY_NOT_FOUND`；非法键值返回 `APPLY_INVALID` | 更新后查询结果随之变化，重复调用结果一致 |
| AC-13 | `workflow.task.advanced` 事件消费方 | 申请登记了属性 | 事件载荷携带 `applyAttributes` | 无属性时为空集合 | 订阅方按业务键读取到结构化属性 |
| AC-14 | 默认工作流通知 | 申请登记了属性 | 通知变量包含 `applyAttribute_<key>`，多值逗号拼接 | 无属性时不注入变量 | 消息模板可引用属性并正确渲染 |
| AC-15 | 默认工作流通知（待办到达） | 申请快照声明 `customConfig.viewPath` / `viewQuery` / `viewActionLabel` | 通知目标改为业务路径，按钮文案用业务声明值 | 路径非法时回退平台 `workflow:task:detail` | 消息目标与按钮文案可在业务列表页正确落地 |
| AC-16 | 默认工作流通知 | 申请快照声明 `customConfig.viewActionEnabled=false` | 不注入任何按钮动作 | 非布尔值按默认注入处理 | 消息只保留标题、正文和查看目标 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-1 | AC-1~AC-5 | 属性按申请存子表，一行一个值：`workflow_business_apply_attribute`，索引 `(apply_id, attr_key)` 与 `(tenant_id, business_type, attr_key, attr_value(48), apply_id)`；不建唯一键，写入路径去重 | `db/migration/workflow/V5__workflow_business_apply_attribute.sql` | 表无读取路径后可保留或删除 |
| D-2 | AC-1~AC-6 | 查询用「每条件一个 `a.id in (select …)` 子查询」，条件间 AND；实测比「派生表 OR + GROUP BY」快 30 倍，比逐条件 `EXISTS` 快 60 倍（见第 6 节压测） | `WorkflowTaskHistoryMapper.xml` | 删除 XML 片段与 `WorkflowApplyAttributeCondition` |
| D-3 | AC-11/AC-12 | 写入走 `filterAttributes`（`Map<String, List<String>>`）：创建时同事务写，更新按申请整体覆盖（先删后插）；键值规则在 `WorkflowApplyAttributeRules` 单点校验，查询侧与写入侧共用 | `CreateWorkflowBusinessApplyCommand`、`UpdateWorkflowBusinessApplyFilterAttributesCommand`、`WorkflowBusinessApplyService`、`WorkflowApplyAttributeRules` | 删除新字段与接口 |
| D-4 | AC-12 | 更新接口复用 `workflow:business-apply:create` 权限码，不新增权限码和初始化数据；只做租户隔离与申请存在性校验，业务数据权限由业务模块自行判定 | `WorkflowBusinessApplyController`、`WorkflowBusinessApplyApi`、`WorkflowBusinessApplyFeignClient` | 删除映射方法 |
| D-5 | AC-7/AC-8 | 新增 `applyTimeBegin`/`applyTimeEnd` 与 `sortBy=applyTime`；排序追加任务 ID 次序键，保证翻页稳定；平台不设默认时间窗，默认时间范围由业务决定 | `WorkflowTaskBusinessPageQuery`、`WorkflowTaskHistoryCriteria`、Mapper XML | 删除字段与排序分支 |
| D-6 | AC-13 | 事件载荷新增 `applyAttributes`，发布时按申请读取一次属性；不把属性塞进 `variables`（流程变量属于引擎语义） | `WorkflowEventPayloadVO`、`WorkflowEventPublisher` | 删除字段与读取逻辑 |
| D-7 | AC-14 | 通知模板是平铺占位符替换，不支持嵌套取值，因此默认通知把属性展开为 `applyAttribute_<key>`，多值英文逗号拼接 | `WorkflowNoticeDomainEventSubscriber` | 删除展开方法 |
| D-8 | AC-4/AC-6 | 未登记的属性一律不匹配；不传筛选时 SQL 与 1.8.0 一致（无任何新条件） | Mapper XML | — |
| D-10 | AC-15/AC-16 | 通知目标与按钮注入由业务 `customConfig` 决定：`viewPath`/`viewQuery`/`viewActionLabel` 对待办通知同样生效，`viewActionEnabled=false` 时不注入按钮；按钮类型仍为 `ROUTE`，默认通知不注入 `EVENT` 动作 | `WorkflowBusinessApplyVO`、`WorkflowEventPayloadVO`、`WorkflowEventPublisher`、`WorkflowNoticeDomainEventSubscriber` | 删除派生字段与展开逻辑；业务不声明配置即回到原行为 |
| D-9 | AC-1~AC-10 | 不复用 `extension_json`、`variables_json`、Flowable 变量表：字段会变，宽表/生成列每加一个字段就要一次平台 DDL；引擎变量在实例结束后无法更新，而「版本状态」会持续变化 | 无（方案取舍） | — |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| I-1 | D-1 | 1 | `mango-workflow-core/src/main/resources/db/migration/workflow/V5__workflow_business_apply_attribute.sql` | 迁移可重复执行，索引与读取路径匹配 |
| I-2 | D-3/D-4 | 2 | `mango-workflow-api/.../query/WorkflowApplyAttributeFilter.java`、`.../command/CreateWorkflowBusinessApplyCommand.java`、`.../command/UpdateWorkflowBusinessApplyFilterAttributesCommand.java`、`.../WorkflowBusinessApplyApi.java` | 契约含校验注解与语义说明，模块编译通过 |
| I-3 | D-1/D-2/D-5 | 3 | `mango-workflow-core/.../entity/WorkflowBusinessApplyAttributeEntity.java`、`.../mapper/WorkflowBusinessApplyAttributeMapper.java`、`.../model/WorkflowApplyAttributeCondition.java`、`.../model/WorkflowTaskHistoryCriteria.java`、`.../service/impl/WorkflowApplyAttributeRules.java`、`.../service/impl/WorkflowBusinessApplyService.java`、`.../service/impl/WorkflowTaskBusinessQueryService.java`、`.../service/IWorkflowBusinessApplyService.java` | 创建/更新/查询链路贯通，计数与列表共用同一条件 |
| I-4 | D-2/D-8 | 4 | `mango-workflow-core/src/main/resources/mapper/workflow/WorkflowTaskHistoryMapper.xml` | 生成 SQL 与压测形态一致，无参数拼接 |
| I-5 | D-4 | 5 | `mango-workflow-starter/.../controller/WorkflowBusinessApplyController.java`、`mango-workflow-starter-remote/.../WorkflowBusinessApplyFeignClient.java` | HTTP 与 Feign 契约一致，权限码复用 |
| I-6 | D-6/D-7 | 6 | `mango-workflow-api/.../vo/WorkflowEventPayloadVO.java`、`mango-workflow-core/.../event/WorkflowEventPublisher.java`、`mango-workflow-starter/.../notice/WorkflowNoticeDomainEventSubscriber.java` | 事件载荷与通知变量可回读 |
| I-7 | AC-1~AC-14 | 7 | `WorkflowTaskAttributeFilterIntegrationTest`、`WorkflowTaskBusinessQueryServiceTest`、`WorkflowBusinessApplyServiceImplIntegrationTest`、`WorkflowEventPublisherTest`、`WorkflowNoticeDomainEventSubscriberTest`、`WorkflowApiSurfaceContractTest` | 新用例通过，既有用例语义不变 |
| I-8 | AC-1~AC-14 | 8 | workflow README、业务接入指南、业务接入变更索引、本记录 | 文档 checker 通过 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-1~AC-10 | H2 集成测试（真实列名夹具） | `mvn -pl mango-platform/mango-workflow/mango-workflow-core -Dtest=WorkflowTaskAttributeFilterIntegrationTest test`（本机用 surefire 3.2.5 显式执行） | PASS：10 用例，覆盖单条件、多值、多条件、缺属性、多值字段、申请时间、分页一致性、申请时间排序、无筛选、租户隔离 | `WorkflowTaskAttributeFilterIntegrationTest` |
| AC-1~AC-9 | 单元测试（校验与条件映射） | `-Dtest=WorkflowTaskBusinessQueryServiceTest` | PASS：37 用例（新增 8 个：条件映射、缺省不过滤、重复键、超限条件、超限值、非法键、空值、申请时间区间） | `WorkflowTaskBusinessQueryServiceTest` |
| AC-11/AC-12 | H2 服务集成测试（真实 Mapper） | `-Dtest=WorkflowBusinessApplyServiceImplIntegrationTest` | PASS：15 用例（新增 5 个：创建写属性、无属性不写、更新覆盖与清空、非法键值、缺失申请） | `WorkflowBusinessApplyServiceImplIntegrationTest` |
| AC-1~AC-12 | HTTP 层契约（MockMvc 真实入口） | `-Dtest=WorkflowApplyAttributeHttpContractTest` | PASS：4 用例（嵌套条件绑定、非法键与超限条件 400、PUT 绑定与清空、缺少请求体 400） | `WorkflowApplyAttributeHttpContractTest` |
| AC-13 | 单元测试 | `-Dtest=WorkflowEventPublisherTest` | PASS：6 用例，含 `applyAttributes` 进入事件载荷 | `WorkflowEventPublisherTest` |
| AC-14 | 单元测试 | `-Dtest=WorkflowNoticeDomainEventSubscriberTest` | PASS：15 用例，含 `applyAttribute_<key>` 展开、多值拼接、业务目标与文案、按钮关闭 | `WorkflowNoticeDomainEventSubscriberTest` |
| AC-15/AC-16 | 服务集成测试（H2 真实 Mapper） | `-Dtest=WorkflowBusinessApplyServiceImplIntegrationTest` | PASS：16 用例，含 `viewActionEnabled` 派生 | `WorkflowBusinessApplyServiceImplIntegrationTest` |
| AC-15/AC-16 | 事件载荷 | `-Dtest=WorkflowEventPublisherTest` | PASS：6 用例，`viewActionEnabled` 缺省按 `true` 下发 | `WorkflowEventPublisherTest` |
| 契约 | 模块全量测试 | `mvn -pl mango-platform/mango-workflow/mango-workflow-api,mango-platform/mango-workflow/mango-workflow-core,mango-platform/mango-workflow/mango-workflow-starter,mango-platform/mango-workflow/mango-workflow-starter-remote test-compile org.apache.maven.plugins:maven-surefire-plugin:3.2.5:test` | PASS：core 206（4 skipped）、starter 56、starter-remote 4，0 失败 | 构建输出 |
| 静态 | Checkstyle / PMD | `mvn -pl <4 modules> checkstyle:check pmd:check` | PASS：4 个模块 Checkstyle 0 违规；PMD 仅存量告警（`UnnecessaryImport` 等），本次新增文件无告警 | 构建输出 |
| 文档 | 能力文档门禁 | `node mango-pmo/tools/audit-module-readmes.mjs`、`audit-readme-source-facts.mjs`、`check-business-guides.mjs`、`check-capability-docs.mjs` | PASS（exit 0） | 脚本输出 |
| 性能 | 本机 MariaDB 11.8 合成数据：30 万申请 / 60 万任务 / 180 万属性行 | 见 `benchmark-seed-mariadb.sql`、`benchmark-sql-shapes-mariadb.sql`、`benchmark-final-shape-mariadb.sql` | 见下方压测结论 | 同目录脚本 |

压测结论（本机 MariaDB 11.8，合成数据；**不是生产 MySQL 8**）：

| 场景 | 条件 | 耗时 |
|---|---|---|
| 计数（3 条件，含高选择性 `beneficiary`） | `a.id in (子查询)` | 0.11 s |
| 取页（同条件，首页） | 同上 | 0.03 s |
| 计数（6 条件） | 同上 | 0.11 s |
| 计数（单条件低选择性，命中 10 万申请） | `guaranteeTypeCode=PERF` | 0.51 s |
| 计数（无属性过滤基线） | 全业务类型 | 0.57 s |
| 取页（低选择性组合命中约 8 万任务） | `guaranteeTypeCode` + `projectType` | 3~4 s |
| 对照：派生表 `OR` + `GROUP BY`（3 条件计数） | 已放弃的形态 | 3.58 s |
| 对照：6 个相关 `EXISTS` 计数 | 已放弃的形态 | 6.72 s |
| 对照：逐条件 `INNER JOIN` 计数 | 已放弃的形态 | 3.99 s |

结论：高选择性筛选（业务常见组合）在 30 万量级为几十毫秒；**低选择性条件且不带时间窗或状态范围时，成本由排序与扫描命中行主导，为秒级**，必须由业务默认时间窗或状态收敛，平台不承诺该组合的交互时延。

## 7. 例外与剩余风险

- `WorkflowBusinessApplyApi` 新增一个方法：自定义实现该接口的代码需要补 `updateFilterAttributes`；`WorkflowApiSurfaceContractTest` 指纹已按新增接口同步更新。
- 平台不回读业务表：业务漏调用 `PUT` 会看到过期属性，筛选结果直接体现；平台不做自动同步，也不做定时对账。
- 未登记属性的申请（含存量数据）在属性筛选下不匹配；切换到属性筛选前必须回填。
- 属性按申请保存，同一业务键多轮申请各自保留本轮取值；需要「按项目当前值筛全部轮次」时必须由业务对该项目全部轮次逐条刷新，本次不提供跨轮次回刷。
- 低选择性条件 + 全量 + 深翻页为秒级（本机实测 3~4 s），平台不设默认时间窗；交互时延由业务页面自行收敛范围。
- 性能证据来自本机 MariaDB 11.8 合成数据，未在生产 MySQL 8 和真实 `baohan_prod` 上复核；真实库 `EXPLAIN` 与业务仓联调待执行。
- 未做「启动完整应用 + 真实 MySQL 8 + Flyway V1~V5 + HTTP 全链路」的真实场景验收：本机只有 MariaDB 11.8，无法执行使用 `utf8mb4_0900_ai_ci` 的存量迁移；该验收必须在 MySQL 8 环境执行，步骤见第 7 节待办。
- SpotBugs 与 Reactor 架构门禁未能在本机执行：SpotBugs 插件依赖在当前网络不可达；`mango-architecture-verification` 的 Enforcer 规则在本机实例化失败（`MangoArchitectureEnforcerRule.<init>`），两者均与本次改动无关，需由 CI 复核。
- `IWorkflowBusinessApplyService` 中的 `io.mango.common.result.R` 未使用导入是存量告警，未在本次清理。
- 行为变更：待办到达通知（`workflow.task.assigned`）此前固定在 `viewPath` 声明时仍跳 `workflow:task:detail`，现在声明 `customConfig.viewPath` 后跳业务路径。未声明 `viewPath` 的存量申请行为不变；不接受该变更的业务可去掉 `customConfig.viewPath` 或设置 `viewActionEnabled=false`。
- 默认通知只支持一个路由按钮；业务需要多按钮或 `EVENT` 类型动作时，必须关闭默认通知并自行发消息，本次不提供该扩展。

## 8. 决定复核

| 视角 | 结论 | 依据与不确定项 |
|---|---|---|
| 事实与用户视角 | 业务要的是「按版本（申请）筛选任务」且字段会持续增加，因此属性必须按申请存储、按业务自定义键写入 | Issue #1248 实测（`limit=2000 → total=158`，`limit=2 → total=6`）、#1231 真实库量级（2316 申请 / 最大类型 2010）；不确定项：真实生产数据量与属性键数量 |
| 技术与风险视角 | 属性子表 + 每条件 `IN` 子查询是唯一同时满足「字段可增」「多值」「可索引」「H2 可测」「不改引擎表」的形态；派生表与 `EXISTS` 形态经压测否决 | 30 万合成数据压测、H2 夹具；不确定项：生产 MySQL 8 优化器行为需 `EXPLAIN` 复核 |
| 验证与交付视角 | H2 集成测试 + 单元测试 + 文档 checker 覆盖语义与契约；性能只对高选择性组合承诺 | 205/50/4 用例通过；低选择性组合为秒级，必须靠业务收敛范围 |

- 被拒方案：`extension_json` + 生成列/函数索引（每加字段一次平台 DDL，且 H2 测试通道不支持 JSON 函数）、申请表加固定宽列（字段会变）、Flowable 变量过滤（实例结束无法更新、变量表无名字/值索引）、派生表 `OR` + `GROUP BY`（压测慢 30 倍）、逐条件 `EXISTS`（慢 60 倍）、业务传子查询/派生表（破坏模块边界）、搜索引擎（当前量级过度设计）。
- 评审选择：`AGENT_ONLY`。用户未指定外部同行评审；本记录中的分析是 Agent 角色隔离分析，不称为人工同行评审或审批。
- 需要人工确认：业务侧回填存量申请的责任人与时间；生产 MySQL 8 上的 `EXPLAIN` 复核；是否需要「按项目当前值筛全部轮次」的跨轮次回刷能力。
