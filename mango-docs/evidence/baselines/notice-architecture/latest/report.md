# Notice 架构债务治理基线对比报告

## 1. 结论

Notice 已按 Payment 政策完成一次性治理。before 为 main `3264cfaa6`，测试基线提交为 `2aac88114`。相同十一模块测试入口由改前 78/78 增至改后 82/82，失败、错误、跳过均为 0；新增 4 条只保护本次发现的真实风险，不为重复参数组合堆用例。

定向实际架构库存由 663 条降为 0：Dependency 0、ArchUnit 0、PMD 0。after 使用十二模块局部 Reactor 的 `full` 模式扫描全部已检测问题，`blockingIssues=0`；按用户要求没有执行全仓检查。

V1-V17 已折叠为唯一 `V1__init_notice.sql`，只含 DDL，建立 20 张最终表。before/after 规范化 schema SHA-256 均为 `6166a2013d22ee3f9a5a3fa39aa622bb01c04b026f65c7505126b419482b8e09`。正式资源初始化 24 个业务类型和 2 个渠道配置；用户联系方式、任务、发送记录、站内信和公告均不初始化，也没有默认 Demo 数据。

完整应用已在独立 MySQL 数据库 `mango_dev_mango_notice_architecture_debt_184`、端口 18184 启动，health 为 UP。10 个鉴权 Notice 查询接口均返回 HTTP 200 / 业务 code 200。登录事件生成 tenant 1 的通知任务，站内信发送成功；未配置外部渠道时 Email/SMS 进入 `RETRY_WAITING/CHANNEL_UNAVAILABLE`、WeCom 为 `CANCELED/RECIPIENT_ACCOUNT_MISSING`，符合原有策略。日志中没有 `ConstraintDeclarationException`、租户为空或事件失败。

## 2. before / after

| 指标 | before | after | 结论 |
|---|---:|---:|---|
| Notice 定向测试 | 78/78 | 82/82 | 20 个报告，0 failure/error/skip |
| Dependency | 0 | 0 | 无债务 |
| ArchUnit | 102 | 0 | 清零 |
| PMD | 561 | 0 | 清零 |
| Flyway | V1-V17，含 DML | 单一纯 DDL V1 | 新库最终态 |
| Notice 表 | 20 | 20 | schema hash 完全一致 |
| 默认个人/运行态数据 | 管理员账户 2 | 0 | 已移出正式初始化 |
| 完整服务 | Payment 编译阻断 | health UP | 直接消费者已等价适配 |

改后测试分布：六类渠道 29、Core 38、Starter 12、Starter Remote 3，共 82。

## 3. 主要治理结果

- `NoticeService` 收敛为门面；配置、投递、记录操作、接收设置、站内信和企业微信同步拆成独立服务。生命周期/多实体编排服务不再错误继承 CRUD 基类。
- Entity、Mapper、XML、内部渠道 SPI、JSON 包装类型、错误码和资源 provider 统一到当前规范；六类渠道行为保持。
- API、Controller、Feign、前端请求目录和实际仓内消费者同批切换固定路径。API 指纹为 `74589dbe9c37d4102da56394ea11f70589d41bfaff0689bf6c0cd9250b694125`，HTTP 指纹为 `1331e2182d04fccc0e085697e8ca5ae9b66221f3b3580a8e193eae9b21021b89`。
- Auth、Identity、Workflow、Payment 发布通知事件时显式携带 tenant；本地和远程 listener 在事务后恢复上下文并在完成后还原，消除 `tenant_id` 为空。
- Bean Validation 统一由 `XxxApi` 持有；Controller 不重复约束。`CTRL-003` 使用真实 API 方法签名识别继承关系，并阻断覆盖参数重复 `@Valid`。
- 正式资源仅位于各模块 `META-INF/mango/resources/`；Notice 无默认 Demo，运行态数据只由实际操作形成。

## 4. 验证入口

```bash
mvn -q -f mango/pom.xml \
  -pl :mango-notice-api,:mango-notice-support,:mango-notice-core,:mango-notice-starter,:mango-notice-starter-remote,:mango-notice-channel-site,:mango-notice-channel-email,:mango-notice-channel-sms,:mango-notice-channel-wecom,:mango-notice-channel-dingtalk,:mango-notice-channel-wechat-official \
  test

mvn -q -f mango/pom.xml -pl :mango-architecture-rules \
  -Dtest=MangoJavaArchitectureRuleTest test

mvn -q -f mango/pom.xml \
  -pl :mango-notice-api,:mango-notice-support,:mango-notice-core,:mango-notice-starter,:mango-notice-starter-remote,:mango-notice-channel-site,:mango-notice-channel-email,:mango-notice-channel-sms,:mango-notice-channel-wecom,:mango-notice-channel-dingtalk,:mango-notice-channel-wechat-official,:mango-architecture-verification \
  -DskipTests -Dmango.architecture.skip=false -Dmango.architecture.mode=full \
  -Dmango.architecture.base=3264cfaa6 -Dmango.architecture.requireFullReactor=false verify

mvn -q -f mango/pom.xml \
  -pl :mango-auth-starter,:mango-identity-core,:mango-workflow-starter,:mango-payment-core \
  -Dtest=AuthSecurityE2ETest,IdentityUserServiceImplIntegrationTest,WorkflowNoticeDomainEventSubscriberTest,PaymentNotificationDispatcherTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

pnpm -C mango-ui --filter @mango/notice build
```

## 5. 限制

- 未配置真实 Email、SMS、WeCom、DingTalk、微信公众号供应商账户，因此只验证了内部投递决策、持久化状态和站内信成功，未向外部供应商真实发消息。
- 页面产品交互没有变化，本次用接口、请求目录与前端包构建验证，未重复浏览器 E2E。
- 按用户要求未执行全仓检查；远端 PR 门禁负责仓库级验证。
- PR [#497](https://github.com/HardyDou/mango/pull/497) 已提交；本报告更新时尚未合并。
