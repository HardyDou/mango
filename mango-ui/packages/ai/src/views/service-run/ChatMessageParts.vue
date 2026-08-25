<template>
  <div class="mango-ai-message-parts">
    <template v-for="(part, index) in parts" :key="`${part.type}-${part.fileId || index}`">
      <p v-if="part.type === 'TEXT'" class="mango-ai-message-parts__text">{{ part.text }}</p>
      <p v-else-if="part.type === 'RICH_TEXT' && streaming" class="mango-ai-message-parts__stream-text">
        {{ part.text }}
      </p>
      <ChatMessageContent v-else-if="part.type === 'RICH_TEXT'" :content="part.text || ''" />
      <div v-else-if="part.type === 'STRUCTURED_DATA'" class="mango-ai-structured-result">
        <div class="mango-ai-structured-result__header">
          <strong>结构化结果</strong>
          <el-button link data-action="ai.service-run.copy-json" @click="copy(part.dataJson || '{}')">
            <el-icon><CopyDocument /></el-icon>复制 JSON
          </el-button>
        </div>
        <pre><code>{{ formatJson(part.dataJson ?? undefined) }}</code></pre>
      </div>
      <ChatFilePart v-else :part="part" />
    </template>
  </div>
</template>

<script setup lang="ts">
import type { AiMessageContentPart } from '@mango/ai-api';
import { CopyDocument } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import ChatFilePart from './ChatFilePart.vue';
import ChatMessageContent from './ChatMessageContent.vue';

defineOptions({ name: 'AiChatMessageParts' });
defineProps<{ parts: AiMessageContentPart[]; streaming?: boolean }>();

function formatJson(value?: string) {
  if (!value) return '{}';
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

async function copy(value: string) {
  try {
    await navigator.clipboard.writeText(formatJson(value));
    ElMessage.success('JSON 已复制');
  } catch {
    ElMessage.error('复制失败，请手动选择内容');
  }
}
</script>
