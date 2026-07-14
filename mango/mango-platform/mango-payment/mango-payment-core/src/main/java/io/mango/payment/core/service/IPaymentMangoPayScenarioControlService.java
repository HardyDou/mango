package io.mango.payment.core.service;

public interface IPaymentMangoPayScenarioControlService {
    Long createScenarioControl(
            io.mango.payment.api.command.CreateMangoPayScenarioControlCommand command);
}
