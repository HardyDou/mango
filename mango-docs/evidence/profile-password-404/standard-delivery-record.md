# 个人中心修改密码接口 404 修复交付记录

## 1. 元数据

- 任务 ID：profile-password-404
- 交付模式：STANDARD
- 需求影响：L2 - 个人中心公开改密接口不可用，涉及认证安全边界
- 方案风险：L2 - 跨前后端公共契约、密码持久化和安全校验链路
- 最终风险：L2
- 工作区决策：REUSE（fix/profile-password-404）
- 验证环境：Java 21.0.10、Node.js 22.23.1、pnpm 11.14.0；后端集成测试使用隔离 H2 数据库 `identity_user_service` / `identity_user_security_service`，测试租户为 `1`；真实 HTTP 使用独立 MySQL 工作区数据库 `mango_dev_mango_fix_profile_password_404_005`、后端 `http://127.0.0.1:18005`，generation 1 bootstrap/runtime 已通过；未记录账号密码或 token。

## 2. 目标与范围

- 目标：使个人中心修改密码请求命中 Identity 当前用户改密接口。
- 成功条件：前端调用 `PUT /identity/me/password`；后端校验旧密码和新密码策略后更新当前用户密码；旧路径不再被调用。
- 处理范围：Identity API/Service/Controller/Feign 契约、认证前端 API 调用、相关测试和本记录。
- 不处理范围：首次登录强制改密、管理员重置密码、数据库结构、菜单和权限模型。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-001 | 个人中心修改密码 | 已登录且属于当前租户，提供旧密码和符合策略的新密码 | 请求 `PUT /identity/me/password` 并更新当前账号密码 | 旧密码错误返回当前密码错误；新密码不符合策略拒绝 | 前端 API 单测和后端服务集成测试通过 |
| AC-002 | Identity HTTP/Feign 适配器 | `ChangeCurrentUserPasswordCommand` JSON body | Controller 与 Feign 暴露相同方法、verb 和路径 | 契约不一致时架构/适配器检查失败 | 契约测试通过 |
| AC-003 | 真实 HTTP 登录与改密链路 | 独立工作区数据库中的 bootstrap 管理员账号，先登录取得会话 | 错误旧密码被拒绝，正确旧密码修改成功；旧密码不能再登录，新密码可以登录 | 非 404、错误密码明确失败 | 真实服务 HTTP 链路通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | AC-001 | 新增 `PUT /identity/me/password`；仅使用当前登录上下文，不接收用户 ID；成功后更新密码时间并清理强制改密和登录锁定状态 | Identity API、core、starter、starter-remote、auth API | 回退本次提交 |
| TD-002 | AC-002 | 前端改用 Identity 当前用户路径，不保留旧 `/user/password` fallback | `mango-ui/packages/auth`、admin API | 回退本次提交 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| TASK-001 | TD-001 | 1 | Identity command/API/service/controller/Feign | 编译、契约和服务测试通过 |
| TASK-002 | TD-002 | 2 | 前端 auth/admin API、README | 请求路径断言为新接口 |
| TASK-003 | TD-001/TD-002 | 3 | 测试与本记录 | 结果和剩余风险已记录 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-001 | 后端集成 + 前端 API 单测 | `mvn -f mango/pom.xml -pl :mango-identity-api,:mango-identity-core,:mango-identity-starter-remote,:mango-identity-starter verify`; `PATH=.../v22.23.1/bin:$PATH pnpm --filter @mango/auth test --run src/api/__tests__/identity.spec.ts` | PASS（后端 52 个 core 测试通过；前端 4/4 通过） | `mango/mango-platform/mango-identity/mango-identity-core/target/surefire-reports/`；终端输出 |
| AC-002 | Identity adapter contract test + Maven verify + 前端生产构建 | 同上；`PATH=.../v22.23.1/bin:$PATH pnpm --filter @mango/auth build`; `PATH=.../v22.23.1/bin:$PATH pnpm --filter mango-admin build` | PASS（Identity starter 19 个测试通过；两个前端构建通过） | `mango/mango-platform/mango-identity/mango-identity-starter/target/surefire-reports/`；终端输出 |
| AC-003 | 真实 HTTP 接口链路 | 独立库执行 `bootstrap apply --strategy=cold`（environment `local-mango_005-mango-monolith-app`、generation 1、revision `mango-005`），再以相同 release tuple 启动 `runtime`；使用 `curl` 调用 `POST /auth/login`、`PUT /identity/me/password` 并再次登录 | PASS：`/actuator/health` HTTP 200 且 `UP`；初始登录成功；改密请求错误旧密码 HTTP 200、业务码 1400（当前密码错误）；正确旧密码 HTTP 200、业务码 200、data=true；旧密码登录业务码 1400；新密码登录业务码 200。接口未出现 404。 | `.runtime/profile-password-404/bootstrap.log`（bootstrap `FINALIZED`、generation 1）；`.runtime/profile-password-404/runtime.log`（runtime receipt accepted）；本次终端 curl 摘要 |

## 7. 例外与剩余风险

- 尚未进行真实浏览器和部署环境联调；本次已完成真实后端 HTTP 联调，不能据此声明浏览器验收通过。
- 该接口新增后，需随 Identity Maven 物料和 `@mango/auth` 前端包一起发布，消费者需成组升级。
