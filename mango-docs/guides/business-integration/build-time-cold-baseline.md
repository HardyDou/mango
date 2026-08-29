# 业务 API 构建期 cold baseline

## 1. 适用场景

当业务系统首次创建空库时，逐模块回放多年 `V1...Vn` 已明显拖慢发布，可以在主分支 API 制品构建中生成 cold baseline。开发者仍只维护模块自己的 `db/migration/<module>/V*.sql`；已有数据库仍按原模块 history table 增量升级。

长期数据库约束以[数据库规范](../../../mango-pmo/rules/backend/04-db.md)为准。本文只说明 POM、Jenkins、制品和排障用法。

## 2. 构建发生在哪里

```text
PR：校验 V，可升级；不生成 B
  -> 合并 main，人工冲突已解决
  -> API artifact build：临时 MySQL + mango:baseline-generate
  -> 临时 schema 显式使用业务目标 charset/collation，不继承构建机默认值
  -> replay/determinism schema 各执行一次最终 V
  -> 两套 schema 分别启动最终业务应用，物化 PORTABLE Resource
  -> 比较结构和受管数据，验证可复现（忽略运行时审计时钟列）
  -> 生成每模块一个 B
  -> verify schema 连续执行 B 两次并比较结构/静态数据
  -> package/repackage 把 B + manifest 打进 JAR
  -> Docker build 复制该不可变 JAR
  -> deploy 只拉镜像，不生成 SQL
```

输出只位于 Maven 构建目录：

```text
target/generated-resources/db/baseline/<module>/B<version>__baseline.sql
target/generated-resources/META-INF/mango/baseline-manifest.json
```

## 3. API POM

在最终 API 应用的构建 profile 中绑定 goal；不要把这段 execution 放进每个业务模块：

```xml
<plugin>
    <groupId>io.mango.tools.maven.plugin</groupId>
    <artifactId>mango-maven-plugin</artifactId>
    <version>${mango.version}</version>
    <executions>
        <execution>
            <id>generate-cold-baselines</id>
            <phase>prepare-package</phase>
            <goals>
                <goal>baseline-generate</goal>
            </goals>
            <configuration>
                <includedModules>system,identity,authorization,workflow,file,guarantee</includedModules>
                <moduleOrder>system,identity,authorization,workflow,file,guarantee</moduleOrder>
                <moduleGroups>system=main,identity=main,authorization=main,workflow=main,file=main,guarantee=main</moduleGroups>
                <characterSet>utf8mb4</characterSet>
                <collation>utf8mb4_unicode_ci</collation>
                <resourceApplicationClass>com.example.GuaranteeApplication</resourceApplicationClass>
                <resourceTimeoutSeconds>300</resourceTimeoutSeconds>
            </configuration>
        </execution>
    </executions>
</plugin>
```

只生成 Flyway baseline 时，可以把模块映射到不同 group。启用 `resourceApplicationClass` 后，当前版本要求所有模块位于同一个 datasource group；多 group 会在创建临时 schema 前失败，不能通过拆分执行绕过应用级 Resource 一致性。

Resource baseline 必须绑定 `prepare-package`：此时最终业务主类和运行时 classpath 已编译完成，生成的 BSQL/manifest 仍会在 Boot repackage 前进入最终 JAR。未配置 `resourceApplicationClass` 的纯 Flyway baseline 可以继续使用较早阶段，但不能据此声明已物化 Resource。

生成器虽然启动最终业务应用，但构建专用 Bootstrap 只选择 Mango Resource contributor。业务自定义 `BootstrapStepContributor`、租户对账和其它运行期步骤默认不执行，也不得通过覆盖 `supportsResourceBaselineBuild()` 加入；它们仍在目标环境的正常 Bootstrap 中执行。Resource 目标是否可进入 BSQL 只由各 `ResourceHandler.baselinePolicy()` 决定。

`characterSet` 和 `collation` 的默认值分别是 `utf8mb4`、`utf8mb4_unicode_ci`，与 Mango CLI 创建的标准业务数据库一致。通常无需在 POM 重复声明；示例显式写出是为了让业务制品的数据库语义可审计。正式目标空库的字符集组合及例外边界以 [数据库规范](../../../mango-pmo/rules/backend/04-db.md) 为准。

## 4. Jenkins 构建

Jenkins 节点本身不需要预装 MySQL。流水线为 API 构建启动一个临时 MySQL 8.4 service/container，等待健康后运行正常 Maven `package`：

