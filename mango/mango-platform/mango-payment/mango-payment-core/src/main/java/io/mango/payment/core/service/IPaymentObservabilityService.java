package io.mango.payment.core.service;

public interface IPaymentObservabilityService {
    io.mango.payment.api.vo.PaymentObservabilitySnapshotVO currentSnapshot();
}
