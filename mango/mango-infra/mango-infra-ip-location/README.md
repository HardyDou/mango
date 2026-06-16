# Mango Infra IP Location

## 1. 概览
`mango-infra-ip-location` 提供本地 IP 归属地解析能力。默认 provider 是 ip2region xdb，解析结果包含国家、区域、省份、城市、运营商、来源、是否内网地址和是否解析成功。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 登录日志、审计日志、操作日志需要展示 IP 归属地 | Maven 依赖 / starter / Java API |
| 安全风控需要把客户端 IP 解析为省市或运营商后作为辅助字段 | Maven 依赖 / starter / Java API |
| 本地离线解析即可满足需求，不希望每次调用第三方实时地理服务 | Maven 依赖 / starter / Java API |


## 3. 能力边界
- 不负责 IP 黑白名单、风控规则、地理围栏和实时威胁情报。
- 不保证 xdb 数据实时更新；数据准确性取决于部署的 ip2region 数据文件。
- 不直接写登录日志或审计表，只提供解析服务。

## 4. 模块入口
- `mango-infra-ip-location-api`：`IpLocationResolver`、`IpLocation`。
- `mango-infra-ip-location-core`：noop resolver、ip2region xdb resolver、缓存包装、显示文本工具。
- `mango-infra-ip-location-starter`：`IpLocationAutoConfiguration` 和 `IpLocationProperties`。

## 5. 接入方式
```xml
<dependency>
    <groupId>io.mango.infra.iplocation</groupId>
    <artifactId>mango-infra-ip-location-starter</artifactId>
</dependency>
```

业务调用：

```java
IpLocation location = ipLocationResolver.resolve(clientIp);
String displayText = location.displayText();
```

如果只需要契约，依赖 `mango-infra-ip-location-api`。

## 6. 配置说明
配置前缀：`mango.ip-location`。

| 配置 | 默认值 | 含义 |
|------|--------|------|
| `enabled` | `true` | 是否启用解析。关闭后返回 noop 结果。 |
| `provider` | `ip2region` | 解析 provider，支持 `ip2region` 和 `noop`。 |
| `fail-fast` | `false` | xdb 文件缺失或初始化失败时是否阻止应用启动。 |
| `cache.enabled` | `true` | 是否启用解析结果本地缓存。 |
| `cache.maximum-size` | `10000` | 缓存最大条目数。 |
| `cache.ttl` | `24h` | 缓存 TTL，Spring Boot Duration 格式。 |
| `ip2region.xdb-location` | 无 | ip2region xdb 文件位置，必须配置后才会真实解析。 |
| `ip2region.vector-index-enabled` | `true` | 是否使用 vector index 方式加载 xdb。 |
| `ip2region.content-cache-enabled` | `false` | 是否把 xdb 全量加载到内存。开启后优先于 vector index。 |

示例：

```yaml
mango:
  ip-location:
    enabled: true
    provider: ip2region
    fail-fast: false
    ip2region:
      xdb-location: file:./config/ip-location/ip2region_v4.xdb
      vector-index-enabled: true
      content-cache-enabled: false
    cache:
      enabled: true
      maximum-size: 10000
      ttl: 24h
```

## 7. API 与扩展
- `IpLocationResolver.resolve(String ip)`：解析 IP。
- `IpLocation`：解析结果，`displayText()` 会拼接国家、省份、城市、运营商，空结果显示 `未知`。
- `NoopIpLocationResolver`：未启用、provider 非 ip2region、xdb 不可用且未 fail-fast 时返回 unresolved 结果。
- `Ip2RegionXdbLocationResolver`：基于 ip2region xdb 解析。
- `CachingIpLocationResolver`：按配置缓存解析结果。

替换解析来源时，业务应用可声明自己的 `IpLocationResolver` Bean，自动配置会让位。

## 8. 数据与初始化
无数据库 migration、无 Runner、无 Initializer。解析数据来自外部 xdb 文件，不应提交生产数据文件到 Git。仓库内 `mango/config/ip-location/README.md` 只说明默认文件位置。

## 9. 管理入口
本模块不创建菜单和权限，不感知租户。解析出的归属地通常作为日志字段保存；日志归属和查询权限由登录日志、审计日志或业务模块控制。

## 10. 快速开始
1. 部署 ip2region xdb 到宿主应用可读路径。
2. 配置 `ip2region.xdb-location`。
3. 登录或审计链路注入 `IpLocationResolver`，保存原始 IP、display text、province、city、isp 和 resolved 状态。
4. 验证公网、内网、非法 IP 和 xdb 缺失场景。

## 11. 问题排查
- 一直返回 `未知`：检查 xdb 文件是否存在、路径是否带 `file:` 或 `classpath:`、应用进程是否可读。
- 启动没有失败但解析为空：默认 `fail-fast=false` 会降级到 noop。
- 数据不准确：替换 xdb 文件并重启应用；本模块不会自动下载数据。

## 12. 相关文档
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 13. 补充资料
- [能力地图](../../../mango-docs/capabilities/README.md)
