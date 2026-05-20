<template>
  <div class="workflow-template-page">
    <section class="page-head">
      <div>
        <h2>流程模板</h2>
        <p>模板是不可直接运行的流程资产，推送到机构后生成该机构自己的流程定义草稿。</p>
      </div>
    </section>

    <div class="template-layout">
      <aside class="category-panel">
        <div class="panel-head">
          <div>
            <h3>模板分类</h3>
            <p>用于沉淀可复用流程资产</p>
          </div>
          <el-button :icon="FolderAdd" type="primary" link @click="openCategoryForm()">新增</el-button>
        </div>
        <div class="category-tools">
          <el-button :icon="Refresh" text @click="loadTemplateCategories">刷新分类</el-button>
        </div>
        <div v-loading="categoryLoading" class="category-list">
          <button
            class="category-item"
            :class="{ active: !query.templateCategoryId }"
            type="button"
            @click="selectTemplateCategory('')"
          >
            <span class="category-main">
              <span class="category-name">全部模板</span>
              <span class="category-code">ALL</span>
            </span>
            <el-tag size="small" type="info">{{ total }}</el-tag>
          </button>
          <button
            v-for="item in templateCategories"
            :key="item.id"
            class="category-item"
            :class="{ active: query.templateCategoryId === item.id }"
            type="button"
            @click="selectTemplateCategory(item.id || '')"
          >
            <span class="category-main">
              <span class="category-name">{{ item.categoryName }}</span>
              <span class="category-code">{{ item.categoryCode }}</span>
            </span>
            <span class="category-actions" @click.stop>
              <el-tag :type="item.status === 1 ? 'success' : 'info'" size="small">
                {{ item.status === 1 ? '启用' : '停用' }}
              </el-tag>
              <el-button link type="primary" @click="openCategoryForm(item)">编辑</el-button>
              <el-button link type="danger" @click="deleteCategory(item)">删除</el-button>
            </span>
          </button>
          <el-empty v-if="!categoryLoading && templateCategories.length === 0" :image-size="96" description="暂无模板分类" />
        </div>
      </aside>

      <section class="template-panel">
        <div class="table-head">
          <div>
            <h3>{{ currentTemplateCategoryName }}</h3>
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
          <el-table-column prop="templateCategoryName" label="模板分类" min-width="130" show-overflow-tooltip />
          <el-table-column prop="categoryName" label="业务场景" min-width="120" show-overflow-tooltip />
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
        <el-form-item label="模板分类">
          <el-select v-model="templateForm.templateCategoryId" clearable filterable placeholder="请选择模板分类">
            <el-option v-for="item in templateCategories" :key="item.id" :label="item.categoryName" :value="item.id!" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务场景编码">
          <el-input v-model="templateForm.categoryCode" />
        </el-form-item>
        <el-form-item label="业务场景名称">
          <el-input v-model="templateForm.categoryName" />
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

    <el-dialog v-model="categoryDialogVisible" :title="categoryForm.id ? '编辑模板分类' : '新增模板分类'" width="520px">
      <el-form ref="categoryFormRef" :model="categoryForm" :rules="categoryRules" label-width="100px">
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="categoryForm.categoryName" />
        </el-form-item>
        <el-form-item label="分类编码" prop="categoryCode">
          <el-input v-model="categoryForm.categoryCode" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="categoryForm.icon" placeholder="如 CollectionTag" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categoryForm.sort" :min="0" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="categoryEnabled" active-text="启用" inactive-text="停用" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="categoryForm.remark" :rows="3" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button :loading="saving" type="primary" @click="saveCategory">保存</el-button>
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
        <el-form-item label="目标流程分类" required>
          <div class="category-target-stack">
            <el-form-item label="分类名称" prop="categoryName">
              <el-input v-model="pushForm.categoryName" placeholder="请输入目标流程分类名称" />
            </el-form-item>
            <el-form-item label="分类编码" prop="categoryCode">
              <el-input v-model="pushForm.categoryCode" placeholder="请输入目标流程分类编码" />
            </el-form-item>
          </div>
        </el-form-item>
        <el-form-item label="推送方式">
          <el-radio-group v-model="pushMode">
            <el-radio label="SELECTED">已选模板</el-radio>
            <el-radio label="CATEGORY">当前分类全部模板</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-alert
          class="import-rule"
          :title="pushMode === 'CATEGORY' ? '将推送当前左侧选中的模板分类下全部启用模板。' : `将推送 ${pushForm.templateIds.length} 个已选模板。`"
          type="info"
          :closable="false"
          show-icon
        />
        <el-alert
          class="import-rule"
          title="目标机构不存在该流程分类时会自动创建；如目标机构已存在同流程编码，推送会整批失败。"
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
import { FolderAdd, Plus, Refresh, Search, Upload } from '@element-plus/icons-vue';
import {
  defaultDesignerJson,
  workflowApi,
  type WorkflowId,
  type WorkflowTemplate,
  type WorkflowTemplateCategory,
  type WorkflowTenantOption,
} from '../../api/workflow';

