<template>
  <div class="file-settings-container">
    <el-card>
      <template #header>
        <div class="settings-header">
          <div>
            <div class="settings-title">文件配置</div>
            <div class="settings-subtitle">
              当前上传上限 {{ formatBytes(form.maxSize) }}
              <span v-if="form.defaultConfig">，使用 yml 默认值</span>
            </div>
          </div>
          <el-button
            v-auth="'file:settings:edit'"
            type="primary"
            :loading="saving"
            @click="handleSave"
          >
            保存配置
          </el-button>
        </div>
      </template>

      <el-skeleton v-if="loading" :rows="8" animated />
      <el-form
        v-else
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="150px"
        class="settings-form"
      >
        <section class="settings-section">
          <div class="section-title">上传准入</div>
          <el-row :gutter="20">
            <el-col :xs="24" :md="12">
              <el-form-item label="单文件大小" prop="maxSize">
                <el-input-number
                  v-model="maxSizeMb"
                  :min="1"
                  :max="serverUploadLimit.maxFileSizeMb"
                  :precision="0"
                  controls-position="right"
                  class="number-input"
                />
                <span class="unit-text">MB</span>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="默认访问级别">
                <el-select v-model="form.defaultAccessLevel" class="wide-input">
                  <el-option label="机构私有" value="PRIVATE" />
                  <el-option label="公开读取" value="PUBLIC_READ" />
                  <el-option label="内部文件" value="INTERNAL" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-alert
            class="limit-alert"
            type="warning"
            show-icon
            :closable="false"
          >
            <template #title>
              上传传输上限：单文件 {{ formatBytes(serverUploadLimit.maxFileSize) }}，单次请求 {{ formatBytes(serverUploadLimit.maxRequestSize) }}
            </template>
            文件中心的单文件大小不能超过单文件传输上限；批量上传时所有文件加起来不能超过单次请求上限。原因是 multipart 会先由 Spring/Tomcat 解析，超过上限会在进入文件中心业务校验前被容器拦截。
          </el-alert>
          <el-form-item label="允许扩展名">
            <el-input
              v-model="allowedExtensionsText"
              type="textarea"
              :rows="2"
              placeholder="留空表示不限制，如 jpg,png,pdf,docx,xlsx"
            />
          </el-form-item>
          <el-form-item label="禁止扩展名">
            <el-input
              v-model="blockedExtensionsText"
              type="textarea"
              :rows="2"
              placeholder="如 exe,bat,cmd,sh,jar"
            />
          </el-form-item>
          <el-row :gutter="20">
            <el-col :xs="24" :md="12">
              <el-form-item label="内容类型校验">
                <el-switch v-model="form.contentTypeCheckEnabled" active-text="启用" inactive-text="停用" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="允许 Content-Type">
            <el-input
              v-model="allowedContentTypesText"
              type="textarea"
              :rows="2"
              placeholder="留空表示不限制，如 image/png,application/pdf"
            />
          </el-form-item>
          <el-form-item label="禁止 Content-Type">
            <el-input
              v-model="blockedContentTypesText"
              type="textarea"
              :rows="2"
              placeholder="如 application/x-msdownload,application/x-sh"
            />
          </el-form-item>
        </section>

        <section class="settings-section">
          <div class="section-title">命名与去重</div>
          <el-row :gutter="20">
            <el-col :xs="24" :md="8">
              <el-form-item label="重名处理">
                <el-select v-model="form.duplicateNameStrategy" class="wide-input">
                  <el-option label="拒绝上传" value="REJECT" />
                  <el-option label="自动重命名" value="AUTO_RENAME" />
                  <el-option label="允许重复" value="ALLOW" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="8">
              <el-form-item label="按目录隔离">
                <el-switch v-model="form.duplicateCheckDirectoryScoped" active-text="启用" inactive-text="停用" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="8">
              <el-form-item label="对象命名">
                <el-select v-model="form.objectNameStrategy" class="wide-input">
                  <el-option label="日期 + UUID" value="DATE_UUID" />
                  <el-option label="内容哈希" value="HASH" />
                  <el-option label="原始文件名" value="ORIGINAL" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :xs="24" :md="8">
              <el-form-item label="秒传">
                <el-switch
                  v-model="form.instantUploadEnabled"
                  active-text="启用"
                  inactive-text="停用"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="8">
              <el-form-item label="秒传范围">
                <el-select v-model="form.instantUploadScope" class="wide-input">
                  <el-option label="机构内匹配" value="TENANT" />
                  <el-option label="全局匹配" value="GLOBAL" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </section>

        <section class="settings-section">
          <div class="section-title">直传与访问</div>
          <el-row :gutter="20">
            <el-col :xs="24" :md="8">
              <el-form-item label="浏览器直传">
                <el-switch
                  v-model="form.directUploadEnabled"
                  active-text="启用"
                  inactive-text="停用"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="8">
              <el-form-item label="直传有效期" prop="directUploadExpireSeconds">
                <el-input-number
                  v-model="form.directUploadExpireSeconds"
                  :min="1"
                  :max="86400"
                  controls-position="right"
                  class="number-input"
                />
                <span class="unit-text">秒</span>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="8">
              <el-form-item label="限时访问令牌">
                <el-switch
                  v-model="form.accessTokenEnabled"
                  active-text="启用"
                  inactive-text="停用"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :xs="24" :md="12">
              <el-form-item label="文件访问方式">
                <el-radio-group v-model="form.accessMode">
                  <el-radio-button label="PROXY">Java代理</el-radio-button>
                  <el-radio-button label="DIRECT">存储直连</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="公开文件签名">
                <el-switch
                  v-model="form.publicReadRequiresToken"
                  active-text="强制"
                  inactive-text="不强制"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :xs="24" :md="12">
              <el-form-item label="访问有效期" prop="accessTokenExpireSeconds">
                <el-input-number
                  v-model="form.accessTokenExpireSeconds"
                  :min="1"
                  :max="86400"
                  controls-position="right"
                  class="number-input"
                />
                <span class="unit-text">秒</span>
              </el-form-item>
            </el-col>
          </el-row>
          <el-alert
            title="Java代理适合默认安全访问；存储直连会优先返回 MinIO/OSS/COS/S3 或本地静态服务地址，缺少公开访问配置时自动退回 Java代理。"
            type="info"
            show-icon
            :closable="false"
          />
        </section>

        <section class="settings-section">
          <div class="section-title">预览策略</div>
          <el-form-item label="文档预览服务">
            <el-input
              v-model="form.previewProviderUrl"
              placeholder="如 http://127.0.0.1:8012/onlinePreview"
              clearable
            />
          </el-form-item>
          <el-row :gutter="20">
            <el-col :xs="24" :md="12">
              <el-form-item label="预览有效期" prop="previewExpireSeconds">
                <el-input-number
                  v-model="form.previewExpireSeconds"
                  :min="1"
                  :max="86400"
                  controls-position="right"
                  class="number-input"
                />
                <span class="unit-text">秒</span>
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="外部预览类型">
            <el-input
              v-model="previewExternalExtensionsText"
              type="textarea"
              :rows="3"
              placeholder="如 doc,docx,xls,xlsx,ppt,pptx,odt,ods,ofd"
            />
          </el-form-item>
        </section>

        <section class="settings-section">
          <div class="section-title">归档与保留</div>
          <el-row :gutter="20">
            <el-col :xs="24" :md="8">
              <el-form-item label="保留归档记录">
                <el-switch v-model="form.archiveRetainEnabled" active-text="启用" inactive-text="停用" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="8">
              <el-form-item label="保留天数" prop="archiveRetainDays">
                <el-input-number
                  v-model="form.archiveRetainDays"
                  :min="1"
                  :max="3650"
                  controls-position="right"
                  class="number-input"
                />
                <span class="unit-text">天</span>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="8">
              <el-form-item label="物理删除">
                <el-switch v-model="form.physicalDeleteEnabled" active-text="启用" inactive-text="停用" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-alert
            title="归档后默认从普通列表隐藏，并禁止普通预览和下载；开启物理删除时，仅在没有其他文件记录引用同一对象后才删除底层对象。"
            type="info"
            show-icon
            :closable="false"
          />
        </section>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import {
  defaultFileSettings,
  fileSettingsApi,
  formatBytes,
  parseExtensions,
  parseTextList,
  stringifyExtensions,
  stringifyTextList,
  type FileSettings,
} from '../../api/fileSettings';

