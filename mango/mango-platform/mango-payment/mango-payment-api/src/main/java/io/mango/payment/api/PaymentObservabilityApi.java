package io.mango.payment.api;

import io.mango.common.result.R;
import io.mango.payment.api.vo.PaymentObservabilitySnapshotVO;
import org.springframework.validation.annotation.Validated;

@Validated
public interface PaymentObservabilityApi {

    R<PaymentObservabilitySnapshotVO> observabilitySnapshot();
}
