# Mango 单体部署入口

面向本地开发和小规模部署的单进程启动入口。

## 职责

| 范围 | 依赖 |
|------|------|
| 安全、认证、身份、授权 | `mango-auth-starter`、`mango-identity-starter`、`mango-authorization-starter` |
| 组织、系统、验证码、消息 | 本地平台 starter |
| KV、实时通信、模块元数据 | infra starter |
| 业务能力 | 后续业务 starter 添加到这里 |

旧的 `mango-admin-app` 目录已合并到该模块。该 app 只是部署装配层，不承载长期领域逻辑。

## 启动

```bash
mvn -pl :mango-monolith-app -am spring-boot:run
```

默认端口：`5555`。

根工程默认让非应用模块跳过 `spring-boot:run`，只有应用模块显式声明 `mainClass` 并开启运行。这样从仓库根目录启动时，依赖模块只参与编译，不会被 Maven 误当作 Spring Boot 应用执行。

本地调试常用参数：

```bash
mvn -pl :mango-monolith-app -am spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=5555 --mango.persistence.flyway.enabled=false --mango.kv.store.type=memory --spring.autoconfigure.exclude=org.redisson.spring.starter.RedissonAutoConfigurationV2"
```
