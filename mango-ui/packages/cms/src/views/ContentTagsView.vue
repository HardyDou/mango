<template>
  <CmsResourceView :config="contentTagConfig" />
</template>

<script setup lang="ts">
import CmsResourceView from './CmsResourceView.vue';
import { contentTagConfig } from './configs';
</script>
