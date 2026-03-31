---
title: "feat: mango-web 前端基础设施完善"
type: feat
status: completed
date: 2026-03-30
origin: docs/plans/2026-03-30-001-feat-mango-web-pigx-gap-analysis-plan.md
deepened: 2026-03-30
---

# mango-web 前端基础设施完善计划

## Overview

本计划面向 mango-web 前端脚手架，补齐与 pigx-ui 在 API 层、权限系统、国际化、工具函数等基础设施上的差距。前端所有 BFF 接口依赖后端 mango-backend 计划中的 BFF 层实现，可与后端并行开发。

## Problem Frame

mango-web 当前处于 MVP 阶段，具备基础的 Vue 3 + Element Plus + Pinia + Vue Router 架构，但相比成熟的 pigx-ui 存在显著差距：

- **基础设施不完善**：API 层、权限系统、工具函数、国际化等能力不足
- **自研组件缺失**：缺少 30+ 业务组件（Editor、Upload、Map 等）
- **企业功能缺失**：无工作流、代码生成、支付等企业级模块

## Scope Boundaries

**本计划范围：**
- mango-web 前端脚手架基础设施完善（P1）
- mango-web P2 核心组件引入

**明确排除：**
- 代码生成、支付、工作流等企业功能 → 属于独立业务域
- 后端模块实现 → 属于 mango-backend 计划
- Phase 3 企业功能 → 降为远期规划

**与 mango-backend 计划的并行关系：**
- 前端 Unit 1.1-1.3 依赖后端 BFF 层接口契约（见 Dependencies）
- 后端计划先完成 BFF 接口定义，前端可同步开发 mock 数据层

---

## Key Technical Decisions

### 决策 1: 前端不持有对称密钥

**结论**：前端加密仅作透传，加密能力置于 BFF 层

**理由**：
- 前端不可信，密钥不能安全存储于 JS bundle
- BFF 层作为加密边界，前端流量到 BFF 可以是明文
- SM2 公钥仅用于加密会话密钥传输，非存储对称密钥

### 决策 2: 权限仅做展示控制

**结论**：v-auth 指令仅控制前端元素显示，不作为安全防线

**理由**：
- 前端权限可被控制台绕过
- 所有按钮操作必须由后端 `mango-permission` 微服务强制校验
- 后端鉴权是唯一可信的权限控制点

### 决策 3: i18n 公开端点

**结论**：语言包作为公开资源，无需登录即可获取

**理由**：
- 语言 key-value 映射不涉及用户隐私
- 避免登录前无法加载语言包的体验问题
- 跨域/聚合场景由 BFF 层处理

### 决策 4: BFF 模式优先

**结论**：生产环境默认启用 BFF 模式，前端直接请求 BFF

**理由**：
- 微服务模式下前端不需要知道后端模块结构
- URL 自动适配由 `VITE_IS_MICRO` 环境变量控制
- 单体部署可通过 `VITE_API_ENC_ENABLED` 启用本地加密

---

## Dependencies

### 前端 BFF 接口依赖（来自 mango-backend 计划）

> ⚠️ 以下接口契约由 mango-backend 计划定义，前端依赖这些契约进行开发。

| 接口 | 方法 | 说明 | 后端对应 |
|------|------|------|---------|
| `/bff/admin/user/info` | GET | 用户信息聚合（含 permissions 数组） | mango-backend Unit 1.2 |
| `/bff/admin/i18n` | GET | 国际化语言包 | mango-backend Unit 1.3 |
| `/admin/i18n/public` | GET | 公开 i18n 端点（无需登录） | mango-backend Unit 1.3 |
| `/admin/i18n/languages` | GET | 支持的语言列表 | mango-backend Unit 1.3 |

### 本地开发依赖

- **pigx-ui 参考**: `/Users/hardy/Work/pigx/pigx-ui` → apiCrypto.ts, authDirective.ts, i18n/index.ts
- **Vue 3**: 已支持
- **Pinia**: 已支持
- **sm-crypto**: 已安装在 package.json（SM2/SM4 支持）

---

## Implementation Units

### Phase 1: 基础设施完善 (P1)

---

#### Unit 1.1: API 层基础设施

**Goal:** 完善请求拦截器、URL 适配、encryption 透传

**Dependencies:** 无（可与后端并行开发，先用 mock 数据）

