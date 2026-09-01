<!-- mango-page-baseline-exception all: 用户维护联合组织树筛选、角色分配、状态控制和凭据操作，不是单一标准列表与短表单弹框。 -->
<template>
  <div class="user-container" data-page="user.management">
    <div class="user-page-layout">
      <aside class="org-filter-panel">
        <el-card class="layout-card">
          <div class="org-filter-header">
            <span>部门组织</span>
            <el-button link type="primary" @click="loadOrgTree"> 刷新 </el-button>
          </div>
          <el-input v-model="orgKeyword" placeholder="请输入部门名称" clearable class="org-filter-search" />
          <el-alert
            v-if="orgLoadError"
            title="部门组织加载失败"
            type="error"
            :closable="false"
            show-icon
            class="state-alert"
          >
            <el-button link type="primary" @click="loadOrgTree">重试</el-button>
          </el-alert>
          <el-tree
            ref="orgTreeRef"
            v-loading="orgLoading"
            class="org-tree"
            :data="orgFilterTreeData"
            node-key="id"
            highlight-current
            default-expand-all
            :filter-node-method="filterOrgNode"
            :props="{ label: 'orgName', children: 'children' }"
            :expand-on-click-node="false"
            :data-state="orgTreeState"
            @node-click="handleOrgClick"
          >
            <template #default="{ data }">
              <span
                class="org-tree-node"
                :data-record-key="`org:${data.id}`"
                :data-state="String(data.id) === String(selectedOrg?.id || ALL_MEMBERS_ID) ? 'selected' : 'idle'"
              >
                <span>{{ data.orgName }}</span>
                <el-tag v-if="data.orgType" size="small" effect="plain">
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
              <span class="current-org-title" data-field="user.scope.name">
                {{ selectedOrg?.orgName || '全部成员' }}
              </span>
              <span class="current-org-subtitle">
                {{ selectedOrg ? '显示该组织本级及全部下级组织成员' : '显示当前机构全部成员' }}
              </span>
            </div>
            <el-button v-if="selectedOrg" data-action="user.org.clear" @click="clearOrgFilter">
              清除部门选择
            </el-button>
          </div>

          <el-form :inline="true" class="search-form" data-surface="user.search">
            <el-form-item label="用户名">
              <el-input v-model="query.username" placeholder="请输入用户名" clearable />
            </el-form-item>
            <el-form-item label="姓名">
              <el-input v-model="query.nickname" placeholder="请输入姓名" clearable />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="query.phone" placeholder="请输入手机号" clearable />
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
              <el-button type="primary" @click="handleSearch"> 查询 </el-button>
              <el-button @click="handleReset"> 重置 </el-button>
            </el-form-item>
          </el-form>

          <div class="action-toolbar">
            <div class="toolbar-left">
              <el-button type="primary" data-action="user.create" @click="handleAdd"> 新增成员 </el-button>
              <el-button type="danger" :disabled="selectedUsers.length === 0" @click="handleBatchDelete">
                批量移出租户成员
              </el-button>
              <el-tooltip :disabled="canSyncWecom" :content="wecomSyncDisabledTip" placement="top">
                <span>
                  <el-button :disabled="!canSyncWecom" :loading="wecomSyncLoading" @click="openWecomSyncDialog">
                    同步企微用户
                  </el-button>
                </span>
              </el-tooltip>
              <el-button v-if="selectedOrg" data-action="user.org.add-existing" @click="handleAddOrgMember">
                添加已有成员
              </el-button>
            </div>
          </div>

          <el-alert
            v-if="listLoadError"
            title="成员列表加载失败"
            type="error"
            :closable="false"
            show-icon
            class="state-alert"
          >
            <el-button link type="primary" @click="loadData">重试</el-button>
          </el-alert>

          <el-table
            v-loading="loading"
            :data="tableData"
            row-key="userId"
            stripe
            class="user-table"
            :data-state="listState"
            @selection-change="handleSelectionChange"
          >
            <template #empty>
              <el-empty :description="listLoadError ? '成员列表加载失败，请重试' : '暂无成员'" :image-size="72" />
            </template>
            <el-table-column type="selection" width="54" :selectable="isRowSelectable" />
            <el-table-column prop="username" label="用户名" min-width="140">
              <template #default="{ row }">
                <span :data-record-key="`user:${row.username}`">{{ row.username }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="nickname" label="姓名" min-width="140" />
            <el-table-column prop="phone" label="手机号" min-width="130" />
            <el-table-column label="所属部门" min-width="190" show-overflow-tooltip>
              <template #default="{ row }">
                {{ orgPathLabel(row.orgId || row.primaryOrgId) || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="已分配角色" min-width="180">
              <template #default="{ row }">
                <div :data-record-key="`user-roles:${row.username}`">
                  <div v-if="row.roleNames?.length" class="role-tags">
                    <el-tag v-for="roleName in row.roleNames" :key="roleName" size="small" effect="plain">
                      {{ roleName }}
                    </el-tag>
                  </div>
                  <span v-else>-</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column v-if="selectedOrg" label="部门岗位" min-width="150">
              <template #default="{ row }">
                <el-tag v-if="row.postName" effect="plain">
                  {{ row.postName }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column v-if="selectedOrg" label="部门主管" width="110">
              <template #default="{ row }">
                <el-tag :type="row.orgLeaderFlag ? 'success' : 'info'" effect="light">
                  {{ row.orgLeaderFlag ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column v-if="selectedOrg" label="主部门" width="100">
              <template #default="{ row }">
                <el-tag :type="row.primaryOrgFlag ? 'primary' : 'info'" effect="plain">
                  {{ row.primaryOrgFlag ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <DictTag dict-code="sys_normal_disable" :value="row.status" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="密码状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.passwordResetRequired ? 'warning' : 'success'" effect="light">
                  {{ row.passwordResetRequired ? '需改密' : '正常' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="锁定状态" width="130">
              <template #default="{ row }">
                <el-tooltip :disabled="!isLocked(row)" :content="lockTip(row)" placement="top">
                  <el-tag :type="isLocked(row) ? 'danger' : 'success'" effect="light">
                    {{ isLocked(row) ? '已锁定' : '未锁定' }}
                  </el-tag>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column prop="lastLoginTime" label="最近登录" width="180">
              <template #default="{ row }">
                {{ formatTime(row.lastLoginTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" :width="selectedOrg ? 700 : 570" fixed="right">
              <template #default="{ row }">
                <div :data-record-key="`user-actions:${row.username}`">
                  <el-button v-if="selectedOrg" link type="primary" size="small" @click="handleEditOrgPost(row)">
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
                    v-if="selectedOrg && row.orgRelationId"
                    link
                    type="danger"
                    size="small"
                    data-action="user.org.remove"
                    @click="handleRemoveFromOrg(row)"
                  >
                    移出当前部门
                  </el-button>
                  <el-button link type="primary" size="small" @click="handleAssignRoles(row)"> 分配角色 </el-button>
                  <el-button link type="primary" size="small" @click="handleExternalIdentity(row)">
                    企微身份
                  </el-button>
                  <el-button link type="primary" size="small" @click="handleEdit(row)"> 编辑 </el-button>
                  <el-button link type="warning" size="small" @click="handleResetPassword(row)"> 重置密码 </el-button>
                  <el-button link type="warning" size="small" @click="handleRequirePasswordReset(row)">
                    要求改密
                  </el-button>
                  <el-button link type="success" size="small" :disabled="!isLocked(row)" @click="handleUnlock(row)">
                    解锁
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
                    data-action="user.tenant.remove"
                    :disabled="!isRowSelectable(row)"
                    @click="handleDelete(row)"
                  >
                    移出租户成员
                  </el-button>
                </div>
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

    <el-dialog v-model="dialogVisible" :title="form.userId ? '编辑成员' : '新增成员'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" data-surface="user.form">
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            :disabled="!!form.userId"
            placeholder="请输入用户名"
            @blur="checkAccountAvailability"
          />
        </el-form-item>
        <el-alert
          v-if="!form.userId && accountAvailability?.status === 'UNAVAILABLE'"
          title="登录账号不可用，请修改登录账号"
          type="error"
          :closable="false"
          show-icon
          class="state-alert"
        />
        <el-alert
          v-else-if="!form.userId && accountAvailabilityError"
          title="登录账号校验失败，请重试"
          type="error"
          :closable="false"
          show-icon
          class="state-alert"
          data-state="account-check-error"
        />
        <div
          v-if="!form.userId && accountAvailability?.status === 'RECOVERABLE'"
          class="recoverable-account"
          data-state="recoverable-account"
        >
          <el-alert title="该账号对应已移出的原成员" type="warning" :closable="false" show-icon />
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="姓名">{{ accountAvailability.displayName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="成员编号">{{ accountAvailability.memberNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ accountAvailability.maskedPhone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ accountAvailability.maskedEmail || '-' }}</el-descriptions-item>
            <el-descriptions-item label="移出时间" :span="2">
              {{ formatTime(accountAvailability.removedAt) || '-' }}
            </el-descriptions-item>
          </el-descriptions>
          <div class="recoverable-account__notice">恢复后不会自动恢复原部门、岗位、角色或权限。</div>
        </div>
        <el-form-item v-if="!form.userId" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="不填默认 Mango@123456" />
          <PasswordPolicyHint v-if="form.password" :password="form.password" />
        </el-form-item>
        <el-form-item label="所属机构">
          <el-input :model-value="institutionName" data-field="user.institution" disabled />
        </el-form-item>
        <el-form-item label="所属部门" prop="orgId">
          <el-tree-select
            v-model="form.orgId"
            :data="orgTreeData"
            :props="{ value: 'id', label: 'orgName', children: 'children' }"
            node-key="id"
            check-strictly
            default-expand-all
            filterable
            :disabled="!!form.userId"
            placeholder="请选择所属部门"
            class="form-select"
            data-field="user.org"
          />
          <div class="org-path-hint" data-field="user.org.path">{{ formOrgPath || '请选择具体组织' }}</div>
        </el-form-item>
        <el-form-item label="姓名" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="item in statusOptions" :key="item.value" :value="Number(item.value)">
              {{ item.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false"> 取消 </el-button>
        <el-button
          v-if="!form.userId && accountAvailability?.status === 'RECOVERABLE'"
          @click="handleChangeLoginAccount"
        >
          修改登录账号
        </el-button>
        <el-button
          type="primary"
          data-action="user.restore"
          :loading="submitLoading || accountAvailabilityLoading"
          :disabled="!form.userId && accountAvailability?.status === 'UNAVAILABLE'"
          @click="handleSubmit"
        >
          {{ !form.userId && accountAvailability?.status === 'RECOVERABLE' ? '恢复原成员' : '确定' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetPasswordDialogVisible" title="重置密码" width="460px" :close-on-click-modal="false">
      <el-form ref="resetPasswordFormRef" :model="resetPasswordForm" :rules="resetPasswordRules" label-width="96px">
        <el-form-item label="用户">
          <span>{{ resetPasswordUser?.username }}</span>
        </el-form-item>
        <el-form-item label="新密码" prop="password">
          <el-input
            v-model="resetPasswordForm.password"
            type="password"
            show-password
            autocomplete="new-password"
            placeholder="至少8位，包含字母和数字"
          />
          <PasswordPolicyHint :password="resetPasswordForm.password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPasswordDialogVisible = false"> 取消 </el-button>
        <el-button
          type="primary"
          :loading="resetPasswordLoading"
          :disabled="!canSubmitResetPassword"
          @click="handleResetPasswordSubmit"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="wecomSyncDialogVisible" title="同步企业微信用户" width="620px">
      <el-form label-width="150px" class="wecom-sync-form">
        <el-form-item label="使用渠道配置">
          <el-switch v-model="wecomSyncUseChannelConfig" />
        </el-form-item>
        <template v-if="!wecomSyncUseChannelConfig">
          <el-form-item label="企业ID">
            <el-input v-model="wecomSyncForm.corpId" placeholder="请输入企业ID" />
          </el-form-item>
          <el-form-item label="通讯录Secret">
            <el-input v-model="wecomSyncForm.secret" type="password" show-password placeholder="请输入通讯录Secret" />
          </el-form-item>
        </template>
        <el-form-item label="同步目标">
          <div class="sync-target">
            <el-tag v-if="selectedOrg" effect="plain">
              {{ selectedOrg.orgName }}
            </el-tag>
            <span class="sync-target-tip">
              {{ wecomSyncTargetTip }}
            </span>
          </div>
        </el-form-item>
        <el-form-item label="同步组织架构">
          <el-switch v-model="wecomSyncForm.syncDepartments" :disabled="!isSelectedCompany" />
        </el-form-item>
        <el-form-item label="同步成员">
          <el-switch v-model="wecomSyncForm.syncUsers" />
        </el-form-item>
        <el-form-item label="同步子部门">
          <el-switch v-model="wecomSyncForm.fetchChild" :disabled="!isSelectedCompany" />
        </el-form-item>
        <el-form-item label="跳过未变化数据">
          <el-switch v-model="wecomSyncForm.skipUnchanged" />
        </el-form-item>
        <el-form-item label="自动创建成员">
          <el-switch v-model="wecomSyncForm.createMissingUsers" />
        </el-form-item>
        <el-form-item label="更新已匹配成员">
          <el-switch v-model="wecomSyncForm.updateMatchedUsers" />
        </el-form-item>
      </el-form>
      <div v-if="wecomSyncResult" class="sync-result">
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="部门总数">
            {{ wecomSyncResult.departmentTotalCount }}
          </el-descriptions-item>
          <el-descriptions-item label="部门新增">
            {{ wecomSyncResult.departmentCreatedCount }}
          </el-descriptions-item>
          <el-descriptions-item label="部门更新">
            {{ wecomSyncResult.departmentUpdatedCount }}
          </el-descriptions-item>
          <el-descriptions-item label="部门跳过">
            {{ wecomSyncResult.departmentSkippedCount }}
          </el-descriptions-item>
          <el-descriptions-item label="总数">
            {{ wecomSyncResult.totalCount }}
          </el-descriptions-item>
          <el-descriptions-item label="匹配">
            {{ wecomSyncResult.matchedCount }}
          </el-descriptions-item>
          <el-descriptions-item label="新增">
            {{ wecomSyncResult.createdCount }}
          </el-descriptions-item>
          <el-descriptions-item label="更新">
            {{ wecomSyncResult.updatedCount }}
          </el-descriptions-item>
          <el-descriptions-item label="身份绑定">
            {{ wecomSyncResult.boundIdentityCount }}
          </el-descriptions-item>
          <el-descriptions-item label="跳过">
            {{ wecomSyncResult.skippedCount }}
          </el-descriptions-item>
          <el-descriptions-item label="未变化">
            {{ wecomSyncResult.unchangedCount }}
          </el-descriptions-item>
          <el-descriptions-item label="失败">
            {{ wecomSyncResult.failedCount }}
          </el-descriptions-item>
        </el-descriptions>
        <el-alert v-if="wecomSyncResult.messages?.length" type="warning" :closable="false" class="sync-messages">
          <div v-for="message in wecomSyncResult.messages" :key="message">
            {{ message }}
          </div>
        </el-alert>
      </div>
      <template #footer>
        <el-button @click="wecomSyncDialogVisible = false"> 取消 </el-button>
        <el-button type="primary" :loading="wecomSyncLoading" @click="handleWecomSync"> 开始同步 </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="externalIdentityDialogVisible" title="企微登录身份" width="620px">
      <el-form label-width="120px" class="external-identity-form">
        <el-form-item label="成员">
          <span>{{ currentUser?.nickname || currentUser?.username }}</span>
        </el-form-item>
        <el-form-item label="企业ID">
          <el-input v-model="externalIdentityForm.corpId" placeholder="请输入企业微信 CorpId" />
        </el-form-item>
        <el-form-item label="企微Userid">
          <el-input v-model="externalIdentityForm.externalUserId" placeholder="请输入企业微信 userid" />
        </el-form-item>
        <el-form-item label="企业微信昵称" required>
          <el-input v-model="externalIdentityForm.displayName" placeholder="请输入企业微信完整昵称" />
        </el-form-item>
      </el-form>
      <el-table :data="externalIdentities" size="small" class="external-identity-table">
        <el-table-column prop="corpId" label="企业ID" min-width="140" />
        <el-table-column prop="displayName" label="企业微信昵称" min-width="140" />
        <el-table-column prop="externalUserId" label="企微Userid" min-width="140" />
        <el-table-column prop="bindSource" label="来源" width="90" />
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button link type="danger" size="small" @click="handleUnbindExternalIdentity(row)"> 解绑 </el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="externalIdentityDialogVisible = false"> 关闭 </el-button>
        <el-button type="primary" :loading="externalIdentityLoading" @click="handleBindExternalIdentity">
          绑定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="assignDialogVisible" title="分配成员角色" width="520px">
      <div class="assign-header">
        <span>{{ currentUser?.nickname || currentUser?.username }}</span>
        <el-tag v-if="currentUser?.username" effect="plain">
          {{ currentUser.username }}
        </el-tag>
      </div>
      <el-alert
        class="assign-data-scope-tip"
        type="info"
        show-icon
        :closable="false"
        title="角色如配置“本人部门”类数据权限，将按该成员主部门动态生效。"
      />
      <el-checkbox-group v-model="selectedRoleIds">
        <div v-for="role in roleOptions" :key="role.roleId" class="role-option">
          <el-checkbox :label="role.roleId"> {{ role.roleName }}（{{ role.roleCode }}） </el-checkbox>
        </div>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="assignDialogVisible = false"> 取消 </el-button>
        <el-button type="primary" :loading="assignSubmitLoading" @click="handleAssignSubmit"> 确定 </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="orgMemberDialogVisible"
      :title="orgMemberForm.relationId ? '调整部门岗位' : '添加已有成员'"
      width="520px"
    >
      <el-form
        ref="orgMemberFormRef"
        :model="orgMemberForm"
        :rules="orgMemberRules"
        label-width="100px"
        data-surface="user.org-member.form"
      >
        <el-form-item v-if="!orgMemberForm.relationId" label="成员" prop="memberId">
          <el-select
            v-model="orgMemberForm.memberId"
            filterable
            remote
            reserve-keyword
            placeholder="请输入用户名、姓名、手机号或邮箱"
            :remote-method="searchCandidateUsers"
            :loading="candidateLoading"
            :data-state="candidateState"
            :no-data-text="candidateLoadError ? '候选成员加载失败，请重新输入关键字' : '暂无可添加成员'"
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
        <el-form-item label="部门岗位" prop="postId">
          <el-select v-model="orgMemberForm.postId" placeholder="请选择岗位" filterable clearable class="form-select">
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
        <el-button @click="orgMemberDialogVisible = false"> 取消 </el-button>
        <el-button type="primary" :loading="orgMemberSubmitLoading" @click="handleOrgMemberSubmit"> 确定 </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts" name="SystemUser">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type TreeInstance } from 'element-plus';
import type { ApiId } from '@mango/api-schema';
import { useDict } from '@mango/common/hooks/useDict';
import {
  DictSelect,
  DictTag,
  Pagination,
  PasswordPolicyHint,
  defaultPasswordPolicy,
  getPasswordPolicyMessage,
  isPasswordPolicyPassed,
} from '@mango/common';
import { Session } from '@mango/common/utils/storage';
import { orgApi, type SysOrg } from '../../api/org';
import { postApi, type PostVO } from '../../api/post';
import { roleApi, type RoleVO } from '../../api/role';
import {
  userApi,
  type ExternalIdentityBindingVO,
  type IdentityAccountAvailabilityVO,
  type IdentityUserVO,
  type WecomUserSyncResult,
} from '../../api/user';

const { options: statusOptions } = useDict('sys_normal_disable');

const loading = ref(false);
const submitLoading = ref(false);
const assignSubmitLoading = ref(false);
const dialogVisible = ref(false);
const assignDialogVisible = ref(false);
const orgMemberDialogVisible = ref(false);
const wecomSyncDialogVisible = ref(false);
const externalIdentityDialogVisible = ref(false);
const resetPasswordDialogVisible = ref(false);
const orgLoading = ref(false);
const orgLoadError = ref(false);
const candidateLoading = ref(false);
const candidateLoadError = ref(false);
const listLoadError = ref(false);
const orgMemberSubmitLoading = ref(false);
const wecomSyncLoading = ref(false);
const externalIdentityLoading = ref(false);
const resetPasswordLoading = ref(false);
const accountAvailabilityLoading = ref(false);
const accountAvailabilityError = ref(false);
const accountAvailability = ref<IdentityAccountAvailabilityVO>();
const tableData = ref<IdentityUserVO[]>([]);
const selectedUsers = ref<IdentityUserVO[]>([]);
const externalIdentities = ref<ExternalIdentityBindingVO[]>([]);
const roleOptions = ref<RoleVO[]>([]);
const postOptions = ref<PostVO[]>([]);
const candidateUsers = ref<IdentityUserVO[]>([]);
const orgTreeData = ref<SysOrg[]>([]);
const selectedRoleIds = ref<ApiId[]>([]);
const total = ref(0);
const formRef = ref<FormInstance>();
const orgTreeRef = ref<TreeInstance>();
const orgMemberFormRef = ref<FormInstance>();
const resetPasswordFormRef = ref<FormInstance>();
const currentUser = ref<IdentityUserVO>();
const resetPasswordUser = ref<IdentityUserVO>();
const selectedOrg = ref<SysOrg>();
const orgKeyword = ref('');
const wecomSyncUseChannelConfig = ref(true);
const wecomSyncResult = ref<WecomUserSyncResult>();
const ALL_MEMBERS_ID = '__all_members__';
const allMembersNode: SysOrg = {
  id: ALL_MEMBERS_ID,
  orgName: '全部成员',
  pid: '0',
};
const orgFilterTreeData = computed(() => [allMembersNode, ...orgTreeData.value]);
let loadSequence = 0;
let orgTreeSequence = 0;
let orgScopeSequence = 0;
let candidateSequence = 0;
let accountAvailabilitySequence = 0;

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  username: '',
  nickname: '',
  phone: '',
  status: undefined as number | undefined,
  orgIds: undefined as ApiId[] | undefined,
});

type UserForm = IdentityUserVO & { orgId?: ApiId };

const form = reactive<UserForm>({
  userId: undefined,
  username: '',
  password: '',
  nickname: '',
  orgId: undefined,
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

const externalIdentityForm = reactive({
  corpId: '',
  externalUserId: '',
  displayName: '',
});

const resetPasswordForm = reactive({
  password: '',
});

const GROUP_ORG_TYPE = 1;
const COMPANY_ORG_TYPE = 2;
const DEPARTMENT_ORG_TYPE = 3;

const wecomSyncForm = reactive({
  corpId: '',
  secret: '',
  fetchChild: true,
  syncDepartments: true,
  syncUsers: true,
  skipUnchanged: true,
  createMissingUsers: true,
  updateMatchedUsers: true,
});

const passwordPolicyMessage = getPasswordPolicyMessage(defaultPasswordPolicy);
const canSubmitResetPassword = computed(() =>
  isPasswordPolicyPassed(resetPasswordForm.password, defaultPasswordPolicy),
);
const institutionName = computed(() => orgTreeData.value[0]?.orgName || '-');
const formOrgPath = computed(() => orgPathLabel(form.orgId));
const listState = computed(() => {
  if (loading.value) return 'loading';
  if (listLoadError.value) return 'error';
  return tableData.value.length ? 'ready' : 'empty';
});
const orgTreeState = computed(() => {
  if (orgLoading.value) return 'loading';
  if (orgLoadError.value) return 'error';
  return orgTreeData.value.length ? 'ready' : 'empty';
});
const candidateState = computed(() => {
  if (candidateLoading.value) return 'loading';
  if (candidateLoadError.value) return 'error';
  return candidateUsers.value.length ? 'ready' : 'empty';
});

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    {
      validator: (_rule, value, callback) => {
        if (!value || isPasswordPolicyPassed(String(value), defaultPasswordPolicy)) {
          callback();
          return;
        }
        callback(new Error(passwordPolicyMessage));
      },
      trigger: 'blur',
    },
  ],
  orgId: [
    {
      validator: (_rule, value, callback) => {
        if (form.userId || value) {
          callback();
          return;
        }
        callback(new Error('请选择所属部门'));
      },
      trigger: 'change',
    },
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
};

const orgMemberRules: FormRules = {
  memberId: [{ required: true, message: '请选择成员', trigger: 'change' }],
};

const resetPasswordRules: FormRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (isPasswordPolicyPassed(String(value || ''), defaultPasswordPolicy)) {
          callback();
          return;
        }
        callback(new Error(passwordPolicyMessage));
      },
      trigger: 'blur',
    },
  ],
};

async function loadData() {
  const sequence = ++loadSequence;
  loading.value = true;
  listLoadError.value = false;
  try {
    const data = await userApi.page(query);
    const memberIds = data.list.map((item) => item.memberId).filter(Boolean) as ApiId[];
    const summaries = memberIds.length ? await roleApi.getSubjectRolesBatch(memberIds) : [];
    if (sequence !== loadSequence) return;
    const rolesByMemberId = new Map(summaries.map((item) => [String(item.subjectId), item.roles]));
    tableData.value = data.list.map((item) => ({
      ...item,
      roleNames: item.memberId ? rolesByMemberId.get(String(item.memberId))?.map((role) => role.roleName) || [] : [],
    }));
    total.value = data.total;
    selectedUsers.value = [];
  } catch {
    if (sequence === loadSequence) {
      tableData.value = [];
      total.value = 0;
      selectedUsers.value = [];
      listLoadError.value = true;
      ElMessage.error('成员列表加载失败，请重试');
    }
  } finally {
    if (sequence === loadSequence) loading.value = false;
  }
}

async function loadOrgTree() {
  const sequence = ++orgTreeSequence;
  orgLoading.value = true;
  orgLoadError.value = false;
  try {
    const data = await orgApi.tree({ parentId: '0' });
    if (sequence !== orgTreeSequence) return;
    orgTreeData.value = data;
    await nextTick();
    orgTreeRef.value?.setCurrentKey(selectedOrg.value?.id || ALL_MEMBERS_ID);
  } catch {
    if (sequence === orgTreeSequence) {
      orgTreeData.value = [];
      orgLoadError.value = true;
      ElMessage.error('部门组织加载失败，请重试');
    }
  } finally {
    if (sequence === orgTreeSequence) orgLoading.value = false;
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

async function handleReset() {
  query.pageNum = 1;
  query.username = '';
  query.nickname = '';
  query.phone = '';
  query.status = undefined;
  await loadData();
}

async function handleOrgClick(row: SysOrg) {
  if (row.id === ALL_MEMBERS_ID) {
    await clearOrgFilter();
    return;
  }
  const sequence = ++orgScopeSequence;
  selectedOrg.value = row;
  loading.value = true;
  listLoadError.value = false;
  try {
    const orgIds = await orgApi.memberScope(row.id);
    if (sequence !== orgScopeSequence) return;
    query.orgIds = orgIds;
    query.pageNum = 1;
    await loadData();
  } catch {
    if (sequence === orgScopeSequence) {
      tableData.value = [];
      total.value = 0;
      listLoadError.value = true;
      loading.value = false;
      ElMessage.error('部门成员范围加载失败，请重试');
    }
  }
}

async function clearOrgFilter() {
  orgScopeSequence += 1;
  selectedOrg.value = undefined;
  query.orgIds = undefined;
  query.pageNum = 1;
  orgTreeRef.value?.setCurrentKey(ALL_MEMBERS_ID);
  await loadData();
}

function filterOrgNode(value: string, data: SysOrg) {
  if (data.id === ALL_MEMBERS_ID) return true;
  return !value || data.orgName?.includes(value) || data.orgCode?.includes(value);
}

function orgPathLabel(orgId?: ApiId) {
  if (!orgId) return '';
  const path = findOrgPath(orgTreeData.value, orgId);
  return path.map((org) => org.orgName).join(' / ');
}

function findOrgPath(nodes: SysOrg[], orgId: ApiId, ancestors: SysOrg[] = []): SysOrg[] {
  for (const node of nodes) {
    const path = [...ancestors, node];
    if (String(node.id) === String(orgId)) return path;
    const childPath = findOrgPath(node.children || [], orgId, path);
    if (childPath.length) return childPath;
  }
  return [];
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

function isLocked(row: IdentityUserVO) {
  return row.locked === true;
}

function lockTip(row: IdentityUserVO) {
  const until = formatTime(row.lockedUntil);
  const reason = row.lockedReason || '登录失败次数超过限制';
  return until === '-' ? reason : `${reason}，锁定至 ${until}`;
}

function resetForm() {
  clearAccountAvailability();
  Object.assign(form, {
    userId: undefined,
    username: '',
    password: '',
    nickname: '',
    orgId: selectedOrg.value?.id,
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

async function checkAccountAvailability(): Promise<boolean> {
  if (form.userId || !form.username.trim()) {
    clearAccountAvailability();
    return false;
  }
  const sequence = ++accountAvailabilitySequence;
  const username = form.username.trim();
  accountAvailabilityLoading.value = true;
  accountAvailabilityError.value = false;
  try {
    const result = await userApi.accountAvailability(username, form.realm || 'INTERNAL');
    if (sequence !== accountAvailabilitySequence || username !== form.username.trim()) return false;
    accountAvailability.value = result;
    return true;
  } catch {
    if (sequence === accountAvailabilitySequence) {
      accountAvailability.value = undefined;
      accountAvailabilityError.value = true;
      ElMessage.error('登录账号校验失败，请重试');
    }
    return false;
  } finally {
    if (sequence === accountAvailabilitySequence) accountAvailabilityLoading.value = false;
  }
}

function handleChangeLoginAccount() {
  clearAccountAvailability();
  form.username = '';
}

function clearAccountAvailability() {
  accountAvailabilitySequence += 1;
  accountAvailability.value = undefined;
  accountAvailabilityError.value = false;
  accountAvailabilityLoading.value = false;
}

function openWecomSyncDialog() {
  if (!selectedOrg.value) {
    ElMessage.warning('请先在左侧选择公司或部门');
    return;
  }
  if (!canSyncWecom.value) {
    ElMessage.warning(wecomSyncDisabledTip.value);
    return;
  }
  const companyMode = isSelectedCompany.value;
  wecomSyncForm.syncDepartments = companyMode;
  wecomSyncForm.fetchChild = companyMode;
  wecomSyncForm.syncUsers = true;
  wecomSyncResult.value = undefined;
  wecomSyncDialogVisible.value = true;
}

async function handleWecomSync() {
  if (!selectedOrg.value?.id) {
    ElMessage.warning('请先在左侧选择公司或部门');
    return;
  }
  if (!wecomSyncUseChannelConfig.value && (!wecomSyncForm.corpId.trim() || !wecomSyncForm.secret.trim())) {
    ElMessage.warning('请填写企业ID和通讯录Secret');
    return;
  }
  wecomSyncLoading.value = true;
  try {
    wecomSyncResult.value = await userApi.syncWecomUsers({
      corpId: wecomSyncUseChannelConfig.value ? undefined : wecomSyncForm.corpId.trim(),
      secret: wecomSyncUseChannelConfig.value ? undefined : wecomSyncForm.secret.trim(),
      targetOrgId: selectedOrg.value.id,
      targetOrgType: selectedOrg.value.orgType,
      fetchChild: isSelectedCompany.value && wecomSyncForm.fetchChild,
      syncDepartments: isSelectedCompany.value && wecomSyncForm.syncDepartments,
      syncUsers: wecomSyncForm.syncUsers,
      skipUnchanged: wecomSyncForm.skipUnchanged,
      createMissingUsers: wecomSyncForm.createMissingUsers,
      updateMatchedUsers: wecomSyncForm.updateMatchedUsers,
    });
    ElMessage.success('同步完成');
    await loadData();
  } finally {
    wecomSyncLoading.value = false;
  }
}

const isSelectedCompany = computed(() => selectedOrg.value?.orgType === COMPANY_ORG_TYPE);
const isSelectedGroup = computed(() => selectedOrg.value?.orgType === GROUP_ORG_TYPE);
const isSelectedDepartment = computed(() => selectedOrg.value?.orgType === DEPARTMENT_ORG_TYPE);
const canSyncWecom = computed(() => isSelectedCompany.value || isSelectedDepartment.value);

const wecomSyncDisabledTip = computed(() => {
  if (!selectedOrg.value) {
    return '请先在左侧选择公司或部门';
  }
  if (isSelectedGroup.value) {
    return '集团节点不支持同步，请选择二级公司或已映射部门';
  }
  return '请选择二级公司或已映射部门同步';
});

const wecomSyncTargetTip = computed(() => {
  if (!selectedOrg.value) {
    return '请先选择左侧公司或部门';
  }
  if (isSelectedCompany.value) {
    return '将企业微信组织架构和成员同步到所选公司下';
  }
  return '仅同步当前部门对应企业微信部门的直属成员';
});

async function handleExternalIdentity(row: IdentityUserVO) {
  if (!row.userId) return;
  currentUser.value = row;
  externalIdentityForm.corpId = '';
  externalIdentityForm.externalUserId = '';
  externalIdentityForm.displayName = '';
  externalIdentityDialogVisible.value = true;
  externalIdentities.value = await userApi.listExternalIdentities(row.userId);
}

async function handleBindExternalIdentity() {
  if (!currentUser.value?.userId) return;
  if (
    !externalIdentityForm.corpId.trim() ||
    !externalIdentityForm.externalUserId.trim() ||
    !externalIdentityForm.displayName.trim()
  ) {
    ElMessage.warning('请填写企业ID、企微Userid和企业微信昵称');
    return;
  }
  externalIdentityLoading.value = true;
  try {
    await userApi.bindExternalIdentity({
      userId: currentUser.value.userId,
      provider: 'WECOM',
      corpId: externalIdentityForm.corpId.trim(),
      externalUserId: externalIdentityForm.externalUserId.trim(),
      displayName: externalIdentityForm.displayName.trim(),
      bindSource: 'ADMIN',
    });
    ElMessage.success('绑定成功');
    externalIdentities.value = await userApi.listExternalIdentities(currentUser.value.userId);
    externalIdentityForm.externalUserId = '';
  } finally {
    externalIdentityLoading.value = false;
  }
}

async function handleUnbindExternalIdentity(row: ExternalIdentityBindingVO) {
  if (!currentUser.value?.userId) return;
  await ElMessageBox.confirm('确认解绑该企业微信登录身份？解绑后该企微账号不能扫码登录此成员。', '解绑确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  });
  await userApi.unbindExternalIdentity({
    userId: currentUser.value.userId,
    provider: row.provider,
    corpId: row.corpId,
    externalUserId: row.externalUserId,
  });
  ElMessage.success('已解绑');
  externalIdentities.value = await userApi.listExternalIdentities(currentUser.value.userId);
}

function handleEdit(row: IdentityUserVO) {
  Object.assign(form, {
    ...row,
    password: '',
    status: row.status ?? 1,
    orgId: row.orgId || row.primaryOrgId,
  });
  formRef.value?.clearValidate();
  dialogVisible.value = true;
}

async function handleSubmit() {
  if (!formRef.value) return;
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  submitLoading.value = true;
  try {
    if (form.userId) {
      await userApi.update(form);
      ElMessage.success('修改成功');
    } else {
      const checked = await checkAccountAvailability();
      if (!checked || !accountAvailability.value) {
        return;
      }
      if (accountAvailability.value?.status === 'UNAVAILABLE') {
        ElMessage.error('登录账号不可用，请修改登录账号');
        return;
      }
      if (accountAvailability.value.status === 'RECOVERABLE') {
        await orgApi.restoreMemberAccount({
          orgId: form.orgId!,
          username: form.username.trim(),
          realm: form.realm || 'INTERNAL',
        });
        ElMessage.success('已恢复原成员');
      } else {
        await orgApi.createMemberAccount({
          orgId: form.orgId!,
          username: form.username,
          password: form.password,
          nickname: form.nickname,
          phone: form.phone,
          email: form.email,
          status: form.status,
          remark: form.remark,
          primaryFlag: true,
          leaderFlag: false,
        });
        ElMessage.success('新增成功');
      }
    }
    dialogVisible.value = false;
    await loadData();
  } finally {
    submitLoading.value = false;
  }
}

function handleDelete(row: IdentityUserVO) {
  if (!row.userId) return;
  ElMessageBox.confirm(
    `确认将成员「${row.username}」移出当前租户？其登录身份和历史将保留，当前角色与全部部门关系会被撤销，恢复时不会自动恢复权限。`,
    '移出租户成员',
    {
      confirmButtonText: '移出租户',
      cancelButtonText: '取消',
      type: 'warning',
    },
  )
    .then(async () => {
      await userApi.delete(row.userId!);
      ElMessage.success('已移出租户成员');
      await loadData();
    })
    .catch(() => {});
}

function handleSelectionChange(rows: IdentityUserVO[]) {
  selectedUsers.value = rows;
}

function isRowSelectable(row: IdentityUserVO) {
  const currentUserId = Session.get('userInfo')?.userId;
  return Boolean(row.userId) && String(row.userId) !== String(currentUserId);
}

function handleBatchDelete() {
  const userIds = selectedUsers.value.map((item) => item.userId).filter(Boolean) as ApiId[];
  if (!userIds.length) {
    ElMessage.warning('请选择要移出租户的成员');
    return;
  }
  ElMessageBox.confirm(
    `确认将选中的 ${userIds.length} 个成员移出租户？角色和全部部门关系会被撤销。`,
    '批量移出租户成员',
    {
      confirmButtonText: '移出租户',
      cancelButtonText: '取消',
      type: 'warning',
    },
  )
    .then(async () => {
      const count = await userApi.deleteBatch(userIds);
      ElMessage.success(`已移除 ${count} 个成员`);
      await loadData();
    })
    .catch(() => {});
}

async function handleRemoveFromOrg(row: IdentityUserVO) {
  if (!row.orgRelationId) return;
  await ElMessageBox.confirm(
    `确认将成员「${row.username}」移出当前部门？成员仍保留在租户中，其角色和其它部门归属不受影响。`,
    '移出当前部门',
    {
      confirmButtonText: '移出部门',
      cancelButtonText: '取消',
      type: 'warning',
    },
  );
  await orgApi.removeMember(row.orgRelationId);
  ElMessage.success('已移出当前部门');
  await loadData();
}

function handleStatus(row: IdentityUserVO) {
  if (!row.userId) return;
  const nextStatus = row.status === 1 ? 0 : 1;
  const action = nextStatus === 1 ? '启用' : '禁用';
  ElMessageBox.confirm(`确认${action}成员「${row.username}」?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await userApi.updateStatus(row.userId!, nextStatus);
      ElMessage.success(`${action}成功`);
      await loadData();
    })
    .catch(() => {});
}

function handleResetPassword(row: IdentityUserVO) {
  if (!row.userId) return;
  resetPasswordUser.value = row;
  resetPasswordForm.password = '';
  resetPasswordDialogVisible.value = true;
  resetPasswordFormRef.value?.clearValidate();
}

async function handleResetPasswordSubmit() {
  if (!resetPasswordUser.value?.userId || !resetPasswordFormRef.value || resetPasswordLoading.value) return;
  const valid = await resetPasswordFormRef.value.validate().catch(() => false);
  if (!valid) return;

  resetPasswordLoading.value = true;
  try {
    await userApi.resetPassword(resetPasswordUser.value.userId, resetPasswordForm.password);
    ElMessage.success('重置成功');
    resetPasswordDialogVisible.value = false;
    resetPasswordForm.password = '';
    await loadData();
  } finally {
    resetPasswordLoading.value = false;
  }
}

function handleRequirePasswordReset(row: IdentityUserVO) {
  if (!row.userId) return;
  ElMessageBox.confirm(`确认要求成员「${row.username}」下次登录修改密码？`, '要求改密确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await userApi.requirePasswordReset(row.userId!);
      ElMessage.success('已设置下次登录改密');
      await loadData();
    })
    .catch(() => {});
}

function handleUnlock(row: IdentityUserVO) {
  if (!row.userId) return;
  ElMessageBox.confirm(`确认解除成员「${row.username}」的登录锁定？`, '解锁确认', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(async () => {
      await userApi.unlock(row.userId!);
      ElMessage.success('已解锁');
      await loadData();
    })
    .catch(() => {});
}

async function handleAssignRoles(row: IdentityUserVO) {
  if (!row.memberId) {
    ElMessage.warning('当前账号没有机构成员身份，不能分配角色');
    return;
  }
  currentUser.value = row;
  assignDialogVisible.value = true;
  const [roles, assignedRoles] = await Promise.all([roleApi.list(), roleApi.getSubjectRoles(row.memberId)]);
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
    await loadData();
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
    primaryFlag: true,
    leaderFlag: false,
  });
  candidateUsers.value = [];
  orgMemberFormRef.value?.clearValidate();
  orgMemberDialogVisible.value = true;
  await searchCandidateUsers('');
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
  if (!selectedOrg.value) return;
  const sequence = ++candidateSequence;
  const targetOrgId = selectedOrg.value.id;
  candidateLoading.value = true;
  candidateLoadError.value = false;
  try {
    const data = await userApi.page({
      pageNum: 1,
      pageSize: 50,
      keyword: keyword.trim() || undefined,
      excludeOrgId: targetOrgId,
    });
    if (sequence !== candidateSequence || String(selectedOrg.value?.id) !== String(targetOrgId)) return;
    candidateUsers.value = data.list.filter((item) => item.memberId);
  } catch {
    if (sequence === candidateSequence) {
      candidateUsers.value = [];
      candidateLoadError.value = true;
      ElMessage.error('候选成员加载失败，请重试');
    }
  } finally {
    if (sequence === candidateSequence) candidateLoading.value = false;
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

watch(
  () => form.username,
  () => {
    if (!form.userId) {
      clearAccountAvailability();
    }
  },
);

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

.state-alert {
  margin-bottom: 12px;
}

.recoverable-account {
  display: grid;
  gap: 12px;
  margin: 0 0 18px 110px;
}

.recoverable-account__notice {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.6;
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

.org-path-hint {
  width: 100%;
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.role-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.wecom-sync-form {
  margin-bottom: 16px;
}

.sync-target {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 32px;
}

.sync-target-tip {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.sync-result {
  margin-top: 8px;
}

.sync-messages {
  margin-top: 12px;
}

.external-identity-form {
  margin-bottom: 12px;
}

.external-identity-table {
  margin-top: 8px;
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
