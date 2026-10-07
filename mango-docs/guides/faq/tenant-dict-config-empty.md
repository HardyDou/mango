# 租户字典配置为空排障

## 1. 适用场景

业务页面中的字典、下拉、组织、用户、岗位、系统配置或初始化数据为空，且问题只在部分租户或部分账号出现。

## 2. 阅读顺序

| 顺序 | 文档                                                                                     | 关注点                                              |
| ---- | ---------------------------------------------------------------------------------------- | --------------------------------------------------- |
| 1    | [Identity 后端 README](../../../mango/mango-platform/mango-identity/README.md)           | 用户、账号、租户身份                                |
| 2    | [Org 后端 README](../../../mango/mango-platform/mango-org/README.md)                     | 组织、岗位、组织树                                  |
| 3    | [System 后端 README](../../../mango/mango-platform/mango-system/README.md)               | 系统配置、字典、参数                                |
| 4    | [Issue #184 数据治理设计](../../designs/2026-07-01-issue-184-data-governance-design.md)  | Flyway、Resource、demo、`INIT_ONLY` 和外部 SQL 边界 |
| 5    | [Resource 后端 README](../../../mango/mango-platform/mango-resource/README.md)           | 资源声明同步、demo 隔离和运行时保留策略             |
| 6    | [Access 后端 README](../../../mango/mango-platform/mango-access/README.md)               | 接口访问和数据权限上下文                            |
| 7    | [Authorization 后端 README](../../../mango/mango-platform/mango-authorization/README.md) | 菜单、角色和权限资源                                |
| 8    | [@mango/admin-shell README](../../../mango-ui/packages/admin-shell/README.md)            | 登录态、租户切换、上下文透传                        |

## 3. 接入检查点

| 环节       | 检查点                                                                                                                                                                                                 |
| ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 租户上下文 | 当前登录用户的 tenantId 与业务数据 tenantId 一致                                                                                                                                                       |
| 请求透传   | 请求头或上下文中租户信息已透传到后端                                                                                                                                                                   |
| 基础数据   | 目标租户已初始化所需字典、配置、组织或岗位数据                                                                                                                                                         |
| 数据过滤   | 查询接口没有被数据权限、组织范围或状态字段过滤掉                                                                                                                                                       |
| 前端参数   | 前端查询参数没有带错 appCode、dictCode、domainCode 或 status                                                                                                                                           |
| 初始化边界 | DDL 和大 SQL 由 Flyway 处理；正式小资源由 Resource `META-INF/mango/resources/` 处理；demo 资源由 `META-INF/mango/demo/` 且默认禁用；运行时可修改但升级要保留的数据使用 `INIT_ONLY` 或业务开通/导入流程 |

## 4. 最小闭环

1. 用目标租户账号登录。
2. 打开同一页面并记录请求中的 tenantId 或租户上下文。
3. 直接调用对应后端查询接口，确认返回数据和页面一致。
4. 补齐租户基础数据后重新登录验证。
5. 用另一个租户账号复测，确认数据隔离符合预期。

## 5. 常见失败

| 现象                         | 优先检查                                 |
| ---------------------------- | ---------------------------------------- |
| 平台租户有数据，业务租户为空 | 租户开通流程、业务导入任务、租户应用绑定 |
| 用户下拉为空                 | identity 用户状态、组织关系、租户上下文  |
| 组织树为空                   | org 初始化数据、组织状态、父子关系       |
| 字典项为空                   | system 字典编码、状态、租户维度          |
| 切换租户后仍显示旧数据       | 前端缓存、登录态刷新、请求头租户 ID      |

## 6. 验证命令

后端验证使用 Java 21。Org/System 的编译基线统一和 Resource 测试目录校准不改变公开 API、配置、菜单、权限、租户过滤、页面、启动或基础数据运行时语义；本场景仍按上文检查租户上下文及正式资源初始化，不通过测试 fixture 补生产数据。

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-identity,mango-platform/mango-org,mango-platform/mango-system,mango-platform/mango-resource -am test
pnpm -F @mango/system build
pnpm -F @mango/admin-shell build
```

模块验证入口：

- [Identity 验证方式](../../../mango/mango-platform/mango-identity/README.md)
- [Org 验证方式](../../../mango/mango-platform/mango-org/README.md)
- [System 验证方式](../../../mango/mango-platform/mango-system/README.md)
- [Resource 同步规则](../../../mango/mango-platform/mango-resource/README.md#10-同步规则)
- [Access 验证方式](../../../mango/mango-platform/mango-access/README.md)
- [Authorization 验证方式](../../../mango/mango-platform/mango-authorization/README.md)
- [数据初始化与停机升级治理](../../designs/2026-07-01-issue-184-data-governance-design.md)

## 7. 关联规则

- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量规则](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 8. 历史变更

详细版本影响见[租户基础数据排障历史索引](../../changelog/business-integration/tenant-dict-config-empty.md)。
