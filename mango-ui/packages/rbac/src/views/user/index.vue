<template>
  <div class="user-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>成员管理</span>
          <el-button
            type="primary"
            @click="handleAdd"
          >
            新增成员
          </el-button>
        </div>
      </template>

      <el-form
        :inline="true"
        class="search-form"
      >
        <el-form-item label="用户名">
          <el-input
            v-model="query.username"
            placeholder="请输入用户名"
            clearable
          />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input
            v-model="query.nickname"
            placeholder="请输入昵称"
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
          prop="username"
          label="用户名"
          min-width="140"
        />
        <el-table-column
          prop="nickname"
          label="昵称"
          min-width="140"
        />
        <el-table-column
          prop="realm"
          label="登录域"
          width="120"
        >
          <template #default="{ row }">
            <DictTag
              dict-code="auth_realm"
              :value="row.realm"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="actorType"
          label="操作者类型"
          width="140"
        >
          <template #default="{ row }">
            <DictTag
              dict-code="auth_actor_type"
              :value="row.actorType"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="phone"
          label="手机号"
          min-width="130"
        />
        <el-table-column
          prop="email"
          label="邮箱"
          min-width="180"
          show-overflow-tooltip
        />
        <el-table-column
          prop="status"
          label="状态"
          width="90"
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
        >
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="300"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              @click="handleAssignRoles(row)"
            >
              分配角色
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
              type="warning"
              size="small"
              @click="handleResetPassword(row)"
            >
              重置密码
            </el-button>
            <el-button
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="handleStatus(row)"
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
      :title="form.userId ? '编辑成员' : '新增成员'"
      width="620px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="110px"
      >
        <el-form-item
          label="用户名"
          prop="username"
        >
          <el-input
            v-model="form.username"
            :disabled="!!form.userId"
            placeholder="请输入用户名"
          />
        </el-form-item>
        <el-form-item
          v-if="!form.userId"
          label="初始密码"
          prop="password"
        >
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="不填默认 admin123"
          />
        </el-form-item>
        <el-form-item
          label="登录域"
          prop="realm"
        >
          <el-select
            v-model="form.realm"
            :disabled="!!form.userId"
            class="form-select"
          >
            <el-option
              v-for="item in realmOptions"
              :key="item.value"
              :label="item.label"
              :value="String(item.value)"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="操作者类型"
          prop="actorType"
        >
          <el-select
            v-model="form.actorType"
            :disabled="!!form.userId"
            class="form-select"
          >
            <el-option
              v-for="item in actorTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="String(item.value)"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="昵称"
          prop="nickname"
        >
          <el-input
            v-model="form.nickname"
            placeholder="请输入昵称"
          />
        </el-form-item>
        <el-form-item
          label="手机号"
          prop="phone"
        >
          <el-input
            v-model="form.phone"
            placeholder="请输入手机号"
          />
        </el-form-item>
        <el-form-item
          label="邮箱"
          prop="email"
        >
          <el-input
            v-model="form.email"
            placeholder="请输入邮箱"
          />
        </el-form-item>
        <el-form-item
          label="归属主体类型"
          prop="partyType"
        >
          <el-input
            v-model="form.partyType"
            placeholder="例如 INTERNAL_ORG"
          />
        </el-form-item>
        <el-form-item
          label="归属主体ID"
          prop="partyId"
        >
          <el-input-number
            v-model="form.partyId"
            :min="0"
            class="form-select"
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
        <el-form-item
          label="备注"
          prop="remark"
        >
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

    <el-dialog
      v-model="assignDialogVisible"
      title="分配成员角色"
      width="520px"
    >
      <div class="assign-header">
        <span>{{ currentUser?.nickname || currentUser?.username }}</span>
        <el-tag
          v-if="currentUser?.username"
          effect="plain"
        >
          {{ currentUser.username }}
        </el-tag>
      </div>
      <el-checkbox-group v-model="selectedRoleIds">
        <div
          v-for="role in roleOptions"
          :key="role.roleId"
          class="role-option"
        >
          <el-checkbox :label="role.roleId">
            {{ role.roleName }}（{{ role.roleCode }}）
          </el-checkbox>
        </div>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="assignDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="assignSubmitLoading"
          @click="handleAssignSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="SystemUser">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { DictTag, Pagination, Session, useDict } from '@mango/common';
import { roleApi, type RoleVO } from '../../api/role';
import { userApi, type IdentityUserVO } from '../../api/user';

const { options: statusOptions } = useDict('sys_normal_disable');
const { options: realmOptions } = useDict('auth_realm');
const { options: actorTypeOptions } = useDict('auth_actor_type');

const loading = ref(false);
const submitLoading = ref(false);
const assignSubmitLoading = ref(false);
const dialogVisible = ref(false);
const assignDialogVisible = ref(false);
const tableData = ref<IdentityUserVO[]>([]);
const roleOptions = ref<RoleVO[]>([]);
const selectedRoleIds = ref<number[]>([]);
const total = ref(0);
const formRef = ref<FormInstance>();
const currentUser = ref<IdentityUserVO>();

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  username: '',
  nickname: '',
  status: undefined as number | undefined,
});

