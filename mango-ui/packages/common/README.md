# @mango/common

## 1. 概览

`@mango/common` 是 Mango 前端公共基础包，提供请求封装、session、菜单和权限工具、主题工具、实时通信、公共 API、Vue hooks 和管理端通用组件。

集成形态：

| 标识                 | 说明                                                                         |
| -------------------- | ---------------------------------------------------------------------------- |
| `admin-shell`        | request、session、菜单树、TagsView、权限函数、主题、实时通信等后台底座工具。 |
| `business-component` | 分页、字典、组织、用户、区域、验证码、编辑器、图表等后台业务页面组件。       |
| `api-client`         | 上传、验证码、组织、地区、字典等公共接口封装。                               |

这个包不是无依赖的官网组件库。它依赖 Vue、Element Plus、Pinia、Vue Router、Mango 请求响应格式、登录 token、租户头和若干后端平台能力。官网、营销页或 C 端站点要复用时，优先只引入明确需要的工具或组件，不建议全量接入。

## 2. 功能清单

| 能力        | 使用入口                                                                                                                                                                                   | 说明                                                        |
| ----------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------- |
| HTTP client | `createMangoHttpClient`                                                                                                                                                                    | 新业务默认入口；host 注入、实例隔离、标准取消和规范化错误。 |
| 旧请求入口  | `request`、`get`、`post`、`put`、`del`                                                                                                                                                     | 迁移期兼容；不得新增业务依赖。                              |
| Session     | `Session`                                                                                                                                                                                  | 保存 token、refresh token、过期时间、用户信息和租户。       |
| API 加密    | `wrapRequest`、`sm2Encrypt`、`sm2Decrypt`                                                                                                                                                  | 按环境变量启用 SM2 或 BFF 透传。                            |
| 菜单和权限  | `buildMenuTree`、权限函数、TagsView 工具                                                                                                                                                   | 给管理后台菜单、按钮权限和标签页使用。                      |
| Web Crypto  | `installWebCryptoRandomUUIDCompatibility`、`createWebCryptoRandomUUID`、`generateRfc4122UuidV4`                                                                                            | 为缺少原生 `randomUUID` 的运行环境提供安全 UUID 兼容。      |
| 公共 API    | `uploadFile`、captcha、org、area、dict API                                                                                                                                                 | 连接 file、captcha、org、system 后端。                      |
| 通用组件    | `MangoListPage`、`MangoSearchPanel`、`MangoListPanel`、`MangoDetailPage`、`MangoFormPage`、`MangoPageSection`、`MangoDialog`、`Pagination`、`DictSelect`、`OrgSelector`、`UserSelector` 等 | 后台页面骨架和复用组件。                                    |
| hooks       | `useTitle`、`useDict`、`useECharts`、`useLocale`                                                                                                                                           | 页面标题、字典、图表和语言相关能力。                        |
| 实时通信    | `useRealtime`、`SSE`、`Websocket`                                                                                                                                                          | SSE/WebSocket client 和组件。                               |
| 主题和消息  | `mangoMessage`、theme 工具、主题 CSS                                                                                                                                                       | 管理端统一提示和主题样式。                                  |

## 3. 接入方式

开发依赖：

```bash
pnpm add @mango/common
```

宿主应用需要提供 peer 依赖：

```bash
pnpm add vue vue-router pinia element-plus vue-i18n
```

引入样式：

```ts
import '@mango/common/style.css';
import '@mango/common/theme/index.css';
```

可选引入管理端页面主题 token。主题变量可以挂在应用、模块或单个页面外层，允许多套样式在同一应用内并存：

```ts
import '@mango/common/theme/admin-standard.css';
import '@mango/common/theme/admin-compact.css';
```

```vue
<template>
  <section class="mango-theme-admin-standard">
    <MangoListPage>...</MangoListPage>
  </section>

  <section data-mango-theme="admin-compact">
    <MangoListPage dense>...</MangoListPage>
  </section>
</template>
```

新业务由 host 创建并向业务 API 注入客户端：

