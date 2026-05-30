<template>
  <section class="{{moduleKebab}}-{{aggregateKebab}}-page" data-mango-layout="list-page">
    <el-form
      :model="query"
      class="{{moduleKebab}}-{{aggregateKebab}}-search"
      data-mango-layout="search"
      inline
      label-width="96px"
      @submit.prevent
    >
      <el-form-item label="{{aggregatePascal}}名称">
        <el-input
          v-model="query.name"
          clearable
          placeholder="请输入{{aggregatePascal}}名称"
          class="search-input"
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
        <el-button :disabled="loading" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="{{moduleKebab}}-{{aggregateKebab}}-actions" data-mango-layout="actions">
      <div class="primary-actions">
        <el-button type="primary" @click="handleSearch">刷新</el-button>
      </div>
      <div class="view-actions">
        <el-button :disabled="loading" @click="handleReset">清空筛选</el-button>
      </div>
    </div>

    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      class="{{moduleKebab}}-{{aggregateKebab}}-error"
      type="error"
      show-icon
      @close="errorMessage = ''"
    />

    <el-table
      v-loading="loading"
      :data="records"
      class="{{moduleKebab}}-{{aggregateKebab}}-table"
      data-mango-layout="table"
      row-key="id"
      empty-text="暂无{{aggregatePascal}}数据"
    >
      <el-table-column prop="id" label="业务标识" min-width="220" show-overflow-tooltip />
      <el-table-column prop="name" label="{{aggregatePascal}}名称" min-width="180" show-overflow-tooltip />
    </el-table>

    <div class="{{moduleKebab}}-{{aggregateKebab}}-pagination" data-mango-layout="pagination">
      <el-pagination
        v-model:current-page="query.pageNo"
        v-model:page-size="query.pageSize"
        :disabled="loading"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        background
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadData"
        @size-change="handleSizeChange"
      />
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { page{{aggregatePascal}}, type {{aggregatePascal}}VO } from '@{{projectKebab}}/{{moduleKebab}}-api';

const loading = ref(false);
const records = ref<{{aggregatePascal}}VO[]>([]);
const total = ref(0);
const errorMessage = ref('');
const query = reactive({
  pageNo: 1,
  pageSize: 20,
  name: '',
});

async function loadData() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const result = await page{{aggregatePascal}}(query);
    records.value = result.list;
    total.value = Number(result.total || 0);
  } catch (error) {
    records.value = [];
    total.value = 0;
    errorMessage.value = error instanceof Error ? error.message : '加载{{aggregatePascal}}列表失败';
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNo = 1;
  void loadData();
}

function handleReset() {
  query.pageNo = 1;
  query.name = '';
  void loadData();
}

function handleSizeChange() {
  query.pageNo = 1;
  void loadData();
}

onMounted(() => {
  void loadData();
});
</script>

<style scoped>
.{{moduleKebab}}-{{aggregateKebab}}-page {
  padding: 16px;
}

.{{moduleKebab}}-{{aggregateKebab}}-search,
.{{moduleKebab}}-{{aggregateKebab}}-actions,
.{{moduleKebab}}-{{aggregateKebab}}-table,
.{{moduleKebab}}-{{aggregateKebab}}-error {
  margin-bottom: 12px;
}

.{{moduleKebab}}-{{aggregateKebab}}-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.search-input {
  width: 240px;
}

.{{moduleKebab}}-{{aggregateKebab}}-pagination {
  display: flex;
  justify-content: flex-end;
}
</style>
