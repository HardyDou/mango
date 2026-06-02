<template>
  <div class="workflow-service-config">
    <el-form-item v-if="node.executionType === 'SPRING_BEAN'" label="Bean 名称">
      <el-input :model-value="valueOf('beanName')" placeholder="workflowAuditService" @input="value => update('beanName', value)" />
    </el-form-item>
    <el-form-item v-if="node.executionType === 'SPRING_BEAN'" label="方法名称">
      <el-input :model-value="valueOf('methodName')" placeholder="execute" @input="value => update('methodName', value)" />
    </el-form-item>

    <el-form-item v-if="node.executionType === 'HTTP_URL'" label="请求地址">
      <el-input :model-value="valueOf('url')" placeholder="https://example.com/callback" @input="value => update('url', value)" />
    </el-form-item>
    <el-form-item v-if="node.executionType === 'HTTP_URL'" label="请求方法">
      <el-select :model-value="valueOf('method', 'POST')" @change="value => update('method', value)">
        <el-option label="POST" value="POST" />
        <el-option label="GET" value="GET" />
        <el-option label="PUT" value="PUT" />
        <el-option label="DELETE" value="DELETE" />
      </el-select>
    </el-form-item>
    <el-form-item v-if="node.executionType === 'HTTP_URL'" label="超时时间">
      <el-input-number :model-value="Number(valueOf('timeoutMillis', 5000))" :min="1000" :step="1000" @change="value => update('timeoutMillis', value || 5000)" />
    </el-form-item>

    <el-form-item v-if="node.executionType === 'REMOTE_SERVICE'" label="服务名称">
      <el-input :model-value="valueOf('serviceName')" placeholder="contract-service" @input="value => update('serviceName', value)" />
    </el-form-item>
    <el-form-item v-if="node.executionType === 'REMOTE_SERVICE'" label="操作编码">
      <el-input :model-value="valueOf('operation')" placeholder="submitBankMaterials" @input="value => update('operation', value)" />
    </el-form-item>

    <el-form-item v-if="node.executionType === 'EVENT_PUBLISH'" label="事件名称">
      <el-input :model-value="valueOf('eventName')" placeholder="workflow.cc" @input="value => update('eventName', value)" />
    </el-form-item>

    <el-form-item v-if="valueOf('businessStage')" label="业务阶段">
      <el-input :model-value="valueOf('businessStage')" disabled />
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import type { WorkflowDesignerNode } from '../../../../api/workflow';

const props = defineProps<{
  node: WorkflowDesignerNode;
}>();

const emit = defineEmits<{
  update: [key: string, value: unknown];
}>();

function valueOf(key: string, fallback: unknown = '') {
  return props.node.properties?.[key] ?? fallback;
}

function update(key: string, value: unknown) {
  emit('update', key, value);
}
</script>
