# mango-infra-persistence

> Mango 关系型持久化基础设施。负责数据库接入、MyBatis-Plus 基础装配、Flyway 迁移、审计填充、租户字段、表结构准入校验，以及 80% 简单 CRUD 场景的默认实现。

## 模块结构

当前目录是 persistence 聚合模块，对外 artifactId 保持不变。

| 子模块 | artifactId | 职责 |
|--------|------------|------|
| `mango-infra-persistence-starter` | `mango-infra-persistence-starter` | 持久化基础能力：实体基类、分页对象、查询注解、CRUD Service 基类、审计填充、Flyway、MyBatis-Plus、结构校验 |
| `mango-infra-persistence-web-starter` | `mango-infra-persistence-web-starter` | Web CRUD 基础能力：标准 Controller、Query/Command 参数约定、导入导出扩展点 |

推荐依赖方式：

- core/service 模块只需要数据库能力时，依赖 `mango-infra-persistence-starter`。
- starter/web 模块需要默认 CRUD Controller 时，依赖 `mango-infra-persistence-web-starter`。
- `mango-infra-persistence-web-starter` 已依赖 `mango-infra-persistence-starter`，业务 Web 模块不需要重复声明。

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-persistence-starter</artifactId>
</dependency>
```

```xml
<dependency>
    <groupId>io.mango</groupId>
    <artifactId>mango-infra-persistence-web-starter</artifactId>
</dependency>
```

## 已提供能力

| 能力 | 说明 |
|------|------|
| 实体基类 | `BaseEntity`、`AuditableEntity`、`TenantEntity` |
| 主键规范 | `BaseEntity.id` 默认 `@TableId(type = IdType.ASSIGN_ID)`，项目规范要求业务表使用 `Long` 雪花 ID |
| 审计填充 | 基于 MangoContext 自动填充 `createdBy`、`createdAt`、`updatedBy`、`updatedAt`、`tenantId` |
| Flyway | 统一装配数据库迁移 |
| MyBatis-Plus | 统一分页插件和基础配置 |
| 查询对象 | `io.mango.common.po.PageQuery`、`PersistencePageResult` |
| CRUD Service | `MangoCrudService`、`MangoCrudServiceImpl` |
| CRUD Controller | `BaseCrudController` |
| 查询注解 | `@QueryField`、`@QueryIgnore`、`QueryType` |
| 租户过滤 | 默认按实体 `tenantId` 字段追加租户条件，可用 `@IgnoreTenant` 跳过 |
| 数据权限扩展 | `DataScopeProvider`、`DataScopeRule` 作为扩展点，具体业务策略由业务模块接入 |
| 导入导出扩展 | `ExcelAdapter`、`ImportableService`、`ExportableService`，Excel 实现由 `mango-infra-excel-starter` 提供 |
| 表结构准入 | `mango:check -Drule=persistence-schema` 和启动期 schema validation |

## 表结构规范

所有 Mango 业务主数据、配置表、授权表默认必须满足：

| 规则 | 要求 |
|------|------|
| 主键 | 必须有 `id BIGINT` 主键 |
| ID 生成 | 禁止 `AUTO_INCREMENT`，统一使用雪花 ID |
| 审计字段 | 必须有 `created_by`、`created_at`、`updated_by`、`updated_at` |
| 租户字段 | 必须有 `tenant_id` |

推荐实体继承：

```java
public class UserEntity extends TenantEntity {
    private String username;
    private String nickname;
}
```

确实不应该携带标准字段的表，例如技术基础设施表、追加型日志表、外部系统同步表，需要在建表语句前显式标记豁免原因：

```sql
-- mango-check: disable persistence-audit-fields reason=外部系统同步表或基础设施运行态表
CREATE TABLE external_event (
  `id` bigint NOT NULL,
  PRIMARY KEY (`id`)
);
```

准入由三层机制保证：

- `mango:gen-crud` 默认生成带标准字段的 Flyway 建表脚本。
- `mvn mango:check -Drule=persistence-schema` 在 CI 阶段扫描 `src/main/resources/db/migration` 下的 `CREATE TABLE`。
- 启动期 `schema-validation` 通过 JDBC metadata 校验实际数据库，避免历史库或手工改库漏字段。

## CRUD 默认方案

CRUD 的目标是覆盖 80% 简单单表场景，复杂业务仍然可以直接使用 MyBatis-Plus、自定义 Mapper XML 或组合独立 QueryService/CommandService。

### Service

业务 Service 继承 `MangoCrudServiceImpl<M, E>`，只需要声明实体类型。Service 入参保持 `Object command/query`，用于支持多个 Controller、内部任务、导入流程复用同一个服务；默认实现按同名属性复制到实体或查询条件。

```java
public interface UserService extends MangoCrudService {
}
```

```java
@Service
public class UserServiceImpl
        extends MangoCrudServiceImpl<UserMapper, UserEntity>
        implements UserService {

    @Override
    protected Class<UserEntity> entityType() {
        return UserEntity.class;
    }
}
```

默认规则：

- `createByCommand(Object command)`：按同名属性复制到 Entity，再执行保存。
- `updateByCommand(Object command)`：按同名属性复制到 Entity，再按 ID 更新。
- `listByQuery(Object query)` / `pageByQuery(Object query)`：按 Query 对象同名字段构造条件。
- 默认返回 Entity；如果接口需要 `UserPageVO`、`UserDetailVO` 等不同返回模型，业务 Service 覆写 `detailById`、`listByQuery`、`pageByQuery` 或新增专用方法。

`MangoCrudService` 标准方法命名：

| 方法 | 说明 |
|------|------|
| `createByCommand` | 创建，入参为 Command 对象，返回新 ID |
| `updateByCommand` | 更新，入参为 Command 对象 |
| `deleteById` | 删除 |
| `batchDeleteByIds` | 批量删除 |
| `detailById` | 详情 |
| `listByQuery` | 列表查询 |
| `pageByQuery` | 分页查询 |

### Controller

Controller 是对外 HTTP 契约层，必须声明明确的 Command/Query 类型，确保 OpenAPI/Swagger 能展示字段结构。Service 仍然接收宽泛 `Object`，不反向约束 Controller 只能使用某一个入参类。

```java
@RestController
@RequestMapping("/users")
public class UserController extends BaseCrudController<UserService, UserCreateCommand, UserUpdateCommand, UserQuery> {

