<template>
  <div class="information-page" data-page="information.form.detail">
    <MangoPageBackBar title="返回列表页" :show-refresh="false" @back="goList">
      <template #extra>
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

    <div v-loading="loading" class="information-detail-body">
      <!-- 上：基础信息 -->
      <el-card shadow="never" class="information-card">
        <div class="information-detail-head">
          <div>
            <h2 class="information-detail-title">{{ detail?.formName || '-' }}</h2>
            <div class="information-detail-meta">
              <span class="information-code">{{ detail?.formCode || '-' }}</span>
              <span> · 创建人：{{ detail?.createdByName || '-' }}</span>
            </div>
          </div>
          <MangoStatusText :tone="formStatusTone(detail?.status)">{{
            formStatusLabel(detail?.status)
          }}</MangoStatusText>
        </div>
        <MangoDescriptionList :items="summaryItems" display="table" :column="3" label-width="96" />
      </el-card>

      <!-- 下：接入方式白色卡片，内部左页面方式、右接口方式 -->
      <el-card
        shadow="never"
        class="information-card information-access-section"
        data-surface="information.form.access"
      >
        <div class="information-section-title">接入方式</div>
        <p class="information-access-hint">
          接入方式分为两种：<strong>页面接入</strong>把托管页面地址发给访客，免登录直接填写；<strong>接口接入</strong>在自己的服务端调用提交接口，需要当前表单专属
          SK。
        </p>
        <div class="information-detail-split">
          <el-card shadow="never" class="information-card information-access-method" data-access-method="page">
            <div class="information-access-head">
              <span class="information-mode-pill">方式 ① · 页面接入</span>
              <MangoStatusText :tone="detail?.status === 'PUBLISHED' ? 'success' : 'info'">
                {{ detail?.status === 'PUBLISHED' ? '无需密钥' : '停用中' }}
              </MangoStatusText>
            </div>
            <div class="information-access-method__body">
              <p class="information-access-desc">把下面的地址发给访客，打开即可填写提交，无需登录也不需要密钥。</p>

              <div class="information-access-field">
                <div class="information-access-subtitle">页面地址</div>
                <div class="information-api-box">
                  <span class="information-api-url">{{ hostedUrl }}</span>
                  <el-button link type="primary" data-access-action="copy-hosted" @click="copyHosted">复制</el-button>
                </div>
              </div>

              <div class="information-access-field">
                <div class="information-access-subtitle">页面效果</div>
                <div class="information-hosted-preview" data-surface="information.hosted.preview">
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
                          :rules="rules"
                          :values="previewValues"
                          :runtime-key="rules.length"
                          disabled
                          plain
                          footnote="提交即表示您同意我们按隐私政策使用相关信息"
                        />
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div class="information-access-field">
                <div class="information-access-subtitle">
                  <span>页面嵌入</span>
                  <el-button link type="primary" data-access-action="copy-embed" @click="copyEmbed">复制代码</el-button>
                </div>
                <pre class="information-code-block" data-sample="embed"><code>{{ embedCode }}</code></pre>
              </div>

              <div class="information-access-actions">
                <el-button link type="primary" data-access-action="open-hosted" @click="openHosted">打开页面</el-button>
              </div>
            </div>
          </el-card>

          <el-card shadow="never" class="information-card information-access-method" data-access-method="api">
            <div class="information-access-head">
              <span class="information-mode-pill">方式 ② · 接口接入</span>
              <MangoStatusText :tone="detail?.status === 'PUBLISHED' ? 'warning' : 'info'">
                {{ detail?.status === 'PUBLISHED' ? '需要表单专属 SK' : '停用中' }}
              </MangoStatusText>
            </div>
            <div class="information-access-method__body">
              <p class="information-access-desc">
                在自己的服务端调用提交接口，用表单专属 SK 鉴权。SK 只放服务端环境变量，不得写入浏览器代码。
              </p>

              <div class="information-access-field">
                <div class="information-access-subtitle">接口地址</div>
                <div class="information-api-box">
                  <span class="information-api-method">POST</span>
                  <span class="information-api-url">{{ endpoint() }}</span>
                  <el-button link type="primary" data-access-action="copy-endpoint" @click="copyEndpoint"
                    >复制</el-button
                  >
                </div>
              </div>

              <div class="information-access-field">
                <div class="information-access-subtitle">表单专属 SK</div>
                <div class="information-api-box">
                  <span class="information-api-key">X-Mango-SK</span>
                  <span class="information-api-value">{{
                    secretVisible ? detail?.serverSecret || '-' : secretText
                  }}</span>
                  <el-button link type="primary" data-secret-action="toggle" @click="secretVisible = !secretVisible">
                    {{ secretVisible ? '隐藏' : '显示' }}
                  </el-button>
                  <el-button link type="primary" data-secret-action="copy" @click="copySecret">复制</el-button>
                </div>
                <div class="information-access-actions">
                  <el-button
                    :loading="regenerating"
                    data-action="information.form.regenerate-secret"
                    @click="regenerate"
                  >
                    重新生成密钥
                  </el-button>
                </div>
              </div>

              <div class="information-access-field">
                <div class="information-access-subtitle">
                  <span>接入代码示例</span>
                  <el-button
                    link
                    type="primary"
                    data-access-action="copy-sample"
                    @click="copySample(currentSample.code)"
                  >
                    复制代码
                  </el-button>
                </div>
                <el-tabs v-model="sampleTab" class="information-code-tabs">
                  <el-tab-pane v-for="sample in samples" :key="sample.key" :label="sample.label" :name="sample.key">
                    <pre class="information-code-block" :data-sample="sample.key"><code>{{ sample.code }}</code></pre>
                  </el-tab-pane>
                </el-tabs>
              </div>

              <div class="information-fields">
                <div class="information-access-subtitle">字段配置</div>
                <div v-if="fields.length" class="information-fields__list">
                  <div v-for="field in fields" :key="field.fieldCode" class="information-fields__row">
                    <span class="information-fields__index">{{ field.sort }}</span>
                    <div class="information-fields__main">
                      <div class="information-fields__title">
                        <strong>{{ field.fieldName }}</strong>
                        <span class="information-code">{{ field.fieldCode }}</span>
                        <MangoStatusText v-if="field.required" tone="danger">必填</MangoStatusText>
                      </div>
                      <div class="information-fields__meta">
                        {{ fieldTypeLabel(field.fieldType)
                        }}<template v-if="field.placeholder"> · {{ field.placeholder }}</template>
                      </div>
                    </div>
                  </div>
                </div>
                <el-empty v-else description="尚未配置采集项" :image-size="60" />
              </div>
            </div>
          </el-card>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { MangoDescriptionList, MangoPageBackBar, MangoStatusText } from '@mango/common';
