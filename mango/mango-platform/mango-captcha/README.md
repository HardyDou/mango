# Mango Captcha 使用说明

`mango-captcha` 是 Mango 的验证码能力。业务系统用它生成和校验图形验证码、滑块验证码、点选文字验证码、无感行为验证码、短信验证码和邮件验证码。

验证码模块只负责验证码的生成、存储、校验和短信/邮件发送 SPI。登录、注册、找回密码、支付确认等业务流程，需要在自己的业务接口里决定什么时候要求验证码，以及验证码通过后执行什么业务动作。

## 1. 概览

`mango-captcha` 对外提供三类入口：

- 公共 HTTP 接口：`/captcha/types`、各类生成接口和校验接口。
- 内部 HTTP 接口：`POST /captcha/send`，只允许 Mango 内部调用发送短信或邮件验证码。
- Java API：`CaptchaApi`，所有方法统一返回 `R<T>`，用于业务后端生成、校验和发送验证码。

当前登录场景的公开发送入口在 `mango-auth`：`POST /auth/captcha/send`，它内部调用 `CaptchaApi.send()`；客户端不能直接访问内部的 `POST /captcha/send`。

## 2. 功能清单

| 能力 | 用途 | 常用入口 |
|------|------|----------|
| 验证码类型查询 | 前端判断后端支持哪些验证码 | `GET /captcha/types` |
| 图形验证码 | 按统一默认策略生成字母数字/算术验证码 | `GET /captcha/arithmetic`、`POST /captcha/verify` |
| 图形验证码兼容入口 | 已接入消费者继续使用；新接入不建议使用 | `GET /captcha/graphic`、`POST /captcha/verify` |
| 滑块验证码 | 拖动滑块拼图校验 | `GET /captcha/block-puzzle`、`POST /captcha/verify` |
| 点选文字验证码 | 按提示点击图片文字 | `GET /captcha/click-word`、`POST /captcha/verify` |
| 无感行为验证 | 前端采集行为数据，后端给出风险评分 | `GET /captcha/behavior`、`POST /captcha/behavior/verify` |
| 短信验证码 | 登录、注册、找回密码、换绑手机号 | `CaptchaApi.send()`、`POST /auth/captcha/send` |
| 内置短信通道 | 阿里云、腾讯云短信通道，凭证与账号配置全部来自配置项（`${ENV}` 注入） | `mango.captcha.sms.provider=aliyun\|tencent` |
| 邮件验证码 | 邮箱登录、换绑邮箱、邮件确认 | `CaptchaApi.send()`、`POST /auth/captcha/send` |
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
| `CaptchaApi.getTypes()` | 查询支持的验证码类型和实际存储实现。 |
| `CaptchaApi.generateArithmetic()` | 按统一默认策略生成图形验证码，推荐使用。 |
| `CaptchaApi.generateGraphic(type, difficulty)` | 已接入消费者的兼容入口；新接入不建议使用。 |
| `CaptchaApi.generateBlockPuzzle()` | 生成滑块验证码。 |
| `CaptchaApi.generateClickWord()` | 生成点选文字验证码。 |
| `CaptchaApi.generateBehavior()` | 生成行为验证码挑战。 |
| `CaptchaApi.verify(CaptchaVerifyRequest)` | 校验验证码；成功后删除一次性答案。 |
| `CaptchaApi.verifyBehavior(CaptchaVerifyRequest)` | 校验行为验证码并返回评分详情。 |
| `CaptchaApi.send(CaptchaSendRequest)` | 内部发送短信或邮件验证码。 |

业务接口校验短信验证码：

```java
CaptchaVerifyRequest request = new CaptchaVerifyRequest();
request.setKey(captchaKey);
request.setType(CaptchaType.SMS);
request.setCode(userInputCode);

R<Boolean> verification = captchaApi.verify(request);
Require.isTrue(verification.isSuccess() && Boolean.TRUE.equals(verification.getData()),
        CaptchaCode.CAPTCHA_INVALID);
```

发送短信验证码：

