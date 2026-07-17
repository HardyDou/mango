# Mango Template 历史债务验收证据

## 1. 范围与基准

- 模块：`mango-platform/mango-template`
- 基准：`origin/main` commit `8f01d1505`
- 分支：`refactor/template-debt`
- 目标：在保持 HTTP 路径、JSON 线格式、权限码、菜单位置和渲染语义不变的前提下，收敛 API、Controller、Service、Entity、Mapper、Flyway 与远程适配边界。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:30006`
- 单体后端：`http://127.0.0.1:18006`
- 微服务后端：Template `http://127.0.0.1:18626`，Domain `http://127.0.0.1:18619`
- 数据库：`mango_dev_mango_template_debt_006`
- 测试租户/账号：租户 `1`（芒果集团），`admin`；不记录密码与 token
- 浏览器：Chromium

## 3. 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| TASK-TPL-001 | TC-001 | 平台能力 / 模板管理 | 分类、模板、版本、渲染与记录闭环 | `E2E_TPL_<timestamp>`，租户 1 | 创建分类和模板成功；V1/V2 发布；历史版本生效；正文与记录一致；清理成功 | 菜单位置、表格、编辑页、历史版本页、预览页、记录抽屉均按真实数据回显 | 无未解释 console error；关键业务 network 无未解释 4xx/5xx | Playwright Chromium 命令结果：13.2 秒，1/1 PASS；长期脚本见 `mango-ui/apps/mango-admin/e2e/specs/template-management.spec.ts` | PASS |
| TASK-TPL-002 | TC-002 | `/template/templates` | 真实 HTTP 校验 | 缺少 `domainCode` 的创建请求 | HTTP 400；消息为“业务域编码不能为空”；无写库副作用 | 非 UI 场景 | 请求按预期返回 400，无系统异常 500 | Spring HTTP 测试与双 JVM HTTP 响应摘要见本文件第 5、7 节 | PASS |
| TASK-TPL-003 | TC-003 | Template→Domain 双 JVM | 创建、发布、FreeMarker 渲染、详情、删除 | `MICRO_E2E_<timestamp>`，合同号 `HT-2026-001` | Feign 调用真实 Domain；渲染正文为“微服务合同：HT-2026-001”；删除成功 | 非 UI 场景 | 两服务健康 UP；业务请求无未解释 4xx/5xx | 两进程日志摘要及 HTTP 响应见本文件第 7 节 | PASS |
| TASK-TPL-004 | TC-004 | Fresh DB / 最终 JAR | DDL、canonical 字段和 migration 清单 | 新库 `mango_dev_mango_template_debt_006` | 四张表字段完整；Flyway 只有成功 V1；JAR 不含 V2 | demo 开启后菜单位于“平台能力” | 启动健康 UP；无 Template migration/schema 错误 | SQL 查询摘要与 `jar tf` 结果见本文件第 6、8 节 | PASS |

## 4. 改前基线

| 项目 | 结果 |
|---|---|
| 目标模块有效测试 | 9/9 通过 |
| 定向架构检查 | 260 个 blocking issue |
| 主要债务 | Core 依赖 `R`、Service/Entity/Mapper 非 canonical、创建修改命令混用、动态 JSON 裸 Map、V1/V2 可重整、缺少真实 HTTP/数据库主链路证明 |

改前测试绿灯仅证明已有局部行为；旧测试没有覆盖最终 MySQL DDL、完整 HTTP 参数校验、JAR migration 清单及真实跨 JVM Domain 调用。

## 5. 改后自动化

| 层级 | 命令/场景 | 结果 |
|---|---|---|
| 单元与集成 | Maven 选择 `mango-template-api/core/starter/starter-remote` 执行 `clean test` | 16/16 通过；core 13、starter 3 |
| 数据库主链路 | H2 + MyBatis + 真实 Mapper + 真实 FreeMarker create→publish→render | 通过；未 Mock Mapper/数据库/渲染引擎 |
| HTTP 契约 | Spring HTTP 创建、修改、查询及非法参数 | 通过；缺少 `domainCode` 返回 HTTP 400 和稳定消息 |
| JSON 兼容 | 动态变量标量、嵌套对象、数组序列化/反序列化 | wire format 保持普通 JSON object |
| 架构 | 目标 Reactor `mango:architecture` full mode | dependency=0、archunit=0、pmd=0、blocking=0 |
| 前端构建 | `@mango/template` 与 `mango-admin-template-app` | 均通过 |

## 6. Fresh DB 与单体 E2E

- 数据库：一次性 MySQL `mango_dev_mango_template_debt_006`。
- demo 关闭启动：DDL 和正式资源成功，演示角色为 0，符合生产初始化边界。
- demo 开启启动：显式 `MANGO_RESOURCE_REGISTRY_DEMO_ENABLED=true`，演示管理员角色与菜单完成同步。
- Flyway：`flyway_schema_history_template` 只有 baseline 记录和成功的 `V1 init template`；无 V2。
- Schema：`template`、`template_category`、`template_version`、`template_render_record` 均包含 canonical tenant/org/audit 字段，无 `create_time/update_time` 遗留。
- 浏览器 E2E：Chromium 1/1 通过（13.2 秒）。从“平台能力 → 模板管理”进入，完成分类创建、模板创建/编辑、变量提取、V1/V2 发布、历史版本生效、真实渲染、渲染记录详情和清理。

## 7. 微服务双进程 E2E

真实启动两个独立 JVM：

- `mango-domain-capability-app`：18619
- `mango-template-capability-app`：18626

Template 通过真实 Feign 调用 Domain，完成创建→发布 TEXT/FreeMarker 版本→渲染 `${contractNo}`→详情→删除，最终正文为 `微服务合同：HT-2026-001`。非法创建请求返回 HTTP 400。测试没有 Mock Domain、Feign、Mapper 或数据库。

本地直连绕过网关，因此按内部传播协议传递 `X-Mango-Tenant-Id`；仅传浏览器 `X-Tenant-Id` 时，下游租户隔离器会按设计拒绝缺失上下文的 SQL，未通过默认租户或关闭检查绕过。

## 8. 最终构件

- 四个 Template artifact 已执行定向 `clean install`。
- `mango-template-core` JAR 的 `db/migration/template/` 仅包含 `V1__init_template.sql`。
- 源码、`target/classes` 与最终 JAR 均不包含已删除 V2。
- Flyway 只含 DDL；正式菜单资源继续位于模块自己的 `META-INF/mango/resources/`，父菜单仍为最新“平台能力”对应的 `data`。

## 9. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 多浏览器兼容 | 本次按日常策略只运行 Chromium | 不影响已证明的主浏览器业务闭环；Firefox/WebKit 留给夜间或发布前矩阵 | 进入发布批次时按影响范围执行 | 不适用，本次无例外放行 |

## 10. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| 业务开发者 | Template 历史债务基线、JSON 兼容、Fresh DB、单体与微服务验收结论 | 本文件及模块 README | 本文件第 5 至 8 节记录的定向命令/入口 | 使用独立工作区库、租户 1、用例唯一前缀并清理 | 任一层失败均按对应证据定位，不以 Mock 或默认租户绕过 | DONE |

## 11. 结论

改前冻结的公开路径、JSON、权限、菜单与渲染行为保持不变；历史债务由 260 个 blocking issue 收敛为 0。单元/集成、真实 HTTP、Fresh DB、浏览器 E2E、双 JVM 微服务链路及最终 JAR 均已验证。