```ts
import { Session } from '@mango/common';
import { createMangoHttpClient } from '@mango/http-client';

const client = createMangoHttpClient({
  baseUrl: '/api',
  getAccessToken: () => Session.getToken(),
  getTenantId: () => Session.get('userInfo')?.tenantId ?? Session.get('tenantId'),
});
```

旧请求入口仅用于迁移期兼容：

```ts
import { get, registerUnauthorizedHandler, setRequestBaseUrl } from '@mango/common';

setRequestBaseUrl('/api');
registerUnauthorizedHandler(() => {
  window.location.hash = '/login';
});

const rows = await get('/system/dict/data/options', {
  params: { typeCode: 'order_status' },
});
```

使用组件：

```vue
<script setup lang="ts">
import {
  DictSelect,
  MangoDetailPage,
  MangoDialog,
  MangoFormPage,
  MangoListPage,
  MangoListPanel,
  MangoPageSection,
  MangoSearchPanel,
  OrgSelector,
  Pagination,
} from '@mango/common';
import '@mango/common/style.css';
</script>
```

使用标准列表页骨架：

```vue
<template>
  <MangoListPage data-page="demo.orders">
    <template #search>
      <MangoSearchPanel :model="query" collapsible :collapsed-count="3" @search="search" @reset="reset">
        <el-form-item label="关键字">
          <el-input v-model="query.keyword" clearable placeholder="请输入关键字" />
        </el-form-item>
        <el-form-item label="状态">
          <el-input v-model="query.status" clearable placeholder="请选择状态" />
        </el-form-item>
        <el-form-item label="负责组织">
          <el-input v-model="query.orgName" clearable placeholder="请输入组织" />
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker
            v-model="query.createdAt"
            type="daterange"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
        </el-form-item>
      </MangoSearchPanel>
    </template>

    <MangoListPanel>
      <template #actions>
        <el-button type="primary" plain>新增</el-button>
      </template>

      <el-table v-loading="loading" :data="rows" row-key="id">
        <el-table-column prop="name" label="名称" min-width="180" show-overflow-tooltip />
      </el-table>

      <template #pagination>
        <Pagination v-model:page="query.page" v-model:limit="query.size" :total="total" @pagination="loadData" />
      </template>
    </MangoListPanel>
  </MangoListPage>
</template>
```

独立详情页和表单页使用对应页面外壳，并通过 `MangoPageSection` 按业务语义分组：

```vue
<template>
  <MangoDetailPage title="订单详情" data-page="orders.detail" @back="router.back()">
    <MangoPageSection title="基本信息">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.statusName }}</el-descriptions-item>
      </el-descriptions>
    </MangoPageSection>
  </MangoDetailPage>

  <MangoFormPage title="编辑订单" data-page="orders.edit" @back="router.back()">
    <MangoPageSection title="基本信息">
      <el-form :model="form" label-width="120px">
        <el-form-item label="订单名称">
          <el-input v-model="form.name" />
        </el-form-item>
      </el-form>
    </MangoPageSection>
    <template #actions>
      <el-button @click="router.back()">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="save">保存</el-button>
    </template>
  </MangoFormPage>
</template>
```

列表上下文中的短详情可以继续使用 Element Plus Drawer；短表单和标准弹框使用 `MangoDialog`。不要把 `MangoDetailPage` 或 `MangoFormPage` 塞进弹框、抽屉。

`MangoSearchPanel` 会按字段顺序识别常用搜索项。启用 `collapsible` 后，收起态默认显示两行字段，展开态显示全部字段；查询、重置按钮默认固定在搜索区域右侧，展开或收起按钮默认以图标形式在搜索区底部居中。组件只负责字段栅格、按钮区和展开收起能力，不自带白底、边框、圆角或阴影。字段区默认桌面端一行四列，也可设置 `columns="auto"` 使用字段宽度自适应；移动端使用单列撑满。字段区和按钮区在桌面端按 `10:2` 比例排布。表单默认使用中等尺寸、label 右对齐并带中文冒号，可通过 `size`、`label-position`、`label-suffix` 覆盖：

```vue
<MangoSearchPanel :model="query" label-suffix="" label-position="left" size="small" @search="search" @reset="reset">
  <el-form-item label="客户名称">
    <el-input v-model="query.customerName" clearable />
  </el-form-item>
</MangoSearchPanel>
```

