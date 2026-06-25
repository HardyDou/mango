<template>
  <CmsResourceView :config="contentCategoryConfig" />
</template>

<script setup lang="ts">
import CmsResourceView from './CmsResourceView.vue';
import { contentCategoryConfig } from './configs';
</script>
