<template>
  <section class="{{moduleKebab}}-{{aggregateKebab}}-page">
    <div class="page-toolbar">
      <el-input
        v-model="query.name"
        clearable
        placeholder="{{aggregatePascal}}名称"
        class="search-input"
        @keyup.enter="loadData"
      />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="createRecord">新增</el-button>
    </div>

    <el-table v-loading="loading" :data="records" row-key="id">
      <el-table-column prop="name" label="{{aggregatePascal}}名称" min-width="180" />
      <el-table-column prop="id" label="业务标识" min-width="180" />
    </el-table>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import {
  create{{aggregatePascal}},
  page{{aggregatePascal}},
  type {{aggregatePascal}}VO,
} from '@{{projectKebab}}/{{moduleKebab}}-api';

const loading = ref(false);
const records = ref<{{aggregatePascal}}VO[]>([]);
const query = reactive({
  page: 1,
  size: 20,
  name: '',
});

async function loadData() {
  loading.value = true;
  try {
    const result = await page{{aggregatePascal}}(query);
    records.value = result.records;
  } finally {
    loading.value = false;
  }
}

async function createRecord() {
  const name = query.name.trim() || `{{aggregatePascal}}-${Date.now()}`;
  await create{{aggregatePascal}}({ name });
  query.name = name;
  await loadData();
}

onMounted(() => {
  void loadData();
});
</script>

<style scoped>
.{{moduleKebab}}-{{aggregateKebab}}-page {
  padding: 16px;
}

.page-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

.search-input {
  width: 240px;
}
</style>
