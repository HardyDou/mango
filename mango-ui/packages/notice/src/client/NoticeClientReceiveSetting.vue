<template>
  <div class="notice-receive-setting-page" data-page="notice.receive-setting">
    <section v-loading="loading.reminder" class="notice-reminder-bar" data-surface="notice.reminder-setting">
      <strong>提醒设置</strong>
      <div class="notice-reminder-bar__body">
        <div class="notice-reminder-bar__controls" data-surface="notice.reminder-controls">
          <span>弹窗提醒</span>
          <el-switch v-model="reminderSetting.popupEnabled" data-field="notice-popup-enabled" />
          <el-select
            v-model="reminderSetting.popupPlacement"
            :disabled="!reminderSetting.popupEnabled"
            class="notice-select"
            data-field="notice-popup-placement"
          >
            <el-option label="右上" value="top-right" />
            <el-option label="右下" value="bottom-right" />
          </el-select>
          <span>声音提醒</span>
          <el-switch v-model="reminderSetting.voiceEnabled" data-field="notice-voice-enabled" />
          <el-select
            v-model="reminderSetting.reminderMode"
            :disabled="!reminderSetting.voiceEnabled"
            class="notice-select"
            data-field="notice-reminder-mode"
          >
            <el-option label="提示音" value="SOUND" />
            <el-option label="语音播报" value="VOICE" />
          </el-select>
          <span>桌面提醒</span>
          <el-switch v-model="reminderSetting.desktopNotificationEnabled" data-field="notice-desktop-enabled" />
        </div>
        <div class="notice-reminder-bar__secondary" data-surface="notice.reminder-secondary">
          <div v-if="reminderSetting.voiceEnabled" class="notice-reminder-bar__voice-content">
            <span>播报内容</span>
            <el-input
              v-model="reminderSetting.voiceText"
              :disabled="reminderSetting.reminderMode !== 'VOICE'"
              maxlength="100"
              placeholder="请输入语音播报内容"
              data-field="notice-voice-text"
            />
          </div>
          <div class="notice-reminder-bar__actions" data-surface="notice.reminder-actions">
            <el-button text @click="testReminderSetting">测试提醒</el-button>
            <el-button type="primary" :loading="reminderSaving" @click="saveReminder">保存设置</el-button>
          </div>
        </div>
      </div>
    </section>

    <section class="notice-preference-layout" data-surface="notice.receive-preferences">
      <aside class="notice-domain-panel">
        <el-input v-model="domainKeyword" clearable placeholder="搜索业务域" />
        <el-menu :default-active="selectedBizGroup" class="notice-domain-menu" @select="selectBizGroup">
          <el-menu-item index="">
            <span>全部业务</span>
            <el-tag size="small" effect="plain">{{ businessTypes.length }}</el-tag>
          </el-menu-item>
          <el-menu-item v-for="domain in filteredDomains" :key="domain.value" :index="domain.value">
            <span>{{ domain.label }}</span>
            <el-tag size="small" effect="plain">{{ domain.count }}</el-tag>
          </el-menu-item>
        </el-menu>
      </aside>

      <section class="notice-preference-list">
        <div class="notice-preference-list__header">
          <div>
            <h2>{{ selectedDomainLabel }}</h2>
            <p>每项修改自动保存；接收账号请在第三方账户中维护。</p>
          </div>
          <el-input v-model="messageKeyword" clearable placeholder="搜索消息类型" class="notice-message-search" />
        </div>

        <el-table
          v-loading="loading.businessTypes || loading.preferences"
          :data="filteredBusinessTypes"
          row-key="bizType"
        >
          <el-table-column prop="bizName" label="消息类型" min-width="190" show-overflow-tooltip />
          <el-table-column label="接收方式" min-width="540">
            <template #default="{ row }">
              <div class="notice-channel-options">
                <el-checkbox
                  v-for="channel in preferenceChannels"
                  :key="channel.value"
                  :model-value="messageChannelEnabled(row.bizType, channel.value)"
                  :data-field="`notice-channel-${channel.value.toLowerCase()}`"
                  @change="handleChannelChange(row.bizType, channel.value, $event)"
                >
                  {{ channel.label }}
                </el-checkbox>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElNotification } from 'element-plus';
import {
  getMyEnabledBusinessTypes,
  getNoticeReminderSetting,
  getReceivePreferences,
  normalizeNoticeReminderSetting,
  saveNoticeReminderSetting,
  saveReceivePreference,
} from '../api/notice';
import { playNoticeSound, showDesktopNotice, speakNoticeText } from '../realtime/noticeRealtime';
import type {
  NoticeBusinessType,
  NoticeChannelType,
  NoticeReceivePreference,
  NoticeReceivePreferenceScopeType,
  NoticeReminderSetting,
  NoticeSiteMessage,
} from '../types/notice';
import { useNoticeDomains } from '../components/useNoticeDomains';

