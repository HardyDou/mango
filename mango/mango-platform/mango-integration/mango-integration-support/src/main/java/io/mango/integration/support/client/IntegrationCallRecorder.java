package io.mango.integration.support.client;

/**
 * 集成调用记录写入能力。
 *
 * <p>由集成模块实现并注册，业务域只需通过客户端触发记录。</p>
 */
public interface IntegrationCallRecorder {

    /**
     * 记录一次外部调用。
     *
     * @param record 调用元数据
     */
    void record(IntegrationCallRecord record);
}
