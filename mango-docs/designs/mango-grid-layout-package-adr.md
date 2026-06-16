# ADR: @mango/grid-layout 独立包定位

## Status

Accepted

## Context

后台首页工作台需要支持用户自定义布局。用户可以从组件库添加系统已有小组件，并通过拖拽、调整宽高、删除、保存、取消和恢复默认完成个人工作台配置。

该能力不是单一工作台页面的私有能力。后续数据看板、业务驾驶舱、详情页自定义面板等场景也可能复用同一套栅格展示和编辑能力。因此，本次需要明确前端包归属、后端配置接口归属，以及它们与工作台业务组件之间的边界。

相关项目规范要求可复用前端能力进入 `mango-ui/packages/*`，公共组件不得依赖宿主 app、admin shell 私有 store、router、菜单、权限装配或启动逻辑；组件样式跟随所属 package，并通过公开样式入口输出。

## Decision

本次将自定义栅格布局能力沉淀为独立前端 npm 包：

- 包目录：`mango-ui/packages/grid-layout`
- 包名：`@mango/grid-layout`
- 发布范围：`@mango` scope
- 能力定位：通用自定义栅格展示与编辑器
- 样式入口：`@mango/grid-layout/style.css`

该包与 `@mango/common`、`@mango/file`、`@mango/calendar`、`@mango/workflow` 等包平级，不放入 `@mango/common`，也不放入 `@mango/admin-shell`。

本次同步新增独立后端模块：

- 模块目录：`mango/mango-platform/mango-grid-layout`
- Maven groupId：`io.mango.platform.gridlayout`
- HTTP 入口：`/grid-layout/personal`
- 数据表：`mango_user_grid_layout`

`@mango/grid-layout` 负责布局展示、布局编辑、组件库搜索添加、碰撞整理、拖拽排序、宽高调整、删除、公开类型和个人布局 API 封装。它不处理工作台小组件业务数据、业务接口、菜单权限、路由跳转、登录用户识别或租户识别。

`mango-grid-layout` 后端模块负责按当前登录用户、当前租户和页面编码保存个人布局 JSON。它不保存默认布局，不判断小组件权限，不查询小组件业务数据，也不解释小组件内部配置。

## Consequences

### Positive

- 组件边界清晰，可以作为 npm 包独立消费。
- 工作台、看板、驾驶舱等页面可以复用同一套布局编辑能力。
- 不增加 `@mango/common` 体积和发布影响面。
- 布局算法、编辑交互、样式和类型可以随包独立演进。
- 个人布局持久化不绑定 `mango-system` 的个人参数配置能力，后续离开系统模块仍可复用。

### Negative

- 需要新增前端 package、后端 module、构建配置、样式聚合配置和能力文档。
- 首次实现成本高于直接写在工作台页面中。
- 消费方需要显式声明依赖并引入 `@mango/grid-layout/style.css`。
- 包内 API 契约后续需要保持兼容，破坏性调整需要版本策略。

### Neutral

- `vue` 和 `element-plus` 作为 peer dependencies，由宿主应用提供运行时版本。
- `@mango/common` 可作为请求封装依赖，但 `@mango/grid-layout` 不反向依赖宿主应用。
- 工作台页面仍负责准备可用小组件列表、默认布局和业务小组件内容。

## Alternatives Considered

### 放入 `@mango/common`

Rejected.

`@mango/common` 更适合承载基础工具、hooks 和轻量公共组件。自定义栅格编辑器包含完整布局算法、拖拽交互、样式体系、类型契约和 API 封装，放入 `common` 会扩大基础包影响面，并增加后续维护复杂度。

### 放入 `@mango/admin-shell`

Rejected.

该能力不是后台 shell 私有能力。放入 `admin-shell` 会导致其它业务包、微前端应用或独立业务项目难以复用，也会让通用组件依赖宿主边界。

### 复用 `sys_personal_config`

Rejected.

`sys_personal_config` 适合系统模块内的轻量个人参数配置。本能力需要独立 API、表结构、JSON 校验和后续可复用装配能力，复用系统个人参数表会把通用布局能力绑定到系统模块。

## Design Constraints

- 前端包只封装个人布局配置 API，不封装工作台业务接口。
- 前端包不读取用户、租户、菜单、权限、路由或宿主 store。
- 前端包通过 `props`、`emits`、`v-model`、slot、公开类型和 `style.css` 输出能力。
- 后端接口从登录上下文识别用户和租户，前端不传 `userId`。
- 默认布局由业务页面提供，不保存到后端。
- 用户布局以手动保存为准；取消编辑丢弃本次未保存结果。
- 恢复默认等价于删除数据库中的个人布局配置。

## References

- `mango-pmo/rules/frontend/03-component-development.md`
- `mango-pmo/rules/frontend/06-monorepo-architecture.md`
- `mango-pmo/rules/backend/03-api.md`
- `mango-pmo/rules/08-capability-docs.md`