const preferenceChannels: Array<{ value: NoticeChannelType; label: string }> = [
  { value: 'SITE', label: '站内信' },
  { value: 'EMAIL', label: '邮件' },
  { value: 'SMS', label: '短信' },
  { value: 'WECOM', label: '企业微信' },
];

const loading = reactive({ preferences: false, businessTypes: false, reminder: false });
const preferences = ref<NoticeReceivePreference[]>([]);
const businessTypes = ref<NoticeBusinessType[]>([]);
const selectedBizGroup = ref('');
const domainKeyword = ref('');
const messageKeyword = ref('');
const reminderSaving = ref(false);
const reminderSetting = reactive<NoticeReminderSetting>(normalizeNoticeReminderSetting());
const desktopPermission = ref<NotificationPermission | 'unsupported'>('unsupported');
const { domainName, loadDomains } = useNoticeDomains();

const domains = computed(() => {
  const domainMap = new Map<string, { value: string; label: string; count: number }>();
  businessTypes.value.forEach((item) => {
    const value = item.bizGroup || item.domainCode || 'OTHER';
    const current = domainMap.get(value);
    domainMap.set(value, {
      value,
      label: domainName(value),
      count: (current?.count || 0) + 1,
    });
  });
  return Array.from(domainMap.values());
});

const filteredDomains = computed(() => {
  const keyword = domainKeyword.value.trim().toLowerCase();
  return keyword ? domains.value.filter((item) => item.label.toLowerCase().includes(keyword)) : domains.value;
});

const selectedDomainLabel = computed(
  () => domains.value.find((item) => item.value === selectedBizGroup.value)?.label || '全部业务',
);

const filteredBusinessTypes = computed(() => {
  const keyword = messageKeyword.value.trim().toLowerCase();
  return businessTypes.value.filter((item) => {
    const domain = item.bizGroup || item.domainCode || 'OTHER';
    return (
      (!selectedBizGroup.value || domain === selectedBizGroup.value) &&
      (!keyword || [item.bizName, item.bizType].some((value) => value?.toLowerCase().includes(keyword)))
    );
  });
});

function selectBizGroup(value: string) {
  selectedBizGroup.value = value;
}

function preferenceKey(
  scopeType: NoticeReceivePreferenceScopeType,
  scopeValue?: string,
  channelType?: NoticeChannelType,
) {
  return [scopeType, scopeValue || '', channelType || 'ALL'].join(':');
}

function findPreference(
  scopeType: NoticeReceivePreferenceScopeType,
  scopeValue?: string,
  channelType?: NoticeChannelType,
) {
  const normalizedScope = scopeValue || '';
  return preferences.value.find(
    (item) =>
      item.scopeType === scopeType &&
      (item.scopeValue || '') === normalizedScope &&
      (item.channelType || undefined) === channelType,
  );
}

function messageChannelEnabled(bizType: string, channelType: NoticeChannelType) {
  return findPreference('BIZ_TYPE', bizType, channelType)?.enabled ?? true;
}

async function loadPreferences() {
  loading.preferences = true;
  try {
    preferences.value = await getReceivePreferences();
  } finally {
    loading.preferences = false;
  }
}

async function loadBusinessTypes() {
  loading.businessTypes = true;
  try {
    const result = await getMyEnabledBusinessTypes({ pageNum: 1, pageSize: 200 });
    businessTypes.value = result.list || [];
  } finally {
    loading.businessTypes = false;
  }
}

async function loadReminderSetting() {
  loading.reminder = true;
  try {
    Object.assign(reminderSetting, await getNoticeReminderSetting());
  } finally {
    loading.reminder = false;
  }
}

async function saveReminder() {
  reminderSaving.value = true;
  try {
    Object.assign(reminderSetting, normalizeNoticeReminderSetting(reminderSetting));
    await saveNoticeReminderSetting(reminderSetting);
    ElMessage.success('提醒设置已保存');
  } finally {
    reminderSaving.value = false;
  }
}

async function saveMessagePreference(bizType: string, channelType: NoticeChannelType, enabled: boolean) {
  const saved = await saveReceivePreference({
    scopeType: 'BIZ_TYPE',
    scopeValue: bizType,
    channelType,
    enabled,
  });
  upsertPreference(saved);
  ElMessage.success('消息接收设置已保存');
}

function handleChannelChange(bizType: string, channelType: NoticeChannelType, value: unknown) {
  void saveMessagePreference(bizType, channelType, Boolean(value));
}

function upsertPreference(preference: NoticeReceivePreference) {
  const key = preferenceKey(preference.scopeType, preference.scopeValue, preference.channelType);
  const index = preferences.value.findIndex(
    (item) => preferenceKey(item.scopeType, item.scopeValue, item.channelType) === key,
  );
  if (index >= 0) preferences.value.splice(index, 1, preference);
  else preferences.value.push(preference);
}

