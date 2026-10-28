<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <MangoListPage class="api-resource-page" data-page="system.api-resource">
    <template #search>
      <MangoSearchPanel :model="query" :columns="3" @search="handleSearch" @reset="handleReset">
        <el-form-item label="模块">
          <el-select
            v-model="query.moduleName"
            data-field="api-resource.module"
            placeholder="请选择模块"
            clearable
            filterable
          >
            <el-option v-for="module in filterOptions.modules" :key="module" :label="module" :value="module" />
          </el-select>
        </el-form-item>
        <el-form-item label="领域">
          <el-select
            v-model="query.domain"
            data-field="api-resource.domain"
            placeholder="请选择领域"
            clearable
            filterable
          >
            <el-option v-for="domain in filterOptions.domains" :key="domain" :label="domain" :value="domain" />
          </el-select>
        </el-form-item>
        <el-form-item label="登记来源">
          <el-select v-model="query.source" data-field="api-resource.source" placeholder="请选择来源" clearable>
            <el-option v-for="item in sourceOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="访问模式">
          <el-select v-model="query.accessMode" data-field="api-resource.access-mode" placeholder="请选择" clearable>
            <el-option v-for="item in accessModeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" data-field="api-resource.status" placeholder="请选择状态" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            data-field="api-resource.keyword"
            placeholder="路径、权限标识或处理类"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
      </MangoSearchPanel>
    </template>

    <MangoListPanel>
      <el-alert
        class="api-resource-tip"
        type="info"
        :closable="false"
        show-icon
        title="人工接管优先级最高：保存后该接口只按人工配置判定，Controller 扫描与固定声明不再覆盖；解除接管后代码声明重新生效。"
      />

      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        row-key="id"
        data-surface="api-resource.table"
        :data-state="tableState"
      >
        <el-table-column label="接口" min-width="300" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="api-resource-cell">
              <el-tag size="small" effect="plain">{{ row.httpMethod }}</el-tag>
              <span class="api-resource-path">{{ row.pathPattern }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="接口说明" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.description">{{ row.description }}</span>
            <span v-else class="api-resource-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="moduleName" label="所属模块" min-width="150" show-overflow-tooltip />
        <el-table-column prop="domain" label="路径领域" width="130" show-overflow-tooltip />
        <el-table-column label="权限标识 / 资源编码" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <div v-if="row.permissionCode || row.resourceCode" class="api-resource-cell api-resource-cell--stack">
              <span v-if="row.permissionCode">{{ row.permissionCode }}</span>
              <span v-if="row.resourceCode && row.resourceCode !== row.permissionCode" class="api-resource-cell__muted">
                资源编码：{{ row.resourceCode }}
              </span>
              <span v-else-if="row.permissionCode" class="api-resource-cell__muted">资源编码同权限标识</span>
            </div>
            <span v-else class="api-resource-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column label="访问模式" width="170">
          <template #default="{ row }">
            <div class="api-resource-cell">
              <el-tag :type="accessModeTagType(row.accessMode)" size="small">
                {{ accessModeLabel(row.accessMode) }}
              </el-tag>
              <el-tag v-if="row.manualOverride" type="warning" size="small" effect="plain">人工接管</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="登记来源" width="130">
          <template #default="{ row }">
            <el-tag :type="sourceTagType(row.source)" size="small" effect="plain">
              {{ sourceLabel(row.source) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理入口" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.handlerClass">{{ handlerLabel(row) }}</span>
            <span v-else class="api-resource-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <DictTag dict-code="sys_normal_disable" :value="row.status" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="创建 / 更新时间" width="190">
          <template #default="{ row }">
            <div class="api-resource-cell api-resource-cell--stack api-resource-cell__muted">
              <span>创建：{{ row.createTime || '-' }}</span>
              <span>更新：{{ row.updateTime || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="资源 ID" width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              data-action="api-resource.edit"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.manualOverride && row.status === 1"
              link
              type="danger"
              size="small"
              data-action="api-resource.release"
              @click="handleRelease(row)"
            >
              解除接管
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <Pagination v-model:page="query.pageNum" v-model:limit="query.pageSize" :total="total" @pagination="loadData" />
      </template>
    </MangoListPanel>

    <el-dialog
      v-model="dialogVisible"
      title="编辑接口权限"
      width="560px"
      data-surface="api-resource.form"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="接口">
          <span class="api-resource-path">{{ form.httpMethod }} {{ form.pathPattern }}</span>
        </el-form-item>
        <el-form-item label="所属模块">
          <span>{{ form.moduleName }}</span>
        </el-form-item>
        <el-form-item label="登记来源">
          <el-tag :type="sourceTagType(editingSource)" size="small" effect="plain">
            {{ sourceLabel(editingSource) }}
          </el-tag>
        </el-form-item>
        <el-form-item label="接管状态">
          <el-tag :type="form.manualOverride ? 'warning' : 'info'" size="small" effect="plain">
            {{ form.manualOverride ? '人工接管中' : '跟随代码声明' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="访问模式" prop="accessMode">
          <el-radio-group v-model="form.accessMode" data-field="api-resource.access-mode">
            <el-radio v-for="item in accessModeOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          v-if="form.accessMode === 'PERMISSION'"
          label="权限标识"
          prop="permissionCode"
        >
          <el-input
            v-model="form.permissionCode"
            data-field="api-resource.permission-code"
            placeholder="例如 system:user:list"
            clearable
          />
        </el-form-item>
        <el-form-item label="接管说明">
          <el-input
            v-model="form.description"
            data-field="api-resource.description"
            type="textarea"
            :rows="2"
            maxlength="200"
            show-word-limit
            placeholder="说明为什么需要人工接管，例如上游系统回调需要匿名访问"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" data-action="api-resource.submit" @click="handleSubmit">
          保存并接管
        </el-button>
      </template>
    </el-dialog>
  </MangoListPage>
</template>

<script setup lang="ts" name="SystemApiResource">
import { DictTag, MangoListPage, MangoListPanel, MangoSearchPanel, Pagination } from '@mango/common';
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import {
  apiResourceApi,
  type ApiResourceAccessMode,
  type ApiResourceFilterOptionsVO,
  type ApiResourceManualCommand,
  type ApiResourceQuery,
  type ApiResourceSource,
  type ApiResourceVO,
} from '../../api/apiResource';

const sourceOptions: Array<{ label: string; value: ApiResourceSource }> = [
  { label: '固定登记', value: 'FIXED_DECLARATION' },
  { label: 'Controller 扫描', value: 'CONTROLLER_SCAN' },
  { label: '人工登记', value: 'MANUAL' },
];

const accessModeOptions: Array<{ label: string; value: ApiResourceAccessMode }> = [
  { label: '公开', value: 'PUBLIC' },
  { label: '登录', value: 'LOGIN' },
  { label: '权限', value: 'PERMISSION' },
  { label: '内部', value: 'INTERNAL' },
];

const loading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);
const editingSource = ref<ApiResourceSource>('CONTROLLER_SCAN');
const formRef = ref<FormInstance>();
const form = reactive<ApiResourceManualCommand>({
  moduleName: '',
  httpMethod: '',
  pathPattern: '',
  accessMode: 'LOGIN',
  permissionCode: '',
  description: '',
  manualOverride: false,
});

const rules: FormRules = {
  accessMode: [{ required: true, message: '请选择访问模式', trigger: 'change' }],
  permissionCode: [
    {
      validator: (_rule, value: string, callback) => {
        if (form.accessMode === 'PERMISSION' && !String(value || '').trim()) {
          callback(new Error('权限模式必须填写权限标识'));
          return;
        }
        callback();
      },
      trigger: 'blur',
    },
  ],
};
const tableData = ref<ApiResourceVO[]>([]);
const total = ref(0);
const filterOptions = reactive<ApiResourceFilterOptionsVO>({ modules: [], domains: [] });

const query = reactive<ApiResourceQuery>({
  pageNum: 1,
  pageSize: 10,
  moduleName: undefined,
  domain: undefined,
  source: undefined,
  accessMode: undefined,
  status: undefined,
  keyword: '',
});

const tableState = computed(() => {
  if (loading.value) {
    return 'loading';
  }
  return tableData.value.length === 0 ? 'empty' : 'ready';
});

async function loadData() {
  loading.value = true;
  try {
    const data = await apiResourceApi.page(query);
    tableData.value = data.list;
    total.value = data.total;
  } catch (error) {
    tableData.value = [];
    total.value = 0;
    ElMessage.error(error instanceof Error ? error.message : '权限标识查询失败');
  } finally {
    loading.value = false;
  }
}

async function loadFilterOptions() {
  try {
    const data = await apiResourceApi.filterOptions();
    filterOptions.modules = data.modules || [];
    filterOptions.domains = data.domains || [];
  } catch {
    filterOptions.modules = [];
    filterOptions.domains = [];
  }
}

function handleSearch() {
  query.pageNum = 1;
  loadData();
}

function handleReset() {
  query.pageNum = 1;
  query.moduleName = undefined;
  query.domain = undefined;
  query.source = undefined;
  query.accessMode = undefined;
  query.status = undefined;
  query.keyword = '';
  loadData();
}

function sourceLabel(source: ApiResourceSource) {
  return sourceOptions.find((item) => item.value === source)?.label || source;
}

function sourceTagType(source: ApiResourceSource) {
  if (source === 'FIXED_DECLARATION') {
    return 'primary';
  }
  return source === 'CONTROLLER_SCAN' ? 'info' : 'warning';
}

function accessModeLabel(accessMode: ApiResourceAccessMode) {
  return accessModeOptions.find((item) => item.value === accessMode)?.label || accessMode;
}

function accessModeTagType(accessMode: ApiResourceAccessMode) {
  if (accessMode === 'PUBLIC') {
    return 'warning';
  }
  if (accessMode === 'PERMISSION') {
    return 'success';
  }
  return accessMode === 'INTERNAL' ? 'danger' : 'info';
}

function handlerLabel(row: ApiResourceVO) {
  const handlerClass = (row.handlerClass || '').split('.').pop() || row.handlerClass || '';
  return row.handlerMethod ? `${handlerClass}#${row.handlerMethod}` : handlerClass;
}

function handleEdit(row: ApiResourceVO) {
  editingSource.value = row.source;
  form.moduleName = row.moduleName;
  form.httpMethod = row.httpMethod;
  form.pathPattern = row.pathPattern;
  form.accessMode = row.accessMode;
  form.permissionCode = row.permissionCode || row.originPermissionCode || '';
  form.description = row.description || '';
  form.manualOverride = Boolean(row.manualOverride);
  dialogVisible.value = true;
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  submitLoading.value = true;
  try {
    await apiResourceApi.saveManual({ ...form });
    ElMessage.success('已保存并接管该接口权限');
    dialogVisible.value = false;
    await loadData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存接口权限失败');
  } finally {
    submitLoading.value = false;
  }
}

async function handleRelease(row: ApiResourceVO) {
  try {
    await ElMessageBox.confirm(
      `确认解除 ${row.httpMethod} ${row.pathPattern} 的人工接管？解除后代码声明重新生效。`,
      '解除人工接管',
      { type: 'warning' },
    );
  } catch {
    return;
  }
  try {
    await apiResourceApi.releaseManual({
      moduleName: row.moduleName,
      httpMethod: row.httpMethod,
      pathPattern: row.pathPattern,
    });
    ElMessage.success('已解除人工接管');
    await loadData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '解除人工接管失败');
  }
}

onMounted(() => {
  loadFilterOptions();
  loadData();
});
</script>

<style scoped lang="scss">
.api-resource-tip {
  margin-bottom: 12px;
}

.api-resource-path {
  font-family: var(--el-font-family-mono, monospace);
}

.api-resource-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.api-resource-cell--stack {
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.api-resource-cell__muted {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.api-resource-empty {
  color: var(--el-text-color-placeholder);
}
</style>
