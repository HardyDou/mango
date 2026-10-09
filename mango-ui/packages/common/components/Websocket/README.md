# Websocket

WebSocket 通信组件。封装浏览器原生 `WebSocket`，连接 `mango-message` 聊天通道，内置 JSON 消息格式、30 秒心跳、指数退避重连逻辑、连接状态指示与通知弹窗。

## 导入

```ts
import { Websocket } from '@mango/common';
```

> 组件内的消息与状态类型（`WSMessage`、`WSStatus`、`WebsocketProps`、`WebsocketEmits`、`WebsocketExpose`）当前随组件源码导出，未在 `@mango/common` 包入口单独导出，使用方按事件载荷自行声明类型即可。

## 后端端点

- 默认连接地址：`/mango-message/ws/chat`
- 协议按当前页面自动选择：HTTPS 页面使用 `wss:`，否则 `ws:`
- 开发环境（`import.meta.env.DEV`）连接 `ws(s)://{当前 host}/mango-message/ws/chat`，经由 Vite 代理转发；生产环境使用相对路径解析
- 鉴权信息通过查询参数传递：`/mango-message/ws/chat?token={token}&tenantId={tenantId}`；同时把 `tenantId` 作为 WebSocket 子协议传入：`new WebSocket(url, [tenantId])`
- `token` 取自 `Session.getToken()`（sessionStorage 的 `MANGO_TOKEN`，回退同名 Cookie），为空时不追加 `token` 参数；`tenantId` 取自 sessionStorage 中 `userInfo.tenantId`，缺省为 `master`
- 收发消息均为 JSON 文本，心跳由客户端定时发送 `{ "type": "ping" }`，服务端回 `{ "type": "pong" }` 时静默忽略
- 环境变量 `VITE_WS_ENABLE === 'false'` 时组件不发起连接（仅打印告警）

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `url` | `string` | `'/mango-message/ws/chat'` | WebSocket 端点（相对路径） |
| `showStatus` | `boolean` | `true` | 是否展示连接状态圆点与文案 |
| `showNotifications` | `boolean` | `true` | 是否对收到的消息弹出 Element Plus 通知；同时控制 `message` 事件是否抛出 |
| `maxRetries` | `number` | `6` | 最大重试次数 |
| `heartbeatInterval` | `number` | `30000` | 心跳发送间隔（毫秒） |
| `enabled` | `boolean` | `true` | 是否在挂载后自动连接 |

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `connected` | - | 连接打开（`onopen`），同时清零重试计数并启动心跳 |
| `disconnected` | - | 连接关闭（`onclose`）或主动断开 |
| `message` | `data: WSMessage` | 收到业务消息；仅当 `showNotifications` 为 `true` 且消息 `content` 非空时抛出 |
| `error` | `error: Event` | WebSocket `onerror` 原生事件对象 |
| `retry` | `count: number` | 进入重试调度时抛出当前次数（从 1 开始） |

## Expose

| 名称 | 参数 | 返回值 | 说明 |
|------|------|--------|------|
| `connect()` | - | - | 建立连接；`connecting`/`connected` 状态下调用会被忽略 |
| `disconnect()` | - | - | 关闭连接、清理心跳与重连定时器并置为已断开 |
| `send(data)` | `data: WSMessage` | - | 以 JSON 文本发送消息；连接非 `OPEN` 状态时静默丢弃 |
| `sendPing()` | - | - | 连接打开时发送 `{ type: 'ping' }`，未打开则不执行 |
| `getStatus()` | - | `WSStatus` | 获取当前连接状态 |

## 类型定义

```ts
interface WSMessage {
  type: 'ping' | 'pong' | 'message';
  content?: string;
}

type WSStatus = 'disconnected' | 'connecting' | 'connected' | 'retrying' | 'error';

interface WebsocketProps {
  url?: string;
  showStatus?: boolean;
  showNotifications?: boolean;
  maxRetries?: number;
  heartbeatInterval?: number;
  enabled?: boolean;
}
```

连接状态与状态点颜色：`connected` 绿色、`connecting`/`retrying` 橙色呼吸动画、`error` 红色、`disconnected` 灰色。

内置中文 i18n 文案（`Websocket/i18n/zh-cn.ts`，命名空间 `websocket`）：

| key | 文案 |
|-----|------|
| `websocket.connected` | 已连接 |
| `websocket.connecting` | 正在连接... |
| `websocket.disconnected` | 已断开 |
| `websocket.retrying` | 连接断开，正在重连 {count}/{max} |
| `websocket.error` | 连接失败 |
| `websocket.reconnect` | 重新连接 |
| `websocket.message` | 消息 |

业务消息统一弹出 info 样式通知，标题为“消息”，内容为 `content`，弹窗时长 5000ms。

## 使用示例

### 基础用法（默认自动连接 + 通知）

```vue
<script setup lang="ts">
import { Websocket } from '@mango/common';

function onMessage(data: { type: string; content?: string }) {
  console.log('收到消息', data.content);
}
</script>

<template>
  <Websocket @message="onMessage" />
</template>
```

### 主动发送消息

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { Websocket } from '@mango/common';

