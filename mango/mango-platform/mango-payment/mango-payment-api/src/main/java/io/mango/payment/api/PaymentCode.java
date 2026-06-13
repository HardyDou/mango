package io.mango.payment.api;

import io.mango.common.result.BizCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付模块业务码。
 */
@Getter
@AllArgsConstructor
public enum PaymentCode implements BizCode {

    /** 支付应用参数不正确。 */
    PAYMENT_APPLICATION_INVALID(3701, "支付应用参数不正确"),

    /** 支付应用不存在。 */
    PAYMENT_APPLICATION_NOT_FOUND(3704, "支付应用不存在"),

    /** 支付应用 AppId 生成失败。 */
    PAYMENT_APPLICATION_APP_ID_GENERATE_FAILED(3705, "支付应用 AppId 生成失败，请重试"),

    /** 支付应用存在关联数据，不能删除。 */
    PAYMENT_APPLICATION_DELETE_HAS_RELATIONS(3709, "支付应用存在关联数据，不能删除，请先清理关联数据"),

    /** 支付应用删除失败。 */
    PAYMENT_APPLICATION_DELETE_FAILED(3710, "支付应用删除失败"),

    /** 企业主体参数不正确。 */
    PAYMENT_ENTERPRISE_SUBJECT_INVALID(3721, "企业主体参数不正确"),

    /** 企业主体不存在。 */
    PAYMENT_ENTERPRISE_SUBJECT_NOT_FOUND(3724, "企业主体不存在"),

    /** 企业主体存在关联数据，不能删除。 */
    PAYMENT_ENTERPRISE_SUBJECT_DELETE_HAS_RELATIONS(3729, "企业主体存在关联数据，不能删除，请先清理关联数据"),

    /** 企业主体删除失败。 */
    PAYMENT_ENTERPRISE_SUBJECT_DELETE_FAILED(3730, "企业主体删除失败"),

    /** 支付通道参数不正确。 */
    PAYMENT_CHANNEL_INVALID(3731, "支付通道参数不正确"),

    /** 支付通道不存在。 */
    PAYMENT_CHANNEL_NOT_FOUND(3734, "支付通道不存在"),

    /** 支付通道存在关联数据，不能删除。 */
    PAYMENT_CHANNEL_DELETE_HAS_RELATIONS(3739, "支付通道存在关联数据，不能删除，请先清理关联数据"),

    /** 支付通道删除失败。 */
    PAYMENT_CHANNEL_DELETE_FAILED(3740, "支付通道删除失败"),

    /** 支付内置通道不允许删除。 */
    PAYMENT_CHANNEL_BUILTIN_DELETE_FORBIDDEN(3816, "支付内置通道不允许删除"),

    /** 通道签约配置参数不正确。 */
    PAYMENT_CHANNEL_CONTRACT_INVALID(3741, "通道签约配置参数不正确"),

    /** 通道签约配置不存在。 */
    PAYMENT_CHANNEL_CONTRACT_NOT_FOUND(3744, "通道签约配置不存在"),

    /** 通道签约配置存在关联数据，不能删除。 */
    PAYMENT_CHANNEL_CONTRACT_DELETE_HAS_RELATIONS(3749, "通道签约配置存在关联数据，不能删除，请先清理关联数据"),

    /** 通道签约配置删除失败。 */
    PAYMENT_CHANNEL_CONTRACT_DELETE_FAILED(3750, "通道签约配置删除失败"),

    /** 通道签约配置字段模板不正确。 */
    PAYMENT_CHANNEL_CONTRACT_TEMPLATE_INVALID(3751, "通道签约配置字段模板不正确"),

    /** 通道签约配置值不正确。 */
    PAYMENT_CHANNEL_CONTRACT_VALUE_INVALID(3752, "通道签约配置值不正确"),

    /** 签约能力不正确。 */
    PAYMENT_CHANNEL_CONTRACT_CAPABILITY_INVALID(3753, "签约能力不正确"),

    /** 支付方式参数不正确。 */
    PAYMENT_METHOD_INVALID(3754, "支付方式参数不正确"),

    /** 支付方式不存在。 */
    PAYMENT_METHOD_NOT_FOUND(3755, "支付方式不存在"),

    /** 支付方式存在关联数据，不能删除。 */
    PAYMENT_METHOD_DELETE_HAS_RELATIONS(3759, "支付方式存在关联数据，不能删除，请先清理关联数据"),

    /** 支付方式删除失败。 */
    PAYMENT_METHOD_DELETE_FAILED(3760, "支付方式删除失败"),

