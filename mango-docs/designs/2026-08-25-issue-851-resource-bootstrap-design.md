# Issue #851 Resource Bootstrap 构建基线与模块增量设计

## 1. 元数据

- Issue：#851
- 状态：APPROVED
- 交付模式：FULL
- 需求影响：L3，改变平台 Resource Bootstrap、持久化回执和发布物料契约。
- 方案风险：L3，跨 Bootstrap、Resource Registry、文件资产和构建链路，失败会阻断发布初始化。
- 最终风险：L3
- 工作区：`M01=CREATE`，`/Users/hardy/Work/mango-issue-851`

## 2. 目标与边界

目标是把 Resource 从每代全量协调改为不可变模块 manifest 加环境 receipt：构建物保存每个模块的完整期望状态和 SHA-256；部署端先比较 receipt，未变化模块不解析声明、不调用 Handler、不写 Registry 或审计表；变化模块在验证成功后才提交 receipt。

本次复用现有 `mango:baseline-generate` 数据库 cold baseline、Bootstrap generation fence、`FILE_ASSET` staged publish 和 sealed release manifest，不建立第二套发布事实源。

不处理版本对版本差量包、通用资源 DAG、生产数据库覆盖、生产环境操作、Baohan 环境操作、Mango 发布和 PR。

## 3. 架构决定

### 3.1 构建物

- `META-INF/mango/resource-bootstrap-manifest.json` 只保存模块 code、模块 hash、固定依赖和完整模块声明；最终应用通过不启用自动配置的最小构建 context 在 `process-classes` 生成。
- 文件对象清单从 `FILE_ASSET` 声明提取逻辑位置、SHA-256、大小和 MIME；不保存 endpoint、bucket、凭据或绝对路径。
- 模块 hash 对规范化声明和文件内容计算，和物理 classpath/JAR 位置无关。
- 构建期把 `asset:`/普通 `classpath:` 内容复制为 `META-INF/mango/files.bundle/objects/<sha256>`，manifest 内位置改写为对应 classpath；部署不依赖构建机资产目录。
- Bootstrap 有构建 manifest 时优先消费模块 envelope；历史应用没有该物料时继续使用运行时扫描兼容路径。
- release prepare 只密封上述构建物的 digest，不重新解释 Resource 内容。

### 3.2 环境 receipt

`resource_module_receipt` 以 `environment + app + service + module` 唯一标识模块安装状态，保存 module hash、generation、manifest fingerprint、状态和计数。

状态只允许：

1. EXPAND 成功后 `EXPANDED`；
2. FINALIZE 成功后 `FINALIZED`；
3. hash 相同且状态足够时 `SKIPPED`；
4. 失败不更新 receipt，重试继续使用旧成功状态。

### 3.3 协调顺序

- 客户端先发送模块 envelope，声明 JSON 留在模块内部。
- 服务端先读取 receipt；hash 相同的模块不反序列化内部声明。
- 变化模块按显式依赖拓扑顺序执行；依赖循环或缺失时 fail closed。
- EXPAND 只处理新增和兼容更新，不禁用缺失资源。
- FINALIZE 处理当前模块完整声明，并只禁用该模块内 Registry 拥有的缺失 `AUTO` 资源。
- `INIT_ONLY` 沿用现有保护语义，不覆盖已存在的运行期目标数据。

### 3.4 一致性与恢复

- Bootstrap generation fence 在读取或写入模块状态前校验。
- Handler 和 Registry 仍要求幂等；进程中断后以旧 receipt 重试模块。
- receipt 只有模块协调和结果观察全部成功后写入。
- 文件发布先写 staging；稳定对象发布成功或失败后都清理 staging。若稳定发布和清理同时失败，主异常保留，清理异常以 suppressed exception 附带。
- 构建期文件先完成 SHA-256 校验，再原子移动到内容寻址对象位置；错误构建不得留下可被后续构建误用的半成品。
- FINALIZE 之后继续 fail closed，不自动逆向数据库、不删除数据卷。

### 3.5 Resource 级增量与目标数据退避

- 模块 receipt 命中仍是第一层跳过；变化模块解析完整声明后，Registry 只调度 canonical hash 或状态变化的 Resource。
- `dependsOnResourceTypes()` 只对本次变化类型排序，不再因依赖变化重放未变化 Resource。业务关系使用稳定 `resourceId`、`code` 或 `bizKey` 解析，不使用模块扫描、JAR 加载、启动顺序或目标表自增 ID 作为发布契约。
- Handler 新批处理入口分离变化声明、同类型完整只读上下文和逐 Resource 同步上下文。`AUTH_MENU`、`API_RESOURCE` 使用完整上下文解析关系，但只写变化声明。
- 需要保护后台修改的 Handler 显式比较目标行可靠的 `updated_at` 与 Registry `last_sync_time`。应用时目标与 Registry 写入同一秒级固定时间；发现不相等时返回 `PRESERVED`，不推进 `source_hash` 和 `last_sync_time`。
- 当前只为一 Resource 对应一条 `sys_config` 行的 `SYSTEM_CONFIG` 启用退避。多表/多行目标不做通用时间猜测，由 Owner Handler 提供受管状态判断；本次不增加 `revision` 字段或通用状态表。
- 现有 cold baseline 只重放 Flyway，不能证明 Handler 目标状态及 Registry 同步时间一致，因此重置发布的完整 Resource baseline 不在本批次完成范围。

