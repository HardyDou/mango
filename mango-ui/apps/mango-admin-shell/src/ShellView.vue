<template>
  <Layout />
</template>

<script setup lang="ts">
import Layout from './layout/index.vue';
import { Session } from '@mango/common';
import { registerShellLocalApps } from './runtime/localApps';
import { useUserInfo } from './stores/userInfo';

registerShellLocalApps();

const userInfoStore = useUserInfo();
const sessionUser = Session.get('userInfo');
if (sessionUser) {
  userInfoStore.setUserInfos(sessionUser);
}
</script>
