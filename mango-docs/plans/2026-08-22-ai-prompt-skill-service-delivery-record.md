# AI Prompt、Skill 与服务配置交付记录

## 目标与范围

在现有 `AI 管理` 下新增三类真实租户级配置：提示词模板、Skill/工具、AI 服务定义。所有数据通过 Mango 持久化、权限和租户链路提供，页面不得使用最终 mock 或固定静态数据。

## 不处理范围

- 不新增模型供应商或模型运行时适配器。
- 不在本次实现五要素识别等具体业务服务执行器。
- 不修改 Spring Boot 主版本。
- 不保留旧 AI 配置入口、旧 Provider 或 fallback 分支。

## 技术决定

- 持久化使用 `ai_prompt_template`、`ai_skill`、`ai_tool`、`ai_service_definition` 四张模块 Flyway 表，均继承租户和审计字段。
- API 使用 `Create/UpdateXxxCommand`、`XxxVO` 和独立 `XxxApi` 契约；密钥、文件地址和完整调用内容不进入这些表。
- Prompt 发布是显式状态变更；Skill 与工具、AI 服务只引用已存在的配置 ID，删除时校验引用关系。
- HTTP 接口通过 `@ApiAccess(PERMISSION)` 声明权限；菜单资源同步新增页面和按钮权限。
- 前端页面从真实 API 加载，覆盖列表、空态、错误态、表单校验和权限指令。

## 实施清单

- [x] API 枚举、Command、VO、Api 契约
- [x] Flyway migration、Entity、Mapper、Service、Controller
- [x] AUTH_MENU 页面与权限资源
- [x] `@mango/ai-api` API 类型和 `@mango/ai` 三个管理页面
- [x] 单元、API 合同、前端包和启动/健康验证
- [x] 搜索旧入口、Provider、fallback 和无用兼容代码

## 验收映射

| 验收项             | 验证方式                                            |
| ------------------ | --------------------------------------------------- |
| 四类配置真实持久化 | Maven 单测、HTTP API、数据库回读                    |
| Prompt 发布状态    | Service 单测和页面状态回显                          |
| 引用关系不能删除   | Service/API 负例测试                                |
| 权限和租户隔离     | Controller 合同及未授权 HTTP 验证                   |
| 页面真实交互       | Chromium 页面、空态、错误态、表单和 Network/Console |
| 废弃代码清理       | `rg` 旧入口、fallback、旧 Provider 标识             |

## 实际验证记录

- 后端：`mvn -f mango/pom.xml -pl :mango-ai-core -am -Drevision=1.0.0-mango-077-SNAPSHOT test`，AI Core 测试通过。
- 后端：`mvn -f mango/pom.xml -pl :mango-ai-starter -am -Drevision=1.0.0-mango-077-SNAPSHOT test`，AI Starter 测试通过。
- 数据库：隔离数据库 `mango_dev_mango_ai_spring_ai_foundation_077` 的 `flyway_schema_history_ai` 已通过正常服务启动执行到版本 `4`；四张配置表存在，四个废弃同步列和旧索引已删除；未删除或重置数据库。
- 启动：运行时通过仓库内 Mango CLI 以 Mango `runtime` 启动，`GET /actuator/health` 返回 `200`，bootstrap generation `5`，后端 `18077`、前端 `30077`。
- 前端：`@mango/ai-api` 1/1、`@mango/ai` 4/4 单测和两个包构建通过；Prettier、前端边界、架构、admin 样式治理检查通过。
- 未授权：`GET /ai/models/provider-types` 返回 `401`，权限链路未绕过。
- 清理扫描：AI 模块和 AI 前端包未发现旧 Provider、旧配置、旧 ChatRequest、fallback 分支或 deprecated 标识；404 契约测试仅用于证明旧 `/ai/sse` 入口不存在。
- 浏览器：真实管理员登录态已完成 8 个内置供应商检查，供应商新增/编辑/删除、模型 CRUD、Prompt CRUD 与发布、MCP 工具/Skill 绑定 CRUD、AI 服务定义 CRUD，以及必填和 JSON 对象校验；除单独断言的废弃同步 URL 404 外，AI 页面 console error、pageerror 和失败 Network 均为 0。
- 清理：每次验收使用唯一 `qa_ai_<timestamp>` 前缀，结束后通过页面或正式 API 删除；数据库六类表回读残留均为 0。
- 视觉：模型页供应商/模型双栏、表格操作区、状态标签和服务页空态布局通过走查；页面切换时 HttpClient 主动取消不再误报 `Request aborted`。

## 剩余风险

AI 服务执行器、审计与用量查询页面仍属于后续阶段；本次只交付其可运营配置定义，不宣称业务执行已经完成。
