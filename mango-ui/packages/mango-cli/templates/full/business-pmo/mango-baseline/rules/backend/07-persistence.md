# 持久化规范

## 0. Mango 持久化基线

- Mango 关系型持久化统一通过 `mango-infra-persistence-starter` 接入。
- 当前 `mango-infra-persistence-starter` 的实现基于 MyBatis-Plus，但业务代码和业务模板必须优先使用 Mango 暴露的实体、CRUD Service 和 Controller 抽象。
- Mango 开发者可以维护 MyBatis-Plus 适配、`MangoCrudService`、`MangoCrudServiceImpl`、`BaseCrudController`、审计、租户、Flyway 和 schema validation 等框架能力。
- 业务开发者不得在普通 CRUD 场景直接继承 MyBatis-Plus `ServiceImpl` 绕过 Mango CRUD 基线；复杂查询可以使用自定义 Mapper 或 XML，但必须保留租户、权限和事务边界。
- Mango CLI、starter 和业务模块模板必须生成真实数据库 CRUD 骨架，禁止生成内存 mock、固定返回值或未接数据库的接口壳。

## 0.1 业务 CRUD 模板要求

业务 CRUD 模板至少必须生成以下闭环：

- migration SQL：创建真实业务表，包含 `id`、`tenant_id`、`created_by`、`created_at`、`updated_by`、`updated_at`。
- Entity：继承 `TenantEntity`，表名和字段与 migration 一致。
- Mapper：继承 `BaseMapper<Entity>`，只承担数据访问。
- Service：继承 `MangoCrudServiceImpl<Mapper, Entity>`，优先复用 `createByCommand`、`updateByCommand`、`deleteById`、`detailById`、`pageByQuery`。
- Controller：优先继承 `BaseCrudController` 或显式调用真实 Service；不得返回假数据。
- 前端 API：必须调用真实后端 CRUD 路径，页面必须能完成新增、分页查询、详情或等价真实功能验证。

模板或 CLI 改动必须用生成后的企业业务项目做真实验证，至少覆盖数据库 migration、后端启动、创建记录、分页回显、详情查询和前端页面调用。

## 0.2 数据库命名规则

- Mango 主库默认使用 `mango`。
- Mango 模块独立数据库统一使用 `mango_{module}`。
- `{module}` 默认取 `module-path`；没有 `module-path` 时取 `module-name` 去掉开头的 `mango-`，并将 `-` 转为 `_`。
- 示例：`mango-job` / `job` 对应 `mango_job`，`mango-system` / `system` 对应 `mango_system`。
- 第三方组件内部库如果归属某个 Mango 模块托管，默认共置到该模块库；如果独立部署，也必须在设计中写清所有权、migration 归属、账号权限和备份边界。
- 禁止新增 `job`、`system`、`file` 这类无 `mango_` 前缀的模块物理数据库名。
- 本地临时验证库和 worktree 库可以使用 `mango_dev_*`，但不得作为模块部署配置样例。

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

## 4. 设计要求

- 事务范围尽量小。
- 长事务必须拆分。
- 有副作用的外部调用要考虑幂等和补偿。

## 5. 禁止事项

- 在事务里做无关长耗时操作
- 把事务注解贴满所有方法
- 用数据库实现跨模块业务耦合
