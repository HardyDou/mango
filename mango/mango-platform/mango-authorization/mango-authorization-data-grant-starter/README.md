# Mango Authorization Data Grant Starter

无状态数据权限 Grant：列表接口把「本页可见记录 ID 集合」签成短期令牌放进响应头，详情接口要求回传该令牌，
并在进入 Controller 之前判定记录是否在集合内。服务端不落库、不缓存，多实例只需要同一份 `secret`。

## 1. 契约

| 项 | 值 |
|----|----|
| 响应头（签发成功） | `X-Mango-Data-Grant: m1.hs256.<kid>.<payload>.<signature>` |
| 响应头（未携带 Grant，optional 模式） | `X-Mango-Data-Grant: absent` |
| 响应头（强制模式缺少 Grant） | `X-Mango-Data-Grant-Required: 1`，HTTP 403 |
| 响应头（记录数超过 `max-ids`，未签发） | `X-Mango-Data-Grant-Skipped: oversize` |
| 请求头（详情接口回传） | `X-Mango-Data-Grant: <token>`，可多个，同一 `domain` 任一通过即可 |
| 记录不在集合内 | HTTP 404，`{"code":404,"message":"资源不存在"}`，与真实不存在完全一致 |

令牌 5 段点分，签名覆盖前 4 段；payload 为无填充 base64url JSON：`t` 租户、`m` 账号、`d` 数据域、
`i` 记录 ID 增量编码、`ia` 签发时间、`ex` 过期时间、`pv` 数据范围指纹（可选）。

三个响应头已由 `DataGrantAutoConfiguration` 幂等追加到 `mango.web.cors.exposed-headers`，
跨域前端可以直接读取；如需自定义暴露头，在 `mango.web.cors.exposed-headers` 里显式配置即可，追加不会重复。

## 2. 配置

```yaml
mango:
  data-grant:
    enabled: true          # 总开关
    mode: optional         # optional | enforce
    secret: ${MANGO_DATA_GRANT_SECRET}   # 必填；enforce 模式下缺失则拒绝启动
    key-id: k1             # 支持轮换，多版本并存时由部署侧切换
    ttl-seconds: 300
    max-ids: 100           # 超过不签发，响应头标记 oversize
```

`secret` 必须由配置中心或环境变量注入，禁止写入版本控制。`mode` 非法或 `ttl-seconds`/`max-ids` 非正时启动失败。

## 3. 接入方式

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
- `mango-authorization-data-grant-starter` 的测试会反射扫描 classpath 上所有 Spring MVC handler：
  带 `@DataGrant` 的详情端点（`@GetMapping` 且路径含 `{id}` 或末尾 `/detail`）必须存在同 `domain` 的列表端点，
  否则测试失败并打印两边的 `Class#method`。新增试点接入时请同步补齐列表端点。

## 4. `pv` 指纹的边界

`pv = sha256(mode|sorted(values)|selfIncluded)` 前 8 位，来自 `DataScopeProvider`。它只挡
**「角色数据范围配置变更」**：管理员改了角色的数据范围后，旧 Grant 立即失效（403 + `-Required: 1`）。

它**挡不住**：

- **用户调岗**：账号主体没变，`m` 与指纹都可能不变；
- **记录改归属**：记录本身从 A 组织挪到 B 组织，Grant 集合是签发时的快照。

这两类只能靠 TTL（默认 300 秒）收敛。业务应用没有 `DataScopeProvider` 实现时（例如 `mango-business-app`），
`pv` 不写入，校验侧自然跳过指纹比对。

## 5. 已知限制

- 列表端点直接返回 `List`（非 `PageResult`）时不签发 Grant，需要先改为分页响应；
- 一次响应只签发一个 `domain` 的 Grant；同一响应包含多个数据域的记录时，需拆分接口；
- 前端 Grant 回传属 PR2，本模块只提供服务端契约。
