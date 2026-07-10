# PMO 可执行质量契约最终实验报告（季度治理总结）

> 实验日期：2026-07-10
>
> 受测实现提交：`be970469d0eab3715880e565e94d1bf756dec4fc`
>
> 能力：`pmo-executable-quality-contract`

## 1. 最终判定

| 判定对象 | 结论 | 依据 |
|---|---|---|
| 确定性规则门禁 | **PASS** | 48/48 场景、192/192 重复判定、120/120 关键阻断、0 合法误阻断 |
| L0-L3 任务分级与 Preflight | **PASS** | 23/23 自测；4px 布局为 L0，权限显隐为 L3 |
| Maven 入口、发布包、starter 同步 | **PASS** | Maven 插件 151 测试；81 文件 0 问题；发布包 71 文件；starter 0 漂移 |
| 定向 Mutation | **PASS** | 1/1 冻结种子被指定测试杀死，Surefire XML 验证真实执行 |
| 空白上下文 Agent 分类 | **FAIL** | 候选精确匹配 88.10% < 95%；每组仅完成 39/42；未完成运行受额度限制 |
| PMO 全量推广认证 | **FAIL / 暂不批准** | Agent 实验未过；真实 MySQL、真实产品 UI/E2E、完整纵向业务试点尚缺 |
| changed-only 试点 | **CONDITIONAL PASS** | 仅允许带硬门禁、可回滚、受监控的 30 天灰度试点 |

这不是“感觉可用”的结论。能由程序确定的部分已经通过；依赖 Agent 语义理解和真实业务环境的部分没有通过，因此不写成 100% 成功，也不批准一次性全量替换。

## 2. 本季度完成了什么

1. 把测试类型统一为 `UNIT`、`API`、`UI`；`E2E` 与 `UI` 同义。静态 Review、Mutation、截图是验证手段，不是第四种测试。
2. 建立差异驱动质量契约：R0 机械变更不强迫写单测；关键逻辑要求 UNIT；复杂后端流程增加真实入口 API；关键用户流程增加 UI。
3. 建立无用测试和 Mock 防逃逸门禁，覆盖常量自证、getter/setter、只断言不报错、Mock 被测目标、包装层 Mock、内部 API route mock 等问题。
4. 将 Java Controller/Service/Mapper、`api/core/starter/starter-remote` 依赖方向和前端页面/表单/UI 规则落成可执行检查。
5. 将质量门禁接入 Maven、CI、`@mango/pmo` 发布包和业务 starter，避免规范只存在于文档。
6. 建立 `latest` 唯一基线、受控 promote、失败报告不可提升、过程数据不入库的证据生命周期。
7. 增加 L0-L3 流程分级，解决“按钮移动 4px 也走完整项目流程”的过度治理。
8. 将“确定性门禁效果”和“Agent 空白上下文理解能力”拆成两套实验，杜绝用门禁 100% 冒充 AI 100%。

## 3. 合并后的问题清单

项目问题与开发反馈合并为 37 项，完整概率、影响和设计决策见[设计文档](../designs/2026-07-10-pmo-executable-quality-contract-design.md)。重点如下：

| 类别 | 问题编号 | 主要问题 | 原发生概率 |
|---|---|---|---|
| 无用测试 | P01-P03 | 常量自证、机械代码测试、无业务断言的覆盖率测试 | 高至极高 |
| Mock 逃逸 | P04-P07、P17 | Mock 被测逻辑、错误层级 Mock、UI Mock 自有 API、包装多层规避 | 高 |
| UI 伪通过 | P08-P11 | 只看 DOM、截图无断言、私有 CSS、固定等待和 force | 中至高 |
| 证据与基线 | P12-P14、P35 | 过程数据入库、多份 latest、测试顺带改基线、报告不可追溯 | 中至高 |
| 风险与证明 | P15-P16、P33、P36 | 测试义务靠 Agent 自选、无执行证明、无 Mutation、测试一刀切 | 高至极高 |
| Agent/输入治理 | P18-P20、P30-P31 | Agent/Skill 只复述、fail-open、版本漂移、主链路可跳过、永久例外 | 中至高 |
| Java | P21-P25 | Controller/Service/Mapper/事务/SQL/模块依赖/协议漂移 | 中至高 |
| Web | P26-P29 | 路由权限闭环、布局伪装、表单状态缺失、设计系统漂移 | 高至极高 |
| 存量与稳定性 | P32、P34 | 新旧债务一起阻断、Flaky 只靠重跑 | 中 |
| 流程成本 | P37 | 微小视觉调整也强制 worktree、计划、全量 E2E、截图和长报告 | 极高 |