const loading = ref(false);
const categoryLoading = ref(false);
const saving = ref(false);
const tenantLoading = ref(false);
const templates = ref<WorkflowTemplate[]>([]);
const total = ref(0);
const templateCategories = ref<WorkflowTemplateCategory[]>([]);
const tenantOptions = ref<WorkflowTenantOption[]>([]);

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: '',
  templateCategoryId: '' as WorkflowId | '',
});

const templateDialogVisible = ref(false);
const categoryDialogVisible = ref(false);
const pushDialogVisible = ref(false);
const pushMode = ref<'SELECTED' | 'CATEGORY'>('SELECTED');
const selectedTemplates = ref<WorkflowTemplate[]>([]);
const templateFormRef = ref<FormInstance>();
const categoryFormRef = ref<FormInstance>();
const pushFormRef = ref<FormInstance>();

const templateForm = reactive<WorkflowTemplate>({
  templateName: '',
  templateCode: '',
  templateCategoryId: '',
  categoryCode: '',
  categoryName: '',
  designerJson: defaultDesignerJson(),
  formJson: '',
  status: 'ENABLED',
});

const categoryForm = reactive<WorkflowTemplateCategory>({
  categoryName: '',
  categoryCode: '',
  icon: '',
  sort: 0,
  status: 1,
});

const pushForm = reactive({
  targetTenantIds: [] as WorkflowId[],
  categoryName: '',
  categoryCode: '',
  templateCategoryId: '' as WorkflowId | '',
  templateIds: [] as WorkflowId[],
});

const categoryEnabled = computed({
  get: () => categoryForm.status === 1,
  set: value => { categoryForm.status = value ? 1 : 0; },
});

const currentTemplateCategoryName = computed(() => {
  if (!query.templateCategoryId) {
    return '全部流程模板';
  }
  return templateCategories.value.find(item => item.id === query.templateCategoryId)?.categoryName || '流程模板';
});

const templateRules: FormRules = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  templateCode: [{ required: true, message: '请输入模板编码', trigger: 'blur' }],
  designerJson: [{ required: true, message: '请输入设计器JSON', trigger: 'blur' }],
};

const categoryRules: FormRules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  categoryCode: [{ required: true, message: '请输入分类编码', trigger: 'blur' }],
};

const pushRules: FormRules = {
  targetTenantIds: [{ required: true, message: '请选择目标机构', trigger: 'change' }],
  categoryName: [{ required: true, message: '请输入目标分类名称', trigger: 'blur' }],
  categoryCode: [{ required: true, message: '请输入目标分类编码', trigger: 'blur' }],
};

onMounted(async () => {
  await Promise.all([loadTemplateCategories(), loadTenants()]);
  await loadTemplates();
});

