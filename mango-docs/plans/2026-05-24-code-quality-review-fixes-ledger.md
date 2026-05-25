# 2026-05-24 代码质量 Review 修复台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|----|------|------|----------|--------|----------|------|----------|
| CQ-001 | 代码质量专项 Review | `mvn mango:check -Drule=all` 不应因 PMD JDK 版本不兼容不可用 | 在现有 PMD/P3C 链路显式设置 `targetJdk=17` | PMD Maven 配置 | PMD/mango-check 命令 | Done | `mvn -pl mango-platform/mango-authorization/mango-authorization-api pmd:pmd -DskipTests` 成功；`mvn mango:check -Drule=all` 已越过 PMD targetJdk，剩余失败为既有 `aktStatus is NULL`/file-preview 依赖 |
| CQ-013 | 代码质量专项 Review | 查询不存在消息不应 NPE | service 查询后判空，Controller 404 分支可达 | notification service/test | 单测 | Done | `mvn -pl mango-platform/mango-biz-notification/mango-biz-notification-core -Dtest=NotificationServiceImplTest test` 成功 |
| CQ-014 | 代码质量专项 Review | 已读/删除消息必须限定当前用户 | Controller 调用用户限定 service 方法 | notification controller/service/test | 单测 | Done | `mvn -pl mango-platform/mango-biz-notification/mango-biz-notification-core -Dtest=NotificationServiceImplTest test` 成功 |
| CQ-019 | 代码质量专项 Review | 默认短信/邮件供应商未配置时不能发送成功 | 默认 Provider fail-closed；发送失败不写入验证码 key | captcha provider/test | 单测 | Done | `mvn -pl mango-platform/mango-captcha/mango-captcha-core -Dtest=CaptchaServiceImplTest test` 成功；`mvn -pl mango-platform/mango-captcha/mango-captcha-starter -am -Dtest=DefaultCaptchaProviderTest -Dsurefire.failIfNoSpecifiedTests=false test` 成功 |
| CQ-020 | 代码质量专项 Review | 内部调用密钥为空时不能放行内部路径 | 内部保护开启且命中内部路径时，空密钥拒绝 | internal call filter/test | 单测 | Done | `mvn -pl mango-infra/mango-infra-web/mango-infra-web-starter -Dtest=InternalCallFilterTest test` 成功 |
| CQ-031 | 代码质量专项 Review | 测试名称与真实物料一致 | Memory 物料迁移到通用测试类，Redis 测试仅用 RedisKvStore | realtime test | mango-check test-fixture | Done | `mvn mango:check -Drule=test-fixture` 成功，0 issue |
| VERIFY-001 | 本轮验证阻断 | Maven reactor 不应因未管理依赖版本无法读 POM | KV 模块改用根 POM 已管理的 `org.redisson:redisson` | KV POM | 定向 Maven 测试 | Done | 修复后所有 Maven 定向命令可进入模块编译/测试 |
| CQ-002/CQ-003/CQ-004/CQ-005/CQ-006/CQ-007/CQ-008/CQ-009/CQ-010/CQ-011/CQ-012/CQ-015/CQ-016/CQ-017/CQ-018/CQ-021/CQ-022/CQ-023/CQ-024/CQ-025/CQ-026/CQ-027/CQ-028/CQ-029/CQ-030/CQ-032 | 代码质量专项 Review | 其余架构、Flyway、前端、生成器和专项治理问题 | 本轮不混入大范围重构，保留后续专项 | 无 | 记录例外 | Deferred | `mango-docs/reviews/2026-05-24-code-quality-review.md` |
