<!-- eslint-disable vue/multi-word-component-names -->
<template>
  <el-dropdown trigger="click" popper-class="layout-breadcrumb-user-popper" @command="handleCommand">
    <div class="layout-breadcrumb-user" :aria-label="`当前用户：${currentDisplayName}`" data-action="user-menu.open">
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
            <MangoAvatar :size="44" :source="currentUser.photo">
              <el-icon><User /></el-icon>
            </MangoAvatar>
            <div class="account-summary__copy">
              <strong>{{ currentDisplayName }}</strong>
              <span :title="organizationLabel">{{ organizationLabel }}</span>
            </div>
          </div>
        </el-dropdown-item>
        <el-dropdown-item divided command="profile" data-action="profile.open">
          <el-icon><User /></el-icon>
          个人中心
        </el-dropdown-item>
        <el-dropdown-item command="password" data-action="profile.password">
          <el-icon><Lock /></el-icon>
          修改密码
        </el-dropdown-item>
        <el-dropdown-item command="theme" data-action="profile.theme">
          <el-icon><Brush /></el-icon>
          主题设置
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
import { ArrowDown, Brush, Lock, SwitchButton, User } from '@element-plus/icons-vue';
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
    case 'theme':
      router.push({ path: '/profile', query: { tab: 'theme' } });
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
  height: 40px;
  padding: 0 8px;
  border-radius: 4px;
  cursor: pointer;
  color: var(--mango-color-top-bar);
  transition:
    background-color 0.18s ease,
    opacity 0.18s ease;

  &:hover {
    background: color-mix(in srgb, var(--mango-color-top-bar) 10%, transparent);
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

:global(.layout-breadcrumb-user-popper.el-popper) {
  padding: 0;
  background: var(--mango-bg-color);
  border: 1px solid var(--mango-border-color);
  border-radius: 4px;
  box-shadow: var(--mango-shadow-light);
}

:global(.layout-breadcrumb-user-popper .el-popper__arrow::before) {
  background: var(--mango-bg-color);
  border-color: var(--mango-border-color);
}

:global(.layout-breadcrumb-user-popper .el-dropdown-menu) {
  min-width: 244px;
  padding: 7px 0;
  background: transparent;
  border: 0;
  box-shadow: none;
}

:global(.layout-breadcrumb-user-popper .el-dropdown-menu__item) {
  height: 40px;
  padding: 0 18px;
  color: var(--mango-text-color);
  font-size: 14px;
  line-height: 40px;
}

:global(.layout-breadcrumb-user-popper .el-dropdown-menu__item .el-icon) {
  margin-right: 12px;
  color: inherit;
  font-size: 18px;
}

:global(.layout-breadcrumb-user-popper .el-dropdown-menu__item:not(.is-disabled):hover),
:global(.layout-breadcrumb-user-popper .el-dropdown-menu__item:not(.is-disabled):focus) {
  color: var(--mango-text-color);
  background: var(--mango-color-menu-hover);
}

:global(.layout-breadcrumb-user-popper .el-dropdown-menu__item--divided) {
  margin-top: 7px;
  border-top-color: var(--mango-border-color);
}

:global(.layout-breadcrumb-user-popper .account-summary-dropdown-item) {
  height: auto;
  min-height: 68px;
  padding: 11px 16px 12px;
  opacity: 1;
  cursor: default;
  color: var(--mango-text-color);
  background: transparent;
}

:global(.layout-breadcrumb-user-popper .account-summary-dropdown-item:hover) {
  color: var(--mango-text-color);
  background: transparent;
}

.account-summary {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  align-items: center;
  gap: 12px;
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
    color: var(--mango-text-color);
    font-size: 15px;
    font-weight: 600;
    line-height: 22px;
  }

  span {
    color: var(--mango-text-color-regular);
    font-size: 13px;
    line-height: 20px;
  }
}
</style>
