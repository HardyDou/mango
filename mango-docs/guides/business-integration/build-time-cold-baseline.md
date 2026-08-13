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
  -> replay/determinism schema 各执行一次最终 V，先验证可复现（忽略标准审计时钟列）
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
            <phase>generate-resources</phase>
            <goals>
                <goal>baseline-generate</goal>
            </goals>
            <configuration>
                <includedModules>system,identity,authorization,workflow,file,guarantee</includedModules>
                <moduleOrder>system,identity,authorization,workflow,file,guarantee</moduleOrder>
                <moduleGroups>system=main,identity=main,authorization=main,workflow=main,file=main,guarantee=main</moduleGroups>
                <characterSet>utf8mb4</characterSet>
                <collation>utf8mb4_unicode_ci</collation>
            </configuration>
        </execution>
    </executions>
</plugin>
```

如果模块分属不同逻辑数据库，将相应模块映射到不同 group。一个 MySQL 实例可以承载多个随机 replay/determinism/verify schema，无需为每个 group 启动单独容器。

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
BOOT-INF/classes/META-INF/mango/baseline-manifest.json
```

manifest 包含目标字符集、目标排序规则、模块、逻辑数据源 group、最高 V 版本、B SHA-256 和 migration fingerprint，不包含 JDBC URL、用户名或密码。Dockerfile 应只复制已经通过上述构建的 Boot JAR。

## 6. 运行时选择

| 数据库状态 | Bootstrap 行为 |
|------------|----------------|
| 真正空库且启用 cold baseline | 执行制品内 B，建立模块原 history table 基线，再执行高于 B 版本的新 V。 |
| 已有模块 history | 忽略 B，继续增量 V。 |
| 非空但没有一致的 history/回执 | fail closed；按环境恢复方案处理，不自动用 B 覆盖。 |

Runtime 不生成 B、不执行 migration，也不访问构建数据库。

## 7. Resource、Workflow 与文件

cold baseline 只快照 V 最终产生的数据库结构和 migration 静态行。菜单、权限、流程发布、租户资源以及运行时可维护配置仍由 Resource Registry 的 Bootstrap 步骤处理。

预置文件使用 `FILE_ASSET` Resource：声明中保留稳定 file ID、目标配置、`classpath:` 或 `asset:` 内容位置和资源摘要；不适合进入 Jar 的大型二进制使用 `asset:<relative-path>`，由各环境通过 `mango.file.asset-root` 映射外部根目录。Bootstrap File handler 把二进制写入配置的文件存储层，再写入/校验文件元数据。B 可以包含 V 历史形成的数据库元数据，但不包含文件二进制，也不替代对象存储上传。这样固定 ID、数据库记录和存储对象由同一 generation/fingerprint 验证，业务不再需要在 Runtime 启动时自行上传文件。

Workflow 定义同理：B 只负责相关表结构和 V 静态行，流程部署与版本回执由 `BOOTSTRAP_REQUIRED` Resource/Workflow handler 完成。

## 8. 常见失败

- `included modules have no V migrations`：模块名与 `db/migration/<module>` 不一致，或依赖 JAR 未进入 API runtime 依赖。
- `moduleOrder must contain every discovered module exactly once`：补齐、删除未知项或处理重复项。
- `migration resource/version collision`：PR 合并前人工确定版本顺序和 SQL 语义，CI 不自动改名。
- `cross-module ... ownership conflict`：两个模块声明了同一表或视图；归并到唯一 Owner。
- `generated baseline is not equivalent`：B 在第二 schema 的结构或静态数据不同；构建已阻断，不要跳过验证。
- `unsupported MySQL character set and collation`：目标组合不存在或不匹配；修正 `mango.baseline.characterSet` / `mango.baseline.collation`，不要退回构建机默认值。
- `V migrations are not deterministic`：同一组 V 在两个空 schema 产生了不同结构或静态数据。生成器只在这一步忽略 `created_at`、`updated_at`、`published_at` 的运行时钟差异；B 仍保留真实值并接受全列等价验证。其它列中的 `UUID()`、当前时间等非确定初始化表达式会继续被阻断，应改用稳定业务值。
- `stored routines and events are not supported`：当前生成器不静默遗漏存储过程、函数或事件；将其迁移方案单独评审。
- Jenkins 没有 MySQL：为该构建增加临时 MySQL 8.4 service/container，而不是改业务 datasource 或把生成推迟到部署服务器。
