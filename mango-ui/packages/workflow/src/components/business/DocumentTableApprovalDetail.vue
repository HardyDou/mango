<template>
  <div class="document-table-approval">
    <div class="document-sheet">
      <div class="document-meta">
        <span>{{ snapshot.applyId || context.applyId || '待提交' }}</span>
        <span>{{ snapshot.expectedSealDate || '未设置用印日期' }}</span>
      </div>
      <h2>合同用印审批单</h2>
      <table class="word-table">
        <tbody>
          <tr v-if="sectionVisible('basicInfo')">
            <th>申请部门</th>
            <td>
              <EditableCell field="department" :editable="fieldEditable('basicInfo')" placeholder="请输入申请部门" />
            </td>
            <th>申请人</th>
            <td>
              <EditableCell field="applicant" :editable="fieldEditable('basicInfo')" placeholder="请输入申请人" />
            </td>
          </tr>
          <tr v-if="sectionVisible('contractInfo')">
            <th>合同名称</th>
            <td>
              <EditableCell field="contractName" :editable="fieldEditable('contractInfo')" placeholder="请输入合同名称" />
            </td>
            <th>合同编号</th>
            <td>
              <EditableCell field="contractCode" :editable="fieldEditable('contractInfo')" placeholder="请输入合同编号" />
            </td>
          </tr>
          <tr v-if="sectionVisible('contractInfo')">
            <th>对方单位</th>
            <td colspan="3">
              <EditableCell field="counterparty" :editable="fieldEditable('contractInfo')" placeholder="请输入对方单位" />
            </td>
          </tr>
          <tr v-if="sectionVisible('contractInfo')">
            <th>合同类型</th>
            <td>
              <EditableCell field="contractType" :editable="fieldEditable('contractInfo')" placeholder="如：采购合同" />
            </td>
            <th>合同金额</th>
            <td>
              <el-input-number
                v-if="fieldEditable('contractInfo')"
                v-model="contractAmount"
                :min="0"
                :precision="2"
                controls-position="right"
              />
              <span v-else>{{ formatAmount(snapshot.contractAmount) }}</span>
            </td>
          </tr>
          <tr v-if="sectionVisible('sealInfo')">
            <th>用印类型</th>
            <td>
              <EditableCell field="sealType" :editable="fieldEditable('sealInfo')" placeholder="如：合同专用章" />
            </td>
            <th>申请份数</th>
            <td>
              <el-input-number
                v-if="fieldEditable('sealInfo')"
                v-model="sealCount"
                :min="1"
                controls-position="right"
              />
              <span v-else>{{ snapshot.sealCount || 0 }} 份</span>
            </td>
          </tr>
          <tr v-if="sectionVisible('sealInfo')">
            <th>期望用印日期</th>
            <td>
              <el-date-picker
                v-if="fieldEditable('sealInfo')"
                v-model="expectedSealDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择日期"
              />
              <span v-else>{{ snapshot.expectedSealDate || '-' }}</span>
            </td>
            <th>紧急程度</th>
            <td>
              <EditableCell field="urgency" :editable="fieldEditable('sealInfo')" placeholder="普通/紧急" />
            </td>
          </tr>
          <tr v-if="sectionVisible('riskInfo')">
            <th>风险等级</th>
            <td>
              <EditableCell field="riskLevel" :editable="fieldEditable('riskInfo')" placeholder="低/中/高" />
            </td>
            <th>附件清单</th>
            <td>
              <EditableCell field="attachmentList" :editable="fieldEditable('riskInfo')" placeholder="合同正文、审批依据等" />
            </td>
          </tr>
          <tr v-if="sectionVisible('purpose')">
            <th>用印事由</th>
            <td colspan="3">
              <EditableTextarea field="purpose" :editable="fieldEditable('purpose')" placeholder="请输入用印事由和背景说明" />
            </td>
          </tr>
          <tr v-if="sectionVisible('legalOpinion')">
            <th>法务意见</th>
            <td colspan="3">
              <EditableTextarea field="legalOpinion" :editable="fieldEditable('legalOpinion')" placeholder="请输入法务审核意见" />
            </td>
          </tr>
          <tr v-if="sectionVisible('financeOpinion')">
            <th>财务意见</th>
            <td colspan="3">
              <EditableTextarea field="financeOpinion" :editable="fieldEditable('financeOpinion')" placeholder="请输入财务复核意见" />
            </td>
          </tr>
          <tr v-if="sectionVisible('sealKeeperOpinion')">
            <th>实际用印份数</th>
            <td>
              <el-input-number
                v-if="fieldEditable('sealKeeperOpinion')"
                v-model="approvedSealCount"
                :min="0"
                controls-position="right"
              />
              <span v-else>{{ snapshot.approvedSealCount || snapshot.sealCount || 0 }} 份</span>
            </td>
            <th>印章管理员意见</th>
            <td>
              <EditableCell field="sealKeeperOpinion" :editable="fieldEditable('sealKeeperOpinion')" placeholder="请输入用印登记意见" />
            </td>
          </tr>
        </tbody>
      </table>
      <div class="document-footer">
        <span>业务单号：{{ context.businessKey || snapshot.contractCode || '-' }}</span>
        <span>当前节点：{{ context.nodeName || '未进入审批' }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h } from 'vue';
