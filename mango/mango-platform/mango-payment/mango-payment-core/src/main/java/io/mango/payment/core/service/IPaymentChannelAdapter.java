package io.mango.payment.core.service;

import io.mango.payment.api.vo.PaymentCashierPayMaterialVO;
import io.mango.payment.api.vo.PaymentRefundOrderVO;
import io.mango.payment.core.entity.PaymentOrderEntity;
import io.mango.payment.core.model.PaymentChannelBillItemRow;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付通道适配接口。
 */
public interface IPaymentChannelAdapter {

    /**
     * 返回通道编码。
     *
     * @return 通道编码
     */
    String channelCode();

    /**
     * 发起支付。
     *
     * @param command 支付命令
     * @return 通道支付结果
     */
    PaymentApplyResult applyPayment(PaymentApplyCommand command);

    /**
     * 支付订单创建后的通道扩展动作。
     *
     * @param command 支付命令
     * @param result 通道支付结果
     * @param order 已创建支付订单
     */
    default void afterPaymentOrderCreated(
            PaymentApplyCommand command,
            PaymentApplyResult result,
            PaymentOrderEntity order) {
    }

    /**
     * 发起退款。
     *
     * @param command 退款命令
     * @return 通道退款结果
     */
    RefundApplyResult applyRefund(RefundApplyCommand command);

    /**
     * 生成通道账单。
     *
     * @param command 账单生成命令
     * @return 通道账单结果
     */
    ChannelBillResult generateBill(ChannelBillCommand command);

    /**
     * 主动查询支付订单状态。
     *
     * @param command 查单命令
     * @return 通道查询结果
     */
    PaymentQueryResult queryPayment(PaymentQueryCommand command);

    /**
     * 主动查询退款订单状态。
     *
     * @param command 查退款命令
     * @return 通道退款查询结果
     */
    RefundQueryResult queryRefund(RefundQueryCommand command);

    /**
     * 支付命令。
     *
     * @param tenantId 租户 ID
     * @param channelCode 通道编码
     * @param contractId 签约配置 ID
     * @param contractConfigValuesJson 签约配置值 JSON
     * @param payOrderNo 支付订单号
     * @param bizOrderNo 业务订单号
     * @param methodCode 标准支付方式编码
     * @param methodName 标准支付方式名称
     * @param paymentMaterialType 支付物料类型
     * @param amount 支付金额，单位分
     * @param currency 币种
     * @param title 订单标题
     * @param expireTime 支付订单过期时间
     * @param subjectId 收款主体 ID
     * @param subjectName 收款主体名称
     * @param payerBankCode 网银付款银行编码
     * @param payerBankName 网银付款银行名称
     * @param payerAccountNo 网银付款账号或卡号
     * @param payerName 网银付款户名
     */
    record PaymentApplyCommand(
            Long tenantId,
            String channelCode,
            Long contractId,
            String contractConfigValuesJson,
            String payOrderNo,
            String bizOrderNo,
            String methodCode,
            String methodName,
            String paymentMaterialType,
            Long amount,
            String currency,
            String title,
            LocalDateTime expireTime,
            Long subjectId,
            String subjectName,
            String payerBankCode,
            String payerBankName,
            String payerAccountNo,
            String payerName) {
    }

    /**
     * 退款命令。
     *
     * @param tenantId 租户 ID
     * @param channelCode 通道编码
     * @param contractId 签约配置 ID
     * @param refundOrderNo 退款订单号
     * @param bizRefundNo 业务退款单号
     * @param payOrderNo 原支付订单号
     * @param bizOrderNo 业务订单号
     * @param channelTradeNo 原通道交易号
     * @param amount 退款金额，单位分
     * @param currency 币种
     * @param reason 退款原因
     */
    record RefundApplyCommand(
            Long tenantId,
            String channelCode,
            Long contractId,
            String refundOrderNo,
            String bizRefundNo,
            String payOrderNo,
            String bizOrderNo,
            String channelTradeNo,
            Long amount,
            String currency,
            String reason) {
    }

    /**
     * 通道账单生成命令。
     *
     * @param tenantId 租户 ID
     * @param channelCode 通道编码
     * @param contractId 签约配置 ID
     * @param billDate 账单日期
     */
    record ChannelBillCommand(Long tenantId, String channelCode, Long contractId, LocalDate billDate) {
    }

    /**
     * 支付查单命令。
     *
     * @param tenantId 租户 ID
     * @param order 支付订单
     */
    record PaymentQueryCommand(Long tenantId, PaymentOrderEntity order) {
    }

    /**
     * 退款查单命令。
     *
     * @param tenantId 租户 ID
     * @param refundOrder 退款订单
     */
    record RefundQueryCommand(Long tenantId, PaymentRefundOrderVO refundOrder) {
    }

    /**
     * 支付结果。
     *
     * @param scenario 通道场景
     * @param returnCode 通道返回码
     * @param resultType 通道结果类型
     * @param status 支付订单初始状态
     * @param channelTradeNo 通道交易号
     * @param material 通道返回的支付物料
     */
    record PaymentApplyResult(
            String scenario,
            String returnCode,
            String resultType,
            String status,
            String channelTradeNo,
            PaymentCashierPayMaterialVO material) {
    }

    /**
     * 退款结果。
     *
     * @param scenario 通道场景
     * @param returnCode 通道返回码
     * @param resultType 通道结果类型
     * @param status 退款订单初始状态
     * @param channelRefundNo 通道退款单号
     */
    record RefundApplyResult(String scenario, String returnCode, String resultType, String status, String channelRefundNo) {
    }

    /**
     * 通道账单结果。
     *
     * @param rows 账单明细行
     */
    record ChannelBillResult(List<PaymentChannelBillItemRow> rows) {
    }

    /**
     * 支付查单结果。
     *
     * @param scenario 通道场景
     * @param returnCode 通道返回码
     * @param resultType 通道结果类型
     * @param status 支付订单目标状态
     */
    record PaymentQueryResult(String scenario, String returnCode, String resultType, String status) {
    }

    /**
     * 退款查单结果。
     *
     * @param scenario 通道场景
     * @param returnCode 通道返回码
     * @param resultType 通道结果类型
     * @param status 退款订单目标状态
     */
    record RefundQueryResult(String scenario, String returnCode, String resultType, String status) {
    }
}
