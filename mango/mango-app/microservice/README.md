# Mango Microservice Apps

## 1. 概览
`mango-app/microservice` 提供 Mango 微服务拓扑示例，把网关、平台能力、业务能力和文件预览拆成独立 Spring Boot 进程。

它用于验证服务边界和部署形态，不是业务逻辑承载层。

## 2. 拓扑

```text
Client
  -> mango-gateway-app:8080
      -> mango-platform-app:8081
      -> mango-business-app:8082
      -> mango-file-preview-app:8083
```

| App | 默认端口 | 当前装配 |
|-----|----------|----------|
| `mango-gateway-app` | `8080` | `mango-infra-module-starter`、`mango-access-gateway-starter`、authorization resource sync。 |
| `mango-platform-app` | `8081` | auth、identity、authorization、org、captcha、system、domain、notice、workflow、job、file、file-preview、template、payment、infra web/kv/event/realtime。 |
| `mango-business-app` | `8082` | common、module、web、kv、auth/identity/authorization remote starter 和业务资源同步。 |
| `mango-file-preview-app` | `8083` | 文件预览独立部署入口。 |

## 3. 接入规则

- 服务提供方依赖本地 `*-starter`，让 Controller、Service、Mapper、migration 和初始化逻辑进入本服务。
- 服务调用方依赖 `*-starter-remote`，通过远程客户端访问提供方能力。
- 新业务能力默认放到 `mango-business-app` 或业务自己的 app，不放到 gateway。
- 平台基础能力默认放到 `mango-platform-app`。
- 网关只做路由、边界鉴权和网关资源同步。

## 4. 启动方式

```bash
mvn -f mango/pom.xml -pl :mango-gateway-app -am spring-boot:run
mvn -f mango/pom.xml -pl :mango-platform-app -am spring-boot:run
mvn -f mango/pom.xml -pl :mango-business-app -am spring-boot:run
mvn -f mango/pom.xml -pl :mango-file-preview-app -am spring-boot:run
```

编译验证：

```bash
mvn -f mango/pom.xml -pl :mango-gateway-app,:mango-platform-app,:mango-business-app,:mango-file-preview-app -am test
```

## 5. 配置说明
| App | 配置 | 说明 |
|-----|------|------|
| gateway | `spring.cloud.gateway.routes` | 默认把平台路径转发到 `localhost:8081`，业务路径转发到 `localhost:8082`。 |
| gateway | `mango.authorization.access.auth-enabled` | 网关边界鉴权开关。 |
| gateway | `mango.authorization.resource-sync.gateway.*` | 网关资源同步模块名和模式。 |
| platform | `spring.datasource.*` | 默认 H2 内存库。 |
| platform | `mango.flyway.enabled` | 当前示例默认 false。 |
| platform | `mango.crypto.sm4.secret-key` | SM4 密钥环境变量。 |
| business | `spring.datasource.*` | 默认 H2 内存库。 |
| business | `mango.flyway.enabled` | 当前示例默认 false。 |
| file-preview | `mango.file-preview.*` | 预览入口、源文件路径和 token 过期时间。 |

生产或真实联调时需要把 H2 改为 MySQL，按服务边界打开 migration，并配置 Nacos、注册发现、服务地址、JWT 密钥和对象存储。

## 6. 数据与初始化
微服务示例默认关闭 Flyway，因此启动后不会自动初始化完整平台表和菜单。真实拆分时：

- 平台 app 打开平台模块 migration。
- 业务 app 打开业务模块 migration。
- 业务资源同步在业务 app 执行，菜单和权限写入 authorization。
- 网关同步网关侧公开路径和受控路径资源。

## 7. 管理入口
网关负责边界鉴权，authorization 负责菜单、权限和角色授权。业务服务应同步自己的 API 权限资源，并让前端菜单 component 指向对应页面 key。

多租户数据隔离仍由业务服务和平台服务的持久化、租户上下文和权限校验完成，不能只依赖网关。

## 8. 问题排查
- 通过网关访问 404：检查 gateway route 的 Path 是否包含目标接口前缀。
- 业务服务权限失败：检查 remote starter、JWT 密钥、authorization 服务地址和资源同步。
- 启动后没有表：微服务示例默认关闭 Flyway，需要按真实服务打开。
- 平台服务越来越大：把业务 starter 移到 business app，platform app 只保留平台能力。

## 9. 相关文档
- [Mango App README](../README.md)
- [单体部署入口](../monolith/mango-monolith-app/README.md)
- [Access README](../../mango-platform/mango-access/README.md)
- [Authorization README](../../mango-platform/mango-authorization/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
