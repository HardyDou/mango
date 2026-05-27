# Mango 产品化 Issue #26 Sprint 6 Mango Initializr 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| S6-001 | Issue #26 #15 | 提供 Mango Initializr | 新增 `create-mango-app` CLI 包 | `mango-ui/packages/create-mango-app` | `node mango-ui/packages/create-mango-app/src/index.mjs --help` | DONE | `mango-ui/packages/create-mango-app/package.json` |
| S6-002 | 用户要求 | 支持类似 Vue init 和 npm create 的启动方式 | 提供 `mango init <project>` bin 入口 | CLI bin | `node mango-ui/packages/create-mango-app/src/index.mjs init /tmp/mango-test --force` | DONE | `mango-ui/packages/create-mango-app/src/index.mjs` |
| S6-003 | Sprint 5 前置 | 基于业务模板生成项目 | CLI 复制 `mango-business-starter` 并替换占位符 | 模板渲染逻辑 | `node mango-ui/packages/create-mango-app/scripts/check-cli.mjs` | DONE | `mango-ui/packages/create-mango-app/src/index.mjs` |
| S6-004 | Issue #26 #10 | 支持 monolith 和 microservice 参数 | CLI 接受 `--topology` 参数，取值为 monolith 或 microservice，并生成 `mango.config.json` | `mango.config.json` | `node mango-ui/packages/create-mango-app/scripts/check-cli.mjs` | DONE | `mango-ui/packages/create-mango-app/src/index.mjs` |
| S6-005 | 交付契约规则 | Initializr 需要可验证 | 新增 CLI 自测脚本生成临时项目并检查关键文件 | `scripts/check-cli.mjs` | `node mango-ui/packages/create-mango-app/scripts/check-cli.mjs` | DONE | `mango-ui/packages/create-mango-app/scripts/check-cli.mjs` |
