# Issue #348 模块运行态诊断验收证据

> 状态：PASS。记录日期：2026-07-23。结论覆盖分层回归及完整 Mango + 独立真实 MySQL + Admin Shell 的本机纵向链路；不代表生产实例或生产业务库已经 READY。

## 1. 验收范围

- 后端：模块安装与制品版本、Flyway 迁移、Resource 当前声明、Authorization menu/API、聚合状态与缓存/超时/single-flight。
- 安全：默认关闭的 Actuator endpoint；启用后只允许 loopback、header bearer、目标 app、目标 tenant 与 `diagnostic:read` 权限。
- 前端：Admin Shell 诊断 bootstrap、mango-link 已登记页面与实际前端版本；未登记页面作为反例。
- CLI：`mango module doctor mango-link` 的编排、严格响应校验、JSON 输出、退出码和无浏览器降级。
- 范围边界：单实例、本机 loopback、默认 `/actuator`、前后端同机端口访问、空或根 context path（`/`）、mango-link。远程管理面、集群聚合及 Issue #346 的 manifest/generator/lock 不在本次范围。

## 2. 执行环境与证据类型

- Java：Java 21；Spring Boot Actuator 与 Spring Security 使用真实进程内应用上下文和真实 endpoint，不用伪 Controller 替代。
- 数据：分层回归继续使用真实 H2/Flyway/MyBatis；纵向验收使用 MySQL `8.4.8` 和本任务独立数据库 `mango_issue348_runtime_20260723`，完整 `mango-monolith-app` 的 DB 与 Resource startup 均为 UP。
- Node：Node.js `v22.23.1`、pnpm `11.14.0`，符合仓库声明的 `>=22.23.1 <23`。
- 浏览器：项目 Playwright 提供的真实 headless Chromium；自动化正反例之外，源码 CLI 还连接真实 Admin Shell `127.0.0.1:14176`，五个 mango-link 页面均 PASS，实际前端版本为 `1.0.14`。
- HTTP：fixture 继续覆盖 callback、CSP、响应大小和协议负例；最终正例使用真实 `mango-monolith-app:15555`、真实 Admin Shell 和真实 browser challenge/callback，不以 fixture 代替纵向验收。
- 凭据：纵向验收通过真实 admin 登录取得短期 bearer token，权限由资源同步落为 `diagnostic:read`；报告不保存 token、nonce、callback URL、账号密码或主体信息。

