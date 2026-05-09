<template>
  <div class="area-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div>
            <span>行政区划</span>
            <span class="current-level">当前层级：{{ currentLevelLabel }}</span>
          </div>
          <div class="header-actions">
            <el-button @click="loadRoot">
              返回省级
            </el-button>
            <el-button
              type="primary"
              @click="handleAdd"
            >
              新增区划
            </el-button>
          </div>
        </div>
      </template>

      <el-breadcrumb
        class="area-breadcrumb"
        separator="/"
      >
        <el-breadcrumb-item>
          <el-button
            link
            type="primary"
            @click="loadRoot"
          >
            全国
          </el-button>
        </el-breadcrumb-item>
        <el-breadcrumb-item
          v-for="item in pathStack"
          :key="item.id"
        >
          <el-button
            link
            type="primary"
            @click="loadChildren(item, true)"
          >
            {{ item.name }}
          </el-button>
        </el-breadcrumb-item>
      </el-breadcrumb>

      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        row-key="id"
      >
        <el-table-column
          prop="name"
          label="区划名称"
          min-width="160"
        />
        <el-table-column
          prop="adcode"
          label="区划编码"
          width="130"
        />
        <el-table-column
          prop="areaType"
          label="层级"
          width="110"
        >
          <template #default="{ row }">
            {{ areaTypeLabel(row.areaType) }}
          </template>
        </el-table-column>
        <el-table-column
          prop="cityCode"
          label="城市编码"
          width="110"
        />
        <el-table-column
          prop="location"
          label="经纬度"
          min-width="150"
          show-overflow-tooltip
        />
        <el-table-column
          prop="hot"
          label="热门"
          width="90"
        >
          <template #default="{ row }">
            <DictTag
              dict-code="sys_yes_no"
              :value="row.hot"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="areaStatus"
          label="状态"
          width="90"
        >
          <template #default="{ row }">
            <DictTag
              dict-code="sys_normal_disable"
              :value="row.areaStatus"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="areaSort"
          label="排序"
          width="90"
        />
        <el-table-column
          label="操作"
          width="230"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              :disabled="Number(row.areaType) >= 4"
              @click="loadChildren(row)"
            >
              下级
            </el-button>
            <el-button
              link
              type="primary"
              size="small"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              link
              type="danger"
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑区划' : '新增区划'"
      width="620px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="110px"
      >
        <el-form-item label="上级区划">
          <el-input
            :model-value="parentLabel"
            disabled
          />
        </el-form-item>
        <el-form-item
          label="区划名称"
          prop="name"
        >
          <el-input
            v-model="form.name"
            placeholder="请输入区划名称"
          />
        </el-form-item>
        <el-form-item
          label="区划编码"
          prop="adcode"
        >
          <el-input-number
            v-model="form.adcode"
            :min="0"
            :controls="false"
            class="full-input"
          />
        </el-form-item>
        <el-form-item
          label="层级"
          prop="areaType"
        >
          <el-select
            v-model="form.areaType"
            placeholder="请选择层级"
          >
            <el-option
              v-for="item in areaTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="城市编码">
          <el-input
            v-model="form.cityCode"
            placeholder="请输入城市编码"
          />
        </el-form-item>
        <el-form-item label="首字母">
          <el-input
            v-model="form.letter"
            placeholder="请输入首字母"
          />
        </el-form-item>
        <el-form-item label="经纬度">
          <el-input
            v-model="form.location"
            placeholder="如：116.4074,39.9042"
          />
        </el-form-item>
        <el-form-item label="热门">
          <el-radio-group v-model="form.hot">
            <el-radio label="1">
              是
            </el-radio>
            <el-radio label="0">
              否
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          label="状态"
          prop="areaStatus"
        >
          <el-radio-group v-model="form.areaStatus">
            <el-radio
              v-for="item in statusOptions"
              :key="item.value"
              :label="String(item.value)"
            >
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number
            v-model="form.areaSort"
            :min="0"
            :max="9999"
          />
        </el-form-item>
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
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="SystemArea">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { DictTag, useDict } from '@mango/common';
import { areaApi, type SysArea } from '../../api/area';

const { options: statusOptions } = useDict('sys_normal_disable');

