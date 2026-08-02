# Mango 1.0.30 / 1.0.3x 到 1.0.31 升级指南

## 1. 适用对象

本指南面向已经使用 Mango Maven `1.0.30`、CLI `1.0.95`、PMO `1.3.8`，或混用了其它 `1.0.3x` 后端与前端包的业务项目。目标是把后端、前端、CLI、PMO、starter/template 和文档一次性对齐到同一发布批次。

这次升级覆盖第三方身份提供商与个人设置、消息中心入口、文件 hash 去重恢复、Bootstrap/Runtime 生命周期、Resource typed declarations、Gitea PMO 事件处理以及生成项目回归修复。既有数据库原地升级，不需要重建。

## 2. 目标版本

| 类型 | 目标 |
| --- | --- |
| Maven non-app Reactor、BOM、Parent、docs bundle | `1.0.31` |
| PMO | `@mango/pmo@1.3.9` |
| CLI | `@mango/cli@1.0.96` |
| 前端聚合包 | `@mango/admin@1.0.61` |
| 前端基础闭包 | `@mango/common@1.0.24`、`@mango/grid-layout@1.0.15`、`@mango/grid-widgets@1.0.21`、`@mango/rbac@1.0.22`、`@mango/auth@1.0.24`、`@mango/system@1.0.30`、`@mango/admin-pages@1.0.31`、`@mango/file@1.0.32` |
| 其它受影响前端包 | `@mango/calendar@1.0.32`、`@mango/cms@1.0.21`、`@mango/home@1.0.13`、`@mango/job@1.0.24`、`@mango/link@1.0.18`、`@mango/notice@1.0.36`、`@mango/numgen@1.0.32`、`@mango/payment@1.0.24`、`@mango/site-shell@1.0.11`、`@mango/template@1.0.32`、`@mango/workflow@1.0.38`、`@mango/workflow-business-example@1.0.37` |

`@mango/api-schema@1.0.3`、`@mango/app-runtime@1.0.6`、`@mango/http-client@1.0.0`、`@mango/link-openapi@1.0.4` 和 `@mango/link-page@1.0.7` 在本批次不变。

## 3. 升级前准备

1. 从可回退的业务分支开始，记录当前 commit、Mango Maven/CLI/PMO/npm 版本和目标环境。
2. 备份数据库，并保存 `.mango/`、`mango.config.json`、后端根 POM、前端 `package.json`/`pnpm-lock.yaml`、Bootstrap 审计表和当前应用日志。
3. 停止对 schema、Resource 声明和身份提供商配置的并发变更；确认旧 Runtime 实例及其 generation。
4. 从发布仓库分别回查 Maven `1.0.31`、PMO `1.3.9`、CLI `1.0.96` 和业务实际安装的每个 npm 坐标。任一坐标缺失时不要开始业务升级。
5. 检查自定义身份提供商回调地址、密钥来源、登录页扩展、个人设置菜单、Notice 路由、菜单权限、租户和数据权限边界，形成升级后回归清单。

## 4. 更新 PMO 与 CLI

先用全局 CLI 完成历史项目入口升级，再回到项目内锁定的 CLI：

```bash
npm install -g @mango/cli@1.0.96 --registry "$MANGO_NPM_REGISTRY"
mango pmo upgrade --project-dir . --to 1.3.9 --sync-shell
mango pmo check --project-dir . --locked
```

检查 PMO 升级 diff，重点确认 GitHub/Gitea workflow、frontend page baseline、Gitea terminal edited-event resolver、规则索引、合同和工具均来自 PMO `1.3.9`。不要手工只复制单个 checker。

## 5. 更新后端

继承 `mango-parent` 的项目统一修改根 POM：

```xml
<mango.version>1.0.31</mango.version>
```

使用自有 Parent 的项目导入：

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.mango</groupId>
      <artifactId>mango-bom</artifactId>
      <version>1.0.31</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

同步 `mango.config.json.mangoBackendVersion`，删除业务模块中对旧 Mango patch 版本的单独覆盖。然后执行：

```bash
mvn -f backend/pom.xml clean verify
```

生成项目仍使用固定非 `${revision}` 版本时，先用 CLI 同步当前模板；不要通过共享 SNAPSHOT 或跳过架构/静态门禁绕过 worktree 隔离检查。

## 6. 更新前端

