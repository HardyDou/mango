package cn.keking.service;

import java.time.Duration;

public interface ConversionTaskLease {
    boolean tryAcquire(String key, Duration leaseDuration) throws InterruptedException;
    void release(String key);
}
