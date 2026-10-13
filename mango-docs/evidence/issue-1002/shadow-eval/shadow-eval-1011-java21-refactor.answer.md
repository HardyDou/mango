已读完两份规范，并按 AGENTS.md 执行了 preflight（`role=dev / phase=develop`）。下面是方案，未做任何写入。

## 0. 前置结论与阻断点（先说这个）

**必须新增独立 worktree。** preflight 判定 `M01 decision: CREATE`，理由是当前 worktree 为 detached HEAD、且未声明复用。改 `mango-parent` / `mango-infra` / `mango-system` 等基础 pom 会触发全 Reactor 重编译，直接在 detached HEAD 上改会在验收时无法界定基线。

**必须读取的规范清单**（preflight `Must read`，18 项）中，本任务最相关的是 `rules/backend/01-code.md`、`rules/backend/08-test.md`、`rules/09-test-case-automation-flow.md`、`rules/11-delivery-assurance.md`、`rules/05-ai-delivery-quality.md`。第 8 条（风险取需求影响与方案风险最大值、只执行能观察目标的 M09-M16）和第 9 条（只验证直接修改的 Maven 模块，全 Reactor 仅用于全局架构输入）直接限定下面的验证范围。

**一个必须 ASK 的边界**：需求说"校准 Chromium 登录选择器"，但 `mango-ui/apps/mango-admin/e2e/specs/` 下有 **27 个 spec、38 处** 使用 `input[placeholder="用户名"]`、**23 处** 使用 `button:has-text("登 录")`，而 `mango-ui/packages/auth/src/views/login.vue:50,55,73` 的实际文案是 `请输入用户名` / `请输入密码` / `登录`。全量替换是独立的前端 E2E 债务清理，不属于 Java 21 验收修复。**我建议只修验收直接依赖的用例，其余登记为独立任务**——这一点请确认，否则第 3 项的改动边界会失控。

---

## 1. 复用判断（REDLINE-REUSE-010）

按"Mango 能力 → 当前模块已有实现 → 标准库 → 已装依赖 → 新增结构"逐条落地：

| 需求点 | 复用对象 | 判定 |
|---|---|---|
| Java 21 统一 | 根 reactor `mango/pom.xml:17-19` 已有 `maven.compiler.release=21`；`mango-parent/pom.xml` 只需对齐为同一套三元组并补 `<release>` | 复用已有机制，**不新增** `maven.compiler.*` 之外的任何属性；`mango-infra`、`mango-infra-context`、`mango-infra-crypto`、`mango-system`、`mango-org-core` 五处的 17 覆盖应**删除**而不是改写成 21（父级已是唯一来源） |
| Redis 禁用 | 已装 Spring Boot 的 `@ConditionalOnProperty` + `KvRedisProperties.enabled`（`KvRedisProperties.java:26` 字段已存在且当前**无任何读取方**） | 复用已有字段和已有注解，**不新增配置键** |
| KV 回退 | `KvStoreAutoConfiguration.autoMemoryKvStore()`（第 101-108 行）在无 `RedissonClient` 时已回退 `MemoryKvStore`；禁用 Redis 后该 `@ConditionalOnMissingBean(RedissonClient)` 自然命中 | **零改动**。这是本次最关键的复用结论：回退能力已存在，缺的只是"让 Redis 真的能关" |
| 分页边界 | `PageQuery.getSize()`（`PageQuery.java:51-56`）已有 `<=0 → 默认 / >MAX → clamp` 归一 | 该处**已正确**。真正的边界缺陷在 `PageResult.of()`（`PageResult.java:75`）的 `(total + size - 1) / size` —— `size<=0` 时除零 |
| 文件预览可选依赖 | `FilePreviewFileGateway` / `FilePreviewTaskServiceImpl` 的构造器注入已是 Spring 标准可选注入位 | 复用 `org.springframework.lang.Nullable`（Spring 已在类路径），**不新增 Optional 包装类型、不新增 `ObjectProvider` 抽象** |
| locker 配置 | `KvCapabilityAutoConfiguration:82,93` 已有 `@ConditionalOnExpression("${mango.kv.capability.locker:false}")`；`KvStoreProperties.Capability.locker`（第 255 行）已存在 | 复用，**只补配置文件里的开关值** |
| Workflow/Bootstrap 测试契约 | `WorkflowApiSurfaceContractTest`、`WorkflowMigrationUpgradeIntegrationTest`、`BootstrapPublicApiDeclarationContractTest` 的既有断言方法 | 复用既有断言结构与既有 fixture 目录，只改期望值/fixture 内容 |
| Chromium 登录选择器 | Playwright 内置 `getByPlaceholder` / `getByRole`（标准能力）+ `login.vue` 实际文案 | 复用 Playwright 标准 API，**不新增 selector 常量文件或 page-object 抽象** |

