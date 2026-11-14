package io.mango.workflow.core.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 按业务申请可筛属性过滤的持久化条件。
 *
 * <p>由服务层把 API 查询对象转换成该条件后再交给 Mapper，避免 API 协议模型进入持久化层。
 * 多个条件之间是「且」，同一条件的多个值之间是「或」。
 */
@Getter
@Builder
public class WorkflowApplyAttributeCondition {

    private final String key;
    private final List<String> values;
}
