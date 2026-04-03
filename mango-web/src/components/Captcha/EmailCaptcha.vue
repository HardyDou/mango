<template>
  <div class="captcha-email">
    <el-input
      v-model="email"
      placeholder="请输入邮箱地址"
      size="large"
      @blur="handleEmailBlur"
    />
    <div class="email-code-row">
      <el-input
        v-model="code"
        placeholder="请输入邮件验证码"
        size="large"
        @keyup.enter="handleVerify"
      />
      <el-button
        type="primary"
        size="large"
        :disabled="countdown > 0 || !emailValid"
        @click="handleSend"
      >
        {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
      </el-button>
    </div>
    <div
      v-if="errorMessage"
      class="error-msg"
    >
      {{ errorMessage }}
    </div>
  </div>
</template>

<script setup lang="ts" name="EmailCaptcha">
import { ref, computed } from 'vue';
import { sendEmail, verifyCaptcha, CaptchaType } from '@/api/admin/captcha';
import { ElMessage } from 'element-plus';

const emit = defineEmits<{
  success: [key: string];
}>();

const email = ref('');
const code = ref('');
const countdown = ref(0);
const errorMessage = ref('');
const captchaKey = ref('');

let countdownTimer: ReturnType<typeof setInterval> | null = null;

const emailValid = computed(() => {
  return /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email.value);
});

const handleEmailBlur = () => {
  if (email.value && !emailValid.value) {
    errorMessage.value = '请输入正确的邮箱地址';
  } else {
    errorMessage.value = '';
  }
};

const handleSend = async () => {
  if (!emailValid.value) {
    errorMessage.value = '请输入正确的邮箱地址';
    return;
  }

  errorMessage.value = '';

  try {
    captchaKey.value = await sendEmail(email.value);
    ElMessage.success('验证码已发送');
    startCountdown();
  } catch (error: any) {
    errorMessage.value = error.message || '发送失败';
  }
};

const startCountdown = () => {
  countdown.value = 60;
  countdownTimer = setInterval(() => {
    countdown.value--;
    if (countdown.value <= 0) {
      countdown.value = 0;
      if (countdownTimer) {
        clearInterval(countdownTimer);
        countdownTimer = null;
      }
    }
  }, 1000);
};

const handleVerify = async () => {
  if (!code.value.trim()) {
    errorMessage.value = '请输入邮件验证码';
    return;
  }

  if (!captchaKey.value) {
    errorMessage.value = '请先获取验证码';
    return;
  }

  try {
    const result = await verifyCaptcha({
      key: captchaKey.value,
      type: CaptchaType.EMAIL,
      code: code.value.trim(),
    });

    if (result) {
      emit('success', captchaKey.value);
    }
  } catch {
    errorMessage.value = '验证码校验失败';
  }
};

defineExpose({
  getEmail: () => email.value,
  getCode: () => code.value,
});
</script>

<style scoped lang="scss">
.captcha-email {
  .email-code-row {
    display: flex;
    gap: 12px;
    margin-top: 12px;

    .el-input {
      flex: 1;
    }
  }

  .error-msg {
    margin-top: 8px;
    font-size: 12px;
    color: #f56c6c;
  }
}
</style>
