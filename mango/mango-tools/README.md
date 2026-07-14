# Mango Tools

## 1. 概览
`mango-tools` 提供 Mango 后端开发期工具，包含无服务器 Java/Spring 架构门禁、质量检查、模块脚手架、CRUD 脚手架和权限资源生成。

主要使用者是 Mango 维护者、业务模块开发者、CI 门禁和 AI Agent。

## 2. 功能清单

| 能力 | 常用入口 |
|------|----------|
| PR 需要执行 Java/Spring 架构门禁 | `mvn verify`，专项调试使用 `mvn mango:architecture` |
| 新增业务模块时生成标准目录和基础文件 | `mango module add`（`@mango/cli`） |
| 快速生成 CRUD API、实体、Mapper、Service、Controller 等脚手架 | Maven 依赖 / HTTP API / Java API |
| 根据接口或资源生成权限数据草稿 | Maven 依赖 / HTTP API / Java API |


## 3. 能力边界
- 不作为业务运行时依赖。
- 不替代真实业务建模、权限设计、README 和测试。
- 不保证生成代码可直接生产发布；生成后必须人工补齐业务逻辑和验收。

## 4. 模块入口
- `mango-tools`：Maven 聚合模块。
- `mango-architecture-rules`：Enforcer 3.6.3、ArchUnit 1.4.2、PMD 7.26.0 规则 JAR。
- `mango-maven-plugin`：Maven 插件，goalPrefix 为 `mango`。
- `mango-architecture-verification`：Reactor 最后执行的自托管验证模块，避免要求预装本仓插件。
- 插件代码读取项目源码、migration、报告文件和 Git 变更，不写运行时数据库。

## 5. 接入方式
在 Mango Reactor 中统一调用：

```bash
mvn -f mango/pom.xml verify
```

只调试架构检查且 Reactor 已完成编译时可执行：

```bash
mvn -f mango/pom.xml mango:architecture
```

业务模块必须使用发布版 CLI 的 canonical 模板：

```bash
mango module add order --aggregate sales-order \
  --aggregate-name 销售订单 --module-name 订单模块 --project-dir <dir>
```

已有模块内可继续使用其余插件 goal：

```bash
mvn -f mango/pom.xml mango:gen-crud
mvn -f mango/pom.xml mango:gen-permission
```

`mango:gen-module` 已 fail-closed 退役，调用时只返回迁移指引，不再生成可能绕过
`api/core/starter/starter-remote`、typed CRUD 和 PMO 门禁的旧结构。

具体参数以对应 Mojo 源码和 PMO 规则为准。

## 6. 配置说明
`mango:check` 常用参数：

| 参数 | 示例 | 含义 |
|------|------|------|
| `rule` | `-Drule=all` | 检查规则范围。 |
| `output` | `-Doutput=json` | 输出格式。 |
| `reportFile` | `-DreportFile=target/mango-check-report.json` | JSON 报告路径。 |
| `mango.check.gate` | `all` 或 `no-new-violations` | `all` 阻断所有问题；`no-new-violations` 只阻断新增问题。 |
| `mango.check.changedFiles` | `path1,path2` | 显式指定变更文件。 |
| `mango.check.changedOnly` | `true` | 将 `no-new-violations` 的新增问题限定到可信变更文件，并作用于支持范围过滤的 Mango 规则。必须同时提供 `changedFiles` 或可解析的 `baseRef`；静态门禁的范围外发现写入 `baselineIssues`，规则扫描主动排除的发现写入 `excludedIssues`。 |
| `mango.check.baseRef` | `origin/main` | 未传 `changedFiles` 时用 Git diff 解析变更。 |
| `mango.check.baselineFile` | `target/baseline.json` | 存量问题基线报告。 |
| `mango.check.codeLevelExcludedModules` | `mango-platform/mango-file-preview` | 仅从 PMD、Checkstyle、SpotBugs 等代码级静态分析门禁中排除指定模块；Mango 自有规则仍会执行。 |
| `mango.check.staticFailurePolicy` | `block` 或 `report` | 静态分析委托失败时阻断或只报告。 |
| `mango.check.resourceStarterDependencyExceptions` | `artifactId=reason` | Resource Registry runtime 依赖例外。必须人工明确确认并写明理由，多个例外用英文逗号分隔。 |

