<template>
  <div class="user-container">
    <div class="user-page-layout">
      <aside class="org-filter-panel">
        <el-card class="layout-card">
          <div class="org-filter-header">
            <span>部门组织</span>
            <el-button
              link
              type="primary"
              @click="loadOrgTree"
            >
              刷新
            </el-button>
          </div>
          <el-input
            v-model="orgKeyword"
            placeholder="请输入部门名称"
            clearable
            class="org-filter-search"
          />
          <el-tree
            ref="orgTreeRef"
            v-loading="orgLoading"
            class="org-tree"
            :data="orgTreeData"
            node-key="id"
            highlight-current
            default-expand-all
            :filter-node-method="filterOrgNode"
            :props="{ label: 'orgName', children: 'children' }"
            :expand-on-click-node="false"
            @node-click="handleOrgClick"
          >
            <template #default="{ data }">
              <span class="org-tree-node">
                <span>{{ data.orgName }}</span>
                <el-tag
                  v-if="data.orgType"
                  size="small"
                  effect="plain"
                >
                  {{ orgTypeLabel(data.orgType) }}
                </el-tag>
              </span>
            </template>
          </el-tree>
        </el-card>
      </aside>

      <section class="user-list-panel">
        <el-card class="layout-card">
          <div class="current-org-bar">
            <div>
              <span class="current-org-title">{{ selectedOrg?.orgName || '全部成员' }}</span>
              <span class="current-org-subtitle">
                {{ selectedOrg ? '当前仅显示该组织下成员，可在此设置部门主管' : '请选择左侧部门查看部门成员' }}
              </span>
            </div>
            <el-button
              v-if="selectedOrg"
              @click="clearOrgFilter"
            >
              查看全部
            </el-button>
          </div>

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
            <el-form-item label="手机号">
              <el-input
                v-model="query.phone"
                placeholder="请输入手机号"
                clearable
              />
            </el-form-item>
            <el-form-item label="状态">
              <DictSelect
                v-model="query.status"
                dict-type="sys_normal_disable"
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
                新增成员
              </el-button>
              <el-button
                v-if="selectedOrg"
                @click="handleAddOrgMember"
              >
                加入当前部门
              </el-button>
            </div>
          </div>

          <el-table
            v-loading="loading"
            :data="tableData"
            stripe
            class="user-table"
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
          v-if="selectedOrg"
          label="部门岗位"
          min-width="150"
        >
          <template #default="{ row }">
            <el-tag
              v-if="row.postName"
              effect="plain"
            >
              {{ row.postName }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column
          v-if="selectedOrg"
          label="部门主管"
          width="110"
        >
          <template #default="{ row }">
            <el-tag
              :type="row.orgLeaderFlag ? 'success' : 'info'"
              effect="light"
            >
              {{ row.orgLeaderFlag ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          v-if="selectedOrg"
          label="主部门"
          width="100"
        >
          <template #default="{ row }">
            <el-tag
              :type="row.primaryOrgFlag ? 'primary' : 'info'"
              effect="plain"
            >
              {{ row.primaryOrgFlag ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
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
          :width="selectedOrg ? 430 : 300"
          fixed="right"
        >
          <template #default="{ row }">
            <el-button
              v-if="selectedOrg"
              link
              type="primary"
              size="small"
              @click="handleEditOrgPost(row)"
            >
              岗位
            </el-button>
            <el-button
              v-if="selectedOrg && !row.orgLeaderFlag"
              link
              type="success"
              size="small"
              @click="handleSetLeader(row)"
            >
              设为主管
            </el-button>
            <el-button
              v-if="selectedOrg && row.orgLeaderFlag"
              link
              type="warning"
              size="small"
              @click="handleUnsetLeader(row)"
            >
              取消主管
            </el-button>
            <el-button
              v-if="selectedOrg && !row.primaryOrgFlag"
              link
              type="primary"
              size="small"
              @click="handleSetPrimaryOrg(row)"
            >
              设主部门
            </el-button>
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
      </section>
    </div>

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
            <el-input
              v-model="form.partyId"
              class="form-select"
              placeholder="请输入归属主体ID"
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

    <el-dialog
      v-model="orgMemberDialogVisible"
      :title="orgMemberForm.relationId ? '调整部门岗位' : '加入当前部门'"
      width="520px"
    >
      <el-form
        ref="orgMemberFormRef"
        :model="orgMemberForm"
        :rules="orgMemberRules"
        label-width="100px"
      >
        <el-form-item
          v-if="!orgMemberForm.relationId"
          label="成员"
          prop="memberId"
        >
          <el-select
            v-model="orgMemberForm.memberId"
            filterable
            remote
            reserve-keyword
            placeholder="请输入用户名或姓名"
            :remote-method="searchCandidateUsers"
            :loading="candidateLoading"
            class="form-select"
          >
            <el-option
              v-for="item in candidateUsers"
              :key="item.memberId"
              :label="`${item.nickname || item.username}（${item.username}）`"
              :value="item.memberId"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          label="部门岗位"
          prop="postId"
        >
          <el-select
            v-model="orgMemberForm.postId"
            placeholder="请选择岗位"
            filterable
            clearable
            class="form-select"
          >
            <el-option
              v-for="item in postOptions"
              :key="item.id"
              :label="`${item.postName}（${item.postCode}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="主部门">
          <el-switch v-model="orgMemberForm.primaryFlag" />
        </el-form-item>
        <el-form-item label="部门主管">
          <el-switch v-model="orgMemberForm.leaderFlag" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="orgMemberDialogVisible = false">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="orgMemberSubmitLoading"
          @click="handleOrgMemberSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="SystemUser">
import { onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type TreeInstance } from 'element-plus';
import type { ApiId } from '@mango/api-schema';
import DictSelect from '@mango/common/components/DictSelect/index.vue';
import DictTag from '@mango/common/components/DictTag/index.vue';
import Pagination from '@mango/common/components/Pagination/index.vue';
import { useDict } from '@mango/common/hooks/useDict';
import { Session } from '@mango/common/utils/storage';
import { orgApi, type OrgMemberVO, type SysOrg } from '../../api/org';
import { postApi, type PostVO } from '../../api/post';
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
const orgMemberDialogVisible = ref(false);
const orgLoading = ref(false);
const candidateLoading = ref(false);
const orgMemberSubmitLoading = ref(false);
const tableData = ref<IdentityUserVO[]>([]);
const roleOptions = ref<RoleVO[]>([]);
const postOptions = ref<PostVO[]>([]);
const candidateUsers = ref<IdentityUserVO[]>([]);
const orgTreeData = ref<SysOrg[]>([]);
const selectedRoleIds = ref<ApiId[]>([]);
const total = ref(0);
const formRef = ref<FormInstance>();
const orgTreeRef = ref<TreeInstance>();
const orgMemberFormRef = ref<FormInstance>();
const currentUser = ref<IdentityUserVO>();
const selectedOrg = ref<SysOrg>();
const orgKeyword = ref('');

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  username: '',
  nickname: '',
  phone: '',
  status: undefined as number | undefined,
  orgId: undefined as ApiId | undefined,
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

const orgMemberForm = reactive({
  relationId: undefined as ApiId | undefined,
  memberId: undefined as ApiId | undefined,
  postId: undefined as ApiId | undefined,
  primaryFlag: false,
  leaderFlag: false,
});

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realm: [{ required: true, message: '请选择登录域', trigger: 'change' }],
  actorType: [{ required: true, message: '请选择操作者类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
};

const orgMemberRules: FormRules = {
  memberId: [{ required: true, message: '请选择成员', trigger: 'change' }],
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

async function loadOrgTree() {
  orgLoading.value = true;
  try {
    orgTreeData.value = await orgApi.tree({ parentId: '0', includeDisabled: true });
  } finally {
    orgLoading.value = false;
  }
}

async function loadPostOptions() {
  const data = await postApi.page({ pageNum: 1, pageSize: 500, postStatus: '1' });
  postOptions.value = data.list;
}

function handleSearch() {
  query.pageNum = 1;
  loadData();
}

function handleReset() {
  query.pageNum = 1;
  query.username = '';
  query.nickname = '';
  query.phone = '';
  query.status = undefined;
  query.orgId = selectedOrg.value?.id;
  loadData();
}

async function handleOrgClick(row: SysOrg) {
  selectedOrg.value = row;
  query.orgId = row.id;
  query.pageNum = 1;
  await loadData();
}

async function clearOrgFilter() {
  selectedOrg.value = undefined;
  query.orgId = undefined;
  query.pageNum = 1;
  await loadData();
}

function filterOrgNode(value: string, data: SysOrg) {
  return !value || data.orgName?.includes(value) || data.orgCode?.includes(value);
}

function orgTypeLabel(type?: number) {
  const labels: Record<number, string> = {
    1: '集团',
    2: '公司',
    3: '部门',
    4: '小组',
  };
  return type ? labels[type] || '组织' : '组织';
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
  selectedRoleIds.value = assignedRoles.map((role) => role.roleId).filter(Boolean) as ApiId[];
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

async function handleAddOrgMember() {
  if (!selectedOrg.value) return;
  await ensureOrgMemberOptions();
  Object.assign(orgMemberForm, {
    relationId: undefined,
    memberId: undefined,
    postId: defaultPostId(),
    primaryFlag: false,
    leaderFlag: false,
  });
  candidateUsers.value = [];
  orgMemberFormRef.value?.clearValidate();
  orgMemberDialogVisible.value = true;
}

async function handleEditOrgPost(row: IdentityUserVO) {
  if (!row.orgRelationId) {
    ElMessage.warning('当前成员没有部门关系，不能调整岗位');
    return;
  }
  await ensureOrgMemberOptions();
  Object.assign(orgMemberForm, {
    relationId: row.orgRelationId,
    memberId: row.memberId,
    postId: row.postId,
    primaryFlag: Boolean(row.primaryOrgFlag),
    leaderFlag: Boolean(row.orgLeaderFlag),
  });
  orgMemberFormRef.value?.clearValidate();
  orgMemberDialogVisible.value = true;
}

async function handleSetLeader(row: IdentityUserVO) {
  if (!row.orgRelationId) {
    return;
  }
  await orgApi.updateMember({
    relationId: row.orgRelationId,
    postId: row.postId,
    primaryFlag: Boolean(row.primaryOrgFlag),
    leaderFlag: true,
  });
  ElMessage.success('已设为主管');
  await loadData();
}

async function handleUnsetLeader(row: IdentityUserVO) {
  if (!row.orgRelationId) return;
  await orgApi.updateMember({
    relationId: row.orgRelationId,
    postId: row.postId,
    primaryFlag: Boolean(row.primaryOrgFlag),
    leaderFlag: false,
  });
  ElMessage.success('已取消主管');
  await loadData();
}

async function handleSetPrimaryOrg(row: IdentityUserVO) {
  if (!row.orgRelationId) return;
  await orgApi.updateMember({
    relationId: row.orgRelationId,
    postId: row.postId,
    primaryFlag: true,
    leaderFlag: Boolean(row.orgLeaderFlag),
  });
  ElMessage.success('已设置主部门');
  await loadData();
}

async function handleOrgMemberSubmit() {
  if (!selectedOrg.value || !orgMemberFormRef.value) return;
  await orgMemberFormRef.value.validate();
  orgMemberSubmitLoading.value = true;
  try {
    if (orgMemberForm.relationId) {
      await orgApi.updateMember({
        relationId: orgMemberForm.relationId,
        postId: orgMemberForm.postId,
        primaryFlag: orgMemberForm.primaryFlag,
        leaderFlag: orgMemberForm.leaderFlag,
      });
      ElMessage.success('调整成功');
    } else if (orgMemberForm.memberId) {
      await orgApi.addMember(selectedOrg.value.id, {
        memberId: orgMemberForm.memberId,
        postId: orgMemberForm.postId,
        primaryFlag: orgMemberForm.primaryFlag,
        leaderFlag: orgMemberForm.leaderFlag,
      });
      ElMessage.success('加入成功');
    }
    orgMemberDialogVisible.value = false;
    await loadData();
  } finally {
    orgMemberSubmitLoading.value = false;
  }
}

async function searchCandidateUsers(keyword: string) {
  candidateLoading.value = true;
  try {
    const data = await userApi.page({
      pageNum: 1,
      pageSize: 50,
      username: keyword,
      nickname: keyword,
    });
    const existingMemberIds = new Set(tableData.value.map(item => item.memberId).filter(Boolean));
    candidateUsers.value = data.list.filter(item => item.memberId && !existingMemberIds.has(item.memberId));
  } finally {
    candidateLoading.value = false;
  }
}

async function ensureOrgMemberOptions() {
  if (!postOptions.value.length) {
    await loadPostOptions();
  }
}

function defaultPostId() {
  return postOptions.value[0]?.id;
}

function formatTime(value?: string) {
  if (!value) return '';
  return value;
}

watch(orgKeyword, (value) => {
  orgTreeRef.value?.filter(value);
});

onMounted(async () => {
  await Promise.all([loadOrgTree(), loadPostOptions()]);
  await loadData();
});
</script>

<style scoped lang="scss">
.user-container {
  padding: 0;
}

.user-page-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.layout-card {
  border: 0;

  :deep(.el-card__body) {
    padding: 20px;
  }
}

.org-filter-panel {
  min-width: 0;
}

.user-list-panel {
  min-width: 0;
}

.org-filter-header,
.card-header,
.assign-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.org-filter-header {
  margin-bottom: 10px;
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.org-filter-search {
  margin-bottom: 12px;
}

.org-tree {
  max-height: calc(100vh - 230px);
  overflow: auto;

  :deep(.el-tree-node__content) {
    min-height: 32px;
  }
}

.org-tree-node {
  display: inline-flex;
  align-items: center;
  min-width: 0;
  gap: 6px;
}

.org-tree-node > span:first-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.current-org-bar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.current-org-title {
  display: block;
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 700;
  line-height: 1.4;
}

.current-org-subtitle {
  display: block;
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  margin-bottom: 14px;

  :deep(.el-form-item) {
    margin-bottom: 0;
  }

  :deep(.el-input),
  :deep(.el-select) {
    width: 190px;
  }
}

.action-toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.toolbar-left {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.user-table {
  width: 100%;
}

.form-select {
  width: 100%;
}

.role-option {
  padding: 6px 0;
}

@media (max-width: 1200px) {
  .user-page-layout {
    grid-template-columns: 280px minmax(0, 1fr);
  }
}

@media (max-width: 900px) {
  .user-page-layout {
    grid-template-columns: 1fr;
  }

  .org-tree {
    max-height: 360px;
  }
}
</style>
