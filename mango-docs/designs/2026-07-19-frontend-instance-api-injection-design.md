# 前端业务 API 实例级注入设计

## 1. 目的

修复 Mango CLI 和业务 starter 生成模块使用模块级 API 单例的问题，确保同一 JavaScript realm 内运行多个单体或 Wujie 应用实例时，`baseUrl`、token、tenant、trace 和取消信号不会跨实例串用。

本设计只调整业务 UI 包与 host 的运行时装配，不改变后端 HTTP 接口，也不让 Vue 页面直接操作 Axios 或中立 `HttpClient`。

## 2. 术语和边界

- `HttpClient` 是 `@mango/api-schema` 提供的中立请求端口，只描述请求方法、相对 URL、query、body、header 和 `AbortSignal`。
- `@mango/http-client` 是默认 Axios adapter。Axios、token、tenant、base URL 和环境配置只存在于 host 装配层。
- `packages/<module>-api` 只导出 DTO、Query、Command、VO 和 `createXxxApi(client)`，不得依赖 Vue、Axios、Element Plus、router 或运行时环境。
- `packages/<module>` 是业务 UI 包，可以使用 Vue composable 获取当前应用实例注入的客户端，但页面只取得 `XxxApi`，不直接调用 `HttpClient`。
- `registerXxxPages()` 只登记页面和业务元数据，不保存客户端或其它应用实例状态。

## 3. 方案选择

### 3.1 采用方案：共享客户端注入键与业务 API composable

host 为每个 Vue `App` 创建独立 `HttpClient`，并通过 `MANGO_HTTP_CLIENT_KEY` 注入。业务 UI 包提供 `useXxxApi()`：在当前组件实例中注入客户端，再通过 `WeakMap<HttpClient, XxxApi>` 获取或创建业务 API。

采用原因：

- 与 Mango CMS 已验证的实例级模式一致；
- 单体与 Wujie 使用同一页面和 API factory；
- 同一 realm 的多个 Vue App 通过各自 provide/inject 上下文隔离；
- `WeakMap` 不保存 token 等敏感数据，不阻止实例回收；
- 页面无法绕过业务 API factory 直接拼装请求。

### 3.2 未采用方案

1. 业务包自有 `InjectionKey` 和 `installXxxApi(app, client)`：隔离成立，但每个业务模块都要求 host 维护额外安装步骤和 manifest 元数据，重复装配较多。
2. 通过 route meta、props 或全局 store 传递 API：动态页面注册链路复杂，且容易把敏感上下文扩散到路由或宿主状态。
3. 保留模块级单例并在挂载时覆盖：无法证明多应用实例隔离，直接违反业务 API 规范。

## 4. 运行时数据流

```text
host entry
  -> createMangoHttpClient(instance configuration)
  -> createMangoAdminApp(...)
  -> app.provide(MANGO_HTTP_CLIENT_KEY, instanceClient)
  -> app.mount()

business page setup
  -> useXxxApi()
  -> inject current App's HttpClient
  -> WeakMap lookup/createXxxApi(client)
  -> typed business API method
  -> relative endpoint request
```

Wujie 微应用由每个微应用 Vue App 注入其 runtime client；单体 host 注入本地 client。两种形态不维护两套业务 API 或页面实现。

## 5. 失败行为

- 页面未获得实例级客户端时，`useXxxApi()` 立即抛出可定位的配置错误，不回退到模块全局变量、默认 Axios 或宿主私有对象。
- 不在 import 阶段创建 API、发起请求或注册副作用。
- `AbortSignal`、陈旧响应隔离和 `HttpError` 处理保持现有业务 API 合同。
- 不为兼容旧模板保留 `configureXxxApi()`、`getXxxApi()` 或可变模块状态。

## 6. 生成模板与业务引用

- CLI `business-module` 模板和 `mango-business-starter` canonical 模板同步采用 `useXxxApi()`。
- 生成项目主入口先创建 app 实例，再向该 app 注入业务客户端，最后挂载。
- 新增模块的 feature registrar 调用 `registerXxxPages()`，不再捕获或传入客户端。
- 业务 UI package 显式声明其直接使用的运行时依赖；不得依赖未声明的传递依赖。
- npm tarball 消费者必须从 package 公开 API 和 `style.css` 引用模块，不读取 Mango workspace 源码。

## 7. 防回归验证

### 7.1 单元与模板检查

- 生成两个 Vue App，分别 provide client A 和 client B；两个页面/composable 必须调用各自 client，第二个实例不得覆盖第一个。
- 缺少 provider 时必须 fail-closed。
- CLI 模板检查禁止可变模块级 `XxxApi`、`configureXxxApi()` 和 `getXxxApi()`，并要求 `inject`、`WeakMap`、`useXxxApi()` 和 host `provide`。
- business starter 机械投影与模板检查必须同步通过。

### 7.2 候选与浏览器验证

- 重跑固定镜像的 exact-source candidate、29 个 tarball 干净消费者和断网 Business Lab。
- 重跑真实后端下的 8 个 Playwright 用例。
- 浏览器结果输出结构化 JSON，记录完整 commit、Git tree、用例名称、状态、开始/结束时间和失败摘要；证据不得只引用忽略目录中的 `.last-run.json`。

### 7.3 文档语义

- 历史阶段设计和证据必须明确标记被采用边界 ADR 与最终候选证据取代。
- `pilot -> affected -> repository` 只表示规范采用范围。
- npm/Nexus/tag/GitHub Release 属于不可变制品发布；应用部署、流量观察、故障注入和生产回滚属于独立业务部署/运维合同。两者都不是前端代码规范本地验证的完成条件。

## 8. 验收标准

1. 生成代码不存在保存业务 API 或 `HttpClient` 的可变模块级单例。
2. 双 App 实例测试证明 client、tenant 和 base URL 不串用。
3. 页面只通过 `useXxxApi()` 获取业务 API；Axios 不进入页面或 `*-api` 包。
4. CLI、starter、PMO 投影、静态边界、类型、单测、构建、tarball consumer 和封闭 Business Lab 全部通过。
5. 真实后端 Playwright 8/8 通过，并生成绑定候选 commit/tree 的结构化报告。
6. 历史文档不再把 5%/25% 流量、故障注入或生产回滚写成前端规范毕业条件。
7. 只有上述结果绑定同一精确源码候选后，状态才恢复为 `STANDARD_VALIDATED_LOCAL`。

## 9. 非目标

- 不发布 npm/Maven 制品，不创建 tag 或 GitHub Release。
- 不部署业务应用，不执行生产流量灰度或故障注入。
- 不清零本轮未触及的历史 ESLint、Stylelint、typecheck、组件分类和 bundle 债务。
- 不修改 `@mango/workflow`；它与本设计无关。
