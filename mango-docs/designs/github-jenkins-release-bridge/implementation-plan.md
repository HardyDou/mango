---
documentId: PLAN-GITHUB-JENKINS-RELEASE
documentType: implementation-plan
pmoVersion: 1.2.1
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: TDD-GITHUB-JENKINS-RELEASE 的最终风险评估
status: APPROVED
action: NEXT
owner: Mango 发布实施负责人
approver: HardyDou
approvalEvidence: review/PLAN-GITHUB-JENKINS-RELEASE.md
upstreamDocumentId: TDD-GITHUB-JENKINS-RELEASE
upstreamDocumentHash: 6811d37250f0c0576f022e8cab1bc9f0898ad26f329baa0139b681297ce5517d
---

# GitHub 到内网 Jenkins 发布实施计划

## 1. 实施目标、范围与交付物

| 交付物ID | 技术设计ID | 交付物 | 路径或模块 | 完成状态定义 | 验收来源 | 不处理边界 |
|---|---|---|---|---|---|---|
| DEL-001 | DEC-001, DEC-002, MOD-001, MOD-002, MOD-003, DM-001, FLOW-001, API-001, DB-001, SEC-001, ERR-001, UI-001, TC-001, IMP-001 | 可审计的 GitHub Workflow、桥接脚本、Jenkinsfile、自动化测试和使用说明 | `.github/workflows/maven-release.yml`, `scripts/ci`, `scripts/tests`, `jenkins` | 静态测试、脚本测试和文档门禁通过；不含明文凭据 | TC-001 | 不改变 PR required check，不发布 Maven 制品 |
| DEL-002 | DEC-001, DEC-002, MOD-001, MOD-002, MOD-003, API-001, SEC-001, IMP-001 | GitHub `mango-release` Environment、仓库级 Release Runner 和更新后的 Jenkins Job | GitHub 仓库与 `192.168.5.243` | Runner 在线且只带 `mango-release` 标签；Jenkins Job 使用受控参数并内联与仓库 Jenkinsfile 一致的脚本，启动时不额外克隆整仓；旧配置有备份 | TC-002 | 不部署 PR Runner，不开放公网入站 |
| DEL-003 | TC-001, TC-002, IMP-001 | dry-run 联通证据与资源升级结论 | `mango-docs/evidence/github-jenkins-release-bridge` | GitHub run、Jenkins build、精确 SHA、发布计划和 Nexus 无写入均可复核 | TC-001, TC-002 | 不执行 `dry_run=false` |

## 2. 工作分解

| 任务ID | 技术设计ID | 交付物ID | 责任角色 | 路径或模块 | 前置任务 | 具体动作 | 完成标准 | 验证ID | 实施批次 | 状态 |
|---|---|---|---|---|---|---|---|---|---|---|
| TASK-001 | DEC-001, DEC-002, MOD-001, MOD-002, MOD-003, DM-001, FLOW-001, API-001, DB-001, SEC-001, ERR-001, UI-001, TC-001, IMP-001 | DEL-001 | Dev/发布工程 | 任务 worktree | NONE | 实现 Workflow、桥接脚本、Jenkinsfile、测试和说明；执行本地门禁 | VAL-001 与 VAL-002 全部通过，提交中没有敏感值 | VAL-001, VAL-002 | batch-1 | PLANNED |
| TASK-002 | DEC-001, MOD-001, MOD-002, SEC-001, UI-001, IMP-001 | DEL-002 | 发布工程 | GitHub 与内网服务器 | TASK-001 | 合并 PR 后创建 Environment；安装独立系统用户的仓库 Runner；凭据只进入 Runner 服务环境；公网 Job 通过单文件 artifact 交付桥接脚本，Runner 不克隆 Mango 仓库 | GitHub 显示 Runner online；服务重启后自动恢复；PR Workflow 不引用该标签；内网 Job 不执行 actions/checkout | VAL-003 | batch-2 | PLANNED |
| TASK-003 | DEC-002, MOD-003, FLOW-001, API-001, DB-001, SEC-001, ERR-001, TC-002, IMP-001 | DEL-002, DEL-003 | 发布工程/QA | Jenkins `mango-maven-release` | TASK-002 | 备份 Job 与 compose；部署内联脚本与 Jenkinsfile 一致的新 Pipeline，避免定义阶段整仓克隆；配置运行时地址；从 main 发起唯一 prerelease dry-run；比较 Nexus 前后 | GitHub 与 Jenkins 同一 SHA、版本、请求号和成功结果；Nexus 无新增目标版本；证据完成 | VAL-003 | batch-3 | PLANNED |