字段较多且需要统一四列排版时，可显式设置固定列数：

```vue
<MangoSearchPanel :model="query" collapsible :columns="4" :collapsed-rows="2" @search="search" @reset="reset">
  <el-form-item label="项目名称">
    <el-input v-model="query.projectName" clearable />
  </el-form-item>
  <el-form-item label="客户名称">
    <el-input v-model="query.customerName" clearable />
  </el-form-item>
  <el-form-item label="业务状态">
    <el-select v-model="query.status" clearable />
  </el-form-item>
</MangoSearchPanel>
```

使用通用弹框：

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { MangoDialog, type MangoDialogExpose } from '@mango/common';
import '@mango/common/style.css';

const visible = ref(false);
const dialogRef = ref<MangoDialogExpose | null>(null);

function focusDialog() {
  dialogRef.value?.bringToFront();
}
</script>

<template>
  <MangoDialog
    ref="dialogRef"
    v-model="visible"
    title="新增应用"
    width="720px"
    footer-align="right"
    draggable
    resizable
  >
    <template #default> 弹框内容 </template>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary">确定</el-button>
    </template>
  </MangoDialog>
</template>
```

`MangoDialog` 会使用 `title` prop 作为弹框的可访问名称；只提供 `#title` slot 时，会通过
`aria-labelledby` 关联可见标题。若同时提供 `title` prop 和 `#title` slot，slot 只覆盖视觉展示，
辅助技术仍使用 `title` prop 识别弹框。

部署时没有单独的 `@mango/common` 后端 starter。它调用的接口来自业务已经启用的后端模块，例如 file、captcha、org、system、auth。

## 4. 配置说明

请求配置：

`@mango/common/utils/request` 是存量兼容入口。新增业务 API 使用 `@mango/api-schema` 的中立合同，由 host 通过 `@mango/http-client` 创建实例并注入；迁移边界见[前端业务 API 规范](../../../mango-pmo/rules/frontend/12-business-api.md)。

| 配置位置                               | 字段               | 默认值                               | 含义                                       |
| -------------------------------------- | ------------------ | ------------------------------------ | ------------------------------------------ |
| `setRequestBaseUrl(baseURL)`           | `baseURL`          | `/api` 或 Wujie runtime `apiBaseUrl` | 设置 axios 默认 API 前缀。                 |
| `registerUnauthorizedHandler(handler)` | handler            | hash 跳转 `/login`                   | 统一 401 处理。                            |
| `RequestConfig`                        | `ignoreToken`      | `false`                              | 不附加 Authorization，登录和公开接口可用。 |
| `RequestConfig`                        | `rawResponse`      | `false`                              | 返回 AxiosResponse，文件下载等场景使用。   |
| `RequestConfig`                        | `skipRefreshToken` | `false`                              | 禁止当前请求触发 refresh token。           |
| `RequestConfig`                        | `silentError`      | `false`                              | 失败时不弹全局错误提示。                   |
| `RequestConfig`                        | `loading`          | 可选                                 | 参与内部 loading 计数，当前不直接展示 UI。 |
| `RequestConfig`                        | `retry`            | 可选                                 | 类型保留，当前没有通用自动重试。           |

请求头：

| Header              | 来源                                      | 含义           |
| ------------------- | ----------------------------------------- | -------------- |
| `Authorization`     | `Session.getToken()`                      | Bearer token。 |
| `X-Mango-Tenant-Id` | `userInfo.tenantId` 或 Session `tenantId` | 当前租户。     |
| `TENANT-ID`         | `userInfo.tenantId` 或 Session `tenantId` | 兼容租户头。   |

API 加密环境变量：

| 变量                   | 默认值  | 含义                                      |
| ---------------------- | ------- | ----------------------------------------- |
| `VITE_API_ENC_ENABLED` | `false` | 是否启用 API 加密包装。                   |
| `VITE_IS_BFF`          | `true`  | BFF 模式下前端透传，非 BFF 模式可用 SM2。 |
| `VITE_SM2_PUBLIC_KEY`  | 空      | 非 BFF 模式 SM2 加密公钥。                |

