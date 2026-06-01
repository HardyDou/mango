# 后端命名规范

## 1. 基本规则

- 模块名使用 `kebab-case`。
- 包名使用小写。
- 类名使用 `PascalCase`。
- 方法和变量使用 `camelCase`。
- 表名和字段名使用小写下划线。

## 2. 统一后缀

- 持久化实体：`XxxEntity`
- 新增入参：`CreateXxxCommand`
- 更新入参：`UpdateXxxCommand`
- 查询入参：`XxxQuery`
- 分页查询：`XxxPageQuery`
- 返回对象：`XxxVO`
- 对外接口：`XxxApi`
- 内部服务：`IXxxService`
- 对外接口实现：`XxxController`、`XxxFeignClient`
- 内部服务实现：`XxxService`
- 扩展接口：`I...Provider`、`I...Checker`、`I...Validator`

## 3. 禁止后缀

- `PO`
- `DO`
- `DTO` 作为仓内业务 API 入参或返回命名

## 4. 包规则

- `api` 只放 `command`、`query`、`vo`、`enums`、`XxxApi`。
- `core` 只放 `entity`、`service`、`mapper`、`convert`。
- 不新增语义不清的 `model`、`dto`、`po`、`util`、`helper` 杂项包。

## 5. 领域词规则

- 同一业务概念在模块、包、类、表中使用同一领域词。
- 不允许同一模块出现多套命名。
- `XxxApi` 名称只表达跨模块能力，不使用 `Registry`、`Dispatcher`、`Manager`、`Session` 等内部实现词。
- 历史命名迁移后不保留第二套新实现。

## 6. KV Key 规则

- Redis/KV key 统一由 `mango-infra-kv` capability 层增加基础设施前缀。
- 默认格式：`mango:kv:{env}:{capability}:{biz-key}`。
- 可选应用隔离格式：`mango:kv:{env}:{app}:{capability}:{biz-key}`。
- 默认不加应用名，保证同一环境内 infra KV 可跨应用共享。
- 环境段用于隔离 dev/test/prod。
- 能力段固定为：`cache`、`lock`、`counter`、`rate-limit`、`idempotent`、`token`、`idgen`。
- 业务代码只传业务段 key，例如 `user:#{#userId}`。
- KV 注解动态 key 必须使用 SpEL 模板或直接 SpEL 表达式。
- 禁止业务代码手写 `mango:kv` 完整前缀。
- 禁止 `user:#userId`、`user:@bean` 这类非标准内联占位写法。

## 7. 禁止事项

- 混用多种命名风格
- 使用无语义缩写
- 用 `data`、`info`、`temp`、`obj` 这类泛化命名
