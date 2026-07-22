# Issue #512 存量模块受控首次纳管实施记录

## 1. 目标与范围

实现 [受控首次纳管设计](../designs/2026-07-22-controlled-legacy-module-onboarding.md)，影响 PMO 架构债务检查器、策略测试、核心治理规则、业务 PMO baseline 投影、使用说明和能力地图。不修改 Java 架构规则、`CTRL-008`、Maven 插件的正常阻断逻辑，不发布制品。

## 2. 风险与保障

- Requirement impact：L3。
- Solution risk：L3。
- Final risk：L3。
- Delivery mode：FULL governance。
- Workspace decision：M01=CREATE，`/Users/hardy/Work/mango-issue-512`。
- M09：启用，验证脚本语法、PMO 规则、投影一致性和工作区布局。
- M10：启用，覆盖参数、预算、Git diff、身份与对抗性策略。
- M11：启用，在临时真实 Git 仓库执行两 PR 生命周期和预算递减链路。
- M12/M13：停用；能力没有 HTTP 或浏览器入口。
- M14：启用；从安全边界、Git/CI 可证明性和向后兼容三个视角复核。
- M15/M16：停用；本任务不修改远端保护配置，也没有自动化之外的业务现场结果。

## 3. 实施项

- [x] 扩展 schema v4 可选纳管审计记录，并在所有预算写回路径中保留记录。
- [x] 增加 `--onboard-module`、`--module-properties` 的两阶段写入入口。
- [x] 校验 base 祖先、身份首次新增、精确 Git diff、完整报告和单模块增量。
- [x] 普通 `--base-ref` 完整报告检查自动复验新纳管记录；baseline-only 对新记录 fail closed。
- [x] 保证记录不可修改、不可删除、不可重复，后续预算只能递减。
- [x] 为旧业务仓首次预算增加独立预算-only PR、可信完整报告和 baseline-only 拒绝门禁。
- [x] 为生成业务项目增加项目自有空预算及 GitHub/Gitea governance 完整 inventory 复验。
- [x] 为存量业务仓增加 CLI workflow 托管、历史标准升级、自定义冲突拒绝/显式接管、幂等同步、字节级回滚和预算不覆盖。
- [x] 为预算缺失迁移增加纯 PMO/workflow 白名单，只接受 inventory-only 完整报告且禁止业务源码、POM 与配置。
- [x] 更新规范、README、能力地图、CLI/starter 模板和业务 baseline 机械投影。
- [x] 完成 ONBOARD-TC-001 至 ONBOARD-TC-008 并记录真实命令与结果。

## 4. 完成标准

所有自动化与真实 Git 生命周期测试通过；M14 无阻断问题；PMO package、business baseline 投影和治理检查通过；工作区只包含已说明变更。未满足任一条件不得声明 Issue 已修复。

## 5. 验证结果

### 5.1 策略、PMO 与投影

- `node --test mango-pmo/tests/architecture-debt-budget.test.mjs`：36/36 通过。覆盖合法纳管、空项目预算、首次预算初始化、纯治理升级迁移、非祖先 base、非 inventory 报告、夹带源码、跨模块增加、审计篡改、重复纳管、后续新增与递减。
- PMO in-scope Node 测试（排除依赖历史已发布 Maven 制品的 release-only dry-run）：136/136 通过（Node 22.23.1）。
- `node mango-pmo/tests/skills/check-skill-evals.mjs`：16 Skills、138 用例通过。
- delivery mode 历史 32 例、delivery assurance 100 例通过。
- `node mango-pmo/tools/check-governance-intent.mjs`、`workspace-layout-check.mjs --root .`、`git diff --check`：通过。
- `node mango-business-starter/scripts/sync-pmo-baseline.mjs`：`@mango/pmo@1.3.4` 的 139 个受管文件无漂移。
- PMO package：bundle `929bf16d2bb5` 构建和校验通过。
- starter template：75 个必需文件、34 个合同检查通过；业务模块后端与前端投影通过。

### 5.2 Java 与真实 Reactor