开发 mock：

| 变量                 | 影响                                                          |
| -------------------- | ------------------------------------------------------------- |
| `VITE_USE_MOCK=true` | `getAreaTree()`、`getOrgTree()` 在 dev 环境返回本地 mock 树。 |

## 5. API 与扩展

请求和工具导出：

| 导出                                      | 用途                                          |
| ----------------------------------------- | --------------------------------------------- |
| `request`                                 | axios 实例。                                  |
| `get`、`post`、`put`、`del`               | 常用请求方法，默认返回后端包裹体中的 `data`。 |
| `setRequestBaseUrl`                       | 设置 API baseURL。                            |
| `registerUnauthorizedHandler`             | 注册未授权处理。                              |
| `normalizeApiPayload`                     | 把明确 ID 字段中的 number 兜底转成字符串。    |
| `Session`                                 | token、refresh token、用户信息和租户存储。    |
| `mangoMessage`                            | Element Plus 消息封装。                       |
| `useRealtime`                             | 实时连接 hook。                               |
| `generateRfc4122UuidV4`                   | 使用调用方提供的安全随机字节生成 UUID v4。    |
| `createWebCryptoRandomUUID`               | 优先原生方法，否则使用 `getRandomValues`。    |
| `installWebCryptoRandomUUIDCompatibility` | 幂等补齐当前 Web Crypto 的 `randomUUID`。     |

公共 API：

| 函数                        | HTTP 接口                                              | 说明                                 |
| --------------------------- | ------------------------------------------------------ | ------------------------------------ |
| `uploadFile(file)`          | `POST /file/files`                                     | 上传普通附件，`purpose=attachment`。 |
| `uploadImage(file)`         | `POST /file/files`                                     | 上传图片，`purpose=image`。          |
| `uploadExcel(file)`         | `POST /file/files`                                     | 上传 Excel，`purpose=excel`。        |
| `uploadMultiple(files)`     | `POST /file/files/batch`                               | 批量上传。                           |
| `getUploadedFileDetail(id)` | `GET /file/files/detail`                               | 查询文件详情。                       |
| `downloadUploadedFile(id)`  | `GET /file/files/download`                             | 下载文件，返回原始 blob response。   |
| `getCaptchaTypes()`         | `GET /captcha/types`                                   | 查询验证码类型和存储。               |
| `generateArithmetic()`      | `GET /captcha/arithmetic`                              | 生成算术验证码。                     |
| `generateBlockPuzzle()`     | `GET /captcha/block-puzzle`                            | 生成拼图验证码。                     |
| `generateClickWord()`       | `GET /captcha/click-word`                              | 生成点选验证码。                     |
| `generateBehavior()`        | `GET /captcha/behavior`                                | 生成行为验证码。                     |
| `verifyCaptcha(request)`    | `POST /captcha/verify`                                 | 校验验证码。                         |
| `sendSms(mobile)`           | `POST /auth/captcha/send`                              | 发送登录短信验证码。                 |
| `sendEmail(email)`          | `POST /auth/captcha/send`                              | 发送登录邮箱验证码。                 |
| `getOrgTree(params)`        | `GET /org/tree`                                        | 组织树。                             |
| `getAreaTree(params)`       | `GET /system/area/tree` 或 `GET /system/area/children` | 地区树或子节点。                     |
| `listDictOptions(typeCode)` | `GET /system/dict/data/options`                        | 字典选项。                           |

组件导出：

