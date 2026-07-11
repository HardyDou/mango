# Java/Spring 无服务器架构门禁设计

## 1. 设计结论

使用 Maven 本地构建链替换当前 Java 源码正则门禁：

- Maven Enforcer 自定义规则检查 Reactor/POM 依赖方向。
- ArchUnit 检查编译后字节码中的包、类型、继承、注解和依赖关系。
- PMD 7 自定义 Java Rule 检查源码 AST 中的方法、参数、注解、泛型返回和调用表达式。
- JUnit 5 正反样例验证每条硬红线。
- 所有规则由 `mvn verify` 执行，不引入 SonarQube、数据库、Docker 服务、后台进程或生产运行时依赖。

版本固定为 Maven Enforcer Plugin 3.6.3、ArchUnit 1.4.2、PMD 7.17.0。PMD 不直接采用 2026 年最新版本，是为了保持 Mango Java 17 构建基线兼容；升级必须重新执行本设计的全部正反和性能验收。

## 2. 设计输入与范围

### 2.1 来源证据

| 结论 | 来源 | 证据 |
|---|---|---|
| 不使用 Server | 用户确认 | 2026-07-11：不希望依赖 SonarQube 或其它 Server |
| 使用 Enforcer + ArchUnit + PMD | 用户确认 | 2026-07-11：批准该单机方案 |
| 新增/修改硬阻断，存量先报告 | 用户确认 | 2026-07-11：批准效果与性能口径 |
| 当前正则不可继续作为硬门禁 | 实测 | 标准 `.mango/worktrees` 路径漏扫；`@RestControllerAdvice` 被误判 |
| Mango 存在存量候选问题 | 实测 | `/tmp` 普通 clone 对 `mango/` 扫描得到 698 条候选，工具失败 0 |

### 2.2 本次解决

1. `api/core/starter/starter-remote` 依赖和类型放置。
2. Feign 只能位于 `starter-remote`，继承唯一 `XxxApi`，具有合法 name/contextId/path。
3. Controller、Service、Mapper、Entity 的类型职责和依赖边界。
4. Controller 的 `@Validated`、`@RequestBody @Valid` 和协议模型校验。
5. Service 的业务动作前置条件结构。
6. `R<T>`、`Require`、`BizCode/ErrorCode` 的层级使用约束。
7. 本地与 CI 构建期不可绕过的 changed-only 门禁。

### 2.3 不在本次范围

- 不部署 SonarQube，不建设质量看板。
- 不修改生产运行时依赖或业务接口行为。
- 不使用静态规则替代关键业务 UNIT/API 测试。
- 不在本次提交中修复 Mango 扫描发现的全部存量业务代码。
- 不保留当前 Java 正则实现作为第二套长期规则；只允许在迁移验证期间用于结果对比，切换后删除对应硬判断。

## 3. 组件与边界

### 3.1 Maven Enforcer 规则

新增独立构建规则 JAR，只依赖 `enforcer-api` 和 Maven 公共 API，绑定 `validate`：

- 根据当前 `MavenProject` 的 artifactId、packaging 和 resolved dependencies 判定模块角色。
- 阻断 `api -> support/core/starter*`。
- 阻断 `core -> 其它 core/starter*`。
- 阻断 `starter-remote` 依赖非本域 api/support、非 `mango-infra-feign-starter` 的 Mango 模块。
- 阻断 `starter-remote -> spring-cloud-starter-openfeign`。
- 不解析 POM 文本，不依赖仓库绝对路径。

### 3.2 ArchUnit 规则

新增独立 ArchUnit 规则 JAR，由 Mango Maven Plugin 在 `compile` 后聚合执行一次：

- 使用 Reactor `target/classes` 作为唯一导入根，不扫描源码目录、`.mango`、模板或第三方未编译源码。
- Controller 必须位于 `starter`；明确登记的反向 Controller 可位于 `starter-remote`。
- Controller 实现本域 `XxxApi`，只依赖 `IXxxService` 或登记的等效接口。
- Service 实现只位于 `core`，实现 `IXxxService`，不实现 `XxxApi`。
- Entity、Mapper 只位于 `core`；Feign 只位于 `starter-remote`。
- 使用真实注解类型判断 `@RestController`，不得以名称前缀匹配 `@RestControllerAdvice`。
- 框架基础类型、第三方内嵌模块和生成模板必须通过精确包/模块作用域排除，禁止模糊目录名排除。

