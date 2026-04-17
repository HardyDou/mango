# Mango Admin App

> 管理后台应用装配层，聚合 RBAC、认证、国际化、组织、区域、AI 与验证码能力。

## 模块职责

| 职责 | 说明 |
|------|------|
| 部署装配 | 作为管理后台 Spring Boot 启动单元，装配本地 starter |
| 能力聚合 | 聚合平台能力并向前端暴露管理后台 API |
| 认证委托 | 通过 `mango-gateway-starter` 与 `mango-auth-starter` 完成认证过滤和登录能力 |
| 本地适配 | 单体模式下为 gateway 需要的公共路径 API 提供本地实现 |

## 架构定位

```text
frontend
  |
  v
mango-admin-app
  |-- mango-gateway-starter
  |-- mango-auth-starter
  |-- mango-rbac-starter
  |-- mango-i18n-starter
  |-- mango-org-starter
  |-- mango-area-starter
  |-- mango-ai-starter
  |-- mango-captcha-starter
  |-- mango-infra-kv-starter
  `-- mango-infra-module-starter
```

`mango-admin-app` 只负责应用装配和必要的本地 adapter，不承载长期领域逻辑。

## 当前依赖清单

| 类型 | artifactId | 判断 |
|------|------------|------|
| Mango | `mango-common` | 合法，公共契约 |
| Mango | `mango-infra-module-starter` | 合法，模块元数据与部署映射 |
| Mango | `mango-org-starter` | 合法，平台组织能力装配 |
| Mango | `mango-area-starter` | 合法，平台区域能力装配 |
| Mango | `mango-ai-starter` | 合法，平台 AI 能力装配 |
| Mango | `mango-i18n-starter` | 合法，平台国际化能力装配 |
| Mango | `mango-rbac-starter` | 合法，当前 RBAC 本地装配 |
| Mango | `mango-captcha-starter` | 合法，验证码能力装配 |
| Mango | `mango-gateway-starter` | 合法，网关认证过滤装配 |
| Mango | `mango-infra-kv-starter` | 合法，KV 能力装配 |
| Mango | `mango-auth-starter` | 合法，认证能力装配 |
| Spring/第三方 | `spring-boot-starter-web`、`spring-boot-starter-jdbc`、`spring-boot-starter-actuator`、`spring-boot-starter-websocket`、`h2`、`mybatis-spring`、`springdoc-openapi-starter-webmvc-ui`、`lombok` | 合法；`spring-boot-starter-websocket` 在 messaging 收敛后复核是否改由 infra messaging 间接提供 |

## 单体模式

```text
MangoAdminAppApplication
  imports:
    - MangoOrgAutoConfiguration
    - MangoAreaAutoConfiguration
    - MangoAiAutoConfiguration
    - I18nAutoConfiguration
    - RbacAutoConfiguration
    - CaptchaAutoConfiguration
  component scan:
    - io.mango
  mapper scan:
    - io.mango.rbac.core.mapper
    - io.mango.i18n.core.mapper
    - io.mango.org.core.mapper
    - io.mango.area.core.mapper
```

`AuthSecurityConfig` 通过 `io.mango` 的 component scan 加载；auth controller 与初始化逻辑在 admin 单体模式下被排除，避免依赖无法通过服务发现访问的远程客户端。

## 微服务模式

微服务部署时，各平台能力可以通过对应 `starter-remote` 或网关路由完成远程访问。当前 `mango-admin-app` POM 使用本地 starter，后续 remote adapter 装配清理由对应 Phase 单独处理。

## BFF 实现原则

`mango-admin-app` 不实现 RBAC、认证、组织、国际化等领域规则，只做部署装配、协议入口和必要的本地 adapter。

应该做：

```java
@Configuration
class LocalAdapterConfiguration {
    // 将本地 starter 暴露的能力适配给装配层需要的接口
}
```

不应该做：

```java
@Service
class AdminBusinessService {
    public boolean hasPermission(Long userId, String permission) {
        // 错误：权限计算应由 RBAC 或认证桥接接口提供
        return false;
    }
}
```

## 核心接口

| 接口 | 说明 |
|------|------|
| `SysPublicPathApi` | gateway 查询匿名路径、登录必需路径和内部路径的 API |
| `ISysPublicPathService` | RBAC core 当前提供的公共路径服务，本地单体模式由 adapter 调用 |

## 配置

### 认证配置

```yaml
mango:
  gateway:
    auth-enabled: true
    jwt-secret: ${JWT_SECRET:mango-secret-key-must-be-at-least-256-bits}
    token-expire-seconds: 7200
```

### 服务扫描

```java
@MapperScan({
    "io.mango.rbac.core.mapper",
    "io.mango.i18n.core.mapper",
    "io.mango.org.core.mapper",
    "io.mango.area.core.mapper"
})
public class MangoAdminAppApplication {
}
```

## 开发约束

1. 新增管理后台能力优先放到对应 platform 模块，再由 starter 装配。
2. `mango-admin-app` 不直接访问 mapper，现有 mapper scan 仅服务本地 starter 装配，后续在 app Phase 统一复核。
3. local / remote 选择不通过业务代码中的条件分支实现。