    public UserController(UserService service) {
        super(service);
    }

    @Override
    protected Class<UserQuery> queryType() {
        return UserQuery.class;
    }
}
```

默认接口：

| 方法 | 路径 | 入参 |
|------|------|------|
| `POST` | `/create` | `CreateCommand` JSON |
| `POST` | `/update` | `UpdateCommand` JSON |
| `POST` | `/delete` | `{ "id": 1 }` |
| `POST` | `/batch-delete` | `{ "ids": [1, 2] }` |
| `GET` | `/detail?id=1` | query 参数 |
| `GET` | `/page?page=1&size=10` | query 参数转换为 Query 对象 |
| `POST` | `/export` | Query JSON |
| `POST` | `/import` | `file` multipart |
| `GET` | `/import-template` | 无 |

接口规范：

- 禁止使用 URI path 参数，例如 `/users/{id}`。
- 单个查询参数可放 query string，例如 `/detail?id=1`。
- 两个以上业务参数必须收敛为 Query/Command 对象。
- Controller 不直接暴露多个散参数方法。

## 查询对象

查询对象推荐继承 `PageQuery`。

```java
public class UserQuery extends PageQuery {

    @QueryField(type = QueryType.LIKE)
    private String username;

    @QueryField(column = "status")
    private Integer status;

    @QueryField(type = QueryType.IN)
    private List<Long> deptIds;