**结论：本任务不需要新增任何结构。** 全部六项都是"修正现有实现的取值/条件/断言"，没有一项落在第 5 类。

---

## 2. 术语（REDLINE-TERMS-030）

**不引入新术语。**

- 不新增公共类型、API、权限码、事件、页面 key、Resource 类型。
- 不新增配置键：`mango.redis.enabled`（`KvRedisProperties.enabled`）、`mango.kv.capability.locker`（`Capability.locker`）**两个键都已存在**，本次只是让既有读取方/配置文件接上，属于"补齐既有术语的实现"，不构成新增公共标识。
- 不重命名任何公共标识。`mango-parent` 的 `<java.version>` 从 17 改 21 是**值变更**，不是标识重命名。

**需要 ASK 的两处**（按规则"术语冲突或主名称不确定时必须 ASK"）：

1. **上节第 0 项的 E2E 选择器范围**——这本质上是"验收修复"与"前端测试债务"的边界划分，不是命名问题但同属任务边界不确定。
2. **`FilePreviewTaskServiceImpl` 中 `convertApi == null` 的失败语义文案**。当前同类路径已有文案 `"暂不支持该文件格式的预览"`、`"暂不支持该文件格式转换为 PDF"`，新增分支必须表达"能力未启用"而非"格式不支持"这一**不同的业务含义**。如果直接复用"暂不支持"系列措辞，会在同一交付范围内制造同义词（REDLINE-OVERDESIGN-060 的"同一概念出现多个名称"反向情形）。我倾向新增明确区分的一条文案，但**请确认是否可接受新增一条面向用户的状态文案**，以及是否需要走 `mango-docs/guides/` 的能力说明同步（`rules/08-capability-docs.md`）。

除此之外无术语决定需要记录，不产生认知风险。

---

## 3. 最小实现（REDLINE-MINIMAL-020）

### 改动边界

**A. Java 21 统一（6 个 pom，仅 pom）**
1. `mango/mango-parent/pom.xml:30-32` → 17/17/17 改 21/21/21，并新增 `<maven.compiler.release>21</maven.compiler.release>`；第 70-71 行 pluginManagement 补 `<release>`。
2. **删除**（不是改写）以下五处的 17 覆盖，让根/父成为唯一事实来源：
   - `mango/mango-infra/pom.xml:19-20`
   - `mango/mango-infra/mango-infra-context/pom.xml:19-20`
   - `mango/mango-infra/mango-infra-crypto/pom.xml:19-20`
   - `mango/mango-platform/mango-system/pom.xml:19-20`
   - `mango/mango-platform/mango-org/mango-org-core/pom.xml:77-87` 整个 `<build>` 块（该块只做 17 覆盖，删后无其他内容）
   
   注意 `mango/mango-platform/mango-org/mango-org-core/pom.xml` 的 `<source>/<target>` 是**字面量 17**，不是属性引用，属于典型的历史债务泄漏点。

**B. Redis 禁用与 KV 回退（1 个类 + 1 个测试）**
- `mango-infra-kv-starter/.../redis/KvRedisAutoConfiguration.java:26-29` 追加 `@ConditionalOnProperty(prefix = "mango.redis", name = "enabled", havingValue = "true", matchIfMissing = true)`。
- `KvStoreAutoConfiguration` **不改**——`auto` 分支的 `@ConditionalOnMissingBean(RedissonClient.class, ignored = IKvStore.class)`（第 103 行）会在 RedissonClient 缺席时自动落到 `MemoryKvStore`。
- 新增测试落在**已存在的** `mango/mango-infra/mango-infra-test/src/test/java/io/mango/infra/kv/starter/KvStoreAutoConfigurationTest.java`：`mango.kv.store.type=auto` + `mango.redis.enabled=false` → 断言无 `RedissonClient` bean 且 `IKvStore` 为 `MemoryKvStore`。这是**回退路径唯一的验证入口**。

**C. 分页大小边界（1 行 + 1 测试）**
- `mango/mango-common/src/main/java/io/mango/common/vo/PageResult.java:75` 的 `pages` 计算对 `size <= 0` 做保护（返回 0），消除除零。
- 测试落在已存在的 `PageResultTest`。
- `PageQuery` 不改（其归一逻辑正确，改了反而会改变 `@Min/@Max` 校验与归一的分工）。

