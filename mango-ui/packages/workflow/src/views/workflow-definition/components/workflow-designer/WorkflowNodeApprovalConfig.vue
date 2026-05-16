<template>
  <div class="workflow-approval-config">
    <div class="approval-drawer-section first">
        <el-radio-group
          :model-value="config.assigneeType"
          class="approval-radio-grid assignee-grid"
          @change="value => $emit('update-assignee-type', value)"
        >
          <el-radio v-for="item in assigneeTypeOptions" :key="item.value" :label="item.value">
            {{ item.label }}
          </el-radio>
        </el-radio-group>

        <div v-if="config.assigneeType === 'SPECIFIED_USER'" class="approval-target-block">
          <el-select
            :model-value="config.assigneeIds || []"
            class="approval-target-select"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            :loading="targetLoading.users"
            :teleported="false"
            placeholder="搜索用户名/姓名选择成员"
            @focus="$emit('ensure-users')"
            @visible-change="visible => visible && $emit('ensure-users')"
            @change="value => $emit('update-list', 'assigneeIds', value)"
          >
            <el-option v-for="item in userOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>

        <div v-if="config.assigneeType === 'SPECIFIED_ROLE'" class="approval-target-block">
          <el-select
            :model-value="config.roleIds || []"
            class="approval-target-select"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            :loading="targetLoading.roles"
            :teleported="false"
            placeholder="选择角色"
            @focus="$emit('ensure-roles')"
            @visible-change="visible => visible && $emit('ensure-roles')"
            @change="value => $emit('update-list', 'roleIds', value)"
          >
            <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>

        <div v-if="config.assigneeType === 'SPECIFIED_POST'" class="approval-target-block">
          <el-select
            :model-value="config.postIds || []"
            class="approval-target-select"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            :loading="targetLoading.posts"
            :teleported="false"
            placeholder="选择岗位"
            @focus="$emit('ensure-posts')"
            @visible-change="visible => visible && $emit('ensure-posts')"
            @change="value => $emit('update-list', 'postIds', value)"
          >
            <el-option v-for="item in postOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>

        <div v-if="config.assigneeType === 'SPECIFIED_ORG'" class="approval-target-block">
          <el-tree-select
            :model-value="config.orgIds || []"
            class="approval-target-select"
            :data="orgTreeOptions"
            :props="{ label: 'label', value: 'value', children: 'children' }"
            multiple
            filterable
            clearable
            check-strictly
            collapse-tags
            collapse-tags-tooltip
            :loading="targetLoading.orgs"
            :teleported="false"
            placeholder="选择部门"
            @focus="$emit('ensure-orgs')"
            @visible-change="visible => visible && $emit('ensure-orgs')"
            @change="value => $emit('update-list', 'orgIds', value)"
          />
        </div>

        <div v-if="config.assigneeType === 'ORG_LEADER'" class="approval-target-block">
          <el-switch
            :model-value="config.orgLeaderUseInitiatorOrg !== false"
            active-text="使用发起人所在组织"
            inactive-text="指定组织主管"
            @change="value => $emit('update-config', { orgLeaderUseInitiatorOrg: Boolean(value) })"
          />
          <el-tree-select
            v-if="config.orgLeaderUseInitiatorOrg === false"
            :model-value="config.orgIds || []"
            class="approval-target-select leader-org-select"
            :data="orgTreeOptions"
            :props="{ label: 'label', value: 'value', children: 'children' }"
            multiple
            filterable
            clearable
            check-strictly
            collapse-tags
            collapse-tags-tooltip
            :loading="targetLoading.orgs"
            :teleported="false"
            placeholder="选择主管所在组织"
            @focus="$emit('ensure-orgs')"
            @visible-change="visible => visible && $emit('ensure-orgs')"
            @change="value => $emit('update-list', 'orgIds', value)"
          />
        </div>

        <div v-if="config.assigneeType === 'FORM_USER'" class="approval-target-block">
          <el-radio-group
            :model-value="config.formUserFieldType || 'USER'"
            class="approval-radio-row form-field-type-row"
            @change="value => $emit('update-config', { formUserFieldType: value })"
          >
            <el-radio label="USER">人员</el-radio>
            <el-radio label="ORG">组织</el-radio>
            <el-radio label="ROLE">角色</el-radio>
            <el-radio label="POST">岗位</el-radio>
          </el-radio-group>
          <el-select
            :model-value="config.formUserField"
            class="approval-target-select"
            clearable
            filterable
            :teleported="false"
            placeholder="选择表单人员字段"
            @change="value => $emit('update-config', { formUserField: value })"
          >
            <el-option v-for="field in formVariables" :key="field.value" :label="`${field.label}（${field.value}）`" :value="field.value" />
          </el-select>
        </div>

        <div v-if="config.assigneeType === 'EXPRESSION'" class="approval-target-block">
          <el-input :model-value="config.expression" placeholder="${managerUserId}" @input="value => $emit('update-config', { expression: value })" />
        </div>
    </div>

    <div v-if="config.assigneeType === 'INITIATOR_SELECT'" class="approval-drawer-section">
        <div class="approval-section-title">选择方式</div>
        <el-radio-group
          :model-value="config.initiatorSelectMultiple ? 'MULTI' : 'SINGLE'"
          class="approval-radio-row"
          @change="value => $emit('update-config', { initiatorSelectMultiple: value === 'MULTI' })"
        >
          <el-radio label="SINGLE">单选</el-radio>
          <el-radio label="MULTI">多选</el-radio>
        </el-radio-group>
    </div>

    <div v-if="showModeConfig" class="approval-drawer-section">
        <div class="approval-section-title">多人审批时采用的审批方式</div>
        <el-radio-group
          :model-value="config.approvalMode"
          class="approval-radio-column"
          @change="value => $emit('update-config', { approvalMode: value })"
        >
          <el-radio label="COUNTERSIGN">会签(需要所有审批人同意)</el-radio>
          <el-radio label="OR_SIGN">或签(一名审批人同意即可)</el-radio>
          <el-radio label="SEQUENTIAL">依次审批(按顺序依次审批)</el-radio>
        </el-radio-group>
    </div>

    <div class="approval-drawer-section">
        <div class="approval-section-title">审批人为空时</div>
        <el-radio-group
          :model-value="config.emptyAssigneeStrategy"
          class="approval-radio-grid empty-strategy-grid"
          @change="value => $emit('update-empty-strategy', value)"
        >
          <el-radio label="AUTO_PASS">自动通过</el-radio>
          <el-radio label="AUTO_REJECT">自动驳回</el-radio>
          <el-radio label="AUTO_END">自动结束</el-radio>
          <el-radio label="TO_ADMIN">转交给管理员</el-radio>
          <el-radio label="TO_USER">指定人员</el-radio>
        </el-radio-group>
        <div v-if="config.emptyAssigneeStrategy === 'TO_USER'" class="approval-target-block">
          <el-select
            :model-value="config.emptyAssigneeUserIds || []"
            class="approval-target-select"
            multiple
            filterable
            collapse-tags
            collapse-tags-tooltip
            :loading="targetLoading.users"
            :teleported="false"
            placeholder="搜索用户名/姓名选择兜底成员"
            @focus="$emit('ensure-users')"
            @visible-change="visible => visible && $emit('ensure-users')"
            @change="value => $emit('update-list', 'emptyAssigneeUserIds', value)"
          >
            <el-option v-for="item in userOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </div>
    </div>

    <div class="approval-drawer-section">
        <div class="approval-section-title">审批被驳回</div>
        <el-radio-group
          :model-value="config.rejectStrategy"
          class="approval-radio-column"
          @change="value => $emit('update-config', { rejectStrategy: value })"
        >
          <el-radio label="END_PROCESS">直接结束流程</el-radio>
          <el-radio label="BACK_TO_START">驳回到发起人</el-radio>
        </el-radio-group>
    </div>

  </div>