    @QueryIgnore
    private String unused;
}
```

支持的查询类型：

| QueryType | 条件 |
|-----------|------|
| `EQ` | 等于 |
| `NE` | 不等于 |
| `LIKE` | 包含匹配 |
| `LEFT_LIKE` | 左匹配 |
| `RIGHT_LIKE` | 右匹配 |
| `IN` | 集合匹配 |
| `BETWEEN` | 区间匹配，当前支持 `List` 前两个值 |
| `GE` / `GT` | 大于等于 / 大于 |
| `LE` / `LT` | 小于等于 / 小于 |

字段未标注 `@QueryField` 时，默认按字段名转下划线列名并使用 `EQ`。

## 导入导出

`BaseCrudController` 默认提供导入导出入口，但是否可用由两个条件决定：

- 业务 Service 实现 `ExportableService<Q, ROW>` 或 `ImportableService<ROW>`。
- 应用引入 Excel 实现模块，提供 `ExcelAdapter` Bean。当前接口预留给 `mango-infra-excel-starter`。

未满足条件时，导入导出接口会返回未启用错误，不会静默执行。

导出使用 ExportExcelRow + 模板填充：业务通过 `@ExcelExport` 声明文件名、模板 key、模板位置、sheet、include/exclude、自定义表头生成器等元信息，实际 Excel 写出由 Excel starter 适配 EasyExcel 或其他实现。

导入使用 ImportExcelRow：字段上可使用 Bean Validation 注解做单行字段校验，Controller 会在调用 `importRows` 前自动执行校验。校验失败时返回 `ImportResult`，包含总行数、成功数、失败行数和失败明细。

导入失败处理模式支持两种：

- `PARTIAL_SUCCESS`：默认模式，跳过失败行，合法行继续导入。
- `ALL_SUCCESS`：只要存在失败行，整批不导入。可通过 `@ExcelImport(mode = ExcelImportMode.ALL_SUCCESS)` 固化，也可在上传请求中传 `importMode=ALL_SUCCESS` 临时覆盖。

如果业务需要 PigX 风格的显式方法签名，可以在自定义 Controller 方法中使用 `@RequestExcel List<UserImportExcelRow>`。框架会通过 `ExcelAdapter` 解析上传文件，并给 `@ExcelLine` 字段写入 Excel 原始行号。

复杂业务校验放在 Service 的 `validateImportRows` 中，比如：

- 上传文件内唯一性校验，例如同一个用户名在当前 Excel 内重复。
- 与数据库已有数据重复校验，例如用户名、手机号、编码已存在。
- 跨字段组合校验，例如“开始时间必须小于结束时间”。
- 需要查询当前租户、应用、组织等上下文的数据校验。

```java
public class UserServiceImpl
        extends MangoCrudServiceImpl<UserMapper, UserEntity>
        implements UserService,
        ExportableService<UserQuery, UserExportExcelRow>,
        ImportableService<UserImportExcelRow> {

    @Override
    public Class<UserExportExcelRow> exportRowType() {
        return UserExportExcelRow.class;
    }

    @Override
    public List<UserExportExcelRow> exportRows(UserQuery query) {
        return listByQuery(query).stream()
                .map(UserVO.class::cast)
                .map(this::toExportRow)
                .toList();
    }

    private UserExportExcelRow toExportRow(UserVO vo) {
        UserExportExcelRow row = new UserExportExcelRow();
        row.setUsername(vo.getUsername());
        row.setNickname(vo.getNickname());
        return row;
    }

    @Override
    public Class<UserImportExcelRow> importRowType() {
        return UserImportExcelRow.class;
    }

    @Override
    public List<ImportError> validateImportRows(List<UserImportExcelRow> rows, ExcelImportContext context) {
        List<ImportError> errors = new ArrayList<>();
        Set<String> usernames = new HashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            int line = context.headRowNumber() + i + 1;
            String username = rows.get(i).getUsername();
            if (!usernames.add(username)) {
                errors.add(ImportError.of(line, "username", "上传文件内用户名重复"));
            }
            if (existsByUsername(username)) {
                errors.add(ImportError.of(line, "username", "用户名已存在"));
            }
        }
        return errors;
    }

    @Override
    public ImportResult importRows(List<UserImportExcelRow> rows) {
        saveBatch(rows.stream().map(this::toEntity).toList());
        return ImportResult.success(rows.size());
    }
}
```

## 多表查询

多表查询不放进 `MangoCrudServiceImpl` 默认 CRUD 基类。CRUD 基类只覆盖单表 80% 场景；多表列表、详情、报表、聚合统计属于读模型，推荐单独建 `QueryService`。

推荐分三档：

| 档位 | 适用场景 | 推荐程度 |
|------|----------|----------|
| XML Mapper | 复杂 JOIN、动态条件、报表查询 | 默认推荐 |
| QueryService + XML Mapper | 需要复用分页、租户、上下文、权限条件 | 框架推荐 |
| Query DSL / 动态 SQL 组件 | 查询组合非常复杂、条件需要跨接口复用 | 后续增强方向 |

不推荐把多表 SQL 写成注解 SQL。注解 SQL 只适合非常短的演示或极简单查询，真实业务至少应放到 `mapper.xml`，原因是：

- SQL 可读性、可维护性更好。
- 可以拆 `<sql>` 片段复用列、JOIN、条件块。
- 动态条件和 resultMap 更清晰。
- 更容易做 SQL Review 和后续性能优化。

当前 starter 提供 `MangoQueryServiceSupport`，用于收敛复杂读模型中反复出现的模板逻辑：

- `page(query)`：把 `PageQuery` 转成 MyBatis-Plus `Page<T>`。
- `pageResult(page)`：把 `IPage<T>` 转成 `PersistencePageResult<T>`。
- `tenantId()` / `currentContext()`：读取当前持久化上下文。

示例：用户列表需要关联部门名称。业务只定义查询对象、VO、QueryService 和 XML Mapper。

```java
public class UserDeptQuery extends PageQuery {
    private String usernamePrefix;
    private String deptName;
}
```

```java
public class UserDeptVO {
    private Long userId;
    private String username;
    private Long deptId;
    private String deptName;
    private String tenantId;
}
```

```java
@Service
public class UserDeptQueryService extends MangoQueryServiceSupport {

    private final UserDeptQueryMapper mapper;

    public UserDeptQueryService(UserDeptQueryMapper mapper,
                                PersistenceContextProvider contextProvider) {
        super(contextProvider);
        this.mapper = mapper;
    }

