package io.mango.auth.core.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LoginAttemptTracker
 *
 * @author Mango
 */
@DisplayName("LoginAttemptTracker Tests")
class LoginAttemptTrackerTest {

    private LoginAttemptTracker tracker;

    @BeforeEach
    void setUp() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        tracker = new LoginAttemptTracker(executor);
    }

    @AfterEach
    void tearDown() {
        tracker.shutdown();
    }

    @Test
    @DisplayName("Should not be locked out on first attempt")
    void recordFailedAttempt_firstAttempt_notLockedOut() {
        tracker.recordFailedAttempt("192.168.1.1");

        assertFalse(tracker.isLockedOut("192.168.1.1"));
    }

    @Test
    @DisplayName("Should be locked out after 5 failed attempts")
    void recordFailedAttempt_fiveAttempts_lockedOut() {
        for (int i = 0; i < 5; i++) {
            tracker.recordFailedAttempt("192.168.1.1");
        }

        assertTrue(tracker.isLockedOut("192.168.1.1"));
    }

    @Test
    @DisplayName("Should not be locked out before 5 attempts")
    void recordFailedAttempt_fourAttempts_notLockedOut() {
        for (int i = 0; i < 4; i++) {
            tracker.recordFailedAttempt("192.168.1.1");
        }

        assertFalse(tracker.isLockedOut("192.168.1.1"));
    }

    @Test
    @DisplayName("Should clear attempts on successful login")
    void clearAttempts_afterFailedAttempts_noLongerLockedOut() {
        for (int i = 0; i < 5; i++) {
            tracker.recordFailedAttempt("192.168.1.1");
        }
        assertTrue(tracker.isLockedOut("192.168.1.1"));

        tracker.clearAttempts("192.168.1.1");

        assertFalse(tracker.isLockedOut("192.168.1.1"));
    }

    @Test
    @DisplayName("Should return 0 lockout time when not locked out")
    void getRemainingLockoutMinutes_notLockedOut_returnsZero() {
        tracker.recordFailedAttempt("192.168.1.1");

        long remaining = tracker.getRemainingLockoutMinutes("192.168.1.1");

        assertEquals(0, remaining);
    }

    @Test
    @DisplayName("Should return positive lockout time when locked out")
    void getRemainingLockoutMinutes_lockedOut_returnsPositive() {
        for (int i = 0; i < 5; i++) {
            tracker.recordFailedAttempt("192.168.1.1");
        }

        long remaining = tracker.getRemainingLockoutMinutes("192.168.1.1");

        assertTrue(remaining > 0);
    }

    @Test
    @DisplayName("Should return 0 for non-existent key")
    void getRemainingLockoutMinutes_nonExistentKey_returnsZero() {
        long remaining = tracker.getRemainingLockoutMinutes("non-existent-key");

        assertEquals(0, remaining);
    }

    @Test
    @DisplayName("Should track multiple independent keys")
    void recordFailedAttempt_multipleKeys_trackedIndependently() {
        tracker.recordFailedAttempt("192.168.1.1");
        tracker.recordFailedAttempt("192.168.1.1");
        tracker.recordFailedAttempt("192.168.1.1");
        tracker.recordFailedAttempt("192.168.1.2");

        assertFalse(tracker.isLockedOut("192.168.1.1"));
        assertFalse(tracker.isLockedOut("192.168.1.2"));
    }

    @Test
    @DisplayName("Should not be locked out for non-existent key")
    void isLockedOut_nonExistentKey_returnsFalse() {
        assertFalse(tracker.isLockedOut("non-existent-key"));
    }
}
