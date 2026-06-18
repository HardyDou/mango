<template>
  <div class="notice-business-config-page">
    <el-card v-if="pageMode === 'LIST'" shadow="never" class="business-main page-card">
      <div class="list-page-header">
        <h1>消息配置</h1>
      </div>
      <div class="definition-layout">
        <DomainSideTree
          v-model="activeDomain"
          title="业务域"
          subtitle="按业务域组织消息定义"
          all-label="全部消息"
          all-code="ALL"
          @change="selectDomain"
          @loaded="handleDomainsLoaded"
        />
        <main class="definition-main">
          <div class="list-toolbar">
            <el-form :inline="true" :model="query" class="notice-filter">
              <el-form-item label="消息编码">
                <el-input v-model="query.bizType" clearable class="filter-control" />
              </el-form-item>
              <el-form-item label="启用状态">
                <el-select v-model="query.enabled" clearable placeholder="全部" class="filter-control">
                  <el-option label="启用" :value="true" />
                  <el-option label="停用" :value="false" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="loadBusinessTypes">查询</el-button>
              </el-form-item>
            </el-form>
            <el-button type="primary" :icon="Plus" @click="openCreate">新增</el-button>
          </div>
          <el-table :data="businessTypes" border stripe v-loading="loading">
            <el-table-column prop="bizName" label="消息名称" min-width="150" />
            <el-table-column prop="bizType" label="消息编码" min-width="180" />
            <el-table-column prop="domainCode" label="业务域" width="110" />
            <el-table-column label="生命周期" width="100">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="同步状态" width="120">
              <template #default="{ row }">
                <el-tooltip :content="row.syncReason || '-'" placement="top">
                  <el-tag :type="row.syncStatus === 'PENDING_PUBLISH' ? 'warning' : 'success'">
                    {{ row.syncStatus === 'PENDING_PUBLISH' ? '待发布' : '已同步' }}
                  </el-tag>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column label="开启渠道" min-width="150">
              <template #default="{ row }">
                <el-tag v-for="item in enabledChannelLabels(row.enabledChannels)" :key="item" class="notice-tag" effect="plain">
                  {{ item }}
                </el-tag>
                <span v-if="enabledChannelLabels(row.enabledChannels).length === 0" class="notice-muted">未发布</span>
              </template>
            </el-table-column>
            <el-table-column label="生效版本" width="100">
              <template #default="{ row }">{{ row.activeVersion ? `V${row.activeVersion}` : '未发布' }}</template>
            </el-table-column>
            <el-table-column prop="lastPublishTime" label="最后发布" width="170" />
            <el-table-column prop="updatedAt" label="更新时间" width="170" />
            <el-table-column label="操作" width="280" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDetail(row)">详情</el-button>
                <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
                <el-button link type="primary" @click="openHistory(row)">历史版本</el-button>
                <el-button link type="success" @click="quickPublish(row)">发布</el-button>
                <el-button link type="danger" @click="removeBusinessType(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </main>
      </div>
    </el-card>

    <el-card v-else-if="pageMode === 'MAINTAIN'" shadow="never" class="business-main page-card">
      <div class="page-header">
        <div>
          <h1>消息配置维护</h1>
          <span>{{ form.bizType || '新增消息配置' }}</span>
        </div>
        <div class="page-actions">
          <el-tag v-if="editingBusinessType" :type="businessConfigStatusTag.type" effect="plain">{{ businessConfigStatusTag.label }}</el-tag>
          <span v-if="editingBusinessType" class="version-summary-inline">当前版本：{{ businessConfigVersionText }}</span>
          <el-button @click="backToList">返回</el-button>
          <el-button @click="saveMaintenance">保存</el-button>
          <el-button type="primary" @click="publishMaintenance">发布新版本</el-button>
        </div>
      </div>

      <el-form class="maintain-form" :model="form" label-width="96px">
        <section class="form-section">
          <div class="section-title">基础信息</div>
          <el-row :gutter="16">
            <el-col :xs="24" :md="12">
              <el-form-item label="业务域" required>
                <el-tree-select
                  v-model="form.domainCode"
                  :data="domainOptions"
                  :props="domainTreeProps"
                  class="form-control"
                  check-strictly
                  clearable
                  filterable
                  node-key="domainCode"
                  placeholder="请选择业务域"
                />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="业务Key" required>
                <el-input v-model="form.bizType" :disabled="!!editingBusinessType" placeholder="order.shipped" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :md="12">
              <el-form-item label="名称" required>
                <el-input v-model="form.bizName" placeholder="出函成功" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="描述">
                <el-input v-model="form.description" type="textarea" :rows="3" placeholder="用于说明该消息配置的业务场景" />
              </el-form-item>
            </el-col>
          </el-row>
        </section>

        <el-row class="maintain-config-row" :gutter="16" align="top">
          <el-col :xs="24" :lg="10" :xl="9">
            <section class="form-section parameter-section">
              <div class="section-title">
                <span>参数设置</span>
                <el-button size="small" type="primary" :icon="Plus" @click="addSchemaField">新增参数</el-button>
              </div>
              <el-tabs v-model="schemaEditMode" class="stable-tabs notice-schema-tabs" @tab-change="handleSchemaModeChange">
                <el-tab-pane label="表单形式" name="FORM">
                  <el-table :data="schemaFields" border size="small" max-height="360" class="schema-field-table">
                    <el-table-column label="参数名" min-width="128">
                      <template #default="{ row }"><el-input v-model="row.name" placeholder="orderNo" /></template>
                    </el-table-column>
                    <el-table-column label="中文名" min-width="118">
                      <template #default="{ row }"><el-input v-model="row.label" placeholder="订单号" /></template>
                    </el-table-column>
                    <el-table-column label="类型" width="108">
                      <template #default="{ row }">
                        <el-select v-model="row.type">
                          <el-option v-for="item in schemaTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
                        </el-select>
                      </template>
                    </el-table-column>
                    <el-table-column label="必填" width="72" align="center">
                      <template #default="{ row }"><el-switch v-model="row.required" /></template>
                    </el-table-column>
                    <el-table-column width="56" align="center">
                      <template #default="{ $index }">
                        <el-tooltip content="删除" placement="top">
                          <el-button
                            class="table-icon-button"
                            text
                            type="danger"
                            :icon="Delete"
                            aria-label="删除参数"
                            @click="removeSchemaField($index)"
                          />
                        </el-tooltip>
                      </template>
                    </el-table-column>
                  </el-table>
                </el-tab-pane>
                <el-tab-pane label="JSON 形式" name="JSON">
                  <el-input
                    v-model="schemaJsonText"
                    class="schema-json-input"
                    type="textarea"
                    :rows="14"
                    placeholder="{ &quot;type&quot;: &quot;object&quot;, &quot;properties&quot;: {} }"
                    spellcheck="false"
                  />
                </el-tab-pane>
              </el-tabs>
            </section>
          </el-col>

          <el-col :xs="24" :lg="14" :xl="15">
            <section class="form-section message-type-section">
              <div class="section-title">
                <span>消息类型</span>
                <span class="notice-muted">启用后必须配置对应消息内容；停用时已有配置不会丢失。</span>
              </div>
              <el-tabs v-model="activeChannel" class="message-type-tabs">
                <el-tab-pane v-for="channel in channels" :key="channel" :label="channelLabel(channel)" :name="channel" />
              </el-tabs>
              <div class="template-config-form">
                <el-row :gutter="16">
                  <el-col :span="24">
                    <el-form-item label="是否启用" class="template-enabled-item">
                      <el-switch
                        v-model="templateForm.enabled"
                        active-text="启用"
                        inactive-text="停用"
                        inline-prompt
                        @change="value => setChannelEnabled(activeChannel, Boolean(value))"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="24">
                    <el-form-item label="通道">
                      <el-select
                        v-model="templateForm.channelConfigId"
                        class="template-route-select"
                        clearable
                        placeholder="AUTO 权重轮换"
                      >
                        <el-option label="AUTO 权重轮换" value="" />
                        <el-option
                          v-for="item in channelConfigOptions(activeChannel)"
                          :key="item.id"
                          :label="item.configName || item.providerCode || item.id"
                          :value="item.id"
                        />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col v-if="showTemplateTitle" :span="24">
                    <el-form-item :label="templateTitleLabel">
                      <el-input
                        v-model="templateForm.titleTemplate"
                        class="template-title-input"
                        :placeholder="templateTitlePlaceholder"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="24">
                    <el-form-item :label="templateContentLabel" class="template-content-item">
                      <Editor
                        v-if="activeChannel === 'EMAIL'"
                        v-model="templateForm.contentTemplate"
                        class="template-editor"
                        height="260px"
                        mode="simple"
                        :placeholder="templateContentPlaceholder"
                      />
                      <el-input
                        v-else
                        v-model="templateForm.contentTemplate"
                        class="template-content-textarea"
                        type="textarea"
                        :rows="8"
                        :placeholder="templateContentPlaceholder"
                      />
                    </el-form-item>
                  </el-col>
                </el-row>
                <div class="param-variable-panel">
                  <span class="notice-muted">参数变量</span>
                  <el-tag
                    v-for="item in templateParamOptions"
                    :key="item.name"
                    class="param-variable-tag"
                    effect="plain"
                    @click="copyVariable(item.name)"
                  >
                    {{ item.label }}：{{ variableText(item.name) }}
                  </el-tag>
                  <span v-if="templateParamOptions.length === 0" class="notice-muted">暂无参数</span>
                </div>
              </div>
            </section>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card v-else-if="pageMode === 'DETAIL'" shadow="never" class="business-main page-card">
      <div class="page-header">
        <div>
          <h1>消息配置详情</h1>
          <span>{{ currentBusinessType?.bizType || '-' }}</span>
        </div>
        <div class="page-actions">
          <el-button @click="backToList">返回</el-button>
          <el-button v-if="currentBusinessType" type="primary" @click="openEdit(currentBusinessType)">编辑</el-button>
        </div>
      </div>

      <div class="readonly-definition-view">
        <section class="form-section">
          <div class="section-title">基础信息</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="业务域">{{ currentBusinessType?.domainCode || currentBusinessType?.bizGroup || '-' }}</el-descriptions-item>
            <el-descriptions-item label="业务Key">{{ currentBusinessType?.bizType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="名称">{{ currentBusinessType?.bizName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="生命周期">
              <el-tag :type="currentBusinessType?.enabled ? 'success' : 'info'">
                {{ currentBusinessType?.enabled ? '启用' : '停用' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="同步状态">
              <el-tag :type="currentBusinessType?.syncStatus === 'PENDING_PUBLISH' ? 'warning' : 'success'">
                {{ currentBusinessType?.syncStatus === 'PENDING_PUBLISH' ? '待发布' : '已同步' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="生效版本">
              {{ currentBusinessType?.activeVersion ? `V${currentBusinessType.activeVersion}` : '未发布' }}
            </el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ currentBusinessType?.description || '-' }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <el-row class="maintain-config-row readonly-config-row" :gutter="16" align="top">
          <el-col :xs="24" :lg="10" :xl="9">
            <section class="form-section parameter-section">
              <div class="section-title">
                <span>参数设置</span>
                <el-tag size="small" effect="plain">{{ schemaFieldCount }} 个</el-tag>
              </div>
              <el-table :data="schemaFields" border size="small" max-height="360" class="schema-field-table">
                <el-table-column prop="name" label="参数名" min-width="128" />
                <el-table-column prop="label" label="中文名" min-width="118" />
                <el-table-column label="类型" width="108">
                  <template #default="{ row }">{{ schemaTypeLabel(row.type) }}</template>
                </el-table-column>
                <el-table-column label="必填" width="72" align="center">
                  <template #default="{ row }">
                    <el-tag size="small" :type="row.required ? 'warning' : 'info'" effect="plain">
                      {{ row.required ? '是' : '否' }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </section>
          </el-col>

          <el-col :xs="24" :lg="14" :xl="15">
            <section class="form-section message-type-section">
              <div class="section-title">
                <span>消息类型</span>
                <span class="notice-muted">只读查看各渠道模板配置</span>
              </div>
              <el-tabs v-model="activeChannel" class="message-type-tabs">
                <el-tab-pane v-for="channel in channels" :key="channel" :label="channelLabel(channel)" :name="channel" />
              </el-tabs>
              <el-descriptions :column="2" border class="template-readonly-descriptions">
                <el-descriptions-item label="是否启用">
                  <el-tag :type="templateForm.enabled ? 'success' : 'info'" effect="plain">
                    {{ templateForm.enabled ? '启用' : '停用' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="通道">{{ channelRouteText(templateForm.channelConfigId) }}</el-descriptions-item>
                <el-descriptions-item v-if="showTemplateTitle" :label="templateTitleLabel" :span="2">
                  {{ templateForm.titleTemplate || '-' }}
                </el-descriptions-item>
                <el-descriptions-item :label="templateContentLabel" :span="2">
                  <div class="readonly-content">{{ templateForm.contentTemplate || '-' }}</div>
                </el-descriptions-item>
              </el-descriptions>
            </section>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <el-card v-else-if="pageMode === 'HISTORY'" shadow="never" class="business-main page-card">
      <div class="page-header">
        <div>
          <h1>历史版本</h1>
          <span>{{ currentBusinessType?.bizType || '-' }}</span>
        </div>
        <div class="page-actions">
          <el-button @click="backToList">返回</el-button>
          <el-button v-if="currentBusinessType" type="primary" @click="openEdit(currentBusinessType)">编辑</el-button>
        </div>
      </div>

      <div class="history-layout">
        <aside class="history-list">
          <div class="section-title">版本列表</div>
          <button
            v-for="item in businessConfigVersions"
            :key="item.id"
            class="history-version-item"
            :class="{ active: selectedHistoryVersion?.id === item.id }"
            type="button"
            @click="selectHistoryVersion(item)"
          >
            <span class="history-version-head">
              <strong>V{{ item.version }}</strong>
              <el-tag :type="versionStatusTag(item.versionStatus).type" size="small" effect="plain">
                {{ versionStatusTag(item.versionStatus).label }}
              </el-tag>
            </span>
            <span>{{ item.publishTime || '未发布' }}</span>
          </button>
          <el-empty v-if="businessConfigVersions.length === 0" :image-size="64" description="暂无历史版本" />
        </aside>
        <main class="history-detail">
          <div class="history-detail-header">
            <div class="section-title">版本详情</div>
            <el-button
              v-if="selectedHistoryVersion && selectedHistoryVersion.versionStatus !== 'ACTIVE'"
              type="success"
              size="small"
              @click="activateHistoryVersion"
            >
              启用
            </el-button>
          </div>
          <div class="readonly-definition-view">
            <section class="form-section">
              <div class="section-title">基础信息</div>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="业务域">{{ currentBusinessType?.domainCode || currentBusinessType?.bizGroup || '-' }}</el-descriptions-item>
                <el-descriptions-item label="业务Key">{{ currentBusinessType?.bizType || '-' }}</el-descriptions-item>
                <el-descriptions-item label="名称">{{ currentBusinessType?.bizName || '-' }}</el-descriptions-item>
                <el-descriptions-item label="查看版本">
                  {{ selectedHistoryVersion ? `V${selectedHistoryVersion.version}` : '-' }}
                </el-descriptions-item>
                <el-descriptions-item label="版本状态">
                  <el-tag :type="versionStatusTag(selectedHistoryVersion?.versionStatus).type" effect="plain">
                    {{ versionStatusTag(selectedHistoryVersion?.versionStatus).label }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="发布时间">{{ selectedHistoryVersion?.publishTime || '-' }}</el-descriptions-item>
                <el-descriptions-item label="描述" :span="2">{{ currentBusinessType?.description || '-' }}</el-descriptions-item>
              </el-descriptions>
            </section>

            <el-row class="maintain-config-row readonly-config-row" :gutter="16" align="top">
              <el-col :xs="24" :lg="10" :xl="9">
                <section class="form-section parameter-section">
                  <div class="section-title">
                    <span>参数设置</span>
                    <el-tag size="small" effect="plain">{{ schemaFieldCount }} 个</el-tag>
                  </div>
                  <el-table :data="schemaFields" border size="small" max-height="360" class="schema-field-table">
                    <el-table-column prop="name" label="参数名" min-width="128" />
                    <el-table-column prop="label" label="中文名" min-width="118" />
                    <el-table-column label="类型" width="108">
                      <template #default="{ row }">{{ schemaTypeLabel(row.type) }}</template>
                    </el-table-column>
                    <el-table-column label="必填" width="72" align="center">
                      <template #default="{ row }">
                        <el-tag size="small" :type="row.required ? 'warning' : 'info'" effect="plain">
                          {{ row.required ? '是' : '否' }}
                        </el-tag>
                      </template>
                    </el-table-column>
                  </el-table>
                </section>
              </el-col>

              <el-col :xs="24" :lg="14" :xl="15">
                <section class="form-section message-type-section">
                  <div class="section-title">
                    <span>消息类型</span>
                    <span class="notice-muted">只读查看当前版本的渠道模板配置</span>
                  </div>
                  <el-tabs v-model="activeChannel" class="message-type-tabs">
                    <el-tab-pane v-for="channel in channels" :key="channel" :label="channelLabel(channel)" :name="channel" />
                  </el-tabs>
                  <el-descriptions :column="2" border class="template-readonly-descriptions">
                    <el-descriptions-item label="是否启用">
                      <el-tag :type="templateForm.enabled ? 'success' : 'info'" effect="plain">
                        {{ templateForm.enabled ? '启用' : '停用' }}
                      </el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item label="通道">{{ channelRouteText(templateForm.channelConfigId) }}</el-descriptions-item>
                    <el-descriptions-item v-if="showTemplateTitle" :label="templateTitleLabel" :span="2">
                      {{ templateForm.titleTemplate || '-' }}
                    </el-descriptions-item>
                    <el-descriptions-item :label="templateContentLabel" :span="2">
                      <div class="readonly-content">{{ templateForm.contentTemplate || '-' }}</div>
                    </el-descriptions-item>
                  </el-descriptions>
                </section>
              </el-col>
            </el-row>
          </div>
        </main>
      </div>
    </el-card>

  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Delete, Plus } from '@element-plus/icons-vue';
import { Editor } from '@mango/common';
import { DomainSideTree } from '@mango/system';
import {
  activateBusinessConfigVersion,
  createBusinessType,
  deleteBusinessType,
  getBusinessConfigVersions,
  getBusinessTypes,
  getChannelConfigs,
  getChannelTemplates,
  getNoticeDomains,
  publishBusinessConfigDraft,
  publishChannelTemplate,
  saveBusinessConfigDraft,
  saveChannelTemplate,
  updateBusinessType,
} from '../../api/notice';
import type {
  NoticeBusinessConfigVersion,
  NoticeBusinessType,
  NoticeDomainOption,
  NoticeChannelConfig,
  NoticeChannelTemplate,
  NoticeChannelType,
  NoticePriority,
  NoticeTemplateVersionStatus,
} from '../../types/notice';

type SchemaFieldType = 'string' | 'number' | 'boolean' | 'datetime';
type SchemaEditMode = 'FORM' | 'JSON';
type PageMode = 'LIST' | 'MAINTAIN' | 'DETAIL' | 'HISTORY';

interface SchemaField {
  name: string;
  label: string;
  type: SchemaFieldType;
  required: boolean;
}

interface TemplateMappingRow {
  paramName: string;
  targetName: string;
}

interface JsonSchemaProperty {
  type: string;
  title?: string;
  description?: string;
  format?: string;
}

interface JsonSchema {
  type: 'object';
  properties: Record<string, JsonSchemaProperty>;
  required?: string[];
}

interface LegacyParamSchema {
  params?: Array<{
    name?: unknown;
    label?: unknown;
    description?: unknown;
    type?: unknown;
    required?: unknown;
  }>;
}

interface BusinessTypeForm {
  bizType: string;
  bizName: string;
  bizGroup: string;
  domainCode: string;
  description: string;
  paramsSchema: string;
  defaultPriority: NoticePriority;
  idempotentStrategy: string;
}

const channels: NoticeChannelType[] = ['SITE', 'SMS', 'EMAIL', 'WECHAT_OFFICIAL', 'WECOM', 'DINGTALK'];
const channelLabels: Record<NoticeChannelType, string> = {
  SITE: '系统消息',
  SMS: '短信',
  EMAIL: '邮件',
  WECHAT_OFFICIAL: '微信公众号',
  WECOM: '企业微信',
  DINGTALK: '钉钉',
};
const schemaTypeOptions: Array<{ label: string; value: SchemaFieldType }> = [
  { label: '文本', value: 'string' },
  { label: '数字', value: 'number' },
  { label: '布尔', value: 'boolean' },
  { label: '日期时间', value: 'datetime' },
];

const loading = ref(false);
const pageMode = ref<PageMode>('LIST');
const businessTypes = ref<NoticeBusinessType[]>([]);
const domainOptions = ref<NoticeDomainOption[]>([]);
const channelConfigs = ref<NoticeChannelConfig[]>([]);
const currentBusinessType = ref<NoticeBusinessType>();
const editingBusinessType = ref<NoticeBusinessType>();
const activeChannel = ref<NoticeChannelType>('SITE');
const templates = ref<NoticeChannelTemplate[]>([]);
const businessConfigVersions = ref<NoticeBusinessConfigVersion[]>([]);
const selectedHistoryVersion = ref<NoticeBusinessConfigVersion>();
const form = reactive<BusinessTypeForm>(createBusinessTypeForm());
const schemaFields = ref<SchemaField[]>([]);
const schemaEditMode = ref<SchemaEditMode>('FORM');
const schemaJsonText = ref('');
const templateForm = reactive<Partial<NoticeChannelTemplate>>({ enabled: true });
const templateMappingRows = ref<TemplateMappingRow[]>([]);
const channelDrafts = reactive<Record<NoticeChannelType, Partial<NoticeChannelTemplate>>>({} as Record<NoticeChannelType, Partial<NoticeChannelTemplate>>);
const channelMappingDrafts = reactive<Record<NoticeChannelType, TemplateMappingRow[]>>({} as Record<NoticeChannelType, TemplateMappingRow[]>);
const query = reactive<{ bizType?: string; enabled?: boolean }>({});
const activeDomain = ref('');
const domainTreeProps = {
  label: 'domainName',
  value: 'domainCode',
  children: 'children',
};

const paramsSchemaJson = computed(() => JSON.stringify(buildParamsSchema(), null, 2));
const schemaFieldCount = computed(() => schemaFields.value.filter(item => item.name.trim()).length);
const templateParamOptions = computed(() => parseSchemaFields(resolveParamsSchemaForOptions()).filter(item => item.name.trim()));
const activeBusinessConfig = computed(() => businessConfigVersions.value.find(item => item.versionStatus === 'ACTIVE'));
const draftBusinessConfig = computed(() => businessConfigVersions.value.find(item => item.versionStatus === 'DRAFT'));
const businessConfigForForm = computed(() => draftBusinessConfig.value || activeBusinessConfig.value);
const businessConfigStatusTag = computed(() => versionStatusTag(draftBusinessConfig.value ? 'DRAFT' : activeBusinessConfig.value?.versionStatus));
const businessConfigVersionText = computed(() => {
  const version = businessConfigForForm.value;
  return version ? `V${version.version}` : '未发布';
});
const currentTemplate = computed(() => {
  const version = pageMode.value === 'HISTORY' ? selectedHistoryVersion.value?.version : undefined;
  if (version) {
    return templates.value.find(item => item.channelType === activeChannel.value && item.version === version)
      || templates.value.find(item => item.channelType === activeChannel.value && item.versionStatus === 'ACTIVE')
      || templates.value.find(item => item.channelType === activeChannel.value);
  }
  return templates.value.find(item => item.channelType === activeChannel.value && item.versionStatus === 'DRAFT')
    || templates.value.find(item => item.channelType === activeChannel.value && item.versionStatus === 'ACTIVE')
    || templates.value.find(item => item.channelType === activeChannel.value);
});
const showTemplateTitle = computed(() => activeChannel.value !== 'SMS');
const templateTitleLabel = computed(() => {
  if (activeChannel.value === 'SITE') {
    return '系统消息标题';
  }
  if (activeChannel.value === 'EMAIL') {
    return '邮件标题';
  }
  return `${channelLabel(activeChannel.value)}标题`;
});
const templateContentLabel = computed(() => {
  if (activeChannel.value === 'SITE') {
    return '系统消息内容';
  }
  if (activeChannel.value === 'SMS') {
    return '短信内容';
  }
  if (activeChannel.value === 'EMAIL') {
    return '邮件内容';
  }
  return `${channelLabel(activeChannel.value)}内容`;
});
const templateTitlePlaceholder = computed(() => {
  if (activeChannel.value === 'SITE') {
    return '请输入系统消息标题，例如：订单 {{orderNo}} 已发货';
  }
  if (activeChannel.value === 'EMAIL') {
    return '请输入邮件标题，例如：订单 {{orderNo}} 已发货';
  }
  return `请输入${channelLabel(activeChannel.value)}标题，例如：订单 {{orderNo}} 已发货`;
});
const templateContentPlaceholder = computed(() => {
  if (activeChannel.value === 'SITE') {
    return '请输入系统消息内容，支持变量 {{orderNo}}';
  }
  if (activeChannel.value === 'WECHAT_OFFICIAL') {
    return '请输入微信公众号内容，例如：您的订单 {{orderNo}} 已发货';
  }
  if (activeChannel.value === 'SMS') {
    return '请输入短信内容，例如：订单 {{orderNo}} 已发货';
  }
  if (activeChannel.value === 'EMAIL') {
    return '请输入邮件内容，支持变量 {{orderNo}}';
  }
  return '请输入通知内容，支持变量 {{orderNo}}';
});
async function loadBusinessTypes() {
  loading.value = true;
  try {
    const result = await getBusinessTypes({
      bizType: query.bizType,
      enabled: query.enabled,
      domainCode: activeDomain.value || undefined,
    });
    businessTypes.value = result.list || [];
  } finally {
    loading.value = false;
  }
}

async function loadChannelConfigs() {
  const result = await getChannelConfigs({ enabled: true, pageSize: 200 });
  channelConfigs.value = result.list || [];
}

async function loadDomainOptions() {
  domainOptions.value = await getNoticeDomains();
}

function handleDomainsLoaded(domains: NoticeDomainOption[]) {
  domainOptions.value = domains;
}

function flattenDomainOptions(options: NoticeDomainOption[]): NoticeDomainOption[] {
  return options.flatMap(item => [item, ...flattenDomainOptions(item.children || [])]);
}

function openCreate() {
  pageMode.value = 'MAINTAIN';
  editingBusinessType.value = undefined;
  currentBusinessType.value = undefined;
  selectedHistoryVersion.value = undefined;
  businessConfigVersions.value = [];
  templates.value = [];
  resetChannelDrafts();
  resetBusinessForm();
  schemaFields.value = [createSchemaField()];
  schemaEditMode.value = 'FORM';
  schemaJsonText.value = paramsSchemaJson.value;
  activeChannel.value = 'SITE';
  applyTemplate();
}

async function openEdit(row: NoticeBusinessType) {
  pageMode.value = 'MAINTAIN';
  editingBusinessType.value = row;
  currentBusinessType.value = row;
  selectedHistoryVersion.value = undefined;
  resetChannelDrafts();
  Object.assign(form, baseFormFromBusiness(row));
  await loadPublishConfig(row);
  applyBusinessConfigToForm();
}

function backToList() {
  pageMode.value = 'LIST';
  currentBusinessType.value = undefined;
  editingBusinessType.value = undefined;
  selectedHistoryVersion.value = undefined;
  resetChannelDrafts();
}

async function openDetail(row: NoticeBusinessType) {
  pageMode.value = 'DETAIL';
  editingBusinessType.value = row;
  currentBusinessType.value = row;
  selectedHistoryVersion.value = undefined;
  resetChannelDrafts();
  Object.assign(form, baseFormFromBusiness(row));
  await loadPublishConfig(row);
  applyBusinessConfigToForm();
}

async function openHistory(row: NoticeBusinessType) {
  pageMode.value = 'HISTORY';
  editingBusinessType.value = row;
  currentBusinessType.value = row;
  resetChannelDrafts();
  Object.assign(form, baseFormFromBusiness(row));
  await loadPublishConfig(row);
  selectedHistoryVersion.value = activeBusinessConfig.value || businessConfigVersions.value[0];
  applyHistoryVersionToForm();
}

async function activateHistoryVersion() {
  if (!currentBusinessType.value || !selectedHistoryVersion.value) {
    return;
  }
  await activateBusinessConfigVersion(currentBusinessType.value.id, selectedHistoryVersion.value.version);
  ElMessage.success('已启用历史版本并生成新版本');
  await loadPublishConfig(currentBusinessType.value);
  selectedHistoryVersion.value = activeBusinessConfig.value || businessConfigVersions.value[0];
  applyHistoryVersionToForm();
  await loadBusinessTypes();
}

function selectHistoryVersion(version: NoticeBusinessConfigVersion) {
  selectedHistoryVersion.value = version;
  applyHistoryVersionToForm();
}

function baseFormFromBusiness(row: NoticeBusinessType): BusinessTypeForm {
  return {
    bizType: row.bizType,
    bizName: row.bizName,
    bizGroup: row.bizGroup || row.domainCode || '',
    domainCode: row.domainCode || row.bizGroup || 'NOTICE',
    description: row.description || '',
    paramsSchema: row.paramsSchema || '',
    defaultPriority: row.defaultPriority || 'NORMAL',
    idempotentStrategy: row.idempotentStrategy || '',
  };
}

async function loadPublishConfig(row: NoticeBusinessType) {
  const [versions, channelTemplates, channelConfigResult] = await Promise.all([
    getBusinessConfigVersions(row.id),
    getChannelTemplates(row.id),
    getChannelConfigs({ enabled: true, pageSize: 200 }),
  ]);
  businessConfigVersions.value = versions || [];
  templates.value = channelTemplates || [];
  channelConfigs.value = channelConfigResult.list || [];
  activeChannel.value = 'SITE';
  applyTemplate();
}

function applyBusinessConfigToForm() {
  const config = businessConfigForForm.value;
  if (config) {
    form.paramsSchema = resolveBusinessConfigParamsSchema(config);
    form.defaultPriority = config.defaultPriority || 'NORMAL';
    form.idempotentStrategy = config.idempotentStrategy || '';
  }
  const parsedFields = parseSchemaFields(form.paramsSchema);
  schemaFields.value = parsedFields.length > 0 ? parsedFields : [createSchemaField()];
  schemaEditMode.value = 'FORM';
  schemaJsonText.value = formatSchemaJson(form.paramsSchema);
  applyTemplate();
}

function applyHistoryVersionToForm() {
  const config = selectedHistoryVersion.value;
  if (!config) {
    return;
  }
  form.paramsSchema = config.paramsSchema || '';
  form.defaultPriority = config.defaultPriority || 'NORMAL';
  form.idempotentStrategy = config.idempotentStrategy || '';
  const parsedFields = parseSchemaFields(form.paramsSchema);
  schemaFields.value = parsedFields.length > 0 ? parsedFields : [];
  schemaEditMode.value = 'FORM';
  schemaJsonText.value = formatSchemaJson(form.paramsSchema);
  applyTemplate();
}

function createBusinessTypeForm(): BusinessTypeForm {
  return {
    bizType: '',
    bizName: '',
    bizGroup: '',
    domainCode: 'NOTICE',
    description: '',
    paramsSchema: '',
    defaultPriority: 'NORMAL',
    idempotentStrategy: '',
  };
}

function resetBusinessForm() {
  Object.assign(form, createBusinessTypeForm());
}

function createSchemaField(): SchemaField {
  return { name: '', label: '', type: 'string', required: false };
}

function addSchemaField() {
  schemaFields.value.push(createSchemaField());
}

function removeSchemaField(index: number) {
  schemaFields.value.splice(index, 1);
}

function buildParamsSchema(): JsonSchema {
  const properties: Record<string, JsonSchemaProperty> = {};
  const required: string[] = [];
  schemaFields.value
    .filter(item => item.name.trim())
    .forEach(item => {
      const property: JsonSchemaProperty = {
        type: item.type === 'datetime' ? 'string' : item.type,
        title: item.label.trim() || item.name.trim(),
      };
      if (item.type === 'datetime') {
        property.format = 'date-time';
      }
      properties[item.name.trim()] = property;
      if (item.required) {
        required.push(item.name.trim());
      }
    });
  return required.length ? { type: 'object', properties, required } : { type: 'object', properties };
}

function formatSchemaJson(value?: string) {
  const schema = parseSchemaJson(value);
  if (schema) {
    return JSON.stringify(schema, null, 2);
  }
  if (value?.trim()) {
    try {
      return JSON.stringify(JSON.parse(value) as unknown, null, 2);
    } catch {
      return value;
    }
  }
  return JSON.stringify(buildParamsSchema(), null, 2);
}

function parseSchemaJson(value?: string) {
  if (!value?.trim()) {
    return { type: 'object', properties: {} } satisfies JsonSchema;
  }
  try {
    const parsed = JSON.parse(value) as unknown;
    const legacySchema = normalizeLegacyParamSchema(parsed as LegacyParamSchema);
    if (legacySchema) {
      return legacySchema;
    }
    if (!isJsonSchema(parsed)) {
      return undefined;
    }
    return parsed;
  } catch {
    return undefined;
  }
}

function normalizeLegacyParamSchema(schema: LegacyParamSchema): JsonSchema | undefined {
  if (!Array.isArray(schema.params)) {
    return undefined;
  }
  const properties: Record<string, JsonSchemaProperty> = {};
  const required: string[] = [];
  schema.params.forEach((item) => {
    if (!isJsonObject(item) || typeof item.name !== 'string' || !item.name.trim()) {
      return;
    }
    const name = item.name.trim();
    properties[name] = {
      type: typeof item.type === 'string' ? item.type : 'string',
      title: typeof item.label === 'string' ? item.label : name,
      description: typeof item.description === 'string' ? item.description : undefined,
    };
    if (item.required === true) {
      required.push(name);
    }
  });
  return { type: 'object', properties, required };
}

function isJsonObject(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}

function isJsonSchema(value: unknown): value is JsonSchema {
  if (!isJsonObject(value) || value.type !== 'object' || !isJsonObject(value.properties)) {
    return false;
  }
  if (value.required !== undefined && !Array.isArray(value.required)) {
    return false;
  }
  return Object.values(value.properties).every(property => isJsonObject(property) && typeof property.type === 'string');
}

function parseSchemaFields(value?: string): SchemaField[] {
  const schema = parseSchemaJson(value);
  if (!schema) {
    return [];
  }
  const requiredFields = new Set(schema.required || []);
  return Object.entries(schema.properties).map(([name, property]) => ({
    name,
    label: property.title || name,
    type: toSchemaFieldType(property),
    required: requiredFields.has(name),
  }));
}

function hasSchemaProperties(value?: string) {
  return parseSchemaFields(value).some(item => item.name.trim());
}

function resolveBusinessConfigParamsSchema(config: NoticeBusinessConfigVersion) {
  if (hasSchemaProperties(config.paramsSchema)) {
    return config.paramsSchema || '';
  }
  if (config.versionStatus === 'DRAFT' && hasSchemaProperties(activeBusinessConfig.value?.paramsSchema)) {
    return activeBusinessConfig.value?.paramsSchema || '';
  }
  if (hasSchemaProperties(currentBusinessType.value?.paramsSchema)) {
    return currentBusinessType.value?.paramsSchema || '';
  }
  return config.paramsSchema || '';
}

function toSchemaFieldType(property: JsonSchemaProperty): SchemaFieldType {
  if (property.type === 'number' || property.type === 'integer') {
    return 'number';
  }
  if (property.type === 'boolean') {
    return 'boolean';
  }
  if (property.type === 'string' && property.format === 'date-time') {
    return 'datetime';
  }
  return 'string';
}

function schemaTypeLabel(type: SchemaFieldType) {
  return schemaTypeOptions.find(item => item.value === type)?.label || type;
}

function applyJsonSchemaToForm() {
  const schema = parseSchemaJson(schemaJsonText.value);
  if (!schema) {
    ElMessage.error('参数 JSON 必须是对象 schema，且包含 properties');
    schemaEditMode.value = 'JSON';
    return false;
  }
  schemaFields.value = parseSchemaFields(JSON.stringify(schema));
  if (schemaFields.value.length === 0) {
    schemaFields.value = [createSchemaField()];
  }
  schemaJsonText.value = JSON.stringify(schema, null, 2);
  return true;
}

function handleSchemaModeChange(mode: string | number) {
  if (mode === 'JSON') {
    schemaJsonText.value = paramsSchemaJson.value;
    return;
  }
  applyJsonSchemaToForm();
}

function resolveParamsSchema() {
  if (schemaEditMode.value === 'FORM') {
    return paramsSchemaJson.value;
  }
  const schema = parseSchemaJson(schemaJsonText.value);
  if (!schema) {
    ElMessage.error('参数 JSON 格式不正确');
    return undefined;
  }
  schemaFields.value = parseSchemaFields(JSON.stringify(schema));
  schemaJsonText.value = JSON.stringify(schema, null, 2);
  return schemaJsonText.value;
}

function channelLabel(channel: NoticeChannelType) {
  return channelLabels[channel];
}

function enabledChannelLabels(value?: string) {
  if (!value) return [];
  return value.split(',').filter(Boolean).map(item => channelLabels[item as NoticeChannelType] || item);
}

function channelConfigOptions(channelType: NoticeChannelType) {
  return channelConfigs.value.filter(item => item.channelType === channelType && item.enabled && item.configStatus === 'COMPLETE');
}

function channelRouteText(channelConfigId?: string) {
  if (!channelConfigId) {
    return 'AUTO 权重轮换';
  }
  const config = channelConfigs.value.find(item => item.id === channelConfigId);
  return config?.configName || config?.providerCode || channelConfigId;
}

function setChannelEnabled(channel: NoticeChannelType, enabled: boolean) {
  if (activeChannel.value !== channel) {
    snapshotTemplateForm(activeChannel.value);
    activeChannel.value = channel;
  }
  templateForm.enabled = enabled;
  snapshotTemplateForm(channel);
}

function validateEnabledTemplates() {
  for (const channel of channels) {
    const draft = channelDrafts[channel] || templates.value.find(item => item.channelType === channel) || createEmptyTemplate(channel);
    if (draft.enabled && !draft.contentTemplate?.trim()) {
      ElMessage.error(`${channelLabel(channel)}已启用，请填写${channelContentLabel(channel)}`);
      activeChannel.value = channel;
      applyTemplate();
      return false;
    }
  }
  return true;
}

function variableText(name: string) {
  return `{{${name}}}`;
}

function channelContentLabel(channel: NoticeChannelType) {
  if (channel === 'SITE') {
    return '系统消息内容';
  }
  if (channel === 'SMS') {
    return '短信内容';
  }
  if (channel === 'EMAIL') {
    return '邮件内容';
  }
  return `${channelLabel(channel)}内容`;
}

async function copyVariable(name: string) {
  const text = variableText(name);
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text);
    ElMessage.success('已复制变量');
  }
}

function selectDomain() {
  loadBusinessTypes();
}

async function quickPublish(row: NoticeBusinessType) {
  if (row.syncStatus !== 'PENDING_PUBLISH') {
    ElMessage.info('当前消息配置已同步，无需发布');
    return;
  }
  await publishBusinessConfigDraft(row.id);
  const templatesToPublish = await getChannelTemplates(row.id);
  await Promise.all(templatesToPublish
    .filter(item => item.versionStatus === 'DRAFT')
    .map(item => publishChannelTemplate(row.id, item.channelType)));
  ElMessage.success('已发布');
  await loadBusinessTypes();
}

async function removeBusinessType(row: NoticeBusinessType) {
  try {
    await ElMessageBox.confirm(`确认删除消息配置「${row.bizName || row.bizType}」？删除后将移除参数、版本和渠道模板配置。`, '删除消息配置', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }
  try {
    await deleteBusinessType(row.id);
    ElMessage.success('删除成功');
    await loadBusinessTypes();
  } catch {
    // request 层已经展示接口错误，这里避免再触发 Vue 全局系统错误。
  }
}

async function saveMaintenance() {
  if (!validateBaseForm()) {
    return;
  }
  try {
    let businessType = editingBusinessType.value;
    if (!businessType) {
      businessType = await createBusinessType({
        bizType: form.bizType.trim(),
        bizName: form.bizName.trim(),
        bizGroup: form.domainCode.trim(),
        domainCode: form.domainCode.trim(),
        description: form.description.trim(),
      });
      editingBusinessType.value = businessType;
      currentBusinessType.value = businessType;
    } else {
      businessType = await updateBusinessType(businessType.id, {
        bizName: form.bizName.trim(),
        bizGroup: form.domainCode.trim(),
        domainCode: form.domainCode.trim(),
        description: form.description.trim(),
      });
      editingBusinessType.value = businessType;
      currentBusinessType.value = businessType;
    }
    const saved = await persistPublishDraft(false);
    if (!saved) {
      return;
    }
    ElMessage.success('已保存');
    await loadBusinessTypes();
  } catch {
    // request 层已经展示接口错误，这里避免叠加 Vue 全局系统异常。
  }
}

async function publishMaintenance() {
  const savedBase = await saveBaseIfNeeded();
  if (!savedBase) {
    return;
  }
  await publishBusinessTemplate();
}

async function saveBaseIfNeeded() {
  if (!validateBaseForm()) {
    return false;
  }
  try {
    if (!editingBusinessType.value) {
      const created = await createBusinessType({
        bizType: form.bizType.trim(),
        bizName: form.bizName.trim(),
        bizGroup: form.domainCode.trim(),
        domainCode: form.domainCode.trim(),
        description: form.description.trim(),
      });
      editingBusinessType.value = created;
      currentBusinessType.value = created;
      return true;
    }
    const updated = await updateBusinessType(editingBusinessType.value.id, {
      bizName: form.bizName.trim(),
      bizGroup: form.domainCode.trim(),
      domainCode: form.domainCode.trim(),
      description: form.description.trim(),
    });
    editingBusinessType.value = updated;
    currentBusinessType.value = updated;
    return true;
  } catch {
    // request 层已经展示接口错误，这里返回失败阻止后续发布。
    return false;
  }
}

function validateBaseForm() {
  if (!form.domainCode.trim()) {
    ElMessage.error('请选择业务域');
    return false;
  }
  if (!form.bizType.trim()) {
    ElMessage.error('请输入业务Key');
    return false;
  }
  if (!form.bizName.trim()) {
    ElMessage.error('请输入名称');
    return false;
  }
  return true;
}

function applyTemplate() {
  const channel = activeChannel.value;
  const draft = channelDrafts[channel] || currentTemplate.value || createEmptyTemplate(channel);
  Object.assign(templateForm, draft);
  templateMappingRows.value = cloneMappingRows(channelMappingDrafts[channel] || parseVariableMapping(templateForm.variableMapping));
}

function snapshotTemplateForm(channel = activeChannel.value) {
  if (pageMode.value !== 'MAINTAIN') {
    return;
  }
  const existing = channelDrafts[channel] || currentTemplate.value || createEmptyTemplate(channel);
  channelDrafts[channel] = {
    ...existing,
    ...templateForm,
    channelType: channel,
    channelConfigId: templateForm.channelConfigId || undefined,
    channelTemplateId: '',
    variableMapping: '',
  };
  channelMappingDrafts[channel] = cloneMappingRows(templateMappingRows.value);
}

async function persistPublishDraft(showSuccess: boolean) {
  const businessType = currentBusinessType.value || editingBusinessType.value;
  if (!businessType) {
    ElMessage.error('请先保存基础信息');
    return false;
  }
  snapshotTemplateForm();
  const paramsSchema = resolveParamsSchema();
  if (!paramsSchema) {
    return false;
  }
  if (!validateEnabledTemplates()) {
    return false;
  }
  await saveBusinessConfigDraft(businessType.id, {
    paramsSchema,
    defaultPriority: form.defaultPriority,
    idempotentStrategy: form.idempotentStrategy.trim(),
  });
  for (const channel of channels) {
    const draft = channelDrafts[channel];
    if (hasTemplateDraft(draft)) {
      await saveChannelTemplate(businessType.id, channel, draft);
    }
  }
  await refreshPublishData(businessType);
  if (showSuccess) {
    ElMessage.success('已保存草稿');
  }
  return true;
}

async function publishBusinessTemplate() {
  const businessType = currentBusinessType.value || editingBusinessType.value;
  if (!businessType) {
    ElMessage.error('请先保存基础信息');
    return;
  }
  const saved = await persistPublishDraft(false);
  if (!saved) {
    return;
  }
  await publishBusinessConfigDraft(businessType.id);
  const draftChannels = channels.filter(channel => templates.value.some(item => item.channelType === channel && item.versionStatus === 'DRAFT'));
  for (const channel of draftChannels) {
    await publishChannelTemplate(businessType.id, channel);
  }
  await refreshPublishData(businessType);
  await loadBusinessTypes();
  ElMessage.success('已发布新版本');
}

async function refreshPublishData(businessType: NoticeBusinessType) {
  await loadPublishConfig(businessType);
  applyBusinessConfigToForm();
  resetChannelDrafts();
}

function createEmptyTemplate(channel: NoticeChannelType): Partial<NoticeChannelTemplate> {
  return {
    channelType: channel,
    enabled: false,
    templateName: '',
    titleTemplate: '',
    contentTemplate: '',
    channelTemplateId: '',
    variableMapping: '',
  };
}

function hasTemplateDraft(draft?: Partial<NoticeChannelTemplate>) {
  if (!draft) {
    return false;
  }
  return Boolean(
    draft.titleTemplate?.trim()
    || draft.contentTemplate?.trim()
    || draft.channelConfigId
    || draft.enabled === true
    || draft.enabled === false,
  );
}

function resetChannelDrafts() {
  channels.forEach((channel) => {
    clearChannelDraft(channel);
  });
}

function clearChannelDraft(channel: NoticeChannelType) {
  delete channelDrafts[channel];
  delete channelMappingDrafts[channel];
}

function cloneMappingRows(rows: TemplateMappingRow[]) {
  return rows.map(item => ({ ...item }));
}

function resolveParamsSchemaForOptions() {
  if (schemaEditMode.value === 'JSON') {
    return schemaJsonText.value;
  }
  if (schemaFieldCount.value > 0) {
    return paramsSchemaJson.value;
  }
  return businessConfigForForm.value?.paramsSchema || currentBusinessType.value?.paramsSchema || form.paramsSchema || paramsSchemaJson.value;
}

function versionStatusTag(status?: NoticeTemplateVersionStatus) {
  if (status === 'DRAFT') {
    return { label: '草稿', type: 'warning' as const };
  }
  if (status === 'ACTIVE') {
    return { label: '生效', type: 'success' as const };
  }
  if (status === 'HISTORY') {
    return { label: '历史', type: 'info' as const };
  }
  return { label: '未配置', type: 'info' as const };
}

function parseVariableMapping(value?: string): TemplateMappingRow[] {
  if (!value?.trim()) {
    return [];
  }
  try {
    const parsed = JSON.parse(value) as unknown;
    if (!isJsonObject(parsed)) {
      return [];
    }
    return Object.entries(parsed)
      .filter(([, targetName]) => typeof targetName === 'string')
      .map(([paramName, targetName]) => ({ paramName, targetName: String(targetName) }));
  } catch {
    return [];
  }
}

watch(activeChannel, (_channel, oldChannel) => {
  if (oldChannel && pageMode.value === 'MAINTAIN') {
    snapshotTemplateForm(oldChannel);
  }
  applyTemplate();
});

onMounted(() => {
  loadDomainOptions();
  loadBusinessTypes();
  loadChannelConfigs();
});
</script>

<style scoped>
.notice-business-config-page {
  display: flex;
  min-height: calc(100vh - var(--mango-header-height) - var(--mango-tags-view-height) - 32px);
  padding: 0;
}

.business-main {
  display: flex;
  flex: 1;
  min-width: 0;
}

.business-main :deep(.el-card__body) {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  padding: 16px;
}

.page-card {
  min-height: calc(100vh - 136px);
}

.list-page-header,
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.list-page-header {
  margin-bottom: 16px;
}

.list-page-header h1 {
  margin: 0;
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 650;
  line-height: 32px;
}

.page-header {
  margin-bottom: 18px;
}

.page-header h1 {
  margin: 0 0 4px;
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 650;
  line-height: 1.3;
}

.page-header span,
.version-summary-inline {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.page-actions {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

.page-actions :deep(.el-button) {
  margin-left: 0;
}

.definition-layout {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 12px;
}

.domain-panel {
  border: 1px solid var(--el-border-color-lighter);
  min-height: 420px;
}

.domain-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 40px;
  padding: 0 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  font-weight: 600;
}

.domain-panel :deep(.el-menu) {
  border-right: none;
}

.definition-main,
.maintain-form,
.form-section,
.history-detail {
  min-width: 0;
}

.maintain-config-row {
  align-items: stretch;
}

.maintain-config-row > :deep(.el-col) {
  display: flex;
  min-width: 0;
}

.maintain-config-row .form-section {
  display: flex;
  flex: 1;
  flex-direction: column;
}

.parameter-section,
.message-type-section {
  min-height: 520px;
}

.message-type-section {
  overflow: visible;
}

.message-type-tabs {
  flex-shrink: 0;
}

.list-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.notice-filter {
  flex: 1;
  min-width: 0;
  margin-bottom: 0;
}

.notice-filter :deep(.el-form-item) {
  margin-bottom: 0;
}

.filter-control {
  width: 160px;
}

.notice-tag {
  margin: 2px 4px 2px 0;
}

.notice-muted {
  color: var(--el-text-color-secondary);
}

.form-section {
  margin-bottom: 16px;
  padding: 16px 16px 2px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  background: var(--el-bg-color);
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
  color: var(--el-text-color-primary);
  font-weight: 650;
}

.form-control {
  width: 100%;
}

.schema-field-table :deep(.el-table__cell) {
  padding: 5px 0;
}

.table-icon-button {
  width: 28px;
  height: 28px;
  padding: 0;
}

.notice-schema-tabs {
  min-width: 0;
}

.notice-schema-tabs :deep(.el-tabs__content) {
  overflow: visible;
}

.schema-json-input :deep(.el-textarea__inner) {
  min-height: 150px !important;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
  line-height: 1.5;
}

.message-type-tabs :deep(.el-tabs__header) {
  margin-bottom: 14px;
}

.template-section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  color: var(--el-text-color-primary);
  font-weight: 650;
}

.template-enabled-item :deep(.el-form-item__content) {
  display: flex;
  align-items: center;
  gap: 10px;
}

.template-config-form,
.template-readonly-descriptions,
.readonly-definition-view {
  min-width: 0;
}

.template-config-form,
.template-readonly-descriptions {
  flex: 1;
  width: 100%;
}

.template-config-form :deep(.el-form-item) {
  width: 100%;
  margin-bottom: 18px;
}

.template-config-form :deep(.el-row) {
  width: 100%;
}

.template-config-form :deep(.el-col) {
  display: block;
  width: 100%;
}

.template-config-form :deep(.el-form-item__content) {
  flex: 1;
  min-width: 0;
}

.template-route-select {
  width: 260px;
  max-width: 100%;
}

.template-title-input {
  width: 420px;
  max-width: 100%;
}

.template-content-item {
  width: 100%;
}

.template-content-item :deep(.el-form-item__content) {
  width: 100%;
  max-width: none;
}

.template-editor,
.template-content-textarea {
  width: 100%;
  min-width: 0;
}

.template-editor:not(.w-e-full-screen-container) {
  display: flex;
  flex-direction: column;
  height: 260px;
  overflow: hidden;
}

.template-editor:not(.w-e-full-screen-container) :deep(.editor-toolbar) {
  flex-shrink: 0;
}

.template-editor:not(.w-e-full-screen-container) :deep(.editor-content) {
  flex: 1;
  height: auto !important;
  min-height: 0;
}

.template-editor:not(.w-e-full-screen-container) :deep(.w-e-text-container),
.template-editor:not(.w-e-full-screen-container) :deep(.w-e-scroll) {
  height: 100% !important;
  min-height: 100% !important;
}

.template-config-form :deep(.el-textarea__inner) {
  height: 260px !important;
  min-height: 260px !important;
  resize: vertical;
}

.readonly-content {
  min-height: 260px;
  max-height: 260px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
}

.param-variable-panel {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 2px;
  padding-top: 12px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.param-variable-panel > .notice-muted {
  line-height: 24px;
}

.param-variable-tag {
  cursor: pointer;
}

.history-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 16px;
  min-height: calc(100vh - 220px);
}

.history-list {
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
}

.history-version-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
  margin-bottom: 8px;
  padding: 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  background: var(--el-bg-color);
  color: var(--el-text-color-regular);
  text-align: left;
  cursor: pointer;
}

.history-version-item.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.history-version-head,
.history-detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

@media (max-width: 980px) {
  .definition-layout,
  .history-layout {
    grid-template-columns: 1fr;
  }

  .maintain-config-row :deep(.el-col) {
    display: block;
  }

  .parameter-section,
  .message-type-section {
    min-height: 0;
  }

  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .page-actions {
    justify-content: flex-start;
  }
}
</style>