## 4. 测试策略：什么该测，什么不该测

| 变更性质 | 最小充分验证 | 禁止做法 |
|---|---|---|
| 常量、getter/setter、简单赋值/委托 | STATIC_REVIEW | 为 `A = 10` 编写 `A == 10` 单测 |
| 关键业务规则、边界、状态转换、算法 | STATIC_REVIEW + UNIT | Mock 被测类、被测方法或关键分支 |
| 多个关键技术/业务节点构成的后端流程 | STATIC_REVIEW + UNIT + 真实入口 API | 直接调 Service 冒充 API；Mock Service/Mapper 后声称全流程通过 |
| 关键用户可见流程 | STATIC_REVIEW + 所需 UNIT/API + UI | 拦截 Mango 自有 API；只截图或只断言元素存在 |

外部不可控系统允许使用 WireMock/Stub，但必须明确它是系统边界；本系统的 Controller、Service、Mapper、权限、事务、工作流结果属于受保护证明路径，不能被 Mock 后再声称覆盖该路径。

## 5. 任务大小、复杂度与风险分级

| 等级 | 典型任务 | 必须做 | 明确不要求 |
|---|---|---|---|
| L0 MICRO | 单页 1-2 文件、纯文字/颜色/间距/位置，无行为与数据变化 | 静态 Review、受影响页面快速 smoke、一行验证说明 | 专用 worktree、详细计划、正式 UI/E2E、截图、基线、长报告 |
| L1 SMALL | 局部低风险修改，约 3 个文件内，易回滚 | 3-5 条短计划、changed-only 检查、按风险选测试、简短总结 | 全套设计、全量 E2E、完整交付报告 |
| L2 STANDARD | 多文件、Controller-Service-Mapper、表单提交等正常业务流 | 专用 worktree、简要设计/计划、关键 UNIT、真实入口 API、受影响 UI、latest 基线 | 无关模块全量 E2E |
| L3 HIGH | 权限、租户、金额、事务、并发、公共 API、Schema、跨服务、难回滚 | 完整设计/计划/回滚、证明路径、定向 Mutation、真实环境、正式报告、受影响流程 UI/E2E | 降级成“只改一行” |

Preflight 先依据任务和路径给出临时等级，交付前必须按真实 Git diff 复核；只允许自动升级，不允许 Agent 自行降级。

实际案例：

- “按钮位置移动一点，只调整 4px 间距”被判为 L0：无专用 worktree、无正式 E2E、无截图，只做静态 Review和受影响页面快速 smoke。
- “按钮权限显隐微调一行”被判为 L3：虽然只有一行，但涉及权限，必须验证权限链路和受影响用户流程，不能伪装为微任务。

## 6. 两套空白上下文实验

### 6.1 确定性门禁 A/B：PASS

每次运行使用独立临时目录、空 `HOME`、空 `CODEX_HOME`、固定 UTC、禁用代理；普通场景重复 3 次，关键场景重复 5 次。

