# Issue #1074 公开表单提交防刷技术设计

## 现象与根本问题

- 现象：公开提交入口可以被脚本持续调用，当前没有共享限流和风险验证。
- 根本问题：提交写库前缺少跨实例的请求风险判定，无法区分正常访客与异常流量。

## 方案

1. `POST /information/open/forms/submit` 先经过 `InformationSubmitProtection`，再进入提交事务。
2. 使用现有 `IKvStore` 的原子固定窗口计数，键按 IP 或 IP+表单配置，保证多实例共享口径。
3. 正常请求不要求验证码；达到风险阈值后要求现有 `CaptchaApi` 的一次性验证码。
4. 超过固定窗口硬上限直接返回 HTTP 429，并通过 `Retry-After` 告知客户端等待秒数；验证码缺失返回 428，验证码错误返回 400；所有拒绝路径均不调用提交服务。
5. KV 故障默认可用性优先（`fail-open=true`），可配置为 503；验证码服务故障不放行风险请求。

## API 接入流程

- 首次提交沿用原 JSON 请求体，不新增必填字段。
- 收到 428 后调用现有 `GET /captcha/arithmetic` 获取验证码，再用原请求体和 `X-Captcha-Key`、`X-Captcha-Code` 请求头重试。
- 400 表示验证码失效或错误，应重新获取验证码；429 表示固定窗口已满，应按 `Retry-After` 等待；503 表示保护依赖不可用。
- `serverSecret` 仅允许服务端使用，不能下发浏览器。

## 关键配置

- `mango.information.public-submit.protection.window-seconds`
- `mango.information.public-submit.protection.risk-threshold`
- `mango.information.public-submit.protection.max-requests`
- `mango.information.public-submit.protection.key-dimension`
- `mango.information.public-submit.protection.fail-open`

## 边界

- 不新增验证码实现，复用 `CaptchaApi` 和 `/captcha/*` 公共能力。
- 不在数据库中记录被拦截请求，不改变表单提交幂等语义。
- IP 取 Mango 上下文中的可信客户端 IP，缺失时回退到 Servlet remote address；不直接信任请求头中的 X-Forwarded-For。
