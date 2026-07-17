# Issue #522 CMS Remote Adapter 标准交付记录

## 1. 元数据

- 任务 ID：ISSUE-522-CMS-REMOTE
- 交付模式：STANDARD
- 需求影响：L2 - 改变 CMS 公共 Java API 的组织方式并补齐已声明的微服务接入能力，HTTP、权限和业务结果保持不变
- 方案风险：L2 - 变更限定在 CMS API、Controller、Feign 和 Spring 装配，失败可整体回退，不涉及数据库或业务状态迁移
- 最终风险：L2
- 工作区决策：REUSE - `/Users/hardy/Work/mango-cms-remote-adapter`，`refactor/cms-remote-adapter`
- 保障措施：M01、M08、M09、M10、M11、M12、M15

## 2. 目标与范围

- 目标：完成 GitHub Issue #522，使 `mango-cms-starter-remote` 为全部 CMS API 提供可自动装配的 Feign 代理，并消除 66 方法的 `CmsAdminApi` 聚合边界。
- 成功条件：管理 API 按 11 个既有 core service 拆分；每个 CMS API 具有且仅具有一个 Controller 和一个 FeignClient；本地与远程 HTTP 契约一致；remote starter 可在 Spring 上下文中装配全部 Feign Bean。
- 处理范围：`mango-cms-api`、`mango-cms-starter`、`mango-cms-starter-remote`、CMS 定向测试、CMS README、能力地图和本记录。
- 不处理范围：数据库、core 业务实现、状态机、权限码、租户隔离、菜单、前端页面、既有 HTTP 路径和 Payment remote starter。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| REQ-001 | CMS 管理端 Java 消费者 | 依赖 `mango-cms-api` | 每项管理能力使用职责单一的 API 契约，不再依赖 66 方法聚合接口 | API、Controller 或 Service 边界不完整时编译或契约测试失败 | 11 个管理 API 分别对应既有 11 个 core service 和 Controller |
| REQ-002 | CMS 本地部署 HTTP 入口 | 使用既有 `/cms/**` 请求 | HTTP verb、完整 path、参数绑定、返回泛型和 `@ApiAccess` 权限保持不变 | 任一契约差异阻断交付 | 拆分前后 66 个管理方法的 HTTP/权限指纹一致 |
| REQ-003 | CMS 公开站点入口 | 使用既有 `/cms/open/**` 请求 | 继续通过统一公开读模型访问站点、栏目、导航、Banner、广告和已发布内容 | 公开访问模式或路径变化阻断交付 | `CmsSiteApi`、Controller 和 Feign 的 8 个方法一致，Controller 保持 PUBLIC |
| REQ-004 | 微服务 Java 消费者 | 依赖 `mango-cms-starter-remote` 并启用 Spring Boot | 自动获得全部 CMS API 的 Feign 代理 Bean | 缺失客户端、重复 contextId 或自动配置未注册时启动/测试失败 | 12 个 FeignClient 全部被 remote 自动配置扫描并与 API/Controller 契约一致 |
| REQ-005 | 业务开发者 | 阅读 CMS README 和能力地图 | 能明确选择本地 starter、remote starter，并定位 API/Controller/Feign 对应关系 | 文档继续宣称空制品可用或与代码不一致时阻断交付 | README 和能力地图与当前代码事实一致 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| DEC-001 | REQ-001 | 删除 `CmsAdminApi`，按现有 `ICms*Service` 划分 11 个管理 API；方法签名、校验和返回泛型原样迁移 | `mango-cms-api` | 整体回退任务提交 |
| DEC-002 | REQ-002 | 将 `CmsAdminController` 拆成 11 个 Controller，每个只依赖对应 `I*Service`；统一保留 `/cms` 根路径和原方法级 mapping、权限及 OpenAPI 描述 | `mango-cms-starter` | 整体回退任务提交 |
| DEC-003 | REQ-003 | `CmsSiteApi` 作为公开站点聚合读模型保留；它共享 `/cms/open`、PUBLIC 边界和 `ICmsSiteService`，不与后台写模型混合 | CMS API/Starter | 整体回退任务提交 |
| DEC-004 | REQ-002, REQ-003, REQ-004 | 为 12 个 API 分别提供 FeignClient；管理客户端 `path=/cms`，公开客户端 `path=/cms/open`，方法注解逐项复制对应 Controller | `mango-cms-starter-remote` | 整体回退任务提交 |
| DEC-005 | REQ-004 | 使用唯一 `CmsRemoteAutoConfiguration` 和 `AutoConfiguration.imports` 扫描 remote 包，不增加业务逻辑或 fallback | remote starter | 整体回退任务提交 |
| DEC-006 | REQ-001, REQ-005 | 不保留 `CmsAdminApi` 兼容门面；仓内无直接消费者，Issue #522 明确禁止长期第二套契约；发布时作为 Java API 迁移项说明 | API/README | 发布前不合并或整体回退 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IMP-001 | DEC-001, DEC-003 | 1 | `mango-cms-api/src/main/java/io/mango/cms/api` | 12 个职责明确的 API，原 74 个方法无遗漏 |
| IMP-002 | DEC-002 | 2 | `mango-cms-starter/.../controller` | 12 个 Controller 与 API 一一对应，原 HTTP/权限契约保持 |
| IMP-003 | DEC-004, DEC-005 | 3 | `mango-cms-starter-remote` | 12 个 FeignClient 和唯一自动配置可装配 |
| IMP-004 | DEC-001 至 DEC-005 | 4 | CMS API/Starter/Remote tests | API、Controller、Feign、装配和历史 HTTP 指纹测试通过 |
| IMP-005 | DEC-006 | 5 | CMS README、能力地图、本记录 | 接入、兼容和验证事实同步 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| REQ-001, REQ-002 | M09/M10/M12 | `mvn -f mango/pom.xml -pl ':mango-cms-api,:mango-cms-starter,:mango-cms-starter-remote' verify` | PASS - Starter 16 个测试通过；管理 API 方法总数 66，拆分前后 HTTP、权限和返回契约指纹一致 | Maven Surefire 报告与 `CmsApiSurfaceContractTest`、`CmsControllerContractTest` |
| REQ-003 | M10/M12 | 同一直接模块 `verify` 中执行公开 API/Controller/Feign 契约断言 | PASS - 公开站点 8 个方法保持 `/cms/open` 和 PUBLIC 边界 | `CmsControllerContractTest`、`CmsRemoteAdapterContractTest` |
| REQ-004 | M09/M10/M11 | 直接模块 `verify`；`mvn -f mango/pom.xml -pl ':mango-cms-api,:mango-cms-starter,:mango-cms-starter-remote' -am test`；检查 remote JAR 清单 | PASS - Remote 5 个测试通过；35 模块依赖链回归成功；12 个 Feign Bean 可装配；JAR 含 12 个 FeignClient、自动配置类及 imports | Surefire 报告、`CmsRemoteAutoConfigurationTest`、`CmsRemoteAdapterContractTest`、JAR 清单 |
| REQ-005 | M08/M09 | `node mango-pmo/tools/check-capability-docs.mjs --base origin/main`；`git diff --check` | PASS - 能力文档和公开文档索引一致，差异格式检查通过 | checker 输出与 Git diff |
| REQ-001 至 REQ-004 | M09 | CI 等价 partial 门禁：加入 `:mango-architecture-verification`，使用 `changed`、`requireFullReactor=false` 和 `no-new-violations` | PASS - dependency、ArchUnit、架构 PMD 均为 0；新增路径静态问题为 0，仅保留 26 条既有 CMS 基线 | `mango/target/mango-static-report.json` 与 Maven 输出 |
| REQ-001 至 REQ-004 | M10 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main`；`node mango-pmo/tools/audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | PASS - 4 个改动测试文件质量检查通过，mock 审计 block=0、warn=0 | checker 输出 |
| REQ-001 至 REQ-005 | M15 | PR required checks 和合并状态回读 | PENDING | GitHub PR |

## 7. 例外与剩余风险

- 当前未发现仓内 `CmsAdminApi` / `CmsSiteApi` 生产消费者；仓外消费者如直接编译依赖 `CmsAdminApi`，升级时需要改用对应能力 API。该迁移影响将在 README 和 PR 中显式说明。
- 不新增兼容门面，避免聚合接口成为长期第二套契约；这是 Issue #522 已确认的设计边界。
- 本次没有数据库、前端或业务状态变更，因此未执行浏览器 UI/E2E；Remote 验证覆盖 Feign 契约和 Spring 装配，不声明已完成跨进程真实网络联调。
