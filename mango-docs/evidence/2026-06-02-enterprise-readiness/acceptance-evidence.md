# Mango 企业业务开发框架正式推广验收证据

## 1. 验收范围

- 页面：单体与微前端中的开发中心组件页、审批中心、系统管理、通知中心、业务项目页面。
- 接口：登录、菜单、用户、租户、文件、工作流、业务 CRUD、操作日志。
- 权限：平台管理员、业务租户管理员、普通业务用户。
- 数据：真实数据库数据、初始化种子数据、业务模块持久化数据。
- 部署形态：单体应用、微前端应用、独立业务项目。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:8512`。
- 后端地址：`http://127.0.0.1:18822`。
- 数据库或租户：本地开发数据，租户 `芒果集团`。
- 测试账号：`admin / admin123`。
- 浏览器：Playwright Chromium，视口 `1440x960`。

## 3. 功能验收记录

| 台账 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|
| ER-003 | 开发中心/文件上传组件 | 打开组件页并检查上传主入口 | 单体应用登录后访问 `/#/components/upload` | 页面包含 `MUpload 文件上传`、`通用/默认用法`、`上传文件`，无组件 404 | 左侧文件上传菜单激活；上传按钮、空文件列表、右侧页面导航可见；主体没有空白、遮挡或错位 | `monolith-readiness-report.json` 中 console/network 增量均为 `0` | `screenshots/upload-components.png`；`monolith-readiness-report.json` | 通过 |
| ER-004 | 开发中心/工作流组件 | 打开组件页并检查运行时表单示例 | 单体应用登录后访问 `/#/components/workflow` | 页面包含 `工作流组件`、`运行时表单渲染`、`业务申请组件注册`，无组件 404 | 左侧工作流组件菜单激活；表单字段、开关、变量标签、右侧导航显示正常；无明显布局错位 | `monolith-readiness-report.json` 中 console/network 增量均为 `0` | `screenshots/workflow-components.png`；`monolith-readiness-report.json` | 通过 |
| ER-005 | 审批中心/流程定义/表单信息 | 内置设计器拖入首个输入框组件 | 新建 `推广验收流程<timestamp>`，表单编码 `enterprise_form_<timestamp>` | 完成基础信息后进入表单信息，拖入 `输入框`，画布出现输入框且右侧组件配置联动 | 设计器左侧组件区、中间画布、右侧配置区完整；拖拽后输入框位于画布，未出现遮挡、空白或异常弹窗 | `monolith-readiness-report.json` 中 console/network 增量均为 `0`，未捕获 `Cannot read properties of undefined (reading 'name')` | `screenshots/workflow-designer-drag.png`；`monolith-readiness-report.json` | 通过 |
| ER-007 | 系统管理/用户 | 用户分页查询页检查 | 单体应用登录后访问 `/#/system/user` | 页面包含 `成员管理`、`用户名`、`查询`，分页 `Total 1` 可见 | 左侧组织树和右侧成员表格布局稳定；筛选栏、操作按钮、分页控件无错位 | `monolith-readiness-report.json` 中 console/network 增量均为 `0`，未捕获分页 total 类型 warning 或分页 deprecated warning | `screenshots/system-user.png`；`monolith-readiness-report.json` | 通过 |
| ER-014 | 微前端/Shell 开发中心组件页 | 文件上传组件页和工作流组件页检查 | `http://a.mango.io:5176` hybrid profile，登录后访问 `/#/components/upload`、`/#/components/workflow` | 两页分别包含上传组件和工作流组件关键标题、示例文案；无组件 404 | Shell 菜单、标签页、页面导航、组件示例布局正常；无空白、遮挡或明显错位 | `micro-readiness-report.json` 中两页 console/network 增量均为 `0` | `screenshots/micro-upload-components.png`；`screenshots/micro-workflow-components.png`；`micro-readiness-report.json` | 通过 |
| ER-014 | 微前端/RBAC 远程子应用 | 角色页远程承载和列表操作入口检查 | hybrid profile 中 `mango-authorization` 配置为 `MICRO_ROUTE`，entry `b.mango.io:5181` | runtime 数据为 `mango-admin-rbac-app`；页面包含 `新增角色`、`角色编码`、`分配权限` | 左侧权限菜单激活；角色表格、状态标签、行操作按钮显示稳定；无空白或错位 | `micro-readiness-report.json` 中该页 console/network 增量均为 `0` | `screenshots/micro-system-role.png`；`micro-readiness-report.json` | 通过 |
| ER-014 | 微前端/Workflow 远程子应用 | 流程定义内置设计器拖入组件 | hybrid profile 中 `mango-workflow` 配置为 `MICRO_ROUTE`，entry `c.mango.io:5182` | runtime 数据为 `mango-admin-workflow-app`；进入表单信息后拖入 `输入框`，画布和右侧配置联动 | 设计器左侧组件区、中间画布、右侧配置区完整；拖入控件后无空白、遮挡或异常弹窗 | `micro-readiness-report.json` 中 network 增量为 `0`；记录到 Wujie/Element 事件参数 warning 和 realtime WebSocket 探测 warning，未影响功能，待后续治理 | `screenshots/micro-workflow-designer-drag.png`；`micro-readiness-report.json` | 通过 |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| 系统管理 | 用户管理 | 查询 | 分页 | 表格、筛选栏、分页无错位；组织树和表格区域没有遮挡 | `screenshots/system-user.png` | PASS |
| 系统管理 | 角色管理 | 列表查看 | 权限分配入口 | 远程 RBAC 子应用表格、标签、行操作正常；无错位 | `screenshots/micro-system-role.png` | PASS |
| 审批中心 | 流程定义 | 新建流程基础信息 | 表单设计器拖入组件 | 表单设计器三栏布局正常，拖入组件后配置面板联动 | `screenshots/workflow-designer-drag.png` | PASS |
| 开发中心 | 文件上传 | 打开上传组件 | 检查上传入口和空列表 | 上传按钮、文件列表、页面导航正常 | `screenshots/upload-components.png` | PASS |
| 开发中心 | 工作流组件 | 打开工作流组件页 | 检查运行时表单示例 | 表单示例、右侧页面导航正常 | `screenshots/workflow-components.png` | PASS |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 微前端 console warning | Workflow 远程子应用表单设计器交互触发 Vue `Invalid event arguments` warning；realtime WebSocket 探测有连接关闭 warning | 当前功能验收通过，但推广前应评估是否需要治理或降低噪声 | 纳入 Sprint 1/Sprint 3 warning 治理；不作为本轮阻塞 | 否 |
| 工作流示例占位文本残留 | 微前端流程定义基础信息和表单信息中仍出现 `guarantee_approve`、`guarantee_apply_form` 占位示例 | 不是废弃保函业务模块代码，但会影响 Mango 通用框架观感和企业推广中性表达 | 后续统一改为 `contract` 或 `business` 中性示例并回归 | 否 |
| 企业业务项目全流程仿真 | 尚未执行 `mango-cli` 独立业务仓初始化和 CRUD 模块开发 | 不能声明企业业务开发全流程就绪 | Sprint 2 执行并留存截图、命令、日志 | 否 |
| 全仓后端测试 | 本轮尚未执行全仓 `mvn test` | 不能作为发布前最终质量结论 | Sprint 4 执行全仓测试，失败项逐项归因 | 否 |
