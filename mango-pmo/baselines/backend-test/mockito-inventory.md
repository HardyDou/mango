# Mockito 测试治理清单

## 1. 定位

- 本清单服务 GitHub issue #183。
- 长期测试规则以 `mango-pmo/rules/backend/08-test.md` 为准。
- 本清单只记录存量 Mockito 测试的分类、迁移优先级和治理进度。
- 当前清单是 #183 第一阶段治理范围，不代表全仓库 Mockito 已完成盘点。
- 第一阶段覆盖 `auth`、`identity`、`authorization`、`workflow`、`payment`、`notice`；infra、tools、org、cms、captcha 等模块后续按同一口径补充。

## 2. 分类

| 分类 | 含义 | 处理 |
|---|---|---|
| KEEP | 测试目标清楚，mock 只替换目标之外的协作者 | 保留 |
| MIGRATE | mock 掉被测核心链路 | 改为真实集成测试或组件测试 |
| REWRITE | 测试有价值但断言、物料或结构错误 | 重写 |
| DELETE | 只测常量、字段赋值、调用次数，或复述实现 | 删除 |

## 3. 判断问题

每个存量 Mockito 测试至少回答：

1. 测试目标是什么？
2. 被测目标是否真实执行？
3. mock 是否替换了关键验收链路？

## 4. 初始扫描命令

```bash
rg -n "Mockito|mock\(|@Mock|MockBean|mockito" \
  mango/mango-platform/mango-auth \
  mango/mango-platform/mango-identity \
  mango/mango-platform/mango-authorization \
  mango/mango-platform/mango-workflow \
  mango/mango-platform/mango-payment \
  mango/mango-platform/mango-notice \
  -g "*.java" -g "pom.xml"
```

## 5. 治理台账

| 模块 | 命中文件 | 当前判断 | 原因 | 处理优先级 | 跟进 |
|---|---:|---|---|---|---|
| mango-authorization-core | 7 | MIGRATE | `service/impl` 测试 mock Mapper，不能证明权限/菜单持久化链路 | P1 | issue #183 |
| mango-authorization-starter | 7 | MIGRATE | resource handler 测试仍有 4 个 mock service/mapper；`AuthRoleResourceHandlerIntegrationTest` 已作为样板改造 | P1 | issue #183 |
| mango-identity-core | 4 | MIGRATE | 用户/成员 service 测试 mock Mapper，不能证明组织、成员、角色清理链路 | P1 | issue #183 |
| mango-identity-starter | 2 | REWRITE | provider/resource handler 测试需要区分外部 API 替身和真实落库验收 | P1 | issue #183 |
| mango-workflow-core | 7 | MIGRATE | 流程定义、运行时和任务测试 mock Mapper/Flowable 关键协作，需拆分真实数据库集成和外部引擎替身 | P1 | issue #183 |
| mango-payment-core | 33 | REWRITE | 支付通道外部系统 mock 多数合理，但 serviceImpl/Mapper 路径需补真实持久化验收 | P2 | issue #183 |
| mango-notice-core | 3 | REWRITE | outbox/announcement/notice service 需区分外部发送替身和真实持久化链路 | P2 | issue #183 |
| mango-notice-starter | 2 | KEEP | controller/autoconfiguration 测试以装配或 Web 边界为主，保留但需避免声明真实业务链路 | P2 | issue #183 |
| mango-auth-starter | 4 | KEEP/REWRITE | 安全配置和拦截器可使用替身；E2E 中 mock 外部 notice API 需要明确为外部系统替身 | P2 | issue #183 |
| mango-authorization-support | 2 | KEEP | token blacklist 测试 mock KV 存储属于旁路能力替身，保留并避免声称覆盖 KV 实现 | P2 | issue #183 |

## 6. 样板改造

| 测试 | 分类 | 测试目标 | mock 边界 | 真实链路 | 处理状态 |
|---|---|---|---|---|---|
| `AuthRoleResourceHandlerTest` | MIGRATE | 验证 `AUTH_ROLE` resource handler 将资源声明同步到 `authorization_role` | 原测试 mock `RoleMapper`，替换了关键落库链路 | 已改为 `AuthRoleResourceHandlerIntegrationTest`，使用 H2、MyBatis-Plus 和真实 `RoleMapper` 覆盖 create、business-key update、disable | DONE |

## 7. 后续文件级清单要求

后续迁移每个测试文件时必须补充文件级记录：

| 测试文件 | 分类 | 测试目标 | mock 替换对象 | 是否替换关键链路 | 目标验证方式 | 状态 |
|---|---|---|---|---|---|---|
| 记录具体测试文件路径 | KEEP/MIGRATE/REWRITE/DELETE | 写明目标 | 写明替身对象 | 是/否 | 单元/组件/集成/E2E | OPEN/DONE |
