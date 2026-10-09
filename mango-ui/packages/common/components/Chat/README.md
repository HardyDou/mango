# Chat

右下角悬浮式 AI 对话组件，提供启动按钮、消息流、可折叠思维链和会话管理，消息传输由调用方通过必传的 `stream` provider 提供。

## 导入

```ts
import { Chat } from '@mango/common';
import type { ChatStreamProvider, ChatExpose, AIEvent } from '@mango/common';
```

## Props

| 名称 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `stream` | `ChatStreamProvider` | - | **必传**。AI 事件流提供者，负责真实传输与鉴权，组件不内置任何请求逻辑 |
| `sessionId` | `string` | `''` | 初始会话 ID；首次 `done` 事件后会更新为服务端返回的会话 ID |
| `welcomeMessage` | `string` | `'chat.welcome'` | 空状态欢迎语配置（当前模板固定读取 i18n 的 `chat.welcome`，详见重要约束） |
| `recommendedQuestions` | `string[]` | `[]` | 空状态展示的推荐问题，点击后直接发送 |
| `enableThinking` | `boolean` | `true` | 是否创建并展示可折叠的思维链消息 |
| `maxLength` | `number` | `2000` | 输入框最大字数，超长会被截断 |
| `placeholder` | `string` | `'chat.placeholder'` | 输入框占位配置（当前模板固定读取 i18n 的 `chat.placeholder`，详见重要约束） |

## Events

| 名称 | 参数 | 说明 |
|------|------|------|
| `message-send` | `content: string` | 用户发送消息时触发 |
| `session-change` | `sessionId: string` | 收到 `done` 事件、会话 ID 更新时触发 |
| `error` | `error: Error` | `stream` provider 抛错时触发；组件内同时展示错误条和重试入口 |

## Expose

| 名称 | 参数 | 说明 |
|------|------|------|
| `open()` | - | 打开聊天窗口 |
| `close()` | - | 关闭聊天窗口 |
| `toggle()` | - | 切换窗口显隐 |
| `clearSession()` | - | 中止当前流、清空消息并将会话 ID 置空 |
| `getSessionId()` | - | 获取当前会话 ID，返回 `string \| null` |

## 类型定义

```ts
/** 聊天消息 */
export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant' | 'thinking';
  content: string;
  timestamp: number;
}

/** 会话快照 */
export interface ChatSession {
  sessionId: string;
  messages: ChatMessage[];
}

/** 每次发送传给 stream provider 的命令 */
export interface ChatStreamCommand {
  message: string;
  sessionId: string | null;
  enableThinking: boolean;
}

/** 调用方实现的事件流提供者 */
export type ChatStreamProvider = (
  command: ChatStreamCommand,
  onEvent: (event: AIEvent) => void,
  signal: AbortSignal,
) => Promise<void>;

export interface AIThinkingEvent {
  type: 'thinking';
  content: string;
}
export interface AIMessageEvent {
  type: 'message';
  content: string;
}
export interface AIDoneEvent {
  type: 'done';
  sessionId: string;
}
export interface AIErrorEvent {
  type: 'error';
  message: string;
}
export type AIEvent = AIThinkingEvent | AIMessageEvent | AIDoneEvent | AIErrorEvent;
```

事件语义：`thinking` 事件把 `content` 追加到思维链消息；`message` 事件插入一条助手消息；`done` 事件记录新的会话 ID 并触发 `session-change`；`error` 事件在消息区内显示错误文案。

## 使用示例

### 基础用法：实现 stream provider

组件不关心传输方式（fetch、SSE、SDK 均可），只需在 provider 内解析数据并回调 `onEvent`。

```vue
<script setup lang="ts">
import { Chat } from '@mango/common';
import type { AIEvent, ChatStreamCommand, ChatStreamProvider } from '@mango/common';

const stream: ChatStreamProvider = async (command, onEvent, signal) => {
  const response = await fetch('/api/ai/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      message: command.message,
      sessionId: command.sessionId,
      enableThinking: command.enableThinking,
    }),
    signal,
  });
  if (!response.ok || !response.body) {
    throw new Error('AI 服务请求失败');
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split('\n');
    buffer = lines.pop() ?? '';
    for (const line of lines) {
      const trimmed = line.trim();
      if (!trimmed.startsWith('data:')) continue;
      const payload = trimmed.slice(5).trim();
      if (payload && payload !== '[DONE]') {
        onEvent(JSON.parse(payload) as AIEvent);
      }
    }
  }
};
</script>

<template>
  <Chat
    :stream="stream"
    :recommended-questions="['如何重置密码？', '如何创建应用？']"
  />
</template>
```

### 携带初始会话并监听事件