```java
CaptchaSendRequest request = new CaptchaSendRequest();
request.setType(CaptchaType.SMS);
request.setTarget(mobile);
request.setBusinessType("LOGIN");
request.setExpireSeconds(300L);

R<String> result = captchaApi.send(request);
String captchaKey = result.getData();
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
- 可选的内置短信通道 `AliyunSmsProvider`、`TencentSmsProvider`，仅在 `mango.captcha.sms.provider` 为 `aliyun` 或 `tencent` 时装配，凭证与账号信息来自 `mango.captcha.sms.aliyun.*` / `mango.captcha.sms.tencent.*` 配置项。

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

推荐使用统一图形验证码：

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

登录页和新业务统一调用 `/captcha/arithmetic`，省略题型和难度时使用服务端全局默认策略。`/captcha/graphic` 仅作为已接入消费者的兼容入口，新接入不建议使用。短信和邮件登录调用的是 `POST /auth/captcha/send`，该接口属于 `mango-auth`，不是 `mango-captcha` 的 HTTP controller。

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

开发和测试环境可以启用固定短信验证码，跳过短信供应商并把配置值保存到验证码会话中，仍使用正常的发送和校验接口：

```yaml
mango:
  captcha:
    sms:
      fixed-code-enabled: true
      fixed-code: "0000"
```

也可以使用环境变量 `MANGO_CAPTCHA_SMS_FIXED_CODE_ENABLED=true` 和
`MANGO_CAPTCHA_SMS_FIXED_CODE=0000`。生产环境保持 `fixed-code-enabled: false`（默认值），继续使用真实 `SmsProvider`。

固定验证码只作用于 `SMS`，邮件验证码和图形验证码不受影响；固定值必须为纯数字。

```java
@Bean
public SmsProvider smsProvider() {
    return (mobile, templateCode, code) -> smsClient.send(mobile, code);
}
```

邮件同理提供 `EmailProvider`。业务模块调用 `CaptchaApi.send()` 后，把返回的 `captchaKey` 给前端保存，后续提交验证码时带回。

### 5.4 内置阿里云/腾讯云短信通道

`mango-captcha-starter` 内置阿里云、腾讯云短信通道。凭证、签名、地域全部由 Spring 配置项提供，**不依赖应用集成**（不需要 `mango-integration`），凭证也不入库。

1. 在配置中选定通道并填写必填项（见下表）。
2. 凭证只允许写成部署环境的 `${ENV}` 占位符，例如 `${ALIYUN_SMS_AK}`；禁止把明文 AccessKey/SecretKey 提交到代码仓库或配置中心模板。
3. 配置在启动时绑定，修改配置后需要**重启**应用才生效。

```yaml
mango:
  captcha:
    sms:
      provider: aliyun          # default | aliyun | tencent
      template-code: SMS_100001 # 第三方短信模板编码，aliyun/tencent 必填
      aliyun:
        access-key-id: ${ALIYUN_SMS_AK}
        access-key-secret: ${ALIYUN_SMS_SK}
        sign-name: ${ALIYUN_SMS_SIGN_NAME}
        endpoint: dysmsapi.aliyuncs.com   # 默认值
        region: cn-hangzhou               # 默认值
```

腾讯云通道把 `provider` 改为 `tencent`，并填写 `tencent.*`：

```yaml
mango:
  captcha:
    sms:
      provider: tencent
      template-code: "100001"
      tencent:
        secret-id: ${TENCENT_SMS_SECRET_ID}
        secret-key: ${TENCENT_SMS_SECRET_KEY}
        sign-name: ${TENCENT_SMS_SIGN_NAME}
        sdk-app-id: ${TENCENT_SMS_SDK_APP_ID}
        region: ap-guangzhou              # 默认值
        endpoint: sms.tencentcloudapi.com # 默认值
        country-code: "+86"               # 默认值
