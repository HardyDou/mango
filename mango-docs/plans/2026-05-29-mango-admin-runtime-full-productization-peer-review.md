# Mango Admin Runtime 完整产品化升级计划同行评审

## 1. 评审结论

可开始，但只能从 Sprint 0 开始，不能直接进入 `@mango/admin` 实现或 starter 改造。

当前计划方向正确，覆盖了默认完整 Mango Admin 复用、可配置裁剪、API SDK/Admin UI 分包、能力依赖自动补齐、菜单权限自动集成、local/micro/mixed 自由组合部署、E2E 截图和布局/颜色/数据/功能验收目标。

未发现会导致目标必然失败的致命架构问题。计划能否达成目标，取决于后续是否严格执行每个 Sprint 的新特性测试、回归测试、截图验收、真实菜单权限和真实数据门禁。

## 2. 致命问题

无。

## 3. 非致命高风险

- capability 仍处于初版，尚未覆盖 `requires`、`menus`、`permissions`、`styles`、`runtime`、`backend`、`e2e` 等完整字段。
- 当前 `package:check` 深度不足，尚不能验证菜单合并、依赖图、样式入口、后端能力要求和 E2E 清单。
- 原完整 Mango 主框架仍主要在 `apps/mango-admin`，目标 `@mango/admin` 尚未存在；Sprint 1 必须抽取完整框架，禁止仿写。

## 4. 开始前门禁

Sprint 0 必须完成以下门禁后，才能进入 Sprint 1：

- 冻结原 Mango Admin 基准截图：登录、首页、左侧菜单、顶栏、小铃铛、用户区、设置、主题、标签页、系统管理、权限、文件、通知、工作流。
- 明确截图验收脚本输出目录和报告格式，证据必须保存在 `mango-docs/evidence`。
- 定义 capability manifest 2.0 类型，至少包含依赖、菜单、权限、样式、运行时、后端能力和 E2E 清单。
- 扩展 `package:check`，拒绝新增混合包、API 包带 UI、Admin 包缺 manifest 2.0 字段、starter 手写 Mango 内置菜单。
- 明确后端菜单字段与 capability 字段映射：`menuCode`、`component`、`permission`、`moduleCode`、排序、父子关系和冲突策略。

## 5. 阶段顺序评审

整体顺序合理：先基准，再基座，再拆包，再依赖和菜单，再部署模式，再 CLI/starter，最后发布和验收。

评审调整：capability manifest 2.0 的类型定义和检查器骨架提前到 Sprint 0，完整依赖解析实现仍保留在 Sprint 3。

## 6. 最容易失败的点和防线

- `@mango/admin` 退化成仿写框架。防线：Sprint 1 必须与 Sprint 0 基准截图对比；顶栏、小铃铛、用户区、设置、主题、标签页缺一项都不能通过。
- API/Admin 拆包后页面继续绕过 API 包散写请求。防线：`package:check` 增加静态扫描；`*-admin` 只能通过对应 `*-api` 发请求；非管理 UI API SDK 消费 E2E 必须通过。
- 菜单权限集成被静态菜单绕过。防线：starter/template 检查禁止 Mango 内置菜单；E2E 必须验证后端菜单、能力菜单、业务菜单合并结果，以及缺权限时菜单隐藏、接口仍做权限校验。

## 7. 最终判断

可以开始 Sprint 0。不得跳过 Sprint 0，也不得在缺少基准截图、manifest 2.0 骨架、检查器防线和菜单字段映射前进入 Sprint 1。
