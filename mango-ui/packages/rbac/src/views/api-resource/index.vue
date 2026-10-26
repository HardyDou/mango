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
        title="同一接口只保留一条启用记录。固定声明优先于 Controller 扫描，被覆盖的记录显示为禁用。"
      />

      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        row-key="id"
        data-surface="api-resource.table"
        :data-state="tableState"
      >
        <el-table-column prop="moduleName" label="模块" min-width="150" show-overflow-tooltip />
        <el-table-column prop="domain" label="领域" width="130" show-overflow-tooltip />
        <el-table-column prop="source" label="登记来源" width="130">
          <template #default="{ row }">
            <el-tag :type="sourceTagType(row.source)" size="small" effect="plain">
              {{ sourceLabel(row.source) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="httpMethod" label="方法" width="90" />
        <el-table-column label="接口路径" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="api-resource-path">{{ row.pathPattern }}</span>
          </template>
        </el-table-column>
        <el-table-column label="权限标识" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.permissionCode">{{ row.permissionCode }}</span>
            <span v-else class="api-resource-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column label="访问模式" width="120">
          <template #default="{ row }">
            <el-tag :type="accessModeTagType(row.accessMode)" size="small">
              {{ accessModeLabel(row.accessMode) }}
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
        <el-table-column label="更新时间" width="180">
          <template #default="{ row }">
            {{ row.updateTime || '-' }}
          </template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <Pagination v-model:page="query.pageNum" v-model:limit="query.pageSize" :total="total" @pagination="loadData" />
      </template>
    </MangoListPanel>
  </MangoListPage>
</template>

<script setup lang="ts" name="SystemApiResource">
import { DictTag, MangoListPage, MangoListPanel, MangoSearchPanel, Pagination } from '@mango/common';
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  apiResourceApi,
  type ApiResourceAccessMode,
  type ApiResourceFilterOptionsVO,
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

.api-resource-empty {
  color: var(--el-text-color-placeholder);
}
</style>
