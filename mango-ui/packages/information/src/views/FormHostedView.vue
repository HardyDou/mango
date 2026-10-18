<template>
  <div class="information-page" data-page="information.form.hosted">
    <MangoPageBackBar title="返回详情页" :show-refresh="false" @back="goDetail">
      <template #extra>
        <el-button @click="copyUrl">复制页面地址</el-button>
        <el-button type="primary" @click="openHostedPage">打开页面 ↗</el-button>
      </template>
    </MangoPageBackBar>

    <el-alert v-if="errorMessage" type="error" :closable="false" show-icon>
      <template #title>
        {{ errorMessage }}
        <el-button link type="primary" @click="load">重试</el-button>
      </template>
    </el-alert>

    <div v-loading="loading" class="information-hosted-layout">
      <div class="information-browser">
        <div class="information-browser-bar">
          <span class="information-browser-dot" />
          <span class="information-browser-dot" />
          <span class="information-browser-dot" />
          <span class="information-browser-url">{{ hostedUrl }}</span>
        </div>
        <div class="information-browser-body">
          <div class="information-form-card">
            <div class="information-hosted-logo">MANGO · ENTERPRISE</div>
            <InformationFormSurface
              :form-name="detail?.formName"
              :description="detail?.description"
              :fields="fields"
              :values="previewValues"
              :submit-label="'提交'"
              show-submit
              disabled
              plain
              footnote="提交即表示您同意我们按隐私政策使用相关信息"
            />
          </div>
        </div>
      </div>

      <div>
        <span class="information-mode-pill">使用方式 ② · 托管页面</span>
        <div class="information-info-list">
          <div class="information-info-item">
            <h3>适合快速上线</h3>
            <p>配置完成后即可获得页面地址。适用于反馈、报名、咨询等标准采集场景。</p>
          </div>
          <div class="information-info-item">
            <h3>复用同一份定义</h3>
            <p>页面字段、必填规则和校验规则来自当前启用表单，不与 API 模式分叉。</p>
          </div>
          <div class="information-info-item">
            <h3>可嵌入业务页面</h3>
            <p>后续可提供 iframe 或组件嵌入，但不在本轮 MVP 中设计嵌入协议。</p>
          </div>
        </div>

        <el-card shadow="never" class="information-card information-access-panel">
          <div class="information-section-title">接入方式</div>
          <div class="information-access-card">
            <div class="information-access-label">
              <strong>浏览器页面 / 托管页面</strong>
              <MangoStatusText tone="success">无需密钥</MangoStatusText>
            </div>
            <p>使用公开表单编码提交。平台通过启用状态、字段校验、限流和验证码控制滥用。</p>
            <div class="information-api-box">
              <span class="information-api-method">POST</span>
              /information/open/forms/{{ detail?.formCode }}/submit
            </div>
          </div>
          <div class="information-access-card">
            <div class="information-access-label">
              <strong>服务端 API</strong>
              <MangoStatusText tone="warning">需要表单专属 SK</MangoStatusText>
            </div>
            <p>
              表单专属 SK 只放在服务端环境变量中，不得写入浏览器代码。后台始终显示当前表单
              SK，可复制、重新生成，并限制到当前租户和表单。
            </p>
            <div class="information-api-box">
              <span class="information-api-key">X-Mango-SK</span>
              <span class="information-api-value">{{ secretText }}</span>
              <el-button link type="primary" @click="copySecret">复制</el-button>
            </div>
            <el-button
              style="margin-top: 10px"
              :loading="regenerating"
              data-action="information.form.regenerate-secret"
              @click="regenerate"
            >
              重新生成密钥
            </el-button>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { MangoPageBackBar, MangoStatusText } from '@mango/common';
import InformationFormSurface from '../components/InformationFormSurface.vue';
import { useInformationApi } from '../composables/useInformationApi';
import type { InformationFormDetail } from '../api/information';
import { defaultValueFor, parseFields } from '../domain/fields';

const api = useInformationApi();
const route = useRoute();
const router = useRouter();

const detail = ref<InformationFormDetail>();
const loading = ref(false);
const regenerating = ref(false);
const errorMessage = ref('');

const formId = computed(() => String(route.params.id || ''));
const fields = computed(() => parseFields(detail.value?.definitionJson));
const previewValues = computed(() =>
  Object.fromEntries(fields.value.map((field) => [field.fieldCode, defaultValueFor(field)])),
);
/** 对外分享地址必须是绝对地址，访客直接打开即可提交，无需登录。 */
const hostedUrl = computed(() =>
  detail.value?.formCode ? `${window.location.origin}/#/information/hosted?formCode=${detail.value.formCode}` : '-',
);
const secretText = computed(() => {
  const secret = detail.value?.serverSecret;
  return secret ? `${secret.slice(0, 8)}••••••••` : '-';
});

async function load() {
  if (!formId.value) {
    return;
  }
  loading.value = true;
  errorMessage.value = '';
  try {
    detail.value = await api.detailForm(formId.value);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '接入信息加载失败';
  } finally {
    loading.value = false;
  }
}

async function copyUrl() {
  await navigator.clipboard?.writeText(hostedUrl.value);
  ElMessage.success('页面地址已复制');
}

async function copySecret() {
  if (!detail.value?.serverSecret) {
    return;
  }
  await navigator.clipboard?.writeText(detail.value.serverSecret);
  ElMessage.success('服务端密钥已复制');
}

function openHostedPage() {
  if (hostedUrl.value !== '-') {
    window.open(hostedUrl.value, '_blank', 'noopener');
  }
}

async function regenerate() {
  try {
    await ElMessageBox.confirm('重新生成后旧密钥立即失效，确认继续？', '重新生成密钥', { type: 'warning' });
  } catch {
    return;
  }
  regenerating.value = true;
  try {
    await api.regenerateFormSecret(formId.value);
    ElMessage.success('密钥已更新');
    await load();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '密钥更新失败');
  } finally {
    regenerating.value = false;
  }
}

function goDetail() {
  return router.push(`/information/forms/${formId.value}/detail`);
}

onMounted(load);
</script>
