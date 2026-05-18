<template>
  <div class="expense-approval-detail">
    <div class="business-title-row">
      <div>
        <div class="business-eyebrow">费用报销</div>
        <h3>{{ snapshot.title || snapshot.expenseCode || context.businessKey || '报销申请' }}</h3>
      </div>
      <div class="title-tags">
        <el-tag v-if="nodePreset" effect="plain">{{ nodePreset }}</el-tag>
        <el-tag :type="amountRiskType" effect="plain">{{ snapshot.riskLabel }}</el-tag>
      </div>
    </div>

    <div class="snapshot-grid">
      <div class="snapshot-item strong">
        <span>申请金额</span>
        <strong>{{ formatAmount(snapshot.amount) }}</strong>
      </div>
      <div class="snapshot-item">
        <span>费用类型</span>
        <strong>{{ snapshot.category || '-' }}</strong>
      </div>
      <div class="snapshot-item">
        <span>发生日期</span>
        <strong>{{ snapshot.expenseDate || '-' }}</strong>
      </div>
      <div class="snapshot-item">
        <span>申请人</span>
        <strong>{{ snapshot.applicant || '-' }}</strong>
      </div>
    </div>

    <section v-if="sectionVisible('expenseReason')" class="business-section">
      <div class="business-section-title">报销事由</div>
      <p class="reason-text">{{ snapshot.reason || '-' }}</p>
    </section>

    <section v-if="sectionVisible('invoiceInfo')" class="business-section">
      <div class="business-section-title">票据与付款信息</div>
      <div class="compact-grid">
        <div>
          <span>票据张数</span>
          <strong>{{ snapshot.invoiceCount || 0 }} 张</strong>
        </div>
        <div>
          <span>收款账户</span>
          <strong>{{ sectionEditable('paymentInfo') ? snapshot.bankAccount || '-' : maskedAccount }}</strong>
        </div>
        <div>
          <span>预算科目</span>
          <strong>{{ snapshot.budgetSubject || '-' }}</strong>
        </div>
      </div>
    </section>

    <section v-if="sectionVisible('financeReview')" class="business-section">
      <div class="business-section-title">财务复核</div>
      <el-form label-width="96px" class="finance-form">
        <el-form-item label="核定金额">
        <el-input-number
            v-model="approvedAmount"
            :disabled="!sectionEditable('financeReview') || context.readonly"
            :min="0"
            :precision="2"
            controls-position="right"
          />
        </el-form-item>
      </el-form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { BusinessApprovalContext } from '../businessApproval';

const props = defineProps<{
  context: BusinessApprovalContext;
}>();

const snapshot = computed(() => {
  const variables = props.context.variables || {};
  return {
    title: variables.title,
    expenseCode: variables.expenseCode || props.context.businessKey,
    applicant: variables.applicant,
    category: variables.category,
    amount: Number(variables.amount || 0),
    expenseDate: variables.expenseDate,
    reason: variables.reason,
    invoiceCount: Number(variables.invoiceCount || 0),
    bankAccount: variables.bankAccount,
    budgetSubject: variables.budgetSubject,
    approvedAmount: Number(variables.approvedAmount || variables.amount || 0),
    riskLabel: Number(variables.amount || 0) >= 5000 ? '大额报销' : '常规报销',
  };
});

const approvedAmount = computed({
  get: () => Number(props.context.variables.approvedAmount ?? snapshot.value.approvedAmount ?? 0),
  set: (value: number | undefined) => {
    props.context.variables.approvedAmount = Number(value || 0);
  },
});

const amountRiskType = computed(() => snapshot.value.amount >= 5000 ? 'warning' : 'success');
const nodePreset = computed(() => String(props.context.nodeExtension?.sectionPreset || '').trim());
const maskedAccount = computed(() => {
  const account = String(snapshot.value.bankAccount || '');
  if (account.length <= 6) return account || '-';
  return `${account.slice(0, 3)} **** ${account.slice(-4)}`;
});

function sectionVisible(section: string) {
  return props.context.permissions[section] !== 'HIDDEN';
}

function sectionEditable(section: string) {
  return props.context.permissions[section] === 'EDITABLE';
}

function formatAmount(value: number) {
  return `¥${Number(value || 0).toFixed(2)}`;
}
</script>

<style scoped>
.expense-approval-detail {
  display: grid;
  gap: 14px;
}

.business-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.title-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.business-eyebrow {
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 600;
}

.business-title-row h3 {
  margin: 4px 0 0;
  color: var(--el-text-color-primary);
  font-size: 17px;
}

.snapshot-grid,
.compact-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.snapshot-item,
.compact-grid > div {
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-extra-light);
}

.snapshot-item span,
.snapshot-item strong,
.compact-grid span,
.compact-grid strong {
  display: block;
}

.snapshot-item span,
.compact-grid span {
  margin-bottom: 5px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.snapshot-item strong,
.compact-grid strong {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.snapshot-item.strong strong {
  color: var(--el-color-primary);
  font-size: 18px;
}

.business-section {
  padding-top: 14px;
  border-top: 1px dashed var(--el-border-color);
}

.business-section-title {
  margin-bottom: 10px;
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
}

.reason-text {
  margin: 0;
  color: var(--el-text-color-regular);
  line-height: 1.7;
}

.finance-form {
  max-width: 360px;
}

@media (max-width: 860px) {
  .snapshot-grid,
  .compact-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .snapshot-grid,
  .compact-grid {
    grid-template-columns: 1fr;
  }
}
</style>
