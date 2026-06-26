<template>
  <div class="payment-offline-collections">
    <section class="payment-offline-collections__header">
      <div>
        <h3>线下收款</h3>
        <p>查询线下转账收款单、批量导入银行流水、确认匹配到账和处理线下退款。</p>
      </div>
    </section>

    <el-tabs v-model="activeTab" class="payment-offline-collections__tabs">
      <el-tab-pane label="收款订单" name="collections" />
      <el-tab-pane label="银行流水" name="bankStatements" />
    </el-tabs>

    <section class="payment-offline-collections__toolbar">
      <el-form v-if="activeTab === 'collections'" :inline="true" :model="query">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="收款单号 / 支付单号 / 业务单号 / 对账码 / 备注"
            clearable
            @keyup.enter="loadRows"
            @clear="loadRows"
          />
        </el-form-item>
        <el-form-item label="收款状态">
          <el-select v-model="query.statusCode" clearable placeholder="全部状态" @change="applyFilters">
            <el-option
              v-for="status in statusOptions"
              :key="status.statusCode"
              :label="status.statusName"
              :value="status.statusCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item class="payment-offline-collections__filter-actions">
          <el-button type="primary" :icon="Search" @click="loadRows">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
      <el-form v-else :inline="true" :model="bankStatementQuery">
        <el-form-item label="关键字">
          <el-input
            v-model="bankStatementQuery.keyword"
            placeholder="批次号 / 文件名 / 摘要 / 导入人 / 账号"
            clearable
            @keyup.enter="loadBankStatements"
            @clear="loadBankStatements"
          />
        </el-form-item>
        <el-form-item label="批次状态">
          <el-select v-model="bankStatementQuery.statusCode" clearable placeholder="全部状态" @change="applyBankStatementFilters">
            <el-option
              v-for="status in bankStatementStatusOptions"
              :key="status.code || status.statusCode"
              :label="status.label || status.statusName"
              :value="status.code || status.statusCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item class="payment-offline-collections__filter-actions">
          <el-button type="primary" :icon="Search" @click="loadBankStatements">查询</el-button>
          <el-button :icon="Refresh" @click="resetBankStatementQuery">重置</el-button>
          <el-upload
            class="payment-offline-collections__upload"
            :auto-upload="false"
            :show-file-list="false"
            accept=".xls,.xlsx"
            :disabled="importingBankStatement"
            :on-change="handleBankStatementFile"
          >
            <el-button type="success" :loading="importingBankStatement">导入银行流水</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
    </section>

    <el-alert
      v-if="activeTab === 'collections' && errorMessage"
      :title="errorMessage"
      type="error"
      show-icon
      :closable="false"
    >
      <template #default>
        <el-button link type="primary" :icon="Refresh" @click="loadRows">重新加载</el-button>
      </template>
    </el-alert>
    <el-alert
      v-if="activeTab === 'bankStatements' && bankStatementErrorMessage"
      :title="bankStatementErrorMessage"
      type="error"
      show-icon
      :closable="false"
    >
      <template #default>
        <el-button link type="primary" :icon="Refresh" @click="loadBankStatements">重新加载</el-button>
      </template>
    </el-alert>

    <el-table
      v-if="activeTab === 'collections'"
      :data="rows"
      v-loading="loading"
      row-key="id"
      stripe
      highlight-current-row
      class="payment-offline-collections__table"
    >
      <template #empty>
        <el-empty :description="emptyDescription" />
      </template>
      <el-table-column prop="offlineCollectionNo" label="线下收款单号" min-width="190" show-overflow-tooltip />
      <el-table-column prop="payOrderNo" label="支付订单号" min-width="190" show-overflow-tooltip />
      <el-table-column prop="bizOrderNo" label="业务订单号" min-width="180" show-overflow-tooltip />
      <el-table-column prop="subjectName" label="收款主体" min-width="160" show-overflow-tooltip />
      <el-table-column label="收款金额（元）" width="120" align="right">
        <template #default="{ row }">
          <span>{{ formatMoney(row.amount) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="reconciliationCode" label="对账码" min-width="130" show-overflow-tooltip />
      <el-table-column prop="transferRemark" label="转账备注" min-width="180" show-overflow-tooltip />
      <el-table-column label="收款状态" width="140">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.collectionStatus)" effect="light">
            {{ row.collectionStatusName || row.collectionStatus || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="凭证" width="88" align="right">
        <template #default="{ row }">
          <span>{{ row.voucherCount ?? 0 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="expireTime" label="过期时间" width="170" show-overflow-tooltip />
      <el-table-column prop="updateTime" label="更新时间" width="170" show-overflow-tooltip />
      <el-table-column label="操作" width="236" fixed="right" align="left" class-name="payment-table__operation-cell">
        <template #default="{ row }">
          <div class="payment-table__actions payment-offline-collections__row-actions">
            <el-tooltip
              :disabled="canConfirmCollection(row)"
              :content="confirmDisabledReason(row)"
              placement="top"
            >
              <span class="payment-offline-collections__action">
                <el-button
                  link
                  type="success"
                  :icon="Check"
                  :disabled="!canConfirmCollection(row)"
                  @click="openConfirm(row)"
                >
                  确认到账
                </el-button>
              </span>
            </el-tooltip>
            <el-tooltip
              :disabled="canRefundCollection(row)"
              :content="refundDisabledReason(row)"
              placement="top"
            >
              <span class="payment-offline-collections__action">
                <el-button
                  link
                  type="warning"
                  :icon="RefreshLeft"
                  :disabled="!canRefundCollection(row)"
                  @click="openRefund(row)"
                >
                  退款
                </el-button>
              </span>
            </el-tooltip>
            <el-button link type="primary" :icon="Tickets" @click="openDetail(row)">详情</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-table
      v-else
      :data="bankStatementRows"
      v-loading="bankStatementLoading"
      row-key="id"
      stripe
      highlight-current-row
      class="payment-offline-collections__table"
    >
      <template #empty>
        <el-empty :description="bankStatementEmptyDescription" />
      </template>
      <el-table-column prop="batchNo" label="导入批次号" min-width="180" show-overflow-tooltip />
      <el-table-column prop="statementFileName" label="文件名" min-width="180" show-overflow-tooltip />
      <el-table-column prop="bankAccountNoMask" label="收款账号" min-width="150" show-overflow-tooltip />
      <el-table-column label="总笔数" width="90" align="right">
        <template #default="{ row }">{{ row.totalCount ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="匹配" width="90" align="right">
        <template #default="{ row }">{{ row.matchedCount ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="已确认" width="90" align="right">
        <template #default="{ row }">{{ row.confirmedCount ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="差异" width="90" align="right">
        <template #default="{ row }">{{ row.differenceCount ?? 0 }}</template>
      </el-table-column>
      <el-table-column label="批次状态" width="120">
        <template #default="{ row }">
          <el-tag :type="bankStatementStatusTagType(row.batchStatus)" effect="light">
            {{ row.batchStatusName || row.batchStatus || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="importerName" label="导入人" width="120" show-overflow-tooltip />
      <el-table-column prop="importTime" label="导入时间" width="170" show-overflow-tooltip />
      <el-table-column label="操作" width="104" fixed="right" align="left" class-name="payment-table__operation-cell">
        <template #default="{ row }">
          <div class="payment-table__actions">
            <el-button link type="primary" :icon="Tickets" @click="openBankStatementDetail(row)">详情</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="activeTab === 'collections'" class="payment-offline-collections__pagination">
      <Pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        @change="loadRows"
      />
    </div>
    <div v-else class="payment-offline-collections__pagination">
      <Pagination
        v-model:current-page="bankStatementQuery.pageNum"
        v-model:page-size="bankStatementQuery.pageSize"
        :total="bankStatementTotal"
        @change="loadBankStatements"
      />
    </div>

    <el-drawer
      v-model="detailVisible"
      title="线下收款详情"
      size="780px"
      destroy-on-close
      append-to-body
      class="payment-offline-collections__drawer"
    >
      <el-skeleton v-if="detailLoading" :rows="10" animated />
      <template v-else-if="detail">
        <el-descriptions title="收款信息" :column="2" border>
          <el-descriptions-item label="线下收款单号">{{ valueText(detail.offlineCollectionNo) }}</el-descriptions-item>
          <el-descriptions-item label="收款状态">
            <el-tag :type="statusTagType(detail.collectionStatus)" effect="light">
              {{ detail.collectionStatusName || detail.collectionStatus || '-' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="收款主体">{{ valueText(detail.subjectName) }}</el-descriptions-item>
          <el-descriptions-item label="收款金额（元）">{{ formatMoney(detail.amount) }}</el-descriptions-item>
          <el-descriptions-item label="开户行">{{ valueText(detail.bankName) }}</el-descriptions-item>
          <el-descriptions-item label="收款户名">{{ valueText(detail.accountName) }}</el-descriptions-item>
          <el-descriptions-item label="收款账号">{{ valueText(detail.accountNoMask) }}</el-descriptions-item>
          <el-descriptions-item label="币种">{{ valueText(detail.currency) }}</el-descriptions-item>
          <el-descriptions-item label="对账码">{{ valueText(detail.reconciliationCode) }}</el-descriptions-item>
          <el-descriptions-item label="转账备注">{{ valueText(detail.transferRemark) }}</el-descriptions-item>
          <el-descriptions-item label="提交转账金额（元）">{{ formatMoney(detail.transferAmount) }}</el-descriptions-item>
          <el-descriptions-item label="确认到账金额（元）">{{ formatMoney(detail.confirmedAmount) }}</el-descriptions-item>
          <el-descriptions-item label="转账凭证">{{ valueText(detail.voucherFileIds) }}</el-descriptions-item>
          <el-descriptions-item label="提交说明">{{ valueText(detail.submitRemark) }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="关联订单" :column="2" border class="payment-offline-collections__detail-block">
          <el-descriptions-item label="支付订单号">{{ valueText(detail.payOrderNo) }}</el-descriptions-item>
          <el-descriptions-item label="业务订单号">{{ valueText(detail.bizOrderNo) }}</el-descriptions-item>
          <el-descriptions-item label="支付标题">{{ valueText(detail.title) }}</el-descriptions-item>
          <el-descriptions-item label="AppId">{{ valueText(detail.appId) }}</el-descriptions-item>
          <el-descriptions-item label="支付通道">{{ channelText(detail) }}</el-descriptions-item>
          <el-descriptions-item label="签约配置">{{ valueText(detail.contractName) }}</el-descriptions-item>
          <el-descriptions-item label="签约能力 ID">{{ valueText(detail.contractCapabilityId) }}</el-descriptions-item>
          <el-descriptions-item label="凭证数量">{{ detail.voucherCount ?? 0 }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="时间信息" :column="2" border class="payment-offline-collections__detail-block">
          <el-descriptions-item label="创建时间">{{ valueText(detail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ valueText(detail.updateTime) }}</el-descriptions-item>
          <el-descriptions-item label="过期时间">{{ valueText(detail.expireTime) }}</el-descriptions-item>
          <el-descriptions-item label="提交凭证时间">{{ valueText(detail.submittedTime) }}</el-descriptions-item>
          <el-descriptions-item label="确认到账时间">{{ valueText(detail.confirmedTime) }}</el-descriptions-item>
          <el-descriptions-item label="确认人">{{ valueText(detail.confirmedByName || detail.confirmedBy) }}</el-descriptions-item>
          <el-descriptions-item label="确认说明">{{ valueText(detail.confirmRemark) }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <el-empty v-else description="未查询到线下收款详情" />
    </el-drawer>

    <el-dialog v-model="confirmVisible" title="确认线下收款到账" width="520px" destroy-on-close append-to-body>
      <el-form ref="confirmFormRef" :model="confirmForm" :rules="confirmRules" label-width="112px" class="payment-dialog-form">
        <section class="payment-form-section">
          <h4 class="payment-form-section__title">收款摘要</h4>
          <div class="payment-form-grid">
            <el-form-item label="收款单号" class="payment-form-item--wide">
              <span class="payment-form-readonly">{{ currentRow?.offlineCollectionNo || '-' }}</span>
            </el-form-item>
            <el-form-item label="应收金额（元）">
              <span class="payment-form-readonly payment-form-readonly--strong">{{ formatMoney(currentRow?.amount) }}</span>
            </el-form-item>
            <el-form-item label="提交金额（元）">
              <span class="payment-form-readonly">{{ formatMoney(currentRow?.transferAmount) }}</span>
            </el-form-item>
            <el-form-item label="凭证数量">
              <span class="payment-form-readonly">{{ currentRow?.voucherCount ?? 0 }}</span>
            </el-form-item>
          </div>
        </section>
        <section class="payment-form-section">
          <h4 class="payment-form-section__title">到账确认</h4>
          <div class="payment-form-grid">
            <el-form-item label="到账金额（元）" prop="confirmedAmount" class="payment-form-item--wide">
              <el-input-number v-model="confirmForm.confirmedAmount" :min="0.01" :precision="2" :controls="false" />
            </el-form-item>
            <el-form-item label="确认说明" class="payment-form-item--wide">
              <el-input v-model="confirmForm.confirmRemark" type="textarea" :rows="3" maxlength="200" show-word-limit />
            </el-form-item>
          </div>
        </section>
      </el-form>
      <template #footer>
        <el-button @click="confirmVisible = false">取消</el-button>
        <el-button type="primary" :loading="confirming" @click="submitConfirm">确认到账</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="refundVisible" title="创建线下退款" width="640px" destroy-on-close append-to-body>
      <el-form ref="refundFormRef" :model="refundForm" :rules="refundRules" label-width="112px" class="payment-dialog-form">
        <section class="payment-form-section">
          <h4 class="payment-form-section__title">原收款信息</h4>
          <div class="payment-form-grid">
            <el-form-item label="收款单号">
              <span class="payment-form-readonly">{{ currentRow?.offlineCollectionNo || '-' }}</span>
            </el-form-item>
            <el-form-item label="已收金额（元）">
              <span class="payment-form-readonly payment-form-readonly--strong">{{ formatMoney(currentRow?.confirmedAmount || currentRow?.amount) }}</span>
            </el-form-item>
          </div>
        </section>
        <section class="payment-form-section">
          <h4 class="payment-form-section__title">退款账户</h4>
          <div class="payment-form-grid">
            <el-form-item label="退款金额（元）" prop="refundAmount" class="payment-form-item--wide">
              <el-input-number v-model="refundForm.refundAmount" :min="0.01" :precision="2" :controls="false" />
            </el-form-item>
            <el-form-item label="退款户名" prop="refundAccountName">
              <el-input v-model="refundForm.refundAccountName" maxlength="80" />
            </el-form-item>
            <el-form-item label="退款账号" prop="refundAccountNo">
              <el-input v-model="refundForm.refundAccountNo" maxlength="80" />
            </el-form-item>
            <el-form-item label="退款开户行" prop="refundBankName" class="payment-form-item--wide">
              <el-input v-model="refundForm.refundBankName" maxlength="80" />
            </el-form-item>
          </div>
        </section>
        <section class="payment-form-section">
          <h4 class="payment-form-section__title">凭证与说明</h4>
          <div class="payment-form-grid">
            <el-form-item label="退款凭证" prop="refundVoucherFileIds" class="payment-form-item--wide">
              <MUpload
                v-model="refundForm.refundVoucherFileIds"
                value-type="id"
                display="list"
                :count="6"
                purpose="payment-offline-refund-voucher"
                biz-type="payment-offline-refund"
                button-text="上传凭证"
              />
            </el-form-item>
            <el-form-item label="退款原因" prop="reason" class="payment-form-item--wide">
              <el-input v-model="refundForm.reason" type="textarea" :rows="2" maxlength="200" show-word-limit />
            </el-form-item>
            <el-form-item label="备注" class="payment-form-item--wide">
              <el-input v-model="refundForm.remark" type="textarea" :rows="2" maxlength="200" show-word-limit />
            </el-form-item>
          </div>
        </section>
      </el-form>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="primary" :loading="refunding" @click="submitRefund">提交退款</el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="bankStatementDetailVisible"
      title="银行流水导入详情"
      size="980px"
      destroy-on-close
      append-to-body
      class="payment-offline-collections__drawer"
    >
      <el-skeleton v-if="bankStatementDetailLoading" :rows="10" animated />
      <template v-else-if="bankStatementDetail">
        <el-descriptions title="导入批次" :column="2" border>
          <el-descriptions-item label="批次号">{{ valueText(bankStatementDetail.batchNo) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="bankStatementStatusTagType(bankStatementDetail.batchStatus)" effect="light">
              {{ bankStatementDetail.batchStatusName || bankStatementDetail.batchStatus || '-' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="文件名">{{ valueText(bankStatementDetail.statementFileName) }}</el-descriptions-item>
          <el-descriptions-item label="文件摘要">{{ valueText(bankStatementDetail.fileDigest) }}</el-descriptions-item>
          <el-descriptions-item label="总笔数">{{ bankStatementDetail.totalCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="匹配/确认/差异">
            {{ bankStatementDetail.matchedCount ?? 0 }} / {{ bankStatementDetail.confirmedCount ?? 0 }} / {{ bankStatementDetail.differenceCount ?? 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="导入人">{{ valueText(bankStatementDetail.importerName) }}</el-descriptions-item>
          <el-descriptions-item label="导入时间">{{ valueText(bankStatementDetail.importTime) }}</el-descriptions-item>
        </el-descriptions>

        <div class="payment-offline-collections__line-actions">
          <el-button
            type="primary"
            :icon="Check"
            :disabled="!selectedBankStatementItemIds.length"
            :loading="confirmingBankStatements"
            @click="openBankStatementConfirm"
          >
            确认匹配到账
          </el-button>
        </div>
        <el-table
          :data="bankStatementDetail.items || []"
          row-key="id"
          stripe
          @selection-change="handleBankStatementSelection"
        >
          <el-table-column type="selection" width="48" :selectable="isBankStatementSelectable" />
          <el-table-column prop="rowNo" label="行号" width="72" />
          <el-table-column prop="bankStatementNo" label="银行流水号" min-width="160" show-overflow-tooltip />
          <el-table-column label="金额（元）" width="110" align="right">
            <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column prop="tradeTime" label="交易时间" min-width="170" show-overflow-tooltip />
          <el-table-column prop="reconciliationCode" label="对账码" min-width="130" show-overflow-tooltip />
          <el-table-column prop="matchedOfflineCollectionNo" label="匹配收款单" min-width="180" show-overflow-tooltip />
          <el-table-column label="匹配状态" width="140">
            <template #default="{ row }">
              <el-tag :type="bankStatementMatchTagType(row.matchStatus)" effect="light">
                {{ row.matchStatusName || row.matchStatus || '-' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="matchMessage" label="匹配说明" min-width="220" show-overflow-tooltip />
          <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
        </el-table>
      </template>
      <el-empty v-else description="未查询到银行流水批次详情" />
    </el-drawer>

    <el-dialog v-model="bankStatementConfirmVisible" title="确认银行流水匹配到账" width="520px" destroy-on-close append-to-body>
      <el-form label-width="112px" class="payment-dialog-form">
        <el-form-item label="确认笔数">
          <span>{{ selectedBankStatementItemIds.length }}</span>
        </el-form-item>
        <el-form-item label="确认说明">
          <el-input v-model="bankStatementConfirmRemark" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bankStatementConfirmVisible = false">取消</el-button>
        <el-button type="primary" :loading="confirmingBankStatements" @click="submitBankStatementConfirm">确认到账</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { Pagination } from '@mango/common';
import { computed, onMounted, reactive, ref } from 'vue';
import { Check, Refresh, RefreshLeft, Search, Tickets } from '@element-plus/icons-vue';
import type { FormInstance, FormRules, UploadFile } from 'element-plus';
import { ElMessage } from 'element-plus';
import { MUpload } from '@mango/file';
import {
  paymentOfflineCollectionApi,
  type PaymentOfflineBankStatementBatch,
  type PaymentOfflineBankStatementItem,
  type PaymentOfflineBankStatementStatus,
  type PaymentOfflineCollection,
  type PaymentOfflineCollectionStatus,
  type PaymentPageQuery,
} from '../../api/payment';

const activeTab = ref<'collections' | 'bankStatements'>('collections');
const query = reactive<PaymentPageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  statusCode: '',
});
const rows = ref<PaymentOfflineCollection[]>([]);
const statusOptions = ref<PaymentOfflineCollectionStatus[]>([]);
const bankStatementStatusOptions = ref<PaymentOfflineBankStatementStatus[]>([]);
const total = ref(0);
const bankStatementTotal = ref(0);
const loading = ref(false);
const bankStatementLoading = ref(false);
const detailVisible = ref(false);
const detailLoading = ref(false);
const detail = ref<PaymentOfflineCollection>();
const bankStatementDetailVisible = ref(false);
const bankStatementDetailLoading = ref(false);
const bankStatementDetail = ref<PaymentOfflineBankStatementBatch>();
const errorMessage = ref('');
const bankStatementErrorMessage = ref('');
const confirmVisible = ref(false);
const refundVisible = ref(false);
const bankStatementConfirmVisible = ref(false);
const confirming = ref(false);
const refunding = ref(false);
const importingBankStatement = ref(false);
const confirmingBankStatements = ref(false);
const currentRow = ref<PaymentOfflineCollection>();
const bankStatementRows = ref<PaymentOfflineBankStatementBatch[]>([]);
const selectedBankStatementItemIds = ref<string[]>([]);
const bankStatementConfirmRemark = ref('');
const confirmFormRef = ref<FormInstance>();
const refundFormRef = ref<FormInstance>();
const bankStatementQuery = reactive<PaymentPageQuery>({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  statusCode: '',
});
const confirmForm = reactive({
  confirmedAmount: 0,
  confirmRemark: '',
});
const refundForm = reactive({
  refundAmount: 0,
  refundAccountName: '',
  refundAccountNo: '',
  refundBankName: '',
  refundVoucherFileIds: [] as string[],
  reason: '',
  remark: '',
});
const confirmRules: FormRules = {
  confirmedAmount: [{ required: true, type: 'number', min: 0.01, message: '请输入确认到账金额', trigger: 'blur' }],
};
const refundRules: FormRules = {
  refundAmount: [{ required: true, type: 'number', min: 0.01, message: '请输入退款金额', trigger: 'blur' }],
  refundAccountName: [{ required: true, message: '请输入退款户名', trigger: 'blur' }],
  refundAccountNo: [{ required: true, message: '请输入退款账号', trigger: 'blur' }],
  refundBankName: [{ required: true, message: '请输入退款开户行', trigger: 'blur' }],
  refundVoucherFileIds: [{ required: true, type: 'array', min: 1, message: '请上传退款凭证', trigger: 'change' }],
  reason: [{ required: true, message: '请输入退款原因', trigger: 'blur' }],
};

const emptyDescription = computed(() => {
  if (errorMessage.value) return '线下收款加载失败';
  return query.keyword || query.statusCode ? '未查询到匹配的线下收款' : '暂无线下收款记录';
});
const bankStatementEmptyDescription = computed(() => {
  if (bankStatementErrorMessage.value) return '银行流水加载失败';
  return bankStatementQuery.keyword || bankStatementQuery.statusCode ? '未查询到匹配的银行流水批次' : '暂未导入银行流水';
});

onMounted(async () => {
  await Promise.all([loadStatuses(), loadBankStatementStatuses(), loadRows(), loadBankStatements()]);
});

async function loadStatuses() {
  statusOptions.value = await paymentOfflineCollectionApi.statuses();
}

async function loadBankStatementStatuses() {
  bankStatementStatusOptions.value = await paymentOfflineCollectionApi.bankStatementStatuses();
}

async function loadRows() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const result = await paymentOfflineCollectionApi.page(query);
    rows.value = result.list;
    total.value = result.total;
  } catch (error) {
    rows.value = [];
    total.value = 0;
    errorMessage.value = error instanceof Error ? error.message : '线下收款加载失败';
  } finally {
    loading.value = false;
  }
}

function resetQuery() {
  query.pageNum = 1;
  query.keyword = '';
  query.statusCode = '';
  void loadRows();
}

function applyFilters() {
  query.pageNum = 1;
  void loadRows();
}

async function loadBankStatements() {
  bankStatementLoading.value = true;
  bankStatementErrorMessage.value = '';
  try {
    const result = await paymentOfflineCollectionApi.bankStatements(bankStatementQuery);
    bankStatementRows.value = result.list;
    bankStatementTotal.value = result.total;
  } catch (error) {
    bankStatementRows.value = [];
    bankStatementTotal.value = 0;
    bankStatementErrorMessage.value = error instanceof Error ? error.message : '银行流水加载失败';
  } finally {
    bankStatementLoading.value = false;
  }
}

function resetBankStatementQuery() {
  bankStatementQuery.pageNum = 1;
  bankStatementQuery.keyword = '';
  bankStatementQuery.statusCode = '';
  void loadBankStatements();
}

function applyBankStatementFilters() {
  bankStatementQuery.pageNum = 1;
  void loadBankStatements();
}

function canConfirmCollection(row: PaymentOfflineCollection) {
  return row.collectionStatus === 'WAITING_TRANSFER' || row.collectionStatus === 'PENDING_CONFIRM';
}

function canRefundCollection(row: PaymentOfflineCollection) {
  return row.collectionStatus === 'CONFIRMED' || row.collectionStatus === 'RECONCILED';
}

function confirmDisabledReason(row: PaymentOfflineCollection) {
  if (row.collectionStatus === 'CONFIRMED' || row.collectionStatus === 'RECONCILED') return '该收款已确认到账';
  if (row.collectionStatus === 'EXPIRED') return '该收款已过期，不能确认到账';
  if (row.collectionStatus === 'CLOSED') return '该收款已关闭，不能确认到账';
  return '当前状态不能确认到账';
}

function refundDisabledReason(row: PaymentOfflineCollection) {
  if (row.collectionStatus === 'WAITING_TRANSFER') return '未确认到账，不能退款';
  if (row.collectionStatus === 'PENDING_CONFIRM') return '待确认到账，不能退款';
  if (row.collectionStatus === 'EXPIRED') return '该收款已过期，不能退款';
  if (row.collectionStatus === 'CLOSED') return '该收款已关闭，不能退款';
  return '当前状态不能退款';
}

async function openDetail(row: PaymentOfflineCollection) {
  if (!row.id) return;
  detailVisible.value = true;
  detailLoading.value = true;
  detail.value = undefined;
  try {
    detail.value = await paymentOfflineCollectionApi.detail(row.id);
  } finally {
    detailLoading.value = false;
  }
}

async function openBankStatementDetail(row: PaymentOfflineBankStatementBatch) {
  if (!row.id) return;
  bankStatementDetailVisible.value = true;
  bankStatementDetailLoading.value = true;
  selectedBankStatementItemIds.value = [];
  bankStatementDetail.value = undefined;
  try {
    bankStatementDetail.value = await paymentOfflineCollectionApi.bankStatementDetail(row.id);
  } finally {
    bankStatementDetailLoading.value = false;
  }
}

function openConfirm(row: PaymentOfflineCollection) {
  if (!canConfirmCollection(row)) return;
  currentRow.value = row;
  confirmForm.confirmedAmount = centsToYuan(row.transferAmount || row.amount || 0);
  confirmForm.confirmRemark = '';
  confirmVisible.value = true;
}

function openRefund(row: PaymentOfflineCollection) {
  if (!canRefundCollection(row)) return;
  currentRow.value = row;
  refundForm.refundAmount = centsToYuan(row.confirmedAmount || row.amount || 0);
  refundForm.refundAccountName = '';
  refundForm.refundAccountNo = '';
  refundForm.refundBankName = '';
  refundForm.refundVoucherFileIds = [];
  refundForm.reason = '';
  refundForm.remark = '';
  refundVisible.value = true;
}

async function submitConfirm() {
  if (!currentRow.value?.id) return;
  await confirmFormRef.value?.validate();
  confirming.value = true;
  try {
    await paymentOfflineCollectionApi.confirm({
      id: currentRow.value.id,
      confirmedAmount: yuanToCents(confirmForm.confirmedAmount),
      confirmRemark: confirmForm.confirmRemark || undefined,
    });
    ElMessage.success('已确认到账');
    confirmVisible.value = false;
    await loadRows();
  } finally {
    confirming.value = false;
  }
}

async function submitRefund() {
  if (!currentRow.value?.id) return;
  await refundFormRef.value?.validate();
  const refundVoucherFileIds = refundForm.refundVoucherFileIds.filter(Boolean).join(',');
  if (!refundVoucherFileIds) {
    ElMessage.warning('请上传退款凭证');
    return;
  }
  refunding.value = true;
  try {
    await paymentOfflineCollectionApi.refund({
      offlineCollectionId: currentRow.value.id,
      refundAmount: yuanToCents(refundForm.refundAmount),
      refundAccountName: refundForm.refundAccountName,
      refundAccountNo: refundForm.refundAccountNo,
      refundBankName: refundForm.refundBankName,
      refundVoucherFileIds,
      reason: refundForm.reason,
      remark: refundForm.remark || undefined,
    });
    ElMessage.success('线下退款已记录');
    refundVisible.value = false;
    await loadRows();
  } finally {
    refunding.value = false;
  }
}

async function handleBankStatementFile(uploadFile: UploadFile) {
  const file = uploadFile.raw;
  if (!file) return;
  importingBankStatement.value = true;
  try {
    const result = await paymentOfflineCollectionApi.importBankStatement(file);
    ElMessage.success('银行流水已导入并生成匹配结果');
    activeTab.value = 'bankStatements';
    await loadBankStatements();
    await openBankStatementDetail(result);
  } finally {
    importingBankStatement.value = false;
  }
}

function handleBankStatementSelection(items: PaymentOfflineBankStatementItem[]) {
  selectedBankStatementItemIds.value = items
    .filter(item => item.id && item.matchStatus === 'MATCHED_PENDING_CONFIRM')
    .map(item => String(item.id));
}

function isBankStatementSelectable(row: PaymentOfflineBankStatementItem) {
  return row.matchStatus === 'MATCHED_PENDING_CONFIRM';
}

function openBankStatementConfirm() {
  if (!selectedBankStatementItemIds.value.length) {
    ElMessage.warning('请选择已匹配待确认的银行流水');
    return;
  }
  bankStatementConfirmRemark.value = '';
  bankStatementConfirmVisible.value = true;
}

async function submitBankStatementConfirm() {
  if (!selectedBankStatementItemIds.value.length) return;
  confirmingBankStatements.value = true;
  try {
    const result = await paymentOfflineCollectionApi.confirmBankStatement({
      itemIds: selectedBankStatementItemIds.value,
      confirmRemark: bankStatementConfirmRemark.value || undefined,
    });
    ElMessage.success('已确认银行流水匹配到账');
    bankStatementConfirmVisible.value = false;
    selectedBankStatementItemIds.value = [];
    bankStatementDetail.value = result;
    await Promise.all([loadRows(), loadBankStatements()]);
  } finally {
    confirmingBankStatements.value = false;
  }
}

function statusTagType(status?: string) {
  if (status === 'CONFIRMED' || status === 'RECONCILED') return 'success';
  if (status === 'WAITING_TRANSFER' || status === 'PENDING_CONFIRM') return 'warning';
  if (status === 'EXPIRED' || status === 'CLOSED') return 'info';
  return '';
}

function bankStatementStatusTagType(status?: string) {
  if (status === 'CONFIRMED') return 'success';
  if (status === 'MATCHED') return 'warning';
  if (status === 'DIFFERENCE') return 'danger';
  return '';
}

function bankStatementMatchTagType(status?: string) {
  if (status === 'CONFIRMED') return 'success';
  if (status === 'MATCHED_PENDING_CONFIRM') return 'warning';
  if (status === 'UNMATCHED' || status === 'AMOUNT_MISMATCH' || status === 'DUPLICATED_STATEMENT') return 'danger';
  return 'info';
}

function channelText(row: PaymentOfflineCollection) {
  return row.channelName || row.channelCode || '-';
}

function formatMoney(value?: number | string) {
  if (value === undefined || value === null || value === '') return '-';
  const cents = Number(value);
  if (!Number.isFinite(cents)) return '-';
  return `¥${(cents / 100).toFixed(2)}`;
}

function valueText(value?: string | number | null) {
  if (value === undefined || value === null || value === '') return '-';
  return String(value);
}

function centsToYuan(value: number | string) {
  const cents = Number(value || 0);
  return Number((cents / 100).toFixed(2));
}

function yuanToCents(value: number) {
  return Math.round(Number(value || 0) * 100);
}
</script>

<style scoped>
.payment-offline-collections__detail-block {
  margin-top: 16px;
}

.payment-table__actions.payment-offline-collections__row-actions {
  display: inline-flex;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
  white-space: nowrap;
}

.payment-offline-collections__action {
  display: inline-flex;
  align-items: center;
  line-height: 1;
}

</style>
