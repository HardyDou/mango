package io.mango.infra.persistence.starter.datasource;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

/**
 * 判断是否启用 Mango 托管多数据源。
 */
class PersistenceManagedDataSourceCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, PersistenceDataSourceProperties.DataSourceConfig> datasources = Binder.get(context.getEnvironment())
                .bind("mango.persistence.datasources",
                        Bindable.mapOf(String.class, PersistenceDataSourceProperties.DataSourceConfig.class))
                .orElse(Map.of());
        if (datasources.isEmpty()) {
            return ConditionOutcome.noMatch(ConditionMessage.forCondition("Mango persistence datasources")
                    .didNotFind("configured datasource").atAll());
        }
        return ConditionOutcome.match(ConditionMessage.forCondition("Mango persistence datasources")
                .found("configured datasources").items(datasources.keySet()));
    }
}
