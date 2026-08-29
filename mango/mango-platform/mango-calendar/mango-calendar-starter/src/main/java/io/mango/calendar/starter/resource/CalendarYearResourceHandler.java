package io.mango.calendar.starter.resource;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mango.calendar.api.command.ImportCalendarDayCommand;
import io.mango.calendar.api.command.ImportCalendarDaysCommand;
import io.mango.calendar.api.command.InitCalendarYearCommand;
import io.mango.calendar.api.command.UpdateCalendarYearEnabledCommand;
import io.mango.calendar.core.entity.CalendarEntity;
import io.mango.calendar.core.mapper.CalendarDayMapper;
import io.mango.calendar.core.mapper.CalendarMapper;
import io.mango.calendar.core.service.ICalendarAdminService;
import io.mango.infra.context.api.MangoContextHolder;
import io.mango.infra.context.api.MangoContextSnapshot;
import io.mango.resource.support.ResourceHandler;
import io.mango.resource.support.PortableResourceIds;
import io.mango.resource.support.model.ResourceDeclaration;
import io.mango.resource.support.model.ResourceHandlerSpec;
import io.mango.resource.support.model.ResourceSyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CalendarYearResourceHandler implements ResourceHandler {

    private static final String TARGET_TABLE = "calendar_day";

    private final CalendarMapper calendarMapper;
    private final CalendarDayMapper calendarDayMapper;
    private final ICalendarAdminService calendarAdminService;
    private final ObjectMapper objectMapper;

    @Override
    public String resourceType() {
        return CalendarResourceTypes.YEAR;
    }

    @Override
    public List<String> dependsOnResourceTypes() {
        return List.of(CalendarResourceTypes.DEFINITION);
    }

    @Override
    public ResourceHandlerSpec spec() {
        return ResourceHandlerSpec.builder()
                .resourceType(resourceType())
                .requiredField("tenantId")
                .requiredField("calendarCode")
                .requiredField("year")
                .requiredField("items")
                .fieldDescription("tenantId", "日历所属租户。")
                .fieldDescription("calendarCode", "已声明的日历编码。")
                .fieldDescription("year", "初始化年度，范围 1900-2100。")
                .fieldDescription("items", "覆盖默认工作日规则的法定节假日和调休日期。")
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult upsert(ResourceDeclaration resource) {
        Payload payload = Payload.from(resource, objectMapper);
        return withTenant(payload.tenantId(), () -> {
            CalendarEntity calendar = calendarMapper.selectByCode(payload.tenantId(), payload.calendarCode());
            if (calendar == null) {
                throw new IllegalStateException("Calendar definition not found: " + payload.calendarCode());
            }
            long count = calendarDayMapper.countByYear(payload.tenantId(), calendar.getId(), payload.year());
            if (count == 0) {
                InitCalendarYearCommand init = new InitCalendarYearCommand();
                init.setCalendarCode(payload.calendarCode());
                init.setYear(payload.year());
                init.setOverwrite(false);
                calendarAdminService.initResourceCalendarYear(init,
                        date -> PortableResourceIds.stable(TARGET_TABLE,
                                payload.tenantId(), payload.calendarCode(), date));
                if (!payload.items().isEmpty()) {
                    ImportCalendarDaysCommand importCommand = new ImportCalendarDaysCommand();
                    importCommand.setCalendarCode(payload.calendarCode());
                    importCommand.setYear(payload.year());
                    importCommand.setItems(payload.items());
                    calendarAdminService.importCalendarDays(importCommand);
                }
            }
            return ResourceSyncResult.of(calendar.getId(), TARGET_TABLE,
                    "Calendar year synced: " + payload.calendarCode() + " " + payload.year());
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResourceSyncResult disable(ResourceDeclaration resource) {
        String tenantId = CalendarResourceFields.requiredText(resource, "tenantId");
        String calendarCode = CalendarResourceFields.requiredText(resource, "calendarCode");
        int year = CalendarResourceFields.requiredInt(resource, "year");
        return withTenant(tenantId, () -> {
            CalendarEntity calendar = calendarMapper.selectByCode(tenantId, calendarCode);
            if (calendar == null || calendarDayMapper.countByYear(tenantId, calendar.getId(), year) == 0) {
                return ResourceSyncResult.of(null, TARGET_TABLE, "Calendar year not found");
            }
            UpdateCalendarYearEnabledCommand command = new UpdateCalendarYearEnabledCommand();
            command.setCalendarCode(calendarCode);
            command.setYear(year);
            command.setEnabled(0);
            calendarAdminService.updateCalendarYearEnabled(command);
            return ResourceSyncResult.of(calendar.getId(), TARGET_TABLE,
                    "Calendar year disabled: " + calendarCode + " " + year);
        });
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

    private record Payload(String tenantId, String calendarCode, int year, List<ImportCalendarDayCommand> items) {

        private static Payload from(ResourceDeclaration resource, ObjectMapper objectMapper) {
            int year = CalendarResourceFields.requiredInt(resource, "year");
            if (year < 1900 || year > 2100) {
                throw new IllegalArgumentException("Calendar resource year must be between 1900 and 2100");
            }
            Object rawItems = CalendarResourceFields.optionalValue(resource, "items");
            if (rawItems == null) {
                throw new IllegalArgumentException("Missing calendar resource field: items");
            }
            JavaType itemType = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, ImportCalendarDayCommand.class);
            List<ImportCalendarDayCommand> items = objectMapper.convertValue(rawItems, itemType);
            if (items.size() > 366) {
                throw new IllegalArgumentException("Calendar resource items cannot exceed 366");
            }
            for (ImportCalendarDayCommand item : items) {
                if (item.getDate() == null || item.getDayType() == null || item.getDate().getYear() != year) {
                    throw new IllegalArgumentException("Calendar resource item date/type is invalid for year " + year);
                }
            }
            return new Payload(
                    CalendarResourceFields.requiredText(resource, "tenantId"),
                    CalendarResourceFields.requiredText(resource, "calendarCode"),
                    year,
                    List.copyOf(items));
        }
    }
}
