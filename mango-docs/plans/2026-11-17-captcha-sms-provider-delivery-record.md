# 标准交付记录

> Issue #1292 验证码短信内置阿里云/腾讯云供应商

## 1. 元数据

- 任务 ID：`captcha-sms-provider-1292`
- 交付模式：STANDARD
- 需求影响：L2 - 新增短信验证码供应商装配与凭证来源，改变 `/auth/captcha/send` 的失败语义（原为 `200` + `data=null`，改为业务错误码），影响登录、注册、找回密码、换绑手机号等所有启用短信验证码的入口。
- 方案风险：L2 - 新增第三方短信调用与凭证读取路径；凭证错误、模板错误或网络失败时不得退化为"提示成功"。
- 最终风险：L2
- 工作区决策：CREATE；任务分支 `feat/captcha-sms-provider-1292`，工作区 `/data/Mango/worktrees/mango-captcha-sms-provider`（仓库外同级目录）
- 非降级事实：SECURITY（凭证读取与外部短信网关）
- 评审选择：`AGENT_ONLY`。用户未要求外部同行评审，保留三视角 Agent 分析，不称为人工评审。
- 启用保障措施：M01、M08、M09、M10、M11

## 2. 目标与范围

- 目标：Mango 内置阿里云、腾讯云短信供应商；业务只配置 `mango.captcha.sms.provider`、`template-code` 与对应凭证配置项即可发送验证码，不写 Java；发送失败对调用方可见。
- 成功条件：
  1. `mango.captcha.sms.provider=aliyun|tencent` 时装配对应 `SmsProvider`，凭证与账号配置全部来自 `mango.captcha.sms.aliyun.*` / `mango.captcha.sms.tencent.*` 配置项（值由部署环境以 `${ENV}` 注入），不依赖 `mango-integration`，凭证不入库、不在代码仓库中保存明文。
  2. 凭证、签名、应用 ID、模板缺失时，`/auth/captcha/send` 返回业务错误码，不再返回 `200` + 空 `data`，且不写验证码 KV。
  3. 现有 `provider=default`（不发送）与固定验证码（`fixed-code-enabled=true`）行为保持兼容。
  4. 前端发送失败不再提示"验证码已发送"，不启动倒计时。
- 处理范围：`mango-captcha-api` 错误码、`mango-captcha-core` 发送语义、`mango-captcha-starter` 供应商与自动配置、`mango-ui/packages/common` 发送失败处理、模块 README、本记录。
- 不处理范围：不修改通知模块 `mango-notice-channel-sms`；不新增按 `businessType` 拆分模板的多模板能力；不改造登录/注册业务；不为邮件通道新增供应商；不引入 `mango-integration` 依赖、不新增连接器声明；不支持每租户独立短信账号；不发布 Maven/npm 版本。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-01 | `SmsCaptcha.vue` → `POST /auth/captcha/send` | `type=SMS`、`provider=aliyun`、`mango.captcha.sms.template-code` 与 `mango.captcha.sms.aliyun.*`（AK/SK/签名）已配置 | 调用阿里云短信接口发送验证码，返回验证码 key，前端提示成功并倒计时 | 发送失败返回 `CaptchaCode.CAPTCHA_SEND_FAILED`(2410)，不写 KV | 单测断言供应商参数映射与错误码；供应商成功分支断言返回 key |
| AC-02 | 同上 | `provider=tencent`、`mango.captcha.sms.tencent.*`（SecretId/SecretKey/签名/SdkAppId）已配置 | 调用腾讯云短信接口发送验证码 | 同 AC-01 | 单测断言腾讯云参数映射与错误码 |
| AC-03 | 同上 | `provider=default` 或未配置供应商，且固定验证码关闭 | 不再返回 `200` + 空 `data`；返回 `CAPTCHA_SEND_FAILED` | 明确失败，日志包含 `type`、`businessType`，不输出手机号明文与凭证 | `CaptchaServiceTest` 断言抛 `BizException` 且无 KV 写入 |
| AC-04 | 同上 | 缺少 `aliyun.access-key-id`/`access-key-secret`/`sign-name`（或腾讯云对应四项）、`template-code` 为空、`provider` 取值不匹配任何内置供应商 | 明确失败 | 返回 `CAPTCHA_SEND_FAILED`，日志记录缺失的配置项键名（`property=`，不含凭证值） | 单测覆盖凭证缺失、签名缺失、模板编码为空、`provider=unknown` 无 Bean 四条分支 |
| AC-05 | `CaptchaApi.send()`（业务代码） | `fixed-code-enabled=true` | 保持原有固定验证码行为，不调用供应商、不读取任何短信凭证配置 | 固定值非纯数字时仍按原有规则失败 | 现有固定验证码测试通过 |
| AC-06 | 应用启动 | `provider` 未配置或为 `default`/`aliyun`/`tencent`/未知值；下游可能排除 SMS SDK 依赖 | 只装配对应供应商 Bean；未知值不装配任何 `SmsProvider`；缺配置不影响启动 | 应用必须能启动：装配期仅 `log.error` 指出缺失配置项，发送期统一 `2410` | `CaptchaAutoConfigurationTest` 覆盖五种情形，装配期无异常 |
| AC-07 | 前端 `SmsCaptcha` | 接口返回 `data` 为空（兼容旧后端或异常响应） | 提示发送失败，不启动倒计时，不保存空 key | 保持现有异常提示路径 | 前端类型检查通过，组件逻辑不再把空值当成功 |

