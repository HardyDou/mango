# Microservice Topology

微服务模式用于业务模块独立部署，调用方通过 starter-remote 访问业务服务。

## 业务服务依赖

业务服务 app 依赖本地 starter：

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-admin-starter</artifactId>
</dependency>
<dependency>
    <groupId>{{groupId}}</groupId>
    <artifactId>{{moduleKebab}}-starter</artifactId>
    <version>{{projectVersion}}</version>
</dependency>
```

业务服务负责暴露 `/{module-path}` API、采集 `module.properties` 并同步资源清单。

## 调用方依赖

其它服务只依赖 API 和 remote starter：

```xml
<dependency>
    <groupId>{{groupId}}</groupId>
    <artifactId>{{moduleKebab}}-starter-remote</artifactId>
    <version>{{projectVersion}}</version>
</dependency>
```

调用方不得依赖 `{{moduleKebab}}-core`。

## 前端依赖

后台 app 与单体模式一致，继续通过 `@mango/admin-shell` 和业务页面包加载页面。

## 验证

- 业务服务执行 Maven test。
- 调用方执行远程调用契约测试。
- 前端执行构建并连接真实网关 API。
