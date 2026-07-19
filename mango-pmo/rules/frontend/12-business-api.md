# 前端业务 API 规范

## 1. 适用范围

- 适用于 Mango CLI/starter 新生成的业务模块和业务项目新增的前端 API。
- 适用于单体、Wujie 微前端和 npm 独立消费三种形态。
- 存量 Mango 领域包内的 `src/api` 按边界基线渐进迁移；修改时不得新增直接传输层或 UI 依赖。

## 2. 目录与职责

- 新业务模块的 API 固定放在 `frontend/packages/<module>-api`。
- `*-api` 包只包含 DTO、Query、Command、VO、错误类型和 `createXxxApi(client)` factory。
- 页面、组件、composable、store、router、样式和运行时装配放在业务 UI 包，不得进入 `*-api`。
- Vue 注入和 API 实例缓存属于业务 UI 包的 composable；API factory 本身不得依赖 Vue。

## 3. 请求合同

- `*-api` 只能依赖 `@mango/api-schema` 的 `HttpClient`、`HttpError`、`ApiId` 等中立合同。
- host 为每个应用实例创建 `HttpClient`，注入 token、租户、trace 和 base URL；业务页面不得创建 Axios 实例。
- API endpoint 使用相对路径，禁止读取 `import.meta.env`、`process.env`、router 或宿主全局对象拼接地址。
- 可取消的读取、搜索、分页和树查询必须接受并传递 `AbortSignal`。
- 页面重复查询或卸载必须取消在途请求，并拒绝陈旧响应覆盖新状态。
- HTTP 错误统一转换为 `HttpError`；API factory 不直接展示 UI 消息。

## 4. 依赖边界

`*-api` 禁止依赖：

- `vue`、`vue-router`、Pinia；
- Element Plus 和其它 UI 库；
- Axios、Fetch wrapper 或 `@mango/common/utils/request`；
- `apps/*`、宿主 store、菜单、权限装配和微前端厂商对象；
- 业务环境变量和绝对服务地址。

Axios 只能存在于 `@mango/http-client` 适配器。Wujie 只能存在于 `@mango/app-runtime` adapter；二者均不得进入业务 API。

## 5. 业务 UI 使用方式

- 单体入口创建本地实例级 `HttpClient` 并注入。
- 微前端入口复用 runtime 提供的实例级 `HttpClient`。
- 页面通过 composable 获取当前实例 API，不得从模块级单例读取 token、租户或 base URL。
- 同一业务包在单体和微前端下使用相同 API factory 和页面代码。

## 6. 验证

- `pnpm frontend-boundaries:check`
- `pnpm architecture:check`
- API factory 单元测试覆盖参数、错误和 `AbortSignal`。
- 至少一个独立消费者验证安装、类型检查和生产构建。
- 涉及页面请求生命周期时，按前端测试规范执行对应组件或浏览器验证。

## 7. 禁止事项

- 在 `.vue` 页面内编写 Axios/Fetch 请求实现。
- 在 `*-api` 包中使用 Vue provide/inject。
- 用全局 Axios 单例共享不同应用实例的 token、租户或 base URL。
- 请求被取消或已过期后继续写入页面状态。
- 为兼容单体或微前端分别维护两套 API 实现。
