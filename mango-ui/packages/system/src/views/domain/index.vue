<template>
  <div class="domain-page">
    <el-card>
      <el-form
        :model="query"
        class="domain-search"
        label-width="72px"
      >
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="7" :lg="6">
            <el-form-item label="编码">
              <el-input
                v-model="query.domainCode"
                clearable
                placeholder="业务域编码"
                @keyup.enter="loadDomains"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="7" :lg="6">
            <el-form-item label="名称">
              <el-input
                v-model="query.domainName"
                clearable
                placeholder="业务域名称"
                @keyup.enter="loadDomains"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6" :lg="5">
            <el-form-item label="状态">
              <el-select
                v-model="query.status"
                clearable
                placeholder="全部状态"
              >
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="6" :lg="7">
            <div class="domain-actions">
              <el-button @click="resetQuery">重置</el-button>
              <el-button type="primary" @click="loadDomains">查询</el-button>
              <el-button type="primary" @click="openCreate">新增业务域</el-button>
            </div>
          </el-col>
        </el-row>
      </el-form>

      <el-table
        v-loading="loading"
        :data="domains"
        row-key="id"
        :tree-props="{ children: 'children' }"
        default-expand-all
        stripe
      >
        <el-table-column prop="domainName" label="业务域" min-width="180" />
        <el-table-column prop="domainCode" label="编码" min-width="180" show-overflow-tooltip />
        <el-table-column prop="domainShortCode" label="简写" width="110" />
        <el-table-column prop="parentName" label="父域" min-width="130">
          <template #default="{ row }">
            {{ row.parentName || '顶级' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="96">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="90" />
        <el-table-column prop="updateTime" label="更新时间" min-width="170" show-overflow-tooltip />
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openCreate(row)">新增下级</el-button>
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" size="small" @click="deleteDomain(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑业务域' : '新增业务域'"
      width="720px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
      >
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="上级业务域">
              <el-tree-select
                v-model="form.parentId"
                :data="parentOptions"
                :props="treeProps"
                node-key="id"
                check-strictly
                clearable
                :disabled="Boolean(form.id)"
                placeholder="顶级业务域"
                class="full-input"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" class="full-input">
                <el-option label="启用" :value="1" />
                <el-option label="停用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="本层编码" prop="domainCode">
              <el-input
                v-model="form.domainCode"
                :disabled="Boolean(form.id)"
                placeholder="如 ORDER 或 PAY"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="最终编码">
              <el-input :model-value="previewCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="编码简写" prop="domainShortCode">
              <el-input v-model="form.domainShortCode" placeholder="如 OD" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="业务域名称" prop="domainName">
              <el-input v-model="form.domainName" placeholder="请输入业务域名称" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="form.sort" :min="0" class="full-input" />
            </el-form-item>
          </el-col>
          <el-col :xs="24">
            <el-form-item label="备注">
              <el-input
                v-model="form.remark"
                type="textarea"
                :rows="3"
                maxlength="512"
                show-word-limit
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { ApiId } from '@mango/api-schema';
import { domainApi, normalizeCode, type DomainItem } from '../../api/domain';

type DomainForm = {
  id?: ApiId;
  domainCode: string;
  domainShortCode: string;
  domainName: string;
  parentId?: ApiId | 0;
  sort: number;
  status: number;
  remark: string;
};

const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const domains = ref<DomainItem[]>([]);
const parentOptions = ref<DomainItem[]>([]);
const formRef = ref<FormInstance>();

const query = reactive({
  domainCode: '',
  domainName: '',
  status: undefined as number | undefined,
});

const form = reactive<DomainForm>({
  domainCode: '',
  domainShortCode: '',
  domainName: '',
  parentId: 0,
  sort: 0,
  status: 1,
  remark: '',
});

const treeProps = {
  label: 'domainName',
  value: 'id',
  children: 'children',
};

const rules: FormRules = {
  domainCode: [{ required: true, message: '请输入业务域编码', trigger: 'blur' }],
  domainShortCode: [{ required: true, message: '请输入业务域简写', trigger: 'blur' }],
  domainName: [{ required: true, message: '请输入业务域名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
};

const previewCode = computed(() => {
  const current = form.domainCode ? normalizeCode(form.domainCode) : '';
  if (!current) {
    return '';
  }
  const parent = findDomainById(parentOptions.value, form.parentId);
  if (!parent) {
    return current;
  }
  const prefix = `${parent.domainCode}_`;
  return current.startsWith(prefix) ? current : `${prefix}${current}`;
});

onMounted(loadDomains);

async function loadDomains() {
  loading.value = true;
  try {
    domains.value = await domainApi.tree({
      domainCode: query.domainCode || undefined,
      domainName: query.domainName || undefined,
      status: query.status,
    });
    parentOptions.value = await domainApi.tree();
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  query.domainCode = '';
  query.domainName = '';
  query.status = undefined;
  loadDomains();
}

function openCreate(parent?: DomainItem) {
  resetForm();
  form.parentId = parent?.id ?? 0;
  dialogVisible.value = true;
}

async function openEdit(row: DomainItem) {
  const detail = await domainApi.detail(row.id!);
  resetForm();
  form.id = detail.id;
  form.parentId = detail.parentId || 0;
  form.domainCode = detail.domainCode;
  form.domainShortCode = detail.domainShortCode;
  form.domainName = detail.domainName;
  form.sort = detail.sort ?? 0;
  form.status = detail.status ?? 1;
  form.remark = detail.remark || '';
  dialogVisible.value = true;
}

async function submitForm() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    if (form.id) {
      await domainApi.update({
        id: form.id,
        domainShortCode: form.domainShortCode,
        domainName: form.domainName,
        sort: form.sort,
        status: form.status,
        remark: form.remark,
      });
    } else {
      await domainApi.create({
        domainCode: form.domainCode,
        domainShortCode: form.domainShortCode,
        domainName: form.domainName,
        parentId: form.parentId || 0,
        sort: form.sort,
        status: form.status,
        remark: form.remark,
      });
    }
    ElMessage.success('保存成功');
    dialogVisible.value = false;
    await loadDomains();
  } finally {
    saving.value = false;
  }
}

async function toggleStatus(row: DomainItem) {
  const nextStatus = row.status === 1 ? 0 : 1;
  await domainApi.updateStatus(row.id!, nextStatus);
  ElMessage.success(nextStatus === 1 ? '已启用' : '已停用');
  await loadDomains();
}

async function deleteDomain(row: DomainItem) {
  await ElMessageBox.confirm(`确认删除业务域“${row.domainName}”？`, '删除确认', {
    type: 'warning',
  });
  await domainApi.delete(row.id!);
  ElMessage.success('删除成功');
  await loadDomains();
}

function resetForm() {
  form.id = undefined;
  form.domainCode = '';
  form.domainShortCode = '';
  form.domainName = '';
  form.parentId = 0;
  form.sort = 0;
  form.status = 1;
  form.remark = '';
  formRef.value?.clearValidate();
}

function findDomainById(items: DomainItem[], id?: ApiId | 0): DomainItem | undefined {
  if (!id || id === 0) {
    return undefined;
  }
  for (const item of items) {
    if (String(item.id) === String(id)) {
      return item;
    }
    const child = findDomainById(item.children || [], id);
    if (child) {
      return child;
    }
  }
  return undefined;
}
</script>

<style scoped>
.domain-page {
  padding: 16px;
}

.domain-search {
  margin-bottom: 8px;
}

.domain-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  width: 100%;
}

.full-input {
  width: 100%;
}

@media (max-width: 768px) {
  .domain-actions {
    justify-content: flex-start;
    flex-wrap: wrap;
  }
}
</style>
