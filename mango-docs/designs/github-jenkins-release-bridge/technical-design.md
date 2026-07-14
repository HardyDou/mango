---
documentId: TDD-GITHUB-JENKINS-RELEASE
documentType: technical-design
pmoVersion: 1.2.1
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: SRS-GITHUB-JENKINS-RELEASE 的平台发布、内网边界、不可变制品和失败传播风险评估
status: APPROVED
action: NEXT
owner: Mango Tech Lead
approver: HardyDou
approvalEvidence: review/TDD-GITHUB-JENKINS-RELEASE.md
upstreamDocumentId: SRS-GITHUB-JENKINS-RELEASE
upstreamDocumentHash: ced6e052286a1fa28d84ef35ff2cacfc87f2cb2df01978a845cb9b8244145f59
---

# GitHub 到内网 Jenkins 发布技术设计

## 1. 设计输入、约束与决策

| 决策ID | 问题 | 候选方案 | 选择 | 理由 | 来源ID或路径 | 是否推断 | 影响 | 风险 | 回退条件 |
|---|---|---|---|---|---|---|---|---|---|
| DEC-001 | GitHub 无法访问内网 Jenkins | 公网暴露 Jenkins、GitHub 托管节点打隧道、内网 Self-hosted Runner 主动领取 | 内网专用 Release Runner 仅出站连接 GitHub，再以内网地址调用 Jenkins | 不开放入站端口；状态仍属于原 GitHub 运行；符合 Nexus 不上公网的确认 | SC-001, IR-001, NFR-001 | 否 | 增加一个常驻 Runner 服务和桥接脚本 | Runner 若执行不可信 PR 会扩大内网攻击面 | Runner 无法限制到 main 手工发布时停用该 Runner 标签 |
| DEC-002 | Jenkins 当前按分支检出且执行全仓 deploy | 保留现状、仅改参数、使用受版本控制 Jenkinsfile 锁定 SHA 和非 app batch | 使用完整 SHA、请求号和显式版本；Jenkinsfile 调用 `publish-maven-batch.sh --all-non-app` | 消除分支漂移，遵循仓库当前发布门禁，并可由 PR 审计 | FR-001, SAC-001, `scripts/publish-maven-batch.sh` | 否 | 更新 Jenkins Job 参数与执行脚本 | 错误配置可能停止现有 Job | 备份 Jenkins Job config，dry-run 失败时恢复备份 |

## 2. 模块与依赖边界

| 模块设计ID | 模块或包 | 职责 | 改动类型 | 依赖方向 | 公开能力 | 系统需求ID | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|
| MOD-001 | `.github/workflows/maven-release.yml` | GitHub 手工入口、main 与版本前置检查、桥接脚本 artifact 交接、Runner 路由和结果摘要 | 新增 | 公网 Job 从批准 SHA 上传单文件短期 artifact；内网 Runner 不克隆 Mango 仓库，只下载该 artifact 并调用 Jenkins | Mango Maven Release 手工 Workflow | FR-001, UC-001, PG-001, BT-001 | rules/05-ai-delivery-quality.md | YAML 静态检查、Runner 无 checkout 断言与真实 workflow_dispatch dry-run |
| MOD-002 | `scripts/ci/jenkins-release-bridge.sh` | 校验输入、隐藏凭据、触发 Jenkins、轮询队列和构建、传播取消与结果 | 新增 | 只依赖 curl、jq 和 Jenkins Remote API | GitHub Runner 内部桥接命令 | FR-001, DR-001, IR-001, NFR-001 | rules/backend/06-security.md | Node 测试使用假 Jenkins 响应覆盖成功和参数失败 |
| MOD-003 | `jenkins/mango-maven-release.Jenkinsfile`, `jenkins/mango-maven-release-job.xml` | 精确检出、main 可达性、版本契约、Maven 工具缓存和非 app 发布 | Job XML 内联与受版本控制 Jenkinsfile 完全一致的 Pipeline 快照 | Jenkins 启动时不为读取 Jenkinsfile 预先克隆整仓；Pipeline 只按批准 SHA 检出一次并调用现有 Maven batch 脚本；Nexus 凭据继续由 Jenkins Maven 配置提供 | Jenkins `mango-maven-release` Pipeline | FR-001, SAC-001 | rules/05-ai-delivery-quality.md | 单元测试比较内联脚本与 Jenkinsfile；Jenkins dry-run、精确 SHA 与 Nexus 版本前后对比 |

