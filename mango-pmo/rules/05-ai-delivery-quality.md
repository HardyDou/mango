# AI 交付质量门禁

## 1. 适用范围

- 适用于开发、验证、发布、PR、Issue 处理和任务收尾。
- 本文件只约束交付行为；代码实现仍遵循对应前端、后端和测试规范。

## 2. 必须执行

1. 开工前锁定目标、范围和不处理范围。
2. 发现新问题时，先判断是否由本次改动引入。
3. 本次引入的问题必须当前任务修复；非本次问题由 Agent 自行发现时必须登记 Issue 后回到主线。用户在当前非 `main` 工作区明确要求解决，即视为已经重新确认范围，必须原地修复且不得新建 worktree。
4. 根因结论必须有基线和证据；基线包括 `main`、任务前版本、发布物料或业务消费项目。
5. 证据必须能定位对象和结果，至少包含代码/配置/数据/日志/请求/截图中的有效项；没有证据只能写推测。
6. 涉及运行态验收时，必须确认服务真实启动成功，并记录访问地址、后端健康检查、数据库名和账号标识；禁止记录密码、token。
7. 页面验收必须记录页面路径、菜单入口、操作步骤、测试数据、业务断言、UI 断言、截图、console/network 结果。
8. 发布后必须验证发布仓库和业务消费入口都能拿到目标版本；模板、starter、CLI 或依赖变更必须给出升版结论和依据。
9. 变更既有公共能力时，必须说明影响范围、兼容性、验证矩阵和发布策略。
10. 新增、修改、删除或重构对外能力时，必须按真实能力影响同步更新或具体说明不更新代码、README/使用说明、需求/设计文档、测试脚本和测试结果基线；风险低不能豁免已经发生的对外说明变化。
11. 按 `rules/09-test-case-automation-flow.md` 分别评估需求影响和解决方案风险，映射 SIMPLE、STANDARD、FULL；M09-M16 按真实观察面启用。M13 只覆盖用户可见布局/交互/浏览器结果；无 UI 的 L3 不伪造 M13。
12. 测试结果基线必须记录测试命令、环境和版本、数据库名或测试数据集标识、账号/租户标识、通过/失败/阻塞/例外列表、证据路径和相对上一基线的行为变化；禁止记录密码、token、密钥。
13. 任务结束必须确认无未说明变更、分支状态明确、服务关闭或说明保留原因。

## 2.1 发布升级日志门禁

发布 Mango npm 包、Maven 物料、CLI、starter、模板或平台级版本前，必须先维护平台级 `CHANGELOG.md`。

`CHANGELOG.md` 最新发布段必须写清：

- 发布对象和版本。
- 新特性和修复。
- 业务升级步骤。
- 已执行验证。

发布后必须创建或回填对应 GitHub Release，并保证 Release 正文包含同一组升级信息。

没有平台级升级日志、没有业务升级步骤、没有验证说明或没有对应 GitHub Release 时，禁止声明发布完成。

## 2.2 多包发布门禁

同一 release 涉及多个 npm 包、Maven 物料、CLI、starter 或模板时，必须先形成发布批次，按依赖顺序规划共享门禁和逐包动作。

必须执行：

- 批次内共享门禁只跑一次，包括升级日志、GitHub Release、样式聚合、package exports、consumer typecheck、release lock registry check。
- 逐包只执行目标包构建、版本存在检查、publish、npm-hosted/npm-group 或 Maven 仓库回查、tarball/产物校验。
- 依赖链按被依赖方先发布，例如 `grid-widgets -> admin-shell -> admin -> cli`。
- 同一 release 涉及多个 Maven jar、starter 或 remote-starter 时，必须使用一次 reactor deploy 发布完整批次，例如 `scripts/publish-maven-batch.sh <targets...>`；发布完整后端平台批次时必须使用 `scripts/publish-maven-batch.sh --all-non-app --release-version <version>`。
- 默认 Maven 发布批次只发布 API、core、support、starter、starter-remote、tools、parent 和聚合 POM 等可消费平台物料；禁止默认发布 `mango-app/**`、`*-app` 或 `*-capability-app` 部署入口 fat jar。
- `mango-app/**` 只作为部署入口，不属于默认 Maven release 物料；确需发布 app 制品时必须单独确认用途，并在发布脚本中显式传入目标模块和 `--include-apps`。
- 禁止使用 `mvn -f mango/pom.xml ... deploy` 作为后端平台默认发布命令；该命令会把 app fat jar 一起发布。
- Maven 发布后必须复用同一个临时本地仓库做统一拉取验证；默认只对对外消费入口执行 `dependency:get -Dtransitive=false`，只有验证完整业务消费链路时才启用传递依赖解析。
- 发布脚本没有 batch 能力时，禁止直接逐包调用会重复全量门禁的单包命令；必须先完成共享门禁，再使用跳过共享门禁的发布入口，或先补齐 batch 发布入口。
- 交付报告必须说明共享门禁只执行一次的命令，以及逐包发布和回查结果。

