# Issue #1074 验收证据

## 自动化验证

- `InformationSubmitProtectionTest.normalRequestIsAllowedBeforeRiskThreshold`
- `InformationSubmitProtectionTest.riskRequestRequiresCaptchaBeforeSubmission`
- `InformationSubmitProtectionTest.validCaptchaAllowsRiskRequest`
- `InformationSubmitProtectionTest.requestOverHardLimitIsRejectedWithoutCaptchaVerification`
- `InformationSubmitProtectionTest.kvFailureCanBeConfiguredFailClosed`
- `InformationPublicControllerTest.rateLimitedHttpRequestReturnsRetryAfterAndDoesNotCallSubmissionService`
- `InformationPublicControllerTest.rateLimitedRequestDoesNotCallSubmissionService`

## 验收结论

- 正常请求不强制验证码。
- 风险阈值后要求一次性验证码。
- 超过硬上限返回 429。
- 防刷拒绝发生在提交服务调用之前，不写入采集记录。
- 限流使用共享 KV，支持多实例部署。
