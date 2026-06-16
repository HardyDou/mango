# Mango Captcha 使用说明

`mango-captcha` 是 Mango 的验证码能力。业务系统用它生成和校验图形验证码、滑块验证码、点选文字验证码、无感行为验证码、短信验证码和邮件验证码。

验证码模块只负责验证码的生成、存储、校验和短信/邮件发送 SPI。登录、注册、找回密码、支付确认等业务流程，需要在自己的业务接口里决定什么时候要求验证码，以及验证码通过后执行什么业务动作。

## 1. 概览

`mango-captcha` 对外提供两类入口：

- 公共 HTTP 接口：`/captcha/**`，用于前端获取图形验证码、滑块验证码、点选文字验证码、无感行为验证码并提交校验。
- Java API：`CaptchaApi`，用于业务后端生成、校验、发送短信验证码和邮件验证码。

短信和邮件发送没有直接暴露在 `CaptchaController` 的 `/captcha/**` 下。当前登录场景的发送入口在 `mango-auth`：`POST /auth/captcha/send`，它内部调用 `CaptchaApi.send()`。

## 2. 功能清单

| 能力 | 用途 | 常用入口 |
|------|------|----------|
| 验证码类型查询 | 前端判断后端支持哪些验证码 | `GET /captcha/types` |
| 算术验证码 | 登录、低风险表单的人机校验 | `GET /captcha/arithmetic`、`POST /captcha/verify` |
| 滑块验证码 | 拖动滑块拼图校验 | `GET /captcha/block-puzzle`、`POST /captcha/verify` |
| 点选文字验证码 | 按提示点击图片文字 | `GET /captcha/click-word`、`POST /captcha/verify` |
| 无感行为验证 | 前端采集行为数据，后端给出风险评分 | `GET /captcha/behavior`、`POST /captcha/behavior/verify` |
| 短信验证码 | 登录、注册、找回密码、换绑手机号 | `CaptchaApi.sendSms()`、`CaptchaApi.send()` |
| 邮件验证码 | 邮箱登录、换绑邮箱、邮件确认 | `CaptchaApi.sendEmail()`、`CaptchaApi.send()` |
| 验证码拦截 | 认证模块按路径要求请求头验证码 | `X-Captcha-Key`、`X-Captcha-Code`、`X-Captcha-Type` |

## 3. 后端接入

### 3.1 开发依赖

业务后端需要生成、发送或校验验证码时，引入 API 契约：

```xml
<dependency>
    <groupId>io.mango.platform.captcha</groupId>
    <artifactId>mango-captcha-api</artifactId>
</dependency>
```

常用 Java API：

| API | 用途 |
|-----|------|
| `CaptchaApi.generate(CaptchaType, String)` | 生成指定类型验证码。 |
| `CaptchaApi.verify(CaptchaVerifyRequest)` | 校验验证码。校验成功后删除验证码。 |
| `CaptchaApi.verifyBehavior(CaptchaVerifyRequest)` | 校验无感行为验证码并返回评分详情。 |
| `CaptchaApi.sendSms(String, String, long)` | 发送短信验证码。 |
| `CaptchaApi.sendEmail(String, String, long)` | 发送邮件验证码。 |
| `CaptchaApi.send(CaptchaSendRequest)` | 统一发送短信或邮件验证码。 |
| `CaptchaApi.getSupportedTypes()` | 查询支持的验证码类型。 |
| `CaptchaApi.getCurrentStorage()` | 查询当前验证码存储实现。 |

业务接口校验短信验证码：

```java
CaptchaVerifyRequest request = new CaptchaVerifyRequest();
request.setKey(captchaKey);
request.setType(CaptchaType.SMS);
request.setCode(userInputCode);

boolean passed = captchaApi.verify(request);
Require.isTrue(passed, CaptchaCode.CAPTCHA_INVALID);
```

发送短信验证码：

```java
CaptchaSendRequest request = new CaptchaSendRequest();
request.setType(CaptchaType.SMS);
request.setTarget(mobile);
request.setBusinessType("LOGIN");
request.setExpireSeconds(300L);

String captchaKey = captchaApi.send(request);
```

