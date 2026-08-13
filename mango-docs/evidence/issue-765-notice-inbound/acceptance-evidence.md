# Issue #765 管理端与接口验收证据

## 1. 验收范围

- 页面：`/notice/inbound`，菜单“接收消息”，列表、筛选、分页、详情、附件下载入口。
- 接口：企业微信 GET/POST、公网邮箱 POST、管理员列表和详情。
- 权限：公网回调匿名；管理查询登录及 `notice:inbound:view`。
- 数据：隔离库入站表、附件表和真实管理查询结果。
- 部署形态：本地单体后端与 `mango-admin`。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:30038`
- 后端地址：`http://127.0.0.1:18038`
- 数据库或租户：`mango_dev_mango_765_notice_inbound_038`，`tenantId=1` / `tenantCode=default`
- 测试账号：初始化管理员；不记录密码或 Token
- 浏览器：本会话未提供 Browser skill 所需控制接口，视觉走查仍为 `BLOCKED`；接口和数据库真实回读已完成

## 3. 功能验收记录

| 台账 ID | 用例 ID | 页面/接口 | 功能点 | 测试数据 | 关键断言 | UI/交互检查 | console/network 结果 | 截图/trace/日志 | 结论 |
|---|---|---|---|---|---|---|---|---|---|
| VAL-765-001 | TC-765001 | GET/POST `/notice/inbound-callbacks/public`；POST `/notice/inbound-mail-callbacks/public` | 匿名回调边界 | 无 Cookie、无 Authorization、不存在的渠道配置 ID | 三个入口均进入渠道配置业务校验；没有返回 401；不存在配置被拒绝，未绕过启用状态或租户绑定 | 非 UI；验证返回错误语义，不把 HTTP 200 包装误判为接收成功 | 三个公网请求无网络 4xx/5xx，响应业务 `code=400`；框架业务错误包装行为已记录 | `automated.md` 的“HTTP 与数据库实测” | PASS |
| VAL-765-002 | TC-765002 | GET `/notice/inbound-messages` | 管理认证边界 | 完全匿名请求 | 匿名请求 HTTP 401；原因：管理列表不得向匿名用户开放 | 非 UI；权限失败语义明确 | network HTTP 401，响应 Unauthorized | `automated.md` 的“HTTP 与数据库实测” | PASS |
| VAL-765-003 | TC-765003 | POST `/auth/login` 后 GET `/notice/inbound-messages` | 管理员查询真实入站列表 | 初始化管理员、默认租户、pageNum=1、pageSize=10 | 登录成功；列表业务 `code=200`；返回三封真实管理员入站回复，状态均为 `BROADCASTED` | 通过 HTTP 验证真实数据合同；浏览器列表视觉另列阻塞 | login 与列表 network 均 HTTP 200，无接口 4xx/5xx | `automated.md` 真实入站证据；临时响应不提交 Token/Cookie | PASS |
| VAL-765-004 | TC-765004 | `/notice/inbound` 与菜单“接收消息” | 搜索、重置、分页、详情、附件 File 下载、空/错/权限态视觉结果 | 真实入站列表已由 API 回读；需要浏览器控制接口补充视觉证据 | 本会话没有 Browser skill 要求的浏览器控制接口，无法证明真实 DOM、交互、截图、console 和 network | 代码和组件测试存在，但不能替代真实浏览器页面验收 | 未取得浏览器 console/network 证据；HTTP 层结果见前三项 | 无截图；阻塞事实记录于本文件 | BLOCKED |
| VAL-765-005 | TC-765005 | 管理端“接收消息”列表 | 显示真实收到的邮件消息 | Notice POP3 真实轮询；消息 `2087866808973164545`、附件 `1.pdf` | 管理列表返回三封真实入站回复，均为 `BROADCASTED`；详情返回 `2222` 正文/HTML 与 File ID `2087866809128353794` | HTTP/API 与数据库真实回读通过；浏览器 DOM/截图仍受控制接口缺失阻塞 | 列表与详情 HTTP 200、业务 `code=200`；File 详情为 `PRIVATE` | `automated.md` 真实入站证据 | PASS（接口/数据）；UI 视觉另列 BLOCKED |

## 4. 回归抽查记录

| 模块 | 页面 | 功能点 1 | 功能点 2 | UI 细节 | 截图/trace | 结论 |
|---|---|---|---|---|---|---|
| `@mango/notice` | `/notice/inbound` 注册合同 | 页面映射和菜单 component key 存在 | HTML 正文按文本节点显示且附件下载只使用 File ID | 真实浏览器视觉走查因控制接口不可用而阻塞 | 无截图；前端 10 文件、38 测试及 build 通过 | BLOCKED |

## 5. 未验证项和风险

| 项目 | 原因 | 影响 | 后续处理 | 用户确认 |
|---|---|---|---|---|
| 管理端真实浏览器验收 | 当前会话没有可调用的 Browser 控制接口 | 无法证明菜单激活、空/错误态视觉、搜索分页详情、附件下载及 console/network | 在有浏览器控制能力和登录态的会话执行 `/notice/inbound` 走查并补截图 | 待确认 |
| 管理端真实消息可见性 | 浏览器控制接口不可用 | 已通过管理员列表/详情 API 和数据库证明真实消息可见；无法补充 DOM/截图证据 | 在具备浏览器控制能力的会话补 `/notice/inbound` 视觉走查 | 待确认 |
| 126 IMAP 新邮件同步 | 120 秒内未出现在 IMAP 视图 | 完整 IMAP 自发自收未证明 | 延长生产等价观察窗口，验证游标推进和幂等 | 待确认 |

## 6. 业务开发交接输出

| 输出对象 | 交接内容 | 材料路径 | 执行入口 | 数据/账号边界 | 失败/例外处理 | 状态 |
|---|---|---|---|---|---|---|
| Mango 业务开发者 | 邮箱拉取/Webhook、企微回调、存储、File ID、广播事件、管理查询的能力和验证边界 | `mango/mango-platform/mango-notice/README.md`、本 evidence 目录 | 项目内 Mango CLI；Notice Maven verify；`@mango/notice` build/test | 每个渠道必须绑定启用租户配置；密钥使用 Secret 管理；测试数据独立 | 回调业务错误检查渠道配置、验签和 provider；IMAP 同步和 UI 阻塞不得包装为通过 | PASS |
