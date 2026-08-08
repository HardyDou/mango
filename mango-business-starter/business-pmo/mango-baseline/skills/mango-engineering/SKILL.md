---
name: mango-engineering
description: 按已确认范围、交付等级、任务 worktree 身份和验证措施实施、重构、提交、Push 或准备 Mango PR。适用于 Java、Spring、前端、数据库、测试、配置和实现交付；不用于只读 PR 评审或不可变制品发布。
---

# Mango 工程实施

## 解析规范源

按顺序选择首个存在的 `PMO_ROOT`：`<repo>/business-pmo/mango-baseline`、`<repo>/mango-pmo`、`<plugin-root>/dist/baseline`。均不存在时 `STOP`。禁止使用记忆或复制的旧规则。

## 编辑前解析

1. 使用角色 `dev`、阶段 `develop` 和实际影响路径执行 PMO preflight。生成代码前应用工作区决定，并选择返回的代码基线。
2. 创建或复用任务 worktree 前执行 `$PMO_ROOT/tools/check-worktree-delivery-integrity.mjs --mode start`。`CREATE` 传 `--reuse-current-task false`，禁止从有本地改动的非 main worktree 开始新任务。`REUSE` 传 `--reuse-current-task true --expected-branch <已记录任务分支>`；期望分支必须来自既有任务或 PR 证据，不能只读取当前分支后反推。发现未确认的脏 worktree 时 `STOP`；只有用户明确确认精确路径属于独立并行任务后，才可传 `--allow-dirty-worktree <精确路径>`。
3. 分别记录需求影响和方案风险，取较高值映射到 `L0-L5`，并验证该等级要求的精简制品。实施改变范围、影响或方案风险时，先升级等级再继续编辑。
4. 在隔离目录渲染所选模板并执行其声明的检查，通过后再集成。当前项目配置、API 合同和领域事实是输入；旧代码不是结构模板。
5. 只有 checker 失败，或接口、数据、安全、事务、模块边界仍不明确时，才读取 preflight 返回的对应参考资料；禁止开工前批量读取。
6. 把 Spring 注册当作需证明的架构合同：业务 `XxxService implements IXxxService` 使用 `@Service`；可替换框架默认实现使用 starter `@Bean + @ConditionalOnMissingBean`；纯 Java helper 不命名为 Service。禁止在 Controller 或业务 Service 中直接构造受管 Service，禁止引入可变静态 Service 状态。
7. 前端工作使用所选前端代码基线约束 package、API context、页面、导出、样式和测试结构。
8. 只执行能观察验收结果的 M09-M16。保留明确例外；没有真实观察对象时不增加测试类型。

编辑前，把历史示例、旧测试和邻近实现只当作业务或兼容证据。所选代码基线及其机器检查决定结构和约定。确认构建和测试产生的可变内容留在当前 worktree；仅可共享已证明不可变的外部依赖缓存。

当前已经位于非 main 任务 worktree，且用户要求解决同一任务时，复用该 worktree。请求属于另一任务时禁止复用；先执行 start 门禁，当前 worktree 有本地改动时必须阻断。禁止为同一任务再建 worktree，也不得用登记 Issue 代替当前实现；只有用户明确选择登记，或归因证明问题在当前任务之外时才登记 Issue。任务身份确实不明确时，改变工作区状态前返回 `ASK`。

等级要求的制品缺失或无效、直接追踪断裂、工作区策略无法执行或请求与已批准范围冲突时，编辑前返回 `STOP`。关键事实未知或请求例外时返回 `ASK`。

## 实施与门禁

1. 只实施已解析范围。`L0/L1` 直接实施；`L2-L4` 遵循对应单文档；`L5` 遵循已批准的技术设计和实施计划，并保留直接追踪编号。
2. 已批准范围明确要求重写或替换时，让新实现成为唯一执行路径；删除旧实现、调用、配置和 fallback，再搜索旧标识与 fallback 条件。只有已批准范围单独写明消费者、截止时间、退出条件和验证时，才保留兼容路径。
3. Java 变更检查新增或修改的 getter、setter 和 constructor。机械代码使用后端规则要求的 Lombok 形式；只有承担已命名的校验、归一化、防御性复制、可见性、懒加载或框架语义时才手写。禁止用统一 `@Data` 改变模型语义。
4. 每个新增或修改测试都要记录观察的规则、状态、失败、副作用或集成边界，并证明目标确实执行。业务行为放在 `core`；机械合同测试不放在 `api`；业务单元测试不放在 `starter-remote`；`starter` 只保留有真实运行态或集成观察对象的测试。
5. Java/后端变更解析直接修改的 Maven 模块，只对这些模块执行质量门禁，禁止使用 `-am` 或 `-amd` 扩大质量 Reactor。依赖构建和消费者兼容单独验证。只有根 POM、parent、架构门禁变化或显式债务盘点时使用完整 Reactor。质量门禁必须执行 Mango Java/Spring 架构检查，编译通过不能替代。
6. 部分 Reactor 必须阻断新增架构问题，但不能更新 schema-v4 债务预算。显式清债时，生成一次完整 Reactor 报告，使用 `check-architecture-debt-budget.mjs --module <moduleKey|artifactId>` 查询或写入目标，仅写入已验证减少项，再执行全局 `--base-ref` 检查。出现新 identity、替换、跨模块转移、不完整报告或模块债务增加时 `STOP`。
7. 前端变更只运行证明受影响结果所需的 lint、type、build、unit 或 browser 门禁。局部纯视觉变化可使用静态检查加定向截图；只打开页面不构成证据。
8. 执行 preflight、设计和实施计划要求的其它命令。禁止通过 suppress、baseline、弱化规则获得通过。
9. 触及旧文件时，在行为不变且当前验证可观察的前提下，清理同一符号或局部代码块内的违规。会改变公共合同、数据、安全、租户、事务或跨模块边界的治理另行处理。
10. 必需门禁失败或无法执行时，返回 `STOP`，报告命令、失败和证据；不得宣称完成。
11. 只有实施项完成、必需门禁通过且变更到测试映射已更新时，才返回 `NEXT: $mango-qa-verification`。

## 提交与交付完整性

1. 提交前查看 `git status --porcelain=v2 --untracked-files=all`，只暂存已审阅的任务文件，再执行 `check-worktree-delivery-integrity.mjs --mode commit --expected-branch <已记录任务分支>`。存在未暂存、未跟踪或冲突文件时 `STOP`；禁止用宽泛自动暂存绕过门禁。
2. 提交后执行 `--mode deliver --require-upstream false`。用户授权 Push 后，以及创建、更新或合并 PR 前，改用 `--require-upstream true` 再执行；必须同时满足 worktree 干净且 upstream 领先/落后均为零。
3. 删除已合并 worktree 前同步 base，并执行 `--mode cleanup --expected-branch <已记录任务分支> --base <base分支>`。保留并报告其它脏 worktree，禁止删除或修改无关用户工作。
4. 报告任务 worktree 路径、已记录分支、暂存文件数、剩余本地变更、upstream 领先/落后数及其它脏 worktree。禁止仅依据 PR diff 宣称提交、PR、合并或清理完成。

禁止相信“门禁已通过”的口头声明。返回 `NEXT` 前定位批准制品和证据，执行或核对精确命令，并报告路径和退出结果。完成报告必须说明：是否拒绝了冲突的历史模板、旧执行路径是否残留、Java 机械代码如何表达，以及每个修改测试观察什么、为何归该模块所有。

空白上下文下，目标、范围和风险事实缺失时返回 `ASK`；禁止猜测等级。