依赖边界检查按 `api` / `support` / `core` / `starter-*` 模型执行：

- `api` 是接口契约，禁止依赖 `support`、`core`、`starter` 或 `starter-*`。
- `support` 是可被其它 `core` 依赖的公共支撑能力，禁止依赖 `core`、`starter` 或 `starter-*`，也禁止包含持久化和自动配置内容。
- `core` 可以依赖其它模块 `api` 或 `support`，禁止依赖其它模块 `core`、`starter` 或 `starter-*`。
- `starter-remote` 在 `io.mango` 依赖中只允许本模块 `api`、本模块 `support` 和 `mango-infra-feign-starter`，禁止直接依赖 `spring-cloud-starter-openfeign`。

Resource Registry 依赖边界作为 #186 的专项守护继续保留：非 `mango-resource` 模块默认只能依赖 `mango-resource-api`，不能直接依赖 `mango-resource-core`、`mango-resource-support`、`mango-resource-starter`、`mango-resource-sync-starter` 或 `mango-resource-starter-remote`。确需例外时，必须在命令行显式传入 `artifactId=reason`；缺少 reason 的例外不会生效。

PR 架构检查推荐命令：

```bash
mvn -f mango/pom.xml verify \
  -Dmango.architecture.base=origin/main
```

`mango:architecture` 的 `reportFile`、`rootDirectory`、`debtBaselineFile` 和
`globalEntityManifest` 均接受普通文件系统路径。Mango Maven `1.0.20` 起，这些参数通过
Maven/Plexus 支持的 `java.io.File` 接收并在插件内部转换为 `Path`，兼容 Maven 3；业务
POM 的现有 XML 配置和 `-Dmango.architecture.debtBaselineFile=<path>` 命令无需改写。

标准 partial PR 门禁把依赖准备和质量扫描分成两个阶段。干净 Runner 先对直接受影响模块执行带 `-am` 的跳过测试安装，把尚未发布的上游 SNAPSHOT 放入本地 Maven 仓库；随后质量阶段仍只选择直接受影响模块和外层架构验证模块，不带 `-am` 或 `-amd`。`mango:check` 委托 PMD、Checkstyle、SpotBugs 时只传入包含代码的 Reactor 模块，不把 `architecture-verification` 或 `mango-architecture-verification` 再次带入嵌套 Maven。PMD 和 Checkstyle 使用报告 goal 收集完整发现，再由 Mango 按 `all` 或 `no-new-violations` 统一判定；工具执行错误仍按 `mango.check.staticFailurePolicy` 处理，历史发现不会在新增问题分类前直接终止嵌套 Maven。外层 `mvn verify` 的架构检查不受影响。

partial Reactor 未包含 API 或 Service 接口模块时，ArchUnit 只能导入外部类型存根。架构检查器按命名和依赖位置继续校验 Controller 的直接 API 契约及 Service 端口，不对缺少方法元数据的存根执行 Controller/API 方法一致性检查；完整 Reactor 导入接口字节码后仍执行全部接口类型和方法一致性校验。

架构报告固定写入 `mango/target/mango-architecture-report.json`。schema v2 报告包含完整 Reactor `modules` 目录，并为 dependency、ArchUnit、PMD 和 blocking 问题写入唯一 `moduleKey`；无法归属、坐标冲突或 Reactor 数量不完整时 fail-closed。默认 `changed` 模式先定位变更影响的问题，再只从 Git base SHA 的 `mango-pmo/baselines/architecture/debt-budget.json` 扣除已批准 stable identities，剩余身份才阻断；PR head 自己修改的预算不能豁免当前新增。报告仍包含全量存量：删除文件会被识别，父 POM 变化传播到全部子 Reactor，`module.properties` 变化传播到同领域类，外置全局 Entity 清单变化传播到 Entity 规则。`-Dmango.architecture.mode=full` 用于专项全量治理。Git base 无法解析、baseline schema/identity 非法、PMD 解析失败、ArchUnit 未导入到字节码或预期 Java 输入为零时均 fail-closed。

