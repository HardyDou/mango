<template>
  <div class="workflow-root-config">
    <el-form-item label="发起人范围">
      <WorkflowParticipantSelector
        :model-value="participantValue"
        :user-options="userOptions"
        :role-options="roleOptions"
        :post-options="postOptions"
        :org-tree-options="orgTreeOptions"
        :target-loading="targetLoading"
        placeholder="请选择可发起的人员、部门、岗位或角色"
        search-placeholder="搜索可发起对象"
        @update:model-value="updateParticipants"
        @ensure-users="$emit('ensure-users')"
        @ensure-roles="$emit('ensure-roles')"
        @ensure-posts="$emit('ensure-posts')"
        @ensure-orgs="$emit('ensure-orgs')"
      />
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { ApprovalOrgTreeOption, ApprovalTargetOption } from './types';
import WorkflowParticipantSelector, { type WorkflowParticipantValue } from './WorkflowParticipantSelector.vue';

export interface WorkflowRootNodeConfig {
  scopeType?: 'ALL' | 'COMPOSITE' | 'SPECIFIED_USER' | 'SPECIFIED_ORG' | string;
  userIds?: string[];
  orgIds?: string[];
  roleIds?: string[];
  postIds?: string[];
}

const props = defineProps<{
  config: WorkflowRootNodeConfig;
  userOptions: ApprovalTargetOption[];
  roleOptions: ApprovalTargetOption[];
  postOptions: ApprovalTargetOption[];
  orgTreeOptions: ApprovalOrgTreeOption[];
  targetLoading: { users: boolean; roles: boolean; posts: boolean; orgs: boolean };
}>();

const emit = defineEmits<{
  update: [patch: Partial<WorkflowRootNodeConfig>];
  'ensure-users': [];
  'ensure-roles': [];
  'ensure-posts': [];
  'ensure-orgs': [];
}>();

const participantValue = computed<WorkflowParticipantValue>(() => ({
  userIds: props.config.userIds || [],
  orgIds: props.config.orgIds || [],
  roleIds: props.config.roleIds || [],
  postIds: props.config.postIds || [],
}));

function updateParticipants(value: WorkflowParticipantValue) {
  const next = {
    userIds: normalizeList(value.userIds),
    orgIds: normalizeList(value.orgIds),
    roleIds: normalizeList(value.roleIds),
    postIds: normalizeList(value.postIds),
  };
  const hasRestriction = Object.values(next).some(list => list.length > 0);
  emit('update', {
    scopeType: hasRestriction ? 'COMPOSITE' : 'ALL',
    ...next,
  });
}

function normalizeList(value: unknown) {
  if (Array.isArray(value)) {
    return value.map(item => String(item).trim()).filter(Boolean);
  }
  if (value === undefined || value === null || value === '') {
    return [];
  }
  return [String(value).trim()].filter(Boolean);
}
</script>