const loading = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();
const form = reactive<FileSettings>({ ...defaultFileSettings });
const allowedExtensionsText = ref('');
const blockedExtensionsText = ref('');
const previewExternalExtensionsText = ref('');
const allowedContentTypesText = ref('');
const blockedContentTypesText = ref('');
const serverUploadLimit = {
  maxFileSize: 512 * 1024 * 1024,
  maxRequestSize: 1024 * 1024 * 1024,
  maxFileSizeMb: 512,
};

const maxSizeMb = computed({
  get: () => Math.max(1, Math.round(Number(form.maxSize || 0) / 1024 / 1024)),
  set: (value: number) => {
    form.maxSize = Number(value || 1) * 1024 * 1024;
  },
});

const rules: FormRules = {
  maxSize: [{ required: true, message: '请输入单文件大小限制', trigger: 'blur' }],
  directUploadExpireSeconds: [{ required: true, message: '请输入直传有效期', trigger: 'blur' }],
  accessTokenExpireSeconds: [{ required: true, message: '请输入访问有效期', trigger: 'blur' }],
  previewExpireSeconds: [{ required: true, message: '请输入预览有效期', trigger: 'blur' }],
  archiveRetainDays: [{ required: true, message: '请输入归档保留天数', trigger: 'blur' }],
};

