# Authorization 历史债务治理设计

## 1. 目标与成功条件

在不改变角色、菜单、按钮权限、API 资源访问模式、数据权限、租户应用绑定、授权快照和前端运行描述业务语义的前提下，一次性清理 `mango-authorization` 的历史架构与数据库债务。只有下列证据同时成立才完成：

- Authorization 所有实际 Maven 子模块测试通过，且记录精确测试数量；
- 真实全新 MySQL 上的 Flyway、资源同步、Spring 装配和 API 成功/失败场景通过；
- 真实后端和前端启动后，Authorization 定向浏览器 UI/E2E 全部通过；
- 最终 JAR 中只包含 Authorization 的 canonical DDL migration，必需资源和 Demo 资源边界正确；
- 正式 Authorization 架构债务从当前 634 条降为 0，未向其它模块转移问题。

## 2. 范围

范围包括 Authorization 的 API、Core、Support、Starter、Resource Sync Starter、Resource Access Starter、Remote Starter，以及为保持契约和真实验收必须同步的直接调用方、RBAC 前端测试和 monolith 启动入口。不清理无关模块的历史债务；直接调用方只做适配 Authorization 当前契约所需的修改。

## 3. 设计决策

| 决策 | 当前问题 | 目标设计 | 行为保持证据 |
|---|---|---|---|
| 契约唯一化 | API 中混有非契约类型，Controller/Feign 绑定与校验不一致 | API 只保留传输无关契约与协议模型；Controller 和 Feign 各自适配 HTTP，路径、verb、binding 和泛型一一对应 | Java/HTTP 指纹、Bean Validation、成功与失败 API 测试 |
| Controller 适配器化 | Controller 转换 Entity、实现多个 API、存在无 API Controller、重复 `@Valid` | 一个 Controller 只实现一个 API，只注入 `I*Service`，直接返回 `R.ok(service...)`；拆分 RoleBinding，补齐 MenuPackage API | Spring 上下文、非法参数、权限拒绝、接口目录和浏览器操作 |
| Service canonical 化 | 11 个 `*ServiceImpl`、直接继承 MyBatis `ServiceImpl`、CRUD 契约不完整 | 实现统一命名 `*Service implements I*Service`；CRUD 使用 Mango Typed CRUD；业务动作使用 `Require + AuthorizationCode` | 同一业务规则、错误码、状态和数据库副作用测试 |
| 持久化 canonical 化 | 15 个 Entity 无 `Entity` 后缀，Mapper 聚合名不一致 | `*Entity`、`BaseMapper<*Entity>`、类型化 Mapper 边界；表名、字段和值不变 | schema/Entity/Mapper 检查、真实 CRUD 与租户隔离 |
| 资源处理服务化 | Starter 的资源 Handler 直接操作 Authorization、Identity、Org Mapper | Handler 通过明确领域服务完成解析和写入；资源依赖顺序保持 | 资源 Handler 真实数据库集成、幂等、禁用和依赖顺序测试 |
| 新库 migration 收敛 | 62 个 migration，V1 和增量文件包含 DML，且 Notice/Job/Payment 菜单越权归属 | 生成 Authorization 最终态纯 DDL V1；删除历史 DML migration；各功能模块菜单继续由自身 `META-INF/mango/resources/` 声明 | 新旧最终 schema 指纹、V1 DML 扫描、空库启动、JAR 清单 |
| 初始化数据分层 | 基础角色、演示租户、应用、菜单套餐和运行配置混在 Flyway | Authorization 必需资源放模块正式 resources；演示租户/角色/绑定放 demo 且默认关闭；运行态数据不预置 | 默认启动与 demo-enabled 两种资源结果对比 |
| 依赖版本闭环 | 单独构建 Authorization 会拿到旧 Resource API SNAPSHOT | 测试入口显式纳入发生契约变化的直接 API 源码模块，并独立验证发布消费者 | Reactor 顺序、消费者编译和最终发布物依赖解析 |
| 自动配置健壮性 | resource-sync 在缺少 Gateway locator 时仍创建 Bean | 自动配置同时按 class、property 和必需 Bean 条件装配 | 四种 classpath/Bean 组合测试与真实应用启动 |