### 3.3 PMD 7 AST 规则

新增独立 PMD Java Rule JAR，接入仓库已有 `maven-pmd-plugin` 的同一次源码解析：

- Controller/API HTTP 方法返回 `R<T>`，不暴露 Entity/PO。
- Controller 类具有 `@Validated`；`@RequestBody` 参数具有 `@Valid`。
- Controller 不调用 `R.fail`，不声明 Mapper/Entity/Feign/具体 Service 字段。
- Service 不返回或调用 `R<T>`、`R.ok`、`R.fail`。
- Mapper 不声明注解 SQL，参数不使用 API Command/Query/VO。
- Service 业务动作中的 `Require` 调用必须传入实现 `BizCode` 的错误码类型。
- PMD 类型解析不可用、源码存在解析错误或规则异常时 fail-closed，不得输出假 PASS。

“某业务动作是否需要某个具体前置条件”不由 PMD 猜测。静态门禁只验证已声明业务动作的结构；业务正确性继续由 UNIT/API 测试证明。

### 3.4 统一入口与报告

统一入口仍为：

```bash
mvn verify
```

执行顺序：

1. `validate`：Enforcer 依赖门禁。
2. `compile`：生成 ArchUnit 输入字节码。
3. `verify`：ArchUnit 架构门禁和 PMD 7 AST 门禁。
4. Mango Maven Plugin 汇总规则编号、文件、行号、模块、严重级别、changed-only 状态和引擎来源。

报告写入 `target/mango-architecture-report.json`；运行日志和性能数据写入 `.runtime/pmo/`，不得提交运行过程数据。

## 4. Changed-only 与存量策略

- Git base/head 必须可解析；未知或空变更集合时 fail-closed。
- 新增、修改、rename 后的新旧路径命中硬红线时，Enforcer/ArchUnit/PMD 任一失败即阻断。
- 新增或修改硬红线不得被 baseline、普通例外或工具声明抵消。
- 全仓存量报告使用稳定指纹，只允许减少；存量未触碰文件暂不阻断普通 PR。
- 主干定时或专项命令执行全仓模式并输出完整报告。

## 5. 已知边界和错误处理

| 场景 | 处理 |
|---|---|
| 仓库位于 `.mango/worktrees` | 必须正常扫描；输入使用 Reactor/classpath 和相对源码根，不检查绝对路径祖先段 |
| `@RestControllerAdvice` | 不属于普通 Controller 规则 |
| 内嵌第三方源码 | 由精确 Maven module/package scope 排除并有正反测试 |
| 模板占位符 | 使用生成后的真实 Maven 项目验证，不直接把占位源码当生产类分析 |
| 字节码未生成 | ArchUnit 阻断并提示先完成 compile，不返回空集合 PASS |
| PMD 类型解析失败 | 阻断并输出 processing error |
| 规则执行数为 0 | 对预期存在 Java 输入的 Reactor 判失败 |
| Enforcer/PMD/ArchUnit 版本变化 | 重新执行全部正反、真实仓和性能基准 |

## 6. 测试与验收

### 6.1 冻结正反样例

| ID | 场景 | 期望 |
|---|---|---|
| TC-ARCH-001 | 合法 api/core/starter/starter-remote Reactor | PASS |
| TC-ARCH-002 | api 依赖 core/starter | FAIL |
| TC-ARCH-003 | core 依赖其它 core/starter | FAIL |
| TC-ARCH-004 | Feign 放错模块、继承多个 API、name/contextId/path 错误 | FAIL |
| TC-ARCH-005 | 合法 Controller 实现 API、Validated/Valid、只依赖接口 | PASS |
| TC-ARCH-006 | Controller 缺 API/Validated/Valid 或依赖 Mapper/具体 Service | FAIL |
| TC-ARCH-007 | `@RestControllerAdvice` | PASS，不执行 Controller 规则 |
| TC-ARCH-008 | 合法 Service 使用 Require + BizCode 且不返回 R | PASS |
| TC-ARCH-009 | Service 返回 R、直接实现 API、错误 Require 参数 | FAIL |
| TC-ARCH-010 | 合法 Mapper | PASS |
| TC-ARCH-011 | Mapper 注解 SQL或接收 API 模型 | FAIL |
| TC-ARCH-012 | 仓库根路径包含 `.mango/worktrees` | 与普通路径结果完全一致 |
| TC-ARCH-013 | PMD/ArchUnit 解析或执行失败 | FAIL_CLOSED |
| TC-ARCH-014 | CRUD 脚手架生成项目 | 编译和全部架构门禁 PASS |

