# 标准交付记录

## 1. 元数据

- 任务 ID：Issue #1234
- 交付模式：STANDARD
- 需求影响：L2 - 新增公开 HTTP 接口与公开 Java API 方法，影响审批流转记录的读取契约；不改变数据库结构、不改变既有接口行为
- 方案风险：L2 - 跨 `mango-workflow-api`/`core`/`starter` 三模块，读取路径首次显式放弃平台行级权限判定，失败后果需要业务侧补偿控制
- 最终风险：L2
- 工作区决策：CREATE（`/Users/hardy/Work/mango-issue-1234`，分支 `feat/issue-1234-trace-by-business-key`，workspace slot 47）

## 2. 目标与范围

- 目标：业务侧不注册 `WorkflowBusinessApplyDataPermissionProvider`、不直接读 `workflow_task_record`，也能按业务键读取完整流转记录（发起/认领/通过/退回/驳回）。
- 成功条件：非申请人能读到 `START`/`CLAIM`/`COMPLETE`/`RETURN` 且不返回 `3645`；多轮退回顺序稳定；重新申请的多个流程实例全部返回；跨租户与未登记申请返回空集合；响应不含业务字段与处理变量。
- 处理范围：`mango-workflow-api`（Query 与方法契约）、`mango-workflow-core`（服务、Mapper、转换器、测试）、`mango-workflow-starter`（端点）、workflow README、业务接入指南、能力地图。
- 不处理范围：网关/条件分支节点（`ACT_HI_ACTINST`）、撤回 `WITHDRAW`（`workflow_business_apply_status_log`）、数据范围与 `workflow:task:all`、`processDetail` 行为、数据库结构。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-1 | 业务详情页 / `POST /workflow/tasks/trace-by-business-key` | 已登记业务申请的 `businessType` + `businessKey`，未注册任何 Provider | 返回该业务键全部流程实例的流转记录，含 `START`/`CLAIM`/`COMPLETE`/`RETURN`/`WITHDRAW` | 不做申请级判定，不返回 `3645` | 非申请人调用能拿到发起与认领记录；撤回后时间轴出现 `WITHDRAW` |
| AC-2 | 同上 | 同一 `businessKey` 两个流程实例 | 两个实例的记录全部返回，可用 `processInstanceId` 分组 | 无命中返回空集合 | 两条 `START` 都在结果内 |
| AC-3 | 同上 | 多轮退回（同一 `taskDefinitionKey` 重复出现） | 按 `createdTime` 升序、同一时间按 `id` 升序 | — | 顺序稳定、可复现 |
| AC-4 | 同上 | `operatorIds`（业务 `user_id`） | 同时匹配 `operatorId` 与经 `identity_user.username` 解析出的 `operatorName` | 传空表示不过滤 | 只写入名称的记录也能命中 |
| AC-5 | 同上 | `deptIds` | 按 `workflow_business_apply.applicant_dept_id` 过滤整条流程实例 | 传空表示不过滤 | 不匹配的实例整体不出现 |
| AC-6 | 同上 | `actions` 含未知动作码 | 拒绝并提示未知动作 | `TASK_QUERY_INVALID` | 不静默返回空集合 |
| AC-7 | 同上 | 跨租户 `businessKey` | 不返回其他租户记录 | 返回空集合 | 租户插件生效 |
| AC-8 | 同上 | 记录数超过 `unpaged-max-rows` | 明确失败，不静默截断 | `TASK_QUERY_INVALID`，提示缩小时间区间 | 不返回被截断的列表 |
| AC-9 | 同上 | 任意命中记录 | 响应只含 11 个任务级字段 | — | `variables` 恒为 null，无审计列/租户列/业务字段 |
| AC-10 | 业务单体注入 `WorkflowTaskRuntimeApi` | 本地 Java 调用 | 与 HTTP 入参同一模型、同一结果 | Bean Validation 生效 | 编译通过并返回相同结果 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-1 | AC-1 | 新增 `WorkflowTaskRuntimeApi#traceByBusinessKey(@Valid WorkflowTaskTraceRequest)`，同时提供 REST 端点；`WorkflowTaskTraceQuery` 位于 `io.mango.workflow.api.query`，`WorkflowTaskTraceRequest` 继承它承载 POST body（架构门禁 `MANGO-ARCH-ADAPTER-007` / `MANGO-ARCH-OPENAPI-004` 要求请求体为 Request 类型） | `WorkflowTaskRuntimeApi`、`WorkflowTaskTraceQuery`、`WorkflowTaskTraceRequest`、`WorkflowTaskController` | 移除方法与端点，不影响既有接口 |
| D-2 | AC-1 | 不调用 `WorkflowBusinessApplyAccessChecker`，不校验 `workflow:task:all`；只保留 `workflow:task:list` 与租户隔离 | `WorkflowTaskBusinessQueryService` | 回退服务实现 |
| D-3 | AC-2 | 两步查询：先用 `workflow_business_apply` 按租户+业务类型+业务键（可选部门）定位 `process_instance_id`，再查 `workflow_task_record`；不做 join | `WorkflowTaskBusinessQueryService` | 回退服务实现 |
| D-11 | AC-1 | 撤回不写 `workflow_task_record`；读取端按实例合并 `workflow_business_apply_status_log` 的 `WITHDRAW` 行（`action=WITHDRAW`、`actionName=撤回`、`variables=null`），覆盖升级前已撤回的历史申请；不改表结构、不改 `processDetail` | `WorkflowTaskBusinessQueryService`、`WorkflowTaskRecordConverter`、`WorkflowTaskAction` | 移除合并逻辑与枚举值 |
| D-4 | AC-1/AC-9 | 记录查询使用字段投影，排除 `variables_json`；`WorkflowTaskRecordVO.variables` 恒为 null | `WorkflowTaskBusinessQueryService` | 回退投影字段 |
| D-5 | AC-9 | 抽取 `WorkflowTaskRecordConverter`，`processDetail` 与流转记录共用同一映射；变量由调用方决定是否传入 | `WorkflowTaskRecordConverter`、`WorkflowTaskRuntimeService` | 回退转换器调用 |
| D-6 | AC-4 | 新增 `WorkflowTaskHistoryMapper#selectUserNamesByIds`，按 `identity_user.username` + `tenant_member` 解析登录名 | `WorkflowTaskHistoryMapper` 与 XML | 回退 Mapper 方法与 SQL |
| D-7 | AC-8 | 查询带 `limit + 1`，超过 `unpaged-max-rows` 抛 `TASK_QUERY_INVALID`，不静默截断 | `WorkflowTaskBusinessQueryService` | 调整上限判断 |
| D-8 | AC-1 | 租户由 MyBatis-Plus 插件注入 `workflow_task_record.tenant_id`；`workflow_business_apply` 侧显式按租户过滤（与 `pageByBusinessKey` 一致） | `WorkflowTaskBusinessQueryService` | 回退过滤条件 |
| D-9 | AC-10 | 约束声明在 `WorkflowTaskTraceQuery` 字段上；`WorkflowTaskController` 类级 `@Validated` 保证本地调用同样校验 | `WorkflowTaskTraceQuery`、`WorkflowTaskController` | 回退注解 |
| D-10 | AC-1 | `WorkflowApiSurfaceContractTest` 的 API/HTTP 指纹常量随新增方法更新，指纹测试继续拦截后续非预期契约变更 | `WorkflowApiSurfaceContractTest` | 回退常量与接口 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| I-1 | D-1 | 1 | `query/WorkflowTaskTraceQuery.java`、`WorkflowTaskRuntimeApi.java` | 编译通过，契约含中文 `@Schema` 与约束 |
| I-2 | D-3/D-4/D-7/D-8 | 2 | `IWorkflowTaskBusinessQueryService`、`WorkflowTaskBusinessQueryService` | 单测覆盖校验、空集合、超限、变量为空 |
| I-3 | D-5 | 3 | `convert/WorkflowTaskRecordConverter`、`WorkflowTaskRuntimeService` | 既有 `processDetail` 测试不回归 |
| I-4 | D-6 | 4 | `WorkflowTaskHistoryMapper`、`mapper/workflow/WorkflowTaskHistoryMapper.xml` | 集成测试覆盖登录名匹配 |
| I-5 | D-1 | 5 | `WorkflowTaskController` | `verify` 架构门禁通过 |
| I-6 | AC-1~AC-10 | 6 | 集成测试、README、接入指南、能力地图 | 文档门禁与测试通过 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-1/AC-2/AC-3 | M11 集成测试 | `mvn -o -f mango/pom.xml -pl :mango-workflow-core -Dtest=WorkflowTaskTraceIntegrationTest test` | PASS（7 个用例） | `mango-workflow-core/target/surefire-reports` |
| AC-4/AC-5/AC-6/AC-7/AC-8/AC-9 | M11 集成测试 + M10 单元测试 | 同上；`-Dtest=WorkflowTaskBusinessQueryServiceTest test` | PASS（23 个用例） | 同上 |
| AC-10 | M09 静态验证 | `mvn -o -f mango/pom.xml -pl :mango-workflow-api,:mango-workflow-core,:mango-workflow-starter verify` | PASS（46 个 starter 用例含 API 指纹、架构门禁） | 构建输出 |
| AC-1~AC-9 | M11 回归 | `mvn -o -f mango/pom.xml -pl :mango-workflow-core test` | PASS（159 个用例，4 skipped） | `mango-workflow-core/target/surefire-reports` |
| AC-1/AC-2/AC-3/AC-4/AC-5/AC-6/AC-7/AC-9 | M12 入口级 + 真实 MySQL | `mango dev start backend`（18047，MySQL 8.4.8）+ 真实审批链路：发起 → 认领 → 通过 → 复审认领 → 退回 → 重新认领 → 通过 → 复审通过，再调 `POST /workflow/tasks/trace-by-business-key` | PASS（9 条记录，动作顺序 START/CLAIM/COMPLETE/CLAIM/RETURN/CLAIM/COMPLETE/CLAIM/COMPLETE） | `real-mysql-acceptance.md`、`real-mysql-trace-single-instance.json` |
| AC-2 | M12 真实 MySQL | `reapplyFromApplyId` 创建第二个实例后再次调用 | PASS（10 条、2 个 `processInstanceId`、2 条 `START`） | `real-mysql-trace-multi-instance.json` |
| AC-4/AC-5/AC-6/AC-7 | M12 真实 MySQL | `operatorIds=[1]/[2]`、`deptIds=[1]/[999]`、`actions=[START]/[RETURN]/[RETRUN]`、未知业务键 | PASS（命中 9/10 条、未命中 0 条、未知动作 `3659`、空集合不报错） | `real-mysql-acceptance.md` |
| AC-9/D-4/D-8 | M12 真实 SQL 回读 | 后端 SQL 日志 | PASS（投影排除 `variables_json`，`tenant_id = 1` 由插件注入，`LIMIT 2001` 探测） | `real-mysql-trace-sql.txt` |
| AC-1 撤回 | M12 真实 MySQL | 新建申请 → 启动 → `POST /workflow/processes/withdraw` → 查流转记录 | PASS（`WITHDRAW` 行含处理人/意见/时间；`actions=[WITHDRAW]` 命中 1 条；历史已撤回实例也能返回） | `real-mysql-trace-withdraw.json`、`real-mysql-withdraw-row.txt` |
| AC-1 权限 | M12 真实 HTTP | 无 `workflow:task:list` 的租户成员调用；无效 token 调用 | PASS（403 Access denied / 401 Unauthorized）；资源注册为 `POST /workflow/tasks/trace-by-business-key` + `workflow:task:list` + `PERMISSION` | `real-mysql-http-contract.txt` |
| 兼容 | M12 真实 MySQL | `GET /workflow/tasks/process-detail` | PASS（9 条记录，`variables_json` 仍在查询字段内，审批页数据源未变） | `real-mysql-acceptance.md` 结果 5 |
| 文档 | 结构检查 | 人工核对 `README` §6.2、接入指南 §7.2、能力地图 | PASS | 本记录所在提交 |

