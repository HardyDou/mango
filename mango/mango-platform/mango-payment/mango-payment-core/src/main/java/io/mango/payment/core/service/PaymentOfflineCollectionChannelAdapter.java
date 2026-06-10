package io.mango.payment.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.common.exception.BizException;
import io.mango.common.result.Require;
import io.mango.payment.api.PaymentCode;
import io.mango.payment.api.enums.PaymentChannelCode;
import io.mango.payment.api.enums.PaymentOfflineCollectionStatusEnum;
import io.mango.payment.api.enums.PaymentOrderStatusEnum;
import io.mango.payment.api.enums.PaymentRefundOrderStatusEnum;
import io.mango.payment.api.vo.PaymentCashierPayMaterialVO;
import io.mango.payment.core.entity.PaymentOfflineCollectionEntity;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.mapper.PaymentOfflineCollectionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 线下收款通道适配器。
 */
@Service
@RequiredArgsConstructor
public class PaymentOfflineCollectionChannelAdapter implements IPaymentChannelAdapter {

    private static final String CHANNEL_CODE = PaymentChannelCode.OFFLINE_COLLECTION.name();
    private static final String MATERIAL_TYPE_TRANSFER_ACCOUNT = "TRANSFER_ACCOUNT";
    private static final String CONFIG_ACCOUNT_NAME = "accountName";
    private static final String CONFIG_ACCOUNT_NO = "accountNo";
    private static final String CONFIG_BANK_NAME = "bankName";
    private static final TypeReference<Map<String, Object>> CONFIG_VALUES_TYPE = new TypeReference<>() {
    };
    private static final char[] RECONCILIATION_CODE_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int RECONCILIATION_CODE_LENGTH = 6;

    private final PaymentOfflineCollectionMapper offlineCollectionMapper;
    private final PaymentSensitiveValueService sensitiveValueService;
    private final ObjectMapper objectMapper;
    private final PaymentNumberService numberService;

    @Override
    public String channelCode() {
        return CHANNEL_CODE;
    }

    @Override
    public PaymentApplyResult applyPayment(PaymentApplyCommand command) {
        validatePaymentCommand(command);
        String reconciliationCode = nextReconciliationCode();
        OfflineCollectionAccount account = collectionAccount(command);
        PaymentCashierPayMaterialVO material = transferMaterial(command, account, reconciliationCode);
        return new PaymentApplyResult(
                "WAITING_TRANSFER",
                "WAITING_TRANSFER",
                "ASYNC_PROCESSING",
                PaymentOrderStatusEnum.PAYING.getCode(),
                offlineChannelTradeNo(command),
                material);
    }

    @Override
    public void afterPaymentOrderCreated(PaymentApplyCommand command, PaymentApplyResult result, PaymentOrderEntity order) {
        validatePaymentCommand(command);
        Require.notNull(result, PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "线下收款通道支付结果不能为空");
        Require.notNull(order, PaymentCode.PAYMENT_ORDER_NOT_FOUND);
        PaymentCashierPayMaterialVO material = result.material();
        Require.notNull(material, PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "线下收款通道支付物料不能为空");
        Require.notBlank(material.getTransferRemark(), PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "线下收款转账备注不能为空");

        PaymentOfflineCollectionEntity entity = new PaymentOfflineCollectionEntity();
        entity.setOfflineCollectionNo(nextOfflineCollectionNo());
        entity.setPaymentOrderId(order.getId());
        entity.setPayOrderNo(order.getPayOrderNo());
        entity.setBusinessOrderId(order.getBusinessOrderId());
        entity.setBizOrderNo(command.bizOrderNo());
        entity.setChannelId(order.getChannelId());
        entity.setChannelCode(CHANNEL_CODE);
        entity.setContractId(order.getContractId());
        entity.setContractCapabilityId(order.getContractCapabilityId());
        entity.setSubjectId(command.subjectId());
        OfflineCollectionAccount account = collectionAccount(command);
        entity.setSubjectName(command.subjectName());
        entity.setBankAccountId(null);
        entity.setAccountName(account.accountName());
        entity.setAccountNoMask(sensitiveValueService.mask(account.accountNo(), 4, 4));
        entity.setBankName(account.bankName());
        entity.setAmount(command.amount());
        entity.setCurrency(command.currency());
        entity.setReconciliationCode(material.getTransferRemark());
        entity.setTransferRemark(material.getTransferRemark());
        entity.setVoucherCount(0);
        entity.setCollectionStatus(PaymentOfflineCollectionStatusEnum.WAITING_TRANSFER.getCode());
        entity.setExpireTime(command.expireTime());
        entity.setConfirmedTime(null);
        entity.setTenantId(command.tenantId());
        entity.setDelFlag(0);
        offlineCollectionMapper.insert(entity);
    }

