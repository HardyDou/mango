# PMO 可执行质量契约实施计划

> 设计来源：[`2026-07-10-pmo-executable-quality-contract-design.md`](../designs/2026-07-10-pmo-executable-quality-contract-design.md)
>
> 工作分支：`feature/pmo-executable-quality-contract`

## 交付批次

### 批次一：规范、契约与 fail-closed

- 新增 `mango-pmo/rules/10-executable-quality-contract.md`，集中维护测试分类、风险义务、证明路径、Mock 边界、基线和例外规则。
- 新增 `mango-pmo/schemas/quality-contract.schema.json`，提供机器可校验的契约结构。
- 更新 `mango-pmo/rules/index.json`、后端测试、前端测试和测试自动化流程，只链接新的唯一规则源。
- 修改 `mango-pmo/tools/pmo-preflight.mjs`，非法 role、phase、未知参数和空关键字段全部 fail-closed，并输出规则指纹。
- 扩充 `mango-pmo/tools/check-pmo-preflight.mjs` 正反用例。

验证：preflight 既有路由用例全部通过；非法 role、phase、未知参数、缺少 task 必须非零退出；同一规则树指纹稳定。

### 批次二：统一质量门禁

- 新增 `mango-pmo/tools/lib/quality-analyzer.mjs`，对契约、Java、Java 测试、前端页面、UI/E2E 和基线资产执行独立检查。
- 新增 `mango-pmo/tools/quality-contract.mjs`，从 Git diff 自动计算风险原因和最低验证义务。
- 新增 `mango-pmo/tools/quality-gate.mjs`，支持 changed-only、显式文件、契约、JSON 报告与严格失败。
- 新增 `mango-pmo/tools/quality-baseline.mjs`，分离 check 与 promote，并保证当前树每个能力/类型只有一个 `latest`。

验证：所有规则均有稳定 rule id、文件位置、原因和修复建议；正例零误报；扫描结果不能由测试内变量名或注释改变。

### 批次三：隔离反例与 A/B 评估

- 新增 `mango-pmo/fixtures/executable-quality/cases.json`，覆盖无用测试、错误 Mock、入口绕过、前端自有 API 拦截、Java 分层、页面伪装、基线和例外。
- 新增 `mango-pmo/tools/eval-executable-quality.mjs`，将每个场景物化到独立临时目录，使用空 `HOME`、空 `CODEX_HOME`、无历史数据执行 current/candidate 两组。
- 普通场景重复三次，关键红线重复五次，生成机器可读和 Markdown 报告。

验证：不少于 30 个场景；关键红线检出率 100%；合法正例误报率 0%；总体正确率不低于 95%；相对 current 提升至少 30 个百分点；重复结论一致率不低于 95%。

### 批次四：构建链路与发布包

- 更新 `.github/workflows/pmo-doc-check.yml`，强制执行 preflight 自测、质量门禁自测、隔离 A/B 评估和 changed-only 门禁。
- 更新 `mango-ui/package.json`，提供统一前端质量脚本。
- 生成并检查 `@mango/pmo` 发布基线，验证源码与发布包包含相同规则、schema、工具和 fixtures。
- 更新必要的 PMO README 使用入口，不复制长期规则。

验证：CI 命令在干净 checkout 中可执行；发布包检查通过；删除或篡改新资产时检查失败。

### 批次五：真实项目纵向样本与报告

- 对 `WorkflowApprovalThreshold` 运行关键边界单测，并执行定向源码 Mutation，证明断言能发现比较符错误。
- 对工作流 API/Controller/Service/Mapper 真实代码执行 Java 边界门禁；报告存量与新增问题分离结果。
- 对现有工作流 UI/E2E 脚本执行自有 API Mock、CSS 选择器、固定等待、`force` 和业务断言检查。
- 生成 `mango-docs/evidence/test-baseline/pmo-executable-quality-contract/{unit,api,ui}/latest` 标准交付物；临时过程数据只放 `.runtime/pmo`。

验证：工具自测、A/B 评估、真实 Maven 测试、前端静态门禁、基线检查和治理检查全部提供可复现命令与退出码；未执行项必须在报告中给出明确环境证据，不能写成通过。

## 提交策略

每个批次独立提交。任何批次失败时只回滚该批次，不通过降低阈值、更新预期或删除反例换取通过。
