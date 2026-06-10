# Issue #94 回归测试报告

- 日期：2026-06-08
- 分支：feature/issue-94-seed-starter
- Issue：#94 企业项目后端 app 编译依赖缺少 mango-seed-starter
- 范围：mango-cli full 企业项目后端依赖生成、CLI 回归检查、生成项目后端消费验证

## 改动验证

默认 full 企业项目不再把可选的 `io.mango.platform.seed:mango-seed-starter` 写入：

- `backend/pom.xml` 的 dependencyManagement
- `backend/app/pom.xml` 的 app dependencies

默认 app 仍保留 `io.mango:mango-admin-starter`，seed 配置继续在 `application.yml` 中保持关闭态；如业务项目需要官方 seed 能力，需要显式添加 `mango-seed-starter` 依赖后再开启 `MANGO_SEED_ENABLED=true`。

## 自动化验证

```bash
pnpm --dir mango-ui/packages/mango-cli test
```

结果：通过。`mango-cli full/custom/add/module checks passed`。CLI 断言已覆盖默认 full POM 不包含 `mango-seed-starter`。

```bash
mvn install -DskipTests
```

结果：通过。Mango 全仓 154 个 reactor 模块本地安装成功，用于生成企业项目消费 `1.0.0-SNAPSHOT`。

```bash
node mango-ui/packages/mango-cli/src/index.mjs init issue94-full-app \
  --preset full --topology monolith \
  --package com.example.issue94 --group-id com.example \
  --mango-version 1.0.0-SNAPSHOT --force
node mango-ui/packages/mango-cli/src/index.mjs module add procurement \
  --module-name 采购管理 \
  --aggregate purchase-order \
  --aggregate-name 采购订单 \
  --project-dir .runtime/projects/issue-94-seed-starter/issue94-full-app
```

结果：通过。生成 full 企业项目并添加采购 CRUD 模块。

```bash
rg -n "mango-seed-starter|mango-admin-starter" backend/pom.xml backend/app/pom.xml
```

结果：通过。只命中 `mango-admin-starter`，未命中 `mango-seed-starter`。

```bash
mvn -f backend/pom.xml -pl modules/procurement/procurement-starter -am -DskipTests compile
```

结果：通过。采购业务模块链路编译成功。

```bash
mvn -f backend/pom.xml -DskipTests compile
```

结果：通过。完整生成后端 app 编译成功，覆盖 #94 复现路径中的关键失败点。

## 截图

- `generated-backend-pom.png`：生成 full 企业项目后端 POM 识别截图，证明默认只依赖 `mango-admin-starter`。
- `backend-compile-success.png`：完整生成后端 `mvn -f backend/pom.xml -DskipTests compile` 成功截图。

## 回归结论

- full 企业项目默认后端 app 不再强制解析 `mango-seed-starter`。
- 企业项目添加采购 CRUD 模块后，业务模块和完整后端 app 均可编译。
- seed 作为可选启动 seed 能力保留，不作为默认企业项目编译前置依赖。

## 未覆盖项

- 未发布新的 `@mango/cli` npm 版本，也未验证私服发布物料；本轮只完成源码修复和本地生成消费验证。
- 未启动完整生成项目前后端做浏览器联调；#94 的验收重点是后端 app Maven compile 依赖解析。
- `mvn install -DskipTests` 跳过测试，仅用于本地 SNAPSHOT 物料安装；功能测试由 CLI 回归和生成项目编译覆盖。
