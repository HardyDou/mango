# Mango UI Agent 入口

进入 `mango-ui` 前端子项目后，仍以根目录 `AGENTS.md` 的 PMO preflight 为第一入口。

## 1. 推荐 preflight

前端开发、修复或重构任务优先执行：

```bash
node ../mango-pmo/tools/pmo-preflight.mjs \
  --role dev \
  --phase develop \
  --task "<用户任务>" \
  --paths "mango-ui/**"
```

涉及验收或 E2E 时使用 `--role qa --phase verify`。

## 2. 前端默认必读

preflight 至少应覆盖：

- `rules/00-dev-flow.md`
- `rules/frontend/05-dev-flow.md`
- `rules/frontend/01-vue-code.md`
- `rules/frontend/06-monorepo-architecture.md`
- `rules/frontend/04-test.md`

## 3. 前端硬约束

- 先判断改动属于 `apps`、业务包还是公共包。
- 禁止 `packages/common` 依赖宿主应用。
- 公共能力必须从公共包导出，不保留第二份长期实现。
- 后端 ID、雪花 ID、业务主键在前端统一按字符串处理。
- 菜单组件加载必须使用可被 Vite 静态分析的映射或 `import.meta.glob()`。