聚合管理端项目至少把 `@mango/admin` 更新到 `1.0.61`；直接消费包的项目按“目标版本”表逐项更新实际依赖。更新项目内 CLI 到 `1.0.96`，重新生成 lockfile并检查没有旧 tuple 残留：

```bash
cd frontend
pnpm install --no-frozen-lockfile --registry "$MANGO_NPM_REGISTRY"
pnpm exec mango pmo check --project-dir .. --locked
pnpm typecheck
pnpm build
```

提交前恢复冻结安装验证：

```bash
pnpm install --frozen-lockfile --registry "$MANGO_NPM_REGISTRY"
```

自定义管理端需要回归 package `style.css`、页面注册、菜单 component key、个人设置和消息中心路由。不要只升级聚合包却保留直接依赖的旧 `@mango/common`、`@mango/admin-pages` 或 `@mango/system`。

## 7. Resource 与 Bootstrap 迁移

旧业务模块若仍使用单一 `resource-manifest.json`，把正式内置资源迁移到 `META-INF/mango/resources/<module>-common-*.json|yml|yaml`，并按 handler 的 `ResourceHandlerSpec` 校验字段。正式启动必需资源使用 `BOOTSTRAP_REQUIRED`；明确允许启动后收敛的对账资源使用 `RUNTIME_EVENTUAL`。

每个 worktree 重新初始化并检查环境：

```bash
cd frontend
pnpm exec mango workspace init
pnpm exec mango workspace doctor
pnpm exec mango dev doctor
```

既有数据库走 rolling 生命周期：

```text
bootstrap plan
bootstrap apply --strategy=rolling
bootstrap verify
runtime
bootstrap finalize
```

首次空库才使用 cold 策略。进入 finalize 前，核对 receipt 的 environment、revision、generation、fingerprint 和 fencing token，并确认旧 generation lease 已排空。finalize 前的候选失败可执行 abort；finalize 开始后保留证据并续跑 finalize，不手工改写审计表。

## 8. 回归验收

至少验证以下业务入口：

- 登录、退出、菜单、按钮权限、租户和未登录 API `401`。
- 头像、实名、手机号、邮箱、密码、主题、第三方授权绑定/解绑与 provider callback。
- 个人设置中的消息中心、接收设置、站内信、公告和登录日志。
- 一个使用真实数据库的 CRUD，文件重复 hash 上传/恢复、预览和删除。
- Bootstrap receipt、Resource 声明拓扑、Runtime readiness、Job/Notice worker 和 Workflow/Flowable 初始化顺序。
- 后端 `clean verify`、可执行 Boot JAR `java -jar`、前端 typecheck/build、独立 Maven consumer 和 BSQL baseline。

记录测试账号/租户标识、数据库名、命令、通过/失败项、日志与截图路径；不要记录密码、token 或 provider secret。

## 9. 失败与回退

| 现象 | 处理 |
| --- | --- |
| 目标包无法从 consume registry 解析 | 停止升级，核对完整 tuple 发布状态；不要切回 workspace link 或本地 publish cache 冒充成功。 |
| `BOOTSTRAP_RECEIPT_MISSING` / fingerprint 或 generation 不一致 | 保留 `.mango`、审计表和日志，停止候选 Runtime，修正源码/制品/环境匹配后重新 plan/verify。 |
| `OLD_RUNTIME_INSTANCES_ACTIVE` | 停止旧 generation 实例并等待 lease 过期，再继续 finalize。 |
| Resource schema/handler 失败 | 修正 typed declaration 或 handler 依赖，不用手工 SQL 跳过同步。 |
| 前端出现重复 Vue、样式缺失或页面 key 404 | 检查精确 package 矩阵、peer dependency、公开 `style.css` 和页面注册；不要只降级一个包。 |
| 新 provider 登录失败 | 核对回调 URL、secret 来源、租户/用户绑定状态和 provider 日志，避免把 secret 写入仓库或验收证据。 |

未进入 finalize 时可以 abort 候选 generation，并把代码、BOM、npm lock 与 PMO lock 一起回退到升级前 commit。不要只回退 Maven jar 或单个 npm 包。

## 10. 关联入口

- [CLI README](../../../mango-ui/packages/mango-cli/README.md)
- [Business Starter](../../../mango-business-starter/README.md)
- [Bootstrap README](../../../mango/mango-infra/mango-infra-bootstrap/README.md)
- [Resource README](../../../mango/mango-platform/mango-resource/README.md)
- [能力地图](../../capabilities/README.md)
- [根 Changelog](../../../CHANGELOG.md)
