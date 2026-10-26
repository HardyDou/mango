# Issue #1120 验证码图形类型与算术难度优化标准交付记录

## 1. 元数据

- 任务 ID：Issue #1120
- 交付模式：STANDARD
- 需求影响：L2 - 影响登录和低风险表单使用的公共验证码接口与前端公共 API。
- 方案风险：L2 - 新增图形验证码生成器、配置解析、HTTP/Feign 契约和验证分支，存在公共接口兼容回归面。
- 最终风险：L2
- 工作区决策：CREATE - `/Users/hardy/mango-issue-1120`
- 评审选择：AGENT_ONLY；未指定外部同行评审，保留后续外部评审入口。

## 2. 目标与范围

- 目标：增加支持全局默认策略和会话级覆盖的统一图形验证码入口，支持字母数字验证码；默认级别为 `SIMPLE`，默认题型保持 `ARITHMETIC` 兼容现有算术场景。
- 成功条件：服务端通过 `mango.captcha.graphic.default-type`、`default-level` 和允许列表定义安全基线；会话请求只能在允许范围内选择题型和级别；`GET /captcha/graphic` 按最终会话策略生成并可通过统一校验；旧算术、滑块、点选、行为、短信和邮件接口保持可用。
- 处理范围：captcha API、core 生成与校验、starter 配置和 Controller、remote Feign 契约、Common 前端 API、使用说明和定向测试。
- 不处理范围：登录业务的验证码策略、验证码限流、存储实现迁移、Canvas 滑块安全性和其它验证码算法。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-001 | 业务后端；`GET /captcha/graphic` | 未传会话参数，使用全局默认策略 | 返回默认题型和 `SIMPLE` 级别的图形验证码及一次性 key | 无效 key、错误答案或过期答案返回验证码校验失败 | 默认级别为 `SIMPLE`；默认题型保持 `ARITHMETIC` |
| AC-002 | 业务后端；`GET /captcha/graphic` | 会话请求 `type=ALPHANUMERIC` 或 `type=ARITHMETIC`，可选 `level` | 在全局允许范围内按会话策略生成图形验证码 | 不允许的类型/级别被拒绝；低于全局安全级别的请求不得降级 | 题型和级别绑定到服务端 key，正确答案只能校验一次 |
| AC-003 | 算术图形验证码 | `level=SIMPLE` | 生成一位数单步加减题 | 错误答案返回验证码校验失败 | 题目适合 1 秒内口算 |
| AC-004 | 业务配置 | 配置默认值和允许列表 | 全局配置定义默认策略与会话可选边界 | 非法配置或不允许的请求不生效 | 不接受前端自定义字符集、操作数或运算符 |
| AC-005 | 远程消费者；Feign | 使用 `CaptchaApi` | Controller、Feign、API 方法和路径保持一对一 | 契约不一致时 parity 测试失败 | `/graphic` 在三端契约中一致 |
| AC-006 | 前端公共 API | 调用 `generateGraphic()` | 返回统一验证码响应并可提交 `/captcha/verify` | HTTP 错误由现有请求层传播 | TypeScript 类型和路径与后端一致 |
| AC-007 | 前端组件 | 使用 `GraphicCaptcha` 或 `CaptchaSelector` 的统一图形页签 | 调用 `/captcha/graphic` 并提交 `GRAPHIC` 校验 | HTTP 错误由现有请求层传播 | 前端组件和 Common API 与后端一致 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | AC-001, AC-002, AC-004 | 新增 `GraphicCaptchaType` 和 `CaptchaDifficulty`；全局配置定义默认题型、默认级别和允许列表，默认级别固定为 `SIMPLE`；会话请求只能选择允许范围内的值。 | api、core、starter、starter-remote | 删除新增 endpoint、配置和生成器，保留旧接口 |
| TD-002 | AC-001, AC-002 | 新增 `/captcha/graphic`，返回类型 `GRAPHIC`；未传参数使用全局默认，低于默认级别的匿名请求按默认级别执行。 | captcha-api、starter、CaptchaService | 回退到旧算术入口 |
| TD-003 | AC-002, AC-003 | 字母数字图形验证码复用 Hutool `LineCaptcha` 和 `RandomGenerator`；答案只进入服务端会话，不通过公开 `extra` 返回。 | captcha-core、CaptchaService | 删除新增生成器和校验分支 |
| TD-004 | AC-003 | 默认 `SIMPLE` 算术题为一位数单步加减，减法保证非负；中等和复杂级别使用固定服务端规则，不接受前端自定义表达式。 | DefaultArithmeticCaptchaGenerator、算术单测 | 回退到旧生成逻辑 |
| TD-005 | AC-005, AC-006 | API、Controller、Feign 和 Common API 增加带可选题型/级别参数的 `generateGraphic`；不改变已有接口路径、请求字段和旧客户端调用。 | captcha-api、starter、starter-remote、mango-ui/packages/common | 删除新增方法和前端 API |
| TD-006 | AC-001, AC-002 | 新会话 key 保存题型、级别和答案；校验以服务端会话为准，成功后删除 key。 | CaptchaService | 回滚新增会话分支 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| TASK-001 | TD-001 | 1 | captcha API 常量、DTO、接口 | 新类型、级别、响应字段和 API 方法可编译 |
| TASK-002 | TD-002, TD-003, TD-004, TD-006 | 2 | captcha core generator/service | 默认和会话级两种图形模式生成、服务端会话存储和一次性校验完成 |
| TASK-003 | TD-001, TD-005 | 3 | starter Controller、配置、Feign | HTTP 与远程契约一致，旧接口保持兼容 |
| TASK-004 | TD-005 | 4 | Common captcha API | 前端可调用统一图形验证码入口并传递可选会话参数 |
| TASK-005 | 全部 | 5 | 测试与 README | 单测、集成、契约和使用说明覆盖验收项 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-001 | M10/M11 | `mvn -f mango/mango-platform/mango-captcha/pom.xml test` | 通过；captcha core 39 项、starter 17 项、remote 3 项测试通过 | Maven Surefire 输出 |
| AC-002 | M10/M11 | `CaptchaServiceTest` 会话存储、一次性校验和默认级别保护 | 通过 | `CaptchaServiceTest` |
| AC-003 | M10/M11 | `ArithmeticCaptchaServiceTest`、Controller 集成测试 | 通过 | Maven Surefire 输出 |
| AC-004 | M09/M11 | `GraphicCaptchaProperties` 默认配置绑定、生成器和服务测试 | 通过 | starter 集成测试与 core 测试 |
| AC-005 | M09/M11 | `CaptchaContractParityTest`、Feign HTTP 集成测试 | 通过；3 项 remote 测试通过 | Maven Surefire 输出 |
| AC-006 | M09 | `pnpm exec vue-tsc --noEmit -p packages/common/tsconfig.json`、Common Vitest | 通过；36 个文件、369 个测试通过 | pnpm 输出 |
| AC-007 | M09 | Common API、`GraphicCaptcha.vue`、`CaptchaSelector` 静态检查 | 通过；前端类型检查通过 | pnpm 输出 |

## 7. 例外与剩余风险

- 当前不执行真实业务应用登录链路验收；未连接业务数据库、租户和登录服务，因此不声明登录页面完成验收。
- 1 秒心算要求通过默认 `SIMPLE` 级别边界（单步、一位操作数、加减法）约束和测试保证；未执行真人计时实验。
- 会话参数只允许选择全局允许列表中的题型和级别；本次不实现业务身份感知的“可信调用方降低默认级别”特例。
- 发布版本、Maven/npm 制品升级和发布验证不在本次开发范围内。
