# Issue #96 Element Plus 2.14.x 统一升级回归报告

## 1. 目标

- 将 `mango-ui`、`mango-business-starter` 和 `mango-cli` 模板中的 Element Plus 依赖统一到 2.14.x。
- 固化发布检查，防止后续重新出现 Element Plus 或 icons-vue 版本不一致。
- 验证生产构建不会因 Element Plus 2.14.x 出现白屏。

## 2. 改动范围

- `element-plus` 统一为 `2.14.1`。
- `@element-plus/icons-vue` 统一为 `2.3.2`。
- `mango-ui/pnpm-lock.yaml` 按新版本刷新。
- `mango-ui/packages/mango-cli/scripts/check-release-versions.mjs` 新增 UI 依赖锁定检查，覆盖 `mango-ui/apps`、`mango-ui/packages`、`mango-ui/packages/mango-cli/templates` 和 `mango-business-starter`。
- `mango-ui/apps/mango-admin/vite.config.ts` 移除旧的 `element-plus` / `@element-plus/*` 独立 manual chunk，避免升级后与 Vue chunk 形成循环初始化。

## 3. 验证命令

```bash
pnpm --dir mango-ui install --lockfile-only
pnpm --dir mango-ui install --frozen-lockfile
pnpm --dir mango-ui/packages/mango-cli test
pnpm --dir mango-ui admin:styles:check
pnpm --dir mango-ui -F @mango/common build
pnpm --dir mango-ui -F @mango/admin build
pnpm --dir mango-ui -F mango-admin-shell build
pnpm --dir mango-ui -F mango-admin build
pnpm --dir mango-ui --filter @mango/file --filter @mango/workflow --filter @mango/notice --filter @mango/template --filter @mango/calendar --filter @mango/numgen build
```

版本扫描：

```bash
rg -n 'element-plus.*2\\.5\\.5|element-plus.*2\\.14\\.0|@element-plus/icons-vue.*\\^' mango-ui mango-business-starter
```

生产预览：

```bash
pnpm --dir mango-ui -F mango-admin preview --host 127.0.0.1 --port 7957 --strictPort
```

Playwright 页面检查：

- 访问地址：`http://127.0.0.1:7957/`
- 验证对象：生产构建登录页
- 验证断言：页面非空、`#app` 非空、无 `pageerror`、无 console error、无 4xx/5xx 资源响应
- 测试隔离：仅在浏览器验证脚本中用 `page.route` 替换 `/api/system/tenant/login-options`，避免本地后端不可用导致登录机构接口 500 噪声

## 4. 验证结果

- 依赖安装与 lockfile 刷新通过。
- CLI 测试通过。
- 样式检查通过。
- 公共包、后台基座、后台应用和业务模块构建通过。
- 版本扫描未发现旧版本声明。
- 生产预览白屏问题已修复，最终浏览器检查无 JS 错误、无失败响应。

## 5. UI 证据

- 依赖版本截图：`dependency-summary.png`
- 构建结果截图：`build-summary.png`
- 生产预览截图：`mango-admin-preview.png`

## 6. 问题记录

- 初始生产预览出现 `Cannot access 'X' before initialization`，页面空白。
- 产物分析发现 `vue` chunk、`element-plus` chunk 和 `icons` chunk 存在循环导入。
- 移除 Element Plus 相关独立 manual chunk 后，生产包恢复正常渲染。

## 7. 风险与未验证项

- 生产包仍存在大 chunk 警告，属于既有后台应用体积问题，本次未扩大处理。
- 本次未连接真实后端做登录链路回归；生产预览的后端接口用浏览器路由桩隔离，仅用于验证前端构建运行态。
- 未执行全量三浏览器 E2E；本次执行的是与依赖升级相关的构建、版本一致性和 Chromium 生产预览检查。
