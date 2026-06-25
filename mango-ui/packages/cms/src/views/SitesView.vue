<template>
  <CmsResourceView :config="siteConfig" />
</template>

<script setup lang="ts">
import CmsResourceView from './CmsResourceView.vue';
import { siteConfig } from './configs';
</script>