| 指标 | Current | Candidate | 结果 |
|---|---:|---:|---|
| 场景级精确匹配 | 13/48（27.08%） | 48/48（100%） | PASS |
| 重复运行加权精确匹配 | 41/192（21.35%） | 192/192（100%） | PASS |
| 关键红线阻断召回 | 5/120（4.17%） | 120/120（100%） | PASS |
| 合法正例错误阻断 | 0 | 0 | PASS |
| 重复一致率 | 100% | 100% | PASS |

`Current 21.35%` 的准确解释是：旧确定性检查在 192 次带重复权重的 PASS/BLOCK 判定中只命中 41 次。关键场景重复次数更多，而旧检查恰好大量漏掉关键红线，所以该值低于场景级的 27.08%。它不是代码覆盖率、项目正确率、测试通过率、AI 正确率或业务质量分。

### 6.2 真实 Agent 空白上下文分类：FAIL

每个案例单独执行 `codex exec --ephemeral --ignore-user-config --ignore-rules --sandbox read-only`；使用临时目录、临时 `HOME`、仅含认证信息的临时 `CODEX_HOME`，只提供同一份任务、政策和输出 schema。模型为 `gpt-5.6-sol`，Codex CLI 为 `0.144.1`。

| 指标 | Current | Candidate | 门槛 |
|---|---:|---:|---:|
| 精确匹配 | 45.24% | 88.10% | ≥95% |
| 关键案例精确匹配 | 41.67% | 86.11% | ≥95% |
| 放行/阻断 + 测试义务匹配 | 71.43% | 92.86% | ≥95% |
| 风险等级匹配 | 78.57% | 90.48% | 参考 |
| 静态 Review 义务匹配 | 76.19% | 90.48% | 参考 |
| 完成运行 | 39/42 | 39/42 | 42/42 |

候选对“纯 4px 按钮移动”的 3 次判定全部正确；但真实 API 场景有 1 次漏掉静态 Review，包装 Mock 场景有 1 次风险等级偏高。最后的权限微调场景中，Candidate 3 次和 Current 2 次因 Codex 使用额度耗尽未返回结果，Current 另 1 次超时。严格按预设阈值，本实验必须判 FAIL，不用已有结果补分，也不把外部限制改写为通过。

## 7. 真实工程验证

### 7.1 Maven 与 Java

| 检查 | 结果 | 边界 |
|---|---|---|
| Maven 插件测试 | 151 个测试通过，含新增 3 个 quality-gate 用例 | 验证成功、缺工具、子进程阻断 |
| Maven 全坐标 goal | 81 个变更文件，0 问题 | `mango:quality-gate` 前缀不可解析，文档和 CI 已改为完整坐标 |
| 工作流定向 Mutation | 1/1 种子杀死 | 每个种子独立 detached worktree；解析 Surefire XML，不以任意非零退出冒充测试杀死 |
| Payment 集成样本 | 7/7 | 使用 Spring、真实 Mapper、H2；不等价于 MySQL 生产环境 |

Mutation 将 `WorkflowApprovalThreshold` 的 `Math.ceil` 临时改为 `Math.floor`：基线运行 2/2 通过；变异运行 2 个测试中 1 个按预期出现 `expected: 2 but was: 1`；源代码和测试文件均带 SHA-256，原工作树未被污染。

### 7.2 发布与消费

- Preflight：23/23。
- 确定性质量门禁：48/48。
- `@mango/pmo@1.1.0`：71 个发布文件。
- `mango-business-starter`：0 missing、0 changed、0 extra。
- 正式基线：每个能力/类型只保留唯一 `latest`；普通 check 只读，promote 必须要求 PASS 报告、owner、approver 和原因。

### 7.3 UI 与现有债务

本能力是 PMO/CLI，没有产品页面，因此没有伪造 UI/E2E 截图或 UI 通过结论。UI 规则由冻结正反例验证；真实产品 UI 必须在后续业务试点中补做。

