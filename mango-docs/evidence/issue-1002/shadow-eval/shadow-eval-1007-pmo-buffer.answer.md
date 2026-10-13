Read both rule files plus the classifier, the baseline projection, the sync script, and the sibling precedent. Plan only — no files touched.

## 事实基线（先说结论依据）

- 源文件：`mango-pmo/tools/classify-pmo-check-scope.mjs`（第 312 行 `changedFiles()`）
- 投影文件：`mango-business-starter/business-pmo/mango-baseline/tools/classify-pmo-check-scope.mjs`
- `diff -q` 两者当前字节一致（rc=0），并在 `mango-business-starter/business-pmo/mango-baseline/baseline.json:1199` 以 `sha256`+`size` 登记
- 失败点：第 312 行 `execFileSync('git', ['diff','--name-only', ...], { cwd: root, encoding: 'utf8' })` **未设 `maxBuffer`**，走 Node 子进程默认 1 MiB 上限；大型文档清理 PR 的文件名列表超限后抛 `ENOBUFS`，被 `runScopeClassifierCli` 的 catch 转成 `return 1`，`preflight_scope` 直接失败，永远到不了 `classifyChangedFiles`
- 同仓先例（同性质修复）：`5fde930fd fix: handle large capability diffs in PR checks`，只改三处 —— `mango-pmo/tools/check-capability-docs.mjs` 的 `gitOutput()` 加 `maxBuffer: 64 * 1024 * 1024`、baseline 投影副本、`baseline.json`
- 更早的同类常量：`mango-pmo/tools/check-architecture-debt-budget.mjs:10` `GIT_OUTPUT_MAX_BYTES = 64 * 1024 * 1024`（第 606 行使用）

---

## 1. 复用判断（REDLINE-REUSE-010）

按顺序结论：**停在标准库层，不需要新增任何结构。**

1. **Mango 已有能力**：`GIT_OUTPUT_MAX_BYTES = 64 * 1024 * 1024`（`check-architecture-debt-budget.mjs:10`）与 `check-capability-docs.mjs` 的 `gitOutput()` 内联 `maxBuffer` 就是同一问题的既有解法，取值已统一为 64 MiB，直接沿用该数值。
2. **当前模块已有实现**：`classify-pmo-check-scope.mjs` 自身已用 `execFileSync`（第 18 行、第 312 行），在既有调用点补选项即可，不需要新建 `runGit`/`gitOutput` 包装函数。
3. **标准库**：`node:child_process` 的 `maxBuffer` 选项本身即所需能力。
4. **已装依赖**：不需要，也没有任何依赖能替代。
5. **新增结构**：**不需要**。不新增 helper、不新增共享 git 工具模块、不新增配置文件项。

被放弃的复用对象与原因（REDLINE-REUSE-010 要求）：**无**——不存在被放弃的选项，因为复用已完整满足需求。

需要留意的判断点：`check-architecture-debt-budget.mjs` 用了**命名常量**，`check-capability-docs.mjs` 用的是**内联字面量**。本任务仅一处使用，且紧邻性质的先例（同一类 diff 缓冲问题）选的是内联字面量，应按后者，避免为单点使用引入新标识符。

## 2. 术语（REDLINE-TERMS-030）

- **不引入**任何新的公共类型、API、配置项、权限码、事件、页面 key、Resource 类型或抽象概念。
- `maxBuffer` 是 Node 标准库选项名，不是 Mango 术语，不进入 PMO 术语表，也不产生同义词。
- 取值 `64 * 1024 * 1024` 与既有两处一致，不制造"同一概念多个数值"的表述分歧。
- **不需要 ASK**：无术语冲突、无主名称不确定、无重命名。若实施时产生偏差（例如有人主张为分类器单独引入 `SCOPE_GIT_MAX_BYTES` 之类新标识符），那才落入 REDLINE-TERMS-030 / REDLINE-OVERDESIGN-060 的判定范围，届时需 ASK。

## 3. 最小实现（REDLINE-MINIMAL-020）

**改动边界**（三处，与 `5fde930fd` 同构）：

1. `mango-pmo/tools/classify-pmo-check-scope.mjs` —— 仅在第 312 行 `changedFiles()` 的 `execFileSync` 选项袋中增加 `maxBuffer: 64 * 1024 * 1024`。函数签名、返回结构、`classifyChangedFiles` / `resolveMavenScope` 的全部输出键不变。
2. `mango-business-starter/business-pmo/mango-baseline/tools/classify-pmo-check-scope.mjs` —— 投影副本，经 `node mango-business-starter/scripts/sync-pmo-baseline.mjs --write` 生成，保持与源字节一致。
3. `mango-business-starter/business-pmo/mango-baseline/baseline.json` —— 由构建/sync 重新生成 `tools/classify-pmo-check-scope.mjs` 的 `sha256`、`size`，以及顶层 `sourceCommit`、`bundleSha256`。**不手工改哈希**。

