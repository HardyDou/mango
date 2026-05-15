<template>
  <div class="workflow-form-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>业务表单</span>
          <el-tag type="info">流程发起表单</el-tag>
        </div>
      </template>

      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column prop="formCode" label="表单编码" min-width="180" />
        <el-table-column prop="definitionName" label="关联流程" min-width="180" />
        <el-table-column prop="groupName" label="流程分组" width="160" />
        <el-table-column prop="status" label="流程状态" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updatedTime" label="更新时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { workflowApi, type WorkflowDefinition } from '../../api/workflow';

const loading = ref(false);
const tableData = ref<WorkflowDefinition[]>([]);

async function loadData() {
  loading.value = true;
  try {
    const result = await workflowApi.definitionsPage({ pageNum: 1, pageSize: 100 });
    tableData.value = result.list.filter(item => Boolean(item.formCode));
  } finally {
    loading.value = false;
  }
}

onMounted(loadData);
</script>

<style scoped>
.workflow-form-page {
  padding: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