**D. 文件预览可选依赖（2 个类，各 1 处签名 + 1 处判空）**
- `FilePreviewFileGateway` 构造器第三参与 `FilePreviewTaskServiceImpl` 构造器第三参标注 `@Nullable`。
- `FilePreviewTaskServiceImpl` 转换分支在调用 `convertApi` 前补齐 `null` 判定与明确的失败状态（**不吞异常、不返回固定成功**，走既有 `update(task, FAILED, PROGRESS_COMPLETE, ...)` 通道）。
- 不改 `FilePreviewAutoConfiguration` 的 `@ConditionalOnClass(IFilePreviewService.class)` 与 `cn.keking` 组件扫描——`ConvertApi` 缺席是运行时可选注入问题，不是自动配置开关问题。

**E. locker 配置（1 行 yml）**
- `mango/mango-app/microservice/mango-file-preview-app/src/main/resources/application.yml` 的 `mango.kv.capability` 下补 `locker: true`，与同级 `enabled` / `token-store` 对齐。`FilePreviewTaskServiceImpl` 强依赖 `ILeaseLocker`，缺该开关会导致启动失败。

**F. Workflow 测试契约（2 个测试 + 1 个 fixture）**
- `WorkflowApiSurfaceContractTest` 的 HTTP 指纹常量重算（`@ApiAccess` 的 `version` 属性参与指纹构建后，期望值必然变化；`apiFingerprint` 若也受影响则同步）。
- `WorkflowMigrationUpgradeIntegrationTest` 三处 `migrationsExecuted` 期望值（`1→2` ×2、`2→3` ×1）——因为生产迁移链新增了 `V2__add_workflow_audit_columns.sql` 与 `V3__workflow_participation_auto_assignment.sql`，而测试期望还停在只有 V1 的时代。
- **`src/test/resources/db/migration/workflow-1.0.20/V1__init_workflow.sql` 是测试 fixture，不是生产历史 migration**，可以补列/补表（该 fixture 目前缺 `workflow_form_instance` 表与多张表的 `tenant_id`/`process_instance_id` 等列，导致 V3 回填迁移无源可读）。`src/main/resources/db/migration/` 下的四个生产文件**一行不动**。

**G. Bootstrap 契约（1 个测试）**
- `BootstrapPublicApiDeclarationContractTest`：`PublicEndpoint` 记录新增 `version` 字段，断言由硬编码 `isEqualTo(1)` 改为 `isEqualTo(endpoint.version())`。这是使断言反映真实契约、消除恒真/硬编码断言的**修正**，不是放宽（`test-quality-check.mjs` 要求阻断同值断言）。

**H. Chromium 登录选择器（范围为待 ASK）**
- 按第 0 项，建议只改 `auth-info.spec.ts` 的登录段：`input[placeholder="用户名"]` → `getByPlaceholder('请输入用户名')`、`input[placeholder="密码"]` → `getByPlaceholder('请输入密码')`、`button:has-text("登 录")` → `getByRole('button', { name: '登录' })`。其余 26 个 spec 登记独立债务任务。

### 当前消费者

| 改动 | 消费者 |
|---|---|
| A | 全 Reactor 编译（`mango-parent` 被 `mango-common`、`mango-tools`、各 `mango-platform/*` 继承） |
| B | `mango-file-preview-capability-app`、`mango-file-preview-app`（`store.type` 非 redis 时的启动路径）；`KvStoreAutoConfigurationTest` |
| C | `WorkflowDefinitionService`、`WorkflowBusinessApplyService`、`SystemEventService`、`MangoCrudServiceImpl` 等全部 `PageResult.of` 调用方（空结果集分页） |
| D | `mango-file-preview-app`（`office.plugin.enabled=false` 的部署形态，见 `MangoFilePreviewAppFlowTest`） |
| E | `FilePreviewTaskServiceImpl` 的 `ILeaseLocker` 构造依赖 |
| F/G | 本 PR 的 Java 21 验收本身 |

无一项依赖"未来可能需要"。

### 验证入口

