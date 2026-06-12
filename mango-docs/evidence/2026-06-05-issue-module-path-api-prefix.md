# Issue: 明确禁止将边缘代理前缀 `/api` 写入后端模块契约

## 背景

业务项目接入 Mango 模块规范时，出现将浏览器侧代理路径 `/api/guarantee` 写入后端 `module-path`、Controller 根路径和权限资源路径的情况。

按现有前端/nginx 约定：

- 浏览器请求使用 `/api/**`。
- Vite、nginx 或网关负责将 `/api` 前缀 rewrite 掉。
- 后端真实模块路径应为 `/guarantee`、`/system`、`/auth` 这类模块路径。

## Mango 主仓核对

已核对 Mango 主仓与业务模块模板：

- `mango-ui/packages/mango-cli/templates/business-module/.../module.properties` 使用 `module-path={{moduleKebab}}`，没有 `/api` 前缀。
- 平台模块使用 `/system`、`/auth`、`/authorization`、`/numgen`、`/file` 等真实模块路径。
- 搜索到的 `/api-resources` 属于授权资源业务名，不是 `/api` 代理前缀。

## 问题

当前规范说明了：

- `module-path` 表示模块正向调用前缀。
- Controller 根路径必须以 `module-path` 开头。
- Feign path 必须以目标模块 `module-path` 开头。

但规范没有明确说明：

- `/api` 是边缘代理前缀，不属于后端模块路径。
- 后端 `module-path`、Controller 根路径、Feign path、权限资源路径禁止包含 `/api`。
- 前端/nginx/Vite 需要在代理层 rewrite `/api`。

这会导致业务项目在迁移时把外部访问路径误登记为模块路径。

## 建议

在 Mango PMO 规范和 CLI 生成模板说明中补充：

- `/api` 仅允许作为浏览器侧、网关侧、nginx 或 dev-server 代理前缀。
- 后端 `module-path`、Controller 根路径、Feign path、权限资源路径必须使用 rewrite 后的真实模块路径。
- CLI 生成业务模块时，如果用户输入的模块路径以 `/api/` 开头，应提示或拒绝。
