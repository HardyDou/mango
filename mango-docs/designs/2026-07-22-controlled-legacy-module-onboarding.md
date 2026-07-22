# 存量业务模块受控首次纳管设计

## 1. 决定

采用两个独立 PR 完成存量模块首次纳管。第一个 PR 只能新增目标 starter 的 `META-INF/mango/module.properties` 并登记该模块由完整 Reactor 报告证明的历史架构 identity；第二个 PR 才允许开发业务功能。首次纳管不修改 `changed`、`CTRL-008` 或 required check 的阻断语义。

需求影响为 L3：能力决定业务项目能否在不放宽核心架构门禁的情况下接纳存量模块。方案风险为 L3：实现直接修改历史债务基线的准入条件，错误会把新增违规登记为历史债务。最终风险为 L3，使用 FULL 治理记录，不伪造产品 BRD/SRS/TDD 链。

## 2. 信任边界

- Git PR base SHA 是存量事实来源；它必须是当前 HEAD 的祖先。
- 完整 Reactor 架构报告是债务事实来源；部分 Reactor 报告不能写入或验证首次纳管。
- 纳管 PR 的 Git diff 只能包含目标 `module.properties` 与债务预算文件。
- `module.properties` 在 base 中必须不存在、在当前树中必须是已跟踪普通文件。
- 当前完整报告相对 base 预算只能在目标模块增加 identity；其它模块、目标模块原有 identity 和 Maven 坐标必须保持不变。
- required check 必须使用可信 CI 现场生成的完整报告复验已提交预算；仅执行 `--baseline-only` 不能批准新的首次纳管记录。
- 业务项目使用 baseline 外的 `business-pmo/architecture-debt-budget.json` 保存项目自有预算；CLI 同时生成 GitHub/Gitea full-Reactor governance 门禁，PMO 同步不能覆盖项目预算。

## 3. 数据与命令

`debt-budget.json` 保持 schema v4，增加可选 `moduleOnboardings` 映射。每条不可变记录包含目标模块、base commit 与预算摘要、模块身份文件路径与摘要、模块身份值、纳管后模块 inventory 摘要、原因和时间。Mango Maven 插件继续读取全局 `identities`，因此保持向后兼容。

先在精确 PR 工作树中构建完整 Reactor，再用插件的受限 inventory 模式生成报告。`inventoryOnly` 只能与 `requireFullReactor=true` 同时使用；它只负责产出全部问题清单，最终准入仍由债务预算检查器决定：

```bash
mvn -f mango/pom.xml \
  -DskipTests \
  -Dmango.architecture.mode=changed \
  -Dmango.architecture.requireFullReactor=true \
  -Dmango.architecture.inventoryOnly=true \
  -Dmango.architecture.base=<pull-request-base-sha> \
  verify
```

必须通过绑定架构插件及其审批配置的 `verify` 生命周期生成报告，不能从根 POM 直接调用裸插件 goal；后者可能丢失验证聚合模块中的 canonical 配置。

写入入口：

```bash
node mango-pmo/tools/check-architecture-debt-budget.mjs \
  --onboard-module <moduleKey-or-artifactId> \
  --module-properties <path> \
  --base-ref <pull-request-base-sha> \
  --reason "<reviewed reason>" \
  --write
```

可信 CI 使用同一完整报告执行普通 `--base-ref` 检查；检查器自动识别 base 与当前预算间新增的纳管记录并复验。后续 PR 继续使用既有 changed/no-new-violations 和模块预算递减入口。

新生成业务项目携带一个 schema v4 空预算。首次完整报告负责提供 Reactor 模块目录；纳管只允许目标模块 identity 增加，任何其它模块增加仍会失败。存量业务项目先独立升级 PMO bundle 和 GitHub/Gitea workflow，再用一个只新增项目预算文件的治理 PR 从完整报告初始化真实存量预算；初始化 CI 禁止 baseline-only 和其它文件变更。迁移期预算缺失只对“至少修改一个托管 workflow 且 diff 完全属于 PMO 治理资产”的升级放行，业务源码、POM、配置均不在白名单。CLI 只升级带托管标识或 hash 命中历史标准版本的 workflow；未知定制默认失败，显式 `--adopt-governance` 才接管并备份。项目预算始终是业务仓资产，sync、upgrade、rollback 均不覆盖。预算合并后才开始身份纳管。

## 4. 失败与恢复

- 业务代码、POM、配置或第二个模块混入纳管 PR时直接失败。
- 报告不完整、base 不可解析、模块身份已存在、identity 被替换或跨模块移动时直接失败。
- 纳管记录被删除、改写、重复创建或与预算/模块身份摘要不一致时直接失败。
- 写入采用同目录临时文件原子替换；校验失败不修改预算。
- 纳管 PR 未合并时整体丢弃；已合并后只能通过独立 revert 同时撤销身份与预算，不能在业务 PR 中静默删除记录。

## 5. 验收矩阵

| ID             | 层级     | 场景                                           | 完成标准                                                                                                   |
| -------------- | -------- | ---------------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| ONBOARD-TC-001 | 单元     | 合法首次纳管                                   | 只给目标模块增加完整报告中的历史 identity，并生成完整审计记录                                              |
| ONBOARD-TC-002 | 策略     | 夹带业务源码、POM 或其它配置                   | 写入与 CI 复验都失败，预算保持不变                                                                         |
| ONBOARD-TC-003 | 策略     | 伪造/部分报告、错误 base、已存在身份、重复纳管 | 全部 fail closed                                                                                           |
| ONBOARD-TC-004 | 策略     | 替换 identity、跨模块移动或抬高其它模块预算    | 全部 fail closed                                                                                           |
| ONBOARD-TC-005 | 集成     | 真实 Git base -> 纳管 PR -> 业务 PR            | 纳管 PR 由完整报告通过；后续历史 identity 不阻断，新 identity 继续阻断                                     |
| ONBOARD-TC-006 | 回归     | 历史债务减少                                   | 既有模块 `--write` 只允许递减并保留不可变纳管记录                                                          |
| ONBOARD-TC-007 | 模板集成 | CLI 生成业务项目                               | 生成项目包含项目预算，GitHub/Gitea governance 现场构建完整 inventory 并复验；partial PR 只做 baseline-only |
| ONBOARD-TC-008 | 策略     | 老项目首次初始化预算                           | 只允许预算文件 Git diff 和可信完整报告；baseline-only、纳管记录或业务源码全部失败                          |

## 6. 不采用的方案

- 不在含业务改动的同一 PR 使用全局 `--accept-increase`，避免把新增违规洗成历史债务。
- 不跳过 `CTRL-008`、required check 或 module identity。
- 不由插件临时编译 base 源码；两 PR 边界提供更简单、可审计的事实隔离。