- **A**：`mvn -q -pl mango/mango-parent,mango/mango-infra,mango/mango-platform/mango-system -am validate` + 检查 `target/classes` 字节码 major version 为 65。全 Reactor 验证仅作为发布前门禁，不作为开发阶段门禁（dev-agent 第 9 条）。
- **B**：`mvn -pl mango/mango-infra/mango-infra-test test -Dtest=KvStoreAutoConfigurationTest`（含新增回退用例）。
- **C**：`mvn -pl mango/mango-common test -Dtest=PageResultTest,PageQueryTest`。
- **D/E**：`mvn -pl mango/mango-platform/mango-file-preview test` + `mvn -pl mango/mango-app/microservice/mango-file-preview-app test`（`MANGO_DB_NAME=mango_dev_*` 时才跑迁移集成测试）。
- **F**：`mvn -pl mango/mango-platform/mango-workflow/mango-workflow-starter test -Dtest=WorkflowApiSurfaceContractTest`；迁移用例需真实 MySQL。
- **G**：`mvn -pl mango/mango-app/monolith/mango-monolith-app test -Dtest=BootstrapPublicApiDeclarationContractTest`。
- **H**：`pnpm --dir mango-ui/apps/mango-admin test:e2e specs/auth-info.spec.ts --project=chromium`。
- **门禁**：`node mango-pmo/tools/test-quality-check.mjs --base origin/main`（必跑，改造了契约测试）。

---

## 4. 简单任务问卷（REDLINE-QUEST-050）

**不适用。**

理由：本任务无新增公共概念、无认知风险。第 2 节两个 ASK 是**任务边界确认**，不是术语冲突，按 REDLINE-QUEST-050 只需在交付报告里记录决策，**不生成固定问卷、不生成术语表、不新开无消费者文档**。

同时不新增无观察目标的测试——第 3 节每一处测试都对应上表明确列出的观察目标，不为"补齐完整性"机械执行 M09-M16。

---

## 5. 过度设计（REDLINE-OVERDESIGN-060）

逐条自查，**当前方案无违规**，并列出必须主动回避的诱导项：

| 判定项 | 本方案的结论 |
|---|---|
| 新增抽象但本 PR 无消费者 | 无。零新增抽象 |
| 新增配置项但无读取逻辑 | **这是本仓库已有的反面案例，本次是修它而非复制它**：`KvRedisProperties.enabled`（第 26 行）字段已存在但从来无人读取；`KvStoreProperties.Capability` 的 11 个开关中多个同样如此。B 项让它首次被真正读取。**严禁**在修复过程中顺手新增任何开关 |
| 新增接口/扩展点但只有一个实现 | 无 |
| 新增依赖但标准库即可实现 | **严禁**为此引入任何新依赖。D 项必须用 `org.springframework.lang.Nullable`（已在类路径），不得引入 Optional 库或 Guava；H 项必须用 Playwright 内置 API，不得引入测试辅助库 |
| 新增公共类型但仅单一模块内部使用 | 无。`PublicEndpoint` 的 `version` 字段是 Bootstrap 测试内的**测试记录**，随测试类存活，不构成公共类型 |
| 同一概念多个名称 | 见第 2 节 ASK 第 2 点——这是本方案**唯一**可能触发的项，靠 ASK 前置拦截 |
| 仅为"代码更整洁"新增抽象层 | 无 |

**特别提示——本任务最危险的一类过度设计**：面对 Java 17/21 混用，很容易倾向"新增一个统一 `java-version` 属性 + 自定义 Maven 插件校验 + CI 脚本检查"。本方案明确**不做**：根 `mango/pom.xml` 的三个属性就是唯一事实来源，五处子 pom 的 17 覆盖**删除**即可，新增任何校验机制都属于"以更整洁为由新增抽象层"，且无本 PR 消费者。

**另一处需要克制的**：`mango-ui/packages/mango-cli/scripts/check-generated-backend-gate.mjs:467` 已断言生成模板包含 `<maven.compiler.release>21</maven.compiler.release>`，`templates/full/backend/pom.xml:26-27` 也已正确。这两处**不需要改动**，只能作为 A 项正确性的旁证，不要重复修改。

同时，需求中"不修改生产历史 migration、HTTP path/verb/binding、JSON 包装、权限或租户语义"五条边界与上表一致——F 项必须严格区分 `src/test/resources/.../workflow-1.0.20/`（可改）与 `src/main/resources/db/migration/workflow/`（不可改），G 项只能改测试断言、不能碰 `@ApiAccess` 的 `version` 默认值或任何 Controller 上的注解值。

---

## 下一步

请确认两件事，然后我创建独立 worktree 进入实施：

1. **H 项范围**：只修 `auth-info.spec.ts`（我推荐），还是全量修 27 个 spec（需要单独任务与单独验证预算）？
2. **D 项文案**：是否允许新增一条区别于"暂不支持"系列的"转换能力未启用"用户可见状态文案？
