<template>
  <div class="captcha-arithmetic">
    <div class="captcha-header">
      <span class="captcha-title">请完成下方计算题</span>
      <el-button
        link
        type="primary"
        @click="refresh"
      >
        刷新
      </el-button>
    </div>
    <div class="captcha-image">
      <img
        v-if="captchaData?.image"
        :src="captchaData.image"
        alt="验证码"
      >
      <div
        v-else
        class="loading"
      >
        加载中...
      </div>
    </div>
    <el-input
      v-model="userInput"
      placeholder="请输入计算结果"
      size="large"
      @keyup.enter="handleVerify"
    />
    <div
      v-if="errorMessage"
      class="error-msg"
    >
      {{ errorMessage }}
    </div>
  </div>
</template>

<script setup lang="ts" name="ArithmeticCaptcha">
import { ref, onMounted } from 'vue';
import { generateArithmetic, verifyCaptcha, type CaptchaResponse, CaptchaType } from '@/api/admin/captcha';

const emit = defineEmits<{
  success: [key: string, code?: string];
  refresh: [];
}>();

const captchaData = ref<CaptchaResponse | null>(null);
const userInput = ref('');
const errorMessage = ref('');

const refresh = async () => {
  errorMessage.value = '';
  userInput.value = '';
  captchaData.value = await generateArithmetic();
  emit('refresh');
};

const handleVerify = async () => {
  if (!userInput.value.trim()) {
    errorMessage.value = '请输入计算结果';
    return;
  }

  if (!captchaData.value?.key) {
    errorMessage.value = '验证码已过期，请刷新';
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
    }
  } catch {
    errorMessage.value = '验证码校验失败';
    await refresh();
  }
};

onMounted(() => {
  refresh();
});

defineExpose({ refresh });
</script>

<style scoped lang="scss">
.captcha-arithmetic {
  .captcha-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .captcha-title {
      font-size: 14px;
      color: #666;
    }
  }

  .captcha-image {
    margin-bottom: 12px;
    border-radius: 4px;
    overflow: hidden;

    img {
      width: 100%;
      height: 60px;
      object-fit: contain;
    }

    .loading {
      height: 60px;
      line-height: 60px;
      text-align: center;
      background: #f5f5f5;
      color: #999;
    }
  }

  .error-msg {
    margin-top: 8px;
    font-size: 12px;
    color: #f56c6c;
  }
}
</style>
