# Java/Spring 无服务器架构门禁交付与验收报告

## 1. 结论

- **批准全量推广到 Mango 及业务项目的新增、修改代码**：统一执行 `mvn verify`，新增或触碰的七类 Java/Spring 架构红线必须阻断。
- **不把存量问题一次性设为阻断**：Mango 全量扫描检出 2,377 项存量候选；日常 changed-only 模式通过，专项 full 模式用于治理清单并按模块消减。
- 不依赖 SonarQube Server、数据库、Docker 或后台进程。构建链固定为 Maven Enforcer 3.6.3、ArchUnit 1.4.2、PMD 7.26.0。
- 旧 `mango:check all` 不再执行 Java 架构正则规则；旧规则只保留显式兼容诊断并打印非权威警告，不能抵消或替代 `mango:architecture`。
- 本任务没有 UI、页面或用户交互变化。按用户定义，E2E 即 UI 测试，因此 E2E 与截图为 `EXCEPTION: 构建期后端静态门禁无 UI 入口`。

## 2. 目标、范围和边界

目标是让以下问题从“文档建议”变成构建期可执行红线：

1. `api/core/starter/starter-remote` 边界失效。
2. Feign 使用位置和职责混乱。
3. Controller、Service、Mapper、Entity 职责混乱。
4. Controller 缺少基础字段校验。
5. Service 缺少业务前置条件校验结构。
6. Service 错误使用 `R<T>`、`BizCode/ErrorCode`、`Require`。
7. 规范存在但编码、构建阶段不能阻断违规。

不处理 Mango 存量业务代码，不修改 `baohan-system`，不证明具体业务规则本身正确，不引入服务端质量平台。

设计输入：

- `mango-docs/designs/2026-07-11-java-spring-static-architecture-gates-design.md`
- `mango-docs/plans/2026-07-11-java-spring-static-architecture-gates-plan.md`
- `mango-pmo/rules/backend/01-code.md`
- `mango-pmo/rules/backend/05-module.md`
- `mango-pmo/rules/backend/08-test.md`

## 3. 实现和规则映射

| 问题 | 实现引擎 | 关键规则 | 阻断阶段 |
|---|---|---|---|
| 模块依赖方向 | MavenProject + Enforcer | `DEP-001` 至 `DEP-006` | validate/verify |
| Feign 位置、继承和注解参数 | ArchUnit | `FEIGN-001` 至 `FEIGN-004` | verify |
| Controller/Service/Mapper/Entity 类型职责 | ArchUnit | `TYPE-001` 至 `TYPE-008` | verify |
| Controller 基础字段校验及 HTTP 返回 | PMD 7 Java AST | `CTRL-001` 至 `CTRL-004`、`HTTP-001` | verify |
| Service 前置条件结构 | PMD 7 Java AST | `SVC-004` | verify |
| Service 的 R/Require/错误码使用 | PMD 7 Java AST 和类型解析 | `SVC-001` 至 `SVC-003` | verify |
| Mapper 注解 SQL 和 API 模型泄漏 | PMD 7 Java AST | `MAPPER-001`、`MAPPER-002` | verify |

统一报告为 `mango/target/mango-architecture-report.json`。解析失败、类型解析失败、Git base 不可解析、预期输入为空均 fail-closed。

## 4. 测试用例登记与结果

| 用例 ID | 场景 | 优先级 | 层级 | 自动化 | 稳定断言 | 结果 |
|---|---|---|---|---|---|---|
| TC-ARCH-001 | 合法四层 Reactor | P0 | 单元/构建 | AUTO | 退出码 0 | PASS |
| TC-ARCH-002 | api 依赖 core/starter | P0 | 单元/构建 | AUTO | `DEP-001`、退出码非 0 | PASS |
| TC-ARCH-003 | core 依赖其它 core/starter | P0 | 单元 | AUTO | `DEP-002` | PASS |
| TC-ARCH-004 | Feign 放错位置、多个 API、参数错误 | P0 | 单元 | AUTO | `FEIGN-*` | PASS |
| TC-ARCH-005 | 合法 Controller | P0 | 单元 | AUTO | 无 Controller 违规 | PASS |
| TC-ARCH-006 | Controller 缺 API/校验或跨层依赖 | P0 | 单元 | AUTO | `TYPE-*`、`CTRL-*` | PASS |
| TC-ARCH-007 | `RestControllerAdvice` | P0 | 单元 | AUTO | 不按 Controller 误报 | PASS |
| TC-ARCH-008 | 合法 Service + Require + BizCode | P0 | 单元 | AUTO | 无 Service 违规 | PASS |
| TC-ARCH-009 | Service 返回 R、实现 API、错误码参数错误 | P0 | 单元 | AUTO | `SVC-*`、`TYPE-*` | PASS |
| TC-ARCH-010 | 合法 Mapper | P1 | 单元 | AUTO | 无 Mapper 违规 | PASS |
| TC-ARCH-011 | Mapper 注解 SQL/API 模型参数 | P0 | 单元 | AUTO | `MAPPER-*` | PASS |
| TC-ARCH-012 | 普通路径与 `.mango/worktrees` 路径 | P0 | 单元 | AUTO | 规则编号集合相同 | PASS |
| TC-ARCH-013 | PMD/ArchUnit 解析或执行失败 | P0 | 单元 | AUTO | 失败而非空 PASS | PASS |
| TC-ARCH-014 | CRUD 脚手架生成代码 | P0 | 生成物集成 | AUTO | PMD 7 架构门禁通过 | PASS |

