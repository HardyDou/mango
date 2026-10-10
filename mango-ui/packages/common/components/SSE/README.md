# SSE

服务器消息推送组件（Server-Sent Events）。基于浏览器原生 `EventSource` 连接消息服务，内置连接状态指示、指数退避重连、通知弹窗和生命周期自动管理。

## 导入

```ts
import { SSE } from '@mango/common';
```

> 组件内的消息与状态类型（`SSEMessage`、`SSEStatus`、`SSEProps`、`SSEEmits`、`SSEExpose`）当前随组件源码导出，未在 `@mango/common` 包入口单独导出，使用方按事件载荷自行声明类型即可。

## 后端端点

- 默认连接地址：`GET /mango-message/sse/connect`
- 连接方式：原生 `EventSource`（仅支持 GET，无法自定义请求头），认证信息通过查询参数传递：`/mango-message/sse/connect?token={token}&tenantId={tenantId}`
- `token` 取自 `Session.getToken()`（sessionStorage 的 `MANGO_TOKEN`，回退同名 Cookie）；`tenantId` 取自 sessionStorage 中 `userInfo.tenantId`，缺省为 `master`
- 消息体为 JSON 字符串，`type` 为 `pong` 的消息视为心跳应答并丢弃
- 环境变量 `VITE_SSE_ENABLE === 'false'` 时组件不发起连接（仅打印告警）

源码注释中保留了基于 `@microsoft/fetch-event-source` 的实现草案（通过 `Authorization: Bearer {token}`、`TENANT-ID`、`Accept: text/event-stream` 请求头认证），当前生效的是原生 EventSource 实现。

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `url` | `string` | `'/mango-message/sse/connect'` | SSE 端点，相对路径基于 `window.location.origin` 解析 |
| `showStatus` | `boolean` | `true` | 是否展示连接状态圆点与文案 |
| `showNotifications` | `boolean` | `true` | 是否对收到的消息弹出 Element Plus 通知；同时控制 `message` 事件是否抛出 |
| `maxRetries` | `number` | `6` | 最大重试次数 |
| `heartbeatInterval` | `number` | `30000` | 心跳间隔（毫秒） |
| `enabled` | `boolean` | `true` | 是否在挂载后自动连接 |

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `connected` | - | 连接建立（`onopen`） |
| `disconnected` | - | 主动断开或连接关闭 |
| `message` | `data: SSEMessage` | 收到业务消息；仅当 `showNotifications` 为 `true` 时才会抛出 |
| `error` | `error: Error` | 重试次数耗尽时抛出，固定为 `new Error('Max retries exceeded')` |
| `retry` | `count: number` | 每次进入重试时抛出当前次数（从 1 开始） |

## Expose

| 名称 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `connect()` | - | - | 建立连接；`connecting`/`connected` 状态下调用会被忽略 |
| `disconnect()` | - | - | 关闭 EventSource、停止心跳定时器并置为已断开 |
| `sendPing()` | - | - | 空实现。原生 EventSource 只能单向接收，无法向服务端发送心跳 |
| `getStatus()` | - | `SSEStatus` | 获取当前连接状态 |

## 类型定义

```ts
interface SSEMessage {
  type: 'notification' | 'alert' | 'pong';
  content?: string;
}

type SSEStatus = 'disconnected' | 'connecting' | 'connected' | 'retrying' | 'error';

interface SSEProps {
  url?: string;
  showStatus?: boolean;
  showNotifications?: boolean;
  maxRetries?: number;
  heartbeatInterval?: number;
  enabled?: boolean;
}
```

连接状态与状态点颜色：`connected` 绿色、`connecting`/`retrying` 橙色呼吸动画、`error` 红色、`disconnected` 灰色。

内置中文 i18n 文案（`SSE/i18n/zh-cn.ts`，命名空间 `sse`）：

| key | 文案 |
|-----|------|
| `sse.connected` | 已连接 |
| `sse.connecting` | 正在连接... |
| `sse.disconnected` | 已断开 |
| `sse.retrying` | 连接断开，正在重连 {count}/{max} |
| `sse.error` | 连接失败 |
| `sse.reconnect` | 重新连接 |
| `sse.notification` | 通知 |
| `sse.alert` | 警告 |

消息通知规则：`alert` 弹出 warning 样式、标题“警告”；`notification` 弹出 info 样式、标题“通知”；弹窗时长均为 5000ms。

## 使用示例

### 基础用法（默认自动连接）

```vue
<script setup lang="ts">
import { SSE } from '@mango/common';

function onMessage(data: { type: string; content?: string }) {
  console.log('收到推送', data);
}
</script>

<template>
  <SSE @message="onMessage" />
</template>
```

### 只监听事件，不弹通知