**Files:**
- Create: `mango-web/src/utils/apiCrypto.ts` → BFF 模式透传 / 单体模式 SM2 加密
- Modify: `mango-web/src/utils/request.ts` → 拦截器完善、URL adaptation
- Modify: `mango-web/src/utils/other.ts` → adaptationUrl 函数

**Approach:**

> ⚠️ **安全架构**：前端在 BFF 模式下**不持有对称密钥**，仅透传 JSON 或用 SM2 公钥加密会话密钥。真正的 SM4 加解密由 BFF 层处理。

> ⚠️ **SM2 密文模式约定**：使用 `C1C2C3` 模式（国密标准推荐），sm-crypto 默认使用 C1C2C3 模式，与后端 BouncyCastle/GmSSL 的 C1C2C3 输出格式一致，互通性已验证。

```
[前端] --明文/开发者自定义--> [BFF层] --密文--> [后端微服务]
```

**mango 方案（前端）：**
```typescript
// 1. apiCrypto.ts
// - BFF 模式（生产默认）：透传，JSON 直通
// - 非 BFF 模式（单体部署）：可用 SM2 加密会话密钥
// - SM2 密文模式：C1C2C3（sm-crypto 默认，与后端一致）
const ENCRYPTION_ENABLED = import.meta.env.VITE_API_ENC_ENABLED === 'true' ?? false;
const IS_BFF_MODE = import.meta.env.VITE_IS_BFF ?? 'true';
const SM2_PUBLIC_KEY = import.meta.env.VITE_SM2_PUBLIC_KEY ?? '';

export function wrapRequest(data: any): any {
  if (!ENCRYPTION_ENABLED) return data;
  if (IS_BFF_MODE === 'true') {
    // BFF 模式：透传不加密，信任 BFF 层
    return data;
  }
  // 单体模式：SM2 加密会话密钥（使用 C1C2C3 模式）
  return { data: sm2Encrypt(JSON.stringify(data), SM2_PUBLIC_KEY) };
}

// 2. request.ts 拦截器
// - 请求拦截：调用 wrapRequest
// - 响应拦截：BFF 模式下不解密（由 BFF 处理）
// - URL 自动适配：IS_MICRO=false 时 URL 添加 /admin 前缀

// 3. adaptationUrl(url)
// VITE_IS_MICRO=false → 自动添加 /admin 前缀
```

**环境变量：**
```
VITE_API_ENC_ENABLED=false  # 默认关闭，前端不加密
VITE_IS_BFF=true             # 默认 BFF 模式
VITE_SM2_PUBLIC_KEY=<SM2公钥> # 仅单体加密模式使用
VITE_API_URL=http://localhost:8080
```

**Test scenarios:**
- `VITE_IS_BFF=true` 时请求直接透传
- `VITE_IS_BFF=false` + `VITE_API_ENC_ENABLED=true` 时 SM2 加密生效
- `VITE_IS_MICRO=false` 时 URL 自动添加 `/admin` 前缀

**Verification:**
- npm run dev 启动无报错
- BFF 模式下请求透传，响应直接解析

---

#### Unit 1.2: 权限系统完善

**Goal:** 实现 v-auth / v-auths / v-auth-all 指令，完善 auth 函数族

**Dependencies:** `/bff/admin/user/info` 接口（可先 mock）

**Files:**
- Create: `mango-web/src/directive/authDirective.ts` → v-auth, v-auths, v-auth-all
- Modify: `mango-web/src/utils/authFunction.ts` → auth / auths / authAll
- Modify: `mango-web/src/stores/userInfo.ts` → authBtnList 状态

**Approach:**

**authDirective.ts：**
```typescript
// v-auth: 任意权限（OR 逻辑）
// v-auths: 多个权限满足任意即可
// v-auth-all: 多个权限必须全部满足

export function authDirective(app: App) {
  app.directive('auth', {
    mounted(el, binding) {
      if (useUserInfo().authBtnList.length === 0) return;
      if (!auth(binding.value)) {
        el.parentNode?.removeChild(el);
      }
    },
    updated(el, binding) {
      if (useUserInfo().authBtnList.length === 0) return;
      if (!auth(binding.value)) {
        el.parentNode?.removeChild(el);
      }
    }
  });
  // v-auths, v-auth-all 类似...
}
```