| 组件                                                                      | 能力                                                                                           |
| ------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------- |
| `MangoListPage`                                                           | 管理后台列表页外壳，按搜索区和列表区组织页面，不渲染额外页面标题。                             |
| `MangoSearchPanel`                                                        | 列表页搜索面板，统一字段栅格、按钮位置、展开收起和查询/重置事件。                              |
| `MangoListPanel`                                                          | 列表卡片，统一功能区、表格区和分页区位置。                                                     |
| `MangoDetailPage`                                                         | 详情页外壳，提供返回栏、内容区和底部操作栏。                                                   |
| `MangoFormPage`                                                           | 表单页外壳，提供返回栏、内容区和底部操作栏。                                                   |
| `MangoPageSection`                                                        | 详情页和表单页的业务分组容器。                                                                 |
| `Pagination`                                                              | 分页器。                                                                                       |
| `MangoDialog`                                                             | 管理端通用弹框外壳，统一标题区、关闭按钮、内容滚动区和底部按钮区。                             |
| `MangoAvatar`                                                             | 统一显示普通图片地址或 `mango-file:{id}` 文件标识，并负责受保护头像的下载回显和对象 URL 回收。 |
| `DictSelect`、`DictTag`                                                   | 字典选择和展示。                                                                               |
| `OrgSelector`、`UserSelector`                                             | 组织和用户选择。                                                                               |
| `CaptchaSelector`、各验证码组件                                           | 验证码展示和交互。                                                                             |
| `ChinaArea`                                                               | 行政区域选择。                                                                                 |
| `IconSelector`、`TreeSelect`、`RightToolbar`                              | 管理端通用选择和工具栏。                                                                       |
| `FormCreate`、`Sign`、`CodeEditor`、`Editor`、`RichTextViewer`、`ECharts` | 表单、签名、代码、富文本编辑/预览和图表。                                                      |
| `SSE`、`Websocket`、`Chat`                                                | 实时通信和聊天 UI。                                                                            |

`MangoAvatar` 使用示例：

```vue
<MangoAvatar :size="40" :source="user.photo">
  <span>{{ user.nickname?.slice(0, 1) }}</span>
</MangoAvatar>
```

| API       | 类型 / 默认值                     | 说明                                                                         |
| --------- | --------------------------------- | ---------------------------------------------------------------------------- |
| `source`  | `string / ''`                     | 支持普通 `http(s)`、站内、data、blob 图片地址或 `mango-file:{id}` 文件标识。 |
| `size`    | `number \| string`                | 透传 Element Plus Avatar 尺寸。                                              |
| `shape`   | `'circle' \| 'square' / 'circle'` | 头像形状。                                                                   |
| `fit`     | CSS object-fit 值 / `cover`       | 图片填充方式。                                                               |
| 默认 slot | -                                 | 图片为空或加载失败时的兜底内容。                                             |
| `error`   | `(error: unknown) => void`        | 受保护文件下载或图片加载失败时触发。                                         |

组件遇到 `mango-file:{id}` 时通过文件中心受保护下载接口生成临时对象 URL，并在来源变化或卸载时释放；不持久化下载地址。缺少文件标识或不支持的来源时保留默认 slot。
`MangoSearchPanel` 折叠规则：

| 属性                            | 默认值                             | 说明                                                                                   |
| ------------------------------- | ---------------------------------- | -------------------------------------------------------------------------------------- |
| `collapsible`                   | `false`                            | 是否启用搜索项展开/收起。                                                              |
| `collapsed-count`               | 自动按列数和 `collapsed-rows` 计算 | 收起态显示前几个搜索项。业务把常用搜索项放在前面。                                     |
| `collapsed-rows`                | `2`                                | 未指定 `collapsed-count` 时，收起态显示几行。                                          |
| `columns`                       | `4`                                | 搜索字段区列数。传入 `auto` 时按字段宽度自适应；固定列数在窄屏下自动降列。             |
| `more-placement`                | `bottom`                           | 展开/收起按钮位置。`actions` 表示跟随查询、重置按钮；`bottom` 表示放在搜索区底部居中。 |
| `field-min-width`               | `280px`                            | 自适应列模式下字段最小宽度。                                                           |
| `field-max-width`               | `320px`                            | 自适应列模式下字段最大宽度。                                                           |
| `default-expanded`              | `false`                            | 初始是否展开全部搜索项。                                                               |
| `expand-text` / `collapse-text` | `展开` / `收起`                    | 展开按钮文案。                                                                         |
| `expand-change`                 | -                                  | 展开状态变化事件。                                                                     |

默认操作区顺序为查询、重置、展开或收起，位置固定在搜索面板右下角；传入 `actions` slot 时由业务自行接管按钮区域。

