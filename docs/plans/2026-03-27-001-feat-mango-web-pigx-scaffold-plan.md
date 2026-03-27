---
title: "feat: Mango 前端 1:1 复刻 pigx-ui + Playwright E2E"
type: feat
status: active
date: 2026-03-27
origin: docs/plans/2026-03-26-002-feat-mango-implementation-plan.md
deepened: 2026-03-27
---

# Mango 前端 1:1 复刻 pigx-ui + Playwright E2E

## 概述

**核心目标**：mango-web 前端 1:1 复刻 pigx-ui 前端基础框架，接口遵循 Mango 规范

**参考项目**：
- 前端参考：`/Users/hardy/Work/pigx/pigx-ui`
- 后端参考：`/Users/hardy/Work/pigx/pigx`（接口规则用 Mango 自己的规范）

**依赖关系**：阶段 3 → 阶段 4 → 阶段 5（本计划），Phase 6 为本计划范围

---

## 技术栈（复刻 pigx-ui）

| 组件 | 技术 | 版本 |
|------|------|------|
| 前端框架 | Vue 3 | 3.5.13 |
| 构建工具 | Vite | 4.x |
| UI 组件库 | Element Plus | 2.5.5 |
| 状态管理 | Pinia | 2.0.32 |
| 路由 | Vue Router | 4.x |
| 国际化 | Vue I18n | 9.2.2 |
| CSS | Tailwind CSS + SCSS | 3.4.x |
| 测试框架 | Playwright | 1.x |
| 包管理器 | pnpm | 8.x |
| TypeScript | TypeScript | 4.9.x |

---

## 需求追踪

- R1: mango-web 1:1 复刻 pigx-ui 前端框架
- R2: 菜单系统（后端控制路由）
- R3: 主题配置（深色模式、布局切换）
- R4: 国际化（中/英文）
- R5: API 代理（/api 代理到后端）
- R6: 四种布局（defaults/classic/transverse/columns）
- R7: Playwright E2E 测试
- R8: 无障碍支持（ARIA 标签、键盘导航、屏幕阅读器）
- R9: UI 交互状态（loading/error/empty/success 状态）

---

## 范围边界

### 包含
- 1:1 复刻 pigx-ui 前端框架结构
- 路由系统（后端控制路由）
- 状态管理（Pinia stores）
- 主题配置系统
- 国际化系统
- 布局系统（四种布局）
- API 请求封装（axios）
- 组件库（Element Plus 基础组件）
- Playwright E2E 测试

### 不包含
- M* 组件实现（ui-rules.md 规定的 MButton/MInput 等包装器，属于业务开发阶段；本 scaffold 直接使用 el-* 以 1:1 复刻 pigx-ui）
- 后端接口实现（后端单独设计，遵循 Mango API 规范）
- 完整业务页面（属于业务开发）
- 移动端适配（本版本聚焦桌面端）

### 设计决策：M* 组件 vs el-* 直接使用

| 决策 | 说明 |
|------|------|
| 本 scaffold 使用 el-* | 1:1 复刻 pigx-ui 基础框架，直接使用 el-button/el-input 等 Element Plus 组件 |
| ui-rules.md M* 要求 | 属于业务页面开发规范，在 `mango-web` 业务项目中实施，不在本 scaffold 范围 |
| ESLint 限制 | `no-restricted-imports` 规则在 mango-web 业务项目中启用，不在 scaffold 中启用 |

---

## 关键技术与研究结论

### pigx-ui 核心技术模式

| 模式 | 实现方式 | 注意事项 |
|------|---------|---------|
| 后端控制路由 | `isRequestRoutes: true` + `/api/sys/menu/routes` | 路由 `name` 需与组件 `name` 一致 |
| Keep-alive 缓存 | 路由 `meta.cacheName` | 硬断点 `1000px` 保留 |
| 深色模式 | CSS 变量 + `isDark` 状态 | Element Plus 主题联动 |
| 四种布局切换 | `component :is` 动态组件 | 布局配置存 `localStorage` |
| 请求拦截器 | 注入 tenantId/SM4加密/Token刷新 | axios interceptors |
| 登录后加载 | 用户信息→路由→i18n 三步 | 顺序不可变 |

