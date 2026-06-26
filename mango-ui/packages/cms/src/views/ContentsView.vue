<template>
  <CmsResourceView :config="contentConfig" />
</template>

<script setup lang="ts">
import CmsResourceView from './CmsResourceView.vue';
import { contentConfig } from './configs';
</script>
