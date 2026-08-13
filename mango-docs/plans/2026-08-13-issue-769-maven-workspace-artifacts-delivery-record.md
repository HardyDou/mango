# Issue #769 Maven Workspace Artifacts Standard Delivery Record

## 1. 元数据

- 任务 ID：GitHub Issue #769
- 交付模式：STANDARD
- 需求影响：L2 - 改变 Mango CLI 本地启动的公开行为，影响业务项目开发构建与缓存边界
- 方案风险：L2 - 改为完整 Reactor 源码运行，必须严格选择目标 app 并保证非 app Boot goal 默认跳过
- 最终风险：L2
- 工作区决策：CREATE 后在 `/Users/hardy/Work/mango-issue-769` REUSE

## 2. 目标与范围

- 目标：让本地 Spring Boot app 从当前 worktree 的完整 Maven Reactor 增量编译并直接运行，不再把本项目制品安装到共享 Maven repository。
- 成功条件：开发启动从 Reactor 根 POM 选择目标 app 并执行 `-pl <app> -am compile spring-boot:run`；本项目 app、core、starter 等使用当前 worktree 的 `target/classes`；不执行 `clean/package/install`；Spring Boot Maven Plugin 管理 classpath，不由 CLI 手工展开。
- 处理范围：Mango CLI workspace/dev、CLI full template、开发环境规范、CLI README、测试和业务模板说明。
- 不处理范围：清空或自动扫描历史 `~/.m2`、删除第三方依赖、修改正式 Maven 发布行为、修改 Spring Boot app 的生产打包能力、预先实现未经真实超长命令复现的 `@argfile` 兜底。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| REQ-001 | `mango dev start backend` | manifest 指向 Reactor 内 Spring Boot app POM | CLI 从 Reactor 根 POM执行 `-pl <app> -am -DskipTests compile spring-boot:run` | 无法定位 Reactor、app selector 或非 app skip 约定时明确失败 | 每个 bootstrap/runtime 动作只执行对应 Reactor 命令，均不含 `install`、`package` 或 `clean` |
| REQ-002 | 当前 worktree 多模块源码 | app 依赖同 Reactor 的 core/api/starter | Maven Reactor 编译上游并让 Boot plugin 使用当前 Reactor 输出，不从 `.m2` 读取本项目旧 SNAPSHOT | Reactor 编译或依赖解析失败即停止启动 | 最小多模块 fixture 修改上游源码后无需 install 即被 app 运行观察到 |
| REQ-003 | 多模块 Reactor | 非 app 模块参与 `-am` | 非 app 的 Spring Boot Maven Plugin 保持 `skip=true`，只有目标 app `skip=false` | 约定不满足时 doctor/plan 明确报告 | POM 静态校验与 Reactor 回归只启动目标 app |
| REQ-004 | 大依赖 classpath | app 具有大量第三方依赖 | classpath 由 Spring Boot Maven Plugin/Java 进程管理，CLI 不拼接 `-cp` | 真实平台发生长度失败时记录证据后再引入 `@argfile` | CLI 命令及源码不存在手工 classpath 展开路径 |
| REQ-005 | 业务开发者阅读 CLI/PMO 说明 | 使用项目内锁定 CLI | 能理解开发源码运行、Maven 仓库和正式发布边界 | 文档不得建议开发 install 或清空整个 `.m2` | README、PMO 规则和投影一致 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| DEC-001 | REQ-001/REQ-002 | Spring Boot Maven app 不再执行 manifest `install`；CLI 将 app POM 解析为 Reactor 根 POM和唯一 artifactId selector | `mango-ui/packages/mango-cli/src`、模板 | 恢复旧 install + 单模块 run 行为 |
| DEC-002 | REQ-001 | 单一 Maven 会话执行 `compile` 和显式版本 Boot plugin `run`；保留 workspace `-Drevision`、Spring 参数和进程管理 | CLI dev command | 恢复从 app POM 单模块启动 |
| DEC-003 | REQ-003 | Reactor 根或 parent 默认 Boot plugin `skip=true`，目标 app POM必须显式 `skip=false`；CLI 在 plan/start 前 fail closed 校验 | CLI 校验、模板 POM | 删除校验但保留 POM约定 |
| DEC-004 | REQ-004 | 不手工生成 classpath、不复制 JAR、不创建运行包；由 Boot Maven Plugin 管理 forked Java classpath | CLI dev command | 若有真实跨平台失败，独立增加 `@argfile` 方案 |
| DEC-005 | REQ-005 | `.m2` 只作为第三方和已发布外部制品缓存；同 Reactor 开发不使用 install；正式 deploy 仍按发布流程 | PMO/README | 恢复旧开发说明 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| TASK-001 | DEC-001/DEC-002 | 1 | CLI Reactor 定位、命令解析与单元测试 | 命令不含 `install`、`package` 或 `clean`，保留 revision 和 Spring 参数 |
| TASK-002 | DEC-003 | 2 | POM skip 约定校验与 fixture 回归 | 非 app 不启动、目标 app可启动 |
| TASK-003 | DEC-001/DEC-005 | 3 | full template、自动发现 manifest、兼容旧 manifest | 新清单不生成 install；旧 install 字段不再执行并给出迁移提示 |
| TASK-004 | REQ-005 | 4 | PMO 规则及 index、CLI/模板/Starter README、baseline 投影 | 公开行为与唯一规范源一致 |
| TASK-005 | 全部 | 5 | 定向测试、CLI 全量测试、template/workspace checks | 无未解释失败 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| REQ-001 | M10/M11 | `pnpm --filter @mango/cli test` 中生成项目 fake Maven 启动回归与真实业务开发生命周期 | PASS | `workspace init/status`、`dev doctor/plan/start/status/logs/restart/stop`、`workspace release` 全通过；68 tests passed |
| REQ-002 | M11 | `tests/dev-maven-reactor-integration.test.mjs` 真实最小多模块 Reactor 运行；CLI 全量生成项目回归 | PASS | app 输出刚编译 library 的 `REACTOR_SOURCE_V2`；测试 GAV 未写入 `~/.m2`；命令无 `install`、`package` 或 `clean` |
| REQ-003 | M09/M11 | Reactor root/app Boot skip contract 单测、生成模板 POM、旧项目发现回归 | PASS | root/parent `skip=true`、app `skip=false` 缺失时 fail closed；CLI 全量通过 |
| REQ-004 | M09 | 源码与命令审计；`dev-maven-reactor.test.mjs` 禁止 `-cp`、fat JAR 和手工 classpath | PASS | 无 CLI classpath 展开；未引入未经复现的 `@argfile` |
| REQ-005 | M08/M09 | `node mango-business-starter/scripts/sync-pmo-baseline.mjs --write`、README/PMO source audit | PASS | baseline synced 142 files；PMO、CLI README、template README 与 baseline 一致 |