一次完整 Reactor 扫描后，可复用同一报告查询或递减模块债务；目录 selector 覆盖其全部 Maven 子模块，唯一 artifactId 定位单模块：

```bash
node mango-pmo/tools/check-architecture-debt-budget.mjs \
  --module mango-platform/mango-system
node mango-pmo/tools/check-architecture-debt-budget.mjs \
  --module mango-system-core --write
node mango-pmo/tools/check-architecture-debt-budget.mjs \
  --base-ref "$(git merge-base HEAD origin/main)"
```

正式预算使用 schema v4，逐模块明细与全局聚合必须精确一致。模块写入只允许持平或下降；禁止使用部分 Reactor 报告写预算、跨模块转移违规或在模块模式接受增加。提交前必须执行最后一条无 `--module` 的全局终检。

业务模板的最终验证同时启用 POM-only `lockFullMode`、`lockFullReactor` 和 `requireFullScope`。即使 Enforcer 的 `requireProperty` 被 `skipRules` 跳过，架构插件仍拒绝 `mode=changed`；聚合静态检查要求每个实际包含 Java 编译源码的 Reactor 模块分别生成 PMD、Checkstyle 和 SpotBugs 报告，任一子模块缺报告都会阻断，纯 POM 聚合模块不要求报告。

使用仓库基线阻断新增问题：

```bash
cd mango
mvn mango:check \
  -Dmango.check.gate=no-new-violations \
  -Dmango.check.baselineFile=../mango-pmo/baselines/mango-check/no-new-violations-baseline.json \
  -Dmango.check.codeLevelExcludedModules=mango-platform/mango-file-preview \
  -DreportFile=target/mango-check-report.json
```

`mango-pmo/baselines/mango-check/no-new-violations-baseline.json` 只服务仍由 `mango:check` 管理的存量规则。Java/Spring 架构红线不读取这个旧静态 baseline，而是读取 Git base 中独立的 schema-v4 architecture identity 预算；迁移窗口只读兼容 schema v3 顶层 identity。历史 identity 即使所在类被修改也不误阻断，同规则替换、跨模块移动或任何新 identity 仍直接阻断。

`mango:architecture` 硬校验：Controller 实现 `XxxApi`、启用 `@Validated/@Valid`、只依赖 `IXxxService`、统一返回 `R<T>`；Service 使用 `Require + BizCode/ErrorCode` 校验业务前置条件且不返回或拼装 `R`；Entity、Mapper、Feign、Controller 和 Service 实现必须位于规定模块。

规则判定以业务边界为准：`MANGO-ARCH-BEAN-004` 只阻断手工构造的 Spring 托管 Service 实现，不把容器中注册的通用 `Map`、`Set` 或异常类型误认成业务 Service；组合 API 输入字段可用 `@Valid` 递归校验，`Require.rethrow` 被视为保持原异常语义的显式出口。三类判断均由正反例测试锁定。

检查当前变更文件内的模块菜单声明：

```bash
mvn mango:check \
  -Drule=module-menu \
  -Dmango.check.changedOnly=true \
  -Dmango.check.changedFiles=mango-platform/mango-demo/mango-demo-core/src/main/resources/db/migration/demo/V1__init_demo.sql \
  -Doutput=json \
  -DreportFile=target/module-menu-check-report.json
```

`module-menu` 的 `changedOnly` 模式仍会扫描 `baseDir` 下的菜单声明问题，但只有变更文件命中的问题进入阻断结果；未命中的历史问题会写入 JSON 报告的 `excludedIssues`，用于区分当前范围失败和历史参考问题。

## 7. API 与扩展
当前 Maven goals：