### Pinia 持久化版本选择

- **推荐**: `pinia-plugin-persistedstate@3.x` — Vue 3 专用，不兼容 Vue 2
- **pigx-ui 原始版本**: 未指定版本，可能为 1.x
- **Mango 决策**: 使用 3.x，放弃 Vue 2 兼容，换取更好的 Vue 3 支持

### Vue I18n 动态国际化

```typescript
// 后端动态加载 i18n（pigx-ui 模式）
const res = await i18nApi() // GET /api/sys/i18n
i18n.global.mergeLocaleMessage('zh-cn', res.data.zhCn)
i18n.global.mergeLocaleMessage('en', res.data.en)
```

### API 代理与后端对接

- **前端代理**: Vite proxy `/api` → `http://127.0.0.1:9999`（接入层添加 `/api`，后端无此前缀）
- **Mango API 路径**: `/{module}/{entity}/{action}` (例如 `/sys/user/list`)
- **pigx API 路径**: `/admin/{entity}/{action}` (不兼容)
- **前端 URL**: `/api/sys/menu/routes` → Vite rewrite → `http://127.0.0.1:9999/sys/menu/routes`

## 项目结构（1:1 复刻 pigx-ui）

```
mango-web/                          # ⭐ 新增：前端脚手架
├── package.json                     # 复刻 pigx-ui dependencies
├── vite.config.ts                  # 复刻：代理 /api 到后端
├── tsconfig.json
├── index.html
├── .env.development                # 开发环境配置
├── .env.production                 # 生产环境配置
├── .eslintrc.js
├── tailwind.config.js
├── postcss.config.js
├── src/
│   ├── main.ts
│   ├── App.vue
│   ├── api/                        # API 请求封装
│   │   └── admin/                  # 后端 API 接口
│   │       ├── i18n.ts             # 国际化接口
│   │       └── sys.ts              # 系统接口
│   ├── assets/                     # 静态资源
│   ├── components/                 # 通用组件（复刻 pigx-ui）
│   │   ├── auth/                   # 权限组件
│   │   ├── DictTag/                # 字典标签
│   │   ├── IconSelector/           # 图标选择器
│   │   ├── Pagination/             # 分页组件
│   │   ├── Popup/                  # 弹出层
│   │   ├── RightToolbar/           # 右侧工具栏
│   │   ├── TreeSelect/             # 树形选择
│   │   └── Verifition/             # 验证码
│   ├── directive/                  # 指令
│   ├── hooks/                      # Hooks
│   ├── i18n/                       # 国际化
│   │   ├── lang/
│   │   │   ├── zh-cn.ts
│   │   │   └── en.ts
│   │   ├── pages/
│   │   │   ├── form/
│   │   │   │   ├── zh-cn.ts
│   │   │   │   └── en.ts
│   │   │   └── login/
│   │   │       ├── zh-cn.ts
│   │   │       └── en.ts
│   │   └── index.ts               # 复刻：从后端动态获取 i18n
│   ├── layout/                     # ⭐ 核心：四种布局
│   │   ├── index.vue               # 布局入口
│   │   ├── component/
│   │   │   ├── aside.vue           # 侧边栏
│   │   │   ├── header.vue          # 顶栏
│   │   │   ├── main.vue            # 主内容区
│   │   │   └── columnsAside.vue    # 分栏侧边
│   │   ├── navMenu/
│   │   │   ├── vertical.vue         # 垂直菜单
│   │   │   ├── horizontal.vue      # 水平菜单
│   │   │   └── subItem.vue         # 子菜单
│   │   ├── navBars/
│   │   │   ├── index.vue           # 导航栏
│   │   │   ├── tagsView/           # 标签页
│   │   │   │   ├── tagsView.vue
│   │   │   │   └── contextmenu.vue
│   │   │   └── breadcrumb/         # 面包屑
│   │   │       ├── breadcrumb.vue
│   │   │       ├── settings.vue     # 主题设置
│   │   │       ├── user.vue         # 用户信息
│   │   │       ├── search.vue       # 路由搜索
│   │   │       └── closeFull.vue    # 全屏关闭
│   │   ├── routerView/              # 路由视图
│   │   │   ├── link.vue
│   │   │   ├── iframes.vue
│   │   │   └── parent.vue
│   │   ├── main/
│   │   │   ├── defaults.vue         # 默认布局
│   │   │   ├── classic.vue          # 经典布局
│   │   │   ├── transverse.vue       # 横向布局
│   │   │   └── columns.vue         # 分栏布局
│   │   ├── logo/
│   │   │   └── index.vue
│   │   └── lockScreen/
│   │       └── index.vue
│   ├── router/                     # 路由系统
│   │   ├── index.ts                # 路由入口（Hash 模式）
│   │   ├── route.ts                # 静态路由
│   │   ├── frontEnd.ts             # 前端控制路由
│   │   └── backEnd.ts              # ⭐ 后端控制路由
│   ├── stores/                     # Pinia 状态管理
│   │   ├── index.ts                # store 入口
│   │   ├── themeConfig.ts          # ⭐ 主题配置（复刻）
│   │   ├── userInfo.ts             # 用户信息
│   │   ├── routesList.ts           # 路由列表
│   │   ├── tagsViewRoutes.ts       # 标签页路由
│   │   ├── keepAliveNames.ts        # 缓存列表
│   │   ├── dict.ts                  # 字典
│   │   ├── msg.ts                   # 消息
│   │   └── requestOldRoutes.ts      # 请求路由
│   ├── theme/                      # 主题
│   │   ├── dark/
│   │   └── light/
│   ├── types/                      # TypeScript 类型
│   ├── utils/                      # 工具函数
│   │   ├── request.ts              # ⭐ axios 封装
│   │   ├── storage.ts              # 存储封装
│   │   ├── authFunction.ts         # 权限函数
│   │   ├── formatTime.ts           # 时间格式化
│   │   ├── validate.ts            # 校验
│   │   ├── theme.ts                # 主题工具
│   │   ├── mitt.ts                 # 事件总线
│   │   ├── errorCode.ts            # 错误码
│   │   └── getStyleSheets.ts       # 样式工具
│   ├── views/                      # 页面
│   │   ├── login/                  # 登录页
│   │   ├── home/                   # 首页
│   │   └── error/                  # 错误页
│   │       ├── 404.vue
│   │       └── 401.vue
│   └── tests/
│       └── e2e/
│           ├── specs/
│           └── __snapshots__/
└── README.md
```

