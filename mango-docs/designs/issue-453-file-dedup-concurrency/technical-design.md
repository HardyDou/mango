---
documentId: TDD-ISSUE-453
documentType: technical-design
pmoVersion: 1.1.0
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: rules/09-test-case-automation-flow.md 中并发与数据一致性变更的 L3 判定
status: APPROVED
action: NEXT
owner: Mango 文件能力负责人
approver: HardyDou
approvalEvidence: review/TDD-ISSUE-453.md
upstreamDocumentId: SRS-ISSUE-453
upstreamDocumentHash: 621143501dc466937951e1a84c4f88e329c5b1108a5cd0e9bf7e8a95863e8bcf
---

# 相同内容并发保存技术设计文档

## 1. 设计输入、约束与决策

| 决策ID | 问题 | 候选方案 | 选择 | 理由 | 来源ID或路径 | 是否推断 | 影响 | 风险 | 回退条件 |
|---|---|---|---|---|---|---|---|---|---|
| DEC-001 | 相同哈希首次并发保存时多个事务均先查未命中，后插入者触发唯一键冲突 | 分布式锁；数据库方言 upsert；保留唯一约束并恢复竞争失败 | 保留唯一约束作为最终仲裁，捕获明确的唯一键冲突后使用当前读查询并复用胜出对象 | 不依赖 Memory、Redis 或 JDBC 锁，不增加配置和方言 SQL；数据库不变量仍是唯一正确性来源 | FR-001, NFR-001, SAC-001；`FileServiceImpl.java`；Issue 453 | 否 | 文件对象与哈希映射创建改为幂等，所有现有保存入口受益 | 普通快照读可能看不到刚提交的胜出对象；失败方可能已写入不同对象名 | 当前读未能找到匹配的已完成对象时通过 `Require + FileCode.FILE_STORE_FAILED` 终止，禁止吞掉非目标冲突或直接抛出运行时异常 |
| DEC-002 | 文件服务新增 `Require` 调用触发业务码包路径规范门禁 | 绕过门禁；保留旧包兼容层；直接迁移规范包路径 | 将 `FileCode` 从 `io.mango.file.api` 迁移到 `io.mango.file.api.enums`，删除旧入口并更新仓库内全部引用 | 业务码数值、消息和调用语义完全不变；项目按新版本整体发布，用户明确无需历史包路径兼容 | rules/backend/03-api.md；用户 2026-07-13 指示 | 否 | Java import 路径变化，HTTP、应用服务、错误码数值和业务特性不变 | 外部源码若仍引用旧包路径需随版本升级修改 import | 全 Reactor 编译与架构门禁必须通过；不得保留重复枚举或兼容门面 |

## 2. 模块与依赖边界

| 模块设计ID | 模块或包 | 职责 | 改动类型 | 依赖方向 | 公开能力 | 系统需求ID | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|
| MOD-001 | `mango-file-core` 的文件服务实现与模块内测试 | 在现有事务中创建或复用物理文件对象、维护哈希映射和引用关系 | 局部实现与回归测试 | 保持 core 依赖现有持久化和存储抽象，不增加 KV 依赖 | 现有上传、生成文件、资料打包和分片完成能力，公开签名不变 | SC-001, SA-001, FR-001, UC-001, IR-001, NFR-001, SAC-001 | rules/backend/01-code.md, rules/backend/05-module.md, rules/backend/08-test.md | 受影响模块编译、五并发集成测试、现有模块测试和质量门禁 |
| MOD-002 | `mango-file-api` 业务码契约 | 将文件业务码放入规范 `api.enums` 包并供 core、remote starter 与测试统一引用 | 包路径规范化 | 下游仍只依赖 `mango-file-api`，不改变模块依赖方向 | `FileCode` 常量集合、数值和消息不变 | SC-001, IR-001, NFR-001 | rules/backend/02-naming.md, rules/backend/03-api.md, rules/backend/05-module.md | 全 Reactor 编译、架构门禁与文件模块回归 |

## 3. 技术对象与状态模型

