# 标准交付记录

## 1. 元数据

- 任务 ID：notice-structured-dialog
- 交付模式：STANDARD
- 需求影响：L2 - 调整通知公共组件展示协议、管理端导航协作和工作流通知目标数据
- 方案风险：L2 - 涉及前后端消息契约兼容、动态路由权限判断与自定义工作流页面降级
- 最终风险：L2 - 保留既有 targetKey 协议并提供通用工作流页面降级，不修改历史消息数据
- 工作区决策：CREATE - `codex/notice-structured-dialog` / `D:\Project\mango-worktrees\notice-structured-dialog`

## 2. 目标与范围

- 目标：让消息详情弹框和右上角实时提醒以最简单的 `label: value` 结构展示，并确保底部业务按钮和铃铛分类都能进入正确页面。
- 成功条件：详情弹框标题只显示消息类型，正文只显示消息类型、消息内容、消息时间；底部仅保留“关闭 + 主操作”；消息 HTML 经白名单清洗后渲染；未读超过 10 条时铃铛显示精确分类统计并进入对应筛选结果；工作流查看类消息优先 `viewPath` 且失败时降级到通用页面。
- 处理范围：`@mango/notice` 消息展示、HTML 清洗、分类统计与交互，`@mango/admin-shell` 路由承接，Notice 分类统计/筛选 API，工作流事件载荷与通知目标，相关单元/协作测试。
- 不处理范围：历史消息数据回填、浏览器原生桌面通知的自定义布局、支付等业务域新增消息生产逻辑、发布和版本升级。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| R1 | 用户 / 消息详情弹框 | 打开一条站内消息 | 标题只显示消息类型；正文按 `label: value` 依次显示消息类型、消息内容、消息时间；底部右侧显示“关闭 + 主操作” | 字段缺失时显示 `-`，不回退展示其它技术字段 | 工作流典型消息和通用消息均只出现三个指定字段 |
| R2 | 用户 / 消息中心或顶部铃铛 | 点击可用的主操作按钮 | 按消息动作进入目标页面，成功后关闭详情弹框 | 目标不存在或无权限时提示，并在配置了降级目标时进入通用页面 | 四个入口使用同一动作载荷；待办进入任务详情 |
| R3 | 用户 / 工作流完成、结束、拒绝消息 | 流程定义 `formJson.customConfig.viewPath` 有效 | 优先进入业务查看页，并携带申请、业务和流程标识 | 自定义路径无效/无权限时进入工作流已办或我发起页面 | 后端测试覆盖 viewPath 优先与无配置降级 |
| R4 | 用户 / 右上角实时提醒 | 收到实时站内消息且弹窗提醒开启 | 标题只显示消息类型，正文按 `label: value` 显示消息类型、消息内容、消息时间和“点击查看” | 数据缺失时显示 `-` 和通用类型 | 组件测试验证 VNode 内容，点击后打开完整详情 |
| R5 | 用户 / 浏览器桌面通知 | 已授权且桌面通知开启 | 使用可理解的标题和精简摘要，点击后打开完整详情 | 浏览器不支持或未授权时静默降级 | 保持原生 API 边界，不尝试自定义桌面通知布局 |
| R6 | 用户 / 消息 HTML 内容 | 消息类型或内容含格式标签、脚本、事件属性或危险链接 | 保留允许的文本格式标签，移除脚本、事件属性、样式和危险协议，再通过 `v-html`/`innerHTML` 展示 | 无 DOM 环境时转义为纯文本，不直接注入原值 | 单元测试覆盖普通标签、`script`、`onclick` 和 `javascript:` |
| R7 | 用户 / 顶部铃铛 | 当前未读总数不超过 10 条 | 按单条消息展示，点击继续打开原详情 | 查询失败时显示空状态，不伪造统计 | 10 条边界仍为单条列表 |
| R8 | 用户 / 顶部铃铛 | 当前未读总数超过 10 条 | 显示审批类、系统通知、业务通知的精确非零数量；点击进入消息中心对应未读分类 | 分类统计失败时显示空状态；分类为空时不显示该项 | `WORKFLOW` 归审批类，`AUTH/IDENTITY/JOB` 归系统类，其余归业务类；数量总和等于未读总数 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| D1 | R1、R4、R5 | 在 notice 包内建立纯函数展示模型，统一字段选择、状态文案、主按钮文案和摘要；组件仅消费模型 | `mango-ui/packages/notice/src/client`、`components` | 删除展示模型并恢复原始 content 展示 |
| D2 | R2 | Notice 组件只发出 `interaction`，宿主使用动态路由表执行导航；事件动作仍由 notice 客户端 API 执行 | notice client、admin shell、site-message | 恢复各入口原有独立处理 |
| D3 | R2、R3 | `targetKey` 兼容路由名和安全的站内绝对路径；`fallbackTargetKey` 仅作为导航元数据，不透传查询参数 | notice targets、workflow notice subscriber | 停用路径目标和 fallback，继续使用固定 route key |
| D4 | R3 | 从业务申请的 `formJsonSnapshot.customConfig.viewPath` 派生只读 `viewPath` 到 VO/事件载荷，不新增表字段 | workflow api/core/starter | 删除派生字段，历史固定目标继续生效 |
| D5 | R6 | Notice 包内提供无依赖 HTML 白名单清洗函数；只允许基础文本格式标签和安全链接协议，所有富文本出口复用清洗结果 | notice client/components | 恢复纯文本插值展示 |
| D6 | R7、R8 | 新增消息分类枚举、未读分类统计 API，并在分页查询增加分类参数；后端根据业务类型配置的 `bizGroup` 精确归类，不新增数据库字段 | notice api/core/starter/remote、notice client | 铃铛恢复固定条数列表并移除分类参数/API |
| D7 | R8 | 铃铛分类点击以 `category + unreadOnly=true` 交给宿主导航，消息中心从路由 query 初始化并调用后端分类筛选 | notice bell、site-message、admin shell | 分类点击统一退回无筛选消息中心 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| I1 | D1 | 1 | `mango-ui/packages/notice/src/client/messagePresentation.ts`、`NoticeDetailDialog.vue`、`NoticeClientBell.vue` | 详情和实时提醒使用同一结构化模型 |
| I2 | D2、D3 | 2 | notice interaction/targets、`NoticeClientMessageCenter.vue`、admin-shell nav bar | 铃铛和消息中心动作均可导航并具备失败提示/降级 |
| I3 | D4 | 3 | workflow VO、业务申请映射、事件发布器、通知订阅器 | 查看类工作流通知生成 viewPath 目标和通用降级目标 |
| I4 | D1-D4 | 4 | notice Vitest、workflow JUnit | 覆盖字段选择、按钮文案、事件发出、viewPath 优先和降级 |
| I5 | D5 | 5 | notice HTML 清洗工具、详情弹框、实时提醒、铃铛单条列表及测试 | 所有通知富文本出口复用安全 HTML，危险内容不可执行 |
| I6 | D6 | 6 | notice 分类枚举/VO/query/service/controller/Feign 及测试 | 分类统计精确且分页分类筛选与统计口径一致 |
| I7 | D7 | 7 | NoticeClientBell、NoticeBell、消息中心、site-message、admin-shell 及测试 | `>10` 聚合、`<=10` 单条，分类点击进入对应未读列表 |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| R1 | Notice 组件测试 + 页面走查 | `pnpm -F @mango/notice test`；内置浏览器打开 `/message-center/site-message` 并查看真实消息详情 | PASS | 9 个测试文件、36 条测试通过；真实弹框标题为“登录成功”，正文仅显示消息类型、消息内容、消息时间，底部为“关闭 + 查看资料”；截图：`历史验收图片已清理（可从 Git 历史恢复）` |
| R2 | Notice/Admin Shell 测试 + 构建 + 页面跳转 | `pnpm -F @mango/notice test`；`pnpm --filter @mango/admin-shell... build`；从详情和铃铛点击真实消息主操作 | PASS | interaction/targets 测试通过，Admin Shell 及 16 个依赖包构建通过；真实点击后进入 `#/profile?username=admin&loginTime=...&appCode=internal-admin&bizId=...`，详情弹框正常关闭 |
| R3 | Workflow 单元/集成测试 | `WorkflowEventPublisherTest`、`WorkflowBusinessApplyServiceImplIntegrationTest`、`WorkflowNoticeDomainEventSubscriberTest` 定向 Maven 测试 | PASS | viewPath 派生、事件载荷、通知目标优先级和固定页面降级均通过 |
| R4 | Notice 组件测试 + 页面截图 | `pnpm -F @mango/notice test`；内置浏览器触发真实实时提醒 | PASS | `NoticeBell.spec.ts` 验证通知标题、三字段 VNode、安全 `innerHTML` 和点击详情；真实 Element Plus Notification 标题仅为“登录成功”，正文显示消息类型、消息内容、消息时间和“点击查看”；截图：`历史验收图片已清理（可从 Git 历史恢复）` |
| R5 | Realtime 单元测试 + 浏览器能力检查 | `pnpm -F @mango/notice test`；检查浏览器通知权限及站内降级 | 站内提醒 PASS；原生桌面通知未人工验证 | realtime 测试验证标题/摘要清洗；浏览器环境显示原生通知已阻止，站内 Element Plus Notification 降级正常；操作系统通知样式与授权后的点击路径未在本次环境人工确认 |
| R6 | HTML 清洗单元/组件测试 + 页面安全检查 | `pnpm -F @mango/notice test`；打开包含格式标签和危险内容的真实消息 | PASS | 单元测试覆盖基础标签、`script`、`onclick`、内联样式、`javascript:` 和协议相对链接；真实页面保留 `<strong>`、`<em>`、`<u>`，移除 `<script>` 与 `javascript:`，且未执行 `window.__noticeXss`；截图：`历史验收图片已清理（可从 Git 历史恢复）` |
| R7 | Notice 组件边界测试 + 页面走查 | `pnpm -F @mango/notice test`；保持 3 条未读并打开铃铛 | PASS | 自动化验证未读 10 条仍为单条列表；真实 3 条未读时按单条消息展示，点击可进入对应资料页面 |
| R8 | Notice 组件测试 + Mapper/H2 集成测试 + 真实 API/页面筛选 | `NoticeServiceIntegrationTest`；制造 11 条未读后检查统计 API、铃铛分组、分类跳转和页面筛选 | PASS | 13 条后端集成测试通过；真实 API 返回审批 0、系统 11、业务 0，铃铛显示“系统通知（11条）”；点击进入 `#/message-center/site-message?category=SYSTEM&unreadOnly=true`，页面筛选为“系统通知 + 未读”且总数 11；截图：`历史验收图片已清理（可从 Git 历史恢复）` |

