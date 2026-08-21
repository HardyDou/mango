# Mango 发布制品与版本同步规范

## 0. 严格适用边界

- **正向要求**：本规范只适用于 Mango 主仓发布 Mango 平台自身的 npm、Maven、CLI、starter、模板、PMO、Skill、文档快照、Tag 和 GitHub Release 等不可变组件制品。仓库内 `mango-release` 是 Mango 主仓维护者能力，只存在于 Mango 主仓，不进入 `@mango/pmo` 插件、Business Starter baseline 或业务项目 `.agents/skills`。
- **禁止项**：禁止把本规范或 `mango-release` 用于 Mango 普通开发/缺陷/治理/Review/提交 PR，禁止用于业务项目 PR、业务应用构建、打包、部署、上线、流量、回滚或业务项目自己的制品发布。Mango 组件 PR 携带 Changeset 只登记未来发布意图，不表示该 PR 进入发布状态机。
- **机器判定**：`@mango/pmo` package、Codex plugin 投影、Business Starter baseline 和 CLI 生成/升级后的项目 Skill 均不得包含 `mango-release`；从旧 PMO bundle 升级时必须原子删除此前由该 bundle 托管的项目级 `mango-release`。正向测试只允许 Mango 主仓精确组件坐标触发，反向测试必须覆盖 Mango 普通 PR、业务 PR 和业务发布。

## 1. 适用范围与版本事实源

- **正向要求**：发布 npm、Maven、CLI、starter、模板、PMO、Skill 或文档快照时，必须由 `mango-catalog/catalog.lock.json` 和 release plan 列出本批次全部制品、当前版本、目标版本、依赖方、消费入口和唯一版本事实源；Catalog 只能由 package/module/docs 作者声明机械生成并通过 `pnpm -C mango-ui catalog:check`，发布人不得手工增删批次成员；`mango-ui/packages/mango-cli/release-versions.json` 只能引用已发布或在同一批次内先发布并完成回查的版本。
- **禁止项**：禁止用 `rules/backend/09-versioning.md` 代替制品版本治理；禁止把尚未发布的模板契约绑定到不支持该契约的旧 Maven/CLI/npm 版本；禁止同时维护互不校验的多个版本常量。
- **正例**：发布矩阵声明 Maven `1.0.16` 先于 `@mango/cli@1.0.68`，CLI 的 release lock、模板 POM、CHANGELOG 和升级说明均引用 `1.0.16`。
- **反例**：模板已写入 `global-entity-exceptions` 新字段，`release-versions.json` 仍锁定无法解析这些字段的 Maven `1.0.15`，却发布新 CLI。

## 2. 发布批次与依赖顺序

- **正向要求**：同一业务能力跨制品变化时必须形成一个 release batch，按“规范/契约与运行时实现 -> Maven/npm 基础制品 -> 聚合包 -> CLI/starter/模板 -> PMO 安装包与文档 -> 消费项目验证”的顺序发布；每一步记录输入版本、输出版本和前置回查证据。正式 Maven 批次必须执行 `scripts/publish-maven-batch.sh --all-non-app --release-version <version>`；该命令在同一批次内先发布非 app Reactor，再把当前提交的 `mango-docs/**` 打包并发布为同版本 `io.mango:mango-docs-bundle:<version>`。
- **禁止项**：禁止先发布依赖方再发布被依赖方；禁止单独升级 CLI、starter 或 PMO 投影而遗漏其运行时插件、规则、模板或版本锁；禁止把源码仓库内可用误当成已发布仓库可用；禁止把 `mango-docs-bundle` 留作批次外人工补发或在缺少该坐标时宣告 Maven 批次完成。
- **正例**：执行一次 `--all-non-app`，发布支持新 Java 架构契约的 Maven Reactor 和同版本文档包，再发布引用该版本的 CLI 和 starter。
- **反例**：只发布 Maven Reactor，事后再依赖维护者手工运行 `deploy:deploy-file` 补发文档包。错误原因：发布清单没有包含业务升级必需物料，步骤容易遗漏。

## 3. 规范、模板、Agent、Skill 与安装包一致性