### 6.1 运行验证记录

- `pnpm --filter @mango/cli test`：PASS；生成模板、旧项目发现、生命周期、打包消费者与 68 个 Node tests 全通过。
- `tests/dev-business-lifecycle-integration.test.mjs`：PASS；真实业务项目完成 workspace/dev 全生命周期，修改上游 library 后 restart 可观察到 `BUSINESS_SOURCE_V1 -> BUSINESS_SOURCE_V2`，未知 app 日志请求正确失败，停止与释放后无残留进程或注册，测试 GAV 未进入 `~/.m2`。
- `pnpm --filter @mango/cli exec node --test tests/dev-maven-reactor-integration.test.mjs`：PASS；真实 Maven Reactor 使用上游 `target/classes` 启动且未安装测试坐标。
- `pnpm admin:styles:check`：PASS；本任务未改前端样式，但按 preflight 要求执行。
- `pnpm admin:module-styles:check`：PASS；本任务未改前端样式，但按 preflight 要求执行。
- `node mango-pmo/tools/workspace-layout-check.mjs --root .`：PASS。
- Node 运行时输出 engine warning（仓库要求 Node `>=22.23.1 <23`，当前环境 `v26.5.0`），未导致测试失败。

### 6.2 独立业务项目 JAR 消费与真实基础设施验证

- 使用专用 Maven repository `.runtime/mango-local-consumer/maven-repo`，以 `1.0.0-mango-local-769-SNAPSHOT` 安装 `mango-infra-kv-starter`、其 Reactor 上游和 `mango-bom`；未安装任何 Mango app JAR，也未写入共享 `~/.m2`。
- 临时业务项目位于 `.runtime/projects/mango-local-consumer`，只通过标准 Maven dependency 引用 `io.mango.infra.kv:mango-infra-kv-starter`，未引用 Mango Reactor 或源码目录。
- 首次普通启动和第二次 Maven `-o` 离线重启均成功；两次 `/integration/check` 都返回 `mysql=true`、`redis=true`。
- MySQL 8.4.8 真实创建 `mango_dev_issue769_consumer.issue769_consumer_probe`、插入并查询，重启后记录数从 1 增至 2。
- Redis 8.6.1 真实写入 `issue769:consumer:<uuid>`，读取值为 `redis-payload`，并观察到正数 TTL。
- `dependency:build-classpath` 生成的 classpath 为 19274 字节，只包含专用 Maven repository 下的 JAR；检查结果为 `NO_MANGO_SOURCE_PATH`。
- 消费解析首次失败揭示 BOM 是 Maven 发布元数据的必要组成：只安装 starter 及 `-am` 上游不包含并列的 `mango-bom`，补装 BOM 后业务项目即可解析。该结论不改变 app 边界：BOM/parent/API/core/starter 需要发布，app fat JAR 不需要安装供开发启动。
- Redisson 3.27.0 在连接池 `max-active=1` 时无法同时满足初始化连接需求并超时；恢复正常池大小 `max-active=8`、`min-idle=1` 后，Redis 连接及业务读写稳定通过。这是业务配置下限风险，不是 Maven 路径写死或 JAR 消费失败。

## 7. 例外与剩余风险

- 不在本次实现 `@argfile`：Spring Boot Maven Plugin 当前负责启动 classpath，只有真实平台失败证据才足以证明需要新增兜底。
- 已经遗留在 `.m2` 中、且 workspace 元数据已丢失的历史版本不自动猜测清理；开发链修复后另行以只读审计和明确目标处理，避免误删仍在使用的制品。
- 业务项目经 Maven/JAR 消费时必须能解析对应版本的 BOM 和 parent POM；正式发布清单应把它们与被消费的 API/core/starter 同批发布，不能只发布叶子 starter。
- KV Redis 客户端连接池不能收缩到单连接；当前真实验证配置使用 `max-active=8`、`min-idle=1`。MacOS 仍有 Netty DNS native provider 缺失 warning，但 loopback Redis 解析和读写均成功。
