package io.mango.calendar.starter.controller;

import io.mango.authorization.api.annotation.ApiAccess;
import io.mango.authorization.api.enums.ApiResourceAccessMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CalendarControllerAccessModeTest {

    @Test
    void calendarCalculationControllerShouldUseLoginAccess() {
        ApiAccess apiAccess = CalendarController.class.getAnnotation(ApiAccess.class);

        assertNotNull(apiAccess);
        assertEquals(ApiResourceAccessMode.LOGIN, apiAccess.mode());
        assertEquals("", apiAccess.permission());
    }
}
