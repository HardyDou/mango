# Phase 4 Web/Security Context Delivery Record

## 范围

- 主模块：`mango-infra-web`、`mango-infra-security`
- 被动适配：`mango-auth-starter` 写入 Spring Security `SecurityContextHolder`
- 未进入范围：`mango-gateway`、`mango-rbac`、`mango-system`、`mango-admin-app` 的主动重构

## P4-T1 / P4-T2 盘点结果

| 模块 | 平台依赖 | 业务感知点 | 判定 |
|------|----------|------------|------|
| `mango-infra-web` | 未发现 `mango-platform` 包或 POM 依赖 | `IInternalPathProvider` 只消费路径接口；`RequestContextContributor` 暴露 header/cookie/request | 保留，补统一 HTTP 请求上下文 provider |
| `mango-infra-security` | 未发现 `mango-platform` 包或 POM 依赖 | 历史 `PermAspect` 直接读取 request attribute `userId`；README 含 RBAC 示例 | 改为 Spring Security Method Security + `ISecurityContextProvider`；README 收敛为技术契约 |

## P4-T3 契约冻结

### `mango-infra-web`

统一 HTTP 请求上下文契约：

| 字段 | 来源 |
|------|------|
| `requestId` | `X-Request-Id`，缺省使用 Servlet request id |
| `traceId` | `X-Trace-Id`，兼容 `TRACE-ID` |
| `clientIp` | `X-Forwarded-For` 首个 IP，其次 `X-Real-IP`，最后 `remoteAddr` |
| `headers` | 当前请求 headers |
| `cookies` | 当前请求 cookies |
| `request` | 当前 Servlet request，作为历史兼容变量保留 |

新增接口：

- `io.mango.infra.web.api.IRequestContextProvider`
- `io.mango.infra.web.api.RequestContextSnapshot`

### `mango-infra-security`

统一安全上下文契约：

| 字段 | 来源 |
|------|------|
| `userId` | Spring Security `Authentication.principal` 中的 `SecurityPrincipal.userId` |
| `tenantId` | Spring Security `Authentication.principal` 中的 `SecurityPrincipal.tenantId` |
| `authenticated` | `Authentication.isAuthenticated()`，并排除匿名认证 |
| `principalName` | `SecurityPrincipal.principalName`，或 `Authentication.getName()` |

新增接口：

- `io.mango.infra.security.api.ISecurityContextProvider`
- `io.mango.infra.security.api.SecurityContext`

默认实现 `SpringSecurityContextProvider` 只读取 Spring Security `SecurityContextHolder`，不解析 token、不访问平台业务存储、不承载业务模型。

## 完成项

- [x] 完成 `mango-infra-web` / `mango-infra-security` 平台依赖与业务命名盘点。
- [x] 新增 `IRequestContextProvider`、`RequestContextSnapshot`，并由 `WebAutoConfiguration` 自动装配。
- [x] `WebRequestContextContributor` 改为通过 provider 贡献 `request`、`headers`、`cookies`、`requestId`、`traceId`、`clientIp`。
- [x] 新增 `ISecurityContextProvider`、`SecurityContext`，并由 `SecurityAutoConfiguration` 自动装配默认 Spring Security provider。
- [x] `@Perm` 改为 Spring Security Method Security，不再自行读取 Servlet request。
- [x] `mango-auth-starter` 被动写入 `SecurityPrincipal` 到 Spring Security `SecurityContextHolder`，用于统一安全上下文认证状态。
- [x] 更新 `mango-infra-web/README.md`、`mango-infra-security/README.md`、相关 POM description 和文档索引。

## 不做项

- 不修改 `mango-gateway` 默认路由、可信 header 注入或清洗规则；该内容属于 Phase 5。
- 不改造 `mango-rbac` 权限存储或权限码业务规则；该内容属于 Phase 6。
- 不重构 `mango-system` 租户过滤器和审计切面；该内容属于 Phase 7。
- 不要求 realtime 在本阶段接入新 provider；本阶段只冻结契约并记录后续被动适配点。

## 被动适配

| 模块 | 适配点 | 原因 |
|------|--------|------|
| `mango-auth-starter` | 认证成功后写入 Spring Security `SecurityContextHolder` | 让 `ISecurityContextProvider` 可统一读取认证状态 |

## 下游后续适配点

- `mango-infra-realtime`：后续只消费 `IRequestContextProvider` / `ISecurityContextProvider` 或由上游传入的 resolver 结果，不自行解析 JWT 或实现权限规则。
- `mango-system` audit：后续可用 `IRequestContextProvider.clientIp()` 替代本地重复 IP 解析。
- `mango-system` tenant：后续可继续作为租户业务适配器写入 `tenantId` attribute，但不应把租户业务规则放入 infra。
- notification / audit / 业务模块：需要当前身份时优先消费 `ISecurityContextProvider`。

## 验证结果

已执行：