## 3. 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| ISSUE-348 | TC-001 | Module API/Core/Starter | 统一五段诊断、状态聚合、版本、超时、缓存与 single-flight | 同 module/app/profile 的并发请求、required WARN、缺安装元数据、超时、取消、全局过载与多 key 并发刷新 | required WARN 聚合为 DEGRADED；缺失安装证据为 UNKNOWN；同 key 只执行一次；超限 fail-fast；缓存容量不超过上限；超时会取消 executor task；duration 安全收敛到 `0..24h` | 后端纯逻辑；逐项断言 condition、reasonCode、required、stale 与 evidence 边界 | 不涉及浏览器；三个模块分别 5、14、20 tests，failure/error 均为 0 | 九模块 `verify` 的 Surefire 汇总与 coordinator 并发回归 | PASS |
| ISSUE-348 | TC-002 | Persistence/Resource/Authorization contributor | 迁移、当前资源声明和授权快照 | H2 内存库、真实 Flyway/MyBatis、当前 fingerprint、app/tenant/permission/apiCode/resourceCode/accessMode | 观察失败不影响 Flyway 主流程；旧 PASS 不能替代当前声明；错误 app 不获得诊断权限；菜单/API 分别使用平台/全局权威租户并具备错误分区反例；诊断 Mapper 缺失时保持 UNKNOWN，不误报 READY | 后端分层集成；只输出低敏计数、fingerprint 和稳定 reasonCode | 不涉及浏览器；Persistence 87、Authorization Core 53、Resource Core 55、Authorization Starter 76 tests，failure/error 均为 0 | Maven Surefire；真实 H2/Flyway/MyBatis 集成测试输出 | PASS |
| ISSUE-348 | TC-003 | `/actuator/mangoModules` | 默认关闭与专用授权 | 匿名、query token、cookie、internal 标志、错误 bearer、缺权限、错误/重复 app、错误 tenant、非 loopback、localhost、POST、根 context path `/` | 默认不装配；启用后仅 loopback GET、header bearer、正确 app/tenant 和 `diagnostic:read` 可访问；空 context path 与 Spring 等价的 `/` 可装配，其他组合被拒绝 | 实际 Actuator endpoint 与最高优先级独立 SecurityFilterChain，不依赖业务页面 | 不涉及浏览器；真实 MockMvc 同时断言 `durationMs` 为 JSON number | `ModuleDiagnosticActuatorSecurityIntegrationTest` 与 Auth 组合回归 | PASS |
| ISSUE-348 | TC-004 | Admin Pages/Admin Shell bridge | 已登记页定向探测和前端实际版本 | mango-link 两个已登记 page key，诊断 challenge 与 callback | registrar、loader、Vue component、chunk 均成立；返回 frontend version `1.0.14`；响应 schema、condition 唯一性和 evidence 上限严格校验 | 诊断 bootstrap 不挂载登录 UI；通用 Shell 不固化会阻断远程 runtime entry 的 CSP | Admin Pages 3 tests、Admin Shell 52 tests；对应 package/app build 通过 | Vitest、Admin Pages build、Admin Shell package/default/enabled build 输出 | PASS |
| ISSUE-348 | TC-005 | Admin Shell 缺页反例 | 防止静态登记或错误映射假绿 | `link/not-registered/index` | 总状态返回 FAILED，`frontend.pageRuntime` 为 FAIL，reasonCode 为 `PAGE_NOT_REGISTERED` | Chromium 真实执行 registrar 和定向 probe | 正反例共 2 tests；普通资源 requestfailed、console error、pageerror 均进入失败判定 | CLI Chromium E2E TAP 输出 | PASS |
| ISSUE-348 | TC-006 | `mango module doctor mango-link` | CLI 编排、JSON、退出码与 loopback 限制 | HTTP fixture、真实 Chromium、临时 callback、大小/深度越界响应 | READY/exit 0；确定性失败 exit 1；未知、权限、网络、过载或浏览器不可用 exit 3；JSON stdout 仅一个对象 | CLI 驱动真实浏览器，不要求人工交互；callback 必须 `response.ok` | CLI 56 tests 通过；真实 Chromium 正反例 2 tests 通过 | Node TAP 与正式 `tests/e2e` 脚本输出 | PASS |
| ISSUE-348 | TC-007 | packed `@mango/cli` consumer | 打包制品可执行且不隐式下载 Chromium | npm pack 后的独立消费目录，无 Playwright 浏览器 | 实际执行打包后的 CLI bin；返回 UNKNOWN/exit 3；安装提示不含 token；未下载 Chromium | 消费者命令行路径可执行，不依赖仓库源码入口 | 不启动浏览器，不产生 Chromium 下载 | `check-module-doctor-packed.mjs` 输出 | PASS |
| ISSUE-348 | TC-008 | starter、构建、样式、格式与文档门禁 | 默认关闭兼容性及交付质量 | Business starter 76 required files、35 contract checks；diagnostics default/enabled 两种构建 | 默认关闭时不装配诊断入口、协调器、版本扫描和 Shell bridge；starter/full/custom/business 模板具备诊断类型与 bootstrap；README/source fact 与实现一致 | 样式和模块样式门禁通过；诊断 E2E HTTP CSP 使用严格 `script-src 'self'` | 构建仅保留仓库既有 Rollup chunk warning；未发现本次新增运行失败 | template、build、style、README、source-fact、diff 门禁输出 | PASS |
| ISSUE-348 | TC-009 | 完整 Mango + MySQL + Admin Shell + 源码 CLI | 真实纵向运行态诊断 | MySQL 8.4.8 独立库、完整 monolith、真实 admin token、源码 Admin Shell、五个 mango-link 页面 | CLI `READY`/exit 0；安装、Flyway、Resource、Authorization、页面运行态五项均 PASS；menu 7/7、API 34/34、页面 5/5；前端版本 `1.0.14` | Chromium 完成真实 challenge/callback；后端版本无可靠元数据时保持 UNKNOWN，不阻断五项 READY | health、DB、resourceStartup 均 UP；源码 CLI JSON schema 验证通过 | `/tmp` 运行输出仅作本机复核；本报告保留无敏聚合证据 | PASS |

## 4. 回归结果摘要

