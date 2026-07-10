# PMO 可执行质量契约 API 最新基线

本能力的真实入口是 PMO/CLI 命令；没有浏览器页面。API 基线从 CLI 入口验证参数 fail-closed、规则路由、门禁报告、发布包和业务 starter 消费链路。

复现命令：

```bash
node mango-pmo/tools/check-pmo-preflight.mjs
node mango-pmo/tools/quality-gate.mjs --self-test
node mango-ui/packages/mango-pmo/scripts/build-package.mjs
node mango-ui/packages/mango-pmo/scripts/check-package.mjs
node mango-ui/packages/mango-cli/src/index.mjs pmo check --project-dir mango-business-starter
node mango-business-starter/business-pmo/mango-baseline/tools/quality-gate.mjs --self-test
node mango-pmo/tools/quality-baseline.mjs check
```

真实 Java 样本同时验证工作流 `api/core/starter/starter-remote` POM、关键规则单测、Payment Service/Mapper/H2 集成测试。真实工作流 UI 脚本的存量问题不计为本能力通过项，单独记录在最终实验报告。