现有 58 个 E2E spec 仍有 144 项债务：47 个内部 API Mock、42 个 Element Plus 内部 class、5 个固定等待、4 个 `force`、46 个 DOM 顺序依赖。后端重点域审计为 0 BLOCK、81 WARN，主要是 Mapper 替身只能证明局部决策。它们是可见存量债务，不算本次已修复。

## 8. 专家联合评审结论

| 专家视角 | 结论 | 已吸收的约束 |
|---|---|---|
| AI Coding | 条件通过 | 分离确定性门禁与 Agent 指标；禁止把 21.35%/100% 写成 AI 正确率 |
| Java/Spring/MySQL/微服务 | 条件通过 | Maven 主链路 fail-closed；真实入口 API；事务/Mapper/依赖方向检查；H2 不冒充 MySQL |
| Web | 条件通过 | UI=E2E；禁止自有 API Mock；稳定语义锚点；L0 smoke 不冒充正式 UI |
| 测试 | 条件通过 | Mutation 每例隔离；解析 Surefire；基线只保留 latest；无用测试不计覆盖证据 |
| 敏捷教练 | 仅批准受控试点 | L0-L3 最小充分流程；changed-only；历史债务不一次性全阻断 |

五个视角的融合结论不是折中为“全部通过”，而是：机器可判定部分立即硬执行，Agent 语义分类继续改进，复杂业务通过真实纵向试点补齐证据，小改动走轻流程。

## 9. 30 天试点准入与退出条件

只允许在 changed-only 范围开展 30 天试点，并至少选择一个工作流、一个支付后端流程和一个真实 UI 流程。升级为全量规则前必须同时满足：

1. Agent 空白上下文所有运行完成，精确匹配、关键精确匹配、决策+义务匹配均达到 95%，关键漏放行为 0。
2. 确定性门禁关键种子 100% 阻断，合法误阻断率低于 2%。
3. 三条真实纵向证明路径均完成：关键 UNIT、真实入口 API、必要 UI/E2E；UI 基线带固定视口截图。
4. MySQL 真实环境验证完成，不以 H2 替代生产兼容性结论。
5. 定向 Mutation 冻结种子 100% 杀死，且无“非测试错误冒充杀死”。
6. CI p95 耗时在预算内，无过期例外、无多份 latest、无新增无用测试。

任何一项不满足，维持 changed-only 或回滚候选门禁，不扩大到全量历史代码。

## 10. 可复现命令

```bash
node mango-pmo/tools/check-pmo-preflight.mjs
node mango-pmo/tools/quality-gate.mjs --self-test
node mango-pmo/tools/eval-executable-quality.mjs
node mango-pmo/tools/eval-agent-classification.mjs --validate-only
node mango-pmo/tools/verify-targeted-mutations.mjs
node mango-pmo/tools/quality-baseline.mjs self-test
node mango-pmo/tools/quality-baseline.mjs check
node mango-ui/packages/mango-pmo/scripts/build-package.mjs
node mango-ui/packages/mango-pmo/scripts/check-package.mjs
node mango-ui/packages/mango-cli/src/index.mjs pmo check --project-dir mango-business-starter
mvn -f mango/mango-tools/mango-maven-plugin/pom.xml test
mvn -f mango/pom.xml io.mango.tools.maven.plugin:mango-maven-plugin:1.0.0-SNAPSHOT:quality-gate \
  -Dmango.quality.baseRef=HEAD~1 -Dmango.quality.headRef=HEAD \
  -Dmango.quality.report=.runtime/pmo/maven-quality-gate-final.json
```

## 11. 交付结论

本季度已经解决“规范只有文字、可随意跳过”和“小改动被过度治理”两个结构性问题：规则具备可执行入口，任务具备 L0-L3 最小充分流程，测试证据与基线有明确边界。

最终结论保持克制且可审计：**确定性质量契约通过；changed-only 试点有条件通过；Agent 语义认证与 PMO 全量推广不通过。** 后续是否升级，只由第 9 节的真实实验数据决定。