---

## 关键模块详细设计

### 1. 主题配置系统（复刻 pigx-ui）

参考 `/Users/hardy/Work/pigx/pigx-ui/src/stores/themeConfig.ts`

```typescript
// stores/themeConfig.ts
export const useThemeConfig = defineStore('themeConfig', {
  state: (): ThemeConfigState => ({
    themeConfig: {
      // 布局配置抽屉
      isDrawer: false,

      // 全局主题
      primary: '#2E5CF6',           // 默认 primary 主题颜色
      isDark: false,                // 深色模式

      // 顶栏设置
      topBar: '#2E5CF6',
      topBarColor: '#FFFFFF',
      isTopBarColorGradual: false,

      // 菜单设置
      menuBar: '#FFFFFF',
      menuBarColor: '#505968',
      menuBarActiveColor: 'rgba(242, 243, 245, 1)',
      isMenuBarColorGradual: false,

      // 分栏设置
      columnsMenuBar: '#545c64',
      columnsMenuBarColor: '#e6e6e6',
      isColumnsMenuBarColorGradual: false,
      isColumnsMenuHoverPreload: false,

      // 界面设置
      isCollapse: false,             // 菜单水平折叠
      isUniqueOpened: true,          // 菜单手风琴
      isFixedHeader: false,          // 固定 Header
      isClassicSplitMenu: true,      // 经典布局分割菜单
      isLockScreen: false,
      lockScreenTime: 30,

      // 界面显示
      isShowLogo: true,
      isBreadcrumb: true,
      isTagsview: true,
      isBreadcrumbIcon: false,
      isTagsviewIcon: false,
      isCacheTagsView: true,
      isSortableTagsView: true,
      isShareTagsView: false,
      isFooter: true,
      isGrayscale: false,
      isInvert: false,
      isWartermark: true,
      wartermarkText: 'Mango',

      // 布局切换
      layout: 'classic',            // defaults/classic/transverse/columns

      // 后端控制路由
      isRequestRoutes: true,         // ⭐ 后端控制路由
      globalI18n: 'zh-cn',
      globalComponentSize: 'default',
      globalTitle: 'Mango',
      footerAuthor: 'Mango',
    },
  }),
})
```

