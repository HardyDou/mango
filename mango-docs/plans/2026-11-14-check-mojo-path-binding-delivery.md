# 标准交付记录

## 1. 元数据

- 任务 ID：MANGO-1270
- 任务名称：修复 `mango:check` 的 `globalEntityManifest` 在 Maven 3 下无法绑定
- 交付模式：STANDARD
- 需求影响：L2 - 该参数按平台模板配置在所有业务仓的 `architecture-verification` 模块出现，解析失败会阻断业务仓唯一后端验证入口 `mvn verify` 和架构/静态门禁结论；不改变规则判定、数据库、接口和服务运行行为。
- 方案风险：L1 - 只在 `mango-maven-plugin` 内把一个参数类型从 `java.nio.file.Path` 改为 `java.io.File` 并在插件内部转换；影响面是单个 mojo 参数，回退为还原字段类型和一处调用。
- 最终风险：L2
- 工作区决策：CREATE - 从 `origin/main` 新建任务 worktree `/data/Mango/worktrees/mango-1270-check-mojo-path-binding` 和分支 `fix/1270-check-mojo-path-binding`；项目内 `@mango/cli` 执行 `workspace init`，分配 slot 6。
- 要求来源：GitHub Issue #1270（P1，`bug`）。

## 2. 目标与范围

- 目标：业务 POM 在 `mango:check` 上按模板配置 `<globalEntityManifest>` 字符串路径时，Maven 3 能正常解析配置并进入全局 Entity 例外校验。
- 成功条件：真实 Maven 3.9.12 下带有该配置的 `mango:check` 调用不再报 `Cannot create instance of interface java.nio.file.Path`，并且规则执行结果与不配置该参数时的行为一致。
- 处理范围：`CheckMojo` 的 `globalEntityManifest` 参数声明与转换、`mango-tools` README 的路径绑定说明、插件描述符与绑定回归测试。
- 不处理范围：`mango:architecture` 已修复的四个参数；Maven 物料发布与 `CHANGELOG`；业务仓 `mango-baohan-risk` 的实际升级与重跑。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| DSR-001 | 业务仓 `mango:check`（`globalEntityManifest` 配置字符串路径） | Maven 3.9.12，POM 配置 `<globalEntityManifest>${maven.multiModuleProjectDirectory}/business-pmo/global-entity-exceptions.json</globalEntityManifest>` | 配置解析成功，goal 正常执行并输出 Check Result | 解析阶段失败时提示`Cannot create instance of interface java.nio.file.Path` | 真实 `mvn ...:check` 返回 `BUILD SUCCESS` 且 `Status: PASS` |
| DSR-002 | 插件描述符 `META-INF/maven/plugin.xml` | 构建生成描述符 | 所有 mojo 参数类型都不是 `java.nio.file.Path` | 存在 `Path` 参数时门禁失败并列出 `goal:parameter` | 描述符回归用例通过 |
| DSR-003 | `check` goal 的 POM 绑定 | 测试用 POM 配置路径元素 | 参数绑定为 `java.io.File`，路径后缀与配置一致 | 绑定为其它类型时用例失败 | `CheckMojoBindingTest` 通过 |
| DSR-004 | 既有全局 Entity 例外校验 | 配置有效 manifest | 例外表集合参与 `PERSISTENCE_SCHEMA` 判定，行为与修复前一致 | manifest 非法时仍报 `MANGO-ARCH-ENGINE-014` | `CheckMojoTest` 相关用例通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| ADR-001 | DSR-001 | 参数声明改为 `java.io.File`，与 `mango:architecture` 的同名参数一致；Maven/Plexus 对 `File` 有内置转换器，业务 POM 的 XML 写法不变 | `CheckMojo.globalEntityManifest` | 还原字段类型和调用处 |
| ADR-002 | DSR-001、DSR-004 | 插件内部用私有 `toPath(File)` 转换为 `Path` 后调用既有 `GlobalEntityManifestLoader.load`，不复制加载与校验逻辑 | `CheckMojo.toPath`、`analyzePersistenceSchema` 调用点 | 删除转换方法 |
| ADR-003 | DSR-002 | 被拒绝方案：自定义 Plexus `Path` 转换器或改成 `String`。放弃原因：`Path` 转换器要给插件引入组件注册且只解决单点，`String` 会丢掉 Maven 字符串插值和相对路径归一语义；`File` 是平台在 Issue #496 已选定的统一方案 | 插件描述符 | 未实施 |
| ADR-004 | 兼容性 | 命令行 `-Dmango.check.globalEntityManifest=<path>` 仍不可用：该参数没有绑定 property，本次不改绑定面，避免发布新的命令行契约 | `CheckMojo` 参数注解 | 无需回滚 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IMP-001 | ADR-001、ADR-002 | 1 | `mango-tools/mango-maven-plugin/src/main/java/io/mango/plugin/check/CheckMojo.java` | 字段改为 `File`，新增 `toPath`，调用 `GlobalEntityManifestLoader.load(rootPath, toPath(...))` |
| IMP-002 | DSR-002 | 2 | `.../src/test/java/io/mango/plugin/PluginDescriptorPathParameterTest.java` | 描述符中不存在 `java.nio.file.Path` 参数 |
| IMP-003 | DSR-003 | 3 | `.../src/test/java/io/mango/plugin/check/CheckMojoBindingTest.java`、`.../src/test/resources/check-path-binding/pom.xml` | 通过 MojoTest 绑定 `check` goal 的路径元素为 `File` |
| IMP-004 | DSR-004 | 4 | `.../src/test/java/io/mango/plugin/check/CheckMojoTest.java` | 反射注入改为 `manifest.toFile()`，既有断言不变 |
| IMP-005 | DSR-001 | 5 | `mango/mango-tools/README.md` | 说明 `mango:check` 的绑定规则、失败现象与升级动作 |

