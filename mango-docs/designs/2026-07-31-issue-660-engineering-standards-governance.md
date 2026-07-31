# Issue #660：工程治理与 Worktree Maven 隔离设计

## 1. 背景与目标

Issue #660 暴露了同一项目多个 worktree 共享本地 Maven SNAPSHOT 产物的问题；Issue 评论和本次会话进一步确认了 AI 重写保留旧实现 fallback、照抄历史违规代码、重复手写 Java 机械代码以及用无价值测试冒充验证等治理缺口。

本任务在保留工程规范与仓库 Skill 强化的同时，直接修复 Issue #660 的 Maven 制品冲突：多个 worktree 继续共享第三方依赖和插件缓存，但本项目开发期 `install` 与 Spring Boot Maven run 使用 workspace 独立版本坐标。

目标：

- 明确多 worktree 只隔离项目产出，允许安全共享不可变外部依赖缓存。
- 让“重写/替换”具有删除旧实现的明确语义，禁止默认保留旧逻辑兜底。
- 让当前规范优先于历史代码示例，历史债务不得授权新增违规。
- 统一 Java 机械代码的 Lombok 使用边界。
- 让测试围绕可观察行为和业务规则，并按 `api/core/starter/starter-remote` 职责落位。
- 将上述约束同步写入主要工程、计划、缺陷修复、模块开发和 QA Skill，使 Agent 在实施时主动执行，而不是只依赖规则被偶然加载。
- 为每个 workspace 生成稳定 Maven revision qualifier，并对每个 Maven reactor 的基础 revision 动态派生最终开发版本。
- 让新生成项目和业务模块统一采用 CI-friendly `${revision}`，同时让旧 workspace 可无损补配置、旧固定版本 POM 在 Maven 执行前明确失败。

不处理范围：

- 不拆分或复制第三方 Maven repository；显式使用 `.mango/m2/repository` 真实目录的完整隔离模式继续保留。
- 不删除或迁移现有 `api`、`starter-remote`、`starter` 测试。
- 不移动当前位于 `api` 的实现逻辑。
- 不新增 Java、Node.js 或 CI 机器门禁。
- 不提交、Push、创建 PR、发布版本或关闭 Issue。

## 2. 风险与交付模式

- 需求影响：`L3`。本任务改变 AI 编码、重构、模块分层和测试资产的长期执行口径。
- 方案风险：`L3`。治理系统修改自身，并改变 CLI 生成模板和本地 Maven 启动参数；规则、模板或命令注入不一致会形成假隔离。
- 最终风险：`L3 = max(L3, L3)`。
- 交付模式：`FULL` 治理任务，使用本治理设计、定向静态验证和人工复核，不伪造产品 BRD/SRS/TDD/Plan。
- 工作区：`M01=CREATE`，分支 `governance/issue-660-standards`，worktree `/Users/hardy/Work/mango-issue-660-standards`。
- 保障措施：启用 M07、M08、M09、M10、M14；不启用 M11-M13、M15、M16。使用纯函数单测、CLI 生成/迁移/命令回归和静态门禁观察新增目标；没有 API 或 UI 行为，不伪造对应验收。

## 3. 方案选择

采用“共享外部缓存、隔离项目坐标，并同步强化规范与执行 Skill”方案：长期约束写入现有最接近的规则文件，CLI 负责 workspace revision，模板负责 CI-friendly Maven 契约，README 只说明使用方式。

### 3.1 Maven 隔离方案

`.mango/m2/repository` 默认仍链接到 `~/.m2/repository`。workspace 分配稳定 `mavenRevisionQualifier`，例如 slot `010` 对应 `mango-010`；CLI 针对每个 `spring-boot-maven` app 向上定位 reactor 根 POM，读取其具体 `<revision>`：

```text
基础 revision：1.0.0-SNAPSHOT
workspace：     mango-010
最终 revision：1.0.0-mango-010-SNAPSHOT
```

最终值以 `-Drevision=...` 同时注入 manifest 的 Maven install 和 Spring Boot Maven run。不同 reactor 可以保留不同基础版本，只共享同一 workspace qualifier；CLI 不修改受版本控制的 POM，也不改变发布命令。

新项目根 POM 使用 `<version>${revision}</version>` 并提供基础 `<revision>`，所有子模块 parent 和本项目内部模块依赖引用 `${revision}`。业务模块规范源与 CLI 投影同步修改，`mango module add` 写入 app 的依赖也使用 `${revision}`。