    @Override
    public RefundApplyResult applyRefund(RefundApplyCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_OFFLINE_REFUND_INVALID.getCode(), "线下退款命令不能为空");
        Require.isTrue(CHANNEL_CODE.equals(command.channelCode()), PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "线下收款通道编码不正确");
        Require.notBlank(command.refundOrderNo(), PaymentCode.PAYMENT_REFUND_ORDER_INVALID.getCode(), "退款订单号不能为空");
        throw unsupported(
                PaymentCode.PAYMENT_OFFLINE_REFUND_INVALID,
                "线下退款需要录入退款账户、退款金额和退款凭证，请通过线下支付/退款订单入口办理");
    }

    @Override
    public ChannelBillResult generateBill(ChannelBillCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "线下收款账单命令不能为空");
        Require.isTrue(CHANNEL_CODE.equals(command.channelCode()), PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "线下收款通道编码不正确");
        Require.notNull(command.billDate(), PaymentCode.PAYMENT_RECONCILIATION_INVALID.getCode(), "账单日期不能为空");
        throw unsupported(
                PaymentCode.PAYMENT_RECONCILIATION_INVALID,
                "线下收款对账以银行流水 Excel 导入为准，请通过线下支付/银行流水导入入口办理");
    }

    @Override
    public PaymentQueryResult queryPayment(PaymentQueryCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_ORDER_STATE_INVALID.getCode(), "线下收款查单命令不能为空");
        Require.notNull(command.order(), PaymentCode.PAYMENT_ORDER_NOT_FOUND);
        PaymentOfflineCollectionEntity collection = offlineCollectionMapper.selectByPayOrderNoForUpdate(
                command.tenantId(),
                command.order().getPayOrderNo());
        Require.notNull(collection, PaymentCode.PAYMENT_OFFLINE_COLLECTION_NOT_FOUND);
        Require.isTrue(CHANNEL_CODE.equals(collection.getChannelCode()), PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "线下收款通道编码不正确");
        return mapPaymentQueryResult(collection.getCollectionStatus());
    }

    @Override
    public RefundQueryResult queryRefund(RefundQueryCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_REFUND_ORDER_INVALID.getCode(), "线下退款查单命令不能为空");
        Require.notNull(command.refundOrder(), PaymentCode.PAYMENT_REFUND_ORDER_NOT_FOUND);
        return new RefundQueryResult(
                "OFFLINE_REFUND_INTERNAL_RECORD",
                "REFUNDING",
                "ASYNC_PROCESSING",
                PaymentRefundOrderStatusEnum.REFUNDING.getCode());
    }

    private void validatePaymentCommand(PaymentApplyCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "线下收款支付命令不能为空");
        Require.isTrue(CHANNEL_CODE.equals(command.channelCode()), PaymentCode.PAYMENT_CHANNEL_INVALID.getCode(), "线下收款通道编码不正确");
        Require.notNull(command.contractId(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "线下收款签约 ID 不能为空");
        Require.notBlank(command.payOrderNo(), PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "支付订单号不能为空");
        Require.notBlank(command.bizOrderNo(), PaymentCode.PAYMENT_BUSINESS_ORDER_INVALID.getCode(), "业务订单号不能为空");
        Require.isTrue(MATERIAL_TYPE_TRANSFER_ACCOUNT.equals(command.paymentMaterialType()),
                PaymentCode.PAYMENT_CASHIER_PAY_INVALID.getCode(), "线下收款只支持转账账号物料");
        Require.notNull(command.amount(), PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), "线下收款金额不能为空");
        Require.isTrue(command.amount() > 0, PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), "线下收款金额必须大于 0");
        Require.notBlank(command.currency(), PaymentCode.PAYMENT_AMOUNT_INVALID.getCode(), "线下收款币种不能为空");
        Require.notNull(command.subjectId(), PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_INVALID.getCode(), "线下收款主体 ID 不能为空");
        Require.notBlank(command.subjectName(), PaymentCode.PAYMENT_ENTERPRISE_SUBJECT_INVALID.getCode(), "线下收款主体名称不能为空");
        Require.notBlank(command.contractConfigValuesJson(), PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "线下收款签约配置缺少收款账户");
    }

    private PaymentCashierPayMaterialVO transferMaterial(
            PaymentApplyCommand command,
            OfflineCollectionAccount account,
            String reconciliationCode) {
        PaymentCashierPayMaterialVO material = new PaymentCashierPayMaterialVO();
        material.setMaterialType(MATERIAL_TYPE_TRANSFER_ACCOUNT);
        material.setAccountName(account.accountName());
        material.setAccountNo(account.accountNo());
        material.setAccountNoMask(sensitiveValueService.mask(account.accountNo(), 4, 4));
        material.setBankName(account.bankName());
        material.setTransferRemark(reconciliationCode);
        material.setTransferInstruction("请按页面金额转账，并在银行转账备注中填写页面展示的转账备注。到账确认前支付结果保持处理中。");
        material.setExpireTime(command.expireTime());
        material.setVoucherRequired(true);
        return material;
    }

    private OfflineCollectionAccount collectionAccount(PaymentApplyCommand command) {
        Map<String, Object> values = parseConfigValues(command.contractConfigValuesJson());
        String accountName = trimToNull(values.get(CONFIG_ACCOUNT_NAME));
        String accountNo = sensitiveValueService.decrypt(trimToNull(values.get(CONFIG_ACCOUNT_NO)));
        String bankName = trimToNull(values.get(CONFIG_BANK_NAME));
        Require.notBlank(accountName, PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "线下收款签约配置缺少收款户名");
        Require.notBlank(accountNo, PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "线下收款签约配置缺少收款账号");
        Require.notBlank(bankName, PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "线下收款签约配置缺少开户行");
        return new OfflineCollectionAccount(accountName, accountNo, bankName);
    }

    private Map<String, Object> parseConfigValues(String configValuesJson) {
        try {
            Map<String, Object> values = objectMapper.readValue(configValuesJson, CONFIG_VALUES_TYPE);
            return values == null ? Map.of() : values;
        } catch (JsonProcessingException ex) {
            throw new BizException(PaymentCode.PAYMENT_CHANNEL_CONTRACT_INVALID.getCode(), "线下收款签约配置不是有效 JSON", ex);
        }
    }

    private String trimToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String offlineChannelTradeNo(PaymentApplyCommand command) {
        return "OC" + command.payOrderNo();
    }

    private String nextOfflineCollectionNo() {
        return numberService.next(PaymentNumberService.PAY_OFFLINE_COLLECTION_NO);
    }

    private String nextReconciliationCode() {
        StringBuilder code = new StringBuilder(RECONCILIATION_CODE_LENGTH);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int index = 0; index < RECONCILIATION_CODE_LENGTH; index++) {
            code.append(RECONCILIATION_CODE_CHARS[random.nextInt(RECONCILIATION_CODE_CHARS.length)]);
        }
        return code.toString();
    }

    private PaymentQueryResult mapPaymentQueryResult(String collectionStatus) {
        if (PaymentOfflineCollectionStatusEnum.CONFIRMED.getCode().equals(collectionStatus)
                || PaymentOfflineCollectionStatusEnum.RECONCILED.getCode().equals(collectionStatus)) {
            return new PaymentQueryResult(
                    "OFFLINE_COLLECTION_CONFIRMED",
                    collectionStatus,
                    "SUCCESS",
                    PaymentOrderStatusEnum.SUCCESS.getCode());
        }
        if (PaymentOfflineCollectionStatusEnum.EXPIRED.getCode().equals(collectionStatus)
                || PaymentOfflineCollectionStatusEnum.CLOSED.getCode().equals(collectionStatus)) {
            return new PaymentQueryResult(
                    "OFFLINE_COLLECTION_CLOSED",
                    collectionStatus,
                    "FAILED",
                    PaymentOrderStatusEnum.FAILED.getCode());
        }
        return new PaymentQueryResult(
                "OFFLINE_COLLECTION_WAITING",
                collectionStatus,
                "ASYNC_PROCESSING",
                PaymentOrderStatusEnum.PAYING.getCode());
    }

    private BizException unsupported(PaymentCode code, String message) {
        return new BizException(code.getCode(), message);
    }

    private record OfflineCollectionAccount(String accountName, String accountNo, String bankName) {
    }
}
