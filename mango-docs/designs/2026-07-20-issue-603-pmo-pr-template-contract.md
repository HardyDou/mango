# Issue #603：业务 PR 模板与 PMO 合同一致性治理设计

## 1. 背景与目标

`@mango/pmo@1.3.2` 的 delivery-assurance schema revision 4 已要求业务 PR 在 `## Risk / Verification` 中填写交付模式、工作区决策和 M01-M16 保障证据，但 `@mango/cli@1.0.85` 发布的 full 模板仍保留旧版 `Selected verification / Why sufficient / Skipped verification` 字段。既有业务仓执行 `mango pmo sync/upgrade` 时也不会同步项目根 PR 模板，`mango pmo check --locked` 因此会对缺失或过期模板给出错误的 current 结论。

本任务的目标是让 PMO 合同、PMO checker、CLI 发布模板和业务仓同步/检查链路使用同一份可验证合同，并在不覆盖业务仓其它 PR 说明的前提下受控维护 `## Risk / Verification` 区段。

不处理范围：

- 不改变 GitHub 或 Gitea 的 required check 名称、触发方式和远端分支保护。
- 不修改业务 PR 已提交的正文；模板只影响后续创建或编辑的 PR。
- 不在本任务中发布 npm 包、创建 tag 或 GitHub Release；修复合并后需要独立发布 `@mango/pmo` 和 `@mango/cli` 补丁版本。

## 2. 风险与交付模式

- 需求影响：`L3`。该问题影响所有消费 CLI/PMO 的业务仓 PR required check，并改变 PMO 核心门禁的升级与检查语义。
- 方案风险：`L3`。实现跨越 PMO 合同、checker、npm baseline、CLI 初始化、sync/check/rollback 和发布包回归，错误会产生模板覆盖或门禁假绿。
- 最终风险：`L3 = max(L3, L3)`。
- 交付模式：`FULL` 治理任务，使用本治理设计、实施计划、定向自动化和独立复核，不伪造 BRD/SRS/TDD 产品文档。
- 工作区：`M01=REUSE`，分支 `fix/issue-603-pmo-pr-template-contract`，worktree `/Users/hardy/Work/mango-issue-603`。
- 保障措施：启用 M07、M08、M09、M10、M11、M14、M15；不启用 M12、M13、M16，因为本任务不改变服务 API、浏览器 UI，也没有必须人工现场验收的结果。M15 用于回读 GitHub Issue #603 的状态、评论和关联 PR，确认外部问题事实没有漂移。

## 3. 方案选择

采用“区段级 PMO 托管”方案：PMO 拥有 `## Risk / Verification` 的标题、字段、顺序和占位说明；业务仓继续拥有 PR 模板的其它区段。

未采用的方案：

- 整文件覆盖：实现简单，但会删除业务仓自行维护的 Summary、变更清单和平台专用说明。
- 只检查不修复：可以阻断漂移，但升级仍要求每个业务仓人工复制字段，不能消除当前绕行成本。

## 4. 唯一合同源

`mango-pmo/contracts/delivery-assurance.json` 继续作为 delivery-assurance 的机器合同源，并把 schema revision 从 4 升级到 5。新增 PR body section 元数据，至少声明：

- 区段标题 `Risk / Verification`；
- 业务模板路径；
- 当前字段 key、显示 label 和固定顺序；
- 已废弃旧字段 label。

`mango-pmo/templates/business-pull-request-template.md` 保存业务项目完整模板。`risk-verification.mjs` 从合同元数据读取字段 label，不再独立维护第二份字段名列表，并同时提供两类校验：

1. PR 正文校验：验证已填写值、风险最大值、模式、工作区和保障证据。
2. PR 模板校验：验证目标区段存在、字段完整且顺序正确、旧字段不存在，不要求占位模板提供真实任务证据。

`@mango/pmo` 构建必须把合同、模板和 checker 放入同一 baseline manifest。包检查直接使用随包 checker 校验随包模板，任何一项漂移都会阻断打包。

## 5. CLI 初始化、同步与检查

### 5.1 新项目

CLI full template 中的 `.github/pull_request_template.md` 与 PMO canonical 业务模板保持逐字节一致。CLI 回归同时校验源码模板、生成项目模板和解析到的 `@mango/pmo` baseline 模板，避免只更新其中一份。

### 5.2 既有项目同步

`mango pmo sync/upgrade` 根据实际选择的 baseline 版本读取 canonical 模板：

