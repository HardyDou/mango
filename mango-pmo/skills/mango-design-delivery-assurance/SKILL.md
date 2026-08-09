---
name: mango-design-delivery-assurance
description: 在实施前或关键事实变化时，确定 Mango 任务的工作区策略、L0-L5 交付等级、精简文档形态、直接追踪和可观察验证能力；不处理发布。
---

# Mango 交付保障

## 解析规范源

按顺序选择首个存在的 `PMO_ROOT`：`<repo>/business-pmo/mango-baseline`、`<repo>/mango-pmo`、`<plugin-root>/dist/baseline`。均不存在时 `STOP`。执行 PMO preflight，读取 `contracts/delivery-assurance.json`、`contracts/lean-documents.json`、`tools/resolve-lean-document-policy.mjs`、`rules/11-delivery-assurance.md` 和所选模板。只有边界不清时才读取对应参考资料。

## 发布独立

不得授权或执行制品、版本、仓库、tag、Release 或发布恢复；转仓库内 `$mango-release`。

## 工作区

只读使用 `NO_WORKTREE`；同任务非 main worktree 使用 `REUSE`；从 main/主工作区修改受版本控制文件使用 `CREATE`，无需询问。只有用户明确要求并确认风险后使用 `MAIN_EXCEPTION`。同一任务不得创建第二个 worktree。

## 确定等级与文档

1. 明确目标、成功结果、范围、不处理范围、角色、可观察行为、系统边界、数据/安全/租户/兼容影响、回滚、代码基线和关键未知项。
2. 分别评估需求影响和方案风险，最终等级取较高值。
3. 严格执行：
   - `L0/L1`：无文档。
   - `L2`：一份 `delivery-l2.md`，最多 1 张 A4 等效内容。
   - `L3`：一份 `delivery-l3.md`，最多 3 张。
   - `L4`：一份 `delivery-l4.md`，最多 5 张。
   - `L5`：业务需求、系统需求、技术设计、实施与验证计划四份独立文档。
4. 新模块、新系统、重大架构替换、核心迁移、不可逆数据处理或跨系统主业务链固定为 `L5`。
5. 用户可提高等级。低于事实等级必须明确确认并记录剩余风险；安全、租户、资金、破坏性数据和不可逆事实不得豁免。

## 询问边界

事实无法从用户输入、批准上游、当前规范或仓库代码确定时，将相关问题集中询问，只问：目标/角色诉求、业务规则和边界结果、状态/数据语义、系统责任，或公共契约/数据/安全/租户/兼容/回滚的关键决定。破坏性动作、外部写入和例外也必须询问。可直接查明的事实不问，未知事实不臆造。

## 记录与重评

记录目标、范围、需求影响、方案风险、最终等级、工作区决定、文档路径、直接追踪、采用的规范版本、代码 commit/SHA、验证能力和剩余风险。范围、耦合、失败影响、数据/外部状态、回滚或验收变化时重新计算，满足更高等级事实时立即升级。

M09-M16 只按可观察结果选择。高等级不自动增加无关 UI 或 E2E。每份新精简文档必须运行 `check-lean-document.mjs`。
