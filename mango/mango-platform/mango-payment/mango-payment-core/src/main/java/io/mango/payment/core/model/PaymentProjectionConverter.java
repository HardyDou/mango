package io.mango.payment.core.model;

import io.mango.common.result.Require;
import io.mango.payment.api.enums.PaymentCode;
import org.springframework.beans.BeanUtils;

import java.util.List;

/** 持久化查询投影到支付 API 读模型的边界转换器。 */
public final class PaymentProjectionConverter {

    private PaymentProjectionConverter() {
    }

    public static <T> T toApi(Object projection, Class<T> apiType) {
        if (projection == null) {
            return null;
        }
        Require.notNull(apiType, PaymentCode.PAYMENT_READONLY_RESOURCE_INVALID, "支付 API 读模型类型不能为空");
        T target = BeanUtils.instantiateClass(apiType);
        BeanUtils.copyProperties(projection, target);
        return target;
    }

    public static <T> List<T> toApiList(List<?> projections, Class<T> apiType) {
        if (projections == null || projections.isEmpty()) {
            return List.of();
        }
        return projections.stream().map(projection -> toApi(projection, apiType)).toList();
    }
}
