<template>
  <el-dialog v-model="visible" :title="panelTitle" width="1120px" destroy-on-close append-to-body>
    <section class="method-route-panel">
      <div class="method-route-panel__toolbar">
        <el-button type="primary" :icon="Plus" @click="openEditor()">新增路由</el-button>
        <el-button :icon="Operation" @click="openTrial">路由试算</el-button>
      </div>

      <el-table :data="routes" v-loading="loading" row-key="id" stripe class="method-route-panel__table">
        <el-table-column prop="ruleCode" label="规则编码" min-width="190" show-overflow-tooltip />
        <el-table-column prop="ruleName" label="规则名称" min-width="190" show-overflow-tooltip />
        <el-table-column label="应用" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.appName || valueText(row.appId) }}</template>
        </el-table-column>
        <el-table-column label="企业主体" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.subjectName || valueText(row.subjectId) }}</template>
        </el-table-column>
        <el-table-column prop="terminalType" label="终端" width="90" />
        <el-table-column label="模式" width="110">
          <template #default="{ row }">{{ routeModeText(row.routeMode) }}</template>
        </el-table-column>
        <el-table-column label="降级" width="80">
          <template #default="{ row }">{{ row.fallbackEnabled === 1 ? '允许' : '禁止' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="openEditor(row)">编辑</el-button>
            <el-button link type="danger" :icon="Delete" @click="deleteRoute(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="editorVisible" :title="editorTitle" width="980px" destroy-on-close append-to-body>
      <el-form ref="editorFormRef" :model="editorForm" :rules="editorRules" label-width="118px" class="method-route-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="规则编码" prop="ruleCode">
              <el-input v-model="editorForm.ruleCode" placeholder="例如 ORDER_CENTER_WECHAT_MANGO_PAY" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规则名称" prop="ruleName">
              <el-input v-model="editorForm.ruleName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="应用">
              <PaymentEntitySelect
                v-model="editorForm.appId"
                :api="paymentApplicationApi"
                label-field="appName"
                description-field="appId"
                placeholder="不选表示租户通用"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="企业主体">
              <PaymentEntitySelect
                v-model="editorForm.subjectId"
                :api="paymentEnterpriseSubjectApi"
                label-field="subjectName"
                description-field="creditCodeMask"
                placeholder="不选表示不限制主体"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="终端" prop="terminalType">
              <el-select v-model="editorForm.terminalType">
                <el-option label="Web" value="WEB" />
                <el-option label="H5" value="H5" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="路由模式" prop="routeMode">
              <el-select v-model="editorForm.routeMode">
                <el-option label="优先级" value="PRIORITY" />
                <el-option label="手工指定" value="MANUAL" />
                <el-option label="权重" value="WEIGHT" />
                <el-option label="成本" value="COST" />
                <el-option label="健康状态" value="HEALTH" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="失败降级" prop="fallbackEnabled">
              <el-switch v-model="editorForm.fallbackEnabled" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="editorForm.status">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="路由明细" prop="items">
          <div class="route-item-editor">
            <div class="route-item-editor__actions">
              <el-button :icon="Plus" @click="addItem">添加签约能力</el-button>
            </div>
            <el-table :data="editorForm.items" size="small" row-key="clientKey" empty-text="请添加可路由的签约能力">
              <el-table-column label="签约能力" min-width="220">
                <template #default="{ row }">
                  <PaymentEntitySelect
                    v-model="row.contractCapabilityId"
                    :api="contractCapabilitySelectApi"
                    label-field="contractName"
                    description-field="channelName"
                    placeholder="选择签约能力"
                    @update:model-value="value => hydrateItem(row, value)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="优先级" width="120">
                <template #default="{ row }">
                  <el-input-number v-model="row.priority" :min="1" :precision="0" controls-position="right" />
                </template>
              </el-table-column>
              <el-table-column label="权重" width="120">
                <template #default="{ row }">
                  <el-input-number v-model="row.weight" :min="1" :precision="0" controls-position="right" />
                </template>
              </el-table-column>
              <el-table-column label="最小金额（元）" width="150">
                <template #default="{ row }">
                  <el-input-number v-model="row.minAmountYuan" :min="0" :precision="2" :step="1" controls-position="right" />
                </template>
              </el-table-column>
              <el-table-column label="最大金额（元）" width="150">
                <template #default="{ row }">
                  <el-input-number v-model="row.maxAmountYuan" :min="0" :precision="2" :step="1" controls-position="right" />
                </template>
              </el-table-column>
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <el-switch v-model="row.status" :active-value="1" :inactive-value="0" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ $index }">
                  <el-button link type="danger" :icon="Delete" @click="removeItem($index)">移除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRoute">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="trialVisible" title="路由试算" width="760px" destroy-on-close append-to-body>
      <el-form ref="trialFormRef" :model="trialForm" :rules="trialRules" label-width="110px" class="method-route-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="应用" prop="applicationId">
              <PaymentEntitySelect v-model="trialForm.applicationId" :api="paymentApplicationApi" label-field="appName" description-field="appId" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="企业主体" prop="subjectId">
              <PaymentEntitySelect v-model="trialForm.subjectId" :api="paymentEnterpriseSubjectApi" label-field="subjectName" description-field="creditCodeMask" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="终端" prop="terminalType">
              <el-select v-model="trialForm.terminalType">
                <el-option label="Web" value="WEB" />
                <el-option label="H5" value="H5" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="金额（元）" prop="amountYuan">
              <el-input-number v-model="trialForm.amountYuan" :min="0.01" :precision="2" :step="1" controls-position="right" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <section v-if="trialResult" class="route-trial-result">
        <el-result
          :icon="trialResult.matched ? 'success' : 'warning'"
          :title="trialResult.matched ? '已命中路由' : '未命中可用能力'"
        >
          <template #sub-title>
            <div v-if="trialResult.matched && trialResult.matchedRule && trialResult.matchedItem">
              {{ trialResult.matchedRule.ruleName }} / {{ trialResult.matchedItem.channelName }} / {{ trialResult.matchedItem.contractName }}
            </div>
            <div v-else>{{ trialResult.filterReasons?.join('；') || '无可用路由' }}</div>
          </template>
        </el-result>
      </section>
      <template #footer>
        <el-button @click="trialVisible = false">关闭</el-button>
        <el-button type="primary" :loading="trialLoading" @click="submitTrial">试算</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { Delete, Edit, Operation, Plus } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import type { ApiId } from '@mango/api-schema';
