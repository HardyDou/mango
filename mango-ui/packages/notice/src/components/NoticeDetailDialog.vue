<template>
 <el-dialog v-model="visible" :title="message?.title || '消息详情'" width="560px" class="notice-detail-dialog">
 <div v-if="message" class="notice-detail">
 <div class="notice-detail__content">
 <div class="notice-detail__content-text">{{ message.content || '-' }}</div>
 </div>
 <div class="notice-detail__time">{{ message.createTime || '-' }}</div>
 </div>
 <template #footer>
 <div class="notice-detail__footer">
 <div class="notice-detail__actions">
 <el-button
 v-for="action in visibleActions"
 :key="action.actionCode"
 plain
 :type="action.interactionType === 'EVENT' ? 'primary' : 'success'"
 :disabled="isActionDisabled(action)"
 @click="emit('action', action)"
 >
 {{ action.actionLabel }}
 </el-button>
 </div>
 <el-button type="primary" @click="visible = false">确认</el-button>
 </div>
 </template>
 </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue';
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
 set: value => emit('update:modelValue', value),
});

const visibleActions = computed(() => (props.message?.actions || [])
 .filter(action => action.status !== 'DISABLED')
 .slice(0, 3));

function isActionDisabled(action: NoticeSiteMessageAction) {
 if (action.interactionType === 'EVENT') {
 return !['AVAILABLE', 'FAILED'].includes(action.status);
 }
 return ['DISABLED', 'EXPIRED'].includes(action.status);
}
</script>

<style scoped>
.notice-detail-dialog :deep(.el-dialog__header) {
 text-align: left;
}

.notice-detail-dialog :deep(.el-dialog__title) {
 display: block;
 text-align: left;
}

.notice-detail {
 display: flex;
 flex-direction: column;
 min-height: 220px;
 max-height: 420px;
}

.notice-detail__content {
 flex: 1;
 display: flex;
 align-items: center;
 justify-content: flex-start;
 overflow-y: auto;
 padding-right: 4px;
 text-align: left;
}

.notice-detail__content-text {
 width: 100%;
 white-space: pre-wrap;
 word-break: break-word;
 color: var(--el-text-color-primary);
 font-size: 15px;
 line-height: 1.8;
}

.notice-detail__time {
 flex: 0 0 auto;
 margin-top: 16px;
 color: var(--el-text-color-secondary);
 font-size: 13px;
 line-height: 1.4;
 text-align: right;
}

.notice-detail__footer {
 display: flex;
 align-items: center;
 justify-content: flex-end;
 gap: 12px;
 width: 100%;
}

.notice-detail__actions {
 display: flex;
 flex-wrap: wrap;
 gap: 8px;
 min-width: 0;
 justify-content: flex-end;
}

.notice-detail__actions :deep(.el-button) {
 margin-left: 0;
}
</style>
