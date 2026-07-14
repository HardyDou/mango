# Workflow 架构债务治理基线对比报告

## 1. 结论

改前生产提交为 `6b72e4a28`，任务分支为 `refactor/workflow-architecture-debt`。修复测试基础设施并补充契约测试后，在不修改生产代码的前提下建立了 43 条全绿基线（Core 37、Starter 6）；治理后同一组测试加上迁移、元数据和资源分层回归共 48 条（Core 40、Starter 8），全部通过。

定向架构债务由 845 条降为 0：Dependency 0、ArchUnit 0、PMD 0。验证范围仅为 Workflow 四个 Maven 子模块及其架构门禁，不代表全仓检查。

最新 main 的通用静态门禁会按文件身份关联历史问题。服务与实体重命名后，旧 Workflow 身份无法继续匹配；本次按最终代码重新登记该模块静态身份，Workflow 通用静态库存由 989 条降为 511 条，门禁结果为 `newIssueCount=0`、`toolFailureCount=0`。这 511 条仍是递减基线，不计入已清零的 Dependency/ArchUnit/PMD 正式架构债务，也不允许后续新增。

新 MySQL 8.4 数据库的完整单体应用启动成功，`/actuator/health` 返回 `UP`。Workflow 只有 `V1__init_workflow.sql`，成功建立 12 张业务表；Flyway 不写 `ACT_GE_PROPERTY`，引擎启动前的正式初始化器按缺失项登记 Flowable 必需元数据，随后 Flowable 正常建立引擎。默认启动时示例流程为 0 条。

## 2. 改前与改后对比

| 指标 | 改前基线 | 改后 | 结论 |
|---|---:|---:|---|
| Core 测试 | 37/37 | 40/40 | 通过；新增 V1、metadata、租户拦截保护 |
| Starter 测试 | 6/6 | 8/8 | 通过；新增完整 API/Controller 与 Demo 声明保护 |
| 合计 | 43/43 | 48/48 | 全绿 |
| Dependency 债务 | 0 | 0 | 无回归 |
| ArchUnit 债务 | 119 | 0 | 清零 |
| PMD 正式债务 | 726 | 0 | 清零 |
| 通用静态身份库存 | 989 | 511 | 净减少 478；新问题 0 |
| Workflow Flyway | V1-V4，含引擎数据 | 单一纯 DDL V1 | 符合新库政策 |
| 默认 Demo | 启动代码默认写入 3 套 | 0 | 默认环境无演示数据 |

## 3. 兼容边界与有意调整

- 冻结并验证流程定义、发起、申请、业务流程、任务运行时、分类、模板分类、模板共 8 个公共 API。
- 冻结并验证全部 `/workflow/**` Controller 的 path、verb、binding、权限和响应包装；业务流程查询从错误归属的 Process Controller 拆到独立 Business Process Controller，但 HTTP 契约保持。
- 保持完成、结果完成、驳回、退回、暂存、转办、加签、认领、释放和抄送已阅的状态、副作用、当前任务快照及领域事件顺序。
- Service 层不再返回 `R`，Controller 统一包装；持久化类统一 `Entity`、Mapper 注解和 CrudService 继承，实现类进入 `impl` 且移除 `Impl` 后缀。
- 复合 JSON 参数改为不可变值对象。回归测试捕获并修复了历史共享 Map 副作用：`applyId` 现在被显式写回启动变量和业务申请持久化变量，外部结果保持一致。

## 4. 数据库与资源验证

正式库 `mango_workflow_app_verify_20260714` 的实际结果：

- `flyway_schema_history_workflow` 仅 baseline 0 和成功的 V1；发布 jar 中仅包含 V1。
- Workflow 表 12 张；`tenant_id` 为 `varchar(64)`；最终结构包含 `org_id`、`domain_code`、`start_entry_visible` 和当前任务认领/候选字段。
- V1 执行后 `ACT_GE_PROPERTY=0`；引擎启动后正式初始化器补齐 11 个缺失项，Flowable 自身再登记 2 个配置项，总数 13。
- `next.dbid` 只在缺失时初始化，不覆盖已有值；单元测试覆盖“空表插入”和“已有值不覆盖”。
- 默认正式启动 `workflow_definition=0`，确认 Demo 不再隐式进入正式库。
- Demo 声明加载器定向验证得到 147 条全项目 Demo 声明，其中 Workflow 恰为 `expense_reimbursement`、`contract_seal_approval`、`leave_application` 3 条；三条均为 `INIT_ONLY`，设计器与表单资产可读取且结构合法。

完整应用启动时额外发现并修复两项仅在真实环境暴露的问题：增量构建残留会把已删除 V2-V4 重新打入 jar，因此发布验证固定使用 clean 打包并检查 jar；`ACT_GE_PROPERTY` 不是租户表，专用 Mapper 已明确关闭租户拦截，避免生成不存在的 `tenant_id` 条件。

## 5. 验证入口

```bash
mvn -f mango/pom.xml \
  -pl :mango-workflow-api,:mango-workflow-core,:mango-workflow-starter,:mango-workflow-starter-remote \
  test

mvn -f mango/pom.xml \
  -pl :mango-workflow-api,:mango-workflow-core,:mango-workflow-starter,:mango-workflow-starter-remote,:mango-architecture-verification \
  verify -DskipTests \
  -Dmango.architecture.base=origin/main \
  -Dmango.architecture.requireFullReactor=false \
  -Dmango.check.baseRef=origin/main
```

新库启动使用 `mango-monolith-app`、独立 MySQL 数据库、关闭本机未安装的文件预览组件并提供测试 SM4 密钥。健康检查为 HTTP 200/UP；上述环境参数只用于本地验收，未写入仓库。
