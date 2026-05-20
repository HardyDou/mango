<template>
  <div class="captcha-form">
    <el-input
      v-model="mobile"
      placeholder="请输入手机号"
      @blur="validateMobile"
    />
    <div class="row">
      <el-input
        v-model="code"
        placeholder="请输入短信验证码"
        @keyup.enter="handleVerify"
      />
      <el-button
        type="primary"
        :disabled="countdown > 0 || !mobileValid"
        @click="handleSend"
      >
        {{ countdown > 0 ? `${countdown}s` : '发送验证码' }}
      </el-button>
    </div>
    <div v-if="errorMessage" class="error-msg">{{ errorMessage }}</div>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { computed, onBeforeUnmount, ref } from 'vue';
import { CaptchaType, sendSms, verifyCaptcha } from '../../api/captcha';

const emit = defineEmits<{
  success: [key: string];
}>();

const mobile = ref('');
const code = ref('');
const countdown = ref(0);
const errorMessage = ref('');
const captchaKey = ref('');
let timer: ReturnType<typeof setInterval> | null = null;

const mobileValid = computed(() => /^1[3-9]\d{9}$/.test(mobile.value));

function validateMobile() {
  errorMessage.value = mobile.value && !mobileValid.value ? '请输入正确的手机号' : '';
}

async function handleSend() {
  if (!mobileValid.value) {
    errorMessage.value = '请输入正确的手机号';
    return;
  }

  captchaKey.value = await sendSms(mobile.value);
  ElMessage.success('验证码已发送');
  countdown.value = 60;
  timer = setInterval(() => {
    countdown.value -= 1;
    if (countdown.value <= 0 && timer) {
      clearInterval(timer);
      timer = null;
      countdown.value = 0;
    }
  }, 1000);
}

async function handleVerify() {
  return verify();
}

async function verify() {
  if (!captchaKey.value) {
    errorMessage.value = '请先获取验证码';
    return false;
  }

  try {
    const result = await verifyCaptcha({
      key: captchaKey.value,
      type: CaptchaType.SMS,
      code: code.value.trim(),
    });
    if (result) {
      emit('success', captchaKey.value);
      return true;
    }
  } catch {
    // ignored
  }

  errorMessage.value = '验证码校验失败';
  return false;
}

function refresh() {
  mobile.value = '';
  code.value = '';
  captchaKey.value = '';
  countdown.value = 0;
  errorMessage.value = '';
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
}

onBeforeUnmount(() => {
  if (timer) clearInterval(timer);
});

defineExpose({ refresh, verify });
</script>

<style scoped lang="scss">
.captcha-form {
  .row {
    display: flex;
    gap: 12px;
    margin-top: 12px;
  }

  .error-msg {
    margin-top: 8px;
    color: #f56c6c;
    font-size: 12px;
  }
}
</style>
