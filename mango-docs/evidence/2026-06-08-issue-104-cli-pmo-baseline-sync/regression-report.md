# Issue 104 回归测试报告

## 范围

- Issue: #104 CLI 支持现有业务项目同步/升级 Mango PMO baseline
- 分支: `feature/issue-104-cli-pmo-baseline-sync`
- 日期: 2026-06-08

## 变更验证

| 场景 | 验证方式 | 关键断言 | 结果 |
|---|---|---|---|
| `mango pmo sync --dry-run` | CLI 临时业务项目回归 | 输出 add/update/skip/warn 计划；不创建 `business-pmo` | PASS |
| 新业务项目 baseline sync | CLI 临时业务项目回归 | 生成 `business-pmo/mango-baseline/**`、`business-pmo/README.md`、缺失的 `business-docs/plans/` 示例 | PASS |
| baseline preflight | 运行同步后的 `business-pmo/mango-baseline/tools/pmo-preflight.mjs` | `--paths "backend/**,frontend/**"` 可执行，并加载后端开发流与前端测试规范 | PASS |
| 最新 DB 命名规则 | 读取同步后的 `rules/backend/07-persistence.md` | 包含 `mango_{module}`、禁止 `job/system/file` 这类无前缀库名 | PASS |
| 业务规则保护 | 同步前写入 `business-pmo/rules/domain/01-owned.md` | 同步后内容保持不变 | PASS |
| 业务文档保护 | 同步前写入 `business-docs/plans/example-ledger.md` | 同步后内容保持不变，只补缺失示例 | PASS |
| 根 `AGENTS.md` 迁移 | legacy 文件引用 `/Users/.../mango-pmo` | 默认只 warn；加 `--write-agents` 后迁移为项目本地 baseline 入口 | PASS |
| 历史 CLI 功能回归 | `full/custom/add/module` 现有验收脚本 | init、add、module add、生成项目 baseline 验收全部通过 | PASS |

## 执行命令

```bash
pnpm --filter @mango/cli test
git diff --check
```

## 结果摘要

```text
release-versions.json matches 15 local package versions.
mango-cli full/custom/add/module/pmo sync checks passed.
```

## UI / 截图识别

- 本次为 CLI 功能，无新增浏览器业务页面。
- 生成本报告 HTML 并通过 Playwright 截图保存，用作回归测试报告截图证据。

## 未验证项与风险

- 未连接真实外部业务仓执行同步；已用 CLI 临时业务项目覆盖文件计划、写入、保护和 preflight 行为。
- 未发布 npm 包；本次仅验证源码 CLI。
