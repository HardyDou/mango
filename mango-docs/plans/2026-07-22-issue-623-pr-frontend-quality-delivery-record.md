# 标准交付记录

任务：#623 PR 前端质量分层

## 1. 元数据

- 任务 ID：GitHub Issue #623
- 交付模式：STANDARD
- 需求影响：L2 - 改变 Mango 主仓 PR 前端质量检查的执行层级和等待时间
- 方案风险：L2 - 调整共享 CI workflow，并新增 PR 专用质量 profile
- 最终风险：L2
- 工作区决策：CREATE（`/Users/hardy/Work/mango-issue-623-pr-quality`）
- 启用能力：M07、M09、M10、M15

## 2. 目标与范围

- 目标：修复 main/手动 Frontend E2E 启动真实后端时缺少 `mango-bom` 的问题，并把 PR 门禁收敛为快速、确定、与改动相关的质量检查。
- 成功条件：PR 不创建 `frontend-e2e-p0` 执行负载；main/手动 E2E 在启动后端前安装最小 Maven 前置模块；PR 始终产生稳定的 `frontend-pr-quality` 结果；深度前端质量和真实 E2E 继续在 main/手动入口执行。
- 处理范围：`.github/workflows/frontend-quality.yml`、前端质量选择/执行脚本及其单测、PR required context 的声明与远端回读。
- 不处理范围：E2E 用例内容、业务页面、后端业务实现、PMO Required Check 内部逻辑、发布流程。
- 停用验证：按用户指令，本次不执行 Frontend E2E；保留 main/手动入口的剩余运行态风险，由首次 CI 运行回读。

## 3. 可观察系统要求

| ID      | 参与者或入口                  | 输入或前置条件                                  | 预期行为                                                                                 | 失败语义                                              | 验收标准                                                 |
| ------- | ----------------------------- | ----------------------------------------------- | ---------------------------------------------------------------------------------------- | ----------------------------------------------------- | -------------------------------------------------------- |
| REQ-001 | GitHub PR                     | PR opened/synchronize/reopened/ready_for_review | 运行稳定的 `frontend-pr-quality`，不运行真实浏览器 E2E                                   | PR 快速门禁失败并阻断合并                             | PR check 列表只有快速前端门禁，无 `frontend-e2e-p0` 执行 |
| REQ-002 | PR 前端质量                   | 已知 base/head 和变更文件                       | 检查变更静态质量、前端边界、受影响 workspace 构建与单测                                  | 未知范围 fail-closed 扩大到全部 workspace，不静默跳过 | 规划单测覆盖 none/affected/full/unknown 分支             |
| REQ-003 | main push / workflow_dispatch | 深度前端质量入口                                | 保留全仓静态、构建、单测和消费者兼容检查                                                 | 深度质量失败并暴露证据                                | workflow 只在非 PR 事件执行 deep profile                 |
| REQ-004 | main/manual Frontend E2E      | 干净 Maven 本地仓库                             | 启动真实后端前安装 `mango-bom`、`mango-parent`、`mango-common`、`mango-tools`            | Maven 前置安装失败时停止，不进入后端启动              | workflow 合同检查 Maven 步骤位于 backend startup 之前    |
| REQ-005 | main 分支保护                 | 新 check 已在真实 PR 产生                       | `frontend-pr-quality` 成为 required context，保留 `pr-contract-check` 和 `pmo-doc-check` | 远端与声明不一致时不得宣称治理完成                    | GitHub API 回读与受版本控制策略一致                      |

## 4. 技术决定

