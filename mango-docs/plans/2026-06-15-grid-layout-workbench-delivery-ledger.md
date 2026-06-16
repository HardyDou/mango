# 工作台自定义布局与 @mango/grid-layout 交付台账

## 1. 目标

新增通用自定义栅格布局组件包 `@mango/grid-layout`，新增后端个人布局配置模块 `mango-grid-layout`，并将后台首页工作台改造为首个消费场景，实现布局查看、编辑、添加、拖拽、缩放、删除、保存、取消和恢复默认闭环。

## 2. 范围

- 前端新增 `mango-ui/packages/grid-layout`。
- 后端新增 `mango/mango-platform/mango-grid-layout`。
- 后台首页 `mango-ui/packages/admin-shell/src/views/home/index.vue` 接入新组件。
- 单体后台 `@mango/admin` 接入新包依赖和样式聚合。
- 后端聚合 starter 接入 `mango-grid-layout-starter`。
- 同步设计文档、能力地图和模块 README。

## 3. 不做范围

- 不把工作台业务小组件接口写入 `@mango/grid-layout`。
- 不保存默认布局到后端。
- 不做用户自定义卡片标题。
- 不做复杂多端冲突合并。
- 不做移动端拖拽编辑。
- 不直接合并 PR；PR 创建后由人工 review 和合并。

## 4. 设计输入

- `mango-docs/designs/mango-grid-layout-package-adr.md`
- `mango-docs/designs/mango-grid-layout-workbench-design.md`
- `mango-pmo/rules/frontend/03-component-development.md`
- `mango-pmo/rules/frontend/06-monorepo-architecture.md`
- `mango-pmo/rules/backend/03-api.md`
- `mango-pmo/rules/08-capability-docs.md`

## 5. 改动项

### 5.1 前端包

- 新增 `MangoGridLayout` 查看态组件。
- 新增 `MangoGridDesigner` 编辑态组件。
- 新增 `useGridEngine` 布局算法。
- 新增 `gridLayoutPersonalApi` 个人布局 API 封装。
- 新增公开类型和 `style.css`。

### 5.2 工作台页面

- 首页增加工作台欢迎区。
- 接入 `MangoGridLayout` 和 `MangoGridDesigner`。
- 支持编辑布局、保存布局、取消编辑和恢复默认。
- 使用 `admin-home-workbench` 作为页面编码。

### 5.3 后端模块

- 新增 `mango-grid-layout-api/core/starter`。
- 新增 `GET /grid-layout/personal`。
- 新增 `PUT /grid-layout/personal`。
- 新增 `DELETE /grid-layout/personal`。
- 新增 `mango_user_grid_layout` migration。
- 新增 service 单测。

### 5.4 构建与装配

- `@mango/admin-shell` 增加 `@mango/grid-layout` 依赖和 Vite linked package 配置。
- `@mango/admin` 增加 `@mango/grid-layout` 依赖、样式声明和 style deps 构建。
- `mango/mango-platform/pom.xml` 增加后端模块。
- `mango/pom.xml` dependencyManagement 增加 starter。
- `mango/mango-admin-starter/pom.xml` 引入 `mango-grid-layout-starter`。

## 6. 接口变化

- `GET /grid-layout/personal?pageCode=...`：查询当前登录人的指定页面布局。
- `PUT /grid-layout/personal`：保存当前登录人的指定页面布局。
- `DELETE /grid-layout/personal?pageCode=...`：删除当前登录人的指定页面布局，用于恢复默认。

## 7. 数据变化

新增表 `mango_user_grid_layout`，按 `tenant_id + user_id + page_code` 唯一保存当前登录人的页面布局 JSON。

## 8. 验收标准

- 前端包构建通过。
- admin shell 构建通过。
- admin 样式聚合生成和检查通过。
- admin 构建通过。
- 后端新模块编译通过。
- 后端新模块核心 service 单测通过。
- 本地启动后，工作台页面可以完成查看态、编辑态、添加、拖拽、调整宽高、删除、保存、刷新回显和恢复默认验证。

