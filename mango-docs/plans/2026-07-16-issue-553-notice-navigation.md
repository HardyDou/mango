# Issue 553 通知入口与登录角色权限标准交付记录

## 1. 元数据

- 任务 ID：HardyDou/mango#553
- 交付模式：STANDARD
- 需求影响：L2 - 影响所有登录用户的个人消息入口、默认角色权限、实时连接和文件基础访问边界。
- 方案风险：L2 - 同时修改共享 Admin Shell 菜单导航与 Notice Resource Registry 权限声明。
- 最终风险：L2
- 工作区决策：CREATE；任务工作区 `/Users/hardy/Work/mango-issue-553-notice-route`。

## 2. 目标与范围

- 目标：修复通知铃铛“查看全部/接收设置”跳转异常，确保所有登录用户通过内置 `ROLE_LOGIN` 获得个人消息、Realtime 与文件上传/预览基础能力，并恢复干净库 admin 的 `ROLE_ADMIN` 绑定。
- 成功条件：铃铛按 `menuCode` 解析路径；登录用户获得个人消息菜单与 API 权限；Realtime 协商、WebSocket、SSE、Polling 全部为 `LOGIN`；普通用户可上传、读取详情、预览内容、创建统一预览链接和下载，但不可访问文件管理写操作。
- 处理范围：Admin Shell/Notice 导航与资源、Realtime 访问契约、内置角色与租户启动对账、File/File Preview 基础登录访问及普通用户真实 API 验收。
- 不处理范围：不把个人消息或 Realtime 权限授予 `ROLE_ANONYMOUS`；不改变通知管理端发送、渠道、任务、记录等管理员权限；不发布 npm/Maven 版本。

## 3. 可观察系统要求

| ID | 参与者或入口 | 输入或前置条件 | 预期行为 | 失败语义 | 验收标准 |
|---|---|---|---|---|---|
| AC-001 | 已登录用户 / 顶部通知铃铛 | Notice feature 和 provider 已注册，用户点击“查看全部” | Shell 从当前菜单树按 `notice:site-message` 找到真实路径并打开消息页面 | 目标未下发时显示提示，不抛未处理路由异常 | URL 进入 `/message-center/site-message`，页面有业务内容，console 无 Vue Router error |
| AC-002 | 已登录用户 / 接收设置 | 用户点击铃铛“设置”或消息页设置入口 | Shell 打开注册的隐藏路径 `/notice/receive-setting` | 页面未注册时显示提示，不抛未处理路由异常 | 接收设置页面成功打开，接口不返回 401/403 |
| AC-003 | 内置登录角色 | 任意已验证登录主体，无额外业务角色 | `ROLE_LOGIN` 提供个人消息、公告阅读、接收设置及其业务类型只读权限，并下发消息中心可见菜单 | 匿名主体不获得个人消息权限 | 用户菜单含“我的消息/公告”，业务类型读取成功，资源契约测试通过 |
| AC-004 | Realtime 客户端 | 已登录用户协商并选择 WebSocket/SSE/Polling | 协商、建连、探测及客户端上行入口均按 `LOGIN` 放行 | 未登录仍由访问层拒绝，服务间入口仍为 `INTERNAL` | Realtime access-mode 测试通过，真实登录会话的 negotiate 与实际传输请求成功 |
| AC-005 | 普通登录用户 / 文件附件 | 用户无文件管理角色 | 允许上传、详情、预览内容、统一预览链接、下载和打包 | 匿名上传/创建预览链接被拒绝；普通用户管理列表、归档、删除和设置修改被拒绝 | 真实后端 E2E 文件权限矩阵通过 |
| AC-006 | 干净库平台管理员 | Resource Registry 已创建 admin 机构成员 | 启动对账将已有 `INSTITUTION_ADMIN` 成员幂等绑定 `ROLE_ADMIN` | 普通成员不得管理员角色 | 无安全上下文集成测试通过，干净库 admin 可创建验收用普通账号 |

## 4. 技术决定

