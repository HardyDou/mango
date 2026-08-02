---
documentId: EVIDENCE-ACCOUNT-643
documentType: delivery-record
pmoVersion: 1.3.8
schemaRevision: 1
riskLevel: L3
status: PASS
action: DONE
owner: Mango Auth 实施负责人
upstreamDocumentId: PLAN-ACCOUNT-643
---

# Issue 643 验收证据

## 1. 验收范围

- 页面：登录页、个人中心页面内导航、个人资料头像上传、账号安全、第三方授权、第三方登录配置。
- 接口：当前用户资料、联系方式验证码与密码校验、第三方 Provider 查询与配置、`LOGIN` / `BIND_CURRENT` 授权发起、授权回调编排。
- 权限：登录态绑定、租户和应用隔离、管理员配置权限、未登录登录入口。
- 数据：实名字段、认证默认值、第三方配置密文、第三方绑定的租户和应用归属。
- 部署形态：Mango 主 admin、四个独立管理微前端、CLI full/custom/add/module 生成与存量 Mango 应用生命周期。

## 2. 执行环境

- 前端地址：`http://127.0.0.1:30015/`。
- 后端地址：`http://127.0.0.1:18015/`，最终健康状态 `UP`。
- 数据库：`mango_dev_mango_issue_643_identity_providers_015`，generation 3 已完成 bootstrap verify 并稳定运行。
- 测试账号：芒果集团 `admin`，`tenantId=1`，`appCode=internal-admin`；证据不记录密码或 token。
- 浏览器：Chrome，Playwright CLI 1.59.0-alpha，隔离会话 `issue643-profile` 与 `i643final`；桌面和 390px 窄屏均完成走查。

## 3. 结果摘要

- `mango-auth-core` 28 项、`mango-identity-core` 31 项、`mango-auth-starter` 44 项测试通过。
- Authorization Resource Sync 19 项、Resource Sync Starter 12 项、Infra Web Starter 31 项测试通过。
- `@mango/auth` 9 项测试、`@mango/common` 290 项测试和两包生产构建通过；38 个前端 workspace 项目生产构建通过。
- ESLint、Stylelint、Typecheck、admin 样式、模块样式、前端架构、前端边界、测试质量、能力文档和 `git diff --check` 均通过。
- 个人中心内部使用个人资料、账号安全、第三方授权、修改密码、主题设置五项导航；390px 视口改为顶部换行切换，无横向溢出。
- 顶部常驻用户区只显示头像、用户名和下拉箭头；紧凑下拉展示头像、姓名和“部门名称｜公司名称”，无主组织时明确显示“部门未设置｜公司名称”。
- 修改密码已进入个人中心子页，原 `/password` 路由继续兼容；右上角不再显示独立设置齿轮，主题设置从头像下拉进入个人中心子页，主题切换即时生效并在验收后恢复默认。
- 头像选择前没有文件上传请求；保存资料时文件上传与资料更新均为 200，数据库只保存 `mango-file:{id}`；刷新后个人中心和顶部用户头像都通过受保护文件接口回显。
- 企业微信 `LOGIN` 和已登录 `BIND_CURRENT` 都生成 `open.work.weixin.qq.com/wwopen/sso/qrConnect` 地址，包含应用、Agent、回调和一次性 state 参数；证据只保留结构化布尔断言，不保留参数值。
- 实名资料真实保存并刷新回显后已清空恢复；显式 SQL 更新保证 nullable 字段会真实写入空值，测试账号未残留 `E2E_ISSUE643`。
- 最终验收未调用企业微信成员目录、搜索或成员详情接口，没有读取或操作任何企业微信成员。

## 4. 功能验收记录

