# PMO 可执行质量契约 UNIT 最新基线

- 能力：`pmo-executable-quality-contract`
- 受测实现提交：`29176bf17`
- 环境：macOS，Node.js，Java 21，Maven；隔离评估固定 UTC、空 `HOME`、空 `CODEX_HOME`、禁用网络代理。
- 数据：`mango-pmo/fixtures/executable-quality/cases.json` 的 48 个冻结正反场景。

复现命令：

```bash
node mango-pmo/tools/quality-gate.mjs --self-test
node mango-pmo/tools/eval-executable-quality.mjs
cd mango
mvn -pl mango-platform/mango-workflow/mango-workflow-core -am -Dtest=WorkflowApprovalThresholdTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Mutation 验证把 `WorkflowApprovalThreshold` 的 `Math.ceil` 临时替换为 `Math.floor`：同一测试出现 `expected: 2 but was: 1` 并非零退出；恢复生产实现后 2/2 测试再次通过。

基线工具自测还验证：`check` 只读、失败报告不能提升、缺少批准人不能提升、合法提升原子替换 `latest`、并存旧版本会失败。

机器明细见 `evaluation.json`，便于按场景和重复轮次复核；可读摘要见 `evaluation.md`。
