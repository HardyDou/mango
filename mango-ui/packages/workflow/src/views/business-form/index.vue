<template>
  <div class="workflow-business-example-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <div>
            <div class="page-title">业务示例</div>
            <div class="page-subtitle">从真实工作流实例读取申请快照、当前节点和审批历史</div>
          </div>
          <div class="header-actions">
            <el-tag type="info">费用报销</el-tag>
            <el-tag type="success">合同用印</el-tag>
            <el-button @click="loadData">刷新</el-button>
            <el-button type="primary" @click="openApplyDialog()">申请报销</el-button>
            <el-button type="success" @click="openSealApplyDialog()">申请合同用印</el-button>
          </div>
        </div>
      </template>

      <div class="example-overview">
        <section class="example-intro">
          <div class="intro-eyebrow">业务接入方式</div>
          <h3>业务保存当前态，工作流保存流程态和关键变量</h3>
          <p>
            列表来自工作流申请中心；详情按业务类型和业务单号查询申请历史。当前节点读取申请中心同步的运行中任务。
          </p>
        </section>
        <section class="example-flow">
          <div v-for="step in integrationSteps" :key="step.title" class="flow-step">
            <span class="step-index">{{ step.index }}</span>
            <div>
              <strong>{{ step.title }}</strong>
              <small>{{ step.desc }}</small>
            </div>
          </div>
        </section>
      </div>

      <el-table v-loading="loading" :data="expenseRows" stripe class="example-table">
        <el-table-column prop="code" label="业务单号" min-width="160" />
        <el-table-column prop="applicant" label="申请人" width="110" />
        <el-table-column prop="category" label="费用类型" width="110">
          <template #default="{ row }">
            <el-tag effect="plain">{{ row.category || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="申请金额" width="120">
          <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="报销事由" min-width="180" show-overflow-tooltip />
        <el-table-column prop="businessStatus" label="业务状态" width="110">
          <template #default="{ row }">
            <el-tag :type="businessStatusTag(row.businessStatus)">{{ row.businessStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentNodeName" label="当前节点" width="150">
          <template #default="{ row }">
            <span class="current-node">{{ row.currentNodeName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="submitCount" label="申请次数" width="100" />
        <el-table-column prop="workflowName" label="关联流程" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetailDialog(row)">详情</el-button>
            <el-button v-if="canSubmit(row)" link type="primary" @click="openApplyDialog(row)">再申请</el-button>
            <el-button v-if="row.currentProcessInstanceId" link type="primary" @click="viewWorkflow(row)">查看流程</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!loading && !expenseRows.length && !sealRows.length"
        description="暂无业务流程实例，点击申请报销或申请合同用印发起真实流程"
      />

      <div class="business-table-divider">
        <div>
          <h3>合同用印审批</h3>
          <p>自定义业务页面，申请和审批均使用 Word 表格式审批单</p>
        </div>
        <el-button type="success" plain @click="openSealApplyDialog()">申请合同用印</el-button>
      </div>

      <el-table v-loading="loading" :data="sealRows" stripe class="example-table">
        <el-table-column prop="code" label="业务单号" min-width="170" />
        <el-table-column prop="contractName" label="合同名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="counterparty" label="对方单位" min-width="160" show-overflow-tooltip />
        <el-table-column prop="contractAmount" label="合同金额" width="130">
          <template #default="{ row }">{{ formatAmount(row.contractAmount) }}</template>
        </el-table-column>
        <el-table-column prop="sealType" label="用印类型" width="130" />
        <el-table-column prop="businessStatus" label="业务状态" width="110">
          <template #default="{ row }">
            <el-tag :type="businessStatusTag(row.businessStatus)">{{ row.businessStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="currentNodeName" label="当前节点" width="160">
          <template #default="{ row }">
            <span class="current-node">{{ row.currentNodeName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="submitCount" label="申请次数" width="100" />
        <el-table-column prop="workflowName" label="关联流程" min-width="170" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openSealDetailDialog(row)">详情</el-button>
            <el-button v-if="canSubmit(row)" link type="primary" @click="openSealApplyDialog(row)">再申请</el-button>
            <el-button v-if="row.currentProcessInstanceId" link type="primary" @click="viewWorkflow(row)">查看流程</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="applyDialogVisible"
      :title="expenseForm.submitCount ? '重新申请报销' : '申请报销'"
      width="720px"
      class="expense-apply-dialog"
      destroy-on-close
    >
      <div class="expense-apply-layout">
        <el-form ref="expenseFormRef" :model="expenseForm" :rules="expenseRules" label-width="92px" class="expense-form">
          <el-form-item label="业务单号">
            <el-input v-model="expenseForm.code" disabled />
          </el-form-item>
          <el-form-item label="申请人" prop="applicant">
            <el-input v-model="expenseForm.applicant" />
          </el-form-item>
          <el-form-item label="费用类型" prop="category">
            <el-select v-model="expenseForm.category" placeholder="请选择费用类型">
              <el-option label="差旅费" value="差旅费" />
              <el-option label="办公费" value="办公费" />
              <el-option label="招待费" value="招待费" />
              <el-option label="培训费" value="培训费" />
            </el-select>
          </el-form-item>
          <el-form-item label="报销金额" prop="amount">
            <el-input-number v-model="expenseForm.amount" :min="0" :precision="2" controls-position="right" />
          </el-form-item>
          <el-form-item label="发生日期" prop="expenseDate">
            <el-date-picker v-model="expenseForm.expenseDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择日期" />
          </el-form-item>
          <el-form-item label="票据张数">
            <el-input-number v-model="expenseForm.invoiceCount" :min="0" controls-position="right" />
          </el-form-item>
          <el-form-item label="预算科目">
            <el-input v-model="expenseForm.budgetSubject" placeholder="请输入预算科目" />
          </el-form-item>
          <el-form-item label="收款账户">
            <el-input v-model="expenseForm.bankAccount" placeholder="请输入收款账户" />
          </el-form-item>
          <el-form-item label="报销事由" prop="reason" class="form-item-full">
            <el-input v-model="expenseForm.reason" type="textarea" :rows="4" placeholder="请输入报销事由" />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="applyDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="submitting" @click="submitExpenseFromDialog">提交审批</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="sealApplyDialogVisible"
      :title="sealForm.submitCount ? '重新申请合同用印' : '申请合同用印'"
      width="960px"
      class="seal-apply-dialog"
      destroy-on-close
    >
      <div class="seal-dialog-body">
        <div class="seal-word-sheet">
          <div class="seal-document-meta">
            <span>{{ sealForm.code }}</span>
            <span>{{ sealForm.expectedSealDate || '待选择用印日期' }}</span>
          </div>
          <h2>合同用印审批单</h2>
          <el-form ref="sealFormRef" :model="sealForm" :rules="sealRules" label-width="0">
            <table class="seal-word-table">
              <tbody>
                <tr>
                  <th>申请部门</th>
                  <td>
                    <el-form-item prop="department">
                      <el-input v-model="sealForm.department" placeholder="请输入申请部门" />
                    </el-form-item>
                  </td>
                  <th>申请人</th>
                  <td>
                    <el-form-item prop="applicant">
                      <el-input v-model="sealForm.applicant" placeholder="请输入申请人" />
                    </el-form-item>
                  </td>
                </tr>
                <tr>
                  <th>合同名称</th>
                  <td>
                    <el-form-item prop="contractName">
                      <el-input v-model="sealForm.contractName" placeholder="请输入合同名称" />
                    </el-form-item>
                  </td>
                  <th>合同编号</th>
                  <td>
                    <el-form-item prop="contractCode">
                      <el-input v-model="sealForm.contractCode" placeholder="请输入合同编号" />
                    </el-form-item>
                  </td>
                </tr>
                <tr>
                  <th>对方单位</th>
                  <td colspan="3">
                    <el-form-item prop="counterparty">
                      <el-input v-model="sealForm.counterparty" placeholder="请输入对方单位" />
                    </el-form-item>
                  </td>
                </tr>
                <tr>
                  <th>合同类型</th>
                  <td>
                    <el-form-item prop="contractType">
                      <el-select v-model="sealForm.contractType" placeholder="请选择合同类型">
                        <el-option label="采购合同" value="采购合同" />
                        <el-option label="销售合同" value="销售合同" />
                        <el-option label="服务合同" value="服务合同" />
                        <el-option label="框架协议" value="框架协议" />
                      </el-select>
                    </el-form-item>
                  </td>
                  <th>合同金额</th>
                  <td>
                    <el-form-item prop="contractAmount">
                      <el-input-number v-model="sealForm.contractAmount" :min="0" :precision="2" controls-position="right" />
                    </el-form-item>
                  </td>
                </tr>
                <tr>
                  <th>用印类型</th>
                  <td>
                    <el-form-item prop="sealType">
                      <el-select v-model="sealForm.sealType" placeholder="请选择用印类型">
                        <el-option label="合同专用章" value="合同专用章" />
                        <el-option label="公章" value="公章" />
                        <el-option label="法人章" value="法人章" />
                      </el-select>
                    </el-form-item>
                  </td>
                  <th>申请份数</th>
                  <td>
                    <el-form-item prop="sealCount">
                      <el-input-number v-model="sealForm.sealCount" :min="1" controls-position="right" />
                    </el-form-item>
                  </td>
                </tr>
                <tr>
                  <th>期望用印日期</th>
                  <td>
                    <el-form-item prop="expectedSealDate">
                      <el-date-picker v-model="sealForm.expectedSealDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择日期" />
                    </el-form-item>
                  </td>
                  <th>紧急程度</th>
                  <td>
                    <el-form-item prop="urgency">
                      <el-select v-model="sealForm.urgency" placeholder="请选择紧急程度">
                        <el-option label="普通" value="普通" />
                        <el-option label="紧急" value="紧急" />
                      </el-select>
                    </el-form-item>
                  </td>
                </tr>
                <tr>
                  <th>风险等级</th>
                  <td>
                    <el-form-item prop="riskLevel">
                      <el-select v-model="sealForm.riskLevel" placeholder="请选择风险等级">
                        <el-option label="低" value="低" />
                        <el-option label="中" value="中" />
                        <el-option label="高" value="高" />
                      </el-select>
                    </el-form-item>
                  </td>
                  <th>附件清单</th>
                  <td>
                    <el-form-item prop="attachmentList">
                      <el-input v-model="sealForm.attachmentList" placeholder="如：合同正文、审批依据" />
                    </el-form-item>
                  </td>
                </tr>
                <tr>
                  <th>用印事由</th>
                  <td colspan="3">
                    <el-form-item prop="purpose">
                      <el-input v-model="sealForm.purpose" type="textarea" :rows="4" placeholder="请输入用印事由和背景说明" />
                    </el-form-item>
                  </td>
                </tr>
              </tbody>
            </table>
          </el-form>
        </div>
      </div>

      <template #footer>
        <el-button @click="sealApplyDialogVisible = false">关闭</el-button>
        <el-button type="success" :loading="submitting" @click="submitSealFromDialog">提交审批</el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="detailDrawerVisible"
      title="报销详情"
      size="min(1120px, 92vw)"
      destroy-on-close
      class="expense-detail-drawer"
    >
      <div v-loading="historyLoading || flowTreeLoading" class="expense-detail-layout">
        <section class="detail-hero">
          <div>
            <div class="detail-eyebrow">费用报销</div>
            <h3>{{ expenseForm.code }}</h3>
            <p>{{ expenseForm.reason || '暂无报销事由' }}</p>
          </div>
          <div class="detail-status">
            <el-tag :type="businessStatusTag(expenseForm.businessStatus)" size="large">{{ expenseForm.businessStatus }}</el-tag>
            <span>{{ currentApplyRecord?.currentNodeName || expenseForm.currentNodeName }}</span>
          </div>
        </section>

        <section class="detail-section">
          <div class="section-title-row">
            <div>
              <h4>申请快照</h4>
              <p>展示本次申请提交给流程的关键业务数据</p>
            </div>
            <el-tag effect="plain">{{ currentApplyRecord?.applyNo || '当前申请' }}</el-tag>
          </div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="申请人">{{ expenseForm.applicant || '-' }}</el-descriptions-item>
            <el-descriptions-item label="费用类型">{{ expenseForm.category || '-' }}</el-descriptions-item>
            <el-descriptions-item label="报销金额">{{ formatAmount(expenseForm.amount) }}</el-descriptions-item>
            <el-descriptions-item label="发生日期">{{ expenseForm.expenseDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="票据张数">{{ expenseForm.invoiceCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="预算科目">{{ expenseForm.budgetSubject || '-' }}</el-descriptions-item>
            <el-descriptions-item label="收款账户" :span="2">{{ expenseForm.bankAccount || '-' }}</el-descriptions-item>
            <el-descriptions-item label="报销事由" :span="2">{{ expenseForm.reason || '-' }}</el-descriptions-item>
          </el-descriptions>
        </section>

        <section class="detail-section">
          <div class="section-title-row">
            <div>
              <h4>审批流程</h4>
              <p>按流程设计器 JSON 渲染，条件分支和并行分支保持原结构</p>
            </div>
            <el-tag effect="plain">{{ detailWorkflowName }}</el-tag>
          </div>
          <div class="flow-tree-scroll">
            <ReadonlyWorkflowTree
              :node="detailWorkflowTree"
              :current-node-key="expenseForm.currentNodeKey"
              :visited-node-keys="detailVisitedNodeKeys"
              :status="expenseForm.businessStatus"
            />
          </div>
        </section>

        <section class="detail-section">
          <div class="section-title-row">
            <div>
              <h4>历史申请</h4>
              <p>同一业务单号可多次申请，每次申请对应一个独立流程实例</p>
            </div>
          </div>
          <el-timeline v-if="expenseForm.applyRecords.length" class="apply-timeline">
            <el-timeline-item
              v-for="record in expenseForm.applyRecords"
              :key="record.applyId"
              :timestamp="record.submittedAt"
              placement="top"
            >
              <div class="apply-record">
                <div class="apply-record-title">
                  <strong>{{ record.applyNo }}</strong>
                  <el-tag :type="businessStatusTag(record.status)" size="small">{{ record.status }}</el-tag>
                </div>
                <div class="apply-record-meta">
                  {{ formatAmount(record.snapshot.amount) }} · {{ record.snapshot.category || '-' }} · {{ record.currentNodeName }}
                </div>
                <div v-if="record.comment" class="apply-record-comment">{{ record.comment }}</div>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无申请记录" />
        </section>
      </div>
    </el-drawer>

    <el-drawer
      v-model="sealDetailDrawerVisible"
      title="合同用印详情"
      size="min(1180px, 94vw)"
      destroy-on-close
      class="expense-detail-drawer"
    >
      <div v-loading="historyLoading || flowTreeLoading" class="expense-detail-layout">
        <section class="detail-hero">
          <div>
            <div class="detail-eyebrow">合同用印</div>
            <h3>{{ sealForm.contractName || sealForm.code }}</h3>
            <p>{{ sealForm.counterparty || '暂无对方单位' }}</p>
          </div>
          <div class="detail-status">
            <el-tag :type="businessStatusTag(sealForm.businessStatus)" size="large">{{ sealForm.businessStatus }}</el-tag>
            <span>{{ currentSealApplyRecord?.currentNodeName || sealForm.currentNodeName }}</span>
          </div>
        </section>

        <section class="detail-section">
          <div class="section-title-row">
            <div>
              <h4>申请快照</h4>
              <p>以业务提交当时的关键数据渲染 Word 表格式审批单</p>
            </div>
            <el-tag effect="plain">{{ currentSealApplyRecord?.applyNo || '当前申请' }}</el-tag>
          </div>
          <DocumentTableApprovalDetail :context="sealReadonlyContext" />
        </section>

        <section class="detail-section">
          <div class="section-title-row">
            <div>
              <h4>审批流程</h4>
              <p>按流程设计器 JSON 渲染，条件分支和并行分支保持原结构</p>
            </div>
            <el-tag effect="plain">{{ detailWorkflowName }}</el-tag>
          </div>
          <div class="flow-tree-scroll">
            <ReadonlyWorkflowTree
              :node="detailWorkflowTree"
              :current-node-key="sealForm.currentNodeKey"
              :visited-node-keys="detailVisitedNodeKeys"
              :status="sealForm.businessStatus"
            />
          </div>
        </section>

        <section class="detail-section">
          <div class="section-title-row">
            <div>
              <h4>历史申请</h4>
              <p>同一业务单号可多次申请，每次申请对应一个独立流程实例</p>
            </div>
          </div>
          <el-timeline v-if="sealForm.applyRecords.length" class="apply-timeline">
            <el-timeline-item
              v-for="record in sealForm.applyRecords"
              :key="record.applyId"
              :timestamp="record.submittedAt"
              placement="top"
            >
              <div class="apply-record">
                <div class="apply-record-title">
                  <strong>{{ record.applyNo }}</strong>
                  <el-tag :type="businessStatusTag(record.status)" size="small">{{ record.status }}</el-tag>
                </div>
                <div class="apply-record-meta">
                  {{ record.snapshot.contractName }} · {{ formatAmount(record.snapshot.contractAmount) }} · {{ record.currentNodeName }}
                </div>
                <div v-if="record.comment" class="apply-record-comment">{{ record.comment }}</div>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无申请记录" />
        </section>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import {
  parseDesignerJson,
  workflowApi,
  type WorkflowBusinessApply,
  type WorkflowDefinition,
  type WorkflowDesignerNode,
} from '../../api/workflow';
import DocumentTableApprovalDetail from '../../components/business/DocumentTableApprovalDetail.vue';
import type { BusinessApprovalContext } from '../../components/businessApproval';
import ReadonlyWorkflowTree from './components/ReadonlyWorkflowTree.vue';

type ExpenseStatus = '草稿' | '审批中' | '已通过' | '已驳回' | '已结束';
type ExampleStatus = ExpenseStatus;

interface ExpenseSnapshot {
  applicant: string;
  category: string;
  amount: number;
  expenseDate: string;
  reason: string;
  invoiceCount: number;
  bankAccount: string;
  budgetSubject: string;
}

interface ExpenseApplyRecord {
  applyId: string;
  applyNo: string;
  processInstanceId?: string;
  submittedAt: string;
  status: ExpenseStatus;
  currentNodeName: string;
  currentNodeKey: string;
  comment?: string;
  snapshot: ExpenseSnapshot;
}

interface ExpenseExampleRow extends ExpenseSnapshot {
  id: string;
  code: string;
  businessStatus: ExpenseStatus;
  workflowName: string;
  currentProcessInstanceId?: string;
  currentApplyId?: string;
  currentNodeName: string;
  currentNodeKey: string;
  submitCount: number;
  startTime?: string;
  endTime?: string;
  applyRecords: ExpenseApplyRecord[];
}

interface SealSnapshot {
  applicant: string;
  department: string;
  contractName: string;
  contractCode: string;
  counterparty: string;
  contractType: string;
  contractAmount: number;
  sealType: string;
  sealCount: number;
  expectedSealDate: string;
  urgency: string;
  riskLevel: string;
  attachmentList: string;
  purpose: string;
  legalOpinion?: string;
  financeOpinion?: string;
  approvedSealCount?: number;
  sealKeeperOpinion?: string;
}

interface SealApplyRecord {
  applyId: string;
  applyNo: string;
  processInstanceId?: string;
  submittedAt: string;
  status: ExampleStatus;
  currentNodeName: string;
  currentNodeKey: string;
  comment?: string;
  snapshot: SealSnapshot;
}

interface SealExampleRow extends SealSnapshot {
  id: string;
  code: string;
  businessStatus: ExampleStatus;
  workflowName: string;
  currentProcessInstanceId?: string;
  currentApplyId?: string;
  currentNodeName: string;
  currentNodeKey: string;
  submitCount: number;
  startTime?: string;
  endTime?: string;
  applyRecords: SealApplyRecord[];
}

const router = useRouter();
const loading = ref(false);
const historyLoading = ref(false);
const flowTreeLoading = ref(false);
const submitting = ref(false);
const applyDialogVisible = ref(false);
const detailDrawerVisible = ref(false);
const sealApplyDialogVisible = ref(false);
const sealDetailDrawerVisible = ref(false);
const expenseFormRef = ref<FormInstance>();
const sealFormRef = ref<FormInstance>();
const workflowDefinitions = ref<WorkflowDefinition[]>([]);
const expenseList = ref<ExpenseExampleRow[]>([]);
const sealList = ref<SealExampleRow[]>([]);
const detailWorkflowTree = ref<WorkflowDesignerNode | null>(null);
const detailWorkflowName = ref('费用报销审批');
const detailVisitedNodeKeys = ref<string[]>([]);

const expenseForm = reactive<ExpenseExampleRow>(createEmptyExpense());
const sealForm = reactive<SealExampleRow>(createEmptySeal());

const expenseRules: FormRules = {
  applicant: [{ required: true, message: '请输入申请人', trigger: 'blur' }],
  category: [{ required: true, message: '请选择费用类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入报销金额', trigger: 'change' }],
  expenseDate: [{ required: true, message: '请选择发生日期', trigger: 'change' }],
  reason: [{ required: true, message: '请输入报销事由', trigger: 'blur' }],
};

const sealRules: FormRules = {
  applicant: [{ required: true, message: '请输入申请人', trigger: 'blur' }],
  department: [{ required: true, message: '请输入申请部门', trigger: 'blur' }],
  contractName: [{ required: true, message: '请输入合同名称', trigger: 'blur' }],
  contractCode: [{ required: true, message: '请输入合同编号', trigger: 'blur' }],
  counterparty: [{ required: true, message: '请输入对方单位', trigger: 'blur' }],
  contractType: [{ required: true, message: '请选择合同类型', trigger: 'change' }],
  contractAmount: [{ required: true, message: '请输入合同金额', trigger: 'change' }],
  sealType: [{ required: true, message: '请选择用印类型', trigger: 'change' }],
  sealCount: [{ required: true, message: '请输入申请份数', trigger: 'change' }],
  expectedSealDate: [{ required: true, message: '请选择期望用印日期', trigger: 'change' }],
  urgency: [{ required: true, message: '请选择紧急程度', trigger: 'change' }],
  riskLevel: [{ required: true, message: '请选择风险等级', trigger: 'change' }],
  purpose: [{ required: true, message: '请输入用印事由', trigger: 'blur' }],
};

const reimbursementFlow = computed(() => workflowDefinitions.value.find(item =>
  item.status === 'PUBLISHED'
  && item.id
  && /报销|费用|expense|reimburse/i.test(`${item.definitionName}${item.definitionKey}${item.formCode || ''}`),
));
const sealFlow = computed(() => workflowDefinitions.value.find(item =>
  item.status === 'PUBLISHED'
  && item.id
  && /合同|用印|印章|seal|contract/i.test(`${item.definitionName}${item.definitionKey}${item.formCode || ''}`),
));

const expenseRows = computed(() => expenseList.value);
const sealRows = computed(() => sealList.value);

const currentApplyRecord = computed(() => expenseForm.applyRecords.find(record => record.applyId === expenseForm.currentApplyId)
  || expenseForm.applyRecords[expenseForm.applyRecords.length - 1]);
const currentSealApplyRecord = computed(() => sealForm.applyRecords.find(record => record.applyId === sealForm.currentApplyId)
  || sealForm.applyRecords[sealForm.applyRecords.length - 1]);
const sealReadonlyContext = computed<BusinessApprovalContext>(() => ({
  businessType: 'CONTRACT_SEAL_APPROVAL',
  businessKey: sealForm.code,
  applyId: sealForm.currentApplyId,
  processInstanceId: sealForm.currentProcessInstanceId,
  taskDefinitionKey: sealForm.currentNodeKey,
  nodeName: sealForm.currentNodeName,
  readonly: true,
  variables: sealForm,
  permissions: {},
}));

const integrationSteps = [
  { index: '01', title: '保存业务数据', desc: '业务侧保存当前报销单和申请快照。' },
  { index: '02', title: '发起工作流', desc: '传业务主键、申请ID和流程判断变量。' },
  { index: '03', title: '展示当前节点', desc: '列表读取运行中任务的节点名称。' },
  { index: '04', title: '事件回写业务', desc: '流程结束后由业务订阅事件更新状态。' },
];

async function loadData() {
  loading.value = true;
  try {
    const [definitionsResult, expenseApplyResult, sealApplyResult] = await Promise.all([
      workflowApi.definitionsPage({ pageNum: 1, pageSize: 100, status: 'PUBLISHED' }),
      workflowApi.businessAppliesPage({ pageNum: 1, pageSize: 100, businessType: 'EXPENSE_REIMBURSEMENT', latestOnly: true }),
      workflowApi.businessAppliesPage({ pageNum: 1, pageSize: 100, businessType: 'CONTRACT_SEAL_APPROVAL', latestOnly: true }),
    ]);
    workflowDefinitions.value = definitionsResult.list;
    expenseList.value = buildExpenseRows(expenseApplyResult.list);
    sealList.value = buildSealRows(sealApplyResult.list);
  } finally {
    loading.value = false;
  }
}

function openApplyDialog(row?: ExpenseExampleRow) {
  Object.assign(expenseForm, cloneExpense(row || createEmptyExpense()));
  applyDialogVisible.value = true;
}

async function openDetailDialog(row: ExpenseExampleRow) {
  Object.assign(expenseForm, cloneExpense(row));
  detailWorkflowName.value = row.workflowName || '费用报销审批';
  detailWorkflowTree.value = null;
  detailVisitedNodeKeys.value = [];
  detailDrawerVisible.value = true;
  if (row?.code) {
    void loadExpenseHistory(row.code);
  }
  void loadDetailWorkflowTree(row);
}

function openSealApplyDialog(row?: SealExampleRow) {
  Object.assign(sealForm, cloneSeal(row || createEmptySeal()));
  sealApplyDialogVisible.value = true;
}

async function openSealDetailDialog(row: SealExampleRow) {
  Object.assign(sealForm, cloneSeal(row));
  detailWorkflowName.value = row.workflowName || '合同用印审批';
  detailWorkflowTree.value = null;
  detailVisitedNodeKeys.value = [];
  sealDetailDrawerVisible.value = true;
  if (row?.code) {
    void loadSealHistory(row.code);
  }
  void loadDetailWorkflowTree(row);
}

function createEmptyExpense(): ExpenseExampleRow {
  return createExpenseRow({
    id: '',
    code: nextExpenseCode(),
    applicant: 'admin',
    category: '',
    amount: 0,
    expenseDate: '',
    reason: '',
    businessStatus: '草稿',
    currentNodeName: '未发起',
    currentNodeKey: 'draft',
    submitCount: 0,
    applyRecords: [],
  });
}

function createEmptySeal(): SealExampleRow {
  return createSealRow({
    id: '',
    code: nextSealCode(),
    applicant: 'admin',
    department: '总经办',
    contractName: '',
    contractCode: nextContractCode(),
    counterparty: '',
    contractType: '采购合同',
    contractAmount: 0,
    sealType: '合同专用章',
    sealCount: 2,
    expectedSealDate: '',
    urgency: '普通',
    riskLevel: '中',
    attachmentList: '合同正文、合同评审记录',
    purpose: '',
    businessStatus: '草稿',
    currentNodeName: '未发起',
    currentNodeKey: 'draft',
    submitCount: 0,
    applyRecords: [],
  });
}

function createExpenseRow(row: Partial<ExpenseExampleRow> & Pick<ExpenseExampleRow,
  'id' | 'code' | 'applicant' | 'category' | 'amount' | 'expenseDate' | 'reason' | 'businessStatus'
>): ExpenseExampleRow {
  return {
    invoiceCount: Number(row.invoiceCount || 0),
    bankAccount: row.bankAccount || '',
    budgetSubject: row.budgetSubject || '',
    workflowName: row.workflowName || '费用报销审批',
    currentProcessInstanceId: row.currentProcessInstanceId,
    currentApplyId: row.currentApplyId,
    currentNodeName: row.currentNodeName || '未发起',
    currentNodeKey: row.currentNodeKey || 'draft',
    submitCount: row.submitCount || 0,
    startTime: row.startTime,
    endTime: row.endTime,
    applyRecords: row.applyRecords || [],
    ...row,
  };
}

function createApplyRecord(
  applyId: string,
  applyNo: string,
  processInstanceId: string | undefined,
  status: ExpenseStatus,
  currentNodeName: string,
  currentNodeKey: string,
  submittedAt: string,
  snapshot: ExpenseSnapshot,
  comment?: string,
): ExpenseApplyRecord {
  return {
    applyId,
    applyNo,
    processInstanceId,
    submittedAt,
    status,
    currentNodeName,
    currentNodeKey,
    snapshot,
    comment,
  };
}

function createSealRow(row: Partial<SealExampleRow> & Pick<SealExampleRow,
  'id' | 'code' | 'applicant' | 'department' | 'contractName' | 'contractCode' | 'counterparty' | 'contractType'
  | 'contractAmount' | 'sealType' | 'sealCount' | 'expectedSealDate' | 'urgency' | 'riskLevel' | 'attachmentList'
  | 'purpose' | 'businessStatus'
>): SealExampleRow {
  return {
    workflowName: row.workflowName || '合同用印审批',
    currentProcessInstanceId: row.currentProcessInstanceId,
    currentApplyId: row.currentApplyId,
    currentNodeName: row.currentNodeName || '未发起',
    currentNodeKey: row.currentNodeKey || 'draft',
    submitCount: row.submitCount || 0,
    startTime: row.startTime,
    endTime: row.endTime,
    applyRecords: row.applyRecords || [],
    legalOpinion: row.legalOpinion || '',
    financeOpinion: row.financeOpinion || '',
    approvedSealCount: Number(row.approvedSealCount || row.sealCount || 0),
    sealKeeperOpinion: row.sealKeeperOpinion || '',
    ...row,
  };
}

function createSealApplyRecord(
  applyId: string,
  applyNo: string,
  processInstanceId: string | undefined,
  status: ExampleStatus,
  currentNodeName: string,
  currentNodeKey: string,
  submittedAt: string,
  snapshot: SealSnapshot,
  comment?: string,
): SealApplyRecord {
  return {
    applyId,
    applyNo,
    processInstanceId,
    submittedAt,
    status,
    currentNodeName,
    currentNodeKey,
    snapshot,
    comment,
  };
}

function nextExpenseCode() {
  const stamp = new Date();
  const month = String(stamp.getMonth() + 1).padStart(2, '0');
  const day = String(stamp.getDate()).padStart(2, '0');
  return `EXP-${stamp.getFullYear()}${month}${day}-${String(Date.now()).slice(-6)}`;
}

function nextSealCode() {
  const stamp = new Date();
  const month = String(stamp.getMonth() + 1).padStart(2, '0');
  const day = String(stamp.getDate()).padStart(2, '0');
  return `SEAL-${stamp.getFullYear()}${month}${day}-${String(Date.now()).slice(-6)}`;
}

function nextContractCode() {
  const stamp = new Date();
  const month = String(stamp.getMonth() + 1).padStart(2, '0');
  return `HT-${stamp.getFullYear()}${month}-${String(Date.now()).slice(-5)}`;
}

function formatAmount(value: number) {
  return `¥${Number(value || 0).toFixed(2)}`;
}

function businessStatusTag(status: ExpenseStatus) {
  if (status === '已通过') return 'success';
  if (status === '已驳回') return 'danger';
  if (status === '审批中') return 'warning';
  return 'info';
}

function canSubmit(row: ExpenseExampleRow | SealExampleRow) {
  return row.businessStatus === '已驳回';
}

async function submitExpenseFromDialog() {
  const valid = await expenseFormRef.value?.validate().catch(() => false);
  if (valid === false) return;
  await submitExpense(cloneExpense(expenseForm));
}

async function submitSealFromDialog() {
  const valid = await sealFormRef.value?.validate().catch(() => false);
  if (valid === false) return;
  await submitSeal(cloneSeal(sealForm));
}

async function submitExpense(row: ExpenseExampleRow) {
  if (!reimbursementFlow.value?.id) {
    ElMessage.warning('未找到已发布的费用报销流程，请先在流程管理中创建并发布报销流程');
    return;
  }
  submitting.value = true;
  try {
    const applyId = `APPLY-${row.code}-${String(row.submitCount + 1).padStart(3, '0')}`;
    await workflowApi.startProcess({
      definitionId: reimbursementFlow.value.id,
      businessType: 'EXPENSE_REIMBURSEMENT',
      businessKey: row.code,
      renderMode: 'CUSTOM_PAGE',
      applyPageKey: 'workflow.expense.apply',
      approvePageKey: 'workflow.expense.approve',
      snapshotRef: `EXPENSE_REIMBURSEMENT:${applyId}`,
      variables: {
        businessType: 'EXPENSE_REIMBURSEMENT',
        businessKey: row.code,
        applyId,
        title: `费用报销 ${row.code}`,
        summary: `${row.category} ${formatAmount(row.amount)}`,
        expenseCode: row.code,
        applicant: row.applicant,
        category: row.category,
        amount: row.amount,
        expenseDate: row.expenseDate,
        reason: row.reason,
        invoiceCount: row.invoiceCount || 1,
        bankAccount: row.bankAccount,
        budgetSubject: row.budgetSubject,
        businessPermissions: {
          manager_approve: {
            expenseReason: 'READONLY',
            invoiceInfo: 'READONLY',
            paymentInfo: 'HIDDEN',
            financeReview: 'HIDDEN',
          },
          finance_review: {
            expenseReason: 'READONLY',
            invoiceInfo: 'READONLY',
            paymentInfo: 'READONLY',
            financeReview: 'EDITABLE',
          },
        },
      },
    });
    ElMessage.success('报销申请已提交审批');
    applyDialogVisible.value = false;
    await loadData();
  } finally {
    submitting.value = false;
  }
}

async function submitSeal(row: SealExampleRow) {
  if (!sealFlow.value?.id) {
    ElMessage.warning('未找到已发布的合同用印流程，请先在流程管理中创建并发布合同用印流程');
    return;
  }
  submitting.value = true;
  try {
    const applyId = `APPLY-${row.code}-${String(row.submitCount + 1).padStart(3, '0')}`;
    await workflowApi.startProcess({
      definitionId: sealFlow.value.id,
      businessType: 'CONTRACT_SEAL_APPROVAL',
      businessKey: row.code,
      renderMode: 'CUSTOM_PAGE',
      applyPageKey: 'workflow.contractSeal.apply',
      approvePageKey: 'workflow.contractSeal.approve',
      snapshotRef: `CONTRACT_SEAL_APPROVAL:${applyId}`,
      variables: {
        businessType: 'CONTRACT_SEAL_APPROVAL',
        businessKey: row.code,
        applyId,
        title: `合同用印 ${row.contractName}`,
        summary: `${row.counterparty} ${formatAmount(row.contractAmount)} ${row.sealType}`,
        applicant: row.applicant,
        department: row.department,
        contractName: row.contractName,
        contractCode: row.contractCode,
        counterparty: row.counterparty,
        contractType: row.contractType,
        contractAmount: row.contractAmount,
        sealType: row.sealType,
        sealCount: row.sealCount,
        expectedSealDate: row.expectedSealDate,
        urgency: row.urgency,
        riskLevel: row.riskLevel,
        attachmentList: row.attachmentList,
        purpose: row.purpose,
        approvedSealCount: row.sealCount,
        businessPermissions: {
          dept_manager_approve: {
            basicInfo: 'READONLY',
            contractInfo: 'READONLY',
            sealInfo: 'READONLY',
            riskInfo: 'READONLY',
            purpose: 'READONLY',
            legalOpinion: 'HIDDEN',
            financeOpinion: 'HIDDEN',
            sealKeeperOpinion: 'HIDDEN',
          },
          legal_review: {
            basicInfo: 'READONLY',
            contractInfo: 'READONLY',
            sealInfo: 'READONLY',
            riskInfo: 'READONLY',
            purpose: 'READONLY',
            legalOpinion: 'EDITABLE',
            financeOpinion: 'HIDDEN',
            sealKeeperOpinion: 'HIDDEN',
          },
          finance_review: {
            basicInfo: 'READONLY',
            contractInfo: 'READONLY',
            sealInfo: 'READONLY',
            riskInfo: 'READONLY',
            purpose: 'READONLY',
            legalOpinion: 'READONLY',
            financeOpinion: 'EDITABLE',
            sealKeeperOpinion: 'HIDDEN',
          },
          seal_keeper: {
            basicInfo: 'READONLY',
            contractInfo: 'READONLY',
            sealInfo: 'READONLY',
            riskInfo: 'READONLY',
            purpose: 'READONLY',
            legalOpinion: 'READONLY',
            financeOpinion: 'READONLY',
            sealKeeperOpinion: 'EDITABLE',
          },
        },
      },
    });
    ElMessage.success('合同用印申请已提交审批');
    sealApplyDialogVisible.value = false;
    await loadData();
  } finally {
    submitting.value = false;
  }
}

function buildExpenseRows(applies: WorkflowBusinessApply[]) {
  return applies
    .filter(apply => apply.businessType === 'EXPENSE_REIMBURSEMENT')
    .map(buildExpenseRowFromApply)
    .sort((a, b) => String(b.startTime || '').localeCompare(String(a.startTime || '')));
}

function buildSealRows(applies: WorkflowBusinessApply[]) {
  return applies
    .filter(apply => apply.businessType === 'CONTRACT_SEAL_APPROVAL')
    .map(buildSealRowFromApply)
    .sort((a, b) => String(b.startTime || '').localeCompare(String(a.startTime || '')));
}

function buildExpenseRowFromApply(apply: WorkflowBusinessApply) {
  const variables = apply.variables || {};
  const status = normalizeBusinessStatus(apply.applyStatusName || apply.applyStatus);
  return createExpenseRow({
    id: String(apply.processInstanceId || apply.id),
    code: String(apply.businessKey || variables.businessKey || variables.expenseCode || apply.id),
    applicant: String(variables.applicant || apply.applicantName || '-'),
    category: String(variables.category || ''),
    amount: Number(variables.amount || 0),
    expenseDate: String(variables.expenseDate || ''),
    reason: String(variables.reason || variables.summary || apply.applySummary || ''),
    invoiceCount: Number(variables.invoiceCount || 0),
    bankAccount: String(variables.bankAccount || ''),
    budgetSubject: String(variables.budgetSubject || ''),
    businessStatus: status,
    workflowName: apply.processName || apply.applyTitle || '费用报销审批',
    currentProcessInstanceId: apply.processInstanceId,
    currentApplyId: String(apply.id || variables.applyId || ''),
    currentNodeName: currentNodeNameOfApply(apply, status),
    currentNodeKey: firstCurrentTaskKey(apply),
    submitCount: 1,
    startTime: apply.createdAt,
    endTime: isTerminalStatus(status) ? apply.updatedAt : '',
    applyRecords: [],
  });
}

function buildSealRowFromApply(apply: WorkflowBusinessApply) {
  const variables = apply.variables || {};
  const status = normalizeBusinessStatus(apply.applyStatusName || apply.applyStatus);
  return createSealRow({
    id: String(apply.processInstanceId || apply.id),
    code: String(apply.businessKey || variables.businessKey || apply.id),
    applicant: String(variables.applicant || apply.applicantName || '-'),
    department: String(variables.department || ''),
    contractName: String(variables.contractName || variables.title || apply.applyTitle || ''),
    contractCode: String(variables.contractCode || ''),
    counterparty: String(variables.counterparty || ''),
    contractType: String(variables.contractType || ''),
    contractAmount: Number(variables.contractAmount || 0),
    sealType: String(variables.sealType || ''),
    sealCount: Number(variables.sealCount || 0),
    expectedSealDate: String(variables.expectedSealDate || ''),
    urgency: String(variables.urgency || ''),
    riskLevel: String(variables.riskLevel || ''),
    attachmentList: String(variables.attachmentList || ''),
    purpose: String(variables.purpose || variables.summary || apply.applySummary || ''),
    legalOpinion: String(variables.legalOpinion || ''),
    financeOpinion: String(variables.financeOpinion || ''),
    approvedSealCount: Number(variables.approvedSealCount || variables.sealCount || 0),
    sealKeeperOpinion: String(variables.sealKeeperOpinion || ''),
    businessStatus: status,
    workflowName: apply.processName || apply.applyTitle || '合同用印审批',
    currentProcessInstanceId: apply.processInstanceId,
    currentApplyId: String(apply.id || variables.applyId || ''),
    currentNodeName: currentNodeNameOfApply(apply, status),
    currentNodeKey: firstCurrentTaskKey(apply),
    submitCount: 1,
    startTime: apply.createdAt,
    endTime: isTerminalStatus(status) ? apply.updatedAt : '',
    applyRecords: [],
  });
}

async function loadDetailWorkflowTree(row: ExpenseExampleRow | SealExampleRow) {
  flowTreeLoading.value = true;
  try {
    const definition = await resolveWorkflowDefinition(row);
    detailWorkflowName.value = definition?.definitionName || row.workflowName || '费用报销审批';
    detailWorkflowTree.value = parseDesignerJson(definition?.designerJson);
  } finally {
    flowTreeLoading.value = false;
  }
}

async function resolveWorkflowDefinition(row: ExpenseExampleRow | SealExampleRow) {
  const matched = workflowDefinitions.value.find(item =>
    item.id
    && (
      item.definitionName === row.workflowName
      || item.definitionKey === row.workflowName
      || workflowPatternOf(row).test(`${item.definitionName}${item.definitionKey}${item.formCode || ''}`)
    ),
  );
  if (!matched?.id) {
    return isSealExampleRow(row) ? sealFlow.value : reimbursementFlow.value;
  }
  try {
    return await workflowApi.definitionDetail(matched.id);
  } catch {
    return matched;
  }
}

async function loadExpenseHistory(businessKey: string) {
  historyLoading.value = true;
  try {
    const history = await workflowApi.businessApplyHistory('EXPENSE_REIMBURSEMENT', businessKey, { pageNum: 1, pageSize: 50 });
    const ordered = sortAppliesAsc(history.list);
    const records = ordered.map((apply, index) => buildApplyRecordFromApply(apply, index + 1));
    expenseForm.applyRecords = records;
    const latest = records[records.length - 1];
    if (latest) {
      expenseForm.currentProcessInstanceId = latest.processInstanceId;
      expenseForm.currentApplyId = latest.applyId;
      expenseForm.currentNodeName = latest.currentNodeName;
      expenseForm.currentNodeKey = latest.currentNodeKey;
      expenseForm.businessStatus = latest.status;
      expenseForm.submitCount = records.length;
    }
    detailVisitedNodeKeys.value = uniqueKeys([
      ...currentTaskKeysFromHistory(ordered),
      ...await loadVisitedNodeKeys(expenseForm.currentProcessInstanceId),
    ]);
  } finally {
    historyLoading.value = false;
  }
}

async function loadSealHistory(businessKey: string) {
  historyLoading.value = true;
  try {
    const history = await workflowApi.businessApplyHistory('CONTRACT_SEAL_APPROVAL', businessKey, { pageNum: 1, pageSize: 50 });
    const ordered = sortAppliesAsc(history.list);
    const records = ordered.map((apply, index) => buildSealApplyRecordFromApply(apply, index + 1));
    sealForm.applyRecords = records;
    const latest = records[records.length - 1];
    if (latest) {
      sealForm.currentProcessInstanceId = latest.processInstanceId;
      sealForm.currentApplyId = latest.applyId;
      sealForm.currentNodeName = latest.currentNodeName;
      sealForm.currentNodeKey = latest.currentNodeKey;
      sealForm.businessStatus = latest.status;
      sealForm.submitCount = records.length;
    }
    detailVisitedNodeKeys.value = uniqueKeys([
      ...currentTaskKeysFromHistory(ordered),
      ...await loadVisitedNodeKeys(sealForm.currentProcessInstanceId),
    ]);
  } finally {
    historyLoading.value = false;
  }
}

async function loadVisitedNodeKeys(processInstanceId?: string) {
  if (!processInstanceId) return [];
  const detail = await workflowApi.processDetail(processInstanceId);
  return Array.from(new Set((detail.records || [])
    .map(record => record.taskDefinitionKey)
    .filter((key): key is string => Boolean(key))));
}

function buildApplyRecordFromApply(apply: WorkflowBusinessApply, index: number) {
  const variables = apply.variables || {};
  const status = normalizeBusinessStatus(apply.applyStatusName || apply.applyStatus);
  return createApplyRecord(
    String(apply.id || variables.applyId || `APPLY-${apply.processInstanceId || apply.businessKey}`),
    `第 ${index} 次申请`,
    apply.processInstanceId,
    status,
    currentNodeNameOfApply(apply, status),
    firstCurrentTaskKey(apply),
    apply.createdAt || '',
    {
      applicant: String(variables.applicant || apply.applicantName || '-'),
      category: String(variables.category || ''),
      amount: Number(variables.amount || 0),
      expenseDate: String(variables.expenseDate || ''),
      reason: String(variables.reason || variables.summary || apply.applySummary || ''),
      invoiceCount: Number(variables.invoiceCount || 0),
      bankAccount: String(variables.bankAccount || ''),
      budgetSubject: String(variables.budgetSubject || ''),
    },
  );
}

function buildSealApplyRecordFromApply(apply: WorkflowBusinessApply, index: number) {
  const variables = apply.variables || {};
  const status = normalizeBusinessStatus(apply.applyStatusName || apply.applyStatus);
  return createSealApplyRecord(
    String(apply.id || variables.applyId || `APPLY-${apply.processInstanceId || apply.businessKey}`),
    `第 ${index} 次申请`,
    apply.processInstanceId,
    status,
    currentNodeNameOfApply(apply, status),
    firstCurrentTaskKey(apply),
    apply.createdAt || '',
    {
      applicant: String(variables.applicant || apply.applicantName || '-'),
      department: String(variables.department || ''),
      contractName: String(variables.contractName || ''),
      contractCode: String(variables.contractCode || ''),
      counterparty: String(variables.counterparty || ''),
      contractType: String(variables.contractType || ''),
      contractAmount: Number(variables.contractAmount || 0),
      sealType: String(variables.sealType || ''),
      sealCount: Number(variables.sealCount || 0),
      expectedSealDate: String(variables.expectedSealDate || ''),
      urgency: String(variables.urgency || ''),
      riskLevel: String(variables.riskLevel || ''),
      attachmentList: String(variables.attachmentList || ''),
      purpose: String(variables.purpose || variables.summary || apply.applySummary || ''),
      legalOpinion: String(variables.legalOpinion || ''),
      financeOpinion: String(variables.financeOpinion || ''),
      approvedSealCount: Number(variables.approvedSealCount || variables.sealCount || 0),
      sealKeeperOpinion: String(variables.sealKeeperOpinion || ''),
    },
  );
}

function normalizeBusinessStatus(status?: string): ExpenseStatus {
  if (status === '已驳回' || status === '已拒绝' || status === 'REJECTED') return '已驳回';
  if (status === '已完成' || status === 'COMPLETED') return '已通过';
  if (status === '已结束' || status === 'ENDED') return '已结束';
  if (status === '草稿') return '草稿';
  return '审批中';
}

function sortAppliesAsc(applies: WorkflowBusinessApply[]) {
  return applies.slice().sort((a, b) => String(a.createdAt || '').localeCompare(String(b.createdAt || '')));
}

function currentNodeNameOfApply(apply: WorkflowBusinessApply, status: ExpenseStatus) {
  if (status === '审批中') {
    return apply.currentTaskNames || apply.currentTasks?.map(task => task.taskName).filter(Boolean).join(',') || '审批中';
  }
  if (status === '已驳回') return '已驳回';
  if (status === '已通过' || status === '已结束') return '已结束';
  return '未发起';
}

function firstCurrentTaskKey(apply: WorkflowBusinessApply) {
  return apply.currentTaskDefinitionKeys
    || apply.currentTasks?.map(task => task.taskDefinitionKey).filter(Boolean).join(',')
    || '';
}

function currentTaskKeysFromHistory(applies: WorkflowBusinessApply[]) {
  return applies.map(apply => firstCurrentTaskKey(apply)).filter(Boolean);
}

function uniqueKeys(keys: string[]) {
  return Array.from(new Set(keys
    .flatMap(key => String(key).split(','))
    .map(key => key.trim())
    .filter(Boolean)));
}

function isTerminalStatus(status: ExpenseStatus) {
  return status === '已通过' || status === '已驳回' || status === '已结束';
}

function viewWorkflow(row: ExpenseExampleRow | SealExampleRow) {
  if (!row.currentProcessInstanceId) {
    ElMessage.info('当前业务单据还未发起流程');
    return;
  }
  router.push({
    path: '/workflow/task/detail',
    query: {
      mode: 'view',
      from: 'initiated',
      processInstanceId: row.currentProcessInstanceId,
    },
  });
}

function cloneExpense(row: ExpenseExampleRow): ExpenseExampleRow {
  return JSON.parse(JSON.stringify(row));
}

function cloneSeal(row: SealExampleRow): SealExampleRow {
  return JSON.parse(JSON.stringify(row));
}

function isSealExampleRow(row: ExpenseExampleRow | SealExampleRow): row is SealExampleRow {
  return 'contractName' in row;
}

function workflowPatternOf(row: ExpenseExampleRow | SealExampleRow) {
  return isSealExampleRow(row) ? /合同|用印|印章|seal|contract/i : /报销|费用|expense|reimburse/i;
}

onMounted(loadData);
</script>

<style scoped>
.workflow-business-example-page {
  padding: 0;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-title {
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 600;
}

.page-subtitle {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.example-overview {
  display: grid;
  grid-template-columns: minmax(260px, 0.9fr) minmax(420px, 1.4fr);
  gap: 24px;
  margin-bottom: 18px;
  padding: 18px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-extra-light);
}

.example-intro h3 {
  margin: 6px 0 8px;
  color: var(--el-text-color-primary);
  font-size: 18px;
}

.example-intro p {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.7;
}

.intro-eyebrow {
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 600;
}

.example-flow {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.flow-step {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 10px;
  align-items: start;
  min-width: 0;
}

.step-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 700;
}

.flow-step strong,
.flow-step small {
  display: block;
}

.flow-step strong {
  color: var(--el-text-color-primary);
  font-size: 14px;
}

.flow-step small {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.example-table {
  width: 100%;
}

.business-table-divider {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 22px 0 12px;
  padding-top: 18px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.business-table-divider h3 {
  margin: 0;
  color: var(--el-text-color-primary);
  font-size: 16px;
}

.business-table-divider p {
  margin: 4px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.current-node {
  color: var(--el-text-color-primary);
  font-weight: 600;
}

.expense-apply-dialog :deep(.el-dialog) {
  max-width: 92vw;
}

.expense-apply-layout {
  max-height: min(66vh, 620px);
  overflow: auto;
  padding-right: 4px;
}

.expense-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}

.expense-form .form-item-full {
  grid-column: 1 / -1;
}

.expense-form :deep(.el-select),
.expense-form :deep(.el-date-editor),
.expense-form :deep(.el-input-number) {
  width: 100%;
}

.seal-apply-dialog :deep(.el-dialog) {
  max-width: 94vw;
}

.seal-dialog-body {
  max-height: min(68vh, 700px);
  overflow: auto;
  background: #f5f5f1;
  padding: 16px;
}

.seal-word-sheet {
  width: min(900px, 100%);
  margin: 0 auto;
  padding: 22px 26px;
  border: 1px solid #d8dce5;
  background: #fffdf8;
  box-shadow: 0 8px 28px rgba(31, 41, 55, 0.08);
}

.seal-document-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: #6b7280;
  font-size: 12px;
}

.seal-word-sheet h2 {
  margin: 8px 0 18px;
  text-align: center;
  color: #111827;
  font-family: SimSun, "Songti SC", serif;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0;
}

.seal-word-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  border: 2px solid #1f2937;
  font-family: SimSun, "Songti SC", serif;
}

.seal-word-table th,
.seal-word-table td {
  min-height: 44px;
  padding: 8px 10px;
  border: 1px solid #1f2937;
  vertical-align: middle;
  line-height: 1.6;
  word-break: break-word;
}

.seal-word-table th {
  width: 118px;
  background: #f5f0e6;
  color: #111827;
  font-weight: 700;
  text-align: center;
}

.seal-word-table td {
  background: #fffefa;
}

.seal-word-table :deep(.el-form-item) {
  margin-bottom: 0;
}

.seal-word-table :deep(.el-input__wrapper),
.seal-word-table :deep(.el-select__wrapper),
.seal-word-table :deep(.el-textarea__inner) {
  box-shadow: none;
  border-radius: 0;
  background: #fffefa;
  font-family: SimSun, "Songti SC", serif;
}

.seal-word-table :deep(.el-select),
.seal-word-table :deep(.el-input-number),
.seal-word-table :deep(.el-date-editor) {
  width: 100%;
}

.expense-detail-drawer :deep(.el-drawer__body) {
  padding: 0;
  background: var(--el-fill-color-extra-light);
}

.expense-detail-layout {
  display: grid;
  gap: 14px;
  padding: 16px;
}

.detail-hero,
.detail-section {
  padding: 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.detail-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.detail-eyebrow {
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 600;
}

.detail-hero h3 {
  margin: 4px 0 6px;
  color: var(--el-text-color-primary);
  font-size: 20px;
  font-weight: 650;
}

.detail-hero p {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.6;
}

.detail-status {
  display: flex;
  align-items: flex-end;
  flex-direction: column;
  gap: 8px;
  min-width: 140px;
}

.detail-status span {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.section-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.section-title-row h4 {
  margin: 0;
  color: var(--el-text-color-primary);
  font-size: 15px;
}

.section-title-row p {
  margin: 4px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.flow-tree-scroll {
  overflow: auto;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.apply-record-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.apply-record-meta,
.apply-record-comment {
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

@media (max-width: 980px) {
  .example-overview {
    grid-template-columns: 1fr;
  }

  .example-flow {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .card-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .example-flow {
    grid-template-columns: 1fr;
  }

  .business-table-divider {
    align-items: flex-start;
    flex-direction: column;
  }

  .expense-form {
    grid-template-columns: 1fr;
  }

  .detail-hero {
    flex-direction: column;
  }

  .detail-status {
    align-items: flex-start;
  }
}
</style>
