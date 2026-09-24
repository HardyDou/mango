# @mango/detail 使用说明

## 1. 概览

`@mango/detail` 是 Mango 的独立详情组合组件包，提供折叠详情骨架和富文本文件预览组合能力。它依赖 `@mango/common` 与 `@mango/file` 的公开 API，不依赖 `@mango/admin`、`@mango/admin-shell`、宿主 store、菜单或权限装配。

包内样式只作用于组件自身稳定 class。消费本包不会引入 Admin 全量样式，也不会修改 `html`、`body`、`#app` 或 `.mango-layout-*` 等宿主布局节点。

## 2. 功能清单

| 能力                      | 用途                                                    | 公开入口                  |
| ------------------------- | ------------------------------------------------------- | ------------------------- |
| `MangoCollapseDetailPage` | 组合返回栏、摘要、Tab、折叠面板、底部操作区和工作流抽屉 | `@mango/detail`           |
| `MangoRichTextPreview`    | 安全展示富文本并组合托管文件预览                        | `@mango/detail`           |
| Package 样式              | 提供仅作用于本包稳定 class 的组件样式                   | `@mango/detail/style.css` |

## 3. 接入方式

安装本包及其 peer dependencies，并分别引入三个包的公开样式：

```bash
pnpm add @mango/detail @mango/common @mango/file element-plus vue
```

```ts
import { MangoCollapseDetailPage } from '@mango/detail';
import type { MangoCollapsePanel } from '@mango/detail';
import '@mango/common/style.css';
import '@mango/file/style.css';
import '@mango/detail/style.css';
```

基础组合示例：

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { MangoCollapseDetailPage, type MangoCollapsePanel } from '@mango/detail';

const detailRef = ref<InstanceType<typeof MangoCollapseDetailPage>>();
const panels: MangoCollapsePanel[] = [
  {
    name: 'base',
    title: '基本信息',
    content: {
      componentType: 'description-list',
      data: { items: [{ key: 'orderNo', label: '订单编号', value: 'BH20260920001' }] },
    },
  },
  {
    name: 'materials',
    title: '资料文件',
    content: { componentType: 'file-list', data: { rows: materialRows, columns: materialColumns } },
  },
];
</script>

<template>
  <MangoCollapseDetailPage ref="detailRef" title="保函详情" :panels="panels" show-workflow>
    <template #workflow><WorkflowTimeline /></template>
    <template #actions><el-button type="primary">提交</el-button></template>
  </MangoCollapseDetailPage>
