# @mango/cli

## 1. 能力定位

`@mango/cli` 是 Mango 项目初始化、业务模块追加、PMO baseline 同步、开发工作区校验和启动编排的命令行入口。主要使用者是业务项目开发者、Mango 维护者和需要生成模板项目的 Agent。

代码事实：

- 包名 `@mango/cli`，bin 命令为 `mango` 和 `mango-cli`。
- 命令入口 `src/index.mjs`。
- 项目模板目录 `templates/full`。
- 业务模块模板目录 `templates/business-module`。
- 版本锁文件 `release-versions.json`。

## 2. 适用场景

- `mango init` 生成 full/custom 业务项目。
- `mango add` 或 `mango module add` 追加业务模块和聚合页面。
- `mango pmo sync` 同步业务 PMO baseline、Agent 入口和兼容脚本。
- `mango validate`、`mango doctor`、`mango plan` 校验开发工作区。
- `mango start`、`mango stop`、`mango status`、`mango logs` 管理本地开发应用。

## 3. 不适用场景

- 不负责运行 Mango 后端服务本身。
- 不替代 Maven、pnpm、Vite 和浏览器端验收。
- 不自动修改业务项目中已有的业务自定义代码。
- 不作为前端运行时依赖引入业务应用。

## 4. 模块边界

CLI 负责模板生成、静态契约检查、工作区命令编排和 PMO baseline 同步。模板内容来自 `mango-business-starter` 和 CLI 内置模板；生成后的业务项目由业务仓库维护。

## 5. 接入方式

安装：

```bash
npm install -g @mango/cli --registry=http://nexus.inner.yunxinbaokeji.com/repository/npm-group/
```

常用命令：

```bash
mango init <project> --preset full --topology monolith
mango add <module>
mango module add <module> --aggregate <name>
mango pmo sync --project-dir <dir> --sync-shell
mango validate
mango doctor
mango plan
mango start
mango status
mango logs <app>
mango stop
mango changelog
```

## 6. 配置项

CLI 向上查找 `mango.dev.json` 作为开发工作区配置。

本地私有配置：

- `.mango/dev-workspace.env`
- `.mango/dev-workspace.local.json`

版本锁来源：

- `release-versions.json`，包含 Maven `mangoBackend` 和 NPM `@mango/*` 包版本。

## 7. 对外接口 / 扩展点

命令面：

- `init <project> --preset full|custom`
- `add <module...>`
- `module add <module> --aggregate <name>`
- `pmo sync --project-dir <dir> [--dry-run] [--write-agents] [--sync-shell]`
- `init-dev`、`validate`、`doctor`、`plan`、`start`、`stop`、`status`、`logs`、`changelog`

模板扩展点：

- `templates/full`
- `templates/business-module`
- `release-versions.json`
- `scripts/check-cli.mjs`
- `scripts/check-release-versions.mjs`

## 8. 数据库 / 初始化数据

本包不包含数据库 migration。数据库结构来自生成项目中的后端模块模板和 Mango 后端平台模块。

## 9. 菜单 / 权限 / 租户

CLI 只生成或同步菜单、权限和 PMO 相关模板文件，不在运行时管理权限和租户。生成后的菜单权限应由业务模块资源清单、authorization migration 或业务 PMO 交付记录维护。

## 10. 验证方式

CLI 自测：

```bash
pnpm -F @mango/cli test
```

该脚本执行 `check:release-versions` 和 `scripts/check-cli.mjs`，会临时生成项目并检查 full/custom 项目、版本、PMO preflight、`mango validate`、`mango plan`、PMO sync 等契约。

## 11. 业务接入最小闭环

full 项目路径：安装 CLI，执行 `mango init <project> --preset full --topology monolith|microservice`，进入生成目录安装依赖，执行 `mango validate`、`mango plan`、`mango start`，再分别做后端和前端构建验收。

custom 项目或已有项目追加模块时使用 `mango module add <module> --aggregate <name>`；full preset 下部分 `mango add` 路径会被保护性拒绝，应根据错误提示改用 module 命令或同步模板。验收断言覆盖：生成项目 PMO preflight 可执行，业务模块 api/core/starter/starter-remote 和前端页面包齐全，菜单 component key 与 resource manifest 对齐。

## 12. 常见问题

- 升级全局 CLI 不会自动改写已有业务项目，需要运行 `mango pmo sync` 或按新模板迁移。
- 修改模板后，同步检查 `release-versions.json` 和 CLI 自测。
- `mango validate` 失败时先检查 `mango.dev.json`、本地 env 和应用路径是否匹配。

## 13. 关联 PMO 规则

- [开发流程规范](../../../mango-pmo/rules/00-dev-flow.md)
- [交付质量门禁](../../../mango-pmo/rules/05-ai-delivery-quality.md)
- [文档资产规范](../../../mango-pmo/rules/06-document-assets.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 14. 历史设计 / 交付记录

- [CHANGELOG](./CHANGELOG.md)
- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
- [Business Starter](../../../mango-business-starter/README.md)
