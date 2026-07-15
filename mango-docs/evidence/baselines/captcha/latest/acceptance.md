# Captcha 历史债务治理验收证据

## 1. 验收范围

- 模块：`mango-captcha-api`、`mango-captcha-core`、`mango-captcha-starter`、
  `mango-captcha-starter-remote`。
- 真实消费者：Auth 的 `AuthController`、`CaptchaInterceptor`，以及
  `mango-admin` 的登录、验证码组件页和 `@mango/common/api/captcha`。
- 契约：8 个公开 Captcha HTTP 能力、API/Controller/Feign 一一对应、参数校验、
  `R<T>` 响应、内部发送安全、验证码过期与一次性消费。
- 初始化：Captcha 运行时只使用 `IKvStore`，不拥有业务表、Flyway migration 或 demo 数据。
- 部署：隔离 Maven 仓库构建可执行 monolith，独立空白 MySQL，真实 `java -jar`、API 和 Chromium。

## 2. 治理前基线

| 项目 | 结果 |
|---|---|
| 测试 | Core 33/33、Starter 14/14，合计 47/47；Remote 没有真实 HTTP/契约测试 |
| 架构 | dependency 0、ArchUnit 71、PMD 55、blocking 126 |
| 直接静态 | Checkstyle 0、PMD 49、SpotBugs 0 |
| API/Feign | Java API、Controller 和 Feign 方法集合不对称；Feign 含默认 unwrap、额外 remote 方法和伪造聚合逻辑 |
| Service | 算法实现以业务 Service 命名；实现类保留 `Impl` 后缀；公开生成入口仍保留 SMS/EMAIL 无效分支 |
| 初始化 | 发布未被运行时使用的 `captcha_code` V1，但验证码实际只写 `IKvStore` |
| 消费链 | Auth Controller 会重复包装 `R`；拦截器没有完整处理失败 `R` 和本地业务异常 |

## 3. 修复结果与兼容边界

| 债务类型 | 修复结果 | 兼容边界 |
|---|---|---|
| API/Controller/Feign | 三层固定为 `getTypes`、四种 generate、`verifyBehavior`、`verify`、`send` 共 8 个方法；Feign 只继承 API 并声明等价 HTTP mapping | 既有 HTTP verb/path、请求绑定、JSON 字段、错误码和 TTL 默认值不变 |
| 校验 | Bean Validation 只声明在 API；Controller 和 Feign 不重复 `@Valid`，避免覆盖方法约束冲突 | 空 key 真实 HTTP 返回 400 和原校验消息 |
| 内部安全 | `/captcha/send` 在 API 和 Controller 同时标记内部能力；Auth 继续通过 `/auth/captcha/send` 作为消费者入口 | 未签名访问内部入口返回 401，不把内部接口公开 |
| Service/算法 | 主实现为 `service/impl/CaptchaService`；算术、滑块、点选和行为能力重命名为 generator/engine，并由 starter 条件装配 | 验证算法、容差、TTL、短信/邮件 provider 顺序和成功语义不变 |
| Java model | `BehaviorCaptchaVerifyResult` 收敛为 `BehaviorCaptchaVerifyResponse`；类型列表使用 `CaptchaTypesResponse`；错误码进入 `enums` | 属于仓内 Java 结构契约调整；HTTP JSON 字段保持现有结构 |
| 一次性消费 | 校验成功后删除 KV；不存在/过期、失败和重放均返回 Captcha 业务错误 | 首次正确答案成功，第二次同 key 被业务码 `2409` 拒绝 |
| Auth 消费 | Auth Controller 直接返回 Captcha API 的 `R`；拦截器显式处理 null、失败 `R`、false 和 `BizException` | 不再产生双层响应包装，原认证入口不变 |
| 数据边界 | 删除未接入运行时的 `V1__init_captcha.sql`，并移除 monolith 过时的 Captcha Flyway 模块声明 | Captcha 不建表；正式/demo 数据分层政策不受影响 |

## 4. 自动化验证

| 层级 | 命令/入口 | 结果 | 结论 |
|---|---|---|---|
| Captcha 单元/集成 | `mvn -Dmaven.repo.local=/Users/hardy/Work/mango-captcha-debt/.mango/m2/repository -f mango/pom.xml -pl :mango-captcha-api,:mango-captcha-core,:mango-captcha-starter,:mango-captcha-starter-remote test` | Core 32、Starter 15、Remote 3；合计 50/50，failure/error/skip 均为 0 | PASS |
| Auth 消费链 | `mvn -Dmaven.repo.local=/Users/hardy/Work/mango-captcha-debt/.mango/m2/repository -f mango/pom.xml -pl :mango-auth-starter -Dtest='AuthSecurityE2ETest,CaptchaInterceptorTest' -Dsurefire.failIfNoSpecifiedTests=false test` | MockMvc Auth 入口 9、真实拦截器分支 4；合计 13/13 | PASS |
| Feign 真实 HTTP | `CaptchaRemoteHttpIntegrationTest` | Spring 创建真实 Feign bean，JDK HTTP server 收到全部 8 个方法的准确 verb/path/body；内部调用带 HMAC header | PASS |
| 契约对称 | `CaptchaContractParityTest` | API/Controller/Feign 方法签名、mapping、binding、校验归属和内部 send 注解完全一致 | PASS |
| 初始化边界 | `CaptchaInitializationBoundaryTest` | 发布 classpath 不含 `db/migration/captcha/V1__init_captcha.sql` | PASS |
| 架构 | 四子模块加 `mango-architecture-verification` 的 partial reactor full mode | dependency=0、ArchUnit=0、PMD=0、blocking=0 | PASS |
| 静态 | 四子模块 `checkstyle:check pmd:check spotbugs:check` | Checkstyle/PMD 均为 0；SpotBugs API=0、Core=0、Starter=0、Remote=0 | PASS |
| 测试质量 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main` | 11 个新增/变更测试资产 | PASS |
| Mock 审计 | `node mango-pmo/tools/audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | block=0、warn=0 | PASS |

