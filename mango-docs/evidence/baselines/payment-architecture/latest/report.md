# 支付架构债务治理基线对比报告

## 1. 结论

`mango-payment-api/core/starter/starter-remote` 使用同一 Maven 测试入口完成改造前后对比：
原有 268 条测试在改造前后均通过，改造后增加 13 条架构、迁移与资源初始化保护测试，
总计 281/281 通过。
支付四模块架构问题从 1,869 条降为 0，未使用排除、预算放宽或兼容双实现。

基线提交：`cd682ec4c`；实现起点：`26626d5e8`；任务分支：
`refactor/payment-architecture-debt`。

## 2. 同一测试入口

```bash
mvn -q -f mango/pom.xml \
  -pl mango-platform/mango-payment/mango-payment-api,mango-platform/mango-payment/mango-payment-core,mango-platform/mango-payment/mango-payment-starter,mango-platform/mango-payment/mango-payment-starter-remote \
  test
```

| 模块 | 改造前 | 改造后 | 失败/错误/跳过 |
|---|---:|---:|---:|
| mango-payment-api | 1 | 1 | 0/0/0 |
| mango-payment-core | 250 | 255 | 0/0/0 |
| mango-payment-starter | 17 | 25 | 0/0/0 |
| mango-payment-starter-remote | 0 | 0 | 0/0/0 |
| 合计 | 268 | 281 | 0/0/0 |

新增的 13 条测试只保护本次迁移边界，不替代既有业务断言：

- core projection 到 API VO 的属性与列表顺序转换：2 条；
- 44 张支付表的标准租户实体与最终态 V1 完整性：2 条；
- OpenAPI 使用非数值 String tenantId 完成签名、建单与 nonce 持久化：1 条；
- 公网回调原始 body/参数/来源地址与纯文本 ACK：1 条；
- 函数式公网回调 GET/POST 匿名资源声明：1 条；
- 正式/演示资源数量、类型、敏感数据和运行态数据边界：3 条；
- payment Resource handler 的类型化写入、密钥拒绝、禁用与物理删除：3 条。

## 3. 架构基线

| 模块 | 改造前问题数 | 改造后问题数 |
|---|---:|---:|
| mango-payment-api | 175 | 0 |
| mango-payment-core | 1,365 | 0 |
| mango-payment-starter | 329 | 0 |
| mango-payment-starter-remote | 0 | 0 |
| 合计 | 1,869 | 0 |

改造后支付定向完整扫描报告中 `dependencyIssues`、`archUnitIssues`、`pmdIssues`、
`blockingIssues` 均为空。检查器只修复了三个有正反例回归的准确性问题：
`Require.rethrow` 的异常传播识别、可选嵌套输入模型只使用 `@Valid` 时的校验识别，
以及全仓 Bean 注册表不应把 Service 内构造的普通值对象误判为手工构造业务 Service。

## 4. 数据迁移验证

验证库：当前 worktree 专属 `mango_dev_mango_payment_architecture_debt_178`，创建前不存在，
未连接或写入共享业务库。

支付模块尚未发布且团队每次使用全新数据库，因此已批准把历史 V3-V102 初始化链重整为
单一最终态 `V1__payment_platform.sql`。在隔离 MySQL 分别执行旧链最终态和新 V1，清理
Flyway 元数据差异后进行 schema dump 对比；差异为 0。

| 检查项 | 最终结果 | 结论 |
|---|---:|---|
| 支付表 | 44 | 与旧链最终结构一致 |
| `tenant_id VARCHAR(64)` | 44 | 直接使用平台 String 租户模型 |
| `org_id BIGINT NULL` | 44 | 标准实体列完整 |
| V1 DML | 0 | Flyway 只负责 DDL |
| 正式资源 | 65 | 默认加载 |
| 演示资源 | 73 | 仅 demo 开关开启时加载 |
| 运行态初始化数据 | 0 | 只允许业务流程产生 |
| 商户密钥值 | 0 | 不随正式或演示资源分发 |