```

| 通道 | 必填项 | 可选项（默认值） |
|------|--------|------------------|
| 公共 | `mango.captcha.sms.template-code` | — |
| 阿里云 | `sms.aliyun.access-key-id`、`sms.aliyun.access-key-secret`、`sms.aliyun.sign-name` | `sms.aliyun.endpoint`（`dysmsapi.aliyuncs.com`）、`sms.aliyun.region`（`cn-hangzhou`） |
| 腾讯云 | `sms.tencent.secret-id`、`sms.tencent.secret-key`、`sms.tencent.sign-name`、`sms.tencent.sdk-app-id` | `sms.tencent.region`（`ap-guangzhou`）、`sms.tencent.endpoint`（`sms.tencentcloudapi.com`）、`sms.tencent.country-code`（`+86`） |

参数映射：阿里云模板参数以 `{"code":"<验证码>"}` 传入；腾讯云模板参数按模板顺序传入，第一个参数为验证码。手机号本身不做格式改写；腾讯云按 `countryCode + mobile` 拼接，`country-code` 缺少 `+` 时自动补齐，非法值按 `2410` 失败。

已知限制：

- **一个部署一套短信账号**：`provider` 只能二选一，不支持每个租户使用不同账号；多账号需求请自行提供 `SmsProvider` Bean（自定义 Bean 始终优先于内置 Bean）。
- **凭证明文存在于部署配置**：靠环境变量注入与部署规范保护，没有加密入库环节。
- **改配置需重启**：配置项在启动时绑定，不支持热更新。
- `@ConditionalOnClass` 守卫两家 SDK：下游排除 SDK 依赖时对应供应商不装配，应用仍可启动，发送按 `2410` 失败。

发送失败统一抛出 `CaptchaCode.CAPTCHA_SEND_FAILED`（`2410`），不保存验证码、不返回 `captchaKey`。常见原因包括：`provider` 取值不匹配任何内置供应商、缺少凭证/签名/`sdk-app-id` 等必填配置、`template-code` 为空、第三方返回非 `OK`。日志只记录 `provider`、`templateCode`、缺失的配置项键名（`property=`）和第三方错误码，不输出凭证值和完整手机号。固定验证码模式和 `provider=default` 行为不变；邮件通道失败仍按原有语义返回空值。

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
    graphic:
      default-type: ALPHANUMERIC
      default-level: SIMPLE
      allowed-types:
        - ARITHMETIC
        - ALPHANUMERIC
      allowed-levels:
        - SIMPLE
        - MEDIUM
        - COMPLEX
      width: 120
      height: 40
      interfere-count: 1
      characters: ABCDEFGHJKLMNPQRSTUVWXYZ
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
      template-code: ""
      fixed-code-enabled: false
      fixed-code: ""
      aliyun:
        access-key-id: ""
        access-key-secret: ""
        sign-name: ""
        endpoint: dysmsapi.aliyuncs.com
        region: cn-hangzhou
      tencent:
        secret-id: ""
        secret-key: ""
        sign-name: ""
        sdk-app-id: ""
        region: ap-guangzhou
        endpoint: sms.tencentcloudapi.com
        country-code: "+86"
    email:
      enabled: true
      length: 6
      provider: default
```

`enabled`、`provider`、`storage` 字段为兼容保留配置。实际验证码存储始终由应用注入的 `IKvStore` 决定，`storage` 字段不会另建数据库存储。

## 7. YAML 配置字段