已有 workspace 再次初始化时自动补充 `mavenRevisionQualifier` 和 `MANGO_MAVEN_REVISION_QUALIFIER`，保持已有端口、数据库、人工连接配置及公共 repository 数据。若 reactor 根 POM 不能证明支持 CI-friendly revision，CLI 在 plan/start 解析阶段失败并给出升级提示，不删除旧 SNAPSHOT，也不静默继续。

未采用的方案：

- 只改规则：规范源正确，但 Skill 在具体工程任务中仍可能只给出笼统的“遵守规范”，无法主动检查重写残留、Lombok 和测试归属。
- 把每个 worktree 指向完全独立 repository：隔离最强，但会重复下载全部第三方依赖，不符合已确认的默认模式；现有真实目录模式保留为显式严格隔离。
- 仅修改 `.m2` 路径而不改变 GAV：共享仓库下仍会覆盖同名 SNAPSHOT，不能解决根因。
- 只向命令添加 `-Drevision` 而不更新模板：固定版本 POM 会忽略参数，形成假隔离。
- 同时清理存量测试和 API 实现：可以立即减少债务，但会把治理任务扩大为跨模块重构并引入回归风险。

## 4. 规范落点

长期规则只维护在 `mango-pmo/rules/**`：

| 规则源 | 本次强化内容 |
|---|---|
| `rules/02-dev-environment.md` | worktree 项目产出隔离、不可变外部依赖缓存共享边界、同 GAV SNAPSHOT 和本地安装产物禁止跨 worktree 共享 |
| `rules/03-ai-coding-redlines.md` | “重写/替换”必须删除旧实现、旧调用链和旧配置；需要兼容时必须作为独立迁移需求重新定义，不能伪装成 fallback |
| `rules/00-dev-flow.md` | 当前规范优先于历史代码；历史违规不得作为模板，历史债务基线不得放行新增违规 |
| `rules/backend/01-code.md` | getter、setter、依赖注入构造器的 Lombok 优先原则及允许手写的语义例外 |
| `rules/backend/08-test.md` | 有效测试判定和 `api/core/starter/starter-remote` 测试归属 |
| `rules/index.json` | 同步更新受影响规则的 reason，使 preflight 能准确说明新增治理语义 |

### 4.1 Worktree 产出隔离

目标口径：项目自产出的构建结果、生成代码、本地安装制品、运行态和测试过程数据归当前 worktree；远端下载且内容不可变的第三方或平台依赖缓存可以共享。相同 GAV 的 SNAPSHOT、本项目 `install` 产物以及来源无法证明的可变缓存不得共享。

工具不能证明共享对象不可变或不能区分项目产出时，应选择 worktree 隔离并明确失败，不能退回共享本地仓库。本次 CLI 以不同 GAV 隔离本项目产出，并在旧 POM 不满足契约时 fail-closed。

### 4.2 重写与历史债务

“重写/替换”表示新实现成为唯一执行路径。交付范围内必须删除旧实现、旧调用、旧配置和仅为旧路径服务的测试，禁止用 feature detection、catch、空值、开关或调用失败重新进入旧实现。

如果业务确实要求兼容迁移，该任务不再按“彻底重写”处理，必须单独明确兼容对象、期限、退出条件和验证入口。历史代码即使仍在运行，也不能作为新增代码模板；新代码和被修改代码按当前规范编写，历史债务只允许保持或减少。

### 4.3 Lombok 使用边界

- 普通 JavaBean 访问器优先使用 `@Getter`、`@Setter`；不得为了省注解无差别使用 `@Data`，尤其不得因此改变 Entity 的 `equals/hashCode/toString` 语义。
- Spring 构造器注入优先使用 `final` 字段和 `@RequiredArgsConstructor`。
- 全参、无参构造器只在确有框架或模型语义时使用对应 Lombok 注解。
- 构造器或访问器包含校验、归一化、防御性复制、权限控制、懒加载、特殊可见性或框架要求时允许手写；必须能指出生成代码无法表达的语义。
- record、枚举和不可变值类型按其语言语义选择，不为使用 Lombok 而改写更合适的结构。

### 4.4 有效测试与模块归属

测试必须能指出被观察的规则、状态、失败语义、副作用或集成边界。只验证 DTO、record accessor、getter/setter、枚举 `values/valueOf`、注解元数据、简单构造器、简单委托、Bean 存在或实现复述的测试不作为有效测试。

模块归属目标：

- `api`：只保存协议和契约；不为机械契约代码建立 `src/test`。发现需要单测的执行逻辑时，优先把逻辑及测试迁移到 `core/support`，不得通过继续在 `api` 增加测试把错误边界固化下来。
- `core`：集中承载业务规则、状态转换、校验、持久化协作和主要单元/集成测试。
- `starter-remote`：只承载远程适配和装配，不放业务规则单测；需要证明 HTTP、Feign、签名、序列化或远程协议兼容时，放到能启动真实边界的集成或入口流程测试模块，不用简单委托和反射快照测试代替。
- `starter`：只保留能证明 Spring 条件装配、HTTP/安全边界、资源初始化或运行时集成结果的少量测试；只检查 Bean 数量、接口实现关系、反射字段或 Controller 透传的测试应删除或由静态架构门禁承担。