async function loadSettings() {
  loading.value = true;
  try {
    Object.assign(form, await fileSettingsApi.get());
    syncTextFieldsFromForm();
  } finally {
    loading.value = false;
  }
}

async function handleSave() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    syncFormFromTextFields();
    await fileSettingsApi.save(form);
    ElMessage.success('保存成功');
    await loadSettings();
  } finally {
    saving.value = false;
  }
}

function syncTextFieldsFromForm() {
  allowedExtensionsText.value = stringifyExtensions(form.allowedExtensions);
  blockedExtensionsText.value = stringifyExtensions(form.blockedExtensions);
  previewExternalExtensionsText.value = stringifyExtensions(form.previewExternalExtensions);
  allowedContentTypesText.value = stringifyTextList(form.allowedContentTypes);
  blockedContentTypesText.value = stringifyTextList(form.blockedContentTypes);
}

function syncFormFromTextFields() {
  form.allowedExtensions = parseExtensions(allowedExtensionsText.value);
  form.blockedExtensions = parseExtensions(blockedExtensionsText.value);
  form.previewExternalExtensions = parseExtensions(previewExternalExtensionsText.value);
  form.allowedContentTypes = parseTextList(allowedContentTypesText.value);
  form.blockedContentTypes = parseTextList(blockedContentTypesText.value);
}

onMounted(loadSettings);
</script>

<style scoped>
.file-settings-container {
  padding: 0;
}

.settings-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.settings-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.settings-subtitle {
  margin-top: 4px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.settings-form {
  max-width: 1080px;
}

.settings-section {
  padding: 4px 0 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  margin-bottom: 24px;
}

.settings-section:last-child {
  border-bottom: 0;
  margin-bottom: 0;
  padding-bottom: 0;
}

.section-title {
  margin: 0 0 18px 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.number-input {
  width: 180px;
}

.wide-input {
  width: 100%;
}

.unit-text {
  margin-left: 8px;
  color: var(--el-text-color-secondary);
}

.limit-alert {
  margin: 0 0 18px;
}
</style>
