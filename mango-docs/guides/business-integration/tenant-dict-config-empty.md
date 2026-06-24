# 租户字典配置为空排障

## 1. 适用场景

业务页面中的字典、下拉、组织、用户、岗位、系统配置或初始化数据为空，且问题只在部分租户或部分账号出现。

## 2. 阅读顺序

| 顺序 | 文档 | 关注点 |
|------|------|--------|
| 1 | [Identity 后端 README](../../../mango/mango-platform/mango-identity/README.md) | 用户、账号、租户身份 |
| 2 | [Org 后端 README](../../../mango/mango-platform/mango-org/README.md) | 组织、岗位、组织树 |
| 3 | [System 后端 README](../../../mango/mango-platform/mango-system/README.md) | 系统配置、字典、参数 |
| 4 | [Resource 后端 README](../../../mango/mango-platform/mango-resource/README.md) | 资源声明同步和模块初始化 |
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
| 初始化边界 | 平台默认资源由 Flyway、Resource Registry 和模块 TenantProvisioner 处理；生产租户数据由业务开通、后台维护或导入流程补齐 |

## 4. 最小闭环

1. 用目标租户账号登录。
2. 打开同一页面并记录请求中的 tenantId 或租户上下文。
3. 直接调用对应后端查询接口，确认返回数据和页面一致。
4. 补齐租户基础数据后重新登录验证。
5. 用另一个租户账号复测，确认数据隔离符合预期。

## 5. 常见失败

| 现象 | 优先检查 |
|------|----------|
| 平台租户有数据，业务租户为空 | 租户开通流程、业务导入任务、租户应用绑定 |
| 用户下拉为空 | identity 用户状态、组织关系、租户上下文 |
| 组织树为空 | org 初始化数据、组织状态、父子关系 |
| 字典项为空 | system 字典编码、状态、租户维度 |
| 切换租户后仍显示旧数据 | 前端缓存、登录态刷新、请求头租户 ID |

