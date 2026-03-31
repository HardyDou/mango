<template>
  <el-dropdown
    trigger="click"
    @command="handleCommand"
  >
    <div class="layout-breadcrumb-user">
      <el-avatar
        :size="28"
        src="/logo.png"
      />
      <span class="username">{{ userInfos.userInfos?.username || 'Admin' }}</span>
      <el-icon class="arrow-icon">
        <ArrowDown />
      </el-icon>
    </div>
    <template #dropdown>
      <el-dropdown-menu>
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
import { ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
import { Session } from '@/utils/storage';
import { useUserInfo } from '@/stores/userInfo';

const router = useRouter();
const storesUserInfo = useUserInfo();
const { userInfos } = storesUserInfo;

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
        Session.clearSession();
        router.push('/login');
      }).catch(() => {});
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
}
</style>
