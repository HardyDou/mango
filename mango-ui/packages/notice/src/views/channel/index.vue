<template>
  <div class="notice-channel-page">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="page-card__header">
          <span>渠道配置</span>
        </div>
      </template>

      <div class="list-toolbar">
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
        <el-button type="primary" :icon="Plus" @click="openCreate">新增</el-button>
      </div>

      <el-table :data="configs" border stripe v-loading="loading">
        <el-table-column label="渠道类型" width="120">
          <template #default="{ row }">{{ channelLabel(row.channelType) }}</template>
        </el-table-column>
        <el-table-column prop="configName" label="通道名称" min-width="170" show-overflow-tooltip />
        <el-table-column label="接入平台" width="140">
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
        <el-table-column label="最近发送" width="110">
          <template #default="{ row }">
            <el-tag :type="sendStatusTag(row.lastSendStatus)">{{ sendStatusLabel(row.lastSendStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastSendTime" label="最近发送时间" width="170" />
        <el-table-column prop="updatedAt" label="更新时间" width="170" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-tooltip v-if="!isBuiltinSiteChannel(row)" content="删除" placement="top">
              <el-button
                class="table-icon-button"
                text
                type="danger"
                :icon="Delete"
                aria-label="删除渠道"
                @click="removeChannel(row)"
              />
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="dialogTitle" width="760px" class="channel-dialog" destroy-on-close>
      <el-form :model="form" label-width="92px" class="channel-form">
        <section class="form-section">
          <div class="section-title">基础信息</div>
          <el-row :gutter="16">
            <el-col :xs="24" :sm="12">
              <el-form-item label="渠道类型" required>
                <el-select v-model="form.channelType" class="form-control" @change="handleChannelTypeChange">
                  <el-option v-for="item in channels" :key="item" :label="channelLabel(item)" :value="item" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="接入平台" required>
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
            <el-col :xs="24" :sm="12">
              <el-form-item label="通道名称" required>
                <el-input v-model="form.configName" placeholder="例如：阿里云短信主通道" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="权重" required>
                <el-input-number v-model="form.weight" :min="1" :max="1000" class="number-control" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="启用">
                <el-switch v-model="form.enabled" />
              </el-form-item>
            </el-col>
          </el-row>
        </section>

        <section class="form-section">
          <div class="section-title">渠道配置</div>
          <el-row :gutter="16">
            <el-col :xs="24" :sm="8">
              <el-form-item label="每分钟">
                <el-input-number v-model="rateLimit.maxPerMinute" :min="0" class="number-control" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-form-item label="超时秒数">
                <el-input-number v-model="rateLimit.timeoutSeconds" :min="1" class="number-control" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="8">
              <el-form-item label="并发限制">
                <el-input-number v-model="rateLimit.concurrentLimit" :min="0" class="number-control" />
              </el-form-item>
            </el-col>
          </el-row>
        </section>

        <section class="form-section">
          <div class="section-title">渠道参数</div>
          <el-tabs v-model="configEditMode" class="stable-tabs channel-config-tabs" @tab-change="handleConfigModeChange">
            <el-tab-pane label="表单形式" name="FORM">
              <template v-if="form.channelType === 'SITE'">
                <el-alert title="系统消息为系统内置通道，这里只配置投递运行参数。" type="info" :closable="false" show-icon />
                <el-row :gutter="16" class="site-config-row">
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="默认发送人">
                      <el-input v-model="channelConfig.senderName" placeholder="系统通知" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="保留天数">
                      <el-input-number v-model="channelConfig.retentionDays" :min="1" :max="3650" class="number-control" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="实时推送">
                      <el-switch v-model="channelConfig.realtimeEnabled" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="弹窗提醒">
                      <el-switch v-model="channelConfig.popupEnabled" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="声音提醒">
                      <el-switch v-model="channelConfig.soundEnabled" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="桌面提醒">
                      <el-switch v-model="channelConfig.desktopNotificationEnabled" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="未读计数">
                      <el-switch v-model="channelConfig.unreadCountEnabled" />
                    </el-form-item>
                  </el-col>
                </el-row>
              </template>
              <template v-else-if="form.channelType === 'SMS'">
                <el-row :gutter="16">
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="AccessKey" required>
                      <el-input v-model="channelConfig.accessKeyId" autocomplete="off" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="Secret" required>
                      <el-input v-model="channelConfig.accessKeySecret" show-password autocomplete="new-password" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="短信签名" required>
                      <el-input v-model="channelConfig.signName" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="模板平台">
                      <el-input v-model="channelConfig.templatePlatform" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="接入地址">
                      <el-input v-model="channelConfig.endpoint" placeholder="dysmsapi.aliyuncs.com" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="通知地址">
                      <el-input v-model="channelConfig.callbackUrl" placeholder="回调地址" />
                    </el-form-item>
                  </el-col>
                </el-row>
              </template>
              <template v-else-if="form.channelType === 'EMAIL' && form.providerCode === 'CUSTOM_SMTP'">
                <el-row :gutter="16">
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="SMTP" required>
                      <el-input v-model="channelConfig.host" placeholder="smtp.example.com" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="端口" required>
                      <el-input-number v-model="channelConfig.port" :min="1" :max="65535" class="number-control" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="账号" required>
                      <el-input v-model="channelConfig.username" autocomplete="off" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="密码" required>
                      <el-input v-model="channelConfig.password" show-password autocomplete="new-password" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="发件人" required>
                      <el-input v-model="channelConfig.from" placeholder="notice@example.com" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="SSL">
                      <el-switch v-model="channelConfig.ssl" />
                    </el-form-item>
                  </el-col>
                </el-row>
              </template>
              <template v-else-if="form.channelType === 'EMAIL' && form.providerCode === 'ALIYUN_DM'">
                <el-row :gutter="16">
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="AccessKey" required>
                      <el-input v-model="channelConfig.accessKeyId" autocomplete="off" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="Secret" required>
                      <el-input v-model="channelConfig.accessKeySecret" show-password autocomplete="new-password" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="区域" required>
                      <el-input v-model="channelConfig.regionId" placeholder="cn-hangzhou" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="Endpoint" required>
                      <el-input v-model="channelConfig.endpoint" placeholder="dm.aliyuncs.com" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="发信地址" required>
                      <el-input v-model="channelConfig.accountName" placeholder="notice@example.com" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="地址类型">
                      <el-select v-model="channelConfig.addressType" class="form-control">
                        <el-option label="随机账号" :value="0" />
                        <el-option label="发信地址" :value="1" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="发信别名">
                      <el-input v-model="channelConfig.fromAlias" placeholder="芒果通知" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="回信地址">
                      <el-input v-model="channelConfig.replyToAddress" placeholder="reply@example.com" />
                    </el-form-item>
                  </el-col>
                </el-row>
              </template>
              <template v-else-if="form.channelType === 'EMAIL'">
                <el-alert title="当前接入平台暂未提供专用表单，请使用 JSON 形式维护渠道参数。" type="warning" :closable="false" show-icon />
              </template>
              <template v-else-if="form.channelType === 'WECHAT_OFFICIAL'">
                <el-row :gutter="16">
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="AppId" required>
                      <el-input v-model="channelConfig.appId" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="Secret" required>
                      <el-input v-model="channelConfig.appSecret" show-password autocomplete="new-password" />
                    </el-form-item>
                  </el-col>
                </el-row>
              </template>
              <template v-else-if="form.channelType === 'WECOM'">
                <el-row :gutter="16">
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="企业ID">
                      <el-input v-model="channelConfig.corpId" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="AgentId">
                      <el-input v-model="channelConfig.agentId" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="Secret">
                      <el-input v-model="channelConfig.secret" show-password autocomplete="new-password" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="Webhook">
                      <el-input v-model="channelConfig.webhookUrl" />
                    </el-form-item>
                  </el-col>
                </el-row>
              </template>
              <template v-else>
                <el-row :gutter="16">
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="应用Key">
                      <el-input v-model="channelConfig.appKey" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="应用Secret">
                      <el-input v-model="channelConfig.appSecret" show-password autocomplete="new-password" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="AgentId">
                      <el-input v-model="channelConfig.agentId" />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12">
                    <el-form-item label="Webhook">
                      <el-input v-model="channelConfig.webhookUrl" />
                    </el-form-item>
                  </el-col>
                </el-row>
              </template>
            </el-tab-pane>
            <el-tab-pane label="JSON 形式" name="JSON">
              <el-input
                v-model="configJsonText"
                class="json-editor"
                type="textarea"
                :rows="12"
                spellcheck="false"
                placeholder="{&quot;host&quot;:&quot;smtp.example.com&quot;}"
              />
            </el-tab-pane>
          </el-tabs>
        </section>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="渠道详情" width="680px" class="channel-detail-dialog" destroy-on-close>
      <template v-if="current">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="渠道类型">{{ channelLabel(current.channelType) }}</el-descriptions-item>
          <el-descriptions-item label="通道名称">{{ current.configName }}</el-descriptions-item>
          <el-descriptions-item label="接入平台">{{ providerLabel(current.channelType, current.providerCode) }}</el-descriptions-item>
          <el-descriptions-item label="权重">{{ current.weight }}</el-descriptions-item>
          <el-descriptions-item label="启用状态">{{ current.enabled ? '启用' : '停用' }}</el-descriptions-item>
          <el-descriptions-item label="配置状态">{{ current.configStatus === 'COMPLETE' ? '完整' : '未完成' }}</el-descriptions-item>
          <el-descriptions-item label="最近发送">{{ sendStatusLabel(current.lastSendStatus) }}</el-descriptions-item>
          <el-descriptions-item label="最近发送时间">{{ current.lastSendTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近失败码">{{ current.lastFailureCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最近失败原因">{{ current.lastFailureReason || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="detail-section-title">渠道配置</div>
        <el-descriptions :column="3" border>
          <el-descriptions-item v-for="item in detailRateLimitItems(current)" :key="item.key" :label="item.label">
            {{ item.value }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="detail-section-title">渠道参数</div>
        <el-descriptions :column="2" border>
          <el-descriptions-item v-for="item in detailConfigItems(current)" :key="item.key" :label="item.label">
            {{ item.value }}
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Delete, Plus } from '@element-plus/icons-vue';
import { deleteChannelConfig, getChannelConfigs, saveChannelConfig } from '../../api/notice';
import type { NoticeChannelConfig, NoticeChannelSendHealthStatus, NoticeChannelType } from '../../types/notice';

type ConfigEditMode = 'FORM' | 'JSON';
type ChannelConfigValue = string | number | boolean | undefined;
type ChannelConfigForm = Record<string, ChannelConfigValue>;

const channels: NoticeChannelType[] = ['SITE', 'SMS', 'EMAIL', 'WECHAT_OFFICIAL', 'WECOM', 'DINGTALK'];
const channelLabels: Record<NoticeChannelType, string> = {
  SITE: '系统消息',
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
  ],
  WECHAT_OFFICIAL: [{ label: '微信公众号', value: 'WECHAT_OFFICIAL' }],
  WECOM: [{ label: '企业微信', value: 'WECOM' }],
  DINGTALK: [{ label: '钉钉', value: 'DINGTALK' }],
};

const loading = ref(false);
const visible = ref(false);
const detailVisible = ref(false);
const configEditMode = ref<ConfigEditMode>('FORM');
const configs = ref<NoticeChannelConfig[]>([]);
const current = ref<NoticeChannelConfig>();
const query = reactive<{ channelType?: NoticeChannelType; enabled?: boolean }>({});
const form = reactive<Partial<NoticeChannelConfig>>({});
const channelConfig = reactive<ChannelConfigForm>({});
const rateLimit = reactive({ maxPerMinute: 0, timeoutSeconds: 10, concurrentLimit: 0 });
const configJsonText = ref('{}');

const dialogTitle = computed(() => (form.id ? '编辑渠道' : '新增渠道'));
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
  Object.assign(form, {
    id: undefined,
    channelType: 'EMAIL',
    providerCode: 'CUSTOM_SMTP',
    configName: '',
    enabled: true,
    weight: 100,
    priority: 0,
  });
  resetRateLimit();
  resetChannelConfig();
  configEditMode.value = 'FORM';
  visible.value = true;
}

function openEdit(row: NoticeChannelConfig) {
  Object.assign(form, row);
  form.providerCode = normalizeProviderCode(row.channelType, row.providerCode);
  resetChannelConfig();
  parseConfig(row.configJson, channelConfig, defaultConfig(row.channelType));
  resetRateLimit();
  parseConfig(row.rateLimitConfig, rateLimit, defaultRateLimit());
  configJsonText.value = configJsonPreview.value;
  configEditMode.value = 'FORM';
  visible.value = true;
}

function openDetail(row: NoticeChannelConfig) {
  current.value = row;
  detailVisible.value = true;
}

function detailConfigItems(row: NoticeChannelConfig) {
  const config = fromJson(row.configJson);
  return configFieldLabels(row.channelType, row.providerCode).map(item => ({
    ...item,
    value: displayConfigValue(config[item.key]),
  }));
}

function detailRateLimitItems(row: NoticeChannelConfig) {
  const config = { ...defaultRateLimit(), ...fromJson(row.rateLimitConfig) };
  return [
    { key: 'maxPerMinute', label: '每分钟', value: displayConfigValue(config.maxPerMinute) },
    { key: 'timeoutSeconds', label: '超时秒数', value: displayConfigValue(config.timeoutSeconds) },
    { key: 'concurrentLimit', label: '并发限制', value: displayConfigValue(config.concurrentLimit) },
  ];
}

function isBuiltinSiteChannel(row: NoticeChannelConfig) {
  return row.channelType === 'SITE' && row.providerCode === 'INTERNAL';
}

function handleChannelTypeChange() {
  const channelType = form.channelType || 'EMAIL';
  form.providerCode = providerOptions(channelType)[0]?.value;
  resetChannelConfig();
}

function resetChannelConfig() {
  Object.keys(channelConfig).forEach(key => delete channelConfig[key]);
  Object.assign(channelConfig, defaultConfig(form.channelType || 'EMAIL'));
  configJsonText.value = configJsonPreview.value;
}

function resetRateLimit() {
  Object.assign(rateLimit, defaultRateLimit());
}

function defaultRateLimit() {
  return { maxPerMinute: 0, timeoutSeconds: 10, concurrentLimit: 0 };
}

function defaultConfig(channelType: NoticeChannelType): ChannelConfigForm {
  if (channelType === 'SITE') {
    return {
      senderName: '系统通知',
      retentionDays: 180,
      realtimeEnabled: true,
      popupEnabled: true,
      soundEnabled: true,
      desktopNotificationEnabled: true,
      unreadCountEnabled: true,
    };
  }
  if (channelType === 'SMS') {
    return {
      accessKeyId: '',
      accessKeySecret: '',
      signName: '',
      templatePlatform: providerLabel('SMS', form.providerCode),
      endpoint: '',
      callbackUrl: '',
    };
  }
  if (channelType === 'EMAIL') return defaultEmailConfig(form.providerCode);
  if (channelType === 'WECHAT_OFFICIAL') return { appId: '', appSecret: '' };
  if (channelType === 'WECOM') return { corpId: '', agentId: '', secret: '', webhookUrl: '' };
  return { appKey: '', appSecret: '', agentId: '', webhookUrl: '' };
}

function defaultEmailConfig(providerCode?: string): ChannelConfigForm {
  if (providerCode === 'ALIYUN_DM') {
    return {
      accessKeyId: '',
      accessKeySecret: '',
      regionId: 'cn-hangzhou',
      endpoint: 'dm.aliyuncs.com',
      accountName: '',
      addressType: 1,
      fromAlias: '',
      replyToAddress: '',
    };
  }
  return { host: '', port: 465, username: '', password: '', from: '', ssl: true };
}

function configFieldLabels(channelType: NoticeChannelType, providerCode?: string) {
  const normalizedProvider = normalizeProviderCode(channelType, providerCode);
  if (channelType === 'SITE') {
    return [
      { key: 'senderName', label: '默认发送人' },
      { key: 'retentionDays', label: '保留天数' },
      { key: 'realtimeEnabled', label: '实时推送' },
      { key: 'popupEnabled', label: '弹窗提醒' },
      { key: 'soundEnabled', label: '声音提醒' },
      { key: 'desktopNotificationEnabled', label: '桌面提醒' },
      { key: 'unreadCountEnabled', label: '未读计数' },
    ];
  }
  if (channelType === 'SMS') {
    return [
      { key: 'accessKeyId', label: 'AccessKey' },
      { key: 'accessKeySecret', label: 'Secret' },
      { key: 'signName', label: '短信签名' },
      { key: 'templatePlatform', label: '模板平台' },
      { key: 'endpoint', label: '接入地址' },
      { key: 'callbackUrl', label: '通知地址' },
    ];
  }
  if (channelType === 'EMAIL' && normalizedProvider === 'ALIYUN_DM') {
    return [
      { key: 'accessKeyId', label: 'AccessKey' },
      { key: 'accessKeySecret', label: 'Secret' },
      { key: 'regionId', label: '区域' },
      { key: 'endpoint', label: 'Endpoint' },
      { key: 'accountName', label: '发信地址' },
      { key: 'addressType', label: '地址类型' },
      { key: 'fromAlias', label: '发信别名' },
      { key: 'replyToAddress', label: '回信地址' },
    ];
  }
  if (channelType === 'EMAIL') {
    return [
      { key: 'host', label: 'SMTP' },
      { key: 'port', label: '端口' },
      { key: 'username', label: '账号' },
      { key: 'password', label: '密码' },
      { key: 'from', label: '发件人' },
      { key: 'ssl', label: 'SSL' },
    ];
  }
  if (channelType === 'WECHAT_OFFICIAL') return [{ key: 'appId', label: 'AppId' }, { key: 'appSecret', label: 'Secret' }];
  if (channelType === 'WECOM') {
    return [
      { key: 'corpId', label: '企业ID' },
      { key: 'agentId', label: 'AgentId' },
      { key: 'secret', label: 'Secret' },
      { key: 'webhookUrl', label: 'Webhook' },
    ];
  }
  return [
    { key: 'appKey', label: '应用Key' },
    { key: 'appSecret', label: '应用Secret' },
    { key: 'agentId', label: 'AgentId' },
    { key: 'webhookUrl', label: 'Webhook' },
  ];
}

function displayConfigValue(value: unknown) {
  if (value === true) return '是';
  if (value === false) return '否';
  if (value === undefined || value === null || value === '') return '-';
  return String(value);
}

function fromJson(value?: string): Record<string, unknown> {
  if (!value) return {};
  try {
    const parsed = JSON.parse(value);
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? parsed : {};
  } catch {
    return {};
  }
}

function providerOptions(channelType: NoticeChannelType) {
  return providers[channelType] || [];
}

function compactObject(source: Record<string, ChannelConfigValue>) {
  return Object.fromEntries(Object.entries(source).filter(([, value]) => value !== '' && value !== undefined));
}

function parseConfig(value: string | undefined, target: Record<string, ChannelConfigValue>, defaults: Record<string, ChannelConfigValue>) {
  Object.keys(target).forEach(key => delete target[key]);
  Object.assign(target, defaults);
  if (!value) return;
  try {
    Object.assign(target, JSON.parse(value));
  } catch {
    ElMessage.warning('配置 JSON 解析失败，已使用默认配置');
  }
}

function handleConfigModeChange(tabName: string | number) {
  if (tabName === 'JSON') {
    configJsonText.value = configJsonPreview.value;
    return;
  }
  try {
    parseConfig(configJsonText.value, channelConfig, defaultConfig(form.channelType || 'EMAIL'));
  } catch {
    ElMessage.error('JSON 格式错误，请修正后再切换');
    configEditMode.value = 'JSON';
  }
}

function channelLabel(channel: NoticeChannelType) {
  return channelLabels[channel];
}

function providerLabel(channelType: NoticeChannelType, providerCode?: string) {
  const normalized = normalizeProviderCode(channelType, providerCode);
  return providerOptions(channelType).find(item => item.value === normalized)?.label || providerCode || '-';
}

function normalizeProviderCode(channelType: NoticeChannelType, providerCode?: string) {
  if (channelType === 'EMAIL' && providerCode === 'SMTP') {
    return 'CUSTOM_SMTP';
  }
  return providerCode;
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

function formatJson(value?: string) {
  if (!value) return '{}';
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

function validateForm() {
  if (!form.channelType) {
    ElMessage.warning('请选择渠道类型');
    return false;
  }
  if (!form.providerCode) {
    ElMessage.warning('请选择接入平台');
    return false;
  }
  if (!form.configName?.trim()) {
    ElMessage.warning('请输入通道名称');
    return false;
  }
  return true;
}

function resolveConfigJson() {
  if (configEditMode.value === 'JSON') {
    try {
      const parsed = JSON.parse(configJsonText.value || '{}');
      return JSON.stringify(parsed, null, 2);
    } catch {
      ElMessage.error('渠道参数 JSON 格式错误');
      return undefined;
    }
  }
  return configJsonPreview.value;
}

async function save() {
  if (!validateForm()) return;
  const configJson = resolveConfigJson();
  if (configJson === undefined) return;
  await saveChannelConfig({
    ...form,
    configJson,
    rateLimitConfig: rateLimitJsonPreview.value,
  });
  ElMessage.success('已保存');
  visible.value = false;
  await load();
}

async function removeChannel(row: NoticeChannelConfig) {
  await ElMessageBox.confirm(`确认删除通道「${row.configName}」？`, '删除渠道', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  });
  await deleteChannelConfig(String(row.id));
  ElMessage.success('删除成功');
  await load();
}

onMounted(load);
</script>

<style scoped>
.notice-channel-page {
  padding: 0;
}

.page-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.list-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.notice-filter {
  flex: 1;
}

.notice-filter :deep(.el-form-item) {
  margin-bottom: 0;
}

.filter-control {
  width: 160px;
}

.channel-form {
  max-height: 66vh;
  overflow-y: auto;
  padding-right: 8px;
}

.form-section + .form-section {
  margin-top: 18px;
}

.section-title {
  display: flex;
  align-items: center;
  min-height: 32px;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.form-control,
.number-control {
  width: 100%;
}

.stable-tabs :deep(.el-tabs__content) {
  min-height: 280px;
}

.channel-config-tabs :deep(.el-form-item:last-child) {
  margin-bottom: 18px;
}

.json-editor :deep(.el-textarea__inner) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
}

.table-icon-button {
  width: 24px;
  height: 24px;
  padding: 0;
  margin-left: 6px;
  vertical-align: middle;
}

.detail-section-title {
  margin: 18px 0 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

@media (max-width: 768px) {
  .list-toolbar {
    display: block;
  }

  .list-toolbar > .el-button {
    margin-top: 12px;
  }
}
</style>
