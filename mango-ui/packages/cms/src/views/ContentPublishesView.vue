<template>
  <CmsResourceView :config="publishConfig" />
</template>

<script setup lang="ts">
import CmsResourceView from './CmsResourceView.vue';
import { publishConfig } from './configs';
</script>
