<template>
  <section class="mango-grid-widget-user-profile">
    <div class="mango-grid-widget-user-profile__main">
      <el-avatar
        :size="64"
        :src="avatar"
        class="mango-grid-widget-user-profile__avatar"
      >
        <el-icon><User /></el-icon>
      </el-avatar>

      <div class="mango-grid-widget-user-profile__info">
        <div
          class="mango-grid-widget-user-profile__name"
          :title="displayName"
        >
          {{ displayName }}
        </div>
        <div
          class="mango-grid-widget-user-profile__meta"
          :title="usernameText"
        >
          {{ usernameText }}
        </div>
        <div
          class="mango-grid-widget-user-profile__tenant"
          :title="tenantText"
        >
          {{ tenantText }}
        </div>
      </div>
    </div>

    <div class="mango-grid-widget-user-profile__details">
      <div
        v-for="item in detailItems"
        :key="item.label"
        class="mango-grid-widget-user-profile__detail"
      >
        <div class="mango-grid-widget-user-profile__detail-label">
          {{ item.label }}
        </div>
        <div
          class="mango-grid-widget-user-profile__detail-value"
          :title="item.value"
        >
          {{ item.value }}
        </div>
      </div>
    </div>

    <div class="mango-grid-widget-user-profile__actions">
      <el-button
        plain
        type="primary"
        @click="navigateToProfile"
      >
        <el-icon><User /></el-icon>
        <span>个人中心</span>
      </el-button>
      <el-button
        plain
        @click="navigateToPassword"
      >
        <el-icon><Lock /></el-icon>
        <span>修改密码</span>
      </el-button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { Lock, User } from '@element-plus/icons-vue';
import type { UserProfileWidgetProps } from '../../types';

defineOptions({
  name: 'MangoUserProfileWidget',
});

const props = withDefaults(defineProps<UserProfileWidgetProps>(), {
  profilePath: '/profile',
  passwordPath: '/password',
});

const displayName = computed(() => {
  return props.runtime?.user?.nickname || props.runtime?.user?.username || '未登录用户';
});

const usernameText = computed(() => {
  const username = props.runtime?.user?.username;
  return username ? `账号：${username}` : '账号：-';
});

const tenantText = computed(() => {
  const tenant = props.runtime?.tenant;
  return tenant?.tenantName || tenant?.tenantCode || (tenant?.tenantId ? `机构 ${tenant.tenantId}` : '暂无组织信息');
});

const roleText = computed(() => {
  const roles = props.runtime?.user?.roles?.filter(Boolean) || [];
  return roles.length ? roles.slice(0, 2).join('、') : '暂无角色';
});

const tenantCodeText = computed(() => {
  const tenant = props.runtime?.tenant;
  return tenant?.tenantCode || (tenant?.tenantId ? String(tenant.tenantId) : '-');
});

const appCodeText = computed(() => props.runtime?.user?.appCode || '-');

const detailItems = computed(() => [
  {
    label: '所属租户',
    value: tenantText.value,
  },
  {
    label: '租户编码',
    value: tenantCodeText.value,
  },
  {
    label: '当前角色',
    value: roleText.value,
  },
  {
    label: '应用标识',
    value: appCodeText.value,
  },
]);

const avatar = computed(() => props.runtime?.user?.avatar || '');

async function navigateToProfile(): Promise<void> {
  await navigateTo(props.profilePath);
}

async function navigateToPassword(): Promise<void> {
  await navigateTo(props.passwordPath);
}

async function navigateTo(path: string): Promise<void> {
  await props.runtime?.navigate?.({ path });
}
</script>