规则模块与 Maven 插件共执行 197 个测试：197 通过，0 失败，0 错误，0 跳过；其中新架构规则模块 27 个测试全部通过。

## 5. 真实项目实验

### 5.1 Mango 全 Reactor

| 模式 | 命令 | 结果 | 架构耗时 | 说明 |
|---|---|---|---:|---|
| changed-only | `mvn -f mango/pom.xml -DskipTests verify` | PASS，阻断 0 | 6,235 ms | 全 Reactor 构建成功，Enforcer 与 `mango:architecture` 均已在 verify 生命周期真实执行 |
| full | `mvn -f mango/pom.xml -DskipTests -Dmango.architecture.mode=full verify` | EXPECTED FAIL | 5,384 ms | 检出 2,377 项存量候选，证明全量模式真实阻断 |

Mango 全量候选分布：依赖 3、ArchUnit 38、PMD 2,336。主要规则数量为：`SVC-003` 1,265、`SVC-002` 445、`SVC-001` 321、`SVC-004` 132、`CTRL-003` 57、`CTRL-001` 39、`CTRL-004` 34、`MAPPER-001` 25、`HTTP-001` 18，其余类型与 Feign 规则 41。

这里的 full 失败不是门禁实现失败，而是存量代码确有候选问题；因此推广策略必须是“新增/修改绝对阻断，存量专项治理”，不能把 2,377 项直接冻结成合法基线。

### 5.2 `baohan-system` 只读消费实验

- 正式源仓：`/Users/hardy/work/Yunxin/baohan-system`
- 扫描前后 HEAD：`098e133b2a027f05d62766451d60828916fb652e`
- 扫描前后正式源仓状态：clean，未修改任何文件。
- 在 `.runtime/pmo/baohan-system-architecture` 的本地实验副本编译 42 个 Maven 模块并执行门禁。
- 检出 232 项候选：依赖 11、ArchUnit 15、PMD 206；changed-only 阻断为 0，架构扫描耗时 2,284 ms。
- 实际识别到 core 依赖 runtime starter、Feign 多 API、API 层 Entity、Controller 未实现 API、Service 返回 R 等问题，证明规则能消费非 `io.mango` 业务 groupId，而不是只对 Mango 自测有效。

### 5.3 反例、路径和自托管实验

- 真实 Enforcer 合法 fixture：`mvn validate` 退出码 0。
- 注入 api 依赖 core：`mvn validate` 退出码非 0，输出 `DEP-001`。
- 临时注入违规 Service：changed-only 构建检出 3 项并失败；移除注入文件后构建恢复通过。
- 同一编译产物复制到普通路径和 `.mango/worktrees` 路径，ArchUnit 与 PMD 规则编号集合一致。
- 空 Maven 本地仓库从 Reactor 构建成功，总耗时 19.10 秒；不要求人工预安装规则 JAR 或插件。

## 6. 性能基线

同一机器、同一 JDK、本地 Maven 缓存预热、编译完成后连续五轮全量架构引擎耗时：5,102、5,483、5,396、5,438、5,455 ms；中位数 5,438 ms，最大值 5,483 ms。

| 指标 | 阈值 | 实测 | 结论 |
|---|---:|---:|---|
| changed-only 架构额外中位耗时 | <= 10 秒 | 5.438 秒 | PASS |
| Mango full 架构额外中位耗时 | <= 30 秒 | 5.438 秒 | PASS |
| 单次最新 changed-only 扫描 | <= 10 秒 | 6.235 秒 | PASS |
| 单次最新 full 扫描 | <= 30 秒 | 5.384 秒 | PASS |
| PMD AST 遍历 | 每次门禁 1 次 | 1 次 | PASS |
| ArchUnit Reactor 字节码导入 | 每次门禁 1 次 | 1 次 | PASS |

## 7. 交付台账

