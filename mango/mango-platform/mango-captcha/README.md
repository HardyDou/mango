# Mango Captcha

## 1. 能力定位

`mango-captcha` 提供验证码生成、存储和验证能力，支持算术、滑块拼图、点选文字、无感行为、短信和邮件验证码。主要使用者是认证、风控和需要二次校验的业务接口。

## 2. 适用场景

- 登录、找回密码、短信发送、邮箱验证等需要验证码校验的接口。
- 需要通过 `mango-infra-kv` 的 `IKvStore` 统一保存验证码答案和过期时间。
- 需要统一 HTTP 接口生成和验证图形、行为验证码。

## 3. 不适用场景

- 不决定业务接口的验证码启用策略。
- 不负责验证码失败后的账号锁定、风控策略或审计。
- 不负责短信和邮件服务商实现，短信/邮件 provider 需要业务接入真实实现。

## 4. 模块边界

`api` 提供验证码契约、类型和 provider SPI，`core` 提供验证码生成和校验逻辑，`starter` 提供自动配置和 HTTP Controller。业务模块负责触发时机、失败处理、短信/邮件发送 provider 和风控策略。

## 5. 接入方式

```xml
<dependency>
    <groupId>io.mango.platform.captcha</groupId>
    <artifactId>mango-captcha-starter</artifactId>
</dependency>
```

只使用契约时依赖 `mango-captcha-api`。

## 6. 配置项

配置前缀：`mango.captcha`。

已发现字段包括 `ttl`、`arithmetic`、`blockPuzzle`、`clickWord`、`sms`、`email`。当前核心存储依赖注入的 `IKvStore`，store 选择通过 `mango-infra-kv` 配置完成；不要把历史或预留的 storage 字段理解为当前运行时 store 切换入口。

## 7. 对外接口 / 扩展点

- API：`CaptchaApi`
- SPI：`SmsProvider`、`EmailProvider`
- 类型枚举：`CaptchaType`
- Core 服务：`ArithmeticCaptchaService`、`BlockPuzzleCaptchaService`、`ClickWordCaptchaService`、`BehaviorCaptchaService`
- Controller 路径 `/captcha`，接口包括 `/types`、`/arithmetic`、`/block-puzzle`、`/click-word`、`/behavior`、`/behavior/verify`、`/verify`

## 8. 数据库 / 初始化数据

Flyway 路径：`mango-captcha-core/src/main/resources/db/migration/captcha`。

`V1__init_captcha.sql` 创建 `captcha_code`，包含唯一键 `uk_code_key` 和过期时间索引 `idx_expire_time`。当前最小运行闭环仍以 infra-kv `IKvStore` 为准，`captcha_code` 表属于历史或预留资产，使用前需要结合代码路径确认是否被当前 store 实现消费。

## 9. 菜单 / 权限 / 租户

本模块不提供管理菜单。验证码通常作为认证或业务接口前置校验能力使用；租户和风控策略由调用方决定并写入业务上下文。

## 10. 验证方式

```bash
mvn -f mango/pom.xml -pl mango-platform/mango-captcha -am test
```

测试入口位于 `mango-captcha-core/src/test/java/io/mango/captcha/core/service/**` 和 `mango-captcha-starter/src/test/java/io/mango/captcha/starter/**`。

## 11. 业务接入最小闭环

业务接口需要验证码时，先生成验证码并把返回的 key 交给前端展示或发送，再在业务动作提交时调用 verify 校验 key 和答案。图形和行为验证码使用 `/captcha/**` HTTP 接口；短信和邮件验证码需要业务实现 `SmsProvider` 或 `EmailProvider` 并接入真实发送服务。

验证码答案存储通过 infra-kv 的 `IKvStore` 控制，业务只关心 key、过期时间和校验结果。验收断言覆盖：正确答案一次通过，错误答案失败，过期后失败，同一验证码不能被重复消费；验证码通过后仍继续执行登录、权限和风控校验。

## 12. 常见问题

- 验证失败先检查验证码 key、过期时间和存储后端是否一致。
- 短信或邮件验证码需要实现 `SmsProvider` 或 `EmailProvider`。
- 验证码通过不代表业务登录或业务操作一定通过，后续仍需业务校验。

## 13. 关联 PMO 规则

- [后端安全规范](../../../mango-pmo/rules/backend/06-security.md)
- [后端模块规范](../../../mango-pmo/rules/backend/05-module.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