    public PersistencePageResult<UserDeptVO> pageUserWithDept(UserDeptQuery query) {
        IPage<UserDeptVO> result = mapper.pageUserWithDept(page(query), query, tenantId());
        return pageResult(result);
    }
}
```

```java
@Mapper
public interface UserDeptQueryMapper {

    IPage<UserDeptVO> pageUserWithDept(Page<UserDeptVO> page,
                                       @Param("query") UserDeptQuery query,
                                       @Param("tenantId") String tenantId);
}
```

`mapper/UserDeptQueryMapper.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.user.UserDeptQueryMapper">

    <resultMap id="UserDeptVOMap" type="com.example.user.UserDeptVO">
        <id column="user_id" property="userId"/>
        <result column="username" property="username"/>
        <result column="tenant_id" property="tenantId"/>
        <result column="dept_id" property="deptId"/>
        <result column="dept_name" property="deptName"/>
    </resultMap>

    <sql id="UserDeptColumns">
        u.id AS user_id,
        u.username AS username,
        u.tenant_id AS tenant_id,
        d.id AS dept_id,
        d.name AS dept_name
    </sql>

    <sql id="TenantScopedUserDeptJoin">
        FROM demo_user u
        JOIN demo_dept d ON d.id = u.dept_id AND d.tenant_id = u.tenant_id
        WHERE u.tenant_id = #{tenantId}
    </sql>

    <sql id="UserDeptConditions">
        <if test="query.usernamePrefix != null and query.usernamePrefix != ''">
            AND u.username LIKE CONCAT(#{query.usernamePrefix}, '%')
        </if>
        <if test="query.deptName != null and query.deptName != ''">
            AND d.name = #{query.deptName}
        </if>
    </sql>

    <select id="pageUserWithDept" resultMap="UserDeptVOMap">
        SELECT
        <include refid="UserDeptColumns"/>
        <include refid="TenantScopedUserDeptJoin"/>
        <include refid="UserDeptConditions"/>
        ORDER BY u.username ASC
    </select>
</mapper>
```

多表查询规范：

- Mapper 方法必须返回专用 VO，不直接返回 Entity。
- XML 中优先使用 `<resultMap>`，不要依赖隐式字段映射处理复杂对象。
- JOIN 条件必须带租户边界，例如 `d.tenant_id = u.tenant_id`。
- WHERE 条件必须显式带当前租户，例如 `u.tenant_id = #{tenantId}`。
- 两个以上查询参数必须收敛到 Query 对象。
- 复杂查询不要复用 CRUD Query，应该定义面向读模型的专用 Query。
- 数据权限条件后续通过 `DataScopeProvider` 或业务 QueryService 注入，不在 XML 中硬编码组织权限规则。

该模式已有真实 H2 集成测试覆盖：`demo_user` JOIN `demo_dept`，通过 XML Mapper 验证分页、查询条件和租户隔离。

## 配置示例

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/mango?useUnicode=true&characterEncoding=utf-8
    username: root
    password: ${DB_PASSWORD}
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

mango:
  persistence:
    flyway:
      enabled: true
    mybatis-plus:
      pagination:
        enabled: true
        max-limit: 500
        overflow: false
        db-type:
    audit:
      enabled: true
    schema-validation:
      enabled: true
      fail-fast: false
      required-columns:
        - created_by
        - created_at
        - updated_by
        - updated_at
        - tenant_id
      excluded-tables:
        - flyway_schema_history
        - databasechangelog
        - databasechangeloglock
        - kv_record
        - infra_kv_entry
        - sys_login_log
        - sys_operation_log
```

## 设计边界

- 本模块不替代 MyBatis-Plus。`MangoCrudServiceImpl` 是薄封装，用来统一项目默认 CRUD 入口和命名。
- 复杂查询、多表联查、报表查询、大批量导入导出，优先在业务模块自定义 Mapper、XML、QueryService 或专用应用服务中实现。
- 数据权限当前提供扩展点，不在 persistence 中固化业务规则。
- Excel 读写实现不放在 persistence 内，persistence-web 只依赖 `ExcelAdapter` 抽象。
- Web 默认接口只使用 query/body 参数，不使用 path 参数。

## 待补齐

| 功能 | 状态 | 说明 |
|------|------|------|
| Excel starter 对接 | 待实现 | 为 `ExcelAdapter` 提供 EasyExcel 或其他实现 |
| 数据权限集成 | 待实现 | 基于 `DataScopeProvider` 接入 authorization/org 权限上下文 |
| 多数据源自动配置 | 待开发 | 动态数据源切换 |
| 分布式事务 | 待开发 | Seata 等能力统一封装 |
| SQL 审计日志 | 待开发 | 记录 SQL 执行情况 |
| 分库分表支持 | 待开发 | ShardingSphere 集成 |
