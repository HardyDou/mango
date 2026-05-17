<template>
  <div class="profile-container">
    <el-row :gutter="20">
      <el-col
        :xs="24"
        :sm="24"
        :md="8"
      >
        <el-card>
          <component
            :is="profileSlots.sidebarTop"
            v-if="profileSlots.sidebarTop"
            :user="currentUser"
          />
          <div class="user-info">
            <el-avatar
              :size="80"
              :src="profileAvatar"
            >
              <el-icon><User /></el-icon>
            </el-avatar>
            <h3 class="username">
              {{ displayName }}
            </h3>
            <p class="role">
              {{ roleLabel }}
            </p>
          </div>
          <el-divider />
          <div class="info-list">
            <div
              v-for="field in visibleSummaryFields"
              :key="field.key"
              class="info-item"
            >
              <el-icon><component :is="field.icon" /></el-icon>
              <span>{{ field.value || '-' }}</span>
            </div>
          </div>
          <component
            :is="profileSlots.sidebarBottom"
            v-if="profileSlots.sidebarBottom"
            :user="currentUser"
          />
        </el-card>
      </el-col>
      <el-col
        :xs="24"
        :sm="24"
        :md="16"
      >
        <el-card>
          <el-tabs v-model="activeTab">
            <el-tab-pane
              label="基本信息"
              name="info"
            >
              <component
                :is="profileSlots.infoBefore"
                v-if="profileSlots.infoBefore"
                :form="form"
                :user="currentUser"
              />
              <el-form
                :model="form"
                label-width="100px"
              >
                <el-form-item
                  v-for="field in editableFields"
                  :key="field.key"
                  :label="field.label"
                >
                  <el-input
                    v-model="(form as any)[field.key]"
                    :disabled="field.readonly"
                  />
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
              <component
                :is="profileSlots.infoAfter"
                v-if="profileSlots.infoAfter"
                :form="form"
                :user="currentUser"
              />
            </el-tab-pane>
            <component
              :is="profileSlots.extraTabs"
              v-if="profileSlots.extraTabs"
              :form="form"
              :user="currentUser"
            />
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts" name="MangoAuthProfile">
import { computed, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Message, Phone, User } from '@element-plus/icons-vue';
import { Session } from '@mango/common/utils/storage';
import { useAuthConfig } from '../composables/useAuthConfig';

const authConfig = useAuthConfig();
const activeTab = ref('info');
const sessionUser = Session.get('userInfo') || {};
const form = reactive({
  username: sessionUser.username || 'admin',
  nickname: sessionUser.nickname || sessionUser.username || '管理员',
  email: sessionUser.email || 'admin@example.com',
  phone: sessionUser.phone || '13800138000',
});

const currentUser = computed(() => ({
  ...sessionUser,
  ...form,
}));
const profileSlots = computed(() => authConfig.value.profile?.slots || {});
const configuredFields = computed(() => authConfig.value.profile?.fields || ['username', 'nickname', 'email', 'phone']);
const fieldDefinitions = computed(() => configuredFields.value.map((key) => ({
  key,
  label: fieldLabels[key] || key,
  readonly: key === 'username',
  value: (currentUser.value as any)[key],
  icon: fieldIcons[key] || User,
})));
const editableFields = computed(() => fieldDefinitions.value);
const visibleSummaryFields = computed(() => fieldDefinitions.value.filter((field) => ['username', 'email', 'phone'].includes(field.key)).slice(0, 4));
const displayName = computed(() => currentUser.value.nickname || currentUser.value.username || '管理员');
const roleLabel = computed(() => authConfig.value.profile?.roleLabel || currentUser.value.roleName || '超级管理员');
const profileAvatar = computed(() =>
  authConfig.value.profile?.avatarUrl
  || currentUser.value.photo
  || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
);

const fieldLabels: Record<string, string> = {
  username: '用户名',
  nickname: '昵称',
  email: '邮箱',
  phone: '手机号',
};

const fieldIcons: Record<string, any> = {
  username: User,
  nickname: User,
  email: Message,
  phone: Phone,
};

const handleSave = () => {
  Session.set('userInfo', currentUser.value);
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