## 6. 验收映射与结果

环境：JDK 21.0.13、Maven 3.9.12、离线本地仓库、版本限定符 `mango-006`；模块 `mango-maven-plugin`。

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| DSR-001 | 真实 Maven 3 消费工程复现 | `.runtime/projects/check-manifest-binding` 中执行 `mvn -B -ntp -o io.mango.tools.maven.plugin:mango-maven-plugin:mango-006:check` | 修复前 `BUILD FAILURE`+`Cannot create instance of interface java.nio.file.Path`；修复后 `Status: PASS`、`BUILD SUCCESS` | `mango-docs/evidence/2026-11-14-check-mojo-path-binding/evidence.md` |
| DSR-002、DSR-003 | M10 单元/组件测试（含反证） | `mvn -f mango/pom.xml -o -pl :mango-maven-plugin -Dtest=PluginDescriptorPathParameterTest,CheckMojoBindingTest test` | 修复后 2 项通过；暂存主代码改动后 2 项失败（`parameters must be java.io.File ... [check:globalEntityManifest]`、`UnixPath cannot be cast to File`） | 同上证据文件第 2、3 节 |
| DSR-004 | M10 模块回归 | `mvn -f mango/pom.xml -o -pl :mango-maven-plugin test` | PASS，277 项通过、12 项存量跳过 | `mango-maven-plugin/target/surefire-reports/` |
| 模块静态验证 | M09 模块构建门禁 | `mvn -f mango/pom.xml -o -pl :mango-maven-plugin -DskipTests verify` | BUILD SUCCESS | 命令输出 |
| 测试质量 | M09 测试门禁 | `node mango-pmo/tools/test-quality-check.mjs --base origin/main`、`node mango-pmo/tools/audit-backend-test-mocks.mjs --report-only --changed-only --base origin/main` | PASS / block=0 warn=0 | 命令输出 |

未启用措施与原因：

- M11：本次没有数据库、框架装配或跨模块协作变化；插件与 Maven 运行时的真实协作已用真实 Maven 3 消费工程验证并登记为 DSR-001 证据。
- M12：未改变 HTTP API 或服务入口。
- M13：无前端或浏览器可见变化。
- M16：无需责任人现场确认的业务或视觉结果。

## 7. 例外与剩余风险

- 发布依赖：修复只有在包含本提交的 Maven 插件版本发布后才会到达业务仓；业务仓需要升级 `mango.build.plugin.version` 后重跑 `mvn -f backend/pom.xml verify`，无需修改 POM 配置。
- 兼容边界：`-Dmango.check.globalEntityManifest` 命令行参数仍不生效（该参数没有绑定 property），本次不新增该契约。
- 业务仓未验证：未在 `mango-baohan-risk` 上实际执行升级后的 `mvn verify`，由该仓任务完成。
- 描述符回归用例读取构建生成的 `plugin.xml`，只能在 Maven 构建中运行；直接以 IDE 单类运行且未生成描述符时会明确报 `missing generated plugin descriptor`。

## 8. 决策复核记录

- 事实与用户视角：Issue #1270 给出 `plugin.xml` 中 `globalEntityManifest` 的类型为 `java.nio.file.Path`、Maven 3.9.12 无 `PathConverter`、删除该元素后 `BUILD SUCCESS` 三组证据；诉求是保留模板配置仍能跑通 `mvn verify`。
- 技术与风险视角：根因是 mojo 参数类型超出 Maven/Plexus 可绑定类型；Issue #496 已在 `architecture` mojo 用 `java.io.File` 解决同类问题，本次沿用同一方案，不新增转换器或抽象。风险限制在单个参数与一处调用，`GlobalEntityManifestLoader` 的加载、校验和错误码不变。
- 验证与交付视角：先用真实 Maven 3 消费工程复现原始异常，再用两个回归用例锁定描述符类型与 POM 绑定，修复后重跑同一复现工程与模块全量测试；文档更新到 `mango-tools` README。
- 评审选择：`AGENT_ONLY`（任务下发时未要求外部同行评审）。该记录不代表人工评审或正式审批。
- 提交授权：尚未获得 Commit、Push、PR 或合并授权。