- 定向 `ArchitectureMojoTest` 通过；`inventoryOnly` 与 partial Reactor 组合按 `MANGO-ARCH-ENGINE-028` 失败。
- `mango-architecture-rules` 175 个测试、`mango-maven-plugin` 225 个测试通过。
- 完整 212/212 Reactor `package` 成功，耗时约 1 分 37 秒。
- 合并最新 `origin/main`（`d28f2bdd2`）后，按 required check 参数重新执行完整 212/212 governance Reactor `verify` 成功，耗时 7 分 34 秒。架构报告 dependency/archunit/pmd/blocking 均为 0；静态质量门禁对 15,461 条历史基线判定 0 新增、0 工具失败。
- 完整报告后的 `node mango-pmo/tools/check-architecture-debt-budget.mjs --base-ref HEAD`：`current=0`，PASS。
- 真实验证使用 Maven `verify` 生命周期加载 `mango-architecture-verification` 的 canonical 配置；根 POM 裸插件 goal 会漏掉已审批反向 Controller 配置，因此未作为交付命令。

### 5.3 业务项目真实消费链路

- `node mango-ui/packages/mango-cli/scripts/check-cli.mjs`：真实生成 full/custom/add/module 项目及 PMO sync/upgrade/rollback 合同全部通过；覆盖 workflow 缺失、历史标准 hash、自定义拒绝/显式接管、幂等、失败前零写入、历史 workflow 字节级回滚和非空预算保持。
- CLI Node 合同测试：35/35 通过；生成后端 Maven 调用预算保持为最多 9 次。
- `MANGO_BACKEND_GATE_VERSION=1.0.0-SNAPSHOT node mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs`：在临时真实生成项目执行 9 次 Maven，验证干净项目、正反架构规则、完整 Reactor inventory-only 报告、无预算旧仓的纯 workflow 升级、项目预算恢复检查和 partial 模式，全部通过。
- GitHub、Gitea 与主仓三份 workflow 经 YAML 解析通过；业务预算和 checker 改动均被 scope classifier 判为 governance，partial PR 保留 baseline-only。
- `pnpm admin:styles:check`、`pnpm admin:module-styles:check`：通过；无前端样式变化。

## 6. M14 三视角复核

### 安全与信任边界

无阻断问题。纳管绑定精确且为 HEAD 祖先的 base commit、可信完整 Reactor、`inventoryOnly=true`、普通非符号链接身份文件、唯一 module-name/module-path、目标 starter 所有权和精确两文件 diff。首次项目预算另行限制为预算-only diff，不能携带纳管记录或业务代码。既有纳管记录不可删改，identity 只能递减。

### Git 与 CI 可证明性

无阻断问题。主仓及 CLI 生成的 GitHub/Gitea workflow 都在 governance 模式现场运行完整 Maven `verify` 并以同一 base SHA 复验预算；partial 模式不能批准首次预算或首次纳管。策略测试使用临时真实 Git 仓库执行 base -> 纳管 -> 后续业务 PR -> 债务递减链路。

### schema 与 Maven 兼容性

无阻断问题。预算继续使用 schema v4，`moduleOnboardings` 为可选字段，旧 v4 可读；Maven 报告 schema 仍为 v2，`inventoryOnly` 为新增布尔字段，检查器兼容字段缺失的普通历史报告。正常架构失败语义不变，只有显式完整 inventory 模式跳过插件自身准入，最终仍由预算检查器决定。

## 7. 未执行与发布边界

- 没有 HTTP、浏览器、数据库或外部业务运行入口，因此 M12/M13/M16 不适用。
- 本任务不发布 Maven/npm 制品，不修改远端分支保护；Commit、Push、PR 和合并按用户当前授权及 required checks 状态执行。
- `publish-maven-batch.test.mjs` 的 3 个 release-only dry-run 在当前机器失败：脚本 dry-run 仍解析历史 `io.mango:mango-bom:1.0.17/1.0.18/1.0.20`，Nexus 返回制品不存在且本机缓存该缺失。该组不覆盖 Issue #512，未通过伪造本地历史制品绕过；其余 PMO 测试 136/136 通过。
- 投产时必须把包含 `inventoryOnly` 的 Mango Maven 插件、PMO bundle、CLI/starter 模板作为兼容批次发布；旧业务仓按“PMO/workflow 升级 PR -> 预算初始化 PR -> 身份纳管 PR -> 业务 PR”迁移。CLI 已托管 GitHub/Gitea workflow：缺失即安装、历史标准 hash 或托管标识可升级、未知定制默认零写入失败并仅在显式 `--adopt-governance` 后接管；dry-run、幂等 sync、workflow rollback 和项目预算不覆盖均纳入真实生成项目回归。当前实现和本地真实链路已通过，消费者尚未获得未发布版本。