补充静态与构建结果：

- `pnpm -F @mango/notice build`：PASS。
- `pnpm --filter @mango/admin-shell... build`：PASS。
- 定向 ESLint（关闭 `navBars/index.vue` 既有的单词组件名基线规则）：0 error、5 warning；warning 为既有属性顺序和可选 props 默认值提示。
- `mvn -pl mango-platform/mango-notice/mango-notice-core -am -DskipTests install`：45 个 Reactor 模块安装成功；重启后 `http://127.0.0.1:18045/actuator/health` 返回 200，MySQL 状态 UP。
- `NoticeControllerAccessModeTest`：1 条测试通过，新统计接口权限保持 `notice:site:view`。
- 768×900 页面走查：消息中心和详情弹框无明显遮挡或溢出；截图：`历史验收图片已清理（可从 Git 历史恢复）`。
- 最终浏览器 console warning/error：0；页面和真实 API 未观察到 4xx/5xx 错误。
- 页面验收创建的 19 条本地站内消息已按创建时间精确清理；保留原有消息，清理后剩余未读 2 条（系统 2、审批 0、业务 0）。

## 7. 例外与剩余风险

- 浏览器原生桌面通知由操作系统绘制，只能控制标题、摘要和点击行为，不能实现与站内弹窗相同的结构化布局。
- 历史消息没有 `viewPath` 时按既有固定工作流页面降级。
- 站内详情、实时提醒、分类点击、安全 HTML、768px 管理端窄屏及 console 已完成真实页面验收；390px 下 Admin Shell 进入现有“暂无可访问菜单”移动端布局，属于 Shell 当前支持边界，不作为本次通知组件回归。
- 浏览器环境的原生桌面通知权限为“已阻止”，因此未验证授权后的 OS 通知样式和点击行为；站内提醒降级路径已验证通过。
- 当前保留本地联调服务：后端 `18045`、Admin Shell `31045`、RBAC `32045`、Workflow `33045`，便于用户继续人工验收。
- 定向 ESLint 直接使用默认规则时会命中 `packages/admin-shell/src/layout/navBars/index.vue` 的既有 `vue/multi-word-component-names` 错误；本次未扩大范围修改该历史问题。