| ID | 对应要求 | 接口/数据/权限/兼容性决定 | 影响路径 | 回滚方式 |
|---|---|---|---|---|
| TD-001 | AC-001, AC-002 | 新增 Shell 内部 `menuCode -> path` 解析；显式导航允许已注册隐藏页面，仍以当前菜单树存在性作为可访问边界；不注册第二套路由 | `admin-shell/src/runtime/menuHost.ts`、`layout/navBars/index.vue` | 恢复原导航函数与调用点 |
| TD-002 | AC-002 | Notice admin-pages 声明接收设置隐藏路由元数据，供 Shell 通配路由运行时挂载；消息中心内部的接收设置/公告使用 Notice 自有稳定路径 | `notice/src/admin-pages.ts`、`notice/src/views/site-message/index.vue` | 删除隐藏路由声明并恢复原页面导航 |
| TD-003 | AC-003 | `notice:site-message`、`notice:announcement-user` 显式绑定 `ROLE_LOGIN`；隐藏基础菜单携带 `notice:site:view/edit`、`notice:receive-setting:view/edit` 和接收设置依赖的只读 `notice:business:view`，不授予业务配置写权限或后台发送权限 | `notice-common-menu.json` | 恢复角色绑定和原 apiCodes |
| TD-004 | AC-004 | Realtime 代码保持不变，复用既有 `@ApiAccess(LOGIN)` 与 WebSocket 资源注册器；只做回归确认 | `mango-infra-realtime-starter` 测试 | 不适用，无实现变更 |
| TD-005 | AC-001 至 AC-004 | 更新 Notice、Admin Shell 和能力地图说明，明确默认登录角色与菜单编码导航契约 | 对应 README、能力地图 | 随代码回滚对应说明 |
| TD-006 | AC-003 | 正式声明 `ROLE_LOGIN`、`ROLE_ANONYMOUS`；新租户创建内置角色并复制平台租户默认绑定，启动时幂等补偿既有租户，避免全新库或存量租户缺失内置角色 | Authorization Starter/Core、System Core | 删除角色声明、租户 provisioner 扩展和启动补偿 runner |
| TD-007 | AC-001, AC-002 | 兼容旧 `mango-admin`：在后台菜单加载前等待 feature registrar，并把 admin-pages 声明的隐藏路由加入运行时路由表；同时支持历史 `route.name === menuCode` | `mango-admin/src/config/menuLoader.ts`、`admin-shell/src/runtime/menuHost.ts` | 恢复原菜单加载与解析逻辑 |
| TD-008 | AC-005 | File Preview 两个按 fileId 创建/跳转预览的接口改为 `LOGIN`；仍通过 File API 执行当前租户文件查找，不放开匿名生成 token | `mango-file-preview-starter` Controller/契约测试 | 恢复两个接口原 `PERMISSION` 注解 |
| TD-009 | AC-006 | Identity 租户 provisioner 在启动无登录上下文时扫描当前租户已有启用的 `INSTITUTION_ADMIN`，幂等恢复 `ROLE_ADMIN` 绑定 | `IdentityTenantProvisioner` | 恢复仅绑定当前创建人的逻辑 |

## 5. 实施清单

| ID | 对应决定 | 顺序 | 改动路径 | 完成条件 |
|---|---|---|---|---|
| IMPL-001 | TD-001 | 1 | `mango-ui/packages/admin-shell/src/runtime/menuHost.ts`、`layout/navBars/index.vue` | 两个铃铛动作按路径跳转，目标缺失有提示 |
| IMPL-002 | TD-002 | 2 | `mango-ui/packages/notice/src/admin-pages.ts` | 接收设置成为已注册隐藏运行时页面 |
| IMPL-003 | TD-003 | 3 | `mango-notice-starter/.../notice-common-menu.json` | 登录角色获得个人消息菜单与最小充分权限 |
| IMPL-004 | TD-006 | 4 | Authorization/System 资源、provisioner、启动补偿及集成测试 | 全新库和既有租户均有内置登录/匿名角色，登录角色继承正式默认绑定 |
| IMPL-005 | TD-007 | 5 | `mango-admin` 菜单加载器、Admin Shell 兼容解析 | source 模式与历史 admin 宿主均能装载隐藏页面 |
| IMPL-006 | TD-001, TD-003, TD-004 | 6 | Admin Shell、Notice、Authorization、System、Realtime 定向测试 | 正常、缺失、隐藏路由、角色/API 权限和四类 Realtime 入口均有回归证据 |
| IMPL-007 | TD-005 | 7 | README、能力地图、验收证据 | 公开行为和真实运行结果可追溯 |
| IMPL-008 | TD-008 | 8 | File Preview Controller/README/契约测试、文件权限 E2E | 普通用户基础文件链路可用，匿名与管理写操作边界不变 |
| IMPL-009 | TD-009 | 9 | Identity 启动对账及集成测试 | 干净库 admin 实际获得 `ROLE_ADMIN` |

## 6. 验收映射与结果

| 要求 ID | 验证方式 | 命令或步骤 | 结果 | 证据 |
|---|---|---|---|---|
| AC-001 | M10 单元测试、M13 UI 验证 | Admin Shell 42 条测试；真实浏览器点击铃铛“查看全部” | PASS | `evidence/2026-07-16-issue-553-notice-navigation/acceptance.md`、`历史验收图片已清理（可从 Git 历史恢复）` |
| AC-002 | M09 构建、M13 UI 验证 | Notice/Admin 构建；分别从消息页和铃铛打开“接收设置” | PASS | `acceptance.md`、`历史验收图片已清理（可从 Git 历史恢复）`；`GET /notice/business-types` 等请求均为 200 |
| AC-003 | M10 资源契约、M11 资源/授权集成、M12 真实 API | Notice 资源、租户 provisioner/补偿测试；全新隔离库使用无显式角色的普通成员登录并读取用户菜单 | PASS | 普通成员显式角色绑定数 0，登录仅自动叠加 `ROLE_LOGIN`；获得 3 个 Notice 菜单和 5 项最小权限；`ROLE_ANONYMOUS` 无 Notice 绑定 |
| AC-004 | M10 访问模式测试、M12 真实 API | Realtime controller/WS 注册测试；普通用户真实会话观察 negotiate 和实际 WebSocket | PASS | negotiate 200；probe 和正式 WebSocket 均收帧，无 401/403 |
| AC-005 | M10 访问模式测试、M12 真实 API | File Preview Controller 契约测试；普通用户文件权限矩阵 E2E | PASS | `acceptance.md`文件验收记录 |
| AC-006 | M11 租户初始化集成测试、M12 真实 API | 无安全上下文恢复已有机构管理员绑定；干净库 admin 仅用于准备/清理普通账号 | PASS | `IdentityUserServiceIntegrationTest`、`acceptance.md` |

## 7. 例外与剩余风险

- 当前无产品例外。发布不在本任务范围；合并后业务项目要消费修复，需在独立发布流程升级 `@mango/admin-shell`、`@mango/notice` 和相关 Maven 物料。
