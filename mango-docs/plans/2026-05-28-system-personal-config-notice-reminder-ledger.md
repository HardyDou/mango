# System 个人参数配置与 Notice 提醒设置交付台账

## 交付契约

- 目标：新增 system 通用个人参数配置能力，并让 notice 接收设置中的提醒设置持久化到服务端；小铃铛按服务端提醒设置执行弹窗与语音提示。
- 范围：`mango-system` API/Core/Starter、system Flyway migration、notice API 类型与接收设置客户端页签、notice bell 运行时配置、管理后台顶部铃铛配置加载。
- 不做：不重构 realtime 单例通道架构；不把配置写入 localStorage；不改接收规则发送判断逻辑；不处理系统消息存储加密。
- 设计输入：用户要求“提醒设置”配置存服务端，system 增加支持多租户的个人参数配置模块，用 `group/bizType/key/value` 表示领域配置，notice 组件消费该配置。
- 交付物：后端表与接口、前端提醒设置页签、铃铛弹窗/语音配置消费、单元测试与构建验证。
- 验收方式：台账检查、后端 system 模块测试、notice 组件测试、mango-admin 构建。
- 风险与限制：当前工作区存在大量历史未提交改动，本次提交只暂存本任务相关文件；完整后端全仓检查可能受既有改动影响。

## 轻量设计说明

### 模块边界

- `mango-system-api`：声明个人参数配置 API 契约、Command、Query、VO。
- `mango-system-core`：实现 `sys_personal_config` 实体、Mapper、Service 和 Flyway migration。
- `mango-system-starter`：暴露 `/system/personal-configs` 登录接口，只操作当前登录用户。
- `mango-ui/packages/notice`：封装 notice 提醒设置默认值、系统个人参数 API 调用、接收设置页签和铃铛消费逻辑。
- `mango-ui/apps/mango-admin`：只负责为 `NoticeBell` 提供当前用户 realtime identity 和加载提醒设置，不在宿主维护重复业务逻辑。

### 接口变化

- `GET /system/personal-configs`：按 `groupCode`、`bizType`、`configKey` 查询当前用户配置列表。
- `GET /system/personal-configs/value`：查询当前用户单个配置。
- `POST /system/personal-configs`：按当前租户、当前用户、`groupCode/bizType/configKey` upsert 配置。
- `DELETE /system/personal-configs`：删除当前用户单个配置。

### 数据变化

- 新增 `sys_personal_config` 表。
- 唯一约束：`tenant_id/user_id/group_code/biz_type/config_key`。
- `config_value` 支持 JSON 字符串，`value_type` 标识值类型。

### 页面变化

- 接收设置增加 `提醒设置` 页签。
- 提醒设置字段：是否弹窗提示、弹窗位置、是否语音提示、语音提示内容。
- 布局采用 Element Plus 标准 `el-card`、`el-form`、`el-row`、`el-col`。

## 原子验收项

| ID | 来源 | 要求 | 设计决策 | 交付物 | 验收方式 | 状态 | 证据文件 |
|---|---|---|---|---|---|---|---|
| SYS-PC-001 | 用户要求 | system 增加通用个人参数配置模块 | 使用 `sys_personal_config` 表，支持租户、用户、group、bizType、key、value | migration、Entity、Mapper、Service | Maven 测试 | DONE | mango/mango-platform/mango-system |
| SYS-PC-002 | 用户要求 | 提供统一 API 给所有业务使用 | `/system/personal-configs` 当前用户自服务接口 | API、Controller | Maven 测试/编译 | DONE | mango/mango-platform/mango-system |
| NOTICE-REM-001 | 用户要求 | 接收设置增加提醒设置页签 | notice client 组件新增第三个 tab | NoticeClientReceiveSetting.vue | Vitest/构建 | DONE | mango-ui/packages/notice/src/client |
| NOTICE-REM-002 | 用户要求 | 配置服务端持久化，切换浏览器/PC 仍生效 | notice 调用 system personal config API 保存 JSON | notice API helpers/types | Vitest/构建 | DONE | mango-ui/packages/notice/src/api |
| NOTICE-REM-003 | 用户要求 | 小铃铛按提醒设置控制弹窗位置和语音 | `NoticeClientBellRuntimeConfig` 支持 `popupPlacement/voiceEnabled/voiceText` | NoticeClientBell.vue/types | Vitest | DONE | mango-ui/packages/notice/src/client |
| NOTICE-REM-004 | 用户要求 | 弹窗内容和小铃铛列表单条格式一致，可去掉 logo | Element Plus Notification 使用标题、摘要、业务域与时间，不显示头像 | NoticeClientBell.vue | Vitest | DONE | mango-ui/packages/notice/src/client |
| ADMIN-REM-001 | 用户要求 | 管理后台顶部铃铛消费服务端提醒配置 | 宿主调用 notice helper 加载配置并传给 Bell | navBars/index.vue | 前端构建 | DONE | mango-ui/apps/mango-admin/src/layout |
| VERIFY-001 | PMO | 交付前执行台账检查和对应验证 | 运行 delivery-contract-check、Maven、Vitest、build | 验证命令输出 | 命令结果 | DONE | 本文件 |