const areaTypeOptions = [
  { label: '国家', value: '0' },
  { label: '省份', value: '1' },
  { label: '城市', value: '2' },
  { label: '区县', value: '3' },
  { label: '街道', value: '4' },
  { label: '自定义区域', value: '5' },
];

const loading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);
const tableData = ref<SysArea[]>([]);
const pathStack = ref<SysArea[]>([]);
const formRef = ref<FormInstance>();

const form = reactive<SysArea>({
  id: undefined,
  pid: 0,
  name: '',
  letter: '',
  adcode: undefined,
  location: '',
  areaSort: 0,
  areaStatus: '1',
  areaType: '1',
  hot: '0',
  cityCode: '',
  tenantId: 1,
});

const rules: FormRules = {
  name: [{ required: true, message: '请输入区划名称', trigger: 'blur' }],
  areaType: [{ required: true, message: '请选择层级', trigger: 'change' }],
  areaStatus: [{ required: true, message: '请选择状态', trigger: 'change' }],
};

const currentParent = computed(() => pathStack.value[pathStack.value.length - 1]);
const currentLevelLabel = computed(() => {
  const parent = currentParent.value;
  if (!parent) return '省/直辖市';
  return `${parent.name} 下级`;
});
const parentLabel = computed(() => currentParent.value?.name || '全国');

async function loadRoot() {
  loading.value = true;
  try {
    pathStack.value = [];
    tableData.value = await areaApi.children(0);
  } finally {
    loading.value = false;
  }
}

async function loadChildren(row: SysArea, fromBreadcrumb = false) {
  if (!row.id) return;
  loading.value = true;
  try {
    if (fromBreadcrumb) {
      const index = pathStack.value.findIndex((item) => item.id === row.id);
      pathStack.value = index >= 0 ? pathStack.value.slice(0, index + 1) : [row];
    } else {
      pathStack.value = [...pathStack.value, row];
    }
    tableData.value = await areaApi.children(row.id);
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  const parent = currentParent.value;
  const nextLevel = parent ? Math.min(Number(parent.areaType || 0) + 1, 5) : 5;
  form.id = undefined;
  form.pid = parent?.id || 0;
  form.name = '';
  form.letter = '';
  form.adcode = undefined;
  form.location = '';
  form.areaSort = 0;
  form.areaStatus = '1';
  form.areaType = String(nextLevel);
  form.hot = '0';
  form.cityCode = '';
  form.tenantId = parent?.tenantId || 1;
}

function handleAdd() {
  resetForm();
  dialogVisible.value = true;
}

function handleEdit(row: SysArea) {
  Object.assign(form, row);
  form.areaStatus = row.areaStatus ?? '1';
  form.areaType = row.areaType ?? '1';
  form.hot = row.hot ?? '0';
  form.areaSort = row.areaSort ?? 0;
  dialogVisible.value = true;
}

async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate();
  submitLoading.value = true;
  try {
    if (form.id) {
      await areaApi.update(form);
      ElMessage.success('修改成功');
    } else {
      await areaApi.create(form);
      ElMessage.success('新增成功');
    }
    dialogVisible.value = false;
    await reloadCurrentLevel();
  } finally {
    submitLoading.value = false;
  }
}

function handleDelete(row: SysArea) {
  if (!row.id) return;
  ElMessageBox.confirm(`确认删除区划「${row.name}」?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await areaApi.delete(row.id!);
    ElMessage.success('删除成功');
    await reloadCurrentLevel();
  }).catch(() => {});
}

async function reloadCurrentLevel() {
  const parent = currentParent.value;
  if (parent?.id) {
    tableData.value = await areaApi.children(parent.id);
    return;
  }
  await loadRoot();
}

function areaTypeLabel(value?: string) {
  return areaTypeOptions.find((item) => item.value === String(value))?.label || value || '-';
}

onMounted(() => {
  loadRoot();
});
</script>

<style scoped lang="scss">
.area-container {
  padding: 0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;

  .current-level {
    margin-left: 12px;
    color: var(--mango-text-color-secondary);
    font-size: 13px;
  }

  .header-actions {
    display: flex;
    gap: 8px;
  }
}

.area-breadcrumb {
  margin-bottom: 16px;
}

.full-input {
  width: 100%;
}
</style>
