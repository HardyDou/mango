---
documentType: technical-design
deliveryLevel: L5
systemKind: NEW
---

# 供应商准入系统技术设计

## 设计依据

- 系统需求：SR-001、SR-002。
- 规范：`rules/backend/05-module.md@1.4.0`、`rules/backend/03-api.md@1.4.0`；采用：模块边界、协议模型和错误响应。
- 代码：`code-templates/business-module@commit:71cb8e4`；采用：复用 api/core/starter/starter-remote 模块骨架。

## 技术决定

1. TD-001 -> SR-001：邀请使用数据库保存的随机令牌摘要，不使用可离线验证 JWT；原因是必须支持单次消费和立即失效；提交事务通过令牌条件更新、申请版本写入和任务创建保证只形成一个申请。
2. TD-002 -> SR-002：审核编排复用 workflow core 并由供应商 core 保存业务状态；法务、财务任务以同一业务版本并行创建，聚合事件只在两项通过时推进准入，驳回事件记录受影响字段组。

## 实现设计

- 架构与模块：`supplier-api` 定义命令和查询协议；`supplier-core` 拥有邀请、申请、资料版本和准入状态；`supplier-starter` 注册实现并适配 workflow；`supplier-starter-remote` 只提供远程协议适配。
- 文件或符号：新增 `SupplierInvitationService`、`SupplierApplicationService`、`SupplierReviewCoordinator` 及对应 repository、API 和 migration。
- 条件设计：提交接口接收令牌和资料，不接受租户/供应商 ID；服务端由令牌解析归属。资料按版本不可变保存，银行账号加密且查询时脱敏。workflow 事件以 `applicationId + version + reviewType` 幂等。

## 技术字典

- 枚举/状态：`DRAFT=10`、`PURCHASE_REVIEW=20`、`PARALLEL_REVIEW=30`、`CORRECTION=40`、`ADMITTED=50`；只允许按 SRS 状态图流转，`ADMITTED` 为终态。
- 字段：`supplier_invitation.token_hash char(64)` 必填且唯一；`expires_at timestamp` 必填；`used_at timestamp` 可空；`supplier_application.version int` 从 1 递增；`bank_account_ciphertext varchar(512)` 必填、敏感、来源为供应商提交。
- 其它：错误码 `SUPPLIER_INVITATION_INVALID/SUPPLIER_DOCUMENT_EXPIRED/SUPPLIER_REVIEW_CONFLICT`；权限 `supplier:invite/create`、`supplier:purchase/review`、`supplier:legal/review`、`supplier:finance/review`；事件 `SUPPLIER_REVIEW_COMPLETED`。

## 测试与回滚

1. VAL-001 -> SR-001：core 并发测试同时消费同一邀请，断言仅一次条件更新成功、仅一个申请和采购任务；失败保存数据库断言并阻止合并。
2. VAL-002 -> SR-002：集成测试发送重复及乱序审核事件，断言状态只合法前进、字段权限不泄露银行信息、审计包含版本和审核类型。
- 切换与回滚：新模块默认不开菜单和邀请权限；migration 只新增表。异常时关闭入口和事件订阅，删除未进入审核的测试申请；已审核数据保留只读，不回退状态。
