<template>
  <div class="workflow-start-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>发起流程</span>
          <el-tag type="success">已发布流程</el-tag>
        </div>
      </template>

      <el-form :inline="true" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="搜索流程名称/编码" clearable @keyup.enter="loadData" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="tableData" stripe>
        <el-table-column prop="definitionName" label="流程名称" min-width="180" />
        <el-table-column prop="definitionKey" label="流程编码" min-width="180" />
        <el-table-column prop="groupName" label="流程分组" width="160" />
        <el-table-column prop="formCode" label="表单编码" min-width="160" show-overflow-tooltip />
        <el-table-column prop="lastDeployTime" label="发布时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :disabled="!row.processDefinitionId" @click="openStartDialog(row)">
              发起
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" title="发起流程" width="680px" destroy-on-close>
      <el-descriptions v-if="selectedDefinition" :column="2" border class="definition-summary">
        <el-descriptions-item label="流程名称">{{ selectedDefinition.definitionName }}</el-descriptions-item>
        <el-descriptions-item label="流程编码">{{ selectedDefinition.definitionKey }}</el-descriptions-item>
        <el-descriptions-item label="流程分组">{{ selectedDefinition.groupName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="引擎版本">{{ selectedDefinition.processDefinitionVersion || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-form ref="startFormRef" :model="formVariables" label-width="96px">
        <el-form-item label="业务主键">
          <el-input v-model="startForm.businessKey" placeholder="可为空；为空时后端自动生成" clearable />
        </el-form-item>

        <template v-if="startFields.length">
          <el-divider content-position="left">流程表单</el-divider>
          <el-form-item v-for="field in startFields" :key="field.key" :label="field.label" :prop="field.key" :rules="field.rules">
            <RuntimeFormRenderer :fields="[field]" :model="formVariables" label-width="0" />
          </el-form-item>
        </template>

        <el-alert
          v-else
          class="start-alert"
          title="当前流程没有可渲染的基础表单字段，可以通过高级变量 JSON 传入流程变量。"
          type="info"
          :closable="false"
          show-icon
        />

        <el-alert
          v-if="unsupportedFields.length"
          class="start-alert"
          :title="`有 ${unsupportedFields.length} 个复杂组件暂未渲染，可通过高级变量 JSON 补充变量。`"
          type="warning"
          :closable="false"
          show-icon
        />

        <template v-if="initiatorSelectNodes.length">
          <el-divider content-position="left">发起人自选审批人</el-divider>
          <el-form-item v-for="node in initiatorSelectNodes" :key="node.id" :label="node.nodeName">
            <el-select
              v-model="selectedAssignees[node.id]"
              class="selected-assignee-select"
              :multiple="node.multiple"
              filterable
              collapse-tags
              collapse-tags-tooltip
              :loading="userLoading"
              :placeholder="node.multiple ? '请选择一个或多个审批人' : '请选择审批人'"
              @focus="ensureUsersLoaded"
              @visible-change="visible => visible && ensureUsersLoaded()"
            >
              <el-option v-for="user in userOptions" :key="user.value" :label="user.label" :value="user.value" />
            </el-select>
          </el-form-item>
        </template>

        <el-collapse class="start-advanced">
          <el-collapse-item title="高级变量 JSON" name="variables">
            <el-input
              v-model="startForm.variablesJson"
              type="textarea"
              :rows="6"
              placeholder='例如：{"days":2,"reason":"年假"}'
            />
          </el-collapse-item>
          <el-collapse-item v-if="selectedDefinition?.formJson" title="表单配置预览" name="formJson">
            <pre class="form-json-preview">{{ selectedDefinition.formJson }}</pre>
          </el-collapse-item>
        </el-collapse>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitStart">确认发起</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage, type FormInstance } from 'element-plus';
import { parseDesignerJson, workflowApi, type WorkflowDefinition, type WorkflowDesignerNode, type WorkflowUserOption } from '../../api/workflow';
import RuntimeFormRenderer from '../../components/RuntimeFormRenderer.vue';
import { createDefaultVariables, parseRuntimeForm, type RuntimeFormField } from '../../components/runtimeForm';

const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const tableData = ref<WorkflowDefinition[]>([]);
const selectedDefinition = ref<WorkflowDefinition | null>(null);
const startFormRef = ref<FormInstance>();
const query = ref({ pageNum: 1, pageSize: 50, keyword: '', status: 'PUBLISHED' });
const startForm = ref({
  businessKey: '',
  variablesJson: '{}',
});
const formVariables = ref<Record<string, any>>({});
const startFields = ref<RuntimeFormField[]>([]);
const unsupportedFields = ref<Array<{ label: string; type: string }>>([]);
const initiatorSelectNodes = ref<Array<{ id: string; nodeName: string; multiple: boolean }>>([]);
const selectedAssignees = ref<Record<string, string | string[]>>({});
const userOptions = ref<WorkflowUserOption[]>([]);
const userLoading = ref(false);
const usersLoaded = ref(false);

async function loadData() {
  loading.value = true;
  try {
    const result = await workflowApi.definitionsPage(query.value);
    tableData.value = result.list;
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  query.value.keyword = '';
  loadData();
}

function openStartDialog(row: WorkflowDefinition) {
  selectedDefinition.value = row;
  const parsed = parseRuntimeForm(row.formJson);
  startFields.value = parsed.fields;
  unsupportedFields.value = parsed.unsupported;
  formVariables.value = createDefaultVariables(parsed.fields);
  startForm.value = {
    businessKey: '',
    variablesJson: '{}',
  };
  initiatorSelectNodes.value = collectInitiatorSelectNodes(row.designerJson);
  selectedAssignees.value = Object.fromEntries(
    initiatorSelectNodes.value.map(node => [node.id, node.multiple ? [] : '']),
  );
  if (initiatorSelectNodes.value.length) {
    void ensureUsersLoaded();
  }
  dialogVisible.value = true;
}

async function submitStart() {
  if (!selectedDefinition.value?.id) return;
  const valid = await startFormRef.value?.validate().catch(() => false);
  if (valid === false) {
    return;
  }
  let advancedVariables: Record<string, any> = {};
  try {
    advancedVariables = startForm.value.variablesJson.trim()
      ? JSON.parse(startForm.value.variablesJson)
      : {};
  } catch {
    ElMessage.error('表单变量必须是合法 JSON');
    return;
  }
  if (!advancedVariables || Array.isArray(advancedVariables) || typeof advancedVariables !== 'object') {
    ElMessage.error('表单变量 JSON 必须是对象');
    return;
  }
  if (!validateSelectedAssignees()) {
    return;
  }
  submitting.value = true;
  try {
    const instance = await workflowApi.startProcess({
      definitionId: selectedDefinition.value.id,
      businessKey: startForm.value.businessKey || undefined,
      variables: {
        ...formVariables.value,
        ...advancedVariables,
      },
      selectedAssignees: normalizeSelectedAssignees(),
    });
    ElMessage.success(`流程已发起：${instance.processInstanceId}`);
    dialogVisible.value = false;
  } finally {
    submitting.value = false;
  }
}

function collectInitiatorSelectNodes(designerJson?: string) {
  const root = parseDesignerJson(designerJson);
  const nodes: Array<{ id: string; nodeName: string; multiple: boolean }> = [];
  const visit = (node?: WorkflowDesignerNode | null) => {
    if (!node) return;
    const config = node.properties?.approvalConfig || {};
    if (config.assigneeType === 'INITIATOR_SELECT') {
      nodes.push({
        id: node.id,
        nodeName: node.nodeName || node.id,
        multiple: Boolean(config.initiatorSelectMultiple),
      });
    }
    visit(node.childNode);
    node.conditionNodes?.forEach(visit);
  };
  visit(root);
  return nodes;
}

function normalizeSelectedAssignees() {
  const result: Record<string, string[]> = {};
  for (const node of initiatorSelectNodes.value) {
    const rawValue = selectedAssignees.value[node.id];
    const values = Array.isArray(rawValue)
      ? rawValue.map(item => String(item).trim()).filter(Boolean)
      : String(rawValue || '').split(',').map(item => item.trim()).filter(Boolean);
    if (values.length) {
      result[node.id] = node.multiple ? values : values.slice(0, 1);
    }
  }
  return result;
}

function validateSelectedAssignees() {
  for (const node of initiatorSelectNodes.value) {
    const rawValue = selectedAssignees.value[node.id];
    const selected = Array.isArray(rawValue)
      ? rawValue.filter(Boolean)
      : (rawValue ? [rawValue] : []);
    if (selected.length === 0) {
      ElMessage.error(`请选择「${node.nodeName}」审批人`);
      return false;
    }
  }
  return true;
}

async function ensureUsersLoaded() {
  if (usersLoaded.value) {
    return;
  }
  userLoading.value = true;
  try {
    userOptions.value = await workflowApi.users();
    usersLoaded.value = true;
  } finally {
    userLoading.value = false;
  }
}

onMounted(loadData);
</script>

<style scoped>
.workflow-start-page {
  padding: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.search-form {
  margin-bottom: 16px;
}

.definition-summary,
.start-alert {
  margin-bottom: 16px;
}

.start-advanced {
  margin-top: 12px;
}

.selected-assignee-select {
  width: 100%;
}

.form-json-preview {
  width: 100%;
  max-height: 180px;
  padding: 12px;
  overflow: auto;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  color: #475569;
  white-space: pre-wrap;
}
</style>
