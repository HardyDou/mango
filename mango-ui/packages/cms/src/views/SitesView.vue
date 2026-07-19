<template>
  <CmsResourceView :config="siteConfig" />
</template>

<script setup lang="ts">
import CmsResourceView from './CmsResourceView.vue';
import { useCmsApi } from '../composables/useCmsApi';
import { createCmsResourceConfigs } from './configs';

const { siteConfig } = createCmsResourceConfigs(useCmsApi());
</script>
