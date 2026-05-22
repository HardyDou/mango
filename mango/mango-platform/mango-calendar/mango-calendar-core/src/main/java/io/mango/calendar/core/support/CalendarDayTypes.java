package io.mango.calendar.core.support;

import io.mango.calendar.api.enums.CalendarDayType;

import java.time.DayOfWeek;
import java.util.EnumSet;

public final class CalendarDayTypes {

    private static final EnumSet<CalendarDayType> WORKDAY_TYPES = EnumSet.of(
            CalendarDayType.WORKDAY,
            CalendarDayType.ADJUSTED_WORKDAY,
            CalendarDayType.TEMP_OPEN_DAY,
            CalendarDayType.CUSTOM_OPEN
    );

    private CalendarDayTypes() {
    }

    public static CalendarDayType normalize(CalendarDayType dayType) {
        if (dayType == null) {
            return null;
        }
        return switch (dayType) {
            case HOLIDAY -> CalendarDayType.LEGAL_HOLIDAY;
            case CUSTOM_CLOSED -> CalendarDayType.TEMP_CLOSED_DAY;
            case CUSTOM_OPEN -> CalendarDayType.TEMP_OPEN_DAY;
            default -> dayType;
        };
    }

    public static CalendarDayType normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return normalize(CalendarDayType.valueOf(value));
    }

    public static boolean isWorkday(CalendarDayType dayType) {
        return WORKDAY_TYPES.contains(normalize(dayType));
    }

    public static boolean isDefaultType(CalendarDayType dayType) {
        CalendarDayType normalized = normalize(dayType);
        return normalized == CalendarDayType.WORKDAY || normalized == CalendarDayType.RESTDAY;
    }

    public static CalendarDayType defaultType(DayOfWeek dayOfWeek) {
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY
                ? CalendarDayType.RESTDAY
                : CalendarDayType.WORKDAY;
    }
}
