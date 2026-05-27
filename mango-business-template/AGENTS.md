# Business Agent Entry

本文件是 Mango 业务项目模板入口。Mango PMO 是框架生态规范源，业务项目只补充领域规则。

## 1. 规范来源

- Mango baseline：引用当前项目依赖的 Mango PMO 版本。
- Business PMO：只维护当前业务领域规则，位于 `business-pmo/`。
- Business docs：只放需求、设计、Sprint 计划、交付台账和评审记录，位于 `business-docs/`。

## 2. 交付流程

进入正式开发、验证、提交或 PR 前必须完成：

1. 明确交付契约。
2. 建立交付台账。
3. 按任务类型加载 Mango baseline 和业务 PMO 规则。
4. 在任务 worktree 或任务分支内开发。
5. 执行后端、前端和台账验证。

## 3. 业务边界

- `backend/**` 归后端 owner。
- `frontend/**` 归前端 owner。
- `business-pmo/**` 归 PMO 或 Tech Lead。
- `business-docs/**` 归 PM、Tech Lead 和 QA。
- API、DB、权限、菜单和流程变化必须经过 Tech Lead 或模块 owner review。

## 4. AI 协作

AI 可以参与实现，但 owner 和验收责任不转移。前端使用 AI 修改后端代码时，必须由后端 owner review。