## 4. 验收映射

| ID | 要求 | 自动化入口 |
|---|---|---|
| AC-001 | 相同模块 hash 时不解析声明、Handler 调用和 Resource 写入均为零 | Resource core 隔离数据库集成测试 |
| AC-002 | 单模块变化只协调目标模块 | Resource core 隔离数据库集成测试 |
| AC-003 | EXPAND/FINALIZE 成功后分别提交 receipt，失败不推进 | Resource core 集成测试 |
| AC-004 | 删除只影响变化模块内 AUTO，INIT_ONLY/人工数据保留 | Resource core 集成测试 |
| AC-005 | manifest 和文件清单相同输入产生相同 digest | sync starter 单元测试 |
| AC-006 | Local 与 MinIO 服从同一文件清单语义 | file core 契约/集成测试 |
| AC-007 | 1291 条无变化小于 10 秒，单模块变化小于 30 秒 | 本地性能测试脚本与报告 |
| AC-008 | 构建上下文不连接业务数据库，生成物进入最终 Boot JAR 并由 Bootstrap 优先消费 | sync starter 单元测试与 Maven Invoker consumer |
| AC-009 | 变化模块内 hash 未变化的 Resource 不调用 Handler，依赖变化也不触发重放 | Resource core 集成测试 |
| AC-010 | `PRESERVED` 不更新目标、`source_hash` 或 `last_sync_time`，并写入可观察同步日志 | Resource core 集成测试 |
| AC-011 | `SYSTEM_CONFIG` 未经后台修改时以同一固定时间更新目标和 Registry | System core 集成测试 |
| AC-012 | `SYSTEM_CONFIG.updated_at` 偏离上次同步时间时保留后台值 | System core 集成测试 |
| AC-013 | `AUTH_MENU`、`API_RESOURCE` 使用完整上下文但只写变化声明 | Authorization starter 单元测试 |

## 5. 真实场景矩阵

| 类别 | 已覆盖场景 | 结论 |
|---|---|---|
| 构建物确定性 | 相同输入稳定 manifest/digest、相同内容物理去重、文件 checksum 错误、unsafe `asset:` 路径、外层 JSON/模块结构损坏、未知 schema | 全部通过；错误输入 fail closed，且不残留部分对象 |
| 模块完整性 | declaration count、module hash、声明归属、依赖缺失、依赖循环 | 全部在协调前拒绝，不写 receipt |
| 增量状态机 | cold EXPAND/FINALIZE、warm skip、新 generation 同内容 skip、单模块变化、FINALIZED 满足后续 EXPAND、不同 environment/app/service 隔离 | 全部通过 |
| 故障与恢复 | 已成功模块变更失败保持旧数据/receipt、多模块中间失败保留成功前缀并在重试时跳过、Local 稳定发布失败、staging PUT 后 publish 失败、同 generation 修复重试 | 全部通过；receipt 只记录成功边界，staging 最终为空 |
| 并发与 fence | 同 generation 两线程 APPLY、旧 generation VERIFY、同 generation fingerprint 漂移 | 并发收敛为一个执行者和一个跳过者；generation/fingerprint 均 fail closed |
| 文件后端 | 真实 Local、官方 MinIO S3-compatible；15 个对象逐一按大小和 SHA-256 回读；changed file 覆盖 | 两种后端完整生命周期均通过 |
| 最终发布物 | `process-classes` 生成物进入最终 Spring Boot JAR，manifest 与对象字节一致 | Maven Invoker 1/1 通过 |
| 消费者回归 | Authorization 手工 Spring 测试配置、File Core 真实 MySQL 并发保存、fileproc 正常 reactor | 全部通过 |

真实公有云 OSS/COS/Kodo 的 IAM、TLS、区域 endpoint、限流和网络故障行为不在本地可复现边界内；MinIO 只证明 S3-compatible 协议与对象语义，不等价于这些云厂商的生产验收。

## 6. 验证能力

- M09：直接修改 Maven 模块编译、`mvn verify` 和范围检查。
- M10：模块 manifest、hash、依赖排序和状态转换单元测试。
- M11：H2/MySQL 隔离数据库、Local/MinIO 存储契约和 Bootstrap 集成测试。
- M14：跨模块高影响设计的独立复核。
- 不启用 M12/M13：本次没有新的 HTTP/API 消费入口或浏览器结果。

本轮 AC-009 至 AC-013 已新增自动化测试资产并完成测试源码编译；受当前 Agent `simple` Skill 的测试执行限制，本轮不执行这些测试，运行结果保留为 PR CI 后续验证项。
