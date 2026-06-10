package io.mango.payment.api.vo;

import lombok.Data;

/**
 * 支付后台任务触发结果。
 */
@Data
public class PaymentTaskDispatchResultVO {

    /** 扫描记录数。 */
    private int scannedCount;

    /** 处理成功数。 */
    private int successCount;

    /** 跳过记录数。 */
    private int skippedCount;

    /** 处理失败数。 */
    private int failedCount;
}