| 配置 | 默认值 | 说明 |
|------|--------|------|
| `mango.captcha.ttl` | `300` | 默认有效期，单位秒。算术、滑块和通用发送接口默认使用它。 |
| `mango.captcha.storage` | `auto` | 兼容保留字段；实际存储实现由注入的 `IKvStore` 决定。 |
| `mango.captcha.arithmetic.enabled` | `true` | 兼容保留的算术验证码启用标识。 |
| `mango.captcha.arithmetic.width` | `120` | 算术验证码图片宽度。 |
| `mango.captcha.arithmetic.height` | `40` | 算术验证码图片高度。 |
| `mango.captcha.graphic.default-type` | `ALPHANUMERIC` | 统一图形验证码默认题型；`/captcha/arithmetic` 和 `/captcha/graphic` 共用。 |
| `mango.captcha.graphic.default-level` | `SIMPLE` | 默认难度；匿名会话不能低于该级别。 |
| `mango.captcha.graphic.allowed-types` | `ARITHMETIC,ALPHANUMERIC` | 会话允许选择的题型。 |
| `mango.captcha.graphic.allowed-levels` | `SIMPLE,MEDIUM,COMPLEX` | 会话允许选择的难度。 |
| `mango.captcha.graphic.width` | `120` | 字母数字图形验证码图片宽度。 |
| `mango.captcha.graphic.height` | `40` | 字母数字图形验证码图片高度。 |
| `mango.captcha.graphic.interfere-count` | `1` | 统一图形验证码干扰线数量；算术兼容入口也使用该配置。 |
| `mango.captcha.graphic.characters` | `ABCDEFGHJKLMNPQRSTUVWXYZ` | 默认英文字母字符集；排除易混淆字符。 |
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
| `mango.captcha.sms.provider` | `default` | 短信供应商。`default` 使用空实现供应商（发送明确失败）；`aliyun`、`tencent` 装配内置通道，需要配合 `template-code` 与对应的 `aliyun.*` / `tencent.*` 凭证。取值不匹配任何内置供应商时不注册 `SmsProvider`，发送按 `2410` 失败。 |
| `mango.captcha.sms.template-code` | 空 | 第三方短信模板编码。`provider=aliyun` 或 `provider=tencent` 时必填。该键由 `CaptchaService` 通过 `@Value` 读取后显式传给 `SmsProvider` 的第二个参数；`CaptchaProperties` 中同名字段只作为配置载体，供应商不再读取该键做回退。 |
| `mango.captcha.sms.aliyun.access-key-id` | 空 | 阿里云 AccessKey ID，仅允许 `${ENV}` 占位符注入。`provider=aliyun` 时必填。 |
| `mango.captcha.sms.aliyun.access-key-secret` | 空 | 阿里云 AccessKey Secret，仅允许 `${ENV}` 占位符注入。`provider=aliyun` 时必填。 |
| `mango.captcha.sms.aliyun.sign-name` | 空 | 阿里云短信签名。`provider=aliyun` 时必填。 |
| `mango.captcha.sms.aliyun.endpoint` | `dysmsapi.aliyuncs.com` | 阿里云短信接口地址。 |
| `mango.captcha.sms.aliyun.region` | `cn-hangzhou` | 阿里云短信地域。 |
| `mango.captcha.sms.tencent.secret-id` | 空 | 腾讯云 SecretId，仅允许 `${ENV}` 占位符注入。`provider=tencent` 时必填。 |
| `mango.captcha.sms.tencent.secret-key` | 空 | 腾讯云 SecretKey，仅允许 `${ENV}` 占位符注入。`provider=tencent` 时必填。 |
| `mango.captcha.sms.tencent.sign-name` | 空 | 腾讯云短信签名。`provider=tencent` 时必填。 |
| `mango.captcha.sms.tencent.sdk-app-id` | 空 | 腾讯云短信应用 ID（SdkAppId）。`provider=tencent` 时必填。 |
| `mango.captcha.sms.tencent.region` | `ap-guangzhou` | 腾讯云短信地域。 |
| `mango.captcha.sms.tencent.endpoint` | `sms.tencentcloudapi.com` | 腾讯云短信接口地址。 |
| `mango.captcha.sms.tencent.country-code` | `+86` | 腾讯云短信国家码，与手机号拼接后发送；缺少 `+` 时自动补齐，非法值按 `2410` 失败。 |
| `mango.captcha.sms.fixed-code-enabled` | `false` | 是否启用固定短信验证码；开发/测试环境可设为 `true`，生产环境保持 `false`。 |
| `mango.captcha.sms.fixed-code` | 空 | 固定短信验证码值；启用固定验证码时必须为纯数字。 |
| `mango.captcha.email.enabled` | `true` | 邮件验证码启用标识。 |
| `mango.captcha.email.length` | `6` | 邮件验证码数字长度。 |
| `mango.captcha.email.provider` | `default` | 邮件供应商配置字段。生产环境应提供 `EmailProvider` Bean。 |

## 8. 运行时配置字段

`CaptchaVerifyRequest`：

| 字段 | 说明 |
|------|------|
| `key` | 验证码键。生成或发送验证码后返回，校验时必传。 |
| `type` | 验证码类型。图形验证码校验使用 `GRAPHIC`。 |
| `code` | 用户输入的文字、数字、短信或邮件验证码。 |
| `pointJson` | 滑块、点选文字、无感行为验证提交的坐标或行为数据。 |

`GET /captcha/graphic` 查询参数（兼容入口，新接入不建议使用）：

| 参数 | 说明 |
|------|------|
| `type` | `ARITHMETIC` 或 `ALPHANUMERIC`，为空使用全局默认题型。 |
| `level` | `SIMPLE`、`MEDIUM` 或 `COMPLEX`，为空使用全局默认级别；低于默认级别时不会降级。 |

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
| `type` | 验证码类型。统一图形验证码返回 `GRAPHIC`。 |
| `graphicType` | 统一图形验证码实际题型。 |
| `difficulty` | 统一图形验证码实际难度。 |
| `image` | 算术、字母数字或点选文字验证码图片，Base64 data URL。 |
| `backgroundImage` | 滑块背景图，Base64 data URL。 |
| `sliderImage` | 滑块拼图片，Base64 data URL。 |
| `backgroundWidth` | 滑块背景图生成宽度。 |
| `backgroundHeight` | 滑块背景图生成高度。 |
| `sliderSize` | 滑块拼图片尺寸。 |
| `x` | 滑块目标 X 坐标。生成响应中存在，前端不要把它展示给用户。 |
| `y` | 滑块目标 Y 坐标，用于渲染拼图块。 |
| `expireTime` | 过期时间，单位秒。 |
| `target` | 点选文字提示或短信/邮件目标。 |
| `extra` | 额外数据。公开图形验证码接口不返回答案；答案只保存在服务端会话中。 |

