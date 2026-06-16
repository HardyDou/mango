# Mango Admin Starter

## 1. 概览
`mango-admin-starter` 是 Mango 后端管理端能力聚合 starter。宿主应用引入它后，会一次性获得管理后台常用的基础设施和平台能力 starter。

它只做 Maven 依赖聚合，不提供自己的 Controller、Service、Mapper、配置文件、migration 或 seed 数据。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 单体管理后台希望快速接入 Mango 平台能力 | Maven 依赖 / HTTP API / Java API |
| 业务宿主应用需要一次性启用认证、身份、授权、组织、系统、文件、模板、工作流、任务、支付等能力 | Maven 依赖 / HTTP API / Java API |
| 本地开发希望减少手工维护 starter 依赖列表 | Maven 依赖 / HTTP API / Java API |


## 3. 能力边界
- 微服务调用方不应直接引入它；调用方应选择具体 `starter-remote`。
- 不用于裁剪后的轻量服务；轻量服务应按需引入具体 starter。
- 不承载业务私有模块，业务 starter 应由业务 app 自己引入。

## 4. 模块入口
`mango-admin-starter` 聚合公开 starter，不直接依赖 `*-core` 模块。真正的 Bean、Controller、migration、Runner、Initializer、配置属性都来自被聚合的 starter。

宿主应用仍负责：

- Spring Boot 启动类。
- datasource、Flyway、KV、JWT、文件目录、端口等运行配置。
- profile、部署、日志、监控。
- 引入业务私有 starter。

## 5. 接入方式
Maven 依赖：

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-admin-starter</artifactId>
</dependency>
```

单体 app 示例：

```xml
<dependencies>
    <dependency>
        <groupId>io.mango</groupId>
        <artifactId>mango-admin-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
</dependencies>
```

## 6. 配置说明
本 starter 没有自己的配置项。配置来自被聚合模块。

宿主应用至少需要准备：

| 配置 | 来源模块 | 含义 |
|------|----------|------|
| datasource | persistence / 各平台模块 | 数据库连接。 |
| Flyway module 开关 | persistence | 控制各模块 migration。 |
| JWT secret | auth / access | 登录和鉴权密钥。 |
| KV store | infra-kv | token、验证码、outbox 等 KV 能力。 |
| file storage | mango-file | 文件存储类型和根目录。 |
| IP location xdb | infra-ip-location | IP 定位数据文件。 |
| doc path | infra-doc | OpenAPI 和 Swagger UI。 |

具体字段看各模块 README 和宿主 app 的 `application.yml`。

## 7. API 与扩展
聚合的基础设施 starter：

- `mango-infra-module-starter`
- `mango-infra-kv-starter`
- `mango-infra-event-starter`
- `mango-infra-ip-location-starter`
- `mango-infra-realtime-starter`
- `mango-infra-persistence-web-starter`
- `mango-infra-doc-starter`
- `mango-infra-web-starter`

聚合的平台 starter：

- `mango-auth-starter`
- `mango-identity-starter`
- `mango-authorization-starter`
- `mango-authorization-resource-sync-starter`
- `mango-authorization-resource-access-starter`
- `mango-access-web-starter`
- `mango-org-starter`
- `mango-captcha-starter`
- `mango-system-starter`
- `mango-domain-starter`
- `mango-notice-starter`
- `mango-file-starter`
- `mango-file-preview-starter`
- `mango-template-starter`
- `mango-workflow-starter`
- `mango-job-starter`
- `mango-calendar-starter`
- `mango-numgen-starter`
- `mango-payment-starter`

## 8. 数据与初始化
本 starter 不提供独立数据库对象。表结构、菜单、权限、字典、租户、任务、模板、流程、支付、文件等初始化来自被聚合模块的 migration、Runner 或 Initializer。

宿主应用必须：

- 提供 datasource。
- 打开目标模块 Flyway 开关。
- 确认初始化逻辑幂等。
- 确认菜单和权限进入 authorization 数据。

## 9. 管理入口
菜单、权限和租户能力来自 `mango-authorization`、`mango-system`、`mango-identity`、`mango-org`、`mango-access` 以及各业务模块。

引入本 starter 后不代表用户自动有权限。还需要：

- 初始化菜单和权限资源。
- 绑定角色和用户。
- 租户绑定应用或菜单包。
- 前端页面 key 与菜单 component 对齐。

## 10. 快速开始
1. 宿主 app 引入 `mango-admin-starter`。
2. 配置 datasource、JWT、KV、文件目录和 Flyway 模块开关。
3. 启动后确认 migration 和初始化数据完成。
4. 登录并验证菜单、权限、租户、字典和核心平台接口。
5. 再按需引入业务私有 starter，并补齐业务菜单和权限。

## 11. 问题排查
- 引入后启动失败：通常是 datasource、KV、文件目录、IP 数据文件或 Bean 条件缺失。
- 引入后没有表：starter 进入 classpath 不等于 migration 已打开。
- 引入后菜单为空：检查 authorization 初始化和角色授权。
- 服务太重：不要用聚合 starter，改为按需引入具体 starter。

## 12. 相关文档
- [后端模块规范](../../mango-pmo/rules/backend/05-module.md)
- [后端测试规范](../../mango-pmo/rules/backend/08-test.md)
- [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)

## 13. 补充资料
- [Mango Monolith App README](../mango-app/monolith/mango-monolith-app/README.md)
- [Mango 后端根 README](../README.md)
- [Mango 能力地图](../../mango-docs/capabilities/README.md)