    /** 支付金额不正确。 */
    PAYMENT_AMOUNT_INVALID(3761, "支付金额不正确"),

    /** 支付方式路由规则参数不正确。 */
    PAYMENT_METHOD_ROUTE_INVALID(3796, "支付方式路由规则参数不正确"),

    /** 支付方式路由规则不存在。 */
    PAYMENT_METHOD_ROUTE_NOT_FOUND(3797, "支付方式路由规则不存在"),

    /** 支付方式路由规则存在关联数据，不能删除。 */
    PAYMENT_METHOD_ROUTE_DELETE_HAS_RELATIONS(3798, "支付方式路由规则存在关联数据，不能删除，请先清理关联数据"),

    /** 支付方式路由规则删除失败。 */
    PAYMENT_METHOD_ROUTE_DELETE_FAILED(3799, "支付方式路由规则删除失败"),

    /** 收银台配置参数不正确。 */
    PAYMENT_CASHIER_CONFIG_INVALID(3762, "收银台配置参数不正确"),

    /** 收银台配置不存在。 */
    PAYMENT_CASHIER_CONFIG_NOT_FOUND(3763, "收银台配置不存在"),

    /** 收银台配置存在关联数据，不能删除。 */
    PAYMENT_CASHIER_CONFIG_DELETE_HAS_RELATIONS(3764, "收银台配置存在关联数据，不能删除，请先清理关联数据"),

    /** 收银台配置删除失败。 */
    PAYMENT_CASHIER_CONFIG_DELETE_FAILED(3765, "收银台配置删除失败"),

    /** 支付只读资源参数不正确。 */
    PAYMENT_READONLY_RESOURCE_INVALID(3766, "支付查询参数不正确"),

    /** 支付只读资源不存在。 */
    PAYMENT_READONLY_RESOURCE_NOT_FOUND(3767, "支付查询数据不存在"),

    /** 业务订单参数不正确。 */
    PAYMENT_BUSINESS_ORDER_INVALID(3768, "业务订单参数不正确"),

    /** 业务订单不存在。 */
    PAYMENT_BUSINESS_ORDER_NOT_FOUND(3769, "业务订单不存在"),

    /** 业务订单当前不可支付。 */
    PAYMENT_BUSINESS_ORDER_NOT_PAYABLE(3770, "业务订单当前不可支付"),

    /** 业务订单已完成支付。 */
    PAYMENT_BUSINESS_ORDER_ALREADY_PAID(3771, "业务订单已完成支付"),

    /** 业务订单状态已变化。 */
    PAYMENT_BUSINESS_ORDER_STATE_CHANGED(3772, "业务订单状态已变化，请刷新后重试"),

    /** 收银台支付参数不正确。 */
    PAYMENT_CASHIER_PAY_INVALID(3773, "收银台支付参数不正确"),

    /** 支付订单不存在。 */
    PAYMENT_ORDER_NOT_FOUND(3774, "支付订单不存在"),

    /** 异常订单参数不正确。 */
    PAYMENT_EXCEPTION_ORDER_INVALID(3775, "异常订单参数不正确"),

    /** 异常订单不存在。 */
    PAYMENT_EXCEPTION_ORDER_NOT_FOUND(3776, "异常订单不存在"),

    /** 异常订单当前状态不允许处理。 */
    PAYMENT_EXCEPTION_ORDER_HANDLE_STATUS_INVALID(3777, "异常订单当前状态不允许处理"),

    /** 通知记录参数不正确。 */
    PAYMENT_NOTIFICATION_RECORD_INVALID(3778, "通知记录参数不正确"),

    /** 通知记录不存在。 */
    PAYMENT_NOTIFICATION_RECORD_NOT_FOUND(3779, "通知记录不存在"),

    /** 通知记录当前状态不允许重推。 */
    PAYMENT_NOTIFICATION_RECORD_RETRY_STATUS_INVALID(3780, "通知记录当前状态不允许重推"),

    /** 对账批次参数不正确。 */
    PAYMENT_RECONCILIATION_INVALID(3781, "对账批次参数不正确"),

    /** 对账批次不存在。 */
    PAYMENT_RECONCILIATION_NOT_FOUND(3782, "对账批次不存在"),

    /** 通道账单文件已导入。 */
    PAYMENT_RECONCILIATION_FILE_DUPLICATED(3783, "同一通道、同一账单日期、同一账单文件已导入"),

    /** 对账差异参数不正确。 */
    PAYMENT_DIFFERENCE_INVALID(3784, "对账差异参数不正确"),

