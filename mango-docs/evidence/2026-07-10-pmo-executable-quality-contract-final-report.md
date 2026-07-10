# PMO 可执行质量契约最终实验报告

> 实验日期：2026-07-10  
> 受测实现提交：`6895a5295`  
> 能力：`pmo-executable-quality-contract`  
> 总结论：**PASS（可验证范围）**

## 1. 结论

本次优化已经从文字建议落到规则、schema、可执行工具、反例集、CI、npm PMO 包和业务 starter。冻结的已知问题在空白上下文重复实验中达到 100% 检出，合法正例误报为 0；真实 Java 样本、Maven 测试、Mutation、PMO CLI、发布包和 starter 同步全部通过。

“PASS”表示本次冻结规则和真实样本达到既定阈值，不表示可以数学证明未来未知业务语义永不漏检。未来发现的新逃逸方式必须先加入反例集，再允许修改门禁。

## 2. 交付结果

| 交付项 | 结果 |
|---|---|
| 正式测试分类 | 只保留 `UNIT`、`API`、`UI`；`E2E` 与 `UI` 同义；静态 Review、Mutation、截图为验证手段 |
| 风险选择 | `R0` 静态 Review，`R1` 增加 UNIT，`R2` 增加 API，`R3` 增加 UI |
| 无用测试 | 常量自证、getter/setter、只断言不抛异常/非空/调用次数进入阻断规则 |
| Mock 防逃逸 | 被测目标、受保护证明节点、API 内部 Service/Mapper、UI 自有 API 替身进入阻断规则 |
| Java 门禁 | Controller/Mapper、参数校验、Service 事务、危险 SQL、api/core/starter/starter-remote 依赖方向可执行检查 |
| Web 门禁 | 页面/表单语义锚点、隐藏伪装、自有 API Mock、Element Plus 内部选择器、固定等待、force、DOM 顺序依赖可执行检查 |
| Preflight | 未知 role、phase、参数和缺少 task 均 fail-closed；输出完整规则 SHA-256 指纹 |
| 基线 | 普通 check 只读；失败报告、缺少批准、并存旧版本均不能提升；promote 原子替换唯一 `latest` |
| CI | PR 强制执行门禁自测、空白 A/B、changed-only、基线、治理和 PMO 包检查 |
| 实际消费 | `@mango/pmo@1.1.0` 构建 67 个 baseline 文件；`mango-business-starter` 为 0 missing、0 changed、0 extra |

完整的 36 项问题、发生概率和设计决策见 [`2026-07-10-pmo-executable-quality-contract-design.md`](../designs/2026-07-10-pmo-executable-quality-contract-design.md)。

## 3. 空白上下文 A/B 实验

### 3.1 隔离方式

- 每次运行创建独立临时目录。
- `HOME` 和 `CODEX_HOME` 指向全新空目录。
- 无历史会话、无用户级 Skill 和记忆。
- 固定 UTC、禁用代理，current/candidate 使用完全相同的文件输入。
- 门禁进程只读取物化后的场景文件；生成者自述不参与评分。
- 普通场景重复 3 次，关键红线重复 5 次。

### 3.2 规模

- 场景：48 个，超过 30 个最低要求。
- Candidate 独立运行：192 次。
- 关键红线运行：120 次。
- 合法正例运行：36 次。
- 覆盖无用测试、错误 Mock、API 入口绕过、Java 分层、危险 SQL、前端自有 API Mock、页面伪装、未知测试类型、过期例外和多份基线。

### 3.3 结果

| 指标 | Current | Candidate | 阈值 | 结论 |
|---|---:|---:|---:|---|
| 总体正确率 | 21.35% | 100.00% | ≥ 95% | PASS |
| 关键红线检出率 | 4.17% | 100.00% | 100% | PASS |
| 合法正例误报 | 0 | 0 | 0 | PASS |
| 重复结论一致率 | 100.00% | 100.00% | ≥ 95% | PASS |
| 相对提升 | - | 78.65 个百分点 | ≥ 30 个百分点 | PASS |
| 指定规则命中 | 未覆盖 | 48/48 场景符合预期 | 100% | PASS |

机器报告：[`evaluation.json`](test-baseline/pmo-executable-quality-contract/unit/latest/evaluation.json)。  
逐场景摘要：[`evaluation.md`](test-baseline/pmo-executable-quality-contract/unit/latest/evaluation.md)。

## 4. 真实项目验证

### 4.1 关键业务/技术 UNIT 与 Mutation

对象：`WorkflowApprovalThreshold`。

1. 原实现执行 `WorkflowApprovalThresholdTest`：2 个测试通过。
2. 临时把 `Math.ceil` 改为 `Math.floor`：同一测试出现 `expected: 2 but was: 1`，Maven 非零退出。
3. 恢复原实现后复跑：2 个测试再次通过。

结论：测试不是常量或实现复述，确实能够杀死关键阈值计算错误；本次指定 Mutation 杀死率为 100%。

### 4.2 Java 结构与真实持久化样本

