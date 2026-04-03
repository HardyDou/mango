<template>
  <div class="tenant-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>租户管理</span>
          <el-button
            type="primary"
            @click="handleAdd"
          >
            新增租户
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
            placeholder="搜索租户名称/编码"
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
              label="启用"
              :value="1"
            />
            <el-option
              label="禁用"
              :value="0"
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
          label="租户名称"
        />
        <el-table-column
          prop="tenantCode"
          label="租户编码"
        />
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
          prop="expireTime"
          label="过期时间"
          width="180"
        >
          <template #default="{ row }">
            {{ row.expireTime || '永不过期' }}
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          label="状态"
          width="80"
        >
          <template #default="{ row }">
            <el-tag
              :type="row.status === 1 ? 'success' : 'danger'"
              size="small"
            >
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
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
      :title="form.id ? '编辑租户' : '新增租户'"
      width="600px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item
          label="租户名称"
          prop="tenantName"
        >
          <el-input
            v-model="form.tenantName"
            placeholder="请输入租户名称"
          />
        </el-form-item>
        <el-form-item
          label="租户编码"
          prop="tenantCode"
        >
          <el-input
            v-model="form.tenantCode"
            placeholder="请输入租户编码"
            :disabled="!!form.id"
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
          label="过期时间"
          prop="expireTime"
        >
          <el-date-picker
            v-model="form.expireTime"
            type="datetime"
            placeholder="不设置则永不过期"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item
          label="状态"
          prop="status"
        >
          <el-radio-group v-model="form.status">
            <el-radio :label="1">
              启用
            </el-radio>
            <el-radio :label="0">
              禁用
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
import Pagination from '@/components/Pagination/index.vue';
import { tenantApi, type SysTenant } from '@/api/admin/tenant';

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
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  expireTime: '',
  status: 1,
});

const rules: FormRules = {
  tenantName: [{ required: true, message: '请输入租户名称', trigger: 'blur' }],
  tenantCode: [{ required: true, message: '请输入租户编码', trigger: 'blur' }],
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
  form.contactName = '';
  form.contactPhone = '';
  form.contactEmail = '';
  form.expireTime = '';
  form.status = 1;
  dialogVisible.value = true;
}

function handleEdit(row: SysTenant) {
  Object.assign(form, row);
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
  ElMessageBox.confirm('确认删除该租户?', '提示', {
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
  padding: 20px;
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
</style>
