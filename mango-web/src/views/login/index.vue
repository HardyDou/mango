<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-left">
        <div class="login-title">
          <h1>Mango Admin</h1>
          <p>企业级管理平台</p>
        </div>
      </div>
      <div class="login-form">
        <h2 class="form-title">{{ $t('login.title') }}</h2>
        <el-form ref="loginFormRef" :model="form" :rules="rules" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              :placeholder="$t('login.username.placeholder')"
              size="large"
              prefix-icon="User"
              clearable
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              :placeholder="$t('login.password.placeholder')"
              type="password"
              size="large"
              prefix-icon="Lock"
              show-password
              clearable
            />
          </el-form-item>
          <el-form-item prop="captcha" v-if="captchaEnabled">
            <el-input
              v-model="form.captcha"
              :placeholder="$t('login.captcha.placeholder')"
              size="large"
              style="width: 60%"
              prefix-icon="CircleCheck"
              clearable
            />
            <div class="captcha-img" @click="refreshCaptcha">
              <img :src="captchaUrl" alt="验证码" />
            </div>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="login-btn"
              @click="handleLogin"
            >
              {{ $t('login.btn') }}
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts" name="Login">
import { ref, reactive, computed } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { User, Lock, CircleCheck } from '@element-plus/icons-vue';
import { Session } from '@/utils/storage';
import { login } from '@/api/admin/sys';

const router = useRouter();
const loginFormRef = ref();

// 表单数据
const form = reactive({
  username: '',
  password: '',
  captcha: '',
});

// 校验规则
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captcha: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
};

// 状态
const loading = ref(false);
const captchaEnabled = ref(false);
const captchaUrl = ref('');

// 获取验证码
const refreshCaptcha = () => {
  captchaUrl.value = `/api/admin/sys/captcha?t=${Date.now()}`;
};

// 登录处理
const handleLogin = async () => {
  if (!loginFormRef.value) return;

  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return;

    loading.value = true;
    try {
      // 模拟登录（实际项目中调用接口）
      // const res = await login(form);
      const mockRes = {
        token: 'mock-token-' + Date.now(),
        userInfo: {
          username: form.username,
          nickname: '管理员',
          photo: '',
          roles: ['admin'],
          permissions: ['*'],
          authBtnList: [],
          tenantId: 'master',
          tenantName: '默认租户',
        },
      };

      // 保存 Token
      Session.setToken(mockRes.token);
      Session.set('userInfo', mockRes.userInfo);

      ElMessage.success('登录成功');
      router.push('/home');
    } catch (error) {
      console.error('登录失败:', error);
      ElMessage.error('登录失败，请检查用户名和密码');
    } finally {
      loading.value = false;
    }
  });
};

// 初始化验证码
refreshCaptcha();
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  display: flex;
  width: 900px;
  height: 500px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  overflow: hidden;
}

.login-left {
  display: flex;
  flex-direction: column;
  justify-content: center;
  width: 50%;
  padding: 40px;
  background: linear-gradient(135deg, #2E5CF6 0%, #764ba2 100%);

  .login-title {
    color: #fff;

    h1 {
      font-size: 36px;
      font-weight: 700;
      margin-bottom: 12px;
    }

    p {
      font-size: 18px;
      opacity: 0.9;
    }
  }
}

.login-form {
  display: flex;
  flex-direction: column;
  justify-content: center;
  width: 50%;
  padding: 40px;

  .form-title {
    margin-bottom: 30px;
    font-size: 24px;
    font-weight: 600;
    color: #333;
    text-align: center;
  }

  .login-btn {
    width: 100%;
  }

  .captcha-img {
    margin-left: 12px;
    height: 40px;
    border-radius: 4px;
    overflow: hidden;
    cursor: pointer;

    img {
      height: 100%;
    }
  }
}

:deep(.el-input__wrapper) {
  padding: 4px 12px;
}
</style>