import InformationFormSurface from '../components/InformationFormSurface.vue';
import { useInformationApi } from '../composables/useInformationApi';
import type { InformationFormDetail } from '../api/information';
import { formatCount, formatDate, formStatusLabel, formStatusTone } from '../domain/status';
import { fieldTypeLabel } from '../domain/fields';
import { parseRules, rulesToFields } from '../domain/rules';
import { buildAccessSamples, buildEmbedCode, type AccessSampleContext } from '../domain/accessSamples';

const api = useInformationApi();
const route = useRoute();
const router = useRouter();

const detail = ref<InformationFormDetail>();
const loading = ref(false);
const regenerating = ref(false);
const sampleTab = ref('curl');
const secretVisible = ref(false);
const errorMessage = ref('');

const formId = computed(() => String(route.params.id || ''));
const rules = computed(() => parseRules(detail.value?.definitionJson));
const fields = computed(() => rulesToFields(rules.value));
const previewValues = computed(() => Object.fromEntries(fields.value.map((field) => [field.fieldCode, ''])));
const sampleContext = computed<AccessSampleContext>(() => ({
  origin: window.location.origin,
  host: window.location.host,
  formCode: detail.value?.formCode || '<formCode>',
  formName: detail.value?.formName || '信息采集表单',
  hostedUrl: hostedUrl.value,
  fields: fields.value,
}));
const embedCode = computed(() => buildEmbedCode(sampleContext.value));
const samples = computed(() => buildAccessSamples(sampleContext.value));
const currentSample = computed(
  () => samples.value.find((sample) => sample.key === sampleTab.value) || samples.value[0],
);

const secretText = computed(() => {
  const secret = detail.value?.serverSecret;
  return secret ? `${secret.slice(0, 8)}••••••••` : '-';
});
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

function endpoint(): string {
  return `/information/open/forms/${detail.value?.formCode || ''}/submit`;
}

/** 接入动作失败时提示手动复制，避免只有点击没有反馈。 */
async function copyText(value: string, successText: string, failureHint: string) {
  try {
    await navigator.clipboard.writeText(value);
    ElMessage.success(successText);
  } catch {
    ElMessage.warning(failureHint);
  }
}

function copyHosted() {
  return copyText(hostedUrl.value, '托管表单地址已复制', '浏览器未授权剪贴板，请手动选择地址复制');
}

function copyEndpoint() {
  return copyText(endpoint(), '接口路径已复制', '浏览器未授权剪贴板，请手动选择路径复制');
}

function copyEmbed() {
  return copyText(embedCode.value, '嵌入代码已复制', '浏览器未授权剪贴板，请手动选择代码复制');
}

function copySample(code: string) {
  return copyText(code, '示例代码已复制', '浏览器未授权剪贴板，请手动选择代码复制');
}

function openHosted() {
  window.open(hostedUrl.value, '_blank', 'noopener');
}

function copySecret() {
  return copyText(detail.value?.serverSecret || '', 'SK 已复制', '浏览器未授权剪贴板，请先点击显示再手动复制');
}

/** 轮换密钥前二次确认：旧密钥会立即失效，已接入的服务端需要同步替换。 */
async function regenerate() {
  regenerating.value = true;
  try {
    await ElMessageBox.confirm('重新生成后旧密钥立即失效，已接入的服务端需要同步替换。是否继续？', '重新生成密钥', {
      type: 'warning',
      confirmButtonText: '重新生成',
      cancelButtonText: '取消',
    });
    // 接口只返回新密钥，这里同步到详情上，避免为了拿密钥再打一次详情接口
    const secret = await api.regenerateFormSecret(formId.value);
    if (detail.value) {
      detail.value = { ...detail.value, serverSecret: secret };
    }
    secretVisible.value = false;
    ElMessage.success('密钥已重新生成');
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error instanceof Error ? error.message : '密钥生成失败');
    }
  } finally {
    regenerating.value = false;
  }
}

function goList() {
  return router.push('/information/forms');
}
function goConfig() {
  return router.push(`/information/forms/${formId.value}/config`);
}
function goSubmissions() {
  return router.push(`/information/forms/${formId.value}/submissions`);
}

onMounted(load);
</script>
