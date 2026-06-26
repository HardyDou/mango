<template>
  <div class="tenant-container">
    <el-card>
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
          <DictSelect
            v-model="query.status"
            dict-type="institution_status"
            placeholder="状态"
            show-any-option
            any-option-label="不限"
            number-value
          />
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

      <div class="action-toolbar">
        <div class="toolbar-left">
          <el-button
            type="primary"
            @click="handleAdd"
          >
            新增机构
          </el-button>
        </div>
      </div>

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
          prop="packageId"
          label="绑定套餐"
          min-width="180"
        >
          <template #default="{ row }">
            <span>{{ packageNameMap.get(row.packageId || '0') || '未绑定套餐' }}</span>
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
          width="90"
        >
          <template #default="{ row }">
            <DictTag
              dict-code="institution_status"
              :value="row.status"
              size="small"
              :type="statusTagType(row.status)"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          label="创建时间"
          width="180"
        >
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="260"
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
              v-if="row.status !== 2"
              link
              type="warning"
              size="small"
              @click="handleUpdateStatus(row, 2)"
            >
              冻结
            </el-button>
            <el-button
              v-if="row.status !== 9"
              link
              type="info"
              size="small"
              @click="handleUpdateStatus(row, 9)"
            >
              归档
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
          label="机构套餐"
          prop="packageId"
        >
          <el-select
            v-model="form.packageId"
            placeholder="请选择机构套餐"
            filterable
            @change="handlePackageChange"
          >
            <el-option
              v-for="item in packageOptions"
              :key="item.packageId"
              :label="item.packageName"
              :value="item.packageId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="菜单预览">
          <el-tree
            ref="packageTreeRef"
            class="package-tree"
            node-key="menuId"
            show-checkbox
            default-expand-all
            :data="packageMenuTree"
            :props="treeProps"
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
import { ref, reactive, onMounted, computed, nextTick } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { DictSelect, DictTag, Pagination, formatDate, useDict } from '@mango/common';
import { tenantApi, type SysTenant } from '../../api/tenant';
import { menuApi, type SysMenuVO } from '@mango/rbac';
import { menuPackageApi, type MenuPackageVO } from '@mango/rbac';
import type { ApiId } from '@mango/api-schema';

const { options: statusOptions } = useDict('institution_status');

const statusActions: Record<number, string> = {
  0: '禁用',
  1: '启用',
  2: '冻结',
  9: '归档',
};

const loading = ref(false);
const tableData = ref<SysTenant[]>([]);
const total = ref(0);
const packageOptions = ref<MenuPackageVO[]>([]);
const packageMenuTree = ref<SysMenuVO[]>([]);
const packageTreeRef = ref();
const packageNameMap = computed(() => new Map(packageOptions.value.map(item => [item.packageId || '0', item.packageName])));
const treeProps = {
  label: 'menuName',
  children: 'children',
};
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
  packageId: undefined,
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  status: 1,
});

const rules: FormRules = {
  tenantName: [{ required: true, message: '请输入机构名称', trigger: 'blur' }],
  tenantCode: [{ required: true, message: '请输入机构编码', trigger: 'blur' }],
  institutionType: [{ required: true, message: '请选择机构类型', trigger: 'change' }],
  packageId: [{ required: true, message: '请选择机构套餐', trigger: 'change' }],
};

async function loadPackageOptions() {
  packageOptions.value = await menuPackageApi.list({ appCode: 'internal-admin', status: 1 });
}

async function loadPackageMenuTree() {
  packageMenuTree.value = await menuApi.getMenuTree({ appCode: 'internal-admin', fmt: 'tree' });
}

async function applyPackagePreview(packageId?: ApiId) {
  await nextTick();
  packageTreeRef.value?.setCheckedKeys([], false);
  if (!packageId) return;
  const detail = await menuPackageApi.detail(packageId);
  packageTreeRef.value?.setCheckedKeys(detail.menuIds || [], false);
}

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
  form.packageId = packageOptions.value[0]?.packageId;
  form.contactName = '';
  form.contactPhone = '';
  form.contactEmail = '';
  form.status = 1;
  dialogVisible.value = true;
  applyPackagePreview(form.packageId);
}

async function handleEdit(row: SysTenant) {
  Object.assign(form, {
    ...row,
  });
  dialogVisible.value = true;
  await applyPackagePreview(row.packageId);
}

async function handlePackageChange(packageId?: ApiId) {
  await applyPackagePreview(packageId);
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
  await handleUpdateStatus(row, newStatus);
}

async function handleUpdateStatus(row: SysTenant, status: number) {
  const action = statusActions[status] || '修改状态';
  const confirmMessage = status === 1
    ? `确认启用机构“${row.tenantName}”？`
    : `确认将机构“${row.tenantName}”${action}？非启用状态下，该机构成员不能登录或继续访问系统。`;
  try {
    await ElMessageBox.confirm(confirmMessage, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: status === 9 ? 'warning' : 'info',
    });
    await tenantApi.updateStatus(row.id!, { status });
    ElMessage.success(`${action}成功`);
    loadData();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error(`${action}失败:`, error);
    }
  }
}

function handleDelete(row: SysTenant) {
  ElMessageBox.confirm('仅允许删除未初始化、无关联数据的机构；已使用机构请归档。确认继续删除?', '提示', {
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

function statusTagType(status?: number) {
  if (status === 1) return 'success';
  if (status === 2) return 'warning';
  if (status === 9) return 'info';
  return 'danger';
}

onMounted(() => {
  loadPackageOptions();
  loadPackageMenuTree();
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

.package-tree {
  width: 100%;
  max-height: 260px;
  overflow: auto;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  padding: 12px;
}
</style>
