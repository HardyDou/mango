<template>
  <div class="captcha-card">
    <div class="captcha-header">
      <span>请完成下方计算题</span>
      <el-button link type="primary" @click="refresh">刷新</el-button>
    </div>
    <div class="captcha-body">
      <button class="captcha-image-button" type="button" title="点击刷新验证码" @click="refresh">
        <img v-if="captchaData?.image" :src="captchaData.image" alt="Arithmetic captcha">
        <span v-else class="captcha-placeholder">加载中...</span>
      </button>
      <el-input
        v-model="userInput"
        class="captcha-input"
        placeholder="请输入计算结果"
        @keyup.enter="handleVerify"
      />
    </div>
    <div v-if="errorMessage" class="error-msg">{{ errorMessage }}</div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';
import { CaptchaType, generateArithmetic, verifyCaptcha, type CaptchaResponse } from '../../api/captcha';

const emit = defineEmits<{
  success: [key: string, code?: string];
  refresh: [];
  inputChange: [value: string];
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
  return verify();
}

async function verify() {
  if (!captchaData.value?.key) {
    errorMessage.value = '验证码已过期，请刷新';
    return false;
  }
  if (!userInput.value.trim()) {
    errorMessage.value = '请输入计算结果';
    return false;
  }

  try {
    const result = await verifyCaptcha({
      key: captchaData.value.key,
      type: CaptchaType.ARITHMETIC,
      code: userInput.value.trim(),
    });
    if (result) {
      emit('success', captchaData.value.key, userInput.value.trim());
      return true;
    }
  } catch {
    // ignored
  }

  errorMessage.value = '验证码校验失败';
  await refresh();
  return false;
}

watch(userInput, (value) => {
  emit('inputChange', value);
});

onMounted(() => {
  void refresh();
});

defineExpose({ refresh, verify });
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
    display: flex;
    align-items: stretch;
    gap: 10px;
  }

  .captcha-image-button {
    flex: 0 0 140px;
    height: 32px;
    padding: 0;
    overflow: hidden;
    border: 1px solid var(--el-border-color);
    border-radius: 4px;
    background: var(--el-fill-color-lighter);
    cursor: pointer;

    img {
      display: block;
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .captcha-input {
    flex: 1 1 0;
    min-width: 0;
  }

  .captcha-placeholder {
    display: flex;
    width: 100%;
    height: 100%;
    align-items: center;
    justify-content: center;
    color: #909399;
    font-size: 13px;
  }

  .error-msg {
    margin-top: 8px;
    color: #f56c6c;
    font-size: 12px;
  }
}

@media (max-width: 520px) {
  .captcha-card {
    .captcha-body {
      flex-direction: column;
    }

    .captcha-image-button {
      flex-basis: 40px;
      width: 160px;
      max-width: 100%;
    }
  }
}
</style>