| 模型ID | 上游ID | 模型职责 | 标识 | 关系 | 状态编码 | 审计或历史 | 归属或租户 | 一致性约束 |
|---|---|---|---|---|---|---|---|---|
| DM-001 | FR-001, DR-001 | `file_object` 表达唯一物理内容，`file_hash_mapping` 提供秒传定位，`file_record` 表达每次独立保存结果 | 物理内容由存储配置、桶、哈希和大小唯一识别；文件结果使用独立标识 | 一个物理内容对应多个文件结果和至多一个当前范围哈希映射 | 物理内容和文件结果沿用已完成状态；哈希映射沿用启用状态 | 沿用现有创建、更新人员和时间字段 | 文件结果与映射按现有租户范围处理，物理内容按既有存储唯一口径复用 | 同一物理内容只保留一行；五次保存保留五条结果；引用数等于有效结果数 |

| 模型ID | 当前状态 | 触发 | 目标状态 | 前置条件 | 副作用 | 失败处理 | 上游ID |
|---|---|---|---|---|---|---|---|
| DM-001 | 不存在相同物理内容 | 五个事务并发保存相同内容 | 一个物理内容为已完成，五个文件结果为已完成 | 租户、存储、哈希和大小有效 | 创建或复用哈希映射，原子累计五次引用，清理竞争失败方的不同对象名 | 仅目标唯一键冲突进入复用；回查不匹配或存储本身失败时保留既有失败语义 | FR-001, UC-001, DR-001, SAC-001 |

## 4. 系统流程、事务与一致性

| 流程设计ID | 系统需求ID | 调用入口 | 参与模块 | 处理顺序 | 事务边界 | 状态变化 | 幂等键 | 并发策略 | 外部失败与补偿 | 用户可见结果 |
|---|---|---|---|---|---|---|---|---|---|---|
| FLOW-001 | FR-001, UC-001, SAC-001 | 现有文件上传、生成文件、资料打包及分片完成入口 | MOD-001 | 计算哈希并查询既有映射；写入存储；尝试插入物理对象；冲突时按唯一口径当前读胜出对象；幂等维护哈希映射；建立独立文件结果；原子增加引用数 | 沿用各公开保存方法现有事务，数据库写入同一事务；对象存储写入在事务外部资源上通过补偿收敛 | 首个事务创建物理对象，其余事务复用；每个事务创建自己的文件结果 | 存储配置、桶、文件哈希、文件大小 | 数据库唯一约束最终仲裁；明确捕获 DuplicateKeyException；使用 `LIMIT 1 FOR UPDATE` 当前读绕过旧快照；哈希映射执行同等幂等恢复 | 写入失败统一使用 `Require + FileCode` 保持 Service 错误契约；失败方对象名与胜出对象不同时删除失败方对象；删除失败记录上下文但不反转已成立的数据库复用 | 五次并发保存均返回独立可用结果且内容一致 |

## 5. API 与远程契约设计

| 接口ID | 系统需求ID | 调用方 | 所属模块 | 入口类型 | 方法与路径 | Command Query或VO | 返回契约 | 校验 | 权限租户或数据权限 | 幂等分页或排序 | 错误码 | 兼容策略 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| API-001 | FR-001, UC-001, SAC-001 | 业务消费系统 | MOD-001 | 现有文件资料打包入口，代表同一核心保存链路 | POST /file/files/package | FilePackageCommand 与 FileRecordVO | R<FileRecordVO> | 沿用文件内容、名称、大小、类型和业务归属校验 | 沿用当前租户、访问级别和文件可见范围校验 | 相同内容并发在内部收敛，每次请求仍返回独立文件结果 | 存储失败继续使用 FILE_STORE_FAILED；目标唯一键竞争不再暴露为错误 | 方法、路径、请求和响应完全兼容 | rules/backend/03-api.md | 现有 Controller 测试及 core 五并发测试 |

## 6. 持久化与数据迁移设计

