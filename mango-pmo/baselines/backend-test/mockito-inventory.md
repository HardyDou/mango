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
4. 如果需要真实数据库，采用哪种隔离物料和清理策略？

数据库测试隔离和物料选择以 `mango-pmo/rules/backend/08-test.md` 为准。不能为了避免脏数据而 mock Mapper 或数据库结果；应使用 H2、Testcontainers、专用测试库、事务回滚或显式清理。

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
| `RoleDataScopeServiceImplTest` | MIGRATE | 验证角色数据范围保存与解析 | 原测试 mock `RoleDataScopeMapper`、`RoleMapper`、`MenuMapper`、`RoleMenuMapper`、`SubjectRoleBindingMapper`，替换了权限数据范围核心链路 | 已改为 `RoleDataScopeServiceImplIntegrationTest`，使用 H2、MyBatis-Plus 和真实 Mapper 覆盖无角色 fallback、角色授权 list 资源后落库、主体角色与数据范围合并解析 | DONE |
| `ApiResourceServiceImplTest` | MIGRATE | 验证 API resource 扫描注册、失效旧路径和运行时访问决策 | 原测试 spy 被测 `ApiResourceServiceImpl` 并 mock `list`、`saveBatch`、`updateBatchById`，替换了关键持久化链路 | 已改为 `ApiResourceServiceImplIntegrationTest`，使用 H2、MyBatis-Plus 和真实 `ApiResourceMapper` 覆盖旧路径禁用和访问决策解析 | DONE |
| `SubjectAuthorityServiceImplTest` | MIGRATE | 验证主体角色到菜单权限解析 | 原测试 mock `SubjectRoleBindingMapper`、`RoleMenuMapper`、`MenuMapper`，替换了授权查询核心链路 | 已改为 `SubjectAuthorityServiceImplIntegrationTest`，使用 H2、MyBatis-Plus 和真实 Mapper 覆盖权限字段优先、legacy menuCode fallback、非法 tenant 过滤 | DONE |
| `TenantMenuPackageBindingHandlerTest` | MIGRATE | 验证租户绑定套餐后管理员角色菜单授权重建 | 原测试 mock `RoleMapper`、`RoleMenuMapper`、`MenuMapper`，替换了角色菜单落库链路 | 已改为 `TenantMenuPackageBindingHandlerIntegrationTest`，使用 H2、MyBatis-Plus 和真实 Mapper 覆盖租户上下文切换、旧绑定清理、菜单父级展开和新绑定落库；套餐菜单列表保留测试替身作为外部输入 | DONE |
| `MenuServiceImplTest` | MIGRATE | 验证菜单查询、树构建、用户菜单过滤、runtime config 和删除保护 | 原测试 mock `MenuMapper`、`FrontendMenuRuntimeConfigMapper` 等 Mapper，替换了菜单持久化链路 | 已改为 `MenuServiceImplIntegrationTest`，使用 H2、MyBatis-Plus 和真实 Mapper 覆盖 runtime config 读写、隐藏启用菜单路由保留、树结构和叶子删除清理；主体权限服务保留测试替身作为外部授权输入 | DONE |

## 7. 后续文件级清单要求

后续迁移每个测试文件时必须补充文件级记录：

| 测试文件 | 分类 | 测试目标 | mock 替换对象 | 是否替换关键链路 | 目标验证方式 | 数据隔离/清理 | 状态 |
|---|---|---|---|---|---|---|---|
| 记录具体测试文件路径 | KEEP/MIGRATE/REWRITE/DELETE | 写明目标 | 写明替身对象 | 是/否 | 单元/组件/集成/E2E | H2/Testcontainers/rollback/显式清理/无数据库 | OPEN/DONE |
| `mango/mango-platform/mango-authorization/mango-authorization-core/src/test/java/io/mango/authorization/core/service/impl/RoleDataScopeServiceImplTest.java` | MIGRATE | 角色数据范围保存与解析 | `RoleDataScopeMapper`、`RoleMapper`、`MenuMapper`、`RoleMenuMapper`、`SubjectRoleBindingMapper` | 是 | 集成测试：`RoleDataScopeServiceImplIntegrationTest` 使用真实 H2/MyBatis-Plus Mapper | H2 内存库，`BeforeEach` 重建最小表结构 | DONE |
| `mango/mango-platform/mango-authorization/mango-authorization-core/src/test/java/io/mango/authorization/core/service/impl/ApiResourceServiceImplTest.java` | MIGRATE | API resource 扫描注册、旧路径禁用和访问决策 | spy 被测 service，替换 `list`、`saveBatch`、`updateBatchById` | 是 | 集成测试：`ApiResourceServiceImplIntegrationTest` 使用真实 H2/MyBatis-Plus Mapper | H2 内存库，`BeforeEach` 重建最小表结构 | DONE |
| `mango/mango-platform/mango-authorization/mango-authorization-core/src/test/java/io/mango/authorization/core/service/impl/SubjectAuthorityServiceImplTest.java` | MIGRATE | 主体角色到菜单权限解析 | `SubjectRoleBindingMapper`、`RoleMenuMapper`、`MenuMapper` | 是 | 集成测试：`SubjectAuthorityServiceImplIntegrationTest` 使用真实 H2/MyBatis-Plus Mapper | H2 内存库，`BeforeEach` 重建最小表结构 | DONE |
| `mango/mango-platform/mango-authorization/mango-authorization-core/src/test/java/io/mango/authorization/core/service/impl/TenantMenuPackageBindingHandlerTest.java` | MIGRATE | 套餐绑定后租户管理员角色菜单授权重建 | `RoleMapper`、`RoleMenuMapper`、`MenuMapper` | 是 | 集成测试：`TenantMenuPackageBindingHandlerIntegrationTest` 使用真实 H2/MyBatis-Plus Mapper，`IMenuPackageService` 仅作为套餐输入替身 | H2 内存库，`BeforeEach` 重建最小表结构 | DONE |
| `mango/mango-platform/mango-authorization/mango-authorization-core/src/test/java/io/mango/authorization/core/service/impl/MenuServiceImplTest.java` | MIGRATE | 菜单持久化、runtime config、树结构和用户菜单过滤 | `MenuMapper`、`FrontendMenuRuntimeConfigMapper`、`AuthorizationAppModuleMapper` 等 Mapper | 是 | 集成测试：`MenuServiceImplIntegrationTest` 使用真实 H2/MyBatis-Plus Mapper，`ISubjectAuthorityService` 仅作为授权输入替身 | H2 内存库，`BeforeEach` 重建最小表结构 | DONE |