关键违规样例每类至少一个正例和一个反例；禁止只断言消息文本，必须断言真实构建退出码和规则编号。

### 6.2 真实仓验证

- Mango：全部 210 个 Maven POM 和实际 Reactor 模块。
- `baohan-system`：保持只读，扫描前后 Git HEAD、tracked/untracked 快照一致。
- 对两个仓库分别执行普通路径与包含 `.mango/worktrees` 的路径一致性验证。
- 对临时普通 clone 注入六类违规，六类必须全部失败；源仓不得修改。

### 6.3 效果阈值

- 冻结硬红线检出率：100%。
- 冻结合法正例误报率：0%。
- 标准 worktree 与普通路径结果差异：0。
- `@RestControllerAdvice` 误报：0。
- 工具失败被记为 PASS：0。
- 对现存候选问题进行分层抽样人工复核；进入硬阻断的规则 precision 必须不低于 98%，合法关键样例误报必须为 0。

### 6.4 性能阈值

在同一机器、相同 JDK、Maven 本地缓存预热后记录至少 5 次：

- changed-only 在编译完成后的额外中位耗时不超过 10 秒。
- Mango 全 Reactor 架构检查额外中位耗时不超过 30 秒。
- 同一进程内 ArchUnit 只导入一次 Reactor 字节码。
- PMD 自定义规则加入既有 PMD AST 遍历，不启动第二次全仓 PMD。
- 报告冷缓存、热缓存、样本数、中位数和最大值；不得只报告最快一次。

## 7. 实施和切换

1. 建立三个隔离规则模块：Enforcer、ArchUnit、PMD，统一规则编号和测试 fixture。
2. 先修复路径作用域和注解语义测试，再实现七项规则。
3. 在同一冻结输入上并行运行旧正则与新引擎，只用于差异定位。
4. 新引擎满足效果、路径一致性和性能阈值后，删除 Java 七项对应的旧正则硬判断。
5. 将 Enforcer、ArchUnit、PMD 绑定 `mvn verify`，CI 只调用统一入口。
6. 更新 Mango 工具说明和业务 PMO 发布物；不发布、不 push、不合并，直到用户明确批准。

## 8. 风险与取舍

- ArchUnit依赖编译成功，因此不能替代 validate 阶段的依赖检查。
- PMD 7 自定义 API存在版本演进成本，使用固定版本和规则测试控制升级风险。
- 存量 698 条包含真实问题和误报候选，不能作为 baseline 自动冻结；必须先完成规则精度复核。
- 静态工具只能证明结构，不证明某条业务前置条件的具体业务含义。
- 三个规则 JAR 增加 Maven 构建依赖，但不增加部署组件、运行进程或生产依赖。

## 9. 自检

| 检查项 | 结果 | 说明 |
|---|---|---|
| 用户批准方案是否完整覆盖 | PASS | Enforcer、ArchUnit、PMD 7，无 Server |
| 七项 Java/Spring 问题是否逐项映射 | PASS | 见第 2、3 节 |
| 当前已发现漏扫和误报是否有验收 | PASS | TC-ARCH-007、012、013 |
| changed-only、存量、失败策略是否明确 | PASS | 第 4、5 节 |
| 效果和性能是否有可测阈值 | PASS | 第 6 节 |
| 是否存在占位、待定或模糊实现选择 | PASS | 版本、组件、入口和切换条件均已固定 |
| 是否引入 Server 或生产依赖 | PASS | 明确禁止 |

最终动作：`NEXT`。