`MangoDialog` 组件：

| 类型   | 名称                                                     | 说明                                                                                    |
| ------ | -------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| props  | `modelValue`                                             | 弹框显示状态，支持 `v-model`。                                                          |
| props  | `title`                                                  | 标题文本，也可通过 `title` slot 覆盖。                                                  |
| props  | `width`                                                  | 弹框宽度，语义与 Element Plus Dialog 一致，默认 `50%`。                                 |
| props  | `showHeader`                                             | 是否展示完整顶部标题区，默认 `true`。                                                   |
| props  | `showClose`                                              | 是否展示关闭按钮，默认 `true`。                                                         |
| props  | `footerAlign`                                            | 底部插槽对齐方式，支持 `left`、`center`、`right`，默认 `right`。                        |
| props  | `destroyOnClose`                                         | 关闭后是否销毁内容，默认 `false`。                                                      |
| props  | `modal`                                                  | 是否保留遮罩；未指定时普通弹框为 `true`，拖拽弹框为 `false`。                           |
| props  | `closeOnClickModal`                                      | 点击遮罩是否关闭，默认 `false`。                                                        |
| props  | `lockScroll`                                             | 是否锁定页面滚动；未指定时跟随 `modal`。                                                |
| props  | `zIndex`                                                 | 弹框动态置顶使用的最低层级；未指定时跟随 Element Plus。                                 |
| props  | `draggable`                                              | 是否允许通过标题区拖动整个弹框，默认 `false`。                                          |
| props  | `resizable`                                              | 是否允许通过四个角自由调整宽高，默认 `false`。                                          |
| props  | `minWidth` / `minHeight`                                 | 交互调整时的最小宽高，默认 `320` / `240` 像素。                                         |
| emits  | `update:modelValue`、`open`、`opened`、`close`、`closed` | 透出弹框显示状态和 Element Plus Dialog 生命周期事件。                                   |
| expose | `bringToFront()`                                         | 将已打开的当前实例提升到其它 `MangoDialog` 之上；关闭中、已关闭或销毁后调用安全 no-op。 |
| slots  | `default`                                                | 内容区域。内容区独立滚动，弹框最大高度为视口高度的 90%。                                |
| slots  | `title`                                                  | 自定义标题内容。                                                                        |
| slots  | `headerExtra`                                            | 标题右侧扩展区域。                                                                      |
| slots  | `footer`                                                 | 底部按钮区域。未传入时不渲染底部。                                                      |

`draggable` 和 `resizable` 相互独立，内容仍由 slots 提供。拖拽默认关闭遮罩，并允许弹框移出浏览器可视区域；需要拖拽且保留遮罩时显式传入 `:modal="true"`。多个无模态弹框同时存在时，用户按下弹框的标题、内容或 footer 任意区域都会将该实例提升到最高层级；置顶监听不阻止内部按钮、输入框和其它控件的原有事件。显式传入的 `zIndex` 会作为动态层级的最低基线。四角调整以每次按下时读取的真实宽高为起点，浏览器视口缩小时只收缩弹框宽高，不强制修改已经拖动的位置；弹框关闭后重新按 `width` 和当前视口恢复自适应布局。

`Editor` 富文本组件：

| 类型  | 名称               | 说明                                                                                                              |
| ----- | ------------------ | ----------------------------------------------------------------------------------------------------------------- |
| props | `toolbarKeys`      | 自定义 WangEditor 工具栏按钮；不传时沿用 `mode` 对应的默认工具栏。                                                |
| props | `imageValueType`   | 图片上传写入策略。默认 `token`，编辑态使用实时地址，`v-model` 保存 `mango-file:<id>`；`url/id` 仅用于旧业务兼容。 |
| props | `pasteImageMode`   | 粘贴图片处理策略。默认 `upload` 并托管剪贴板图片；显式 `default` 可恢复 WangEditor 原行为。                       |
| props | `attachmentAccept` | 内置回形针附件按钮的文件类型限制，格式与原生 `input[accept]` 一致；默认不限制。                                   |
| emits | `asset-error`      | 图片或附件上传、解析或序列化失败。图片错误同时继续触发兼容事件 `image-error`。                                    |
| emits | `uploading-change` | 首个文件开始上传时发送 `true`，并发上传全部结束后发送 `false`。                                                   |

