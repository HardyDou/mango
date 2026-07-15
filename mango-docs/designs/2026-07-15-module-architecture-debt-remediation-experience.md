# 模块历史债务治理经验总结

## 1. 背景

本文记录 Payment、CMS、Workflow、Notice 治理以及 Authorization 开工基线暴露的真实失败模式，作为后续模块排序、方案设计和验收的经验输入。长期开发、数据库、测试和交付约束仍以 `mango-pmo` 为唯一规范源，本文不替代规范。

## 2. 已发生的问题类型

| 类型 | 已发生问题 | 为什么旧检查没有发现 | 后续治理关注点 |
|---|---|---|---|
| 构建与发布物污染 | 源码正确，但旧 `target/classes` 被打进 JAR；同一模块出现重复 V1 或旧 migration | 只看源码和本机缓存，没有从仓库重新下载并检查发布物 | clean 构建；发布后重新下载；核对 JAR 文件清单、migration 内容和 SHA-256 |
| Flyway 历史链损坏 | 新 V1 已包含最终字段，后续 V2 又重复加列/索引；空库启动失败 | 单元测试使用 H2 fixture 或关闭 Flyway，没有真实执行发布物迁移链 | 每个模块使用独立全新 MySQL；启动应用；检查 `flyway_schema_history` 和最终 schema |
| DDL、必需数据和 Demo 混杂 | Flyway 同时建表、写基础角色、演示租户、菜单和运行态数据；跨模块 migration 修改其它模块菜单 | SQL 文件存在不等于数据所有权和启动结果正确 | Flyway 只保留 DDL；必需资源与 Demo 资源分开；资源由所属模块登记，Demo 默认关闭 |
| 数据结构与实体漂移 | 实体继承的审计字段未出现在 SQL，运行时插入/更新才报缺列 | Mock Mapper、简化 H2 schema 和只读接口绕开真实写入 | migration、Entity、Mapper、真实写入和更新必须一起验证 |
| API 与 Controller 校验冲突 | API 参数已有 Bean Validation，Controller 覆盖方法再次声明 `@Valid`，Spring 启动或调用时报继承约束异常 | 只调用 Service 或直接 new Controller，没有真实 Spring 方法校验代理 | API 声明约束；Controller 只继承；真实 Spring 上下文和非法请求接口测试 |
| 分层边界失效 | Controller 转换 Entity、一个 Controller 实现多个 API、Controller 未实现 API；Service 使用 `Impl`、直接继承 MyBatis `ServiceImpl` | 测试只验证返回值，未验证结构和真实适配链 | API、Controller、Service、Mapper、Entity、Feign 按根因整体迁移，不保留第二套实现 |
| Mock 证明范围被夸大 | Mock Mapper/数据库的测试被用于证明 SQL、字段、事务或资源落库正确 | 测试数量和覆盖率替代了测试目标审计 | Mock 只隔离外部协作者；数据库、Mapper、权限、资源同步和事务使用真实集成物料 |
| 部分绿灯冒充模块绿灯 | 聚合 POM `mvn test` 0.7 秒成功但实际执行 0 条测试；只跑 core 后宣称整个模块通过 | 没核对 Reactor 模块、Surefire 报告和测试数量 | 显式选择实际子模块；记录每个模块的 tests/failures/errors/skipped；零测试不得作为基线 |
| 源码与仓库依赖版本错位 | Authorization 使用了当前 `mango-resource-api` 新方法，但只选择 Authorization Reactor 时解析到 Nexus 旧 SNAPSHOT，编译报 `@Override` 错误 | 测试命令没有纳入发生契约变化的直接源码依赖 | 基线命令显式纳入必要的直接契约模块；同时保留独立消费者编译证据 |
| 自动配置条件不完整 | resource-sync 测试因 classpath 存在 Gateway 类就装配 Bean，但缺少 `RouteDefinitionLocator`，Spring 上下文失败 | 只测配置类存在或单类逻辑，未启动组合 classpath | 自动配置测试覆盖依赖存在、依赖缺失、Bean 存在和 Bean 缺失组合 |
| 流程范围错位 | 发布物修复被扩大成无关代码 PR，或为解决仓库物问题修改源码 | 没先区分源码缺陷、构建缺陷、发布物缺陷和流程缺陷 | 先确定缺陷层级，只修改承担根因的层；一个模块一个任务分支和 PR |

## 3. 改前与改后不变性的证明方式

治理不以“代码看起来等价”为结论，而以同一组可观察结果对比：

1. 冻结公开 Java/HTTP 契约、权限码、错误码、状态、租户边界和关键数据库副作用。
2. 在生产代码未修改时运行有效单元、真实数据库集成和 API 测试，登记精确用例数及失败事实。
3. 使用全新数据库真实启动服务，验证 Flyway、资源初始化、健康检查和关键读写。
4. 从浏览器用户入口执行 UI/E2E，验证菜单、页面、操作结果、无权限、租户隔离、console 和 network。
5. 修改后在另一套全新数据库执行完全相同的命令、账号角色和业务断言。
6. 检查最终 JAR，而不只检查 `src/main/resources`；确认 migration 清单、DDL/DML 边界和资源清单正确。
7. 单元、集成/API、UI/E2E 任一层失败时，模块保持未验收状态，不提交“部分完成”PR。

## 4. 后续模块处理节奏

每次只处理一个模块：从最新 `main` 创建专用工作区，完成改前基线、根因治理、改后同套验证、真实新库启动和浏览器 E2E；验收通过后提交一个 PR，合并并清理工作区，再选择下一个模块。优先级综合权限/资金/流程等失败后果、下游依赖数量、空库启动风险和正式债务规模决定，不以修改文件数决定。