禁止：

- 同一 release 对每个包重复执行完整 consumer typecheck、全量 workspace build 或完整文档门禁。
- 同一 Maven release 对每个 starter 重复发布相同上游模块，或每个 artifact 使用独立空 Maven local repo 重复下载依赖。
- 后端平台发布直接执行全仓 `mvn deploy`，或在未确认用途时发布 app fat jar。
- 为了省时间跳过发布后仓库回查或 tarball/产物校验。

## 2.3 PR 评审门禁

评审 PR 前必须先识别 PR 改动内容：

- 确认 base、head、最新提交和文件清单。
- 按文件和 diff 判断改动类型、影响模块、接口/数据/配置/页面/发布物料变化。
- 专业不确定性、高影响决定或治理系统修改自身时启用 M14，并选择与问题相关的最小充分专家视角；额外外部评审成本或责任主体无法确定时再询问用户。
- M14=`ENABLE` 时，专家评审必须输出阻断问题、非阻断建议和结论。
- 存在阻断问题时不得合并；必须把问题登记到 PR 评论或评审结论中。
- 无阻断问题时，合并前必须记录评审视角、验证命令、未验证项和风险。
- PR 只检查模式和事实启用的能力说明、验证、复核、回读和人工验收证据；例外保留依据和剩余风险，不要求伪造无关字段。
- PR 保留需求影响、解决方案风险、二者最大值、交付模式、工作区决策和 M01-M16 执行证据；main 例外、降级和破坏性动作必须登记人工确认。

前端官方模块、admin 聚合入口、CLI 模块清单或样式发布物料发生变化时，PR 门禁必须包含：

- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`

任一检查失败时不得提交 PR 或合并。

## 2.4 Required Check、仓库治理模式与 CODEOWNERS 门禁

PMO 治理使用以下稳定检查身份：

| 项目 | 固定值 |
|---|---|
| PR 合同 Workflow 文件 | GitHub：`.github/workflows/pr-contract-check.yml` |
| PR 合同 Workflow 名称 | `PR Contract Checks` |
| PR 合同 Job ID / 默认 check-run 名称 | `pr-contract-check` |
| PR 合同页面显示 | `PR Contract Checks / pr-contract-check` |
| Workflow 文件 | GitHub：`.github/workflows/pmo-doc-check.yml`；Gitea：`.gitea/workflows/pmo-doc-check.yml` |
| Workflow 名称 | `PMO Documentation Checks` |
| Job ID / 默认 check-run 名称 | `pmo-doc-check` |
| PR 页面显示 | `PMO Documentation Checks / pmo-doc-check` |

必须执行：

- Mango 主仓 `main` 的 branch protection 或 ruleset 必须同时要求秒级 `pr-contract-check` 和重型 `pmo-doc-check` 成功；GitHub PR body 的 `edited` 事件只重跑前者，代码 SHA 未变化时禁止因此重跑后者。Gitea 和业务仓继续使用各自标准 `pmo-doc-check` Workflow；稳定检查身份和门禁含义不因仓库治理模式变化而关闭。
- 仓库必须在 `.github/branch-protection-policy.json` 声明 `single-owner` 或 `multi-maintainer`，并让远端 branch protection 与声明一致。
- `single-owner` 适用于只有一个最终 Owner、该 Owner 需要创建并合并 PR、协作者不能构成稳定独立审批门禁的仓库。该模式必须把 required approving review count 设为 `0`、关闭远端 Code Owner approval，同时保留 CODEOWNERS 作为责任范围和可选评审路由；Owner 只能在 required check 成功、对话已解决后通过 PR 合并。
- `multi-maintainer` 适用于至少两名可稳定承担独立评审的维护者。该模式必须要求至少一人批准并启用 Code Owner approval；CODEOWNERS 文件存在不等于审批已经启用。
- 仓库治理模式变化必须作为独立治理变更，更新机器策略、PMO 规则和远端保护证据；禁止在每次发布时临时开关审批门禁。
- 首次配置或修改保护规则时，必须从真实 PR 的 check-run 或 GitHub API 读取 context，禁止仅凭文档字符串猜测。
- 修改 workflow `name`、job ID、job `name` 或触发事件时，必须在同一变更中同步 branch protection/ruleset 和本节；旧 required check 不得因改名永久 pending。
- `pr-contract-check` 必须从受信任的 base SHA 读取合同 checker，只检出必需脚本并校验 PR body；禁止在该 Workflow 启动 pnpm、Java、Maven 或生成项目验证。
- 业务项目的 `pmo-doc-check` 必须对每个 PR 运行，不得配置会跳过整个 workflow 的 `paths`/`paths-ignore`；文档或后端没有变化时可以在 job 内执行可审计的轻量判定，但稳定 check-run 必须产生结果。
- 业务项目的同一 `pmo-doc-check` 必须从 `mango.config.json.paths` 读取后端、前端和业务文档根目录，并对 `paths.businessDocs` 执行 `check-document-set.mjs`；缺失配置时使用 `backend`、`frontend`、`business-docs` 默认值，配置存在但后端 POM 不存在时必须失败，禁止静默跳过。有后端影响时，质量门禁只对直接修改的 Maven 模块执行 `mvn verify`，依赖构建和消费者兼容性另行验证；根 POM、全局 parent、架构规则/插件或门禁变化才执行完整 Reactor。四类生命周期文档遗漏 `documentType`、使用未知类型、重复 ID、上游断链或摘要失效时必须失败。PMO 合同启用前形成的历史生命周期文档只能登记在业务文档根目录的 `.mango-pmo-legacy-documents.json`，逐文件记录相对路径、内容 SHA-256 和迁移原因；内容变化、路径失效、重复或越界时必须失败，新文档不得登记为历史基线。
- `mango-pmo` 规则、合同、Agent、Skill、模板、Java 架构 checker、PMO workflow、业务模板和 PMO/CLI 发布脚本必须由 `.github/CODEOWNERS` 覆盖。
- required check 成功只证明机器门禁通过，不能替代业务、架构、QA 或发布结论；`multi-maintainer` 还必须取得独立 Code Owner 结论，`single-owner` 由 Owner 的 PR 合并动作承担最终人工授权并保留 PR 记录。
- 声明“分支保护已生效”前，必须提供远端 ruleset/branch protection 或受保护 PR 的验证证据。

禁止：

- `multi-maintainer` 用 workflow 绿灯替代 Code Owner review。
- `single-owner` 关闭 required check、直接推送 `main`，或为单次发布临时关闭再恢复审批门禁。
- 在未验证远端配置时声明 required check 已启用。
- 为绕过失败而删除 required check、缩小 workflow 触发范围或把关键步骤改成非阻断。
- 只在人工命令中列出单文档 checker，却不让 CI 自动扫描 `business-docs`。

## 2.5 历史架构债务与递减预算

- **正向要求**：普通后端 PR 使用 Git 变更映射直接 Maven 模块；质量门禁只扫描这些模块，不使用 `-am` 或 `-amd` 扩大 Reactor。依赖构建、消费者编译和 API 兼容性验证根据当前依赖和兼容事实启用；`changed`/`no-new-violations` 只阻断本次可归因的新违规，partial-reactor 报告只作为本次门禁证据。`.github/workflows/architecture-debt-inventory.yml` 定时或手工执行完整 Reactor，报告使用 schema v2、`inventoryScope=full-reactor`、`issueInventory=all-detected-issues`，全部问题唯一归属 `moduleKey`。schema v4 预算保存全局和逐模块规则计数、稳定 identity 与受控首次纳管审计记录；历史问题修复后只能用同一份完整报告执行全局或 `--module ... --write` 下调，后续不得回升。
- **首次纳管**：base 中缺少 `module.properties` 的存量业务模块分两个 PR 处理。纳管 PR 只能新增目标 starter 的 `module.properties` 和债务预算，使用当前 PR base SHA，以及由 `requireFullReactor=true`、`inventoryOnly=true` 现场生成的完整 Reactor 报告，通过 `--onboard-module ... --module-properties ... --write` 登记首次可见的历史 identity；业务代码在纳管 PR 合并后的独立 PR 开发。`inventoryOnly` 不得用于部分 Reactor，且只产出清单、不独立作出准入决定。required check 必须以可信 CI 现场重建的报告执行普通 `--base-ref` 检查，`--baseline-only` 不得批准新纳管记录。既有纳管记录不可删除、修改或重复，后续 identity 只能递减。
- **禁止项**：禁止每个普通 PR 为历史盘点扫描全部 Reactor；禁止让未改动文件的存量问题阻断普通需求；禁止用历史预算放行本次新增违规；禁止在首次纳管 PR 中夹带 Java、POM、业务配置、规则升级或其它模块变更；禁止只选 starter 而漏掉同一业务域中实际增加 identity 的 API/Core 等 Maven 模块；禁止只选直接文件而漏掉其所属 Maven 模块；禁止在 partial 质量门禁中使用 `-am` 或 `-amd` 扩大 Reactor；禁止使用 partial-reactor 报告、`unknown` 模块或重复单模块报告修改正式预算；禁止在规则或模块之间转移违规维持总数；禁止手工抬高预算。
- **正例**：修改 `mango-system-core` 时，质量门禁只验证该模块并由架构验证模块执行规则，不扫描未修改的上下游；若方案改变公共 API，再单独选择消费者编译或 API 验证。定时完整扫描仍记录全仓 9,038 条和各模块余额。清理 core 后，从完整报告执行 `--module mango-system-core --write`，只降低该模块及全局聚合。
- **反例**：每个 starter 同步 PR 都扫描 212 个 Maven 模块；或从模块 A 删除一条旧 identity、在模块 B 新增一条后以总数没变为由更新预算。错误原因：前者把历史盘点成本强加给无关变更，后者违反模块和 identity 单调递减。
- **机器判定**：`classify-pmo-check-scope.mjs` 输出 `backend_mode` 和直接修改模块 selectors；partial 质量门禁使用 selectors 和 `requireFullReactor=false`，禁止 `-am`、`-amd`，仍由 base identity 只阻断新问题；根 POM、parent、架构门禁和基线变更 fail-closed 到 full。只有 full 报告才能运行 `check-architecture-debt-budget.mjs` 或 `--write`；任一规则、identity、模块归属增加或报告不完整都失败。

## 3. 验收判定

- `PASS`：服务启动、真实链路、业务断言、UI 断言、异常检查和证据全部满足。
- `FAIL`：功能错误、UI 明显异常，或 console/network 存在未解释错误。
- `BLOCKED`：服务、数据库、账号、权限或依赖环境不可用。
- `EXCEPTION`：必须有用户确认依据和风险说明。

## 4. 禁止事项

- 禁止服务未启动仍声明浏览器验收通过。
- 禁止把 404、空菜单、错误页或错误截图当作通过证据。
- 禁止未对比基线就断定问题不是本次引入。
- 禁止把 Mango 主框架、企业模板项目、业务仿真项目和历史生成项目混作同一个验证对象。
- 禁止在修复中重写既有框架壳层、菜单、页签、消息入口等公共能力。
- 禁止模板、starter、前端包或后端依赖变更后不判断是否需要升版发布。
- 禁止发布、验证、提交或清理只做一半就声明完成。