const wsRef = ref<InstanceType<typeof Websocket> | null>(null);
const text = ref('');

function sendText() {
  wsRef.value?.send({ type: 'message', content: text.value });
  text.value = '';
}

function onConnected() {
  console.log('聊天通道已连接');
}
</script>

<template>
  <Websocket ref="wsRef" @connected="onConnected" />
  <el-input v-model="text" placeholder="输入消息" @keyup.enter="sendText" />
  <el-button type="primary" @click="sendText">发送</el-button>
</template>
```

### 监听连接状态并手动断线重连

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { Websocket } from '@mango/common';

const wsRef = ref<InstanceType<typeof Websocket> | null>(null);

function onDisconnected() {
  // 运行时断线不会自动重连，需要宿主自行决定是否重新连接
  console.log('连接已关闭');
}

function reconnect() {
  wsRef.value?.disconnect();
  wsRef.value?.connect();
}
</script>

<template>
  <Websocket
    ref="wsRef"
    :show-notifications="false"
    @disconnected="onDisconnected"
  />
  <el-button size="small" @click="reconnect">重新连接</el-button>
</template>
```

## 重要约束

- 鉴权依赖查询参数 `token`、`tenantId` 与子协议 `[tenantId]`，浏览器 WebSocket 无法设置 `Authorization` 请求头，后端必须按此方式鉴权和路由租户。
- 运行时连接异常时，`onerror` 只抛出 `error` 事件，随后的 `onclose` 只把状态置为 `disconnected` 并抛出 `disconnected`，二者均不会触发自动重连；指数退避重试逻辑（`min(1000 × 2^次数, 30000)` ms，最多 6 次）仅在 `new WebSocket()` 构造同步抛异常的 catch 分支中执行。需要断线自动重连时，宿主应监听 `disconnected`/`error` 后调用 `connect()`。
- `retryCount` 只在连接成功或手动重连时清零；因此“重试 6 次后显示红色‘重新连接’按钮”的 UI 在常规运行时断线路径下不会出现。
- `send()` 与 `sendPing()` 仅在 `readyState === OPEN` 时发送，其余状态静默丢弃，没有发送队列；调用方需通过 `connected` 事件或 `getStatus()` 自行保证时机。
- `message` 事件在通知函数内抛出：`showNotifications` 为 `false`、或消息没有非空 `content` 时，既不弹通知也不触发 `message` 事件，无法实现“静默监听”。
- 收到无法 `JSON.parse` 的数据只打印错误；`pong` 消息静默忽略；组件不会解析 `ping` 之外的其他系统帧。
- 组件挂载时若 `enabled` 为 `true` 自动连接，卸载时自动关闭并清理心跳/重连定时器；多实例会建立多条独立连接。

## 概览

面向 `mango-message` 实时聊天通道的双向通信组件，提供连接管理、心跳保活与消息通知能力。

## 功能清单

支持自动连接、JSON 收发、30 秒 ping 心跳、租户子协议、状态指示、消息弹窗、指数退避重试逻辑、手动连接/断开/发送及卸载自动清理。

## 接入方式

从 `@mango/common` 导入 `Websocket`，放置在需要实时聊天或双向消息的页面/布局中，默认挂载即连接 `/mango-message/ws/chat`，通过 `send()` 发消息、`message` 事件收消息。

## 配置说明

用 `url` 覆盖端点，`showStatus` 控制状态 UI，`showNotifications` 控制弹窗与消息事件，`maxRetries`/`heartbeatInterval` 调整重试与心跳参数，`enabled` 控制是否自动连接。

## API 与扩展

暴露 `connect()`、`disconnect()`、`send(data)`、`sendPing()`、`getStatus()`；事件包括 `connected`、`disconnected`、`message`、`error`、`retry`。需要消息队列、二进制帧或自定义协议时建议直接使用原生 `WebSocket` 另行封装。

## 数据与初始化

无前端数据初始化。认证上下文依赖登录后写入 sessionStorage 的 `MANGO_TOKEN` 与 `userInfo`；未登录时不带 `token` 参数、租户取 `master`。

## 管理入口

无独立管理入口，通常由聊天面板或全局布局按需挂载。

## 快速开始

放置 `<Websocket @message="handleMessage" />` 即自动建连；在 `connected` 事件后调用 ref 的 `send({ type: 'message', content })` 发送消息。

## 问题排查

- 连接立即关闭：检查 URL、`token` 查询参数及后端 WebSocket 子协议校验（组件会把 `tenantId` 作为子协议上送）。
- 断线后没有自动重连：当前实现运行时断线只抛 `disconnected`，需宿主监听后手动 `connect()`，参见示例。
- 收不到 `message` 事件：确认消息 JSON 含非空 `content`，且没有把 `showNotifications` 置为 `false`。
- 发送消息无反应：连接未处于 OPEN 状态时发送会被静默丢弃，请先等待 `connected`。
- 环境中完全不连接：检查 `VITE_WS_ENABLE` 是否被置为字符串 `'false'`。
- 开发环境连不通：开发态走当前页面 host 的 Vite 代理，需在 Vite 配置中代理 `/mango-message/ws/chat`。

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
