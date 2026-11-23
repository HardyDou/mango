# Mango Authorization Data Grant Starter

无状态数据权限 Grant：列表接口把「本页可见记录 ID 集合」签成短期令牌放进响应头，详情接口要求回传该令牌，
并在进入 Controller 之前判定记录是否在集合内。服务端不落库、不缓存，多实例只需要同一份 `secret`。

## 1. 概览

列表查询的数据权限在 MyBatis 层收窄结果，但详情接口按 ID 直查时无法复用那次收窄结果，业务侧只能重复写
`scopedById` 之类的私有方法。本模块把这层能力上收为平台契约：列表签发、详情校验，业务侧每个详情端点只加
一行 `@DataGrant` 注解。

三种典型接入形态：

- 平台自用试点：`mango-workflow-starter` 的业务申请列表与详情；
- 业务应用：`mango-business-app` 引入本模块后给自己详情端点加注解；
- 内部调用：带 `@Inner` 的服务间请求跳过校验，由内部调用签名保证来源。

## 2. 功能清单

- 列表签发：响应 `data` 为 `PageResult` 且记录带 ID 时签发 `X-Mango-Data-Grant`，令牌 5 段点分、HMAC-SHA256 签名。
- 详情校验：进入 Controller 之前比对记录 ID 是否在令牌集合内，不在集合返回 404，文案与「资源不存在」完全一致。
- 无回表判决：ID 判定发生在拦截器阶段，不查库、不调 Service，避免响应耗时与文案泄露记录是否存在。
- 可观测性：未携带 Grant 记 `X-Mango-Data-Grant: absent`，记录数超限记 `X-Mango-Data-Grant-Skipped: oversize`，强制模式缺 Grant 记 `X-Mango-Data-Grant-Required: 1`。
- 域标记：签发与拒绝响应都写 `X-Mango-Data-Grant-Domain`，值即 `@DataGrant(domain = ...)`，前端只用它当缓存键，不做 URL 前缀映射。
- 多值请求头：请求头按逗号切分多值，逐个 trim、丢弃空值与含空白/控制字符的值、去重；去重后超过 16 个或序列化总长超过 4096 字符按未携带处理。
- 幂等 CORS 暴露：四个响应头自动追加进 `mango.web.cors.exposed-headers`、`X-Mango-Data-Grant` 追加进 `mango.web.cors.allowed-headers`（配置为 `*` 时不追加），两种情况都不覆盖部署侧已有配置。
- 测试期护栏：反射扫描全部 Spring MVC handler，详情端点缺同域列表端点时测试失败并打印两边的 `Class#method`。

## 3. 配置说明

**当前状态：半成品，默认关闭，禁止在生产启用。** 未完成项：刷新/直链进入详情页时数据权限无法恢复（issue #1420）。
默认 `enabled=false`，即模块随包发布但不生效；下面的配置示例是启用后的形态。

```yaml
mango:
  data-grant:
    enabled: true          # 总开关，默认 false；#1420 完成前禁止开启
    mode: optional         # optional | enforce
    secret: ${MANGO_DATA_GRANT_SECRET}   # 必填；enforce 模式下缺失则拒绝启动
    key-id: k1             # 支持轮换，多版本并存时由部署侧切换
    ttl-seconds: 300
    max-ids: 100           # 超过不签发，响应头标记 oversize
```

`secret` 必须由配置中心或环境变量注入，禁止写入版本控制。`mode` 非法、`ttl-seconds` 或 `max-ids` 非正时启动失败；
`optional` 模式下未配置 `secret` 时既不签发也不校验，只记 warn，因此灰度期可以先开模块再补密钥。

## 4. API 与扩展

契约一览：

| 项 | 值 |
|----|----|
| 响应头（签发成功） | `X-Mango-Data-Grant: m1.hs256.REPLACE_KID.REPLACE_PAYLOAD.REPLACE_SIGNATURE` |
| 响应头（未携带 Grant，optional 模式） | `X-Mango-Data-Grant: absent` |
| 响应头（强制模式缺少 Grant） | `X-Mango-Data-Grant-Required: 1`，HTTP 403 |
| 响应头（记录数超过 `max-ids`，未签发） | `X-Mango-Data-Grant-Skipped: oversize` |
| 响应头（数据域标记，始终伴随上面三种） | `X-Mango-Data-Grant-Domain: <@DataGrant 的 domain>` |
| 请求头（详情接口回传） | `X-Mango-Data-Grant`，可多个（逗号分隔或重复头），任一与 `domain` 匹配即通过；全部不匹配等同未携带 |
| 记录不在集合内 | HTTP 404，`{"code":404,"message":"资源不存在"}`，与真实不存在完全一致 |

