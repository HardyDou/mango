<template>
  <div class="admin-branding-page">
    <el-card
      class="admin-branding-card"
      shadow="never"
    >
      <template #header>
        <div class="admin-branding-header">
          <div class="admin-branding-title">系统外观配置</div>
        </div>
      </template>

      <el-form
        ref="formRef"
        v-loading="loading"
        :model="form"
        :rules="rules"
        label-width="120px"
      >
        <section class="admin-branding-section">
          <div class="admin-branding-section__title">基础信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="系统名称" prop="title">
                <el-input
                  v-model.trim="form.title"
                  maxlength="100"
                  show-word-limit
                  placeholder="例如：Mango 后台管理系统"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="左上角文案" prop="shortTitle">
                <el-input
                  v-model.trim="form.shortTitle"
                  maxlength="50"
                  show-word-limit
                  placeholder="例如：Mango Admin"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="登录页标题" prop="loginTitle">
                <el-input
                  v-model.trim="form.loginTitle"
                  maxlength="100"
                  show-word-limit
                  placeholder="例如：Mango Admin"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="登录页副标题" prop="loginSubtitle">
                <el-input
                  v-model.trim="form.loginSubtitle"
                  maxlength="200"
                  show-word-limit
                  placeholder="例如：企业级管理平台"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </section>

        <section class="admin-branding-section">
          <div class="admin-branding-section__title">品牌资源</div>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="Logo 图片" prop="logoFile">
                <MUpload
                  v-model="form.logoFile"
                  value-type="id"
                  display="thumbnail"
                  fmt="image"
                  purpose="admin-branding"
                  access-level="PUBLIC_READ"
                  button-text="选择图片"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="浏览器 favicon" prop="faviconFile">
                <MUpload
                  v-model="form.faviconFile"
                  value-type="id"
                  display="thumbnail"
                  fmt="image"
                  purpose="admin-branding"
                  access-level="PUBLIC_READ"
                  button-text="选择文件"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="登录页图片" prop="loginImageFile">
                <MUpload
                  v-model="form.loginImageFile"
                  value-type="id"
                  display="thumbnail"
                  fmt="image"
                  purpose="admin-branding"
                  access-level="PUBLIC_READ"
                  button-text="选择图片"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </section>

        <section class="admin-branding-section">
          <div class="admin-branding-section__title">页脚信息</div>
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="版权信息" prop="footerCopyright">
                <el-input
                  v-model.trim="form.footerCopyright"
                  maxlength="200"
                  show-word-limit
                  placeholder="例如：Copyright © 2026 Mango. All Rights Reserved."
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="备案号" prop="icp">
                <el-input
                  v-model.trim="form.icp"
                  maxlength="100"
                  show-word-limit
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="联系方式" prop="contact">
                <el-input
                  v-model.trim="form.contact"
                  maxlength="100"
                  show-word-limit
                />
              </el-form-item>
            </el-col>
          </el-row>
        </section>

        <section class="admin-branding-section">
          <div class="admin-branding-section__title">启用状态</div>
          <el-form-item label="启用状态">
            <el-switch
              v-model="form.enabled"
            />
            <span class="admin-branding-status-tip">启用后覆盖系统默认展示</span>
          </el-form-item>
        </section>

        <div class="admin-branding-actions">
          <el-button @click="loadConfig">
            重置
          </el-button>
          <el-button
            type="primary"
            :loading="saving"
            @click="handleSave"
          >
            保存配置
          </el-button>
        </div>

      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { ElMessage } from 'element-plus';
import { MUpload } from '@mango/file';
import { adminBrandingApi, type AdminBranding } from '../../api/adminBranding';

defineOptions({
  name: 'AdminBrandingView',
});

const defaultForm: AdminBranding = {
  enabled: true,
  title: 'Mango Admin',
  shortTitle: 'Mango',
  subtitle: '企业级管理平台',
  loginTitle: 'Mango Admin',
  loginSubtitle: '企业级管理平台',
  logoFile: '',
  faviconFile: '',
  loginImageFile: '',
  footerCopyright: '© Mango',
  icp: '',
  contact: '',
};

const formRef = ref<FormInstance>();
const loading = ref(false);
const saving = ref(false);
const form = reactive<AdminBranding>({ ...defaultForm });

const rules: FormRules<AdminBranding> = {
  title: [{ required: true, message: '请输入后台名称', trigger: 'blur' }],
  shortTitle: [{ required: true, message: '请输入后台简称', trigger: 'blur' }],
  loginTitle: [{ required: true, message: '请输入登录页标题', trigger: 'blur' }],
};

onMounted(() => {
  void loadConfig();
});

async function loadConfig() {
  loading.value = true;
  try {
    Object.assign(form, defaultForm, await adminBrandingApi.get());
  } finally {
    loading.value = false;
  }
}

async function handleSave() {
  await formRef.value?.validate();
  saving.value = true;
  try {
    await adminBrandingApi.save({ ...form });
    ElMessage.success('后台品牌配置已保存');
  } finally {
    saving.value = false;
  }
}
</script>

<style scoped>
.admin-branding-page {
  padding: 0;
}

.admin-branding-card {
  border-radius: 8px;
}

.admin-branding-header {
  display: flex;
  align-items: center;
}

.admin-branding-title {
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 600;
}

.admin-branding-section {
  padding: 18px 0 2px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.admin-branding-section:first-of-type {
  padding-top: 4px;
}

.admin-branding-section__title {
  margin-bottom: 18px;
  color: var(--el-text-color-primary);
  font-size: 15px;
  font-weight: 600;
}

.admin-branding-status-tip {
  margin-left: 10px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.admin-branding-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 22px;
}

@media (max-width: 960px) {
  .admin-branding-page :deep(.el-col) {
    max-width: 100%;
    flex: 0 0 100%;
  }
}
</style>