```bash
export MANGO_BASELINE_DB_PASSWORD="$CI_BASELINE_DB_PASSWORD"

mvn -pl baohan-api -am package \
  -Dmango.baseline.jdbcUrl='jdbc:mysql://127.0.0.1:33060/mysql?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai' \
  -Dmango.baseline.username=root \
  -Dmango.baseline.passwordEnv=MANGO_BASELINE_DB_PASSWORD \
  -Dmango.baseline.characterSet=utf8mb4 \
  -Dmango.baseline.collation=utf8mb4_unicode_ci
```

临时账号需要创建和删除临时 schema 的权限。Jenkins 只传构建数据库 URL 和目标 schema 语义；插件不读取 `spring.datasource.*`，也不会修改业务环境的数据库连接。构建机 MySQL 即使默认使用 `utf8mb4_0900_ai_ci`，生成器创建的三类临时 schema 仍使用上述目标值。

构建 profile 只在主分支正式 API 制品任务启用。PR 任务继续执行 migration 命名、重复版本、checksum 和升级测试，但不产出正式 B。

## 5. 制品验收

普通 JAR：

```bash
jar tf target/*.jar | grep -E 'db/baseline/.+/B.+__baseline.sql|META-INF/mango/baseline-manifest.json'
```

Spring Boot JAR 的预期路径：

```text
BOOT-INF/classes/db/baseline/<module>/B<version>__baseline.sql
META-INF/mango/baseline-manifest.json
```

manifest 包含目标字符集、目标排序规则、模块、逻辑数据源 group、最高 V 版本、B SHA-256 和 migration fingerprint，不包含 JDBC URL、用户名或密码。Dockerfile 应只复制已经通过上述构建的 Boot JAR。

## 6. 运行时选择

| 数据库状态 | Bootstrap 行为 |
|------------|----------------|
| 真正空库且启用 cold baseline | 执行制品内 B，建立模块原 history table 基线，再执行高于 B 版本的新 V。 |
| 已有模块 history | 忽略 B，继续增量 V。 |
| 非空但没有一致的 history/回执 | fail closed；按环境恢复方案处理，不自动用 B 覆盖。 |

Runtime 不生成 B，也不访问构建数据库。部署 Bootstrap 只消费制品中的 B 和 manifest；已有库继续执行未执行的 V。

## 7. Resource、Workflow 与文件

cold baseline 在 Flyway 最终状态上继续物化可移植的数据库 Resource。`ResourceHandler.baselinePolicy()` 默认为 `PORTABLE`；只写本地、可移植数据库状态的 Handler 可以进入 BSQL。读取凭据、对象存储、主机路径、外部服务或其它部署环境状态的 Handler 必须使用 `ENVIRONMENT_REQUIRED`，恢复后首次 Bootstrap 再处理。

构建期 BSQL 保留便携目标表数据及 `resource_registry.source_hash`，但清除模块 receipt、Resource 审计和 Bootstrap 运行记录。这样部署环境会重新建立自己的 receipt，同时按逐 Resource hash 跳过已物化 Handler。没有本地 Handler 的远程目标也会延迟处理；构建器不会调用远程 Dispatcher。

预置文件使用 `FILE_ASSET` Resource：声明中保留稳定 file ID、目标配置、`classpath:` 或 `asset:` 内容位置和资源摘要。构建期 Resource 生成器把两类输入统一复制为内容寻址对象，并将 manifest 中的内容位置改写到 `classpath:META-INF/mango/files.bundle/objects/<sha256>`。Bootstrap File handler 再把该对象写入环境配置的存储层并写入/校验文件元数据。B 可以包含 V 历史形成的数据库元数据，但不包含文件二进制，也不替代对象存储上传。

Workflow 或其它 Handler 是否进入 BSQL，以其 `baselinePolicy()` 和目标状态是否可移植为准；环境依赖和版本回执始终在恢复环境建立。

### 7.1 Resource 构建物 POM

在最终 API 应用中把生成器绑定到 `process-classes`。此时应用 class 和普通 resources 已进入 `${project.build.outputDirectory}`，生成器也直接写入该目录，后续 JAR/Boot repackage 会密封相同字节：

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>3.5.1</version>
    <executions>
        <execution>
            <id>generate-resource-artifacts</id>
            <phase>process-classes</phase>
            <goals><goal>java</goal></goals>
            <configuration>
                <mainClass>io.mango.resource.sync.starter.ResourceManifestBuildApplication</mainClass>
                <classpathScope>runtime</classpathScope>
                <arguments>
                    <argument>--mango.resource.registry.artifact-output-directory=${project.build.outputDirectory}</argument>
                    <argument>--mango.file.asset-root=${project.basedir}/src/main/assets</argument>
                    <argument>--mango.resource.registry.artifact-context-sources=com.example.build.ResourceBuildProviders</argument>
                </arguments>
            </configuration>
        </execution>
    </executions>