async function loadTemplates() {
  loading.value = true;
  try {
    const page = await workflowApi.templatesPage(query);
    templates.value = page.list;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
}

function handleTemplateSelectionChange(rows: WorkflowTemplate[]) {
  selectedTemplates.value = rows;
}

async function loadTemplateCategories() {
  categoryLoading.value = true;
  try {
    templateCategories.value = await workflowApi.templateCategoriesList();
  } finally {
    categoryLoading.value = false;
  }
}

function resetQuery() {
  Object.assign(query, { pageNum: 1, pageSize: 10, keyword: '', status: '', templateCategoryId: '' });
  loadTemplates();
}

function selectTemplateCategory(categoryId: WorkflowId | '') {
  query.templateCategoryId = categoryId;
  query.pageNum = 1;
  loadTemplates();
}

function openTemplateForm(row?: WorkflowTemplate) {
  Object.assign(templateForm, row || {
    id: undefined,
    templateName: '',
    templateCode: '',
    templateCategoryId: templateCategories.value[0]?.id || '',
    categoryCode: '',
    categoryName: '',
    designerJson: defaultDesignerJson(),
    formJson: '',
    status: 'ENABLED',
    remark: '',
  });
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

function openCategoryForm(row?: WorkflowTemplateCategory) {
  Object.assign(categoryForm, row || { id: undefined, categoryName: '', categoryCode: '', icon: '', sort: 0, status: 1, remark: '' });
  categoryDialogVisible.value = true;
}

async function saveCategory() {
  await categoryFormRef.value?.validate();
  saving.value = true;
  try {
    if (categoryForm.id) {
      await workflowApi.updateTemplateCategory(categoryForm);
    } else {
      await workflowApi.createTemplateCategory(categoryForm);
    }
    ElMessage.success('保存成功');
    categoryDialogVisible.value = false;
    await loadTemplateCategories();
  } finally {
    saving.value = false;
  }
}

async function deleteCategory(row: WorkflowTemplateCategory) {
  await ElMessageBox.confirm(`确认删除模板分类「${row.categoryName}」？`, '删除模板分类', { type: 'warning' });
  await workflowApi.deleteTemplateCategory(row.id!);
  ElMessage.success('删除成功');
  await loadTemplateCategories();
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
  const category = templateCategories.value.find(item => item.id === query.templateCategoryId);
  pushMode.value = row || selected.length > 0 ? 'SELECTED' : 'CATEGORY';
  Object.assign(pushForm, {
    targetTenantIds: [],
    categoryName: category?.categoryName || '通用流程',
    categoryCode: category?.categoryCode || 'COMMON',
    templateCategoryId: query.templateCategoryId || '',
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
  if (pushMode.value === 'CATEGORY' && !pushForm.templateCategoryId) {
    ElMessage.warning('请先在左侧选择一个模板分类');
    return;
  }
  saving.value = true;
  try {
    const result = await workflowApi.pushTemplates({
      targetTenantIds: pushForm.targetTenantIds,
      categoryName: pushForm.categoryName,
      categoryCode: pushForm.categoryCode,
      templateCategoryId: pushMode.value === 'CATEGORY' ? pushForm.templateCategoryId : undefined,
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

.category-panel,
.template-panel {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.category-panel {
  position: sticky;
  top: 12px;
  overflow: hidden;
}

.template-panel {
  min-width: 0;
  padding: 16px;
}

.panel-head,
.table-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.panel-head {
  padding: 16px 16px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.table-head {
  margin-bottom: 16px;
}

.panel-head h3,
.table-head h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 650;
  color: var(--el-text-color-primary);
}

.panel-head p,
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

.category-tools {
  padding: 8px 10px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.category-list {
  display: flex;
  flex-direction: column;
  min-height: 180px;
  max-height: calc(100vh - 260px);
  overflow-y: auto;
  padding: 8px;
}

.category-item {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 8px 10px 12px;
  color: var(--el-text-color-primary);
  text-align: left;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 6px;
  cursor: pointer;
}

.category-item:hover {
  background: var(--el-fill-color-light);
}

.category-item.active {
  background: var(--el-color-primary-light-9);
  border-color: var(--el-color-primary-light-7);
}

.category-main {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 4px;
}

.category-name,
.category-code {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-name {
  font-size: 14px;
  font-weight: 600;
}

.category-code {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.category-actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 6px;
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

.category-target-stack {
  display: flex;
  width: 100%;
  flex-direction: column;
  gap: 14px;
}

.category-target-stack :deep(.el-form-item) {
  margin-bottom: 0;
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

  .category-panel {
    position: static;
  }

  .category-list {
    max-height: none;
  }

  .table-head,
  .panel-head {
    flex-direction: column;
  }

}
</style>
