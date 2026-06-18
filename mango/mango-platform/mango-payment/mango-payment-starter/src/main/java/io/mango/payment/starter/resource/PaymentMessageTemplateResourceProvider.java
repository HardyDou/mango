package io.mango.payment.starter.resource;

import io.mango.notice.api.enums.NoticePriority;
import io.mango.notice.api.resource.NoticeMessageTemplateResourceDeclarations;
import io.mango.notice.api.resource.NoticeMessageTemplateResourceDeclarations.MessageTemplateSpec;
import io.mango.resource.api.ResourceProvider;
import io.mango.resource.api.model.ResourceDeclaration;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Payment module message template resources.
 */
@Component
public class PaymentMessageTemplateResourceProvider implements ResourceProvider {

    @Override
    public List<String> moduleCodes() {
        return List.of("payment");
    }

    @Override
    public List<ResourceDeclaration> provide() {
        return specs().stream()
                .flatMap(spec -> NoticeMessageTemplateResourceDeclarations.fourChannels(spec).stream())
                .toList();
    }

    private List<MessageTemplateSpec> specs() {
        return List.of(
                spec(2026061900400010000L, 2060000000001040000L, "payment.order.success", "支付成功",
                        "支付订单成功后通知业务方或运营人员。", NoticePriority.NORMAL,
                        "支付成功：{{payOrderNo}}", "支付订单 {{payOrderNo}} 已支付成功，金额：{{amount}}，通道：{{channelCode}}。",
                        "【Mango】支付成功：{{payOrderNo}}", "支付订单 {{payOrderNo}} 已支付成功，业务单号：{{bizOrderNo}}，金额：{{amount}}，通道：{{channelCode}}。",
                        "支付成功：{{payOrderNo}}", "支付订单 {{payOrderNo}} 已支付成功，金额：{{amount}}。",
                        "支付订单 {{payOrderNo}} 已成功。"),
                spec(2026061900400010100L, 2060000000001040100L, "payment.order.failed", "支付失败",
                        "支付订单失败后通知业务方或运营人员。", NoticePriority.NORMAL,
                        "支付失败：{{payOrderNo}}", "支付订单 {{payOrderNo}} 支付失败，原因：{{failReason}}。",
                        "【Mango】支付失败：{{payOrderNo}}", "支付订单 {{payOrderNo}} 支付失败，业务单号：{{bizOrderNo}}，原因：{{failReason}}。",
                        "支付失败：{{payOrderNo}}", "支付订单 {{payOrderNo}} 支付失败，原因：{{failReason}}。",
                        "支付订单 {{payOrderNo}} 支付失败。"),
                spec(2026061900400010200L, 2060000000001040200L, "payment.refund.success", "退款成功",
                        "退款订单成功后通知业务方或运营人员。", NoticePriority.NORMAL,
                        "退款成功：{{refundOrderNo}}", "退款订单 {{refundOrderNo}} 已退款成功，金额：{{refundAmount}}。",
                        "【Mango】退款成功：{{refundOrderNo}}", "退款订单 {{refundOrderNo}} 已退款成功，支付单号：{{payOrderNo}}，金额：{{refundAmount}}。",
                        "退款成功：{{refundOrderNo}}", "退款订单 {{refundOrderNo}} 已退款成功，金额：{{refundAmount}}。",
                        "退款订单 {{refundOrderNo}} 已成功。"),
                spec(2026061900400010300L, 2060000000001040300L, "payment.refund.failed", "退款失败",
                        "退款订单失败后通知业务方或运营人员。", NoticePriority.HIGH,
                        "退款失败：{{refundOrderNo}}", "退款订单 {{refundOrderNo}} 退款失败，原因：{{failReason}}。",
                        "【Mango】退款失败：{{refundOrderNo}}", "退款订单 {{refundOrderNo}} 退款失败，支付单号：{{payOrderNo}}，原因：{{failReason}}。",
                        "退款失败：{{refundOrderNo}}", "退款订单 {{refundOrderNo}} 退款失败，原因：{{failReason}}。",
                        "退款订单 {{refundOrderNo}} 退款失败。"),
                spec(2026061900400010400L, 2060000000001040400L, "payment.refund.approval.created", "退款审批发起",
                        "退款审批创建后通知审批人或运营人员。", NoticePriority.HIGH,
                        "退款审批已发起：{{approvalNo}}", "退款审批 {{approvalNo}} 已发起，支付单号：{{payOrderNo}}，退款金额：{{refundAmount}}。",
                        "【Mango】退款审批已发起：{{approvalNo}}", "退款审批 {{approvalNo}} 已发起，支付单号：{{payOrderNo}}，退款金额：{{refundAmount}}，原因：{{reason}}。",
                        "退款审批已发起：{{approvalNo}}", "退款审批 {{approvalNo}} 已发起，退款金额：{{refundAmount}}。",
                        "退款审批 {{approvalNo}} 已发起。"),
                spec(2026061900400010500L, 2060000000001040500L, "payment.exception.order.created", "支付异常订单",
                        "支付异常订单创建后通知运营人员。", NoticePriority.URGENT,
                        "支付异常订单：{{exceptionNo}}", "支付异常订单 {{exceptionNo}} 已创建，类型：{{exceptionType}}，关联单号：{{relatedOrderNo}}。",
                        "【Mango】支付异常订单：{{exceptionNo}}", "支付异常订单 {{exceptionNo}} 已创建，类型：{{exceptionType}}，关联单号：{{relatedOrderNo}}。请及时处理。",
                        "支付异常订单：{{exceptionNo}}", "支付异常订单 {{exceptionNo}} 已创建，关联单号：{{relatedOrderNo}}。",
                        "支付异常订单 {{exceptionNo}} 已创建。"),
                spec(2026061900400010600L, 2060000000001040600L, "payment.reconciliation.difference", "对账差异",
                        "对账产生差异后通知运营人员。", NoticePriority.HIGH,
                        "对账存在差异：{{reconciliationNo}}", "对账批次 {{reconciliationNo}} 存在差异，差异数量：{{differenceCount}}，差异金额：{{differenceAmount}}。",
                        "【Mango】对账存在差异：{{reconciliationNo}}", "对账批次 {{reconciliationNo}} 存在差异，差异数量：{{differenceCount}}，差异金额：{{differenceAmount}}。请进入支付/对账差异处理。",
                        "对账存在差异：{{reconciliationNo}}", "对账批次 {{reconciliationNo}} 存在差异，数量：{{differenceCount}}。",
                        "对账批次 {{reconciliationNo}} 存在差异。"),
                spec(2026061900400010700L, 2060000000001040700L, "payment.settlement.unresolved", "结算存在未处理差异",
                        "结算确认前存在未处理差异时通知运营人员。", NoticePriority.HIGH,
                        "结算存在未处理差异：{{settlementNo}}", "结算 {{settlementNo}} 存在未处理差异，数量：{{unresolvedCount}}，金额：{{unresolvedAmount}}。",
                        "【Mango】结算存在未处理差异：{{settlementNo}}", "结算 {{settlementNo}} 存在未处理差异，数量：{{unresolvedCount}}，金额：{{unresolvedAmount}}。请先处理差异后再确认结算。",
                        "结算存在未处理差异：{{settlementNo}}", "结算 {{settlementNo}} 存在未处理差异，数量：{{unresolvedCount}}。",
                        "结算 {{settlementNo}} 存在未处理差异。")
        );
    }

