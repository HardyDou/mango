# Mango Infra - 基础设施模块规范

## 模块定位

`mango-infra` 是 Mango 脚手架的基础设施模块，**职责是集成第三方中间件、基础组件和服务**，以 Starter 形式提供开箱即用的能力。

## 集成范围

| 类型 | 示例 |
|------|------|
| 中间件 | Redis、Kafka、ES、OSS、MQ |
| 基础组件 | **Feign**（服务间 RPC 调用）、HttpClient5 |
| 服务集成 | Nacos、XXL-Job、Sentinel |

## 当前方案：独立 Starter

**每个中间件/服务独立一个 Maven 模块**，通过 `mango-infra-all` 聚合。

### 现有模块（13 个 Starter + 1 个聚合）

| 模块 | 说明 | 实现状态 |
|------|------|---------|
| `mango-infra-all` | 聚合所有基础设施，一键引入 | ✅ |
| `mango-infra-web` | Web 层（CORS、异常处理、响应封装） | ⭐⭐ 部分 |
| `mango-infra-persistence` | 关系型持久化、迁移、事务、MyBatis-Plus | ⭐ 依赖引入 |
| `mango-infra-redis` | Redis & Redisson | ⭐⭐⭐ 完善 |
| `mango-infra-lock` | 分布式锁 | ⭐ 框架 |
| `mango-infra-mq` | 消息队列 (Kafka) | ⭐ 框架 |
| `mango-infra-search` | 搜索引擎 (Elasticsearch) | ⭐ 框架 |
| `mango-infra-storage` | 文件存储 (OSS/S3/MinIO) | ⭐ 框架 |
| `mango-infra-feign` | OpenFeign 服务间 RPC 调用 | ⭐ 框架 |
| `mango-infra-idgen` | ID 生成器 (雪花算法) | ⭐ 框架 |
| `mango-infra-config` | 配置中心 (Nacos) | ⭐⭐⭐ 完善 |
| `mango-infra-job` | 任务调度 (XXL-Job) | ⭐⭐ 依赖引入 |
| `mango-infra-security` | 安全 (JWT) | ⭐ 框架 |
| `mango-infra-log` | 日志格式 / 审计注解 | ⭐ 框架 |
| `mango-infra-observability` | 可观测性 (Micrometer) | ⭐⭐ 部分 |
| `mango-infra-doc` | API 文档 (OpenAPI) | ⭐⭐ 部分 |

**实现状态说明：**
- ⭐⭐⭐ 完善：功能完整，有 Properties + AutoConfiguration + 实际 Bean 创建
- ⭐⭐ 部分：部分功能已实现，AutoConfiguration 不为空
- ⭐ 依赖引入：仅有 pom 依赖 + 空壳 AutoConfiguration，待完善
- ⭐ 框架：仅有 pom 依赖，无 AutoConfiguration

## 核心原则

1. **零侵入业务** - infra 模块不包含任何业务逻辑，只负责技术层面的集成
2. **SPI + Starter 模式** - 每个中间件/服务独立模块，通过 Starter 提供自动配置
3. **可插拔** - 用户只需引入需要的 Starter，不需要的可以完全不引入
4. **约定优于配置** - 提供合理的默认配置，同时支持外部化配置覆盖

## 使用方式

```xml
<!-- 引入全部基础设施 -->
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-all</artifactId>
</dependency>

<!-- 或单独引入 Web 层 -->
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-web-starter</artifactId>
</dependency>
```

## 配置示例

```yaml
# application.yml
mango:
  redis:
    enabled: true
    host: localhost
    port: 6379
  mybatis:
    enabled: true
```

## AutoConfiguration 规范

### Spring Boot 3.x 自动配置

使用 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件：

```
# META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
io.mango.infra.redis.starter.RedisAutoConfiguration
```

### @ConditionalOnProperty 控制

```java
@Configuration
@ConditionalOnProperty(prefix = "mango.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisAutoConfiguration {
    // ...
}
```

## 配置属性规范

### 属性前缀

| 模块 | 配置前缀 | 示例 |
|------|---------|------|
| Redis | `mango.redis` | `mango.redis.host=localhost` |
| Feign | `mango.feign` | `mango.feign.connect-timeout=5000` |
| MyBatis | `mango.mybatis` | `mango.mybatis.mapper-locations=classpath*:/mapper/**/*.xml` |

### 配置属性类命名

```java
// ✅ 正确：XxxProperties
@ConfigurationProperties(prefix = "mango.redis")
public class RedisProperties {
    private String host = "localhost";
    private int port = 6379;
}
```

## 包命名规范

所有 Starter 模块统一使用 `.starter` 后缀包名：

| 层次 | 包路径 | 示例 |
|------|--------|------|
| 核心接口/类 | `io.mango.infra.[module]` | `io.mango.infra.redis` |
| 配置 & 属性 | `io.mango.infra.[module].starter` | `io.mango.infra.redis.starter` |

> 注意：`RedisProperties` 放在 `io.mango.infra.redis.starter` 包内，不单独拆分 `prop` 包。拆分会增加包层级复杂度，AI Agent 理解成本高。

## 设计决策

### 为什么用 `mango-` 前缀？

配置属性统一使用 `mango.` 前缀，便于用户识别这是 Mango 脚手架的配置项。

### 为什么每个中间件独立一个模块？

1. **AI Agent 可理解性** - 每个模块职责单一，AI Agent 读取 README 即可理解模块用途
2. **按需引入** - 用户只需引入实际用到的 Starter，不引入不产生任何影响
3. **版本独立** - 每个中间件可独立升级版本，互不影响
4. **聚合便捷** - `mango-infra-all` 提供一键全量引入，平衡便利性和灵活性

### 为什么配置前缀用 `mango.`？

与 Spring Boot 的 `spring.*`、MyBatis-Plus 的 `mybatis-plus.*` 保持一致，约定优于配置。
