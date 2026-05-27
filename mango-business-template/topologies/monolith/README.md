# Monolith Topology

单体模式用于一个业务应用内直接装配 Mango 后台基础能力和业务模块本地 starter。

## 后端依赖

业务 app 依赖：

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

单体 app 不依赖 `{{moduleKebab}}-starter-remote`。

## 前端依赖

后台 app 依赖：

```json
{
  "dependencies": {
    "@mango/admin-shell": "^1.0.0",
    "@{{projectKebab}}/{{moduleKebab}}": "{{projectVersion}}"
  }
}
```

## 验证

- 后端执行业务 app 的 Maven test。
- 前端执行 `pnpm -F {{projectKebab}}-admin build`。
- 浏览器验证菜单 `{{moduleKebab}}/{{aggregateKebab}}/index` 可加载。
