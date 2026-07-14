---
documentId: SRS-GITHUB-JENKINS-RELEASE
documentType: system-requirements
pmoVersion: 1.2.1
schemaRevision: 1
riskLevel: L3
riskAssessmentEvidence: BRD-GITHUB-JENKINS-RELEASE 的平台发布影响与内网凭据边界评估
status: APPROVED
action: NEXT
owner: Mango 发布系统负责人
approver: HardyDou
approvalEvidence: review/SRS-GITHUB-JENKINS-RELEASE.md
upstreamDocumentId: BRD-GITHUB-JENKINS-RELEASE
upstreamDocumentHash: 9191933df39c476bb3df630d62afbf9646e89c90923c6eb06ca61c6791e927b8
---

# GitHub 到内网 Jenkins 发布系统需求规格说明书

## 1. 系统范围与上下文

| 上下文ID | 上游ID | 系统责任 | 边界外责任 | 参与方 | 可观察输出 |
|---|---|---|---|---|---|
| SC-001 | BG-001, BS-001, BS-002, BF-001 | GitHub 校验发布元数据并派发任务；内网 Runner 主动领取；Jenkins 按精确提交执行；结果返回同一 GitHub 运行记录 | Nexus 对外开放、公共 PR 内网执行、npm 和生产部署不属于本系统 | 发布负责人、GitHub Actions、内网 Runner、Jenkins、Gitea 或 GitHub 源、Nexus | 前置检查、等待状态、Jenkins 构建号、最终结果和 dry-run 发布计划 |

## 2. 系统参与者与访问行为

| 系统参与者ID | 上游参与者ID | 使用入口类别 | 可见范围 | 可执行动作 | 禁止动作 | 用户可见原因 |
|---|---|---|---|---|---|---|
| SA-001 | BA-001 | GitHub 手工 Workflow | Mango 主仓 main 的提交、输入版本和本次内网执行结果 | 发起 dry-run；在版本锁与变更日志就绪后发起正式发布；取消等待任务 | 从其它分支正式发布、读取 Jenkins Token、让 PR 任务使用发布 Runner | 发布负责人只需在 GitHub 完成操作，同时保持内网凭据不可见 |

## 3. 功能需求

| 功能ID | 上游ID | 触发者ID | 前置条件 | 输入信息语义 | 系统行为 | 成功反馈 | 失败或禁止反馈 | 状态影响 |
|---|---|---|---|---|---|---|---|---|
| FR-001 | BG-001, BF-001, BR-001, BAC-001 | SA-001 | Workflow 从 main 手工发起，内网 Runner 在线，Jenkins Job 可调用 | 完整 Git SHA、Maven 版本、GitHub 请求号、dry-run 和测试标志 | 先校验 main、版本和变更日志，再把同一参数交给内网；Jenkins 校验提交可从 main 到达，调用非 app 发布批次；Runner 轮询并映射最终结果 | GitHub 显示 Jenkins 构建号、链接和 SUCCESS，dry-run 不写 Nexus | 参数非法、提交不可达、Runner 离线、Jenkins 拒绝、超时、构建失败或版本冲突时返回失败并停止 | BO-001 从等待内网执行进入已完成成功或失败 |

## 4. 用户场景与交互流程

| 场景ID | 上游流程ID | 功能ID | 参与者ID | 入口 | 前置状态 | 用户动作 | 系统反馈 | 替代或异常路径 | 完成状态 |
|---|---|---|---|---|---|---|---|---|---|
| UC-001 | BF-001 | FR-001 | SA-001 | GitHub Actions 的 Mango Maven Release 页面 | main 已包含待验证提交 | 输入唯一 prerelease 版本，保持 dry-run 并运行 Workflow | 公网前置检查先完成，内网任务随后显示排队、执行和最终 Jenkins 结果 | Runner 离线时保持排队；取消 GitHub 任务时停止对应 Jenkins 队列或构建；失败时保留构建链接 | dry-run 成功且 Nexus 无新增版本 |

## 5. 页面、信息与动作需求

| 页面ID | 页面名称 | 用途 | 参与者ID | 信息区域 | 页面状态 | 功能ID |
|---|---|---|---|---|---|---|
| PG-001 | Mango Maven Release Workflow | 输入发布参数并查看公网与内网两段执行结果 | SA-001 | 版本、dry-run、测试标志、提交、请求号、Jenkins 构建和结果摘要 | 等待、运行、成功、失败、取消 | FR-001 |

| 动作ID | 页面ID | 动作名称 | 显示条件 | 可用条件 | 用户交互 | 成功反馈 | 失败反馈 | 功能ID |
|---|---|---|---|---|---|---|---|---|
| BT-001 | PG-001 | Run workflow | 具有仓库写权限 | 选择 main 并填写合法版本 | 先选择 dry-run，确认后提交 | 两个 Job 依次完成并在摘要中显示 Jenkins 结果 | 前置条件或内网任务失败时显示失败步骤和日志入口 | FR-001 |

| 页面ID | 信息名称 | 业务语义 | 来源类别 | 必填条件 | 输入限制 | 空值含义 | 展示要求 |
|---|---|---|---|---|---|---|---|
| PG-001 | Maven 版本 | 本次计划验证或发布的不可变版本 | 用户输入并由仓库版本锁复核 | 每次执行必填 | SemVer-like；正式发布必须等于版本锁 | 不允许为空 | 在前置检查、Jenkins 构建名称和最终摘要中保持一致 |

