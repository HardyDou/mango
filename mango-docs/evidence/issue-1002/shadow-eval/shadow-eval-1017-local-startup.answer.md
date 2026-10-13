已经掌握足够事实。以下为只读实现方案，未修改任何文件。

## 事实基线（来自本次勘察）

| 事实 | 证据 |
|---|---|
| 后端入口强制要求首个参数为 `bootstrap`/`runtime` | `mango/mango-app/monolith/mango-monolith-app/src/main/java/io/mango/app/monolith/MangoMonolithApplication.java:15` → `mango/mango-infra/mango-infra-bootstrap/mango-infra-bootstrap-starter/src/main/java/io/mango/infra/bootstrap/starter/MangoApplication.java:19-41`（缺失即 `IllegalArgumentException: Mango process mode is required`） |
| IDEA 直接 Run/Debug 只传 `args=[]`，因此必然失败 | 同上；`MangoApplicationTest.rejectsMissingOrUnsupportedLifecycleCommandsBeforeStartingSpring` 已固化该契约 |
| `mango dev start` 的编排全在 CLI，不在 Java | `mango-ui/packages/mango-cli/src/index.mjs:2093`（`startDevWorkspace`）、`:2125`（env 初始化/同步）、`:2192`（app 启动）、`:2909`（回执注入）、`:2980`（Spring Boot 命令构造） |
| Runtime 启动依赖 CLI 注入的 5 个参数 | `mango-ui/packages/mango-cli/src/dev-bootstrap-receipt.mjs:96`（`injectStableBootstrapIdentity`） |
| 工作区事实源是 `.mango/workspace.json` + `.mango/dev-workspace.env`，且禁止提交 | `mango-pmo/rules/02-dev-environment.md` 第 2、3 节 |
| 现有约定禁止绕过 CLI 手写启动 | `mango-pmo/rules/02-dev-environment.md` 第 7 节 |
| 仓库当前没有任何 IDEA 运行配置或模板 | 全仓无 `.run/`、`*.run.xml`、`*.iml`，`mango-pmo`、`mango-docs` 中零命中 IDEA/IntelliJ |

需求本质：**把 CLI 已经算好的"启动意图"落到 IDE 可复用的运行配置上**，不是重写启动逻辑。

---

## 1. 复用判断（REDLINE-REUSE-010）

按顺序逐层判断：

**(1) Mango 已有能力 —— 全部命中，本任务的能力已在位**

- 生命周期选择与回执门禁：`MangoApplication`（bootstrap/runtime + `mango.release.*` + environment-key）
- 环境与回执编排：`@mango/cli` 的 `startDevWorkspace` / `ensureDevWorkspaceEnv` / `applyStableBootstrapReceipt` / `resolveDevCommand`
- 回执读写与校验：`dev-bootstrap-receipt.mjs`
- 工作区端口、库名、qualifier 分配：`ensureWorkspaceConfig`、`buildWorkspaceConfig`、`~/.mango/workspaces.json` 注册表
- 声明式清单：`mango.dev.json`（`apps.mango-backend.goal`、`processMode: runtime`、`args`、`portEnv`、`health`）
- 状态观测：`mango dev status` / `logs` / `doctor`

**(2) 当前模块已有实现**：`MangoMonolithApplication` 本身无需改动。它的 `main` 已经是正确的委派，问题不在它。

**(3) 标准库 / (4) 已装依赖**：IDEA 的运行配置是 IDE 侧纯文本资产（`.run/*.run.xml` 或 `.idea/runConfigurations/*.xml`），无需任何新依赖。

**(5) 新增结构 —— 结论：可以做到零 Java 结构新增**

需要新增的只是**一份受版本控制的 IDE 运行配置资产**，加一个已存在的输出通道。判定如下：

| 候选新增物 | 是否必要 | 理由 |
|---|---|---|
| IDEA `Spring Boot` 运行配置（Main class + Program arguments + EnvFile） | **需要** | 这是本需求的唯一交付物，且 IDEA 侧无任何现成等价物 |
| Java 侧"无参默认 runtime"兜底 | **不需要** | 会直接破坏 `MangoApplicationTest` 已固化的契约（缺失 mode 必须失败），并让 `SpringApplication.run` 兼容直启与 Mango 生命周期入口语义混淆，属红线级的语义回退 |
| 新的 CLI 子命令（如 `mango dev config idea`） | **不需要** | 见第 5 条，当前无第二个消费者 |
| 新的 Java 类 / SPI / 配置项 | **不需要** | 见第 2、5 条 |

