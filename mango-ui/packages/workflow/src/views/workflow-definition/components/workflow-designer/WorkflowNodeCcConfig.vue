<template>
  <div class="workflow-cc-config">
    <el-form-item label="抄送对象">
      <WorkflowParticipantSelector
        :model-value="participantValue"
        :user-options="userOptions"
        :role-options="roleOptions"
        :post-options="postOptions"
        :org-tree-options="orgTreeOptions"
        :target-loading="targetLoading"
        placeholder="请选择抄送的人员、部门、岗位或角色"
        search-placeholder="搜索抄送对象"
        @update:model-value="updateParticipants"
        @ensure-users="$emit('ensure-users')"
        @ensure-roles="$emit('ensure-roles')"
        @ensure-posts="$emit('ensure-posts')"
        @ensure-orgs="$emit('ensure-orgs')"
      />
    </el-form-item>

    <el-form-item label="通知事件">
      <el-input :model-value="config.eventName || 'workflow.cc'" placeholder="workflow.cc" @input="value => update({ eventName: String(value || '') })" />
    </el-form-item>

    <el-form-item label="通知内容">
      <el-input
        :model-value="config.messageTemplate || ''"
        :rows="3"
        placeholder="如：${initiator} 发起的流程已流转到抄送节点"
        type="textarea"
        @input="value => update({ messageTemplate: String(value || '') })"
      />
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { ApprovalOrgTreeOption, ApprovalTargetOption } from './types';
import WorkflowParticipantSelector, { type WorkflowParticipantValue } from './WorkflowParticipantSelector.vue';

export interface WorkflowCcNodeConfig {
  targetTypes?: string[];
  userIds?: string[];
  orgIds?: string[];
  roleIds?: string[];
  postIds?: string[];
  eventName?: string;
  messageTemplate?: string;
}

const props = defineProps<{
  config: WorkflowCcNodeConfig;
  userOptions: ApprovalTargetOption[];
  roleOptions: ApprovalTargetOption[];
  postOptions: ApprovalTargetOption[];
  orgTreeOptions: ApprovalOrgTreeOption[];
  targetLoading: { users: boolean; roles: boolean; posts: boolean; orgs: boolean };
}>();

const emit = defineEmits<{
  update: [patch: WorkflowCcNodeConfig];
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
  update({
    ...next,
    targetTypes: targetTypesOf(next),
  });
}

function update(patch: Partial<WorkflowCcNodeConfig>) {
  emit('update', {
    ...props.config,
    ...patch,
  });
}

function targetTypesOf(value: Required<Pick<WorkflowCcNodeConfig, 'userIds' | 'orgIds' | 'roleIds' | 'postIds'>>) {
  const types: string[] = [];
  if (value.userIds.length) types.push('USER');
  if (value.orgIds.length) types.push('ORG');
  if (value.roleIds.length) types.push('ROLE');
  if (value.postIds.length) types.push('POST');
  return types;
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
