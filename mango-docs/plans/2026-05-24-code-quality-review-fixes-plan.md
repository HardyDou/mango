# 2026-05-24 代码质量 Review 修复计划

## 1. 背景

依据 `mango-docs/reviews/2026-05-24-code-quality-review.md` 的问题登记，先处理可独立验证且风险收敛的高优先级缺陷。

## 2. 目标

- 恢复 PMD 在 JDK 21 编译环境下的可执行性。
- 修复通知模块查询空指针和用户消息写操作越权风险。
- 修复验证码默认短信、邮件供应商未配置时发送成功的 fail-open 行为。
- 修复内部调用保护在空密钥时放行的 fail-open 行为。
- 修复 realtime presence 测试名称与实际测试物料不一致。

## 3. 范围

- `mango/pom.xml`
- `mango/mango-parent/pom.xml`
- `mango/mango-common/pom.xml`
- `mango/mango-platform/mango-biz-notification`
- `mango/mango-platform/mango-captcha`
- `mango/mango-infra/mango-infra-web`
- `mango/mango-infra/mango-infra-test`

## 4. 不在本轮处理

- `core` 依赖 `starter`、API 契约收敛、Controller 模块迁移等跨模块架构债务。
- Flyway 历史 migration 改写、跨域 seed、未跟踪 SQL 的最终治理。
- 前端菜单路由、ID 类型、富文本文件 URL、monorepo 反向依赖等前端专项治理。
- Workflow 占位接口、AI 历史保存、numgen 异常日志、配置密钥治理、CRUD 生成器映射等其它问题。

## 5. 改动设计

- PMD 保持现有 P3C 2.1.1 规则链，显式配置 `targetJdk=17`，避免 PMD 6 不支持 `21` 导致检查入口不可用。
- 通知详情查询在 service 层处理不存在记录；写操作新增当前用户限定方法，Controller 的已读、批量已读、删除走当前登录用户范围。
- 默认短信、邮件供应商仅作为兜底 bean，不再返回发送成功；未配置真实供应商时返回失败并记录告警。
- 内部调用保护开启时，内部路径请求必须配置共享密钥；密钥为空直接拒绝签名保护路径。
- 将使用 Memory 物料的 group presence 测试拆到通用测试类，Redis 命名测试仅保留真实 Redis 物料。

## 6. 接口和数据变化

- HTTP 路径和公开 API 不变。
- `INotificationService` 增加用户限定写操作方法，仅供本模块 Controller 使用。
- 无数据库 migration。

## 7. 验证方式

- 运行通知模块单测。
- 运行 captcha core/starter 相关单测或至少编译测试。
- 运行 infra web 过滤器单测。
- 运行 realtime test-fixture 规则。
- 运行 PMD 或 `mango:check` 聚合入口，确认不再卡在 `Unsupported targetJdk value '21'`。

## 8. 风险

- 默认短信、邮件供应商改为失败会暴露未配置真实供应商的环境，需要环境显式接入真实 Provider。
- 内部调用密钥为空改为拒绝会暴露未配置 `mango.web.inner.secret` 的环境，需要按环境补齐密钥或关闭内部保护。
