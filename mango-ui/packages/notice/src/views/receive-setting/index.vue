<template>
  <div class="notice-receive-setting-page">
    <el-card shadow="never">
      <template #header>
        <div class="notice-receive-setting-page__header">
          <span>接收设置</span>
          <el-button type="primary" :loading="saving" @click="saveSettings">保存</el-button>
        </div>
      </template>

      <el-form v-loading="loading" :model="form" label-width="140px" class="notice-receive-setting-page__form">
        <el-divider content-position="left">提醒方式</el-divider>
        <el-form-item label="提示音">
          <el-switch v-model="form.soundEnabled" />
        </el-form-item>
        <el-form-item label="桌面通知">
          <el-switch v-model="form.desktopEnabled" />
        </el-form-item>

        <el-divider content-position="left">发送控制</el-divider>
        <el-form-item label="最大重试次数">
          <el-input-number v-model="form.maxRetry" :min="0" />
        </el-form-item>
        <el-form-item label="消息保留天数">
          <el-input-number v-model="form.retentionDays" :min="1" />
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getNoticeSettings, saveNoticeSettings } from '../../api/notice';

type ReceiveSettingForm = {
  soundEnabled: boolean;
  desktopEnabled: boolean;
  maxRetry: number;
  retentionDays: number;
};

const loading = ref(false);
const saving = ref(false);
const form = reactive<ReceiveSettingForm>({
  soundEnabled: true,
  desktopEnabled: true,
  maxRetry: 3,
  retentionDays: 180,
});

async function loadSettings() {
  loading.value = true;
  try {
    const result = await getNoticeSettings();
    Object.assign(form, result);
  } finally {
    loading.value = false;
  }
}

async function saveSettings() {
  saving.value = true;
  try {
    await saveNoticeSettings({ ...form });
    ElMessage.success('接收设置已保存');
  } finally {
    saving.value = false;
  }
}

onMounted(loadSettings);
</script>

<style scoped>
.notice-receive-setting-page {
  padding: 0;
}

.notice-receive-setting-page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.notice-receive-setting-page__form {
  max-width: 760px;
}
</style>
