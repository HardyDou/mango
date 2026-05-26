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

推荐从仓库根目录使用统一环境文件启动：

```bash
scripts/dev-env.sh .env.development backend
```

前后端一起启动：

```bash
scripts/dev-env.sh .env.development all
```

`.env.development` 统一配置本地端口和数据库：

```text
MANGO_BACKEND_PORT=5555
MANGO_DB_URL=jdbc:mysql://127.0.0.1:3306/mango?...
MANGO_DB_USERNAME=root
MANGO_DB_PASSWORD=
VITE_PORT=7777
VITE_ADMIN_PROXY_PATH=http://127.0.0.1:5555
```

后端 `application.yml` 只提供默认值，实际联调优先使用 `.env.development`。

也可以直接启动 Maven：

```bash
mvn -pl :mango-monolith-app -am spring-boot:run
```

默认端口：`5555`。

根工程默认让非应用模块跳过 `spring-boot:run`，只有应用模块显式声明 `mainClass` 并开启运行。这样从仓库根目录启动时，依赖模块只参与编译，不会被 Maven 误当作 Spring Boot 应用执行。

本地默认配置已经使用内存 KV，并排除了 Redisson 自动配置；数据库结构由 Mango Flyway 模块初始化。正常开发和验证不要关闭 Flyway，否则清库后会出现表结构或内置数据缺失。

需要临时改端口时只传端口参数：

```bash
mvn -pl :mango-monolith-app -am spring-boot:run \
  -Dspring-boot.run.arguments="--server.port=5555"
```
