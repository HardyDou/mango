# IP Location Data

## 1. 用途

该目录用于放置运行时 ip2region xdb 数据文件，供 `mango-infra-ip-location` 的 ip2region provider 读取。它是部署资产说明，不是 Java 模块。

## 2. 默认文件位置

推荐放置路径：

```text
config/ip-location/ip2region_v4.xdb
```

宿主应用配置示例：

```yaml
mango:
  ip-location:
    provider: ip2region
    fail-fast: false
    ip2region:
      xdb-location: file:./config/ip-location/ip2region_v4.xdb
```

## 3. 缺失时行为

当 xdb 文件不存在且 `mango.ip-location.fail-fast=false` 时，应用仍会启动，解析能力降级为 noop，归属地显示为 `未知`。当 `fail-fast=true` 时，xdb 缺失或初始化失败会阻止应用启动。

## 4. 更新方式

替换该目录下的 xdb 文件后重启服务。不要把生产 xdb 数据文件提交到 Git；如需在内网发布，走部署资产或配置中心的文件分发流程。

## 5. 验证

启动应用后调用依赖 `IpLocationResolver` 的登录日志或审计链路，使用公网 IP 验证 province、city、isp 和 `resolved` 字段；再临时移除 xdb 文件验证降级或 fail-fast 行为符合环境要求。
