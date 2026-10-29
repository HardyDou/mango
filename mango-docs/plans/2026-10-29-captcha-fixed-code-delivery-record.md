# 标准交付记录

> 短信验证码开发/测试固定值配置

## 1. 元数据

- 任务 ID：captcha-fixed-code-env
- 交付模式：STANDARD
- 需求影响：L2 - 改变短信验证码公共配置和开发/测试认证流程；生产验证码行为必须保持真实供应商链路。
- 方案风险：L2 - 验证码服务增加配置分支，错误配置可能影响认证安全边界；默认关闭且生产保持随机验证码与真实供应商。
- 最终风险：L2
- 工作区决策：CREATE；任务分支 `feat/captcha-fixed-code-env`，工作区 `/Users/hardy/Work/mango-captcha-fixed-code`
- 非降级事实：SECURITY
- 评审选择：`AGENT_ONLY`。用户未要求外部同行评审，保留三视角 Agent 分析，不将其称为人工评审。
- 启用保障措施：M01、M08、M09、M10、M11、M14

## 2. 目标与范围

- 目标：允许开发/测试环境通过配置使用固定短信验证码，例如 `0000`，并让生产环境默认继续使用随机验证码和真实 `SmsProvider`。
- 成功条件：
  1. 开发/测试启用固定值后，发送接口保存配置值并跳过短信供应商。
  2. 校验接口仍使用原有 `key`、`type`、`code` 链路，固定值可以正常校验。
  3. 未启用固定值时，短信验证码行为保持兼容。
  4. 固定值不是纯数字时明确失败，不保存验证码。
  5. README、配置参数和环境变量说明完整。
- 处理范围：`mango-captcha` 验证码服务、配置属性、单元测试、模块 README 和本交付记录。
- 不处理范围：不新增真实短信供应商；不修改登录、注册、找回密码或绑定手机号业务流程；不在生产环境自动启用固定验证码。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-01 | `CaptchaApi.send()` / `/captcha/send` | `SMS`、固定验证码开关为 `true`、配置值为 `0000` | 保存 `0000` 并返回业务验证码 key，不调用短信供应商 | 供应商不可用不阻断固定模式 | 单元测试断言 KV 值为 `0000` 且无供应商调用 |
| AC-02 | `CaptchaApi.verify()` / `/captcha/verify` | 使用 AC-01 返回的 key 和 `0000` | 复用原有验证码校验并成功消费一次性验证码 | 错误值、过期 key 按原有验证码错误返回 | 现有验证码校验测试和服务测试通过 |
| AC-03 | 生产或未启用固定值的 SMS 发送 | `fixed-code-enabled=false` 或未配置 | 继续随机生成验证码并调用 `SmsProvider` | 无供应商或供应商失败返回发送失败且不保存 | 现有随机发送和供应商失败测试通过 |
| AC-04 | 配置校验 | 固定模式启用但值包含非数字字符 | 拒绝发送 | 返回验证码配置错误且不写 KV | 单元测试断言抛出业务异常且无 KV 写入 |

### 三视角复核

