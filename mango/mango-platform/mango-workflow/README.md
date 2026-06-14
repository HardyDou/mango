# Mango Workflow

## 1. 能力定位

`mango-workflow` 提供通用流程编排、审批任务、流程定义、流程模板、业务申请中心、任务处理和流程进度查询能力。主要使用者是需要接入审批流的业务模块和管理端流程配置功能。

代码事实：

- 聚合模块 `io.mango.platform.workflow:mango-workflow`。
- 子模块包括 `mango-workflow-api`、`mango-workflow-core`、`mango-workflow-starter`。
- Controller 路径覆盖 `/workflow/business-applies`、`/workflow/tasks`、`/workflow/template-categories`、`/workflow/processes`、`/workflow/templates`、`/workflow/definitions`、`/workflow/categories`。
- 自动配置 `WorkflowAutoConfiguration` 默认匹配启用。

## 2. 适用场景

- 配置流程分类、流程定义、节点定义和流程版本。
- 管理流程模板、模板分类、从模板生成定义、导入和发布模板。
- 发起流程、处理待办、驳回、暂存、转办、加签、签收和取消签收。
- 查询业务申请进度、当前任务、历史任务和抄送任务。
- 业务模块需要统一审批运行时，但业务主数据仍由业务模块保存。

## 3. 不适用场景

- 不保存完整业务单据主表和不可变业务快照。
- 不替代业务模块的状态机、业务校验和审批完成回写。
- 不作为任务调度引擎使用，定时任务归属 `mango-job`。
- 不把所有业务字段都写入流程变量。

## 4. 模块边界

Workflow 保存流程定义、流程版本、流程实例、待办、审批记录、业务申请关系、节点权限和必要变量。业务模块保存业务主表、业务快照、业务附件关系和审批完成后的领域状态。

## 5. 接入方式

本地工作流服务接入：

```xml
<dependency>
    <groupId>io.mango.platform.workflow</groupId>
    <artifactId>mango-workflow-starter</artifactId>
</dependency>
```

只使用契约模型时依赖：

```xml
<dependency>
    <groupId>io.mango.platform.workflow</groupId>
    <artifactId>mango-workflow-api</artifactId>
</dependency>
```

业务模块通过 `WorkflowBusinessApplyApi` 和 `WorkflowBusinessProcessApi` 查询流程申请和业务进度。

## 6. 配置项

已发现配置前缀：

- `mango.workflow.enabled`：工作流自动配置开关，默认匹配启用。
- `mango.workflow.samples`：示例数据配置，来源 `WorkflowSampleProperties`。

## 7. 对外接口 / 扩展点

- API：`WorkflowBusinessApplyApi`、`WorkflowBusinessProcessApi`。
- Controller：`WorkflowBusinessApplyController`、`WorkflowTaskController`、`WorkflowTemplateCategoryController`、`WorkflowProcessController`、`WorkflowTemplateController`、`WorkflowDefinitionController`、`WorkflowCategoryController`。
- 命令对象覆盖定义、模板、任务、发起、通过、驳回、转办、加签、签收、暂存、导入和发布。
- 节点执行器包括事件发布、Spring Bean、noop，以及预留/受限的 HTTP URL、远程服务执行器；生产使用前需要结合网络边界、超时和安全策略验收。

## 8. 数据库 / 初始化数据

Flyway 路径：`mango-workflow-core/src/main/resources/db/migration/workflow`。

核心表：

- Flowable 7 官方 `ACT_*` 引擎表和属性表。
- `workflow_category`
- `workflow_definition`
- `workflow_template_category`
- `workflow_template`
- `workflow_node_definition`
- `workflow_definition_version`
- `workflow_form_instance`
- `workflow_task_record`
- `workflow_copied_task`
- `workflow_business_apply`
- `workflow_business_apply_current_task`
- `workflow_business_apply_status_log`

`V1__init_workflow.sql` 还包含默认流程分类、模板分类和节点定义初始化数据。`V2__workflow_domain.sql` 增加 `domain_code` 及相关索引。

## 9. 菜单 / 权限 / 租户

流程定义、模板、分类和任务接口属于 workflow 能力资产。当前迁移包含 workflow 菜单/权限初始化事实；新增或治理时应按模块菜单规范通过 workflow 资源清单或 migration 维护。业务申请和任务查询需要结合当前用户、租户和业务类型过滤；具体业务单据权限仍由业务模块负责。

## 10. 验证方式

最小验证命令：

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-workflow -am test
```

代表性验收：

- 创建流程定义并部署版本。
- 发起业务流程后生成业务申请和当前任务。
- 待办接口能返回当前处理人任务。
- 完成或驳回任务后，状态流水和历史任务可查询。
- 完整集成验收需要真实数据库，覆盖部署版本、发起流程、完成任务和业务进度查询。

## 11. 业务接入最小闭环

业务单据接入 workflow 时，业务模块先保存业务主表、申请快照和附件关系，再以 `businessType`、`businessKey`、申请人和必要流程变量发起流程。workflow 保存流程申请、任务和进度，业务最终状态仍由业务模块在审批完成事件或回调中更新。

最小验收链路：部署流程版本，发起业务申请得到 `applyId` 和流程实例，待办用户完成或驳回任务，业务列表通过 `WorkflowBusinessProcessApi` 批量查询进度。驳回后再次申请应创建新申请或新实例，不覆盖旧审批历史。

## 12. 常见问题

- 业务列表要显示审批进度时，优先通过 `WorkflowBusinessProcessApi` 批量查询，不要复制工作流表结构。
- 驳回后再次申请应创建新申请和新流程实例，旧实例保留历史状态。
- 流程变量只保存流程判断和通用展示所需字段，复杂业务快照留在业务模块。

## 13. 关联 PMO 规则

- [后端 API 规范](../../../mango-pmo/rules/backend/03-api.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [持久化规范](../../../mango-pmo/rules/backend/07-persistence.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
