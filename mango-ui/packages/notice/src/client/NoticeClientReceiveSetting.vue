<template>
  <div class="notice-receive-setting-page">
    <el-card shadow="never" class="notice-section">
      <el-tabs v-model="activeTab" class="notice-receive-tabs">
        <el-tab-pane label="提醒设置" name="reminder">
          <el-form
            :model="reminderSetting"
            label-width="120px"
            class="notice-reminder-form"
            v-loading="loading.reminder"
          >
            <div class="notice-reminder-group">
              <div class="notice-reminder-group__title">弹窗提示</div>
              <el-row :gutter="24">
                <el-col :xs="24" :md="12">
                  <el-form-item label="弹窗提示">
                    <el-switch
                      v-model="reminderSetting.popupEnabled"
                      active-text="开启"
                      inactive-text="关闭"
                    />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="12">
                  <el-form-item label="弹窗位置">
                    <el-radio-group
                      v-model="reminderSetting.popupPlacement"
                      :disabled="!reminderSetting.popupEnabled"
                    >
                      <el-radio-button label="top-right">右上</el-radio-button>
                      <el-radio-button label="bottom-right">右下</el-radio-button>
                    </el-radio-group>
                  </el-form-item>
                </el-col>
              </el-row>
            </div>

            <div class="notice-reminder-group">
              <div class="notice-reminder-group__title">声音提醒</div>
              <el-row :gutter="24">
                <el-col :xs="24" :md="12">
                  <el-form-item label="声音提醒">
                    <el-switch
                      v-model="reminderSetting.voiceEnabled"
                      active-text="开启"
                      inactive-text="关闭"
                    />
                  </el-form-item>
                  <el-form-item label="提醒方式">
                    <el-radio-group
                      v-model="reminderSetting.reminderMode"
                      :disabled="!reminderSetting.voiceEnabled"
                    >
                      <el-radio-button label="SOUND">提示音</el-radio-button>
                      <el-radio-button label="VOICE">语音播报</el-radio-button>
                    </el-radio-group>
                  </el-form-item>
                  <el-form-item
                    v-if="reminderSetting.reminderMode === 'SOUND'"
                    label="提示音"
                  >
                    <el-select
                      v-model="reminderSetting.soundType"
                      class="notice-form-control"
                      :disabled="!reminderSetting.voiceEnabled"
                    >
                      <el-option label="清脆提示音" value="IM" />
                      <el-option label="柔和提示音" value="SOFT" />
                      <el-option label="双音提示音" value="DOUBLE" />
                      <el-option label="无提示音" value="NONE" />
                    </el-select>
                  </el-form-item>
                  <el-form-item
                    v-if="reminderSetting.reminderMode === 'VOICE'"
                    label="提示内容"
                  >
                    <el-input
                      v-model="reminderSetting.voiceText"
                      :disabled="!reminderSetting.voiceEnabled"
                      maxlength="80"
                      show-word-limit
                      placeholder="请输入语音播报内容"
                    />
                  </el-form-item>
                </el-col>
              </el-row>
            </div>

            <div class="notice-reminder-group">
              <div class="notice-reminder-group__title">桌面提示</div>
              <el-row :gutter="24">
                <el-col :xs="24" :md="12">
                  <el-form-item label="桌面提示">
                    <div class="notice-desktop-setting">
                      <el-switch
                        v-model="reminderSetting.desktopNotificationEnabled"
                        active-text="开启"
                        inactive-text="关闭"
                      />
                      <el-tag :type="desktopPermissionTagType" effect="plain">
                        {{ desktopPermissionText }}
                      </el-tag>
                      <el-button
                        v-if="desktopPermission === 'default'"
                        link
                        type="primary"
                        @click="requestDesktopNoticePermission"
                      >
                        授权
                      </el-button>
                    </div>
                  </el-form-item>
                </el-col>
                <el-col v-if="desktopPermission === 'denied'" :xs="24">
                  <el-alert
                    title="浏览器已阻止桌面提示，请在地址栏左侧站点设置中允许通知。"
                    type="warning"
                    :closable="false"
                    show-icon
                  />
                </el-col>
              </el-row>
            </div>

            <el-form-item class="notice-reminder-actions">
              <el-button
                type="primary"
                :loading="reminderSaving"
                @click="saveReminder"
              >
                保存
              </el-button>
              <el-button @click="testReminderSetting">测试提醒</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="账号" name="accounts">
          <el-table :data="accountRows" border stripe v-loading="loading.accounts">
            <el-table-column prop="label" label="账号类型" width="150" />
            <el-table-column label="账号" min-width="260">
              <template #default="{ row }">
                <span v-if="row.system">{{ row.label }}：{{ row.accountValue }}</span>
                <span v-else-if="row.account">{{ row.label }}：{{ accountDisplayValue(row) }}</span>
                <span v-else class="notice-muted">未绑定</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="row.system || row.account ? 'success' : 'info'">
                  {{ row.system ? '已启用' : row.account ? accountStatusText(row.account.verifiedStatus) : '未绑定' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right" align="center">
              <template #default="{ row }">
                <span v-if="row.system">-</span>
                <template v-else-if="row.type === 'WECOM'">
                  <el-button v-if="row.account" link type="danger" @click="disableAccount(row.account)">解绑</el-button>
                  <span v-else>-</span>
                </template>
                <template v-else-if="row.account">
                  <el-button link type="primary" @click="openAccountEdit(row)">{{ modifyAccountText(row) }}</el-button>
                  <el-button v-if="canUnbindAccount(row)" link type="danger" @click="disableAccount(row.account)">解绑</el-button>
                </template>
                <el-button v-else link type="primary" @click="openAccountBind(row)">绑定{{ row.label }}</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="接收规则配置" name="rules">
          <div class="notice-section__header">
            <el-input v-model="bizKeyword" clearable placeholder="搜索消息名称/Key" class="notice-search" />
          </div>

          <el-table :data="filteredBusinessTypes" border stripe v-loading="loading.businessTypes || loading.preferences">
            <el-table-column label="业务域" width="160" show-overflow-tooltip>
              <template #default="{ row }">{{ domainText(row.bizGroup || row.domainCode) }}</template>
            </el-table-column>
            <el-table-column prop="bizName" label="消息名称" min-width="180" show-overflow-tooltip />
            <el-table-column width="110" align="center">
              <template #header>
                <div class="notice-checkbox-header">
                  <span>全部</span>
                  <el-checkbox
                    :model-value="allVisibleRowsEnabled"
                    :indeterminate="allVisibleRowsIndeterminate"
                    @change="value => saveAllVisibleRows(Boolean(value))"
                  />
                </div>
              </template>
              <template #default="{ row }">
                <el-checkbox
                  :model-value="messageAllChannelsEnabled(row.bizType)"
                  :indeterminate="messageAllChannelsIndeterminate(row.bizType)"
                  @change="value => saveMessageAllChannels(row.bizType, Boolean(value))"
                />
              </template>
            </el-table-column>
            <el-table-column
              v-for="channel in preferenceChannels"
              :key="channel.value"
              width="110"
              align="center"
            >
              <template #header>
                <div class="notice-checkbox-header">
                  <span>{{ channel.label }}</span>
                  <el-checkbox
                    :model-value="channelVisibleRowsEnabled(channel.value)"
                    :indeterminate="channelVisibleRowsIndeterminate(channel.value)"
                    @change="value => saveVisibleRowsChannel(channel.value, Boolean(value))"
                  />
                </div>
              </template>
              <template #default="{ row }">
                <el-checkbox
                  :model-value="messageChannelEnabled(row.bizType, channel.value)"
                  @change="value => saveMessagePreference(row.bizType, channel.value, Boolean(value))"
                />
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="accountDialog.visible" :title="accountDialogTitle" width="520px">
      <el-form :model="accountDialog.form" label-width="96px">
        <el-form-item label="账号类型" required>
          <el-select v-model="accountDialog.form.accountType" class="notice-form-control">
            <el-option v-for="item in accountTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号" required>
          <el-input v-model="accountDialog.form.accountValue" placeholder="手机号、邮箱或平台账号标识" />
        </el-form-item>
        <el-form-item label="显示名称">
          <el-input v-model="accountDialog.form.displayName" />
        </el-form-item>
        <el-form-item label="验证状态">
          <el-select v-model="accountDialog.form.verifiedStatus" class="notice-form-control">
            <el-option label="已验证" value="VERIFIED" />
            <el-option label="待验证" value="PENDING_VERIFY" />
            <el-option label="未绑定" value="UNBOUND" />
          </el-select>
        </el-form-item>
        <el-form-item label="默认账号">
          <el-switch v-model="accountDialog.form.defaultAccount" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="accountDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="accountDialog.saving" @click="saveAccount">保存</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus';
import {
  disableRecipientAccount,
  getBusinessTypes,
  getNoticeReminderSetting,
  getReceivePreferences,
  getRecipientAccounts,
  normalizeNoticeReminderSetting,
  saveReceivePreference,
  saveNoticeReminderSetting,
  saveRecipientAccount,
} from '../api/notice';
import { playNoticeSound, showDesktopNotice, speakNoticeText } from '../realtime/noticeRealtime';
import type {
  NoticeBusinessType,
  NoticeChannelType,
  NoticeReceivePreference,
  NoticeReceivePreferenceScopeType,
  NoticeReminderSetting,
  NoticeRecipientAccount,
  NoticeRecipientAccountStatus,
  NoticeRecipientAccountType,
  NoticeSiteMessage,
} from '../types/notice';
import { useNoticeDomains } from '../components/useNoticeDomains';

type AccountRow = {
  type?: NoticeRecipientAccountType;
  label: string;
  account?: NoticeRecipientAccount;
  accountValue?: string;
  system?: boolean;
};

const accountTypeOptions: Array<{ value: NoticeRecipientAccountType; label: string }> = [
  { value: 'MOBILE', label: '手机号' },
  { value: 'EMAIL', label: '邮箱' },
  { value: 'WECHAT', label: '微信' },
  { value: 'DINGTALK', label: '钉钉' },
  { value: 'FEISHU', label: '飞书' },
];

const accountRowsConfig: Array<{ type: NoticeRecipientAccountType; label: string }> = [
  { type: 'MOBILE', label: '手机号' },
  { type: 'EMAIL', label: '邮箱' },
  { type: 'WECHAT', label: '微信' },
  { type: 'WECOM', label: '企业微信' },
  { type: 'DINGTALK', label: '钉钉' },
  { type: 'FEISHU', label: '飞书' },
];

const preferenceChannels: Array<{ value: NoticeChannelType; label: string }> = [
  { value: 'SITE', label: '系统消息' },
  { value: 'SMS', label: '短信' },
  { value: 'EMAIL', label: '邮件' },
  { value: 'WECHAT_OFFICIAL', label: '公众号' },
  { value: 'WECOM', label: '企业微信' },
  { value: 'DINGTALK', label: '钉钉' },
];

const activeTab = ref('reminder');
const loading = reactive({ accounts: false, preferences: false, businessTypes: false, reminder: false });
const accounts = ref<NoticeRecipientAccount[]>([]);
const preferences = ref<NoticeReceivePreference[]>([]);
const businessTypes = ref<NoticeBusinessType[]>([]);
const bizKeyword = ref('');
const reminderSaving = ref(false);
const reminderSetting = reactive<NoticeReminderSetting>(normalizeNoticeReminderSetting());
const desktopPermission = ref<NotificationPermission | 'unsupported'>('unsupported');
const accountDialog = reactive({
  visible: false,
  saving: false,
  form: {
    id: '',
    accountType: 'MOBILE' as NoticeRecipientAccountType,
    accountValue: '',
    displayName: '',
    verifiedStatus: 'VERIFIED' as NoticeRecipientAccountStatus,
    defaultAccount: false,
  },
});
const { domainText, loadDomains } = useNoticeDomains();
const accountRows = computed<AccountRow[]>(() => {
  return [{
    label: '系统账号',
    accountValue: '账号ID',
    system: true,
  }, ...accountRowsConfig.map(item => ({
    ...item,
    account: accounts.value.find(account => account.accountType === item.type && account.enabled !== false),
  }))];
});

const filteredBusinessTypes = computed(() => {
  const keyword = bizKeyword.value.trim().toLowerCase();
  if (!keyword) return businessTypes.value;
  return businessTypes.value.filter(item =>
    [item.bizName, item.bizType, item.bizGroup].some(value => value?.toLowerCase().includes(keyword)),
  );
});

const accountDialogTitle = computed(() => {
  const label = accountTypeText(accountDialog.form.accountType);
  return `${accountDialog.form.id ? '修改' : '绑定'}${label}`;
});

const desktopPermissionText = computed(() => {
  const textMap: Record<typeof desktopPermission.value, string> = {
    granted: '已授权',
    default: '未授权',
    denied: '已阻止',
    unsupported: '不支持',
  };
  return textMap[desktopPermission.value];
});

const desktopPermissionTagType = computed(() => {
  const typeMap: Record<typeof desktopPermission.value, 'success' | 'warning' | 'danger' | 'info'> = {
    granted: 'success',
    default: 'warning',
    denied: 'danger',
    unsupported: 'info',
  };
  return typeMap[desktopPermission.value];
});

const allVisibleRowsEnabled = computed(() => {
  const rows = filteredBusinessTypes.value;
  return rows.length > 0 && rows.every(row => messageAllChannelsEnabled(row.bizType));
});

const allVisibleRowsIndeterminate = computed(() => {
  const rows = filteredBusinessTypes.value;
  if (rows.length === 0) return false;
  const enabledCount = rows.filter(row => messageAllChannelsEnabled(row.bizType)).length;
  return enabledCount > 0 && enabledCount < rows.length;
});

function preferenceKey(scopeType: NoticeReceivePreferenceScopeType, scopeValue?: string, channelType?: NoticeChannelType) {
  return [scopeType, scopeValue || '', channelType || 'ALL'].join(':');
}

function findPreference(scopeType: NoticeReceivePreferenceScopeType, scopeValue?: string, channelType?: NoticeChannelType) {
  const normalizedScope = scopeValue || '';
  return preferences.value.find(item =>
    item.scopeType === scopeType
    && (item.scopeValue || '') === normalizedScope
    && (item.channelType || undefined) === channelType,
  );
}

function messageChannelEnabled(bizType: string, channelType: NoticeChannelType) {
  return findPreference('BIZ_TYPE', bizType, channelType)?.enabled ?? true;
}

function messageAllChannelsEnabled(bizType: string) {
  return preferenceChannels.every(channel => messageChannelEnabled(bizType, channel.value));
}

function messageAllChannelsIndeterminate(bizType: string) {
  const enabledCount = preferenceChannels.filter(channel => messageChannelEnabled(bizType, channel.value)).length;
  return enabledCount > 0 && enabledCount < preferenceChannels.length;
}

function channelVisibleRowsEnabled(channelType: NoticeChannelType) {
  const rows = filteredBusinessTypes.value;
  return rows.length > 0 && rows.every(row => messageChannelEnabled(row.bizType, channelType));
}

function channelVisibleRowsIndeterminate(channelType: NoticeChannelType) {
  const rows = filteredBusinessTypes.value;
  if (rows.length === 0) return false;
  const enabledCount = rows.filter(row => messageChannelEnabled(row.bizType, channelType)).length;
  return enabledCount > 0 && enabledCount < rows.length;
}

function accountDisplayValue(row: AccountRow) {
  return row.account?.displayName || row.account?.accountValue || '-';
}

function modifyAccountText(row: AccountRow) {
  return `修改${row.label}`;
}

function canUnbindAccount(row: AccountRow) {
  if (!row.type) return false;
  return row.type !== 'MOBILE' && row.type !== 'EMAIL';
}

function openAccountEdit(row: AccountRow) {
  openAccountDialog(row.account);
}

function openAccountBind(row: AccountRow) {
  openAccountDialog(undefined, row.type);
}

function accountTypeText(type: NoticeRecipientAccountType) {
  return accountTypeOptions.find(item => item.value === type)?.label || type;
}

function accountStatusText(status: NoticeRecipientAccountStatus) {
  return ({ VERIFIED: '已验证', PENDING_VERIFY: '待验证', UNBOUND: '未绑定', DISABLED: '已停用' } as Record<string, string>)[status] || status;
}

async function loadAccounts() {
  loading.accounts = true;
  try {
    accounts.value = await getRecipientAccounts();
  } finally {
    loading.accounts = false;
  }
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
    const result = await getBusinessTypes({ pageNum: 1, pageSize: 200 });
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

function openAccountDialog(row?: NoticeRecipientAccount, accountType: NoticeRecipientAccountType = 'MOBILE') {
  Object.assign(accountDialog.form, row ? {
    id: row.id,
    accountType: row.accountType,
    accountValue: row.accountValue,
    displayName: row.displayName || '',
    verifiedStatus: row.verifiedStatus,
    defaultAccount: row.defaultAccount,
  } : {
    id: '',
    accountType,
    accountValue: '',
    displayName: '',
    verifiedStatus: 'VERIFIED',
    defaultAccount: false,
  });
  accountDialog.visible = true;
}

async function saveAccount() {
  accountDialog.saving = true;
  try {
    await saveRecipientAccount({
      ...accountDialog.form,
      id: accountDialog.form.id || undefined,
    });
    ElMessage.success('账号已保存');
    accountDialog.visible = false;
    await loadAccounts();
  } finally {
    accountDialog.saving = false;
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

function testReminderSetting() {
  const setting = normalizeNoticeReminderSetting(reminderSetting);
  Object.assign(reminderSetting, setting);
  const now = new Date().toLocaleString('zh-CN', { hour12: false });
  const message: NoticeSiteMessage = {
    id: `notice-reminder-test-${Date.now()}`,
    title: '提醒设置测试',
    content: `这是一条本地测试消息，用于验证浏览器内弹窗、语音提示和 Chrome 桌面提示。测试时间：${now}`,
    userId: 'current',
    priority: 'NORMAL',
    readStatus: 'UNREAD',
    bizGroup: '系统',
    bizName: '提醒设置',
    bizType: 'notice.reminder_test',
    createTime: now,
  };
  if (setting.popupEnabled) {
    try {
      ElNotification({
        title: message.title,
        message: message.content,
        type: 'info',
        position: setting.popupPlacement,
        duration: 5000,
      });
    } catch {
      ElMessage.warning('浏览器内弹窗触发失败');
    }
  }
  if (setting.voiceEnabled) {
    try {
      if (setting.reminderMode === 'VOICE') {
        speakNoticeText(setting.voiceText || message.title);
      } else {
        playNoticeSound(setting.soundType);
      }
    } catch {
      ElMessage.warning('声音提醒触发失败');
    }
  }
  testDesktopNotification(message);
  ElMessage.success('已触发本地提醒测试');
}

async function testDesktopNotification(message: NoticeSiteMessage) {
  refreshDesktopPermission();
  const setting = normalizeNoticeReminderSetting(reminderSetting);
  if (!setting.desktopNotificationEnabled) {
    return;
  }
  if (!('Notification' in window)) {
    desktopPermission.value = 'unsupported';
    ElMessage.warning('当前浏览器不支持桌面提示');
    return;
  }
  if (desktopPermission.value === 'default') {
    await requestDesktopNoticePermission();
  }
  if (desktopPermission.value === 'granted') {
    try {
      showDesktopNotice(message, () => undefined);
    } catch {
      ElMessage.warning('桌面提示触发失败');
    }
  } else if (desktopPermission.value === 'denied') {
    ElMessage.warning('桌面提示已被浏览器阻止，请在站点设置中允许通知');
  }
}

function refreshDesktopPermission() {
  desktopPermission.value = 'Notification' in window ? Notification.permission : 'unsupported';
}

async function requestDesktopNoticePermission() {
  if (!('Notification' in window)) {
    desktopPermission.value = 'unsupported';
    ElMessage.warning('当前浏览器不支持桌面提示');
    return;
  }
  const permission = await Notification.requestPermission();
  desktopPermission.value = permission;
  if (permission === 'granted') {
    ElMessage.success('桌面提示已授权');
  } else if (permission === 'denied') {
    ElMessage.warning('桌面提示已被浏览器阻止');
  }
}

async function disableAccount(row: NoticeRecipientAccount) {
  await ElMessageBox.confirm('确认解绑该账号吗？', '解绑确认', { type: 'warning' });
  await disableRecipientAccount(row.id);
  ElMessage.success('账号已解绑');
  await loadAccounts();
}

async function saveMessagePreference(bizType: string, channelType: NoticeChannelType | undefined, enabled: boolean) {
  const saved = await saveReceivePreference({
    scopeType: 'BIZ_TYPE',
    scopeValue: bizType,
    channelType,
    enabled,
  });
  upsertPreference(saved);
  ElMessage.success('消息接收设置已保存');
}

async function saveMessageAllChannels(bizType: string, enabled: boolean) {
  await Promise.all(preferenceChannels.map(channel => saveReceivePreference({
    scopeType: 'BIZ_TYPE',
    scopeValue: bizType,
    channelType: channel.value,
    enabled,
  }).then(upsertPreference)));
  ElMessage.success('消息接收设置已保存');
}

async function saveVisibleRowsChannel(channelType: NoticeChannelType, enabled: boolean) {
  await Promise.all(filteredBusinessTypes.value.map(row => saveReceivePreference({
    scopeType: 'BIZ_TYPE',
    scopeValue: row.bizType,
    channelType,
    enabled,
  }).then(upsertPreference)));
  ElMessage.success('接收规则已保存');
}

async function saveAllVisibleRows(enabled: boolean) {
  const commands = filteredBusinessTypes.value.flatMap(row => preferenceChannels.map(channel => ({
    scopeType: 'BIZ_TYPE' as const,
    scopeValue: row.bizType,
    channelType: channel.value,
    enabled,
  })));
  await Promise.all(commands.map(command => saveReceivePreference(command).then(upsertPreference)));
  ElMessage.success('接收规则已保存');
}

function upsertPreference(preference: NoticeReceivePreference) {
  const key = preferenceKey(preference.scopeType, preference.scopeValue, preference.channelType);
  const index = preferences.value.findIndex(item => preferenceKey(item.scopeType, item.scopeValue, item.channelType) === key);
  if (index >= 0) {
    preferences.value.splice(index, 1, preference);
  } else {
    preferences.value.push(preference);
  }
}

onMounted(() => {
  refreshDesktopPermission();
  void loadDomains();
  void loadAccounts();
  void loadPreferences();
  void loadBusinessTypes();
  void loadReminderSetting();
});
</script>

<style scoped>
.notice-receive-setting-page {
  padding: 0;
}

.notice-section + .notice-section {
  margin-top: 16px;
}

.notice-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.notice-search {
  width: 260px;
}

.notice-form-control {
  width: 100%;
}

.notice-reminder-form {
  max-width: 960px;
  padding-top: 4px;
}

.notice-reminder-group {
  padding: 4px 0 10px;
}

.notice-reminder-group + .notice-reminder-group {
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.notice-reminder-group__title {
  margin-bottom: 14px;
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
}

.notice-reminder-actions {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.notice-desktop-setting {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

</style>
