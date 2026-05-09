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
          prop="roleType"
          label="类型"
          width="100"
        >
          <template #default="{ row }">
            <DictTag
              dict-code="authorization_role_type"
              :value="row.roleType"
              size="small"
            />
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
            <DictTag
              dict-code="sys_normal_disable"
              :value="row.status"
            />
          </template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          label="创建时间"
        />
        <el-table-column
          label="操作"
          width="260"
        >
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              size="small"
              @click="handleAssignMenus(row)"
            >
              分配权限
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
          <el-select
            v-model="form.appCode"
            filterable
            class="form-select"
            placeholder="请选择应用"
            @change="handleAppChange"
          >
            <el-option
              v-for="app in appOptions"
              :key="app.appCode"
              :label="`${app.appName}（${app.appCode}）`"
              :value="app.appCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="登录上下文" prop="realm">
          <el-select
            v-model="selectedContextKey"
            filterable
            class="form-select"
            placeholder="请选择登录上下文"
            @change="handleContextChange"
          >
            <el-option
              v-for="context in currentAppContexts"
              :key="contextKey(context)"
              :label="contextLabel(context)"
              :value="contextKey(context)"
            />
          </el-select>
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
            <el-radio
              v-for="item in roleTypeOptions"
              :key="item.value"
              :label="Number(item.value)"
            >
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
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

    <el-dialog
      v-model="assignDialogVisible"
      title="分配角色权限"
      width="640px"
    >
      <div class="assign-header">
        <span>{{ currentRole?.roleName }}</span>
        <el-tag
          v-if="currentRole?.roleCode"
          effect="plain"
        >
          {{ currentRole.roleCode }}
        </el-tag>
      </div>
      <el-tree
        ref="menuTreeRef"
        v-loading="assignLoading"
        :data="assignableMenus"
        node-key="menuId"
        show-checkbox
        default-expand-all
        :props="{ label: 'menuName', children: 'children' }"
      />
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

<script setup lang="ts" name="SystemRole">
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type TreeInstance } from 'element-plus';
import { DictTag, Session, useDict } from '@mango/common';
import { appApi, type AppLoginContext, type AuthorizationApp } from '../../api/app';
import { roleApi, type RoleVO } from '../../api/role';
import type { SysMenuVO } from '../../api/menu';

const { options: roleTypeOptions } = useDict('authorization_role_type');
const { options: statusOptions } = useDict('sys_normal_disable');
const { getLabel: getRealmLabel } = useDict('auth_realm');
const { getLabel: getActorTypeLabel } = useDict('auth_actor_type');

const loading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);
const tableData = ref<RoleVO[]>([]);
const appOptions = ref<AuthorizationApp[]>([]);
const selectedContextKey = ref('');
const formRef = ref<FormInstance>();
const assignDialogVisible = ref(false);
const assignLoading = ref(false);
const assignSubmitLoading = ref(false);
const currentRole = ref<RoleVO>();
const assignableMenus = ref<SysMenuVO[]>([]);
const menuTreeRef = ref<TreeInstance>();

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

const currentAppContexts = computed(() => {
  const app = appOptions.value.find((item) => item.appCode === form.appCode);
  return app?.loginContexts?.filter((item) => item.status === 1) || [];
});

async function loadData() {
  loading.value = true;
  try {
    const [roles, apps] = await Promise.all([
      roleApi.list(),
      loadAppOptions(),
    ]);
    tableData.value = roles;
    appOptions.value = apps;
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
  selectDefaultContext();
  formRef.value?.clearValidate();
}

async function loadAppOptions() {
  try {
    const apps = await appApi.list();
    if (apps.length > 0) {
      return apps;
    }
  } catch (error) {
    console.warn('应用列表不可访问，使用当前登录应用上下文:', error);
  }
  const userInfo = Session.get('userInfo') || {};
  return [{
    appCode: userInfo.appCode || 'internal-admin',
    appName: '当前应用',
    status: 1,
    loginContexts: [{
      appCode: userInfo.appCode || 'internal-admin',
      realm: userInfo.realm || 'INTERNAL',
      actorType: userInfo.actorType || 'INTERNAL_USER',
      defaultFlag: 1,
      status: 1,
      sort: 0,
    }],
  }];
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
  selectedContextKey.value = contextKey({
    realm: form.realm,
    actorType: form.actorType || '',
    defaultFlag: 0,
    status: 1,
  });
  formRef.value?.clearValidate();
  dialogVisible.value = true;
};

const handleAppChange = () => {
  selectDefaultContext();
};

const handleContextChange = (value: string | number | boolean | undefined) => {
  const key = String(value || '');
  const context = currentAppContexts.value.find((item) => contextKey(item) === key);
  if (!context) return;
  form.realm = context.realm;
  form.actorType = context.actorType;
};

function selectDefaultContext() {
  const contexts = currentAppContexts.value;
  const context = contexts.find((item) => item.defaultFlag === 1) || contexts[0];
  if (!context) {
    selectedContextKey.value = '';
    return;
  }
  selectedContextKey.value = contextKey(context);
  form.realm = context.realm;
  form.actorType = context.actorType;
}

function contextKey(context: Pick<AppLoginContext, 'realm' | 'actorType'>) {
  return `${context.realm}:${context.actorType}`;
}

function contextLabel(context: Pick<AppLoginContext, 'realm' | 'actorType'>) {
  return `${getRealmLabel(context.realm)} / ${getActorTypeLabel(context.actorType)}`;
}

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

async function handleAssignMenus(row: RoleVO) {
  if (!row.roleId) return;
  currentRole.value = row;
  assignDialogVisible.value = true;
  assignLoading.value = true;
  try {
    const [menus, checkedMenuIds] = await Promise.all([
      roleApi.getAssignableMenus(row.appCode),
      roleApi.getMenuIds(row.roleId),
    ]);
    assignableMenus.value = menus;
    await nextTick();
    menuTreeRef.value?.setCheckedKeys(checkedMenuIds, false);
  } catch (error) {
    console.error('加载角色权限失败:', error);
  } finally {
    assignLoading.value = false;
  }
}

async function handleAssignSubmit() {
  if (!currentRole.value?.roleId || !menuTreeRef.value) return;
  assignSubmitLoading.value = true;
  try {
    const checkedKeys = menuTreeRef.value.getCheckedKeys(false) as number[];
    const halfCheckedKeys = menuTreeRef.value.getHalfCheckedKeys() as number[];
    const menuIds = Array.from(new Set([...checkedKeys, ...halfCheckedKeys].map(Number)));
    await roleApi.assignMenus(currentRole.value.roleId, menuIds);
    ElMessage.success('分配成功');
    assignDialogVisible.value = false;
  } catch (error) {
    console.error('分配角色权限失败:', error);
  } finally {
    assignSubmitLoading.value = false;
  }
}

onMounted(() => {
  loadData();
});
</script>

<style scoped lang="scss">
.role-container {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-select {
  width: 100%;
}

.assign-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
</style>
