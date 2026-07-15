# Mango Auth 历史债务治理验收证据

## 1. 验收范围

- 页面：Admin Shell 登录页、登录后首页、用户菜单退出入口。
- 接口：`AuthApi`、`AuthController`、`AuthFeignClient` 的 10 个认证方法；登录、机构选择、刷新、信息、校验、退出、验证码、企微和强制改密 HTTP 入口。
- 权限：真实 `admin` 成员以 `ROLE_ADMIN` 登录，菜单与权限来自 Authorization demo Resource，不使用超级管理员测试绕过。
- 数据：Auth 不拥有 Flyway；最终应用从全新 MySQL 数据库启动，正式资源默认加载，验收时显式启用 demo Resource。
- 部署形态：当前 Auth 生产者与真实 monolith 消费者同 Maven reactor；真实嵌入式 Tomcat；单体管理端 Chromium。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:30192`
- 后端地址：`http://127.0.0.1:18192`
- 数据库或租户：`mango_dev_mango_auth_debt_192`；租户 1（芒果集团）；220 张表；20 张模块 Flyway history 表全部无失败记录。
- 测试账号：公开 demo 账号 `admin`；密码仅来自本地 demo 环境或 `MANGO_E2E_ADMIN_PASSWORD`，证据不记录 token。
- 浏览器：Playwright Chromium / Chrome；用例重复执行 3 次。

## 3. 治理前基线与已证明缺陷

| 项目 | 治理前结果 |
|---|---|
| Auth 自有自动化 | 41 条；Core 11、Starter 30，API/Remote 为 0 |
| 架构债务 | 125 条：API 2、Core 39、Starter 84；dependency 2、ArchUnit 48、PMD 75 |
| 真实入口 | 原 `AuthSecurityE2ETest` 实际是 MockMvc 并替换大量核心协作者，没有真实 Tomcat 或浏览器证据 |
| 签名 JSON | HandlerInterceptor 调用 `getReader()` 后 Controller 再读请求体，真实 POST 抛出 `Cannot call getInputStream() after getReader()` |
| 幂等失败 | 失败请求遗留 `PROCESSING`，调用方修正请求后仍无法重试；成功响应没有形成稳定缓存契约 |
| 页面退出 | 只清前端 store 并跳登录页，未调用 `/auth/logout`，HttpOnly `MANGO_TOKEN` 继续有效 |
| 行为漂移 | Service 重构时登录审计成功文案误写为英文 `success`，与原 `R.ok()` 的“操作成功”不一致 |

## 4. 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| AUTH-001 | TC-001 | 随机端口 Tomcat `/auth/login` | Controller 参数校验、Cookie、真实客户端信息 | 有效与空登录命令 | 有效请求进入 Service；空用户名/密码 HTTP 400 且 Service 未调用 | 无页面；以真实 HTTP 请求、响应头和 Service 调用边界验收 | 登录 2xx、校验失败 400 均符合断言，无额外 HTTP 错误 | `AuthHttpEntryFlowTest` | PASS |
| AUTH-002 | TC-002 | 签名 POST JSON | 防重放读取后请求体仍可供 Controller 读取 | HMAC 请求头与 JSON | Controller 收到完整 JSON，签名有效 | 无页面；以真实 Filter→Controller 请求体传递结果验收 | 签名请求 2xx，未出现二次读取异常 | `AuthHttpEntryFlowTest`、`AntiReplayRequestBodyFlowTest` | PASS |
| AUTH-003 | TC-003 | 带幂等键 POST | 成功缓存、处理中冲突、失败释放 | 三类请求状态 | 重复成功返回同一响应；处理中 409；失败后可重试 | 无页面；以真实 HTTP 重复请求的可见响应验收 | 成功 2xx、处理中 409、失败重试结果均符合契约 | `AuthHttpEntryFlowTest`、`AntiReplayFilterTest` | PASS |
| AUTH-004 | TC-004 | API/Controller/Feign | 10 方法契约完全一致 | 反射读取公开签名与 Mapping | 方法、verb、path、参数绑定一致 | 无页面；逐项检查 Java API 与 HTTP/Feign 适配签名 | 静态契约测试无缺失或重复 Mapping | `AuthApiContractTest`、`AuthAdapterContractTest` | PASS |
| AUTH-005 | TC-005 | `/#/login` → `/#/home` | 真实登录与刷新保持 | demo admin / 芒果集团 | 登录 200；`ROLE_ADMIN`；HttpOnly/Lax Cookie；刷新后菜单仍可见 | 首页主内容、顶部菜单、用户区正常 | 0 console error、0 pageerror、无 4xx/5xx | [首页截图](./auth-e2e-home.png)、`auth-session.spec.ts` | PASS |
| AUTH-006 | TC-006 | 用户菜单 → 退出登录 | 真实服务端撤销与浏览器清理 | 当前 access token | `/auth/logout` 200；Cookie 不存在；sessionStorage 清空；返回登录页 | 确认弹窗关闭，登录表单正常 | 0 console error、0 pageerror、无 4xx/5xx | [退出截图](./auth-e2e-logout.png)、`auth-session.spec.ts` | PASS |

