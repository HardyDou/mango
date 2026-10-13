根据需求"增大受信任的 release-scope 分类器输出缓冲区，使大型文档清理 PR 能在 PMO 门禁运行前完成分类"，我已阅读相关规则和代码。

## 1. 复用判断（REDLINE-REUSE-010）

**应复用现有能力**：
- `mango-pmo/tools/classify-pmo-check-scope.mjs:315` 已使用 `maxBuffer: 64 * 1024 * 1024` 处理大型 Git 输出
- Node.js 标准库 `child_process.spawnSync` 的 `maxBuffer` 选项

**不需要新增任何结构**：只需在 `mango-ui/scripts/release/classify-release-pr.mjs:133` 的 `spawnSync` 调用中添加相同的 `maxBuffer` 参数，复用已验证的缓冲区大小（64 MB）。

**被放弃的复用对象**：无。这是补齐缺失的标准参数，而非替换现有实现。

## 2. 术语（REDLINE-TERMS-030）

**不适用**。本次改动不引入新的公共类型、API、配置、权限码、事件或概念名。`maxBuffer` 是 Node.js 标准 API 的既有参数，不产生术语冲突或同义词。

## 3. 最小实现（REDLINE-MINIMAL-020）

**改动边界**：
- 文件：`mango-ui/scripts/release/classify-release-pr.mjs`
- 位置：第 133 行 `spawnSync('git', ['diff', '--name-only', ...])`
- 操作：在 options 对象中添加 `maxBuffer: 64 * 1024 * 1024`

**当前消费者**：
1. `.github/workflows/pmo-doc-check.yml:75` - PR 门禁工作流
2. `mango-ui/scripts/release/run-local-release-checks.mjs:41` - 本地 release 检查

**验证入口**：
1. 单元测试：`mango-ui/scripts/release/classify-release-pr.test.mjs` 可增加大输出场景覆盖
2. 集成验证：针对包含大量文档变更的 PR（例如 500+ 文件）执行分类器，观察是否不再因 `maxBuffer exceeded` 失败
3. CI 门禁：在实际 PR 工作流中观察大型文档清理 PR 是否能通过文件列表收集阶段

## 4. 简单任务问卷（REDLINE-QUEST-050）

**不适用**。本任务为简单参数补全，无新增公共概念或认知风险，不需要生成固定问卷或术语表。

## 5. 过度设计（REDLINE-OVERDESIGN-060）

**无过度设计**：
- ❌ 无新增抽象
- ❌ 无新增配置项（`maxBuffer` 是直接硬编码值，与 PMO 分类器保持一致）
- ❌ 无新增接口或扩展点
- ❌ 无新增依赖
- ❌ 无新增公共类型
- ❌ 无新造同义词

**实现方案**：仅在 line 133 的 `spawnSync` 第三个参数（options 对象）中添加 `maxBuffer: 64 * 1024 * 1024`，与 `encoding: 'utf8'` 并列。
