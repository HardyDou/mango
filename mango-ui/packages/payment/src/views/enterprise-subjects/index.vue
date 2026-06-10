<template>
  <PaymentResourcePage
    title="企业主体"
    description="维护租户下可用于收款、结算和通道开户的企业主体。"
    :columns="columns"
    :api="paymentEnterpriseSubjectApi"
    :defaults="{ status: 1 }"
    :rules="rules"
    :to-save-payload="toSavePayload"
  >
    <template #form="{ form }">
      <el-form-item label="主体名称" prop="subjectName">
        <el-input v-model="form.subjectName" placeholder="企业全称" />
      </el-form-item>
      <el-form-item label="统一社会信用代码" prop="creditCode">
        <el-input v-model="form.creditCode" :placeholder="editingHint(form.creditCodeMask)" />
      </el-form-item>
      <el-form-item label="银行账户" prop="bankAccountNo">
        <el-input v-model="form.bankAccountNo" :placeholder="editingHint(form.bankAccountNoMask)" />
      </el-form-item>
      <el-form-item label="开户行" prop="bankName">
        <el-input v-model="form.bankName" />
      </el-form-item>
      <el-form-item label="证照文件">
        <MUpload
          v-model="form.licenseFileId"
          value-type="id"
          display="list"
          :count="1"
          biz-type="payment-enterprise-license"
          purpose="business-license"
          button-text="上传证照"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :label="1">启用</el-radio>
          <el-radio :label="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>
    </template>
  </PaymentResourcePage>
</template>

<script setup lang="ts">
import type { FormRules } from 'element-plus';
import { MUpload } from '@mango/file';
import PaymentResourcePage from '../../components/PaymentResourcePage.vue';
import { paymentEnterpriseSubjectApi, type PaymentRecord, type PaymentTableColumn } from '../../api/payment';

const columns: PaymentTableColumn[] = [
  { prop: 'subjectName', label: '主体名称', minWidth: 220 },
  { prop: 'creditCodeMask', label: '统一社会信用代码', minWidth: 190 },
  { prop: 'bankName', label: '开户行', minWidth: 180 },
  { prop: 'bankAccountNoMask', label: '银行账户', minWidth: 170 },
];

const rules: FormRules = {
  subjectName: [{ required: true, message: '请输入主体名称', trigger: 'blur' }],
  creditCode: [{ required: true, message: '请输入统一社会信用代码', trigger: 'blur' }],
  bankAccountNo: [{ required: true, message: '请输入银行账户', trigger: 'blur' }],
  bankName: [{ required: true, message: '请输入开户行', trigger: 'blur' }],
};

function toSavePayload(form: PaymentRecord) {
  return {
    id: form.id,
    subjectName: form.subjectName,
    creditCode: form.creditCode,
    bankAccountNo: form.bankAccountNo,
    bankName: form.bankName,
    licenseFileId: form.licenseFileId,
    status: form.status,
  };
}

function editingHint(value: unknown) {
  return typeof value === 'string' && value ? `当前：${value}，保存时需重新输入` : '';
}
</script>
