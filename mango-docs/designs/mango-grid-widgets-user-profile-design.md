# 用户信息系统小组件设计方案

## 1. 目标

在 `@mango/grid-widgets` 中新增 Mango 预制小组件“用户信息”，用于工作台展示当前登录人的基础信息，并提供“个人中心”和“修改密码”两个已有功能页面入口。

## 2. 范围

- 新增 `system.user-profile` 系统小组件。
- 将快捷入口小组件从 `src/system` 根目录整理到 `src/system/quick-entry/`，保持公开导出和调用方式不变。
- 用户信息小组件相关文件放入 `src/system/user-profile/` 独立目录。
- 工作台默认布局增加用户信息小组件。
- 更新 `@mango/grid-widgets` README 和能力地图。

## 3. 不做什么

- 不修改 `@mango/grid-layout`。
- 不新增后端接口、数据库表、菜单、按钮权限或角色授权数据。
- 不在小组件内读取宿主 store、router 或全局登录态。
- 不把修改密码做成弹框表单，本次两个按钮都跳转到已有功能页面。
- 不改变快捷入口现有运行行为。

## 4. 设计输入

- 用户要求“用户信息”作为系统小组件开发。
- 用户要求每个小组件文件放在独立文件夹内，快捷入口也同步整理。
- 用户确认“个人中心”和“修改密码”两个按钮都跳转到已有功能页面。
- 现有个人中心页面路径为 `/profile`，修改密码页面路径为 `/password`。
- 现有小组件运行时通过 `MangoWidgetRuntimeContext.navigate` 处理单体和微前端跳转。

## 5. 设计说明

### 5.1 影响模块

| 模块 | 影响 |
|------|------|
| `@mango/grid-widgets` | 新增用户信息小组件，整理快捷入口目录，补充类型、样式、导出和 README |
| `@mango/admin-shell` 工作台 | 默认布局加入用户信息小组件，并通过 runtime 传入当前登录人信息 |
| `mango-docs` | 增加设计说明和交付台账，更新能力地图 |

### 5.2 组件目录

```text
mango-ui/packages/grid-widgets/src/system/
├─ quick-entry/
│  ├─ QuickEntryWidget.vue
│  ├─ index.ts
│  └─ quick-entry.ts
├─ user-profile/
│  ├─ UserProfileWidget.vue
│  ├─ index.ts
│  └─ user-profile.ts
└─ index.ts
```

### 5.3 交互设计

用户信息小组件展示：

- 头像：优先使用 `runtime.user.avatar`。
- 姓名：优先 `nickname`，其次 `username`。
- 账号：展示 `username`。
- 组织：优先展示 `tenantName`，其次 `tenantCode` 或 `tenantId`。
- 信息块：展示所属租户、租户编码、当前角色和应用标识，缺失时使用兜底文案。
- 操作：`个人中心` 跳转 `/profile`，`修改密码` 跳转 `/password`。

小组件自身不做业务表单，不处理保存、不发起接口请求。

### 5.4 接口变化

无后端接口变化。

前端类型扩展：

- `MangoWidgetRuntimeUser` 增加 `avatar`、`roles`、`appCode`。
- `MangoWidgetRuntimeTenant` 增加 `tenantCode`、`tenantName`。
- 新增 `UserProfileWidgetProps`。

### 5.5 数据变化

无数据库变化。

用户信息小组件只消费 `runtime.user` 和 `runtime.tenant`，不持久化任何数据。工作台默认布局新增一条 `system.user-profile` 布局项；用户保存个人布局后仍以个人布局数据为准。

### 5.6 菜单/页面/权限变化

不新增菜单、页面或权限。两个按钮复用已有页面：

- `/profile`
- `/password`

### 5.7 测试范围

- `@mango/grid-widgets` 包构建。
- `@mango/admin-shell` 构建。
- admin 样式聚合检查。
- package exports 检查。
- 能力文档检查。
- 交付契约检查。

## 6. 风险与限制

- 用户信息字段取决于宿主传入的 runtime，缺失时使用兜底文案。
- 跳转能力由宿主 `runtime.navigate` 承担，微前端场景需要宿主继续保持现有跳转适配。
- 快捷入口目录整理会移动文件路径，但包公开导出保持不变。