### 三视角复核

| 视角 | 当前来源 | 结论与依据 | 未解决项 | 下一步输入 |
|---|---|---|---|---|
| 事实与用户视角 | Issue #1292、`mango/mango-platform/mango-captcha/README.md`（§5.4 短信通道）、`mango-docs/plans/2026-10-29-captcha-fixed-code-delivery-record.md:27` | 用户要求"开箱可用"，但明确否决"凭证经应用集成连接实例读取"与"系统参数 `sys_config` 明文存入数据库"两条路线；最终定为纯配置项方案 | 无 | 按方案 A 实施 |
| 技术与风险视角 | `CaptchaService.java`（SMS 分支）、`DefaultSmsProvider.java:16-17`、`CaptchaProperties.Sms`、`AliyunSmsSender.java`、`TencentSmsSender.java` | 通知模块网关为包内私有且绑定 `notice_channel_config`，无法直接复用；供应商放在 `mango-captcha-starter` 内可避免新模块与配置前缀重复绑定；凭证走 `mango.captcha.sms.*` 配置项直接绑定，链路最短、无额外 RPC 与解密环节 | 通知网关与验证码网关存在同类 SDK 调用重复，未来若新增第三个消费方需再评估下沉 | 限制在一次实现，不引入新模块 |
| 验证与交付视角 | `mango-pmo/rules/backend/08-test.md`、现有 `CaptchaServiceTest`、`mango-captcha-starter/src/test` | 单测可覆盖参数映射、配置缺失、失败语义；SDK 调用本身用测试替身隔离（第三方短信属于允许替身范围） | 真实短信发送需真实账号与凭证，本次不执行外部真实发送 | 交付时标记未验证项 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-01 | AC-01/02 | 供应商实现放在 `mango-captcha-starter` 的 `io.mango.captcha.starter.provider` 包，与既有 `DefaultSmsProvider` 同层；不新增 Maven 模块。理由：`CaptchaProperties` 已在该模块，新建模块会重复绑定 `mango.captcha.sms` 前缀或形成循环依赖。 | `mango-captcha-starter` | 删除新增类与 pom 依赖 |
| D-02 | AC-01/02/04 | 凭证与账户级配置全部来自 Spring 配置项，**不依赖应用集成**：`mango.captcha.sms.aliyun.{access-key-id,access-key-secret,sign-name,endpoint,region}`、`mango.captcha.sms.tencent.{secret-id,secret-key,sign-name,sdk-app-id,region,endpoint,country-code}`，值由部署环境以 `${ENV}` 占位符注入。凭证不入库（不写 `sys_config`、不写集成连接实例），也不提供 yml 明文默认值。缺失时不回退、不猜测，直接按 2410 失败。 | `CaptchaProperties.Sms.Aliyun/Tencent`、`AliyunSmsProvider`/`TencentSmsProvider`、`AliyunSmsSender`/`TencentSmsSender` | 回退配置 `provider=default`；凭证配置残留不影响其他模块 |
| D-03 | AC-01/02 | 模板编码统一由 `mango.captcha.sms.template-code` 提供，且**只有 `CaptchaService` 一个读者**：用 `@Value("${mango.captcha.sms.template-code:}")` 读取，并按 SPI 现状作为 `SmsProvider.send(mobile, templateCode, code)` 的第二参显式传入；`CaptchaProperties.Sms.templateCode` 仅作为配置项载体存在，供应商只校验调用方传入值非空，不再读同一键做回退。理由：与既有 `ttl`、`fixed-code` 的 `@Value` 写法一致，避免同一配置键出现两个读者。 | `CaptchaService.java`、`CaptchaProperties.Sms`、供应商实现 | 删除配置项并恢复原调用 |
| D-04 | AC-01/03/04 | 失败语义统一为 `CaptchaCode.CAPTCHA_SEND_FAILED(2410)`：无供应商、`provider` 取值不匹配、配置缺失、供应商返回 `false`、SDK 抛异常均映射为该业务码；公开入口不返回供应商内部错误文本，详情只进日志（不含凭证与完整手机号）。装配期不抛异常（应用必须能启动），只 `log.error` 指出缺失的配置项键名，失败留到发送期统一 2410。 | `mango-captcha-api` 枚举、`CaptchaService.java`（SMS 分支）、`SmsProviderSupport`、`mango-auth` 既有 `AuthApiResponseAdapter.requireCaptchaData` 透传错误码 | 恢复 `return null` |
| D-05 | AC-06 | 供应商装配只依赖配置项，不新增连接器声明、不改管理端下拉项：`aliyunSmsProvider`/`tencentSmsProvider` 使用 `@ConditionalOnProperty(name="mango.captcha.sms.provider", havingValue="aliyun|tencent")` + `@ConditionalOnMissingBean(SmsProvider.class)` + `@ConditionalOnClass`（对应 SDK 客户端类，避免下游排除依赖时 `NoClassDefFoundError`）；`defaultSmsProvider` 用 `havingValue="default", matchIfMissing=true` 保持开发期 warn 语义。`provider` 为未知值时不装配任何 `SmsProvider`，发送按 2410 失败——这是期望行为，由 `CaptchaAutoConfigurationTest.withUnknownProvider` 固定。 | `CaptchaAutoConfiguration.java` | 删除对应 `@Bean` 与配置项 |
| D-06 | AC-07 | 前端 `SmsCaptcha.vue` 只在返回非空 key 时提示成功并倒计时；空值按失败处理。 | `mango-ui/packages/common/components/Captcha/SmsCaptcha.vue` | 还原组件 |
| D-07 | 兼容性 | `SmsProvider` SPI 签名不变；`provider=default`、固定验证码、邮件通道行为不变；新增配置项均有默认值或仅在显式启用时生效。 | `mango-captcha-*` | 回退分支 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| I-01 | D-04 | 1 | `mango/mango-platform/mango-captcha/mango-captcha-api/src/main/java/io/mango/captcha/api/enums/CaptchaCode.java` | 新增 `CAPTCHA_SEND_FAILED(2410)`，Javadoc 说明业务含义 |
| I-02 | D-02/D-03 | 2 | `mango/mango-platform/mango-captcha/mango-captcha-starter/src/main/java/io/mango/captcha/starter/properties/CaptchaProperties.java` | `Sms` 增加 `provider`、`templateCode` 与嵌套 `Aliyun`/`Tencent`；默认值集中在常量（`DEFAULT_ALIYUN_ENDPOINT` 等）；移除 `instanceCode` |
| I-03 | D-02/D-04 | 3 | 新增 `.../provider/AliyunSmsProvider.java`、`.../provider/TencentSmsProvider.java`、`.../provider/AliyunSmsSender.java`、`.../provider/TencentSmsSender.java`、`.../provider/SmsProviderSupport.java` | 供应商从 `CaptchaProperties` 读凭证、映射参数、失败抛 `CAPTCHA_SEND_FAILED`；装配期不抛异常 |
| I-04 | D-01/D-05 | 4 | `.../config/CaptchaAutoConfiguration.java`、`mango-captcha-starter/pom.xml` | 按 `mango.captcha.sms.provider` 条件装配；两个内置通道 `@Bean` 分别加 `@ConditionalOnClass` 指向 SDK 客户端类；pom 只加两个短信 SDK，不加 `mango-integration-api`、不显式声明 `jackson-databind` |
| I-05 | D-03/D-04 | 5 | `mango/mango-platform/mango-captcha/mango-captcha-core/src/main/java/io/mango/captcha/core/service/impl/CaptchaService.java` | 发送失败抛业务异常并保留脱敏日志；模板编码由 `CaptchaService` 通过 `@Value` 读取并显式传给 `SmsProvider`，core 不新增 starter 依赖 |
| I-06 | —（已撤销） | 6 | `mango/mango-platform/mango-integration/mango-integration-starter/src/main/resources/META-INF/mango/resources/integration-connectors.json`、`mango-ui/packages/rbac/src/views/integration-connector/IntegrationConnectorView.vue` | 本方案不声明连接器：两个文件均已 `git checkout --` 回退，工作区无改动 |
| I-07 | D-06 | 7 | `mango-ui/packages/common/components/Captcha/SmsCaptcha.vue` | 空 key 视为失败 |
| I-08 | AC-01..06 | 8 | `.../mango-captcha-starter/src/test/java/io/mango/captcha/starter/provider/*`、`.../mango-captcha-starter/src/test/java/io/mango/captcha/starter/config/CaptchaAutoConfigurationTest.java`、`.../mango-captcha-core/src/test/java/io/mango/captcha/core/service/CaptchaServiceTest.java` | 覆盖参数映射、配置缺失、失败语义、`provider` 条件装配、固定验证码兼容 |
| I-09 | M08 | 9 | `mango/mango-platform/mango-captcha/README.md`、根 `CHANGELOG.md` | 写明配置项全量表、yml 示例、`${ENV}` 注入要求、失败语义与已知限制（不依赖应用集成、改配置需重启、一个部署一套账号） |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-01/02/04 | 单元测试（SDK 客户端测试替身：`Recording*Sender extends *SmsSender` 覆写 `createClient`） | `cd mango && mvn -o -B -ntp -pl mango-platform/mango-captcha/mango-captcha-api,mango-platform/mango-captcha/mango-captcha-core,mango-platform/mango-captcha/mango-captcha-starter verify` | PASS（BUILD SUCCESS，5.692 s；core 44 + starter 61 = 105 tests，failures 0 / errors 0） | `AliyunSmsProviderTest`(12)、`TencentSmsProviderTest`(14)、`AliyunSmsSenderTest`(5)、`TencentSmsSenderTest`(5)、`CaptchaAutoConfigurationTest`(7) |
| AC-03/05 | 单元测试 | `CaptchaServiceTest` 26 例（含 `sendSms_providerReturnsFalse_throwsSendFailedAndDoesNotSave`、`sendSms_withoutProvider_throwsSendFailedAndDoesNotSave`、固定验证码不调供应商、邮件分支仍返回 `null`） | PASS | 同上 verify 输出 |
| AC-06 | 条件装配 | `CaptchaAutoConfigurationTest`：`provider` 未配置 / `=default` / `=aliyun` / `=tencent` / `=unknown` / 用户自定义 `SmsProvider` Bean | PASS（7 例；`=unknown` 断言 `doesNotHaveBean(SmsProvider.class)`，用户 Bean 胜出） | 同上 verify 输出 |
| AC-01..06 | 模块静态验证与架构门禁 | `mvn -o -B -ntp -pl <同上三模块> verify` | PASS（BUILD SUCCESS） | 命令输出 |
| AC-07 | 前端静态门禁（上一轮结果，本轮未改前端逻辑） | `cd mango-ui && node ./scripts/quality/run-static-gate.mjs typecheck` | PASS（`typecheck PASS {"failedWorkspaces":21,"diagnostics":426}` ratchet，新增诊断 0） | 门禁输出 |
| AC-01/02 | 真实短信发送 | 需要真实阿里云/腾讯云账号、签名、模板与部署环境凭证注入 | 未验证 | 本次不执行外部真实发送 |