只保留加粗、文字颜色、有序列表、无序列表和图片上传时：

```vue
<script setup lang="ts">
const toolbarKeys = ['bold', 'color', '|', 'numberedList', 'bulletedList', '|', 'uploadImage'];
</script>

<template>
  <Editor v-model="content" :toolbar-keys="toolbarKeys" />
</template>
```

图片调用 `uploadImage(file)`（`purpose=image`），普通附件调用 `uploadFile(file)`（`purpose=attachment`）。工具栏图片按钮、内置附件按钮、复制粘贴和拖拽都使用同一托管节点协议。图片保存为 `img[data-file-id]`，附件保存为 `a[data-file-id]`，两者的 `src/href` 最终均为 `mango-file:<id>`；附件文本为文件名，并带 `target="_blank" rel="noopener noreferrer"`。

只处理组件主动上传且带 `data-file-id` 或 `mango-file:` 的托管节点。用户自行写入的 Base64、Blob URL、签名地址、第三方图片和普通链接不转换、不删除。重新编辑时 `Editor` 会按文件 ID 调用详情接口获取最新地址；只读场景使用 `RichTextViewer`：

```vue
<RichTextViewer :content="content" />
```

`RichTextViewer` 默认调用 `getUploadedFileDetail(id)`。匿名站点可通过 `:resolve-file="publicResolver"` 注入公开预览接口。展示侧会移除 `script`、事件属性和 `javascript:` 地址，但保留用户自带的 Data/Blob/第三方资源地址。`toolbar-actions` slot 仍可用于其它业务动作，不再需要借助 slot 才能上传附件。

## 6. 数据与初始化

`@mango/common` 不创建后端数据。组件和 API 能否返回数据，取决于后端模块是否已初始化：

| 数据              | 后端来源                | 前端消费                                                  |
| ----------------- | ----------------------- | --------------------------------------------------------- |
| token、用户、租户 | auth、identity、system  | request 请求头、Session、权限函数。                       |
| 字典              | system 字典             | `DictSelect`、`DictTag`、`useDict`、`listDictOptions()`。 |
| 组织              | org                     | `OrgSelector`、`getOrgTree()`。                           |
| 地区              | system area             | `ChinaArea`、`getAreaTree()`。                            |
| 文件              | file                    | 上传、下载、文件详情 API。                                |
| 验证码            | captcha、auth           | 登录验证码和二次校验。                                    |
| 实时连接          | realtime 后端或业务服务 | `useRealtime`、`SSE`、`Websocket`。                       |

## 7. 管理入口

本包不注册菜单，也不写权限资源。它提供的权限函数只用于前端展示控制，接口访问必须由后端再次校验。

接入管理后台时至少确认：

| 检查项   | 说明                                                     |
| -------- | -------------------------------------------------------- |
| token    | 登录后 `Session` 中存在 access token。                   |
| 租户     | `userInfo.tenantId` 或 Session `tenantId` 能写入租户头。 |
| baseURL  | `/api` 或运行时 `apiBaseUrl` 能转发到后端。              |
| 数据权限 | 公共选择器返回的数据已经由后端按租户和权限过滤。         |

## 8. 快速开始

1. 安装 `@mango/common` 和 peer 依赖。
2. 引入 `@mango/common/style.css` 和主题 CSS。
3. 调用 `setRequestBaseUrl()` 设置 API 前缀。
4. 调用 `registerUnauthorizedHandler()` 接入登录页跳转。
5. 登录成功后写入 token、refresh token、用户和租户信息。
6. 在页面中按需使用公共 API、hooks 和组件。

## 9. 问题排查

