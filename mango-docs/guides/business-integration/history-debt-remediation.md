# Mango 业务模块历史债务修复指南

## 1. 定位

本指南面向基于 Mango 开发业务模块的开发者，用于在不改变既有业务语义、HTTP 契约、权限和租户边界的前提下，治理 API、Controller、Service、Entity、数据库、初始化资源、测试和发布物中的历史债务。

本文是操作手册，不是第二份规范源。实际任务先执行 PMO preflight，并以其 `Must read` 输出和 [Mango PMO 规则](../../../mango-pmo/rules/00-dev-flow.md) 为准。

## 2. 什么才算修复成功

历史债务治理不是“编译通过”或“架构告警变少”。一个模块只有同时满足以下结果，才能宣布成功：

1. 本次新增的架构违规为零，已纳入范围的历史债务被真正移除，不是转移到另一个模块或换一个名字。
2. 同一组有价值的单元测试在修改前建立基线，修改后重新通过。
3. 使用真实 Spring 装配、Mapper 和隔离数据库的集成测试通过。
4. 从真实 HTTP 入口验证参数校验、返回结构、错误码、权限、租户和数据副作用。
5. 使用全新数据库启动真实服务，Flyway、Resource Registry、健康检查和关键读写全部正常。
6. 有浏览器入口的模块，使用真实账号、角色、菜单和后端执行 UI/E2E，业务断言、UI 断言、console 和 network 全部正常。
7. 检查最终 JAR/发布物，确认迁移脚本、资源文件和依赖版本与源码一致。

任何一层失败，模块状态都是“未验收”，不是“基本完成”。

## 3. 常见债务类型

| 类型 | 典型表现 | 根因 | 主要证明手段 |
|---|---|---|---|
| API 契约债务 | API 无校验，Controller 重复 `@Valid`，Feign 与 Controller 绑定不一致 | 契约分散在多个适配器 | API 契约测试、非法请求 API 测试、Controller/Feign parity |
| Controller 分层债务 | Controller 转换 Entity、组装查询、处理业务分支，一个 Controller 实现多个 API | 业务用例未收敛到 Service | 结构检查、MockMvc/真实 API、Service 规则测试 |
| Service 债务 | `XxxServiceImpl`、Service 散落在非 `impl` 目录、直接继承 MyBatis `ServiceImpl`、直接抛通用异常 | 模块自建了第二套 CRUD/异常语义 | 定向架构检查、Service 单元/集成测试 |
| Entity/Mapper 债务 | Entity 未以 `Entity` 结尾，重复 ID/租户/审计字段，Mapper 聚合名不一致 | 持久化模型没有继承 Mango canonical Entity | 编译、Mapper 真实读写、Entity/schema 对照 |
| Flyway 债务 | 重复 V1，V1 已有字段而 V2 再加一次，新库启动失败 | 只验证老库或本地缓存 | 独立新 MySQL、`flyway_schema_history`、最终 schema |
| 初始数据债务 | DDL、必需数据、demo 混在 SQL；本模块 SQL 改其它模块菜单 | 数据所有权不清 | Flyway DDL 检查、Resource 同步日志、demo 开关对比 |
| 自动配置债务 | classpath 有某类就装配，但所需 Bean 不存在，应用启动失败 | Conditional 条件不完整 | 有类/无类、有 Bean/无 Bean 组合装配测试 |
| 依赖版本债务 | 源码编译正常，仅选业务模块时解析到 Nexus 旧 SNAPSHOT 而失败 | 源码 Reactor 与仓库版本错位 | 明确纳入直接契约模块，另做仓库消费者编译 |
| 测试假绿 | `mvn test` 显示 SUCCESS 但执行 0 条；Mock Mapper 却声称 SQL 正确 | 只看退出码或测试数量 | Surefire 分模块计数、被测链路审计、真实 DB/API/E2E |
| 发布物污染 | 源码正确，JAR 里却带旧 `target/classes`、重复 migration | 未 clean 构建，只检查工作区 | clean package/deploy，仓库重新下载，`jar tf`、SHA-256 |
| UI/菜单债务 | 接口 200，但菜单不可见、按钮越权、路由 404 | 只验证后端单层 | 真实角色浏览器 E2E、console/network、不同租户对比 |
| 验证物料债务 | 增量构建复用旧静态报告；端口就绪但资源同步尚未完成 | 把退出码或首个 HTTP 响应当成最终状态 | 报告时间/class 签名/模块报告交叉核验；等待健康与资源派生关系稳定 |

