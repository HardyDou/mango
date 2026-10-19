# 标准交付记录

> Issue #1056：CMS 匿名站点解析支持 `siteCode`，解除匿名只能用 `domain` 的限制

## 1. 元数据

- 任务 ID：Issue #1056
- 交付模式：STANDARD
- 需求影响：L2 - 匿名公开站点解析接口的选择器行为变化，站点编码本身为公开信息，解析结果仍为公开站点元数据。
- 方案风险：L2 - 移除 `CmsSiteService` 匿名分支校验并保留唯一性与下游租户绑定；不改数据库、权限放行、Controller 路由和前端契约。
- 最终风险：L2
- 工作区决策：CREATE：`/Users/hardy/Work/mango-issue-1056`，分支 `fix/issue-1056-cms-anonymous-site-code`
- Non-downgradable facts：AUTHORIZATION/TENANT - 匿名解析选择器涉及匿名访问边界；未申请降级，按 L2 默认 STANDARD 执行。
- 决策复核：`AGENT_ONLY`；未指定外部同行评审。

## 2. 目标与范围

- 目标：匿名调用 `GET /cms/open/sites/resolve` 时可使用 `siteCode` 或 `domain` 二选一解析启用站点，消除 `匿名站点解析必须提供域名` 与 `匿名站点解析不能使用站点编码` 两条限制。
- 成功条件：匿名 `siteCode` 能解析唯一启用站点；`siteCode` 与 `domain` 同时缺失时返回 `站点编码或域名不能为空`；匿名跨租户存在同 `siteCode` 时返回 `站点不存在或不唯一`；匿名 `domain` 解析结果与错误消息不变；解析结果仍绑定目标站点 `tenantId`；CMS 后端测试通过。
- 处理范围：`CmsSiteService.resolveSiteEntity` 匿名校验、`CmsSupportTest`、`CmsSiteServiceBehaviorTest`、`mango-cms/README.md` 排查说明、本记录。
- 不处理范围：不新增 `cms_site_domain` 多域名映射表和站点表单；不改 `@mango/site-shell` 前端解析逻辑；不改 `CmsSiteController` 路由、`@ApiAccess` 放行路径、权限码或数据库结构。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| SR-001 | 匿名 `GET /cms/open/sites/resolve?siteCode=<code>` | 库中存在唯一启用站点，无租户上下文 | 返回该站点 `SiteResolveVO` | 无匹配或多匹配时返回 400 `站点不存在或不唯一` | `CmsSiteServiceBehaviorTest.anonymousResolve_acceptsSiteCode` 通过 |
| SR-002 | 匿名 `GET /cms/open/sites/resolve` | 未提供 `siteCode` 和 `domain` | 拒绝请求，不查询站点 | 400 `站点编码或域名不能为空` | `anonymousResolve_requiresSiteCodeOrDomain` 通过，且 `siteMapper.selectList` 未被调用 |
| SR-003 | 匿名 `GET /cms/open/sites/resolve?siteCode=<code>` | 多个租户存在同 `siteCode` | 拒绝请求，不猜测租户 | 400 `站点不存在或不唯一` | `anonymousResolve_rejectsAmbiguousSiteCodeAcrossTenants` 通过 |
| SR-004 | 匿名 `GET /cms/open/sites/resolve?domain=<host>` | 库中存在唯一启用域名站点 | 与修复前一致的 `SiteResolveVO` 与错误消息 | 无匹配时保持 400 `站点不存在或不唯一` | `resolveSite_returnsTheSingleEnabledDomainSite`、`resolveSite_rejectsMissingOrAmbiguousSite` 通过 |
| SR-005 | 公开内容、栏目、导航、Banner、广告、文件读取 | 已按匿名 `siteCode` 解析出站点 | 下游查询继续绑定解析站点的 `tenantId`/`siteId`，不跨租户泄露 | 公开资格不满足时按既有语义返回不存在或空 | 既有 `CmsSiteServiceBehaviorTest` 公开读取用例保持通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | SR-001, SR-002, SR-003 | 删除仅针对匿名分支的 `validateResolveScope`；匿名解析改为与带租户上下文一致，接受 `siteCode` 或 `domain` 二选一，唯一性仍由 `sites.size() == 1` 判定 | `mango/mango-platform/mango-cms/mango-cms-core/.../CmsSiteService.java` | 恢复匿名分支校验方法及调用 |
| TD-002 | SR-004, SR-005 | 不改 Controller、`@ApiAccess`、权限码、数据库和下游租户绑定逻辑；`domain` 解析优先级和既有错误消息不变 | 无额外生产路径 | 无需回滚，未改这些契约 |
| TD-003 | SR-001, SR-002, SR-003 | 复用 `SiteBaseQuery` 已有 `siteCode`/`domain` 字段和 `siteMapper` 既有查询，不新增表、字段、接口或依赖，满足 `REDLINE-REUSE-010` 与 `REDLINE-MINIMAL-020` | 无新增公共类型 | 无需回滚，无新增结构 |
| TD-004 | 全部 | 更新模块 README 排查说明；历史 `cms-architecture-debt` TDD 的 `SEC-002`/`API-003`/`TC-002` 关于“匿名只能按域名解析”的表述由本记录取代，不就地修改已批准且被生命周期哈希链接的 TDD | `mango/mango-platform/mango-cms/README.md`、本记录 | 恢复 README 文案 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IM-001 | TD-001 | 1 | `CmsSiteService.java` | 移除 `validateResolveScope` 方法及其调用，匿名与有租户解析共用唯一性校验 |
| IM-002 | TD-001, TD-003 | 2 | `CmsSupportTest.java`、`CmsSiteServiceBehaviorTest.java` | 删除已失效校验测试，新增匿名 `siteCode` 成功、参数缺失、跨租户歧义三个用例 |
| IM-003 | TD-004 | 3 | `mango/mango-platform/mango-cms/README.md` | 快速开始与问题排查说明 `domain`/`siteCode` 二选一及匿名歧义语义 |
| IM-004 | TD-004 | 4 | 本记录 | 目标、决定、验证证据和剩余风险可追溯 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| SR-001, SR-002, SR-003, SR-004, SR-005 | 定向单元测试（M10） | `mvn -f mango/mango-platform/mango-cms/pom.xml test` | PASS：CMS 四模块全部通过；`mango-cms-core` 27 条（`CmsSiteServiceBehaviorTest` 11、`CmsSupportTest` 3、`CmsAdminServiceSecurityTest` 11、`CmsMigrationContractTest` 2），失败/错误/跳过 0 | `mango-cms-core/target/surefire-reports/` 各 `*.txt` |
| SR-004 | 域名解析行为回归 | 同上，`resolveSite_returnsTheSingleEnabledDomainSite`、`resolveSite_rejectsMissingOrAmbiguousSite` | PASS | 同上 |
| SR-001, SR-002, SR-003 | 修复前失效基线 | 修复前 `CmsSupportTest.validateResolveScope_匿名解析只能使用域名` 断言 `匿名站点解析不能使用站点编码`；修复后该断言删除并由 `anonymousResolve_acceptsSiteCode` 取代 | PASS：新用例在旧实现下会抛 `匿名站点解析必须提供域名` | `git diff` 中删除的旧断言 |
| 静态编译（M09） | 编译与测试执行 | 同上命令完成 `mango-cms-api`/`core`/`starter`/`starter-remote` 编译 | PASS | Maven 构建输出与 surefire 报告 |

## 7. 例外与剩余风险

- 未修改已批准的 `mango-docs/designs/cms-architecture-debt/technical-design.md`：其 `SEC-002`、`API-003`、`TC-002` 记录的“匿名只能按域名解析”是那次债务治理任务的状态；该 TDD 已被 `implementation-plan.md` 的 `upstreamDocumentHash` 锁定，就地修改会破坏生命周期 handoff。本记录以 TD-004 明确取代该口径。
- 未执行真实浏览器或真实后端 HTTP 验收（M13/M12）：本改动不涉及 UI，且 `CmsSiteController` 路由与 query 绑定未变化，行为变化由 `mango-cms-core` 服务层用例观察；HTTP 契约由既有 `CmsControllerContractTest` 路由断言保护。
- 匿名 `siteCode` 在多个租户使用相同编码时返回 `站点不存在或不唯一`，与 `domain` 匿名解析的歧义语义一致；平台不新增“按租户域名绑定 siteCode”的消歧能力，需要多入口访问同一站点时由业务侧统一使用 `siteCode` 或在单租户内保证唯一。
- 发布：本修复属 Maven 平台物料，随下一批 Maven 版本发布；本记录不声明已发布。