`BehaviorCaptchaVerifyResponse`：

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
| `/captcha/types`、生成和校验路径 | 公共验证码生成和校验接口。 |
| `/captcha/send` | 仅内部调用的短信/邮件发送接口。 |
| `/auth/captcha/send` | auth 模块提供的短信/邮件验证码发送入口。 |
| `CaptchaApi` | 后端业务代码生成、发送和校验验证码。 |
| `@mango/common/api/captcha` | 前端验证码 API 封装。 |
| `@mango/common` 验证码组件 | 前端验证码组件。 |

验证码查询、生成和校验接口为公共 API，不需要登录态；`POST /captcha/send` 为内部 API，必须通过内部调用认证。

## 11. 数据与初始化

| 数据 | 来源 | 说明 |
|------|------|------|
| 内置滑块图库 | `mango-captcha-core/src/main/resources/captcha/block-puzzle` | 滑块验证码默认背景图。 |
| 模块声明 | `META-INF/mango/module.properties` | `module-name=mango-captcha`、`module-path=/captcha`。 |

captcha 模块没有 DDL、演示数据、菜单或权限初始化。验证码和挑战数据全部通过 `IKvStore` 保存；登录页、认证拦截和公开发送入口由 auth 模块提供。

## 12. 问题排查

| 现象 | 常见原因 | 处理 |
|------|----------|------|
| `/captcha/arithmetic` 404 | 应用没有引入 `mango-captcha-starter` | 在提供验证码的应用引入 starter。 |
| 生成后校验总失败 | `key` 过期、传错 `type`、提交字段不匹配 | 检查 `ttl`、`key`、`code`、`pointJson`。 |
| 登录接口返回 428 | 该路径要求验证码但没有请求头 | 先获取验证码，再带 `X-Captcha-Key`、`X-Captcha-Code`、`X-Captcha-Type`。 |
| 短信发送报错 `2410` | `provider` 取值不匹配任何内置供应商、缺少凭证/`signName`/`sdkAppId` 等必填配置、`template-code` 为空，或第三方返回非 `OK` | 查看日志中的 `provider`、`templateCode`、缺失的配置项键名（`property=`）与第三方错误码；补齐配置并重启应用。 |
| 邮件验证码发送返回空 | 没有可用 `EmailProvider` | 在业务应用提供邮件供应商 Bean。 |
| `/auth/captcha/send` 不存在 | 没有启用 auth 模块 | 引入并启用 `mango-auth-starter`，或业务模块自己提供发送入口调用 `CaptchaApi.send()`。 |
| 滑块背景图加载失败 | 自定义 `image-locations` 路径不可读 | 检查 `classpath:`、`file:` 或 URL 路径；为空时使用内置图库。 |
| 多实例校验失败 | 验证码存储不是共享存储 | 使用共享 KV 存储，例如 Redis。 |

## 13. 相关文档

- [Auth 使用说明](../mango-auth/README.md)
- [@mango/common 前端包](../../../mango-ui/packages/common/README.md)
- [能力文档维护规则](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 升级与兼容

- Unreleased（当前 `1.0.0-SNAPSHOT`）：新增内置阿里云/腾讯云短信通道（`mango.captcha.sms.provider=aliyun|tencent`，凭证与账号配置全部来自 `mango.captcha.sms.aliyun.*` / `mango.captcha.sms.tencent.*` 配置项，由部署环境 `${ENV}` 注入，不依赖 `mango-integration`），短信发送失败改为抛出 `CaptchaCode.CAPTCHA_SEND_FAILED`（`2410`）且不再返回空 `captchaKey`。`SmsProvider` SPI 签名不变，`provider=default`、固定验证码和邮件通道行为保持兼容；回滚时把 `provider` 改回 `default`（或移除内置通道配置）即可，配置项残留不影响其它模块。

- `1.1.2`：新增统一图形验证码 API、题型/难度配置和 `GraphicCaptcha` 前端组件。旧算术、滑块、点选、行为、短信和邮件验证码保持兼容；使用新能力的消费者需要同时升级 Captcha Maven 依赖与 `@mango/common`，验证 `/captcha/graphic` 和 `/captcha/verify`，回滚时移除新入口并恢复上一版本依赖。
