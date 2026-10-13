Based on my analysis of the redlines, requirements, and current implementation, here's the implementation plan:

## 1. 复用判断（REDLINE-REUSE-010）

**复用现有能力：**
- Node.js 标准库：`readFileSync`、`String.split`、`String.match`、`String.indexOf`、正则表达式
- 项目现有工具：`sectionAfter`、`escapeRegExp`、`valueAfter`、`fail` 函数
- 测试基础设施：`node:test`、`spawnSync`、`mkdtempSync`

**不需要新增：**
- 不需要新增依赖包
- 不需要新增抽象层或公共类型
- 不需要新增配置项
- 只需要在现有函数内重写表格解析逻辑，从"全行文本匹配"改为"按列解析每个单元格"

**选择理由：**
当前 `firstDataRow` 和 `hasDataRow` 使用全行正则，无法区分"阻断问题列写了 None"和"结论列写了 PASS"。需要按表头定位列边界，逐单元格验证，但不需要引入 CSV 解析库或 Markdown AST，因为决策复核表格格式受控且列数固定。

## 2. 术语（REDLINE-TERMS-030）

**不引入新术语。**
- 现有术语：`AGENT_ONLY`、`EXTERNAL_PEER_REVIEW`、`PASS`、`RECORDED`、`BLOCKED`、`评审选择`、`询问记录`、`阻断问题`、`建议`、`结论`
- 修复后继续使用这些术语，不创造同义词
- "旧 URL" 指 commit `06aa9c1ee` 之前的三个 FAQ 路径：
  - `/mango-docs/guides/business-integration/permission-button-troubleshooting`
  - `/mango-docs/guides/business-integration/rbac-menu-page-troubleshooting`
  - `/mango-docs/guides/business-integration/tenant-dict-config-empty`

**不确定项：**
需求提到"为三个历史 FAQ 保留旧 URL"，但 `stage-public-docs.mjs` 中没有 VitePress `rewrites` 或 `redirects` 配置。当前实现是把旧路径内容移动到 `changelog/business-integration/` 作为历史追溯，而把精简后的排障步骤放在 `guides/faq/`。如果"保留旧 URL"是指在 VitePress 配置中添加 URL 重定向，需要用户明确这是否属于本次范围，还是已经通过 `changelog` 目录的交叉引用完成。

## 3. 最小实现（REDLINE-MINIMAL-020）

**改动边界：**
- `mango-pmo/tools/check-decision-review.mjs`：重写表格解析逻辑
- `mango-pmo/tests/decision-review.test.mjs`：增加覆盖新拒绝场景的测试用例
- `mango-business-starter/business-pmo/mango-baseline/tools/check-decision-review.mjs`：自动同步（通过 `sync-pmo-baseline.mjs` 或 PMO 包构建流程）

**当前消费者：**
- CI 门禁：`.github/workflows/pmo-doc-check.yml` 中的 node --test 调用（虽然当前未显式列出 `decision-review.test.mjs`，但测试文件存在且可执行）
- 本地验证：开发者运行 `node mango-pmo/tools/check-decision-review.mjs --document <file>`
- 决策复核文档作者：按 `rules/14-decision-expert-review.md` 和 `templates/decision-review.md` 生成的决策文件

**验证入口：**
- `node --test mango-pmo/tests/decision-review.test.mjs`
- 手工执行 checker 对包含边界场景的决策文件
- 验证空询问、模式不一致、开放阻断、BLOCKED 状态和空结论能被正确拒绝
- 验证 `AGENT_ONLY` 和"未指定按 AGENT_ONLY 继续"能通过
- 验证关键词在非关键列（如建议列写 PASS）不掩盖阻断问题

## 4. 简单任务问卷（REDLINE-QUEST-050）

**不适用。**
- 本任务是修复现有校验工具的逻辑缺陷，不引入新概念、不改变公共术语
- 决策复核的术语和格式已在 `rules/14-decision-expert-review.md` 和 `templates/decision-review.md` 中定义
- 不需要生成新的术语表或固定问卷

## 5. 过度设计（REDLINE-OVERDESIGN-060）

**无过度设计：**
- 不新增抽象层：直接在 checker 内重写表格解析函数
- 不新增配置项：校验规则固定，由 `rules/14-decision-expert-review.md` 定义
- 不新增依赖：只使用 Node.js 标准库
- 不新增公共类型：`parseTableRow` 或类似辅助函数只在 checker 内部使用
- 不创造同义词：继续使用现有术语

**实现方式：**
1. 读取章节 6 的表格
2. 识别表头行，提取列名和列边界（通过 `|` 分隔符位置）
3. 对每个数据行，按列边界切分单元格
4. 分别验证：
   - `评审方式` 列：必须为 `AGENT_ONLY` 或 `EXTERNAL_PEER_REVIEW`，且与 bullet 中的 `**评审选择：**` 一致
   - `阻断问题` 列：`EXTERNAL_PEER_REVIEW` 模式要求为 `None` 或明确无阻断描述；出现未解决阻断时拒绝
   - `结论` 列：`EXTERNAL_PEER_REVIEW` 要求 `PASS`；`AGENT_ONLY` 要求 `PASS`、`RECORDED` 或 `AGENT_ONLY`；出现 `BLOCKED` 时拒绝
   - 空值检查：`评审人/Agent 角色`、`阻断问题`、`建议`、`结论` 不能为空或只有空白符
5. `AGENT_ONLY` 模式要求 bullet 中的 `**询问记录：**` 包含实质内容（不能为空、不能只写"无"或模板占位符）
6. 保留现有对 `BLOCKED` 状态的顶层检查：`**状态：**` 字段
7. 三个历史 FAQ 的旧 URL 保留：如果需求指 VitePress rewrites，需明确配置位置；如果指 `changelog` 目录的交叉引用，则已完成，无需修改 checker