</template>
```

## 4. 配置说明

本包没有宿主级全局配置，也不会读取路由、store、菜单或权限。运行行为通过组件 props、events、slots 和 expose 方法配置；主题颜色通过 Element Plus 变量及 `--mango-table-header-bg`、`--mango-table-header-text` 等 Mango CSS 变量提供。

## 5. API 与扩展

### MangoCollapseDetailPage

内置面板类型包括：

| `componentType`     | 作用                             | 底层能力                      |
| ------------------- | -------------------------------- | ----------------------------- |
| `description-list`  | 分组描述信息                     | `MangoDescriptionList`        |
| `file-list`         | 文件资料列表、预览、下载和行合并 | `MangoFileList`               |
| `list-table`        | 详情内嵌列表                     | 无卡片模式的 `MangoDataTable` |
| `rich-text-preview` | 安全富文本和托管文件预览         | `MangoRichTextPreview`        |
| `custom`            | 业务自定义内容                   | `panel-*` 或 `panel` slot     |

完整 Props：

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `title` | 必填 | 页面标题。 |
| `panels` / `tabs` | `undefined` | 二选一；直接面板或 Tab 下的面板配置。两者同时传入会报错。 |
| `responsive` | `undefined` | 描述列表栅格响应式配置：`gutter`、`span`、`xs`、`sm`、`md`、`lg`、`xl`。 |
| `activeTab` / `defaultActiveTab` | `undefined` | 受控/初始 Tab 名称。 |
| `autoExpandPanels` | `true` | 自动展开全部面板，并跟随新增面板。 |
| `defaultActivePanelNames` | `undefined` | `autoExpandPanels=false` 时指定初始展开 Key。 |
| `backLabel` | `返回` | 返回按钮文案。 |
| `backTo` | `undefined` | 传给 `MangoPageBackBar` 的目标路由。 |
| `navigateOnBack` | `false` | 返回时是否由组件执行路由跳转；关闭时只触发 `back`。 |
| `showBackBar` | `true` | 是否显示返回栏。 |
| `showRefresh` / `refreshLoading` | `true` / `false` | 刷新按钮及其 loading。 |
| `showContentShadow` | `true` | 内容区默认阴影。 |
| `summary` | `undefined` | `MangoDetailSummary` 配置。 |
| `showBackTop` / `backTopThreshold` | `true` / `300` | 是否显示回到顶部按钮及触发滚动距离（px）。 |
| `showActions` / `actionsAlign` | `true` / `right` | 底部操作区及对齐方式 `left | center | right`。 |
| `showWorkflow` / `showWorkflowTrigger` | `false` / `true` | 工作流抽屉及右下角触发按钮。 |
| `workflowTitle` / `workflowDrawerSize` | `节点过程` / `min(420px, 100vw)` | 抽屉标题和尺寸。 |
| `dataPage` | `undefined` | 写入稳定数据标识 `data-page`，用于埋点和验收。 |
| `loading` | `false` | 加载态，优先于错误态。 |
| `errorText` | `''` | 错误文案；非空时显示重试入口。 |
| `emptyDescription` | `暂无详情信息` | 无面板或空内容文案。 |

`defaultActivePanelNames` 只能在 `autoExpandPanels=false` 时使用。空、重复或不存在的 Key，以及同时传入 `panels` 和 `tabs`，都会抛出明确配置错误。

面板与内容类型：`description-list.data` 是 `MangoDescriptionListProps`，`file-list.data` 是 `MangoFileListProps`，`list-table.data` 是去掉卡片/模式切换字段的 `MangoDataTableProps<Record<string, unknown>>`，`rich-text-preview.data` 为 `{ content?: string }`，`custom` 不需要 `data` 并通过 slot 渲染。面板字段为 `name`、`title`、`disabled`、`emptyDescription`、`dataSurface`、`headerActions` 和 `content`；Tab 字段为 `name`、`label`、`disabled`、`lazy`、`panels`、`emptyDescription`；面板操作字段为 `name`、`label`、`icon`、`disabled`、`loading`、`dataAction`、`dataStage`。

公开事件及 payload：

| 事件 | payload |
| --- | --- |
| `back`、`refresh`、`retry` | 无参数。 |
| `update:activeTab`、`tab-change` | Tab `name: string`。前者用于 `v-model:active-tab`。 |
| `change` | 当前展开面板名 `string[]`。 |
| `panel-action` | `{ action, panel, panelIndex, tab?, tabIndex? }`。 |
| `file-list-selection-change` | `{ panel, panelIndex, rows, tab?, tabIndex? }`，`rows` 为 `MangoFileListRow[]`。 |

Slots：`toolbar`（返回栏工具区）、`summary`（摘要）、`actions`（底部操作）、`workflow`（工作流抽屉）、`panel` / `panel-{name}`（自定义面板，参数含 `panel/index/expanded/tab/tabIndex`）、`tab-{name}`（参数含 `tab/index/active`）。描述列表使用 `description-item-{panelName}__{slot}`、`description-group-extra-{panelName}__{slot}`；文件列表使用 `file-list-cell-{panelName}__{slot}`、`file-list-file-actions-{panelName}__{slot}`；内嵌表格使用 `list-table-cell-{panelName}__{slot}`。这些细粒度 slot 会额外收到对应的 `panel`、索引、Tab 上下文。

公开方法包括：

- `openWorkflowDrawer()`：打开右侧抽屉，即使隐藏默认触发按钮也可调用。
- `closeWorkflowDrawer()`：关闭右侧抽屉。
- `toggleWorkflowDrawer()`：切换右侧抽屉。
- `scrollToPageTop()`：滚动到当前详情容器顶部。

工作流内容始终由 `workflow` slot 注入，本包不内置任何业务工作流组件。`toolbar`、`summary`、`actions`、`panel-*`、`tab-*` 以及内置列表的细粒度 slot 均可独立扩展。

### MangoRichTextPreview

`MangoRichTextPreview` 只有一个可选 prop：`content?: string`（默认空字符串）。它复用 `RichTextViewer` 过滤 HTML、解析 `mango-file:{id}` token，并在点击受保护文件时使用 `MangoFilePreviewDialog` 展示文件或文本内容；组件本身没有 emits 或 expose。文件详情解析依赖 `@mango/file` 的请求实例和后端文件预览接口，业务只应持久化文件 ID，不要把临时 URL 写入业务数据。

## 6. 数据与初始化

本包不包含数据库 migration、默认数据、菜单或权限资源，也不持久化业务数据。面板、文件、富文本及工作流内容均由消费系统传入；文件访问继续使用 `@mango/file` 的文件 ID 和请求实例契约。

## 7. 管理入口

本包不注册管理菜单、页面路由或权限码。消费系统负责页面入口、路由鉴权和业务操作权限，并通过公开组件 API 注入已授权的数据与动作。

## 8. 快速开始

1. 安装 `@mango/detail` 及其 peer dependencies。
2. 在应用入口引入 common、file、detail 三个包的公开样式。
3. 按“接入方式”中的基础组合示例声明面板并渲染 `MangoCollapseDetailPage`。
4. 需要工作流时使用 `workflow` slot；隐藏默认按钮后仍可调用 `openWorkflowDrawer()` 打开抽屉。

## 9. 样式与主题

组件颜色和间距优先读取 Element Plus 与 Mango 主题变量。表格表头使用 `--mango-table-header-bg` 和 `--mango-table-header-text`，由消费系统主题提供。

本包不导入 Element Plus 全局 CSS、`@mango/admin/style-full.css` 或任何 Shell 样式。宿主应在应用入口自行安装 Element Plus，并按实际使用的包引入公开样式。

## 10. 问题排查

| 问题               | 排查方向                                                                                   |
| ------------------ | ------------------------------------------------------------------------------------------ |
| 组件有结构但无样式 | 确认已引入 `@mango/common/style.css`、`@mango/file/style.css` 和 `@mango/detail/style.css` |
| 文件预览不可用     | 确认文件 ID 有效、`@mango/file` 请求实例已按宿主方式配置，并检查网络错误                   |
| 指定展开项时报错   | 先关闭 `autoExpandPanels`，再检查 Key 是否非空、唯一且存在                                 |
| 工作流抽屉没有内容 | 通过 `workflow` slot 注入业务内容；本包只提供按钮、抽屉和公开方法                          |
| 宿主布局变化       | 检查消费入口是否误引入 `@mango/admin/style-full.css`；本包只需要三个公开 `style.css`       |

## 11. 相关文档

- [Mango 能力地图](../../../mango-docs/capabilities/README.md)
- [Common 使用说明](../common/README.md)
- [File 使用说明](../file/README.md)
- [前端组件开发规范](../../../mango-pmo/rules/frontend/03-component-development.md)
- [STANDARD 交付记录](../../../mango-docs/plans/2026-09-20-mango-collapse-detail-standard-delivery.md)

## 12. 验证命令

```bash
pnpm --filter @mango/detail test
pnpm --filter @mango/detail build
pnpm package-exports:check
pnpm component-contracts:check
```
