<template>
  <div class="config-container">
    <template v-if="viewMode === 'list'">
      <div class="config-layout">
        <DomainSideTree
          v-model="selectedDomainCode"
          title="业务域"
          subtitle="按业务域维护系统参数"
          all-label="全部参数"
          all-code="ALL"
          :all-count="filteredConfigs.length"
          :options="domainTree"
          @change="handleDomainChange"
          @loaded="handleDomainLoaded"
        />

        <section class="config-main">
          <el-card
            class="config-search"
            shadow="never"
          >
            <el-form
              :inline="true"
              :model="query"
              class="config-search__form"
            >
              <el-form-item label="关键词">
                <el-input
                  v-model="query.keyword"
                  placeholder="搜索参数名称/参数键/介绍"
                  clearable
                  @keyup.enter="handleSearch"
                  @clear="handleSearch"
                />
              </el-form-item>
              <el-form-item label="参数分类">
                <el-select
                  v-model="query.configGroup"
                  placeholder="不限"
                  clearable
                  @change="handleSearch"
                >
                  <el-option
                    v-for="item in configTypeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="String(item.value)"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="展示类型">
                <el-select
                  v-model="query.valueType"
                  placeholder="不限"
                  clearable
                  @change="handleSearch"
                >
                  <el-option
                    v-for="item in valueTypeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="状态">
                <el-select
                  v-model="query.status"
                  placeholder="不限"
                  clearable
                  @change="handleSearch"
                >
                  <el-option
                    v-for="item in statusOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="Number(item.value)"
                  />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button
                  type="primary"
                  @click="handleSearch"
                >
                  查询
                </el-button>
                <el-button @click="handleReset">
                  重置
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>

          <el-card
            class="config-table-card"
            shadow="never"
          >
            <div class="config-toolbar">
              <div class="config-toolbar__left">
                <el-button
                  type="primary"
                  plain
                  @click="handleAdd"
                >
                  新增参数
                </el-button>
                <el-button
                  plain
                  @click="openOperationPanel"
                >
                  操作面板
                </el-button>
              </div>
              <div class="config-toolbar__right">
                <el-button
                  plain
                  :loading="listLoading"
                  @click="loadConfigList"
                >
                  刷新
                </el-button>
              </div>
            </div>

            <el-table
              v-loading="listLoading"
              :data="pagedConfigs"
              stripe
              row-key="id"
            >
              <el-table-column
                prop="configName"
                label="参数定义"
                min-width="190"
                show-overflow-tooltip
              >
                <template #default="{ row }">
                  <div class="config-name-cell">
                    <span>{{ row.configName || row.configKey }}</span>
                    <small>{{ row.configKey }}</small>
                  </div>
                </template>
              </el-table-column>
              <el-table-column
                prop="domainCode"
                label="业务域"
                width="160"
                show-overflow-tooltip
              >
                <template #default="{ row }">
                  {{ domainDisplayName(row.domainCode) }}
                </template>
              </el-table-column>
              <el-table-column
                prop="valueType"
                label="展示类型"
                width="110"
              >
                <template #default="{ row }">
                  {{ valueTypeLabel(row.valueType) }}
                </template>
              </el-table-column>
              <el-table-column
                prop="defaultValue"
                label="默认值"
                min-width="150"
                show-overflow-tooltip
              >
                <template #default="{ row }">
                  {{ displayValue(row.defaultValue) }}
                </template>
              </el-table-column>
              <el-table-column
                prop="options"
                label="可选择的值"
                min-width="180"
                show-overflow-tooltip
              >
                <template #default="{ row }">
                  {{ displayOptions(row) }}
                </template>
              </el-table-column>
              <el-table-column
                prop="optionSource"
                label="选项来源"
                width="120"
              >
                <template #default="{ row }">
                  {{ optionSourceLabel(row.optionSource) }}
                </template>
              </el-table-column>
              <el-table-column
                prop="dictType"
                label="绑定字典"
                min-width="130"
                show-overflow-tooltip
              >
                <template #default="{ row }">
                  {{ dictTypeLabel(row.dictType) }}
                </template>
              </el-table-column>
              <el-table-column
                prop="configValue"
                label="当前值"
                min-width="160"
                show-overflow-tooltip
              >
                <template #default="{ row }">
                  {{ displayValue(row.configValue) }}
                </template>
              </el-table-column>
              <el-table-column
                prop="editable"
                label="可编辑"
                width="100"
              >
                <template #default="{ row }">
                  <el-tag
                    :type="row.editable === false ? 'warning' : 'success'"
                    effect="plain"
                    size="small"
                  >
                    {{ row.editable === false ? '只读' : '可编辑' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column
                prop="status"
                label="状态"
                width="90"
              >
                <template #default="{ row }">
                  <el-tag
                    :type="row.status === 0 ? 'info' : 'success'"
                    effect="plain"
                    size="small"
                  >
                    {{ row.status === 0 ? '禁用' : '启用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column
                label="操作"
                width="190"
                fixed="right"
              >
                <template #default="{ row }">
                  <el-button
                    link
                    type="primary"
                    @click="handleView(row)"
                  >
                    详情
                  </el-button>
                  <el-button
                    link
                    type="primary"
                    @click="handleEdit(row)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    link
                    type="danger"
                    @click="handleDelete(row)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <Pagination
              v-model:page="query.pageNum"
              v-model:limit="query.pageSize"
              :total="filteredConfigs.length"
            />
          </el-card>
        </section>
      </div>
    </template>

    <template v-else>
      <el-card
        class="config-panel-card"
        shadow="never"
      >
        <div class="config-panel-toolbar">
          <div>
            <el-button @click="backToList">
              返回列表
            </el-button>
            <span class="config-panel-toolbar__title">
              操作面板
            </span>
          </div>
        </div>

        <SystemConfigPanel
          :key="panelKey"
          :domain-codes="operationPanelDomains"
          :domain-labels="domainLabelMap"
          :keyword="query.keyword"
          :readonly="false"
          :type-filter="panelValueTypes"
          @loaded="handlePanelLoaded"
        />
      </el-card>
    </template>

    <el-dialog
      v-model="detailVisible"
      title="参数详情"
      width="720px"
    >
      <el-descriptions
        v-if="detailConfig"
        :column="2"
        border
      >
        <el-descriptions-item label="参数定义">
          {{ detailConfig.configName || detailConfig.configKey }}
        </el-descriptions-item>
        <el-descriptions-item label="参数键">
          {{ detailConfig.configKey }}
        </el-descriptions-item>
        <el-descriptions-item label="业务域">
          {{ domainDisplayName(detailConfig.domainCode) }}
        </el-descriptions-item>
        <el-descriptions-item label="展示类型">
          {{ valueTypeLabel(detailConfig.valueType) }}
        </el-descriptions-item>
        <el-descriptions-item label="默认值">
          {{ displayValue(detailConfig.defaultValue) }}
        </el-descriptions-item>
        <el-descriptions-item label="当前值">
          {{ displayValue(detailConfig.configValue) }}
        </el-descriptions-item>
        <el-descriptions-item label="可选择的值">
          {{ displayOptions(detailConfig) }}
        </el-descriptions-item>
        <el-descriptions-item label="选项来源">
          {{ optionSourceLabel(detailConfig.optionSource) }}
        </el-descriptions-item>
        <el-descriptions-item label="绑定字典">
          {{ dictTypeLabel(detailConfig.dictType) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ detailConfig.status === 0 ? '禁用' : '启用' }}
        </el-descriptions-item>
        <el-descriptions-item label="可编辑">
          {{ detailConfig.editable === false ? (detailConfig.editableReason || '只读') : '可编辑' }}
        </el-descriptions-item>
        <el-descriptions-item label="配置介绍">
          {{ detailConfig.description || detailConfig.remark || '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑参数' : '新增参数'"
      width="760px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="110px"
      >
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item
              label="参数键"
              prop="configKey"
            >
              <el-input
                v-model="form.configKey"
                placeholder="请输入参数键"
                :disabled="!!form.id"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="参数名称"
              prop="configName"
            >
              <el-input
                v-model="form.configName"
                placeholder="请输入参数名称"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="业务域"
              prop="domainCode"
            >
              <el-tree-select
                v-model="form.domainCode"
                :data="domainTree"
                :loading="domainLoading"
                :props="domainTreeProps"
                node-key="domainCode"
                value-key="domainCode"
                check-strictly
                filterable
                default-expand-all
                placeholder="请选择业务域"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="参数分类"
              prop="configGroup"
            >
              <el-select
                v-model="form.configGroup"
                placeholder="请选择参数分类"
              >
                <el-option
                  v-for="item in configTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="String(item.value)"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="展示类型"
              prop="valueType"
            >
              <el-select
                v-model="form.valueType"
                placeholder="请选择展示类型"
              >
                <el-option
                  v-for="item in valueTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item
              label="状态"
              prop="status"
            >
              <el-radio-group v-model="form.status">
                <el-radio
                  v-for="item in statusOptions"
                  :key="item.value"
                  :label="Number(item.value)"
                >
                  {{ item.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="可编辑">
              <el-switch v-model="form.editable" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分组编码">
              <el-input
                v-model="form.groupCode"
                placeholder="可选"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分组名称">
              <el-input
                v-model="form.groupName"
                placeholder="可选"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="默认值">
              <el-input
                v-model="form.defaultValue"
                placeholder="请输入默认值"
              />
            </el-form-item>
          </el-col>
          <el-col
            v-if="usesOptions(form.valueType)"
            :span="12"
          >
            <el-form-item label="选项来源">
              <el-radio-group v-model="form.optionSource">
                <el-radio label="CUSTOM">
                  自定义
                </el-radio>
                <el-radio label="DICT">
                  字典
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col
            v-if="usesOptions(form.valueType) && form.optionSource === 'DICT'"
            :span="12"
          >
            <el-form-item
              label="绑定字典"
              prop="dictType"
            >
              <el-select
                v-model="form.dictType"
                :loading="dictTypeLoading"
                filterable
                clearable
                placeholder="请选择字典类型"
              >
                <el-option
                  v-for="item in dictTypes"
                  :key="item.code"
                  :label="`${item.name}（${item.code}）`"
                  :value="item.code"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item
              label="当前值"
              prop="configValue"
            >
              <el-input
                v-model="form.configValue"
                type="textarea"
                :rows="3"
                placeholder="日期区间使用 JSON 数组，例如：[&quot;2026-06-01&quot;,&quot;2026-06-23&quot;]"
              />
            </el-form-item>
          </el-col>
          <el-col
            v-if="usesOptions(form.valueType) && form.optionSource !== 'DICT'"
            :span="24"
          >
            <el-form-item label="可选择的值">
              <el-input
                v-model="form.options"
                type="textarea"
                :rows="2"
                placeholder="使用 JSON 数组，例如：[{&quot;label&quot;:&quot;高&quot;,&quot;value&quot;:&quot;high&quot;}]"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="参数介绍">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="2"
                placeholder="请输入参数介绍"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="不可编辑原因">
              <el-input
                v-model="form.editableReason"
                placeholder="参数不可编辑时展示"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="handleSubmit"
        >
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="SystemConfig">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { Pagination, useDict } from '@mango/common';
import { configApi, type ConfigOptionSource, type ConfigValueType, type SysConfig } from '../../api/config';
import { dictTypeApi, type DictType } from '../../api/dict';
import { domainApi, type DomainItem } from '../../api/domain';
import DomainSideTree from '../../components/DomainSideTree/index.vue';
import SystemConfigPanel from '../../components/SystemConfigPanel/index.vue';

type ViewMode = 'list' | 'panel';

interface ConfigQuery {
  keyword: string;
  configGroup: string;
  valueType: ConfigValueType | '';
  status: number | undefined;
  pageNum: number;
  pageSize: number;
}

interface DomainTreeProps {
  label: string;
  value: string;
  children: string;
}

const { options: statusOptions } = useDict('sys_normal_disable');
const { options: configTypeOptions } = useDict('system_config_type');

const viewMode = ref<ViewMode>('list');
const selectedDomainCode = ref('');
const domainTree = ref<DomainItem[]>([]);
const domainLoading = ref(false);
const listLoading = ref(false);
const submitLoading = ref(false);
const dictTypeLoading = ref(false);
const configList = ref<SysConfig[]>([]);
const dictTypes = ref<DictType[]>([]);
const panelKey = ref(0);

const domainTreeProps: DomainTreeProps = {
  label: 'domainName',
  value: 'domainCode',
  children: 'children',
};

const query = reactive<ConfigQuery>({
  keyword: '',
  configGroup: '',
  valueType: '',
  status: undefined,
  pageNum: 1,
  pageSize: 10,
});

const valueTypeOptions: Array<{ label: string; value: ConfigValueType }> = [
  { label: '开关', value: 'BOOLEAN' },
  { label: '文本', value: 'STRING' },
  { label: '数字', value: 'NUMBER' },
  { label: '单选', value: 'RADIO' },
  { label: '下拉', value: 'SELECT' },
  { label: '多选', value: 'MULTI_SELECT' },
  { label: '日期', value: 'DATE' },
  { label: '日期区间', value: 'DATE_RANGE' },
];

const optionSourceOptions: Array<{ label: string; value: ConfigOptionSource }> = [
  { label: '自定义', value: 'CUSTOM' },
  { label: '字典', value: 'DICT' },
];

const filteredConfigs = computed(() => {
  const keyword = query.keyword.trim().toLowerCase();
  return configList.value.filter((config) => {
    const keywordMatched = !keyword
      || [
        config.configName,
        config.configKey,
        config.description,
        config.remark,
      ].some((value) => String(value || '').toLowerCase().includes(keyword));
    const valueTypeMatched = !query.valueType || config.valueType === query.valueType;
    const statusMatched = query.status === undefined || config.status === query.status;
    return keywordMatched && valueTypeMatched && statusMatched;
  });
});

const pagedConfigs = computed(() => {
  const start = (query.pageNum - 1) * query.pageSize;
  return filteredConfigs.value.slice(start, start + query.pageSize);
});

const flatDomains = computed(() => flattenDomains(domainTree.value));

const domainLabelMap = computed(() => {
  return flatDomains.value.reduce<Record<string, string>>((result, domain) => {
    result[domain.domainCode] = domain.domainName;
    return result;
  }, {});
});

const operationPanelDomains = computed(() => {
  if (selectedDomainCode.value) {
    return [selectedDomainCode.value];
  }
  const configuredCodes = new Set(configList.value
    .map((config) => config.domainCode)
    .filter((code): code is string => Boolean(code)));
  const domainCodes = flatDomains.value
    .map((domain) => domain.domainCode)
    .filter((code) => configuredCodes.has(code));
  const sortedCodes = [
    ...domainCodes.filter((code) => code !== 'COMMON'),
    ...domainCodes.filter((code) => code === 'COMMON'),
  ];
  return sortedCodes.length ? sortedCodes : ['COMMON'];
});

const panelValueTypes = computed(() => {
  return query.valueType ? [query.valueType] : [];
});

const dialogVisible = ref(false);
const detailVisible = ref(false);
const formRef = ref<FormInstance>();
const detailConfig = ref<SysConfig>();
const form = reactive<SysConfig>({
  id: undefined,
  configKey: '',
  configValue: '',
  configName: '',
  configGroup: 'system',
  type: 'SYSTEM',
  domainCode: 'COMMON',
  valueType: 'STRING',
  groupCode: '',
  groupName: '',
  defaultValue: '',
  options: '',
  optionSource: 'CUSTOM',
  dictType: '',
  editable: true,
  editableReason: '',
  description: '',
  status: 1,
});

const rules: FormRules = {
  configKey: [{ required: true, message: '请输入参数键', trigger: 'blur' }],
  configName: [{ required: true, message: '请输入参数名称', trigger: 'blur' }],
  configValue: [{ required: true, message: '请输入当前值', trigger: 'blur' }],
  configGroup: [{ required: true, message: '请选择参数分类', trigger: 'change' }],
  domainCode: [{ required: true, message: '请选择业务域', trigger: 'change' }],
  valueType: [{ required: true, message: '请选择展示类型', trigger: 'change' }],
  dictType: [{
    validator: (_rule, value, callback) => {
      if (usesOptions(form.valueType) && form.optionSource === 'DICT' && !value) {
        callback(new Error('请选择绑定字典'));
        return;
      }
      callback();
    },
    trigger: 'change',
  }],
};

onMounted(() => {
  void loadDomains();
  void loadDictTypes();
  void loadConfigList();
});

async function loadDomains() {
  domainLoading.value = true;
  try {
    domainTree.value = await domainApi.enabledTree();
  } finally {
    domainLoading.value = false;
  }
}

function handleDomainLoaded(domains: DomainItem[]) {
  domainTree.value = domains;
}

function handleDomainChange() {
  query.pageNum = 1;
  void loadConfigList();
}

async function loadConfigList() {
  listLoading.value = true;
  try {
    const result = await configApi.list({
      configGroup: query.configGroup || undefined,
      domainCode: selectedDomainCode.value || undefined,
    });
    configList.value = result.list || [];
  } finally {
    listLoading.value = false;
  }
}

async function loadDictTypes() {
  dictTypeLoading.value = true;
  try {
    const result = await dictTypeApi.list({ pageNum: 1, pageSize: 500 });
    dictTypes.value = result.list || [];
  } finally {
    dictTypeLoading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  void loadConfigList();
}

function handleReset() {
  query.keyword = '';
  query.configGroup = '';
  query.valueType = '';
  query.status = undefined;
  query.pageNum = 1;
  void loadConfigList();
}

function openOperationPanel() {
  panelKey.value += 1;
  viewMode.value = 'panel';
}

function backToList() {
  viewMode.value = 'list';
  void loadConfigList();
}

function handlePanelLoaded(domainCode: string, configs: SysConfig[]) {
  const existingCodes = new Set(flatDomains.value.map((domain) => domain.domainCode));
  if (!existingCodes.has(domainCode) && configs.length > 0) {
    void loadDomains();
  }
}

function handleAdd() {
  resetForm({
    domainCode: selectedDomainCode.value || firstDomainCode(),
  });
  dialogVisible.value = true;
}

function handleEdit(row: SysConfig) {
  resetForm(row);
  dialogVisible.value = true;
}

function handleView(row: SysConfig) {
  detailConfig.value = row;
  detailVisible.value = true;
}

async function handleDelete(row: SysConfig) {
  if (!row.id) {
    return;
  }
  await ElMessageBox.confirm(`确认删除参数“${row.configName || row.configKey}”？`, '删除确认', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning',
    confirmButtonClass: 'el-button--danger',
  });
  await configApi.delete(row.id);
  ElMessage.success('删除成功');
  await loadConfigList();
  panelKey.value += 1;
}

async function handleSubmit() {
  if (!formRef.value) {
    return;
  }
  await formRef.value.validate();
  submitLoading.value = true;
  try {
    const payload: SysConfig = {
      ...form,
      configGroup: form.configGroup || 'system',
      type: form.type || String(form.configGroup || 'system').toUpperCase(),
      optionSource: usesOptions(form.valueType) ? (form.optionSource || 'CUSTOM') : 'CUSTOM',
      dictType: usesOptions(form.valueType) && form.optionSource === 'DICT' ? form.dictType : '',
      options: usesOptions(form.valueType) && form.optionSource !== 'DICT' ? form.options : '',
      description: form.description,
      remark: form.description,
    };
    if (form.id) {
      await configApi.update(payload);
    } else {
      await configApi.create(payload);
    }
    ElMessage.success(form.id ? '修改成功' : '新增成功');
    dialogVisible.value = false;
    await loadConfigList();
    panelKey.value += 1;
  } finally {
    submitLoading.value = false;
  }
}

function resetForm(source?: Partial<SysConfig>) {
  Object.assign(form, {
    id: undefined,
    configKey: '',
    configValue: '',
    configName: '',
    configGroup: 'system',
    type: 'SYSTEM',
    domainCode: 'COMMON',
    valueType: 'STRING',
    groupCode: '',
    groupName: '',
    defaultValue: '',
    options: '',
    optionSource: 'CUSTOM',
    dictType: '',
    editable: true,
    editableReason: '',
    description: '',
    status: 1,
    ...source,
  });
}

function flattenDomains(domains: DomainItem[]): DomainItem[] {
  return domains.flatMap((domain) => [
    domain,
    ...flattenDomains(domain.children || []),
  ]);
}

function firstDomainCode() {
  return flatDomains.value[0]?.domainCode || 'COMMON';
}

function domainDisplayName(domainCode?: string) {
  if (!domainCode) {
    return '-';
  }
  const label = domainLabelMap.value[domainCode];
  return label ? `${label}（${domainCode}）` : domainCode;
}

function valueTypeLabel(type?: ConfigValueType) {
  const matched = valueTypeOptions.find((item) => item.value === (type || 'STRING'));
  return matched?.label || '文本';
}

function usesOptions(type?: ConfigValueType) {
  return type === 'RADIO' || type === 'SELECT' || type === 'MULTI_SELECT';
}

function optionSourceLabel(source?: ConfigOptionSource) {
  const matched = optionSourceOptions.find((item) => item.value === (source || 'CUSTOM'));
  return matched?.label || '自定义';
}

function dictTypeLabel(dictType?: string) {
  if (!dictType) {
    return '-';
  }
  const matched = dictTypes.value.find((item) => item.code === dictType);
  return matched ? `${matched.name}（${dictType}）` : dictType;
}

function displayValue(value?: string) {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return value;
}

function displayOptions(config?: SysConfig) {
  if (!config) {
    return '-';
  }
  if (config.optionSource === 'DICT') {
    return config.dictType ? `字典：${dictTypeLabel(config.dictType)}` : '字典：未绑定';
  }
  if (!config.options) {
    return '-';
  }
  try {
    const parsed = JSON.parse(config.options) as Array<{ label?: string; value?: string | number | boolean }>;
    if (!Array.isArray(parsed)) {
      return config.options;
    }
    return parsed
      .map((item) => `${item.label || item.value}: ${item.value}`)
      .join('，') || '-';
  } catch {
    return config.options;
  }
}
</script>

<style scoped lang="scss">
.config-container {
  padding: 0;
}

.config-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 14px;
  align-items: start;
}

.config-main {
  min-width: 0;
}

.config-search {
  margin-bottom: 12px;
}

.config-search__form {
  :deep(.el-form-item) {
    margin-bottom: 10px;
  }
}

.config-table-card {
  min-width: 0;
}

.config-toolbar,
.config-panel-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.config-toolbar__left,
.config-toolbar__right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.config-name-cell {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;

  span,
  small {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  small {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }
}

.config-panel-toolbar__title {
  margin-left: 10px;
  color: var(--el-text-color-primary);
  font-weight: 600;
}

@media (max-width: 960px) {
  .config-layout {
    grid-template-columns: 1fr;
  }
}
</style>
