# IP 地理位置库模块规划

更新时间：2026-05-09

## 目标

为 Mango 提供统一的 IP 归属地解析基础能力，优先服务登录日志、操作日志、安全审计和后续风控规则。

该能力必须是基础设施模块，不归属于系统日志模块。日志模块只消费解析接口，不直接绑定具体 IP 数据库或第三方 SDK。

## 当前实现状态

- 已完成基础设施模块 `mango-infra-ip-location`，并纳入 `mango-infra` 聚合。
- 已完成单体集成，`mango-monolith-app` 引入 `mango-infra-ip-location-starter`。
- 已完成登录日志和操作日志接入，两个日志写入点都只依赖 `IpLocationResolver` 抽象。
- 已完成 `system.V13__add_operation_log_location.sql`，操作日志表已补齐 `location` 字段。
- 已验证本地 `config/ip-location/ip2region_v4.xdb` 存在时可解析公网 IP；文件缺失时按 `fail-fast=false` 兜底为 `未知`，不影响服务启动和日志写入。

## 结论

第一阶段默认采用 `ip2region xdb` 离线库。

原因：

- 本地离线查询，不依赖外部网络，适合登录、审计日志这种主流程旁路能力。
- Apache-2.0，集成和分发边界相对清晰。
- 支持 IPv4/IPv6，Java 可直接接入。
- xdb 数据文件可以外部化部署，后续替换文件即可更新数据。

MaxMind GeoLite2 暂不作为默认实现，只作为后续扩展 Provider 预留。

原因：

- GeoLite2 下载和使用需要接受 EULA，通常需要账号和 license key。
- 如果产品向客户分发内置数据，需要额外评估再分发许可。
- 数据更新存在合规要求，不能简单随代码打包后长期不更新。

参考：

- ip2region GitHub: https://github.com/lionsoul2014/ip2region
- MaxMind GeoLite2 EULA: https://www.maxmind.com/en/geolite/eula
- MaxMind 数据更新说明: https://support.maxmind.com/hc/en-us/articles/4408927681307-Maintain-Up-to-Date-Data

## 模块边界

新增基础设施模块：

```text
mango-infra/
  mango-infra-ip-location/
    mango-infra-ip-location-api/
    mango-infra-ip-location-core/
    mango-infra-ip-location-starter/
```

父级 `mango-infra/pom.xml` 增加：

```xml
<module>mango-infra-ip-location</module>
```

`mango-infra-ip-location/pom.xml` 聚合三个子模块。

## 分层职责

### mango-infra-ip-location-api

只放稳定抽象，不依赖 Spring，不依赖具体数据源。

建议包名：

```text
io.mango.infra.iplocation.api
```

核心接口：

```java
public interface IpLocationResolver {
    IpLocation resolve(String ip);
}
```

核心模型：

```java
public class IpLocation {
    private String ip;
    private String country;
    private String region;
    private String province;
    private String city;
    private String isp;
    private String source;
    private boolean privateAddress;
    private boolean resolved;
}
```

必要约定：

- 空 IP、非法 IP、内网 IP 返回 `IpLocation.empty(ip)`，不抛业务异常。
- `displayText()` 作为展示聚合字段：如 `中国 广东省 深圳市 电信`。
- `resolved=false` 表示无法识别，调用方可展示 `未知` 或空。

### mango-infra-ip-location-core

放具体解析实现和通用工具。

建议包名：

```text
io.mango.infra.iplocation.core
```

核心类：

```text
core/
  IpAddressClassifier        // 判断空、非法、内网、回环、本机地址
  CompositeIpLocationResolver
  NoopIpLocationResolver
  cache/CachingIpLocationResolver
  ip2region/Ip2RegionXdbLocationResolver
  support/IpLocationFormatter
```

第一阶段实现：

- `NoopIpLocationResolver`：无数据文件或关闭功能时使用。
- `Ip2RegionXdbLocationResolver`：基于 xdb 文件解析。
- `CachingIpLocationResolver`：按 IP 做轻量缓存，避免日志高频重复解析。

缓存建议：

- 默认开启本地 Caffeine 缓存。
- key 为标准化 IP 字符串。
- 默认最大 10000 条。
- 默认 TTL 24 小时。
- 不使用当前 memory KV，避免把高频解析缓存混入业务 KV 语义。

### mango-infra-ip-location-starter

提供 Spring Boot 自动配置。

建议包名：

```text
io.mango.infra.iplocation.starter
```

核心类：

```text
IpLocationAutoConfiguration
IpLocationProperties
```

自动配置文件：

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
META-INF/mango/module.properties
```

配置前缀：

```yaml
mango:
  ip-location:
    enabled: true
    provider: ip2region
    fail-fast: false
    private-address-as-empty: true
    cache:
      enabled: true
      maximum-size: 10000
      ttl: 24h
    ip2region:
      xdb-location: classpath:ip-location/ip2region.xdb
      vector-index-enabled: true
      content-cache-enabled: false
