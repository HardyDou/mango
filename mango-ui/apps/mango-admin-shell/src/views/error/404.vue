<template>
  <div class="error-container">
    <div class="error-content">
      <div class="error-code">
        404
      </div>
      <div class="error-message">
        页面不存在
      </div>
      <el-button
        type="primary"
        @click="goHome"
      >
        返回首页
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts" name="ShellNotFound">
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
  min-height: calc(100vh - var(--mango-header-height) - var(--mango-tags-view-height) - 32px);
  background: var(--mango-bg-color);
}

.error-content {
  text-align: center;

  .error-code {
    font-size: 120px;
    font-weight: 700;
    color: var(--mango-color-primary);
    line-height: 1;
  }

  .error-message {
    margin: 20px 0 30px;
    font-size: 20px;
    color: var(--mango-text-color-regular);
  }
}
</style>
