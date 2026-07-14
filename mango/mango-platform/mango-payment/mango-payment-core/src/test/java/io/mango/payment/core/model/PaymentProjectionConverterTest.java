package io.mango.payment.core.model;

import io.mango.payment.api.vo.PaymentOrderVO;
import io.mango.payment.core.model.projection.PaymentOrderProjection;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentProjectionConverterTest {

    @Test
    void toApiCopiesFlatReadModelPropertiesWithoutSharingTheProjectionInstance() {
        LocalDateTime paidAt = LocalDateTime.of(2026, 7, 13, 12, 30);
        PaymentOrderProjection projection = new PaymentOrderProjection();
        projection.setId(370001L);
        projection.setPayOrderNo("PO202607130001");
        projection.setAmount(12800L);
        projection.setStatus("SUCCESS");
        projection.setPayTime(paidAt);

        PaymentOrderVO result = PaymentProjectionConverter.toApi(projection, PaymentOrderVO.class);

        assertThat(result).isNotSameAs(projection);
        assertThat(result.getId()).isEqualTo(370001L);
        assertThat(result.getPayOrderNo()).isEqualTo("PO202607130001");
        assertThat(result.getAmount()).isEqualTo(12800L);
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getPayTime()).isEqualTo(paidAt);
    }

    @Test
    void toApiListPreservesOrderAndNormalizesAbsentRowsToEmptyList() {
        PaymentOrderProjection first = new PaymentOrderProjection();
        first.setPayOrderNo("PO-1");
        PaymentOrderProjection second = new PaymentOrderProjection();
        second.setPayOrderNo("PO-2");

        assertThat(PaymentProjectionConverter.toApiList(List.of(first, second), PaymentOrderVO.class))
                .extracting(PaymentOrderVO::getPayOrderNo)
                .containsExactly("PO-1", "PO-2");
        assertThat(PaymentProjectionConverter.toApiList(null, PaymentOrderVO.class)).isEmpty();
        assertThat(PaymentProjectionConverter.toApi(null, PaymentOrderVO.class)).isNull();
    }
}
