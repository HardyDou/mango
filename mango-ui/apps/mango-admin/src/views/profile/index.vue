<template>
  <div class="profile-container">
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card>
          <div class="user-info">
            <el-avatar
              :size="80"
              src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png"
            />
            <h3 class="username">
              管理员
            </h3>
            <p class="role">
              超级管理员
            </p>
          </div>
          <el-divider />
          <div class="info-list">
            <div class="info-item">
              <el-icon><User /></el-icon>
              <span>admin</span>
            </div>
            <div class="info-item">
              <el-icon><Message /></el-icon>
              <span>admin@example.com</span>
            </div>
            <div class="info-item">
              <el-icon><Phone /></el-icon>
              <span>13800138000</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="16">
        <el-card>
          <el-tabs v-model="activeTab">
            <el-tab-pane
              label="基本信息"
              name="info"
            >
              <el-form
                :model="form"
                label-width="100px"
              >
                <el-form-item label="用户名">
                  <el-input v-model="form.username" />
                </el-form-item>
                <el-form-item label="昵称">
                  <el-input v-model="form.nickname" />
                </el-form-item>
                <el-form-item label="邮箱">
                  <el-input v-model="form.email" />
                </el-form-item>
                <el-form-item label="手机号">
                  <el-input v-model="form.phone" />
                </el-form-item>
                <el-form-item>
                  <el-button
                    type="primary"
                    @click="handleSave"
                  >
                    保存
                  </el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts" name="Profile">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import { User, Message, Phone } from '@element-plus/icons-vue';

const activeTab = ref('info');
const form = ref({
  username: 'admin',
  nickname: '管理员',
  email: 'admin@example.com',
  phone: '13800138000',
});

const handleSave = () => {
  ElMessage.success('保存成功');
};
</script>

<style scoped lang="scss">
.profile-container {
  padding: 20px;
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;

  .username {
    margin-top: 16px;
    margin-bottom: 4px;
    font-size: 18px;
    font-weight: 600;
  }

  .role {
    color: var(--mango-text-color-regular);
    font-size: 14px;
  }
}

.info-list {
  .info-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 0;
    color: var(--mango-text-color-regular);

    .el-icon {
      color: var(--mango-text-color-secondary);
    }
  }
}
</style>
