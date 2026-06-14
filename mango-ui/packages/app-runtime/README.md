# 应用运行时

## 1. 能力定位

提供普通应用和 Vue 微前端运行时封装。

主要使用者：前端开发者、业务开发者和 AI Agent。

## 2. 适用场景

业务子应用需要接入 Mango Shell 或独立运行时使用。

## 3. 不适用场景

不负责业务页面和后端权限。

## 4. 模块边界

包名：`@mango/app-runtime`。本包只提供前端运行时、页面、组件、API 封装或页面注册能力，不改变后端接口契约。

## 5. 接入方式

在业务前端或 Mango 前端包中引入 `@mango/app-runtime`；如包导出 `./admin-pages`，需要同步注册到统一页面注册表。

## 6. 配置项

配置来自业务应用 Vite、Shell runtimeConfig、后端 API baseURL 和包导出的注册入口；本 README 不复制长期前端规则。

## 7. 对外接口 / 扩展点

公开入口以 `package.json` exports 和 `src/index.ts` 为准；页面包通常额外导出 `src/admin-pages.ts`。

## 8. 数据库 / 初始化数据

无前端数据库。菜单、权限和初始化数据由对应后端模块或 business starter 维护。

## 9. 菜单 / 权限 / 租户

前端只负责页面注册、菜单 component 映射和交互展示；权限、租户和数据归属由后端接口校验。

## 10. 验证方式

```bash
node - <<'NODE'
const fs = require('fs');
const pkg = JSON.parse(fs.readFileSync('mango-ui/packages/app-runtime/package.json', 'utf8'));
const src = fs.readFileSync('mango-ui/packages/app-runtime/src/index.ts', 'utf8');
if (pkg.exports['.'] !== './src/index.ts' || pkg.exports['./vue-micro'] !== './src/vue-micro.ts') process.exit(1);
for (const token of ['normalizeRuntimeConfig', 'isValidRuntimeEntry', 'MangoRuntimeConfigError']) {
  if (!src.includes(token)) process.exit(1);
}
NODE
```

## 11. 业务接入最小闭环

业务子应用通过 runtime 挂载，校验 bootstrap/mount/unmount 和 Shell 注入上下文。

## 12. 常见问题

如果页面打不开，先检查包是否构建、样式是否引入、菜单 component 是否注册、后端 API 是否返回真实数据。

## 13. 关联 PMO 规则

- [前端模块规范](../../../mango-pmo/rules/frontend/01-vue-code.md)
- [前端测试规范](../../../mango-pmo/rules/frontend/04-test.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [能力地图](../../../mango-docs/capabilities/README.md)
