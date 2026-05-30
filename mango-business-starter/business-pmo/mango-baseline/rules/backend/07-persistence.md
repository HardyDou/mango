# 持久化规范

## 1. 事务规则

- 写操作必须放在明确事务边界内。
- 读操作默认不开事务，确有需要再加。
- 同一业务动作只定义一个主事务边界。
- 不在内部私有调用上重复定义事务。

## 2. 模式切换

- 单体或聚合部署使用本地事务。
- 微服务部署按项目基线使用分布式事务。
- 事务模式通过配置切换，不靠业务代码分叉。

## 3. 数据访问规则

- 持久化层只做数据访问。
- 业务判断放业务层。
- 跨域数据不通过跨表 join 解决。
- 业务模块必须优先依赖 `mango-infra-persistence-starter` 或 `mango-infra-persistence-web-starter`，复用其中的 MyBatis-Plus 装配、基础实体、分页、CRUD Service、审计、租户和 schema validation 能力。
- 业务表实体优先继承 `BaseEntity`、`AuditableEntity` 或 `TenantEntity`，禁止在业务模块重复实现主键、审计字段和租户字段的通用映射。
- 简单单表 CRUD 优先使用 `MangoCrudService`、`MangoCrudServiceImpl`、`BaseCrudController` 或 MyBatis-Plus `BaseMapper`，复杂查询再补充自定义 Mapper/XML。
- 业务模块禁止直接依赖 `JdbcTemplate`、`DataSource`、`Connection`、`Statement` 或手写 JDBC SQL 作为业务持久化主路径；只有基础设施组件、迁移工具、schema 检查、诊断工具或明确设计批准的跨库适配可以使用 JDBC。
- 业务模块禁止直接声明 MyBatis-Plus starter 依赖来绕过 infra-persistence；MyBatis-Plus 依赖、分页插件、审计填充和租户能力由 infra-persistence 统一提供。

## 4. 设计要求

- 事务范围尽量小。
- 长事务必须拆分。
- 有副作用的外部调用要考虑幂等和补偿。

## 5. 禁止事项

- 在事务里做无关长耗时操作
- 把事务注解贴满所有方法
- 用数据库实现跨模块业务耦合