### 验证缺口

- `/auth/captcha/send` 的 HTTP 层 2410 响应未在端到端层验证：`mango-auth` 不在本次模块范围内，现有 `CaptchaControllerIntegrationTest`（15 例）覆盖的是 `/captcha/verify` 等服务层路径；本次只验证到 `CaptchaService.send` 抛 2410 且不写 KV。
- 真实短信发送未验证：环境没有阿里云/腾讯云账号，发送成功路径仅由 SDK 替身覆盖。
- `mango-ui/packages/common/tsconfig.json` 的 `include` 为 `src/**`，而该包源码位于 `components/`、`api/`、`utils/`，官方 typecheck 几乎不覆盖 `SmsCaptcha.vue`；上一轮用临时 tsconfig 单独跑 `vue-tsc` 通过，临时文件已删除。tsconfig 缺口建议单独开 issue 跟踪。
- 未运行 `mvn mango:check` 与全 reactor 架构测试（根 pom 的 checkstyle/PMD/SpotBugs 均为 `failOnViolation=false`，`verify` 已通过）。
- 构建约束：必须把 `mango-captcha-api` 与 `mango-captcha-core` 放在同一个 reactor 中。单独构建 `mango-captcha-core` 时 Maven 命中 `~/.m2` 里的旧 `mango-captcha-api` jar，报 `NoSuchFieldError: CAPTCHA_SEND_FAILED`（已实际复现）；这是本地仓库陈旧 artifact 导致，不是代码缺陷，但在 CI 缓存或下游单独构建场景会再次出现，发布前需先 `mvn install` api 模块。

