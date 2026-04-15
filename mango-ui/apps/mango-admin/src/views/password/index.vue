<template>
  <div class="password-container">
    <el-row justify="center">
      <el-col :span="8">
        <el-card>
          <template #header>
            <span>修改密码</span>
          </template>
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
                @click="handleSubmit"
              >
                提交
              </el-button>
              <el-button @click="handleReset">
                重置
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts" name="Password">
import { ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';

const formRef = ref();
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});

const validateConfirm = (rule: any, value: string, callback: any) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'));
  } else {
    callback();
  }
};

const rules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
};

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false);
  if (valid) {
    ElMessage.success('密码修改成功');
    handleReset();
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
</style>
