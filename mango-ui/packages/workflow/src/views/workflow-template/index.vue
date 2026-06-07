<template>
  <div class="workflow-template-page">
    <section class="page-head">
      <div>
        <h2>流程模板</h2>
        <p>模板是不可直接运行的流程资产，推送到机构后生成该机构自己的流程定义草稿。</p>
      </div>
    </section>

    <div class="template-layout">
      <DomainSideTree
        v-model="query.domainCode"
        title="业务域"
        subtitle="按业务域沉淀流程模板"
        all-label="全部模板"
        all-code="ALL"
        :all-count="total"
        @change="selectTemplateDomain"
        @loaded="handleDomainsLoaded"
      />

      <section class="template-panel">
        <div class="table-head">
          <div>
            <h3>{{ currentTemplateDomainName }}</h3>
            <p>模板只能推送或导入为流程草稿后使用，模板本身不参与运行。</p>
          </div>
          <div class="table-actions">
            <el-button :icon="Upload" type="primary" @click="openPushDialog()">推送流程</el-button>
          </div>
        </div>

        <el-form :inline="true" :model="query" class="filter-form">
          <el-form-item label="关键词">
            <el-input v-model="query.keyword" clearable placeholder="模板名称/编码/场景" @keyup.enter="loadTemplates" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 140px">
              <el-option label="启用" value="ENABLED" />
              <el-option label="停用" value="DISABLED" />
              <el-option label="归档" value="ARCHIVED" />
            </el-select>
          </el-form-item>
          <el-form-item label="模板分类">
            <el-select
              v-model="query.templateCategoryId"
              clearable
              filterable
              placeholder="全部分类"
              style="width: 180px"
            >
              <el-option
                v-for="item in templateCategoryOptions"
                :key="item.id"
                :label="item.categoryName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button :icon="Search" type="primary" @click="loadTemplates">查询</el-button>
            <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-table
          v-loading="loading"
          :data="templates"
          stripe
          @selection-change="handleTemplateSelectionChange"
        >
          <el-table-column type="selection" width="48" />
          <el-table-column prop="templateName" label="模板名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="templateCode" label="模板编码" min-width="170" show-overflow-tooltip />
          <el-table-column label="业务域" min-width="140">
            <template #default="{ row }">
              <span>{{ domainName(row.categoryCode) }}</span>
              <span class="domain-code-cell">{{ row.categoryCode || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="模板分类" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.templateCategoryName || '未分类' }}
            </template>
          </el-table-column>
          <el-table-column label="版本" width="90">
            <template #default="{ row }">v{{ row.versionNo || 1 }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ENABLED' ? 'success' : row.status === 'ARCHIVED' ? 'warning' : 'info'">
                {{ row.statusName || row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="sourceDefinitionName" label="来源流程" min-width="150" show-overflow-tooltip />
          <el-table-column prop="updatedTime" label="更新时间" width="170" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openPushDialog(row)">推送流程</el-button>
              <el-button link type="primary" @click="openPreview(row)">预览</el-button>
              <el-button link type="danger" @click="deleteTemplate(row)">删除</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无流程模板" />
          </template>
        </el-table>

        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          class="pagination"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadTemplates"
          @current-change="loadTemplates"
        />
      </section>
    </div>

    <el-dialog v-model="templateDialogVisible" :title="templateForm.id ? '查看模板' : '新增模板'" width="720px">
      <el-form ref="templateFormRef" :model="templateForm" :rules="templateRules" label-width="110px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="templateForm.templateName" />
        </el-form-item>
        <el-form-item label="模板编码" prop="templateCode">
          <el-input v-model="templateForm.templateCode" />
        </el-form-item>
        <el-form-item label="业务域" prop="categoryCode">
          <el-tree-select
            v-model="templateForm.categoryCode"
            :data="domainOptions"
            :props="domainTreeProps"
            check-strictly
            filterable
            node-key="domainCode"
            placeholder="请选择业务域"
            @change="syncTemplateDomainName"
          />
        </el-form-item>
        <el-form-item label="模板分类" prop="templateCategoryId">
          <el-select
            v-model="templateForm.templateCategoryId"
            clearable
            filterable
            placeholder="请选择模板分类"
          >
            <el-option
              v-for="item in templateCategoryOptions"
              :key="item.id"
              :label="item.categoryName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="设计器JSON" prop="designerJson">
          <el-input v-model="templateForm.designerJson" :rows="8" type="textarea" />
        </el-form-item>
        <el-form-item label="动态表单JSON">
          <el-input v-model="templateForm.formJson" :rows="5" type="textarea" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="templateForm.status">
            <el-option label="启用" value="ENABLED" />
            <el-option label="停用" value="DISABLED" />
            <el-option label="归档" value="ARCHIVED" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="templateForm.remark" :rows="3" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="saveTemplate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pushDialogVisible" title="推送流程" width="600px">
      <el-form ref="pushFormRef" :model="pushForm" :rules="pushRules" label-position="top">
        <el-form-item label="目标机构" prop="targetTenantIds">
          <el-select
            v-model="pushForm.targetTenantIds"
            multiple
            filterable
            remote
            :remote-method="loadTenants"
            :loading="tenantLoading"
            placeholder="请选择目标机构"
          >
            <el-option
              v-for="item in tenantOptions"
              :key="item.id"
              :label="`${item.tenantName}${item.tenantCode ? `（${item.tenantCode}）` : ''}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标业务域" prop="domainCode">
          <el-tree-select
            v-model="pushForm.domainCode"
            :data="domainOptions"
            :props="domainTreeProps"
            check-strictly
            filterable
            node-key="domainCode"
            placeholder="请选择目标业务域"
          />
        </el-form-item>
        <el-form-item label="推送方式">
          <el-radio-group v-model="pushMode">
            <el-radio label="SELECTED">已选模板</el-radio>
            <el-radio label="CATEGORY">当前分类全部模板</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-alert
          class="import-rule"
          :title="pushMode === 'CATEGORY' ? pushRangeTip : `将推送 ${pushForm.templateIds.length} 个已选模板。`"
          type="info"
          :closable="false"
          show-icon
        />
        <el-alert
          class="import-rule"
          title="推送会在目标机构生成流程草稿；如目标机构已存在同流程编码，推送会整批失败。"
          type="warning"
          :closable="false"
          show-icon
        />
      </el-form>
      <template #footer>
        <el-button @click="pushDialogVisible = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="submitPush">确认推送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { Refresh, Search, Upload } from '@element-plus/icons-vue';
import DomainSideTree from '../../../../system/src/components/DomainSideTree/index.vue';
import {
  defaultDesignerJson,
  workflowApi,
  type WorkflowId,
  type WorkflowDomainOption,
  type WorkflowTemplate,
  type WorkflowTemplateCategory,
  type WorkflowTenantOption,
} from '../../api/workflow';

const loading = ref(false);
const saving = ref(false);
const tenantLoading = ref(false);
const templates = ref<WorkflowTemplate[]>([]);
const total = ref(0);
const domainOptions = ref<WorkflowDomainOption[]>([]);
const templateCategoryOptions = ref<WorkflowTemplateCategory[]>([]);
const tenantOptions = ref<WorkflowTenantOption[]>([]);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: '',
  domainCode: '',
  templateCategoryId: '' as WorkflowId | '',
});

