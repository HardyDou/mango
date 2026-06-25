<template>
  <CmsResourceView :config="advertisementConfig" />
</template>

<script setup lang="ts">
import CmsResourceView from './CmsResourceView.vue';
import { advertisementConfig } from './configs';
</script>
