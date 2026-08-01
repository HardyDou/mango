# Issue 673 PMO required-check 发布说明覆盖设计

## 1. 背景与目标

`@mango/pmo@1.3.8` 同时包含历史生命周期文档兼容修复和前端页面基线治理，但根
`CHANGELOG.md` 与 `@mango/cli@1.0.94` 的内置 changelog 只披露前者。现有发布说明
检查器只验证包名、版本和固定章节是否存在，无法识别 CLI 锁定的 PMO 版本新增了
required check，也无法证明消费入口说明包含迁移、例外和验证方式。

目标：

- 完整补录 PMO 1.3.8 的规则、checker、required-check 行为和业务升级步骤。
- 为 PMO 建立可直接访问的版本 changelog。
- 发布 CLI 时，根据其锁定的 PMO 版本识别相对上一 CLI 锁定基线新增的 required-check
  checker，并阻断消费入口说明遗漏。

## 2. 范围与边界

本次包含：

- 根 changelog、CLI 1.0.94 changelog 和 PMO 1.3.8 changelog。
- `check-release-notes.mjs` 的 PMO required-check 覆盖判定。
- 发布说明检查器的定向自动化测试。

本次不包含：

- 改写或重新发布已经发布的 npm 制品。
- 修改 PMO 页面基线规则、checker 或 workflow 行为。
- 创建 tag、GitHub Release、Push、PR 或关闭 Issue；这些外部写操作需要独立授权。

## 3. 风险与交付模式

- 需求影响：L3。发布说明是平台升级入口，遗漏会使业务仓在未知条件下被新增
  required check 阻断。
- 方案风险：L3。修复会修改发布门禁自身，错误判定可能阻断后续 CLI/PMO 发布。
- 最终风险：L3，采用 FULL 治理流程。
- 工作区：M01=REUSE，`fix/issue-673-release-notes`。

## 4. 设计

### 4.1 发布说明合同

新增 PMO required check 时，PMO、CLI 消费入口和根发布段都使用
`PMO Required Checks` 章节。每个 checker 以文件名作为稳定身份，并分别记录：

- `Migration`：升级前需要检查或迁移的业务代码。
- `Exception`：不适用时可复核的例外合同。
- `Verify`：业务仓可直接执行的验证命令。

PMO 1.3.8 的条目覆盖列表、详情、表单和标准弹框公共骨架，记录
`mango-page-baseline-exception` 注释合同，并给出 checker 的 base/head 命令。

### 4.2 新增 checker 识别

当检查 `@mango/cli` 发布说明时：

1. 从 CLI `release-versions.json` 读取锁定的 `@mango/pmo` 版本。
2. 从已合并 tag 中找到版本不同的最近 CLI 发布，读取其 PMO 锁；锁未变化时不重复要求
   披露，锁发生变化时把该 CLI 发布作为上一已发布基线。
3. 对比上一 CLI 发布与当前工作树中的 GitHub/Gitea 业务 `pmo-doc-check` 模板，提取
   随 PMO 锁升级新增的 `business-pmo/mango-baseline/tools/check-*.mjs`。
4. 要求 PMO 对应版本 changelog、CLI 对应版本 changelog、根目标版本发布段，以及启用
   `--check-github-release` 时的 Release 正文，都包含该 checker 的 Migration、Exception、
   Verify 三项说明。

根 Changelog 必须按目标包版本精确选择发布段，后续新发布条目不能改变历史版本检查结果。

`@mango/pmo` 的发布说明检查也提前执行同一 CLI 消费覆盖判定，确保 PMO 制品进入不可变
publish 之前发现遗漏，而不是等 PMO 已发布后才在 CLI 步骤失败。

无法解析 CLI 锁、PMO 版本、上一 CLI 发布 tag 或历史 workflow 时 fail-closed，避免门禁在
证据不足时静默放行。

### 4.3 兼容性与恢复

- 非 CLI 包继续使用原有发布说明检查，不增加 PMO 依赖判定。
- CLI 没有新增 PMO required check 时不要求空的 checker 条目。
- 检查器只读 Git 历史和工作树，不修改发布物料。
- 如判定错误，可回退检查器和测试；发布说明补录作为历史事实保留。

## 5. 验收

- AC-001：根、CLI、PMO 的 1.3.8/1.0.94 说明同时披露前端页面基线规则、checker、
  required-check 接入、迁移、例外和验证命令。
- AC-002：检查器能从 PMO 1.3.7 发布基线识别
  `check-frontend-page-baseline.mjs` 为 1.3.8 新增 checker。
- AC-003：任一消费入口缺少 checker、Migration、Exception 或 Verify 时检查失败。
- AC-004：完整说明通过 `@mango/cli@1.0.94` 的实际 release-notes 检查。
- AC-005：工作区、样式和定向测试门禁通过，diff 不包含版本、锁文件或发布动作。
