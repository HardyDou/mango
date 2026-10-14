# 标准交付记录

## 隐藏菜单路由可访问

## 1. 元数据

- 任务 ID：hidden-menu-route
- 交付模式：STANDARD
- 需求影响：L2 - 改变当前用户菜单接口返回范围和管理端路由可用性
- 方案风险：L2 - 涉及授权菜单服务、前端路由装配和已有隐藏权限菜单兼容行为
- 最终风险：L2
- 工作区决策：CREATE - `/Users/hardy/Work/mango-hidden-menu-route`
- 评审选择：AGENT_ONLY；用户未要求外部同行评审

## 2. 目标与范围

- 目标：菜单配置 `visible=0` 时不显示在导航中，但已授权且具备页面运行信息的菜单仍返回给前端并可作为路由访问。
- 成功条件：用户菜单接口返回隐藏页面菜单；前端保留 path/component 并标记 `meta.isHide=true`；隐藏权限资源仍不作为页面路由返回；访问隐藏路由时最近的可见祖先菜单高亮。
- 处理范围：Authorization 用户菜单筛选、RBAC 菜单表单和列表的导航显示配置、Admin Shell 菜单契约测试、模块 README 说明。
- 不处理范围：角色权限模型、API 权限校验、数据库结构、页面组件注册和浏览器真实环境部署。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| R-01 | `/authorization/menus/user` | 启用菜单、角色已授权、`visible=0`、`menuType=2`、存在 path/component | 返回隐藏页面菜单并保留 `visible=0` | 菜单接口缺失该路由时前端无法访问 | 后端集成测试包含隐藏页面菜单 |
| R-02 | `/authorization/menus/user` | 隐藏菜单没有页面 path/component，仅承载基础权限 | 不作为页面路由返回 | 不应把权限资源误装配为页面 | 后端测试仍排除隐藏权限菜单 |
| R-03 | RBAC 菜单管理 / Admin Shell | 页面菜单设置 `visible=0` 并由用户获得授权 | 管理表单保存隐藏值；路由保留，导航元数据为 `isHide=true`；最近的可见祖先高亮 | 菜单表单无法设置隐藏值、祖先无法定位或页面 key 不匹配时无法完成验收 | 表单提供“导航显示=隐藏”；前端测试确认 path/component 保留、导航隐藏和祖先高亮 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D-01 | R-01/R-02 | Authorization 只对已授权的可运行页面菜单放行 `visible=0`；无页面运行信息的隐藏菜单继续作为权限资源过滤 | `MenuService.listUserMenus`、`resolveAuthorizedMenuIds` | 恢复隐藏菜单过滤条件 |
| D-02 | R-03 | 复用现有 Shell `visible -> meta.isHide` 转换，不新增前端路由协议 | `menuHost.ts`、Admin Shell 菜单测试 | 回退后端变更并保留现有前端逻辑 |
| D-03 | R-01/R-03 | 不修改数据库结构和权限判断，页面 API 仍沿用既有访问控制 | Authorization README、Admin Shell README | 回退代码与说明 |
| D-04 | R-03 | 复用后端已有 `visible` 字段，在 RBAC 页面菜单/目录表单增加“导航显示”单选并在列表显示“显示/隐藏”；按钮不展示该配置 | `mango-ui/packages/rbac/src/views/menu/index.vue`、RBAC README | 回退表单字段，后端兼容既有 `visible` 值 |
| D-05 | R-03 | 导航过滤后为可见祖先保留隐藏后代路径索引，按菜单树关系计算侧边栏高亮，并显式高亮对应的多级子菜单标题，不依赖 path 前缀 | `mango-ui/packages/admin-shell/src/layout/component/aside.vue`、`layout/index.vue`、`navMenu/activeMenuPath.ts`、`navMenu/vertical.vue`、`navMenu/subItem.vue` | 回退到路径前缀匹配；隐藏路由仍可访问 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---:|---|---|
| I-01 | D-01 | 1 | `mango/mango-platform/mango-authorization/mango-authorization-core/src/main/java/io/mango/authorization/core/service/impl/MenuService.java` | 已授权隐藏页面菜单可返回，隐藏权限资源仍过滤 |
| I-02 | D-01 | 2 | `mango/mango-platform/mango-authorization/mango-authorization-core/src/test/java/io/mango/authorization/core/service/impl/MenuServiceImplIntegrationTest.java` | 集成测试覆盖返回与过滤边界 |
| I-03 | D-02 | 3 | `mango-ui/packages/admin-shell/src/__tests__/menuHost.spec.ts` | 测试覆盖路由保留和导航隐藏 |
| I-04 | D-03 | 4 | Authorization/Admin Shell README | 使用说明与接口语义一致 |
| I-05 | D-04 | 5 | `mango-ui/packages/rbac/src/views/menu/index.vue`、`mango-ui/packages/rbac/README.md` | 管理员可在菜单表单设置显示或隐藏，并在列表回看结果 |
| I-06 | D-05 | 6 | `mango-ui/packages/admin-shell/src/layout/component/aside.vue`、`layout/index.vue`、`navMenu/activeMenuPath.ts`、`navMenu/vertical.vue`、`navMenu/subItem.vue`、`activeMenuPath.spec.ts` | 隐藏路由访问时最近可见祖先菜单展开并高亮 |
| I-07 | D-01 | 7 | `mango/mango-platform/mango-authorization/mango-authorization-core/src/main/java/io/mango/authorization/core/service/impl/MenuService.java` | 清偿该文件存量 checkstyle/spotbugs 问题，使 `no-new-violations` 门禁不再把 main 遗留的基线漂移误判为本次新增违规；`updateMenu` 非空字段写入抽取为 `applyMenuUpdates`/`setIfNotNull`，魔法数 `3` 提取为 `MENU_TYPE_BUTTON`，两处 `parentId` 三元表达式改为 `Objects.requireNonNullElse`，行为等价 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| R-01/R-02 | 后端集成测试 | `mvn -f mango/pom.xml -pl :mango-authorization-api,:mango-authorization-core -am -Dtest=MenuServiceImplIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS，5 tests，0 failures | Surefire `mango/mango-platform/mango-authorization/mango-authorization-core/target/surefire-reports` |
| R-03 | 前端单元测试 | `cd mango-ui && pnpm -F @mango/admin-shell test -- src/__tests__/activeMenuPath.spec.ts src/__tests__/menuHost.spec.ts` | PASS，18 files，89 tests | Vitest terminal output |
| R-03 | 前端静态检查 | `pnpm exec eslint packages/admin-shell/src/__tests__/menuHost.spec.ts`；`pnpm exec prettier --check packages/admin-shell/src/__tests__/menuHost.spec.ts` | PASS | command output |
| R-03 | RBAC 包测试与表单静态检查 | `pnpm -F @mango/rbac test`；`pnpm exec prettier --check packages/rbac/src/views/menu/index.vue` | PASS，3 files，9 tests；Prettier PASS | command output |
| R-03 | RBAC 表单 ESLint | `pnpm exec eslint packages/rbac/src/views/menu/index.vue` | BLOCKED，存量 `vue/multi-word-component-names`（`index.vue`） | command output；与本次字段改动无关 |
| 全部改动 | 后端编译/质量入口 | `mvn -f mango/pom.xml -pl :mango-authorization-api,:mango-authorization-core -am -DskipTests verify` | PASS；已有 PMD processing warnings，无本次改动失败 | Maven terminal output |
| I-07 | Java 质量门禁（CI 同款） | `mvn -f mango/pom.xml -pl ":mango-architecture-verification,mango-platform/mango-authorization/mango-authorization-core" -DskipTests -Dmango.architecture.skip=false -Dmango.architecture.mode=changed -Dmango.architecture.requireFullReactor=false -Dmango.architecture.base=85a6fd8113f592b5883caeea56fa5fa4a7e91082 -Dmango.check.changedOnly=true -Dmango.check.gate=no-new-violations -Dmango.check.baseRef=85a6fd8113f592b5883caeea56fa5fa4a7e91082 verify` | PASS，`gateStatus=PASS`，`newIssueCount=0`，`baselineIssueCount=101` | `mango/target/mango-static-report.json`；命令输出 |
| I-07 | 后端集成测试回归 | `mvn -f mango/pom.xml -pl :mango-authorization-api,:mango-authorization-core -am -Dtest=MenuServiceImplIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test` | PASS，5 tests，0 failures | Surefire 报告 |
| 全部改动 | Diff 检查 | `git diff --check` | PASS | command output |

## 7. 例外与剩余风险

- 未执行真实浏览器登录、菜单后台新增、页面点击和直接 URL 访问；本次已用后端菜单集成测试和 Admin Shell 路由契约测试覆盖核心行为，真实环境联调仍需按 R-01 至 R-03 在运行中的管理后台复核。
- 首次直接执行单模块 Maven 测试因未构建当前 Reactor 的 `mango-authorization-api` 快照而失败；改用包含 API/Core 的 Reactor 定向测试后通过，未修改该无关基线问题。
- `Java gates` 曾报 `newIssues=25`：根因是 main 上 `MenuServiceImpl.java` 已在提交 `8f6f7fab9`（PR #519）按命名规范重命名为 `MenuService.java`，但静态分析基线 `mango-pmo/baselines/mango-check/no-new-violations-baseline.json` 仍记录旧路径，导致该文件存量问题在本次被误判为新增违规。本次选择不改受治理的基线（避免切到 governance 全量模式），改为清偿 `MenuService.java` 内这 25 个存量问题（I-07）。命名规范是否放开 `*ServiceImpl` 后缀、以及剩余自有 `*ServiceImpl` 类的迁移，属于独立治理议题，不在本 PR 处理。
