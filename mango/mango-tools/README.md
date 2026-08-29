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

反向内部 Controller（例如独立 Worker 接收平台派发）必须在所属聚合模块显式登记精确类名和
反向路径，并同步到 `mango-architecture-verification` 的完整 Reactor 验证配置。该登记只建模
已审计的反向适配器，不会关闭 Controller/API、参数绑定、返回包装或内部调用安全规则。

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

Mango Maven `1.0.31` 起，静态分析的嵌套 Maven 会继承外层已解析的本地仓库、存在的 user/global settings、offline/update 模式、active profiles，以及 `revision`、`sha1`、`changelist` 三个 CI-friendly 版本属性。任意其它用户属性都不会透传，避免把密码或 token 写入子进程参数和门禁日志。使用 `-Dmaven.repo.local=<isolated-repository>`、`-s <settings.xml>` 或私有镜像的业务构建因此与外层依赖解析保持一致。

`mango:check` 委托 Checkstyle 时始终显式选择规则文件，不使用 Maven Checkstyle 插件的 Sun 默认规则。规则选择优先级如下：

1. `-Dmango.check.checkstyleConfigLocation=<path>` 指定的业务规则。
2. `-Dcheckstyle.config.location=<path>` 指定的标准 Checkstyle 规则。
3. 项目根目录的 `config/quality/checkstyle.xml`，业务项目可以直接维护该文件。
4. Mango Maven 插件内置的 Mango Checkstyle 规范。

Mango 内置规则及 CLI 业务模板允许使用三元表达式，也不对单行字符数设置硬限制；复杂度默认只检查圈复杂度，最大值为 15，不启用 NPath 和布尔表达式复杂度检查。业务项目如需更严格规则，可以通过上述自定义规则入口覆盖。

例如使用企业自定义规则：

```bash
mvn mango:check \
  -Dmango.check.checkstyleConfigLocation=config/quality/company-checkstyle.xml
```

配置路径不存在或规则文件无效时，Checkstyle 会按现有静态工具失败策略报错；不会静默切换到 Sun 规则。JSON 报告的 `gateMessages` 会标识本次使用的是自定义、项目还是 Mango 内置规则。

当 partial Reactor 中所有业务模块都没有 Java 源码时，`mango:architecture` 仍执行 Maven 依赖检查、模块归属和 schema v2 报告生成；bytecode、PMD 与 Java 命名空间检查以空输入记录为 0。非法依赖仍进入 blocking issues，不能通过零源码状态绕过门禁。

partial Reactor 未包含 API 或 Service 接口模块时，ArchUnit 只能导入外部类型存根。架构检查器按命名和依赖位置继续校验 Controller 的直接 API 契约及 Service 端口，不对缺少方法元数据的存根执行 Controller/API 方法一致性检查；完整 Reactor 导入接口字节码后仍执行全部接口类型和方法一致性校验。

架构报告固定写入 `mango/target/mango-architecture-report.json`。schema v2 报告包含完整 Reactor `modules` 目录，并为 dependency、ArchUnit、PMD 和 blocking 问题写入唯一 `moduleKey`；无法归属、坐标冲突或 Reactor 数量不完整时 fail-closed。默认 `changed` 模式先定位变更影响的问题，再只从 Git base SHA 的 `mango-pmo/baselines/architecture/debt-budget.json` 扣除已批准 stable identities，剩余身份才阻断；PR head 自己修改的预算不能豁免当前新增。报告仍包含全量存量：删除文件会被识别，父 POM 变化传播到全部子 Reactor，`module.properties` 变化传播到同领域类，外置全局 Entity 清单变化传播到 Entity 规则。`-Dmango.architecture.mode=full` 用于专项全量治理。Git base 无法解析、baseline schema/identity 非法、PMD 解析失败、包含 Java 源码却未导入到字节码或预期 Java 输入为零时均 fail-closed；整个 Reactor 本身没有 Java 源码时按前述 dependency-only 语义执行。

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

所有 `mango:check` JSON 报告都会为 `issues`、`newIssues`、`baselineIssues` 和 `excludedIssues` 中的问题写入稳定指纹。文件路径在报告生成时按 `baseDir` 转为项目相对路径；`no-new-violations` 加载基线时优先使用报告内指纹，因此同一业务仓库在不同绝对 worktree 路径下可以直接复用基线。旧版本生成且缺少指纹的报告仍按现有兼容逻辑读取；跨 worktree 使用前应由包含本修复的插件重新生成一次基线。

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
| `mango:baseline-generate` | `BaselineGenerateMojo` | 在 API 制品构建中回放模块 V，生成并验证每模块唯一 cold baseline。 |

`CheckMojo` 当前仍较大，复杂度偏高，是既有技术债；后续拆分时应保持参数兼容和报告格式兼容。

