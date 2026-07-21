# Mango BOM 统一依赖治理交付记录

## 1. 元数据

- 任务 ID：mango-bom-dependency-management
- 交付模式：STANDARD
- 需求影响：L2 - 调整所有业务消费方的 Maven 依赖版本入口。
- 方案风险：L2 - 新增发布制品并修改发布 Parent 的 dependencyManagement，需要全 Reactor 和独立消费者验证。
- 最终风险：L2
- 工作区决策：CREATE

## 2. 目标与范围

- 目标：提供可独立导入的 `io.mango:mango-bom`，让业务项目通过一个 `mango.version` 获得同批 Mango 模块及兼容第三方依赖版本。
- 成功条件：BOM 覆盖全部已发布非 app Mango JAR 和 Reactor 中显式声明版本的依赖坐标；`mango-parent` 只导入同版本 BOM；Flyway 固定为 11.20.3，Redisson 固定为与当前 KV API 兼容的 3.27.0；BOM 可在不解析聚合根 POM 的情况下独立消费。
- 处理范围：Maven Reactor 模块、BOM、Parent、BOM 覆盖回归测试、后端能力说明，以及 Maven `1.0.25`、CLI `1.0.89` 和既有待发布前端补丁矩阵的合并发布准备与验证。
- 不处理范围：`mango-app/**` 部署制品、Maven 插件版本目录、把 CLI 模板从 Parent 继承自动迁移为 BOM 直接导入、PMO 升版。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AR-001 | 保留自有 parent 的业务项目 | 导入一个已发布版本的 `io.mango:mango-bom` | Mango JAR 和已管理第三方依赖均可省略版本 | 依赖缺少版本、混入不同批次或 BOM 需要聚合根 POM 才能解析 | 隔离消费者 validate/dependency tree 通过，无混合 Mango 版本 |
| AR-002 | 继承 `mango-parent` 的业务项目 | parent 与 Mango 依赖使用同一发布版本 | parent 通过唯一 BOM import 获得完整依赖目录 | parent 继续维护不完整的重复依赖清单 | 模型测试断言 parent 仅导入 `mango-bom` |
| AR-003 | Mango 维护者 | 新增发布 JAR或显式依赖版本 | 覆盖测试阻止 BOM 漏登和重复坐标 | 新模块或本地版本未进入 BOM | `MangoBomCoverageTest` 通过 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | AR-001 | BOM 是独立 POM，不继承或反向导入同版本聚合根，避免冷缓存消费者无法解析 | `mango/mango-bom` | 删除 BOM 模块并恢复原消费方式 |
| TD-002 | AR-002 | Parent 的 dependencyManagement 只保留 `io.mango:mango-bom:${revision}` import；插件版本仍归 pluginManagement | `mango/mango-parent` | 恢复 parent 原依赖清单 |
| TD-003 | AR-003 | 模型测试扫描非 app 发布 JAR、Reactor 显式版本依赖、重复 BOM 坐标和关键锁定版本 | `MangoBomCoverageTest` | 删除测试并恢复人工审计 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IM-001 | TD-001 | 1 | `mango/mango-bom/pom.xml`、`README.md` | 完整依赖目录可独立 validate |
| IM-002 | TD-002 | 2 | `mango/mango-parent/pom.xml`、`README.md` | Parent 只导入 BOM，依赖不重复维护 |
| IM-003 | TD-003 | 3 | `mango-tools/mango-maven-plugin` 测试 | BOM 覆盖模型测试通过 |
| IM-004 | TD-001/TD-002 | 4 | 后端 README、能力地图、架构文档 | 业务使用口径一致 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AR-001 | M09 Maven 模型与隔离消费验证 | `mvn -q -f mango/mango-bom/pom.xml -Drevision=1.0.25-SNAPSHOT validate`；隔离消费者仅导入 `io.mango:mango-bom:1.0.25-SNAPSHOT` 后执行 `validate dependency:tree` | PASS；独立 BOM 可解析，两个代表性 Mango JAR 均解析为 `1.0.25-SNAPSHOT` | 命令输出与本文件 |
| AR-001 | M09 本地发布与独立运行时仿真 | `mvn -f mango/pom.xml -Drevision=1.0.25 -pl :mango-bom,:mango-infra-context-starter -am install -DskipTests`；新建不继承 Mango Parent、只导入 `io.mango:mango-bom:1.0.25` 的 Spring Boot 项目，使用独立 Maven 缓存完成 `clean package`、`dependency:tree` 和 `java -jar` | LOCAL_SIMULATION_PASS；干净缓存来源为 `mango-local-publish`，可执行 JAR 和依赖树均为 Mango `1.0.25`、Spring Boot `3.5.14`、Flyway `11.20.3`、Redisson `3.27.0`；`/actuator/health` 与 `/simulation` 均返回 HTTP 200，健康状态 `UP`，`mangoContextExecutor` 在线程 `mango-context-async-1` 实际执行 | 2026-07-21 本地 Maven 仓库、独立消费者构建输出、依赖树、HTTP 响应与启动日志；验收后服务已关闭，端口已释放 |
| AR-002 | M09/M14 模型测试与 Reactor 验证 | `mvn -f mango/pom.xml -Drevision=1.0.25-SNAPSHOT -pl :mango-maven-plugin -am test -Dtest=MangoBomCoverageTest -Dsurefire.failIfNoSpecifiedTests=false`；加载专用 MySQL 验证环境并执行 `mvn -f mango/pom.xml -Drevision=1.0.25-SNAPSHOT verify` | PASS；模型测试 2 项通过，全 Reactor 212 个模块 `BUILD SUCCESS`，架构门禁新增问题 0 | Surefire 报告、Reactor 汇总与命令输出 |
| AR-003 | M09/M11 静态覆盖与依赖树 | `MangoBomCoverageTest`；隔离消费者依赖树检查 Flyway、Redisson 和 Mango 同版本；`FileServiceConcurrentSaveIntegrationTest` 使用专用 `_concurrency` MySQL 库执行 | PASS；Flyway `11.20.3`、Redisson `3.27.0`，并发集成测试 1 项通过 | Surefire 报告、依赖树与命令输出 |

