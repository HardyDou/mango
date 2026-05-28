# Mango 产品化 Sprint 1 前端质量修复台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| S1-FE-001 | Issue #26 #4 | 修复 `@mango/common` CodeEditor 在 Vite dev 下 `CodeMirror is not defined` | 将 CodeMirror 5 core、modes 和 addons 收敛为组件运行时统一加载，避免 Vite dev 预构建拆分后 addon 找不到 CodeMirror 全局对象 | `mango-ui/packages/common/components/CodeEditor/index.vue` | CodeEditor Vite dev 浏览器冒烟可挂载 `.CodeMirror`，且无 `CodeMirror is not defined` | DONE | Playwright smoke：`codeMirrorCount: 1`、`codeMirrorErrors: []`；`pnpm -F mango-admin build` |
| S1-FE-002 | Issue #26 #14 | 解决 `@mango/workflow-business-example` 静态和动态导入混用导致的构建 warning | 默认页面注册仍动态加载业务页面；业务审批组件注册改为异步按需导入，避免同一个包在 `admin-pages` 中既静态又动态导入 | `mango-ui/packages/admin-pages/src/defaults.ts` | 前端构建不再出现该包 static/dynamic import warning；workflow business form 页面仍可动态加载 | DONE | `pnpm exec vite build` in `mango-ui/packages/admin-pages`；`pnpm -F mango-admin build`；build log scan returned no target warning |
| S1-FE-003 | PMO 要求 | 本 Sprint 必须可验证并更新台账 | 代码完成后执行前端构建、浏览器冒烟和交付台账检查；现有 CodeEditor 单测入口因根级解析不到 `@vue/test-utils` 未作为完成证据 | 本台账 | delivery-contract-check verify 通过 | DONE | `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-05-27-mango-productization-sprint-1-frontend-quality.md --ledger mango-docs/plans/2026-05-27-mango-productization-sprint-1-frontend-quality-ledger.md --mode verify` |
