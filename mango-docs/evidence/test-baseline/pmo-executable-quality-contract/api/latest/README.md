# PMO 可执行质量契约 API 最新基线

本能力的实际入口是 PMO CLI 和 Maven goal，没有产品浏览器页面。本基线验证参数 fail-closed、L0-L3 任务分级、Maven 主链路、发布包和业务 starter 消费。

- 4px 按钮纯布局调整：L0；无需专用 worktree、详细计划、正式 UI/E2E、截图、基线和长报告，只做静态 Review、受影响页面快速 smoke、一行说明。
- “一行权限显隐调整”：L3；不得伪装成微任务，要求完整证明路径和受影响用户流程 UI/E2E。
- Preflight 23/23；Maven 插件 151 个测试通过；全坐标质量门禁扫描 95 个文件、0 问题。
- `@mango/pmo@1.1.0` 构建 71 个文件，业务 starter 为 0 missing、0 changed、0 extra。
- Payment 集成样本 7/7，但数据库为 H2，仅证明 Spring/Mapper/持久化装配，不声称 MySQL 生产等价。
- PMO/CLI 无产品页面，因此没有伪造 UI/E2E 截图或 UI 通过结论。

复现命令：

```bash
node mango-pmo/tools/check-pmo-preflight.mjs
mvn -f mango/mango-tools/mango-maven-plugin/pom.xml test
mvn -f mango/pom.xml io.mango.tools.maven.plugin:mango-maven-plugin:1.0.0-SNAPSHOT:quality-gate -Dmango.quality.baseRef=HEAD~1 -Dmango.quality.headRef=HEAD -Dmango.quality.report=.runtime/pmo/maven-quality-gate-final.json
node mango-ui/packages/mango-pmo/scripts/build-package.mjs
node mango-ui/packages/mango-pmo/scripts/check-package.mjs
node mango-ui/packages/mango-cli/src/index.mjs pmo check --project-dir mango-business-starter
```
