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
10. 新增、修改、删除或重构对外能力时，必须按任务 `L0-L3` 同步更新或说明不更新代码、README/使用说明、需求/设计文档、测试脚本和测试结果基线。
11. 有 UI 影响时，UI/E2E 脚本必须覆盖本次交付影响的用户可见入口、关键业务流程、权限/租户/数据权限边界、关键异常或回归场景；无 UI 的 `L3` 任务必须执行 API/应用入口级流程测试。只打开页面、接口 200 或无明显报错不能作为完整断言。
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
- 根据改动类型选择至少 3 个相关专家视角评审，例如 PMO、架构、后端、前端、QA、安全、支付、发布或文档治理。
- 专家评审必须输出阻断问题、非阻断建议和结论。
- 存在阻断问题时不得合并；必须把问题登记到 PR 评论或评审结论中。
- 无阻断问题时，合并前必须记录评审视角、验证命令、未验证项和风险。
- 对外能力变更的 PR 门禁必须检查交付台账中适用的 UI/E2E 或入口级流程脚本和测试结果基线是否填写可复核路径，或有明确 `EXCEPTION` 依据。

前端官方模块、admin 聚合入口、CLI 模块清单或样式发布物料发生变化时，PR 门禁必须包含：

- `pnpm admin:styles:check`
- `pnpm admin:module-styles:check`

任一检查失败时不得提交 PR 或合并。

## 2.4 Required Check 与 CODEOWNERS 门禁

PMO 治理使用以下稳定检查身份：

| 项目 | 固定值 |
|---|---|
| Workflow 文件 | `.github/workflows/pmo-doc-check.yml` |
| Workflow 名称 | `PMO Documentation Checks` |
| Job ID / 默认 check-run 名称 | `pmo-doc-check` |
| PR 页面显示 | `PMO Documentation Checks / pmo-doc-check` |

必须执行：

- `main` 的 branch protection 或 ruleset 必须要求 `pmo-doc-check` 成功，并启用 Code Owner approval；CODEOWNERS 文件存在不等于审批已经启用。
- 首次配置或修改保护规则时，必须从真实 PR 的 check-run 或 GitHub API 读取 context，禁止仅凭文档字符串猜测。
- 修改 workflow `name`、job ID、job `name` 或触发事件时，必须在同一变更中同步 branch protection/ruleset 和本节；旧 required check 不得因改名永久 pending。
- 业务项目的 `pmo-doc-check` 必须对每个 PR 运行，不得配置会跳过整个 workflow 的 `paths`/`paths-ignore`；文档或后端没有变化时可以在 job 内执行可审计的轻量判定，但稳定 check-run 必须产生结果。
- 业务项目的同一 `pmo-doc-check` 必须执行 `check-document-set.mjs --root business-docs` 和完整后端 `mvn verify`；四类生命周期文档遗漏 `documentType`、使用未知类型、重复 ID、上游断链或摘要失效时必须失败。
- `mango-pmo` 规则、合同、Agent、Skill、模板、Java 架构 checker、PMO workflow、业务模板和 PMO/CLI 发布脚本必须由 `.github/CODEOWNERS` 覆盖。
- required check 成功只证明机器门禁通过，不能替代业务、架构、QA、发布或 Code Owner 的人工结论。
- 声明“分支保护已生效”前，必须提供远端 ruleset/branch protection 或受保护 PR 的验证证据。

禁止：

- 用 workflow 绿灯替代 Code Owner review。
- 在未验证远端配置时声明 required check 已启用。
- 为绕过失败而删除 required check、缩小 workflow 触发范围或把关键步骤改成非阻断。
- 只在人工命令中列出单文档 checker，却不让 CI 自动扫描 `business-docs`。

## 2.5 历史架构债务与递减预算

- **正向要求**：PR 显式使用 `changed`/`no-new-violations` 只阻断本次可归因的新违规；同一次完整 Reactor 检查仍统计全部历史问题，报告必须声明 `inventoryScope=full-reactor`、`issueInventory=all-detected-issues` 且实际/预期 Reactor 项目数相等。CI 使用 `base budget -> PR budget -> current full report` 三方比较，通过 `mango-pmo/baselines/architecture/debt-budget.json` 同时锁定规则计数和稳定问题身份多重集合。PMD 身份使用相对文件、规则、消息和违规源码行内容摘要，不使用易漂移的绝对路径或行号。历史问题被修复后必须运行 `check-architecture-debt-budget.mjs --write` 下调预算，后续不得回升。
- **禁止项**：禁止让未改动文件的存量问题阻断普通新需求；禁止用历史预算放行本次新增违规；禁止在规则之间转移违规维持总数；禁止在同一 PR 手工抬高预算掩盖新增违规；禁止复用旧审批原因或把不完整 Reactor 报告当作全量清单。规则升级首次暴露存量债务时，只有 PMO 治理任务获得明确批准、记录非空原因、把该原因绑定到 base 预算 SHA-256、完成存量盘点并通过 Code Owner 审核，才允许 `--accept-increase`。
- **正例**：PR 修改订单 Service，门禁只因该 Service 新增的 `BEAN-004` 失败；同时完整债务从 8744 降至 8738，提交者下调按规则预算，下一次上限固定为 8738。
- **反例**：历史有 8744 条，所以本次新增 3 条也算 baseline。错误原因：baseline 只隔离旧事实，不授权新增违规。
- **机器判定**：Maven `changed` 门禁先用 committed identity 多重集合扣除历史问题，只阻断剩余新身份；CI 随后执行 `node mango-pmo/tools/check-architecture-debt-budget.mjs --base-ref "$BASE_SHA"`。当前报告必须与 PR 预算逐规则、逐身份精确相等；PR 预算相对 base 的任一规则或身份增加必须包含新原因和精确 base 预算摘要；已有下降未写回递减预算时失败。“同规则修一条又新增一条”会因 identity 一减一增而失败。

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
