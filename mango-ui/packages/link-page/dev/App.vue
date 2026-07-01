<template>
  <MangoLinkPage
    base-url=""
    :headers="headers"
    :login-defaults="loginDefaults"
  />
</template>

<script setup lang="ts">
import { MangoLinkPage } from '../src';

const headers = {
  'X-Mango-Tenant-Id': '1',
};

const loginDefaults = {
  tenantId: '1',
  tenantCode: 'default',
  realm: 'INTERNAL',
  actorType: 'INTERNAL_USER',
  partyType: 'INTERNAL_ORG',
  partyId: 1,
  appCode: 'internal-admin',
};
</script>
