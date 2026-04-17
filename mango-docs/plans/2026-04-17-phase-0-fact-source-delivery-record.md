# Phase 0 交付记录

## 范围

- 本次处理模块：事实源文档、`mango-admin-app` 装配叙事、旧命名搜索证据。
- 本次未处理模块：`mango-common`、`mango-infra`、`mango-platform`、`mango-app` 的业务边界与实现重构。

## 完成项

- [x] 已按 Phase 0 交接基线核对 `mango-admin-app` 当前依赖清单，当前 POM 依赖均属于合法或待后续 Phase 复核项。
- [x] 已核对聚合 POM 模块数：`mango-infra` 16 个、`mango-platform` 9 个、`mango-app` 1 个。
- [x] 已搜索源码/POM 中的 `mango-user`、`mango-user-api`、`mango-permission`、`io.mango.user`、`io.mango.permission` 残留，并完成分类。
- [x] 已确认 `mango-rbac` 与根 POM 不再引用 `mango-user-api`，且 `mango-rbac` 专项编译通过。
- [x] 已搜索当前有效 README、索引文档、架构文档中的旧命名，并更新当前事实源文档。
- [x] 已更新 `mango-admin-app` POM description、README、启动类说明和本地公共路径 adapter 说明，使其与 RBAC/Auth/I18n/Org/Area/AI/Captcha 当前装配一致。
- [x] 已更新 `mango/README.md` 模块结构，使其与当前聚合 POM 模块一致。
- [x] 已更新 `mango-docs/index.md` 后端执行入口，使模块级 Phase 计划成为当前后端待执行顺序事实源。
- [x] 已将模块级重构计划纳入 Sprint 15 后续执行跟踪 review 结果。
- [x] 已更新 `mango-docs/mango-architecture-design.md` 和 `mango-docs/mango-backend-architecture-boundary-refactor-master-plan.md`，不再把旧 `mango-user` / `mango-permission` 作为当前有效模块。
- [x] 已输出 gateway 错误命名清单，作为 Phase 5 输入。

## 主动不做项

- [x] 事项：不修改 `mango-tools/mango-maven-plugin` 中使用 `mango-user` 的生成器示例和测试用例。
  原因：这些命中是工具示例/测试输入，不是当前有效后端模块依赖或装配事实；修改生成器模板语义不属于 Phase 0 当前事实源清理范围。
- [x] 事项：不修改 `mango-gateway` 的 `GatewayProperties.userService` 默认值。
  原因：Phase 0 只要求记录 gateway 错误命名清单；gateway 路径与服务名策略属于 Phase 5。
- [x] 事项：不批量修改历史 Sprint 计划文档中的旧命名。
  原因：Phase 0 明确历史 Sprint 文档旧命名不参与当前事实源验收。
- [x] 事项：不启动应用、不调整运行时配置。
  原因：Phase 0 未修改运行时配置，验收命令只要求 compile 与搜索证据。

## 被动适配

- [x] `MangoAdminAppApplication` 类注释从 user/permission 旧叙事调整为 RBAC/Auth/I18n/Org/Area/AI/Captcha 当前装配叙事。
- [x] `LocalSysPublicPathApi` 注释从 permission service 调整为 RBAC public path API，代码行为未变。
- [x] `mango-admin-app` README 将依赖说明同步到当前 POM 的 local starter 列表，并记录 `spring-boot-starter-websocket` 为 messaging 后续复核项。

## 验证结果

- `mvn -q -DskipTests compile`：通过。
- `mvn -q -pl mango-platform/mango-rbac -am -DskipTests compile`：通过，确认 `mango-rbac` 删除 `mango-user-api` 后不导致编译失败。
- `mvn test`：未执行；Phase 0 验收命令未要求。
- 其它搜索/检查命令：
  - `rg -n "<module>" mango/mango-infra/pom.xml mango/mango-platform/pom.xml mango/mango-app/pom.xml`：确认 infra 16 个、platform 9 个、app 1 个。
  - `rg -n "mango-user-api|mango-user|io\\.mango\\.user" mango/mango-platform/mango-rbac mango/pom.xml --glob '!**/target/**'`：无命中。
  - `rg -n "mango-user|mango-permission|io\\.mango\\.user|UserAutoConfiguration|io\\.mango\\.permission" mango --glob '!**/target/**' --glob 'pom.xml' --glob '*.java' --glob '*.xml' --glob '*.yml' --glob '*.yaml' --glob '*.properties'`：未发现当前 POM 直接依赖旧 artifact；命中仅为 `mango-tools` 示例/测试与 gateway 默认服务名。
  - `rg -n "mango-user|mango-permission|io\\.mango\\.user|user, permission|UserAutoConfiguration|PermissionAutoConfiguration|io\\.mango\\.permission" README.md mango/README.md mango-docs/index.md mango-docs/README.md mango-docs/mango-architecture-design.md mango-docs/mango-backend-architecture-boundary-refactor-master-plan.md mango/**/README.md --glob '!**/target/**'`：当前有效文档中只剩总计划里明确标注为历史旧叙述的说明，不再作为当前模块事实源。
  - `rg -n "mango-user|mango-permission|io\\.mango\\.user|UserAutoConfiguration|PermissionAutoConfiguration|io\\.mango\\.permission" mango/mango-app/mango-admin-app --glob '!**/target/**'`：无命中。
- 人工确认人或执行 agent：Codex。

## 遗留问题

- [x] `mango/mango-infra/mango-gateway/mango-gateway-core/src/main/java/io/mango/gateway/core/config/GatewayProperties.java` 中 `userService` 默认值仍为 `mango-user-starter`，登记为 Phase 5 gateway 错误命名输入。
- [x] `mango-tools/mango-maven-plugin` 示例/测试仍使用 `mango-user` 作为生成器样例名，当前不阻塞 Phase 1；如后续规则要求工具模板也避开旧名，应在工具链 Phase 单独处理。
- [x] Sprint 15 已追加后续执行跟踪区，记录模块级重构计划 review 结果；后续若 PMO 要求独立 Sprint 编号，可再从该跟踪项拆出。

## 下一 Phase 前置条件

- [x] Phase 1 可以以当前事实源进入 `mango-common` 收敛：当前 POM、admin-app 装配叙事、README、索引文档和架构文档中不存在会阻碍 `mango-common` 的旧模块事实源。
- [x] Phase 1 仍需遵守 Phase -1 决策：`SysUser` / 用户实体本轮暂留 `mango-rbac`，不进入 `mango-common`。
- [x] Phase 5 开始前必须处理或重新确认 gateway 默认服务名中的 `mango-user-starter` 命名问题。