```

配置语义：

- `enabled=false`：注册 `NoopIpLocationResolver`。
- `provider=ip2region`：启用 xdb 解析。
- `fail-fast=false`：xdb 缺失时服务仍启动，解析返回空。
- `fail-fast=true`：生产环境可开启，数据文件缺失则启动失败。
- `xdb-location` 支持 `classpath:` 和外部文件路径。
- `content-cache-enabled=true` 时整库载入内存，只在内存允许时开启。

## 数据文件管理

不建议把 xdb 文件强绑定到业务代码。

推荐策略：

- 开发环境：可以提供一个小型测试 xdb 或通过配置指向本地文件。
- 单体部署：通过 `config/ip-location/ip2region.xdb` 外部挂载。
- 容器部署：通过镜像层或挂载卷提供数据文件。
- 数据更新：替换 xdb 文件后重启服务；热更新作为后续增强。

代码仓库策略：

- 不提交大型正式 IP 库文件。
- 可以提交 `README.md` 和测试夹具。
- 如果必须提交小型测试文件，只放在测试资源目录，不作为生产数据。

## 依赖方向

正确依赖方向：

```text
mango-system-core
  -> mango-infra-ip-location-api

mango-auth-starter
  -> mango-infra-ip-location-api

mango-monolith-app
  -> mango-infra-ip-location-starter
```

禁止方向：

```text
mango-infra-ip-location -> mango-system
mango-infra-ip-location -> mango-auth
日志切面 -> ip2region 具体 SDK
AuthController -> ip2region 具体 SDK
```

## 日志接入点

### 登录日志

当前位置：

```text
mango-platform/mango-auth/mango-auth-starter
```

登录日志写入时已有 `ip` 和 `location` 字段。

改造方式：

- `AuthController` 注入 `ObjectProvider<IpLocationResolver>`。
- 记录登录日志时用 `resolver.resolve(clientIp).displayText()` 填充 `location`。
- 解析失败不影响登录，不影响日志写入。

### 操作日志

当前位置：

```text
mango-platform/mango-system/mango-system-core
```

操作日志表已有 `ip`，当前需要确认是否已有 `location` 字段：

- 如果已有，直接填充。
- 如果没有，新增 Flyway 迁移增加 `location varchar(255)`。

改造方式：

- `OperationLogAspect` 只依赖 `IpLocationResolver` 抽象。
- 操作日志写入前补 `location`。
- 异常操作日志也应记录归属地。

## 数据库字段建议

第一阶段只做审计展示，不拆统计维度：

```sql
location varchar(255) comment 'IP归属地'
```

后续如果要做风控统计、区域报表、异常登录策略，再考虑拆分：

```text
country
province
city
isp
```

不建议第一阶段拆太细，因为不同数据源字段粒度不一致，会提前绑定实现细节。

## 前端展示

登录日志：

- 列表显示：`IP地址`、`登录地点`。
- 详情显示：`IP地址`、`登录地点`、`浏览器`、`操作系统`。

操作日志：

- 列表显示：`IP地址`、`操作地点`。
- 详情显示：`IP地址`、`操作地点`、`请求路径`、`HTTP方法`、`处理器方法`。

展示规则：

- `location` 为空时显示 `未知`。
- 内网、回环、本机地址显示 `内网地址`，不要误导成公网地区。

## 管理能力边界

第一阶段不做后台管理页面。

原因：

- IP 库是部署级基础设施，不是业务元数据。
- 普通管理员不应该在页面上传或替换 IP 库。
- 数据文件更新涉及运维、合规和回滚。

后续可以增加只读诊断接口，仅平台管理员可访问：

```text
GET /system/ip-location/status
GET /system/ip-location/resolve?ip=8.8.8.8
```

但这不是第一阶段必须项。

## 验收标准

### 后端单测

- 空 IP 返回未解析。
- 非法 IP 返回未解析。
- `127.0.0.1`、`localhost`、`10.x`、`172.16-31.x`、`192.168.x` 返回内网地址。
- xdb 文件不存在且 `fail-fast=false` 时服务可启动。
- xdb 文件不存在且 `fail-fast=true` 时启动失败。
- 正常公网 IP 返回可展示归属地。

### 后端直连

- 登录成功后，登录日志写入 `ip` 和 `location`。
- 登录失败后，登录日志也写入 `ip` 和 `location`。
- 触发一个 `@Log` 操作后，操作日志写入 `ip` 和 `location`。
- xdb 文件缺失时登录和业务操作不失败，只是归属地为空。

### 前端 E2E

- 登录日志列表展示登录地点。
- 登录日志详情展示登录地点。
- 操作日志列表展示操作地点。
- 操作日志详情展示操作地点。
- 页面无 401/403/未授权提示。

## 实施顺序

1. 新增 `mango-infra-ip-location` 三层模块和 POM 聚合。
2. 实现 API 模型、空实现、IP 分类器。
3. 接入 `ip2region xdb` Provider 和配置加载。
4. 单体服务引入 starter，并配置默认 `xdb-location`。
5. 登录日志接入 `IpLocationResolver`。
6. 操作日志接入 `IpLocationResolver`，必要时新增 `location` 字段迁移。
7. 前端日志页面补展示字段。
8. 执行单测、后端直连、前端 E2E。

## 不做事项

- 不在日志模块内直接实现 IP 库。
- 不把 MaxMind GeoLite2 作为默认内置库。
- 不把正式 xdb 大文件提交到代码仓库。
- 不做外部 HTTP IP 查询兜底。
- 不在第一阶段做后台上传 IP 库页面。
- 不让 IP 解析失败影响登录、鉴权、业务接口主流程。