未执行：前端 UI 验收（M13）。本仓没有消费 `traceByBusinessKey` 的页面，业务面板在 `mango-baohan-risk`；当前环境无浏览器工具，未做截图或点击验收。

## 7. 例外与剩余风险

- 网关/条件分支节点与撤回 `WITHDRAW` 不在本接口返回范围，需要另立项：网关需基于 `ACT_HI_ACTINST` 与新 VO，撤回需合并 `workflow_business_apply_status_log`。
- 本接口无行级权限判定：持有 `workflow:task:list` 且知道 `businessType + businessKey` 的账号可读全部流转记录与审批意见原文；本地注入 API 调用不经过 HTTP 过滤器。业务侧已书面接受，补偿控制为详情页 `baohan:riskcontrol:audit:detail` 与服务端 `versionId → businessKey` 解析。
- `operatorIds` 只匹配 `user_id` 与其登录名；写入时没有用户上下文的系统动作（`operator_id` 为空、`operator_name` 为 `anonymous`）不会命中过滤，属于既定行为。
- 历史数据 `tenant_id` 默认值为 `1`，非 1 租户的旧记录会被租户隔离挡住，只影响老安装。
- `CHANGELOG.md` 未修改：本仓 CHANGELOG 由发布流程生成，未发布特性不在此登记（参照 #1232）。
