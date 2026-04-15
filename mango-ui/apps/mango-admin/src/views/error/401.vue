<template>
  <div class="error-container">
    <div class="error-content">
      <div class="error-code">
        401
      </div>
      <div class="error-message">
        {{ $t('noPermission.message') }}
      </div>
      <el-button
        type="primary"
        @click="goHome"
      >
        {{ $t('noPermission.btn') }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts" name="NoPermission">
import { useRouter } from 'vue-router';

const router = useRouter();

const goHome = () => {
  router.push('/home');
};
</script>

<style scoped lang="scss">
.error-container {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100vh;
  background: var(--mango-bg-color);
}

.error-content {
  text-align: center;

  .error-code {
    font-size: 120px;
    font-weight: 700;
    color: var(--mango-color-danger);
    line-height: 1;
  }

  .error-message {
    margin: 20px 0 30px;
    font-size: 20px;
    color: var(--mango-text-color-regular);
  }
}
</style>