- 项目没有 `.github/pull_request_template.md`：新增完整 canonical 模板。
- 项目已有且只有一个 `## Risk / Verification`：只替换该区段，保留区段前后所有业务内容。
- 项目已有但缺少该区段：优先插入到 `## Validation` 前；没有 Validation 时追加到文件末尾。
- 项目存在重复目标区段：拒绝猜测合并，输出重复位置和人工合并后重跑命令。
- 当前区段与 canonical 完全一致：记录为 skip。

dry-run 必须显示该项目文件的 add/update/skip/warn 计划。普通 sync、显式 upgrade 和新项目 init 使用同一个区段处理函数，避免三条路径再次分叉。

### 5.3 状态检查

`mango pmo status/check [--locked]` 使用被检查 baseline 中的 schema revision 和 canonical 模板验证项目根文件：

- 缺失文件、缺失区段、重复区段、旧字段或当前字段漂移均进入 errors。
- 输出明确包含目标路径、锁定 PMO 身份和修复命令 `mango pmo sync --project-dir .`；如果可用 CLI 与项目锁不一致，保留现有“使用项目内锁定 CLI 或 upgrade”提示。
- 对不含 schema revision 5 模板元数据的历史 baseline 保持历史行为，不由新版 CLI 凭空套用新版字段。

### 5.4 回滚

升级事务创建既有 PMO backup 时，同时保存升级前项目 PR 模板（存在时）。回滚到 schema revision 5 或更新版本时按所选 baseline 重新同步目标区段；回滚到更早合同且 backup 保存了原模板时恢复原文件。这样不会出现 baseline 已回滚而项目模板仍停留在新合同的状态。

## 6. 错误处理与兼容边界

- 区段识别只接受二级标题 `## Risk / Verification`，不模糊匹配相似标题。
- 解析和替换保持目标文件原有区段外字节内容，不重排其它标题，不修改 CODEOWNERS 或 workflow。
- canonical 模板缺失、合同字段元数据非法、模板与合同不一致时 fail closed，禁止继续输出 current 或完成同步。
- 写入仍复用 CLI 现有 plan/dry-run 和目录创建机制，不引入隐藏覆盖开关。
- GitHub 与 Gitea 共用同一个项目模板文件和 baseline checker，因此不增加平台分支。

## 7. 验证设计

### PMO 单元与包验证

- 当前 canonical 模板通过模板合同校验。
- 缺字段、错序、旧字段、重复区段和缺区段分别失败。
- 填充后的 canonical 模板继续通过现有 PR 正文值校验。
- `@mango/pmo` build/check 和真实 pack 后，tarball 内合同、模板、checker 使用同一 revision 并相互校验通过。

### CLI 集成回归

- full 项目生成正确新版模板。
- 空业务仓 sync 新增模板。
- 旧模板 sync 只替换 Risk 区段并保留自定义内容。
- 缺区段模板按固定位置插入。
- 重复区段同步失败且不改文件。
- check 对缺失和旧模板失败并输出修复命令；sync 后 check 通过。
- dry-run 只报告计划，不写文件。
- upgrade 后模板切换到新合同，rollback 恢复升级前模板。
- 从实际 CLI tarball 与配套 PMO tarball 执行生成、sync、check，防止 workspace 源码掩盖发布清单问题。

### 仓库门禁与能力说明

- 更新 CLI、PMO package、Business PMO README 和能力地图，说明模板所有权、同步行为、检查错误和升级方式。
- 执行 PMO 投影检查、CLI 定向测试、PMO package check、workspace layout check，以及 preflight 要求的 admin styles/module styles 检查。
- 回读 GitHub Issue #603 的状态、评论和关联 PR；不修改 Issue，不在本任务中创建远端状态。
- M14 独立复核重点检查：单一合同源、业务内容保留、历史锁兼容、回滚和 tarball 真实消费。

## 8. 验收标准

1. 新生成项目的 PR 模板只包含 delivery-assurance schema revision 5 当前字段，不含旧字段。
2. 缺失或过期模板的既有业务仓执行 `mango pmo sync/upgrade` 后获得当前 Risk 区段，业务自定义区段保持不变。
3. `mango pmo check --locked` 对缺失、重复或合同漂移模板失败，并给出可执行修复命令。
4. PMO canonical 模板、CLI 发布模板、生成项目和发布 tarball 均由同版本 checker 验证，不再允许字段漂移。
5. 回滚不会留下与已回滚 baseline 不兼容的项目模板。
6. 所有启用的自动化与复核通过；未执行发布时明确记录需要发布的 PMO/CLI 补丁版本，不声明业务仓已经获得修复。
