# 验证码 Captcha

## 1. 概览
`mango-captcha` 提供验证码生成、发送、存储和校验能力，覆盖算术验证码、滑块拼图、点选文字、无感行为验证、短信验证码和邮件验证码。

主要使用者是登录认证、找回密码、短信发送、邮箱验证、敏感操作二次确认和风控前置校验。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| 前端需要通过统一 HTTP 接口获取图形验证码或行为验证 challenge | Maven 依赖 / HTTP API / Java API |
| 业务接口需要按 key 校验用户提交的验证码答案 | Maven 依赖 / HTTP API / Java API |
| 短信或邮件验证码需要统一生成 key、code、过期时间，并交给业务 provider 发送 | Maven 依赖 / HTTP API / Java API |
| 项目希望验证码答案通过 mango-infra-kv 的 IKvStore 统一存储 | Maven 依赖 / HTTP API / Java API |

## 3. 适用场景
- 前端需要通过统一 HTTP 接口获取图形验证码或行为验证 challenge。
- 业务接口需要按 key 校验用户提交的验证码答案。
- 短信或邮件验证码需要统一生成 key、code、过期时间，并交给业务 provider 发送。
- 项目希望验证码答案通过 `mango-infra-kv` 的 `IKvStore` 统一存储。

## 4. 边界说明
- 不决定哪些业务接口必须启用验证码；触发策略由认证、风控或业务模块决定。
- 不负责登录失败锁定、IP 黑名单、设备指纹、账号风控和审计。
- 不内置真实短信、邮件服务商；默认 provider 只适合开发验证，生产必须替换。
- 不负责前端交互组件样式，只提供生成和校验接口。

## 5. 模块组成
- `mango-captcha-api`：`CaptchaApi`、验证码类型、请求响应 DTO、`SmsProvider`、`EmailProvider`、`CaptchaStorage` SPI。
- `mango-captcha-core`：算术、滑块、点选文字、无感行为验证码生成和校验逻辑，内置滑块图库。
- `mango-captcha-starter`：`CaptchaAutoConfiguration`、`CaptchaProperties`、`CaptchaController` 和默认短信/邮件 provider。

业务模块负责决定验证码校验时机、错误次数策略、真实短信/邮件发送实现和校验通过后的业务动作。

## 6. 接入方式
需要暴露验证码 HTTP 接口的服务引入 starter：

```xml
<dependency>
    <groupId>io.mango.platform.captcha</groupId>
    <artifactId>mango-captcha-starter</artifactId>
</dependency>
```

只需要编译期契约时引入 API：

```xml
<dependency>
    <groupId>io.mango.platform.captcha</groupId>
    <artifactId>mango-captcha-api</artifactId>
</dependency>
```

生产环境如果使用短信或邮件验证码，需要提供 `SmsProvider` 或 `EmailProvider` Bean，并在配置中指定 provider 名称。

## 7. 配置说明
配置前缀：`mango.captcha`。

| 配置项 | 类型 | 默认值 | 含义 |
|--------|------|--------|------|
| `storage` | string | `auto` | 存储策略标识，注释中保留 `redis`、`db`、`memory` 语义；当前核心运行依赖注入的存储实现，实际后端要结合 `mango-infra-kv` 配置验收。 |
| `ttl` | long | `300` | 验证码默认有效期，单位秒。 |
| `arithmetic.enabled` | boolean | `true` | 是否启用算术验证码。 |
| `arithmetic.width` | int | `120` | 算术验证码图片宽度。 |
| `arithmetic.height` | int | `40` | 算术验证码图片高度。 |
| `block-puzzle.enabled` | boolean | `true` | 是否启用滑块拼图验证码。 |
| `block-puzzle.width` | int | `280` | 滑块背景图宽度。 |
| `block-puzzle.height` | int | `160` | 滑块背景图高度。 |
| `block-puzzle.slider-size` | int | `50` | 滑块尺寸。 |
| `block-puzzle.image-locations` | list | 空列表 | 自定义图库位置，支持 `classpath:`、`file:`、HTTP 和 HTTPS；为空使用内置图库。 |
| `click-word.enabled` | boolean | `true` | 是否启用点选文字验证码。 |
| `click-word.width` | int | `320` | 点选验证码图片宽度。 |
| `click-word.height` | int | `180` | 点选验证码图片高度。 |
| `click-word.word-count` | int | `4` | 图片中生成的候选文字数量。 |
| `click-word.target-count` | int | `3` | 用户需要按顺序点击的目标文字数量。 |
| `click-word.tolerance` | int | `24` | 点选坐标容差，单位像素。 |
| `sms.enabled` | boolean | `true` | 是否启用短信验证码能力。 |
| `sms.length` | int | `6` | 短信验证码长度。 |
| `sms.period` | int | `60` | 短信发送间隔，单位秒。 |
| `sms.provider` | string | `default` | 短信 provider 名称。 |
| `email.enabled` | boolean | `true` | 是否启用邮件验证码能力。 |
| `email.length` | int | `6` | 邮件验证码长度。 |
| `email.provider` | string | `default` | 邮件 provider 名称。 |

