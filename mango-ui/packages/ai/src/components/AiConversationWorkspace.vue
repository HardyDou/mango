<template>
  <div class="mango-ai-chat" :class="{ 'is-sidebar-collapsed': sidebarCollapsed }" data-surface="ai.conversation-workspace">
    <aside v-show="!sidebarCollapsed" class="mango-ai-chat__sidebar" data-surface="ai.service-run.conversations">
      <div class="mango-ai-chat__sidebar-heading">
        <span>对话</span>
        <el-tooltip content="收起会话栏" placement="right">
          <el-button
            circle
            text
            aria-label="收起会话栏"
            data-action="ai.conversation-workspace.collapse"
            @click="sidebarCollapsed = true"
          >
            <el-icon><Fold /></el-icon>
          </el-button>
        </el-tooltip>
      </div>
      <AiConversationSessionList
        :conversations="conversations"
        :active-conversation-id="activeConversationId"
        :sending="sending"
        @create="emit('create')"
        @select="emit('select', $event)"
        @delete="emit('delete', $event)"
      />
    </aside>

    <section class="mango-ai-chat__main" data-surface="ai.service-run.chat">
      <div class="mango-ai-chat__conversation-bar">
        <div class="mango-ai-chat__conversation-title">
          <el-tooltip v-if="sidebarCollapsed" content="展开会话栏" placement="bottom">
            <el-button
              circle
              text
              aria-label="展开会话栏"
              data-action="ai.conversation-workspace.expand"
              @click="sidebarCollapsed = false"
            >
              <el-icon><Expand /></el-icon>
            </el-button>
          </el-tooltip>
          <el-button
            class="mango-ai-chat__mobile-sessions"
            text
            aria-label="打开对话列表"
            data-action="ai.conversation-workspace.mobile-sessions"
            @click="mobileSessionsOpen = true"
          >
            <el-icon><Menu /></el-icon>
          </el-button>
          <div>
            <strong>{{ activeConversation?.title || '新对话' }}</strong>
          </div>
        </div>
        <div class="mango-ai-chat__conversation-state">
          <el-tag v-if="sending" size="small" type="warning" effect="plain">正在生成</el-tag>
          <el-tag v-else-if="activeConversation?.persisted" size="small" type="success" effect="plain">已保存</el-tag>
        </div>
      </div>

      <div
        ref="messagePanel"
        class="mango-ai-chat__messages"
        data-surface="ai.service-run.messages"
        :aria-busy="sending"
        @scroll.passive="handleMessageScroll"
      >
        <div v-if="conversationLoading" class="mango-ai-chat__loading"><el-skeleton :rows="6" animated /></div>
        <div v-else-if="!activeConversation?.messages.length" class="mango-ai-chat__welcome" data-state="empty">
          <div class="mango-ai-chat__welcome-mark">AI</div>
          <h1>{{ welcomeTitle }}</h1>
          <p>{{ welcomeDescription }}</p>
          <div class="mango-ai-chat__suggestions">
            <button
              v-for="suggestion in suggestions"
              :key="suggestion.title"
              type="button"
              :disabled="sending"
              @click="emit('suggestion', suggestion.value)"
            >
              <strong>{{ suggestion.title }}</strong>
              <span>{{ suggestion.description }}</span>
            </button>
          </div>
        </div>

        <article
          v-for="message in activeConversation?.messages || []"
          v-else
          :key="message.id"
          class="mango-ai-chat__message"
          :class="`is-${message.role}`"
          :data-state="`ai.service-run.message.${message.role}`"
        >
          <div class="mango-ai-chat__avatar">{{ message.role === 'user' ? '你' : 'AI' }}</div>
          <div class="mango-ai-chat__message-body">
            <details v-if="message.role === 'thinking'" class="mango-ai-chat__reasoning">
              <summary>思考过程</summary>
              <p>{{ message.reasoning || '正在思考…' }}</p>
            </details>
            <template v-else>
              <slot name="message" :message="message" />
              <div
                v-if="message.role === 'assistant' && message.modelName"
                class="mango-ai-chat__message-model"
                data-state="ai.service-run.message-model"
              >
                <span>{{ message.modelName }}</span>
                <span v-if="message.providerCode">{{ message.providerCode }}</span>
                <span v-if="message.thinkingEnabled">深度思考</span>
              </div>
              <div
                v-if="message.generationStatus"
                class="mango-ai-chat__stream-status"
                :data-state="`ai.service-run.${message.generationStatus}`"
                role="status"
                aria-live="polite"
              >
                <span class="mango-ai-chat__stream-dots" aria-hidden="true"><i /><i /><i /></span>
                <span>{{ generationStatusLabel(message.generationStatus) }}</span>
              </div>
            </template>
          </div>
        </article>
      </div>

      <el-button
        v-if="showScrollToLatest"
        class="mango-ai-chat__scroll-latest"
        circle
        aria-label="回到最新消息"
        data-action="ai.service-run.scroll-latest"
        @click="scrollToLatest"
      >
        <el-icon><ArrowDown /></el-icon>
      </el-button>

      <div class="mango-ai-chat__composer-wrap">
        <slot name="composer" />
      </div>
    </section>

    <el-drawer
      v-model="mobileSessionsOpen"
      class="mango-ai-chat__mobile-drawer"
      direction="ltr"
      size="min(86vw, 336px)"
      title="对话"
      :with-header="true"
    >
      <AiConversationSessionList
        :conversations="conversations"
        :active-conversation-id="activeConversationId"
        :sending="sending"
        @create="handleMobileCreate"
        @select="handleMobileSelect"
        @delete="emit('delete', $event)"
      />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import type { AiMessageContentPart } from '@mango/ai-api';