</template>

<script setup lang="ts">
import type { WorkflowApprovalNodeConfig } from '../../../../api/workflow';
import type { ApprovalOrgTreeOption, ApprovalTargetOption, WorkflowVariableOption } from './types';

defineProps<{
  config: WorkflowApprovalNodeConfig;
  assigneeTypeOptions: Array<{ label: string; value: string }>;
  userOptions: ApprovalTargetOption[];
  roleOptions: ApprovalTargetOption[];
  postOptions: ApprovalTargetOption[];
  orgTreeOptions: ApprovalOrgTreeOption[];
  targetLoading: { users: boolean; roles: boolean; posts: boolean; orgs: boolean };
  formVariables: WorkflowVariableOption[];
  showModeConfig: boolean;
}>();

defineEmits<{
  'update-assignee-type': [value: unknown];
  'update-config': [patch: Partial<WorkflowApprovalNodeConfig>];
  'update-empty-strategy': [value: unknown];
  'update-list': [key: keyof WorkflowApprovalNodeConfig, value: unknown];
  'ensure-users': [];
  'ensure-roles': [];
  'ensure-posts': [];
  'ensure-orgs': [];
}>();
</script>

<style scoped>
.approval-target-select {
  width: 100%;
}

.approval-drawer-section {
  padding: 16px 0;
  border-top: 1px solid var(--el-border-color-light);
}

.approval-drawer-section.first {
  padding-top: 0;
  border-top: 0;
}

.approval-section-title {
  margin-bottom: 10px;
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 700;
}

.approval-radio-grid {
  display: grid;
  width: 100%;
  gap: 8px;
}

.assignee-grid,
.empty-strategy-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.approval-radio-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.approval-radio-column {
  display: grid;
  gap: 8px;
}

.approval-radio-grid :deep(.el-radio),
.approval-radio-row :deep(.el-radio),
.approval-radio-column :deep(.el-radio) {
  display: flex;
  align-items: center;
  min-height: 36px;
  height: auto;
  margin-right: 0;
  padding: 8px 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 600;
}

.approval-radio-grid :deep(.el-radio.is-checked),
.approval-radio-row :deep(.el-radio.is-checked),
.approval-radio-column :deep(.el-radio.is-checked) {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.approval-target-block {
  margin-top: 10px;
}

.leader-org-select,
.form-field-type-row {
  margin-top: 10px;
}

.approval-target-block :deep(.el-select-dropdown),
.approval-target-block :deep(.el-tree-select__popper) {
  max-width: 100%;
}

.drawer-form {
  padding: 4px 0;
}

</style>