## 6. 验证命令

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-identity,mango-platform/mango-org,mango-platform/mango-system,mango-platform/mango-resource -am test
pnpm -F @mango/system build
pnpm -F @mango/admin-shell build
```

模块验证入口：

- [Identity 验证方式](../../../mango/mango-platform/mango-identity/README.md#10-验证方式)
- [Org 验证方式](../../../mango/mango-platform/mango-org/README.md#10-验证方式)
- [System 验证方式](../../../mango/mango-platform/mango-system/README.md#10-验证方式)
- [Resource 同步规则](../../../mango/mango-platform/mango-resource/README.md#10-同步规则)
- [Access 验证方式](../../../mango/mango-platform/mango-access/README.md#10-验证方式)
- [Authorization 验证方式](../../../mango/mango-platform/mango-authorization/README.md#10-验证方式)

## 7. 关联规则

- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量规则](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 8. 变更影响记录

- 本次 PR 新增 `@mango/grid-widgets` 消息中心系统小组件；不改变租户字典、组织、用户、系统配置的公开查询 API、配置、权限、租户隔离方式、页面入口和运行时行为。消息中心小组件只读取当前登录人的通知接口，不写入租户基础数据、字典或系统配置。

- Issue #217 新增系统参数业务控制面板，样例 `NOTICE`、`WORKFLOW`、`CMS` 业务域、字典和系统参数通过 `mango-resource` 的 `BUSINESS_DOMAIN`、`SYSTEM_DICT`、`SYSTEM_CONFIG` 资源声明注入；不改变租户字典、组织、用户、系统配置的公开查询 API、权限、租户隔离方式、启动方式和本场景基础排障步骤。配置面板为空时，需要同时确认三类资源同步成功、`domainCode` 与业务域一致、系统参数状态启用，且目标租户已具备对应资源数据。

- 本次 PR 新增 `@mango/grid-widgets` 用户信息系统小组件，并在工作台 runtime 透传租户编码和租户名称用于展示；不改变租户字典、组织、用户、系统配置的公开查询 API、配置、权限、租户隔离方式、页面入口和运行时行为。用户信息小组件仅展示当前登录上下文中的租户信息，不写入租户基础数据、字典或系统配置。

- PR #216 加固前端 `@mango/*` npm 包发布边界，非 CLI 包不再发布 `src` 等源码目录，并补充发布包 tarball 和业务消费 typecheck 基线；不改变租户字典、组织、用户、系统配置的公开查询 API、配置、权限、租户隔离方式、页面入口和运行时行为。业务项目应继续使用公开 package 入口和样式入口，升级到后续发布的新包版本后重新运行前端 typecheck。

- PR #215 新增 `@mango/grid-widgets` 小组件注册聚合能力与工作台快捷入口小组件；本次不改变租户字典、组织、用户、系统配置的公开查询 API、配置、权限、租户隔离方式、页面入口和运行时行为。快捷入口配置暂存浏览器本地，不写入租户基础数据或后端配置表。

- Issue #186 runtime baseline follow-up 补齐 Resource Registry 运行态验收、Nacos 能力 app 配置、Feign 动态目标保持、系统菜单套餐继承和管理端 E2E 基线；不改变租户字典、组织、用户、系统配置的公开查询 API、权限、租户隔离方式和页面入口。业务升级后排查基础数据为空时，需要同时确认 `SYSTEM_DICT`、`SYSTEM_CONFIG`、`AUTH_MENU` 等资源声明同步成功，能力 app 使用正确 Nacos 服务名和 context path，且当前部署拓扑装配了本地或远程 Resource Registry runtime。
- PR #199 将平台菜单、接口权限和多类默认数据注入统一纳入 Resource Registry 运行态，并补齐单体/微服务能力 app 的资源同步入口；不改变租户字典、组织、用户、系统配置的公开查询 API、页面、权限和租户隔离方式。清库重建或 1.0 rebase 升级后，排查基础数据为空时，需要同时确认对应资源声明已同步、目标模块 handler 消费成功、租户应用绑定和角色授权数据已重建。
- PR #195 加固前端 `@mango/*` 包的 `exports`、`types` 和生成声明文件，使业务项目通过发布后的 `dist` 产物独立消费；不改变租户字典、组织、用户、系统配置的公开查询 API、配置、权限、租户、页面、启动方式和本场景排障步骤。业务项目应继续使用公开 package 入口和 `./style.css`，不要依赖包内 `src` 路径。
- PR #194 发布资源注册中心版本并升级 `@mango/admin-shell@1.0.20`、`@mango/system@1.0.9`、`@mango/common@1.0.10`、`@mango/cli@1.0.34` 等前端包；不改变租户字典、组织、用户、系统配置的公开查询 API、页面、权限、租户隔离方式、启动方式和本场景排障步骤。业务升级时应成组升级前端 `@mango/*` 包并刷新后端 Mango `1.0.0-SNAPSHOT` 依赖。
- PR #193 新增 `mango-resource` 注册中心并将系统字典、系统配置、业务域、消息模板、编号规则等默认数据迁移为资源声明同步；不改变租户字典、组织、用户、系统配置的公开查询 API、页面、权限和租户隔离方式。排查基础数据为空时，需要同时确认对应资源声明已同步且目标模块 `ResourceHandler` 已写入目标表。
- PR #176 新增按钮展示规则配置和登录态 `buttonRules` 返回；不改变租户字典、组织、用户、系统配置的公开 API、配置、租户隔离方式和数据初始化路径。排查租户数据为空时，仍按租户上下文、基础数据和数据过滤链路定位。
- PR #171 新增角色数据权限并让部门范围依赖成员主部门解析；不改变租户字典、组织、用户、系统配置的公开 API、配置、页面和租户隔离方式。排查租户下拉或组织类数据为空时，需要同时确认当前角色的数据权限模式和成员主部门。
- PR #166 工作台自定义布局新增 `@mango/grid-layout` 和 `mango-grid-layout`，个人布局按当前登录租户和用户隔离保存；不改变租户字典、组织、用户、系统配置的公开 API、配置、权限、租户、页面和运行时行为。
- PR #153 Maven revision 支持只调整构建和发布版本解析，不改变租户字典、组织、用户、系统配置的公开 API、配置、权限、租户、页面和运行时行为。
- PR 本次持久化基线与 README 发布物料治理只补充业务开发查看 Mango 能力文档的入口，并让 npm 包携带 package README；不改变租户字典、组织、用户、系统配置的公开 API、配置、权限、租户、页面、启动和运行时行为。