配置示例：

```yaml
mango:
  captcha:
    ttl: 300
    block-puzzle:
      image-locations:
        - classpath:/captcha/block-puzzle/city.jpg
    sms:
      provider: aliyun-sms
      period: 60
    email:
      provider: smtp-mail
```

## 8. API 与扩展
公共 HTTP 根路径：`/captcha`，Controller 标记为 `PUBLIC`。

| 方法 | 路径 | 用途 |
|------|------|------|
| GET | `/captcha/types` | 查询当前支持的验证码类型和存储策略。 |
| GET | `/captcha/arithmetic` | 生成算术验证码。 |
| GET | `/captcha/block-puzzle` | 生成滑块拼图验证码。 |
| GET | `/captcha/click-word` | 生成点选文字验证码。 |
| GET | `/captcha/behavior` | 生成无感行为验证 challenge。 |
| POST | `/captcha/behavior/verify` | 校验行为数据，返回评分、风险等级和建议动作。 |
| POST | `/captcha/verify` | 按 key、类型和用户答案校验验证码。 |

Java 契约：

- `CaptchaApi.generate`：生成指定类型验证码。
- `CaptchaApi.verify`：校验验证码答案。
- `CaptchaApi.verifyBehavior`：校验无感行为验证并返回评分详情。
- `CaptchaApi.sendSms`、`sendEmail`、`send`：生成并发送短信或邮件验证码。
- `SmsProvider`：接入真实短信服务商。
- `EmailProvider`：接入真实邮件服务商。
- `CaptchaStorage`：验证码存储 SPI。

## 9. 数据与初始化
Flyway 路径：`mango-captcha-core/src/main/resources/db/migration/captcha`。

`V1__init_captcha.sql` 创建 `captcha_code` 表，字段包含 `tenant_id`、`code_key`、`code_value`、`expire_time`，并建立：

- 唯一键 `uk_code_key`。
- 过期时间索引 `idx_expire_time`。

当前最小运行闭环应以实际注入的验证码存储和 `mango-infra-kv` 配置为准。`captcha_code` 表存在于模块迁移里，使用数据库存储前必须确认当前运行时确实启用了对应 `CaptchaStorage` 实现。

## 10. 管理入口
本模块不提供管理菜单。`CaptchaController` 是公共接口，允许登录前调用。验证码 key 的业务含义、租户上下文、IP 限流和失败次数由调用方控制。

如果验证码用于登录，校验通过只代表验证码正确，不代表账号、密码、租户、权限或风控通过。

## 11. 快速开始
1. 引入 `mango-captcha-starter`。
2. 按场景配置验证码类型、有效期、短信或邮件 provider。
3. 前端先调用 `/captcha/arithmetic`、`/captcha/block-puzzle`、`/captcha/click-word` 或 `/captcha/behavior` 获取 key 和展示数据。
4. 用户提交业务动作时带上验证码 key、类型和答案。
5. 业务接口先调用 `CaptchaApi.verify` 或 `/captcha/verify`，通过后再执行登录、发送短信、修改密码等业务动作。
6. 业务侧记录失败次数、IP 限流和审计。

## 12. 问题排查
- 验证一直失败：检查 key、验证码类型、答案字段、存储后端和服务实例是否一致。
- 生产短信没发出：默认短信 provider 不是生产实现，需要注册真实 `SmsProvider` Bean 并配置 `sms.provider`。
- 滑块图片不符合业务风格：配置 `block-puzzle.image-locations` 指向自定义图库。
- 开启验证码后登录仍不安全：验证码只解决人机校验，账号密码、MFA、风控和权限仍要独立校验。

## 13. 相关文档
- [后端安全规范](../../../mango-pmo/rules/backend/06-security.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史资料
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
