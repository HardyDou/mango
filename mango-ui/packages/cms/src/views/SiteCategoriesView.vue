<template>
  <CmsResourceView :config="siteCategoryConfig" />
</template>

<script setup lang="ts">
import CmsResourceView from './CmsResourceView.vue';
import { siteCategoryConfig } from './configs';
</script>