### 7.1 构建期 cold baseline

业务 API 只生成 Flyway baseline 时可以在 `generate-resources` 执行；同时物化 Resource 时必须绑定到 `prepare-package` 并配置最终业务应用主类。生成器扫描当前工程和运行时依赖 JAR 中的 `db/migration/<module>/V*.sql`，在临时 MySQL 中按 `moduleOrder` 回放；配置 Resource 后分别在 replay/determinism 库执行正式 `bootstrap apply`，再把结果写入：

```text
target/generated-resources/db/baseline/<module>/B<version>__baseline.sql
target/generated-resources/META-INF/mango/baseline-manifest.json
```

这些目录会被注册为 Maven resource。普通 JAR 从 `db` 目录下的 `baseline` 子目录读取。Spring Boot 可执行 JAR 中，B SQL 位于下面所示的 `BOOT-INF` classes 层级，manifest 必须保留在 JAR 根目录的 `META-INF/mango/baseline-manifest.json`，不能移动到 `BOOT-INF/classes/META-INF/...`：

```text
BOOT-INF
└── classes
    └── db
        └── baseline
META-INF
└── mango
    └── baseline-manifest.json
```

源码目录不会生成 B；Docker 镜像只复制已经完成校验的最终 JAR。

最小 POM 配置和 Jenkins 使用方式见[业务 API 构建期 cold baseline](../../mango-docs/guides/business-integration/build-time-cold-baseline.md)。常用参数：

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `mango.baseline.jdbcUrl` | 无 | 一次性 MySQL 服务器 JDBC URL；必填，URL 中原数据库名不会被使用。 |
| `mango.baseline.username` | `root` | 一次性数据库账号。 |
| `mango.baseline.passwordEnv` | `MANGO_BASELINE_DB_PASSWORD` | 保存密码的环境变量名；密码不进入 POM、manifest 或日志。 |
| `mango.baseline.characterSet` | `utf8mb4` | replay、determinism 和 verify schema 使用的目标字符集；默认与 Mango CLI 创建的业务数据库一致。 |
| `mango.baseline.collation` | `utf8mb4_unicode_ci` | 三类临时 schema 使用的目标排序规则；默认与 Mango CLI 创建的业务数据库一致。 |
| `mango.baseline.includedModules` | 全部发现模块 | 当前 API 制品需要打包的模块清单。 |
| `mango.baseline.moduleOrder` | 模块名稳定顺序 | 显式配置时必须恰好包含所有发现模块一次。 |
| `mango.baseline.moduleGroups` | 全部为 `default` | `module=group` 列表；同组模块使用同一对临时 schema，不同组隔离。 |
| `mango.baseline.resourceApplicationClass` | 无 | 最终 Spring Boot 应用主类；配置后启用 `PORTABLE` Resource 数据库基线，并要求 goal 绑定 `prepare-package`。 |
| `mango.baseline.resourceTimeoutSeconds` | `300` | 每次 Resource baseline 应用执行的超时秒数，必须大于 0。 |
| `resourceAdditionalClasspathElements` | 无 | 仅加入 Resource baseline 构建子进程的外部只读目录或 JAR；不存在或不可读时失败，不复制进应用 JAR。 |
| `mango.baseline.outputDirectory` | `target/generated-resources` | 仅构建目录；不要指向 `src/main/resources`。 |
| `mango.baseline.keepSchemas` | `false` | 诊断时保留临时 schema；正常 CI 保持关闭。 |

生成过程使用 replay/determinism/verify 三套 schema，三者都会显式使用 `mango.baseline.characterSet` 和 `mango.baseline.collation`，不继承构建机 MySQL 的 server 默认值。插件在创建临时 schema 前校验字符集与排序规则组合；未知组合或不安全的标识符会直接阻断构建。B SQL 和 manifest 分别固化实际表结构以及 `targetCharacterSet`、`targetCollation`，生成指纹也包含这两个目标值。业务项目如果覆盖默认值，目标空库必须使用相同字符集和排序规则，避免 B 之后新增的 V migration 重新继承另一套数据库语义。

前两套 schema 独立回放 V 和便携 Resource 后，生成器会把标准及历史运行审计时间列规范化为固定值，例如 `created_at`、`updated_at`、`create_time`、`update_time` 和 `last_sync_time` 的非空值统一为 `2000-01-01` 或 `2000-01-01 00:00:00`，再生成 B SQL、manifest 和 generation fingerprint。`NULL` 保持为空，Resource Registry `last_sync_time` 与目标表 `updated_at` 使用同一规范值，因此恢复后仍保留运行期修改判断语义。双库确定性比较继续忽略这些受控审计列，verify schema 连续执行 B 两次后按全部列比较结构和静态数据。`publish_time` 等普通业务时间不属于审计列，不会被忽略或规范化；业务列中的 `UUID()`、当前时间或环境值继续阻断构建。