- **正向要求**：`mango-pmo` 是规范、合同、模板、Agent、Skill 的唯一源；`@mango/pmo`、CLI full template 和 `mango-business-starter/business-pmo/mango-baseline` 必须由规范源机械构建或同步，并以清单哈希验证无缺失、无陈旧文件、无额外影子文件。
- **禁止项**：禁止在 starter、CLI template 或 npm `dist` 中手工修补规范正文；禁止只同步模板而遗漏对应 Agent、Skill、合同或 checker；禁止包版本不变但静默替换已发布内容。
- **正例**：`@mango/pmo` 构建生成 manifest，starter 通过 `sync-pmo-baseline.mjs --check` 验证当前 manifest 声明的全部受管文件，CLI 安装测试验证升级、陈旧清理、冲突拒绝和回滚。
- **反例**：源规则新增发布门禁，但业务 starter 仍保留旧 `rules/index.json`，导致业务 Agent preflight 无法召回新规则。

## 4. 发布前兼容与门禁矩阵

- **正向要求**：发布前必须执行受影响制品的单元/集成测试、包清单与 exports 校验、版本影响检查、规范投影检查、生成项目验收、Java 架构与静态分析门禁；模板或 CLI 变化必须分别验证当前源码 snapshot 和计划发布版本的兼容矩阵。
- **禁止项**：禁止只验证编译；禁止只在 Mango monorepo 内验证而不测试安装后的 npm/Maven 制品；禁止用 baseline、skip、report-only 或旧版插件绕过新规则；禁止把“旧发布版本预期不兼容”记录为通过。
- **正例**：CI 先安装当前 Maven reactor，再以 `MANGO_BACKEND_GATE_VERSION=<待发布版本>` 生成四层业务模块，正向 `clean verify`，并验证 PathVariable、直接 ServiceImpl、缺报告和门禁参数绕过均被阻断。
- **反例**：生成模板测试默认使用旧发布版本并失败后，简单改成跳过 architecture goal，而不建立 Maven 与 CLI 的发布先后约束。

## 5. 发布、仓库回查与消费验证

- **正向要求**：发布必须使用仓库规范指定的 batch 入口；Maven 批次只调用 `publish-maven-batch.sh`，不再维护独立文档包发布命令；发布后从目标 Maven/npm 仓库重新解析精确版本和关键文件，使用干净临时目录验证 CLI/Skill 安装、项目生成、模块生成、构建和业务消费入口；所有证据绑定 registry、坐标、版本、校验和与时间。
- **禁止项**：禁止以本地仓库、workspace link、缓存 tarball 或未清理的生成目录代替发布后回查；禁止只看到 HTTP 200 或版本号存在就判定内容正确；禁止发布 app 部署制品进入默认平台 Maven 批次。
- **正例**：Nexus 回查 npm tarball 的 manifest/Skill/README，Maven 使用临时 local repository 拉取目标插件，随后生成项目完成全量门禁。
- **反例**：`npm publish` 成功后未安装 tarball、未验证 `dist/baseline`，也未确认 Maven 插件版本，却声明整个 Mango 批次完成。

## 6. 关闭条件与机器门禁

- **正向要求**：只有版本矩阵全部一致、前置制品均已发布并回查、消费验证通过、CHANGELOG/升级说明/GitHub Release 指向同一批次、规范投影无漂移时才能 `NEXT`；发布报告必须列出精确命令和每个制品的仓库证据。
- **禁止项**：禁止缺任一制品版本、失败命令、消费验证或人工授权时进入 `NEXT`；禁止通过修改 baseline、release lock 或证据文件隐藏版本漂移；禁止在用户未授权时实际发布。
- **正例**：`release:impact`、包检查、生成后端门禁、仓库回查和业务升级验证全部通过，报告给出 Maven/npm/CLI/PMO 同批次版本和 GitHub Release。
- **反例**：只更新 CHANGELOG 和版本号，未发布 Maven 前置插件、未回查 npm 包、未验证业务项目，仍标记发布完成。
- **机器判定**：至少执行 `pnpm -C mango-ui release:impact --base=<base> --head=<head>`、`node mango-business-starter/scripts/sync-pmo-baseline.mjs --check`、`node mango-ui/packages/mango-cli/scripts/check-release-versions.mjs`、`node mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs` 及本批次发布工具规定的 repository back-check；任何非零退出均为 `STOP`。

## 7. 统一发布状态机