```vue
<script setup lang="ts">
import { Chat } from '@mango/common';
import type { ChatStreamProvider } from '@mango/common';

const stream: ChatStreamProvider = async (command, onEvent, signal) => {
  // 由业务侧实现，鉴权信息在此处统一注入
};

function handleSessionChange(sessionId: string) {
  console.log('会话 ID 已更新：', sessionId);
}

function handleError(error: Error) {
  console.error('对话失败：', error.message);
}
</script>

<template>
  <Chat
    session-id="sess-1001"
    :stream="stream"
    :enable-thinking="false"
    :max-length="1000"
    @session-change="handleSessionChange"
    @error="handleError"
  />
</template>
```

### 通过 ref 控制窗口和会话

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { Chat } from '@mango/common';
import type { ChatExpose, ChatStreamProvider } from '@mango/common';

const chatRef = ref<ChatExpose>();
const stream: ChatStreamProvider = async () => {};

function openAndCheck() {
  chatRef.value?.open();
  console.log('当前会话：', chatRef.value?.getSessionId());
}

function restart() {
  chatRef.value?.clearSession();
}
</script>

<template>
  <el-button @click="openAndCheck">打开助手</el-button>
  <el-button @click="restart">重置会话</el-button>
  <Chat ref="chatRef" :stream="stream" />
</template>
```

## 重要约束

- `stream` 为必传 prop。组件不内置任何传输、地址和鉴权逻辑；请求地址、请求头（含令牌）、SSE/SDK 解析全部由调用方在 provider 中完成
- provider 必须遵守 `AbortSignal`：发送新消息、清空会话时组件会中止上一个请求；被中止导致的抛错不会展示错误条
- 组件通过 `vue-i18n` 的 `useI18n()` 读取 `chat.title`、`chat.welcome`、`chat.placeholder`、`chat.send`、`chat.retry`、`chat.recommended`、`chat.sessionNew`、`chat.thinking`、`chat.error` 等文案，宿主需保证 i18n 环境可用（简体中文默认欢迎语为「您好，我是 AI 助手，有什么可以帮您的吗？」）
- 当前模板空状态欢迎语固定取 i18n key `chat.welcome`、输入框占位固定取 `chat.placeholder`；`welcomeMessage` / `placeholder` 两个 prop 及其「含 `.` 按 i18n key 解析、否则按字面量」的计算属性已声明但未被模板引用，传入不会改变界面文案
- 窗口为 `position: fixed` 固定在视口右下角（`bottom/right: 20px`，`z-index: 1000`），窗口尺寸 380×520，不支持通过 prop 调整位置和尺寸，也没有打开状态的 `v-model`
- 仅 Enter（无 Shift）发送，Shift+Enter 换行；加载中发送按钮禁用，思维链仅在 `enableThinking` 为 `true` 时出现
- 组件无 Slots

## 概览

提供悬浮入口的 AI 对话窗口，支持流式回复、思维链折叠、推荐问题、错误重试和会话切换。

## 功能清单

悬浮启动按钮、消息列表与自动滚动、流式事件渲染、可折叠思维链、推荐问题、错误条与重试、新对话、字数限制和会话 ID 管理。

## 接入方式

从 `@mango/common` 导入 `Chat`，自行实现 `ChatStreamProvider` 并通过必传的 `stream` prop 传入；页面直接放置组件即可获得右下角悬浮入口。

## 配置说明

通过 `sessionId` 指定初始会话，`recommendedQuestions` 配置推荐问题，`enableThinking` 控制思维链，`maxLength` 限制输入长度；传输相关配置属于调用方的 provider。

## API 与扩展

支持 `message-send`、`session-change`、`error` 事件，暴露 `open()`、`close()`、`toggle()`、`clearSession()`、`getSessionId()` 方法；通过 `AIEvent` 联合类型扩展事件协议的消费方代码，组件不支持 slot 自定义 UI。

## 数据与初始化

无本地持久化，消息仅保存在组件内部状态；刷新或 `clearSession()` 后清空，会话连续性依赖 provider 透传 `sessionId`。

## 管理入口

无独立管理入口，由业务页面挂载。

## 快速开始

参见「基础用法：实现 stream provider」示例，最小可用接入只需提供一个 `stream` 函数。

## 问题排查

- 窗口无响应或发不出消息：确认已传入 `stream` 且 provider 内调用了 `onEvent`
- 回复不显示：核对事件 `type` 是否为 `thinking` / `message` / `done` / `error` 之一，`done` 必须带 `sessionId`
- 出现错误条：provider 抛错时会展示 `Error.message`（缺失时回退到 i18n 的 `chat.error`），用户可点击重试，重试会从最后一条用户消息处重新发起
- 文案为 key 原文：检查宿主的 vue-i18n 是否注册了 `chat` 命名空间文案

## 相关文档

- [Common 包说明](../../README.md)
- [能力说明规范](../../../../../mango-pmo/rules/08-capability-docs.md)
