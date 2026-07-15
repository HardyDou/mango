package io.mango.infra.event.core;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.mango.infra.event.api.command.ReconsumeSystemEventCommand;
import io.mango.infra.event.api.query.SystemEventPageQuery;
import io.mango.infra.event.api.vo.SystemEventVO;
import io.mango.infra.event.core.system.SystemEventService;
import io.mango.infra.event.starter.controller.SystemEventController;
import io.mango.infra.kv.api.IOutboxStore;
import io.mango.infra.kv.api.OutboxMessage;
import io.mango.infra.kv.api.OutboxStatus;
import io.mango.infra.kv.api.OutboxTopics;
import io.mango.infra.kv.core.memory.MemoryKvStore;
import io.mango.infra.kv.core.outbox.KvOutboxStore;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SystemEventServiceIntegrationTest {

    private MemoryKvStore kvStore;
    private IOutboxStore outboxStore;
    private SystemEventService service;

    @BeforeEach
    void setUp() {
        kvStore = new MemoryKvStore();
        outboxStore = new KvOutboxStore(kvStore, JsonMapper.builder().findAndAddModules().build());
        service = new SystemEventService(outboxStore, Clock.systemUTC());
    }

    @AfterEach
    void tearDown() {
        kvStore.close();
    }

    @Test
    void pageAndDetail_shouldExposeOnlyDomainEventTopicAndPreserveJsonContract() throws Exception {
        OutboxMessage domainEvent = message("event-domain", OutboxTopics.DOMAIN_EVENT, OutboxStatus.FAILED);
        OutboxMessage noticeEvent = message("event-notice", OutboxTopics.NOTICE, OutboxStatus.FAILED);
        outboxStore.enqueue(domainEvent);
        outboxStore.enqueue(noticeEvent);

        SystemEventPageQuery query = new SystemEventPageQuery();
        query.setAbnormalOnly(false);
        var page = service.page(query);

        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getList()).extracting(SystemEventVO::getMessageId).containsExactly("event-domain");
        assertThat(service.detail("event-domain")).isNotNull();
        assertThat(service.detail("event-notice")).isNull();

        SystemEventVO detail = service.detail("event-domain");
        detail.getPayload().put("changed", true);
        detail.getHeaders().put("tenantId", "changed");
        assertThat(service.detail("event-domain").getPayload()).doesNotContainKey("changed");
        assertThat(service.detail("event-domain").getHeaders()).containsEntry("tenantId", "1");

        Map<String, Object> sourcePayload = new LinkedHashMap<>();
        sourcePayload.put("amount", 100);
        detail.setPayload(sourcePayload);
        sourcePayload.put("changedAfterSet", true);
        assertThat(detail.getPayload()).doesNotContainKey("changedAfterSet");

        JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();
        String json = mapper.writeValueAsString(detail);
        assertThat(json).contains("\"payload\":{\"amount\":100}")
                .contains("\"headers\":{\"tenantId\":\"1\"}")
                .doesNotContain("\"json\"");
        SystemEventVO roundTrip = mapper.readValue(json, SystemEventVO.class);
        assertThat(roundTrip.getPayload()).containsEntry("amount", 100);
        assertThat(roundTrip.getHeaders()).containsEntry("tenantId", "1");
    }

    @Test
    void reconsume_shouldRejectSuccessForeignTopicAndMissingMessage() {
        outboxStore.enqueue(message("event-failed", OutboxTopics.DOMAIN_EVENT, OutboxStatus.FAILED));
        outboxStore.enqueue(message("event-success", OutboxTopics.DOMAIN_EVENT, OutboxStatus.SUCCESS));
        outboxStore.enqueue(message("notice-failed", OutboxTopics.NOTICE, OutboxStatus.FAILED));

        assertThat(service.reconsume(command("event-failed"))).isTrue();
        assertThat(outboxStore.findById("event-failed").getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(service.reconsume(command("event-success"))).isFalse();
        assertThat(outboxStore.findById("event-success").getStatus()).isEqualTo(OutboxStatus.SUCCESS);
        assertThat(service.reconsume(command("notice-failed"))).isFalse();
        assertThat(service.reconsume(command("missing"))).isFalse();
    }

    @Test
    void apiOwnedValidation_shouldApplyWithoutControllerConstraintRedefinition() throws Exception {
        SystemEventController controller = new SystemEventController(service);
        Method detail = SystemEventController.class.getMethod("detail", String.class);
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            Set<ConstraintViolation<SystemEventController>> violations = validator.forExecutables()
                    .validateParameters(controller, detail, new Object[]{" "});

            assertThat(violations).extracting(ConstraintViolation::getMessage).containsExactly("消息 ID 不能为空");
        }
    }

    private OutboxMessage message(String messageId, String topic, OutboxStatus status) {
        return OutboxMessage.builder()
                .messageId(messageId)
                .topic(topic)
                .eventType("event.test")
                .businessType("EVENT_TEST")
                .businessKey(messageId)
                .status(status)
                .payload(Map.of("source", "integration"))
                .headers(Map.of("tenantId", "1"))
                .build();
    }

    private ReconsumeSystemEventCommand command(String messageId) {
        ReconsumeSystemEventCommand command = new ReconsumeSystemEventCommand();
        command.setMessageId(messageId);
        return command;
    }
}
