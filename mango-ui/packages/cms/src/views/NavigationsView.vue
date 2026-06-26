<template>
  <CmsResourceView :config="navigationConfig" />
</template>

<script setup lang="ts">
import CmsResourceView from './CmsResourceView.vue';
import { navigationConfig } from './configs';
</script>
