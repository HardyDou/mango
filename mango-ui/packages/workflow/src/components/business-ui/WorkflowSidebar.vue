<template>
  <div class="workflow-sidebar">
    <div class="workflow-sidebar__panel">
      <div class="workflow-sidebar__panel-head">
        <WorkflowInstanceSummary :summary="summary" />
        <div class="workflow-sidebar__actions">
          <el-tooltip v-if="showGraphButton" content="查看流程图" placement="top">
            <el-button :icon="Share" circle plain :disabled="!node" @click="graphVisible = true" />
          </el-tooltip>
          <el-tooltip v-if="showHistoryButton" content="查看历史申请" placement="top">
            <el-button
              :icon="Clock"
              circle
              plain
              :disabled="!businessType || !businessKey"
              @click="historyVisible = true"
            />
          </el-tooltip>
        </div>
      </div>

      <div v-if="mode !== 'HIDDEN'" class="workflow-sidebar__body">
        <WorkflowInstanceProgress
          v-if="mode !== 'APPROVAL_RECORDS' && mode !== 'CUSTOM' && node"
          :node="node"
          :current-node-key="currentNodeKey"
          :visited-node-keys="visitedNodeKeys"
          :status="status"
          :records="records"
        />
        <WorkflowApprovalTimeline
          v-else-if="mode !== 'CUSTOM'"
          :records="records"
          :show-variables="false"
          empty-text="暂无审批记录"
        />
        <slot v-else />
        <slot name="extra" />
      </div>
    </div>

    <WorkflowDefinitionGraphDialog
      v-if="showGraphButton && node"
      v-model="graphVisible"
      :node="node"
      :current-node-key="currentNodeKey"
      :visited-node-keys="visitedNodeKeys"
      :status="status"
    />

    <WorkflowInstanceHistoryDialog
      v-if="showHistoryButton"
      v-model="historyVisible"
      :business-type="businessType"
      :business-key="businessKey"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { Clock, Share } from '@element-plus/icons-vue';
import WorkflowApprovalTimeline from '../trace/WorkflowApprovalTimeline.vue';
import WorkflowDefinitionGraphDialog from './WorkflowDefinitionGraphDialog.vue';
import WorkflowInstanceHistoryDialog from './WorkflowInstanceHistoryDialog.vue';
import WorkflowInstanceProgress from './WorkflowInstanceProgress.vue';
import WorkflowInstanceSummary from './WorkflowInstanceSummary.vue';
import type { WorkflowDefinitionGraphProps, WorkflowSidebarRecordMode } from './types';
import type { WorkflowInstanceSummaryData } from './types';
import type { WorkflowTaskRecord } from '../../api/workflow';

defineOptions({ name: 'WorkflowSidebar' });

withDefaults(defineProps<{
  summary?: WorkflowInstanceSummaryData;
  node?: WorkflowDefinitionGraphProps['node'];
  currentNodeKey?: string;
  visitedNodeKeys?: string[];
  status?: string;
  records?: WorkflowTaskRecord[];
  businessType?: string;
  businessKey?: string;
  mode?: WorkflowSidebarRecordMode;
  showGraphButton?: boolean;
  showHistoryButton?: boolean;
}>(), {
  summary: () => ({}),
  node: null,
  currentNodeKey: '',
  visitedNodeKeys: () => [],
  status: '',
  records: () => [],
  businessType: '',
  businessKey: '',
  mode: 'PROGRESS',
  showGraphButton: true,
  showHistoryButton: true,
});

const graphVisible = ref(false);
const historyVisible = ref(false);
</script>

<style scoped>
.workflow-sidebar {
  min-width: 0;
}

.workflow-sidebar__panel {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.workflow-sidebar__panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.workflow-sidebar__actions {
  display: inline-flex;
  gap: 8px;
  flex: none;
}

.workflow-sidebar__body {
  padding: 14px;
}
</style>