import { ElInput } from 'element-plus';
import type { BusinessApprovalContext } from '../businessApproval';

const props = defineProps<{
  context: BusinessApprovalContext;
}>();

const snapshot = computed(() => props.context.variables || {});

const contractAmount = computed({
  get: () => Number(props.context.variables.contractAmount || 0),
  set: value => {
    props.context.variables.contractAmount = Number(value || 0);
  },
});

const sealCount = computed({
  get: () => Number(props.context.variables.sealCount || 1),
  set: value => {
    props.context.variables.sealCount = Number(value || 0);
  },
});

const expectedSealDate = computed({
  get: () => String(props.context.variables.expectedSealDate || ''),
  set: value => {
    props.context.variables.expectedSealDate = value;
  },
});

const approvedSealCount = computed({
  get: () => Number(props.context.variables.approvedSealCount ?? props.context.variables.sealCount ?? 0),
  set: value => {
    props.context.variables.approvedSealCount = Number(value || 0);
  },
});

const EditableCell = defineComponent({
  name: 'EditableCell',
  props: {
    field: { type: String, required: true },
    editable: { type: Boolean, default: false },
    placeholder: { type: String, default: '' },
  },
  setup(cellProps) {
    return () => cellProps.editable
      ? h(ElInput, {
        modelValue: props.context.variables[cellProps.field],
        placeholder: cellProps.placeholder,
        'onUpdate:modelValue': (value: string) => {
          props.context.variables[cellProps.field] = value;
        },
      })
      : h('span', String(props.context.variables[cellProps.field] || '-'));
  },
});

const EditableTextarea = defineComponent({
  name: 'EditableTextarea',
  props: {
    field: { type: String, required: true },
    editable: { type: Boolean, default: false },
    placeholder: { type: String, default: '' },
  },
  setup(cellProps) {
    return () => cellProps.editable
      ? h(ElInput, {
        modelValue: props.context.variables[cellProps.field],
        type: 'textarea',
        rows: 3,
        placeholder: cellProps.placeholder,
        'onUpdate:modelValue': (value: string) => {
          props.context.variables[cellProps.field] = value;
        },
      })
      : h('span', { class: 'multiline-text' }, String(props.context.variables[cellProps.field] || '-'));
  },
});

function sectionVisible(section: string) {
  return props.context.permissions[section] !== 'HIDDEN';
}

function fieldEditable(section: string) {
  return !props.context.readonly && props.context.permissions[section] === 'EDITABLE';
}

function formatAmount(value: unknown) {
  return `¥${Number(value || 0).toFixed(2)}`;
}
</script>

<style scoped>
.document-table-approval {
  display: flex;
  justify-content: center;
  min-width: 0;
}

.document-sheet {
  width: min(900px, 100%);
  padding: 22px 26px;
  border: 1px solid #d8dce5;
  background: #fffdf8;
  color: #1f2937;
  box-shadow: 0 8px 28px rgba(31, 41, 55, 0.08);
}

.document-meta,
.document-footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #6b7280;
  font-size: 12px;
}

.document-sheet h2 {
  margin: 8px 0 18px;
  text-align: center;
  color: #111827;
  font-family: SimSun, "Songti SC", serif;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0;
}

.word-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  border: 2px solid #1f2937;
  font-family: SimSun, "Songti SC", serif;
}

.word-table th,
.word-table td {
  min-height: 44px;
  padding: 8px 10px;
  border: 1px solid #1f2937;
  vertical-align: middle;
  line-height: 1.6;
  word-break: break-word;
}

.word-table th {
  width: 118px;
  background: #f5f0e6;
  color: #111827;
  font-weight: 700;
  text-align: center;
}

.word-table td {
  background: #fffefa;
}

.word-table :deep(.el-input__wrapper),
.word-table :deep(.el-textarea__inner) {
  box-shadow: none;
  border-radius: 0;
  background: #fffefa;
  font-family: SimSun, "Songti SC", serif;
}

.word-table :deep(.el-input-number),
.word-table :deep(.el-date-editor) {
  width: 100%;
}

.multiline-text {
  display: block;
  min-height: 54px;
  white-space: pre-wrap;
}

.document-footer {
  margin-top: 12px;
}

@media (max-width: 720px) {
  .document-sheet {
    padding: 16px 12px;
  }

  .word-table {
    min-width: 680px;
  }

  .document-table-approval {
    justify-content: flex-start;
    overflow: auto;
  }
}
</style>