## 5. 自动化和架构结果

| 层级 | 执行入口 | 结果 |
|---|---|---|
| Auth 单元/组件/入口流程 | 四个 Auth 子模块定向 `mvn test` | 49/49；Core 11、Starter 37、Remote 1；失败/错误/跳过均为 0 |
| Admin Shell 单元 | `pnpm exec vitest run` | 38/38；新增退出顺序成功/失败 2 条 |
| Admin Shell 生产构建 | `pnpm run build` | PASS；165 modules transformed |
| Chromium E2E | `auth-session.spec.ts --repeat-each=3 --workers=1` | 3/3 PASS；每次使用独立空 storage state 和真实后端 |
| Fresh MySQL | 空库启动后查询 schema/history | 220 张表；20 张 history 表；`success=0` 合计 0 |
| demo 边界 | 默认关闭，验收显式 `mango.resource.registry.demo-enabled=true` | 4 个 demo role、4 个 subject-role binding、293 个 role-menu binding |
| 当前生产者与消费者 | Auth 四模块和 `mango-monolith-app` 同 reactor `install` | 162 个实际消费者依赖模块 BUILD SUCCESS |
| 架构 | Auth 四模块 `mango:architecture` full mode | dependency=0、ArchUnit=0、PMD=0、blocking=0；125→0 |
| 测试质量 | `test-quality-check --base origin/main` | 9 个新增/变更测试资产，PASS |
| Mock 审计 | `audit-backend-test-mocks --changed-only` | block=0、warn=0；真实入口证据不依赖 MockMvc |

## 6. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| Auth + Authorization + Admin Shell | 登录/首页/退出 | 机构、账号、角色和菜单闭环 | 刷新保持、真实撤销和 Cookie 清理 | 页面无空白、404、无限 loading 或残留确认弹窗 | `auth-e2e-home.png`、`auth-e2e-logout.png`、Playwright trace | PASS |

## 7. 行为兼容结论

- 保持：公开 HTTP 路径、合法请求返回结构、token claim、JWT 有效期配置、Cookie 名称、登录机构选择、验证码、企微、首次改密、失败锁定、通知和审计能力。
- 修复：签名 JSON 请求体被消费、失败幂等无法重试、页面假退出、退出状态竞态 401、登录审计成功文案漂移。
- 边界：Controller 只做 HTTP 绑定和请求上下文补充；认证业务、审计、通知、验证码和企微逻辑由 Service 承担；Cookie 副作用由 ResponseAdvice 承担。
- 数据：Auth 不新增数据库或 seed；验收所需角色/绑定属于 Authorization demo Resource，默认不进入正式启动路径。

## 8. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 外部真实企业微信 OAuth | 需要真实第三方 CorpId/Secret 和回调域名 | 不证明第三方平台可用性；本地配置与绑定契约已由组件测试覆盖 | 由部署环境做渠道联调 | 不属于本次历史债务 E2E |

## 9. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| 业务开发者 | Auth API/HTTP/前端退出契约、配置、数据依赖和排障说明 | Auth README、Admin Shell README | 四模块 Maven 测试；`@p0 @mango-auth` Playwright | 正式环境不启用 demo；业务自行提供 identity/authorization 数据 | 登录成功但菜单空先检查成员角色绑定；退出必须调用后端 | READY |
