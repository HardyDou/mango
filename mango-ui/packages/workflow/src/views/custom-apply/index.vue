<template>
  <div class="workflow-custom-apply-page">
    <el-card v-loading="loading" class="custom-apply-card">
      <template #header>
        <div class="card-header">
          <div>
            <div class="page-title">{{ pageTitle }}</div>
            <div class="page-subtitle">{{ definition?.definitionName || applyPageKey || '-' }}</div>
          </div>
          <el-button @click="backToStart">返回</el-button>
        </div>
      </template>

      <el-empty v-if="!applyPageKey" description="缺少自定义申请页 Key" />
      <el-alert
        v-else-if="!registration"
        type="warning"
        show-icon
        :closable="false"
        :title="`自定义申请页未注册：${applyPageKey}`"
        description="请确认业务模块已在应用启动时注册 applyPageKey 对应的申请组件。"
      />
      <component
        :is="registration.component"
        v-else
        :context="businessApplyContext"
        @submitted="handleSubmitted"
        @cancel="backToStart"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { workflowApi, type WorkflowDefinition } from '../../api/workflow';
import {
  resolveBusinessApplyRegistration,
  type BusinessApplyContext,
} from '../../components/businessApply';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const definition = ref<WorkflowDefinition | null>(null);

const applyPageKey = computed(() => String(route.query.applyPageKey || '').trim());
const definitionId = computed(() => String(route.query.definitionId || '').trim());
const definitionKey = computed(() => String(route.query.definitionKey || '').trim());
const registration = computed(() => resolveBusinessApplyRegistration(applyPageKey.value));
const pageTitle = computed(() => registration.value?.title || definition.value?.definitionName || '流程申请');
const businessApplyContext = computed<BusinessApplyContext>(() => ({
  definitionId: definitionId.value,
  definitionKey: definitionKey.value,
  applyPageKey: applyPageKey.value,
  definition: definition.value,
  query: { ...route.query },
}));

async function loadDefinition() {
  if (!definitionId.value) {
    definition.value = null;
    return;
  }
  loading.value = true;
  try {
    definition.value = await workflowApi.definitionDetail(definitionId.value);
  } finally {
    loading.value = false;
  }
}

function backToStart() {
  router.push('/workflow/start-process');
}

function handleSubmitted() {
  ElMessage.success('申请已提交审批');
  backToStart();
}

onMounted(loadDefinition);
</script>

<style scoped>
.workflow-custom-apply-page {
  padding: 0;
}

.custom-apply-card {
  min-height: calc(100vh - 148px);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-title {
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 700;
}

.page-subtitle {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
