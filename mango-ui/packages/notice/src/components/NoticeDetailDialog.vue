<template>
 <el-dialog v-model="visible" title="系统消息详情" width="560px">
 <el-descriptions v-if="message" :column="1" border>
 <el-descriptions-item label="标题">{{ message.title }}</el-descriptions-item>
 <el-descriptions-item label="业务类型">{{ message.bizType || '-' }}</el-descriptions-item>
 <el-descriptions-item label="业务ID">{{ message.bizId || '-' }}</el-descriptions-item>
 <el-descriptions-item label="优先级">{{ message.priority }}</el-descriptions-item>
 <el-descriptions-item label="状态">{{ message.readStatus === 'READ' ? '已读' : '未读' }}</el-descriptions-item>
 <el-descriptions-item label="创建时间">{{ message.createTime || '-' }}</el-descriptions-item>
 <el-descriptions-item label="内容">{{ message.content }}</el-descriptions-item>
 </el-descriptions>
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