const templateDialogVisible = ref(false);
const pushDialogVisible = ref(false);
const pushMode = ref<'SELECTED' | 'CATEGORY'>('SELECTED');
const selectedTemplates = ref<WorkflowTemplate[]>([]);
const templateFormRef = ref<FormInstance>();
const pushFormRef = ref<FormInstance>();

const templateForm = reactive<WorkflowTemplate>({
  templateName: '',
  templateCode: '',
  categoryCode: '',
  categoryName: '',
  designerJson: defaultDesignerJson(),
  formJson: '',
  status: 'ENABLED',
});

const pushForm = reactive({
  targetTenantIds: [] as WorkflowId[],
  domainCode: 'WORKFLOW',
  templateIds: [] as WorkflowId[],
});

const domainTreeProps = {
  label: 'domainName',
  value: 'domainCode',
  children: 'children',
};

const currentTemplateDomainName = computed(() => {
  if (!query.domainCode) {
    return '全部流程模板';
  }
  return domainName(query.domainCode);
});

const pushRangeTip = computed(() => {
  if (query.templateCategoryId) {
    const categoryName = templateCategoryOptions.value.find(item => item.id === query.templateCategoryId)?.categoryName || '当前模板分类';
    return `将推送「${categoryName}」下全部启用模板。`;
  }
  return `将推送「${query.domainCode ? domainName(query.domainCode) : '全部业务域'}」下全部启用模板。`;
});

const templateRules: FormRules = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  categoryCode: [{ required: true, message: '请选择业务域', trigger: 'change' }],
  designerJson: [{ required: true, message: '请输入设计器JSON', trigger: 'blur' }],
};

const pushRules: FormRules = {
  targetTenantIds: [{ required: true, message: '请选择目标机构', trigger: 'change' }],
  domainCode: [{ required: true, message: '请选择目标业务域', trigger: 'change' }],
};

onMounted(async () => {
  await Promise.all([loadDomainOptions(), loadTemplateCategories(), loadTenants()]);
  await loadTemplates();
});

async function loadDomainOptions() {
  domainOptions.value = await workflowApi.enabledDomains();
}

async function loadTemplateCategories() {
  templateCategoryOptions.value = await workflowApi.templateCategoriesList(1);
}

function handleDomainsLoaded(domains: WorkflowDomainOption[]) {
  domainOptions.value = domains;
}

function flattenDomainOptions(options: WorkflowDomainOption[]): WorkflowDomainOption[] {
  return options.flatMap(item => [item, ...flattenDomainOptions(item.children || [])]);
}

