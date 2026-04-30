package io.mango.infra.persistence.api.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 带审计字段的基础实体。
 *
 * @param <ID> 主键类型。
 */
@Getter
@Setter
public class AuditableEntity<ID extends Serializable> extends BaseEntity<ID> {

    private static final long serialVersionUID = 1L;

    /**
     * 创建人 ID。
     */
    private Long createdBy;

    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 更新人 ID。
     */
    private Long updatedBy;

    /**
     * 更新时间。
     */
    private LocalDateTime updatedAt;
}