| 台账 ID     | 用例 ID   | 页面/接口                                                                                  | 功能点                                                       | 测试数据                                                          | 关键断言                                                                                                                                                             | UI/交互检查                                                                                | console/network 结果                                                                                                                            | 截图/trace/日志                                                                                                           | 结论 |
| ----------- | --------- | ------------------------------------------------------------------------------------------ | ------------------------------------------------------------ | ----------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------- | ---- |
| ACC-643-001 | TC-643001 | `/#/profile`、`GET/PUT /identity/me/profile`                                               | 实名信息默认值、保存、回显和清空                             | admin；临时姓名前缀 `E2E_ISSUE643`，完成后精确清理                | 姓名、证件类型、证件号码可录入；默认认证状态为未认证、来源为无；保存刷新后回显；姓名和证件字段清空后数据库恢复空值                                                   | 字段分组、标签、输入框、状态标签和保存按钮完整，页面无溢出或遮挡                           | profile 请求状态为 200；Errors: 0，Warnings: 0                                                                                                  | `profile-real-name.png`；Identity Core 31 项测试                                                                          | PASS |
| ACC-643-002 | TC-643002 | `/#/profile` 联系方式弹窗                                                                  | 修改手机号和邮箱的双重校验入口                               | admin 当前脱敏手机号与邮箱；未提交新值                            | 两个弹窗都要求新联系方式、验证码和当前密码；当前密码输入框的 DOM `type=password`                                                                                     | 两个弹窗标题、必填标识、发送验证码、取消和确认修改按钮完整；仅打开并取消，没有修改账号数据 | 页面会话 Errors: 0；未提交写请求，未产生 4xx/5xx                                                                                                | `profile-phone-change-dialog.png`、`profile-email-change-dialog.png`                                                      | PASS |
| ACC-643-003 | TC-643003 | `/#/profile` 第三方授权页签、`GET /auth/providers`、`GET /identity/me/external-identities` | 已登录用户管理当前应用第三方授权                             | admin、tenant 1、internal-admin                                   | 企业微信显示未绑定且绑定可用；钉钉显示未配置且绑定禁用；两种状态属于不同行，不混淆                                                                                   | 页签、刷新、表头、状态标签和操作按钮完整；布局稳定                                         | 三个页面请求状态均为 200；Errors: 0，Warnings: 0                                                                                                | `profile-third-party-authorizations.png`                                                                                  | PASS |
| ACC-643-004 | TC-643004 | `/#/system/provider-configs`、`GET /auth/provider-configs`                                 | 管理员查看企业微信和钉钉配置状态                             | internal-admin；隔离测试数据库中的 Provider 配置                  | 企业微信完整、已启用、密钥已配置；钉钉待完善、未启用、密钥未配置；列表不返回密钥明文                                                                                 | 系统菜单、应用编码查询、双 Provider 表格和配置入口完整                                     | 配置查询状态为 200；Errors: 0，Warnings: 0                                                                                                      | `provider-config-list.png`                                                                                                | PASS |
| ACC-643-005 | TC-643005 | `/#/login`、`POST /auth/providers/authorize`                                               | 未登录企业微信登录与已登录当前账号绑定                       | LOGIN 与 BIND_CURRENT；合法回调地址；一次性 state                 | 两种 intent 均返回企业微信二维码授权域名和路径；应用、Agent、回调、state 参数全部存在；有效期 600 秒；BIND_CURRENT 使用真实 Bearer 安全主体成功                      | 登录页仅展示当前可用的企业微信登录按钮；钉钉未配置时不展示登录入口                         | 登录页 Provider 查询状态为 200；匿名会话 Errors: 0；授权发起未调用厂商成员接口                                                                  | `login-wecom-entry.png`；结构化授权地址断言输出                                                                           | PASS |
| ACC-643-006 | TC-643006 | Auth/Identity Core 与安全入口                                                              | 授权状态一次性消费、绑定冲突、租户与应用隔离、资料空值持久化 | JUnit fixture、H2 隔离库、不同租户和应用安全主体                  | state/ticket 只能消费一次；跨租户或跨应用绑定拒绝；未登录 BIND_CURRENT 拒绝；MyBatis nullable 字段显式更新                                                           | 不适用：后端单元和集成测试验证业务与安全边界                                               | 测试不访问企业微信成员服务；59 项 core 测试全部通过                                                                                             | Maven Surefire 输出                                                                                                       | PASS |
| ACC-643-007 | TC-643007 | Flyway、Bootstrap、真实 MySQL runtime                                                      | 实名和绑定 schema、空库形成、重启稳定性                      | worktree 独立 MySQL 8.4 数据库；generation 3                      | Identity V1 历史 migration 不变，V2 增加实名和 appCode；Auth V1 建立 Provider 配置与密文列；bootstrap verify 后 runtime 健康                                         | 不适用：数据库与服务生命周期验收                                                           | 健康检查返回 `UP`，数据库组件 `UP`；未记录连接密码                                                                                              | migration contract、bootstrap generation 3 和 health 输出                                                                 | PASS |
| ACC-643-008 | TC-643008 | Mango CLI、starter、主仓和生成项目                                                         | Issue 687/688 启动链修复与 Mango 多场景消费                  | 主仓、full/custom/add/module、自动发现、存量自管 `mango.dev.json` | 当前分支源码 CLI 使用明确 runtime 生命周期；非 Web bootstrap 不装配 MVC；新旧入口边界明确；资源同步可重复；真实执行 bootstrap verify generation 3 后稳定启动 runtime | 不适用：CLI 与后端生命周期回归                                                             | CLI 60 项回归、63 模块初始 Reactor、合并最新 main 后 46 模块受影响 Reactor 和相关 starter 测试全部通过；机器上的全局旧 CLI 不作为本分支交付入口 | CLI、Web/Authorization/Resource Sync 验证输出                                                                             | PASS |
| ACC-643-009 | TC-643009 | 主 admin 与四个独立管理微前端                                                              | auth 页面路由、样式独立消费和生产构建                        | admin、rbac、workflow、template、cms 应用                         | 每个微前端显式引入 `@mango/auth/style.css`；独立构建产物包含 `mango-auth` CSS chunk；frontend boundary 无新增违规                                                    | 主 admin 页面截图布局正确；微前端样式通过显式 package 入口进入隔离容器                     | 38 workspace build 成功；Typecheck ratchet PASS；frontend boundaries PASS                                                                       | 前端构建、架构、样式和边界门禁输出                                                                                        | PASS |
| ACC-643-010 | TC-643010 | `/#/profile`、文件上传与受保护下载接口                                                     | 页面内导航、头像选择/上传/移除及刷新回显                     | admin；两个仅用于本用例的头像文件，验收后删除                     | 页面导航不进入框架主菜单；选图时上传请求数为 0；保存时上传和资料更新均为 200；数据库只保存 `mango-file:{id}`；刷新后个人中心与顶部用户区均回显                       | 桌面左侧五项导航、右侧基础/实名信息分组清晰；390px 变为顶部五项换行导航且无横向溢出        | 最终 Errors: 0，Warnings: 0；相关请求全部 200                                                                                                   | `profile-avatar-upload.png`、`profile-settings-navigation.png`、`profile-settings-mobile.png`；`MangoAvatar` 3 项组件测试 | PASS |
| ACC-643-011 | TC-643011 | 顶部用户区、`/#/profile?tab=password`、`/#/profile?tab=theme`、`/#/password`               | 用户摘要、修改密码和页内主题设置                             | admin；当前成员无主组织；主题布局切换后恢复默认                   | 顶栏常驻区只有头像、用户名和箭头且没有独立设置齿轮；下拉显示“部门未设置｜芒果集团”；个人中心、修改密码、主题设置、退出四个动作可用；修改密码子页和旧路由均可访问；主题切换即时生效 | 下拉为白底、深色文字和细分隔线，默认各行透明，仅当前悬停行显示浅灰；密码和主题页面位于个人中心右侧；Vue 插槽组件保持 raw，不产生响应式组件警告 | 定向 Playwright 4 项通过；最终 Errors: 0，Warnings: 0；主题设置期间路由保持在个人中心，恢复后布局为 defaults | `profile-header-user-dropdown.png`、`profile-password.png`、`profile-theme.png`；`@mango/auth` 配置单测 | PASS |