import PaymentEntitySelect from '../../components/PaymentEntitySelect.vue';
import {
  paymentApplicationApi,
  paymentChannelContractApi,
  paymentEnterpriseSubjectApi,
  paymentMethodRouteApi,
  type PageResult,
  type PaymentChannelContract,
  type PaymentMethod,
  type PaymentMethodRouteRule,
  type PaymentMethodRouteRuleItem,
  type PaymentMethodRouteTrialCommand,
  type PaymentMethodRouteTrialResult,
  type PaymentRecord,
  type PaymentResourceApi,
} from '../../api/payment';

type RouteItemForm = Partial<PaymentMethodRouteRuleItem> & {
  clientKey: string;
  minAmountYuan?: number;
  maxAmountYuan?: number;
};
type RouteForm = Partial<PaymentMethodRouteRule> & { items: RouteItemForm[] };
type TrialForm = Omit<PaymentMethodRouteTrialCommand, 'amount'> & { amountYuan: number };

const visible = ref(false);
const loading = ref(false);
const saving = ref(false);
const editorVisible = ref(false);
const trialVisible = ref(false);
const trialLoading = ref(false);
const currentMethod = ref<PaymentMethod>();
const routes = ref<PaymentMethodRouteRule[]>([]);
const editorFormRef = ref<FormInstance>();
const trialFormRef = ref<FormInstance>();
const trialResult = ref<PaymentMethodRouteTrialResult>();
const editorForm = reactive<RouteForm>({
  methodCode: '',
  terminalType: 'WEB',
  routeMode: 'PRIORITY',
  fallbackEnabled: 1,
  status: 1,
  items: [],
});
const trialForm = reactive<TrialForm>({
  applicationId: '',
  subjectId: '',
  methodCode: '',
  terminalType: 'WEB',
  amountYuan: 0.01,
});

const panelTitle = computed(() => `${currentMethod.value?.methodName || currentMethod.value?.methodCode || ''} 路由策略`);
const editorTitle = computed(() => `${editorForm.id ? '编辑' : '新增'}路由策略`);