本任务不按新口径删除存量测试。后续触及相关模块时，以测试目标和真实观察面逐项迁移或删除，不按目录机械宣布全部通过或全部无价值。

## 5. Skill 强化

Skill 不复制规则全文，只加载规则源并把执行检查写成明确动作：

| Skill | 新增执行要求 |
|---|---|
| `mango-engineering` | 实施前检查历史示例是否违规；重写后搜索并删除旧路径；Java 检查 Lombok 机械代码；按模块职责建立有效测试 |
| `mango-defect-fix` | 修复方案为重写时禁止旧实现 fallback；回归测试必须直接观察缺陷根因和新路径 |
| `mango-module-development` | 模块计划明确 `api/core/starter/starter-remote` 代码和测试归属，禁止把业务逻辑或机械测试放入适配层 |
| `mango-plan-implementation` | 计划项必须包含旧路径删除、当前规范核对和测试目标映射，不得以“补单测”作为无观察对象的默认任务 |
| `mango-qa-verification` | 验收前逐项核对测试目标、被测对象是否真实执行、结论是否超过观察范围，并拒绝无价值测试充数 |

Skill 的结束条件应要求报告：新实现是否仍存在旧路径、历史代码是否被当作模板、Java 机械代码选择、测试目标与模块归属。规则细节继续链接到规范源，避免形成第二套规范。

## 6. 文档与能力说明

本任务改变 AI 工程、CLI workspace 配置、生成项目 Maven 契约和测试验收方式，M08 启用。同步更新 CLI README、full template README/AGENTS、business starter Agent/baseline 与能力地图；没有 API、菜单、权限、租户或 UI 变化。

## 7. 验证设计

验证同时观察 CLI 行为和规范/Skill 一致性：

1. 确认所有长期约束只在 `mango-pmo/rules/**` 定义，Skill 使用链接和执行动作，不复制规则段落。
2. 运行 CLI 纯函数单测，覆盖 qualifier、revision 派生、幂等注入和 Maven 命令识别。
3. 运行 CLI 完整回归，覆盖两个 workspace qualifier 不同且稳定、公共 repository 链接仍共享、旧 env 自动补写、install/run 参数一致、非 CI-friendly POM fail-closed、生成模板和业务模块投影一致。
4. 检查 `rules/index.json` 可解析，受影响 reason 与规则语义一致，并运行 PMO Skill/package 适用门禁。
5. 运行 README/能力文档审计、样式静态门禁和 `workspace-layout-check.mjs`。
6. 独立复核安全缓存共享与 SNAPSHOT 隔离、彻底重写与兼容迁移、当前规范与历史模板、Lombok 机械代码与语义方法、有效测试与机械测试。

本任务不运行 API/UI/E2E；功能不新增这些入口。Maven 版本派生与命令调用由 CLI 回归中的生成项目和 fake Maven 直接观察，不用无关 Java 全量 Reactor 代替。

## 8. 验收标准

1. 多 worktree 规则能区分可共享的不可变外部依赖缓存与必须隔离的项目 SNAPSHOT/本地安装产物。
2. 重写规则明确删除旧实现，不再把旧逻辑作为默认 fallback；兼容迁移需要独立范围和退出条件。
3. 当前规范优先级、历史债务不扩张和历史代码非模板规则在流程与工程 Skill 中一致。
4. Lombok 规则覆盖 getter、setter、构造器和允许手写的语义例外，不鼓励无差别 `@Data`。
5. 测试规则能判断测试价值，并明确 `api/core/starter/starter-remote` 的测试归属。
6. 五个相关 Skill 都把规则转成可执行检查和结束条件，而不是只写“遵守规范”。
7. 两个 worktree 保持同一公共 Maven repository，但相同基础 revision 被派生为不同 GAV；同一 workspace 重复初始化保持稳定。
8. install 与 Spring Boot Maven run 注入同一最终 revision，显式 Maven wrapper 同样支持，非 Maven command 不被改写。
9. full project 与 business module 模板的根版本、parent 和内部依赖均使用 `${revision}`，`mango module add` 不重新写入固定项目版本。
10. 旧 workspace 自动补 workspace/env 字段且不删除 repository 数据；非 CI-friendly 旧项目在 Maven 执行前明确失败。
