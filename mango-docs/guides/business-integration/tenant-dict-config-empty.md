# 租户字典配置为空排障

## 1. 适用场景

业务页面中的字典、下拉、组织、用户、岗位、系统配置或初始化数据为空，且问题只在部分租户或部分账号出现。

## 2. 阅读顺序

| 顺序 | 文档 | 关注点 |
|------|------|--------|
| 1 | [Identity 后端 README](../../../mango/mango-platform/mango-identity/README.md) | 用户、账号、租户身份 |
| 2 | [Org 后端 README](../../../mango/mango-platform/mango-org/README.md) | 组织、岗位、组织树 |
| 3 | [System 后端 README](../../../mango/mango-platform/mango-system/README.md) | 系统配置、字典、参数 |
| 4 | [Issue #184 数据治理设计](../../designs/2026-07-01-issue-184-data-governance-design.md) | Flyway、Resource、demo、`INIT_ONLY` 和外部 SQL 边界 |
| 5 | [Resource 后端 README](../../../mango/mango-platform/mango-resource/README.md) | 资源声明同步、demo 隔离和运行时保留策略 |
| 6 | [Access 后端 README](../../../mango/mango-platform/mango-access/README.md) | 接口访问和数据权限上下文 |
| 7 | [Authorization 后端 README](../../../mango/mango-platform/mango-authorization/README.md) | 菜单、角色和权限资源 |
| 8 | [@mango/admin-shell README](../../../mango-ui/packages/admin-shell/README.md) | 登录态、租户切换、上下文透传 |

## 3. 接入检查点

| 环节 | 检查点 |
|------|--------|
| 租户上下文 | 当前登录用户的 tenantId 与业务数据 tenantId 一致 |
| 请求透传 | 请求头或上下文中租户信息已透传到后端 |
| 基础数据 | 目标租户已初始化所需字典、配置、组织或岗位数据 |
| 数据过滤 | 查询接口没有被数据权限、组织范围或状态字段过滤掉 |
| 前端参数 | 前端查询参数没有带错 appCode、dictCode、domainCode 或 status |
| 初始化边界 | DDL 和大 SQL 由 Flyway 处理；正式小资源由 Resource `META-INF/mango/resources/` 处理；demo 资源由 `META-INF/mango/demo/` 且默认禁用；运行时可修改但升级要保留的数据使用 `INIT_ONLY` 或业务开通/导入流程 |

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
- [数据初始化与停机升级治理](../../designs/2026-07-01-issue-184-data-governance-design.md)

## 7. 关联规则

- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量规则](../../../mango-pmo/rules/05-ai-delivery-quality.md)

## 8. 变更影响记录

- Issue #184 明确数据初始化与停机升级治理边界：结构和版本化 SQL 继续归 Flyway，正式小资源归 `META-INF/mango/resources/`，demo 数据归 `META-INF/mango/demo/` 且默认不启用，运行时会被用户修改且升级要保留的数据使用 Resource `INIT_ONLY` 或业务开通/导入流程，大 SQL、磁盘 SQL、远程 URL SQL 和新库 schema baseline pack 走 `mango-infra-persistence` 的模块化 Flyway `locations`。排查租户基础数据为空时，应先判断缺失数据属于正式资源、demo、运行时租户数据还是停机升级 SQL，避免把 demo 或运行时数据混入默认 migration。

- v2026.06.30-maven-1.0.1-admin-branding-cli-release 发布固定后端 Maven `1.0.1` 和后台品牌配置前端批次；品牌配置复用系统配置资源和文件中心 ID，不改变租户字典、组织、用户、系统配置公开查询 API、权限、租户隔离、页面入口、页面路由、启动方式和运行时数据行为。排查品牌图片为空时，同时确认配置资源同步、文件 ID 有效和文件读取权限。

- v2026.06.29-workflow-return-cli-db-release 只发布 CLI/PMO 基线、工作流退回和前端聚合版本锁；不改变租户字典、组织、用户、系统配置的公开查询 API、配置、权限、租户隔离方式、页面入口、页面路由、启动方式和运行时数据行为。

- PR #295 只治理 Issue #183 后端测试规范、Mockito 审计和核心 service/resource handler 集成测试；不改变租户字典、组织、用户、系统配置的公开查询 API、配置、权限、租户隔离方式、页面入口、页面路由、启动方式和运行时数据行为。

- v2026.06.27-workflow-history-dialog-release 同步发布工作流 UI 修复批次和前端聚合版本锁；不改变租户绑定、字典配置、系统配置、默认数据初始化、后端公开 API、权限、菜单、页面入口、启动方式和本场景排障步骤。