## 5. 回归抽查记录

| 模块          | 页面           | 功能点 1                               | 功能点 2                                           | UI 细节                                                 | 截图/trace                                                                                                                                                                           | 结论 |
| ------------- | -------------- | -------------------------------------- | -------------------------------------------------- | ------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ---- |
| `@mango/auth` | 登录页         | 账号密码登录入口保留                   | 企业微信入口按配置展示                             | 卡片、表单和第三方入口对齐                              | `login-wecom-entry.png`                                                                                                                                                              | PASS |
| `@mango/auth` | 个人中心       | 页面内五项设置导航、基本资料与实名字段 | 头像上传、联系方式、第三方授权、修改密码和主题设置 | 桌面左右栏与 390px 顶部换行导航均无重叠、遮挡或横向溢出 | `profile-settings-navigation.png`、`profile-avatar-upload.png`、`profile-settings-mobile.png`、`profile-third-party-authorizations.png`、`profile-password.png`、`profile-theme.png` | PASS |
| admin shell   | 顶部用户区     | 头像、用户名和下拉箭头                 | 部门｜公司摘要与四项功能菜单                       | 无独立设置齿轮；白底分组、深色文字、细分隔线与单行浅灰悬停，组织缺省文案明确 | `profile-header-user-dropdown.png`                                                                                                                                                   | PASS |
| 管理端聚合    | 第三方登录配置 | 企业微信完整/启用                      | 钉钉待完善/禁用                                    | 菜单、查询区和表格列对齐                                | `provider-config-list.png`                                                                                                                                                           | PASS |