**新增物的"被放弃复用对象 + 原因"（REDLINE-REUSE-010 强制项）**：放弃"直接复用 `mango dev start backend` 作为唯一入口"，原因是它无法在进程内提供断点调试能力，而这是本需求的目标本身；CLI 编排逻辑本身仍被 100% 复用，只是由 IDE 运行配置把它产出的结果**消费**一次。

---

## 2. 术语（REDLINE-TERMS-030）

**结论：不引入任何新的公共类型、API、权限码、事件、页面 key 或 Resource 类型。** 无 Java 公共标识变更，无新配置项，无新权限码。

概念命名上唯一需要定名的是一次性资产，命名必须**逐字复用既有主术语**：

- 进程模式：`runtime` / `bootstrap`（沿用 `MangoApplication` 与 `MANGO_*` 体系）
- 工作区事实：`.mango/workspace.json`、`.mango/dev-workspace.env`
- 清单：`mango.dev.json`，app 名 `mango-backend`，goal `spring-boot:run`
- 被注入参数名：`--mango.bootstrap.environment-key`、`--mango.release.id/revision/generation/fingerprint`（不得改写、不得另起别名）

**同义词风险点（必须守住）**：

1. 不得把运行配置命名为 "Local"、"Dev"、"BootRun" 之类自造概念名——仓库里这些词没有主名称地位，会制造同义词。建议沿用清单里的 app id。同时**不得**把它叫 `run`、`serve`、`start`、`debug` 等已在别处承载含义的词。
2. 不得引入第二个"环境键"叫法（如 `profile`、`env`）来指 workspace id。
3. 不得为"后端"造新词（如 `server`、`api`），沿用 `backend`。

**是否需要 ASK：需要，且这是阻塞项。** 两个主名称不确定点：运行配置的**定名**（`mango-backend` 还是别的）、以及它落在 `.run/`（可提交、团队共享）还是 `.idea/`（通常 gitignore）。任选其一都等于替团队定主术语，按 REDLINE-TERMS-030 必须 `ASK`，不能自行决定。

---

## 3. 最小实现（REDLINE-MINIMAL-020）

**改动边界（只读方案，不含写入）**

1. **新增一个受版本控制的 IDEA 运行配置资产**，承接 CLI 已算好的启动三元组：
   - Main class：`io.mango.app.monolith.MangoMonolithApplication`
   - Program arguments：`runtime` + CLI 注入的 5 个 `--mango.release.*` / `--mango.bootstrap.environment-key` 值
   - Environment：`.mango/dev-workspace.env` 全量（`MANGO_BACKEND_PORT`、`MANGO_DB_NAME`、`MANGO_DB_HOST/PORT/USERNAME/PASSWORD`、`MANGO_CRYPTO_SM4_SECRET_KEY`、`MANGO_MAVEN_REVISION_QUALIFIER`、`MANGO_OFFICE_PLUGIN_ENABLED`）
   - Working directory：仓库根
2. **不改动** `MangoMonolithApplication`、`MangoApplication`、`dev-bootstrap-receipt.mjs`、`mango.dev.json`、`@mango/cli` 的任何现有逻辑。
3. **不改动** `.gitignore` 对 `.mango/` 的忽略——运行配置只**引用**工作区文件，不复制、不落库密码到受版本控制文件。
4. 需要随附一句 README/规则侧说明：该配置是 `mango dev start backend` 的等价观测入口，仍受第 4 节规则约束（启动前仍应先 `mango dev start backend` 完成 bootstrap，或由 CLI 生成这些值）。

**前置顺序（不可省，否则必然失败）**：先执行一次 `mango dev start backend`，让 CLI 完成 workspace init、库自动创建、`bootstrap apply` 与回执落盘。IDEA 配置只承担"用同一 release tuple 起 runtime"这一段。

**当前消费者**：本仓库开发者（含 Agent 无法覆盖的人工调试场景）在 IDEA 中对 `mango-monolith-app` 打断点调试。

**责任边界**
- 适用：单 worktree、单 worktree 自有 `mango_dev_*` 库、本机已具备 MySQL 与 `mysql` 客户端、`mango dev start backend` 至少成功执行过一次。
- 不适用：空库首次 bootstrap（仍需 CLI cold apply）、多后端 app 的并行启动、前端 `mango-admin`、生产/预发/CI、worktree 端口冲突场景、按 MDC 号隔离的多 worktree 并行调试（IDEA 单配置只绑定一个工作区）。