核心集成测试同时使用真实 MyBatis/H2 SQL 验证标准实体新增 `org_id` 后的查询、插入、
租户条件、唯一约束和主要支付业务链路。

## 5. 批准后的契约变化

| 边界 | 旧契约 | 新契约 | 正确性控制 |
|---|---|---|---|
| 错误码 | `io.mango.payment.api.PaymentCode` | `io.mango.payment.api.enums.PaymentCode` | code/message 保持，Require 测试覆盖 |
| Java API | 带 Spring MVC、Multipart、IOException 或 `R` 的混合契约 | 传输无关 command/query/VO，Controller 统一包装 `R` | API/Controller 目录与校验测试 |
| Open API HTTP | 路径变量、GET/POST 混合 | 八个固定 POST detail/create 路径，统一 body | MVC 契约、HMAC、nonce、IP 与篡改测试 |
| 公网通道回调 | 动态路径 Controller | `GET/POST /payment/channel-callbacks/public?channelCode=...` | 原始协议 ACK 测试与 PUBLIC 资源声明 |
| Mapper 读模型 | 直接返回 API VO | core projection，Service 边界显式转 API VO | projection 转换测试与全部 Service 回归 |
| 租户实体 | 支付自定义 Long tenant 字段 | 平台 `TenantEntity`：String tenantId + Long orgId | V1、元数据与集成测试 |
| remote 适配 | core 泄漏 `R` | remote 层独立 `PaymentRemoteOutcome` 解包 | remote/workflow 契约测试 |

## 6. 业务不变量对比

原有 268 条测试未删除、未跳过且全部继续通过，覆盖支付订单、金额、状态流转、幂等、
签名与防重放、收银台、回调、退款及审批、通知、异常订单、对账、差异、结算、线下收付、
敏感字段、租户隔离和工作流失败补偿。网关纯文本 ACK、错误码数值、状态推进与通知触发语义
没有非批准变化。

批准变化仅限上表列出的 Java/HTTP/数据库结构；其它模块不存在支付依赖，因此没有保留历史
接口的第二套兼容实现。

## 7. 执行结果

| 验证 | 结果 |
|---|---|
| 支付四模块同入口测试 | PASS，281/281 |
| architecture-rules 单元测试 | PASS |
| 支付四模块完整架构扫描 | PASS，1,869 → 0 |
| 旧链最终态与 V1 schema 对比 | PASS，44 表，schema dump 差异 0 |
| 全新库默认启动 | PASS，health UP；baseline + V1；正式 65、demo 0、运行态 0 |
| 全新库 demo 启动 | PASS，health UP；正式 65、demo 73、运行态 0、密钥值 0 |
| 登录与支付接口 smoke | PASS，登录成功；3 个支付查询接口 HTTP/业务均成功 |
| 支付定向架构门禁 | PASS，dependency/ArchUnit/PMD/blocking 均为 0 |
| 支付前端包构建 | PASS，`@mango/payment` build |
| payment-center Playwright 收集 | PASS，3 浏览器共 108 条用例定义，开放接口用例契约可解析 |
| payment-center 浏览器执行 | EXCEPTION，本次未启动完整页面环境；不以 Playwright list 或接口 200 代替页面结果 |
| payment 四模块债务预算 | PASS，四个交付模块均下调为 0 |
| 全仓债务预算比较 | NOT RATCHETED；后续 changed-mode 报告显示规则准确性修正带来的非 payment 历史误报减少，该报告不用于改写范围外预算；其余 209 个模块预算条目保持原值 |
| `git diff --check` | PASS |

浏览器执行例外不扩大本报告对 UI 页面结果的结论。用户要求的单元/接口基线由 281 条
后端测试、MVC/OpenAPI 契约测试、真实 MySQL V1/Resource 启动证明和支付定向门禁支撑；E2E 脚本
已同步新路径并通过 Playwright 收集，真实页面验收留给具备干净全量应用数据库的独立环境。
