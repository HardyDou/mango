<template>
  <MangoLinkPage
    base-url=""
    :headers="headers"
    headline="保函业务快捷入口"
    subtitle="集中访问保函查询、风险核验和常用辅助工具"
  />
</template>

<script setup lang="ts">
import { MangoLinkPage } from '../src';

const headers = {
  'X-Mango-Tenant-Id': '1',
};
</script>