const editorRules: FormRules = {
  ruleCode: [{ required: true, message: '请输入规则编码', trigger: 'blur' }],
  ruleName: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  terminalType: [{ required: true, message: '请选择终端', trigger: 'change' }],
  routeMode: [{ required: true, message: '请选择路由模式', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
};
const trialRules: FormRules = {
  applicationId: [{ required: true, message: '请选择应用', trigger: 'change' }],
  subjectId: [{ required: true, message: '请选择企业主体', trigger: 'change' }],
  terminalType: [{ required: true, message: '请选择终端', trigger: 'change' }],
  amountYuan: [{ required: true, type: 'number', min: 0.01, message: '请输入金额', trigger: 'blur' }],
};

const contractCapabilitySelectApi: PaymentResourceApi<PaymentRecord> = {
  page: async params => {
    const result = await paymentChannelContractApi.page(params);
    const list = result.list.flatMap(contract => toContractCapabilityOptions(contract));
    return {
      list,
      total: list.length,
      pageNum: params?.pageNum || 1,
      pageSize: params?.pageSize || 50,
    } as PageResult<PaymentRecord>;
  },
  detail: async id => {
    const contracts = await paymentChannelContractApi.page({ pageNum: 1, pageSize: 200, status: 1 });
    const option = contracts.list.flatMap(contract => toContractCapabilityOptions(contract)).find(item => String(item.id) === String(id));
    return option || {};
  },
  create: async () => undefined,
  update: async () => undefined,
  remove: async () => false,
};

function open(method: PaymentMethod) {
  currentMethod.value = method;
  visible.value = true;
  void loadRoutes();
}

defineExpose({ open });

async function loadRoutes() {
  if (!currentMethod.value?.methodCode) return;
  loading.value = true;
  try {
    const result = await paymentMethodRouteApi.page({
      pageNum: 1,
      pageSize: 100,
      methodCode: currentMethod.value.methodCode,
    });
    routes.value = result.list;
  } finally {
    loading.value = false;
  }
}

async function openEditor(row?: PaymentMethodRouteRule) {
  resetEditor();
  if (row?.id) {
    const detail = await paymentMethodRouteApi.detail(row.id);
    Object.assign(editorForm, {
      ...detail,
      items: (detail.items || []).map(toItemForm),
    });
  } else {
    Object.assign(editorForm, {
      ruleCode: `${currentMethod.value?.methodCode || 'METHOD'}_${Date.now()}`,
      ruleName: `${currentMethod.value?.methodName || currentMethod.value?.methodCode || '支付方式'}路由`,
      methodCode: currentMethod.value?.methodCode || '',
      terminalType: defaultTerminal(),
      routeMode: 'PRIORITY',
      fallbackEnabled: 1,
      status: 1,
      items: [],
    });
  }
  editorVisible.value = true;
}

function resetEditor() {
  Object.keys(editorForm).forEach(key => delete (editorForm as Record<string, unknown>)[key]);
  Object.assign(editorForm, { items: [] });
}

function addItem() {
  editorForm.items.push({
    clientKey: `${Date.now()}-${Math.random()}`,
    priority: editorForm.items.length * 10 + 10,
    weight: 100,
    status: 1,
  });
}

function removeItem(index: number) {
  editorForm.items.splice(index, 1);
}

async function hydrateItem(row: RouteItemForm, value: ApiId | ApiId[] | string) {
  const id = Array.isArray(value) ? value[0] : value;
  if (!id) return;
  const option = await contractCapabilitySelectApi.detail(id);
  row.contractId = option.contractId as ApiId;
  row.contractName = option.contractName as string;
  row.channelId = option.channelId as ApiId;
  row.channelName = option.channelName as string;
  row.methodCode = option.methodCode as string;
  row.terminalType = option.terminalType as string;
  row.minAmountYuan = centsToYuan(option.minAmount as number | undefined);
  row.maxAmountYuan = centsToYuan(option.maxAmount as number | undefined);
}

async function saveRoute() {
  await editorFormRef.value?.validate();
  if (!editorForm.items.length) {
    ElMessage.error('请至少添加一条路由明细');
    return;
  }
  saving.value = true;
  try {
    const payload = toRoutePayload();
    if (editorForm.id) {
      await paymentMethodRouteApi.update(payload);
      ElMessage.success('已保存');
    } else {
      await paymentMethodRouteApi.create(payload);
      ElMessage.success('已新增');
    }
    editorVisible.value = false;
    await loadRoutes();
  } finally {
    saving.value = false;
  }
}

async function deleteRoute(row: PaymentMethodRouteRule) {
  if (!row.id) return;
  await ElMessageBox.confirm(`确认删除路由策略 ${row.ruleName || row.ruleCode}？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  });
  await paymentMethodRouteApi.remove(row.id);
  ElMessage.success('已删除');
  await loadRoutes();
}

function openTrial() {
  trialResult.value = undefined;
  trialForm.methodCode = currentMethod.value?.methodCode || '';
  trialForm.terminalType = defaultTerminal();
  trialForm.amountYuan = 0.01;
  trialVisible.value = true;
}

async function submitTrial() {
  await trialFormRef.value?.validate();
  trialLoading.value = true;
  try {
    trialResult.value = await paymentMethodRouteApi.trial({
      applicationId: trialForm.applicationId,
      subjectId: trialForm.subjectId,
      methodCode: trialForm.methodCode,
      terminalType: trialForm.terminalType,
      amount: yuanToCents(trialForm.amountYuan),
    });
  } finally {
    trialLoading.value = false;
  }
}

function toRoutePayload(): PaymentMethodRouteRule {
  return {
    id: editorForm.id,
    ruleCode: String(editorForm.ruleCode || ''),
    ruleName: String(editorForm.ruleName || ''),
    appId: emptyToUndefined(editorForm.appId),
    subjectId: emptyToUndefined(editorForm.subjectId),
    methodCode: currentMethod.value?.methodCode || String(editorForm.methodCode || ''),
    terminalType: String(editorForm.terminalType || 'WEB'),
    routeMode: String(editorForm.routeMode || 'PRIORITY'),
    fallbackEnabled: Number(editorForm.fallbackEnabled ?? 1),
    status: Number(editorForm.status ?? 1),
    items: editorForm.items.map(item => ({
      id: item.id,
      contractCapabilityId: item.contractCapabilityId,
      priority: item.priority ?? 100,
      weight: item.weight ?? 100,
      minAmount: item.minAmountYuan === undefined || item.minAmountYuan === null ? undefined : yuanToCents(item.minAmountYuan),
      maxAmount: item.maxAmountYuan === undefined || item.maxAmountYuan === null ? undefined : yuanToCents(item.maxAmountYuan),
      status: item.status ?? 1,
    })),
  };
}

function toContractCapabilityOptions(contract: PaymentChannelContract): PaymentRecord[] {
  return (contract.capabilities || [])
    .filter(item => item.id && item.status !== 0 && item.methodCode === currentMethod.value?.methodCode)
    .map(item => ({
      id: item.id,
      contractCapabilityId: item.id,
      contractId: contract.id,
      contractName: contract.contractName,
      channelId: contract.channelId,
      channelName: contract.channelName,
      methodCode: item.methodCode,
      terminalType: item.terminalType,
      minAmount: item.minAmount,
      maxAmount: item.maxAmount,
    }));
}

function toItemForm(item: PaymentMethodRouteRuleItem): RouteItemForm {
  return {
    ...item,
    clientKey: String(item.id || `${Date.now()}-${Math.random()}`),
    minAmountYuan: centsToYuan(item.minAmount),
    maxAmountYuan: centsToYuan(item.maxAmount),
  };
}

function defaultTerminal() {
  const terminals = String(currentMethod.value?.terminalScope || 'WEB').split(',').map(item => item.trim());
  return terminals.includes('WEB') ? 'WEB' : terminals[0] || 'WEB';
}

function routeModeText(value?: string) {
  const map: Record<string, string> = {
    PRIORITY: '优先级',
    MANUAL: '手工指定',
    WEIGHT: '权重',
    COST: '成本',
    HEALTH: '健康状态',
  };
  return map[String(value || '')] || value || '-';
}

function valueText(value: unknown) {
  return value === undefined || value === null || value === '' ? '通用' : String(value);
}

function emptyToUndefined(value: unknown) {
  return value === undefined || value === null || value === '' ? undefined : value as ApiId;
}

function centsToYuan(value?: number | null) {
  if (value === undefined || value === null) return undefined;
  return Number((Number(value) / 100).toFixed(2));
}

function yuanToCents(value?: number | null) {
  return Math.round(Number(value || 0) * 100);
}
</script>

<style scoped>
.method-route-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.method-route-panel__toolbar {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.method-route-panel__table {
  width: 100%;
}

.method-route-form :deep(.el-input),
.method-route-form :deep(.el-select),
.method-route-form :deep(.el-input-number) {
  width: 100%;
}

.route-item-editor {
  width: 100%;
}

.route-item-editor__actions {
  margin-bottom: 8px;
}

.route-trial-result {
  margin-top: 12px;
}
</style>
