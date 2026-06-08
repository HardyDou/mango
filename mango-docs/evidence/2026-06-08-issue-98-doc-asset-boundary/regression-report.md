# Issue #98 回归测试报告

- 日期：2026-06-08
- 分支：feature/issue-98-doc-asset-boundary
- Issue：#98 治理：明确规范、使用说明与设计文档的归档边界
- 范围：PMO 文档资产归档规则、preflight 规则索引、mango-docs 入口链接

## 改动验证

新增 `mango-pmo/rules/06-document-assets.md`，明确规范、角色约束、使用说明、设计文档、Sprint 计划和交付记录的归档边界。

更新 `mango-pmo/rules/index.json`，使 PMO governance preflight 和文档归档关键词能够加载该规则。

更新 `mango-docs/README.md`、`mango-docs/index.md`，只增加 PMO 规则链接，不复制长期规则正文。

## 验证命令

```bash
node mango-pmo/tools/pmo-preflight.mjs --role pmo --phase governance --task "Issue #98 治理：明确规范、使用说明与设计文档的归档边界" --paths "mango-pmo/**,mango-docs/**"
```

结果：通过。Must read 包含 `rules/06-document-assets.md`。

```bash
node -e "JSON.parse(require('fs').readFileSync('mango-pmo/rules/index.json','utf8')); console.log('index json ok')"
```

结果：通过。`index json ok`。

```bash
rg -n 'process\.documentAssets|06-document-assets|文档资产归档边界|多数据源说明落位|唯一长期规范源|禁止在 .?mango-docs.? 新增规范文件' mango-pmo/rules/index.json mango-pmo/rules/06-document-assets.md mango-docs/README.md mango-docs/index.md
```

结果：通过。命中新规则、preflight 索引、docs 入口链接和多数据源落位说明。

```bash
pnpm exec playwright screenshot file:///Users/hardy/Work/mango/.mango/worktrees/issue-98-doc-asset-boundary/mango-pmo/rules/06-document-assets.md /Users/hardy/Work/mango/.mango/worktrees/issue-98-doc-asset-boundary/mango-docs/evidence/2026-06-08-issue-98-doc-asset-boundary/document-assets-rule.png

pnpm exec playwright screenshot file:///Users/hardy/Work/mango/.mango/worktrees/issue-98-doc-asset-boundary/mango-docs/index.md /Users/hardy/Work/mango/.mango/worktrees/issue-98-doc-asset-boundary/mango-docs/evidence/2026-06-08-issue-98-doc-asset-boundary/docs-index-link.png
```

结果：通过。截图已生成。

## 截图

- `document-assets-rule.png`：新增 PMO 文档资产归档规则。
- `docs-index-link.png`：`mango-docs` 索引仅链接 PMO 规则源。

## 回归结论

- 文档资产边界已收口到 `mango-pmo/rules/06-document-assets.md`。
- `mango-docs` 未复制长期规则，只保留规范源链接。
- PMO preflight 能加载新增规则。

## 未覆盖项

- 本次为治理文档改动，不涉及服务启动、数据库、API 或 UI 功能链路。
- 首次截图命令在 #98 worktree 中执行失败，原因是该 worktree 未安装前端依赖；已改用主工作区已安装的 Playwright CLI 截取 #98 worktree 文件，最终截图通过。
