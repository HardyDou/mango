# Notice 架构债务治理基线对比报告

## 1. 当前结论

before 生产提交为 `3264cfaa6`，任务分支为 `refactor/notice-architecture-debt`。原有 71 条定向测试全部通过；在不修改生产 Java/资源/迁移内容的前提下，新增 7 条高价值契约测试，增强 before 为 78/78：六类渠道 29、Core 38、Starter 9、Starter Remote 2。测试质量检查为 PASS。

新增保护覆盖两个公共 Java API 的方法/参数/校验/返回，全部 Notice Controller 的 verb/path/binding/permission，Notice Feign 目录，二十张最终业务表及关键最终字段，以及模块正式资源不含管理员联系方式和运行态数据。没有为重复参数组合堆测试。

同一生产基线的定向架构实际库存为 663：Dependency 0、ArchUnit 102、PMD 561；仓库债务预算仍登记 697，存在 34 条历史漂移，after 以同一实际扫描入口对比。通用静态库存为 3464，`newIssueCount=0`、`toolFailureCount=0`。

before 的 V1-V17 已在 workspace 独立 MySQL 顺序执行，得到 20 张 Notice 表，规范化 schema SHA-256 为 `6166a2013d22ee3f9a5a3fa39aa622bb01c04b026f65c7505126b419482b8e09`。默认迁移会写入两条管理员接收账户；这是本次必须移除的初始化债务。

完整应用 before 启动在编译阶段发现前一 Workflow 改造的真实消费者兼容问题：Payment 仍向 `WorkflowJsonRequest` 入参传 `Map<String,Object>`。该问题不是 Notice 测试失败，但会阻断最终单体启动；设计要求在终验前做等价最小修复并运行 Payment 定向测试。

## 2. before 基线

| 指标 | before | after | 当前结论 |
|---|---:|---:|---|
| 原有测试 | 71/71 | 待执行 | 全绿 |
| 增强测试 | 78/78 | 待执行 | channel 29、core 38、starter 9、remote 2 |
| Dependency 债务 | 0 | 待执行 | 无依赖债务 |
| ArchUnit 债务 | 102 | 待执行 | 待清零 |
| PMD 正式债务 | 561 | 待执行 | 待清零 |
| 通用静态库存 | 3464 | 待执行 | before 新问题0、工具失败0 |
| Notice Flyway | V1-V17，含 INSERT/UPDATE | 待执行 | 目标为单一纯 DDL V1 |
| Notice 表 | 20 | 待执行 | after 必须等价 |
| 管理员接收账户 | 2 | 待执行 | after 默认必须为0 |
| 完整单体启动 | Payment 编译阻断 | 待执行 | 终验前等价修复直接消费者 |

## 3. 公开契约指纹

- Java API：`8ed5782740c8a93aba3627484a61f21e2c846713360732e2a74e43086bbae470`。
- HTTP endpoint/permission：`4ea31ab91737e718cccc1b00db3442a9c2a6280047174e60edf5c83c48e1b976`。
- Feign endpoint/binding：`e9477ea905e555ddccd8919eaa19f478c3a4747918778b3e2746daaa8e02677f`。
- HTTP 与 Feign 指纹会因已批准的固定路径迁移发生有意变化；after 必须形成唯一新指纹并同步仓内调用方。Java 业务方法和返回指纹原则上保持，如协议校验注解规范化导致指纹变化，必须逐项解释且业务字段/结果不变。

## 4. 已确认的架构热点

- `NoticeService.java` 3165 行、152 条正式问题，混合投递、任务、业务配置、模板、渠道、设置、收件偏好、站内信动作和企业通讯同步。
- 17 个实体未统一继承租户基类；18 个 Mapper 缺少规范注解，6 处注解 SQL 需迁 XML。
- `ChannelSendCommand` 是内部渠道 SPI，却以公开协议命令命名并触发 61 条模型问题；六类渠道应迁到准确的 `NoticeChannelMessage` 语义。
- Controller、Feign 和仓内页面包含 48 条路径变量与 40 条路径规范问题；按 Payment 政策迁到固定路径和明确 query/body。
- V9/V10 重复写入管理员邮箱与手机号，V11/V13 含数据更新，V5/V6/V8/V14 已是空壳历史说明；新库政策下均应折入纯 DDL 最终态或正式资源。

## 5. before 验证入口

```bash
mvn -f mango/pom.xml \
  -pl :mango-notice-api,:mango-notice-support,:mango-notice-core,:mango-notice-starter,:mango-notice-starter-remote,:mango-notice-channel-site,:mango-notice-channel-email,:mango-notice-channel-sms,:mango-notice-channel-wecom,:mango-notice-channel-dingtalk,:mango-notice-channel-wechat-official \
  test

node mango-pmo/tools/test-quality-check.mjs --base origin/main
```

验证范围只覆盖 Notice 十一个子模块及其直接测试，不代表全仓检查。
