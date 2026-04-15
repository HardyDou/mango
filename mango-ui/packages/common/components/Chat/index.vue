<template>
  <div class="chat-component">
    <!-- Chat Launcher Button -->
    <div
      v-if="!isOpen"
      class="chat-launcher"
      @click="toggle"
    >
      <el-icon :size="24">
        <ChatDotRound />
      </el-icon>
    </div>

    <!-- Chat Window -->
    <div
      v-if="isOpen"
      class="chat-window"
    >
      <!-- Header -->
      <div class="chat-header">
        <div class="chat-title">
          <el-icon>
            <ChatDotRound />
          </el-icon>
          <span>{{ t('chat.title') }}</span>
        </div>
        <div class="chat-actions">
          <el-button
            text
            size="small"
            @click="handleNewSession"
          >
            {{ t('chat.sessionNew') }}
          </el-button>
          <el-button
            text
            size="small"
            @click="close"
          >
            <el-icon>
              <Close />
            </el-icon>
          </el-button>
        </div>
      </div>

      <!-- Messages -->
      <div
        ref="messagesRef"
        class="chat-messages"
      >
        <!-- Welcome Message -->
        <div
          v-if="messages.length === 0"
          class="chat-welcome"
        >
          <div class="welcome-icon">
            <el-icon :size="48">
              <ChatDotRound />
            </el-icon>
          </div>
          <p class="welcome-text">
            {{ t('chat.welcome') }}
          </p>

          <!-- Recommended Questions -->
          <div
            v-if="recommendedQuestions && recommendedQuestions.length > 0"
            class="recommended-questions"
          >
            <div class="recommended-label">
              {{ t('chat.recommended') }}
            </div>
            <div class="recommended-list">
              <el-tag
                v-for="q in recommendedQuestions"
                :key="q"
                class="recommended-item"
                @click="sendRecommended(q)"
              >
                {{ q }}
              </el-tag>
            </div>
          </div>
        </div>

        <!-- Message List -->
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message-item"
          :class="`message-${msg.role}`"
        >
          <div class="message-avatar">
            <el-icon v-if="msg.role === 'user'">
              <User />
            </el-icon>
            <el-icon v-else-if="msg.role === 'assistant'">
              <ChatLineRound />
            </el-icon>
            <el-icon v-else>
              <Loading />
            </el-icon>
          </div>
          <div class="message-content">
            <!-- Thinking chain (collapsible) -->
            <div
              v-if="msg.role === 'thinking' && enableThinking"
              class="thinking-section"
            >
              <div
                class="thinking-header"
                @click="toggleThinking(msg.id)"
              >
                <el-icon>
                  <QuestionFilled />
                </el-icon>
                <span>{{ t('chat.thinking') }}</span>
                <el-icon class="thinking-arrow">
                  <ArrowDown v-if="!collapsedThinking.has(msg.id)" />
                  <ArrowUp v-else />
                </el-icon>
              </div>
              <div
                v-show="!collapsedThinking.has(msg.id)"
                class="thinking-content"
              >
                {{ msg.content }}
              </div>
            </div>

            <!-- Regular message content -->
            <div
              v-else-if="msg.role !== 'thinking'"
              class="message-text"
            >
              {{ msg.content }}
            </div>
          </div>
        </div>

        <!-- Loading indicator -->
        <div
          v-if="isLoading"
          class="message-item message-assistant"
        >
          <div class="message-avatar">
            <el-icon>
              <ChatLineRound />
            </el-icon>
          </div>
          <div class="message-content">
            <div class="message-text typing">
              <span class="typing-dot" />
              <span class="typing-dot" />
              <span class="typing-dot" />
            </div>
          </div>
        </div>

        <!-- Error message -->
        <div
          v-if="error"
          class="chat-error"
        >
          <el-icon>
            <WarnTriangleFilled />
          </el-icon>
          <span>{{ errorMessage }}</span>
          <el-button
            size="small"
            type="primary"
            link
            @click="retryLastMessage"
          >
            {{ t('chat.retry') }}
          </el-button>
        </div>
      </div>

      <!-- Input Area -->
      <div class="chat-input-area">
        <el-input
          v-model="inputText"
          type="textarea"
          :placeholder="t('chat.placeholder')"
          :rows="2"
          :maxlength="maxLength"
          show-word-limit
          @keydown.enter.exact.prevent="handleSend"
        />
        <div class="chat-input-actions">
          <el-button
            :disabled="!canSend"
            type="primary"
            @click="handleSend"
          >
            {{ t('chat.send') }}
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * Chat Component - AI Dialogue
 *
 * Features:
 * - Floating chat window with launcher button
 * - SSE streaming for AI responses
 * - Thinking chain display (collapsible)
 * - Session management
 * - Recommended questions
 * - Auto-scroll to latest message
 *
 * Backend API: POST /api/ai/chat (SSE)
 * Headers: Authorization: Bearer {token}, TENANT-ID: {tenantId}
 * Request: {message, sessionId?, enableThinking?}
 * Response (SSE):
 *   data: {"type": "thinking", "content": "..."}
 *   data: {"type": "message", "content": "..."}
 *   data: {"type": "done", "sessionId": "..."}
 *   data: {"type": "error", "message": "..."}
 */