</plugin>
```

声明文件会自动从 runtime classpath 的默认 Resource locations 收集。只有 Java Provider 不能由最小上下文直接构造时，才通过 `artifact-context-sources` 传入逗号分隔的 `@Configuration` 类；这些配置只提供确定性 Provider 及其纯构建依赖。

构建上下文不启用 Spring Boot auto-configuration、component scan 或业务主类，因此不会连接 `spring.datasource.*`，不会执行 Flyway，也不会启动普通 Bootstrap。构建输出为：

```text
META-INF/mango/resource-bootstrap-manifest.json
META-INF/mango/files-manifest.json
META-INF/mango/files.bundle/objects/<sha256>
```

Boot JAR 验收：

```bash
jar tf target/*.jar | grep -E '^META-INF/mango/(resource-bootstrap-manifest.json|files-manifest.json|files.bundle/objects/)'
```

存在构建 manifest 时，Bootstrap 优先消费其中的模块 envelope；相同 hash 的模块在内部 declarations JSON 解析前通过环境 receipt 跳过。没有构建 manifest 的历史应用保留运行时扫描兼容路径。

### 7.2 发布物料关系

- `mango:baseline-generate` 继续是数据库 cold baseline 的唯一生成入口；Resource 构建器不创建或连接数据库。
- 配置 `resourceApplicationClass` 后，`mango:baseline-generate` 负责启动最终业务应用并把 `PORTABLE` Resource 合入同一组 BSQL；Resource manifest 构建器仍只负责声明和文件 bundle。
- `files.bundle` 只携带逻辑 object key、SHA-256、大小、MIME 和内容对象，不携带 endpoint、bucket 或凭据；部署仍走现有 `FILE_ASSET` staged publish。
- sealed Maven release manifest 已逐 JAR 记录 size/SHA-256，因此 JAR 内 Resource manifest 和 files bundle 自动受外层 JAR digest 保护，不再新增第二套 release manifest 或 digest 来源。
- `statObject`/metadata 快速判断是后续可选优化；当前 Handler 仍读取对象并校验 SHA-256，正确性路径不依赖该优化。

## 8. 常见失败

- `included modules have no V migrations`：模块名与 `db/migration/<module>` 不一致，或依赖 JAR 未进入 API runtime 依赖。
- `moduleOrder must contain every discovered module exactly once`：补齐、删除未知项或处理重复项。
- `migration resource/version collision`：PR 合并前人工确定版本顺序和 SQL 语义，CI 不自动改名。
- `cross-module ... ownership conflict`：两个模块声明了同一表或视图；归并到唯一 Owner。
- `generated baseline is not equivalent`：B 在第二 schema 的结构或静态数据不同；构建已阻断，不要跳过验证。
- `unsupported MySQL character set and collation`：目标组合不存在或不匹配；修正 `mango.baseline.characterSet` / `mango.baseline.collation`，不要退回构建机默认值。
- `migrations and portable Resource handlers are not deterministic`：两套空 schema 的 Flyway + 便携 Resource 最终状态不同。确定性比较忽略标准及历史审计时钟列，B 仍保留真实时间；其它业务列中的 UUID、当前时间或环境值继续阻断构建。
- `Resource baseline application must be compiled before baseline-generate`：将 goal 绑定到 `prepare-package`，并确认 `resourceApplicationClass` 是最终 Spring Boot 应用主类。
- `Resource baseline generation currently requires one datasource group`：当前应用级 Resource 物化不能跨多个逻辑 datasource group；收敛为一个 group，或暂不启用 Resource baseline。
- `Resource baseline build mode requires ...`：不要在部署命令中手工打开构建专用开关；确认使用 `mango:baseline-generate` 启动最终应用，并且应用已装配 `mango-resource-sync-starter`。
- `stored routines and events are not supported`：当前生成器不静默遗漏存储过程、函数或事件；将其迁移方案单独评审。
- Jenkins 没有 MySQL：为该构建增加临时 MySQL 8.4 service/container，而不是改业务 datasource 或把生成推迟到部署服务器。