**authFunction.ts：**
```typescript
// 无 '*' 超级权限，admin 权限由后端返回具体权限码
export function auth(value: string): boolean {
  const userInfo = Session.get('userInfo');
  if (!userInfo) return false;
  const { permissions = [] } = userInfo;
  return permissions.some((v: string) => v === value);
}

export function auths(value: string[]): boolean {
  return value.some(v => auth(v));
}

export function authAll(value: string[]): boolean {
  return value.every(v => auth(v));
}
```

**stores/userInfo.ts：**
```typescript
// authBtnList: string[]  // 后端返回的按钮权限码列表
setUserInfo(res) {
  this.authBtnList = res.data.permissions || [];
}
```

> ⚠️ **后端强制鉴权**：v-auth 仅做前端展示控制。真正鉴权在后端 `mango-permission` 微服务中强制校验，前端隐藏的元素不能阻止直接调用 API。

**Test scenarios:**
- v-auth 指令在无权限时正确隐藏元素
- authBtnList 为空时 v-auth 不隐藏元素（等待数据加载）
- 有具体权限码时 v-auth 正确显示
- authBtnList 加载完成后 updated 钩子重新检查

**Verification:**
- 登录后 authBtnList 包含用户权限码
- v-auth="'fake:permission'" 的按钮对任何用户不可见
- 控制台无法通过修改 DOM 绕过 v-auth（但仍可直接调用 API，需后端鉴权）

---

#### Unit 1.3: 国际化增强

**Goal:** 支持后端动态加载语言包、运行时切换

**Dependencies:** `/admin/i18n/public` 公开端点（无需登录）

**Files:**
- Create: `mango-web/src/api/admin/i18n.ts` → i18n API
- Modify: `mango-web/src/i18n/index.ts` → 增加远程加载
- Modify: `mango-web/src/hooks/useLocale.ts` → 运行时切换（含 loading 状态）

**Approach:**

**api/admin/i18n.ts：**
```typescript
// ✅ 公开端点：无需登录即可获取语言包
export function getI18nPublic() {
  return axios.get(import.meta.env.VITE_API_URL + '/admin/i18n/public');
}

export function getLanguages() {
  return axios.get(import.meta.env.VITE_API_URL + '/admin/i18n/languages');
}
```

**i18n/index.ts：**
```typescript
async function fetchI18n() {
  try {
    const res = await getI18nPublic();
    const { zhCn, en } = res.data;
    // 后端返回格式: { "zh-cn": [...], "en": [...] }
    const zhMessages = {};
    const enMessages = {};
    zhCn.forEach(item => Object.assign(zhMessages, item));
    en.forEach(item => Object.assign(enMessages, item));
    i18n.global.mergeLocaleMessage('zh-cn', zhMessages);
    i18n.global.mergeLocaleMessage('en', enMessages);
  } catch (e) {
    console.warn('Failed to load i18n from backend, using fallback');
  }
}
```

**useLocale.ts（含 loading 状态）：**
```typescript
export function useLocale() {
  const locale = ref(i18n.global.locale.value);
  const loading = ref(false);

  async function switchLocale(newLocale: string) {
    if (locale.value === newLocale) return;
    loading.value = true;
    try {
      locale.value = newLocale;
      localStorage.setItem('locale', newLocale);
    } finally {
      loading.value = false;
    }
  }

  return { locale, switchLocale, loading };
}
```

**Test scenarios:**
- 切换语言后所有文本正确切换
- 页面刷新后语言状态保持
- 后端 i18n 覆盖前端静态文本
- 网络失败时有 fallback 不阻塞页面加载

**Verification:**
- 切换语言后无刷新页面，文本实时切换
- authBtnList 未加载时按钮保持可见

---

#### Unit 1.4: 工具函数补齐

**Goal:** 补齐缺失的常用工具函数

**Dependencies:** 无

**Files:**
- Modify: `mango-web/src/utils/other.ts` → 增加 adaptationUrl、懒加载等
- Create: `mango-web/src/utils/arrayOperation.ts` → 数组操作工具
- Create: `mango-web/src/utils/toolsValidate.ts` → 工具验证

**Add functions:**
| 函数 | 说明 |
|------|------|
| lazyImg(el, arr) | 图片懒加载 |
| handleTree(data, id, parentId, children, rootId) | 列表转树 |
| generateUUID() | 生成唯一 ID |
| getQueryString(url, paraName) | URL 参数解析 |
| adaptationUrl(url) | URL 自动适配（依赖 VITE_IS_MICRO） |
| getNonDuplicateID() | 生成不重复 ID |
| handleEmpty(list) | 移除空值元素 |
| openWindow(url, title, w, h) | 打开新窗口 |