| 视角 | 当前来源 | 结论与依据 | 未解决项 | 下一步输入 |
|---|---|---|---|---|
| 事实与用户视角 | `mango-captcha` README、`CaptchaService`、`DefaultSmsProvider`、用户任务 | 当前服务随机生成验证码，默认短信供应商拒绝发送；开发/测试需要固定值完成流程验证。 | 未指定具体测试环境 profile 名称。 | 使用显式配置开关和固定值，环境通过 YAML 或 Spring 环境变量覆盖。 |
| 技术与风险视角 | `CaptchaService.send()`、`CaptchaProperties.Sms`、`SmsProvider` SPI | 固定模式在服务生成验证码处生效并跳过供应商；默认开关关闭，生产不改配置即保持真实链路；非数字值拒绝。 | 生产环境是否统一使用 `prod` profile 不由框架保证。 | README 明确生产保持关闭，默认值为关闭。 |
| 验证与交付视角 | `CaptchaServiceTest`、captcha Maven 聚合测试、README 能力说明 | 单元测试可观察固定值、跳过供应商、非法配置和兼容随机路径；Maven verify 覆盖 starter 装配和接口回归。 | 未执行真实短信供应商发送。 | 将真实供应商联调列为未验证，不影响固定模式验收。 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-01 | AC-01/AC-03 | 新增 `mango.captcha.sms.fixed-code-enabled`，默认 `false`；新增 `mango.captcha.sms.fixed-code`，启用时要求纯数字。 | `CaptchaService.java`、`CaptchaProperties.java` | 删除两个配置字段和固定分支，恢复随机生成。 |
| D-02 | AC-01 | 固定模式直接将固定值写入现有验证码 KV 会话并跳过 `SmsProvider`，不改变 API 契约。 | `CaptchaService.java` | 关闭配置或删除固定分支。 |
| D-03 | AC-02/AC-03 | 校验路径不变；固定值只影响 SMS 发送，EMAIL、图形、滑块和行为验证码不变。 | `CaptchaService.java` | 删除固定值逻辑。 |
| D-04 | AC-01/AC-03/AC-04 | 通过 Spring YAML 或 `MANGO_CAPTCHA_SMS_FIXED_CODE_ENABLED`、`MANGO_CAPTCHA_SMS_FIXED_CODE` 配置环境差异。 | `mango-captcha/README.md` | 恢复旧 README 配置说明。 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| I-01 | D-01 | 1 | `mango-captcha-starter/.../CaptchaProperties.java` | 配置属性包含默认关闭的固定验证码开关和值。 |
| I-02 | D-02/D-03 | 2 | `mango-captcha-core/.../CaptchaService.java` | 固定 SMS 值保存并绕过供应商；默认路径兼容。 |
| I-03 | AC-01/AC-04 | 3 | `mango-captcha-core/.../CaptchaServiceTest.java` | 覆盖固定值成功、供应商跳过和非法值失败。 |
| I-04 | D-04 | 4 | `mango-captcha/README.md` | 配置、环境变量、生产边界和验证方式可被业务开发者直接使用。 |
| I-05 | 全部 | 5 | 本交付记录 | 记录风险、三视角、命令和结果。 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-01 | M10 | `mvn -f mango/mango-platform/mango-captcha/pom.xml verify` | PASS；固定模式保存 `0000` 且不调用 `SmsProvider`。 | `mango-captcha-core` 的 `CaptchaServiceTest`，43 项通过 |
| AC-02 | M10/M11 | 同上 | PASS；现有校验、单次消费和 Controller 集成测试通过。 | `mango-captcha-starter` 测试，18 项通过 |
| AC-03 | M10 | 同上 | PASS；随机 SMS 和供应商失败分支保持通过。 | `CaptchaServiceTest` |
| AC-04 | M10 | 同上 | PASS；非数字固定值抛业务异常且不写 KV。 | `CaptchaServiceTest` |
| M09 | M09 | `git diff --check`、`node mango-pmo/tools/test-quality-check.mjs --base origin/main`、`node mango-pmo/tools/audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | PASS | 差异检查、测试质量检查和 mock 审计通过 |
| M08 | M09 | `node mango-pmo/tools/audit-module-readmes.mjs`、`node mango-pmo/tools/audit-readme-source-facts.mjs`、`node mango-pmo/tools/check-standard-delivery-record.mjs mango-docs/plans/2026-10-29-captcha-fixed-code-delivery-record.md` | PASS | README 审计、源码事实审计和标准交付记录检查通过 |
| M09 | M09 | `mvn -f mango/pom.xml -pl :mango-architecture-verification io.mango.tools.maven.plugin:mango-maven-plugin:1.0.0-SNAPSHOT:check -Drule=module-info -Dmango.check.changedOnly=true -Dmango.check.gate=no-new-violations -Dmango.check.baseRef=origin/main` | PASS | 3 个既有 module-info 问题均为 baseline，本次新增问题为 0 |
| M10 | M10 | `mvn -f mango/mango-platform/mango-captcha/pom.xml verify` | PASS | captcha 聚合模块测试通过：核心 43 项、starter 18 项、remote starter 3 项 |
| M11 | M11 | `mvn -f mango/pom.xml -pl :mango-captcha-core,:mango-captcha-starter checkstyle:check`、`mvn -f mango/pom.xml -pl :mango-captcha-core,:mango-captcha-starter pmd:check` | PASS | Checkstyle 0 violations；PMD 命令成功，但报告既有处理警告，详见剩余风险 |

## 7. 例外与剩余风险

- 例外：无。
- 剩余风险：固定验证码是开发/测试便利能力；如果生产错误设置 `fixed-code-enabled=true`，认证安全性会降低。生产部署必须保持默认值 `false`，并由环境配置审查避免开启。
- 未验证项：未调用真实短信供应商，未执行生产环境部署；本次只验证固定模式、原有随机模式和模块测试链路。
- 工具限制：直接从 `mango/mango-platform/mango-captcha` 聚合 POM 执行 `pmd:check` 时，历史配置引用的 `mango-tools` 相对路径不存在；从仓库根 POM 对受影响模块执行 PMD 时命令返回成功但报告了既有 PMD 处理警告。根 POM 的 `checkstyle:check` 通过；`mango:check` 前缀在受影响模块的局部 reactor 中未注册，按 CI 同源的 `mango-maven-plugin:check` 入口执行时仅发现基线问题，未发现本次改动新增问题。
- 服务状态：未启动运行态服务；本任务不需要数据库重建或业务环境写入。