## 7. 例外与剩余风险

1. 未执行真实短信发送验证：缺少可用短信账号、签名与模板，需在实际业务项目环境验证。
2. 凭证以明文形式存在于部署配置中（yml / 配置中心 / 环境变量），靠 `${ENV}` 占位符与部署规范保护；没有加密入库环节，也没有运行期轮换机制，读取权限由部署侧控制。
3. 改配置需重启：配置项在应用启动时绑定，不支持热更新，凭证轮换必须重新发布/重启。
4. 一个部署只能一套短信账号（`provider` 二选一），不支持每租户不同账号；多账号需求只能自定义 `SmsProvider` Bean。
5. 通知模块与验证码模块各自封装一次短信 SDK 调用，存在同类实现重复；本任务不重构通知模块，避免扩大范围。
6. 仅支持单模板（`mango.captcha.sms.template-code`）；按 `businessType` 分配模板属于后续需求，本次不做。
7. 邮件通道发送失败仍返回 `null`（`R.ok(null)`），本任务只收敛短信语义；邮件通道是否同样改为显式失败需另开需求。
8. `SmsProvider` Bean 存在三处 `@ConditionalOnMissingBean`，业务自定义 `SmsProvider` 会静默让内置通道让位；装配行为依赖 Bean 定义顺序，已在 README 说明，但未加启动时互斥校验。
9. `DefaultSmsProvider` 保留 `@Component` 但无害（自动配置的 `defaultSmsProvider` 为 `@Bean` 同名定义）；若下游用 `@ComponentScan("io.mango")` 扫描到该类并同时显式配置 `provider=default`，可能出现 `BeanDefinitionOverride` 冲突。本轮不改注解，避免影响既有装配，列为已知风险。
10. `provider` 取未知值（如拼写错误）时不装配任何 `SmsProvider`，发送期才报 2410；应用启动不报错，只能靠装配期的 `log.error` 提示发现，属有意取舍（应用必须能启动）。
11. 真实第三方响应码只覆盖了 `OK` / 非 `OK` 两类；配额不足、签名未审核、模板未审核等具体错误码未逐个映射，统一为 2410 + 日志中的原始错误码。

