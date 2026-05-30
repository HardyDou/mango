<template>
  <MangoAdminHomeView />
</template>

<script setup lang="ts" name="Home">
import { defineAsyncComponent } from 'vue';

const MangoAdminHomeView = defineAsyncComponent(() => import('@mango/admin-shell/home'));
</script>
