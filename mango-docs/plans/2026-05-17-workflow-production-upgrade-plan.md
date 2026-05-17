# Workflow Production Upgrade Plan

更新时间：2026-05-17

## 执行规则

- P0 / P1 按任务逐个闭环：开发 1 个、验证 1 个、提交 1 个、尝试 push 1 个、清理状态后继续下一个。
- push 如果受网络阻塞，记录本地提交和失败原因，不伪造已推送状态。
- 参数校验统一使用 `io.mango.common.result.Require`。

## 最新设计决策

- 增加 `Workflow Business Apply Center`，统一管理业务申请与流程实例关系。
- Apply Center 管申请记录、状态、当前节点、历史申请、渲染入口和快照引用；业务模块仍管理业务主数据、完整业务快照和审批结果落库。
- 申请/审批主流程固定，业务内容支持 `DYNAMIC_FORM` 和 `CUSTOM_PAGE` 两种渲染模式。
- 业务列表展示工作流列时，通过 Apply Center 批量补齐状态、当前节点、当前处理人；高频业务可订阅 outbox 事件冗余到业务表。
- 不引入 MQ，继续基于 `infra-kv outbox` 做事件发布订阅。

## 任务拆分

| 优先级 | 任务 | 验收标准 | 状态 |
|---|---|---|---|
| P0 | Apply Center 后端基础模型与接口 | 新增申请、当前任务、状态流水模型；支持创建申请、分页查询、最新进度批量查询、申请历史查询 | 已完成 |
| P0 | 流程启动/审批流转接入 Apply Center | 发起流程生成 apply 关系；通过/驳回/结束同步申请状态、当前节点、状态流水 | 已完成 |
| P0 | 前端业务列表接入 Apply Center | 业务列表通过批量进度接口显示审批状态、当前节点、处理人；详情展示历史申请与流程图 | 已完成 |
| P0 | 渲染模式接入 | 申请/审批固定外壳，根据 `DYNAMIC_FORM` / `CUSTOM_PAGE` 渲染动态表单或业务自定义页面 | 已完成 |
| P0 | 详情审批流程图按真实 tree 渲染状态 | 经过、当前、未经过、驳回节点用不同状态标识，支持分支/并发结构显示 | 已完成 |
| P1 | 自定义 Word 表格式审批页面案例 | 合同用印类案例使用自定义申请/审批页面，支持节点权限只读/编辑/隐藏 | 未开始 |
| P1 | 表单设计和渲染组件补齐 | 开源 form-create 基础/子/辅助/布局组件与公共业务组件可设计、可预览、可渲染 | 未开始 |
| P1 | 任务管理页面筛选和列优化 | 待办/已办/我发起等页面筛选项、列字段符合办公审批实际使用 | 未开始 |
| P1 | 协同办公升级为审批中心信息架构 | 协同办公审批相关菜单统一为审批中心口径，入口、标题、图标和路由命名清晰，不改变底层固定审批流程 | 未开始 |
| P1 | 协同办公菜单图标补齐 | 协同办公下所有菜单有清晰图标 | 未开始 |
| P1 | README 接入文档 | 后端 workflow README 写清业务如何接入 Apply Center、快照、事件、动态表单和自定义页面 | 未开始 |

## 当前闭环记录

- 已完成任务：业务示例列表显示当前审批节点。
- 已完成验证：
  - `mvn -pl :mango-guarantee-core -am -Dtest=GuaranteeCaseServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
  - `mvn -pl :mango-guarantee-api,:mango-guarantee-core,:mango-workflow-api,:mango-workflow-core -am -DskipITs -DskipTests compile`
  - `git diff --check`
  - `rg "throw new IllegalArgumentException|Objects\\.requireNonNull|new IllegalStateException|requireText\\(" mango/mango-platform/mango-guarantee mango/mango-platform/mango-workflow -n`
- 已完成任务：P0 Apply Center 后端基础模型与接口。
- 已完成验证：
  - `mvn -pl :mango-workflow-api,:mango-workflow-core,:mango-workflow-starter -am -DskipITs -DskipTests compile`
  - `git diff --check`
  - `rg "throw new IllegalArgumentException|Objects\\.requireNonNull|new IllegalStateException|requireText\\(" mango/mango-platform/mango-workflow -n`
- 已完成任务：P0 流程启动/审批流转接入 Apply Center。
- 已完成验证：
  - `mvn -pl :mango-workflow-api,:mango-workflow-core,:mango-workflow-starter -am -DskipITs -DskipTests compile`
  - `git diff --check`
  - `rg "throw new IllegalArgumentException|Objects\\.requireNonNull|new IllegalStateException|requireText\\(" mango/mango-platform/mango-workflow -n`
- 已完成任务：P0 前端业务列表接入 Apply Center。
- 已完成验证：
  - `pnpm -F mango-admin build`
  - `git diff --check`
- 已完成任务：P0 渲染模式接入。
- 已完成验证：
  - `mvn -pl :mango-workflow-api,:mango-workflow-core,:mango-workflow-starter -am -DskipITs -DskipTests compile`
  - `pnpm -F mango-admin build`
  - `git diff --check`
  - `rg "throw new IllegalArgumentException|Objects\\.requireNonNull|new IllegalStateException|requireText\\(" mango/mango-platform/mango-workflow -n`
- 已完成任务：P0 详情审批流程图按真实 tree 渲染状态。
- 已完成验证：
  - `pnpm -F mango-admin build`
  - `git diff --check`
- 当前任务：P1 自定义 Word 表格式审批页面案例。