| ID | 来源 | 要求 | 设计决策 | 代码交付物 | README/使用说明 | 需求/设计文档 | E2E 脚本 | 测试结果基线 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | 用户要求 | 七类 Java/Spring 红线可阻断 | Enforcer + ArchUnit + PMD 7 | `mango/mango-tools/mango-architecture-rules` | `mango/mango-tools/README.md` | `mango-docs/designs/2026-07-11-java-spring-static-architecture-gates-design.md` | EXCEPTION: 本任务只修改后端构建期规则，不存在可操作页面，用户已定义 E2E 为 UI 测试 | `mango-docs/evidence/2026-07-11-java-spring-architecture-gates/delivery-contract.md` | 正反单元和构建测试 | DONE | 本文第 4、5 节 |
| TASK-002 | 用户要求 | 单机运行且无需 Server | Maven Reactor 自托管 | `mango/mango-tools/mango-architecture-verification` | `mango/mango-tools/README.md` | `mango-docs/designs/2026-07-11-java-spring-static-architecture-gates-design.md` | EXCEPTION: 本任务只修改后端构建期规则，不存在可操作页面，用户已定义 E2E 为 UI 测试 | `mango-docs/evidence/2026-07-11-java-spring-architecture-gates/delivery-contract.md` | 空本地仓库构建 | DONE | 本文 5.3 |
| TASK-003 | 用户要求 | Mango 全模块验证 | changed-only 日常阻断，full 输出存量 | `mango/mango-tools/mango-maven-plugin/src/main/java/io/mango/plugin/architecture/ArchitectureMojo.java` | `mango/mango-tools/README.md` | `mango-docs/designs/2026-07-11-java-spring-static-architecture-gates-design.md` | EXCEPTION: 本任务只修改后端构建期规则，不存在可操作页面，用户已定义 E2E 为 UI 测试 | `mango-docs/evidence/2026-07-11-java-spring-architecture-gates/delivery-contract.md` | 全 Reactor verify | DONE | 本文 5.1 |
| TASK-004 | 用户要求 | 业务项目真实检出 | 只读源仓，本地副本执行 | `mango/mango-tools/mango-architecture-verification/pom.xml` | `mango/mango-tools/README.md` | `mango-docs/designs/2026-07-11-java-spring-static-architecture-gates-design.md` | EXCEPTION: 本任务只修改后端构建期规则，不存在可操作页面，用户已定义 E2E 为 UI 测试 | `mango-docs/evidence/2026-07-11-java-spring-architecture-gates/delivery-contract.md` | 42 模块消费实验 | DONE | 本文 5.2 |
| TASK-005 | PMO | 不保留双重架构权威 | `mango:check all` 移除旧正则架构调用 | `mango/mango-tools/mango-maven-plugin/src/main/java/io/mango/plugin/check/CheckMojo.java` | `mango/mango-tools/README.md` | `mango-pmo/rules/backend/01-code.md` | EXCEPTION: 本任务只修改后端构建期规则，不存在可操作页面，用户已定义 E2E 为 UI 测试 | `mango-docs/evidence/2026-07-11-java-spring-architecture-gates/delivery-contract.md` | 插件回归与 CI 配置检查 | DONE | 本文第 1、4 节 |

台账共 5 项，DONE 5 项，EXCEPTION 状态 0 项；E2E 物料列有 5 个有依据的不适用例外。未完成项 0。

## 8. 测试结果基线

| 基线 ID | 覆盖台账 ID | E2E 脚本 | 测试命令 | 环境/版本 | 数据库或数据集 | 账号/租户标识 | 结果摘要 | 失败/阻塞/例外 | 报告/截图/日志路径 | 行为变化 |
|---|---|---|---|---|---|---|---|---|---|---|
| BASELINE-ARCH-20260711 | TASK-001 至 TASK-005 | EXCEPTION: 本任务只修改后端构建期规则，不存在可操作页面，用户已定义 E2E 为 UI 测试 | 规则测试、changed/full verify、只读消费实验 | macOS，本地 JDK/Maven，固定工具版本 | Mango Reactor、baohan 只读副本、正反 fixture | 不适用 | 197/197 测试通过；changed PASS；full 检出 2,377 | full 的存量失败符合预期 | `mango-docs/evidence/2026-07-11-java-spring-architecture-gates/delivery-contract.md` | 从源码正则硬门禁切换为 Maven/字节码/AST 单一权威 |

本文件是当前最新基线；运行过程日志和临时项目只在 `.runtime/pmo`，不提交过程数据。

## 9. 业务开发交接

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| Mango 与业务开发者 | 正常开发执行 `mvn verify`；专项治理执行 `-Dmango.architecture.mode=full`；报告按规则编号定位 | `mango/mango-tools/README.md`、`mango-pmo/rules/backend/01-code.md`、本文 | `mvn -f mango/pom.xml verify` | 只分析 POM、编译字节码和 Java 源码，不连接数据库或账号 | 新增/修改违规必须修复；存量问题登记治理，不加入合法基线；工具异常按失败处理 | DONE |

## 10. 风险和推广边界

- 可以绝对拒绝的是已形式化的结构红线：模块依赖、类型位置/继承、Feign 契约、Controller 校验结构、Service 的 R/Require/错误码结构、Mapper 接口形态，以及工具/解析失败。
- 不能由静态门禁证明的是某个具体业务前置条件是否完整、错误码业务含义是否正确、跨服务全流程是否正确；这些仍需关键业务单元/API 测试，复杂主干流程需入口级测试。
- `SVC-004` 以业务动作命名前缀识别入口，能阻断明显漏校验，但不应替代领域验收。
- 推广批准范围是全仓和业务项目的 changed-only 硬门禁；存量全量清零必须另建治理批次，不能借 baseline 规避。