- v2026.06.27-admin-shell-menu-redirect-release 发布 `@mango/admin-shell@1.0.28`、`@mango/admin@1.0.32` 和 `@mango/cli@1.0.45`，仅让业务项目可通过 npm 包消费 Issue #274 的目录菜单 redirect 修复；不改变租户字典、组织、用户、系统配置的公开查询 API、配置、权限、租户隔离方式、页面入口、页面路由、启动方式和运行时数据行为。

- Issue #274 修复 `@mango/admin-shell` 目录型菜单 redirect 解析，仅影响目录菜单在当前用户可见菜单树中的落点选择；不改变租户字典、组织、用户、系统配置的公开查询 API、配置、权限、租户隔离方式、页面入口、页面路由、启动方式和运行时数据行为。排查租户基础数据为空时，仍以当前实际打开页面的请求、tenantId 和数据过滤链路为准。

- v2026.06.27-system-component-release 同步发布 `@mango/system@1.0.11` 及其前端依赖批次，仅对齐 npm 物料和 CLI/starter 版本锁；不改变租户字典、组织、用户、系统配置的公开查询 API、配置、权限、租户隔离方式、页面入口、页面路由、启动方式和运行时数据行为。业务项目排查系统配置或字典页面异常时，仍先确认前端包批次一致和当前租户基础数据完整。

- PR #267 新增公告发布对象解析，首期支持全员、组织、角色、指定用户。公告发布对象为空或人数异常时，应优先排查当前租户下的用户、组织、角色数据和角色成员关系；全员发布按当前租户可接收用户解析，组织/角色/指定用户去重后落到用户收件快照。此次不改变租户字典、组织、用户、系统配置的公开查询 API、配置、权限、租户隔离方式、页面入口、页面路由和运行时数据行为。

- Issue #264 发布 `@mango/grid-widgets@1.0.3`、`@mango/system@1.0.10`、`@mango/admin-pages@1.0.11`、`@mango/admin-shell@1.0.25`、`@mango/admin@1.0.29`、`@mango/cli@1.0.42`，补齐此前未进入 npm 物料的工作台日历小组件和新版系统配置页面；不改变租户字典、组织、用户、系统配置的公开查询 API、配置、权限、租户隔离方式、页面入口、页面路由、启动方式和运行时数据行为。业务项目排查首页日历缺失或系统配置页面旧版时，应先确认前端包和 CLI/starter 锁已升级到本批次。

- PR #256 将后台工作台默认布局调整为页面内固定配置，仅影响没有个人工作台配置或恢复默认后的首页卡片排布；不改变租户字典、组织、用户、系统配置的公开查询 API、配置、权限、租户隔离方式、页面入口、页面路由、启动方式和运行时数据行为。

- PR #253 新增 Resource Registry 的 `IDENTITY_USER`、`ORG_UNIT`、`ORG_POST`、`ORG_MEMBER_BINDING` 基线声明，可让 demo/bootstrap 用户、租户成员、组织、岗位和成员组织岗位关系随资源同步注入；不改变租户字典、系统配置公开查询 API、权限、租户隔离方式、页面入口和本场景排障步骤。排查清库初始化后的用户、组织或岗位为空时，可额外确认这些基线声明和目标 handler 同步结果。

- PR #246 发布 `@mango/grid-widgets@1.0.2`、`@mango/admin-shell@1.0.23`、`@mango/admin@1.0.27`、`@mango/cli@1.0.39`，用于修复 grid widgets 样式发布产物并对齐业务项目版本锁；不改变租户字典、组织、用户、系统配置的公开查询 API、配置、权限、租户隔离方式、页面入口、页面路由和运行时数据行为。

- PR #243 新增 CMS 站点、栏目、内容、广告等 Flyway 种子数据和公开站点消费链路；不改变租户字典、组织、用户、系统配置的公开查询 API、配置、权限、租户隔离方式、页面入口、启动方式和本场景排障步骤。CMS 内容数据归属 `mango-cms` 表和 CMS 管理入口，排查 CMS 内容为空时先确认站点、发布状态、有效期和 CMS migration。

- PR #241 发布 `@mango/admin-shell@1.0.22`、`@mango/admin@1.0.25` 并新增工作流业务回传路径与审批任务详情页布局优化；不改变租户字典、组织、用户、系统配置的公开查询 API、配置、权限、租户隔离方式、页面入口、页面路由和运行时数据行为。回传路径仅读取工作流任务详情页路由参数，不写入租户基础数据、字典或系统配置。

