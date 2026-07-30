<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="640px"
    class="notice-detail-dialog"
    destroy-on-close
  >
    <div v-if="message" class="notice-detail">
      <div
        v-for="row in detailRows"
        :key="row.key"
        class="notice-detail__row"
      >
        <span class="notice-detail__label">{{ row.label }}：</span>
        <!-- 字段值已由 notice HTML 白名单清洗，按消息协议要求使用 v-html 保留基础格式。 -->
        <!-- eslint-disable-next-line vue/no-v-html -->
        <span class="notice-detail__value" v-html="row.html" />
      </div>
    </div>

    <template #footer>
      <div class="notice-detail__footer">
        <el-button @click="visible = false">关闭</el-button>
        <el-button
          v-if="presentation.primaryAction"
          type="primary"
          data-test="notice-primary-action"
          @click="emit('action', presentation.primaryAction)"
        >
          {{ presentation.primaryActionLabel }}
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { presentNoticeMessage } from '../client/messagePresentation';
import { noticePlainText, sanitizeNoticeHtml } from '../client/html';
import type { NoticeSiteMessage, NoticeSiteMessageAction } from '../types/notice';

const props = defineProps<{
  modelValue: boolean;
  message?: NoticeSiteMessage;
}>();

const emit = defineEmits<{
  (event: 'update:modelValue', value: boolean): void;
  (event: 'action', action: NoticeSiteMessageAction): void;
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value),
});

const presentation = computed(() => presentNoticeMessage(props.message || emptyMessage));
const dialogTitle = computed(() => noticePlainText(presentation.value.typeLabel));
const detailRows = computed(() => {
  const current = presentation.value;
  return [
    { key: 'type', label: '消息类型', html: sanitizeNoticeHtml(current.typeLabel) },
    { key: 'content', label: '消息内容', html: sanitizeNoticeHtml(props.message?.content) },
    { key: 'time', label: '消息时间', html: sanitizeNoticeHtml(props.message?.createTime) },
  ];
});

const emptyMessage: NoticeSiteMessage = {
  id: '',
  title: '',
  content: '',
  userId: '',
  priority: 'NORMAL',
  readStatus: 'READ',
};
</script>

<style scoped>
.notice-detail-dialog :deep(.el-dialog__header),
.notice-detail-dialog :deep(.el-dialog__title) {
  text-align: left;
}

.notice-detail {
  display: flex;
  max-height: min(64vh, 560px);
  flex-direction: column;
  overflow-y: auto;
  padding-right: 4px;
}

.notice-detail__row {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  padding: 10px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
  font-size: 14px;
  line-height: 22px;
}

.notice-detail__row:last-child {
  border-bottom: 0;
}

.notice-detail__label {
  color: var(--el-text-color-secondary);
}

.notice-detail__value {
  min-width: 0;
  color: var(--el-text-color-primary);
  white-space: pre-wrap;
  word-break: break-word;
}

.notice-detail__value :deep(p:first-child) {
  margin-top: 0;
}

.notice-detail__value :deep(p:last-child) {
  margin-bottom: 0;
}

.notice-detail__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  width: 100%;
}

.notice-detail__footer :deep(.el-button) {
  margin-left: 0;
}

@media (max-width: 680px) {
  :global(.notice-detail-dialog.el-dialog) {
    width: calc(100vw - 24px) !important;
  }

  .notice-detail__row {
    grid-template-columns: 80px minmax(0, 1fr);
  }
}
</style>