### 2. 路由系统（后端控制路由）

参考 `/Users/hardy/Work/pigx/pigx-ui/src/router/index.ts`

```typescript
// router/index.ts
export const router = createRouter({
  history: createWebHashHistory(),  // Hash 模式
  routes: [...notFoundAndNoPower, ...staticRoutes],
})

// 路由守卫
router.beforeEach(async (to, from, next) => {
  NProgress.start()
  const token = Session.getToken()

  if (to.meta.isAuth !== undefined && !to.meta.isAuth) {
    next()
  } else {
    if (!token) {
      next(`/login?redirect=${to.path}`)
    } else if (token && to.path === '/login') {
      next('/home')
    } else {
      const storesRoutesList = useRoutesList(pinia)
      if (storesRoutesList.routesList.length === 0) {
        try {
          // ⭐ 三步登录：用户信息 → 路由 → i18n，每步失败都有降级
          await initBackEndControlRoutes()
          next({ path: to.path, query: to.query })
        } catch (error) {
          // 路由加载失败：清除 Token 并跳转登录
          Session.clearToken()
          Message.error('路由加载失败，请重新登录')
          next('/login')
        }
      } else {
        next()
      }
    }
  }
})
```

### 3. API 代理配置

参考 `/Users/hardy/Work/pigx/pigx-ui/vite.config.ts`

```typescript
// vite.config.ts
const ALLOWED_PROXY_HOSTS = ['127.0.0.1', 'localhost']

function validateProxyTarget(target: string): string {
  try {
    const url = new URL(target)
    if (!ALLOWED_PROXY_HOSTS.includes(url.hostname)) {
      throw new Error(`Proxy target hostname '${url.hostname}' not allowed. Allowed: ${ALLOWED_PROXY_HOSTS.join(', ')}`)
    }
    return target
  } catch {
    throw new Error(`Invalid VITE_ADMIN_PROXY_PATH: ${target}`)
  }
}

export default defineConfig((mode) => {
  const env = loadEnv(mode.mode, process.cwd())
  const proxyTarget = validateProxyTarget(env.VITE_ADMIN_PROXY_PATH || 'http://127.0.0.1:9999')

  return {
    server: {
      proxy: {
        '/api': {
          target: proxyTarget,
          ws: true,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, ''),
        },
      },
    },
  }
})
```

### 4. 国际化系统

参考 `/Users/hardy/Work/pigx/pigx-ui/src/i18n/index.ts`

```typescript
// i18n/index.ts
export const i18n = createI18n({
  legacy: false,
  locale: themeConfig.value.globalI18n,
  fallbackLocale: 'zh-cn',
  messages,
})

// 后端动态获取 i18n
async function fetchI18n() {
  const res = await i18nApi()
  i18n.global.mergeLocaleMessage('zh-cn', res.data.zhCn)
  i18n.global.mergeLocaleMessage('en', res.data.en)
}
```

---

## 实施单元

### Phase 1: 项目初始化

- [x] **Unit 1.1: 项目基础结构** ✅

**Goal:** 创建 Vue 3 + Vite + TypeScript + Tailwind CSS 项目

**Requirements:** R1

**Files:**
- Create: `mango-web/package.json`
- Create: `mango-web/vite.config.ts`
- Create: `mango-web/tsconfig.json`
- Create: `mango-web/index.html`
- Create: `mango-web/.env.development`
- Create: `mango-web/.env.production`
- Create: `mango-web/.eslintrc.js`
- Create: `mango-web/tailwind.config.js`
- Create: `mango-web/postcss.config.js`

---

- [x] **Unit 1.2: 主入口文件** ✅

**Files:**
- Create: `mango-web/src/main.ts`
- Create: `mango-web/src/App.vue`

