<template>
  <div class="workflow-event-notify-config">
    <el-form class="drawer-form compact-node-form" label-position="top">
      <el-form-item label="启用通知">
        <el-switch :model-value="config.enabled" @change="value => $emit('update', { enabled: Boolean(value) })" />
      </el-form-item>
      <template v-if="config.enabled">
        <el-form-item label="通知类型">
          <el-radio-group :model-value="config.type || 'HTTP'" @change="value => $emit('update', { type: value as WorkflowEventNotifyConfig['type'] })">
            <el-radio-button label="HTTP">HTTP 回调</el-radio-button>
            <el-radio-button label="EVENT">事件发布</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="(config.type || 'HTTP') === 'HTTP'" label="回调地址">
          <el-input :model-value="config.url" placeholder="https://example.com/workflow/callback" @input="value => $emit('update', { url: String(value || '') })" />
        </el-form-item>
        <el-form-item v-if="(config.type || 'HTTP') === 'HTTP'" label="请求方法">
          <el-select :model-value="config.method || 'POST'" placeholder="请求方法" @change="value => $emit('update', { method: value as WorkflowEventNotifyConfig['method'] })">
            <el-option label="POST" value="POST" />
            <el-option label="PUT" value="PUT" />
            <el-option label="GET" value="GET" />
            <el-option label="DELETE" value="DELETE" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="(config.type || 'HTTP') === 'EVENT'" label="事件名称">
          <el-input :model-value="config.eventName" placeholder="workflow.task.completed" @input="value => $emit('update', { eventName: String(value || '') })" />
        </el-form-item>
        <el-form-item label="超时时间(ms)">
          <el-input-number :model-value="config.timeoutMillis || 5000" :min="1000" :step="1000" controls-position="right" @change="value => $emit('update', { timeoutMillis: Number(value || 5000) })" />
        </el-form-item>
        <el-form-item label="载荷模板">
          <el-input
            :model-value="config.payloadTemplate"
            :rows="4"
            placeholder='{"definitionKey":"${definitionKey}","nodeId":"${nodeId}"}'
            type="textarea"
            @input="value => $emit('update', { payloadTemplate: String(value || '') })"
          />
        </el-form-item>
      </template>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import type { WorkflowEventNotifyConfig } from '../../../../api/workflow';

defineProps<{
  config: WorkflowEventNotifyConfig;
}>();

defineEmits<{
  update: [patch: Partial<WorkflowEventNotifyConfig>];
}>();
</script>
