package io.mango.calendar.api.enums;

public enum CalendarDayType {
    WORKDAY,
    RESTDAY,
    LEGAL_HOLIDAY,
    ADJUSTED_WORKDAY,
    TEMP_CLOSED_DAY,
    TEMP_OPEN_DAY,

    /**
     * 兼容历史值，等同于 LEGAL_HOLIDAY。
     */
    HOLIDAY,

    /**
     * 兼容历史值，等同于 TEMP_CLOSED_DAY。
     */
    CUSTOM_CLOSED,

    /**
     * 兼容历史值，等同于 TEMP_OPEN_DAY。
     */
    CUSTOM_OPEN
}