## 7. 例外与剩余风险

- 本地精确版本 `1.0.25` 已安装到 `~/.m2` 供仿真使用；该结果只代表 `LOCAL_SIMULATION_PASS`，不是 Maven 私服发布、仓库回查、tag 或 GitHub Release 完成。
- 第一次独立缓存构建被用户级 Maven 全局 mirror 重写到尚无 `1.0.25` 的内网 Nexus 而失败；仿真使用无 mirror 的一次性 settings 后，从 `file://${user.home}/.m2/repository` 成功解析。正式业务消费仍应等待私服发布，不能依赖该本地设置。
- 聚合根 POM 与独立 BOM 当前都保存依赖目录，后续新增或升级依赖必须同步，并由覆盖测试阻止坐标漏登；版本值漂移仍需在评审和消费者依赖树中检查。
- `mango-bom` 是新增 Maven 发布制品，必须先于或同时于 `mango-parent` 发布；发布前业务不能使用文档中的目标版本。
- 本次只更新 CLI 的 Maven 与 npm 兼容版本锁，不把生成模板从 Parent 继承改为 BOM 直接导入；是否改变默认接入方式应作为独立兼容性任务处理。
- 正式发布批次锁定 Maven `1.0.25`、CLI `1.0.89`、PMO `1.3.4` 和 16 个既有待发布前端补丁版本；私服发布、双仓回查、GitHub Release 和发布后干净消费者结论在状态机实际完成前保持 `PENDING`。
- 全 Reactor 验证时只加载数据库连接变量，并取消导出与被测配置绑定冲突的工作区级 `MANGO_CRYPTO_SM4_SECRET_KEY`；专用数据库和隔离消费者临时目录已在验证后删除。