**验证入口**
- 断点可命中：在 `MangoMonolithApplication.main` 后、某个 Controller 或 Service 上打断点，`runtime` 路径可达。
- 与 CLI 启动等价：`MANGO_BACKEND_PORT` 对应端口 `/actuator/health` 返回 `UP`，且日志中不出现 `BOOTSTRAP_RUNTIME_RECEIPT_MISSING` / `_STALE_REVISION` / `_ENVIRONMENT_MISMATCH`。
- 端口所有权一致：`mango dev status` 与 IDEA 进程指向同一 workspace id、端口和数据库名。
- 回执门禁未被绕过：故意改错 `--mango.release.revision`，必须看到回执校验失败（证明门禁仍生效，而非配置绕过了它）。

---

## 4. 简单任务问卷（REDLINE-QUEST-050）

**结论：不生成固定问卷。** 本任务**不新增任何公共概念**——进程模式、参数名、配置文件、app 名全部是既有主术语，无认知风险，因此不触发 REDLINE-QUEST-050。

**术语表：仅按需记录一条**，即第 2 条里必须 ASK 的那个运行配置定名结果（以及 `.run/` vs `.idea/` 的选择）。这是一条最小术语决定记录，不是问卷。除它之外不生成术语表、不生成无消费者文档、不生成观察不到目标的测试。

---

## 5. 过度设计（REDLINE-OVERDESIGN-060）

逐条对照，结论：**按本方案无命中；若越界，以下四类即为违例**

| 过度设计形态 | 本方案风险 | 判定 |
|---|---|---|
| 新增抽象但本 PR 无消费者 | 若顺手加 `MangoLocalStartupConfiguration`、`LocalRunSupport` 之类 Java 类 | **违例，禁止** —— 消费者的正式形式是 IDE 配置，Java 侧无调用者 |
| 新增配置项但无读取逻辑 | 若新增 `mango.local.debug=true` 或 `mango.dev.ide.enabled` 之类开关 | **违例，禁止** —— 无任何代码读取它 |
| 新增接口/扩展点但只有一个实现 | 若为 IDE 配置抽一个 provider/extension point | **违例，禁止** —— 只有 `mango-backend` 一个实现，扩展需求也未进入已批准需求 |
| 仅单模块内部使用的公共类型 | 若把参数拼接逻辑提成公共工具类 | **违例，禁止** —— 拼接逻辑已存在于 `dev-bootstrap-receipt.mjs`，重复实现即双份】
| 无必要新依赖 | 无 | **不命中** —— 无 Java/npm 依赖变更 |
| 同一概念多名称 | 若运行配置另起 "local/dev/bootrun" 名 | **违例，禁止** —— 见第 2 条 |
| 以"更整洁"为由加抽象层 | 无 | **不命中** |

**额外两条明确不做（避免把红线扩大解释成允许范围）**：

- **不做"无参默认 runtime"兜底**：改 `MangoApplication` 让 `args` 为空时默认 `runtime`，会让 `MangoApplicationTest:57-65` 的 fail-fast 契约失效，并把"缺参数"从显式错误变成静默路径——属第 2 节禁止的"通过异常捕获/空值判断重新进入旧路径"的同类问题，且使 `runtime` 在无回执时可能出现"启动即拒绝"的更难排查故障。
- **不做新 CLI 子命令**：`mango dev start backend` 已完整覆盖，新增 `mango dev config idea` 会立刻构成"新增抽象但只有一个消费者、且当前 PR 无第二消费者"的过度设计，同时把 IDE 路径耦合进 CLI 版本发布节奏。

---

## 阻塞点与下一步

**必须先 ASK 的两项（阻塞，不 ASK 无法定名/定位置）**：

1. 该 IDEA 运行配置的**主名称**（是否直接沿用 `mango-backend`）。
2. 落位 `.run/`（团队共享、进版本控制）还是 `.idea/`（本机私有、gitignore）。这直接影响"是否引入受版本控制新资产"以及是否需要 PMO preflight 的 `M01` 判断。

**建议的下一步**：先确认上述两点 → 再按 `REDLINE-MINIMAL-020` 补一份含"当前消费者/责任边界/验证入口"的最小方案记录 → 由 Dev 阶段实施。若你确认名称沿用 `mango-backend` 且落 `.run/`，我可以直接产出该运行配置的具体内容（仍为只读输出，不落盘）。