```bash
cd mango
mvn -q -DskipTests compile -pl mango-infra/mango-infra-web,mango-infra/mango-infra-security -am
```

结果：通过。

已执行相关测试：

```bash
cd mango
mvn -q test -pl mango-infra/mango-infra-web,mango-infra/mango-infra-security -am
```

结果：通过。测试过程中 `mango-maven-plugin` 的测试用例会打印临时目录中的预期 warning / failed check 样例，但 Maven 进程退出码为 0。

已执行 Phase 指定全量命令：

```bash
cd mango
mvn -q -DskipTests compile
```

结果：失败，失败点在 Phase 5 模块 `mango-gateway-core`，缺少 Spring / SLF4J 相关依赖导致 `DynamicWhiteListConfig`、`GatewayProperties`、`AuthFilter` 编译失败。本阶段按禁止跨 Phase 原则未修改 gateway。

已执行依赖方向搜索：

```bash
rg -n "mango-platform|io\\.mango\\.rbac|io\\.mango\\.auth|io\\.mango\\.system" mango/mango-infra/mango-infra-web mango/mango-infra/mango-infra-security
```

结果：无命中。

已执行 infra-web follow-up 受影响模块编译：

```bash
cd mango
mvn -q -DskipTests compile -pl mango-infra/mango-infra-web,mango-infra/mango-infra-realtime,mango-platform/mango-rbac,mango-tools/mango-maven-plugin -am
```

结果：通过。

已执行 infra-web 与 mango:check 插件测试：

```bash
cd mango
mvn -q test -pl mango-infra/mango-infra-web,mango-tools/mango-maven-plugin -am
```

结果：通过。插件单测会打印临时目录中的预期违规样例，Maven 进程退出码为 0。

已执行真实仓库 Web 边界规则：

```bash
cd mango
mvn -q -pl mango-tools/mango-maven-plugin install -DskipTests
mvn -q io.mango:mango-maven-plugin:1.0.0-SNAPSHOT:check -Drule=web-boundary
```

结果：通过。

已执行 `mango:check` 聚合规则：

```bash
cd mango
mvn -q io.mango:mango-maven-plugin:1.0.0-SNAPSHOT:check -Drule=all
```

结果：失败，失败点在聚合静态分析委托阶段，`mango-infra-module-starter` 解析不到本地 `mango-infra-module-core:1.0.0-SNAPSHOT`。这不是本次 infra-web/web-boundary 规则失败。

已执行格式检查：

```bash
git diff --check
```

结果：通过。

已执行业务命名搜索：

```bash
rg -n "SysUser|Role|Menu|Tenant|Org|Message|.*DTO|.*VO" mango/mango-infra/mango-infra-web mango/mango-infra/mango-infra-security
```

结果：命中均为技术类或通用方法名误报：

- `MessageDigest`：JDK 摘要算法，不是业务 Message。
- `getMessage()`：异常消息方法，不是业务 Message。
- 测试断言中的异常消息文本，不是业务 Message。

## 遗留问题

- 全量 compile 被 Phase 5 的 `mango-gateway-core` 依赖缺失阻塞；需要在 Phase 5 修复。
- `mango-infra-security` 仍保留 `TokenContextHolder` 供 Feign 传播历史 token，上下文传播彻底统一应在后续 infra/gateway/feign 相关 Phase 评估。

## Phase 4 Follow-up：infra-web 标准化

- 已将 `mango-infra-web` 拆为 `mango-infra-web-api` / `mango-infra-web-starter`。
- 已新增 `@Inner` 注解，支持标注 API 或 Controller 的类 / 方法。
- 已新增 `InnerMappingScanner`，将 `@Inner` 标注的 Spring MVC 最终路径注册为内部路径来源。
- 已将内部路径来源改为聚合模式，支持注解扫描、模块 provider、数据库 provider 等多来源。
- 已新增 `MangoWebProperties`，Spring Boot 原生 Web 配置继续使用 `server.*` / `spring.*`，Mango 只新增 `mango.web.*` 横切配置。
- 已新增 `WebMdcFilter` 和 Trace resolver，统一写入 `requestId`、`traceId`、`clientIp` 到 MDC。
- 已清理 `mango-infra-realtime-core` 对 `spring-boot-starter-web` 的直接依赖，改为更窄的 `spring-webmvc`。
- 已在 `mango:check` 增加 `web-boundary` 规则，并同步 PMO 模块/API 规范。

## 下一 Phase 前置条件

- Phase 5 需要修复 `mango-gateway-core` POM 依赖，恢复全量编译。
- Phase 5 需要明确可信 header 注入与外部伪造 header 清洗规则，避免下游直接信任公网身份/租户 header。
- Phase 6/8 只允许补齐 RBAC/Auth 对 `IPermissionProvider`、`ISecurityContextProvider` 的 adapter，不应推翻本阶段已冻结的上下文字段语义。