## 3. 顺序、依赖与里程碑

| 里程碑ID | 包含任务ID | 进入条件 | 完成条件 | 依赖 | 可并行任务 | 阻塞升级 | 责任人 |
|---|---|---|---|---|---|---|---|
| MS-001 | TASK-001 | TDD 已批准且任务 worktree 基于最新 main | 仓库文件和本地验证完成 | NONE | NONE | 发现现有发布状态机冲突时停止并回到 Tech Lead | 实施负责人 |
| MS-002 | TASK-002, TASK-003 | MS-001 的 PR 已通过 required check 并合并 | Runner 在线、Job 更新、dry-run 端到端通过且 Nexus 无写入 | MS-001 | NONE | Jenkins 认证、Runner 网络或精确 SHA 失败时恢复备份并保留证据 | 实施负责人 |

## 4. 验证计划

| 验证ID | 测试或验收ID | 任务ID | 验证层级 | 命令或步骤 | 环境 | 测试数据 | 权限或租户边界 | 预期结果 | 证据路径 | 责任人 | 失败处理 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| VAL-001 | TC-001 | TASK-001 | STATIC 与组件 | `bash -n scripts/ci/jenkins-release-bridge.sh`；`node --test scripts/tests/jenkins-release-bridge.test.mjs`；检查 Workflow 与 Jenkinsfile diff | 本地任务 worktree，Node 26、bash、jq | 假 Jenkins queue/build JSON 和假 Token | 不访问真实 Jenkins/Nexus；无业务租户 | 短 SHA 被拒绝；成功结果产生 GitHub outputs；Token 不出现在输出；Jenkinsfile 只调用非 app batch | 本地命令输出与 `mango-docs/evidence/github-jenkins-release-bridge/test-baseline.md` | Dev | 任一失败在 TASK-001 修复并重跑 |
| VAL-002 | TC-001 | TASK-001 | STATIC | 运行四阶段 checker、lifecycle handoff、workspace layout、能力文档检查和 Git diff Secret 扫描 | 当前任务 worktree | 四文档、review 证据和 PR body | 审批人 HardyDou；不记录 Token | 所有 checker 通过，风险为 L3，STATIC/API 选择和跳过理由完整 | checker 输出与 PR required check | Dev/PMO | 按 ruleId 修复，禁止降低门禁 |
| VAL-003 | TC-002 | TASK-002, TASK-003 | API 与入口流程 | 确认 Runner online；记录 Nexus 目标版本不存在；从 main workflow_dispatch 唯一 prerelease dry-run；核对 GitHub run、Jenkins build、SHA、版本、请求号、batch 命令和 Nexus 前后 | GitHub、`192.168.5.243:8081`、内网 Nexus；当前先使用 4GB 服务器只 dry-run | `0.0.0-bridge.<run-id>` 与 main SHA | GitHub 发布负责人；Runner 只接 main 手工 Workflow；无业务租户 | 两端 SUCCESS；Jenkins 精确 SHA；命令包含 `--all-non-app --dry-run`；Nexus 没有该版本；凭据不出现在日志 | `mango-docs/evidence/github-jenkins-release-bridge/test-baseline.md` | QA/发布工程 | 任一不一致立即注销 Runner 或恢复 Job/compose 备份，不执行正式发布 |

## 5. 数据、升级、发布与回滚步骤

| 发布步骤ID | 技术设计ID | 环境 | 前置检查 | 动作 | 顺序 | 数据备份或回填 | 兼容窗口 | 验证 | 失败停止条件 | 回滚或补偿 | 责任人 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| REL-001 | DEC-001, DEC-002, DB-001, IMP-001 | GitHub 与内网 Jenkins | PR 已合并；旧 Job config 和 compose 已时间戳备份；Runner 出站 GitHub 与内网 Jenkins 连通；本次版本为 dry-run 唯一 prerelease | 先部署 Runner，再更新 Job，最后从 main 发起 dry-run | Runner -> Jenkins Job -> GitHub dry-run -> Nexus 回查 | 不涉及业务数据；只备份 Jenkins 配置文件；无回填 | 旧 Job 在切换前保持可用，切换窗口不执行其它发布 | VAL-003 | Runner 不在线、Jenkinsfile 解析失败、提交不可达、命令不是非 app dry-run、任一凭据泄露或 Nexus 出现新制品 | 停止容器/服务，移除 Runner 注册，恢复 Job 与 compose 备份并重启 Jenkins | 发布负责人 |