| 问题                           | 常见原因                                               | 处理方式                                                                              |
| ------------------------------ | ------------------------------------------------------ | ------------------------------------------------------------------------------------- |
| 请求没有 token                 | 未登录、Session 未写入或 `ignoreToken=true`            | 检查登录保存逻辑和请求配置。                                                          |
| 租户头缺失                     | userInfo 和 Session 都没有 tenantId                    | 登录后写入租户上下文。                                                                |
| 401 后没有跳登录               | 没注册 unauthorized handler                            | 调用 `registerUnauthorizedHandler()`。                                                |
| 文件下载拿到 JSON              | 后端返回业务错误而不是 blob                            | 看 JSON 中 `message` 或 `msg`，排查文件权限。                                         |
| 字典、组织、地区为空           | 后端数据未初始化或权限不足                             | 分别检查 system、org 和接口权限。                                                     |
| 官网引入后体积过大             | 全量 common 带管理端组件和依赖                         | 改为按子路径引入，或拆出站点专用轻量组件。                                            |
| 大 ID 精度问题                 | 业务把 id 当 number 继续运算                           | ID 字段按字符串处理，使用 `ApiId`。                                                   |
| `randomUUID is not a function` | 浏览器或 WebView 只有 `getRandomValues`，缺少原生 UUID | 使用匹配版本的 Admin Shell；Shell 启动时自动安装兼容方法，业务入口无需增加 polyfill。 |

## 10. 相关文档

- [@mango/api-schema](../api-schema/README.md)
- [后端 File](../../../mango/mango-platform/mango-file/README.md)
- [后端 Captcha](../../../mango/mango-platform/mango-captcha/README.md)
- [后端 System](../../../mango/mango-platform/mango-system/README.md)
- [后端 Org](../../../mango/mango-platform/mango-org/README.md)
- [能力说明维护规范](../../../mango-pmo/rules/08-capability-docs.md)

## 11. 变更影响记录

- Issue #805 待发布修复保留 `Pagination.small?: boolean` 公开入参，内部改为向 Element Plus 传递 `size="small" | "default"`，消除废弃 `small` prop 告警。页码、每页数、总数、双向绑定和 `pagination` 事件保持兼容。

- Issue #722 新增 Web Crypto UUID 兼容 API：原生 `randomUUID` 始终优先，缺失时只使用 `getRandomValues` 生成 RFC 4122 v4；完全没有安全 Web Crypto 时不会安装 `Math.random` 伪实现。`generateUUID()` 复用同一安全路径，并仅在普通非安全唯一标识语义下保留原有最终 fallback。

- `@mango/common@1.0.23` 发布 `MangoDialogExpose.bringToFront()`。父组件可以通过类型安全的组件 ref 提升已打开实例；用户按下标题、内容或 footer 时也会自动置顶，关闭中、已关闭或销毁后的调用安全忽略。既有弹框 props、事件、拖拽、缩放、遮罩和默认层级行为保持兼容。

- `@mango/common@1.0.20` publishes the `MangoSearchPanel` bottom expand/collapse spacer fix and the `Editor` `toolbarKeys` / `imageValueType` options. Search, reset, field grid, events and public props stay compatible; Editor still writes image URLs by default, while consumers may opt into file ID or `mango-file:<id>` storage for business-side preview resolution.

- 本次扩展 `Editor` 富文本组件，新增 `toolbarKeys` 和 `imageValueType` 配置。默认仍按原逻辑写入图片 URL；消费项目可显式选择写入文件 ID 或 `mango-file:<id>`，用于业务保存后自行解析预览。图片上传仍依赖后端 `mango-file` 的 `/file/files` 接口。
- `@mango/common@1.0.17` 发布主题 token 和 `MangoSearchPanel` 搜索面板布局修复。主题 CSS 继续通过
  `@mango/common/theme/index.css` 和公开主题入口消费；搜索项不需要展开时，底部更多操作行保留固定高度，
  只隐藏展开/收起按钮，查询、重置、字段栅格、事件和公开 props 不变。
- 本次新增 `PasswordPolicyHint` 组件和 `passwordPolicy` 工具，用于统一展示密码规则和强弱判断。它们只影响前端密码提示和表单校验，不改变 request、Session、菜单、权限、租户头、公开 API 路径或后端存储结构。包的公开入口已通过 `@mango/common` 主入口和 `./components/PasswordPolicyHint/index.vue` 子路径导出。