| 页面ID | 状态类型 | 触发场景 | 展示内容 | 可见动作 | 可用动作 | 不可操作原因 |
|---|---|---|---|---|---|---|
| PG-001 | 正常 | Jenkins 已取得构建号或返回 SUCCESS | 版本、构建号、运行状态和最终结果 | 查看、取消或查看摘要 | 运行时可取消，完成后可查看 | NONE |
| PG-001 | 空 | 尚未发起过发布请求 | Workflow 输入说明 | Run workflow | Run workflow | NONE |
| PG-001 | 加载 | 没有匹配的在线 Runner、Jenkins 无空闲执行器或构建仍在运行 | 当前等待 Job、GitHub 请求号、构建号和运行状态 | 取消、查看日志 | 取消、查看日志 | 内网资源尚未领取或执行完成 |
| PG-001 | 失败 | 任一前置或 Jenkins 步骤失败 | 失败阶段、错误摘要和内部构建链接 | 查看日志、重新发起新请求 | 查看日志 | 失败请求不可改写，只能修复原因后新建请求 |
| PG-001 | 不可操作 | 用户取消、超时或请求已完成 | 终止原因和已执行范围 | 查看日志 | 查看 | 请求已终止，参数不可修改 |

## 6. 逻辑数据需求

| 数据需求ID | 上游或功能ID | 业务信息 | 来源 | 使用场景 | 完整性或唯一性口径 | 保留要求 | 敏感级别 | 空值业务语义 |
|---|---|---|---|---|---|---|---|---|
| DR-001 | BO-001, FR-001 | Git SHA、版本、请求号、dry-run、测试标志、Jenkins 队列号、构建号和结果 | GitHub 运行上下文、用户输入和 Jenkins 返回 | 参数传递、状态轮询、取消和结果摘要 | 请求号唯一；Git SHA 固定 40 位；一个请求只映射一个 Jenkins 构建 | 遵循 GitHub 与 Jenkins 各自构建保留策略 | 内部；Jenkins Token 为敏感且不属于业务记录 | 构建号仅在 Jenkins 接受任务后出现，之前为空 |

## 7. 外部交互需求

| 外部交互ID | 上游或功能ID | 外部参与方 | 业务目的 | 触发条件 | 输入业务信息 | 输出业务信息 | 时效要求 | 重复或失败处理 | 责任边界 |
|---|---|---|---|---|---|---|---|---|---|
| IR-001 | BF-001, BR-001, FR-001 | GitHub Actions 与内网 Jenkins | 让不可被公网访问的 Jenkins 接收经过 GitHub 校验的发布请求 | 公网前置检查成功且匹配 Runner 在线 | Git SHA、版本、请求号、dry-run 和测试标志 | 队列位置、构建号、运行状态和最终结果 | Runner 在线时应在一分钟内领取；发布总等待上限三小时 | GitHub 同一 Workflow 使用串行发布并发组；失败、取消和超时向 Jenkins 传播停止请求 | GitHub 负责身份与 main 入口；Runner 负责安全转发；Jenkins 负责真实构建与 Nexus 写入 |

## 8. 非功能需求

| 非功能ID | 上游ID | 类别 | 适用场景 | 度量指标 | 目标值 | 测量条件 | 失败影响 | 验收方式 |
|---|---|---|---|---|---|---|---|---|
| NFR-001 | BG-001, BS-001, BR-001, BAC-001 | 安全与可靠性 | 所有发布请求 | 公网入站端口、凭据暴露、提交漂移和状态误报数量 | 均为零；默认 dry-run；Jenkins 非成功必须使 GitHub 失败 | Runner 通过出站 443 连接 GitHub，Jenkins/Nexus 保持内网 | 可能泄露发布权限或产生错误平台版本 | 静态脚本测试、GitHub 到 Jenkins dry-run 和 Nexus 版本前后对比 |

## 9. 系统验收标准

| 系统验收ID | 业务验收ID | 系统需求ID | 前置状态 | 用户或外部动作 | 可观察结果 | 失败或边界结果 | 验收类型 |
|---|---|---|---|---|---|---|---|
| SAC-001 | BAC-001 | FR-001, UC-001, PG-001, BT-001, DR-001, IR-001, NFR-001 | main 上 Workflow 可用，Runner 和 Jenkins 在线 | 从 main 发起唯一 prerelease 版本的 dry-run | 前置 Job 通过；内网 Runner 领取；Jenkins 精确检出 SHA；非 app 发布命令以 dry-run 运行；GitHub 显示同一构建号和 SUCCESS；Nexus 无新增版本 | 非 main、短 SHA、非法版本、Runner 离线、Jenkins 失败或超时分别产生明确失败，不泄露 Token | STATIC 与 API |

## 10. 系统需求追踪矩阵

| 上游ID | 系统需求ID | 系统验收ID | 覆盖说明 |
|---|---|---|---|
| BP-001, BG-001, BS-001, BS-002, BA-001, BO-001, BF-001, BR-001, BAC-001 | SC-001, SA-001, FR-001, UC-001, PG-001, BT-001, DR-001, IR-001, NFR-001, SAC-001 | SAC-001 | 全部业务项落实为 GitHub 前置、内网领取、Jenkins 精确构建、失败传播和 dry-run 验收 |

## 11. 阶段判定与审批

| 检查项 | 结果 | 证据 |
|---|---|---|
| 系统需求 checker | PASS | `check-system-requirements` 输出 |
| 生命周期 handoff | PASS | BRD 摘要和追踪检查输出 |
| 未关闭阻断数量 | 0 | 无开放阻断 |
| 系统需求审批 | APPROVED | `review/SRS-GITHUB-JENKINS-RELEASE.md` |
