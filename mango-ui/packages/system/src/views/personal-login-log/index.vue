<template>
  <MangoListPage class="personal-login-log-page" data-page="system.personal-login-log">
    <template #search>
      <MangoSearchPanel :model="query" @search="search" @reset="resetSearch">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" clearable placeholder="用户名/IP" @keyup.enter="search" />
        </el-form-item>
        <el-form-item label="登录时间">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            class="personal-login-log-page__time-range"
            @change="applyTimeRange"
          />
        </el-form-item>
      </MangoSearchPanel>
    </template>

    <MangoListPanel>
      <template #view-actions>
        <el-button type="primary" plain :loading="loading" @click="loadData">刷新</el-button>
      </template>

      <el-table
        v-loading="loading"
        :data="rows"
        row-key="id"
        border
        stripe
        data-surface="system.personal-login-log.table"
      >
        <el-table-column prop="loginTime" label="登录时间" width="165" />
        <el-table-column prop="ip" label="IP 地址" width="135" show-overflow-tooltip />
        <el-table-column prop="location" label="IP 地区" width="125" show-overflow-tooltip>
          <template #default="{ row }">{{ row.location || '-' }}</template>
        </el-table-column>
        <el-table-column prop="browser" label="浏览器 UA" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.browser || '-' }}</template>
        </el-table-column>
      </el-table>

      <template #pagination>
        <Pagination v-model:page="query.pageNum" v-model:limit="query.pageSize" :total="total" @pagination="loadData" />
      </template>
    </MangoListPanel>
  </MangoListPage>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { MangoListPage, MangoListPanel, MangoSearchPanel, Pagination } from '@mango/common';
import { loginLogApi, type SysLoginLog } from '../../api/log';

defineOptions({ name: 'PersonalLoginLogView' });

const loading = ref(false);
const rows = ref<SysLoginLog[]>([]);
const total = ref(0);
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', startTime: '', endTime: '' });
const timeRange = ref<[string, string] | undefined>();

async function loadData() {
  loading.value = true;
  try {
    const result = await loginLogApi.currentUserList(query);
    rows.value = result.list;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}

function search() {
  query.pageNum = 1;
  void loadData();
}

function resetSearch() {
  query.pageNum = 1;
  query.keyword = '';
  query.startTime = '';
  query.endTime = '';
  timeRange.value = undefined;
  void loadData();
}

function applyTimeRange(value: [string, string] | undefined) {
  query.startTime = value?.[0] || '';
  query.endTime = value?.[1] || '';
  search();
}

onMounted(() => {
  void loadData();
});
</script>

<style scoped>
.personal-login-log-page {
  min-width: 0;
}

.personal-login-log-page__time-range {
  width: 100%;
}
</style>