import { ref, computed, watch, nextTick, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElIcon } from 'element-plus';
import {
  ChatDotRound,
  Close,
  User,
  ChatLineRound,
  Loading,
  QuestionFilled,
  ArrowDown,
  ArrowUp,
  WarnTriangleFilled,
} from '@element-plus/icons-vue';
import { Session } from '../../utils/storage';
import type { ChatMessage, ChatProps, ChatEmits, ChatExpose, AIEvent } from './types';

const props = withDefaults(
  defineProps<ChatProps>(),
  {
    sessionId: '',
    welcomeMessage: 'chat.welcome',
    recommendedQuestions: () => [],
    enableThinking: true,
    maxLength: 2000,
    placeholder: 'chat.placeholder',
  }
);

const emit = defineEmits<ChatEmits>();

const { t } = useI18n();

// State
const isOpen = ref(false);
const inputText = ref('');
const messages = ref<ChatMessage[]>([]);
const currentSessionId = ref<string | null>(props.sessionId || null);
const isLoading = ref(false);
const error = ref(false);
const errorMessage = ref('');
const collapsedThinking = ref<Set<string>>(new Set());

// Refs
const messagesRef = ref<HTMLElement>();

// Computed
const canSend = computed(() => {
  return inputText.value.trim().length > 0 && !isLoading.value;
});

const welcomeText = computed(() => {
  const key = props.welcomeMessage;
  return key.includes('.') ? t(key) : key;
});

/**
 * Generate unique message ID
 */
function generateId(): string {
  return `msg_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`;
}

/**
 * Scroll to bottom of messages
 */
function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight;
    }
  });
}

/**
 * Toggle chat window
 */
function toggle() {
  isOpen.value = !isOpen.value;
}

/**
 * Open chat window
 */
function open() {
  isOpen.value = true;
}

/**
 * Close chat window
 */
function close() {
  isOpen.value = false;
}

/**
 * Toggle thinking chain visibility
 */
function toggleThinking(msgId: string) {
  if (collapsedThinking.value.has(msgId)) {
    collapsedThinking.value.delete(msgId);
  } else {
    collapsedThinking.value.add(msgId);
  }
}

/**
 * Send a message
 */
async function handleSend() {
  const text = inputText.value.trim();
  if (!text || isLoading.value) return;

  // Add user message
  const userMsg: ChatMessage = {
    id: generateId(),
    role: 'user',
    content: text,
    timestamp: Date.now(),
  };
  messages.value.push(userMsg);
  inputText.value = '';
  error.value = false;
  errorMessage.value = '';
  scrollToBottom();

  // Emit send event
  emit('message-send', text);

  // Start AI response
  await streamAIResponse(text);
}

/**
 * Stream AI response via SSE
 */
