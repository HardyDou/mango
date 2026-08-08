---
name: mango-design-delivery-assurance
description: 在实施前或关键事实变化时，推荐并让用户一次确认 Mango 工作区策略、中文文档版本、L0-L5 内部等级和 M01-M16 交付措施；事实模糊时集中询问，不处理发布。
---

# Mango 交付保障

## 解析规范源

按顺序选择首个存在的 `PMO_ROOT`：`<repo>/business-pmo/mango-baseline`、`<repo>/mango-pmo`、`<plugin-root>/dist/baseline`。均不存在时 `STOP`。执行 PMO preflight，读取 `contracts/delivery-assurance.json`、`contracts/lean-documents.json`、`tools/resolve-lean-document-policy.mjs`、`tools/select-delivery-assurance.mjs`、`rules/11-delivery-assurance.md` 和所选模板。只有边界不清时才读取对应参考资料。

## 发布独立

不得授权或执行制品、版本、仓库、tag、Release 或发布恢复；转仓库内 `$mango-release`。

## 工作区

只读使用 `NO_WORKTREE`；同任务非 main worktree 使用 `REUSE`；从 main/主工作区修改受版本控制文件使用 `CREATE`，无需询问。只有用户明确要求并确认风险后使用 `MAIN_EXCEPTION`。同一任务不得创建第二个 worktree。

在确定 `CREATE/REUSE` 前运行 `tools/check-worktree-delivery-integrity.mjs --mode start`。`REUSE` 必须携带从既有任务、Issue 或 PR 证据取得的 `--expected-branch`；禁止只看当前分支后自行宣称同一任务。当前非 main worktree 有未提交内容而请求属于另一任务时 `STOP`，不得先新建 worktree 再遗忘旧改动。发现其它脏 worktree 时集中列出；只有用户明确确认精确路径属于需保留的并行任务后，才可传 `--allow-dirty-worktree <path>` 继续。已合并 worktree 残留本地改动不得豁免或复用。

## 推荐文档版本

1. 明确目标、成功结果、范围、不处理范围、角色、可观察行为、系统边界、数据/安全/租户/兼容影响、回滚、代码基线和关键未知项。
2. 分别评估需求影响和方案风险，最终等级取较高值。
3. 使用中文版本名向用户展示：`直接做` 对应 `L0/L1`；`一页纸` 对应 `L2`；`标准版` 对应 `L3`；`详细版` 对应 `L4`；`四文档` 对应 `L5`。
4. 新模块、新系统、重大架构替换、核心迁移、不可逆数据处理或跨系统主业务链固定为 `L5`。
5. 用户可提高等级。低于事实等级必须明确确认并记录剩余风险；安全、租户、资金、破坏性数据和不可逆事实不得豁免。

## 询问边界

事实无法从用户输入、批准上游、当前规范或仓库代码确定时，将相关问题集中询问，只问：目标/角色诉求、业务规则和边界结果、状态/数据语义、系统责任，或公共契约/数据/安全/租户/兼容/回滚的关键决定。先补齐会改变推荐结果的事实，再展示选择器。破坏性动作、外部写入和例外仍需独立授权。可直接查明的事实不问，未知事实不臆造。

## 一次确认

1. 按任务事实推荐文档版本和 M01-M16；M01 使用 preflight 已解析的 `CREATE/REUSE/MAIN_EXCEPTION` 精确值，M02 及 M03-M16 只推荐有真实触发事实的项。
2. 调用 `node "$PMO_ROOT/tools/select-delivery-assurance.mjs" --等级 <L0-L5> --措施 <机器值清单>`。业务项目也可使用 `mango pmo 选择`。
3. 让用户在同一界面用 `↑/↓`、`Space` 完成文档版本单选和 M01-M16 多选，只按一次 `Enter`。不得逐项提问。
4. 界面只显示中文版本名、勾选框和具体描述；`[x]` 即采用，不显示“启用/不启用”。`R` 恢复推荐，`Esc` 取消。
5. 固定 L5 事实传 `--fixed-level L5`。非 TTY 使用一次批量输入；不得改成 16 轮问答。
6. 人类摘要只列已勾选项；完整机器结果保留稳定英文值。M02 和 `MAIN_EXCEPTION` 的勾选不替代执行前的独立授权。

## 记录与重评

记录目标、范围、需求影响、方案风险、最终等级、中文文档版本、工作区决定、文档路径、直接追踪、采用的规范版本、代码 commit/SHA、用户勾选措施和剩余风险。范围、耦合、失败影响、数据/外部状态、回滚或验收变化时重新计算，满足更高等级事实时立即升级并重新一次确认。

M09-M16 只按可观察结果推荐。高等级不自动增加无关 UI 或 E2E。执行用户勾选的措施；每份新精简文档必须运行 `check-lean-document.mjs`。