### 3.2 部署依赖

提供验证码能力的应用引入 `mango-captcha-starter`：

```xml
<dependency>
    <groupId>io.mango.platform.captcha</groupId>
    <artifactId>mango-captcha-starter</artifactId>
</dependency>
```

`mango-captcha-starter` 会注册：

- `CaptchaController`，提供 `/captcha/**` 公共接口。
- `CaptchaApi` 默认实现。
- 算术、滑块、点选文字、无感行为验证码服务。
- 默认 `SmsProvider`、`EmailProvider`。

验证码值存储依赖 `IKvStore`。实际存储由 `mango-infra-kv` 提供；有 Redis 时可走 Redis，没有 Redis 时按 KV 模块配置使用其他实现。

## 4. 前端接入

前端公共封装在 `@mango/common`：

```ts
import {
  CaptchaType,
  generateArithmetic,
  generateBlockPuzzle,
  generateClickWord,
  generateBehavior,
  verifyCaptcha,
  verifyBehaviorCaptcha,
} from '@mango/common/api/captcha';
```

算术验证码：

```ts
const captcha = await generateArithmetic();

await verifyCaptcha({
  key: captcha.key,
  type: CaptchaType.ARITHMETIC,
  code: userInput,
});
```

滑块验证码：

```ts
const captcha = await generateBlockPuzzle();

await verifyCaptcha({
  key: captcha.key,
  type: CaptchaType.BLOCK_PUZZLE,
  pointJson: JSON.stringify({ x: sliderX }),
});
```

登录页获取验证码使用 `@mango/auth`，它通过 `@mango/common/api/captcha` 调用 `/captcha/arithmetic`。短信和邮件登录调用的是 `POST /auth/captcha/send`，该接口属于 `mango-auth`，不是 `mango-captcha` 的 HTTP controller。

`@mango/common` 包内提供验证码组件；这些组件是业务可复用组件，不依赖 Admin Pages。

## 5. 快速开始

### 5.1 登录页使用算术验证码

1. 后端应用启用 `mango-captcha-starter` 和 KV 能力。
2. 前端登录页调用 `generateArithmetic()` 获取 `key` 和 `image`。
3. 用户输入答案后，登录请求携带 `captchaKey` 和 `captchaCode`。
4. 认证模块调用 `CaptchaApi.verify()` 校验，通过后继续登录。

### 5.2 业务接口强制验证码请求头

认证模块的验证码拦截器读取以下请求头：

```http
X-Captcha-Key: <captcha key>
X-Captcha-Code: <user input>
X-Captcha-Type: ARITHMETIC
```

路径需要验证码但没有请求头时返回 HTTP `428`。验证码错误时返回 HTTP `400`。连续失败次数过多时返回 HTTP `429`。

### 5.3 接入短信或邮件供应商

默认 `SmsProvider`、`EmailProvider` 只是默认实现。生产环境需要在业务应用中提供自己的 Bean：

```java
@Bean
public SmsProvider smsProvider() {
    return (mobile, templateCode, code) -> smsClient.send(mobile, code);
}
```

邮件同理提供 `EmailProvider`。业务模块调用 `CaptchaApi.send()` 后，把返回的 `captchaKey` 给前端保存，后续提交验证码时带回。

## 6. 配置说明

```yaml
mango:
  captcha:
    ttl: 300
    storage: auto
    arithmetic:
      enabled: true
      width: 120
      height: 40
    block-puzzle:
      enabled: true
      width: 280
      height: 160
      slider-size: 50
      image-locations:
        - classpath:captcha/block-puzzle/workspace.jpg
    click-word:
      enabled: true
      width: 320
      height: 180
      word-count: 4
      target-count: 3
      tolerance: 24
    sms:
      enabled: true
      length: 6
      period: 60
      provider: default
    email:
      enabled: true
      length: 6
      provider: default
  persistence:
    flyway:
      modules:
        captcha:
          enabled: true
```

