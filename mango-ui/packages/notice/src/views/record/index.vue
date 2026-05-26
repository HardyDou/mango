<template>
 <div class="notice-record-page">
 <el-card shadow="never">
 <template #header><span>发送记录</span></template>
 <el-table :data="records" border stripe v-loading="loading">
 <el-table-column prop="taskId" label="任务ID" width="180" />
 <el-table-column prop="recipientId" label="接收人ID" width="180" />
 <el-table-column prop="bizType" label="业务类型" width="180" />
 <el-table-column prop="bizId" label="业务对象" width="160" />
 <el-table-column prop="channelType" label="渠道" width="160" />
 <el-table-column prop="requestId" label="请求流水号" width="180" show-overflow-tooltip />
 <el-table-column prop="status" label="状态" width="140" />
 <el-table-column prop="renderedTitle" label="标题" min-width="200" />
 <el-table-column prop="renderedContent" label="内容摘要" min-width="220" show-overflow-tooltip />
 <el-table-column prop="requestSnapshot" label="请求摘要" min-width="260" show-overflow-tooltip />
 <el-table-column prop="responseSnapshot" label="响应摘要" min-width="260" show-overflow-tooltip />
 <el-table-column prop="providerMessageId" label="供应商消息ID" width="180" />
 <el-table-column prop="failReason" label="失败原因" min-width="220" />
 <el-table-column prop="retryCount" label="重试" width="80" />
 </el-table>
 </el-card>
 </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { getSendRecords } from '../../api/notice';
import type { NoticeSendRecord } from '../../types/notice';
const loading = ref(false);
const records = ref<NoticeSendRecord[]>([]);
async function load() { loading.value = true; try { const result = await getSendRecords(); records.value = result.list || []; } finally { loading.value = false; } }
onMounted(load);
</script>
<style scoped>.notice-record-page{padding:0}</style>
