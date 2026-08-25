<template>
  <div class="mango-ai-chat__session-list">
    <el-button class="mango-ai-chat__new" data-action="ai.service-run.new-conversation" :disabled="sending" @click="emit('create')">
      <el-icon><Plus /></el-icon>新对话
    </el-button>
    <div class="mango-ai-chat__sidebar-label">最近对话</div>
    <div v-if="!conversations.length" class="mango-ai-chat__sidebar-empty">暂无历史对话</div>
    <div
      v-for="conversation in conversations"
      :key="conversation.id"
      class="mango-ai-chat__session-wrap"
      :class="{ 'is-active': conversation.id === activeConversationId }"
    >
      <button
        type="button"
        class="mango-ai-chat__session"
        :disabled="sending"
        :data-record-key="conversation.id"
        @click="emit('select', conversation.id)"
      >
        <span>{{ conversation.title }}</span>
        <small>{{ conversation.persisted ? `${conversation.messageCount} 条消息` : '尚未开始' }}</small>
      </button>
      <el-button
        v-if="conversation.persisted"
        link
        class="mango-ai-chat__delete"
        aria-label="删除对话"
        data-action="ai.service-run.delete-conversation"
        :disabled="sending"
        @click.stop="emit('delete', conversation)"
      >
        <el-icon><Delete /></el-icon>
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Delete, Plus } from '@element-plus/icons-vue';
import type { AiConversationWorkspaceSession } from './AiConversationWorkspace.vue';

defineOptions({ name: 'AiConversationSessionList' });

defineProps<{
  conversations: AiConversationWorkspaceSession[];
  activeConversationId: string;
  sending: boolean;
}>();

const emit = defineEmits<{
  create: [];
  select: [id: string];
  delete: [conversation: AiConversationWorkspaceSession];
}>();
</script>