    private MessageTemplateSpec spec(long resourceBase, long targetBase, String bizType, String bizName,
                                     String description, NoticePriority priority, String siteTitle, String siteContent,
                                     String emailTitle, String emailContent, String wecomTitle, String wecomContent,
                                     String smsContent) {
        return new MessageTemplateSpec("payment", "支付中心", resourceBase, targetBase, targetBase + 1,
                targetBase + 2, 1, bizType, bizName, "PAYMENT", "PAYMENT", description, paramsSchema(),
                priority, "BIZ_ID", true, siteTitle, siteContent, emailTitle, emailContent, wecomTitle,
                wecomContent, siteTitle, smsContent);
    }

    private String paramsSchema() {
        return "{\"type\":\"object\",\"properties\":{\"payOrderNo\":{\"type\":\"string\",\"title\":\"支付订单号\"},\"bizOrderNo\":{\"type\":\"string\",\"title\":\"业务订单号\"},\"refundOrderNo\":{\"type\":\"string\",\"title\":\"退款订单号\"},\"approvalNo\":{\"type\":\"string\",\"title\":\"审批单号\"},\"reconciliationNo\":{\"type\":\"string\",\"title\":\"对账批次号\"},\"settlementNo\":{\"type\":\"string\",\"title\":\"结算单号\"},\"amount\":{\"type\":\"number\",\"title\":\"金额\"},\"refundAmount\":{\"type\":\"number\",\"title\":\"退款金额\"},\"channelCode\":{\"type\":\"string\",\"title\":\"通道编码\"},\"failReason\":{\"type\":\"string\",\"title\":\"失败原因\"},\"exceptionNo\":{\"type\":\"string\",\"title\":\"异常单号\"},\"exceptionType\":{\"type\":\"string\",\"title\":\"异常类型\"},\"relatedOrderNo\":{\"type\":\"string\",\"title\":\"关联单号\"},\"differenceCount\":{\"type\":\"number\",\"title\":\"差异数量\"},\"differenceAmount\":{\"type\":\"number\",\"title\":\"差异金额\"},\"unresolvedCount\":{\"type\":\"number\",\"title\":\"未处理数量\"},\"unresolvedAmount\":{\"type\":\"number\",\"title\":\"未处理金额\"},\"reason\":{\"type\":\"string\",\"title\":\"原因\"}}}";
    }
}
