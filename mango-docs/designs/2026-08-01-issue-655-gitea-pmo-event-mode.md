# Issue #655 Gitea PMO 事件模式设计

## 背景与目标

Gitea 在 PR 合并或关闭后编辑正文时，事件的 `base.sha` 已不再表示原 PR base，`head.sha`
仍为原 PR head。对这一终态元数据事件执行 diff 会构造无效范围，使稳定
`pmo-doc-check` 错误失败。

目标是在不改变开放 PR、push 和手动触发质量门禁语义的前提下，让终态 PR 正文仍能接受
风险与验证合同检查。

## 范围与边界

包含：

- Gitea 模板中的事件模式分流。
- 可发布 PMO baseline 中的纯事件模式解析器和测试。
- 生成项目与 PMO 使用说明。

不包含：

- 修改 GitHub workflow、required check 名称或分支保护。
- 放宽任何可提供有效 diff 的事件的失败策略。
- 发布 npm 包、升级业务仓或修改业务代码。

## 决策

事件解析器按事件生命周期和能力分类，不依赖仓库、PR 编号或指定 SHA：

- `change-validation`：opened、synchronize、reopened、开放 PR 的 edited、push、手动触发和信息缺失事件。继续执行分类器；base/head 缺失或无效时保持失败关闭。
- `contract-only`：已关闭或已合并 PR 的 `edited`。只写入并校验 PR body，不读取 Git diff，也不执行文档、前端或 Maven 质量门禁。

终态事件仍由同一个 `pmo-doc-check` job 产生结果，因此不改变 required check 身份。开放 PR
的正文编辑继续走完整门禁，不能用一次正文编辑覆盖此前的代码检查失败。

## 风险与恢复

- 需求影响：L3。变更平台 PMO Required Check 的执行语义，影响所有同步该 Gitea 模板的业务仓。
- 方案风险：L3。错误分流可能跳过应执行的质量门禁或继续构造无效 diff。
- 最终风险：L3，FULL 治理；M01=CREATE。
- 恢复方式：回退本次模板和 baseline tool 变更，开放 PR 和其它事件仍沿用原来的 fail-closed diff 流程。

## 验收

- 已关闭和已合并 PR 的 `edited` 解析为 `contract-only`。
- 开放 PR 的 opened、synchronize、reopened、edited 解析为 `change-validation`。
- push、手动触发和不完整事件仍为 `change-validation`。
- PMO bundle、生成项目模板和 CLI 全量测试均能消费新工具。
