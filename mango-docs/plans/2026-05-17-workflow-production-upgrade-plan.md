# Workflow Production Upgrade Plan

更新时间：2026-05-17

## 执行规则

- P0 / P1 按任务逐个闭环：开发 1 个、验证 1 个、提交 1 个、尝试 push 1 个、清理状态后继续下一个。
- push 如果受网络阻塞，记录本地提交和失败原因，不伪造已推送状态。
- 参数校验统一使用 `io.mango.common.result.Require`。

## 任务拆分

| 优先级 | 任务 | 验收标准 | 状态 |
|---|---|---|---|
| P0 | 业务示例列表显示当前审批节点 | 业务侧列表/详情返回最新流程实例、流程状态、当前任务名称和节点 key；无工作流模块时业务列表不失败 | 已完成 |
| P0 | 业务示例改为真实业务列表驱动 | 前端业务示例从业务接口读取列表，发起后业务单与流程实例关联，避免只从我的发起流程反推 | 未开始 |
| P0 | 详情审批流程图按真实 tree 渲染状态 | 经过、当前、未经过节点用不同状态标识，支持分支/并发结构显示 | 未开始 |
| P0 | 审批页布局和动作统一 | 左主右记录、底部固定动作区，统一“驳回”命名，审批记录时间线 | 未开始 |
| P1 | 自定义 Word 表格式审批页面案例 | 合同用印类案例使用自定义申请/审批页面，支持节点权限只读/编辑/隐藏 | 未开始 |
| P1 | 表单设计和渲染组件补齐 | 开源 form-create 基础/子/辅助/布局组件与公共业务组件可设计、可预览、可渲染 | 未开始 |
| P1 | 任务管理页面筛选和列优化 | 待办/已办/我发起等页面筛选项、列字段符合办公审批实际使用 | 未开始 |
| P1 | 协同办公菜单图标补齐 | 协同办公下所有菜单有清晰图标 | 未开始 |

## 当前闭环记录

- 当前任务：P0 业务示例列表显示当前审批节点。
- 后端方案：新增工作流窄接口，业务模块按业务主键查询最新流程状态并填充列表/详情 VO。
- 已完成验证：
  - `mvn -pl :mango-guarantee-core -am -Dtest=GuaranteeCaseServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - `mvn -pl :mango-guarantee-api,:mango-guarantee-core,:mango-workflow-api,:mango-workflow-core -am -DskipITs -DskipTests compile`
  - `git diff --check`
  - `rg "throw new IllegalArgumentException|Objects\\.requireNonNull|new IllegalStateException|requireText\\(" mango/mango-platform/mango-guarantee mango/mango-platform/mango-workflow -n`