## 6. 文档与能力同步计划

| 文档项ID | 技术设计或交付物ID | 目标文档 | 变化 | 责任人 | 完成条件 | 检查命令 | 不适用依据 |
|---|---|---|---|---|---|---|---|
| DOC-001 | IMP-001, DEL-001, TASK-001 | `scripts/ci/README.md` 与 `mango-docs/capabilities/README.md` | 增加 GitHub 到内网 Jenkins 的入口、配置边界、正式发布命令、回滚和验证说明 | Dev/发布工程 | 维护者可从能力地图进入操作说明，所有地址和凭据边界与实际配置一致 | `node mango-pmo/tools/audit-module-readmes.mjs` 与 `node mango-pmo/tools/audit-readme-source-facts.mjs` | NONE |
| DOC-002 | DEL-003, TASK-003 | `mango-docs/evidence/github-jenkins-release-bridge/test-baseline.md` | 记录真实 dry-run 的 GitHub/Jenkins/Nexus 最小证据和 16 vCPU / 32GB 升级建议 | QA | 不含 Token；能复核同一 SHA、版本、构建号和 Nexus 无写入 | 人工证据审阅和能力文档门禁 | NONE |

## 7. 风险、阻塞与例外

| 风险ID | 风险等级 | 类型 | 触发条件 | 影响 | 预防 | 应对 | 责任人 | 截止时间 | 状态 | 例外ruleId | 例外批准与到期 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| RISK-001 | L3 | RISK | Release Runner 被公共 PR 或非 main Workflow 调用 | 不可信代码可能获得内网和发布权限 | 独立 `mango-release` 标签、Workflow 仅 `workflow_dispatch`、main 前置、GitHub Environment 和本机凭据；本次不部署 PR Runner | 立即注销 Runner、轮换 Jenkins Token、审计 Jenkins 与 Nexus | 发布负责人 | 2026-07-14 | CLOSED | NONE | NONE |
| RISK-002 | L3 | RISK | 当前 Jenkins 主机只有 4GB 内存，正式 Reactor 发布资源不足 | 构建变慢或被 OOM 终止 | 本次只 dry-run；用户确认后续升级到 16 vCPU / 32GB；正式发布前复核内存和 JVM/Maven 参数 | dry-run 后停止，不执行正式发布；升级完成后重新验证工具链与磁盘 | 发布负责人 | 2026-07-31 | CLOSED | NONE | NONE |

## 8. 实施追踪矩阵

| 上游设计ID | 交付物ID | 任务ID | 验证ID | 里程碑发布文档或风险项ID | 覆盖说明 |
|---|---|---|---|---|---|
| DEC-001, DEC-002, MOD-001, MOD-002, MOD-003, DM-001, FLOW-001, API-001, DB-001, SEC-001, ERR-001, UI-001, TC-001, TC-002, IMP-001 | DEL-001, DEL-002, DEL-003 | TASK-001, TASK-002, TASK-003 | VAL-001, VAL-002, VAL-003 | MS-001, MS-002, REL-001, DOC-001, DOC-002, RISK-001, RISK-002 | 所有技术设计均映射到仓库产物、Runner/Jenkins 部署、静态验证、真实 dry-run、文档和回滚控制 |

## 9. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 实施计划 checker | PASS | `check-implementation-plan` 输出 |
| 生命周期 handoff | PASS | TDD 摘要和追踪检查输出 |
| 依赖图 | PASS | TASK-001 -> TASK-002 -> TASK-003，无循环 |
| 未关闭阻断数量 | 0 | RISK-001 与 RISK-002 已有明确边界和停止条件；本次不执行正式发布 |
| 实施审批 | APPROVED | `review/PLAN-GITHUB-JENKINS-RELEASE.md` |