| 组件 | 验证结果 | 结论 |
|---|---|---|
| 九个 Maven 模块 | Module API 5、Core 14、Starter 20、Persistence 87、Resource API 无测试、Authorization Core 53、Resource Core 55、Authorization Starter 76、Auth 44；共 354 tests，failure/error 均为 0；本轮直接受影响模块及上游依赖另执行 `verify`，56 个 reactor 模块全部成功 | PASS |
| `@mango/admin-pages` | 3 tests；package build 通过 | PASS |
| `@mango/admin-shell` | 52 tests；package build、应用默认关闭 build、diagnostics enabled build 均通过 | PASS |
| `@mango/cli` | 56 tests；packed consumer 与真实 Chromium E2E 通过 | PASS |
| Business starter | 76 required files、35 contract checks | PASS |
| 静态、架构与文档门禁 | changed-mode 架构门禁 dependency/ArchUnit/PMD 均为 0、`blocking=0`；通用静态门禁 `newIssueCount=0`、`toolFailureCount=0`，保留 273 项基线 inventory，因此不声明“全仓零告警” | PASS |

## 5. 真实纵向证据与问题闭环

- MySQL：`8.4.8`；独立数据库共有 220 张业务表、20 张模块 Flyway history 表，`flyway_schema_history_link` 当前成功版本为 1；Authorization API 资源共 757 条，全部位于全局租户 `default`。
- CLI：源码执行 `mango module doctor mango-link` 返回 `READY`、exit 0；五个 required condition 全部 PASS。Authorization 证据为 menu expected 7/missing 0、API expected 34/missing 0；页面 expected 5/failed 0。
- 版本：前端实际版本 `1.0.14`，来源 `PAGE_REGISTRAR`；后端和 expected version 均无可靠 provider，因此明确 UNKNOWN，而非猜测版本或制造假 PASS。
- 闭环一：完整单体的 Spring root context path 配置值为 `/`，语义等价于空路径；装配条件现仅额外接受精确 `/`，仍拒绝非空业务路径、独立 management port 和非默认 Actuator base path。
- 闭环二：菜单属于平台租户 `1`，API 资源属于全局租户 `default`；诊断分别查各自权威域，并用“API 只存在 tenant 1 时必须 FAIL”的反例防止跨域假绿。
- 闭环三：Mango 全局将 `long` 序列化为字符串，但 CLI 合同要求 `durationMs` 为 JSON number。最终将 wire 字段定义为有界 `int`，保留 long 输入重载并统一钳制到 `0..24h`；不引入 Jackson 到纯 API 模块，也不放宽 CLI 接受错误字符串类型。
- 专家复核：架构意见坚持 wire number 且反对放宽 CLI；安全运维与可行性评审结合 API 模块依赖边界、全局 serializer 实测和完整纵向结果，接受当前有界整数实现。真实 MockMvc 与完整 CLI 均已证明最终 wire contract。

## 6. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 状态 |
|---|---|---|---|---|
| LibreOffice 文档转换插件 | 本机未安装 LibreOffice，完整单体以正式配置 `KK_OFFICE_PLUGIN_ENABLED=false` 禁用该插件 | 不影响模块诊断五段链路，但本报告不证明 Office 转换能力 | 在具备 LibreOffice 的文件预览专项环境单独验证 | 未验证 |
| 非根业务 context path、独立 management port 与非默认 Actuator base path | 首版只承诺默认 `/actuator`、同端口以及空或根 context path `/` | 这些部署形态可能需要补充 endpoint URL 与 callback 规则 | 有明确部署需求后单独设计并增加 E2E | 未验证 |
| 远程管理面、集群和多实例聚合 | 首版明确限制 loopback 单实例，避免扩大凭据、SSRF 与实例归属风险 | 当前结果只代表被访问的本机单实例 | 另立需求处理 TLS、owner、实例身份和聚合语义 | 未验证 |

## 7. 业务开发交接

| 输出对象 | 获得能力 | 使用入口 | 失败语义与边界 |
|---|---|---|---|
| Mango 平台维护者 | 统一模块运行态诊断模型、稳定 condition/reasonCode、required/stale/UNKNOWN 语义，以及后端制品与前端实际版本 | Module Diagnostic API/Core/Starter 扩展点和各模块 contributor | 默认关闭、不影响 readiness；实例证据不足时保守 UNKNOWN，禁止假 READY |
| 基于 Mango 的业务开发者 | 一条 CLI 检查安装、迁移、资源、授权、页面运行态及前后端版本 | `mango module doctor mango-link --app internal-admin --backend-url <loopback> --frontend-url <loopback> --project-dir <admin-shell>` | exit 1 为确定性失败；exit 3 为证据未知、权限、网络、过载或浏览器不可用；首版只支持本文列出的 loopback 部署边界 |