便携 Resource 的业务主键必须由声明中的固定 ID 或稳定业务身份确定。生成器只规范化非语义审计时间，不会忽略普通业务列或主键差异；Authorization、Calendar、Identity、Notice 等正式 Handler 因此必须在两套独立空库中生成相同目标行。若某个 Handler 仍依赖雪花、自增、随机值、当前业务时间或调用顺序，replay/determinism 比较会直接失败并定位差异，而不是产出不可复现的 BSQL。

Resource baseline 当前只支持一个 datasource group。`PORTABLE` Handler 的目标状态与 Registry hash 进入 BSQL；`ENVIRONMENT_REQUIRED` Handler 和没有本地 Handler 的远程目标在恢复后处理，构建期不会调用远程 Dispatcher。构建专用 Bootstrap 只选择 Mango Resource contributor，普通业务、租户和其它运行期 contributor 在目标环境 Bootstrap 执行。生成前清除环境 receipt、Resource 审计和 Bootstrap 运行记录，避免把构建过程当成部署成功回执。

如果最终应用通过 Spring Boot `loader.path` 等入口加载 JAR 外资产，应在插件 execution 中配置 `resourceAdditionalClasspathElements`，让构建期 Resource baseline 使用同一份资产。该参数不改变最终 JAR 内容；业务镜像或部署包仍负责携带外部目录。

结构比较使用 MySQL 元数据的结构化语义快照，不按 `SHOW CREATE` 文本逐字比较。表、列、索引和约束的首个差异会定位到 `table:<name>`、`table:<name>.column:<name>`、`table:<name>.index:<name>` 或 `table:<name>.constraint:<name>`；隐式继承与显式声明只要落库后的 charset/collation 相同即视为等价。视图和触发器使用去除环境噪声后的 canonical DDL 比较。静态数据按列读取并用二进制十六进制值比较，因此字符集转换和不可见字节不会被字符串展示掩盖。存储过程、函数和事件当前 fail closed；重复版本、跨模块对象所有权、制品碰撞、缺失或被修改的 B、不可重入、结构或数据不等价都会使构建失败。安装新生成目录前保留上一次结果，安装异常时回滚。

## 8. 数据与初始化
质量和脚手架 goal 不连接生产数据库。`baseline-generate` 只连接构建参数指定的一次性 MySQL，创建并清理带随机后缀的 replay/determinism/verify schema；不要向它提供生产或共享业务数据库账号。

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
- `mango:check` 的嵌套 Maven 在外层隔离仓库已安装依赖后仍转向默认仓库：升级到 Mango Maven `1.0.31` 或更高版本，并保留外层 `-Dmaven.repo.local`/`-s` 参数；不要把尚未验证的候选版本提前发布到远端来绕过本地门禁。
- `mango:check` 的嵌套 Maven 报 `mango.architecture.skip` 等治理属性缺失：确认使用的 Mango Maven 插件已经包含治理聚合模块过滤；架构验证应由外层 `mvn verify` 执行，不应进入 PMD、Checkstyle、SpotBugs 的嵌套 Reactor。
- 生成代码编译不过：脚手架只提供结构，业务字段、依赖和 mapper 仍要补齐。
- 生成权限后页面仍无按钮：还需要菜单资源入库、角色授权和前端按钮权限接入。
- Maven 批次只缺 `mango-architecture-verification` POM：使用当前 `publish-maven-batch.sh` 重新规划新版本发布；不可变版本已经尝试后只能先核对仓库事实，禁止整批重发。

## 12. 相关文档

- [后端模块规范](../../mango-pmo/rules/backend/05-module.md)
- [模块菜单规范](../../mango-pmo/rules/backend/11-module-menu.md)
- [能力说明维护规范](../../mango-pmo/rules/08-capability-docs.md)
- [AI 交付质量门禁](../../mango-pmo/rules/05-ai-delivery-quality.md)

## 13. Issue #690 构建消费说明

业务仓若使用过 Maven `1.0.30` 或其它 `1.0.3x`，应等待包含 #690 修复的新 release tuple，并同步 Maven、CLI、PMO 和前端矩阵。BSQL 生成目录必须进入最终 JAR 的 `BOOT-INF` 下 `classes`、`db`、`baseline` 嵌套层级，manifest 保留在 JAR 根 `META-INF/mango/baseline-manifest.json`；结构化比较、确定性回放、重复版本、对象所有权和不可重入校验任一失败都应 fail closed。升级后至少执行 `mvn verify`、`mvn install`、独立 consumer offline verify 和 Boot JAR `java -jar` health 验证，不能通过跳过 baseline 或缩小 Reactor 掩盖问题。

## 14. 补充资料
- [Mango 能力地图](../../mango-docs/capabilities/README.md)