## 3. 技术对象与状态模型

| 模型ID | 上游ID | 模型职责 | 标识 | 关系 | 状态编码 | 审计或历史 | 归属或租户 | 一致性约束 |
|---|---|---|---|---|---|---|---|---|
| DM-001 | DR-001, FR-001 | 在 GitHub 与 Jenkins 之间传递不可变发布请求 | `GITHUB_RUN_ID-GITHUB_RUN_ATTEMPT` | 一个请求对应一个 Jenkins 队列项和最多一个构建 | QUEUED, RUNNING, SUCCESS, FAILURE, CANCELLED, TIMEOUT | GitHub 与 Jenkins 各自保留运行日志和构建号 | Mango 主仓；不涉及业务租户 | Git SHA、版本、请求号和 dry-run 在两端必须完全一致；SUCCESS 只接受 Jenkins SUCCESS |

| 模型ID | 当前状态 | 触发 | 目标状态 | 前置条件 | 副作用 | 失败处理 | 上游ID |
|---|---|---|---|---|---|---|---|
| DM-001 | QUEUED | Jenkins 返回 executable URL | RUNNING | 队列未取消且构建号存在 | GitHub 开始轮询具体构建 | 非法 JSON、取消或超时使 GitHub Job 失败并请求取消 Jenkins | FR-001, SAC-001 |
| DM-001 | RUNNING | Jenkins `building=false` | SUCCESS 或 FAILURE | Jenkins 返回最终 result | 写入 GitHub outputs 和 step summary | 非 SUCCESS 使用非零退出码，禁止改写结果 | FR-001, SAC-001 |

## 4. 系统流程、事务与一致性

| 流程设计ID | 系统需求ID | 调用入口 | 参与模块 | 处理顺序 | 事务边界 | 状态变化 | 幂等键 | 并发策略 | 外部失败与补偿 | 用户可见结果 |
|---|---|---|---|---|---|---|---|---|---|---|
| FLOW-001 | FR-001, UC-001, SAC-001 | GitHub `workflow_dispatch` | MOD-001, MOD-002, MOD-003 | GitHub 校验 main 与版本并上传单文件桥接 artifact；Release Runner 下载该 artifact 后触发 Jenkins，不重复克隆仓库；Jenkins 重试等待镜像 SHA、检出并校验 main 可达；执行 batch；桥接轮询并回传 | 无跨系统数据库事务；不可变制品由发布前冲突检查和 Nexus 坐标保证 | DM-001 从 QUEUED 到 RUNNING 再到最终状态 | REQUEST_ID；正式制品再由 Maven 坐标唯一约束 | GitHub concurrency 与 Jenkins disableConcurrentBuilds 均串行 | GitHub 取消或桥接超时调用 Jenkins stop/cancel；正式发布失败后只允许按现有发布修复流程验证已尝试状态 | GitHub 显示前置检查、内网任务、构建号和最终同色结果 |

## 5. API 与远程契约设计

| 接口ID | 系统需求ID | 调用方 | 所属模块 | 入口类型 | 方法与路径 | Command Query或VO | 返回契约 | 校验 | 权限租户或数据权限 | 幂等分页或排序 | 错误码 | 兼容策略 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| API-001 | FR-001, SAC-001 | MOD-001 | MOD-002 | 内网 Remote API 归一化契约 | POST /job/mango-maven-release/buildWithParameters | GIT_SHA、RELEASE_VERSION、REQUEST_ID、DRY_RUN、RUN_TESTS | R<JenkinsBuildVO> | 桥接脚本限制 SHA、版本、请求号、布尔值、Job 路径和超时范围；内部将 HTTP 201 Location 与 queue/build JSON 归一化为该结果 | Jenkins API Token 只在 Runner 服务环境；Job 只从 main 手工 Workflow 调用；无业务租户 | REQUEST_ID 追踪；GitHub 与 Jenkins 双重串行；不分页 | BRIDGE_INPUT_INVALID, JENKINS_REJECTED, JENKINS_TIMEOUT, JENKINS_RESULT_FAILURE | 新增独立 Workflow，不改变现有 PR required check 名称 | rules/backend/06-security.md | 假响应自动化测试与真实 dry-run |

