# Business Agent Entry

本文件是 Mango 业务项目 Agent 入口。生成项目依赖 Mango 进行业务开发时，必须同时加载随项目带出的 Mango baseline 和当前业务 PMO。

## 1. 规范来源

- Mango baseline：`business-pmo/mango-baseline/`，来自当前项目依赖的 Mango PMO 快照。
- Business PMO：只维护当前业务领域规则，位于 `business-pmo/`。
- Business docs：只放需求、设计、Sprint 计划、交付台账和评审记录，位于 `business-docs/`。

业务 PMO 不能放宽 Mango baseline 中的模块、API、数据库、测试和交付规则。需要升级规则时，先升级 Mango 依赖版本，再同步 baseline 快照。

## 2. 交付流程

进入正式开发、验证、提交或 PR 前必须完成：

1. 明确交付契约。
2. 建立交付台账。
3. 执行 Mango baseline preflight。
4. 按任务类型读取 preflight 输出中的所有 `Must read` 文件原文。
5. 读取本业务 `business-pmo/README.md` 和相关领域规则。
6. 在任务 worktree 或任务分支内开发。
7. 执行后端、前端和台账验证。

推荐命令：

```bash
node business-pmo/mango-baseline/tools/pmo-preflight.mjs \
  --role dev \
  --phase develop \
  --task "<用户任务>" \
  --paths "<影响路径，逗号分隔>"
```

台账检查：

```bash
node business-pmo/mango-baseline/tools/delivery-contract-check.mjs \
  --design business-docs/plans/<plan>.md \
  --ledger business-docs/plans/<ledger>.md \
  --mode verify
```

## 3. Mango Baseline 版本

本 starter 携带的 baseline 快照来自 Mango commit：

```text
7bca6b8f
```

业务项目升级 Mango 版本时，应同时更新：

- Maven 依赖版本。
- npm 依赖版本。
- `business-pmo/mango-baseline/` 快照。
- 业务验证命令和交付台账。

## 4. 业务边界

- `backend/**` 归后端 owner。
- `frontend/**` 归前端 owner。
- `business-pmo/rules/**` 归 PMO 或 Tech Lead。
- `business-pmo/mango-baseline/**` 归 Mango baseline 升级任务，不接受业务需求内随意修改。
- `business-docs/**` 归 PM、Tech Lead 和 QA。
- API、DB、权限、菜单和流程变化必须经过 Tech Lead 或模块 owner review。

## 5. AI 协作

AI 可以参与实现，但 owner 和验收责任不转移。前端使用 AI 修改后端代码时，必须由后端 owner review。

正式交付的最终报告必须包含：

- 改动范围。
- 实际加载的 Mango baseline 文件。
- 实际加载的业务 PMO 文件。
- 执行的验证命令。
- 未验证项和风险。
- PMO 例外说明。
