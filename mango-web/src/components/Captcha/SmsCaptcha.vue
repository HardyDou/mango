<template>
  <div class="captcha-sms">
    <el-input
      v-model="mobile"
      placeholder="请输入手机号"
      size="large"
      @blur="handleMobileBlur"
    >
      <template #prepend>
        <el-select
          v-model="mobilePrefix"
          style="width: 90px"
        >
          <el-option
            label="+86"
            value="+86"
          />
        </el-select>
      </template>
    </el-input>
    <div class="sms-code-row">
      <el-input
        v-model="code"
        placeholder="请输入短信验证码"
        size="large"
        @keyup.enter="handleVerify"
      />
      <el-button
        type="primary"
        size="large"
        :disabled="countdown > 0 || !mobileValid"
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

<script setup lang="ts" name="SmsCaptcha">
import { ref, computed } from 'vue';
import { sendSms, verifyCaptcha, CaptchaType } from '@/api/admin/captcha';
import { ElMessage } from 'element-plus';

const emit = defineEmits<{
  success: [key: string];
}>();

const mobile = ref('');
const mobilePrefix = ref('+86');
const code = ref('');
const countdown = ref(0);
const errorMessage = ref('');
const captchaKey = ref('');

let countdownTimer: ReturnType<typeof setInterval> | null = null;

const mobileValid = computed(() => {
  const fullMobile = mobilePrefix.value + mobile.value;
  return /^1[3-9]\d{9}$/.test(mobile.value);
});

const handleMobileBlur = () => {
  if (mobile.value && !mobileValid.value) {
    errorMessage.value = '请输入正确的手机号';
  } else {
    errorMessage.value = '';
  }
};

const handleSend = async () => {
  if (!mobileValid.value) {
    errorMessage.value = '请输入正确的手机号';
    return;
  }

  errorMessage.value = '';

  try {
    captchaKey.value = await sendSms(mobile.value);
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
    errorMessage.value = '请输入短信验证码';
    return;
  }

  if (!captchaKey.value) {
    errorMessage.value = '请先获取验证码';
    return;
  }

  try {
    const result = await verifyCaptcha({
      key: captchaKey.value,
      type: CaptchaType.SMS,
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
  getMobile: () => mobilePrefix.value + mobile.value,
  getCode: () => code.value,
});
</script>

<style scoped lang="scss">
.captcha-sms {
  .sms-code-row {
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