## 6. 持久化与数据迁移设计

| 数据设计ID | 上游或模型ID | 表或实体 | 字段变化 | 约束 | 索引 | 租户审计 | Mapper边界 | 数据来源 | migration或回填 | 回滚或补偿 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| DB-001 | DR-001, DM-001 | GitHub Workflow 运行记录和 Jenkins 原生 build metadata | 不新增 Mango 业务表或字段 | 由平台运行号、Jenkins 构建号和 Maven 坐标保证唯一 | 使用平台原生索引，不新增数据库索引 | 不涉及业务租户；日志禁止 Token | 不新增 Mango Mapper | GitHub 运行上下文与 Jenkins 构建元数据 | NONE；无需 migration 或回填 | 还原 Jenkins Job 备份并移除 Runner 注册；历史构建记录保留 | rules/backend/06-security.md | 文件 diff、Job 配置备份校验和 dry-run 运行记录 |

## 7. 安全、权限、租户与数据边界

| 安全设计ID | 系统需求ID | 能力 | 权限资源 | 默认授权 | 后端校验入口 | 租户边界 | 数据归属断言 | 前端反馈 | 审计 | 适用规范ruleId | 验证方式 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| SEC-001 | SA-001, FR-001, DR-001, SAC-001 | 触发内网 Maven 发布 | GitHub main、`mango-release` Environment、Self-hosted Runner 标签、Jenkins Job | GitHub 有写权限的发布负责人；Runner 不接受 pull_request | Workflow main 检查、Runner 标签路由、桥接输入校验、Jenkins main 可达性检查 | 不涉及业务租户；Runner 只能访问发布所需内网目标 | SHA 必须从镜像 main 可达；正式版本必须等于锁定版本 | 非法请求在公网前置或 Jenkins Contract 阶段明确失败 | 记录 GitHub run、attempt、SHA、版本和 Jenkins build，不记录 Token | rules/backend/06-security.md | 仓库 Secret 扫描、进程输出测试、非 main/短 SHA 负例和 dry-run |

## 8. 错误码、异常与可观测性

| 错误设计ID | 系统需求ID | 失败场景 | 触发条件 | 错误码 | 异常类型 | 用户反馈 | 日志上下文 | 指标或告警 | 重试或补偿 | 敏感信息处理 |
|---|---|---|---|---|---|---|---|---|---|---|
| ERR-001 | FR-001, UC-001, SAC-001 | Jenkins 请求、排队或构建失败 | HTTP 非 201、Location 缺失、JSON 非法、取消、超时或 result 非 SUCCESS | JENKINS_REJECTED / JENKINS_TIMEOUT / JENKINS_RESULT_FAILURE | Shell 非零退出码和 GitHub Job failure | 显示失败阶段、请求号、构建号和内网链接 | 只记录 Job、SHA、版本、request id、队列和构建 URL | GitHub Actions 失败通知与 Jenkins 失败记录 | 修复后使用新 attempt；超时和取消主动停止 Jenkins | Token 写临时 0600 netrc，退出删除；日志和摘要不输出 Token |

## 9. 前端结构与交互实现映射

| 前端设计ID | 系统需求ID | 页面或动作 | 页面key或路由 | 区域与组件 | 状态来源 | API依赖 | 权限或不可操作 | 空加载或失败态 | 语义测试锚点 | 复用判断 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|
| UI-001 | FR-001, PG-001, BT-001, SAC-001 | GitHub Actions 手工 Workflow 表单与执行图 | GitHub 仓库 Actions / Mango Maven Release | GitHub 原生 inputs、jobs、logs 和 step summary | MOD-001 Job 与 MOD-002 outputs | API-001 | GitHub 权限、main 和 Environment 决定可操作性 | GitHub 原生 queued/in progress/failure/cancelled 状态 | Workflow 名、Job 名和 outputs 作为稳定识别 | 完全复用 GitHub 原生 UI，不新增 Mango 前端代码 | rules/09-test-case-automation-flow.md |

## 10. 测试设计与验收映射

