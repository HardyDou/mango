<template>
  <component :is="shellComponent" />
</template>

<script setup lang="ts">
import { computed } from 'vue';
import Layout from './layout/index.vue';
import { Session } from '@mango/common/utils/storage';
import { getMangoAdminShellOptions } from './config';
import { registerShellLocalApps } from './runtime/localApps';
import { useUserInfo } from './stores/userInfo';

registerShellLocalApps();

const shellComponent = computed(() => getMangoAdminShellOptions().components?.layout || Layout);

const userInfoStore = useUserInfo();
const sessionUser = Session.get('userInfo');
if (sessionUser) {
  userInfoStore.setUserInfos(sessionUser);
}
</script>
