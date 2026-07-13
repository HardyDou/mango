# Mango 发布制品与版本同步规范

## 1. 适用范围与版本事实源

- **正向要求**：发布 npm、Maven、CLI、starter、模板、PMO、Skill 或文档快照时，必须先列出本批次全部制品、当前版本、目标版本、依赖方、消费入口和唯一版本事实源；`mango-ui/packages/mango-cli/release-versions.json` 只能引用已发布或在同一批次内先发布并完成回查的版本。
- **禁止项**：禁止用 `rules/backend/09-versioning.md` 代替制品版本治理；禁止把尚未发布的模板契约绑定到不支持该契约的旧 Maven/CLI/npm 版本；禁止同时维护互不校验的多个版本常量。
- **正例**：发布矩阵声明 Maven `1.0.16` 先于 `@mango/cli@1.0.68`，CLI 的 release lock、模板 POM、CHANGELOG 和升级说明均引用 `1.0.16`。
- **反例**：模板已写入 `global-entity-exceptions` 新字段，`release-versions.json` 仍锁定无法解析这些字段的 Maven `1.0.15`，却发布新 CLI。

## 2. 发布批次与依赖顺序

- **正向要求**：同一业务能力跨制品变化时必须形成一个 release batch，按“规范/契约与运行时实现 -> Maven/npm 基础制品 -> 聚合包 -> CLI/starter/模板 -> PMO 安装包与文档 -> 消费项目验证”的顺序发布；每一步记录输入版本、输出版本和前置回查证据。
- **禁止项**：禁止先发布依赖方再发布被依赖方；禁止单独升级 CLI、starter 或 PMO 投影而遗漏其运行时插件、规则、模板或版本锁；禁止把源码仓库内可用误当成已发布仓库可用。
- **正例**：先发布支持新 Java 架构契约的 Maven 插件并从 Nexus 拉取验证，再发布引用该版本的 CLI 和 starter，最后在空目录生成业务项目执行 `clean verify`。
- **反例**：本机 snapshot 测试通过后直接发布 CLI，但业务开发者安装 CLI 后只能解析到旧 Maven 插件，生成项目首次 `verify` 即失败。

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

- **正向要求**：发布必须使用仓库规范指定的 batch 入口；发布后从目标 Maven/npm 仓库重新解析精确版本和关键文件，使用干净临时目录验证 CLI/Skill 安装、项目生成、模块生成、构建和业务消费入口；所有证据绑定 registry、坐标、版本、校验和与时间。
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

- **正向要求**：正式批次统一使用 `mango release publish/status/verify/repair --version <version>` 和 `mango release registry doctor`；状态固定为 `source`、`versions`、`changelog`、`readmes`、`tests`、`pr`、`tag`、`github-release`、`maven`、`npm`、`cli-lock`、`private-registry-publish`、`private-registry-consume-verify`、`docs-latest`、`docs-snapshot`、`post-verify`、`cleanup`。每项只能是 `passed/failed/pending/not_applicable`，必须记录非空原因；适用且已执行的状态必须逐次记录命令、工作目录、退出码、开始/完成时间和非空脱敏输出。`not_applicable` 因未执行命令，只记录明确原因和判定时间。
- **禁止项**：禁止绕过状态机直接声明整批发布完成；禁止把未执行状态写成 passed；禁止 `repair` 重发已经成功的不可变 Maven/npm/tag/Release/文档快照；禁止在项目或用户配置中持久化发布授权、token、password 或 URL userinfo。
- **正例**：npm 发布成功但消费仓库回查失败，manifest 保留 npm passed 和 consume-verify failed；修复缓存后 `repair` 只运行 consume verify 和后续状态，不重新执行 npm publish。
- **反例**：第二次执行整套脚本试图覆盖发布。错误原因：不可变版本可能已经存在，且无法区分发布失败与回查失败。
- **机器判定**：`publish/repair` 只接受本次 `--authorize` 或 `MANGO_RELEASE_AUTHORIZED=1`；配置优先级为 CLI > 环境变量 > 用户配置 > 项目配置；Maven/npm 分别显式选择 `private-registry/public-registry/artifact-only/disabled`，disabled 必须有原因，缺模式或 registry 时 doctor 和 publish 失败。completed 必须同时满足所有状态关闭、状态 applicability 与配置一致和全部适用状态 evidence 结构完整；必需状态禁止篡改为 `not_applicable`。不可变 repair 只接受精确的 `{kind: verify-existing}` 引用同状态 verify adapter，独立命令、空数组或额外字段均失败。

### 7.1 Registry 抽象与文档策略

- **正向要求**：分别配置 Maven publish、Maven consume、npm publish、npm consume 四个角色；Maven 认证只引用 `settings.xml` server ID，npm 只引用 token 环境变量名或 npm config。正式 Maven 发布自动要求 `docs-snapshot`，npm-only 默认只更新并验证 Latest。
- **禁止项**：禁止在发布实现中硬编码 Nexus、Artifactory、GitHub Packages 或其它地址；禁止用本地缓存代替 consume registry；禁止 npm-only 版本占用 Maven 文档版本列表。
- **正例**：同一 adapter 可通过环境变量切换 Nexus 和 Artifactory，manifest 记录 registry URL、坐标和 checksum，但不记录凭据值。
- **反例**：脚本内置内网 Nexus 为默认地址。错误原因：环境不可移植，且未配置时可能误发到错误仓库。
