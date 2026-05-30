# Sprint 4 E2E 截图识别报告

## 1. 结论

- full preset 和 custom preset 截图均显示复用 Mango 原 Admin Shell：顶部蓝色导航、左侧菜单、标签页、右上角搜索/全屏/设置/用户区、设置抽屉均存在。
- custom preset 在同一 Shell 中追加业务菜单 `业务报表`，不是独立仿写主框架。
- 抽查页面未发现空白页、运行态错误块、横向溢出、明显遮挡或样式断裂。
- 组织架构页面必须显示真实组织树、右侧详情数据和直属下级表格；早前截图只证明页面挂载，未证明组织数据回显，已将 E2E 断言从静态标题 `组织详情` 收紧为直属下级表格编码 `MANGO_TECH`。
- Element Plus 2.14.0 升级后，干净重跑 full/custom E2E，未再出现 `dayjs.min.js does not provide an export named 'default'` 运行错误，也未再生成 `Failed to resolve dependency: dayjs` 预优化警告。

## 2. 截图识别样本

| 模式 | 截图 | 识别结论 |
|---|---|---|
| full | `full-preset/home-1440x960.png` | 复用 Mango 顶栏、侧栏、标签页和首页卡片布局；主题色为 Mango 蓝；无空白主区域。 |
| custom | `custom-preset/home-1440x960.png` | 与 full Shell 布局一致，顶部追加业务菜单 `业务报表`；未出现仿写壳差异。 |
| custom | `custom-preset/sample-业务报表-业务报表.png` | 业务菜单在 Mango Shell 内打开，表格字段 `报表名称`、`负责人`、`状态` 和两条业务行可见。 |
| full | `full-preset/sample-系统管理-组织架构.png` | 系统管理子菜单、标签页和组织架构页面结构可见；必须显示 `芒果集团` 组织树、右侧 `MANGO_GROUP` 详情数据和直属下级 `MANGO_TECH`。 |
| full | `full-preset/function-settings-drawer.png` | 设置抽屉从右侧打开，布局配置、主题色、顶栏/菜单设置可见；遮罩和抽屉层级正常。 |

## 3. 自动报告摘要

- full preset：`full-preset/layout-report.json` 中 `hasLayout`、`hasNavbars`、`hasAside`、`hasMain`、`hasTags`、`hasUserArea`、`hasSettingsEntry`、`hasPrimaryColor`、`hasNoHorizontalOverflow`、`hasBackendMenuTree`、`hasMenuMergeReport` 均为 `true`。
- custom preset：`custom-preset/layout-report.json` 中 `usesCustomPresetPackage`、`hasResolutionReport`、`autoInstalledFoundations`、`businessMenuAppended`、`backendMenusRemainAuthoritative` 均为 `true`。
- full preset 一级菜单抽查：4 个一级菜单，12 个子页面，全部 `passed=true`；组织架构断言直属下级表格编码 `MANGO_TECH`。
- custom preset 一级菜单抽查：5 个一级菜单，13 个子页面，全部 `passed=true`，包含业务菜单 `业务报表`；组织架构断言直属下级表格编码 `MANGO_TECH`。
- full/custom 一级功能抽查：用户菜单和设置抽屉均 `passed=true`。

## 4. 证据文件

- `full-preset/summary.md`
- `full-preset/layout-report.json`
- `full-preset/menu-sampling-report.json`
- `full-preset/function-sampling-report.json`
- `custom-preset/summary.md`
- `custom-preset/layout-report.json`
- `custom-preset/menu-sampling-report.json`
- `custom-preset/function-sampling-report.json`