| 数据设计ID | 上游或模型ID | 表或实体 | 字段变化 | 约束 | 索引 | 租户审计 | Mapper边界 | 数据来源 | migration或回填 | 回滚或补偿 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| DB-001 | DR-001, FR-001, DM-001 | `file_object`、`file_hash_mapping`、`file_record` 及现有实体 | 无 | 复用 `uk_file_object_hash_storage` 与 `uk_file_hash_mapping_target` | 无变化 | 沿用现有租户和审计字段 | 现有 Mapper 负责插入、当前读、更新和原子引用增量，不新增跨模块仓储 | 文件内容哈希、存储配置、租户上下文和保存请求 | 不需要 migration 或历史回填 | 非目标冲突通过 `Require + FileCode.FILE_STORE_FAILED` 终止；失败方不同对象名执行存储删除补偿；代码回滚即可恢复旧行为 | rules/backend/04-db.md, rules/backend/07-persistence.md | `mango workspace init` 分配的 worktree 专属 MySQL 8.4 数据库使用真实 Mapper、唯一约束、InnoDB 事务和默认隔离级别执行五并发测试；模块 verify |

## 7. 安全、权限、租户与数据边界

| 安全设计ID | 系统需求ID | 能力 | 权限资源 | 默认授权 | 后端校验入口 | 租户边界 | 数据归属断言 | 前端反馈 | 审计 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| SEC-001 | SA-001, FR-001, DR-001, SAC-001 | 相同内容文件复用 | 不新增权限资源 | 不改变现有授权 | 现有文件服务入口与上下文校验 | 哈希映射继续按配置的租户或全局范围定位，文件结果保持请求租户归属 | 只复用同一存储唯一口径下的已完成物理对象，不复用文件结果或访问权限 | 沿用现有成功和失败反馈 | 新文件结果沿用请求用户审计；复用对象保留原创建审计 | rules/backend/06-security.md | 并发测试固定同一测试租户并断言每条文件结果租户归属 |

## 8. 错误码、异常与可观测性

| 错误设计ID | 系统需求ID | 失败场景 | 触发条件 | 错误码 | 异常类型 | 用户反馈 | 日志上下文 | 指标或告警 | 重试或补偿 | 敏感信息处理 |
|---|---|---|---|---|---|---|---|---|---|---|
| ERR-001 | FR-001, UC-001, SAC-001 | 物理对象或哈希映射发生目标唯一键竞争 | 并发事务在先查未命中后同时插入相同唯一口径 | 不新增错误码；竞争恢复成功返回正常结果，恢复无法确认时沿用 FILE_STORE_FAILED | DuplicateKeyException 仅在精确插入边界捕获；无法回查时调用 `Require.fail(FileCode.FILE_STORE_FAILED)` | 正常竞争不显示错误；真实存储或持久化失败保持既有反馈 | 失败方对象名、胜出对象名、存储配置、桶、哈希和大小；不记录文件正文 | 存储补偿失败记录警告日志 | 当前读一次确认胜出对象；不同对象名执行一次删除补偿，不做无界重试 | 不记录文件正文、访问凭证、token 或密钥 |

## 9. 前端结构与交互实现映射

| 前端设计ID | 系统需求ID | 页面或动作 | 页面key或路由 | 区域与组件 | 状态来源 | API依赖 | 权限或不可操作 | 空加载或失败态 | 语义测试锚点 | 复用判断 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|
| UI-001 | FR-001, PG-001, BT-001, SAC-001 | 业务消费系统的文件保存结果呈现 | 不修改 Mango 前端页面或路由 | 继续由消费方既有区域和组件呈现 | API-001 的既有返回 | API-001 | 沿用消费方既有访问边界 | 现有状态无需变化；并发竞争由失败转为正常结果 | 不新增前端锚点 | 后端行为修复即可满足需求，无前端代码交付 | rules/frontend/04-test.md |

## 10. 测试设计与验收映射