```vue
<script setup lang="ts">
import { SSE } from '@mango/common';

function onConnected() {
  console.log('SSE 已连接');
}

function onDisconnected() {
  console.log('SSE 已断开');
}

function onRetry(count: number) {
  console.log(`第 ${count} 次重连`);
}
</script>

<template>
  <!-- showNotifications=false 时 message 事件也不会抛出，消息需由后端配合其他通道处理 -->
  <SSE
    :show-notifications="false"
    :show-status="true"
    @connected="onConnected"
    @disconnected="onDisconnected"
    @retry="onRetry"
  />
</template>
```

### 手动控制连接

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { SSE } from '@mango/common';

const sseRef = ref<InstanceType<typeof SSE> | null>(null);

function reconnect() {
  sseRef.value?.disconnect();
  sseRef.value?.connect();
}
</script>

<template>
  <SSE ref="sseRef" :enabled="false" />
  <el-button size="small" @click="sseRef?.connect()">连接</el-button>
  <el-button size="small" @click="sseRef?.disconnect()">断开</el-button>
</template>
```

## 重要约束

- 当前生效的是原生 EventSource：只能以 GET 建立连接、不能设置 `Authorization` 请求头，令牌通过 `token` 查询参数传递，后端必须接受 query 形式的鉴权。
- `sendPing()` 是空实现，`heartbeatInterval` 定时器会按时调用但不会真正发送数据；保活依赖服务端推送与浏览器原生重连，真正的双向心跳请使用 `Websocket` 组件。
- 收到无法 `JSON.parse` 的消息只打印错误，不抛出事件；`pong` 消息被静默丢弃。
- `message` 事件在 `showNotification()` 内抛出，当 `showNotifications` 为 `false` 时既不弹通知也不会触发 `message` 事件，无法实现“静默监听”。
- 自动重连采用指数退避 `min(1000 × 2^次数, 30000)` ms；但当前实现中每次重连调用 `connect()` 都会把 `retryCount` 重置为 0，自动重连计数每轮重新开始，因此 `maxRetries` 上限与“重试耗尽后显示红色‘重新连接’按钮”的 UI 在纯自动重连路径下实际不会触发；需要可靠重试上限时由宿主自行计数并调用 `connect()`。
- `error` 事件只在重试耗尽时抛出 `Max retries exceeded`；EventSource 原生 error 事件对象仅打印到控制台。
- 组件挂载时若 `enabled` 为 `true` 自动连接，卸载时自动断开并清理定时器；多个实例会建立多条独立连接。

## 概览

面向 `mango-message` 消息服务的单向实时推送接入组件，用于通知、告警等服务端到浏览器的消息下发。

## 功能清单

支持自动连接、状态指示、通知/告警弹窗、指数退避重连、手动连接/断开、环境变量开关和卸载自动清理。

## 接入方式

从 `@mango/common` 导入 `SSE`，直接放置在全局布局或业务页面中，默认挂载即连接 `/mango-message/sse/connect`，通过 `message`、`connected`、`disconnected` 事件处理业务。

## 配置说明

用 `url` 覆盖端点，`showStatus` 控制状态 UI，`showNotifications` 控制弹窗与消息事件，`maxRetries`/`heartbeatInterval` 调整重试与心跳参数，`enabled` 控制是否自动连接。

## API 与扩展

暴露 `connect()`、`disconnect()`、`sendPing()`、`getStatus()`；事件包括 `connected`、`disconnected`、`message`、`error`、`retry`。需要自定义请求头或 POST 建连时，需改用 `@microsoft/fetch-event-source`（源码中保留了草案）或 Websocket。

## 数据与初始化

无前端数据初始化。认证上下文依赖登录后写入 sessionStorage 的 `MANGO_TOKEN` 与 `userInfo`；未登录时 `token` 参数为空字符串、租户取 `master`。

## 管理入口

无独立管理入口，通常由应用根布局挂载一个全局实例。

## 快速开始

直接 `<SSE @message="handleMessage" />` 即可；登录态变化后可通过 ref 先 `disconnect()` 再 `connect()` 重建连接。

## 问题排查

- 连接建立后收不到消息：确认后端返回 `Content-Type: text/event-stream` 且消息体是 `{ type, content }` 结构的 JSON。
- 401/鉴权失败：原生 EventSource 走 query 参数鉴权，确认后端支持 `token` 参数而不是只读 `Authorization` 头。
- 想静默监听消息：当前不支持，关闭 `showNotifications` 会同时关闭 `message` 事件，请在通知层自行过滤。
- 控制台出现重连但看不到红色重连按钮：自动重连计数每轮重置所致，参见“重要约束”。
- 环境中完全不连接：检查 `VITE_SSE_ENABLE` 是否被置为字符串 `'false'`。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