**Approach:**
```typescript
// main.ts
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersist from 'pinia-plugin-persistedstate'
import App from './App.vue'
import router from './router'
import { i18n } from './i18n'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import '@/theme/index.scss'

const app = createApp(App)

app.use(createPinia().use(piniaPluginPersist))
app.use(router)
app.use(i18n)
app.use(ElementPlus)

app.mount('#app')
```

---

### Phase 2: 状态管理 + 主题系统

- [x] **Unit 2.1: Pinia Stores** ✅

**Files:**
- Create: `mango-web/src/stores/index.ts`
- Create: `mango-web/src/stores/themeConfig.ts`
- Create: `mango-web/src/stores/userInfo.ts`
- Create: `mango-web/src/stores/routesList.ts`
- Create: `mango-web/src/stores/tagsViewRoutes.ts`
- Create: `mango-web/src/stores/keepAliveNames.ts`
- Create: `mango-web/src/stores/dict.ts`
- Create: `mango-web/src/stores/msg.ts`

---

- [x] **Unit 2.2: 主题系统** ✅

**Files:**
- Create: `mango-web/src/theme/index.scss`
- Create: `mango-web/src/theme/dark/index.scss`
- Create: `mango-web/src/theme/light/index.scss`

---

### Phase 3: 布局系统（四种布局）

- [ ] **Unit 3.1: 布局核心组件**

**Files:**
- Create: `mango-web/src/layout/index.vue`
- Create: `mango-web/src/layout/component/header.vue`
- Create: `mango-web/src/layout/component/aside.vue`
- Create: `mango-web/src/layout/component/main.vue`
- Create: `mango-web/src/layout/component/columnsAside.vue`

---

- [ ] **Unit 3.2: 四种布局**

**Files:**
- Create: `mango-web/src/layout/main/defaults.vue`
- Create: `mango-web/src/layout/main/classic.vue`
- Create: `mango-web/src/layout/main/transverse.vue`
- Create: `mango-web/src/layout/main/columns.vue`

---

- [ ] **Unit 3.3: 导航菜单**

**Files:**
- Create: `mango-web/src/layout/navMenu/vertical.vue`
- Create: `mango-web/src/layout/navMenu/horizontal.vue`
- Create: `mango-web/src/layout/navMenu/subItem.vue`

---

- [ ] **Unit 3.4: 导航栏**

**Files:**
- Create: `mango-web/src/layout/navBars/index.vue`
- Create: `mango-web/src/layout/navBars/tagsView/tagsView.vue`
- Create: `mango-web/src/layout/navBars/tagsView/contextmenu.vue`
- Create: `mango-web/src/layout/navBars/breadcrumb/breadcrumb.vue`
- Create: `mango-web/src/layout/navBars/breadcrumb/settings.vue`
- Create: `mango-web/src/layout/navBars/breadcrumb/user.vue`
- Create: `mango-web/src/layout/navBars/breadcrumb/search.vue`
- Create: `mango-web/src/layout/navBars/breadcrumb/closeFull.vue`

---

### Phase 4: 路由系统

- [ ] **Unit 4.1: 路由配置**

**Files:**
- Create: `mango-web/src/router/index.ts`
- Create: `mango-web/src/router/route.ts`
- Create: `mango-web/src/router/backEnd.ts`
- Create: `mango-web/src/router/frontEnd.ts`

---

### Phase 5: 国际化

- [ ] **Unit 5.1: 国际化配置**

**Files:**
- Create: `mango-web/src/i18n/index.ts`
- Create: `mango-web/src/i18n/lang/zh-cn.ts`
- Create: `mango-web/src/i18n/lang/en.ts`
- Create: `mango-web/src/i18n/pages/form/zh-cn.ts`
- Create: `mango-web/src/i18n/pages/form/en.ts`
- Create: `mango-web/src/i18n/pages/login/zh-cn.ts`
- Create: `mango-web/src/i18n/pages/login/en.ts`

---

### Phase 6: 工具函数 + API

- [ ] **Unit 6.1: 工具函数**

**Files:**
- Create: `mango-web/src/utils/request.ts`（axios 封装）
- Create: `mango-web/src/utils/storage.ts`
- Create: `mango-web/src/utils/authFunction.ts`
- Create: `mango-web/src/utils/formatTime.ts`
- Create: `mango-web/src/utils/validate.ts`
- Create: `mango-web/src/utils/theme.ts`
- Create: `mango-web/src/utils/mitt.ts`
- Create: `mango-web/src/utils/errorCode.ts`