async function streamAIResponse(userMessage: string) {
  isLoading.value = true;

  // Create placeholder for AI response
  const thinkingMsgId = props.enableThinking ? generateId() : null;
  const messageMsgId = generateId();

  if (thinkingMsgId) {
    messages.value.push({
      id: thinkingMsgId,
      role: 'thinking',
      content: '',
      timestamp: Date.now(),
    });
  }

  try {
    const token = Session.getToken();
    const userInfo = Session.get('userInfo');
    const tenantId = userInfo?.tenantId || 'master';

    // Build request
    const requestBody = {
      message: userMessage,
      sessionId: currentSessionId.value,
      enableThinking: props.enableThinking,
    };

    // For mock mode, simulate SSE response
    if (import.meta.env.DEV && import.meta.env.VITE_USE_MOCK === 'true') {
      await mockStreamResponse(userMessage, thinkingMsgId, messageMsgId);
      return;
    }

    // Real SSE implementation using fetch with ReadableStream
    const response = await fetch('/api/ai/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
        'TENANT-ID': tenantId,
      },
      body: JSON.stringify(requestBody),
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }

    const reader = response.body?.getReader();
    const decoder = new TextDecoder();

    if (!reader) {
      throw new Error('No response body');
    }

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      const chunk = decoder.decode(value, { stream: true });
      // Parse SSE data: data: {"type": "thinking", "content": "..."}
      const lines = chunk.split('\n');
      for (const line of lines) {
        if (line.startsWith('data: ')) {
          try {
            const data = JSON.parse(line.substring(6)) as AIEvent;
            handleAIEvent(data, thinkingMsgId, messageMsgId);
          } catch (parseErr) {
            console.warn('[Chat] Failed to parse SSE data:', line);
          }
        }
      }
    }
  } catch (err) {
    console.error('[Chat] Stream error:', err);
    error.value = true;
    errorMessage.value = err instanceof Error ? err.message : t('chat.error');
    emit('error', err as Error);

    // Remove thinking message if exists
    if (thinkingMsgId) {
      messages.value = messages.value.filter((m) => m.id !== thinkingMsgId);
    }
  } finally {
    isLoading.value = false;
  }
}

/**
 * Handle AI SSE event
 */
function handleAIEvent(event: AIEvent, thinkingMsgId: string | null, messageMsgId: string) {
  switch (event.type) {
    case 'thinking':
      if (thinkingMsgId) {
        const msg = messages.value.find((m) => m.id === thinkingMsgId);
        if (msg) {
          msg.content += event.content;
        }
      }
      scrollToBottom();
      break;

    case 'message': {
      const assistantMsg: ChatMessage = {
        id: messageMsgId,
        role: 'assistant',
        content: event.content,
        timestamp: Date.now(),
      };
      messages.value.push(assistantMsg);
      scrollToBottom();
      break;
    }

    case 'done':
      currentSessionId.value = event.sessionId;
      emit('session-change', event.sessionId);
      break;

    case 'error':
      error.value = true;
      errorMessage.value = event.message;
      break;
  }
}

/**
 * Mock stream response for development
 */
async function mockStreamResponse(
  userMessage: string,
  thinkingMsgId: string | null,
  messageMsgId: string
) {
  // Simulate thinking
  if (thinkingMsgId) {
    const thinkingTexts = [
      '我需要仔细分析这个问题。',
      '用户询问的是关于',
      '让我思考一下相关的概念和原理',
      '根据我的分析，这个问题可以从以下几个方面来解答',
    ];

    for (const text of thinkingTexts) {
      await new Promise((resolve) => setTimeout(resolve, 300));
      const msg = messages.value.find((m) => m.id === thinkingMsgId);
      if (msg) {
        msg.content = text;
      }
      scrollToBottom();
    }
  }

  // Simulate response
  const responses: Record<string, string> = {
    default: '感谢您的提问。关于您提到的内容，我需要进一步了解具体需求才能给出准确的答案。建议您提供更多背景信息，我可以为您提供更详细的解答。',
    hello: '您好！很高兴为您服务。有什么可以帮助您的吗？',
    help: '我可以帮助您解答各种问题，包括技术咨询、业务指导、问题诊断等。请告诉我您具体需要什么帮助。',
  };

  const responseText = responses[userMessage.toLowerCase()] || responses.default;

  await new Promise((resolve) => setTimeout(resolve, 500));

  // Stream response character by character
  for (let i = 0; i < responseText.length; i++) {
    await new Promise((resolve) => setTimeout(resolve, 30));

    const existingMsg = messages.value.find((m) => m.id === messageMsgId);
    if (existingMsg) {
      existingMsg.content = responseText.substring(0, i + 1);
    } else {
      messages.value.push({
        id: messageMsgId,
        role: 'assistant',
        content: responseText.substring(0, i + 1),
        timestamp: Date.now(),
      });
    }
    scrollToBottom();
  }

  // Remove thinking message
  if (thinkingMsgId) {
    messages.value = messages.value.filter((m) => m.id !== thinkingMsgId);
  }
}