## 6. 未验证项和风险

| 项目                               | 原因                                                             | 影响                                                                   | 后续处理                                                     | 用户确认                                 |
| ---------------------------------- | ---------------------------------------------------------------- | ---------------------------------------------------------------------- | ------------------------------------------------------------ | ---------------------------------------- |
| 企业微信真实回调完成和成员身份换取 | 当前没有“豆晓雨”本人的 OAuth code/userId，且用户禁止读取其他成员 | 已证明授权发起、状态保护和回调编排，但未宣称完成本人扫码后的厂商端闭环 | 仅在豆晓雨本人扫码并提供一次性回调条件后执行，不查询成员列表 | 用户明确限定只能使用豆晓雨，已遵守       |
| 钉钉真实沙箱授权                   | 本次没有提供钉钉应用凭据，运行态保持未配置                       | 已完成协议适配、配置管理和未配置禁用行为，未宣称厂商端扫码闭环         | 配置合法钉钉沙箱后复用同一授权回调流程验收                   | 用户确认本次按建议实施企业微信和钉钉能力 |
| Node 运行版本                      | 本机为 Node 26，仓库声明范围为 Node 22                           | pnpm 输出 engine warning，但全部构建和质量门禁完成                     | 正式 CI/发布使用仓库声明的 Node 22；本次不发布               | 本轮没有发布授权                         |

## 7. 业务开发交接输出

| 输出对象                | 交接内容                                                                                                  | 材料路径                                                                                                                         | 执行入口                                                          | 数据/账号边界                                                                       | 失败/例外处理                                                                  | 状态 |
| ----------------------- | --------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------- | ----------------------------------------------------------------------------------- | ------------------------------------------------------------------------------ | ---- |
| 基于 Mango 的业务开发者 | 使用 Provider 配置表管理企业微信/钉钉；宿主注册公开回调、个人中心和管理员配置页；微前端显式引入 auth 样式 | `mango/mango-platform/mango-auth/README.md`、`mango/mango-platform/mango-identity/README.md`、`mango-ui/packages/auth/README.md` | Mango CLI full/custom/add/module；`mango dev start`；`pnpm build` | 每个租户和 appCode 独立配置；密钥只进加密存储，不写源码；实名和绑定遵守当前安全主体 | 配置不完整时 Provider 不可用；回调 state 失效或重复时拒绝；跨租户/应用绑定拒绝 | DONE |

## 8. 敏感信息

- 企业微信配置值仅保存在隔离测试数据库，未写入源码、测试、日志、文档或截图。
- 所有截图只展示脱敏手机号、脱敏邮箱和 Provider 配置状态，不展示证件号、密钥、token、企业标识或 Agent 标识。
- 本次没有调用企业微信成员目录、搜索或成员详情接口。
- 头像验收只清理本用例上传的两个文件；admin 头像资料已恢复为空，没有删除或修改其他用户文件。