function testReminderSetting() {
  const setting = normalizeNoticeReminderSetting(reminderSetting);
  Object.assign(reminderSetting, setting);
  const now = new Date().toLocaleString('zh-CN', { hour12: false });
  const message: NoticeSiteMessage = {
    id: `notice-reminder-test-${Date.now()}`,
    title: '提醒设置测试',
    content: `这是一条本地测试消息。测试时间：${now}`,
    userId: 'current',
    priority: 'NORMAL',
    readStatus: 'UNREAD',
    bizGroup: '系统',
    bizName: '提醒设置',
    bizType: 'notice.reminder_test',
    createTime: now,
  };
  if (setting.popupEnabled) {
    ElNotification({
      title: message.title,
      message: message.content,
      type: 'info',
      position: setting.popupPlacement,
      duration: 5000,
    });
  }
  if (setting.voiceEnabled) {
    if (setting.reminderMode === 'VOICE') speakNoticeText(setting.voiceText || message.title);
    else playNoticeSound(setting.soundType);
  }
  void testDesktopNotification(message);
  ElMessage.success('已触发本地提醒测试');
}

async function testDesktopNotification(message: NoticeSiteMessage) {
  desktopPermission.value = 'Notification' in window ? Notification.permission : 'unsupported';
  if (!reminderSetting.desktopNotificationEnabled || desktopPermission.value === 'unsupported') return;
  if (desktopPermission.value === 'default') desktopPermission.value = await Notification.requestPermission();
  if (desktopPermission.value === 'granted') showDesktopNotice(message, () => undefined);
  else if (desktopPermission.value === 'denied') ElMessage.warning('桌面提示已被浏览器阻止');
}

onMounted(() => {
  void loadDomains();
  void Promise.all([loadPreferences(), loadBusinessTypes(), loadReminderSetting()]);
});
</script>

<style scoped>
.notice-receive-setting-page {
  display: grid;
  gap: 16px;
}

.notice-reminder-bar {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  min-height: 52px;
  padding: 10px 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-lighter);
}

.notice-reminder-bar > strong {
  padding-top: 7px;
  white-space: nowrap;
}

.notice-reminder-bar__body {
  display: grid;
  flex: 1;
  gap: 10px;
  min-width: 0;
}

.notice-reminder-bar__controls {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  width: 100%;
}

.notice-reminder-bar__controls > span {
  color: var(--el-text-color-regular);
  font-size: 13px;
  white-space: nowrap;
}

.notice-select {
  width: 100px;
}

.notice-reminder-bar__secondary {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16px;
  min-width: 0;
  width: 100%;
}

.notice-reminder-bar__voice-content {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.notice-reminder-bar__voice-content > span {
  color: var(--el-text-color-regular);
  font-size: 13px;
  white-space: nowrap;
}

.notice-reminder-bar__voice-content .el-input {
  max-width: 420px;
}

.notice-reminder-bar__actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
  white-space: nowrap;
}

.notice-preference-layout {
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
  min-height: 480px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  overflow: hidden;
}

.notice-domain-panel {
  padding: 12px;
  border-right: 1px solid var(--el-border-color-lighter);
  background: var(--el-fill-color-lighter);
}

.notice-domain-menu {
  margin-top: 10px;
  border-right: 0;
  background: transparent;
}

.notice-domain-menu :deep(.el-menu-item) {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 2px;
  padding: 0 10px;
  border-radius: 4px;
}

.notice-preference-list {
  min-width: 0;
  padding: 18px;
}

.notice-preference-list__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.notice-preference-list__header h2 {
  margin: 0 0 4px;
  font-size: 16px;
}

.notice-preference-list__header p {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.notice-message-search {
  width: 220px;
}

.notice-channel-options {
  display: flex;
  align-items: center;
  gap: 18px;
  min-width: max-content;
  white-space: nowrap;
}

@media (width <= 960px) {
  .notice-reminder-bar {
    flex-wrap: wrap;
  }

  .notice-reminder-bar__controls {
    flex-wrap: wrap;
  }

  .notice-reminder-bar__secondary {
    align-items: stretch;
    flex-direction: column;
  }

  .notice-reminder-bar__voice-content .el-input {
    max-width: none;
  }
}

@media (width <= 720px) {
  .notice-preference-layout {
    grid-template-columns: 1fr;
  }

  .notice-domain-panel {
    border-right: 0;
    border-bottom: 1px solid var(--el-border-color-lighter);
  }

  .notice-domain-menu {
    display: flex;
    overflow-x: auto;
  }

  .notice-preference-list__header {
    align-items: stretch;
    flex-direction: column;
  }

  .notice-message-search {
    width: 100%;
  }
}
</style>
