<template>
  <CmsResourceView :config="publishConfig" />
</template>

<script setup lang="ts">
import CmsResourceView from './CmsResourceView.vue';
import { useCmsApi } from '../api/context';
import { createCmsResourceConfigs } from './configs';

const { publishConfig } = createCmsResourceConfigs(useCmsApi());
</script>
