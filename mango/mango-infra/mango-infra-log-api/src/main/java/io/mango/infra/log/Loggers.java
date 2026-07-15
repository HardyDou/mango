package io.mango.infra.log;

import io.mango.common.contract.LocalCapabilityContract;

/**
 * Mango 日志分类名称。
 */
@LocalCapabilityContract
public final class Loggers {

    /**
     * 独立操作日志 appender 使用的 logger 名称。
     */
    public static final String OPERATION = "io.mango.infra.log.annotation.Log";

    private Loggers() {
    }
}