**Test scenarios:**
- handleTree 将平铺数组转为树结构
- adaptationUrl 在 VITE_IS_MICRO=false 时自动添加 /admin 前缀
- generateUUID 生成符合 RFC4122 的 UUID

**Verification:**
- 工具函数覆盖 pigx-ui other.ts 的核心能力

---

### Phase 2: 核心组件引入 (P2)

---

#### Unit 2.1: 富文本编辑器

**Goal:** 引入 WangEditor 作为默认富文本编辑器

**Effort:** M（1-2天）

**Files:**
- Create: `mango-web/src/components/Editor/` → Editor.vue, WangEditor config
- Modify: `mango-web/src/components/index.ts` → 全局注册

**Approach:**
1. 安装: @wangeditor-next/editor, @wangeditor-next/editor-for-vue
2. 封装: v-model 绑定、高度自适应、工具栏配置
3. AI 使用提示: `<Editor v-model="content" />`

**Test scenarios:**
- 富文本编辑器加载正常，工具栏完整显示
- 粘贴富文本内容（带 HTML 样式）后内容正确保留
- 提交表单时 v-model 正确绑定

**Verification:** `<Editor v-model="content" />` 双向绑定正确工作

---

#### Unit 2.2: 代码编辑器

**Goal:** 引入 CodeMirror 作为代码展示/编辑组件

**Effort:** S（0.5天）

**Files:**
- Create: `mango-web/src/components/CodeEditor/` → CodeEditor.vue
- Modify: `mango-web/package.json` → 添加 codemirror 依赖

**Approach:**
1. 安装 CodeMirror 5.x
2. 支持 Vue 3、主题配置（dark/light）、只读模式

**Test scenarios:**
- 切换 dark/light 主题正确生效
- 只读模式下内容不可修改
- 代码高亮正确渲染

**Verification:** 代码编辑器在查看和编辑模式下正确切换

---

#### Unit 2.3: 上传组件增强

**Goal:** 实现 Excel/Image/File 分类上传组件

**Effort:** M（1-2天）

**Files:**
- Create: `mango-web/src/components/Upload/` → Upload.vue, ImageUpload.vue, FileUpload.vue, ExcelUpload.vue
- Create: `mango-web/src/api/admin/upload.ts` → 上传 API

**Approach:**
1. ExcelUpload: 解析 Excel 返回 JSON 数据
2. ImageUpload: 图片上传+预览
3. FileUpload: 通用文件上传

**Test scenarios:**
- Excel 文件上传后正确解析为 JSON 数据
- 图片上传后正确预览，支持删除
- 文件类型校验正确拒绝非法文件
- 上传进度显示正确

**Verification:** 三种上传组件（Excel/Image/File）各自功能正确

---

#### Unit 2.4: 数据可视化

**Goal:** 引入 ECharts 基础封装

**Effort:** M（1-2天）

**Files:**
- Create: `mango-web/src/components/ECharts/` → index.vue, useECharts.ts
- Modify: `package.json` → 添加 echarts, vue-echarts

**Approach:**
1. 按需引入 ECharts（减少打包体积）
2. useECharts: 自动 resize、主题切换、响应式配置
3. 基础图表: 折线图、柱状图、饼图

**Test scenarios:**
- ECharts 图表正确渲染数据
- 窗口 resize 后图表自适应
- 主题切换（dark/light）正确应用

**Verification:** 折线图、柱状图、饼图三种基础图表正确显示

---

## Risks & Dependencies

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 后端 BFF 接口未就绪 | 前端无法联调 | 先用 mock 数据开发，接口就绪后切换 |
| 组件依赖膨胀 | 打包体积过大 | 按需引入，使用 vite-plugin-codegen |
| 国际化覆盖冲突 | 部分文本显示异常 | 后端语言包仅覆盖必要字段，前端兜底 |

---

## Sources & References

- **Origin:** [docs/plans/2026-03-30-001-feat-mango-web-pigx-gap-analysis-plan.md](docs/plans/2026-03-30-001-feat-mango-web-pigx-gap-analysis-plan.md)
- **pigx-ui 源码:** `/Users/hardy/Work/pigx/pigx-ui`
- **mango-web 源码:** `/Users/hardy/Work/company02/mango-web`
- **后端并行计划:** `docs/plans/2026-03-30-003-feat-mango-backend-modules-plan.md`
