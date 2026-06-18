<template>
  <div class="role-container">
    <el-card>
      <div class="action-toolbar">
        <div class="toolbar-left">
          <el-button
            type="primary"
            @click="handleAdd"
          >
            新增角色
          </el-button>
        </div>
      </div>
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
        >
          <template #default="{ row }">
            {{ formatDate(row.createTime) }}
          </template>
        </el-table-column>
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
              @click="handleDataScopes(row)"
            >
              数据权限
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
      >
        <template #default="{ data }">
          <span class="assign-tree-node">
            <span>{{ data.menuName }}</span>
            <el-tag
              v-if="shouldShowButtonType(data)"
              size="small"
              type="info"
              effect="plain"
            >
              {{ buttonTypeLabel(data.buttonType) }}
            </el-tag>
          </span>
        </template>
      </el-tree>
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

    <el-dialog
      v-model="dataScopeDialogVisible"
      title="角色数据权限"
      width="920px"
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
      <div class="data-scope-toolbar">
        <el-button
          type="primary"
          plain
          :disabled="!!editingDataScopeKey"
          @click="addDataScope"
        >
          新增数据权限
        </el-button>
      </div>
      <el-table
        v-loading="dataScopeLoading"
        :data="dataScopeTableRows"
        border
        size="small"
        :row-key="dataScopeRowKey"
        class="data-scope-table"
      >
        <el-table-column
          prop="resourceCode"
          label="数据资源"
          min-width="340"
        >
          <template #default="{ row }">
            <el-tree-select
              v-if="isEditingDataScope(row) && isNewDataScopeRow(row)"
              v-model="dataScopeEditRow.resourceCode"
              :data="dataScopeResourceTree"
              :loading="dataScopeResourceLoading"
              filterable
              clearable
              check-strictly
              default-expand-all
              node-key="value"
              :props="dataScopeResourceTreeProps"
              class="form-select"
              placeholder="请选择数据资源"
              data-test="data-scope-resource-tree"
            />
            <div
              v-else
              class="data-resource-cell"
            >
              <span>{{ dataScopeResourceName(row.resourceCode) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column
          prop="scopeMode"
          label="范围"
          width="170"
        >
          <template #default="{ row }">
            <el-select
              v-if="isEditingDataScope(row)"
              v-model="dataScopeEditRow.scopeMode"
              class="form-select"
              placeholder="请选择范围"
              data-test="data-scope-mode-select"
            >
              <el-option
                v-for="option in dataScopeModeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
            <span v-else>
              {{ dataScopeModeLabel(row.scopeMode) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          prop="scopeValues"
          label="范围值"
          min-width="220"
        >
          <template #default="{ row }">
            <el-tree-select
              v-if="isEditingDataScope(row) && dataScopeEditRow.scopeMode === 'ORG'"
              v-model="dataScopeEditRow.scopeValues"
              :data="orgTreeData"
              multiple
              check-strictly
              filterable
              clearable
              node-key="id"
              :props="{ label: 'orgName', children: 'children', value: 'id' }"
              class="form-select"
              placeholder="请选择组织范围"
            />
            <span v-else-if="isEditingDataScope(row)">
              -
            </span>
            <span v-else>
              {{ dataScopeValueLabel(row) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          prop="status"
          label="状态"
          width="120"
        >
          <template #default="{ row }">
            <el-switch
              v-if="isEditingDataScope(row)"
              v-model="dataScopeEditRow.status"
              :active-value="1"
              :inactive-value="0"
              active-text="启用"
              inactive-text="停用"
            />
            <DictTag
              v-else
              dict-code="sys_normal_disable"
              :value="row.status"
            />
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          width="130"
          fixed="right"
        >
          <template #default="{ row }">
            <template v-if="isEditingDataScope(row)">
              <el-button
                link
                type="primary"
                size="small"
                :loading="dataScopeSubmitLoading"
                data-test="data-scope-row-save"
                @click="saveDataScopeEditRow"
              >
                保存
              </el-button>
              <el-button
                link
                size="small"
                :disabled="dataScopeSubmitLoading"
                @click="cancelDataScopeEdit"
              >
                取消
              </el-button>
            </template>
            <template v-else>
              <el-button
                link
                type="primary"
                size="small"
                :disabled="!!editingDataScopeKey"
                @click="editDataScope(row)"
              >
                编辑
              </el-button>
              <el-button
                link
                type="danger"
                size="small"
                :disabled="!!editingDataScopeKey"
                @click="deleteDataScope(row)"
              >
                删除
              </el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="dataScopeDialogVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="SystemRole">
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type TreeInstance } from 'element-plus';
import type { ApiId } from '@mango/api-schema';
import DictTag from '@mango/common/components/DictTag/index.vue';
import { useDict } from '@mango/common/hooks/useDict';
import { formatDate } from '@mango/common/utils/formatTime';
import { Session } from '@mango/common/utils/storage';
import { appApi, type AppLoginContext, type AuthorizationApp } from '../../api/app';
import { roleApi, type DataScopeMode, type RoleDataScopeVO, type RoleVO } from '../../api/role';
import type { SysMenuVO } from '../../api/menu';
import { orgApi, type SysOrg } from '../../api/org';

interface DataScopeResourceOption {
  code: string;
  name: string;
  label: string;
  menuName: string;
}

interface DataScopeResourceTreeNode {
  value: string;
  label: string;
  name: string;
  code?: string;
  disabled?: boolean;
  children?: DataScopeResourceTreeNode[];
}

interface DataScopeEditRow {
  resourceCode: string;
  scopeMode: DataScopeMode;
  scopeValues: string[];
  status: number;
  isNew: boolean;
}

type DataScopeTableRow = RoleDataScopeVO | DataScopeEditRow;

const buttonTypeOptions = [
  { label: '表格按钮', value: 'TABLE' },
  { label: '非表格按钮', value: 'NON_TABLE' },
];

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
const dataScopeDialogVisible = ref(false);
const dataScopeLoading = ref(false);
const dataScopeSubmitLoading = ref(false);
const dataScopeResourceLoading = ref(false);
const dataScopes = ref<RoleDataScopeVO[]>([]);
const orgTreeData = ref<SysOrg[]>([]);
const dataScopeResourceOptions = ref<DataScopeResourceOption[]>([]);
const dataScopeResourceTree = ref<DataScopeResourceTreeNode[]>([]);
const editingDataScopeKey = ref('');
const dataScopeResourceTreeProps = {
  label: 'label',
  children: 'children',
  value: 'value',
  disabled: 'disabled',
};

const dataScopeModeOptions = [
  { label: '全部', value: 'ALL' },
  { label: '本人', value: 'SELF' },
  { label: '本人部门', value: 'SELF_ORG' },
  { label: '本人部门及下级', value: 'SELF_ORG_AND_CHILDREN' },
  { label: '指定组织', value: 'ORG' },
];

function buttonTypeLabel(type?: string) {
  return buttonTypeOptions.find((item) => item.value === type)?.label || type || '-';
}

function shouldShowButtonType(menu: SysMenuVO) {
  return menu.menuType === 3
    && Boolean(menu.buttonType)
    && !menu.menuName?.includes('列表');
}

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

const dataScopeEditRow = reactive<DataScopeEditRow>({
  resourceCode: '',
  scopeMode: 'SELF',
  scopeValues: [],
  status: 1,
  isNew: false,
});

const currentAppContexts = computed(() => {
  const app = appOptions.value.find((item) => item.appCode === form.appCode);
  return app?.loginContexts?.filter((item) => item.status === 1) || [];
});

const dataScopeTableRows = computed<DataScopeTableRow[]>(() => {
  if (dataScopeEditRow.isNew) {
    return [dataScopeEditRow, ...dataScopes.value];
  }
  return dataScopes.value;
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
    const apps = await appApi.runtime();
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
    const checkedKeys = menuTreeRef.value.getCheckedKeys(false) as ApiId[];
    const halfCheckedKeys = menuTreeRef.value.getHalfCheckedKeys() as ApiId[];
    const menuIds = Array.from(new Set([...checkedKeys, ...halfCheckedKeys].map(String)));
    await roleApi.assignMenus(currentRole.value.roleId, menuIds);
    ElMessage.success('分配成功');
    assignDialogVisible.value = false;
  } catch (error) {
    console.error('分配角色权限失败:', error);
  } finally {
    assignSubmitLoading.value = false;
  }
}

async function handleDataScopes(row: RoleVO) {
  if (!row.roleId) return;
  currentRole.value = row;
  dataScopeDialogVisible.value = true;
  resetDataScopeEditRow();
  await Promise.all([
    loadDataScopes(),
    loadDataScopeResources(row.appCode),
    loadOrgTree(),
  ]);
}

async function loadDataScopes() {
  if (!currentRole.value?.roleId) return;
  dataScopeLoading.value = true;
  try {
    dataScopes.value = await roleApi.getDataScopes(currentRole.value.roleId);
  } catch (error) {
    console.error('加载数据权限失败:', error);
  } finally {
    dataScopeLoading.value = false;
  }
}

async function loadOrgTree() {
  if (orgTreeData.value.length > 0) return;
  try {
    orgTreeData.value = await orgApi.tree({ parentId: '0', includeDisabled: true });
  } catch (error) {
    console.error('加载组织树失败:', error);
  }
}

async function loadDataScopeResources(appCode?: string) {
  dataScopeResourceLoading.value = true;
  try {
    const menus = await roleApi.getAssignableMenus(appCode);
    dataScopeResourceOptions.value = buildDataScopeResourceOptions(menus);
    dataScopeResourceTree.value = buildDataScopeResourceTree(menus);
  } catch (error) {
    dataScopeResourceOptions.value = [];
    dataScopeResourceTree.value = [];
    console.error('加载数据资源失败:', error);
  } finally {
    dataScopeResourceLoading.value = false;
  }
}

function buildDataScopeResourceOptions(menus: SysMenuVO[] = []): DataScopeResourceOption[] {
  const optionMap = new Map<string, DataScopeResourceOption>();
  const visit = (items: SysMenuVO[]) => {
    items.forEach((item) => {
      splitPermissions(item.permissions).forEach((code) => {
        if (isListResourceCode(code) && !optionMap.has(code)) {
          const name = `${listResourceName(item.menuName)} / ${code}`;
          optionMap.set(code, {
            code,
            name,
            label: name,
            menuName: item.menuName,
          });
        }
      });
      visit(item.children || []);
    });
  };
  visit(menus);
  return Array.from(optionMap.values()).sort((left, right) => left.code.localeCompare(right.code));
}

function buildDataScopeResourceTree(menus: SysMenuVO[] = []): DataScopeResourceTreeNode[] {
  const usedResourceCodes = new Set<string>();
  const buildNodes = (items: SysMenuVO[]): DataScopeResourceTreeNode[] => {
    return items
      .map((item) => {
        const children = buildNodes(item.children || []);
        const listResourceCode = splitPermissions(item.permissions)
          .find((code) => isListResourceCode(code) && !usedResourceCodes.has(code));
        if (listResourceCode) {
          usedResourceCodes.add(listResourceCode);
        }
        if (children.length === 0 && !listResourceCode) {
          return undefined;
        }
        return {
          value: listResourceCode || `menu:${item.menuId}`,
          label: listResourceCode ? `${listResourceName(item.menuName)} / ${listResourceCode}` : item.menuName,
          name: listResourceCode ? listResourceName(item.menuName) : item.menuName,
          code: listResourceCode,
          disabled: !listResourceCode,
          children,
        };
      })
      .filter((item): item is DataScopeResourceTreeNode => Boolean(item));
  };
  return buildNodes(menus);
}

function splitPermissions(permissions?: string) {
  return (permissions || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

function isListResourceCode(code: string) {
  return code.endsWith(':list');
}

function listResourceName(menuName: string) {
  const normalizedName = menuName
    .replace(/^查询/, '')
    .replace(/管理列表$/, '列表')
    .replace(/管理$/, '');
  return normalizedName.endsWith('列表') ? normalizedName : `${normalizedName}列表`;
}

function dataScopeResourceName(resourceCode?: string) {
  if (!resourceCode) return '-';
  const option = dataScopeResourceOptions.value.find((item) => item.code === resourceCode);
  return option?.name || resourceCode;
}

function dataScopeRowKey(row: DataScopeTableRow) {
  return isNewDataScopeRow(row) ? '__new__' : row.resourceCode;
}

function isEditingDataScope(row: DataScopeTableRow) {
  return editingDataScopeKey.value === dataScopeRowKey(row);
}

function isNewDataScopeRow(row: DataScopeTableRow): row is DataScopeEditRow {
  return 'isNew' in row && row.isNew;
}

function resetDataScopeEditRow() {
  Object.assign(dataScopeEditRow, {
    resourceCode: '',
    scopeMode: 'SELF',
    scopeValues: [],
    status: 1,
    isNew: false,
  });
  editingDataScopeKey.value = '';
}

function addDataScope() {
  Object.assign(dataScopeEditRow, {
    resourceCode: '',
    scopeMode: 'SELF_ORG',
    scopeValues: [],
    status: 1,
    isNew: true,
  });
  editingDataScopeKey.value = '__new__';
}

function editDataScope(row: RoleDataScopeVO) {
  Object.assign(dataScopeEditRow, {
    resourceCode: row.resourceCode,
    scopeMode: row.scopeMode || 'SELF',
    scopeValues: row.scopeValues || [],
    status: row.status ?? 1,
    isNew: false,
  });
  editingDataScopeKey.value = row.resourceCode;
}

function cancelDataScopeEdit() {
  resetDataScopeEditRow();
}

async function deleteDataScope(row: RoleDataScopeVO) {
  if (!currentRole.value?.roleId) return;
  await ElMessageBox.confirm(`确认删除资源「${row.resourceCode}」的数据权限配置?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  });
  await roleApi.deleteDataScope(currentRole.value.roleId, row.resourceCode);
  ElMessage.success('删除成功');
  await loadDataScopes();
  resetDataScopeEditRow();
}

async function saveDataScopeEditRow() {
  if (!currentRole.value?.roleId) return;
  if (!dataScopeEditRow.resourceCode) {
    ElMessage.warning('请选择数据资源');
    return;
  }
  if (!dataScopeEditRow.scopeMode) {
    ElMessage.warning('请选择数据范围');
    return;
  }
  dataScopeSubmitLoading.value = true;
  try {
    await roleApi.saveDataScope({
      roleId: currentRole.value.roleId,
      resourceCode: dataScopeEditRow.resourceCode,
      scopeMode: dataScopeEditRow.scopeMode,
      scopeValues: dataScopeEditRow.scopeMode === 'ORG' ? dataScopeEditRow.scopeValues.map(String) : [],
      includeChildren: dataScopeEditRow.scopeMode === 'SELF_ORG_AND_CHILDREN',
      status: dataScopeEditRow.status,
    });
    ElMessage.success('保存成功');
    await loadDataScopes();
    resetDataScopeEditRow();
  } catch (error) {
    console.error('保存数据权限失败:', error);
  } finally {
    dataScopeSubmitLoading.value = false;
  }
}

function dataScopeModeLabel(mode: DataScopeMode) {
  return dataScopeModeOptions.find((item) => item.value === mode)?.label || mode;
}

function dataScopeValueLabel(row: RoleDataScopeVO) {
  if (row.scopeMode === 'SELF_ORG') {
    return '成员主部门';
  }
  if (row.scopeMode === 'SELF_ORG_AND_CHILDREN') {
    return '成员主部门及下级';
  }
  if (row.scopeMode !== 'ORG' || !row.scopeValues?.length) {
    return '-';
  }
  const orgNameMap = buildOrgNameMap(orgTreeData.value);
  return row.scopeValues
    .map((value) => orgNameMap.get(String(value)) || String(value))
    .join(', ');
}

function buildOrgNameMap(orgs: SysOrg[]) {
  const nameMap = new Map<string, string>();
  const visit = (items: SysOrg[]) => {
    items.forEach((item) => {
      nameMap.set(String(item.id), item.orgName);
      visit(item.children || []);
    });
  };
  visit(orgs);
  return nameMap;
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

.data-scope-toolbar {
  display: flex;
  justify-content: flex-start;
  margin-bottom: 8px;
}

.data-scope-table {
  width: 100%;
}

.data-resource-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.4;
}

.assign-tree-node {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
</style>