---

- [ ] **Unit 6.2: API 接口**

**Files:**
- Create: `mango-web/src/api/admin/sys.ts`
- Create: `mango-web/src/api/admin/i18n.ts`

---

### Phase 7: 通用组件

- [ ] **Unit 7.1: 权限组件**

**Files:**
- Create: `mango-web/src/components/auth/auth.vue`
- Create: `mango-web/src/components/auth/auths.vue`
- Create: `mango-web/src/components/auth/authAll.vue`

---

- [ ] **Unit 7.2: 其他通用组件**

**Files:**
- Create: `mango-web/src/components/Pagination/index.vue`
- Create: `mango-web/src/components/DictTag/index.vue`
- Create: `mango-web/src/components/IconSelector/index.vue`
- Create: `mango-web/src/components/TreeSelect/index.vue`
- Create: `mango-web/src/components/RightToolbar/index.vue`

---

### Phase 8: 页面

- [ ] **Unit 8.1: 登录页 + 错误页**

**Files:**
- Create: `mango-web/src/views/login/index.vue`
- Create: `mango-web/src/views/error/404.vue`
- Create: `mango-web/src/views/error/401.vue`
- Create: `mango-web/src/views/home/index.vue`

**Requirements:** R8, R9

**UI 交互状态要求（R9）:**
- 登录按钮：`loading` 状态（`:loading="submitting"`），防重复提交
- 登录失败：`el-alert` 显示错误原因，3 秒自动消失
- 登录成功：跳转前显示 `el-message` "登录成功"
- 空状态：首页/列表页无数据时显示空状态插画 + 文案
- 网络错误：axios 响应拦截器统一处理，显示 `ElMessage.error`

**无障碍要求（R8）:**
- 所有 `<el-button>` 添加 `aria-label`（无文本按钮）
- 表单输入框使用 `<label for="xxx">` 关联或 `aria-label`
- 键盘导航：Tab 顺序合理，回车/空格可触发按钮
- 错误提示：使用 `aria-live="polite"` 区域通知屏幕阅读器

---

### Phase 9: Playwright E2E

- [ ] **Unit 9.1: Playwright 配置**

**Files:**
- Create: `mango-web/playwright.config.ts`
- Create: `mango-web/src/tests/e2e/`

---

- [ ] **Unit 9.2: E2E 测试用例**

**Files:**
- Create: `mango-web/src/tests/e2e/specs/layout.spec.ts`
- Create: `mango-web/src/tests/e2e/specs/theme.spec.ts`
- Create: `mango-web/src/tests/e2e/specs/login.spec.ts`
- Create: `mango-web/src/tests/e2e/specs/a11y.spec.ts`

**Requirements:** R7, R8, R9

**E2E 测试用例清单：**
| 文件 | 测试场景 |
|------|---------|
| `login.spec.ts` | 登录成功/失败/loading 状态/空表单 |
| `layout.spec.ts` | 四种布局切换/标签页/面包屑 |
| `theme.spec.ts` | 深色模式切换/主题配置持久化 |
| `a11y.spec.ts` | 键盘导航/Tab 顺序/ARIA 属性存在性 |

---

## API 接口规范（Mango 自己的规范）

### 接口路径规范

> **注意**：以下路径为前端访问路径。Vite 代理在接入层添加 `/api` 前缀，后端服务路径无此前缀（见 api-rules.md）。

```
前端路径：/api/{module}/{entity}/{action}
后端路径：/{module}/{entity}/{action}
```

| 模块 | 前端路径 | 后端路径 |
|------|---------|---------|
| 系统模块 | /api/sys/user/list | /sys/user/list |
| 菜单模块 | /api/sys/menu/list | /sys/menu/list |
| 字典模块 | /api/sys/dict/list | /sys/dict/list |

### 接口响应格式

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

### Mango API vs pigx API

| 功能 | pigx API | Mango API |
|------|---------|----------|
| 登录 | /oauth2/token | /api/auth/login |
| 用户信息 | /user/info | /api/sys/user/info |
| 路由 | /user/routes | /api/sys/menu/routes |
| 国际化 | /i18n | /api/sys/i18n |