令牌 5 段点分，签名覆盖前 4 段；payload 为无填充 base64url JSON：`t` 租户、`m` 账号、`d` 数据域、
`i` 记录 ID 增量编码、`ia` 签发时间、`ex` 过期时间、`pv` 数据范围指纹（可选）。

## 5. 接入方式

```java
@PostMapping("/page")
@DataGrant(domain = "workflow:business-apply")
public R<PageResult<FooVO>> page(@RequestBody FooPageRequest request) { ... }

@GetMapping("/detail")
@DataGrant(domain = "workflow:business-apply")
public R<FooVO> detail(@RequestParam("id") Long id) { ... }
```

- 列表端点：成功响应且 `data` 为 `PageResult` 时签发；一期不支持直接返回 `List` 的端点（不会签发 Grant）。
- 详情端点：记录 ID 按「注解 `idField` 同名参数 → 唯一数值型 `@RequestParam` → 路径变量 `{id}`」解析；
  解析不到 ID 视为列表端点，直接放行。
- 带 `@Inner` 的内部调用（`InternalCallAttributes.VERIFIED` 已置位）跳过校验。
- 多值回传：一个请求可以带多个数据域的 Grant（`X-Mango-Data-Grant: g1, g2`），不属于本 `domain` 的令牌被跳过而不是判失败；只有全部不匹配才按未携带处理。上限 16 个、序列化总长 ≤4096 字符，超限整次按未携带处理。
- 同域上限：同一个 `domain` 内最多接受 4 个令牌，超过即判 `INVALID`；多值不是「取第一个」的捷径，也不能用来绕过过期、范围指纹等排序。无法解出数据域的坏令牌同样判 `INVALID`，不会因为另一个令牌通过而被忽略。
- 扩展点：`DataScopeProvider`（`mango-infra-persistence-api`）用于计算 `pv` 指纹，可不装配；
  `HmacSignatures`（`mango-infra-web-support`）是本模块与内部调用签名共用的 HMAC 工具。

## 6. 数据与初始化

本模块不建表、不写 migration、不注册菜单与默认数据，也不占用 `META-INF/mango/resources` 声明目录。

启动初始化只有一步：`DataGrantAutoConfiguration` 在容器启动时校验 `mango.data-grant.*` 配置合法性，
并把四个响应头追加进 `mango.web.cors.exposed-headers`、把 `X-Mango-Data-Grant` 追加进 `mango.web.cors.allowed-headers`（幂等，重复启动不会重复追加；后者配置为 `*` 时跳过）。

两侧都要有：`exposed-headers` 只决定浏览器**能不能读**响应头，`allowed-headers` 才决定**允不允许带**这个请求头。只补前者时，跨源部署下详情请求会在浏览器层被拦掉，表现为「列表正常、详情永远 401/403」，而同源单进程测不出来。
`enforce` 模式缺少 `mango.data-grant.secret` 时启动直接失败，避免上线后才发现所有详情请求 403。

## 7. 管理入口

本模块不提供后台管理页面，所有行为由配置与注解决定。

排查与观测入口：

- 响应头 `X-Mango-Data-Grant` / `-Domain` / `-Required` / `-Skipped` 直接体现当前请求的 Grant 状态；
- 服务端日志 `数据权限 Grant 校验未通过` 与 `缺少数据权限 Grant`（后者为 warn）记录 `domain`、记录 ID 与路径；
- `mango.data-grant.mode` 是唯一的全局开关，灰度期用 `optional`，全量验证后再切 `enforce`。

## 8. 快速开始

1. 在应用 `pom.xml` 引入 `mango-authorization-data-grant-starter`（`mango-platform-app` 与 `mango-business-app` 已内置）。
2. 配置 `mango.data-grant.secret`（建议环境变量 `MANGO_DATA_GRANT_SECRET`），保持 `mode: optional`。
3. 给列表端点和对应详情端点加同一个 `domain` 的 `@DataGrant`，跑一次列表接口，确认响应头出现 `X-Mango-Data-Grant`。
4. 观察一段时间日志无 warn 后，把 `mode` 切到 `enforce`。

## 9. 问题排查