/**
 * Send a recommended question
 */
function sendRecommended(question: string) {
  inputText.value = question;
  handleSend();
}

/**
 * Retry last message
 */
function retryLastMessage() {
  const lastUserMsg = messages.value.filter((m) => m.role === 'user').pop();
  if (lastUserMsg) {
    // Remove last user message and any assistant responses
    const lastUserIndex = messages.value.findIndex((m) => m.id === lastUserMsg.id);
    messages.value = messages.value.slice(0, lastUserIndex);
    error.value = false;
    errorMessage.value = '';
    streamAIResponse(lastUserMsg.content);
  }
}

/**
 * Start a new session
 */
function handleNewSession() {
  messages.value = [];
  currentSessionId.value = null;
  error.value = false;
  errorMessage.value = '';
}

/**
 * Get current session ID
 */
function getSessionId(): string | null {
  return currentSessionId.value;
}

/**
 * Clear current session
 */
function clearSession() {
  messages.value = [];
  currentSessionId.value = null;
  error.value = false;
  errorMessage.value = '';
}

// Watch for input changes
watch(inputText, (val) => {
  if (val.length > props.maxLength) {
    inputText.value = val.substring(0, props.maxLength);
  }
});

// Expose methods
defineExpose<ChatExpose>({
  open,
  close,
  toggle,
  clearSession,
  getSessionId,
});
</script>

<style scoped lang="scss">
.chat-component {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 1000;
}

.chat-launcher {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: scale(1.05);
    box-shadow: 0 6px 16px rgba(64, 158, 255, 0.5);
  }
}

.chat-window {
  width: 380px;
  height: 520px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);
  color: white;
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 500;
}

.chat-actions {
  display: flex;
  gap: 8px;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f7fa;
}

.chat-welcome {
  text-align: center;
  padding: 40px 20px;

  .welcome-icon {
    color: #409eff;
    margin-bottom: 16px;
  }

  .welcome-text {
    color: #606266;
    font-size: 14px;
    line-height: 1.6;
    margin-bottom: 24px;
  }
}

.recommended-questions {
  text-align: left;

  .recommended-label {
    font-size: 12px;
    color: #909399;
    margin-bottom: 8px;
  }

  .recommended-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .recommended-item {
    cursor: pointer;
  }
}

.message-item {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;

  &.message-user {
    flex-direction: row-reverse;

    .message-content {
      align-items: flex-end;
    }

    .message-text {
      background: #409eff;
      color: white;
      border-radius: 16px 16px 4px 16px;
    }
  }

  &.message-assistant {
    .message-text {
      background: white;
      color: #303133;
      border-radius: 16px 16px 16px 4px;
    }
  }

  &.message-thinking {
    .message-content {
      width: 100%;
    }
  }
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #ecf5ff;
  color: #409eff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.message-content {
  display: flex;
  flex-direction: column;
  max-width: 75%;
}

.message-text {
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;

  &.typing {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 12px 16px;
  }
}

.typing-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #909399;
  animation: typing 1.4s infinite;

  &:nth-child(2) {
    animation-delay: 0.2s;
  }

  &:nth-child(3) {
    animation-delay: 0.4s;
  }
}

@keyframes typing {
  0%, 60%, 100% {
    opacity: 0.3;
    transform: translateY(0);
  }
  30% {
    opacity: 1;
    transform: translateY(-4px);
  }
}

.thinking-section {
  background: white;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 8px;
}

.thinking-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  font-size: 12px;
  color: #909399;
  cursor: pointer;
  background: #f5f7fa;
  border-radius: 8px;

  .thinking-arrow {
    margin-left: auto;
  }
}

.thinking-content {
  padding: 8px 12px;
  font-size: 12px;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
  border-top: 1px solid #ebeef5;
}

.chat-error {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #fef0f0;
  border-radius: 8px;
  color: #f56c6c;
  font-size: 12px;

  span {
    flex: 1;
  }
}

.chat-input-area {
  padding: 12px;
  background: white;
  border-top: 1px solid #ebeef5;
}

.chat-input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}
</style>