import { ArrowDown, Expand, Fold, Menu } from '@element-plus/icons-vue';
import { nextTick, ref } from 'vue';
import AiConversationSessionList from './AiConversationSessionList.vue';

defineOptions({ name: 'AiConversationWorkspace' });

export interface AiConversationWorkspaceMessage {
  id: string;
  role: 'user' | 'assistant' | 'thinking';
  contentParts: AiMessageContentPart[];
  reasoning?: string;
  modelName?: string;
  providerCode?: string;
  thinkingEnabled?: boolean;
  generationStatus?: 'connecting' | 'thinking' | 'responding' | 'finalizing';
}

export interface AiConversationWorkspaceSession<TMessage extends AiConversationWorkspaceMessage = AiConversationWorkspaceMessage> {
  id: string;
  title: string;
  messages: TMessage[];
  persisted: boolean;
  messageCount: number;
}

export interface AiConversationSuggestion {
  title: string;
  description: string;
  value: string;
}

defineProps<{
  conversations: AiConversationWorkspaceSession[];
  activeConversationId: string;
  activeConversation?: AiConversationWorkspaceSession;
  sending: boolean;
  conversationLoading: boolean;
  welcomeTitle: string;
  welcomeDescription: string;
  suggestions: AiConversationSuggestion[];
}>();

const emit = defineEmits<{
  create: [];
  select: [id: string];
  delete: [conversation: AiConversationWorkspaceSession];
  suggestion: [value: string];
}>();

defineSlots<{
  message(props: { message: AiConversationWorkspaceMessage }): unknown;
  composer(): unknown;
}>();

const messagePanel = ref<HTMLElement>();
const sidebarCollapsed = ref(false);
const mobileSessionsOpen = ref(false);
const showScrollToLatest = ref(false);

function handleMessageScroll() {
  const panel = messagePanel.value;
  if (panel) showScrollToLatest.value = panel.scrollHeight - panel.scrollTop - panel.clientHeight > 96;
}

function generationStatusLabel(status: NonNullable<AiConversationWorkspaceMessage['generationStatus']>) {
  return {
    connecting: '正在连接模型',
    thinking: '正在思考',
    responding: '正在生成',
    finalizing: '正在整理回答',
  }[status];
}

function scrollToLatest() {
  showScrollToLatest.value = false;
  void scrollToBottom(true, true);
}

function handleMobileCreate() {
  mobileSessionsOpen.value = false;
  emit('create');
}

function handleMobileSelect(id: string) {
  mobileSessionsOpen.value = false;
  emit('select', id);
}

async function scrollToBottom(force = false, smooth = false) {
  if (!force && showScrollToLatest.value) return;
  await nextTick();
  if (messagePanel.value) {
    if (smooth && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      messagePanel.value.scrollTo({ top: messagePanel.value.scrollHeight, behavior: 'smooth' });
    } else {
      messagePanel.value.scrollTop = messagePanel.value.scrollHeight;
    }
    showScrollToLatest.value = false;
  }
}

defineExpose({ scrollToBottom });
</script>