- **正向要求**：Mango 发布只使用仓库内 `mango-pmo/skills/mango-release`。唯一入口是 `mango release plan -> prepare -> publish/status/repair` 和 `mango release registry doctor`；状态固定为 `VALIDATED`、`PREPARED`、`READY`、`PUBLISHING`、`PARTIAL`、`AMBIGUOUS`、`REPAIR`、`COMPLETED`。plan 绑定 Catalog、源码 commit/tree、baseline、Changesets、release notes、版本策略、完整 tuple、closure 和顺序；prepare 只构建一次并以 `preparedCandidateId` 封存 npm、Maven、docs 和 source archive 字节。
- **禁止项**：禁止恢复单包发布脚本或其它 fallback；禁止人工维护发布包清单；禁止以最近一次提交代替累计 Changesets；禁止 READY 后重新构建、补文件或换 candidate；禁止 hosted 已存在时重发；禁止消费验证前创建 Tag/GitHub Release；禁止持久化授权、凭据或自动回收 stale lock。
- **正例**：请求已登记但结果未知时进入 `AMBIGUOUS`；`status` 回读双仓并报告 journal 差异，`repair` 只接受同一 candidate，远端字节一致时推进验证，未发请求的 absent 坐标才允许继续发布。
- **反例**：发布失败后重新运行构建和逐包脚本，或发现旧坐标后把它认领为当前 candidate。错误原因：候选与发布物不再同源，且不可变坐标可能被覆盖或错误归属。
- **机器判定**：功能 PR 的 Git 影响与 Changeset 声明必须一致；Release PR 的计划由机器重算，只有版本、依赖、CHANGELOG、Changeset 消费和计划投影时才走轻量检查，混入源码自动回到普通门禁。`publish/repair` 只接受当前回合 `--authorize` 或 `MANGO_RELEASE_AUTHORIZED=1`；任一写入前必须完成全部 npm/Maven/docs 坐标双侧预检，任一 unknown、已有未归属坐标、sealed digest 或 READY identity 不一致都保持零新增远端写入。

### 7.1 Registry 抽象与文档策略

- **正向要求**：分别配置 Maven publish、Maven consume、npm publish、npm consume 四个角色；Maven 认证只引用 `settings.xml` server ID，npm 只引用 token 环境变量名或 npm config。正式 Maven 发布自动要求 `docs-snapshot`，npm-only 默认只更新并验证 Latest。
- **禁止项**：禁止在发布实现中硬编码 Nexus、Artifactory、GitHub Packages 或其它地址；禁止用本地缓存代替 consume registry；禁止 npm-only 版本占用 Maven 文档版本列表。
- **正例**：同一 adapter 可通过环境变量切换 Nexus 和 Artifactory，manifest 记录 registry URL、坐标和 checksum，但不记录凭据值。
- **反例**：脚本内置内网 Nexus 为默认地址。错误原因：环境不可移植，且未配置时可能误发到错误仓库。

### 7.2 发布说明与收尾

- **正向要求**：不可变动作前，平台 CHANGELOG、制品 changelog 和 GitHub Release 预稿必须从上次成功发布基线覆盖到候选的完整实际发布 PR；每个 PR 按 `Fixed`、`Added` 或 `Changed` 分类并映射精确发布包和业务适配。被取代、恢复或仅供审计的 PR 必须单独标记，不得冒充本批次新增能力。
- **正向要求**：发布正文必须包含非空的 `Pull Requests`、至少一个 `Fixed/Added/Changed`、`Versions`、`Published Packages`、`Business Impact`、`Upgrade Estimate`、`Upgrade Notes`、`Verification` 和 `Rollback`。`Upgrade Estimate` 必须分别说明升级对象、工程工作量、执行窗口、服务停机、回退工作量和估算前提；估算必须区分适用消费形态，不能用一个无前提数字代替。
- **机器判定**：`mango release prepare` 和 GitHub Release 创建前使用同一个 release-notes checker 检查章节存在且非空、PR 编号、PR 到分类/制品/业务适配的映射、估价字段和未替换占位符；`.changeset/release-notes-template.md` 是发布人填写结构，机器计划和 prepare 不得自动编造业务影响或估价。结构化验证应解析 YAML/JSON/manifest 语义，禁止把引号、缩进等非契约格式写成发布成败条件。
- **正向要求**：发布完成后必须通过 PR 把平台 CHANGELOG 的 `PENDING` 回填为真实发布状态和完整 manifest 证据，再停止服务、释放任务 workspace/数据库、清理已合并 worktree 与分支、同步 `main` 并证明 `HEAD == origin/main`。发布 tag 保持指向制品源码提交，不移动到仅含收尾文档的提交。
- **禁止项**：禁止缺 PR 清单、制品映射、业务影响、估价、升级、验证、回退或存在占位符时进入 npm/Maven publish；禁止制品已发布后因验证脚本格式误判而重发；禁止保留 `PENDING`、未清理发布 worktree 或未同步 main 却声明整批收尾完成。
