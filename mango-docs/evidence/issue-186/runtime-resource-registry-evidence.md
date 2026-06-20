# Issue 186 Resource Registry 运行态验收证据

## 1. 验收范围

- 页面：登录页、验证码组件页、管理后台菜单、通知中心、审批中心、工作流管理与办理页面。
- 接口：Resource Registry 同步、AUTH_MENU/API_RESOURCE 消费、验证码公开接口、workflow 权限接口。
- 权限：`/captcha/**` PUBLIC、Gateway route 开关、workflow PERMISSION 权限码与角色菜单授权闭环。
- 数据：clean DB 重建后的 Flyway、`resource_registry`、`authorization_menu`、`authorization_api_resource`、`resource_sync_log`。
- 部署形态：本地单体运行态，后端 `http://127.0.0.1:18558`，前端 `http://127.0.0.1:8248`。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:8248`
- 后端地址：`http://127.0.0.1:18558`
- 数据库或租户：`mango_dev_02e197`，租户 `default / 芒果集团`
- 测试账号：`admin`，未记录密码或 token
- 浏览器：Playwright Chromium
- 执行基线提交：`31467758b` 后续工作区修复后执行，最终提交以本证据提交记录为准
- 本地库备份：`.runtime/issue-186/mango_dev_02e197-20260620-102319.sql`，未提交

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| ISSUE-186-P1-1 | `GatewayRouteResourceSyncAutoConfiguration` | `mango.authorization.resource-sync.gateway.enabled=false` 可单独关闭 Gateway route 资源采集 | 单测覆盖 `ApplicationContextRunner` 开关场景 | `GatewayRouteResourceProvider` 在 gateway 开关关闭时不装配，总开关仍控制整体 resource sync | 不涉及页面，属于自动配置装配行为 | Maven 单测通过，无网络异常 | `mvn -f mango/pom.xml -pl mango-platform/mango-authorization/mango-authorization-resource-sync-starter -am -Dtest=GatewayRouteResourceSyncRunnerTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS |
| ISSUE-186-P1-2 | `/captcha/**` | 验证码登录前公开链路不被 Gateway route 默认 LOGIN 破坏 | clean DB，`/captcha/types`、`/captcha/arithmetic` 等 8 条资源 | `authorization_api_resource` 中 8 条 `/captcha/*` 记录均为 `access_mode=PUBLIC,status=1,deleted=0` | 登录页可加载，验证码组件可刷新算术验证码与滑块验证码 | Playwright `captcha.spec.ts` 通过；接口请求返回真实后端验证码数据 | `.runtime/issue-186/evidence/runtime-db-assertions.txt`；Playwright `23 passed` 结果 | PASS |
| ISSUE-186-P1-3 | Resource Registry 静态门禁 | 最新修复后重跑资源边界检查 | `resource-registry`、`remote-adapter`、`module-menu` | 三个 `mango:check` report 均为 PASS，报告文件无失败项 | 不涉及页面，属于静态门禁 | 命令执行成功，无 checker fail | `mango-docs/evidence/issue-186/resource-registry-check-current.json`、`remote-adapter-check-current.json`、`module-menu-check-current.json` | PASS |
| ISSUE-186-P1-4 | clean DB 启动与资源注入 | 删除重建数据库后启动单体并完成 Resource Registry 同步 | DB `mango_dev_02e197`，服务 `mango-monolith-app` | `resource_registry=769`，`resource_sync_log` 为 `SUCCESS/CREATE=769`，`AUTH_MENU=13`，`API_RESOURCE=567` | 管理后台菜单按后端菜单树渲染，系统管理/审批中心/平台能力/通知中心均可见 | 后端 health 为 `UP` 且前端首页响应含管理后台 HTML；菜单 E2E 捕获用户菜单树请求并校验结构 | `.runtime/issue-186/evidence/runtime-db-assertions.txt`；`scripts/dev-workspace.sh status` 显示前后端运行 | PASS |
| ISSUE-186-P1-4-AUTH | 授权菜单与权限 | AUTH_MENU handler 消费后菜单无孤儿且通知/工作流入口正常 | clean DB 菜单 361 条 | `authorization_menu` active 361，孤儿菜单 0；workflow 菜单 45 条；发送任务页面查询进入授权处理分支并返回任务分页数据 | 右上角消息入口可见；通知中心菜单含我的消息、发送任务、渠道配置、发送记录、失败重试 | `menu-navigation.spec.ts` 两个租户场景通过，network 捕获 `/api/notice/tasks` 查询且页面标题为“发送任务” | Playwright 串行复跑 `23 passed (2.1m)` | PASS |
| ISSUE-186-P1-4-WF | workflow 403 回归 | Resource 菜单权限码与 API_RESOURCE PERMISSION 权限闭环 | `workflow:definition:list`、`workflow:process:start`、`workflow:business-apply:list` | 修复后真实接口 `/workflow/categories/list` 返回 COMMON 分类数据，`/workflow/definitions/page` 返回定义分页，`/workflow/business-applies/page` 返回申请分页 | 工作流菜单、流程定义、发起流程、待办、已办、业务示例页面主流程均通过 | 首轮 E2E 暴露 403；修复 `SubjectAuthorityServiceImpl` 后串行 E2E 23/23 通过 | `.runtime/issue-186/evidence/workflow-*-response.json`；`SubjectAuthorityServiceImplTest` 2/2 通过 | PASS |
| ISSUE-186-P1-5 | 推荐单体接入链路 | `mango-admin-starter` 自带本地 Resource Registry runtime | `mango-admin-starter`、`mango-monolith-app` POM | 单体推荐依赖闭合，`mango-monolith-app` 不再重复声明 `mango-resource-starter` / `mango-resource-sync-starter` | 不涉及页面，属于部署装配行为 | Maven 编译通过，无依赖解析错误 | `mvn -f mango/pom.xml -pl :mango-admin-starter,:mango-monolith-app -am -DskipTests compile` | PASS |
| ISSUE-186-P1-6 | Gateway route 资源注册模式 | `gateway.mode=read` 不注册 Resource Registry provider，`write` 才注册 | `GatewayRouteResourceSyncAutoConfiguration` | `read` 模式只保留 discoverer；`write` 模式装配 `GatewayRouteResourceProvider`；`gateway.enabled=false` 仍完全关闭 | 不涉及页面，属于自动配置装配行为 | 单测 7/7 通过 | `mvn -f mango/pom.xml -pl mango-platform/mango-authorization/mango-authorization-resource-sync-starter -am -Dtest=GatewayRouteResourceSyncRunnerTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS |
| ISSUE-186-P1-7 | Resource 管理权限入口 | `system:resource:*` API 权限有可授权菜单项 | `authorization-common-menu.json` | 资源管理 API 权限挂到系统菜单管理授权树，声明版本从 2 升到 3，不新增不存在的页面入口 | 菜单页面仍使用现有 `system/menu/index` | `module-menu` 静态检查通过 | `mvn -f mango/pom.xml mango:check -Drule=module-menu -Doutput=json -DreportFile=mango-docs/evidence/issue-186/module-menu-check-current.json` | PASS |
| ISSUE-186-P2-1 | moduleCodes-only 远程注册 | `declarations=null` 按空声明处理，不在同步阶段 NPE | `syncRemote(app, service, moduleCodes, null)` | 允许缺失禁用语义，仍要求 declarations 或 moduleCodes 至少一个非空 | 不涉及页面，属于远程注册契约 | Resource core 集成测试 14/14 通过 | `mvn -f mango/pom.xml -pl mango-platform/mango-resource/mango-resource-core -Dtest=ResourceRegistrySyncServiceIntegrationTest test` | PASS |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 登录/验证码 | `/#/login`、`/#/components/captcha` | admin 真实后端登录成功 | 验证码类型和图片刷新返回真实数据 | 登录表单、移动端布局、loading 和错误提示通过 E2E 断言 | Playwright list 输出 `login.spec.ts`、`captcha.spec.ts` 全通过 | PASS |
| 菜单/通知 | 管理后台主框架 | 芒果集团/A 公司菜单树按后端返回渲染 | 通知中心发送任务接口有权限 | 右上角 `.notice-bell` 可见，菜单图标逐项断言 | Playwright list 输出 `menu-navigation.spec.ts` 全通过 | PASS |
| 工作流 | 审批中心 | 流程定义、模板、发起流程、待办审批闭环 | 业务示例申请/驳回/再申请/历史查看闭环 | 三步设计工作台、发起弹窗、成员选择器和上传组件通过断言 | Playwright list 输出 `workflow-management.spec.ts` 全通过 | PASS |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 微服务/Nacos 部署形态 | 本轮 P1 评审问题只要求新增修复后的最终运行态证据；当前证据覆盖本地单体 clean DB 和现有前端 E2E | 不影响本次 P1-1 到 P1-4 的修复判断，但微服务全矩阵仍需独立回归 | 保留 issue-186 后续微服务验收计划，不在本提交伪造微服务证据 | 未请求例外确认 |
