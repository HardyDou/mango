<template>
  <div class="password-container">
    <el-row justify="center">
      <el-col
        :xs="24"
        :sm="18"
        :md="10"
        :lg="8"
      >
        <el-card>
          <template #header>
            <div class="password-card-header">
              <span>修改密码</span>
              <component
                :is="passwordSlots.headerExtra"
                v-if="passwordSlots.headerExtra"
              />
            </div>
          </template>
          <component
            :is="passwordSlots.formBefore"
            v-if="passwordSlots.formBefore"
            :form="form"
          />
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="100px"
          >
            <el-form-item
              label="旧密码"
              prop="oldPassword"
            >
              <el-input
                v-model="form.oldPassword"
                type="password"
                show-password
              />
            </el-form-item>
            <el-form-item
              label="新密码"
              prop="newPassword"
            >
              <el-input
                v-model="form.newPassword"
                type="password"
                show-password
              />
              <PasswordPolicyHint :password="form.newPassword" />
            </el-form-item>
            <el-form-item
              label="确认密码"
              prop="confirmPassword"
            >
              <el-input
                v-model="form.confirmPassword"
                type="password"
                show-password
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                :loading="submitting"
                :disabled="!canSubmit"
                @click="handleSubmit"
              >
                提交
              </el-button>
              <el-button @click="handleReset">
                重置
              </el-button>
            </el-form-item>
          </el-form>
          <component
            :is="passwordSlots.formAfter"
            v-if="passwordSlots.formAfter"
            :form="form"
          />
          <component
            :is="passwordSlots.footer"
            v-if="passwordSlots.footer"
          />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts" name="MangoAuthPassword">
import { computed, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import {
  defaultPasswordPolicy,
  getPasswordPolicyMessage,
  isPasswordPolicyPassed,
  PasswordPolicyHint,
} from '@mango/common';
import { updatePassword } from '../api/sys';
import { useAuthConfig } from '../composables/useAuthConfig';

const authConfig = useAuthConfig();
const formRef = ref();
const submitting = ref(false);
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});
const passwordSlots = computed(() => authConfig.value.password?.slots || {});
const passwordPolicyMessage = getPasswordPolicyMessage(defaultPasswordPolicy);
const canSubmit = computed(() =>
  Boolean(form.oldPassword)
  && isPasswordPolicyPassed(form.newPassword, defaultPasswordPolicy)
  && form.confirmPassword === form.newPassword
);

const validateConfirm = (_rule: any, value: string, callback: any) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'));
  } else {
    callback();
  }
};

const rules = computed(() => ({
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (!isPasswordPolicyPassed(String(value || ''), defaultPasswordPolicy)) {
          callback(new Error(passwordPolicyMessage));
          return;
        }
        callback();
      },
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}));

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) {
    return;
  }
  submitting.value = true;
  try {
    await updatePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
    });
    ElMessage.success('密码修改成功');
    handleReset();
  } finally {
    submitting.value = false;
  }
};

const handleReset = () => {
  formRef.value?.resetFields();
};
</script>

<style scoped lang="scss">
.password-container {
  padding: 40px 20px;
}

.password-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
</style>
