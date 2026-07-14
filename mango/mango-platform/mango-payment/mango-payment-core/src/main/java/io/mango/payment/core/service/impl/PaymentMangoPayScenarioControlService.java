package io.mango.payment.core.service.impl;

import io.mango.payment.core.service.IPaymentMangoPayScenarioControlService;
import io.mango.payment.core.service.PaymentContextSupport;
import io.mango.payment.core.service.PaymentMangoPayResultTranslator;
import io.mango.payment.core.service.PaymentNumberGenerator;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.mango.common.result.Require;
import io.mango.payment.api.enums.PaymentCode;
import io.mango.payment.api.command.CreateMangoPayScenarioControlCommand;
import io.mango.payment.core.entity.PaymentChannelEntity;
import io.mango.payment.core.entity.PaymentChannelContractEntity;
import io.mango.payment.core.entity.PaymentMangoPayScenarioControlEntity;
import io.mango.payment.core.mapper.PaymentChannelContractMapper;
import io.mango.payment.core.mapper.PaymentChannelMapper;
import io.mango.payment.core.mapper.PaymentMangoPayScenarioControlMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaymentMangoPayScenarioControlService implements IPaymentMangoPayScenarioControlService {

    private static final String MANGO_PAY_CHANNEL_CODE = "MANGO_PAY";
    private static final Set<String> SCENARIO_TYPES = Set.of("PAYMENT", "PAYMENT_QUERY", "REFUND", "REFUND_QUERY", "BILL", "CALLBACK_DELAY");
    private static final Set<String> BILL_DIFFERENCE_TYPES = Set.of("AMOUNT_PLUS", "AMOUNT_MINUS");

    private final PaymentMangoPayScenarioControlMapper scenarioControlMapper;
    private final PaymentChannelMapper channelMapper;
    private final PaymentChannelContractMapper channelContractMapper;
    private final PaymentMangoPayResultTranslator resultMappingService;
    private final PaymentOperationAuditService auditService;
    private final PaymentNumberGenerator numberService;

    @Transactional(rollbackFor = Exception.class)
    public Long createScenarioControl(CreateMangoPayScenarioControlCommand command) {
        Require.notNull(command, PaymentCode.PAYMENT_MANGO_PAY_SCENARIO_INVALID);
        String tenantId = PaymentContextSupport.currentTenantId();
        String channelCode = normalize(command.getChannelCode());
        String scenarioType = normalize(command.getScenarioType());
        String scenarioCode = normalize(command.getScenarioCode());
        String billDifferenceType = normalize(command.getBillDifferenceType());
        Require.isTrue(MANGO_PAY_CHANNEL_CODE.equals(channelCode),
                PaymentCode.PAYMENT_MANGO_PAY_SCENARIO_INVALID, "异常场景控制仅支持芒果支付内置虚拟通道");
        Require.isTrue(SCENARIO_TYPES.contains(scenarioType),
                PaymentCode.PAYMENT_MANGO_PAY_SCENARIO_INVALID, "场景类型仅支持 PAYMENT、PAYMENT_QUERY、REFUND、REFUND_QUERY、BILL、CALLBACK_DELAY");
        Require.notNull(command.getEffectiveCount(), PaymentCode.PAYMENT_MANGO_PAY_SCENARIO_INVALID, "生效次数不能为空");
        Require.isTrue(command.getEffectiveCount() > 0 && command.getEffectiveCount() <= 100,
                PaymentCode.PAYMENT_MANGO_PAY_SCENARIO_INVALID, "生效次数必须在 1 到 100 之间");
        requireMangoPayChannel(tenantId, channelCode);
        requireContract(tenantId, command.getContractId(), channelCode);
        if ("BILL".equals(scenarioType)) {
            Require.isTrue(BILL_DIFFERENCE_TYPES.contains(billDifferenceType),
                    PaymentCode.PAYMENT_MANGO_PAY_SCENARIO_INVALID, "账单差异类型仅支持 AMOUNT_PLUS、AMOUNT_MINUS");
            Require.notNull(command.getDifferenceAmount(), PaymentCode.PAYMENT_MANGO_PAY_SCENARIO_INVALID, "账单差异金额不能为空");
            Require.isTrue(command.getDifferenceAmount() > 0,
                    PaymentCode.PAYMENT_MANGO_PAY_SCENARIO_INVALID, "账单差异金额必须大于 0 分");
            scenarioCode = null;
        } else if ("CALLBACK_DELAY".equals(scenarioType)) {
            Require.notNull(command.getCallbackDelayMinutes(), PaymentCode.PAYMENT_MANGO_PAY_SCENARIO_INVALID, "回调延迟分钟数不能为空");
            Require.isTrue(command.getCallbackDelayMinutes() > 0 && command.getCallbackDelayMinutes() <= 1440,
                    PaymentCode.PAYMENT_MANGO_PAY_SCENARIO_INVALID, "回调延迟分钟数必须在 1 到 1440 之间");
            scenarioCode = null;
            billDifferenceType = null;
            command.setDifferenceAmount(null);
        } else {
            Require.notBlank(scenarioCode, PaymentCode.PAYMENT_MANGO_PAY_SCENARIO_INVALID, "交易场景码不能为空");
            billDifferenceType = null;
            command.setDifferenceAmount(null);
            command.setCallbackDelayMinutes(null);
            validateScenarioCode(scenarioType, scenarioCode);
        }

        LocalDateTime now = LocalDateTime.now();
        Long operatorId = PaymentContextSupport.currentUserId();
        PaymentMangoPayScenarioControlEntity entity = new PaymentMangoPayScenarioControlEntity();
        entity.setControlNo(numberService.next(PaymentNumberGenerator.PAY_MANGO_SCENARIO_NO));
        entity.setChannelCode(channelCode);
        entity.setContractId(command.getContractId());
        entity.setScenarioType(scenarioType);
        entity.setScenarioCode(scenarioCode);
        entity.setBillDifferenceType(billDifferenceType);
        entity.setDifferenceAmount(command.getDifferenceAmount());
        entity.setCallbackDelayMinutes(command.getCallbackDelayMinutes());
        entity.setEffectiveCount(command.getEffectiveCount());
        entity.setConsumedCount(0);
        entity.setStatus("ACTIVE");
        entity.setRemark(PaymentContextSupport.trimToNull(command.getRemark()));
        entity.setTenantId(tenantId);
        entity.setCreatedBy(operatorId);
        entity.setCreatedAt(now);
        entity.setUpdatedBy(operatorId);
        entity.setUpdatedAt(now);
        scenarioControlMapper.insert(entity);
        auditService.record(new PaymentOperationAuditService.AuditEntry(
                PaymentOperationAuditService.ACTION_CREATE_MANGO_PAY_CHANNEL_SCENARIO,
                PaymentOperationAuditService.RESOURCE_PAYMENT_MANGO_PAY_CHANNEL_SCENARIO,
                entity.getControlNo(),
                PaymentOperationAuditService.RESULT_SUCCESS));
        return entity.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentMangoPayResultTranslator.PaymentChannelResult consumePaymentScenario(Long contractId, String scenarioType) {
        PaymentMangoPayScenarioControlEntity control = consumeNext(MANGO_PAY_CHANNEL_CODE, contractId, scenarioType);
        if (control == null) {
            return null;
        }
        return resultMappingService.mapPayment(Map.of("mangoPayScenario", control.getScenarioCode()));
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentMangoPayResultTranslator.RefundChannelResult consumeRefundScenario(Long contractId, String scenarioType) {
        PaymentMangoPayScenarioControlEntity control = consumeNext(MANGO_PAY_CHANNEL_CODE, contractId, scenarioType);
        if (control == null) {
            return null;
        }
        return resultMappingService.mapRefund(Map.of("mangoPayRefundScenario", control.getScenarioCode()));
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentMangoPayScenarioControlEntity consumeBillScenario(Long contractId) {
        return consumeNext(MANGO_PAY_CHANNEL_CODE, contractId, "BILL");
    }

    @Transactional(rollbackFor = Exception.class)
    public PaymentMangoPayScenarioControlEntity consumeCallbackDelayScenario(Long contractId) {
        return consumeNext(MANGO_PAY_CHANNEL_CODE, contractId, "CALLBACK_DELAY");
    }

    private PaymentMangoPayScenarioControlEntity consumeNext(String channelCode, Long contractId, String scenarioType) {
        String normalizedType = normalize(scenarioType);
        Require.isTrue(SCENARIO_TYPES.contains(normalizedType),
                PaymentCode.PAYMENT_MANGO_PAY_SCENARIO_INVALID, "场景类型不正确");
        String tenantId = PaymentContextSupport.currentTenantId();
        PaymentMangoPayScenarioControlEntity control = scenarioControlMapper.selectNextActive(tenantId, channelCode, contractId, normalizedType);
        if (control == null) {
            return null;
        }
        int updated = scenarioControlMapper.consume(tenantId, control.getId(), PaymentContextSupport.currentUserId());
        Require.isTrue(updated == 1,
                PaymentCode.PAYMENT_MANGO_PAY_SCENARIO_INVALID, "芒果支付场景控制已被消费，请重试");
        control.setConsumedCount(control.getConsumedCount() + 1);
        if (control.getConsumedCount() >= control.getEffectiveCount()) {
            control.setStatus("CONSUMED");
        }
        control.setConsumedAt(LocalDateTime.now());
        return control;
    }

    private void requireMangoPayChannel(String tenantId, String channelCode) {
        PaymentChannelEntity channel = channelMapper.selectOne(new LambdaQueryWrapper<PaymentChannelEntity>()
                .eq(PaymentChannelEntity::getTenantId, tenantId)
                .eq(PaymentChannelEntity::getChannelCode, channelCode)
                .eq(PaymentChannelEntity::getStatus, 1)
                .last("limit 1"));
        Require.notNull(channel, PaymentCode.PAYMENT_CHANNEL_NOT_FOUND, "芒果支付内置虚拟通道不存在或未启用");
    }

    private void requireContract(String tenantId, Long contractId, String channelCode) {
        if (contractId == null) {
            return;
        }
        PaymentChannelContractEntity contract = channelContractMapper.selectById(contractId);
        Require.notNull(contract, PaymentCode.PAYMENT_CHANNEL_CONTRACT_NOT_FOUND);
        Require.isTrue(tenantId.equals(contract.getTenantId()), PaymentCode.PAYMENT_CHANNEL_CONTRACT_NOT_FOUND);
        PaymentChannelEntity channel = channelMapper.selectById(contract.getChannelId());
        Require.notNull(channel, PaymentCode.PAYMENT_CHANNEL_NOT_FOUND);
        Require.isTrue(tenantId.equals(channel.getTenantId()) && channelCode.equals(channel.getChannelCode()),
                PaymentCode.PAYMENT_MANGO_PAY_SCENARIO_INVALID, "签约配置不属于芒果支付内置虚拟通道");
    }

    private void validateScenarioCode(String scenarioType, String scenarioCode) {
        if ("PAYMENT".equals(scenarioType) || "PAYMENT_QUERY".equals(scenarioType)) {
            PaymentMangoPayResultTranslator.PaymentChannelResult result =
                    resultMappingService.mapPayment(Map.of("mangoPayScenario", scenarioCode));
            Require.isTrue(!"UNKNOWN".equals(result.resultType()) || "UNKNOWN".equals(result.scenario()),
                    PaymentCode.PAYMENT_MANGO_PAY_SCENARIO_INVALID, "支付场景码不受支持");
            return;
        }
        PaymentMangoPayResultTranslator.RefundChannelResult result =
                resultMappingService.mapRefund(Map.of("mangoPayRefundScenario", scenarioCode));
        Require.isTrue(!"UNKNOWN".equals(result.resultType()) || "UNKNOWN".equals(result.scenario()),
                PaymentCode.PAYMENT_MANGO_PAY_SCENARIO_INVALID, "退款场景码不受支持");
    }

    private String normalize(String value) {
        String trimmed = PaymentContextSupport.trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

}