| Goal | Mojo | 用途 |
|------|------|------|
| `mango:architecture` | `ArchitectureMojo` | 聚合 Enforcer、ArchUnit、PMD 7 架构结果并阻断新增违规。 |
| `mango:check` | `CheckMojo` | 执行 Mango 后端质量检查和 PR 门禁。 |
| `mango:gen-module` | `GenModuleMojo` | 已退役；fail-closed 并指向 `mango module add`。 |
| `mango:gen-crud` | `GenCrudMojo` | 生成 CRUD 脚手架。 |
| `mango:gen-permission` | `GenPermissionMojo` | 生成权限资源草稿。 |

`CheckMojo` 当前仍较大，复杂度偏高，是既有技术债；后续拆分时应保持参数兼容和报告格式兼容。

## 8. 数据与初始化
无生产数据库。工具可能读取 migration 和权限资源文件，但不直接连接生产库，也不授予运行时权限。

## 9. 管理入口
工具可生成或检查菜单、权限、API 资源相关文件；真正的菜单入库、角色授权和租户隔离仍由 `mango-authorization`、Flyway migration 和业务初始化流程负责。

## 10. 快速开始
1. 使用 `mango module add` 生成模块；仅在已有模块内使用 `gen-crud` 生成聚合初稿。
2. 补齐业务模型、Controller 权限、migration、README、能力地图和测试。
3. 执行 `mvn verify`。
4. 检查生成文件是否符合 PMO 模板和模块边界。

发布完整非应用后端批次使用：

```bash
scripts/publish-maven-batch.sh --all-non-app \
  --release-version <version> \
  --verify-base-url <maven-consume-repository>
```

脚本在一次 Reactor deploy 中发布普通平台模块，并把 `mango-architecture-verification` 的扁平化 POM 单独部署；这样不会在排除 `mango-app/**` 的发布 Reactor 中错误执行该模块的全 Reactor verify 阶段。发布后仍统一回查全部目标坐标。

## 11. 问题排查
- `mango:architecture` 在 goal 执行前报告 `Cannot create instance of interface java.nio.file.Path`：升级到 Mango Maven `1.0.20` 或更高版本；这是旧插件 descriptor 的参数绑定缺陷，不能通过关闭架构门禁绕过。
- `mango:check` 报存量问题：PR 模式使用 `no-new-violations` 和 baseline，但不能把新增问题放进 baseline。
- `no-new-violations` 在分类前被 PMD/Checkstyle 历史问题直接终止：升级到 Mango Maven `1.0.19` 或更高版本；不得通过 `skip`、`report` 策略或修改业务规则绕过。
- partial PR 在干净 Runner 报上游 SNAPSHOT 找不到：确认 workflow 已先执行受影响模块的依赖安装阶段，并且 scope classifier 输出了非空 `maven_dependency_projects`；不要给后续质量命令追加 `-am`。
- `mango:check` 的嵌套 Maven 报 `mango.architecture.skip` 等治理属性缺失：确认使用的 Mango Maven 插件已经包含治理聚合模块过滤；架构验证应由外层 `mvn verify` 执行，不应进入 PMD、Checkstyle、SpotBugs 的嵌套 Reactor。
- 生成代码编译不过：脚手架只提供结构，业务字段、依赖和 mapper 仍要补齐。
- 生成权限后页面仍无按钮：还需要菜单资源入库、角色授权和前端按钮权限接入。
- Maven 批次只缺 `mango-architecture-verification` POM：使用当前 `publish-maven-batch.sh` 重新规划新版本发布；不可变版本已经尝试后只能先核对仓库事实，禁止整批重发。

## 12. 相关文档
- [后端模块规范](../../mango-pmo/rules/backend/05-module.md)
- [模块菜单规范](../../mango-pmo/rules/backend/11-module-menu.md)
- [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../mango-pmo/rules/05-ai-delivery-quality.md)

## 13. 补充资料
- [Mango 能力地图](../../mango-docs/capabilities/README.md)
