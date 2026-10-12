# Issue #1010 前端质量门禁第一阶段治理记录

## 1. 决定与范围

- Issue：[#1010](https://github.com/HardyDou/mango/issues/1010)
- 目标：恢复真实 PR 前端质量检查；第一阶段默认只给建议，不因历史静态债务阻断 PR。
- 阻断范围：仅受影响 workspace 的递归生产构建失败。构建失败表示受影响前端无法产出可运行构件。
- 建议范围：ESLint、Prettier、Stylelint、TypeScript、单测、架构/边界、组件契约、Catalog、运行时配置、E2E 选择器、包导出和消费者类型检查失败均记录为 advisory。
- 不处理：本任务不清理 23 条存量 ESLint 诊断，不抬高 `quality-baseline.json`，不修改业务页面和测试业务逻辑。
- 交付模式：FULL；需求影响 L3（修改主仓 PR/CI 门禁语义），方案风险 L3（错误分流可能放行不可构建前端）。
- 工作区：M01=CREATE，`/Users/hardy/Work/mango-issue-1010`，分支 `fix/issue-1010-frontend-quality-gate`。
- 启用措施：M01、M07、M09、M10、M14；M15 待真实 PR check-run 产生后再回读，当前不宣称远端保护已验证。

## 2. 技术决定

1. `.github/workflows/frontend-quality.yml` 的 `frontend-pr-quality` 恢复 checkout、依赖安装和 `pnpm check:pr`。
2. `check-affected.mjs` 在 `--profile=pr` 下继续执行全部 PR 计划；非构建失败继续执行并记录 `advisories`。
3. `pnpm --filter ... -r build` 失败立即返回失败并阻断 PR。
4. main push 和手动 workflow 继续使用原有深度检查及 P0 浏览器验收，不降低主干质量信号。
5. 质量基线仍按原规则只减不增；本任务不使用基线更新绕过 Issue #1010。

## 3. 三视角复核

| 视角       | 当前事实                                                                         | 结论                                                                        | 未解决项                                                         |
| ---------- | -------------------------------------------------------------------------------- | --------------------------------------------------------------------------- | ---------------------------------------------------------------- |
| 事实与用户 | PR 当前 job 只输出暂停 notice；Issue #1010 记录 23 条主分支已有 ESLint identity  | 必须恢复真实 PR 检查，同时避免存量问题阻断无关 PR                           | 真实 GitHub PR 的执行时间和 advisory 数量需回读                  |
| 技术与风险 | `check:pr` 已有受影响 workspace 构建；静态/类型/测试失败不等价于无法产出运行构件 | 仅 build 失败阻断，其它失败保留报告；deep/main 语义不变                     | 构建命令覆盖范围需由本地和 CI 报告确认                           |
| 验证与交付 | workflow test 当前锁定“PR 暂停”；策略可用单测区分 build 与非 build               | 更新 workflow 合同测试和策略单测，保留 `.runtime/frontend-quality` 证据上传 | 未有本次分支的远端 check-run，不能声明远端 required check 已恢复 |

评审选择：未指定外部同行评审，按 `AGENT_ONLY` 继续。上述为 Agent 三视角分析，不代表人工审批或外部评审。

## 4. 验收标准

- PR job 不再是空操作，且执行 `pnpm check:pr`。
- PR 不启动浏览器 E2E 或真实后端。
- 非构建检查失败时，命令继续执行，最终结果为成功并输出 advisory 数量及 `.runtime/frontend-quality/affected.json`。
- 受影响 workspace 递归 build 失败时，PR job 失败并上传报告。
- main push、手动 workflow、P0 E2E 分支语义保持不变。
- 相关 Node 单测通过；依赖安装后执行同源前端质量检查。

## 5. 回退与剩余风险

- 回退：恢复 PR job 的旧步骤或回退本分支提交；不修改远端分支保护配置。
- 剩余风险：第一阶段允许类型、测试、架构和包契约问题进入 PR；这些问题仍在 PR evidence、main push 或手动深度检查中暴露，不能称为全量质量通过。
- 下一阶段：修复基线 identity 和高价值静态债务后，再讨论提高阻断范围；必须由独立任务决定，不在本 PR 自动升级。