**责任边界**：
- 适用：`git diff --name-only <base>...<head>` 输出超过子进程默认缓冲的场景（大型文档清理 PR）。
- 不适用：第 18 行 `resolveRepositoryRoot` 的 `git rev-parse --show-toplevel` —— 输出只有一个仓库根路径，没有任何证据表明会溢出，**不改**（改了即为无依据改动）。
- 范围外：不调大其他工具的缓冲区，不改分类规则，不动 `resolveMavenScope` 的 POM 扫描，不引入流式/分页读取。

**当前消费者**（均已存在，无新增消费者）：
- `.github/workflows/pmo-doc-check.yml:67`
- `mango-ui/packages/mango-cli/templates/full/.github/workflows/pmo-doc-check.yml:51`
- `mango-ui/packages/mango-cli/templates/full/.gitea/workflows/pmo-doc-check.yml:47`
- `mango-ui/scripts/release/run-local-release-checks.mjs:31`
- 字符串钉住（本次不改其内容）：`mango-pmo/tools/check-governance-intent.mjs:209`、`mango-ui/packages/mango-cli/scripts/check-cli.mjs:462`

**验证入口**：
- `node mango-pmo/tools/classify-pmo-check-scope.mjs --base <base-sha> --head <head-sha>` —— 在触发本次需求的真实大 PR 上直接复现，确认从"收集阶段失败"变为输出完整 scope JSON。
- 新增回归测试到 `mango-pmo/tests/pmo-check-scope.test.mjs`（该文件已在 CI 执行：`.github/workflows/pmo-doc-check.yml:133`、`run-local-release-checks.mjs:71`）。做法对齐 `mango-pmo/tests/architecture-debt-budget.test.mjs:655` 的 "CI base-ref reads a budget larger than the child-process default buffer"：建真实 git fixture，构造文件名列表总量 > 1 MiB 的改动，断言分类器退出码为 0 且输出包含预期 scope 键。**不 mock git**，避免用替身冒充真实链路。
- `node mango-business-starter/scripts/sync-pmo-baseline.mjs`（check 模式，不带 `--write`）—— 证明投影与 canonical 一致、哈希登记正确。
- `pnpm -F @mango/pmo check` —— 校验包内基线与清单。
- 按 `AGENTS.md` §2，实施前先跑 `pmo-preflight.mjs --role dev --phase develop --reuseCurrentTask false`，`--paths` 覆盖上述三处。

## 4. 简单任务问卷（REDLINE-QUEST-050）

**不需要。** 本任务是单点缺陷修复：无新增公共概念、无认知风险、无待定术语，因此不生成固定问卷、无消费者文档或观察目标缺失的测试。第 3 项新增的回归测试有明确观察目标（> 1 MiB 真实 diff 下分类器可达），不属于本条款禁止的"无观察目标的测试"。

## 5. 过度设计（REDLINE-OVERDESIGN-060）

按最小方案实施时**均不触发**。需显式规避的越界写法：

- ❌ 新建 `GIT_OUTPUT_MAX_BYTES` / `SCOPE_GIT_MAX_BYTES` 等命名常量而仅单点使用 —— 单模块内部使用的标识符，收益仅"看起来整洁"。
- ❌ 新建 `mango-pmo/tools/lib/git-output.mjs` 之类共享 git helper 供两个工具复用 —— 本 PR 不产生该抽象的迁移需求。
- ❌ 引入 `stream`/`spawn` 流式收集、分页 diff、`--no-renames` 等额外机制 —— 超出"缓冲区不足"这一既有证据。
- ❌ 新增任何 npm 依赖（无目标能力缺口）。
- ⚠️ 需与评审确认的唯一取舍：内联字面量 vs 命名常量。本方案选**内联字面量 64 MiB**，理由是紧邻先例 `5fde930fd` 同法、取同一数值、单点使用；若评审坚持与 `check-architecture-debt-budget.mjs` 的命名常量风格统一，则仍只增一个文件内局部常量，且不得跨模块导出为公共标识。

**闭环风险**：投影文件与 `baseline.json` 若不随源文件同步更新，`sync-pmo-baseline.mjs` check 与 `check-package.mjs` 会因哈希不一致失败——这类"只改一半"正是本次要避免的，故三处必须同一 PR 内完成。
