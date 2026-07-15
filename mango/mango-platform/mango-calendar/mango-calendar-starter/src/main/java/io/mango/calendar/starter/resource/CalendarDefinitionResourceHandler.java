package io.mango.calendar.starter.resource;

import io.mango.calendar.api.command.CreateCalendarCommand;
import io.mango.calendar.api.command.UpdateCalendarCommand;
import io.mango.calendar.api.command.UpdateCalendarStatusCommand;
import io.mango.calendar.core.entity.CalendarEntity;
import io.mango.calendar.core.mapper.CalendarMapper;
import io.mango.calendar.core.service.ICalendarAdminService;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.resource.api.ResourceHandler;
import io.mango.resource.api.model.ResourceDeclaration;
import io.mango.resource.api.model.ResourceHandlerSpec;
import io.mango.resource.api.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CalendarDefinitionResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "calendar";

    private final CalendarMapper calendarMapper;
    private final ICalendarAdminService calendarAdminService;

    @Override
    public String resourceType() {
        return CalendarResourceTypes.DEFINITION;
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("tenantId")
                .requiredField("calendarCode")
                .requiredField("calendarName")
                .requiredField("status")
                .fieldDescription("tenantId", "日历所属租户。")
                .fieldDescription("calendarCode", "租户内唯一的日历编码。")
                .fieldDescription("calendarName", "日历名称。")
                .fieldDescription("status", "日历状态：1-启用，0-停用。")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        Payload payload = Payload.from(resource);
        return withTenant(payload.tenantId(), () -> {
            CalendarEntity existing = calendarMapper.selectByCode(payload.tenantId(), payload.calendarCode());
            Long targetId;
            if (existing == null) {
                CreateCalendarCommand command = new CreateCalendarCommand();
                command.setCalendarCode(payload.calendarCode());
                command.setCalendarName(payload.calendarName());
                targetId = calendarAdminService.createCalendar(command);
            } else {
                UpdateCalendarCommand command = new UpdateCalendarCommand();
                command.setId(existing.getId());
                command.setCalendarCode(payload.calendarCode());
                command.setCalendarName(payload.calendarName());
                calendarAdminService.updateCalendar(command);
                targetId = existing.getId();
            }
            updateStatus(targetId, payload.status());
            return ResourceSyncResult.of(targetId, TARGET_TABLE,
                    "Calendar definition synced: " + payload.calendarCode());
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        String tenantId = CalendarResourceFields.requiredText(resource, "tenantId");
        String calendarCode = CalendarResourceFields.requiredText(resource, "calendarCode");
        return withTenant(tenantId, () -> {
            CalendarEntity existing = calendarMapper.selectByCode(tenantId, calendarCode);
            if (existing == null) {
                return ResourceSyncResult.of(null, TARGET_TABLE, "Calendar definition not found");
            }
            updateStatus(existing.getId(), 0);
            return ResourceSyncResult.of(existing.getId(), TARGET_TABLE,
                    "Calendar definition disabled: " + calendarCode);
        });
    }

    private void updateStatus(Long id, int status) {
        UpdateCalendarStatusCommand command = new UpdateCalendarStatusCommand();
        command.setId(id);
        command.setStatus(status);
        calendarAdminService.updateCalendarStatus(command);
    }

    private <T> T withTenant(String tenantId, java.util.function.Supplier<T> action) {
        MangoContextSnapshot previous = MangoContextHolder.get();
        try {
            MangoContextHolder.set(previous.withTenantId(tenantId));
            return action.get();
        } finally {
            MangoContextHolder.set(previous);
        }
    }

    private record Payload(String tenantId, String calendarCode, String calendarName, int status) {

        private static Payload from(ResourceDeclaration resource) {
            int status = CalendarResourceFields.requiredInt(resource, "status");
            if (status < 0 || status > 1) {
                throw new IllegalArgumentException("Calendar resource status must be 0 or 1");
            }
            return new Payload(
                    CalendarResourceFields.requiredText(resource, "tenantId"),
                    CalendarResourceFields.requiredText(resource, "calendarCode"),
                    CalendarResourceFields.requiredText(resource, "calendarName"),
                    status);
        }
    }
}
