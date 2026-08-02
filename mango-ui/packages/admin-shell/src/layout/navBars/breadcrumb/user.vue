<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <el-dropdown trigger="click" popper-class="layout-breadcrumb-user-popper" @command="handleCommand">
    <div class="layout-breadcrumb-user" :aria-label="`当前用户：${currentDisplayName}`">
      <MangoAvatar :size="28" :source="currentUser.photo">
        <el-icon><User /></el-icon>
      </MangoAvatar>
      <span class="username" data-field="current-user.display-name">
        {{ currentDisplayName }}
      </span>
      <el-icon class="arrow-icon">
        <ArrowDown />
      </el-icon>
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item disabled class="account-summary-dropdown-item">
          <div class="account-summary" data-surface="current-user.summary">
            <MangoAvatar :size="40" :source="currentUser.photo">
              <el-icon><User /></el-icon>
            </MangoAvatar>
            <div class="account-summary__copy">
              <strong>{{ currentDisplayName }}</strong>
              <span :title="organizationLabel">{{ organizationLabel }}</span>
            </div>
          </div>
        </el-dropdown-item>
        <el-dropdown-item divided command="profile">
          <el-icon><User /></el-icon>
          个人中心
        </el-dropdown-item>
        <el-dropdown-item command="password">
          <el-icon><Lock /></el-icon>
          修改密码
        </el-dropdown-item>
        <el-dropdown-item divided command="logout" data-action="auth.logout">
          <el-icon><SwitchButton /></el-icon>
          退出登录
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts" name="breadcrumbUser">
import { User, Lock, SwitchButton, ArrowDown } from '@element-plus/icons-vue';
import { logout } from '@mango/auth';
import { MangoAvatar } from '@mango/common';
import { Session } from '@mango/common/utils/storage';
import { computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
import { useUserInfo } from '../../../stores/userInfo';
import { useTagsViewRoutes } from '../../../stores/tagsViewRoutes';
import { completeLogout } from '../../../runtime/logoutFlow';

const router = useRouter();
const storesUserInfo = useUserInfo();
const storesTagsViewRoutes = useTagsViewRoutes();
const currentUser = computed(() => storesUserInfo.userInfos);
const currentDisplayName = computed(() => currentUser.value.nickname || currentUser.value.username || 'Admin');

const organizationLabel = computed(() => {
  const info = currentUser.value;
  const departmentName = info.departmentName || info.deptName || info.orgName || '';
  const companyName = info.companyName || info.tenantName || info.tenantCode || '';
  const labels = [departmentName || '部门未设置', companyName || '公司未设置'];
  return labels.join('｜');
});

async function confirmLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
  } catch {
    return;
  }

  try {
    await completeLogout({
      revokeSession: logout,
      clearSession: () => Session.clearSession(),
      clearNavigation: () => storesTagsViewRoutes.clearTagsView(),
      clearUserInfo: () => storesUserInfo.clearUserInfo(),
      redirectToLogin: () => router.push('/login'),
    });
  } catch (error) {
    console.error('退出登录失败', error);
    ElMessage.error('退出登录失败，请重试');
  }
}

const handleCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile');
      break;
    case 'password':
      router.push({ path: '/profile', query: { tab: 'password' } });
      break;
    case 'logout':
      void confirmLogout();
      break;
  }
};
</script>

<style scoped lang="scss">
.layout-breadcrumb-user {
  display: flex;
  align-items: center;
  padding: 0 12px;
  height: 40px;
  cursor: pointer;
  color: var(--mango-color-top-bar);
  transition: color 0.2s;

  &:hover {
    opacity: 0.85;
  }

  .username {
    max-width: 120px;
    margin-left: 8px;
    overflow: hidden;
    font-size: 13px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .arrow-icon {
    margin-left: 4px;
    font-size: 12px;
  }
}

:global(.layout-breadcrumb-user-popper .el-dropdown-menu) {
  min-width: 212px;
  background: var(--mango-bg-color);
  border: 1px solid var(--mango-border-color);
}

:global(.layout-breadcrumb-user-popper .el-dropdown-menu__item) {
  height: 40px;
  padding: 0 13px;
  color: var(--mango-text-color-regular);

  .el-icon {
    margin-right: 10px;
    font-size: 16px;
  }

  &:hover {
    color: var(--mango-color-primary);
    background: var(--mango-color-menu-hover);
  }
}

:global(.layout-breadcrumb-user-popper .account-summary-dropdown-item) {
  height: auto;
  padding: 10px 12px;
  opacity: 1;
  cursor: default;

  &:hover {
    color: var(--mango-text-color-primary);
    background: transparent;
  }
}

.account-summary {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  width: 100%;
}

.account-summary__copy {
  display: grid;
  min-width: 0;
  gap: 3px;

  strong,
  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  strong {
    color: var(--mango-text-color-primary);
    font-size: 14px;
    font-weight: 600;
  }

  span {
    color: var(--mango-text-color-secondary);
    font-size: 12px;
  }
}
</style>
