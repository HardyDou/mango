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

      <div v-loading="loading" class="workflow-launch-board">
        <el-empty v-if="!groupedDefinitions.length" description="暂无可发起流程" />
        <section v-for="group in groupedDefinitions" v-else :key="group.name" class="workflow-launch-group">
          <div class="group-title">{{ group.name }}</div>
          <div class="workflow-launch-grid">
            <button
              v-for="item in group.items"
              :key="item.id || item.definitionKey"
              class="workflow-launch-card"
              :class="{ disabled: !item.processDefinitionId }"
              type="button"
              :disabled="!item.processDefinitionId"
              @click="openStartDialog(item)"
            >
              <span class="workflow-launch-icon">
                <img v-if="isImageIcon(item.icon)" :src="item.icon" :alt="item.definitionName" />
                <el-icon v-else><component :is="workflowIconComponent(item.icon)" /></el-icon>
              </span>
              <span class="workflow-launch-content">
                <span class="workflow-launch-name">{{ item.definitionName }}</span>
                <span class="workflow-launch-subtitle">{{ item.remark || item.definitionKey }}</span>
              </span>
            </button>
          </div>
        </section>
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="selectedDefinition?.definitionName || '发起流程'" width="680px" destroy-on-close>
      <div v-if="selectedDefinition" class="definition-summary">
        <span class="definition-name">{{ selectedDefinition.definitionName }}</span>
        <span class="definition-meta">{{ selectedDefinition.definitionKey }}</span>
        <span v-if="selectedDefinition.groupName" class="definition-meta">{{ selectedDefinition.groupName }}</span>
        <span v-if="selectedDefinition.processDefinitionVersion" class="definition-meta">
          v{{ selectedDefinition.processDefinitionVersion }}
        </span>
      </div>

      <el-form ref="startFormRef" :model="formVariables" label-width="96px">
        <template v-if="startFields.length">
          <el-divider content-position="left">流程表单</el-divider>
          <RuntimeFormRenderer :fields="startFields" :model="formVariables" />
        </template>

        <el-alert
          v-else
          class="start-alert"
          title="当前流程没有可渲染的申请表单，请在流程定义中配置动态表单或自定义申请页。"
          type="info"
          :closable="false"
          show-icon
        />

        <el-alert
          v-if="unsupportedFields.length"
          class="start-alert"
          :title="`有 ${unsupportedFields.length} 个复杂组件暂未渲染，请检查表单设计器组件支持情况。`"
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

        <el-collapse v-if="showDebugFormTools" v-model="debugPanels" class="start-advanced">
          <el-collapse-item title="开发调试" name="debug">
            <el-input
              v-model="startForm.variablesJson"
              type="textarea"
              :rows="6"
              placeholder='高级变量 JSON，例如：{"days":2,"reason":"年假"}'
            />
            <pre v-if="selectedDefinition?.formJson" class="form-json-preview">{{ selectedDefinition.formJson }}</pre>
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
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, type FormInstance } from 'element-plus';
import { Bell, Box, Cloudy, Connection, DocumentChecked, ForkSpoon, Share, User } from '@element-plus/icons-vue';
import { parseDesignerJson, workflowApi, type WorkflowDefinition, type WorkflowDesignerNode, type WorkflowUserOption } from '../../api/workflow';
import { customApplyRouteOf } from '../../workflowFormConfig';
import RuntimeFormRenderer from '../../components/RuntimeFormRenderer.vue';
import { createDefaultVariables, parseRuntimeForm, type RuntimeFormField } from '../../components/runtimeForm';

const router = useRouter();
const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const tableData = ref<WorkflowDefinition[]>([]);
const selectedDefinition = ref<WorkflowDefinition | null>(null);
const startFormRef = ref<FormInstance>();
const query = ref({ pageNum: 1, pageSize: 50, keyword: '', status: 'PUBLISHED' });
const startForm = ref({
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
const showDebugFormTools = import.meta.env.DEV;
const debugPanels = ref<string[]>([]);

const groupedDefinitions = computed(() => {
  const groupMap = new Map<string, WorkflowDefinition[]>();
  tableData.value.forEach(item => {
    const groupName = item.groupName || '未分组';
    if (!groupMap.has(groupName)) {
      groupMap.set(groupName, []);
    }
    groupMap.get(groupName)!.push(item);
  });
  return Array.from(groupMap.entries()).map(([name, items]) => ({ name, items }));
});

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

function isImageIcon(value?: string) {
  return Boolean(value && (/^(https?:|data:|blob:)/.test(value) || value.startsWith('/')));
}

function workflowIconComponent(value?: string) {
  const iconMap = {
    User,
    Bell,
    ForkSpoon,
    Share,
    Box,
    Cloudy,
    Connection,
  };
  return iconMap[value as keyof typeof iconMap] || DocumentChecked;
}

function openStartDialog(row: WorkflowDefinition) {
  const customApplyRoute = customApplyRouteOf(row);
  if (customApplyRoute) {
    router.push(customApplyRoute);
    return;
  }
  selectedDefinition.value = row;
  const parsed = parseRuntimeForm(row.formJson);
  startFields.value = parsed.fields;
  unsupportedFields.value = parsed.unsupported;
  formVariables.value = createDefaultVariables(parsed.fields);
  startForm.value = {
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
  if (showDebugFormTools) {
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
  }
  if (!validateSelectedAssignees()) {
    return;
  }
  submitting.value = true;
  try {
    const instance = await workflowApi.startProcess({
      definitionId: selectedDefinition.value.id,
      businessType: String(formVariables.value.businessType || selectedDefinition.value.definitionKey || ''),
      businessKey: String(formVariables.value.businessKey || formVariables.value.code || formVariables.value.applyCode || ''),
      renderMode: 'DYNAMIC_FORM',
      variables: {
        title: formVariables.value.title || selectedDefinition.value.definitionName,
        summary: formVariables.value.summary || selectedDefinition.value.remark || selectedDefinition.value.definitionKey,
        businessType: formVariables.value.businessType || selectedDefinition.value.definitionKey,
        businessKey: formVariables.value.businessKey || formVariables.value.code || formVariables.value.applyCode,
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
  padding: 0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.search-form {
  margin-bottom: 12px;
}

.workflow-launch-board {
  min-height: 280px;
}

.workflow-launch-group + .workflow-launch-group {
  margin-top: 22px;
}

.group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: var(--el-text-color-primary);
  font-size: 15px;
  font-weight: 700;
}

.group-title::before {
  content: '';
  width: 4px;
  height: 14px;
  border-radius: 2px;
  background: var(--el-color-primary);
}

.workflow-launch-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
}

.workflow-launch-card {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  min-height: 76px;
  padding: 14px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
  cursor: pointer;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}

.workflow-launch-card:hover {
  border-color: var(--el-color-primary-light-5);
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

.workflow-launch-card.disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.workflow-launch-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  overflow: hidden;
  border-radius: 8px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.workflow-launch-icon img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.workflow-launch-icon .el-icon {
  font-size: 24px;
}

.workflow-launch-content {
  display: grid;
  min-width: 0;
  gap: 4px;
  text-align: left;
}

.workflow-launch-name {
  max-width: 100%;
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 15px;
  font-weight: 600;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workflow-launch-subtitle {
  max-width: 100%;
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.start-alert {
  margin-bottom: 16px;
}

.definition-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px 10px;
  margin-bottom: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.definition-name {
  max-width: 220px;
  overflow: hidden;
  color: var(--el-text-color-regular);
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.definition-meta {
  position: relative;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.definition-meta::before {
  content: '/';
  margin-right: 10px;
  color: var(--el-text-color-placeholder);
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
