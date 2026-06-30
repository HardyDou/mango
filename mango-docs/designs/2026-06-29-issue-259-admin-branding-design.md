# Issue 259 Admin 后台品牌配置详细设计

## 1. 目标与范围

本次为 Mango Admin 增加独立的后台品牌配置能力，覆盖登录页标题、副标题、登录页图片、后台名称、简称、Logo、favicon、页脚版权、备案号和联系方式。

本次不引入 CMS、站点管理、广告位或租户站点模型；不处理登录策略、权限策略、默认首页、上传限制、运行参数配置等非品牌展示能力。

## 2. 设计输入

| 来源 | 内容 | 结论 |
|---|---|---|
| GitHub Issue #259 | Admin 自身需要独立品牌配置 | 新增系统管理下的后台品牌配置模块 |
| 用户确认 | 暂不考虑站点，放在系统管理下 | 不新增站点管理依赖 |
| 当前代码 | `sys_config` 已承载系统运行配置 | 不新建表，复用 `sys_config` |
| 文件规则 | 业务配置不能持久化文件访问 URL | 文件字段保存 `mango-file:{id}` token |

## 3. 影响模块

| 模块 | 路径 | 改动 |
|---|---|---|
| 后端系统 API | `mango-system-api` | 新增保存命令和展示 VO |
| 后端系统 Core | `mango-system-core` | 新增品牌配置服务，复用 `sys_config` |
| 后端系统 Starter | `mango-system-starter` | 新增 REST 接口、菜单资源、默认配置资源 |
| 前端系统包 | `mango-ui/packages/system` | 新增后台品牌配置页面和 API |
| Admin 页面注册 | `mango-ui/packages/admin-pages` | 注册 `system/admin-branding/index` |
| Admin Shell | `mango-ui/packages/admin-shell` | 读取品牌配置并应用 Logo、标题、favicon、页脚 |
| Auth | `mango-ui/packages/auth` | 登录页支持品牌图片 |
| Admin App | `mango-ui/apps/mango-admin` | 浏览器标题使用动态品牌名称 |

## 4. 数据设计

不新增数据库表。每个品牌字段映射一条 `sys_config`：

| 配置 Key | 含义 | 默认值 |
|---|---|---|
| `admin.branding.enabled` | 是否启用后台品牌配置 | `true` |
| `admin.branding.title` | 后台名称/浏览器标题 | `Mango Admin` |
| `admin.branding.shortTitle` | 后台简称/折叠 Logo 文案 | `Mango` |
| `admin.branding.subtitle` | 后台副标题 | `企业级管理平台` |
| `admin.branding.loginTitle` | 登录页主标题 | `Mango Admin` |
| `admin.branding.loginSubtitle` | 登录页副标题 | `企业级管理平台` |
| `admin.branding.logoFile` | 后台 Logo 文件 token | 空 |
| `admin.branding.faviconFile` | favicon 文件 token | 空 |
| `admin.branding.loginImageFile` | 登录页图片文件 token | 空 |
| `admin.branding.footerCopyright` | 页脚版权 | `© Mango` |
| `admin.branding.icp` | 备案号 | 空 |
| `admin.branding.contact` | 联系方式 | 空 |

文件字段只保存文件中心 token，不保存预览 URL。前端运行时根据 token 调用文件中心接口派生临时展示地址。

## 5. 接口设计

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| `GET` | `/system/admin-branding` | `system:admin-branding:query` | 后台配置页读取品牌配置 |
| `GET` | `/system/admin-branding/public` | `PUBLIC` | 登录页和 Shell 未登录/初始化时读取品牌配置 |
| `PUT` | `/system/admin-branding` | `system:admin-branding:edit` | 保存品牌配置 |

接口返回语义化 `AdminBrandingVO`，前端不感知底层多条 `sys_config` 的拆分。

## 6. 前端设计

配置页放在“系统管理 / 后台品牌配置”，使用 Element Plus 表单：

- 基础信息：后台名称、后台简称、副标题。
- 登录页展示：登录页标题、副标题、登录页图片。
- 品牌资源：Logo、favicon。
- 页脚信息：版权、备案号、联系方式。

运行时新增品牌状态：

- 启动时读取 public 接口。
- 读取失败时使用内置默认值，不阻断登录页和后台框架。
- 文件 token 派生为运行时图片 URL 后，只用于页面展示。
- 保存配置后刷新运行时品牌，页面立即看到效果。

## 7. 异常与边界

| 场景 | 处理 |
|---|---|
| 品牌配置不存在 | 返回默认值 |
| 配置禁用或值为空 | 返回默认值或空值，保证页面可用 |
| public 接口失败 | 前端静默降级到默认品牌 |
| 文件预览失败 | 对应图片不展示，文字品牌仍可用 |
| 保存空文件字段 | 清空文件 token，不保存 URL |

## 8. 验证方式

- 后端单测覆盖默认值读取、保存 insert/update、文件 token 不转 URL。
- 前端包构建覆盖 `@mango/system`、`@mango/admin-shell`、`@mango/auth`。
- 页面验证覆盖配置页读取、保存、登录页展示、Logo/favcion/页脚回显。
- PR 前执行 PMO 文档检查与受影响样式检查。

## 9. 风险与取舍

- 复用 `sys_config` 可以避免新增表和 migration，但品牌字段以多条配置存储，必须通过语义化 API 屏蔽细节。
- favicon 和图片展示依赖文件中心预览能力；失败时应降级，不影响登录。
- Admin Shell 和 Auth 都会消费品牌配置，需要保证默认值稳定，避免未登录阶段因接口失败导致空白。
