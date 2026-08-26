# 发布 baseline 连续性治理记录

## 1. 元数据

- 状态：IMPLEMENTED_AND_VERIFIED
- 交付模式：FULL
- 需求影响：L3，成功发布 baseline 是后续发布计划、制品 tuple 和恢复判断的事实源。
- 方案风险：L3，修改发布状态机校验、Release PR CI 分类和 CLI 运行时入口。
- 工作区：M01=CREATE；分支 `fix/release-baseline-carry-forward`。
- 授权边界：本任务只实施和验证方案 B，不发布 registry 制品，不创建或移动 Tag/GitHub Release，不 Commit、Push 或创建 PR。

## 2. 触发事实与根因

2026-08-22 的 npm-only 发布收尾 PR [#849](https://github.com/HardyDou/mango/pull/849) 把成功 baseline 更新到新 npm tuple，同时按既有 builder 保留了上一批 Maven `1.0.39` 的完整证据。completed plan 校验却把 `plan.maven === null` 解释为 `baseline.maven` 也必须为空，导致下一次计划检查失败。

该 PR 只修改 changelog、baseline 和已消费 Changeset。原 CI 分类没有把 `release-baseline.json` 识别为计划门禁触发项，`CLI and JavaScript gates`、`PMO gates` 等均被跳过，聚合 `pmo-doc-check` 仍通过。因此问题由两个缺口叠加形成：baseline 语义不一致，以及收尾变更未进入真实校验。

## 3. 方案 B 决定

1. npm-only completed baseline 必须与 plan 中的上一成功 Maven 证据做完整结构比较，而不是要求 Maven 为空；坐标、文件大小或 checksum 漂移均失败。
2. 回归测试连续覆盖“已有 Maven 成功证据 -> npm-only 收尾 -> 下一次 release plan”，同时覆盖证据篡改。
3. PR 修改 `release-plan.json` 或 `release-baseline.json` 时强制执行 `check-release-plan.mjs`，并将结果纳入稳定聚合门禁。
4. CLI 和可直接执行的 release plan/prepare/publish 脚本在任何计划读取、锁、子进程或 registry 访问前校验 `mango-ui/package.json` 的 Node engine；范围格式异常同样失败闭合。

## 4. 验收映射

| ID       | 验收场景                            | 预期                                  | 验证入口                             | 当前结果 |
| -------- | ----------------------------------- | ------------------------------------- | ------------------------------------ | -------- |
| BASE-001 | Maven 成功证据后执行 npm-only 收尾  | Maven 结构完整继承                    | release plan/manifest 单测           | PASS     |
| BASE-002 | 使用 npm-only baseline 生成下一计划 | 完整 tuple 仍包含 Maven 坐标          | release plan 连续回归                | PASS     |
| BASE-003 | 篡改携带的 Maven checksum           | completed plan 校验失败               | release plan 单测                    | PASS     |
| CI-001   | PR 修改 plan 或 baseline            | 独立 Release plan gate 执行并进入聚合 | classifier 与 workflow contract test | PASS     |
| NODE-001 | Node 版本低于下限或高于上限         | 在计划、锁、子进程和 registry 前失败  | runtime/CLI 单测与默认 Node 实测     | PASS     |
| PMO-001  | PMO 规定的仓库门禁                  | 样式、测试质量、workspace 布局通过    | PMO preflight 要求命令               | PASS     |

## 5. 验证证据

- Node `22.23.1`：release 脚本测试 `82/82` 通过；完整 CLI 套件 `77` 通过、`2` 个平台条件跳过、`0` 失败；PMO workflow 合同 `21/21` 通过。
- 普通源码 PR 的正式 release change checker 通过，机器计算的直接包和完整发布闭包均仅为 `@mango/cli`；patch Changeset 与该结果一致。
- 当前成功 baseline 的真实 `check-release-plan.mjs` 通过，plan digest 为 `a948d84b41cbdb2dcedad09e874440c46a94cc2b9c154ac182f65a08bcc99692`。
- 默认 Node `26.5.0` 在 release plan import 入口直接报告 `Mango release requires Node >=22.23.1 <23`。
- Admin 样式、模块样式、测试质量、workspace 布局、模块 README、README 源码事实、Prettier 和 workflow YAML 解析均通过。

## 6. 回退与剩余风险

- 合并前可整体撤销本任务代码、测试和文档，不影响已发布坐标或成功 baseline。
- 已发布的 PR #849、npm/Maven 坐标与 Tag/Release 保持不变；本任务不通过编辑 baseline 隐藏历史事实。
- CI 只能阻止后续非法 plan/baseline 进入主干，不能改变已合并历史。Node engine 仍由 `mango-ui/package.json` 维护，本次实现不放宽当前范围。
