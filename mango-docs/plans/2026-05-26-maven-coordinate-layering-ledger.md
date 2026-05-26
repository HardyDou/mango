# 2026-05-26 Maven 坐标分层调整台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|----|------|------|----------|--------|----------|------|----------|
| MCL-001 | 用户要求 | `mango-infra-kv` 作为 Maven 发布路径中的能力层，下面再区分 `api/core/starter` | 用 `groupId=io.mango.infra.kv` 表达能力层，保留完整 `artifactId` | `mango-infra-kv` 及子模块 POM | Maven validate 和 POM 检查 | DONE | `mvn -f mango/pom.xml validate -DskipTests` 成功；`mvn -f mango/pom.xml -pl mango-infra/mango-infra-kv/mango-infra-kv-api -am test` 成功 |
| MCL-002 | 用户要求 | 统一调整其它模块路径，发布到 Maven 仓库后分层明确 | 按顶层领域和能力模块统一生成 groupId 映射 | 全仓 Mango POM 内部坐标 | Maven validate 和 groupId 扫描 | DONE | `mvn -f mango/pom.xml compile -DskipTests` 成功，128 个模块编译通过 |
| MCL-003 | PMO 模块分层规范 | 坐标调整不得破坏模块边界校验 | Mango Maven Plugin 识别 `io.mango.*` 内部模块坐标 | `CheckMojo.java` | 插件模块测试或编译验证 | DONE | `mvn -f mango/pom.xml -pl mango-platform/mango-identity/mango-identity-api -am test` 成功，插件测试 69 个通过；`mvn -f mango/pom.xml mango:check -Drule=dependency -DskipTests` 成功 |
| MCL-004 | 交付契约 | 变更需要可验证并记录风险 | 执行 Maven/PMO 验证，记录未验证项 | 验证结果和台账 | `delivery-contract-check` verify | DONE | `node mango-pmo/tools/delivery-contract-check.mjs --design mango-docs/plans/2026-05-26-maven-coordinate-layering-plan.md --ledger mango-docs/plans/2026-05-26-maven-coordinate-layering-ledger.md --mode verify` |