## 4. 保持不变的业务契约

- HTTP 根路径、业务动作、请求/响应字段、权限码和统一错误语义保持；为消除历史路径变量所需的固定路径/query 迁移，必须同步全部仓内调用方并由接口指纹记录。
- `ROLE_ADMIN`、普通角色、角色菜单、菜单 `apiCodes`、主体角色、有效数据范围和授权快照计算结果保持。
- `PUBLIC`、`LOGIN`、`PERMISSION` API 访问模式和无权限 403 行为保持。
- 同租户可见、跨租户不可见；平台管理员不得把自身不可授权菜单或数据范围授予普通租户。
- 菜单树父子关系、排序、可见性、页面 key、运行模式和菜单套餐结果保持。

## 5. 测试基线与验收矩阵

| ID | 层级 | 场景 | 长期入口 | 完成标准 |
|---|---|---|---|---|
| AUTH-TC-001 | 单元/组件 | 权限匹配、菜单树、角色授权、防越权、数据范围、JWT 与资源转换 | Authorization 7 个 Maven 子模块定向 test | failures/errors/skipped 为 0；被测规则真实执行 |
| AUTH-TC-002 | 集成 | Service/Mapper/事务、资源 Handler、自动配置组合 | H2/MySQL 集成测试 | Mapper 与数据库真实参与；自动配置缺失依赖不误装配 |
| AUTH-TC-003 | API | 角色、角色绑定、菜单、菜单套餐、数据范围、应用、模块、租户绑定、API 资源和授权快照 | Spring API/入口流程测试 | 正常、非法参数、无权限、跨租户和错误语义通过 |
| AUTH-TC-004 | 数据库 | 纯 DDL V1、正式/demo 资源、最终 schema、JAR migration | 全新 `mango_dev_*` MySQL + packaged JAR 检查 | Flyway 成功、V1 无 DML、默认无 Demo、必需资源完整 |
| AUTH-TC-005 | UI/E2E | 应用、角色权限、角色数据范围、菜单、菜单套餐和租户越权拒绝 | `app-management-save`、`role-permission`、`role-data-scope`、`menu-management`、`menu-package-management` | 浏览器业务断言全部通过，console/network 无未解释错误 |
| AUTH-TC-006 | 架构 | 634 条正式债务归零 | Authorization 定向架构报告与正式预算 | Authorization 为 0，其它模块无新增 identity |

改前和改后使用同一组命令、数据库初始化方式、账号/租户角色与业务断言。改前存在的真实失败会保留在基线中，修复后必须转为通过；不得删除用例、弱化断言或改名掩盖差异。

## 6. 回退与停止条件

任一公开契约、权限/租户结果、数据库最终结构、必需资源、浏览器业务结果或正式债务无法证明时停止提交。任务只支持可丢弃的新数据库；不提供旧 Authorization Flyway history 的原地升级路径。任务分支未验收前可整体丢弃，不保留半迁移兼容层。

## 7. 最终验证结果

- Authorization/System/Resource 定向测试：189/189 通过。
- Auth、Access、Home、Context、Feign 直接兼容消费者测试通过。
- 增量架构门禁：阻断项 0，Authorization 债务项 0。
- demo 开启的全新 MySQL：Flyway 只有 baseline + V1，服务启动成功；8/8 Chromium E2E 通过。
- demo 关闭的全新 MySQL：正式应用 1、套餐 2、授权资源 739；演示角色 0、演示绑定 0。
- 重新开启 demo：演示角色 4、演示绑定 4，服务健康状态 `UP`。
- E2E 首轮发现文件预览与实时 WebSocket 注册器仍调用旧 `ApiResourceApi.registerApiResources(List)`；最终同步改用规范 `ApiResourceRegisterRequest`，并通过两个消费者单测和真实应用启动验证发布物整体一致。
