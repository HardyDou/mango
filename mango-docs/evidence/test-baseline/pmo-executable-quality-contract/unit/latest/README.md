# PMO 可执行质量契约 UNIT 最新基线

- 受测实现：`be970469d0eab3715880e565e94d1bf756dec4fc`
- PASS 范围：确定性质量门禁、基线保护和定向 Mutation。
- 确定性门禁：48/48 场景、192/192 重复判定符合标签；关键红线 120/120 阻断；合法正例 0 误阻断。
- 定向 Mutation：独立临时 worktree 中将 `Math.ceil` 改为 `Math.floor`，2 个测试中的目标用例按预期失败并被 Surefire XML 证实。

`21.35%` 是旧确定性门禁在 192 次加权判定中的 41 次命中，不是 AI、代码或业务正确率。

受控空白上下文 Agent 分类报告作为补充证据保存在 `agent-classification.json`：候选精确匹配率为 88.10%，每组完成 39/42，结论为 FAIL，不能纳入本基线的 PASS 范围，也不能据此批准 PMO 全量推广。

复现命令：

```bash
node mango-pmo/tools/quality-gate.mjs --self-test
node mango-pmo/tools/eval-executable-quality.mjs
node mango-pmo/tools/verify-targeted-mutations.mjs
node mango-pmo/tools/quality-baseline.mjs self-test
```
