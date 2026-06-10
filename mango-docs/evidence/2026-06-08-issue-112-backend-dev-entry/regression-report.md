# Issue #112 业务项目后端开发启动入口回归报告

## 1. 目标

- 生成项目只推荐 `scripts/dev-workspace.sh backend` 作为后端开发启动入口。
- 脚本封装多模块 Maven 启动流程，避免开发者手写 `mvn -pl ...`、`mvn -f backend/app/pom.xml ...` 或使用 `java -jar` 作为开发启动方式。
- 启动失败时输出可操作诊断。

## 2. 改动范围

- 新增 `mango-ui/packages/mango-cli/templates/full/scripts/dev-workspace.sh`。
- 保留 `scripts/backend-dev.sh` 作为兼容 wrapper，转发到 `scripts/dev-workspace.sh backend`。
- CLI 生成后为 `scripts/dev-workspace.sh` 设置可执行权限，并在 Next steps 中只展示新入口。
- 更新 full 模板 `README.md`、`AGENTS.md` 和 `mango-business-starter/README.md` 的开发启动说明。
- 更新 `mango-ui/packages/mango-cli/scripts/check-cli.mjs`，固化生成项目入口、权限、诊断和 wrapper 断言。

## 3. 验证命令

```bash
pnpm --dir mango-ui/packages/mango-cli test
```

生成项目验证：

```bash
node ../../mango-ui/packages/mango-cli/src/index.mjs init issue112-acceptance \
  --preset full \
  --topology monolith \
  --package com.example.issue112 \
  --group-id com.example \
  --force

scripts/dev-workspace.sh init
scripts/dev-workspace.sh print
```

端口占用诊断验证：

```bash
python3 -m http.server 5555 --bind 127.0.0.1
scripts/dev-workspace.sh backend
```

Maven 失败诊断验证：

```bash
PATH="<stub-mvn-dir>:$PATH" MANGO_BACKEND_PORT=5566 MANGO_DB_AUTO_CREATE=false scripts/dev-workspace.sh backend
```

文档扫描：

```bash
rg -n "scripts/backend-dev\\.sh|java -jar|mvn -f backend/app|mvn -pl|spring-boot:run" \
  mango-ui/packages/mango-cli/templates/full \
  mango-business-starter \
  mango-ui/packages/mango-cli/src/index.mjs \
  mango-ui/packages/mango-cli/scripts/check-cli.mjs
```

## 4. 验证结果

- CLI full/custom/add/module 回归通过。
- 生成项目 Next steps 只展示 `scripts/dev-workspace.sh backend`。
- 生成项目 `scripts/dev-workspace.sh init/print` 通过，默认数据库名为 `issue112_acceptance`。
- `scripts/backend-dev.sh` 仅作为兼容 wrapper，执行时转发到新入口。
- 端口占用时输出 `Backend port 5555 is already in use` 和修改 `.mango/dev-workspace.env` 的提示。
- 模拟 Maven 失败时覆盖数据库连接、Flyway、mainClass、Maven plugin goal 和依赖解析诊断。

## 5. UI/证据

- 截图：`backend-dev-entry-validation.png`

## 6. 风险与未验证项

- 未连接真实 MySQL 和 Maven 仓库执行完整 `spring-boot:run` 到健康检查；本次验证聚焦模板生成、入口唯一性和失败诊断。
- `java -jar` 仍会在 README 中以“部署形态，不用于本地开发”的方式出现。
- `scripts/backend-dev.sh` 为兼容旧项目保留，不作为推荐开发入口。