Mockito 仅隔离外部 KV/provider 等协作者；Spring Controller、Feign 组装、Auth 拦截器、真实 HTTP、
MySQL、最终可执行 JAR 和浏览器链路均有独立非 Mock 证据。

## 5. Fresh MySQL、发布物和真实 API

| 项目 | 结果 |
|---|---|
| 工作区 | slot 192；后端 `18192`；前端 `30192`；数据库 `mango_dev_mango_captcha_debt_192` |
| 空库前置 | 最终启动前 drop/create，表数量为 0 |
| 启动方式 | 通过隔离仓库 `clean package` 生成 executable jar，再执行 `java -jar`；未使用 `spring-boot:run` |
| 健康 | `Started MangoMonolithApplication in 19.874 seconds`；`/actuator/health` HTTP 200/UP，MySQL UP |
| 进程 | `lsof` 确认运行进程打开全局 `~/.m2` 文件数量为 0 |
| 数据库 | 最终 220 张表；`captcha_code=0`；`flyway_schema_history_captcha=0` |
| 正式/demo 边界 | 默认启动不安装 demo 角色；Chromium 验收显式使用 `--mango.resource.registry.demo-enabled=true`，形成 4 个 demo role 和 4 个 subject-role 绑定 |
| API | `/captcha/types` 和 `/captcha/arithmetic` HTTP 200；类型 6 种；PNG data URL、key 和 TTL 正确 |
| 校验 | 正确答案首次 `data=true`；重放 `success=false/code=2409`；空请求 HTTP 400 |
| 安全 | 未签名 POST `/captcha/send` 返回 HTTP 401 |

最终 executable jar 的 `BOOT-INF/lib` 与隔离 Maven 仓库 SHA-256 对比：

| 产物 | SHA-256 | 结果 |
|---|---|---|
| `mango-captcha-api` | `256a54549c1c10ed094ce0c19ab57e62d72f6ed3a81b7ac5eac628056af3d702` | MATCH |
| `mango-captcha-core` | `3a5b3a6a08f05934ee27b4c3f6913621d046eed7b20e9e953ea69b843d2f585c` | MATCH |
| `mango-captcha-starter` | `9942adbac710a513d4e0ea277fccd3d85e837ecd33a8888872ee1ed59f478ac8` | MATCH |
| `mango-captcha-starter-remote` | 单体不依赖 Remote | N/A；由独立 Spring/Feign HTTP 测试验证 |

## 6. Chromium 端到端验收

命令：

```bash
PLAYWRIGHT_USE_EXTERNAL_WEBSERVER=true \
PLAYWRIGHT_BASE_URL=http://127.0.0.1:30192 \
PLAYWRIGHT_API_BASE_URL=http://127.0.0.1:18192 \
pnpm -F mango-admin exec playwright test \
  e2e/specs/captcha.spec.ts \
  --config playwright.config.ts \
  --project=chromium --reporter=line
```

结果为 1/1 通过，耗时 2.4 秒。用例使用真实 `admin/admin123` demo 账号登录，进入
`/#/components/captcha`，验证类型 API、算术验证码首次加载和刷新、PNG 图片渲染、滑块生成响应和页面提示；
没有路由或业务响应 mock。用例注册并最终断言 console error、pageerror、requestfailed 和非预期 HTTP >= 400
全部为空；成功截图由同一 spec 自动写入：[captcha-ui-success.png](./captcha-ui-success.png)，不依赖手工截图。

首轮 E2E 的 API 登录和 Captcha 类型请求已成功，但默认正式启动没有 demo 管理员角色，菜单返回空数组，
路由守卫按预期退回登录页。修复方式是按现行规范显式打开 demo Resource 开关并重新从空库启动，
没有手工写库，也没有把角色数据塞回 Flyway；同一用例随后通过。

## 7. 未验证项和风险

| 项目 | 原因 | 结论 |
|---|---|---|
| 短信/邮件第三方真实发送 | 当前 monolith 未配置外部 provider 凭据；第三方不属于 Captcha 内部逻辑 | provider 成功/失败分支由单元与 Auth MockMvc 覆盖；不宣称真实短信/邮件送达 |
| Remote 在 monolith 中运行 | 单体使用本地 Starter，不依赖 Remote | N/A；Remote 由独立 Spring Feign bean 和真实 HTTP server 验证 |
| 非 Captcha 全仓模块 | 用户要求不重复全仓检查 | 仅证明 Captcha、Auth 真实消费者及 monolith/UI 入口，不外推其它模块 |

## 8. 业务开发交接

Captcha 后续变更必须同时保持 8 方法 API/Controller/Feign parity，参数约束继续由 API 单点声明，
`/captcha/send` 保持内部签名保护。验证码存储只通过 `IKvStore`；除非未来出现真实持久化业务模型，
不得重新创建 Captcha Flyway 模块或空 history。新增验证码类型时必须同步扩展 Core、Controller、Feign、
真实 HTTP 契约测试和 Chromium 组件回归。
