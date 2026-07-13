package io.mango.payment.starter.resource;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.payment.core.entity.PaymentChannelContractEntity;
import io.mango.payment.core.entity.PaymentChannelContractValueEntity;
import io.mango.payment.core.mapper.PaymentChannelContractMapper;
import io.mango.payment.core.mapper.PaymentChannelContractValueMapper;
import io.mango.resource.api.enums.ResourceFieldType;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceField;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentTableResourceHandlerTest {

    private final PaymentChannelContractMapper mapper = mock(PaymentChannelContractMapper.class);
    private final PaymentTableResourceHandler<PaymentChannelContractEntity> handler =
            new PaymentTableResourceHandler<>(
                    mapper,
                    new ObjectMapper(),
                    new PaymentTableResourceHandler.Definition<>(
                            "PAYMENT_TEST_CONFIG",
                            "payment_channel_contract",
                            PaymentChannelContractEntity.class,
                            Map.of("targetId", "id", "contractName", "contract_name",
                                    "configValuesJson", "config_values_json"),
                            Set.of("targetId", "contractName"),
                            List.of(),
                            "status",
                            null));

    @Test
    void upsertMapsOnlyDeclaredFieldsAndSerializesJson() {
        when(mapper.selectById(anyLong())).thenReturn(null);
        ResourceDeclaration declaration = declaration(Map.of(
                "targetId", field(ResourceFieldType.LONG, 101L),
                "contractName", field(ResourceFieldType.STRING, "default"),
                "configValuesJson", field(ResourceFieldType.JSON, Map.of("mode", "SAFE"))));

        handler.upsert(declaration);

        ArgumentCaptor<PaymentChannelContractEntity> entity =
                ArgumentCaptor.forClass(PaymentChannelContractEntity.class);
        verify(mapper).insert(entity.capture());
        assertThat(entity.getValue().getId()).isEqualTo(101L);
        assertThat(entity.getValue().getContractName()).isEqualTo("default");
        assertThat(entity.getValue().getConfigValuesJson()).isEqualTo("{\"mode\":\"SAFE\"}");
    }

    @Test
    void upsertRejectsUnknownFieldsAndMerchantSecrets() {
        Map<String, ResourceField> unknownFields = new LinkedHashMap<>();
        unknownFields.put("targetId", field(ResourceFieldType.LONG, 101L));
        unknownFields.put("contractName", field(ResourceFieldType.STRING, "default"));
        unknownFields.put("unexpected", field(ResourceFieldType.STRING, "value"));
        assertThatThrownBy(() -> handler.upsert(declaration(unknownFields)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported payment resource fields");

        assertThatThrownBy(() -> handler.upsert(declaration(Map.of(
                "targetId", field(ResourceFieldType.LONG, 101L),
                "contractName", field(ResourceFieldType.STRING, "default"),
                "configValuesJson", field(ResourceFieldType.JSON, Map.of("privateKey", "secret"))))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not contain merchant secrets");
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void disableUsesTheConfiguredLifecycleStrategy() {
        handler.disable(declaration(Map.of(
                "targetId", field(ResourceFieldType.LONG, 101L),
                "contractName", field(ResourceFieldType.STRING, "default"))));

        ArgumentCaptor<UpdateWrapper<PaymentChannelContractEntity>> update =
                (ArgumentCaptor) ArgumentCaptor.forClass(UpdateWrapper.class);
        verify(mapper).update(isNull(), update.capture());
        assertThat(update.getValue().getSqlSet()).contains("status=");
        assertThat(update.getValue().getCustomSqlSegment()).contains("id");
        assertThat(update.getValue().getParamNameValuePairs().values()).contains(0, 101L);

        PaymentChannelContractValueMapper valueMapper = mock(PaymentChannelContractValueMapper.class);
        PaymentTableResourceHandler<PaymentChannelContractValueEntity> valueHandler =
                new PaymentTableResourceHandler<>(
                        valueMapper,
                        new ObjectMapper(),
                        new PaymentTableResourceHandler.Definition<>(
                                "PAYMENT_TEST_CONFIG",
                                "payment_channel_contract_value",
                                PaymentChannelContractValueEntity.class,
                                Map.of("targetId", "id", "contractName", "contract_name"),
                                Set.of("targetId", "contractName"),
                                List.of(),
                                null,
                                valueMapper::deletePhysicallyById));
        valueHandler.disable(declaration(Map.of(
                "targetId", field(ResourceFieldType.LONG, 101L),
                "contractName", field(ResourceFieldType.STRING, "default"))));
        verify(valueMapper).deletePhysicallyById(101L);
    }

    private ResourceDeclaration declaration(Map<String, ResourceField> fields) {
        ResourceDeclaration declaration = new ResourceDeclaration();
        declaration.setResourceType("PAYMENT_TEST_CONFIG");
        declaration.setBizKey("payment.test.default");
        declaration.setFields(new LinkedHashMap<>(fields));
        return declaration;
    }

    private ResourceField field(ResourceFieldType type, Object value) {
        ResourceField field = new ResourceField();
        field.setType(type);
        field.setValue(value);
        return field;
    }
}
