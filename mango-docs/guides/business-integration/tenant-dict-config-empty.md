# 租户字典配置为空排障

## 1. 适用场景

业务页面中的字典、下拉、组织、用户、岗位、系统配置或初始化数据为空，且问题只在部分租户或部分账号出现。

## 2. 阅读顺序

| 顺序 | 文档 | 关注点 |
|------|------|--------|
| 1 | [Identity 后端 README](../../../mango/mango-platform/mango-identity/README.md) | 用户、账号、租户身份 |
| 2 | [Org 后端 README](../../../mango/mango-platform/mango-org/README.md) | 组织、岗位、组织树 |
| 3 | [System 后端 README](../../../mango/mango-platform/mango-system/README.md) | 系统配置、字典、参数 |
| 4 | [Seed 后端 README](../../../mango/mango-platform/mango-seed/README.md) | 官方固定种子数据 |
| 5 | [Access 后端 README](../../../mango/mango-platform/mango-access/README.md) | 接口访问和数据权限上下文 |
| 6 | [Authorization 后端 README](../../../mango/mango-platform/mango-authorization/README.md) | 菜单、角色和权限资源 |
| 7 | [@mango/admin-shell README](../../../mango-ui/packages/admin-shell/README.md) | 登录态、租户切换、上下文透传 |

## 3. 接入检查点

| 环节 | 检查点 |
|------|--------|
| 租户上下文 | 当前登录用户的 tenantId 与业务数据 tenantId 一致 |
| 请求透传 | 请求头或上下文中租户信息已透传到后端 |
| 基础数据 | 目标租户已初始化所需字典、配置、组织或岗位数据 |
| 数据过滤 | 查询接口没有被数据权限、组织范围或状态字段过滤掉 |
| 前端参数 | 前端查询参数没有带错 appCode、dictCode、domainCode 或 status |
| 初始化边界 | 种子数据只覆盖官方固定基线，业务租户数据由业务初始化流程补齐 |

## 4. 最小闭环

1. 用目标租户账号登录。
2. 打开同一页面并记录请求中的 tenantId 或租户上下文。
3. 直接调用对应后端查询接口，确认返回数据和页面一致。
4. 补齐租户基础数据后重新登录验证。
5. 用另一个租户账号复测，确认数据隔离符合预期。

## 5. 常见失败

| 现象 | 优先检查 |
|------|----------|
| 平台租户有数据，业务租户为空 | 租户初始化流程、业务数据 seed、租户应用绑定 |
| 用户下拉为空 | identity 用户状态、组织关系、租户上下文 |
| 组织树为空 | org 初始化数据、组织状态、父子关系 |
| 字典项为空 | system 字典编码、状态、租户维度 |
| 切换租户后仍显示旧数据 | 前端缓存、登录态刷新、请求头租户 ID |

## 6. 验证命令

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-identity,mango-platform/mango-org,mango-platform/mango-system,mango-platform/mango-seed -am test
pnpm -F @mango/system build
pnpm -F @mango/admin-shell build
```

模块验证入口：

- [Identity 验证方式](../../../mango/mango-platform/mango-identity/README.md#10-验证方式)
- [Org 验证方式](../../../mango/mango-platform/mango-org/README.md#10-验证方式)
- [System 验证方式](../../../mango/mango-platform/mango-system/README.md#10-验证方式)
- [Seed 验证方式](../../../mango/mango-platform/mango-seed/README.md#10-验证方式)
- [Access 验证方式](../../../mango/mango-platform/mango-access/README.md#10-验证方式)
- [Authorization 验证方式](../../../mango/mango-platform/mango-authorization/README.md#10-验证方式)

## 7. 关联规则

- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量规则](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 8. 变更影响记录

- PR #166 工作台自定义布局新增 `@mango/grid-layout` 和 `mango-grid-layout`，个人布局按当前登录租户和用户隔离保存；不改变租户字典、组织、用户、系统配置的公开 API、配置、权限、租户、页面和运行时行为。
- PR #153 Maven revision 支持只调整构建和发布版本解析，不改变租户字典、组织、用户、系统配置的公开 API、配置、权限、租户、页面和运行时行为。