function domainName(domainCode?: string) {
  if (!domainCode) {
    return '未设置业务域';
  }
  return flattenDomainOptions(domainOptions.value).find(item => item.domainCode === domainCode)?.domainName || domainCode;
}

function syncTemplateDomainName() {
  templateForm.categoryName = domainName(templateForm.categoryCode);
}

async function loadTemplates() {
  loading.value = true;
  try {
    const page = await workflowApi.templatesPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      keyword: query.keyword,
      status: query.status,
      categoryCode: query.domainCode,
      templateCategoryId: query.templateCategoryId,
    });
    templates.value = page.list;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
}

function handleTemplateSelectionChange(rows: WorkflowTemplate[]) {
  selectedTemplates.value = rows;
}

function resetQuery() {
  Object.assign(query, { pageNum: 1, pageSize: 10, keyword: '', status: '', domainCode: '', templateCategoryId: '' });
  loadTemplates();
}

function selectTemplateDomain() {
  query.pageNum = 1;
  loadTemplates();
}

function openTemplateForm(row?: WorkflowTemplate) {
  Object.assign(templateForm, row || {
    id: undefined,
    templateName: '',
    templateCode: '',
    templateCategoryId: query.templateCategoryId || undefined,
    categoryCode: query.domainCode || domainOptions.value[0]?.domainCode || 'WORKFLOW',
    categoryName: domainName(query.domainCode || domainOptions.value[0]?.domainCode || 'WORKFLOW'),
    designerJson: defaultDesignerJson(),
    formJson: '',
    status: 'ENABLED',
    remark: '',
  });
  syncTemplateDomainName();
  templateDialogVisible.value = true;
}

async function saveTemplate() {
  await templateFormRef.value?.validate();
  saving.value = true;
  try {
    await workflowApi.createTemplate(templateForm);
    ElMessage.success('保存成功');
    templateDialogVisible.value = false;
    await loadTemplates();
  } finally {
    saving.value = false;
  }
}

function openPreview(row: WorkflowTemplate) {
  openTemplateForm(row);
}

async function deleteTemplate(row: WorkflowTemplate) {
  await ElMessageBox.confirm(`确认删除模板「${row.templateName}」？`, '删除模板', { type: 'warning' });
  await workflowApi.deleteTemplate(row.id!);
  ElMessage.success('删除成功');
  await loadTemplates();
}

async function loadTenants(keyword = '') {
  tenantLoading.value = true;
  try {
    tenantOptions.value = await workflowApi.tenants(keyword);
  } finally {
    tenantLoading.value = false;
  }
}

function openPushDialog(row?: WorkflowTemplate) {
  const selected = row ? [row] : selectedTemplates.value;
  pushMode.value = row || selected.length > 0 ? 'SELECTED' : 'CATEGORY';
  Object.assign(pushForm, {
    targetTenantIds: [],
    domainCode: row?.categoryCode || query.domainCode || domainOptions.value[0]?.domainCode || 'WORKFLOW',
    templateIds: selected.map(item => item.id!).filter(Boolean),
  });
  pushDialogVisible.value = true;
}

async function submitPush() {
  await pushFormRef.value?.validate();
  if (pushMode.value === 'SELECTED' && pushForm.templateIds.length === 0) {
    ElMessage.warning('请先勾选要推送的流程模板');
    return;
  }
  if (pushMode.value === 'CATEGORY' && !pushForm.domainCode) {
    ElMessage.warning('请先在左侧选择一个业务域');
    return;
  }
  saving.value = true;
  try {
    const result = await workflowApi.pushTemplates({
      targetTenantIds: pushForm.targetTenantIds,
      domainCode: pushForm.domainCode,
      templateCategoryId: pushMode.value === 'CATEGORY' ? query.templateCategoryId || undefined : undefined,
      templateIds: pushMode.value === 'SELECTED' ? pushForm.templateIds : undefined,
    });
    ElMessage.success(`推送成功 ${result.definitionIds.length} 个流程`);
    pushDialogVisible.value = false;
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.workflow-template-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-head,
.card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-head {
  padding: 18px 20px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.page-head h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 650;
}

.page-head p {
  margin: 8px 0 0;
  color: var(--el-text-color-regular);
}

.page-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.template-layout {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.template-panel {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.template-panel {
  min-width: 0;
  padding: 16px;
}

.table-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.table-head {
  margin-bottom: 16px;
}

.table-head h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 650;
  color: var(--el-text-color-primary);
}

.table-head p {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.table-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.filter-form {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.import-rule {
  margin-top: 12px;
}

@media (max-width: 768px) {
  .page-head,
  .card-header {
    flex-direction: column;
  }

  .page-actions {
    flex-wrap: wrap;
  }

  .template-layout {
    grid-template-columns: 1fr;
  }

  .table-head {
    flex-direction: column;
  }
}
</style>