- PR #235 发布 `@mango/admin-shell@1.0.21`、`@mango/admin@1.0.24` 并对齐 `@mango/cli@1.0.36` 的发布版本锁；不改变租户字典、组织、用户、系统配置的公开查询 API、配置、权限、租户隔离方式、页面入口、页面路由和运行时数据行为。

- 本次 PR 调整 `@mango/admin-shell` 在布局 1、2、4 下的 footer 贴边和内容区底部安全距离；不改变租户字典、组织、用户、系统配置的公开查询 API、配置、权限、租户隔离方式、页面入口、页面路由和运行时数据行为。

- 本次 PR 新增 `@mango/grid-widgets` 我的申请系统小组件和 workflow 申请统计接口；不改变租户字典、组织、用户、系统配置的公开查询 API、配置、权限、租户隔离方式、页面入口和运行时行为。我的申请统计以后端 workflow 当前登录上下文为准，不写入租户基础数据、字典或系统配置。

- 本次 PR 新增 `@mango/grid-widgets` 我的待办系统小组件和 workflow 待办统计接口；不改变租户字典、组织、用户、系统配置的公开查询 API、配置、权限、租户隔离方式、页面入口和运行时行为。我的待办统计以后端 workflow 当前登录上下文为准，不写入租户基础数据、字典或系统配置。

- 本次 PR 为管理端 Element Plus 全局中文 locale 配置，确保分页等内置组件默认文案使用中文；不改变租户字典、组织、用户、系统配置的公开查询 API、配置、权限、租户隔离方式、页面入口、启动方式和运行时数据行为。

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

- Issue #250 仅调整身份安全、首次改密、密码复杂度、登录失败锁定和用户解锁/重置密码能力，不改变租户字典、组织、用户、系统配置的公开查询 API、租户切换、初始化、数据过滤或本排障场景的定位步骤。
- PR #153 Maven revision 支持只调整构建和发布版本解析，不改变租户字典、组织、用户、系统配置的公开 API、配置、权限、租户、页面和运行时行为。
- PR 本次持久化基线与 README 发布物料治理只补充业务开发查看 Mango 能力文档的入口，并让 npm 包携带 package README；不改变租户字典、组织、用户、系统配置的公开 API、配置、权限、租户、页面、启动和运行时行为。
- 本次用户信息小组件视觉优化 PR 仅调整 `@mango/grid-widgets` 中用户信息卡片展示和后台工作台默认布局高度；不改变租户字典、组织、用户、系统配置的公开查询 API、配置、权限、租户隔离方式、页面入口、页面路由和运行时数据行为。
- PR 本次新增 `@mango/grid-widgets` 日历系统小组件，并在工作台默认布局中展示；小组件仅读取日历公开接口，不写入租户基础数据、字典或系统配置；不改变租户字典、组织、用户、系统配置的公开 API、配置、权限、租户隔离方式、页面、启动和运行时行为。
- 本次 PR 隐藏后台布局配置抽屉中的深色模式、组件大小、缓存 Tagsview 和页面动画入口，仅收口未开放或未完整生效的个人偏好配置展示；不改变租户字典、组织、用户、系统配置的公开查询 API、配置、权限、租户隔离方式、页面、启动和运行时行为。
- 本次 PR 仅调整工作台系统小组件展示文案、提示方式、卡片排布和字号；不改变租户字典、组织、用户、系统配置公开查询 API、配置、权限、租户隔离、页面入口、页面路由、启动方式和运行时数据行为。
- 本次 PR 仅在进入登录页或退出登录时清理后台 TagsView 当前打开标签缓存，避免换账号后访问上一账号页面导致 404；不改变租户字典、组织、用户、系统配置公开查询 API、配置、权限、租户隔离、页面入口、页面路由、启动方式和运行时数据行为。

- Issue #259 复用 `sys_config` 保存后台品牌配置，不新增租户、字典、组织、用户基础数据表；不改变租户字典、组织、用户、系统配置公开查询 API、权限、租户隔离、页面入口、页面路由、启动方式和运行时数据行为。品牌图片字段仅保存文件中心 ID，不保存文件 preview、download 或 direct URL，排查图片不可见时应同时确认文件 ID 对应文件是否仍可访问。

- Issue #354 为 Resource Registry 增加资源类型依赖排序，仅改变同一同步批次内 handler 执行顺序，例如用户、组织、角色先于成员组织绑定和角色绑定同步；不改变租户字典、组织、用户、系统配置公开查询 API、权限、租户隔离、页面入口、页面路由、启动方式和运行时数据行为。排查基础数据为空时仍确认资源声明已同步、目标模块 handler 消费成功、租户上下文和角色/租户绑定有效。
