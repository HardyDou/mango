<template>
  <div class="role-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>角色管理</span>
          <el-button
            type="primary"
            @click="handleAdd"
          >
            新增角色
          </el-button>
        </div>
      </template>
      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
      >
        <el-table-column
          prop="roleName"
          label="角色名称"
        />
        <el-table-column
          prop="roleCode"
          label="角色编码"
        />
        <el-table-column
          prop="realm"
          label="登录域"
          width="120"
        />
        <el-table-column
          prop="roleType"
          label="类型"
          width="100"
        >
          <template #default="{ row }">
            <el-tag size="small">
              {{ row.roleType === 1 ? '系统角色' : '业务角色' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="remark"
          label="备注"
        />
        <el-table-column
          prop="status"
          label="状态"
        >
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          label="创建时间"
        />
        <el-table-column
          label="操作"
          width="180"
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
              type="danger"
              size="small"
              :disabled="row.roleId === 1"
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
      :title="form.roleId ? '编辑角色' : '新增角色'"
      width="560px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="应用编码" prop="appCode">
          <el-input v-model="form.appCode" />
        </el-form-item>
        <el-form-item label="登录域" prop="realm">
          <el-input v-model="form.realm" />
        </el-form-item>
        <el-form-item label="操作者类型" prop="actorType">
          <el-input v-model="form.actorType" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input
            v-model="form.roleCode"
            :disabled="!!form.roleId"
          />
        </el-form-item>
        <el-form-item label="角色类型" prop="roleType">
          <el-radio-group v-model="form.roleType">
            <el-radio :label="1">
              系统角色
            </el-radio>
            <el-radio :label="2">
              业务角色
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">
              启用
            </el-radio>
            <el-radio :label="0">
              禁用
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number
            v-model="form.sort"
            :min="0"
            :max="9999"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
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

<script setup lang="ts" name="SystemRole">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { roleApi, type RoleVO } from '../../api/role';

const loading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);
const tableData = ref<RoleVO[]>([]);
const formRef = ref<FormInstance>();

const form = reactive<RoleVO>({
  roleId: undefined,
  appCode: 'internal-admin',
  realm: 'INTERNAL',
  actorType: 'INTERNAL_USER',
  roleCode: '',
  roleName: '',
  roleType: 1,
  status: 1,
  sort: 0,
  remark: '',
});

const rules: FormRules = {
  appCode: [{ required: true, message: '请输入应用编码', trigger: 'blur' }],
  realm: [{ required: true, message: '请输入登录域', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleType: [{ required: true, message: '请选择角色类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
};

async function loadData() {
  loading.value = true;
  try {
    tableData.value = await roleApi.list();
  } catch (error) {
    console.error('加载角色失败:', error);
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  Object.assign(form, {
    roleId: undefined,
    appCode: 'internal-admin',
    realm: 'INTERNAL',
    actorType: 'INTERNAL_USER',
    roleCode: '',
    roleName: '',
    roleType: 1,
    status: 1,
    sort: 0,
    remark: '',
  });
  formRef.value?.clearValidate();
}

const handleAdd = () => {
  resetForm();
  dialogVisible.value = true;
};

const handleEdit = (row: RoleVO) => {
  Object.assign(form, {
    roleId: row.roleId,
    appCode: row.appCode || 'internal-admin',
    realm: row.realm || 'INTERNAL',
    actorType: row.actorType || 'INTERNAL_USER',
    roleCode: row.roleCode,
    roleName: row.roleName,
    roleType: row.roleType || 1,
    status: row.status ?? 1,
    sort: row.sort ?? 0,
    remark: row.remark || '',
  });
  formRef.value?.clearValidate();
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate();
  submitLoading.value = true;
  try {
    if (form.roleId) {
      await roleApi.update(form);
      ElMessage.success('修改成功');
    } else {
      await roleApi.create(form);
      ElMessage.success('新增成功');
    }
    dialogVisible.value = false;
    await loadData();
  } catch (error) {
    console.error('保存角色失败:', error);
  } finally {
    submitLoading.value = false;
  }
};

const handleDelete = (row: RoleVO) => {
  if (!row.roleId) return;
  ElMessageBox.confirm(`确认删除角色「${row.roleName}」?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await roleApi.delete(row.roleId!);
      ElMessage.success('删除成功');
      await loadData();
    } catch (error) {
      console.error('删除角色失败:', error);
    }
  }).catch(() => {});
};

onMounted(() => {
  loadData();
});
</script>

<style scoped lang="scss">
.role-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