## 9. 交付台账

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| TASK-001 | 用户要求 / 设计方案 | 通用自定义栅格编辑器包 | 抽成独立 `@mango/grid-layout` 包，不绑定工作台业务 | `mango-ui/packages/grid-layout` | `pnpm.cmd -F @mango/grid-layout build` | DONE | 本地命令通过 |
| TASK-002 | 用户要求 / 设计方案 | 后端个人布局保存接口 | 新增独立 `mango-grid-layout` 模块，按登录人与租户保存布局 | `mango/mango-platform/mango-grid-layout` | `mvn -f mango/mango-platform/mango-grid-layout/mango-grid-layout-core/pom.xml test` | DONE | 6 个 service 单测通过 |
| TASK-003 | 用户要求 / 设计方案 | 后台首页工作台接入 | 工作台作为首个消费方，负责默认布局和业务小组件内容 | `mango-ui/packages/admin-shell/src/views/home/index.vue` | 本地启动后页面验收 | DONE | `http://127.0.0.1:7861/#/home` 浏览器验收通过 |
| TASK-004 | 项目规范 / 前端架构规范 | 单体后台样式与构建接入 | 通过 admin 包声明依赖并生成样式聚合，不手工维护样式清单 | `mango-ui/packages/admin/**` | `pnpm.cmd admin:styles:check`、`pnpm.cmd -F @mango/admin build` | DONE | 本地命令通过 |
| TASK-005 | 项目规范 / 后端架构规范 | 后端 starter 和聚合装配 | 将 `mango-grid-layout-starter` 接入平台聚合与 admin starter | `mango/pom.xml`、`mango/mango-platform/pom.xml`、`mango/mango-admin-starter/pom.xml` | `mvn -f mango/pom.xml -pl mango-platform/mango-grid-layout/mango-grid-layout-starter -am clean install -DskipTests` | DONE | 本地命令通过 |
| TASK-006 | 项目规范 / 能力说明规范 | 文档同步 | 同步设计文档、模块 README、能力地图和交付台账 | 设计文档、README、能力地图、交付台账 | 文档检查与交付台账检查 | DONE | 本文档与能力地图已更新 |

## 10. 已执行验证记录

| 命令 | 结果 | 说明 |
| --- | --- | --- |
| `pnpm.cmd -F @mango/grid-layout build` | PASS | 前端布局包构建通过 |
| `pnpm.cmd -F @mango/admin-shell build` | PASS | 后台 shell 构建通过 |
| `pnpm.cmd admin:styles` | PASS | admin 样式聚合生成通过 |
| `pnpm.cmd admin:styles:check` | PASS | admin 样式聚合检查通过 |
| `pnpm.cmd -F @mango/admin build` | PASS | 单体后台构建通过 |
| `mvn -f mango/pom.xml -pl mango-platform/mango-grid-layout/mango-grid-layout-starter -am clean install -DskipTests` | PASS | 后端新模块编译安装通过 |
| `mvn -f mango/mango-platform/mango-grid-layout/mango-grid-layout-core/pom.xml test` | PASS | `GridLayoutPersonalServiceTest` 通过 |
| `mvn -f mango/pom.xml -pl mango-platform/mango-grid-layout/mango-grid-layout-starter -am test` | FAIL | 聚合链路在上游 `mango-maven-plugin` 历史测试失败，未进入本模块完整测试 |
| `Invoke-WebRequest http://127.0.0.1:7861/index.html` | PASS | 前端入口返回 200 |
| `Invoke-WebRequest http://127.0.0.1:18171/actuator/health` | PASS | 后端健康状态 `UP`，数据库组件 `UP` |
| `mysql ... SHOW TABLES LIKE 'mango_user_grid_layout'` | PASS | 个人布局表已通过 Flyway 创建 |
| `mysql ... SELECT COUNT(*) ... page_code='admin-home-workbench'` | PASS | 恢复默认后个人布局记录数为 `0` |

## 11. 浏览器联调记录

- 前端地址：`http://127.0.0.1:7861/index.html#/home`。
- 后端地址：`http://127.0.0.1:18171`。
- 数据库名：`mango_dev_a07383`。
- 登录账号：`admin`。
- 查看态：工作台默认展示 6 个卡片，未出现“布局加载失败”提示。
- 编辑态：组件库展示 7 个可用小组件；已有卡片均展示删除按钮、右侧宽度拖拽手柄和底部高度拖拽手柄。
- 添加：点击“通知公告”后布局区新增第 7 个卡片，并展示“暂无通知公告”。
- 拖拽：拖动已有卡片后，其它卡片自动整理位置，未出现重叠。
- 调整宽度：拖拽右侧手柄后卡片宽度增大，未出现宽度变为 0。
- 调整高度：拖拽底部手柄后卡片高度增大。
- 删除：删除新增“通知公告”后卡片数量恢复为 6。
- 保存：点击“保存布局”后退出编辑态，刷新页面后个人布局仍能回显。
- 恢复默认：点击“恢复默认”并确认后回到默认布局，刷新后仍为默认布局；数据库中 `admin-home-workbench` 个人记录数为 `0`。
- console/network：带时间标记刷新后未新增 `warn` 或 `error` 日志；历史 `MetricWidget` props warning 为热更新前日志残留，当前刷新后未复现。

## 12. 风险与限制

- `@mango/grid-layout` 第一版以可用和边界清晰为主，拖拽动画后续仍可继续细化。
- 工作台小组件第一版使用现有首页信息和静态展示，真实业务数据接口后续按小组件逐个接入。
- 单体显式配置了 `mango.persistence.flyway.modules`，本次已补充 `grid-layout.enabled=true`，确保 `db/migration/grid-layout` 下的 migration 会执行。
- 聚合 Maven test 当前受上游历史测试影响，交付报告中需要继续说明。
- 当前 Windows/Git Bash 环境下 `scripts/dev-workspace.sh stop` 依赖的进程清理工具不可用，后端启动验证使用等价环境变量通过 PowerShell 手动启动；前端、后端端口仍来自 `.mango/dev-workspace.env`。