---

## 文档

- `mango-web/README.md` - 前端项目说明
- `mango/rules/ui-rules.md` - UI/UX 规范（已存在）
- `mango/rules/test-rules.md` - 测试规范（已存在）
- `mango/rules/api-rules.md` - API 规范（已存在）

---

## 后端 API 依赖与 Mock 策略

### 所需后端接口

以下接口在 mango 后端中尚未实现，需要先完成或提供 Mock 数据：

| 接口 | 方法 | 路径 | 说明 | 状态 |
|------|------|------|------|------|
| 登录 | POST | /api/auth/login | 登录认证 | 待实现 |
| 用户信息 | GET | /api/sys/user/info | 当前用户信息 | 待实现 |
| 路由 | GET | /api/sys/menu/routes | 后端返回菜单路由 | 待实现 |
| 国际化 | GET | /api/sys/i18n | 中英文语言包 | 待实现 |

### Mock 策略

在 Phase 1-2 阶段，使用 Mock 数据进行前端开发，避免阻塞：

- **Token**: 使用固定测试 Token，通过 axios interceptor 注入
- **用户信息**: 返回测试用户数据（包含权限码）
- **路由**: 返回静态路由 JSON（参考 pigx-ui 的 mock 数据结构）
- **i18n**: 使用 `/src/i18n/lang/` 下的静态文件，暂不动态请求后端

### 后端实现要求

后端需按 Mango API 规范（api-rules.md）实现以上接口：
- 路径无 `/api` 前缀（Vite 代理处理）
- 响应格式：`{ code: 200, message: "ok", data: {} }`

### Token 存储策略

| 存储位置 | 优势 | 风险 | Mango 决策 |
|---------|------|------|-----------|
| localStorage | 刷新页面不丢失 | XSS 可被 JS 读取 | ✅ 采用 |
| memory | XSS 无法直接读取 | 刷新页面 Token 丢失 | 备选 |

> 防护措施：Token 仅存储在 localStorage 中的 `MANGO_TOKEN` 键，配合 Content Security Policy (CSP) 限制内联脚本，防止 XSS 盗取。

### SM4 加密密钥管理

| 场景 | 密钥来源 | 存储位置 | 说明 |
|------|---------|---------|------|
| 开发/测试 | 固定测试密钥 | `.env.development` | VITE_SM4_KEY |
| 生产 | 后端接口返回 | 内存（不持久化） | 每个请求动态获取 |

> 注意：生产环境 SM4 密钥由后端接口 `/api/auth/sm4-key` 返回，前端存于内存，页面关闭后自动丢失。

---

## 风险与依赖

| 风险 | 影响 | 应对 |
|------|------|------|
| keep-alive 缓存不生效 | 标签页切换丢失状态 | 严格保持路由 `name` 与组件 `name` 一致 |
| 硬断点 1000px | 平板/大屏手机误判 | 保留该逻辑，不做修改 |
| 三步登录顺序错误 | 路由加载失败 | 严格按顺序 + try-catch 降级 |
| Pinia 持久化版本不兼容 | 状态无法恢复 | 使用 `pinia-plugin-persistedstate@3.x`（Vue 3 专用） |
| 后端 API 端点缺失 | 前端无法联调 | Mock 数据推进开发，待后端实现 |
| SSRF 代理风险 | 可配置指向任意地址 | 仅允许 127.0.0.1/localhost |
| Token XSS 盗窃风险 | 用户身份被盗用 | CSP 策略 + 最小化 Token 使用范围 |
| SM4 密钥泄露风险 | 加密通信被破解 | 生产密钥存内存不持久化 |

## 执行顺序

```
Phase 1: 项目初始化
    ↓
Phase 2: 状态管理 + 主题系统
    ↓
Phase 3: 布局系统（四种布局）
    ↓
Phase 4: 路由系统
    ↓
Phase 5: 国际化
    ↓
Phase 6: 工具函数 + API
    ↓
Phase 7: 通用组件
    ↓
Phase 8: 页面
    ↓
Phase 9: Playwright E2E
```