## 4. 标准修复流程

### 4.1 一次只处理一个模块

从最新 `main` 建立独立任务分支/工作区。先完成当前模块的修改、同套验证、PR 和合并，再进入下一个模块。这样可以让失败归因、回退和债务预算下调都有清晰边界。

执行 preflight 示例：

```bash
node mango-pmo/tools/pmo-preflight.mjs \
  --role dev \
  --phase develop \
  --task "治理 <module> 历史债务" \
  --paths "<module-path>"
```

### 4.2 锁定不变性契约

改代码前，先按 [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md) 和 [测试用例与自动化流程](../../../mango-pmo/rules/09-test-case-automation-flow.md) 形成业务方可观察行为的基线。本次历史债务记录通常包含：

- 公开 Java API、HTTP method/path、参数绑定和 JSON 结构。
- 成功、失败、不存在、无权限和非法参数的状态码与错误语义。
- 权限码、菜单可见性、按钮可用性、租户和数据权限边界。
- 关键表的写入、更新、删除、状态变化和幂等性。
- 前端菜单位置、路由、页面主操作和典型角色可见范围。

如果发现旧行为本身就是缺陷，可在任务范围中分别记录“缺陷修复”和“结构治理”，并由回归用例承载修复后的目标语义；问题归属和范围调整参见 [任务中发现新问题](../../../mango-pmo/rules/00-dev-flow.md#341-任务中发现新问题)。

### 4.3 建立改前基线

基线不要只记录“BUILD SUCCESS”，至少记录：

| 证据 | 要记录的内容 |
|---|---|
| 源码基准 | `main`/tag/发布版本和 commit SHA |
| 测试命令 | 实际 Maven/Node/Playwright 命令和选中的子模块 |
| 测试结果 | tests/failures/errors/skipped，不接受不加说明的 0 tests |
| 运行环境 | JDK、Maven/Node、Mango/CLI 版本、后端/前端端口 |
| 数据库 | 一次性数据库名、Flyway 结果、测试租户/账号标识，不记录密码 |
| API 指纹 | 关键请求的 method/path/参数/状态码/响应摘要/数据副作用 |
| UI/E2E | 菜单入口、用例、角色、业务断言、console/network 结果和证据路径 |
| 发布物 | JAR 资源清单、migration 数量和 SHA-256 |

测试命令应显式选择真正有测试的子模块。如果当前源码依赖了未发布的直接契约，把该 API 模块一起放进 Reactor，不要让仓库旧 SNAPSHOT 伪装成业务代码编译错误。

### 4.4 盘点债务，再设计收敛方案

按层次建立问题清单：

1. API 契约和 model。
2. Controller/Feign 适配器。
3. Service 用例、事务和异常。
4. Entity、Mapper 和 schema。
5. starter 自动配置与依赖边界。
6. Flyway、正式资源、demo 资源和运行时数据。
7. 前端包、菜单、路由、按钮权限和 E2E。
8. 发布脚本、版本锁和最终仓库物料。

修复顺序推荐从“契约和数据边界”开始，再向适配器和页面收敛。如果先改 Controller，后面又改 API 和 Service，容易反复返工。

### 4.5 逐层实施

#### API 与 Controller

- 一个 Controller 对应一个 `XxxApi`。
- 参数约束由 API 接口声明，Controller 使用 `@Validated` 并继承约束，避免在覆盖方法上重复声明冲突的 Bean Validation。
- Command/Request 使用 body，Query 使用明确 query binding；Controller 与 Feign 保持 verb、path、参数名和绑定一致。
- Controller 只依赖 `I*Service`，转换、当前用户查询组装、业务分支和事务移入 Service。
- 不改变对外 HTTP 结构。如果 Java 类型需要拆分，先确认 JSON 序列化后的字段和语义保持不变。

#### Service、Entity 与 Mapper

- 业务实现使用 `XxxService implements IXxxService`，放在 `service/impl`。
- 基础 CRUD 复用 Mango canonical CRUD contract/实现，不再包一层自建 MyBatis `ServiceImpl`。
- 用稳定的业务条件和错误码表达失败，不要在各 Service 临时抛不同通用异常。
- 持久化类使用 `XxxEntity`，继承 Mango canonical `TenantEntity`；不在子类重复声明 ID、tenantId 和审计字段。
- Mapper 只访问本域表，直接继承 `BaseMapper<XxxEntity>`，Mapper/Entity 聚合名一致。
- Entity 重构后的 migration 对照范围包括继承的 `tenant_id`/`org_id`/`created_by`/`created_at`/`updated_by`/`updated_at`；字段与 schema 的正式约束参见 [持久化与 CRUD 规范](../../../mango-pmo/rules/backend/07-persistence.md)。

#### Flyway 与资源数据

数据按所有权和生命周期分类：

| 数据 | 放置位置 | 特点 |
|---|---|---|
| 表、列、索引等 DDL | 模块 `db/migration/**` | 由 Flyway 管理 |
| 菜单、权限、字典、正式小资源 | `META-INF/mango/resources/` | 默认扫描，由对应 ResourceHandler 落库 |
| 演示账号、演示配置、示例业务数据 | `META-INF/mango/demo/` | 只在 `mango.resource.registry.demo-enabled=true` 时扫描 |
| 运行时可修改、升级时需保留的数据 | Resource `INIT_ONLY` 或业务开通/导入流程 | 初始化模式和数据归属参见 [数据库规范](../../../mango-pmo/rules/backend/04-db.md) |
| 大 SQL、停机修复、外部升级包 | 模块化外部 Flyway location | 不进入默认 classpath 启动链 |

对仅面向全新数据库、尚未发布的模块，可以将当前最终 schema 重整为一份纯 DDL `V1`。已对外发布并可能有存量数据库的模块，不能擅自改已执行 migration，需要追加新版本并设计升级路径。

模块间 migration、表和资源的数据所有权以 [数据库规范](../../../mango-pmo/rules/backend/04-db.md) 与 [持久化与 CRUD 规范](../../../mango-pmo/rules/backend/07-persistence.md) 为准；本指南示例按资源所属模块在各自的 `META-INF/mango/resources/` 下登记。

### 4.6 使用同一组验证回放

修改后使用与基线相同的子模块、数据、账号、角色、请求和断言回放。新增的债务回归用例叠加到同一套中，不要删除或放宽旧断言来制造通过。

## 5. 测试基线设计

### 5.1 各层能证明什么

| 层级 | 适合证明 | 不能单独证明 |
|---|---|---|
| 静态/编译 | 类型、依赖、结构、注解和基本配置 | 运行时业务、SQL、权限 |
| 单元测试 | 纯规则、算法、转换、状态机和异常分支 | Spring 装配、Mapper、真实数据库 |
| 集成测试 | Spring、Service、Mapper、事务、资源注入和真实持久化协作 | 公开 HTTP 绑定、浏览器交互 |
| API 测试 | HTTP 契约、非法参数、权限、错误码、返回值和写库结果 | 菜单可见性、路由和 UI |
| UI/E2E | 用户入口、登录、菜单、路由、页面、按钮、业务操作和浏览器异常 | 所有内部边界分支 |
| 发布物检查 | 真正交付的 JAR/包内容和仓库可消费性 | 业务语义 |

### 5.2 Mock 边界

Mock 只用于隔离被测目标之外的协作者，不能替换本次要证明的核心链路。

- Mock 了 Mapper/数据库，就只能证明 Service 在指定输入下的分支，不能证明 SQL、列名、租户过滤或事务正确。
- 直接 `new Controller` 不能证明 Spring 方法校验、安全过滤器和参数绑定正确。
- Mock 了权限结果，不能证明菜单、按钮、角色授权和数据权限链路正确。
- 支付、短信、对象存储等第三方可以使用沙箱或可控测试替身，但 Mango 内部主链路应真实参与。

### 5.3 最低有效用例矩阵

| 场景 | 建议层级 | 最少断言 |
|---|---|---|
| 新增/修改成功 | 单元 + 集成 + API | 返回、数据库字段和审计/租户字段正确 |
| 非法参数 | API | HTTP 状态/业务错误与契约一致，Service 未产生副作用 |
| 记录不存在 | Service + API | 错误语义不被结构治理改变 |
| 无权限 | API + UI/E2E | 接口拒绝，页面/按钮不越权 |
| 租户隔离 | 集成 + API + UI/E2E | A 租户不可读写 B 租户数据 |
| 初始化 | 新库启动 + 集成 | DDL 成功，正式资源存在，demo 开关语义正确 |
| 历史缺陷 | 最近根因的自动化层 + 真实入口 | 修改前可复现，修改后稳定防回归 |

## 6. 新库、Resource 和发布物验证

### 6.1 全新数据库

每次正式验收使用新建的一次性数据库，不复用修复过、手工补列或修改过 Flyway history 的数据库。

启动后检查：

1. 应用健康状态为 UP。
2. `flyway_schema_history` 无失败或重复版本。
3. Entity 使用的列全部存在，不只验证建表成功。
4. Resource Registry 已发现当前模块声明，sync log 为 SUCCESS，目标表有对应结果。
5. demo 关闭时没有演示数据，demo 开启时才导入且可重复执行。
6. 至少一条关键新增、查询、修改链路真实读写成功。
7. 健康探测和端口可访问只证明进程入口可响应；必须等待正式资源、demo 资源和角色菜单等派生关系达到预期稳定值后再执行 API/UI 验收。

### 6.2 最终 JAR

示例检查：

```bash
jar tf <artifact.jar> | sort
jar tf <artifact.jar> | rg 'db/migration|META-INF/mango/(resources|demo)'
shasum -a 256 <artifact.jar>
```

重点确认：

- 每个 Flyway location 没有重复版本或过期脚本。
- 重整为 V1 的模块，JAR 内只有预期 V1，且不含演示 DML。
- 正式资源和 demo 资源分别位于正确目录。
- 仓库重新下载的 JAR 与本次发布内容一致，不以当前工作区 `target/classes` 代替。

## 7. API 和浏览器端到端验收

### 7.1 API 不只检查 200

对每个核心能力至少验证：

- 正常请求的业务字段和数据库副作用。
- 缺少必填字段、越界数值、非法枚举和错误 ID。
- 未登录、无权限和不同角色。
- 不同租户和数据权限范围。
- 不存在记录、重复提交和关键异常分支。

### 7.2 UI/E2E 最小闭环

每个有页面入口的模块至少保留以下用户目标：

1. 使用真实测试账号登录。
2. 从当前正式菜单进入页面，不直接输入隐藏路由规避菜单问题。
3. 等待真实列表或详情请求成功，断言业务数据，不只断言页面有文字。
4. 完成一次关键查询或 CRUD/状态操作，检查页面回显和数据库结果。
5. 用另一个角色验证菜单、按钮和接口拒绝语义。
6. 用另一个租户验证数据隔离。
7. 检查 console 无未解释 error，network 无未解释 4xx/5xx，并保留失败 trace/截图。

E2E 优先使用 `data-page`、`data-surface`、`data-action`、`data-field`、`data-record-key` 和 `data-state` 等业务语义锚点。不要在 spec 中依赖 DOM 层级、`nth()`、`waitForTimeout()` 或 `force: true` 掩盖真实交互问题。

## 8. 失败归因决策树

```text
编译/测试/启动/E2E 失败
├─ 当前源码就能复现？
│  ├─ 是：代码或设计缺陷，在当前模块修复并补回归测试
│  └─ 否：继续比对基线和发布物
├─ clean 后正常，非 clean 失败？
│  └─ 是：旧 target/classes 或构建污染
├─ 源码正常，仓库下载 JAR 失败？
│  └─ 是：发布物/版本问题，检查 JAR 清单和 SHA-256
├─ 旧库正常，新库失败？
│  └─ 是：Flyway/schema/正式资源问题
├─ API 通过，页面失败？
│  └─ 检查菜单资源、角色授权、路由、前端版本和 network
└─ 只有 Mock 测试通过？
   └─ 结论仅限于被测局部规则，补真实集成/API/E2E
```

失败归因和问题归属参见 [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)。排查时可先区分发布物与源码证据：前者核对制品清单、仓库版本和摘要，后者回到源码、迁移和回归测试定位。

## 9. 常见假通过

- 聚合 POM 返回 SUCCESS，但实际没有选中任何测试类。
- 单元测试 Mock 了 Mapper，却宣布数据库字段和 SQL 已验证。
- 只对 Controller 直接调用，没有经过 Spring Validation 代理和 HTTP 绑定。
- 服务启动时复用了已手工修复的老库。
- 只调用列表 GET，没有触发 Entity 字段映射和审计填充的真实写入。
- 页面可以打开，但列表请求是 403/500，或 console 已有错误。
- E2E 直接输入路由，跳过了菜单、权限和租户上下文。
- 只检查 `src/main/resources`，没有检查真正发布的 JAR。
- 发布成功后没有从目标 Maven/npm 仓库重新拉取验证。
- 为了让旧 E2E 变绿，放宽权限断言或回退已经确认的新菜单结构。
- 聚合静态报告的生成时间早于当前 class，却仍将其中旧行号和旧构造器当成当前提交问题。
- CLI 返回端口 ready 后立即读取初始化表，没有等待 Resource Registry 完成就宣布缺数据或通过。

## 10. 交付前检查表

### 范围与基线

- [ ] 已记录基准 commit/版本。
- [ ] 已锁定公开契约、权限、租户、数据副作用和 UI 可观察结果。
- [ ] 修改前单元、集成/API、新库和 UI/E2E 基线已记录。
- [ ] 测试数量和 Surefire/Playwright 结果可追溯。

### 代码与数据

- [ ] API/Controller/Feign 契约一致，非法请求可验证。
- [ ] Controller 只做 HTTP 适配，Service 承担业务用例。
- [ ] Service、Entity、Mapper 使用 Mango canonical 结构。
- [ ] Entity 字段与最终 schema 一致，真实写入已验证。
- [ ] Flyway 只承担当前模块 DDL，无菜单/demo/跨模块数据 DML。
- [ ] 正式资源、demo 资源、运行时数据分类正确。

### 验证与发布物

- [ ] 同一组修改后单元测试全部通过。
- [ ] 真实数据库集成测试和 API 测试全部通过。
- [ ] 全新数据库启动、Flyway、Resource sync 和关键读写通过。
- [ ] 不同角色和不同租户的浏览器 UI/E2E 通过。
- [ ] console/network 无未解释错误。
- [ ] clean 构建后的 JAR 内容、migration 和资源清单正确。
- [ ] 如果发布，已从目标仓库回查版本并重新下载验证。

### 收尾

- [ ] 只保留当前模块相关改动，无未说明文件。
- [ ] 验证证据记录了环境、数据库、账号/租户标识、通过/失败项和证据路径。
- [ ] PR 说明修复的问题类型、不变性结论、实际验证命令和遗留风险。
- [ ] 当前模块验收并合并后，再开始下一个模块。

## 11. 建议的证据摘要模板

```text
模块：
基准 commit/版本：
目标 commit/版本：
不变契约：

单元测试：<command> -> tests/failures/errors/skipped
集成测试：<command> -> tests/failures/errors/skipped
API 测试：<command> -> 用例和结果
UI/E2E：<command> -> 用例和结果

新数据库：<database-id>
账号/租户标识：<non-secret-id>
Flyway：
Resource sync：
健康检查：
关键写入/查询：

JAR/包检查：
与改前基线的行为差异：
遗留问题/风险：
证据路径：
```

## 12. 相关入口

- [模块历史债务治理经验总结](../../designs/2026-07-15-module-architecture-debt-remediation-experience.md)
- [Mango PMO 总流程](../../../mango-pmo/rules/00-dev-flow.md)
- [AI 交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)
- [测试用例与自动化流程](../../../mango-pmo/rules/09-test-case-automation-flow.md)
- [后端测试规范](../../../mango-pmo/rules/backend/08-test.md)
- [API 规范](../../../mango-pmo/rules/backend/03-api.md)
- [数据库规范](../../../mango-pmo/rules/backend/04-db.md)
- [Resource Registry 使用说明](../../../mango/mango-platform/mango-resource/README.md)