const form = reactive<IdentityUserVO>({
  userId: undefined,
  username: '',
  password: '',
  nickname: '',
  realm: 'INTERNAL',
  actorType: 'INTERNAL_USER',
  partyType: 'INTERNAL_ORG',
  partyId: undefined,
  email: '',
  phone: '',
  status: 1,
  remark: '',
});

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realm: [{ required: true, message: '请选择登录域', trigger: 'change' }],
  actorType: [{ required: true, message: '请选择操作者类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
};

async function loadData() {
  loading.value = true;
  try {
    const data = await userApi.page(query);
    tableData.value = data.list;
    total.value = data.total;
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNum = 1;
  loadData();
}

function handleReset() {
  query.pageNum = 1;
  query.username = '';
  query.nickname = '';
  query.status = undefined;
  loadData();
}

function resetForm() {
  const userInfo = Session.get('userInfo') || {};
  Object.assign(form, {
    userId: undefined,
    username: '',
    password: '',
    nickname: '',
    realm: userInfo.realm || 'INTERNAL',
    actorType: userInfo.actorType || 'INTERNAL_USER',
    partyType: userInfo.partyType || 'INTERNAL_ORG',
    partyId: userInfo.partyId,
    email: '',
    phone: '',
    avatar: '',
    status: 1,
    remark: '',
  });
  formRef.value?.clearValidate();
}

function handleAdd() {
  resetForm();
  dialogVisible.value = true;
}

function handleEdit(row: IdentityUserVO) {
  Object.assign(form, {
    ...row,
    password: '',
    status: row.status ?? 1,
    partyId: row.partyId,
  });
  formRef.value?.clearValidate();
  dialogVisible.value = true;
}

async function handleSubmit() {
  if (!formRef.value) return;
  await formRef.value.validate();
  submitLoading.value = true;
  try {
    if (form.userId) {
      await userApi.update(form);
      ElMessage.success('修改成功');
    } else {
      await userApi.create(form);
      ElMessage.success('新增成功');
    }
    dialogVisible.value = false;
    await loadData();
  } finally {
    submitLoading.value = false;
  }
}

function handleDelete(row: IdentityUserVO) {
  if (!row.userId) return;
  ElMessageBox.confirm(`确认移除成员「${row.username}」?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await userApi.delete(row.userId!);
    ElMessage.success('移除成功');
    await loadData();
  }).catch(() => {});
}

function handleStatus(row: IdentityUserVO) {
  if (!row.userId) return;
  const nextStatus = row.status === 1 ? 0 : 1;
  const action = nextStatus === 1 ? '启用' : '禁用';
  ElMessageBox.confirm(`确认${action}成员「${row.username}」?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    await userApi.updateStatus(row.userId!, nextStatus);
    ElMessage.success(`${action}成功`);
    await loadData();
  }).catch(() => {});
}

function handleResetPassword(row: IdentityUserVO) {
  if (!row.userId) return;
  ElMessageBox.prompt(`请输入用户「${row.username}」的新密码`, '重置密码', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputType: 'password',
    inputPattern: /^.{6,200}$/,
    inputErrorMessage: '密码长度必须在6到200个字符之间',
  }).then(async ({ value }) => {
    await userApi.resetPassword(row.userId!, value);
    ElMessage.success('重置成功');
  }).catch(() => {});
}

async function handleAssignRoles(row: IdentityUserVO) {
  if (!row.memberId) {
    ElMessage.warning('当前账号没有机构成员身份，不能分配角色');
    return;
  }
  currentUser.value = row;
  assignDialogVisible.value = true;
  const [roles, assignedRoles] = await Promise.all([
    roleApi.list(),
    roleApi.getSubjectRoles(row.memberId),
  ]);
  roleOptions.value = roles;
  selectedRoleIds.value = assignedRoles.map((role) => role.roleId).filter(Boolean) as number[];
}

async function handleAssignSubmit() {
  if (!currentUser.value?.memberId) return;
  assignSubmitLoading.value = true;
  try {
    const userInfo = Session.get('userInfo') || {};
    await roleApi.assignSubjectRoles({
      subjectId: currentUser.value.memberId,
      appCode: userInfo.appCode || 'internal-admin',
      realm: currentUser.value.realm || userInfo.realm || 'INTERNAL',
      actorType: currentUser.value.actorType || userInfo.actorType || 'INTERNAL_USER',
      partyType: currentUser.value.partyType || userInfo.partyType || 'INTERNAL_ORG',
      partyId: currentUser.value.partyId || userInfo.partyId,
      roleIds: selectedRoleIds.value,
    });
    ElMessage.success('分配成功');
    assignDialogVisible.value = false;
  } finally {
    assignSubmitLoading.value = false;
  }
}

function formatTime(value?: string | number[]) {
  if (!value) return '';
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value;
    return `${year}-${pad(month)}-${pad(day)} ${pad(hour)}:${pad(minute)}:${pad(second)}`;
  }
  return value;
}

function pad(value: number) {
  return String(value).padStart(2, '0');
}

onMounted(loadData);
</script>

<style scoped lang="scss">
.user-container {
  padding: 0;
}

.card-header,
.assign-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.search-form {
  margin-bottom: 16px;

  :deep(.el-form-item) {
    margin-bottom: 0;
  }
}

.form-select {
  width: 100%;
}

.role-option {
  padding: 6px 0;
}
</style>
