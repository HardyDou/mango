<template>
  <el-dropdown
    trigger="click"
    @command="handleCommand"
  >
    <div class="layout-breadcrumb-user">
      <el-avatar
        :size="28"
      >
        <el-icon><User /></el-icon>
      </el-avatar>
      <span class="username">{{ currentUser.username || 'Admin' }}</span>
      <span
        v-if="institutionLabel"
        class="institution-context"
        :title="institutionLabel"
      >
        {{ institutionLabel }}
      </span>
      <el-icon class="arrow-icon">
        <ArrowDown />
      </el-icon>
    </div>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-if="institutionLabel"
          disabled
          class="institution-dropdown-item"
        >
          {{ institutionLabel }}
        </el-dropdown-item>
        <el-dropdown-item command="profile">
          <el-icon><User /></el-icon>
          个人中心
        </el-dropdown-item>
        <el-dropdown-item command="password">
          <el-icon><Lock /></el-icon>
          修改密码
        </el-dropdown-item>
        <el-dropdown-item
          divided
          command="logout"
        >
          <el-icon><SwitchButton /></el-icon>
          退出登录
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts" name="breadcrumbUser">
import { User, Lock, SwitchButton, ArrowDown } from '@element-plus/icons-vue';
import { computed } from 'vue';
import { ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
import { Session } from '@mango/common/utils/storage';
import { useUserInfo } from '../../../stores/userInfo';

const router = useRouter();
const storesUserInfo = useUserInfo();
const currentUser = computed(() => storesUserInfo.userInfos);

const institutionLabel = computed(() => {
  const info = currentUser.value;
  return info.tenantName || info.tenantCode || (info.tenantId ? `机构 ${info.tenantId}` : '');
});

const handleCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile');
      break;
    case 'password':
      router.push('/password');
      break;
    case 'logout':
      ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        storesUserInfo.clearUserInfo();
        router.push('/login');
      }).catch(() => {
        // User canceled, do nothing
      });
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
    margin-left: 8px;
    font-size: 13px;
  }

  .institution-context {
    max-width: 96px;
    margin-left: 8px;
    padding-left: 8px;
    overflow: hidden;
    color: var(--mango-color-top-bar);
    font-size: 12px;
    opacity: 0.75;
    text-overflow: ellipsis;
    white-space: nowrap;
    border-left: 1px solid currentColor;
  }

  .arrow-icon {
    margin-left: 4px;
    font-size: 12px;
  }
}

// 下拉菜单样式 - 修复在蓝色背景上的显示
:deep(.el-dropdown-menu) {
  background: var(--mango-bg-color);
  border: 1px solid var(--mango-border-color);

  .el-dropdown-menu__item {
    color: var(--mango-text-color-regular);
    &:hover {
      color: var(--mango-color-primary);
      background: var(--mango-color-menu-hover);
    }
  }

  .institution-dropdown-item {
    max-width: 180px;
    overflow: hidden;
    color: var(--mango-text-color-primary);
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
    opacity: 1;
    cursor: default;
  }
}
</style>