| 测试用例ID | 系统验收ID | 设计项ID | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 权限或租户边界 | 稳定契约 | 执行入口 | 证据 | 失败处理 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| TC-453 | SAC-001 | DEC-001, MOD-001, DM-001, FLOW-001, API-001, DB-001, SEC-001, ERR-001, UI-001, IMP-001 | 五个线程从无既有对象开始并发保存完全相同字节 | P1 | 集成测试 | AUTO | `IT_453_` 唯一前缀、相同字节和同一存储配置；测试只允许连接名称匹配 `mango_dev_*` 的 worktree 专属 MySQL 8.4 数据库，测试前重建目标表 | 固定测试租户与用户；不接触共享数据库 | 真实 MySQL、真实 Mapper、唯一约束、Spring 事务、线程屏障和线程安全存储替身；断言五次成功、一对象、一映射、五结果、引用数五、存储对象一份 | `set -a; source .mango/dev-workspace.env; set +a; mvn -f mango/pom.xml -pl mango-platform/mango-file/mango-file-core -Dtest=FileServiceConcurrentSaveIntegrationTest test` | `mango-docs/evidence/issue-453-file-dedup-concurrency/test-baseline.md` | 数据库名不匹配 `mango_dev_*` 或任一断言失败即阻断提交；不得降级为 H2、降低并发数或弱化断言 | rules/09-test-case-automation-flow.md, rules/backend/08-test.md |

## 11. 兼容、发布与能力文档影响

| 影响ID | 设计项ID | 影响对象 | 当前行为 | 目标行为 | 兼容策略 | 升级或回滚 | README或能力地图 | 发布批次 | 验证 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|---|
| IMP-001 | DEC-001, DEC-002, MOD-001, MOD-002, DM-001, FLOW-001, API-001, DB-001, SEC-001, ERR-001, UI-001 | Mango 文件能力消费者与后端发布物 | 相同内容首次并发保存可能返回失败，文件业务码仍位于非规范包路径 | 相同请求全部成功并复用物理内容；业务码位于规范 `api.enums` 包 | HTTP、应用服务、配置、数据结构、错误码数值和单线程行为不变；Java 消费者随新版本更新 import，不保留旧路径兼容 | 随下一后端修复版本升级；回滚代码即可恢复旧行为，无数据迁移 | 更新模块 README 的 `FileCode` import 说明；能力地图不变，因为文件业务能力与接入流程未变化 | 后端文件模块修复批次，本任务不执行版本发布 | 全 Reactor 编译、模块测试、质量门禁和 PR 检查 | Mango 文件能力负责人 |

## 12. 技术追踪矩阵

| 上游ID | 设计项ID | 测试用例ID | 覆盖说明 |
|---|---|---|---|
| SC-001, SA-001, FR-001, UC-001, PG-001, BT-001, DR-001, IR-001, NFR-001, SAC-001 | DEC-001, DEC-002, MOD-001, MOD-002, DM-001, FLOW-001, API-001, DB-001, SEC-001, ERR-001, UI-001, IMP-001 | TC-453 | 全部系统需求由数据库仲裁、事务内恢复、规范业务码契约、存储补偿和五并发真实持久化断言覆盖 |

## 13. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 技术设计 checker | PASS | `node mango-pmo/tools/check-technical-design.mjs --document mango-docs/designs/issue-453-file-dedup-concurrency/technical-design.md` |
| 生命周期 handoff | PASS | `node mango-pmo/tools/check-lifecycle-handoff.mjs --brd mango-docs/designs/issue-453-file-dedup-concurrency/business-requirements.md --srs mango-docs/designs/issue-453-file-dedup-concurrency/system-requirements.md --tdd mango-docs/designs/issue-453-file-dedup-concurrency/technical-design.md --risk L3 --through tdd` |
| 专项规范检查计划 | PASS | 后端测试质量、Mockito 边界、模块 verify、PMD、Checkstyle 和 Mango 检查均已进入 TC-453-001 与实施计划 |
| 未关闭阻断数量 | 0 | 当前设计无开放阻断或例外 |
| Tech Lead 审批 | APPROVED | `review/TDD-ISSUE-453.md` |
