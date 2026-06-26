<template>
  <div class="notice-announcement-page">
    <div class="page-header">
      <h1>公告管理</h1>
      <div class="page-actions">
        <el-button :loading="loading" @click="loadData">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增公告</el-button>
      </div>
    </div>

    <el-card shadow="never">
      <div class="filter-bar">
        <el-input v-model="query.keyword" clearable placeholder="搜索标题或内容" class="filter-control" @keyup.enter="loadData" />
        <el-select v-model="query.status" clearable placeholder="状态" class="filter-control">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="已下线" value="OFFLINE" />
        </el-select>
        <el-button @click="loadData">查询</el-button>
      </div>

      <el-table :data="rows" border stripe v-loading="loading">
        <el-table-column prop="title" label="公告标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="设置" width="150">
          <template #default="{ row }">
            <el-tag v-if="row.pinned" effect="plain">置顶</el-tag>
            <el-tag v-if="row.confirmRequired" effect="plain" type="warning">需确认</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="接收/已读/确认" width="150">
          <template #default="{ row }">
            {{ row.stats?.recipientCount || 0 }}/{{ row.stats?.readCount || 0 }}/{{ row.stats?.confirmedCount || 0 }}
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="170" show-overflow-tooltip />
        <el-table-column prop="updatedAt" label="更新时间" width="170" show-overflow-tooltip />
        <el-table-column label="操作" width="210" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="primary" @click="publish(row)">发布</el-button>
            <el-button v-if="row.status === 'PUBLISHED'" link type="warning" @click="offline(row)">下线</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="formVisible" :title="editingId ? '编辑公告' : '新增公告'" width="820px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="104px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="200" show-word-limit clearable />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" />
        </el-form-item>
        <el-form-item label="有效时间">
          <el-date-picker
            v-model="validRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            class="full-width"
          />
        </el-form-item>
        <el-form-item label="发布设置">
          <el-checkbox v-model="form.pinned">置顶</el-checkbox>
          <el-checkbox v-model="form.confirmRequired">需要确认</el-checkbox>
          <el-checkbox v-model="form.syncMessageEnabled">生成系统消息提醒</el-checkbox>
        </el-form-item>
        <el-form-item label="发布对象" prop="targets">
          <div class="target-panel">
            <el-switch v-model="allUsers" active-text="全员" @change="handleAllUsersChange" />
            <ParticipantSelector
              v-if="!allUsers"
              v-model="participantValue"
              :user-options="participantUserOptions"
              :role-options="participantRoleOptions"
              :org-tree-options="orgTreeOptions"
              :target-loading="participantLoading"
              @ensure-users="ensureUsersLoaded"
              @ensure-orgs="loadOrgTree"
              @ensure-roles="loadRoles"
            />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button :loading="saving" @click="save(false)">保存草稿</el-button>
        <el-button type="primary" :loading="saving" @click="save(true)">保存并发布</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="公告详情" width="760px" destroy-on-close>
      <el-descriptions v-if="current" :column="2" border>
        <el-descriptions-item label="标题">{{ current.title }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusLabel(current.status) }}</el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ current.publishTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="有效期">{{ validText(current) }}</el-descriptions-item>
        <el-descriptions-item label="接收人数">{{ current.stats?.recipientCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="已读人数">{{ current.stats?.readCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="待确认">{{ current.stats?.pendingConfirmCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="已确认">{{ current.stats?.confirmedCount || 0 }}</el-descriptions-item>
      </el-descriptions>
      <div v-if="current" class="content-box">{{ current.content }}</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage, ElMessageBox } from 'element-plus';
import { ParticipantSelector } from '@mango/system';
import type { ParticipantOrgTreeOption, ParticipantSelectorValue, ParticipantTargetOption } from '@mango/system';
import {
  createAnnouncement,
  getAnnouncements,
  getIdentityUsers,
  getNoticeOrgTree,
  getNoticeRoles,
  offlineAnnouncement,
  publishAnnouncement,
  updateAnnouncement,
  type NoticeIdentityUser,
  type NoticeOrgNode,
  type NoticeRole,
} from '../../api/notice';
import type {
  NoticeAnnouncement,
  NoticeAnnouncementStatus,
  NoticeAnnouncementTargetCommand,
  SaveNoticeAnnouncementCommand,
} from '../../types/notice';

const loading = ref(false);
const saving = ref(false);
const rows = ref<NoticeAnnouncement[]>([]);
const query = reactive<{ keyword?: string; status?: NoticeAnnouncementStatus }>({});
const formVisible = ref(false);
const detailVisible = ref(false);
const editingId = ref('');
const current = ref<NoticeAnnouncement>();
const formRef = ref<FormInstance>();
const allUsers = ref(false);
const validRange = ref<[string, string] | []>([]);
const userLoading = ref(false);
const orgLoading = ref(false);
const roleLoading = ref(false);
const userOptions = ref<NoticeIdentityUser[]>([]);
const orgTreeOptions = ref<ParticipantOrgTreeOption[]>([]);
const roleOptions = ref<NoticeRole[]>([]);

const form = reactive<SaveNoticeAnnouncementCommand>({
  title: '',
  content: '',
  pinned: false,
  confirmRequired: false,
  syncMessageEnabled: true,
  targets: [],
});

const rules: FormRules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
  targets: [{
    validator: (_rule, _value, callback) => {
      if (allUsers.value || (form.targets && form.targets.length > 0)) {
        callback();
      } else {
        callback(new Error('请选择发布对象'));
      }
    },
    trigger: 'change',
  }],
};

const participantValue = computed<ParticipantSelectorValue>({
  get: () => ({
    userIds: targetIds('USER'),
    orgIds: targetIds('ORG'),
    roleIds: targetIds('ROLE'),
  }),
  set: value => {
    form.targets = [
      ...targetsOf('USER', value.userIds || [], participantUserOptions.value),
      ...targetsOf('ORG', value.orgIds || [], flattenOrgOptions(orgTreeOptions.value)),
      ...targetsOf('ROLE', value.roleIds || [], participantRoleOptions.value),
    ];
    formRef.value?.clearValidate('targets');
  },
});

const participantLoading = computed(() => ({
  users: userLoading.value,
  orgs: orgLoading.value,
  roles: roleLoading.value,
}));

const participantUserOptions = computed<ParticipantTargetOption[]>(() => userOptions.value
  .filter(item => item.userId !== undefined)
  .map(item => ({ value: String(item.userId), label: item.nickname || item.username || String(item.userId) })));

const participantRoleOptions = computed<ParticipantTargetOption[]>(() => roleOptions.value
  .filter(item => item.roleId !== undefined)
  .map(item => ({ value: String(item.roleId), label: item.roleName || String(item.roleId) })));

async function loadData() {
  loading.value = true;
  try {
    const result = await getAnnouncements({ pageNum: 1, pageSize: 50, ...query });
    rows.value = result.list || [];
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  resetForm();
  formVisible.value = true;
  ensureUsersLoaded();
}

function openEdit(row: NoticeAnnouncement) {
  resetForm();
  editingId.value = row.id;
  form.title = row.title;
  form.content = row.content;
  form.pinned = Boolean(row.pinned);
  form.confirmRequired = Boolean(row.confirmRequired);
  form.syncMessageEnabled = row.syncMessageEnabled !== false;
  validRange.value = row.validStartTime && row.validEndTime ? [row.validStartTime, row.validEndTime] : [];
  allUsers.value = row.targets?.some(target => target.targetType === 'ALL') || false;
  form.targets = row.targets?.map(target => ({
    targetType: target.targetType,
    targetId: target.targetId,
    targetName: target.targetName,
    includeChildren: target.includeChildren,
  })) || [];
  formVisible.value = true;
  ensureUsersLoaded();
  loadOrgTree();
  loadRoles();
}

function openDetail(row: NoticeAnnouncement) {
  current.value = row;
  detailVisible.value = true;
}

async function save(shouldPublish: boolean) {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) {
    return;
  }
  saving.value = true;
  try {
    applyValidRange();
    const payload = normalizePayload();
    const saved = editingId.value
      ? await updateAnnouncement(editingId.value, payload)
      : await createAnnouncement(payload);
    if (shouldPublish) {
      await publishAnnouncement(saved.id);
      ElMessage.success('公告已发布');
    } else {
      ElMessage.success('公告已保存');
    }
    formVisible.value = false;
    await loadData();
  } finally {
    saving.value = false;
  }
}

async function publish(row: NoticeAnnouncement) {
  await ElMessageBox.confirm(`确认发布公告「${row.title}」？`, '发布公告', { type: 'warning' });
  await publishAnnouncement(row.id);
  ElMessage.success('公告已发布');
  loadData();
}

async function offline(row: NoticeAnnouncement) {
  await ElMessageBox.confirm(`确认下线公告「${row.title}」？`, '下线公告', { type: 'warning' });
  await offlineAnnouncement(row.id);
  ElMessage.success('公告已下线');
  loadData();
}

function resetForm() {
  editingId.value = '';
  allUsers.value = false;
  validRange.value = [];
  Object.assign(form, {
    title: '',
    content: '',
    pinned: false,
    confirmRequired: false,
    syncMessageEnabled: true,
    targets: [],
  });
  formRef.value?.clearValidate();
}

function normalizePayload(): SaveNoticeAnnouncementCommand {
  return {
    ...form,
    targets: allUsers.value ? [{ targetType: 'ALL', targetName: '全员' }] : form.targets,
  };
}

function applyValidRange() {
  form.validStartTime = validRange.value?.[0];
  form.validEndTime = validRange.value?.[1];
}

function handleAllUsersChange(value: string | number | boolean) {
  if (Boolean(value)) {
    form.targets = [{ targetType: 'ALL', targetName: '全员' }];
  } else {
    form.targets = [];
  }
  formRef.value?.clearValidate('targets');
}

async function ensureUsersLoaded() {
  if (userOptions.value.length > 0) {
    return;
  }
  userLoading.value = true;
  try {
    const result = await getIdentityUsers('', { pageNum: 1, pageSize: 20, status: 1 });
    userOptions.value = result.list || [];
  } finally {
    userLoading.value = false;
  }
}

async function loadOrgTree() {
  if (orgTreeOptions.value.length > 0) {
    return;
  }
  orgLoading.value = true;
  try {
    const result = await getNoticeOrgTree({ parentId: '0', includeDisabled: false });
    orgTreeOptions.value = toOrgTreeOptions(result || []);
  } finally {
    orgLoading.value = false;
  }
}

async function loadRoles() {
  if (roleOptions.value.length > 0) {
    return;
  }
  roleLoading.value = true;
  try {
    const result = await getNoticeRoles();
    roleOptions.value = (result || []).filter(item => item.roleId !== undefined && item.status !== 0);
  } finally {
    roleLoading.value = false;
  }
}

function targetIds(type: NoticeAnnouncementTargetCommand['targetType']) {
  return (form.targets || [])
    .filter(target => target.targetType === type && target.targetId)
    .map(target => String(target.targetId));
}

function targetsOf(type: NoticeAnnouncementTargetCommand['targetType'], ids: string[], options: ParticipantTargetOption[]) {
  return ids.map(id => ({
    targetType: type,
    targetId: id,
    targetName: options.find(option => option.value === id)?.label,
    includeChildren: false,
  }));
}

function toOrgTreeOptions(nodes: NoticeOrgNode[]): ParticipantOrgTreeOption[] {
  return nodes.map(node => ({
    value: String(node.id),
    label: node.orgName || String(node.id),
    children: node.children?.length ? toOrgTreeOptions(node.children) : undefined,
  }));
}

function flattenOrgOptions(nodes: ParticipantOrgTreeOption[]): ParticipantTargetOption[] {
  return nodes.flatMap(node => [{ value: node.value, label: node.label }, ...flattenOrgOptions(node.children || [])]);
}

function statusLabel(status: NoticeAnnouncementStatus) {
  return ({ DRAFT: '草稿', PUBLISHED: '已发布', OFFLINE: '已下线' } as Record<NoticeAnnouncementStatus, string>)[status] || status;
}

function statusTag(status: NoticeAnnouncementStatus) {
  return status === 'PUBLISHED' ? 'success' : status === 'OFFLINE' ? 'info' : 'warning';
}

function validText(row: NoticeAnnouncement) {
  if (!row.validStartTime && !row.validEndTime) {
    return '长期有效';
  }
  return `${row.validStartTime || '-'} 至 ${row.validEndTime || '-'}`;
}

onMounted(loadData);
</script>

<style scoped>
.notice-announcement-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header,
.page-actions,
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-header {
  justify-content: space-between;
}

.page-header h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.filter-control {
  width: 220px;
}

.full-width {
  width: 100%;
}

.target-panel {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.content-box {
  margin-top: 16px;
  padding: 12px;
  min-height: 120px;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  white-space: pre-wrap;
}
</style>
