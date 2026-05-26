<template>
  <div class="notice-channel-page">
    <el-card shadow="never">
      <template #header>
        <div class="notice-page-header">
          <span>渠道配置</span>
          <el-button type="primary" @click="openCreate">新增渠道</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="query" class="notice-filter">
        <el-form-item label="渠道类型">
          <el-select v-model="query.channelType" clearable placeholder="全部" class="filter-control">
            <el-option v-for="item in channels" :key="item" :label="channelLabel(item)" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用状态">
          <el-select v-model="query.enabled" clearable placeholder="全部" class="filter-control">
            <el-option label="启用" :value="true" />
            <el-option label="停用" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="configs" border stripe v-loading="loading">
        <el-table-column label="渠道类型" width="130">
          <template #default="{ row }">{{ channelLabel(row.channelType) }}</template>
        </el-table-column>
        <el-table-column prop="configName" label="渠道名称" min-width="170" />
        <el-table-column label="供应商" width="140">
          <template #default="{ row }">{{ providerLabel(row.channelType, row.providerCode) }}</template>
        </el-table-column>
        <el-table-column prop="weight" label="权重" width="90" />
        <el-table-column label="启用状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="配置状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.configStatus === 'COMPLETE' ? 'success' : 'warning'">
              {{ row.configStatus === 'COMPLETE' ? '完整' : '未完成' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最近发送状态" width="130">
          <template #default="{ row }">
            <el-tag :type="sendStatusTag(row.lastSendStatus)">
              {{ sendStatusLabel(row.lastSendStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastSendTime" label="最近发送时间" width="170" />
        <el-table-column prop="updatedAt" label="更新时间" width="170" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="dialogTitle" width="860px">
      <el-form :model="form" label-width="112px">
        <el-divider content-position="left">基础信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="渠道类型">
              <el-select v-model="form.channelType" class="form-control" @change="handleChannelTypeChange">
                <el-option v-for="item in channels" :key="item" :label="channelLabel(item)" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商">
              <el-select v-model="form.providerCode" class="form-control" @change="resetChannelConfig">
                <el-option
                  v-for="item in providerOptions(form.channelType || 'EMAIL')"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="渠道名称"><el-input v-model="form.configName" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="权重"><el-input-number v-model="form.weight" :min="1" :max="1000" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="启用"><el-switch v-model="form.enabled" /></el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">渠道参数</el-divider>
        <template v-if="form.channelType === 'SITE'">
          <el-alert title="站内信使用系统内置投递能力，无需配置供应商账号。" type="info" :closable="false" show-icon />
        </template>
        <template v-else-if="form.channelType === 'SMS'">
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="AccessKey"><el-input v-model="channelConfig.accessKeyId" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="Secret"><el-input v-model="channelConfig.accessKeySecret" show-password /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="签名"><el-input v-model="channelConfig.signName" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="模板平台"><el-input v-model="channelConfig.templatePlatform" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="接入地址"><el-input v-model="channelConfig.endpoint" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="通知地址"><el-input v-model="channelConfig.callbackUrl" /></el-form-item></el-col>
          </el-row>
        </template>
        <template v-else-if="form.channelType === 'EMAIL'">
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="SMTP"><el-input v-model="channelConfig.host" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="端口"><el-input-number v-model="channelConfig.port" :min="1" :max="65535" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="账号"><el-input v-model="channelConfig.username" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="密码"><el-input v-model="channelConfig.password" show-password /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="发件人"><el-input v-model="channelConfig.from" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="SSL"><el-switch v-model="channelConfig.ssl" /></el-form-item></el-col>
          </el-row>
        </template>
        <template v-else-if="form.channelType === 'WECHAT_OFFICIAL'">
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="AppId"><el-input v-model="channelConfig.appId" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="Secret"><el-input v-model="channelConfig.appSecret" show-password /></el-form-item></el-col>
          </el-row>
        </template>
        <template v-else-if="form.channelType === 'WECOM'">
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="企业 ID"><el-input v-model="channelConfig.corpId" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="AgentId"><el-input v-model="channelConfig.agentId" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="Secret"><el-input v-model="channelConfig.secret" show-password /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="Webhook"><el-input v-model="channelConfig.webhookUrl" /></el-form-item></el-col>
          </el-row>
        </template>
        <template v-else>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="应用 Key"><el-input v-model="channelConfig.appKey" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="应用 Secret"><el-input v-model="channelConfig.appSecret" show-password /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="AgentId"><el-input v-model="channelConfig.agentId" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="Webhook"><el-input v-model="channelConfig.webhookUrl" /></el-form-item></el-col>
          </el-row>
        </template>

        <el-divider content-position="left">通用配置</el-divider>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="每分钟"><el-input-number v-model="rateLimit.maxPerMinute" :min="0" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="超时秒数"><el-input-number v-model="rateLimit.timeoutSeconds" :min="1" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="并发限制"><el-input-number v-model="rateLimit.concurrentLimit" :min="0" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="渠道详情" size="520px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="渠道类型">{{ channelLabel(current.channelType) }}</el-descriptions-item>
        <el-descriptions-item label="渠道名称">{{ current.configName }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ providerLabel(current.channelType, current.providerCode) }}</el-descriptions-item>
        <el-descriptions-item label="权重">{{ current.weight }}</el-descriptions-item>
        <el-descriptions-item label="配置状态">{{ current.configStatus === 'COMPLETE' ? '完整' : '未完成' }}</el-descriptions-item>
        <el-descriptions-item label="最近失败">{{ current.lastFailureReason || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { getChannelConfigs, saveChannelConfig } from '../../api/notice';
import type { NoticeChannelConfig, NoticeChannelSendHealthStatus, NoticeChannelType } from '../../types/notice';

type ChannelConfigValue = string | number | boolean | undefined;
type ChannelConfigForm = Record<string, ChannelConfigValue>;

const channels: NoticeChannelType[] = ['SITE', 'SMS', 'EMAIL', 'WECHAT_OFFICIAL', 'WECOM', 'DINGTALK'];
const channelLabels: Record<NoticeChannelType, string> = {
  SITE: '站内信',
  SMS: '短信',
  EMAIL: '邮件',
  WECHAT_OFFICIAL: '微信公众号',
  WECOM: '企业微信',
  DINGTALK: '钉钉',
};
const providers: Record<NoticeChannelType, Array<{ label: string; value: string }>> = {
  SITE: [{ label: '系统内置', value: 'INTERNAL' }],
  SMS: [
    { label: '阿里云短信', value: 'ALIYUN_SMS' },
    { label: '腾讯云短信', value: 'TENCENT_SMS' },
  ],
  EMAIL: [
    { label: '自建 SMTP', value: 'CUSTOM_SMTP' },
    { label: '阿里云邮件推送', value: 'ALIYUN_DM' },
    { label: '腾讯云 SES', value: 'TENCENT_SES' },
    { label: 'SendCloud', value: 'SENDCLOUD' },
    { label: 'Mailgun', value: 'MAILGUN' },
    { label: '其它', value: 'OTHER' },
  ],
  WECHAT_OFFICIAL: [{ label: '微信公众号', value: 'WECHAT_OFFICIAL' }],
  WECOM: [{ label: '企业微信', value: 'WECOM' }],
  DINGTALK: [{ label: '钉钉', value: 'DINGTALK' }],
};

const loading = ref(false);
const visible = ref(false);
const detailVisible = ref(false);
const configs = ref<NoticeChannelConfig[]>([]);
const current = ref<NoticeChannelConfig>();
const query = reactive<{ channelType?: NoticeChannelType; enabled?: boolean }>({});
const form = reactive<Partial<NoticeChannelConfig>>({ channelType: 'EMAIL', providerCode: 'CUSTOM_SMTP', enabled: true, weight: 100, priority: 0 });
const channelConfig = reactive<ChannelConfigForm>({});
const rateLimit = reactive({ maxPerMinute: 0, timeoutSeconds: 10, concurrentLimit: 0 });

const dialogTitle = computed(() => (form.id ? '编辑渠道配置' : '新增渠道配置'));
const configJsonPreview = computed(() => JSON.stringify(compactObject(channelConfig), null, 2));
const rateLimitJsonPreview = computed(() => JSON.stringify(compactObject(rateLimit), null, 2));

async function load() {
  loading.value = true;
  try {
    const result = await getChannelConfigs(query);
    configs.value = result.list || [];
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  Object.assign(form, { id: undefined, channelType: 'EMAIL', providerCode: 'CUSTOM_SMTP', configName: '', enabled: true, weight: 100, priority: 0 });
  resetChannelConfig();
  visible.value = true;
}

function openEdit(row: NoticeChannelConfig) {
  Object.assign(form, row);
  parseConfig(row.configJson, channelConfig);
  parseConfig(row.rateLimitConfig, rateLimit);
  visible.value = true;
}

function openDetail(row: NoticeChannelConfig) {
  current.value = row;
  detailVisible.value = true;
}

function handleChannelTypeChange() {
  const channelType = form.channelType || 'EMAIL';
  form.providerCode = providerOptions(channelType)[0]?.value;
  resetChannelConfig();
}

function resetChannelConfig() {
  Object.keys(channelConfig).forEach(key => delete channelConfig[key]);
  Object.assign(channelConfig, defaultConfig(form.channelType || 'EMAIL'));
}

function defaultConfig(channelType: NoticeChannelType): ChannelConfigForm {
  if (channelType === 'SITE') return {};
  if (channelType === 'SMS') return { accessKeyId: '', accessKeySecret: '', signName: '', templatePlatform: providerLabel('SMS', form.providerCode), endpoint: '', callbackUrl: '' };
  if (channelType === 'EMAIL') return { host: '', port: 465, username: '', password: '', from: '', ssl: true };
  if (channelType === 'WECHAT_OFFICIAL') return { appId: '', appSecret: '' };
  if (channelType === 'WECOM') return { corpId: '', agentId: '', secret: '', webhookUrl: '' };
  return { appKey: '', appSecret: '', agentId: '', webhookUrl: '' };
}

function providerOptions(channelType: NoticeChannelType) {
  return providers[channelType] || [];
}

function compactObject(source: Record<string, ChannelConfigValue>) {
  return Object.fromEntries(Object.entries(source).filter(([, value]) => value !== '' && value !== undefined));
}

function parseConfig(value: string | undefined, target: Record<string, ChannelConfigValue>) {
  Object.keys(target).forEach(key => delete target[key]);
  if (!value) return;
  try {
    Object.assign(target, JSON.parse(value));
  } catch {
    Object.assign(target, {});
  }
}

function channelLabel(channel: NoticeChannelType) {
  return channelLabels[channel];
}

function providerLabel(channelType: NoticeChannelType, providerCode?: string) {
  return providerOptions(channelType).find(item => item.value === providerCode)?.label || providerCode || '-';
}

function sendStatusLabel(status?: NoticeChannelSendHealthStatus) {
  if (status === 'SUCCESS') return '成功';
  if (status === 'FAILED') return '失败';
  return '未发送';
}

function sendStatusTag(status?: NoticeChannelSendHealthStatus) {
  if (status === 'SUCCESS') return 'success';
  if (status === 'FAILED') return 'danger';
  return 'info';
}

async function save() {
  await saveChannelConfig({
    ...form,
    configJson: configJsonPreview.value,
    rateLimitConfig: rateLimitJsonPreview.value,
  });
  ElMessage.success('已保存');
  visible.value = false;
  await load();
}

onMounted(load);
</script>

<style scoped>
.notice-channel-page { padding: 0; }
.notice-page-header { display: flex; align-items: center; justify-content: space-between; }
.notice-filter { margin-bottom: 12px; }
.filter-control { width: 160px; }
.form-control { width: 100%; }
</style>