| 现象 | 原因 | 处理 |
|------|------|------|
| 列表响应没有 `X-Mango-Data-Grant` | 未配置 `secret`、响应不是 `PageResult`、记录缺 ID，或记录数超过 `max-ids` | 先看是否有 `-Skipped: oversize`，再看服务端 warn |
| 详情一直 404，但记录确实存在 | 记录的 ID 不在签发时的集合内（分页外或数据权限刚变更） | 刷新列表重新签发；确认列表与详情使用同一 `domain` |
| 详情 403 且带 `-Required: 1` | `enforce` 模式缺少 Grant 或 Grant 过期 | 前端公共层（`@mango/http-client`、`@mango/common`）已自动回传；仍报错时确认浏览器实际发的 `X-Mango-Data-Grant` |
| 跨域前端读不到响应头 | 部署侧自定义了 `mango.web.cors.exposed-headers` 且未包含本模块的四个头 | 在自定义配置里显式列出，模块只做追加、不覆盖 |
| 跨域详情请求 401/403、列表正常 | 部署侧自定义了 `mango.web.cors.allowed-headers` 且未包含 `X-Mango-Data-Grant`，浏览器拦掉了回传的请求头 | 把该头加进 `allowed-headers`（模块已自动追加，除非部署侧覆盖或配成 `*`）；网关/WAF 也需允许 `X-` 自定义请求头 |
| 启动报 `secret` 缺失 | `mode: enforce` 但未配置密钥 | 先配密钥再切模式 |

## 10. `pv` 指纹的边界

`pv = sha256(mode|sorted(values)|selfIncluded)` 前 8 位，来自 `DataScopeProvider`。它只挡
**「角色数据范围配置变更」**：管理员改了角色的数据范围后，旧 Grant 立即失效（403 + `-Required: 1`）。

它**挡不住**：

- **用户调岗**：账号主体没变，`m` 与指纹都可能不变；
- **记录改归属**：记录本身从 A 组织挪到 B 组织，Grant 集合是签发时的快照。

这两类只能靠 TTL（默认 300 秒）收敛。业务应用没有 `DataScopeProvider` 实现时（例如 `mango-business-app`），
`pv` 不写入，校验侧自然跳过指纹比对。

## 10.1 启动期保护姿态日志

启动时会按当前配置打一行确定日志，便于运维区分三种姿态：

| 姿态 | 日志 | 运行期表现 |
|------|------|-----------|
| `mango.data-grant.enabled=false`（默认） | WARN「数据权限 Grant 已关闭（默认状态）…」 | 不签发、不校验，响应里没有 Grant 头 |
| 启用但缺 `secret` | WARN「…未配置 mango.data-grant.secret…」 | 列表不签发，详情按未携带处理（`optional` 放行 / `enforce` 403） |
| 启用且配置密钥 | INFO「数据权限 Grant 已启用, mode=…」 | 正常签发与校验 |

该日志由 `DataGrantPostureConfiguration` 输出，且不受 `enabled` 开关约束——关闭这一最需要被看见的姿态不能没有日志。

## 11. 已知限制

- 详情端点必须能从请求中解析记录主键（同名请求参数或同名路径变量）。解析不到且不是分页端点时按配置错误 fail-closed（服务端错误），不会静默放行；用非 `id` 命名的主键请在 `@DataGrant(idField = "...")` 显式声明。

- 数据范围实现存在但取不到规则时按未签发处理，响应带 `X-Mango-Data-Grant-Skipped: scope-unresolved`；只有「当前部署没有数据范围实现」才留空指纹照常签发。

- 列表端点直接返回 `List`（非 `PageResult`）时不签发 Grant，需要先改为分页响应；
- 一次响应只签发一个 `domain` 的 Grant；同一响应包含多个数据域的记录时，需拆分接口；
- 记录 ID 解析在参数名冲突时会退回「唯一数值型参数」启发式，存在多个数值型参数时请显式声明 `idField`。

前端接力（PR2 已落地）的已知限制：

- 网关/WAF 过滤 `X-` 前缀头时，前端拿不到 `X-Mango-Data-Grant-Domain`，接力失效但不会误放行；每个环境都要确认白名单。
- 浏览器 preflight 与 `Access-Control-Expose-Headers` 有缓存窗口，后端已改而部分用户仍 401/403 属预期现象，需用户刷新或等缓存过期。
- 过渡期内无法识别的旧 Grant 在前端一律删除（不兼容重放），用户需重新打开列表。
- 平台侧不得记录 Grant 完整值：日志只打长度或前 6 位，本模块的失败日志只打 `domain`、记录 ID 与路径。

## 12. 相关文档

- [Mango Authorization](../README.md)
- [数据权限扩展点](../../../mango-infra/mango-infra-persistence/mango-infra-persistence-api/src/main/java/io/mango/infra/persistence/api/scope/DataScopeProvider.java)
- [能力说明维护规范](../../../../mango-pmo/rules/08-capability-docs.md)
- [业务接入指南](../../../../mango-docs/guides/business-integration/README.md)
