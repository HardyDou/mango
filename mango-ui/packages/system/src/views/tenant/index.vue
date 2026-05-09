<template>
  <div class="tenant-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>机构管理</span>
          <el-button
            type="primary"
            @click="handleAdd"
          >
            新增机构
          </el-button>
        </div>
      </template>

      <el-form
        :inline="true"
        class="search-form"
      >
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="搜索机构名称/编码"
            clearable
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="query.status"
            placeholder="请选择"
            clearable
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

      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
      >
        <el-table-column
          prop="tenantName"
          label="机构名称"
        />
        <el-table-column
          prop="tenantCode"
          label="机构编码"
        />
        <el-table-column
          prop="institutionType"
          label="机构类型"
          width="120"
        >
          <template #default="{ row }">
            <DictTag
              dict-code="institution_type"
              :value="row.institutionType"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="capabilityCodes"
          label="开通能力"
          min-width="220"
        >
          <template #default="{ row }">
            <template v-if="row.capabilityCodeList?.length">
              <DictTag
                v-for="code in row.capabilityCodeList"
                :key="code"
                dict-code="institution_capability"
                :value="code"
                size="small"
              />
            </template>
            <span
              v-else
              class="empty-text"
            >
              未开通
            </span>
          </template>
        </el-table-column>
        <el-table-column
          prop="contactName"
          label="联系人"
        />
        <el-table-column
          prop="contactPhone"
          label="联系电话"
        />
        <el-table-column
          prop="contactEmail"
          label="联系邮箱"
        />
        <el-table-column
          prop="status"
          label="状态"
          width="80"
        >
          <template #default="{ row }">
            <DictTag
              dict-code="sys_normal_disable"
              :value="row.status"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          label="创建时间"
          width="180"
        />
        <el-table-column
          label="操作"
          width="200"
          fixed="right"
        >
          <template #default="{ row }">
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
              type="warning"
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
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

      <Pagination
        v-model:page="query.pageNum"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="loadData"
      />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑机构' : '新增机构'"
      width="600px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item
          label="机构名称"
          prop="tenantName"
        >
          <el-input
            v-model="form.tenantName"
            placeholder="请输入机构名称"
          />
        </el-form-item>
        <el-form-item
          label="机构编码"
          prop="tenantCode"
        >
          <el-input
            v-model="form.tenantCode"
            placeholder="请输入机构编码"
            :disabled="!!form.id"
          />
        </el-form-item>
        <el-form-item
          label="机构类型"
          prop="institutionType"
        >
          <DictSelect
            v-model="form.institutionType"
            dict-type="institution_type"
            placeholder="请选择机构类型"
          />
        </el-form-item>
        <el-form-item
          label="开通能力"
          prop="capabilityCodeList"
        >
          <DictSelect
            v-model="form.capabilityCodeList"
            dict-type="institution_capability"
            placeholder="请选择开通能力"
            multiple
            filterable
          />
        </el-form-item>
        <el-form-item
          label="联系人"
          prop="contactName"
        >
          <el-input
            v-model="form.contactName"
            placeholder="请输入联系人"
          />
        </el-form-item>
        <el-form-item
          label="联系电话"
          prop="contactPhone"
        >
          <el-input
            v-model="form.contactPhone"
            placeholder="请输入联系电话"
          />
        </el-form-item>
        <el-form-item
          label="联系邮箱"
          prop="contactEmail"
        >
          <el-input
            v-model="form.contactEmail"
            placeholder="请输入联系邮箱"
          />
        </el-form-item>
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
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="SystemTenant">
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { DictSelect, DictTag, Pagination, useDict } from '@mango/common';
import { tenantApi, type SysTenant } from '../../api/tenant';

const { options: statusOptions } = useDict('sys_normal_disable');

const loading = ref(false);
const tableData = ref<SysTenant[]>([]);
const total = ref(0);
const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: undefined as number | undefined,
});

const dialogVisible = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<SysTenant>({
  id: undefined,
  tenantName: '',
  tenantCode: '',
  institutionType: 'ENTERPRISE',
  capabilityCodeList: ['SYSTEM_ADMIN', 'AUTH_ADMIN', 'ORG_ADMIN', 'WORKFLOW'],
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  status: 1,
});

const rules: FormRules = {
  tenantName: [{ required: true, message: '请输入机构名称', trigger: 'blur' }],
  tenantCode: [{ required: true, message: '请输入机构编码', trigger: 'blur' }],
  institutionType: [{ required: true, message: '请选择机构类型', trigger: 'change' }],
};

async function loadData() {
  loading.value = true;
  try {
    const data = await tenantApi.list(query);
    tableData.value = data.list;
    total.value = data.total;
  } catch (error) {
    console.error('加载数据失败:', error);
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  loadData();
}

function handleReset() {
  query.keyword = '';
  query.status = undefined;
  query.pageNum = 1;
  loadData();
}

function handleAdd() {
  form.id = undefined;
  form.tenantName = '';
  form.tenantCode = '';
  form.institutionType = 'ENTERPRISE';
  form.capabilityCodeList = ['SYSTEM_ADMIN', 'AUTH_ADMIN', 'ORG_ADMIN', 'WORKFLOW'];
  form.contactName = '';
  form.contactPhone = '';
  form.contactEmail = '';
  form.status = 1;
  dialogVisible.value = true;
}

function handleEdit(row: SysTenant) {
  Object.assign(form, {
    ...row,
    capabilityCodeList: row.capabilityCodeList
      ?? (row.capabilityCodes ? row.capabilityCodes.split(',').filter(Boolean) : []),
  });
  dialogVisible.value = true;
}

async function handleSubmit() {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
    if (form.id) {
      await tenantApi.update(form);
      ElMessage.success('修改成功');
    } else {
      await tenantApi.create(form);
      ElMessage.success('新增成功');
    }
    dialogVisible.value = false;
    loadData();
  } catch (error) {
    console.error('提交失败:', error);
  }
}

async function handleToggleStatus(row: SysTenant) {
  const newStatus = row.status === 1 ? 0 : 1;
  const action = newStatus === 1 ? '启用' : '禁用';
  try {
    await tenantApi.updateStatus(row.id!, { status: newStatus });
    ElMessage.success(`${action}成功`);
    loadData();
  } catch (error) {
    console.error(`${action}失败:`, error);
  }
}

function handleDelete(row: SysTenant) {
  ElMessageBox.confirm('确认删除该机构?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await tenantApi.delete(row.id!);
      ElMessage.success('删除成功');
      loadData();
    } catch (error) {
      console.error('删除失败:', error);
    }
  }).catch(() => {});
}

onMounted(() => {
  loadData();
});
</script>

<style scoped lang="scss">
.tenant-container {
  padding: 0;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.search-form {
  margin-bottom: 16px;
  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}
.empty-text {
  color: var(--el-text-color-placeholder);
}
</style>