| 测试用例ID | 系统验收ID | 设计项ID | 场景 | 优先级 | 测试层级 | 自动化判断 | 测试数据 | 权限或租户边界 | 稳定契约 | 执行入口 | 证据 | 失败处理 | 适用规范ruleId |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| TC-001 | SAC-001 | DEC-001, DEC-002, MOD-001, MOD-002, MOD-003, DM-001, FLOW-001, API-001, DB-001, SEC-001, ERR-001, UI-001, IMP-001 | 短 SHA 拒绝、成功 Jenkins 响应、Token 不出现在输出、Jenkinsfile 锁定 SHA 且只调用非 app batch | P0 | STATIC 与组件 | AUTO | 临时目录和假 Jenkins JSON，不访问内网 | 使用假 Token；无业务租户 | 脚本退出码、GitHub outputs 和 Jenkinsfile 关键命令 | `node --test scripts/tests/jenkins-release-bridge.test.mjs` | 测试输出和交付基线 | 任一断言失败阻断提交 | rules/09-test-case-automation-flow.md |
| TC-002 | SAC-001 | DEC-001, DEC-002, MOD-001, MOD-002, MOD-003, DM-001, FLOW-001, API-001, DB-001, SEC-001, ERR-001, UI-001, IMP-001 | GitHub main 发起唯一 prerelease dry-run，Jenkins 精确检出并生成非 app 发布计划，Nexus 无写入 | P0 | API 与入口流程 | AUTO | `0.0.0-bridge.<run-id>` 和当前 main SHA | GitHub 发布负责人；Runner 无公共 PR 入口；无业务租户 | GitHub run id、SHA、版本、Jenkins build 和 Nexus 版本前后集合 | GitHub workflow_dispatch 与 Jenkins build log | GitHub URL、Jenkins build 号和 Nexus 对比摘要 | 任一端状态不一致或 Nexus 新增制品立即阻断并回滚 Job 配置 | rules/09-test-case-automation-flow.md |

## 11. 兼容、发布与能力文档影响

| 影响ID | 设计项ID | 影响对象 | 当前行为 | 目标行为 | 兼容策略 | 升级或回滚 | README或能力地图 | 发布批次 | 验证 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|---|
| IMP-001 | DEC-001, DEC-002, MOD-001, MOD-002, MOD-003, API-001, SEC-001 | Mango 发布维护者、GitHub 仓库和 Jenkins `mango-maven-release` Job | 人工进入 Jenkins，按分支执行旧全仓 deploy Pipeline | GitHub main 手工发起，精确 SHA、默认 dry-run、非 app batch、结果自动回传 | 现有 PR required check 身份不变；旧 Job config 先备份；首次只 dry-run | dry-run 失败时注销 Runner、恢复 Job config 和 compose 备份；正式发布仍遵循现有版本状态机 | 新增 `scripts/ci/README.md` 并更新能力地图 | 只发布 Maven 非 app 批次；本任务本身不发布制品 | TC-001 与 TC-002 | 发布负责人 |

## 12. 技术追踪矩阵

| 上游ID | 设计项ID | 测试用例ID | 覆盖说明 |
|---|---|---|---|
| SC-001, SA-001, FR-001, UC-001, PG-001, BT-001, DR-001, IR-001, NFR-001, SAC-001 | DEC-001, DEC-002, MOD-001, MOD-002, MOD-003, DM-001, FLOW-001, API-001, DB-001, SEC-001, ERR-001, UI-001, IMP-001 | TC-001, TC-002 | 所有系统要求均由出站 Runner、精确 SHA、Jenkins 非 app batch、安全边界、自动化脚本测试和真实 dry-run 承接 |

## 13. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 技术设计 checker | PASS | `check-technical-design` 输出 |
| 生命周期 handoff | PASS | SRS 摘要和追踪检查输出 |
| 专项规范检查计划 | PASS | GitHub Workflow、Shell 安全、Jenkins Pipeline、发布批次和能力说明检查清单 |
| 未关闭阻断数量 | 0 | 无开放阻断；正式发布不在本次联通验证范围 |
| Tech Lead 审批 | APPROVED | `review/TDD-GITHUB-JENKINS-RELEASE.md` |
