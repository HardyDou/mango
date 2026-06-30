# Issue #184 S3 Flyway 外部升级脚本计划

## 背景

当前 Mango 已经按模块执行 Flyway migration，每个模块独立 history table。DDL 和版本化 SQL 已由 Flyway 管理，但脚本来源固定为 `classpath:db/migration/<module>`，不能覆盖停机升级时由运维指定磁盘目录或远程 URL SQL 的场景。

## 目标

复用现有模块化 Flyway，支持模块级外部 migration locations：

- 默认行为不变：未配置时仍执行 `classpath:db/migration/<module>`。
- 支持 `filesystem:/path/to/migrations`，用于磁盘升级包。
- 支持 `http://.../Vxxx__name.sql` 和 `https://.../Vxxx__name.sql`，启动迁移前下载到临时目录后交给 Flyway。

## 范围

- `mango-infra-persistence-starter` 增加模块级 `locations` 配置。
- Flyway 初始化器解析 classpath/filesystem/http(s) locations。
- README 补充停机升级配置示例。
- 单元测试覆盖默认路径、filesystem、URL SQL。

## 不做

- 不实现在线影子表升级。
- 不引入 DataOps 任务历史表。
- 不新增管理页面。
- 不提供绕过 Flyway history table 的裸 SQL 执行器。

## 验收计划

- 未配置 `locations` 时，模块仍执行默认 classpath migration。
- 配置 `filesystem:` 目录时，Flyway 执行磁盘目录内版本化 SQL。
- 配置 `http(s)` SQL 文件时，Flyway 下载并执行该 SQL。
- URL 文件必须是 `.sql` 文件。
- Persistence Flyway 相关 Maven 测试通过。