    /** 对账差异不存在。 */
    PAYMENT_DIFFERENCE_NOT_FOUND(3785, "对账差异不存在"),

    /** 对账差异当前状态不允许处理。 */
    PAYMENT_DIFFERENCE_HANDLE_STATUS_INVALID(3786, "对账差异当前状态不允许处理"),

    /** 结算汇总参数不正确。 */
    PAYMENT_SETTLEMENT_SUMMARY_INVALID(3787, "结算汇总参数不正确"),

    /** 结算汇总不存在。 */
    PAYMENT_SETTLEMENT_SUMMARY_NOT_FOUND(3788, "结算汇总不存在"),

    /** 结算汇总当前状态不允许操作。 */
    PAYMENT_SETTLEMENT_SUMMARY_STATUS_INVALID(3789, "结算汇总当前状态不允许操作"),

    /** 结算汇总确认前必须完成对账。 */
    PAYMENT_SETTLEMENT_RECONCILIATION_REQUIRED(3790, "结算汇总确认前必须完成对应范围对账"),

    /** 结算汇总存在未处理差异，不能确认。 */
    PAYMENT_SETTLEMENT_UNRESOLVED_DIFFERENCE(3791, "结算汇总存在未处理差异，不能确认"),

    /** 支付开放接口认证失败。 */
    PAYMENT_OPENAPI_AUTH_INVALID(3792, "支付开放接口认证失败"),

    /** 支付开放接口 nonce 已使用。 */
    PAYMENT_OPENAPI_NONCE_REPLAY(3793, "支付开放接口请求已处理，请勿重复提交"),

    /** 支付开放接口幂等冲突。 */
    PAYMENT_OPENAPI_IDEMPOTENT_CONFLICT(3794, "支付开放接口幂等冲突"),

    /** 支付开放接口收银台不可用。 */
    PAYMENT_OPENAPI_CASHIER_UNAVAILABLE(3795, "支付开放接口收银台不可用"),

    /** 退款订单参数不正确。 */
    PAYMENT_REFUND_ORDER_INVALID(3800, "退款订单参数不正确"),

    /** 退款订单不存在。 */
    PAYMENT_REFUND_ORDER_NOT_FOUND(3801, "退款订单不存在"),

    /** 退款金额超过可退金额。 */
    PAYMENT_REFUND_AMOUNT_EXCEEDED(3802, "退款金额超过可退金额"),

    /** 支付订单状态不允许当前操作。 */
    PAYMENT_ORDER_STATE_INVALID(3803, "支付订单状态不允许当前操作"),

    /** 退款订单状态不允许当前操作。 */
    PAYMENT_REFUND_ORDER_STATE_INVALID(3804, "退款订单状态不允许当前操作"),

    /** 芒果支付异常场景控制参数不正确。 */
    PAYMENT_MANGO_PAY_SCENARIO_INVALID(3805, "芒果支付异常场景控制参数不正确"),

    /** 支付敏感字段处理失败。 */
    PAYMENT_SENSITIVE_VALUE_INVALID(3806, "支付敏感字段处理失败"),

    /** 退款审批参数不正确。 */
    PAYMENT_REFUND_APPROVAL_INVALID(3807, "退款审批参数不正确"),

    /** 退款审批不存在。 */
    PAYMENT_REFUND_APPROVAL_NOT_FOUND(3808, "退款审批不存在"),

    /** 退款审批状态不允许当前操作。 */
    PAYMENT_REFUND_APPROVAL_STATUS_INVALID(3809, "退款审批状态不允许当前操作"),

    /** 支付可观测性参数不正确。 */
    PAYMENT_OBSERVABILITY_INVALID(3810, "支付可观测性参数不正确"),

    /** 线下收款参数不正确。 */
    PAYMENT_OFFLINE_COLLECTION_INVALID(3811, "线下收款参数不正确"),

    /** 线下收款不存在。 */
    PAYMENT_OFFLINE_COLLECTION_NOT_FOUND(3812, "线下收款不存在"),

    /** 线下收款状态不允许当前操作。 */
    PAYMENT_OFFLINE_COLLECTION_STATE_INVALID(3813, "线下收款状态不允许当前操作"),

    /** 线下退款参数不正确。 */
    PAYMENT_OFFLINE_REFUND_INVALID(3814, "线下退款参数不正确"),

    /** 线下退款不存在。 */
    PAYMENT_OFFLINE_REFUND_NOT_FOUND(3815, "线下退款不存在");

    private final int code;
    private final String message;
}
