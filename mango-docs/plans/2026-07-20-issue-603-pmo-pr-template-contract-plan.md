# Issue #603：业务 PR 模板与 PMO 合同一致性实施计划

## 1. 目标与基线

- 设计来源：`mango-docs/designs/2026-07-20-issue-603-pmo-pr-template-contract.md`。
- 最终风险：L3。
- 交付模式：FULL 治理。
- 工作区：`fix/issue-603-pmo-pr-template-contract` / `/Users/hardy/Work/mango-issue-603`。
- 成功条件：PMO 合同、checker、CLI full 模板、业务仓 sync/check/rollback 和发布 tarball 使用同一 Risk / Verification 合同，且业务自定义模板区段不被覆盖。

## 2. 有序实施清单

### TASK-001：建立合同与模板失败用例

影响路径：

- `mango-pmo/tests/risk-verification.test.mjs`
- `mango-ui/packages/mango-cli/scripts/check-cli.mjs`

动作：

1. 为 canonical 模板、缺字段、旧字段、错序、重复区段建立模板校验测试。
2. 为 CLI 生成、缺失模板同步、旧区段替换、区段外内容保留、check 失败提示和 rollback 建立回归断言。
3. 先运行定向测试，确认新增用例在实现前按预期失败。

### TASK-002：升级 delivery-assurance 合同和 canonical 模板

影响路径：

- `mango-pmo/contracts/delivery-assurance.json`
- `mango-pmo/templates/business-pull-request-template.md`
- `mango-pmo/tools/risk-verification.mjs`
- `mango-ui/packages/mango-pmo/scripts/check-package.mjs`

动作：

1. schema revision 升级到 5，并登记 PR body section、字段 key/label/顺序和旧字段。
2. 新增 canonical 业务 PR 模板。
3. checker 的字段读取改为合同 key 映射，并导出模板区段解析、替换和校验能力。
4. CLI 模式增加 `--template`，供包和发布回归直接校验模板结构。
5. PMO package check 将 canonical 模板列为必需文件并运行同包 checker。

### TASK-003：统一 CLI full 模板和新项目生成

影响路径：

- `mango-ui/packages/mango-cli/templates/full/.github/pull_request_template.md`
- `mango-ui/packages/mango-cli/scripts/check-cli.mjs`

动作：

1. CLI full 模板投影 canonical 内容。
2. 生成项目回归验证当前字段、无旧字段，并与当前 PMO baseline canonical 模板一致。

### TASK-004：实现业务仓 sync/check/rollback

影响路径：

- `mango-ui/packages/mango-cli/src/index.mjs`
- `mango-ui/packages/mango-cli/scripts/check-cli.mjs`

动作：

1. 从实际选择的 PMO baseline 读取 canonical 模板与合同 revision。
2. 为 sync/upgrade 生成项目 PR 模板 add/update/skip/warn 计划。
3. 仅替换单一 Risk / Verification 区段；缺失时插入；重复时 fail closed。
4. status/check 报告缺失或漂移并输出 sync 修复命令。
5. PMO backup 保存升级前项目模板，rollback 恢复历史模板或同步所选新合同。
6. 保持历史 baseline 没有 schema 5 模板时的兼容行为。

### TASK-005：同步 PMO 投影和能力说明

影响路径：

- `mango-ui/packages/mango-cli/README.md`
- `mango-ui/packages/mango-pmo/README.md`
- `mango-pmo/README.md`
- `mango-business-starter/business-pmo/README.md`
- `mango-docs/capabilities/README.md`
- `mango-business-starter/business-pmo/mango-baseline/**`

动作：

1. 说明 PR 模板区段所有权、sync/check 行为、错误恢复和升级入口。
2. 将 canonical PMO source 机械构建并同步到 starter baseline，禁止手工编辑投影。
3. 能力地图登记 #603 的行为变化和发布要求。

### TASK-006：验证与复核

按顺序执行：

```bash
node --test mango-pmo/tests/risk-verification.test.mjs
pnpm -F @mango/pmo build
pnpm -F @mango/pmo check
node mango-business-starter/scripts/sync-pmo-baseline.mjs --check
pnpm -F @mango/cli test
pnpm admin:styles:check
pnpm admin:module-styles:check
node mango-pmo/tools/workspace-layout-check.mjs --root .
git diff --check
```

若完整 CLI 测试因与本任务无关的外部环境失败，必须保留失败命令和输出，并继续执行能够观察 #603 目标的定向 CLI 回归；不得把未执行或失败项写成通过。

M14 复核检查：

- 合同字段是否仍存在第二事实源。
- sync 是否只修改目标区段。
- 缺失、旧版、重复和历史锁是否 fail closed 或有明确兼容行为。
- rollback 是否恢复合同一致性。
- PMO 与 CLI tarball 是否从安装后路径完成验证。

## 3. 回滚方式

- 实现提交前可按文件撤销当前 worktree 变更；禁止修改 main。
- 业务仓 upgrade 会在现有 PMO backup 中保存升级前 PR 模板，`mango pmo rollback` 恢复对应状态。
- npm 发布属于独立授权流程；本任务不发布，因此不产生远端不可变制品回滚动作。

## 4. 完成判定

- TASK-001 至 TASK-006 均完成，或任何例外具有真实失败证据和剩余风险。
- 设计中的六项验收标准均能映射到自动化结果。
- 交付报告明确说明 PMO/CLI 是否需要升版发布，以及业务仓在发布前仍需使用的临时处理方式。