`enabled`、`provider`、`storage` 字段当前主要作为配置表达和扩展预留；实际 Bean 是否注册以 starter 自动配置和业务自定义 Bean 为准。

## 7. YAML 配置字段

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `mango.captcha.ttl` | `300` | 默认有效期，单位秒。算术、滑块和通用发送接口默认使用它。 |
| `mango.captcha.storage` | `auto` | 存储策略配置字段。当前默认实现实际通过 `IKvStore` 写入验证码值。 |
| `mango.captcha.arithmetic.enabled` | `true` | 算术验证码启用标识。 |
| `mango.captcha.arithmetic.width` | `120` | 算术验证码图片宽度。 |
| `mango.captcha.arithmetic.height` | `40` | 算术验证码图片高度。 |
| `mango.captcha.block-puzzle.enabled` | `true` | 滑块验证码启用标识。 |
| `mango.captcha.block-puzzle.width` | `280` | 滑块背景图生成宽度。 |
| `mango.captcha.block-puzzle.height` | `160` | 滑块背景图生成高度。 |
| `mango.captcha.block-puzzle.slider-size` | `50` | 滑块拼图片尺寸。 |
| `mango.captcha.block-puzzle.image-locations` | 空 | 自定义滑块图库。支持 `classpath:`、`file:`、`http:`、`https:`；为空使用内置图库。 |
| `mango.captcha.click-word.enabled` | `true` | 点选文字验证码启用标识。 |
| `mango.captcha.click-word.width` | `320` | 点选图片宽度。 |
| `mango.captcha.click-word.height` | `180` | 点选图片高度。 |
| `mango.captcha.click-word.word-count` | `4` | 图片上绘制的文字数量。 |
| `mango.captcha.click-word.target-count` | `3` | 用户需要点击的目标文字数量。 |
| `mango.captcha.click-word.tolerance` | `24` | 点选坐标容忍范围，单位像素。 |
| `mango.captcha.sms.enabled` | `true` | 短信验证码启用标识。 |
| `mango.captcha.sms.length` | `6` | 短信验证码数字长度。 |
| `mango.captcha.sms.period` | `60` | 短信发送间隔配置字段，单位秒。 |
| `mango.captcha.sms.provider` | `default` | 短信供应商配置字段。生产环境应提供 `SmsProvider` Bean。 |
| `mango.captcha.email.enabled` | `true` | 邮件验证码启用标识。 |
| `mango.captcha.email.length` | `6` | 邮件验证码数字长度。 |
| `mango.captcha.email.provider` | `default` | 邮件供应商配置字段。生产环境应提供 `EmailProvider` Bean。 |
| `mango.persistence.flyway.modules.captcha.enabled` | 无全局默认 | 是否执行 captcha 模块迁移。 |

## 8. 运行时配置字段

`CaptchaVerifyRequest`：

| 字段 | 说明 |
|------|------|
| `key` | 验证码键。生成或发送验证码后返回，校验时必传。 |
| `type` | 验证码类型。可为空；服务端会按参数推断部分类型。 |
| `code` | 用户输入的文字、数字、短信或邮件验证码。 |
| `pointJson` | 滑块、点选文字、无感行为验证提交的坐标或行为数据。 |

`CaptchaSendRequest`：

| 字段 | 说明 |
|------|------|
| `type` | `SMS` 或 `EMAIL`。 |
| `target` | 手机号或邮箱。 |
| `businessType` | 业务类型，例如 `LOGIN`、`REGISTER`、`FORGOT_PASSWORD`、`CHANGE_MOBILE`。 |
| `expireSeconds` | 有效期，单位秒，默认 `300`。 |

## 9. 返回字段

`CaptchaResponse`：

