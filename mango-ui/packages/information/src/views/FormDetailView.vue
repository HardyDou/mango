<template>
  <div class="information-page" data-page="information.form.detail">
    <MangoPageBackBar title="返回列表页" :show-refresh="false" @back="goList">
      <template #extra>
        <el-button @click="goHosted">接入方式</el-button>
        <el-button @click="goSubmissions">提交记录</el-button>
        <el-button type="primary" @click="goConfig">配置表单</el-button>
      </template>
    </MangoPageBackBar>

    <el-alert v-if="errorMessage" type="error" :closable="false" show-icon>
      <template #title>
        {{ errorMessage }}
        <el-button link type="primary" @click="load">重试</el-button>
      </template>
    </el-alert>

    <el-card v-loading="loading" shadow="never" class="information-card">
      <div class="information-detail-head">
        <div>
          <h2 class="information-detail-title">{{ detail?.formName || '-' }}</h2>
          <div class="information-detail-meta">
            <span class="information-code">{{ detail?.formCode || '-' }}</span>
            <span> · 创建人：{{ detail?.createdByName || '-' }}</span>
          </div>
        </div>
        <MangoStatusText :tone="formStatusTone(detail?.status)">{{ formStatusLabel(detail?.status) }}</MangoStatusText>
      </div>

      <MangoDescriptionList :items="summaryItems" display="table" :column="3" label-width="96" />

      <div class="information-section">
        <div class="information-section-title">字段配置</div>
        <el-table :data="fields" row-key="fieldCode" empty-text="尚未配置采集项">
          <el-table-column label="字段标签" min-width="150">
            <template #default="{ row }">{{ row.fieldName }}</template>
          </el-table-column>
          <el-table-column label="字段编码" min-width="140">
            <template #default="{ row }"
              ><span class="information-code">{{ row.fieldCode }}</span></template
            >
          </el-table-column>
          <el-table-column label="字段类型" width="120">
            <template #default="{ row }">{{ fieldTypeLabel(row.fieldType) }}</template>
          </el-table-column>
          <el-table-column label="必填" width="90">
            <template #default="{ row }">
              <MangoStatusText :tone="row.required ? 'success' : 'neutral'">{{
                row.required ? '是' : '否'
              }}</MangoStatusText>
            </template>
          </el-table-column>
          <el-table-column label="提示文字" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ row.placeholder || '-' }}</template>
          </el-table-column>
          <el-table-column label="排序" width="80">
            <template #default="{ row }">{{ row.sort }}</template>
          </el-table-column>
        </el-table>
      </div>

      <div class="information-section">
        <div class="information-section-title">接入方式</div>
        <div class="information-access-grid">
          <div class="information-access-card">
            <div class="information-access-label">
              <strong>托管表单</strong>
              <MangoStatusText tone="success">可用</MangoStatusText>
            </div>
            <p>直接使用平台渲染页面。</p>
            <span class="information-code">{{ hostedUrl }}</span>
          </div>
          <div class="information-access-card">
            <div class="information-access-label">
              <strong>API</strong>
              <MangoStatusText tone="success">可用</MangoStatusText>
            </div>
            <p>自定义页面按表单编码提交。</p>
            <span class="information-code">POST /information/open/forms/{{ detail?.formCode }}/submit</span>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { MangoDescriptionList, MangoPageBackBar, MangoStatusText } from '@mango/common';
import { useInformationApi } from '../composables/useInformationApi';
import type { InformationFormDetail } from '../api/information';
import { formatCount, formatDate, formStatusLabel, formStatusTone } from '../domain/status';
import { fieldTypeLabel, parseFields } from '../domain/fields';

const api = useInformationApi();
const route = useRoute();
const router = useRouter();

const detail = ref<InformationFormDetail>();
const loading = ref(false);
const errorMessage = ref('');

const formId = computed(() => String(route.params.id || ''));
const fields = computed(() => parseFields(detail.value?.definitionJson));
const hostedUrl = computed(() =>
  detail.value?.formCode ? `${window.location.origin}/#/information/hosted?formCode=${detail.value.formCode}` : '-',
);
const summaryItems = computed(() => [
  { key: 'formName', label: '表单名称', value: detail.value?.formName },
  { key: 'formCode', label: '表单编码', value: detail.value?.formCode },
  { key: 'description', label: '备注', value: detail.value?.description },
  { key: 'submissionCount', label: '提交量', value: formatCount(detail.value?.submissionCount) },
  { key: 'createdAt', label: '创建日期', value: formatDate(detail.value?.createdAt) },
  { key: 'updatedAt', label: '更新日期', value: formatDate(detail.value?.updatedAt) },
]);

async function load() {
  if (!formId.value) {
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  try {
    detail.value = await api.detailForm(formId.value);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '表单详情加载失败';
  } finally {
    loading.value = false;
  }
}

function goList() {
  return router.push('/information/forms');
}
function goConfig() {
  return router.push(`/information/forms/${formId.value}/config`);
}
function goHosted() {
  return router.push(`/information/forms/${formId.value}/hosted`);
}
function goSubmissions() {
  return router.push(`/information/forms/${formId.value}/submissions`);
}

onMounted(load);
</script>
