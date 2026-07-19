<template>
  <CmsResourceView :config="contentConfig" />
</template>

<script setup lang="ts">
import CmsResourceView from './CmsResourceView.vue';
import { useCmsApi } from '../api/context';
import { createCmsResourceConfigs } from './configs';

const { contentConfig } = createCmsResourceConfigs(useCmsApi());
</script>
