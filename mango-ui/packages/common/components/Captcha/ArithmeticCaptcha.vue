<template>
  <div class="captcha-card">
    <div class="captcha-header">
      <span>请完成下方计算题</span>
      <el-button link type="primary" @click="refresh">刷新</el-button>
    </div>
    <div class="captcha-body">
      <img v-if="captchaData?.image" :src="captchaData.image" alt="Arithmetic captcha">
      <div v-else class="captcha-placeholder">加载中...</div>
    </div>
    <el-input
      v-model="userInput"
      placeholder="请输入计算结果"
      @keyup.enter="handleVerify"
    />
    <div v-if="errorMessage" class="error-msg">{{ errorMessage }}</div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { CaptchaType, generateArithmetic, verifyCaptcha, type CaptchaResponse } from '../../api/captcha';

const emit = defineEmits<{
  success: [key: string, code?: string];
  refresh: [];
}>();

const captchaData = ref<CaptchaResponse | null>(null);
const userInput = ref('');
const errorMessage = ref('');

async function refresh() {
  errorMessage.value = '';
  userInput.value = '';
  captchaData.value = await generateArithmetic();
  emit('refresh');
}

async function handleVerify() {
  if (!captchaData.value?.key) {
    errorMessage.value = '验证码已过期，请刷新';
    return;
  }
  if (!userInput.value.trim()) {
    errorMessage.value = '请输入计算结果';
    return;
  }

  try {
    const result = await verifyCaptcha({
      key: captchaData.value.key,
      type: CaptchaType.ARITHMETIC,
      code: userInput.value.trim(),
    });
    if (result) {
      emit('success', captchaData.value.key, userInput.value.trim());
      return;
    }
  } catch {
    // ignored
  }

  errorMessage.value = '验证码校验失败';
  await refresh();
}

onMounted(() => {
  void refresh();
});

defineExpose({ refresh });
</script>

<style scoped lang="scss">
.captcha-card {
  .captcha-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
  }

  .captcha-body {
    margin-bottom: 12px;
  }

  .captcha-placeholder {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 4px;
    background: #f5f7fa;
    color: #909399;
  }

  .error-msg {
    margin-top: 8px;
    color: #f56c6c;
    font-size: 12px;
  }
}
</style>