| 字段 | 说明 |
|------|------|
| `key` | 验证码键。 |
| `type` | 验证码类型。 |
| `image` | 算术或点选文字验证码图片，Base64 data URL。 |
| `backgroundImage` | 滑块背景图，Base64 data URL。 |
| `sliderImage` | 滑块拼图片，Base64 data URL。 |
| `backgroundWidth` | 滑块背景图生成宽度。 |
| `backgroundHeight` | 滑块背景图生成高度。 |
| `sliderSize` | 滑块拼图片尺寸。 |
| `x` | 滑块目标 X 坐标。生成响应中存在，前端不要把它展示给用户。 |
| `y` | 滑块目标 Y 坐标，用于渲染拼图块。 |
| `expireTime` | 过期时间，单位秒。 |
| `target` | 点选文字提示或短信/邮件目标。 |
| `extra` | 额外数据。算术验证码当前会返回答案，生产登录链路不要依赖前端自行判定，应以服务端校验为准。 |

`BehaviorCaptchaVerifyResult`：

| 字段 | 说明 |
|------|------|
| `key` | 验证码键。 |
| `score` | `0.0` 到 `1.0` 的行为评分，分数越高越像真人。 |
| `passed` | 是否通过。 |
| `riskLevel` | `LOW`、`MEDIUM`、`HIGH`。 |
| `suggestAction` | `ALLOW`、`SECONDARY_VERIFY`、`DENY`。 |
| `reason` | 评分原因。 |

## 10. 管理入口

`mango-captcha` 没有独立管理页面。它的使用入口在业务流程里：

| 入口 | 说明 |
|------|------|
| `/captcha/**` | 公共验证码生成和校验接口。 |
| `/auth/captcha/send` | auth 模块提供的短信/邮件验证码发送入口。 |
| `CaptchaApi` | 后端业务代码生成、发送和校验验证码。 |
| `@mango/common/api/captcha` | 前端验证码 API 封装。 |
| `@mango/common` 验证码组件 | 前端验证码组件。 |

`/captcha/**` 标记为公共 API，不需要登录态。

## 11. 数据与初始化

| 数据 | 来源 | 说明 |
|------|------|------|
| `captcha_code` | `db/migration/captcha/V1__init_captcha.sql` | 验证码表。当前默认实现主要通过 `IKvStore` 存储验证码值；启用 DB 存储时使用该表。 |
| 内置滑块图库 | `mango-captcha-core/src/main/resources/captcha/block-puzzle` | 滑块验证码默认背景图。 |
| 模块声明 | `META-INF/mango/module.properties` | `module-name=mango-captcha`、`module-path=/captcha`。 |

captcha 模块不初始化菜单和权限。登录页、认证拦截和验证码发送入口由 auth 模块提供。

## 12. 问题排查

| 现象 | 常见原因 | 处理 |
|------|----------|------|
| `/captcha/arithmetic` 404 | 应用没有引入 `mango-captcha-starter` | 在提供验证码的应用引入 starter。 |
| 生成后校验总失败 | `key` 过期、传错 `type`、提交字段不匹配 | 检查 `ttl`、`key`、`code`、`pointJson`。 |
| 登录接口返回 428 | 该路径要求验证码但没有请求头 | 先获取验证码，再带 `X-Captcha-Key`、`X-Captcha-Code`、`X-Captcha-Type`。 |
| 短信或邮件发送返回空 | 没有可用 `SmsProvider` 或 `EmailProvider` | 在业务应用提供生产供应商 Bean。 |
| `/auth/captcha/send` 不存在 | 没有启用 auth 模块 | 引入并启用 `mango-auth-starter`，或业务模块自己提供发送入口调用 `CaptchaApi.send()`。 |
| 滑块背景图加载失败 | 自定义 `image-locations` 路径不可读 | 检查 `classpath:`、`file:` 或 URL 路径；为空时使用内置图库。 |
| 多实例校验失败 | 验证码存储不是共享存储 | 使用共享 KV 存储，例如 Redis。 |

## 13. 相关文档

- [Auth 使用说明](../mango-auth/README.md)
- [@mango/common 前端包](../../../mango-ui/packages/common/README.md)
- [能力文档维护规则](../../../mango-pmo/rules/08-capability-docs.md)