质量门禁显式扫描 8 个真实文件：工作流 `api/core/starter/starter-remote` POM、阈值生产/测试代码、Payment Service 和集成测试，结果 0 问题。

`PaymentChannelContractServiceImplIntegrationTest` 使用 Spring、真实 Mapper 和隔离 H2 内存库执行：7 个测试通过，0 失败。门禁没有把合法的 test-scope starter 依赖误判为生产依赖。

现有六个重点后端域的 Mockito 盘点结果为 0 BLOCK、81 WARN。WARN 主要是 Mapper 替身测试，其结论只能覆盖局部决策，不能声称覆盖真实持久化；新增或修改时由 changed-only 门禁重新判定。

### 4.3 UI/E2E 门禁与存量债务

本次 PMO/CLI 能力风险为 `R1`，没有产品页面或用户浏览器流程，因此正式基线不伪造 UI/E2E 通过，也不提交无业务含义截图。UI 规则使用 13 个正反场景验证，包含自有 API Mock 必须失败、第三方外部替身必须通过、截图不能代替业务断言等情况，结果全部符合预期。

对现有 58 个真实 E2E spec 执行报告模式扫描，发现 144 个存量问题：

| 规则 | 数量 | 说明 |
|---|---:|---|
| `PQT-UI-001` | 47 | Mango 自有 API 被 route/fulfill；应改成真实 UI/E2E，或重新归类为非 E2E 测试 |
| `PQT-UI-002` | 42 | 直接依赖 `.el-*` 内部 class |
| `PQT-UI-003` | 5 | 固定等待 `waitForTimeout` |
| `PQT-UI-004` | 4 | `force: true` 绕过真实可交互性 |
| `PQT-UI-005` | 46 | `nth/first/last` 等 DOM 顺序依赖 |

其中 `workflow-management.spec.ts` 被稳定识别出 Element Plus 内部 class 和 DOM 顺序依赖。以上为历史债务，不冒充已修复；CI 对新增和修改文件硬阻断，历史文件被触碰时必须迁移或提供有效的限期例外。

## 5. 发布包与业务应用验证

| 检查 | 结果 |
|---|---|
| PMO preflight 自测 | 20/20 PASS；非法 role、phase、未知参数和缺少 task 均失败 |
| 质量门禁自测 | 48/48 PASS |
| 基线保护自测 | 只读 check、审批、PASS 报告、原子 latest、旧版本拒绝全部 PASS |
| `@mango/pmo` build/check | `1.1.0`，67 个文件，PASS |
| 发布包内质量门禁 | 48/48 PASS |
| 业务 starter PMO check | 0 missing、0 changed、0 extra |
| 业务 starter 内质量门禁 | 48/48 PASS |
| 治理检查 | governance intent、module README、README 源事实、5 个业务指南、68 个变更文件能力文档检查全部 PASS |
| 最终差异质量契约 | 68 个文件，0 问题，PASS |

## 6. 正式基线

- UNIT 最新基线：[`unit/latest`](test-baseline/pmo-executable-quality-contract/unit/latest/README.md)
- API/CLI 最新基线：[`api/latest`](test-baseline/pmo-executable-quality-contract/api/latest/README.md)
- 机器质量契约：[`quality-contract.json`](test-baseline/pmo-executable-quality-contract/api/latest/quality-contract.json)

当前树每个已要求的能力/类型只有一个 `latest`。过程报告保存在 `.runtime/pmo`，不提交。普通测试不能更新正式基线；提升必须经过独立 `quality-baseline.mjs promote`。

## 7. 复现命令

```bash
node mango-pmo/tools/quality-gate.mjs --self-test
node mango-pmo/tools/eval-executable-quality.mjs
node mango-pmo/tools/check-pmo-preflight.mjs
node mango-pmo/tools/quality-baseline.mjs self-test
node mango-pmo/tools/quality-baseline.mjs check
node mango-ui/packages/mango-pmo/scripts/build-package.mjs
node mango-ui/packages/mango-pmo/scripts/check-package.mjs
node mango-ui/packages/mango-cli/src/index.mjs pmo check --project-dir mango-business-starter
node mango-business-starter/business-pmo/mango-baseline/tools/quality-gate.mjs --self-test
```

真实 Java 命令：

```bash
cd mango
mvn -pl mango-platform/mango-workflow/mango-workflow-core -am -Dtest=WorkflowApprovalThresholdTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl mango-platform/mango-payment/mango-payment-core -am -Dtest=PaymentChannelContractServiceImplIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test
```

## 8. 最终判定

本次候选 PMO 达到替换现行执行方式的实验阈值：冻结红线 100% 检出、合法正例 0 误报、真实项目样本通过、发布包和 starter 一致、CI 已接入、基线不可被普通测试静默改写。

仍需按 changed-only 策略逐步清理的历史债务是 81 个后端 Mock 警告和 144 个 UI/E2E 结构问题。这些债务已经可见、可定位且不会再对新增代码放行，但不应被表述为本次已经全部修复。