| ID      | 对应要求         | 接口/数据/权限/兼容性决定                                                                                                          | 影响路径                                                                    | 回滚方式                                         |
| ------- | ---------------- | ---------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------- | ------------------------------------------------ |
| DEC-001 | REQ-001、REQ-003 | 同一 workflow 保留 PR 事件；PR job 与 deep job 使用事件级 job condition 分流，保证稳定 check 身份                                  | `.github/workflows/frontend-quality.yml`                                    | 删除 PR job 并恢复原 `frontend-quality` 事件范围 |
| DEC-002 | REQ-002          | `check-affected.mjs` 增加 `--profile=pr`；纯函数生成命令计划，deep profile 保持现有语义                                            | `mango-ui/scripts/quality/**`、`mango-ui/package.json`                      | 删除 PR profile 和脚本入口                       |
| DEC-003 | REQ-002          | PR profile 保留改动文件静态检查、边界检查、受影响构建/单测；共享或未知范围选择全部 workspace；仅深度 profile 执行完整 `check:full` | `mango-ui/scripts/quality/**`                                               | 将 PR job 临时切回 deep profile                  |
| DEC-004 | REQ-004          | 复用 PMO Java workflow 已验证的 Maven install 批次，不引入新的依赖集合                                                             | `.github/workflows/frontend-quality.yml`                                    | 移除前置安装步骤并恢复任务前状态                 |
| DEC-005 | REQ-005          | required context 两阶段启用：先让 PR 产生真实 check-run，再更新远端保护并回读证据                                                  | `.github/branch-protection-policy.json`、治理证据、GitHub branch protection | 从远端和声明同时删除新增 context                 |

## 5. 实施清单

| ID      | 对应决定         | 顺序 | 改动路径                                            | 完成条件                                   |
| ------- | ---------------- | ---: | --------------------------------------------------- | ------------------------------------------ |
| IMP-001 | DEC-002、DEC-003 |    1 | `mango-ui/scripts/quality`、`mango-ui/package.json` | PR/deep 命令计划与单测完成                 |
| IMP-002 | DEC-001、DEC-004 |    2 | `.github/workflows/frontend-quality.yml`            | PR E2E 停用、deep 分流、Maven 前置安装完成 |
| IMP-003 | DEC-001、DEC-004 |    3 | `mango-ui/scripts/quality/*workflow*.test.mjs`      | workflow 事件和步骤顺序由自动化锁定        |
| IMP-004 | DEC-005          |    4 | 分支保护策略、远端 GitHub 状态与回读证据            | 三个 required contexts 与声明一致          |
| IMP-005 | 全部             |    5 | 本记录                                              | 验证结果、证据和剩余风险回填               |

## 6. 验收映射与结果

| 要求 ID | 验证方式                   | 命令或步骤                                                                                         | 结果    | 证据                                                                                    |
| ------- | -------------------------- | -------------------------------------------------------------------------------------------------- | ------- | --------------------------------------------------------------------------------------- |
| REQ-001 | M09 workflow 合同测试      | `node --test mango-ui/scripts/quality/frontend-quality-workflow.test.mjs`                          | PASS    | 3 项 workflow 合同断言通过；PR job 不含 Playwright、后端启动或 E2E 命令                 |
| REQ-002 | M10 规划单测               | `pnpm quality:gate:test`                                                                           | PASS    | Node 22.23.1 下 81 项通过；覆盖 none/affected/full/unknown、后端和无关 workflow 分类    |
| REQ-003 | M09 静态与构建验证         | `pnpm check:pr -- --base=origin/main --head=HEAD`                                                  | PASS    | 含 ratchet typecheck 的 fail-closed 全 workspace 路径通过，`real 223.06s`；未执行 E2E   |
| REQ-004 | M09 workflow 与 Maven 验证 | `mvn -f mango/pom.xml -pl :mango-bom,:mango-parent,:mango-common,:mango-tools -DskipTests install` | PASS    | Reactor 4/4 SUCCESS，6.661 秒；合同测试确认 install 位于 backend startup 前             |
| REQ-005 | M15 外部状态回读           | GitHub branch protection API 与真实 PR check-run                                                   | PASS    | PR #626 已产生 GitHub Actions `frontend-pr-quality`（app ID 15368）；远端 required contexts 更新为 3 项并经 API 回读与声明一致 |

## 7. 例外与剩余风险

- 用户明确停用本次 Frontend E2E 验证；不会用本地 E2E 声明运行态通过。
- main/manual E2E 的首次真实结果需要在包含本变更的 GitHub Actions 运行中回读。
- `frontend-pr-quality` 只在 PR #626 真实 check-run 出现后加入 required contexts，远端保护与受版本控制声明已经 API 回读一致。
- 本地验证固定使用 Node 22.23.1 和 pnpm 11.14.0，与 workflow 版本一致。
