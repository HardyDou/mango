<template>
 <el-dialog v-model="visible" :title="message?.title || '消息详情'" width="520px" class="notice-detail-dialog">
 <div v-if="message" class="notice-detail">
 <div class="notice-detail__content">{{ message.content || '-' }}</div>
 <div class="notice-detail__time">{{ message.createTime || '-' }}</div>
 </div>
 <template #footer>
 <el-button type="primary" @click="visible = false">确认</el-button>
 </template>
 </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { NoticeSiteMessage } from '../types/notice';

const props = defineProps<{
 modelValue: boolean;
 message?: NoticeSiteMessage;
}>();

const emit = defineEmits<{
 (event: 'update:modelValue', value: boolean): void;
}>();

const visible = computed({
 get: () => props.modelValue,
 set: value => emit('update:modelValue', value),
});
</script>

<style scoped>
.notice-detail {
 min-height: 120px;
}

.notice-detail__content {
 white-space: pre-wrap;
 word-break: break-word;
 color: var(--el-text-color-primary);
 font-size: 14px;
 line-height: 1.8;
}

.notice-detail__time {
 margin-top: 20px;
 color: var(--el-text-color-secondary);
 font-size: 13px;
 line-height: 1.4;
 text-align: right;
}
</style>