## 8. 方案比较与被拒方案

| 方案 | 描述 | 结论 | 拒绝/采纳理由 |
|---|---|---|---|
| **A（采纳）** 纯配置项 | 凭证与账号配置全部走 `mango.captcha.sms.*` 配置项，值由部署环境 `${ENV}` 注入；供应商直接读 `CaptchaProperties` | 采纳 | 链路最短：无额外 RPC、无解密环节、无新模块；与既有 `mango.captcha.*` 配置风格一致；单测与本地验证成本最低 |
| B 应用集成连接实例 | 在 `mango-integration` 声明 `aliyun-sms`/`tencent-sms` 连接器，凭证存连接实例并加密入库，`mango.captcha.sms.instance-code` 指向实例 | 拒绝（已回退） | 用户明确否决。代价过高：需引入 `mango-integration-api` 依赖与运行时 `ObjectProvider` 探测；`AK_SK` 认证方式 `IntegrationOutboundAuth` 并不支持，仍需自建 SDK 客户端绕过 `IntegrationClient`，等于只借用了"凭证存放位置"却引入整条依赖链；同时要在管理端补 `AK_SK` 下拉项、维护连接器声明与连接实例运维步骤，部署复杂度与故障面都变大 |
| C 系统参数 `sys_config` | 凭证写入系统参数表（明文），运行期按 key 读取 | 拒绝 | 明文入库违反最低凭证保护要求；且当前 `sys_config` 缺少按 key 读取的接口（只有列表/批量配置能力），要新增接口、新增缓存与失效策略，反而比方案 A 改动面更大 |
| D 复用通知模块短信网关 | 直接使用 `mango-notice-channel-sms` 的 `AliyunSmsGateway`/`TencentSmsGateway` | 拒绝 | 两个网关均为包内私有，并绑定 `notice_channel_config` 与渠道配置语义；强行复用需下沉为公共模块，超出本 issue 范围 |
| E 不内置，仅保留 SPI | 只保留 `SmsProvider` 扩展点，要求业务自行实现 | 拒绝 | 不满足 issue #1292 "开箱可用" 的目标 |
